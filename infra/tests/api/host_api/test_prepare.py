"""Tests enqueuing host preparing task."""

import pytest
import http.client

import walle.tasks
import walle.util.tasks
from infra.walle.server.tests.lib.util import (
    TestCase,
    generate_host_action_authentication_tests,
    hosts_api_url,
    drop_none,
    mock_schedule_host_preparing,
    monkeypatch_function,
    monkeypatch_audit_log,
    BOT_PROJECT_ID,
)
from walle import restrictions
from walle.clients import inventory, bot
from walle.constants import NetworkTarget, FLEXY_EINE_PROFILE
from walle.errors import InvalidHostStateError
from walle.hosts import HostState, HostStatus
from walle.stages import Stages, get_by_name
from walle.util.deploy_config import DeployConfigPolicies


@pytest.fixture
def test(mp, monkeypatch_timestamp, request):
    monkeypatch_audit_log(mp)
    monkeypatch_function(mp, inventory.get_eine_profiles, return_value=["profile-mock", FLEXY_EINE_PROFILE])
    mp.function(inventory.check_deploy_configuration)
    mp.function(
        bot.missed_preordered_hosts,
        return_value={
            10: {"inv": 10, "fqdn": "not-collected-10.mock", "order": 9999},
            11: {"inv": 11, "fqdn": "missing-in-preorder-11.mock", "order": 9999},
        },
    )
    return TestCase.create(request)


generate_host_action_authentication_tests(globals(), "/prepare", {"profile": "profile-mock"})


@pytest.mark.parametrize("update_firmware", [True, False])
def test_with_no_hostname(test, update_firmware):
    host = test.mock_host({"name": None, "state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, "inv", action="/prepare"), data={"skip_profile": True, "update_firmware": update_firmware}
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(host, update_firmware_needed=update_firmware)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("update_firmware", [True, False])
def test_with_auto_healing(test, update_firmware):
    host = test.mock_host({"name": None, "state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, "inv", action="/prepare"),
        data={"skip_profile": True, "with_auto_healing": True, "update_firmware": update_firmware},
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(host, with_auto_healing=True, update_firmware_needed=update_firmware)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


def test_profile_provisioner_config(test):
    host = test.mock_host({"state": HostState.FREE})

    deploy_config_policy = DeployConfigPolicies.DISKMANAGER
    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"),
        data={
            "profile": "profile-mock",
            "provisioner": test.host_provisioner,
            "config": test.host_deploy_config,
            "deploy_config_policy": deploy_config_policy,
            "update_firmware": False,
        },
    )

    inventory.check_deploy_configuration.assert_called_once_with(
        test.host_provisioner, test.host_deploy_config, None, None, False, None, deploy_config_policy
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(
        host,
        profile="profile-mock",
        provisioner=test.host_provisioner,
        config=test.host_deploy_config,
        deploy_config_policy=deploy_config_policy,
        custom_deploy_config=True,
        update_firmware_needed=False,
    )
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("update_firmware", [True, False])
def test_bot_project_id(test, update_firmware):
    project = test.mock_project({"id": "project-mock", "bot_project_id": BOT_PROJECT_ID})
    host = test.mock_host({"project": project.id, "state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"), data={"skip_profile": True, "update_firmware": update_firmware}
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(host, update_firmware_needed=update_firmware)

    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("update_firmware", [True, False])
@pytest.mark.parametrize("deploy_tags", [["request-tag"], [], None])
def test_deploy_config_tags(test, deploy_tags, update_firmware):
    host = test.mock_host({"state": HostState.FREE})

    deploy_config = "prepare-deploy-config"
    need_certificate = False

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"),
        data=drop_none(
            {
                "skip_profile": True,
                "config": deploy_config,
                "deploy_tags": deploy_tags,
                "update_firmware": update_firmware,
            }
        ),
    )

    inventory.check_deploy_configuration.assert_called_once_with(
        test.project_provisioner, deploy_config, None, deploy_tags or None, need_certificate, None, None
    )
    mock_schedule_host_preparing(
        host,
        provisioner=test.project_provisioner,
        config=deploy_config,
        deploy_tags=deploy_tags or None,
        custom_deploy_config=True,
        update_firmware_needed=update_firmware,
    )

    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.parametrize("update_firmware", [True, False])
@pytest.mark.parametrize("deploy_network", NetworkTarget.DEPLOYABLE + [None])
def test_deploy_network(test, deploy_network, update_firmware):
    host = test.mock_host({"state": HostState.FREE})

    deploy_config = "prepare-deploy-config"
    need_certificate = False

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"),
        data=drop_none(
            {
                "skip_profile": True,
                "config": deploy_config,
                "deploy_network": deploy_network,
                "update_firmware": update_firmware,
            }
        ),
    )

    inventory.check_deploy_configuration.assert_called_once_with(
        test.project_provisioner, deploy_config, None, None, need_certificate, deploy_network, None
    )
    mock_schedule_host_preparing(
        host,
        provisioner=test.project_provisioner,
        config=deploy_config,
        deploy_network=deploy_network,
        custom_deploy_config=True,
        update_firmware_needed=update_firmware,
    )

    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


def test_profile_config(test):
    host = test.mock_host({"state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"), data={"profile": "profile-mock", "config": test.host_deploy_config}
    )

    inventory.check_deploy_configuration.assert_called_once_with(
        test.project_provisioner, test.host_deploy_config, None, None, False, None, None
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(
        host,
        profile="profile-mock",
        provisioner=test.project_provisioner,
        config=test.host_deploy_config,
        custom_deploy_config=True,
    )
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


def test_profile_and_deploy_with_certificate(test):
    test.mock_project({"id": "test-project", "name": "Test project", "certificate_deploy": True})
    host = test.mock_host({"state": HostState.FREE, "project": "test-project"})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"),
        data={"profile": "profile-mock", "provisioner": test.host_provisioner, "config": test.host_deploy_config},
    )

    inventory.check_deploy_configuration.assert_called_once_with(
        test.host_provisioner, test.host_deploy_config, None, None, True, None, None
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(
        host,
        profile="profile-mock",
        provisioner=test.host_provisioner,
        config=test.host_deploy_config,
        need_certificate=True,
        custom_deploy_config=True,
    )
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


def test_skip_profile_no_config(test):
    host = test.mock_host({"state": HostState.FREE})

    result = test.api_client.post(hosts_api_url(host, action="/prepare"), data={"skip_profile": True})
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(host)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("extra_vlans", [[], [1, 2, 3]])
def test_extra_vlans(test, extra_vlans):
    host = test.mock_host({"state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"), data={"skip_profile": True, "extra_vlans": extra_vlans}
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(host, extra_vlans=extra_vlans or None)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("profile_tags", (["tag-1", "tag-2", "tag-3=None"], []))
def test_profile_tags(test, profile_tags):
    host = test.mock_host({"state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"), data={"profile": "profile-mock", "profile_tags": profile_tags}
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(host, profile="profile-mock", profile_tags=profile_tags)
    assert result.json == host.to_api_obj()

    eine_profile_stage = get_by_name(host.task.stages, Stages.PROFILE, Stages.EINE_PROFILE)
    assert sorted(eine_profile_stage.params["profile_tags"]) == sorted(profile_tags)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "user_restrictions,result_restrictions",
    ((None, None), ([restrictions.AUTOMATED_REBOOT, restrictions.AUTOMATED_REDEPLOY], [restrictions.AUTOMATED_REBOOT])),
)
def test_restrictions(test, user_restrictions, result_restrictions):
    host = test.mock_host({"state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"),
        data=drop_none({"skip_profile": True, "restrictions": user_restrictions}),
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(host, restrictions=result_restrictions)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", set(HostState.ALL) - {HostState.FREE})
def test_reject_by_state(test, state):
    host = test.mock_host({"state": state, "status": HostStatus.READY})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"), data={"profile": "profile-mock", "config": "config-mock"}
    )
    allowed_states = [HostState.FREE]
    allowed_statuses = HostStatus.ALL_STEADY
    error_message = InvalidHostStateError(host, allowed_states=allowed_states, allowed_statuses=allowed_statuses)
    assert result.status_code == http.client.CONFLICT
    assert result.json['message'] == str(error_message)

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
def test_reject_by_status(test, status):
    host = test.mock_host({"state": HostState.FREE, "status": status})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"), data={"profile": "profile-mock", "config": "config-mock"}
    )
    allowed_states = [HostState.FREE]
    allowed_statuses = HostStatus.ALL_STEADY
    error_message = InvalidHostStateError(host, allowed_states=allowed_states, allowed_statuses=allowed_statuses)
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == str(error_message)

    test.hosts.assert_equal()


@pytest.mark.parametrize("ignore_cms", [None, True, False])
def test_ignore_cms(test, mp, ignore_cms):
    mock_cms_probe = mp.function(walle.util.tasks.reject_request_if_needed)
    host = test.mock_host({"state": HostState.FREE})

    result = test.api_client.post(
        hosts_api_url(host, action="/prepare"), data=drop_none({"skip_profile": True, "ignore_cms": ignore_cms})
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_preparing(host, ignore_cms=ignore_cms is True)
    assert result.json == host.to_api_obj()
    # don't ask cms for permission here, we don't know host's name yet.
    assert not mock_cms_probe.called
    test.hosts.assert_equal()


def test_profile_checking(test):
    host = test.mock_host({"state": HostState.FREE})

    result = test.api_client.post(hosts_api_url(host, action="/prepare"), data={"profile": "invalid-profile-mock"})
    assert result.status_code == http.client.BAD_REQUEST
    assert "Invalid Einstellung profile name" in result.json["message"]

    test.hosts.assert_equal()


def test_not_collected_hosts_raise(test):
    host = test.mock_host({"inv": 10, "state": HostState.FREE})

    result = test.api_client.post(hosts_api_url(host, action="/prepare"), data={})
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"].startswith(
        "Host #10 (name: not-collected-10.mock) has not been collected from preorder #9999"
    )
