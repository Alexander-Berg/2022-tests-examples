"""Tests EINE netmap synchronization."""

import pytest

from walle.constants import NETWORK_SOURCE_EINE, MAC_SOURCE_EINE, NETWORK_SOURCE_RACKTABLES, MAC_SOURCE_RACKTABLES
from walle.db_sync import eine_netmap
from walle.hosts import HostLocation, HostStatus
from walle.clients.eine import EineMacsInfo, EineSwitchInfo
from walle.models import timestamp
import walle.clients.eine

from infra.walle.server.tests.lib.util import TestCase


@pytest.fixture
def test(request, monkeypatch_timestamp, mp):
    mp.method(
        walle.clients.eine.EineClient.get_network_map,
        return_value=inv_to_eine_status,
        obj=walle.clients.eine.EineClient,
    )
    return TestCase.create(request)


def get_eine_host_status(switch, port, active_mac, ts):
    return EineSwitchInfo(switch, port, ts), EineMacsInfo(active_mac, ts)


mocked_eine_ts1 = timestamp()

inv_to_eine_status = {
    1: get_eine_host_status("one-switch", "one-port", "00:00:00:00:00:02", mocked_eine_ts1),
    2: get_eine_host_status("two-switch", "two-port", "00:00:00:00:00:03", 1),
}


def mock_host_network(test, overrides, network_overrides):
    host = test.mock_host(dict(**overrides))
    host_network = test.mock_host_network(network_overrides, host=host)
    return host, host_network


def test_invalid(test):
    mock_host_network(
        test,
        dict(
            inv=1,
            status=HostStatus.INVALID,
            name="mock-host",
            macs=["00:00:00:00:00:01", "00:00:00:00:00:02", "00:00:00:00:00:03"],
            active_mac="00:00:00:00:00:01",
            active_mac_source=MAC_SOURCE_RACKTABLES,
            location=HostLocation(switch="zero-switch", port="zero-port", network_source=NETWORK_SOURCE_RACKTABLES),
        ),
        dict(
            network_switch="zero-switch",
            network_port="zero-port",
            network_timestamp=0,
            network_source=NETWORK_SOURCE_RACKTABLES,
            active_mac_time=0,
        ),
    )
    eine_netmap._sync()

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_synchronization(test):
    host, network = mock_host_network(
        test,
        dict(
            inv=1,
            name="mock-host",
            macs=["00:00:00:00:00:01", "00:00:00:00:00:02", "00:00:00:00:00:03"],
            active_mac="00:00:00:00:00:01",
            active_mac_source=MAC_SOURCE_RACKTABLES,
            location=HostLocation(switch="zero-switch", port="zero-port", network_source=NETWORK_SOURCE_RACKTABLES),
        ),
        dict(
            network_switch="zero-switch",
            network_port="zero-port",
            network_timestamp=0,
            network_source=NETWORK_SOURCE_RACKTABLES,
            active_mac_time=0,
        ),
    )
    eine_netmap._sync()

    host.location.network_source = NETWORK_SOURCE_EINE
    host.location.switch = "one-switch"
    host.location.port = "one-port"
    host.active_mac = "00:00:00:00:00:02"
    host.active_mac_source = MAC_SOURCE_EINE

    network.network_timestamp = mocked_eine_ts1
    network.network_switch = "one-switch"
    network.network_port = "one-port"
    network.active_mac = "00:00:00:00:00:02"
    network.active_mac_time = mocked_eine_ts1
    network.active_mac_source = MAC_SOURCE_EINE
    network.network_source = NETWORK_SOURCE_EINE

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_old_data(test):

    mock_host_network(
        test,
        dict(
            inv=2,
            name="mock-host",
            macs=["00:00:00:00:00:01", "00:00:00:00:00:02", "00:00:00:00:00:03"],
            active_mac="00:00:00:00:00:02",
            active_mac_source=MAC_SOURCE_RACKTABLES,
            location=HostLocation(switch="one-switch", port="one-port", network_source=NETWORK_SOURCE_RACKTABLES),
        ),
        dict(
            network_switch="one-switch",
            network_port="one-port",
            network_timestamp=1000,
            network_source=NETWORK_SOURCE_RACKTABLES,
            active_mac_time=1000,
        ),
    )
    eine_netmap._sync()

    test.hosts.assert_equal()
    test.host_network.assert_equal()
