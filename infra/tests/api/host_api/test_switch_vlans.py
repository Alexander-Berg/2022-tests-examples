"""Tests VLAN switching."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    generate_host_action_authentication_tests,
    hosts_api_url,
    patch,
    mock_schedule_switch_vlans,
)
from walle import constants as walle_constants, network
from walle.constants import NetworkTarget
from walle.hosts import HostState, HostStatus
from walle.util.misc import drop_none
from walle.util.tasks import _network_target_project

VLANS = [2]
VLAN_CONFIG = network._VlanConfig(VLANS, VLANS[0], 1388)


@pytest.fixture
def test(monkeypatch_timestamp, request, monkeypatch_audit_log):
    return TestCase.create(request)


generate_host_action_authentication_tests(globals(), "/switch-vlans", {"vlans": []})


@pytest.fixture
def mock_vlans_params(mp):
    mp.function(network.get_host_expected_vlans, return_value=VLAN_CONFIG)
    mp.function(_network_target_project, return_value=NetworkTarget.PROJECT)


@pytest.mark.parametrize("vlan", (walle_constants.VLAN_ID_MIN - 1, walle_constants.VLAN_ID_MAX + 1))
def test_invalid_vlan(test, vlan):
    host = test.mock_host({"inv": 0})
    result = test.api_client.post(hosts_api_url(host, action="/switch-vlans"), data={"vlans": [vlan]})
    assert result.status_code == http.client.BAD_REQUEST
    test.hosts.assert_equal()


@pytest.mark.all_status_owner_combinations()
def test_switching(test, mock_vlans_params, status, owner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status, "status_author": owner})

    result = test.api_client.post(hosts_api_url(host, action="/switch-vlans"), data=drop_none({"vlans": []}))
    assert result.status_code == http.client.NO_CONTENT

    mock_schedule_switch_vlans(host, network=NetworkTarget.PROJECT)
    test.hosts.assert_equal()


@pytest.mark.all_status_owner_combinations()
def test_switching_to_configured_vlans(test, mock_vlans_params, status, owner):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status, "status_author": owner})

    result = test.api_client.post(hosts_api_url(host, action="/switch-vlans"), data={})
    assert result.status_code == http.client.NO_CONTENT

    mock_schedule_switch_vlans(host, network=NetworkTarget.PROJECT)
    test.hosts.assert_equal()


def test_switch_vlans_with_network_target_and_vlans(test, mock_vlans_params):
    project = test.mock_project({"id": "mock-project", "owned_vlans": [1, 2]})
    host = test.mock_host({"project": project.id, "state": HostState.ASSIGNED})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-vlans"),
        data=drop_none({"vlans": [6, 7], "native_vlan": None, "network_target": NetworkTarget.PARKING}),
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert "Use only one parameter: network_target or vlans" in result.json["message"]

    test.hosts.assert_equal()


@pytest.mark.parametrize("network_target", (NetworkTarget.PARKING, NetworkTarget.SERVICE, NetworkTarget.PROJECT))
def test_switch_vlans_with_network_target(test, mock_vlans_params, network_target):
    _switching_vlans(
        test, vlans=None, native_vlan=None, network_target=network_target, result_network_target=network_target
    )


@pytest.mark.parametrize(
    "vlans, result_network_target", (([999], NetworkTarget.PARKING), ([542], NetworkTarget.SERVICE))
)
def test_switching_vlans_with_999_or_542_vlan(test, mock_vlans_params, vlans, result_network_target):
    _switching_vlans(
        test, vlans=vlans, native_vlan=None, network_target=None, result_network_target=result_network_target
    )


@pytest.mark.parametrize(
    "vlans, native_vlan, network_target, return_native_vlan",
    (
        (None, None, None, None),
        (None, None, NetworkTarget.PROJECT, None),
        ([], None, None, None),
        ([], 2, None, 2),
        ([2], None, None, None),
    ),
)
def test_switching_vlans_with_network_target_project(
    test, mock_vlans_params, vlans, native_vlan, network_target, return_native_vlan
):
    _switching_vlans(
        test,
        vlans=vlans,
        native_vlan=native_vlan,
        network_target=network_target,
        result_native_vlan=return_native_vlan,
        result_network_target=NetworkTarget.PROJECT,
    )


def test_switching_custom_vlans(test, mock_vlans_params):
    _switching_vlans(
        test, vlans=[1, 2], native_vlan=None, network_target=None, result_vlans=[1, 2], result_network_target=None
    )


def _switching_vlans(
    test, vlans, native_vlan, network_target, result_vlans=None, result_native_vlan=None, result_network_target=None
):
    project = test.mock_project({"id": "mock-project", "owned_vlans": [1, 2]})
    host = test.mock_host({"project": project.id, "state": HostState.ASSIGNED})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-vlans"),
        data=drop_none({"vlans": vlans, "native_vlan": native_vlan, "network_target": network_target}),
    )
    assert result.status_code == http.client.NO_CONTENT

    mock_schedule_switch_vlans(host, network=result_network_target, vlans=result_vlans, native_vlan=result_native_vlan)
    test.hosts.assert_equal()


@pytest.mark.parametrize("status", set(HostStatus.ALL) - set(HostStatus.ALL_STEADY))
def test_reject_by_status(test, mock_vlans_params, status):
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status})

    result = test.api_client.post(hosts_api_url(host, action="/switch-vlans"), data={"vlans": []})
    assert result.status_code == http.client.CONFLICT
    assert "The host has an invalid state for this operation" in result.json["message"]

    test.hosts.assert_equal()


def test_reject_by_maintenance(test, mock_vlans_params, mock_maintenance_host):
    host = mock_maintenance_host(test)

    result = test.api_client.post(hosts_api_url(host, action="/switch-vlans"), data={"vlans": []})
    assert result.status_code == http.client.CONFLICT
    assert (
        "The host is under maintenance by other-user@. "
        "Add 'ignore maintenance' flag to your request "
        "if this action won't break anything." in result.json["message"]
    )

    test.hosts.assert_equal()


@patch("walle.network.get_current_host_switch_port", return_value=("switch-mock", "port-mock", "source-mock", 0))
@patch("walle.clients.racktables.switch_vlans")
def test_allow_by_maintenance(switch_vlans, get_witch_port, test, mock_maintenance_host, mock_vlans_params):
    host = mock_maintenance_host(test)

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-vlans"), query_string="ignore_maintenance=true", data={"vlans": []}
    )
    assert result.status_code == http.client.NO_CONTENT


@patch("walle.clients.racktables.switch_vlans")
def test_reject_by_project_vlans(switch_vlans, test, mock_vlans_params):
    host = test.mock_host({"state": HostState.ASSIGNED})

    result = test.api_client.post(hosts_api_url(host, action="/switch-vlans"), data=drop_none({"vlans": [1]}))
    assert result.status_code == http.client.FORBIDDEN
    assert "doesn't own the following VLAN" in result.json["message"]

    assert switch_vlans.call_count == 0
    test.hosts.assert_equal()


def test_switching_vlans_with_update_network_location(test, mock_vlans_params):
    project = test.mock_project({"id": "mock-project", "owned_vlans": [1, 2]})
    host = test.mock_host({"project": project.id, "state": HostState.ASSIGNED})

    result = test.api_client.post(
        hosts_api_url(host, action="/switch-vlans"),
        data=drop_none({"vlans": None, "native_vlan": None, "network_target": None, "update_network_location": True}),
    )
    assert result.status_code == http.client.NO_CONTENT

    mock_schedule_switch_vlans(
        host, network=NetworkTarget.PROJECT, vlans=None, native_vlan=None, update_network_location=True
    )
    test.hosts.assert_equal()
