"""Test DNS client."""

from unittest import mock

import dns.resolver
import pytest

import walle.clients.dns.local_dns_resolver
from sepelib.yandex.dns_api import DnsApiClient
from walle.clients.dns import local_dns_resolver, slayer_dns_api
from walle.clients.dns.slayer_dns_api import DnsClient, DnsInvalidZone, DnsAclKey, DnsAclKeyType


@pytest.fixture
def dns_api_client():
    dns_api_client = mock.MagicMock(DnsApiClient)
    return dns_api_client


def test_reverse_name_from_address():
    assert local_dns_resolver.reverse_name_from_address("192.168.0.1") == "1.0.168.192.in-addr.arpa."

    assert (
        local_dns_resolver.reverse_name_from_address("2a02:6b8:b010:70:225:90ff:fe83:1afc")
        == "c.f.a.1.3.8.e.f.f.f.0.9.5.2.2.0.0.7.0.0.0.1.0.b.8.b.6.0.2.0.a.2.ip6.arpa."
    )

    assert (
        local_dns_resolver.reverse_name_from_address("2a02:6b8:b010:0070:225:90ff:fe83:1afc")
        == "c.f.a.1.3.8.e.f.f.f.0.9.5.2.2.0.0.7.0.0.0.1.0.b.8.b.6.0.2.0.a.2.ip6.arpa."
    )

    assert (
        local_dns_resolver.reverse_name_from_address("::1")
        == "1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa."
    )


@pytest.mark.parametrize("suffix", ("", "."))
def test_address_from_reverse_name(suffix):
    assert slayer_dns_api.address_from_reverse_name("1.0.168.192.in-addr.arpa" + suffix) == "192.168.0.1"

    assert (
        slayer_dns_api.address_from_reverse_name(
            "c.f.a.1.3.8.e.f.f.f.0.9.5.2.2.0.0.7.0.0.0.1.0.b.8.b.6.0.2.0.a.2.ip6.arpa" + suffix
        )
        == "2a02:6b8:b010:70:225:90ff:fe83:1afc"
    )

    assert (
        slayer_dns_api.address_from_reverse_name(
            "1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa" + suffix
        )
        == "::1"
    )


def test_is_domain_in_zone():
    assert slayer_dns_api.is_domain_in_zone("1.1.168.192.in-addr.arpa", "168.192.in-addr.arpa.")
    assert slayer_dns_api.is_domain_in_zone("1.1.168.192.in-addr.arpa", "1.168.192.in-addr.arpa.")
    assert slayer_dns_api.is_domain_in_zone("1.1.168.192.in-addr.arpa", "0/25.1.168.192.in-addr.arpa.")
    assert not slayer_dns_api.is_domain_in_zone("254.1.168.192.in-addr.arpa", "0/25.1.168.192.in-addr.arpa.")

    assert slayer_dns_api.is_domain_in_zone(
        "c.f.a.1.3.8.e.f.f.f.0.9.5.2.2.0.0.7.0.0.0.1.0.b.8.b.6.0.2.0.a.2.ip6.arpa", "1.0.b.8.b.6.0.2.0.a.2.ip6.arpa"
    )
    assert not slayer_dns_api.is_domain_in_zone(
        "c.f.a.1.3.8.e.f.f.f.0.9.5.2.2.0.0.7.0.0.0.1.0.b.8.b.6.0.2.0.a.2.ip6.arpa", "1.1.0.b.8.b.6.0.2.0.a.2.ip6.arpa"
    )

    assert slayer_dns_api.is_domain_in_zone("sas1-2000.search.yandex.net", "search.yandex.net")
    assert slayer_dns_api.is_domain_in_zone("sas1-2000.freud.search.yandex.net", "freud.search.yandex.net")
    assert slayer_dns_api.is_domain_in_zone("sas1-2000.freud.search.yandex.net", "search.yandex.net")
    assert not slayer_dns_api.is_domain_in_zone("sas1-2000.search.yandex.net", "arch.yandex.net")


class TestIsZoneOwner:
    def mock_dns_client(self, mp, monkeypatch, dns_api_client, zone_info):
        resolver = walle.clients.dns.local_dns_resolver.LocalDNSResolver()
        monkeypatch.setattr(dns.resolver.Resolver, name="query", value=mock.MagicMock())
        zone_for_name = mock.MagicMock(side_effect=lambda x: "yt.yandex.net.")

        mp.config("dns_api.host", "foo")
        mp.config("dns_api.access_token", "foo")
        mp.config("dns_api.user", "foo")
        mp.config("dns_api.validate_only", "foo")

        get_zone_info = mock.MagicMock(side_effect=lambda x: zone_info)
        monkeypatch.setattr(resolver, "get_zone_for_name", zone_for_name)
        monkeypatch.setattr(dns_api_client, "zone_info", get_zone_info)
        dns_api_client.login = "robot-walle"
        dns_client = DnsClient()
        dns_client._dns_api_client = dns_api_client
        dns_client._ns_client = resolver
        return dns_client

    @pytest.mark.parametrize(
        "acl_list,expected_parsed",
        [
            (
                "USER_KEY(robot-walle);USER_KEY(selfdns-api-search);",
                [
                    DnsAclKey("robot-walle", DnsAclKeyType.USER),
                    DnsAclKey("selfdns-api-search", DnsAclKeyType.USER),
                ],
            ),
            (
                "GROUP_KEYS(dpt_yandex_mnt_sa_runtime_mondev_0023);GROUP_KEYS(dpt_yandex_mnt_sa_searchinf_infra);",
                [
                    DnsAclKey("dpt_yandex_mnt_sa_runtime_mondev_0023", DnsAclKeyType.GROUP),
                    DnsAclKey("dpt_yandex_mnt_sa_searchinf_infra", DnsAclKeyType.GROUP),
                ],
            ),
        ],
    )
    def test_dns_acl_parser(self, acl_list, expected_parsed):
        assert [k for k in DnsClient._parse_dns_acl(acl_list)] == expected_parsed

    def test_is_zone_owner(self, mp, monkeypatch, dns_api_client):
        dns_client = self.mock_dns_client(
            mp, monkeypatch, dns_api_client, {"acl-list": "USER_KEY(robot-walle);USER_KEY(selfdns-api-search);"}
        )

        assert dns_client.is_zone_owner("m01i.freud.yt.yandex.net")

    def test_is_not_zone_owner(self, mp, monkeypatch, dns_api_client):
        dns_client = self.mock_dns_client(
            mp,
            monkeypatch,
            dns_api_client,
            {"acl-list": "GROUP_KEYS(dpt_yandex_mnt_sa_perform_parallel);USER_KEY(selfdns-api-search);"},
        )

        assert not dns_client.is_zone_owner("m01i.freud.yt.yandex.net")

    def test_invalid_zone(self, mp, monkeypatch, dns_api_client):
        dns_client = self.mock_dns_client(mp, monkeypatch, dns_api_client, {"mock": "mock"})

        with pytest.raises(DnsInvalidZone):
            dns_client.is_zone_owner("m01i.freud.yt.yandex.net")
