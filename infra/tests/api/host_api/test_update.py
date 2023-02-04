"""Tests host update API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    hosts_api_url,
    generate_host_action_authentication_tests,
    monkeypatch_request_params_validation,
)
from walle import restrictions, constants as walle_constants
from walle.clients import deploy
from walle.constants import NetworkTarget, HostType
from walle.hosts import HostState, HostStatus, HostLocation
from walle.operations_log.constants import Operation
from walle.util.deploy_config import DeployConfigPolicies


@pytest.fixture
def test(monkeypatch_timestamp, request):
    return TestCase.create(request)


generate_host_action_authentication_tests(globals(), "", {"restrictions": []}, methods=("POST", "PATCH"))


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.all_status_owner_combinations()
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_restrictions(test, method, host_id_field, status, owner):
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": HostState.ASSIGNED,
            "status": status,
            "status_author": owner,
        }
    )

    result = test.api_client.open(
        hosts_api_url(host, host_id_field),
        method=method,
        data={"restrictions": [restrictions.REBOOT, restrictions.REBOOT], "reason": "mock"},
    )
    assert result.status_code == http.client.OK

    host.restrictions = [restrictions.REBOOT]
    test.hosts.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.all_status_owner_combinations()
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_unset_restrictions(test, method, host_id_field, status, owner):
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": HostState.ASSIGNED,
            "status": status,
            "status_author": owner,
            "restrictions": restrictions.ALL,
        }
    )

    result = test.api_client.open(
        hosts_api_url(host, host_id_field), method=method, data={"restrictions": [], "reason": "mock"}
    )
    assert result.status_code == http.client.OK

    del host.restrictions
    test.hosts.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_restrictions_reject_by_maintenance(test, method, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test, {"restrictions": restrictions.ALL})

    result = test.api_client.open(
        hosts_api_url(host, host_id_field), method=method, data={"restrictions": [], "reason": "mock"}
    )
    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_restrictions_reject_by_lack_of_reason(test, method, host_id_field):
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
        }
    )
    result = test.api_client.open(
        hosts_api_url(host, host_id_field), method=method, data={"restrictions": [restrictions.REBOOT]}
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert "Please specify a ticket or human-readable reason" in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_restrictions_allow_by_ignore_maintenance(test, method, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test, {"restrictions": restrictions.ALL})

    result = test.api_client.open(
        hosts_api_url(host, host_id_field),
        method=method,
        query_string="ignore_maintenance=true",
        data={"restrictions": [], "reason": "mock"},
    )
    assert result.status_code == http.client.OK

    del host.restrictions
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", set(HostState.ALL_ASSIGNED) - {HostState.MAINTENANCE})
@pytest.mark.parametrize(
    "status",
    set(HostStatus.ALL_STEADY + HostStatus.ALL_TASK) - {Operation.REDEPLOY.host_status, Operation.PREPARE.host_status},
)
@pytest.mark.parametrize("provisioner", walle_constants.PROVISIONERS)
@pytest.mark.parametrize("update_tags", [False, True])
def test_set_config_allow_for_correct_status(mp, test, state, status, provisioner, update_tags):
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": state,
            "status": status,
            "provisioner": provisioner,
            "config": "default-test-config",
            "deploy_network": NetworkTarget.PROJECT,
            "deploy_tags": None,
        }
    )

    request_data = {
        "config": "test-config-from-request",
        "deploy_config_policy": DeployConfigPolicies.DISKMANAGER,
        "deploy_network": NetworkTarget.SERVICE,
        "reason": "mock",
    }

    if update_tags:
        request_data["deploy_tags"] = ["tag-1", "tag-2"]

    lui_mock = mp.function(deploy.get_lui_client)
    monkeypatch_request_params_validation(mp)

    result = test.api_client.open("/v1/hosts/0/deploy_config", method="PUT", data=request_data)
    assert result.status_code == http.client.OK

    host.provisioner = provisioner
    host.config = "test-config-from-request"
    host.deploy_config_policy = DeployConfigPolicies.DISKMANAGER
    host.deploy_network = NetworkTarget.SERVICE

    if update_tags:
        host.deploy_tags = ["tag-1", "tag-2"]

    if provisioner == walle_constants.PROVISIONER_LUI:
        lui_mock(provider=None).set_host_config.assert_called_once_with("test", "test-config-from-request")
    else:
        assert not lui_mock(provider=None).set_host_config.called
    test.hosts.assert_equal()


@pytest.mark.parametrize("provisioner", walle_constants.PROVISIONERS)
def test_set_config_set_provisioner(mp, test, provisioner):
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "deploy_network": NetworkTarget.PROJECT,
            "provisioner": None,
            "config": None,
            "deploy_tags": None,
        }
    )

    request_data = {
        "config": "test-config-from-request",
        "provisioner": provisioner,
        "deploy_network": NetworkTarget.SERVICE,
        "reason": "mock",
    }

    lui_mock = mp.function(deploy.get_lui_client)
    monkeypatch_request_params_validation(mp)

    result = test.api_client.open("/v1/hosts/0/deploy_config", method="PUT", data=request_data)
    assert result.status_code == http.client.OK

    host.config = "test-config-from-request"
    host.provisioner = provisioner
    host.deploy_network = NetworkTarget.SERVICE

    if provisioner == walle_constants.PROVISIONER_LUI:
        lui_mock(provider=None).set_host_config.assert_called_once_with("test", "test-config-from-request")
    else:
        assert not lui_mock(provider=None).set_host_config.called
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    ["state", "status"],
    [
        (HostState.FREE, Operation.PREPARE.host_status),
        (HostState.FREE, HostStatus.READY),
        (HostState.FREE, HostStatus.DEAD),
    ],
)
def test_set_config_is_forbidden_for_listed_states_and_statuses(mp, test, state, status):
    test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": state,
            "status": status,
        }
    )

    lui_mock = mp.function(deploy.get_lui_client)
    monkeypatch_request_params_validation(mp)

    result = test.api_client.open(
        "/v1/hosts/0/deploy_config", method="PUT", data={"config": "config-from-request", "reason": "mock"}
    )

    assert result.status_code == http.client.CONFLICT
    assert not lui_mock(provider=None).set_host_config.called
    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_provisioner_reject_by_maintenance(test, mp, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test)
    mp.function(deploy.get_lui_client)
    monkeypatch_request_params_validation(mp)

    result = test.api_client.open(
        hosts_api_url(host, host_id_field, "/deploy_config"),
        method="PUT",
        data={"config": "test-config-from-request", "reason": "mock"},
    )

    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_set_provisioner_allow_by_ignore_maintenance(test, mp, mock_maintenance_host, host_id_field):
    host = mock_maintenance_host(test)
    mp.function(deploy.get_lui_client)
    monkeypatch_request_params_validation(mp)

    config_from_request = "test-config-from-request"
    result = test.api_client.open(
        hosts_api_url(host, host_id_field, "/deploy_config"),
        method="PUT",
        query_string="ignore_maintenance=true",
        data={"config": config_from_request, "provisioner": walle_constants.PROVISIONER_LUI, "reason": "mock"},
    )

    assert result.status_code == http.client.OK

    host.config = config_from_request
    host.provisioner = walle_constants.PROVISIONER_LUI
    test.hosts.assert_equal()


@pytest.mark.parametrize("owner", [TestCase.api_issuer, "other-user@"])
@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize(
    "status", [Operation.POWER_ON.host_status, Operation.PREPARE.host_status, HostStatus.READY, HostStatus.MANUAL]
)
def test_set_config_delete(mp, test, state, status, owner):
    if status == HostStatus.default(HostState.MAINTENANCE) and state != HostState.MAINTENANCE:
        return  # Skip tests with deprecated state/status combinations

    good_states = HostState.ALL_ASSIGNED

    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "state": state,
            "status": status,
            "state_author": owner,
            "status_author": owner,
            "provisioner": walle_constants.PROVISIONER_EINE,
            "config": "test-config",
            "deploy_config_policy": DeployConfigPolicies.DISKMANAGER,
            "deploy_tags": ["tag-1"],
            "deploy_network": NetworkTarget.SERVICE,
        }
    )

    monkeypatch_request_params_validation(mp)

    result = test.api_client.open("/v1/hosts/0/deploy_config", method="DELETE")

    if (state == HostState.MAINTENANCE or status == HostStatus.MANUAL) and owner != TestCase.api_issuer:
        assert result.status_code == http.client.CONFLICT
        assert (
            "The host is under maintenance by other-user@. "
            "Add 'ignore maintenance' flag to your request "
            "if this action won't break anything." in result.json["message"]
        )
        test.hosts.assert_equal()

    elif state in good_states:
        assert result.status_code == http.client.OK

        del host.config
        del host.deploy_config_policy
        del host.provisioner
        del host.deploy_tags
        del host.deploy_network

        test.hosts.assert_equal()
    else:
        assert result.status_code == http.client.CONFLICT
        test.hosts.assert_equal()


def test_set_location_for_vm_with_success(test):
    host = test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "type": HostType.VM,
            "location": {"short_datacenter_name": "wrong-dc", "short_queue_name": "wrong-queue"},
        }
    )

    request_data = {
        "switch": "switch",
        "port": "port",
        "network_source": walle_constants.NETWORK_SOURCE_LLDP,
        "country": "RU",
        "city": "VLA",
        "datacenter": "vladimir",
        "queue": "queue",
        "rack": "rack",
        "unit": "unit",
        "physical_timestamp": "1",
        "short_datacenter_name": "short-vladimir",
        "short_queue_name": "short-queue-in-vladimir",
    }

    result = test.api_client.open("/v1/hosts/0/location", method="PUT", data=request_data)
    assert result.status_code == http.client.OK

    location = HostLocation(**request_data)
    host.location = location

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_type", [HostType.SERVER, HostType.MAC, HostType.SHADOW_SERVER])
def test_set_location_for_not_vm_with_fail(test, host_type):
    test.mock_host(
        {
            "inv": 0,
            "name": "test",
            "type": host_type,
            "location": {"short_datacenter_name": "wrong-dc", "short_queue_name": "wrong-queue"},
        }
    )

    request_data = {
        "switch": "switch",
        "port": "port",
        "network_source": walle_constants.NETWORK_SOURCE_LLDP,
        "country": "RU",
        "city": "VLA",
        "datacenter": "vladimir",
        "queue": "queue",
        "rack": "rack",
        "unit": "unit",
        "physical_timestamp": "1",
        "short_datacenter_name": "short-vladimir",
        "short_queue_name": "short-queue-in-vladimir",
    }

    result = test.api_client.open("/v1/hosts/0/location", method="PUT", data=request_data)
    assert result.status_code == http.client.CONFLICT

    test.hosts.assert_equal()
