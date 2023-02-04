"""Tests netmap synchronization."""

import pytest

import walle.clients.network.netmap
from infra.walle.server.tests.lib.util import TestCase
from sepelib.core import constants
from walle.clients.racktables import RacktablesSwitchInfo
from walle.constants import NETWORK_SOURCE_RACKTABLES, MAC_SOURCE_RACKTABLES
from walle.db_sync import racktables_netmap
from walle.hosts import HostLocation, HostStatus, HostState
from walle.models import timestamp


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("status", HostStatus.ALL)
def test_synchronization(test, monkeypatch, state, status):
    def mock_host_network(overrides, network_overrides):
        host = test.mock_host(dict(state=state, status=status, **overrides))
        host_network = test.mock_host_network(network_overrides, host=host)
        return host, host_network

    mac_to_switch = {
        0: RacktablesSwitchInfo(switch="zero-switch", port="zero-port", int_mac=0, timestamp=1),
        1: None,
        2: RacktablesSwitchInfo(switch="two-switch", port="two-port", int_mac=2, timestamp=1),
        3: RacktablesSwitchInfo(switch="three-switch", port="three-port", int_mac=3, timestamp=1),
        4: None,
        5: RacktablesSwitchInfo(switch="five-switch", port="five-port", int_mac=5, timestamp=1),
        # this one will be picked, because it's newer
        6: RacktablesSwitchInfo(switch="six-switch", port="six-port", int_mac=6, timestamp=timestamp()),
        7: RacktablesSwitchInfo(switch="seven-switch", port="seven-port", int_mac=7, timestamp=1),
    }

    switch_to_mac = {(info.switch, info.port): (mac,) for mac, info in mac_to_switch.items() if info is not None}

    monkeypatch.setattr(walle.clients.network.netmap.Netmap, "_get_mac_to_inv_mapping", lambda self: {})
    monkeypatch.setattr(
        walle.clients.network.netmap.Netmap, "_get_mac_switch_mappings", lambda self: (mac_to_switch, switch_to_mac)
    )

    # set info for the first time
    no_info_host, no_info_network = mock_host_network(
        dict(inv=0, name="zero", location=HostLocation(), macs=["00:00:00:00:00:00"]), {}
    )

    # info was never set + not found
    mock_host_network(dict(inv=1, name="one", location=HostLocation(), macs=["00:00:00:00:00:01"]), {})

    # up-to-date info
    mock_host_network(
        dict(
            inv=2,
            name="two",
            macs=["00:00:00:00:00:02"],
            location=HostLocation(
                switch="two-other-switch", port="two-other-port", network_source=NETWORK_SOURCE_RACKTABLES
            ),
        ),
        dict(
            network_switch="two-other-switch",
            network_port="two-other-port",
            network_timestamp=1,
            network_source=NETWORK_SOURCE_RACKTABLES,
            active_mac_time=1,
        ),
    )

    # outdated info -> update
    outdated_host, outdated_network = mock_host_network(
        dict(
            inv=3,
            name="three",
            macs=["00:00:00:00:00:03"],
            location=HostLocation(switch="invalid-switch", port="three-port", network_source=NETWORK_SOURCE_RACKTABLES),
        ),
        dict(
            network_switch="invalid-switch",
            network_port="three-port",
            network_timestamp=0,
            network_source=NETWORK_SOURCE_RACKTABLES,
            active_mac_time=0,
        ),
    )

    # three MACs in netmap
    five_six_seven, five_six_seven_network = mock_host_network(
        dict(
            inv=5,
            name="five",
            location=HostLocation(),
            macs=["00:00:00:00:00:05", "00:00:00:00:00:06", "00:00:00:00:00:07"],
        ),
        {},
    )

    racktables_netmap._sync()

    if status != HostStatus.INVALID:
        no_info_host.active_mac = "00:00:00:00:00:00"
        no_info_host.active_mac_source = MAC_SOURCE_RACKTABLES
        no_info_host.location = HostLocation(
            switch="zero-switch", port="zero-port", network_source=NETWORK_SOURCE_RACKTABLES
        )

        no_info_network.active_mac = "00:00:00:00:00:00"
        no_info_network.active_mac_time = 1
        no_info_network.active_mac_source = MAC_SOURCE_RACKTABLES
        no_info_network.network_switch = "zero-switch"
        no_info_network.network_port = "zero-port"
        no_info_network.network_timestamp = 1
        no_info_network.network_source = NETWORK_SOURCE_RACKTABLES

        outdated_host.active_mac = "00:00:00:00:00:03"
        outdated_host.active_mac_source = MAC_SOURCE_RACKTABLES
        outdated_host.location.switch = "three-switch"

        outdated_network.active_mac = "00:00:00:00:00:03"
        outdated_network.active_mac_time = 1
        outdated_network.active_mac_source = MAC_SOURCE_RACKTABLES
        outdated_network.network_switch = "three-switch"
        outdated_network.network_timestamp = 1

        five_six_seven.active_mac = "00:00:00:00:00:06"
        five_six_seven.active_mac_source = MAC_SOURCE_RACKTABLES
        five_six_seven.location = HostLocation(
            switch="six-switch", port="six-port", network_source=NETWORK_SOURCE_RACKTABLES
        )

        five_six_seven_network.active_mac = "00:00:00:00:00:06"
        five_six_seven_network.active_mac_time = timestamp() - constants.HOUR_SECONDS
        five_six_seven_network.active_mac_source = MAC_SOURCE_RACKTABLES
        five_six_seven_network.network_switch = "six-switch"
        five_six_seven_network.network_port = "six-port"
        five_six_seven_network.network_timestamp = timestamp() - constants.HOUR_SECONDS
        five_six_seven_network.network_source = NETWORK_SOURCE_RACKTABLES

    test.hosts.assert_equal()
    test.host_network.assert_equal()
