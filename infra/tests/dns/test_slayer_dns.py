"""Test DNS operations module."""
import ipaddress
from unittest import mock

import pytest

import walle.dns.dns_lib
from walle.clients.dns import DnsApiOperation, slayer_dns_api
from .mocks import configure_nameservers


@pytest.fixture
def dns_client(monkeypatch):
    dns_client_mock = mock.MagicMock(slayer_dns_api.DnsClient)
    monkeypatch.setattr("walle.clients.dns.slayer_dns_api.DnsClient", dns_client_mock)

    network_client = mock.MagicMock(slayer_dns_api.dns_api._DnsApiNetworkClient)()
    dns_client_mock()._dns_api_client.network_client = network_client

    return dns_client_mock


def test_build_dns_map__1(dns_client, test):
    project = test.mock_project({"id": "some-id"})
    test.mock_host({"name": "host-1", "inv": 1, "project": project.id})
    mock_data = {
        "A": {"host-1.": "127.0.0.1", "host-2.": "10.0.0.1"},
        "PTR": {
            "127.0.0.1": "host-2.",
            "10.0.0.1": "host-3.",
            "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "host-4.",
        },
        "AAAA": {"host-3.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334"},
    }
    configure_nameservers(dns_client, mock_data)

    expected = mock_data
    actual = walle.dns.dns_lib._build_dns_map(project, "host-1", ["127.0.0.1"], None, max_depth=4)

    assert actual == expected


@pytest.mark.parametrize("max_depth", [1, 2])
def test_build_dns_map__2(dns_client, max_depth, test):
    """build_dns_map should return consistent results for small maps with various max_depth"""
    project = test.mock_project({"id": "some-id"})
    test.mock_host({"name": "hostname", "inv": 1, "project": project.id})
    mock_data = {
        "A": {"hostname.": "10.0.0.1"},
        "AAAA": {"hostname.": "2a02:06b8:b010:0070:0225:90ff:fe88:b555"},
        "PTR": {
            "10.0.0.1": "hostname.",
            "2a02:06b8:b010:0070:0225:90ff:fe88:b555": "hostname.",
        },
    }
    configure_nameservers(dns_client, mock_data)

    expected = mock_data
    actual = walle.dns.dns_lib._build_dns_map(
        project,
        "hostname",
        ["127.0.0.1"],
        ["2a02:06b8:b010:0070:0225:90ff:fe88:b334"],
        max_depth=max_depth,
    )

    assert actual == expected


def test_get_dns_operations__noop__single_network(dns_client, test):
    test.mock_host({"name": "host-1", "inv": 1})
    mock_data = {
        "A": {"host-1.": "127.0.0.1"},
        "AAAA": {"host-1.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334"},
        "PTR": {
            "127.0.0.1": "host-1.",
            "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "host-1.",
        },
    }
    configure_nameservers(dns_client, mock_data)

    expected = []
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "host-1",
        ["127.0.0.1"],
        ["2a02:06b8:b010:0070:0225:90ff:fe88:b334"],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__noop__multiple_networks(dns_client, test):
    test.mock_host({"name": "host-1", "inv": 1})
    mock_data = {
        "A": {"host-1.": "127.0.0.1"},
        "AAAA": {"host-1.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334"},
        "PTR": {
            "127.0.0.1": "host-1.",
            "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "host-1.",
        },
    }
    configure_nameservers(dns_client, mock_data)

    expected = []
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "host-1",
        ["192.168.1.10", "127.0.0.1"],
        ["2a02:06b8:b011:0070:0225:90ff:fe88:b334", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__new_host__single_network(dns_client, test):
    test.mock_host({"name": "host-1", "inv": 1})
    mock_data = {
        "A": {},
        "AAAA": {},
        "PTR": {},
    }
    configure_nameservers(dns_client, mock_data)

    expected = [
        DnsApiOperation.add("A", "host-1", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "host-1"),
        DnsApiOperation.add("AAAA", "host-1", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "host-1"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "host-1",
        ["127.0.0.1"],
        ["2a02:06b8:b010:0070:0225:90ff:fe88:b334"],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__new_host__multiple_networks(dns_client, test):
    test.mock_host({"name": "host-1", "inv": 1})
    mock_data = {
        "A": {},
        "AAAA": {},
        "PTR": {},
    }
    configure_nameservers(dns_client, mock_data)

    expected = [
        DnsApiOperation.add("A", "host-1", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "host-1"),
        DnsApiOperation.add("AAAA", "host-1", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "host-1"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "host-1",
        ["127.0.0.1", "127.0.0.2"],
        ["2a02:06b8:b010:0070:0225:90ff:fe88:b334", "2a02:06b8:b011:0070:0225:90ff:fe88:b334"],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__change_ip_1(dns_client, test):
    """New IP-address is free."""
    test.mock_host({"name": "hostname", "inv": 1})
    configure_nameservers(
        dns_client,
        {
            "A": {"hostname.": "10.0.0.1"},
            "AAAA": {"hostname.": "2a02:06b8:b010:0070:0225:90ff:fe88:b555"},
            "PTR": {
                "10.0.0.1": "hostname.",
                "2a02:06b8:b010:0070:0225:90ff:fe88:b555": "hostname.",
            },
        },
    )
    expected = [
        DnsApiOperation.delete("PTR", "10.0.0.1", "hostname."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b555", "hostname."),
        DnsApiOperation.delete("A", "hostname", "10.0.0.1"),
        DnsApiOperation.delete("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b555"),
        DnsApiOperation.add("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "hostname"),
        DnsApiOperation.add("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "hostname",
        ["127.0.0.1", "192.168.1.1"],
        ["2a02:06b8:b010:0070:0225:90ff:fe88:b334", "2a02:06b8:b011:0070:0225:90ff:fe88:b334"],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__change_ip_2(dns_client, test):
    """New IP-addresses were taken by other hosts."""
    test.mock_host({"name": "hostname", "inv": 1})
    configure_nameservers(
        dns_client,
        {
            "A": {
                "hostname.": "10.0.0.1",
                "some-other-host-1.": "127.0.0.1",
                "some-other-host-2.": "127.0.2.1",
            },
            "AAAA": {
                "hostname.": "2a02:06b8:b010:0070:0225:90ff:fe88:b555",
                "some-other-host-1.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334",
                "some-other-host-2.": "2a02:06b8:b012:0070:0225:90ff:fe88:b334",
            },
            "PTR": {
                "10.0.0.1": "hostname.",
                "2a02:06b8:b010:0070:0225:90ff:fe88:b555": "hostname.",
                "127.0.0.1": "some-other-host-1.",
                "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "some-other-host-1.",
                "127.0.2.1": "some-other-host-2.",
                "2a02:06b8:b012:0070:0225:90ff:fe88:b334": "some-other-host-2.",
            },
        },
    )
    expected = [
        DnsApiOperation.delete("A", "some-other-host-2", "127.0.2.1"),
        DnsApiOperation.delete("AAAA", "some-other-host-2", "2a02:06b8:b012:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.delete("A", "some-other-host-1", "127.0.0.1"),
        DnsApiOperation.delete("AAAA", "some-other-host-1", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.delete("PTR", "10.0.0.1", "hostname."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b555", "hostname."),
        DnsApiOperation.delete("PTR", "127.0.2.1", "some-other-host-2."),
        DnsApiOperation.delete("PTR", "127.0.0.1", "some-other-host-1."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b012:0070:0225:90ff:fe88:b334", "some-other-host-2."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "some-other-host-1."),
        DnsApiOperation.delete("A", "hostname", "10.0.0.1"),
        DnsApiOperation.delete("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b555"),
        DnsApiOperation.add("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "hostname"),
        DnsApiOperation.add("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "hostname",
        ["127.0.0.1", "127.0.1.1", "127.0.2.1"],
        [
            "2a02:06b8:b010:0070:0225:90ff:fe88:b334",
            "2a02:06b8:b011:0070:0225:90ff:fe88:b334",
            "2a02:06b8:b012:0070:0225:90ff:fe88:b334",
        ],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__change_ip_3(dns_client, test):
    test.mock_host({"name": "hostname", "inv": 1})
    """New IP-addresses were taken by other hosts and there were no records for previous addresses."""
    configure_nameservers(
        dns_client,
        {
            "A": {"some-other-host.": "127.0.0.1"},
            "AAAA": {"some-other-host.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334"},
            "PTR": {
                "10.0.0.1": "some-other-host.",  # do not delete this
                "127.0.0.1": "some-other-host.",
                "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "some-other-host.",
            },
        },
    )
    expected = [
        DnsApiOperation.delete("A", "some-other-host", "127.0.0.1"),
        DnsApiOperation.delete("PTR", "127.0.0.1", "some-other-host."),
        DnsApiOperation.add("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "hostname"),
        DnsApiOperation.add("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b555"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b555", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "hostname",
        ["127.0.0.1"],
        ["2a02:06b8:b010:0070:0225:90ff:fe88:b555"],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__change_hostname_1(dns_client, test):
    test.mock_host({"name": "hostname", "inv": 1})
    configure_nameservers(
        dns_client,
        {
            "A": {"some-hostname.": "127.0.0.1"},
            "PTR": {
                "127.0.0.1": "some-hostname.",
                "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "some-hostname.",
            },
            "AAAA": {"some-hostname.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334"},
        },
    )
    expected = [
        DnsApiOperation.delete("A", "some-hostname", "127.0.0.1"),
        DnsApiOperation.delete("AAAA", "some-hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.delete("PTR", "127.0.0.1", "some-hostname."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "some-hostname."),
        DnsApiOperation.add("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "hostname"),
        DnsApiOperation.add("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "hostname",
        ["127.0.0.1", "127.1.0.1"],
        ["2a02:06b8:b010:0070:0225:90ff:fe88:b334", "2a02:06b8:b011:0070:0225:90ff:fe88:b334"],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__change_hostname_2(dns_client, test):
    test.mock_host({"name": "hostname", "inv": 1})
    configure_nameservers(
        dns_client,
        {
            "A": {"some-hostname.": "127.1.0.1"},
            "PTR": {
                "127.1.0.1": "some-hostname.",
                "2a02:06b8:b011:0070:0225:90ff:fe88:b334": "some-hostname.",
            },
            "AAAA": {"some-hostname.": "2a02:06b8:b011:0070:0225:90ff:fe88:b334"},
        },
    )
    expected = [
        DnsApiOperation.delete("A", "some-hostname", "127.1.0.1"),
        DnsApiOperation.delete("AAAA", "some-hostname", "2a02:06b8:b011:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.delete("PTR", "127.1.0.1", "some-hostname."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b011:0070:0225:90ff:fe88:b334", "some-hostname."),
        DnsApiOperation.add("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "hostname"),
        DnsApiOperation.add("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "hostname",
        ["127.0.0.1", "127.1.0.1"],
        ["2a02:06b8:b010:0070:0225:90ff:fe88:b334", "2a02:06b8:b011:0070:0225:90ff:fe88:b334"],
        mock.Mock(),
    )

    assert actual == expected


def test_get_dns_operations__dont_delete_hostname_1(dns_client, test):
    test.mock_host({"name": "hostname", "inv": 1})
    # Some third-party hostname points to our IP
    # We must not delete it
    # PS: We don't delete it because PTR points to our hostname ;-)
    configure_nameservers(
        dns_client,
        {
            "A": {"hostname.": "127.0.0.1", "some-other-hostname.": "127.0.0.1"},
            "PTR": {"127.0.0.1": "hostname."},
            "AAAA": {},
        },
    )
    expected = [
        DnsApiOperation.delete("PTR", "127.0.0.1", "hostname."),
        DnsApiOperation.delete("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("A", "hostname", "10.0.0.1"),
        DnsApiOperation.add("PTR", "10.0.0.1", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host("hostname", ["10.0.0.1"], None, mock.Mock())
    assert actual == expected


def test_get_dns_operations__dont_delete_hostname_2(dns_client, test):
    test.mock_host({"name": "hostname", "inv": 1})
    configure_nameservers(
        dns_client,
        {
            "A": {"hostname.": "127.0.0.1", "some-other-hostname.": "127.0.0.1"},
            "PTR": {"127.0.0.1": "some-other-hostname."},
            "AAAA": {},
        },
    )
    expected = [
        DnsApiOperation.delete("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("A", "hostname", "10.0.0.1"),
        DnsApiOperation.add("PTR", "10.0.0.1", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host("hostname", ["10.0.0.1"], None, mock.Mock())

    assert actual == expected


def test_get_dns_operations__dont_delete_hostname_3(dns_client, test):
    test.mock_host({"name": "hostname", "inv": 1})
    configure_nameservers(
        dns_client,
        {
            "A": {
                "hostname.": "127.0.0.1",
                "some-other-hostname.": "127.0.0.2",
                "some-other-hostname-2.": "127.0.0.2",
            },
            "AAAA": {"some-other-hostname-2.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334"},
            "PTR": {
                "127.0.0.1": "some-other-hostname.",
                "127.0.0.2": "some-other-hostname-2.",
                "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "some-other-hostname-2.",
            },
        },
    )
    expected = [
        DnsApiOperation.delete("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("A", "hostname", "10.0.0.1"),
        DnsApiOperation.add("PTR", "10.0.0.1", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host("hostname", ["10.0.0.1"], None, mock.Mock())

    assert actual == expected


def test_get_dns_operations__dont_delete_hostname_4(dns_client, test):
    test.mock_host({"name": "new-hostname", "inv": 1})
    configure_nameservers(
        dns_client,
        {
            "A": {
                "hostname.": "127.0.0.1",
                "some-other-hostname.": "127.0.0.2",
                "some-other-hostname-2.": "127.0.0.2",
            },
            "AAAA": {"some-other-hostname-2.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334"},
            "PTR": {
                "127.0.0.1": "some-other-hostname.",
                "127.0.0.2": "some-other-hostname-2.",
                "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "some-other-hostname-2.",
            },
        },
    )
    expected = [
        DnsApiOperation.delete("PTR", "127.0.0.1", "some-other-hostname."),
        DnsApiOperation.add("A", "new-hostname", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "new-hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host("new-hostname", ["127.0.0.1"], None, mock.Mock())

    assert actual == expected


def test_get_dns_operations__dont_delete_hostname_5(dns_client, test):
    test.mock_host({"name": "hostname", "inv": 1})
    configure_nameservers(
        dns_client,
        {
            "A": {"hostname.": "127.0.0.1"},
            "PTR": {"127.0.0.1": "some-other-hostname."},
            "AAAA": {},
        },
    )
    expected = [
        DnsApiOperation.delete("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("A", "hostname", "10.0.0.1"),
        DnsApiOperation.add("PTR", "10.0.0.1", "hostname"),
    ]
    actual = walle.dns.dns_lib._get_operations_for_one_host(
        "hostname",
        ["10.0.0.1"],
        None,
        mock.Mock(),
        __max_dns_map_depth=1,
    )

    assert actual == expected


def test_iterative_deletion(dns_client, test):
    test.mock_host({"name": "hostname", "inv": 1})

    def is_ip(value):
        try:
            ipaddress.ip_address(str(value))
        except ValueError:
            return False
        else:
            return True

    nameservers_data = {
        "A": {
            "hostname.": "10.0.0.1",
            "some-other-host-1.": "127.0.0.1",
            "some-other-host-2.": "192.168.1.1",
        },
        "AAAA": {
            "hostname.": "2a02:06b8:b010:0070:0225:90ff:fe88:b555",
            "some-other-host-1.": "2a02:06b8:b010:0070:0225:90ff:fe88:b334",
            "some-other-host-2.": "2a02:06b8:b011:0070:0225:90ff:fe88:b334",
        },
        "PTR": {
            "10.0.0.1": "hostname.",
            "127.0.0.1": "some-other-host-1.",
            "192.168.1.1": "some-other-host-2.",
            "2a02:06b8:b010:0070:0225:90ff:fe88:b555": "hostname.",
            "2a02:06b8:b010:0070:0225:90ff:fe88:b334": "some-other-host-1.",
            "2a02:06b8:b011:0070:0225:90ff:fe88:b334": "some-other-host-2.",
        },
    }
    configure_nameservers(dns_client, nameservers_data)

    expected = [
        DnsApiOperation.delete("A", "some-other-host-2", "192.168.1.1"),
        DnsApiOperation.delete("AAAA", "some-other-host-2", "2a02:06b8:b011:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.delete("A", "some-other-host-1", "127.0.0.1"),
        DnsApiOperation.delete("AAAA", "some-other-host-1", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.delete("PTR", "10.0.0.1", "hostname."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b555", "hostname."),
        DnsApiOperation.delete("PTR", "192.168.1.1", "some-other-host-2."),
        DnsApiOperation.delete("PTR", "127.0.0.1", "some-other-host-1."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b011:0070:0225:90ff:fe88:b334", "some-other-host-2."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "some-other-host-1."),
        DnsApiOperation.delete("A", "hostname", "10.0.0.1"),
        DnsApiOperation.delete("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b555"),
        DnsApiOperation.add("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "hostname"),
        DnsApiOperation.add("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "hostname"),
    ]

    for i in range(len(expected)):
        from pprint import pprint

        print("nameservers_data:")
        pprint(nameservers_data)
        actual = walle.dns.dns_lib._get_operations_for_one_host(
            "hostname",
            ["127.0.0.1", "192.168.1.1"],
            ["2a02:06b8:b010:0070:0225:90ff:fe88:b334", "2a02:06b8:b011:0070:0225:90ff:fe88:b334"],
            mock.Mock(),
        )
        assert actual == expected[i:]

        operation = expected[i]
        key = operation.name if is_ip(operation.name) else operation.name + "."
        if operation.operation == operation.DNS_API_DELETE:
            del nameservers_data[operation.type][key]
        else:
            value = operation.data if is_ip(operation.data) else operation.data + "."
            nameservers_data[operation.type][key] = value
