"""Test stage that searches for network location info."""

from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    check_stage_initialization,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    mock_commit_stage_changes,
    mock_fail_current_stage,
    monkeypatch_method,
)
from sepelib.core.constants import MINUTE_SECONDS, DAY_SECONDS, HOUR_SECONDS
from walle.clients import racktables, eine
from walle.clients.eine import EineHostDoesNotExistError
from walle.clients.network.racktables_client import RacktablesClient
from walle.constants import (
    MAC_SOURCE_AGENT,
    MAC_SOURCE_SNMP,
    VLAN_SCHEME_SEARCH,
    MAC_SOURCE_EINE,
    NETWORK_SOURCE_LLDP,
    NETWORK_SOURCE_SNMP,
    NETWORK_SOURCE_EINE,
)
from walle.fsm_stages import network_location
from walle.models import timestamp
from walle.stages import Stage, Stages


@pytest.fixture()
def test(request, mp, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.mark.parametrize(
    "stage", [Stages.UPDATE_NETWORK_LOCATION, Stages.WAIT_FOR_ACTIVE_MAC, Stages.WAIT_FOR_SWITCH_PORT]
)
def test_stage_initialization(test, stage):
    check_stage_initialization(test, Stage(name=stage), status=network_location._STATUS_CHECK)


@pytest.mark.parametrize("stage", [Stages.WAIT_FOR_ACTIVE_MAC, Stages.WAIT_FOR_SWITCH_PORT])
class TestLegacyCompatibility:
    def test_make_init_with_default_status(self, test, stage):
        host = test.mock_host({"task": mock_task(stage=stage)})

        handle_host(host)
        mock_commit_stage_changes(host, status=network_location._STATUS_CHECK)

        test.hosts.assert_equal()

    def test_call_the_new_handler(self, test, stage):
        host = test.mock_host({"task": mock_task(stage=stage, stage_status=network_location._STATUS_CHECK)})

        handle_host(host)

        mock_commit_stage_changes(host, status=network_location._STATUS_UPDATE_FROM_RACKTABLES)

        test.hosts.assert_equal()


@pytest.mark.parametrize("force", (True, False))
def test_host_has_active_mac_and_location(test, force):
    host = test.mock_host(
        {
            "active_mac": "00:00:00:00:00:00",
            "active_mac_source": MAC_SOURCE_AGENT,
            "location": {
                "switch": "mock-s01",
                "port": "mock/001",
                "network_source": NETWORK_SOURCE_LLDP,
            },
            "task": mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_CHECK,
                stage_params={"force": force},
            ),
        }
    )
    test.mock_host_network(
        {
            "active_mac": "00:00:00:00:00:00",
            "network_switch": "mock-s01",
            "network_port": "mock/001",
            "network_timestamp": timestamp(),
            "network_source": NETWORK_SOURCE_LLDP,
        },
        host=host,
    )
    handle_host(host)

    if force:
        mock_commit_stage_changes(host, status=network_location._STATUS_UPDATE_FROM_RACKTABLES)
    else:
        mock_complete_current_stage(host, expected_data={"update_network_success": True})
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_host_has_single_mac_address_and_location(test):
    host = test.mock_host(
        {
            "macs": ["00:00:00:00:00:00"],
            "location": {
                "switch": "mock-s01",
                "port": "mock/001",
                "network_source": NETWORK_SOURCE_LLDP,
            },
            "task": mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_CHECK,
            ),
        }
    )
    test.mock_host_network(
        {
            "network_switch": "mock-s01",
            "network_port": "mock/001",
            "network_source": NETWORK_SOURCE_LLDP,
            "network_timestamp": timestamp(),
        },
        host=host,
    )

    handle_host(host)
    mock_complete_current_stage(host, expected_data={"update_network_success": True})

    test.hosts.assert_equal()
    test.host_network.assert_equal()


@pytest.mark.parametrize("missing", ({"active_mac"}, {"location"}, {"location", "active_mac"}))
def test_host_does_not_have_mac_address_or_location_location(test, missing):
    host_data = dict(
        _mock_host_location_params(),
        task=mock_task(
            stage=Stages.UPDATE_NETWORK_LOCATION,
            stage_status=network_location._STATUS_CHECK,
        ),
    )

    if "location" in missing:
        host_data["location"].pop("switch")
    if "active_mac" in missing:
        host_data.pop("active_mac")

    host = test.mock_host(host_data)

    test.mock_host_network({"active_mac_time": timestamp(), "network_timestamp": timestamp()}, host=host)

    handle_host(host)

    mock_commit_stage_changes(host, status=network_location._STATUS_UPDATE_FROM_RACKTABLES)
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_racktables_l123_has_network_info(mp, test):
    time = timestamp()
    mock_l123_info_0 = {"MAC": "00:00:00:00:00:AA"}
    mock_l123_info_1 = {
        "MAC": "11:11:11:11:11:BB",
        "Switch": "switch-mock",
        "Port": "port-mock",
        "Port_timestamp": time,
        "ips": [
            {
                "IP": "2a02:6b8:b000:b70d:92e2:baff:fe74:7dca",
                "Network": "2a02:6b8:b000:b70d::/64",
                "Location": "\u041c\u044f\u043d\u0442\u0441\u044f\u043b\u044f-1",
                "VLAN": "1387",
            },
            {
                "IP": "2a02:6b8:f000:280d:92e2:baff:fe74:7dca",
                "Network": "2a02:6b8:f000:280d::/64",
                "Location": "\u041c\u044f\u043d\u0442\u0441\u044f\u043b\u044f-1",
                "VLAN": "767",
            },
        ],
    }
    mp.function(racktables.json_request, side_effect=[mock_l123_info_0, mock_l123_info_1])

    monkeypatch_method(mp, method=RacktablesClient.is_interconnect_switch, obj=RacktablesClient, return_value=False)

    project = test.mock_project({"vlan_scheme": VLAN_SCHEME_SEARCH, "id": "some-id"})
    host = test.mock_host(
        dict(
            _mock_host_location_params(),
            project=project.id,
            task=mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_UPDATE_FROM_RACKTABLES,
            ),
        )
    )
    host_network = test.mock_host_network({"active_mac_time": time - 1, "network_timestamp": time - 1}, host=host)
    handle_host(host)

    host.active_mac = "11:11:11:11:11:bb"
    host_network.active_mac = "11:11:11:11:11:bb"
    host_network.active_mac_time = time
    host.active_mac_source = MAC_SOURCE_SNMP
    host_network.active_mac_source = MAC_SOURCE_SNMP
    host.location.switch = "switch-mock"
    host_network.network_switch = "switch-mock"
    host.location.port = "port-mock"
    host_network.network_port = "port-mock"
    host_network.network_timestamp = time
    host.location.network_source = NETWORK_SOURCE_SNMP
    host_network.network_source = NETWORK_SOURCE_SNMP

    mock_complete_current_stage(host, expected_data={"update_network_success": True})
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_racktables_l123_does_not_have_network_info(mp, test):
    time = timestamp()
    mock_l123_info_0 = {"MAC": "00:00:00:00:00:AA"}
    mock_l123_info_1 = {
        # stale data
        "MAC": "11:11:11:11:11:BB",
        "Switch": "switch-mock",
        "Port": "port-mock",
    }
    mp.function(racktables.json_request, side_effect=[mock_l123_info_0, mock_l123_info_1])
    monkeypatch_method(mp, method=RacktablesClient.is_interconnect_switch, obj=RacktablesClient, return_value=False)

    project = test.mock_project({"vlan_scheme": VLAN_SCHEME_SEARCH, "id": "some-id"})
    host = test.mock_host(
        dict(
            _mock_host_location_params(),
            project=project.id,
            task=mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_UPDATE_FROM_RACKTABLES,
            ),
        )
    )
    test.mock_host_network({"active_mac_time": time - 1, "network_timestamp": time - 1}, host=host)
    handle_host(host)

    mock_commit_stage_changes(host, status=network_location._STATUS_UPDATE_FROM_EINE)
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_eine_has_recent_network_info(mp, test):
    time = timestamp()
    mac_from_eine = "11:11:11:11:11:bb"
    mac_result_mock = Mock(
        active_mac=Mock(return_value=Mock(timestamp=time, active=mac_from_eine)),
        switch=Mock(return_value=Mock(switch="switch-mock", port="port-mock", timestamp=time)),
    )
    mp.method(eine.EineClient.get_host_status, return_value=mac_result_mock, obj=eine.EineClient)
    mp.function(racktables.json_request, return_value={"MAC": "00:00:00:00:00:00"})

    project = test.mock_project({"vlan_scheme": VLAN_SCHEME_SEARCH, "id": "some-id"})
    host = test.mock_host(
        dict(
            _mock_host_location_params(),
            project=project.id,
            task=mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_UPDATE_FROM_EINE,
            ),
        )
    )
    host_network = test.mock_host_network({"active_mac_time": time - 1, "network_timestamp": time - 1}, host=host)

    handle_host(host)

    host.active_mac = "11:11:11:11:11:bb"
    host_network.active_mac = "11:11:11:11:11:bb"
    host_network.active_mac_time = time
    host.active_mac_source = MAC_SOURCE_EINE
    host_network.active_mac_source = MAC_SOURCE_EINE
    host.location.switch = "switch-mock"
    host_network.network_switch = "switch-mock"
    host.location.port = "port-mock"
    host_network.network_port = "port-mock"
    host_network.network_timestamp = time
    host.location.network_source = NETWORK_SOURCE_EINE
    host_network.network_source = NETWORK_SOURCE_EINE

    mock_complete_current_stage(host, expected_data={"update_network_success": True})
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_eine_has_old_network_info(test, mp):
    time = timestamp() - (DAY_SECONDS + HOUR_SECONDS)
    mac_from_eine = "11:11:11:11:11:bb"
    mac_result_mock = Mock(
        active_mac=Mock(return_value=Mock(timestamp=time, active=mac_from_eine)),
        switch=Mock(return_value=Mock(switch="switch-mock", port="port-mock", timestamp=time)),
    )

    mp.method(eine.EineClient.get_host_status, return_value=mac_result_mock, obj=eine.EineClient)
    mp.function(racktables.json_request, return_value={"MAC": "00:00:00:00:00:00"})

    project = test.mock_project({"vlan_scheme": VLAN_SCHEME_SEARCH, "id": "some-id"})
    host = test.mock_host(
        dict(
            _mock_host_location_params(),
            project=project.id,
            task=mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_UPDATE_FROM_EINE,
            ),
        )
    )

    test.mock_host_network({"active_mac_time": time - 1, "network_timestamp": time - 1}, host=host)

    handle_host(host)

    mock_commit_stage_changes(host, status=network_location._STATUS_WAIT_FOR_NETMAP)
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_do_not_wait_for_racktables(test, mp):
    mp.function(racktables.json_request, return_value={"MAC": "00:00:00:00:00:00"})

    project = test.mock_project({"vlan_scheme": VLAN_SCHEME_SEARCH, "id": "some-id"})
    host = test.mock_host(
        dict(
            project=project.id,
            task=mock_task(stage=Stages.UPDATE_NETWORK_LOCATION, stage_status=network_location._STATUS_WAIT_FOR_NETMAP),
        )
    )
    test.mock_host_network({}, host=host)
    mp.method(
        eine.EineClient.get_host_status,
        side_effect=EineHostDoesNotExistError(host.inv, "Some error"),
        obj=eine.EineClient,
    )

    handle_host(host)

    mock_fail_current_stage(host, reason="Failed to determine network location and active mac address.")
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_wait_more(mp, test):
    mp.function(racktables.json_request, return_value={"MAC": "00:00:00:00:00:00"})

    project = test.mock_project({"vlan_scheme": VLAN_SCHEME_SEARCH, "id": "some-id"})
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_WAIT_FOR_NETMAP,
                stage_params={"wait_for_racktables": True},
            ),
            "project": project.id,
        }
    )
    test.mock_host_network({}, host=host)
    mp.method(
        eine.EineClient.get_host_status,
        side_effect=EineHostDoesNotExistError(host.inv, "Some error"),
        obj=eine.EineClient,
    )

    handle_host(host)

    mock_commit_stage_changes(host, check_after=MINUTE_SECONDS)
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_wait_timeout(mp, test):
    mp.function(racktables.json_request, return_value={"MAC": "00:00:00:00:00:00"})

    wait_timeout = 80 * MINUTE_SECONDS
    project = test.mock_project({"vlan_scheme": VLAN_SCHEME_SEARCH, "id": "some-id"})
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_WAIT_FOR_NETMAP,
                stage_status_time=timestamp() - wait_timeout,
                stage_params={"wait_for_racktables": True},
            ),
            "project": project.id,
        }
    )
    test.mock_host_network({"active_mac_time": timestamp(), "network_timestamp": timestamp()}, host=host)
    mp.method(
        eine.EineClient.get_host_status,
        side_effect=EineHostDoesNotExistError(host.inv, "Some error"),
        obj=eine.EineClient,
    )

    handle_host(host)

    reason = "Can't determine host's network location, giving up after {} seconds".format(wait_timeout)
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_wait_success(mp, test):
    mp.function(racktables.json_request, return_value={"MAC": "00:00:00:00:00:00"})

    wait_timeout = 80 * MINUTE_SECONDS
    project = test.mock_project({"vlan_scheme": VLAN_SCHEME_SEARCH, "id": "some-id"})
    host = test.mock_host(
        dict(
            _mock_host_location_params(),
            task=mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_WAIT_FOR_NETMAP,
                stage_status_time=timestamp() - wait_timeout,
                stage_params={"wait_for_racktables": True},
            ),
            project=project.id,
        )
    )
    test.mock_host_network({"active_mac_time": timestamp(), "network_timestamp": timestamp()}, host=host)
    mp.method(
        eine.EineClient.get_host_status,
        side_effect=EineHostDoesNotExistError(host.inv, "Some error"),
        obj=eine.EineClient,
    )

    handle_host(host)

    mock_complete_current_stage(host, expected_data={"update_network_success": True})
    test.hosts.assert_equal()
    test.host_network.assert_equal()


@pytest.mark.parametrize("vlan_scheme", [None])
def test_dont_wait_if_specific_vlan_scheme(test, mp, vlan_scheme):
    time = timestamp()
    mac_from_eine = "11:11:11:11:11:bb"
    mac_result_mock = Mock(active_mac=Mock(return_value=Mock(timestamp=time, active=mac_from_eine)))
    mp.method(eine.EineClient.get_host_status, return_value=mac_result_mock, obj=eine.EineClient)
    mp.function(racktables.json_request, return_value={"MAC": "00:00:00:00:00:00"})

    project = test.mock_project({"vlan_scheme": vlan_scheme, "id": "some-id"})
    host = test.mock_host(
        {
            "task": mock_task(
                stage=Stages.UPDATE_NETWORK_LOCATION,
                stage_status=network_location._STATUS_WAIT_FOR_NETMAP,
            ),
            "project": project.id,
        }
    )
    test.mock_host_network({}, host=host)
    handle_host(host)
    reason = "Failed to determine network location and active mac address."
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_switch_port_verification_pass(test):
    host = test.mock_host(
        {
            "active_mac": "00:00:00:00:00:00",
            "task": mock_task(
                stage=Stages.VERIFY_SWITCH_PORT,
                stage_status=network_location._STATUS_CHECK_SWITCH_PORT_IN_WALLE,
            ),
        }
    )
    test.mock_host_network(
        {
            "network_switch": "mock-s01",
            "network_port": "mock/001",
            "network_timestamp": timestamp(),
        },
        host=host,
    )
    test.mock_host_network(
        {
            "uuid": "other_host",
            "network_switch": "mock-s01",
            "network_port": "mock/001",
            "network_timestamp": 0,
        }
    )

    handle_host(host)
    mock_commit_stage_changes(host, status=network_location._STATUS_CHECK_POWER_ON_NEED)

    test.hosts.assert_equal()


def test_switch_port_verification_fail(test):
    host = test.mock_host(
        {
            "active_mac": "00:00:00:00:00:00",
            "task": mock_task(
                stage=Stages.VERIFY_SWITCH_PORT,
                stage_status=network_location._STATUS_CHECK_SWITCH_PORT_IN_WALLE,
            ),
        }
    )
    test.mock_host_network(
        {
            "network_switch": "mock-s01",
            "network_port": "mock/001",
            "network_timestamp": timestamp(),
        },
        host=host,
    )
    test.mock_host_network(
        {
            "uuid": "other_host",
            "network_switch": "mock-s01",
            "network_port": "mock/001",
            "network_timestamp": timestamp() + 1,
        }
    )

    handle_host(host)
    mock_fail_current_stage(
        host, reason="found more recent host on mock-s01/mock/001; subsequent operations are dangerous"
    )

    test.hosts.assert_equal()


def _mock_host_location_params(**kwargs):
    return dict(
        {
            "macs": ["00:00:00:00:00:aa", "11:11:11:11:11:bb"],
            "active_mac": "00:00:00:00:00:00",
            "active_mac_source": MAC_SOURCE_AGENT,
            "location": {
                "switch": "mock-s01",
                "port": "mock/001",
                "network_source": NETWORK_SOURCE_LLDP,
            },
        },
        **kwargs
    )
