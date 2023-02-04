"""Tests enqueuing host profiling task."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    generate_host_action_authentication_tests,
    hosts_api_url,
    mock_schedule_host_profiling,
    monkeypatch_function,
    monkeypatch_request_params_validation,
    monkeypatch_config,
)
from walle import restrictions, hosts, constants
from walle.admin_requests.severity import get_eine_tag_by_repair_request_severity
from walle.clients import inventory
from walle.clients.eine import EineProfileTags, ProfileMode
from walle.constants import NetworkTarget
from walle.hosts import HostState, HostStatus
from walle.projects import RepairRequestSeverity
from walle.stages import Stages, get_by_name
from walle.util.deploy_config import DeployConfigPolicies
from walle.util.misc import drop_none

PROFILE_NAME = "valid-profile-mock"
DEPLOY_CONFIG_NAME = "valid-config-mock"
DEPLOY_CONFIG_POLICY = DeployConfigPolicies.DISKMANAGER


@pytest.fixture
def test(mp, monkeypatch_timestamp, monkeypatch_audit_log, request):
    monkeypatch_function(mp, inventory.get_eine_profiles, return_value=[PROFILE_NAME, DEPLOY_CONFIG_NAME])
    return TestCase.create(request)


@pytest.fixture(params=[True, False])
def is_eaas(mp, request):
    if request.param:
        mp.setattr(hosts, "EINE_PROFILES_WITH_DC_SUPPORT", [PROFILE_NAME])


generate_host_action_authentication_tests(globals(), "/profile", {"profile": "profile-mock"})  # Use non-valid profile


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("profile_tags", [None, [], ["tag-1", "tag-2"], [EineProfileTags.FULL_PROFILING]])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.all_status_owner_combinations()
def test_enqueueing(test, mp, is_eaas, host_id_field, status, owner, profile_tags):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status, "status_author": owner})

    expected_notify_fsm_calls = []

    data = {"profile": PROFILE_NAME}
    if profile_tags is not None:
        data["profile_tags"] = profile_tags

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/profile"), data=data)
    assert result.status_code == http.client.OK

    mock_schedule_host_profiling(
        host,
        profile=PROFILE_NAME,
        manual=True,
        profile_tags=profile_tags,
        expected_notify_fsm_calls=expected_notify_fsm_calls,
    )
    assert result.json == host.to_api_obj()

    eine_profile_stage = get_by_name(host.task.stages, Stages.PROFILE, Stages.EINE_PROFILE)
    assert sorted(eine_profile_stage.params["profile_tags"]) == sorted(profile_tags or [])

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("redeploy", [True, False, None])
@pytest.mark.parametrize("need_certificate", [True, False])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_disk_dangerous(mp, test, host_id_field, redeploy, need_certificate):
    test.mock_project({"id": "test-project", "name": "Test project", "certificate_deploy": need_certificate})
    # this operation enqueues profile with reboot stage
    host = test.mock_host({"state": HostState.ASSIGNED, "project": "test-project"})
    mp.setattr(hosts, "EINE_PROFILES_WITH_DC_SUPPORT", [PROFILE_NAME])

    profile_tags = [EineProfileTags.DANGEROUS_LOAD, EineProfileTags.FULL_PROFILING]

    expected_notify_fsm_calls = []

    data = drop_none({"profile": PROFILE_NAME, "profile_tags": profile_tags, "redeploy": redeploy})

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/profile"), data=data)
    assert result.status_code == http.client.OK

    if redeploy is False:
        mock_schedule_host_profiling(
            host,
            manual=True,
            profile=PROFILE_NAME,
            profile_tags=profile_tags,
            expected_notify_fsm_calls=expected_notify_fsm_calls,
        )
    else:
        mock_schedule_host_profiling(
            host,
            manual=True,
            profile=PROFILE_NAME,
            profile_tags=profile_tags,
            provisioner=constants.PROVISIONER_LUI,
            deploy_config="config-mock",
            need_certificate=need_certificate,
            expected_notify_fsm_calls=expected_notify_fsm_calls,
        )

    assert result.json == host.to_api_obj()

    eine_profile_stage = get_by_name(host.task.stages, Stages.PROFILE, Stages.EINE_PROFILE)
    assert sorted(eine_profile_stage.params["profile_tags"]) == sorted(profile_tags)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("need_certificate", [True, False])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing__with_redeploy(mp, test, host_id_field, need_certificate):
    test.mock_project({"id": "test-project", "name": "Test project", "certificate_deploy": need_certificate})
    # this operation enqueues profile with reboot stage
    host = test.mock_host({"state": HostState.ASSIGNED, "project": "test-project"})

    expected_notify_fsm_calls = []

    data = {"profile": PROFILE_NAME, "redeploy": True}

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/profile"), data=data)
    assert result.status_code == http.client.OK

    mock_schedule_host_profiling(
        host,
        manual=True,
        profile=PROFILE_NAME,
        provisioner=constants.PROVISIONER_LUI,
        deploy_config="config-mock",
        need_certificate=need_certificate,
        expected_notify_fsm_calls=expected_notify_fsm_calls,
    )
    assert result.json == host.to_api_obj()

    eine_profile_stage = get_by_name(host.task.stages, Stages.PROFILE, Stages.EINE_PROFILE)
    assert sorted(eine_profile_stage.params["profile_tags"]) == []

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize(
    ("provisioner", "deploy_tags", "deploy_network"),
    [
        (constants.PROVISIONER_EINE, None, None),
        (constants.PROVISIONER_EINE, [], None),
        (constants.PROVISIONER_EINE, ["eaas"], None),
        (constants.PROVISIONER_LUI, None, None),
        (constants.PROVISIONER_LUI, [], None),
        (constants.PROVISIONER_LUI, [], NetworkTarget.SERVICE),
        (None, None, NetworkTarget.SERVICE),
    ],
)
@pytest.mark.parametrize("redeploy", [True, None])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing__with_redeploy_config(mp, test, host_id_field, redeploy, provisioner, deploy_tags, deploy_network):
    # this operation enqueues profile with reboot stage
    host = test.mock_host({"state": HostState.ASSIGNED})
    monkeypatch_request_params_validation(mp)

    expected_notify_fsm_calls = []

    data = {
        "profile": PROFILE_NAME,
        "redeploy": redeploy,
        "provisioner": provisioner,
        "config": DEPLOY_CONFIG_NAME,
        "deploy_config_policy": DEPLOY_CONFIG_POLICY,
        "deploy_tags": deploy_tags,
        "deploy_network": deploy_network,
    }

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/profile"), data=drop_none(data))
    assert result.status_code == http.client.OK

    mock_schedule_host_profiling(
        host,
        manual=True,
        profile=PROFILE_NAME,
        provisioner=provisioner or constants.PROVISIONER_LUI,
        deploy_config=DEPLOY_CONFIG_NAME,
        deploy_config_policy=DEPLOY_CONFIG_POLICY,
        deploy_tags=deploy_tags or None,
        deploy_config_forced=True,
        deploy_network=deploy_network,
        expected_notify_fsm_calls=expected_notify_fsm_calls,
    )

    assert result.json == host.to_api_obj()

    eine_profile_stage = get_by_name(host.task.stages, Stages.PROFILE, Stages.EINE_PROFILE)
    assert sorted(eine_profile_stage.params["profile_tags"]) == []

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_config_with_redeploy_forbidden(test, host_id_field):
    # this operation enqueues profile with reboot stage
    host = test.mock_host({"state": HostState.ASSIGNED})

    data = {"profile": PROFILE_NAME, "redeploy": False, "config": DEPLOY_CONFIG_NAME}

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/profile"), data=drop_none(data))
    assert result.status_code == http.client.BAD_REQUEST

    test.hosts.assert_equal()


def test_reject_by_invalid_profile(test):
    host = test.mock_host({"state": HostState.ASSIGNED})

    result = test.api_client.post(hosts_api_url(host, action="/profile"), data={"profile": "invalid-profile-mock"})
    assert result.status_code == http.client.BAD_REQUEST
    assert "Invalid Einstellung profile name" in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
def test_reject_by_status(test, status):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status})

    result = test.api_client.post(hosts_api_url(host, action="/profile"), data={"profile": PROFILE_NAME})
    assert result.status_code == http.client.CONFLICT
    assert "The host has an invalid state for this operation" in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
def test_reject_by_maintenance(test, mock_maintenance_host):
    host = mock_maintenance_host(test)

    result = test.api_client.post(hosts_api_url(host, action="/profile"), data={"profile": PROFILE_NAME})

    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
def test_allow_by_ignore_maintenance(test, mock_maintenance_host):
    host = mock_maintenance_host(test)

    result = test.api_client.post(
        hosts_api_url(host, action="/profile"), query_string="ignore_maintenance=true", data={"profile": PROFILE_NAME}
    )

    assert result.status_code == http.client.OK
    mock_schedule_host_profiling(host, manual=True, profile=PROFILE_NAME)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("restriction", [restrictions.REBOOT, restrictions.PROFILE])
def test_reject_by_restriction(test, restriction):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY, "restrictions": [restriction]})

    result = test.api_client.post(hosts_api_url(host, action="/profile"), data={"profile": PROFILE_NAME})
    assert result.status_code == http.client.CONFLICT
    assert "Operation restricted for this host" in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_reject")
def test_reject_by_cms(test):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/profile"), data={"profile": PROFILE_NAME})
    assert result.status_code == http.client.CONFLICT
    assert "The request has been rejected by CMS" in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize(
    ["profile_mode", "profile_tags"],
    [
        (None, []),
        (ProfileMode.DEFAULT, ProfileMode.DEFAULT_ADD_TAGS),
        (ProfileMode.HIGHLOAD_TEST, ProfileMode.HIGHLOAD_TEST_ADD_TAGS),
        (ProfileMode.FIRMWARE_UPDATE, ProfileMode.FIRMWARE_UPDATE_ADD_TAGS),
        (ProfileMode.DISK_RW_TEST, ProfileMode.DISK_RW_TEST_ADD_TAGS),
        (ProfileMode.SWP_UP, ProfileMode.SWP_UP_ADD_TAGS),
    ],
)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_with_profile_mode(test, host_id_field, profile_mode, profile_tags, mp):
    mp.setattr(hosts, "EINE_PROFILES_WITH_DC_SUPPORT", [PROFILE_NAME])

    host = test.mock_host({"state": HostState.ASSIGNED})
    expected_notify_fsm_calls = []
    data = dict(profile=PROFILE_NAME, profile_mode=profile_mode, redeploy=False)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/profile"), data=data)
    assert result.status_code == http.client.OK

    mock_schedule_host_profiling(
        host,
        profile=PROFILE_NAME,
        manual=True,
        profile_mode=profile_mode,
        expected_notify_fsm_calls=expected_notify_fsm_calls,
    )
    assert result.json == host.to_api_obj()

    eine_profile_stage = get_by_name(host.task.stages, Stages.PROFILE, Stages.EINE_PROFILE)
    assert sorted(eine_profile_stage.params["profile_tags"]) == sorted(profile_tags)

    test.hosts.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("repair_request_severity", [None] + RepairRequestSeverity.ALL)
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_with_repair_request_severity_for_admins(test, host_id_field, repair_request_severity, mp):
    mp.setattr(hosts, "EINE_PROFILES_WITH_DC_SUPPORT", [PROFILE_NAME])

    host = test.mock_host({"state": HostState.ASSIGNED})
    expected_notify_fsm_calls = []
    data = dict(profile=PROFILE_NAME, repair_request_severity=repair_request_severity, redeploy=False)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/profile"), data=data)
    assert result.status_code == http.client.OK

    mock_schedule_host_profiling(
        host,
        profile=PROFILE_NAME,
        manual=True,
        repair_request_severity=get_eine_tag_by_repair_request_severity(repair_request_severity)
        if repair_request_severity
        else None,
        expected_notify_fsm_calls=expected_notify_fsm_calls,
    )
    assert result.json == host.to_api_obj()

    eine_profile_stage = get_by_name(host.task.stages, Stages.PROFILE, Stages.EINE_PROFILE)

    if repair_request_severity:
        assert eine_profile_stage.params["repair_request_severity"] == get_eine_tag_by_repair_request_severity(
            repair_request_severity
        )
    else:
        assert "repair_request_severity" not in eine_profile_stage.params

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_with_high_repair_request_severity_by_non_admin(test, host_id_field, mp):
    mp.setattr(hosts, "EINE_PROFILES_WITH_DC_SUPPORT", [PROFILE_NAME])

    host = test.mock_host({"state": HostState.ASSIGNED})
    data = dict(profile=PROFILE_NAME, repair_request_severity=RepairRequestSeverity.HIGH, redeploy=False)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/profile"), data=data)
    assert result.status_code == http.client.FORBIDDEN

    test.hosts.assert_equal()
