"""Tests enqueuing host redeployment task."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    generate_host_action_authentication_tests,
    hosts_api_url,
    mock_schedule_host_redeployment,
    monkeypatch_request_params_validation,
    monkeypatch_config,
)
from walle import constants as walle_constants, restrictions
from walle.clients import inventory
from walle.constants import NetworkTarget, PROVISIONERS
from walle.hosts import HostState, HostStatus
from walle.util.deploy_config import DeployConfigPolicies
from walle.util.misc import drop_none


@pytest.fixture
def test(mp, monkeypatch_timestamp, monkeypatch_audit_log, request):
    return TestCase.create(request)


generate_host_action_authentication_tests(globals(), "/redeploy", {})


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.all_status_owner_combinations()
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.parametrize("provisioner", PROVISIONERS)
def test_enqueueing(mp, test, host_id_field, status, owner, provisioner):
    host = test.mock_host(
        {"state": HostState.ASSIGNED, "status": status, "status_author": owner, "provisioner": provisioner}
    )

    expected_notify_fsm_calls = []
    monkeypatch_request_params_validation(mp)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=drop_none({}))
    assert result.status_code == http.client.OK

    mock_schedule_host_redeployment(host, manual=True, expected_notify_fsm_calls=expected_notify_fsm_calls)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("check", (None, True, False))
def test_enqueueing_with_check_param(mp, test, check):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    monkeypatch_request_params_validation(mp)
    result = test.api_client.post(hosts_api_url(host, action="/redeploy"), data=drop_none({"check": check}))
    assert result.status_code == http.client.OK

    mock_schedule_host_redeployment(host, manual=True, check=check is not False)
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("with_auto_healing", (None, True, False))
def test_enqueueing_with_auto_healing_param(mp, test, with_auto_healing):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    monkeypatch_request_params_validation(mp)
    result = test.api_client.post(
        hosts_api_url(host, action="/redeploy"), data=drop_none({"with_auto_healing": with_auto_healing})
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_redeployment(host, manual=True, with_auto_healing=with_auto_healing)
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("disable_admin_requests", (None, True, False))
def test_enqueueing_with_disable_admin_requests_param(mp, test, disable_admin_requests):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    monkeypatch_request_params_validation(mp)
    result = test.api_client.post(
        hosts_api_url(host, action="/redeploy"), data=drop_none({"disable_admin_requests": disable_admin_requests})
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_redeployment(host, manual=True, disable_admin_requests=disable_admin_requests)
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("need_certificate", (True, False))
def test_enqueueing_with_certificate_stage(mp, test, need_certificate):
    test.mock_project({"id": "test-project", "name": "Test project", "certificate_deploy": need_certificate})
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY, "project": "test-project"})

    monkeypatch_request_params_validation(mp)

    result = test.api_client.post(hosts_api_url(host, action="/redeploy"), data={})
    assert result.status_code == http.client.OK

    mock_schedule_host_redeployment(host, need_certificate=need_certificate, manual=True)
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("support_ipxe", (True, False))
def test_enqueueing_with_without_ipxe_support(mp, test, support_ipxe):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    monkeypatch_request_params_validation(mp)
    if not support_ipxe:
        monkeypatch_config(mp, "deployment.projects_without_ipxe_support", [host.project])

    result = test.api_client.post(hosts_api_url(host, action="/redeploy"), data={})
    assert result.status_code == http.client.OK

    mock_schedule_host_redeployment(host, manual=True, support_ipxe=support_ipxe)
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_reject")
def test_ignore_cms(mp, test):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    monkeypatch_request_params_validation(mp)

    result = test.api_client.post(hosts_api_url(host, action="/redeploy"), data=drop_none({"ignore_cms": True}))
    assert result.status_code == http.client.OK

    mock_schedule_host_redeployment(host, manual=True, ignore_cms=True)
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_status(mp, test, host_id_field, status):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status})

    monkeypatch_request_params_validation(mp)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_reject_by_maintenance(mp, test, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test)

    monkeypatch_request_params_validation(mp)

    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_accept")
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_by_ignore_maintenance(mp, test, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test, {"provisioner": walle_constants.PROVISIONER_LUI, "config": "mock-config"})

    expected_notify_fsm_calls = []
    monkeypatch_request_params_validation(mp)

    result = test.api_client.post(
        hosts_api_url(host, host_id_field, "/redeploy"), query_string="ignore_maintenance=true", data=drop_none({})
    )
    assert result.status_code == http.client.OK

    mock_schedule_host_redeployment(host, manual=True, expected_notify_fsm_calls=expected_notify_fsm_calls)
    assert result.json == host.to_api_obj()
    test.hosts.assert_equal()


@pytest.mark.parametrize("restriction", [restrictions.REBOOT, restrictions.REDEPLOY])
def test_reject_by_restriction(test, restriction):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY, "restrictions": [restriction]})

    result = test.api_client.post(hosts_api_url(host, action="/redeploy"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.usefixtures("cms_reject")
def test_reject_by_cms(test):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    result = test.api_client.post(hosts_api_url(host, action="/redeploy"), data={})
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()


@pytest.mark.parametrize("provisioner_in_project", [True, False])
@pytest.mark.parametrize("config_in_project", [True, False])
@pytest.mark.parametrize("deploy_config_policy", [None] + DeployConfigPolicies.get_all_names())
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.parametrize("provisioner", PROVISIONERS)
def test_enqueueing_with_deploy_config_change(
    mp, test, host_id_field, provisioner, provisioner_in_project, config_in_project, deploy_config_policy
):
    project_mock_params = {"id": "test-project"}

    host_mock_params = {"state": HostState.ASSIGNED, "status": HostStatus.READY, "project": "test-project"}
    if provisioner_in_project:
        project_mock_params["provisioner"] = provisioner
    else:
        host_mock_params["provisioner"] = provisioner

    if config_in_project:
        project_mock_params["deploy_config"] = "linux-config"
    else:
        host_mock_params["config"] = "linux-config"

    test.mock_project(project_mock_params)
    host = test.mock_host(host_mock_params)

    monkeypatch_request_params_validation(mp)

    request = {"config": "freebsd-config", "ignore_cms": True}
    if deploy_config_policy:
        request["deploy_config_policy"] = deploy_config_policy
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=request)

    mock_schedule_host_redeployment(
        host, config=request.get("config"), manual=True, ignore_cms=True, deploy_config_policy=deploy_config_policy
    )
    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_enqueueing_with_wrong_deploy_config_policy(mp, test, host_id_field):
    host = test.mock_host()
    monkeypatch_request_params_validation(mp)

    nonexistent_policy = "doesnt_even_exist"
    request = {"deploy_config_policy": nonexistent_policy}
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=request)
    assert result.status_code == http.client.BAD_REQUEST
    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.parametrize("provisioner", PROVISIONERS)
def test_enqueueing_with_provisioner_and_no_config_change(mp, test, host_id_field, provisioner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    monkeypatch_request_params_validation(mp)

    request = {"provisioner": provisioner, "ignore_cms": True}
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=request)

    assert result.status_code == http.client.BAD_REQUEST

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.parametrize("provisioner", PROVISIONERS)
def test_enqueueing_with_provisioner_and_config_change(mp, test, host_id_field, provisioner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    monkeypatch_request_params_validation(mp)

    request = {"provisioner": provisioner, "config": "test-config-from-request", "ignore_cms": True}
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=request)
    mock_schedule_host_redeployment(
        host, provisioner=provisioner, config=request["config"], manual=True, ignore_cms=True
    )

    host.provisioner = provisioner
    host.config = "test-config-from-request"

    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.parametrize("provisioner", PROVISIONERS)
def test_enqueueing_with_provisioner_and_config_and_tags_change(mp, test, host_id_field, provisioner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    tags = ["tag-1", "tag-2"]

    if provisioner != walle_constants.PROVISIONER_EINE:
        mp.function(inventory.check_deploy_configuration, side_effect=inventory.InvalidDeployConfiguration(""))
    else:
        mp.function(inventory.check_deploy_configuration)

    request = {"provisioner": provisioner, "config": "test-config-from-request", "tags": tags, "ignore_cms": True}
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=request)

    if provisioner == walle_constants.PROVISIONER_EINE:
        mock_schedule_host_redeployment(
            host,
            provisioner=provisioner,
            config=request.get("config"),
            tags=request.get("tags"),
            manual=True,
            ignore_cms=True,
        )

        host.provisioner = provisioner

        assert result.status_code == http.client.OK
        assert result.json == host.to_api_obj()
    else:
        assert result.status_code == http.client.BAD_REQUEST

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.parametrize("provisioner", PROVISIONERS)
def test_enqueueing_with_provisioner_and_config_and_tags_change__2(mp, test, host_id_field, provisioner):
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "provisioner": walle_constants.PROVISIONER_EINE,
            "deploy_tags": ["tag-1", "tag-2"],
        }
    )

    monkeypatch_request_params_validation(mp)

    request = {"provisioner": provisioner, "config": "test-config-from-request", "ignore_cms": True}
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=request)

    mock_schedule_host_redeployment(
        host, provisioner=provisioner, config=request.get("config"), manual=True, ignore_cms=True
    )

    host.provisioner = provisioner
    host.config = "test-config-from-request"
    host.deploy_tags = None

    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.parametrize("provisioner", PROVISIONERS)
def test_enqueueing_with_provisioner_and_config_and_tags_change__3(mp, test, host_id_field, provisioner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    if provisioner != walle_constants.PROVISIONER_EINE:
        mp.function(inventory.check_deploy_configuration, side_effect=inventory.InvalidDeployConfiguration(""))
    else:
        mp.function(inventory.check_deploy_configuration)

    request = {"provisioner": provisioner, "config": "test-config-from-request", "tags": [], "ignore_cms": True}
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=request)

    if provisioner == walle_constants.PROVISIONER_EINE:
        mock_schedule_host_redeployment(
            host, provisioner=provisioner, config=request.get("config"), tags=None, manual=True, ignore_cms=True
        )

        host.provisioner = provisioner

        assert result.status_code == http.client.OK
        assert result.json == host.to_api_obj()
    else:
        assert result.status_code == http.client.BAD_REQUEST

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
@pytest.mark.parametrize("deploy_network", NetworkTarget.DEPLOYABLE + [None])
@pytest.mark.parametrize("provisioner", PROVISIONERS)
def test_enqueueing_with_deploy_network_change(mp, test, host_id_field, provisioner, deploy_network):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": HostStatus.READY})

    monkeypatch_request_params_validation(mp)

    request = {
        "provisioner": provisioner,
        "config": "test-config-from-request",
        "network": deploy_network,
        "ignore_cms": True,
    }
    result = test.api_client.post(hosts_api_url(host, host_id_field, "/redeploy"), data=drop_none(request))
    mock_schedule_host_redeployment(
        host, provisioner=provisioner, config=request["config"], network=deploy_network, manual=True, ignore_cms=True
    )

    host.provisioner = provisioner
    host.config = "test-config-from-request"
    host.deploy_network = deploy_network

    assert result.status_code == http.client.OK
    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()
