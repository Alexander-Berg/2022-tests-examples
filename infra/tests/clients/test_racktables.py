"""Tests RackTables client."""

from ipaddress import IPv4Address
from unittest.mock import patch

import pytest
import six
from requests import Response

import walle.clients.network.racktables_client
import walle.constants as walle_constants
from infra.walle.server.tests.lib.util import load_mock_data
from infra.walle.server.tests.lib.util import monkeypatch_config, monkeypatch_method
from walle.clients import racktables
from walle.clients.network.racktables_client import RacktablesClient
from walle.clients.racktables import (
    _parse_vlan_specification,
    _get_update_vlan_query,
    VlanArgs,
    _raw_request_to_cloud,
    _raw_request_to_racktables,
    map_vlan_name_to_l3_networks,
    _L3NetworkMap,
    is_l3_switch,
)
from walle.models import timestamp


def racktables_mock_response_generator(filepath):
    f = six.BytesIO(six.ensure_binary(load_mock_data(filepath)))
    result = Response()
    result.raw = f
    yield result


@pytest.fixture(scope="module")
def racktables_net_layout():
    return racktables_mock_response_generator("mocks/net-layout.xml")


@pytest.fixture(scope="module")
def racktables_l3_tors():
    return racktables_mock_response_generator("mocks/l3-tors.txt")


@pytest.fixture(scope="module")
def racktables_vm_projects():
    return racktables_mock_response_generator("mocks/vm-projects.txt")


@pytest.fixture(scope="module")
def racktables_nat64_networks_list():
    return racktables_mock_response_generator("mocks/networklist.nat64.txt")


@pytest.fixture(scope="module")
def racktables_tun64_networks_list():
    return racktables_mock_response_generator("mocks/networklist.tun64.txt")


@pytest.fixture()
def racktables_get_l123_active():
    return racktables_mock_response_generator("mocks/get-l123-active.txt")


@pytest.fixture(scope="module")
def racktables_get_l123_not_active():
    return racktables_mock_response_generator("mocks/get-l123-not-active.txt")


@pytest.fixture(scope="module")
def racktables_interconnect_switch_list():
    """Mock response for
    https://racktables.yandex.net/export/allobjects.php?text=%7Binterconnect%7D&noresolve=true"""
    return racktables_mock_response_generator("mocks/interconnect_switch_list.txt")


def mock_empty_response():
    result = Response()
    result._content = ""
    return result


@pytest.mark.parametrize(
    "encoded,parsed",
    (
        ("T", ([], None)),
        ("A999", ([999], 999)),
        ("T999", ([999], 999)),
        ("T+600", ([600], None)),
        ("T604+761", ([604, 761], 604)),
        ("T604+761,604,600", ([600, 604, 761], 604)),
        ("T+604,761,604,600", ([600, 604, 761], None)),
        ("T999+2-499,600", (list(range(2, 500)) + [600, 999], 999)),
        ("T999+604, 761", ([604, 761, 999], 999)),
    ),
)
def test_vlan_spec_parsing(encoded, parsed):
    assert _parse_vlan_specification(encoded) == parsed


@pytest.mark.parametrize(
    "encoded",
    (
        "A",
        "A+100",
        "A100+200",
        "A100,200",  # Invalid access mode specification
        "A0",
        "A4095",
        "T100,4095",  # Invalid VLAN IDs
        "T+600+900",
        "T100,200+300",  # Invalid trunk mode specification
        "T100,200-300-400,500",  # Invalid range specification
    ),
)
def test_invalid_vlan_spec_parsing(encoded):
    with pytest.raises(ValueError):
        _parse_vlan_specification(encoded)


@pytest.mark.parametrize(
    "vlans,native_vlan,encoded",
    (
        ([], None, {"mode": "trunk"}),
        ([], 999, {"mode": "access", "native": 999}),
        ([999], 999, {"mode": "access", "native": 999}),
        ([600], None, {"mode": "trunk", "allowed[]": [600]}),
        ([761, 604, 600], None, {"mode": "trunk", "allowed[]": [600, 604, 761]}),
        ([761, 604, 600], 604, {"mode": "trunk", "allowed[]": [600, 761], "native": 604}),
    ),
)
def test_vlan_spec_encoding(vlans, native_vlan, encoded):
    assert _get_update_vlan_query(vlans, native_vlan) == encoded


def test_net_layout(mp, racktables_l3_tors, racktables_net_layout):
    mp.function(
        racktables._raw_request_to_racktables, side_effect=[next(racktables_l3_tors), next(racktables_net_layout)]
    )

    monkeypatch_config(mp, "racktables.deprecated_networks", ["2a02:6b8:b001:d01::/64"])

    # l2-only datacenters have networks assigned to the whole datacenter
    # first datacenter
    for switch in ("man1-a2", "man1-a3"):
        assert racktables.get_vlan_networks(switch, 801) == ["2a02:6b8:0:eba::/64"]
        assert racktables.get_vlan_networks(switch, 803) == ["2a02:6b8:0:ebb::/64"]
        assert racktables.get_vlan_networks(switch, 1668) == ["2a02:6b8:b001:d00::/64"]

        assert racktables.get_vlan_networks(switch, 802) is None

    # second datacenter
    for switch in ("sas1-a1", "sas1-a2"):
        assert racktables.get_vlan_networks(switch, 1488) == ["2a02:6b8:b010:e::/64"]
        assert racktables.get_vlan_networks(switch, 542) == ["2a02:6b8:0:1a05::/64"]

        # another network assigned to this vlan in the second datacenter
        assert racktables.get_vlan_networks(switch, 801) == ["2a02:6b8:0:1a1a::/64"]

        # we have these vlans in exported data but there is not suitable IPv6 networks assigned to them.
        assert racktables.get_vlan_networks(switch, 85) is None
        assert racktables.get_vlan_networks(switch, 541) is None

        # this vlan is not accessible in this datacenter
        assert racktables.get_vlan_networks(switch, 803) is None

    # l3 datacenters have networks assigned to switch, not to the datacenter
    assert racktables.get_vlan_networks("sas1-s21", 761) == ["2a02:6b8:f000:57::/64"]
    assert racktables.get_vlan_networks("sas1-s25", 761) is None

    assert racktables.get_vlan_networks("sas1-s21", 762) == ["2a02:6b8:f000:157::/64"]
    assert racktables.get_vlan_networks("sas1-s26", 762) == ["2a02:6b8:f000:142::/64"]

    assert racktables.get_vlan_networks("sas1-s21", 604) == ["2a02:6b8:b000:173::/64"]
    assert racktables.get_vlan_networks("man1-s60", 604) == ["2a02:6b8:b000:6003::/64"]
    assert racktables.get_vlan_networks("man1-s61", 604) is None

    # this vlans have two network attached to them in exported data
    # l3
    assert racktables.get_vlan_networks("man1-s60", 768) == ["2a02:6b8:f000:302e::/64", "2a02:6b8:f001:302e::/64"]
    # l2
    # "actual networks go in reversed order compared to the export file.
    assert racktables.get_vlan_networks("man1-a2", 755) == ["2a02:6b8:b001:d00::/64", "2a02:6b8:a001:d00::/64"]
    # Deprecated networks go after actual networks.
    assert racktables.get_vlan_networks("man1-a2", 756) == [
        "2a02:6b8:c001:d01::/64",
        "2a02:6b8:a001:d01::/64",
        "2a02:6b8:b001:d01::/64",
    ]

    # these vlans have /57 l3 networks attached to them but we only store first /64 subnet.
    assert racktables.get_vlan_networks("iva8-s12", 688) == ["2a02:6b8:c0c:d80::/64"]
    assert racktables.get_vlan_networks("iva8-s12", 788) == ["2a02:6b8:fc08:d80::/64"]


def test_vm_projects(mp, racktables_vm_projects):
    mp.function(racktables._raw_request_to_racktables, return_value=next(racktables_vm_projects))

    # take two sample projects and ensure they are present
    assert int("410e", 16) in racktables.get_hbf_projects()
    assert racktables.get_hbf_projects().get(int("1488", 16)) == "JETSTYLENETS"


def test_nat64_networks(mp, racktables_nat64_networks_list, racktables_tun64_networks_list):
    mp.function(
        racktables._raw_request_to_racktables,
        side_effect=[next(racktables_nat64_networks_list), next(racktables_tun64_networks_list)],
    )

    # use some random IP-addresses that does or does not belong to the listed networks.
    nat64_addresses = ["77.88.57.192", "77.88.57.193", "95.108.140.71"]
    tun64_addresses = ["141.8.143.10", "95.108.178.140"]
    not_nat_tun_64_addresses = ["77.88.57.191", "5.45.235.60", "95.108.140.31"]

    # test the fetcher method directly
    networks_list = racktables._get_v4_to_v6_networks()
    for address in nat64_addresses + tun64_addresses:
        address = IPv4Address(address)
        assert any(address in network for network in networks_list)

    for address in not_nat_tun_64_addresses:
        address = IPv4Address(address)
        assert not any(address in network for network in networks_list)

    # test the convenience wrapper
    for address in nat64_addresses + tun64_addresses:
        assert racktables.is_nat64_network(address)

    for address in not_nat_tun_64_addresses:
        assert not racktables.is_nat64_network(address)


@pytest.mark.parametrize("is_interconnect", (True, False))
def test_get_mac_status_active_mac(mp, racktables_get_l123_active, is_interconnect):
    """Test snmp trap handle client."""
    racktables_response = next(racktables_get_l123_active)
    racktables_response.status_code = 200
    mp.function(racktables._raw_request_to_racktables, return_value=racktables_response)

    monkeypatch_method(
        mp, method=RacktablesClient.is_interconnect_switch, obj=RacktablesClient, return_value=is_interconnect
    )

    if is_interconnect:
        # we don't mark interconnect mac addresses as active.
        assert walle.clients.network.racktables_client.get_mac_status("90:E2:BA:74:7D:CA") is None
    else:
        mac_info = walle.clients.network.racktables_client.get_mac_status("90:E2:BA:74:7D:CA")
        assert mac_info["MAC"] == "90:E2:BA:74:7D:CA"
        assert 0 < mac_info["Port_timestamp"] < timestamp()


def test_get_mac_status_not_active_mac(mp, racktables_get_l123_not_active):
    """Test snmp trap handle client."""
    racktables_response = next(racktables_get_l123_not_active)
    racktables_response.status_code = 200
    mp.function(racktables._raw_request_to_racktables, return_value=racktables_response)
    monkeypatch_method(mp, method=RacktablesClient.is_interconnect_switch, obj=RacktablesClient, return_value=False)

    assert walle.clients.network.racktables_client.get_mac_status("90:E2:BA:74:7D:CB") is None


def test_interconnect_switch_list(database, mp, racktables_interconnect_switch_list):
    mp.function(racktables._raw_request_to_racktables, return_value=next(racktables_interconnect_switch_list))

    # take two sample switches
    assert RacktablesClient().is_interconnect_switch("leaf-1-10")
    assert not RacktablesClient().is_interconnect_switch("iva8-s12")


@pytest.mark.parametrize(
    ["vlan_scheme", "result"],
    [
        (walle_constants.VLAN_SCHEME_CLOUD, VlanArgs(_raw_request_to_cloud, "/netbox", "Cloud")),
        (walle_constants.VLAN_SCHEME_SEARCH, VlanArgs(_raw_request_to_racktables, "/export/l3-tors.txt", 'Racktables')),
    ],
)
def test_map_vlan_name_to_l3_networks(vlan_scheme, result):
    assert map_vlan_name_to_l3_networks(vlan_scheme) == result


def mock_get_l3_switch_vlan_networks_search(vlan_scheme):
    if vlan_scheme == walle_constants.VLAN_SCHEME_SEARCH:
        return _L3NetworkMap(switches=["switch"], network_map=None)
    return _L3NetworkMap(switches=[None], network_map=None)


def mock_get_l3_switch_vlan_networks_cloud(vlan_scheme):
    if vlan_scheme == walle_constants.VLAN_SCHEME_CLOUD:
        return _L3NetworkMap(switches=["switch"], network_map=None)
    return _L3NetworkMap(switches=[None], network_map=None)


def mock_get_l3_switch_vlan_networks_none(vlan_scheme):
    return _L3NetworkMap(switches=[None], network_map=None)


@pytest.mark.parametrize(
    ["vlan_scheme_with_switch", "mock_func", "result"],
    [
        (walle_constants.VLAN_SCHEME_SEARCH, mock_get_l3_switch_vlan_networks_search, True),
        (walle_constants.VLAN_SCHEME_CLOUD, mock_get_l3_switch_vlan_networks_cloud, True),
        (None, mock_get_l3_switch_vlan_networks_none, False),
    ],
)
def test_is_l3_switch(walle_test, vlan_scheme_with_switch, mock_func, result):
    with patch("walle.clients.racktables._get_l3_switch_vlan_networks", side_effect=mock_func):
        assert is_l3_switch("switch") == result
