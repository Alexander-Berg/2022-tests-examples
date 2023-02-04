"""Tests network module."""


from unittest import mock

import pytest

import walle.views.helpers.validators as validators
from infra.walle.server.tests.lib.util import patch, patch_attr, TestCase, mock_location, NATIVE_VLAN
from sepelib.core.exceptions import LogicalError
from walle import constants as walle_constants, network
from walle.hosts import Host, HostLocation
from walle.network import DnsRecord
from walle.projects import Project
from walle.errors import HostNameTemplateError

_DNS_DOMAIN = 'fake.yandex.net'


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture(params=("search", "qloud-h"))
def mock_search_host(monkeypatch, request):
    domain = "{}.{}".format(request.param, _DNS_DOMAIN)
    host = Host(name="localhost.walle.{}".format(domain), project=request.param)
    patch_attr(
        monkeypatch,
        host,
        "get_project",
        return_value=Project(vlan_scheme=walle_constants.VLAN_SCHEME_SEARCH, native_vlan=604, dns_domain=domain),
    )
    return host


@pytest.fixture()
def mock_hbf_search_host(monkeypatch):
    host = Host(name="localhost.walle.{}".format(_DNS_DOMAIN))
    patch_attr(
        monkeypatch,
        host,
        "get_project",
        return_value=Project(
            vlan_scheme=walle_constants.VLAN_SCHEME_SEARCH,
            native_vlan=604,
            hbf_project_id=int("686", 16),
            dns_domain=_DNS_DOMAIN,
        ),
    )
    return host


@pytest.fixture()
def mock_mtn_host(monkeypatch):
    host = Host(name="localhost.walle.{}".format(_DNS_DOMAIN))
    patch_attr(
        monkeypatch,
        host,
        "get_project",
        return_value=Project(
            vlan_scheme=walle_constants.VLAN_SCHEME_MTN,
            native_vlan=333,
            hbf_project_id=int("686", 16),
            dns_domain=_DNS_DOMAIN,
        ),
    )
    return host


@pytest.fixture()
def mock_mtn_hostid_host(monkeypatch):
    host = Host(name="localhost.walle.{}".format(_DNS_DOMAIN))
    patch_attr(
        monkeypatch,
        host,
        "get_project",
        return_value=Project(
            vlan_scheme=walle_constants.VLAN_SCHEME_MTN_HOSTID,
            native_vlan=333,
            hbf_project_id=int("686", 16),
            dns_domain=_DNS_DOMAIN,
        ),
    )
    return host


@pytest.fixture()
def mock_static_host(monkeypatch):
    domain = "{}.{}".format("space-x", _DNS_DOMAIN)
    host = Host(name="localhost.walle.{}".format(domain))
    patch_attr(
        monkeypatch,
        host,
        "get_project",
        return_value=Project(vlan_scheme=walle_constants.VLAN_SCHEME_STATIC, native_vlan=604, dns_domain=domain),
    )
    return host


@pytest.fixture()
def mock_hbf_static_host(monkeypatch):
    domain = "{}.{}".format("space-x", _DNS_DOMAIN)
    host = Host(name="localhost.walle.{}".format(domain))
    patch_attr(
        monkeypatch,
        host,
        "get_project",
        return_value=Project(
            vlan_scheme=walle_constants.VLAN_SCHEME_STATIC,
            native_vlan=604,
            hbf_project_id=int("686", 16),
            dns_domain=domain,
        ),
    )
    return host


def test_free_host_name_template():
    template = network.get_free_host_name_template()

    assert template.fqdn_matches("free-12345.wall-e.yandex.net")
    assert template.fqdn_matches("free-123456.wall-e.yandex.net")  # inventory num is not limited to 5 digits
    assert not template.fqdn_matches("free1-12345.wall-e.yandex.net")  # no buckets in free hosts
    assert not template.fqdn_matches("free-12345.search.yandex.net")  # domain doesn't match

    assert template.shortname_matches("free-12345.wall-e.yandex.net")
    assert template.shortname_matches("free-12345.search.yandex.net")
    assert template.shortname_matches("free-123456.wall-e.yandex.net")
    assert not template.shortname_matches("free-12345")
    assert not template.shortname_matches("free1-12345.wall-e.yandex.net")

    assert template.fill(1) == "free-1.wall-e.yandex.net"
    assert template.fill(9999) == "free-9999.wall-e.yandex.net"


def test_default_hostname_template():
    template = network.get_default_host_name_template("sas", "search.yandex.net")

    assert template.fqdn_matches("sas0-1234.search.yandex.net")
    assert template.fqdn_matches("sas1-1234.search.yandex.net")
    assert template.fqdn_matches("sas2-1234.search.yandex.net")
    assert not template.fqdn_matches("sas3-1234.qloud-h.yandex.net")
    assert not template.fqdn_matches("sas-1234.search.yandex.net")
    assert not template.fqdn_matches("sas30-1234.search.yandex.net")
    assert not template.fqdn_matches("sas1-01234.search.yandex.net")
    assert not template.fqdn_matches("sas1-123.search.yandex.net")

    assert template.shortname_matches("sas0-1234.search.yandex.net")
    assert template.shortname_matches("sas1-1234.search.yandex.net")
    assert template.shortname_matches("sas2-1234.qloud-h.yandex.net")
    assert not template.shortname_matches("sas0-1234")
    assert not template.shortname_matches("sas-1234.search.yandex.net")
    assert not template.shortname_matches("sas30-1234.search.yandex.net")
    assert not template.shortname_matches("sas1-01234.search.yandex.net")
    assert not template.shortname_matches("sas1-123.search.yandex.net")

    assert template.fill(1) == "sas0-0001.search.yandex.net"
    assert template.fill(9999) == "sas0-9999.search.yandex.net"
    assert template.fill(10001) == "sas1-0001.search.yandex.net"
    assert template.fill(19999) == "sas1-9999.search.yandex.net"

    with pytest.raises(HostNameTemplateError):
        template.fill(100000)


@pytest.mark.parametrize(
    "location,name",
    (
        ({"country": "FI", "city": "MANTSALA"}, "man"),
        ({"country": "RU", "city": "IVA"}, "iva"),
        ({"country": "RU", "city": "MYT"}, "myt"),
        ({"country": "RU", "city": "SAS"}, "sas"),
        ({"country": "RU", "city": "VLADIMIR"}, "vla"),
        ({"country": "RU", "city": "MOW", "datacenter": "FOL"}, "fol"),
        ({"country": "RU", "city": "MOW", "datacenter": "UGRB"}, "ugr"),
    ),
)
def test_search_host_name_generation(location, name):
    host = Host(location=HostLocation(physical_timestamp=0, **location), project="search")

    project_mock = Project(vlan_scheme=walle_constants.VLAN_SCHEME_SEARCH, dns_domain=_DNS_DOMAIN)
    with mock.patch.object(host, "get_project", return_value=project_mock):
        template = network.get_host_name_template(host)

    assert template.fill(0) == name + "0-0000.{}".format(_DNS_DOMAIN)


class TestHostNameTemplate:
    domain = "search.yandex.net"
    location = "mocloc"

    @classmethod
    def _mk_template(cls, template, validate=True):
        if validate:
            validators.validated_host_shortname_template(template)
        return network.get_custom_host_name_template(template, cls.location, cls.domain)

    @pytest.mark.parametrize(
        ["template", "index", "expected"],
        [
            ("solomon-pre-fetcher-{location}-{index}", 1, "solomon-pre-fetcher-mocloc-1.search.yandex.net"),
            ("solomon-pre-fetcher-{location}-{index}", 20, "solomon-pre-fetcher-mocloc-20.search.yandex.net"),
            ("solomon-pre-fetcher-{location}-{index}", 300, "solomon-pre-fetcher-mocloc-300.search.yandex.net"),
        ],
    )
    def test_host_name_generation_without_bucket_without_index_padding(self, template, index, expected):
        actual = self._mk_template(template).fill(index)
        assert expected == actual

    @pytest.mark.parametrize(
        ["template", "index", "expected"],
        [
            ("solomon-pre-fetcher-{location}-{index:02}", 1, "solomon-pre-fetcher-mocloc-01.search.yandex.net"),
            ("solomon-pre-fetcher-{location}-{index:02d}", 1, "solomon-pre-fetcher-mocloc-01.search.yandex.net"),
            ("solomon-pre-fetcher-{location}-{index:02d}", 100, "solomon-pre-fetcher-mocloc-100.search.yandex.net"),
            ("solomon-pre-fetcher-{location}-{index:05}", 100, "solomon-pre-fetcher-mocloc-00100.search.yandex.net"),
            (
                "solomon-pre-fetcher-{location}-{index:05}",
                100000,
                "solomon-pre-fetcher-mocloc-100000.search.yandex.net",
            ),
        ],
    )
    def test_host_name_generation_without_bucket_with_index_padding(self, template, index, expected):
        actual = self._mk_template(template).fill(index)
        assert expected == actual

    @pytest.mark.parametrize(
        ["template", "index", "expected"],
        [
            ("lbkx-{location}{bucket}-{index:02}", 1, "lbkx-mocloc0-01.search.yandex.net"),
            ("lbks-{location}{bucket}-{index:02d}", 1, "lbks-mocloc0-01.search.yandex.net"),
            ("spof-{location}{bucket}-{index:02d}", 100, "spof-mocloc1-00.search.yandex.net"),
            ("spof-{location}{bucket}-{index:02d}", 999, "spof-mocloc9-99.search.yandex.net"),
        ],
    )
    def test_host_name_generation_with_bucket_with_upper_bound(self, template, index, expected):
        actual = self._mk_template(template).fill(index)
        assert expected == actual

    @pytest.mark.parametrize(
        ["template", "index"],
        [
            ("lbkx-{location}{bucket}-{index:01}", 100),
            ("lbks-{location}{bucket}-{index:02d}", 2000),
        ],
    )
    def test_host_name_generation_with_bucket_above_upper_bound_is_logical_error(self, template, index):
        with pytest.raises(HostNameTemplateError):
            self._mk_template(template).fill(index)

    @pytest.mark.parametrize(
        ["template", "index"],
        [
            ("lbkx-{location}{bucket}-{index}", 1),
            ("spof-{location}{bucket}-{index}", 100),
        ],
    )
    def test_host_name_generation_with_bucket_without_upper_bound_is_logical_error(self, template, index):
        with pytest.raises(LogicalError):
            # this is not valid template actually
            self._mk_template(template, validate=False).fill(index)

    @pytest.mark.parametrize(
        ["template", "fqdn", "expected_index"],
        [
            ("spof-{location}-{index:01}", "spof-mocloc-1.search.yandex.net", 1),
            ("spof-{location}-{index:02}", "spof-mocloc-01.search.yandex.net", 1),
            ("spof-{location}-{index:02d}", "spof-mocloc-01.search.yandex.net", 1),
            ("spof-{location}-{index:02d}", "spof-mocloc-10.search.yandex.net", 10),
            ("spof-{location}-{index}", "spof-mocloc-10.search.yandex.net", 10),
            ("spof-{location}-{index}", "spof-mocloc-010.search.yandex.net", 10),
        ],
    )
    def test_index_extraction_without_bucket(self, template, fqdn, expected_index):
        index = self._mk_template(template).get_index(fqdn)
        assert expected_index == index

    @pytest.mark.parametrize(
        ["template", "fqdn", "expected_index"],
        [
            ("spof-{location}{bucket}-{index:01}", "spof-mocloc0-1.search.yandex.net", 1),
            ("spof-{location}{bucket}-{index:01}", "spof-mocloc1-1.search.yandex.net", 11),
            ("spof-{location}{bucket}-{index:02}", "spof-mocloc0-01.search.yandex.net", 1),
            ("spof-{location}{bucket}-{index:02}", "spof-mocloc1-01.search.yandex.net", 101),
            ("spof-{location}{bucket}-{index:02d}", "spof-mocloc0-01.search.yandex.net", 1),
            ("spof-{location}{bucket}-{index:02d}", "spof-mocloc1-01.search.yandex.net", 101),
            ("spof-{location}{bucket}-{index:02d}", "spof-mocloc1-10.search.yandex.net", 110),
        ],
    )
    def test_index_extraction_with_bucket(self, template, fqdn, expected_index):
        index = self._mk_template(template).get_index(fqdn)
        assert expected_index == index

    @pytest.mark.parametrize(
        ["template", "fqdn"],
        [
            # index length does not match
            ("spof-{location}{bucket}-{index:01}", "spof-mocloc0-11.search.yandex.net"),
            # no bucket in fqdn
            ("spof-{location}{bucket}-{index:01}", "spof-mocloc-1.search.yandex.net"),
            # no bucket in template
            ("spof-{location}-{index:01}", "spof-mocloc0-1.search.yandex.net"),
            # location does not match
            ("spof-{location}-{index:01}", "spof-sas-1.search.yandex.net"),
            # prefix does not match
            ("{location}-{index:01}", "spof-mocloc-1.search.yandex.net"),
        ],
    )
    def test_index_extraction_for_not_matching_fqdn_is_logical_error(self, template, fqdn):
        with pytest.raises(LogicalError):
            self._mk_template(template).get_index(fqdn)


_MOCK_FB_VLANS_MAP = {
    (NATIVE_VLAN, "MAN"): [721],
    (NATIVE_VLAN, None): [722],
}


@pytest.mark.parametrize("vlan_scheme", walle_constants.VLAN_SCHEMES)
@pytest.mark.parametrize("create_location", [True, False])
@patch("walle.clients.network.racktables_client.get_fb_vlans_map", return_value=_MOCK_FB_VLANS_MAP)
@patch("walle.util.net.get_host_ips", return_value=(None, []))
def test_get_host_expected_vlans(mock_search_host_ips, mock_vlans_map, test, vlan_scheme, create_location):
    native_vlan = walle_constants.MTN_NATIVE_VLAN if vlan_scheme in walle_constants.MTN_VLAN_SCHEMES else NATIVE_VLAN

    project = test.mock_project(
        {
            "id": "project-mock",
            "vlan_scheme": vlan_scheme,
            "native_vlan": native_vlan,
            "hbf_project_id": int("0x1388", 16),
        }
    )

    host_kwargs = {"location": mock_location(country="FI", city="MANTSALA")} if create_location else {}
    host_kwargs.update(project=project.id)
    host = test.mock_host(host_kwargs)

    expected_vlans_map = {
        walle_constants.VLAN_SCHEME_STATIC: [],
        walle_constants.VLAN_SCHEME_SEARCH: [721] if create_location else [722],
        walle_constants.VLAN_SCHEME_MTN: [walle_constants.MTN_FASTBONE_VLAN] + walle_constants.MTN_EXTRA_VLANS,
        walle_constants.VLAN_SCHEME_MTN_HOSTID: [walle_constants.MTN_FASTBONE_VLAN] + walle_constants.MTN_EXTRA_VLANS,
        walle_constants.VLAN_SCHEME_CLOUD: [],
        walle_constants.VLAN_SCHEME_MTN_WITHOUT_FASTBONE: walle_constants.MTN_EXTRA_VLANS,
    }

    native_vlan = project.native_vlan
    expected_vlans = sorted([native_vlan] + expected_vlans_map[vlan_scheme])

    vlan_config = network._VlanConfig(
        expected_vlans, native_vlan, "0x1388" if vlan_scheme in walle_constants.MTN_VLAN_SCHEMES else None
    )
    assert vlan_config == network.get_host_expected_vlans(host)


# Test host IP-address generation.


@patch("walle.network._get_host_ipv4", return_value=None)
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_static_no_ipv4_no_hbf_project_id(vlans, get_network, mock_static_host):
    records = network.get_host_dns_records(
        mock_static_host, mock_static_host.get_project(), mock_static_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [("AAAA", mock_static_host.name, ["2a02:6b8::20b:34ff:fecf:1"])]


@patch("walle.network._get_fb_vlan_for_search_scheme", return_value=(None, 762))
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_search_no_ipv4_no_hbf_project_id(vlans, get_network, mock_search_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_search_host.name)
    records = network.get_host_dns_records(
        mock_search_host, mock_search_host.get_project(), mock_search_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        ("AAAA", name, ["2a02:6b8::20b:34ff:fecf:1"]),
        ("AAAA", fb_name, ["2a02:6b8::20b:34ff:fecf:1"]),
    ]


@patch("walle.network._get_validated_fb_vlan", return_value=762)
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_mtn(vlans, get_network, mock_mtn_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_mtn_host.name)
    records = network.get_host_dns_records(
        mock_mtn_host, mock_mtn_host.get_project(), mock_mtn_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        ("AAAA", name, ["2a02:6b8::686:34cf:1"]),
        ("AAAA", fb_name, ["2a02:6b8::686:34cf:1"]),
    ]


@patch("walle.network._get_validated_fb_vlan", return_value=762)
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_mtn_hostid(vlans, get_network, mock_mtn_hostid_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_mtn_hostid_host.name)
    records = network.get_host_dns_records(
        mock_mtn_hostid_host,
        mock_mtn_hostid_host.get_project(),
        mock_mtn_hostid_host.name,
        "someswitch",
        "00:0b:34:cf:00:01",
    )

    # we have calculated ip address for this hostname. if you change hostname, change ip address too.
    # $ md5 -s 'localhost.walle.fake.yandex.net'
    # > MD5 ("localhost.walle.fake.yandex.net") = 16b52af5ec89ffbb430e84b9568dd1f8

    assert mock_mtn_hostid_host.name == "localhost.walle.fake.yandex.net"
    assert records == [
        ("AAAA", name, ["2a02:6b8::686:16b5:2af5"]),
        ("AAAA", fb_name, ["2a02:6b8::686:16b5:2af5"]),
    ]


@patch("walle.network._get_host_ipv4", return_value=None)
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64", "2a02:6b9::/64"])
def test_generate_dns_records_static_no_ipv4_multiple_networks(vlans, get_network, mock_static_host):
    records = network.get_host_dns_records(
        mock_static_host, mock_static_host.get_project(), mock_static_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [("AAAA", mock_static_host.name, ["2a02:6b8::20b:34ff:fecf:1", "2a02:6b9::20b:34ff:fecf:1"])]


@patch("walle.network._get_fb_vlan_for_search_scheme", return_value=(None, 762))
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64", "2a02:6b9::/64"])
def test_generate_dns_records_search_no_ipv4_multiple_networks(vlans, get_network, mock_search_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_search_host.name)
    records = network.get_host_dns_records(
        mock_search_host, mock_search_host.get_project(), mock_search_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        ("AAAA", name, ["2a02:6b8::20b:34ff:fecf:1", "2a02:6b9::20b:34ff:fecf:1"]),
        ("AAAA", fb_name, ["2a02:6b8::20b:34ff:fecf:1", "2a02:6b9::20b:34ff:fecf:1"]),
    ]


@patch("walle.network._get_validated_fb_vlan", return_value=762)
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64", "2a02:6b9::/64"])
def test_generate_dns_records_mtn_multiple_networks(vlans, get_network, mock_mtn_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_mtn_host.name)
    records = network.get_host_dns_records(
        mock_mtn_host, mock_mtn_host.get_project(), mock_mtn_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        ("AAAA", name, ["2a02:6b8::686:34cf:1", "2a02:6b9::686:34cf:1"]),
        ("AAAA", fb_name, ["2a02:6b8::686:34cf:1", "2a02:6b9::686:34cf:1"]),
    ]


@patch("walle.network._get_validated_fb_vlan", return_value=762)
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64", "2a02:6b9::/64"])
def test_generate_dns_records_mtn_hostid_multiple_networks(vlans, get_network, mock_mtn_hostid_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_mtn_hostid_host.name)
    records = network.get_host_dns_records(
        mock_mtn_hostid_host,
        mock_mtn_hostid_host.get_project(),
        mock_mtn_hostid_host.name,
        "someswitch",
        "00:0b:34:cf:00:01",
    )

    # we have calculated ip address for this hostname. if you change hostname, change ip address too.
    # $ md5 -s 'localhost.walle.fake.yandex.net'
    # > MD5 ("localhost.walle.fake.yandex.net") = 16b52af5ec89ffbb430e84b9568dd1f8
    assert mock_mtn_hostid_host.name == "localhost.walle.fake.yandex.net"
    assert records == [
        ("AAAA", name, ["2a02:6b8::686:16b5:2af5", "2a02:6b9::686:16b5:2af5"]),
        ("AAAA", fb_name, ["2a02:6b8::686:16b5:2af5", "2a02:6b9::686:16b5:2af5"]),
    ]


@patch("walle.network._get_host_ipv4", return_value="178.154.132.5")
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_static_with_ipv4(vlans, get_network, mock_static_host):
    records = network.get_host_dns_records(
        mock_static_host, mock_static_host.get_project(), mock_static_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        ("A", mock_static_host.name, ["178.154.132.5"]),
        ("AAAA", mock_static_host.name, ["2a02:6b8:0:160b::b29a:8405", "2a02:6b8::20b:34ff:fecf:1"]),
    ]


@patch("walle.network._get_fb_vlan_for_search_scheme", return_value=("178.154.132.5", 761))
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_search_with_ipv4(vlans, get_network, mock_search_host):
    # There is some tricky logic here.
    # If the records not found, we create only one record for the host.
    # For backbone we create a record with ipv4-based IPv6 address if IPv4 is present, EUI-64 otherwise.
    # For fastbone we create a record with EUI-64 IPv6 address.
    # But to allow old hosts keeping their existing IP-addresses and DNS records
    # we return all IP-address types here, with our preferred IP-address first.
    # Then DNS-fixer should use the first IP-address from the list to create a new DNS record.

    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_search_host.name)
    records = network.get_host_dns_records(
        mock_search_host, mock_search_host.get_project(), mock_search_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        DnsRecord(type="A", name=name, value=["178.154.132.5"]),
        DnsRecord(type="AAAA", name=name, value=["2a02:6b8:0:160b::b29a:8405", "2a02:6b8::20b:34ff:fecf:1"]),
        DnsRecord(type="AAAA", name=fb_name, value=['2a02:6b8::20b:34ff:fecf:1']),
    ]


@patch("walle.network._get_fb_vlan_for_search_scheme", return_value=("178.154.223.136", 761))
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_search_with_ipv4_plus_ra(vlans, get_network, mock_search_host):
    # There is some tricky logic here.
    # If the records not found, we create only one record for the host.
    # For backbone we create a record with ipv4-based IPv6 address if IPv4 is present, EUI-64 otherwise.
    # For fastbone we create a record with EUI-64 IPv6 address.
    # But to allow old hosts keeping their existing IP-addresses and DNS records
    # we return all IP-address types here, with our preferred IP-address first.
    # Then DNS-fixer should use the first IP-address from the list to create a new DNS record.

    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_search_host.name)
    records = network.get_host_dns_records(
        mock_search_host, mock_search_host.get_project(), mock_search_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        ("A", name, ["178.154.223.136"]),
        ("AAAA", name, ["2a02:6b8:0:c28::b29a:df88", "2a02:6b8::20b:34ff:fecf:1"]),
        ("AAAA", fb_name, ["2a02:6b8::20b:34ff:fecf:1"]),
    ]


@patch("walle.network._get_host_ipv4", return_value="178.154.132.5")
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64", "2a02:6b9::/64"])
def test_generate_dns_records_static_with_ipv4_multiple_networks(vlans, get_network, mock_static_host):
    records = network.get_host_dns_records(
        mock_static_host, mock_static_host.get_project(), mock_static_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        ("A", mock_static_host.name, ["178.154.132.5"]),
        (
            "AAAA",
            mock_static_host.name,
            ["2a02:6b8:0:160b::b29a:8405", "2a02:6b8::20b:34ff:fecf:1", "2a02:6b9::20b:34ff:fecf:1"],
        ),
    ]


@patch("walle.network._get_fb_vlan_for_search_scheme", return_value=("178.154.132.5", 761))
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64", "2a02:6b9::/64"])
def test_generate_dns_records_search_with_ipv4_multiple_networks(vlans, get_network, mock_search_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_search_host.name)
    records = network.get_host_dns_records(
        mock_search_host, mock_search_host.get_project(), mock_search_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        DnsRecord(type="A", name=name, value=["178.154.132.5"]),
        DnsRecord(
            type="AAAA",
            name=name,
            value=["2a02:6b8:0:160b::b29a:8405", "2a02:6b8::20b:34ff:fecf:1", "2a02:6b9::20b:34ff:fecf:1"],
        ),
        DnsRecord(type="AAAA", name=fb_name, value=["2a02:6b8::20b:34ff:fecf:1", "2a02:6b9::20b:34ff:fecf:1"]),
    ]


@patch("walle.network._get_host_ipv4", return_value="178.154.132.5")
@patch("walle.network._get_validated_fb_vlan", return_value=762)
@patch("walle.network.racktables.is_nat64_network", return_value=True)
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_mtn_with_ipv4(ipv4, vlans, get_network, is_nat64_network, mock_mtn_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_mtn_host.name)
    records = network.get_host_dns_records(
        mock_mtn_host, mock_mtn_host.get_project(), mock_mtn_host.name, "someswitch", "00:0b:34:cf:00:01"
    )
    assert records == [
        ("A", name, ["178.154.132.5"]),
        ("AAAA", name, ["2a02:6b8::686:34cf:1"]),
        ("AAAA", fb_name, ["2a02:6b8::686:34cf:1"]),
    ]


@patch("walle.network._get_host_ipv4", return_value="178.154.132.5")
@patch("walle.network._get_validated_fb_vlan", return_value=762)
@patch("walle.network.racktables.is_nat64_network", return_value=True)
@patch("walle.network.racktables.get_vlan_networks", return_value=["2a02:6b8::/64"])
def test_generate_dns_records_mtn_hostid_with_ipv4(ipv4, vlans, get_network, is_nat64_network, mock_mtn_hostid_host):
    name, fb_name = network._get_host_fqdns_for_fastbone_vlan_scheme(mock_mtn_hostid_host.name)
    records = network.get_host_dns_records(
        mock_mtn_hostid_host, mock_mtn_hostid_host.get_project(), name, "someswitch", "00:0b:34:cf:00:01"
    )

    # we calculated ip address for this hostname. if you change hostname, change ip address too.
    # $ md5 -s 'localhost.walle.fake.yandex.net'
    # > MD5 ("localhost.walle.fake.yandex.net") = 16b52af5ec89ffbb430e84b9568dd1f8

    assert mock_mtn_hostid_host.name == "localhost.walle.fake.yandex.net"
    assert records == [
        ("A", name, ["178.154.132.5"]),
        ("AAAA", name, ["2a02:6b8::686:16b5:2af5"]),
        ("AAAA", fb_name, ["2a02:6b8::686:16b5:2af5"]),
    ]
