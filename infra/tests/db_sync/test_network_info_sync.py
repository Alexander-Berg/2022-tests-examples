"""Tests EINE netmap synchronization."""

import pytest

from infra.walle.server.tests.lib.util import TestCase
from sepelib.core import constants
from walle.constants import (
    NETWORK_SOURCE_EINE,
    NETWORK_SOURCE_RACKTABLES,
    MAC_SOURCE_RACKTABLES,
    MAC_SOURCE_EINE,
)
from walle.db_sync import network_data
from walle.host_network import HostNetwork
from walle.hosts import HostLocation, HostStatus
from walle.models import timestamp


@pytest.fixture
def test(request, monkeypatch_timestamp, mp):

    return TestCase.create(request)


def _mock_host_network(test, overrides, network_overrides):
    host = test.mock_host(dict(**overrides))
    host_network = test.mock_host_network(network_overrides, host=host)
    return host, host_network


def _mock_non_synced_host_network(
    test, inv=1, status=HostStatus.READY, mac_none=False, switch_none=False, ips_none=False
):
    return _mock_host_network(
        test,
        dict(
            inv=inv,
            status=status,
            name="mock-host-{}".format(inv),
            ips=["old_ips"],
            active_mac="old_mac",
            active_mac_source=MAC_SOURCE_RACKTABLES,
            location=HostLocation(switch="old-switch", port="old_port", network_source=NETWORK_SOURCE_RACKTABLES),
        ),
        dict(
            active_mac=None if mac_none else "00:00:00:00:00:02",
            active_mac_source=None if mac_none else MAC_SOURCE_EINE,
            active_mac_time=None if mac_none else timestamp(),
            network_switch=None if switch_none else "one-switch",
            network_port=None if switch_none else "one-port",
            network_source=NETWORK_SOURCE_EINE,
            network_timestamp=timestamp(),
            ips=None if ips_none else ["new_ips"],
            ips_time=timestamp(),
        ),
    )


def test_remove_old_data_without_timeout(test):
    test.mock_host_network(dict(uuid="1", active_mac_time=None))
    assert HostNetwork.objects(uuid="1").count() == 1
    network_data.sync_hosts_network_info_wrapper()
    assert HostNetwork.objects(uuid="1").count() == 0


def test_remove_old_data_on_timeout(test):
    test.mock_host_network(dict(uuid="1", active_mac_time=timestamp() - constants.DAY_SECONDS - 1), add=False)
    network_data.sync_hosts_network_info_wrapper()
    test.host_network.assert_equal()


def test_do_not_remove_old_data_until_timeout(test):
    test.mock_host_network(dict(uuid="1", active_mac_time=(timestamp() - 1000)), add=True)
    network_data.sync_hosts_network_info_wrapper()
    test.host_network.assert_equal()


def test_update_limit_failed(test, monkeypatch):
    monkeypatch.setattr(network_data, "MAX_NETWORK_UPDATES", 2)

    for inv in range(3):
        _mock_non_synced_host_network(test, inv)

    assert not network_data.sync_hosts_network_info_wrapper()


def test_remove_limit_failed(test, monkeypatch):
    monkeypatch.setattr(network_data, "MAX_NETWORK_REMOVES", 2)

    for inv in range(3):
        test.mock_host_network({"uuid": str(inv)})

    assert not network_data.sync_hosts_network_info_wrapper()


def test_limit_passed(test, monkeypatch):
    monkeypatch.setattr(network_data, "MAX_NETWORK_UPDATES", 2)

    for inv in range(2):
        _mock_non_synced_host_network(test, inv)

    assert network_data.sync_hosts_network_info_wrapper()


def test_host_synchronization(test):
    host, network = _mock_non_synced_host_network(test, 1)

    network_data.sync_hosts_network_info_wrapper()

    host.active_mac = "00:00:00:00:00:02"
    host.active_mac_source = MAC_SOURCE_EINE
    host.location.switch = "one-switch"
    host.location.port = "one-port"
    host.location.network_source = NETWORK_SOURCE_EINE
    host.ips = ["new_ips"]

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_host_synchronization_only_mac(test):
    host, network = _mock_non_synced_host_network(test, 1, ips_none=True, switch_none=True)

    network_data.sync_hosts_network_info_wrapper()

    host.active_mac = "00:00:00:00:00:02"
    host.active_mac_source = MAC_SOURCE_EINE

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_host_synchronization_only_ips(test):
    host, network = _mock_non_synced_host_network(test, 1, mac_none=True, switch_none=True)

    network_data.sync_hosts_network_info_wrapper()

    host.ips = ["new_ips"]

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_host_synchronization_only_switch_port(test):
    host, network = _mock_non_synced_host_network(test, 1, mac_none=True, ips_none=True)

    network_data.sync_hosts_network_info_wrapper()

    host.location.switch = "one-switch"
    host.location.port = "one-port"
    host.location.network_source = NETWORK_SOURCE_EINE

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_invalid_no_sync(test):
    _mock_non_synced_host_network(test, 1, HostStatus.INVALID)

    network_data.sync_hosts_network_info_wrapper()

    test.hosts.assert_equal()
    test.host_network.assert_equal()
