"""
    NOTE(rocco66): rurikk_dns has automatic for PTR creation https://st.yandex-team.ru/CLOUD-78777
"""

from unittest import mock

import pytest

import walle.dns.dns_lib
from infra.walle.server.tests.lib import dns as dns_test_lib
from walle.clients.dns import DnsApiOperation, rurikk_dns_api
from .mocks import configure_nameservers

TEST_HOSTNAME = "hostname"
TEST_HOSTNAME_WITH_DOT = f"{TEST_HOSTNAME}."
TEST_IPV6 = "2a02:06b8:b010:0070:0225:90ff:fe88:b334"
TEST_IPV4 = "127.0.0.1"


@pytest.fixture(autouse=True)
def prepare_dns_box_config_and_grpc_client(mp):
    dns_test_lib.dns_box(mp)


@pytest.fixture
def dns_client(monkeypatch):
    dns_client_mock = mock.MagicMock(rurikk_dns_api.RurikkDnsClient)
    monkeypatch.setattr("walle.clients.dns.rurikk_dns_api.RurikkDnsClient", dns_client_mock)
    return dns_client_mock


def _check(test, dns_client, dns_state, expected, ipv4_exists=False):
    project = test.mock_project(
        {
            "id": dns_test_lib.TEST_DNS_BOX_PROJECT,
            "name": "Some name",
            "yc_dns_zone_id": dns_test_lib.TEST_DNS_ZONE_ID,
        }
    )
    test.mock_host({"name": TEST_HOSTNAME, "inv": 1, "project": project.id})
    configure_nameservers(dns_client, dns_state)
    ipv4_list = []
    if ipv4_exists:
        ipv4_list.append(TEST_IPV4)
    actual = walle.dns.dns_lib._get_operations_for_one_host(TEST_HOSTNAME, ipv4_list, [TEST_IPV6], mock.Mock())
    assert actual == expected


@pytest.mark.parametrize("ipv4_exists", [True, False])
def test_init_state(dns_client, test, ipv4_exists):
    # NOTE(rocco66): keep A, create AAAA, do not create PTR
    dns_state = {}
    if ipv4_exists:
        dns_state["A"] = {TEST_HOSTNAME_WITH_DOT: TEST_IPV4}
    expected = [
        DnsApiOperation.add("AAAA", TEST_HOSTNAME, TEST_IPV6),
    ]
    _check(test, dns_client, dns_state, expected, ipv4_exists)


@pytest.mark.parametrize("ptr_exists", [True, False])
def test_keep_aaaa_ignore_ptr(dns_client, test, ptr_exists):
    dns_state = {
        "AAAA": {TEST_HOSTNAME_WITH_DOT: TEST_IPV6},
    }
    if ptr_exists:
        dns_state["PTR"] = {TEST_IPV6: TEST_HOSTNAME_WITH_DOT}
    expected = []
    _check(test, dns_client, dns_state, expected)


@pytest.mark.parametrize("wrong_ptr_exists", [True, False])
def test_fix_aaaa_ignore_ptr(dns_client, test, wrong_ptr_exists):
    wrong_ipv6 = f"{TEST_IPV6[:-5]}:beef"
    dns_state = {
        "AAAA": {TEST_HOSTNAME_WITH_DOT: wrong_ipv6},
    }
    if wrong_ptr_exists:
        dns_state["PTR"] = {wrong_ipv6: TEST_HOSTNAME_WITH_DOT}
    expected = [
        DnsApiOperation.delete("AAAA", TEST_HOSTNAME, wrong_ipv6),
        DnsApiOperation.add("AAAA", TEST_HOSTNAME, TEST_IPV6),
    ]
    _check(test, dns_client, dns_state, expected)
