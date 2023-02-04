"""Tests network utils."""

from ipaddress import ip_address

from walle.util import net
from walle.util.net import mac_to_int, mac_from_int, ip_to_int, is_local_ip


def test_mac_convertion():
    assert mac_from_int(mac_to_int("90:2B:34.cf:34:18")) == "90:2b:34:cf:34:18"


def test_ip_to_int():
    assert ip_to_int("2a02:6b8:0:c19::b29a:dc59") == int(ip_address("2a02:6b8:0:c19::b29a:dc59"))
    assert ip_to_int("222.1.41.90") == int(ip_address("::ffff:222.1.41.90"))


def test_is_local_ip():
    assert is_local_ip(ip_to_int("10.1.1.1"))
    assert is_local_ip(ip_to_int("169.254.1.1"))
    assert is_local_ip(ip_to_int("172.16.1.1"))
    assert is_local_ip(ip_to_int("192.168.1.1"))

    assert not is_local_ip(ip_to_int("178.154.220.89"))
    assert not is_local_ip(ip_to_int("2a02:6b8:0:c19::b29a:dc59"))


def test_get_eui_64_address():
    result_addr = net.get_eui_64_address("2a02:6b8:b060:16b::/64", "e4:1d:2d:00:83:f0")
    assert result_addr == "2a02:6b8:b060:16b:e61d:2dff:fe00:83f0"


def test_get_hbf_project_ipv6_address_with_mac_hostid():
    produced_ip_address = net.get_hbf_ipv6_address_with_mac_hostid(
        "2a02:6b8:c04::/64", int("696", 16), "01:23:7d:4b:4d:b8"
    )

    assert produced_ip_address == "2a02:6b8:c04::696:7d4b:4db8"


def test_get_hbf_project_ipv6_address_with_hostname_hostid():
    project_id_str = "696"
    produced_ip_address = net.get_hbf_ipv6_address_with_hostname_hostid(
        "2a02:6b8:c04::/64", int(project_id_str, 16), "walle.fake.yandex.net"
    )

    # MD5 ("walle.fake.yandex.net") = 09fad0a8d0dd7c86429f3e39a19fca16
    hostname_hash = "09fad0a8d0dd7c86429f3e39a19fca16"
    expected_ip_address = "2a02:6b8:c04::{}:{}:{}".format(
        project_id_str, hostname_hash[:4].lstrip("0"), hostname_hash[4:8].lstrip("0")
    )
    assert produced_ip_address == expected_ip_address


def test_get_ipv4_embedded_ipv6_address():
    assert net.get_ipv4_embedded_ipv6_address("2a02:6b8:b060:16b::/64", "222.1.41.90") == "2a02:6b8:b060:16b::de01:295a"
    assert net.get_ipv4_embedded_ipv6_address("2a02:6b8::/64", "222.11.41.90") == "2a02:6b8::de0b:295a"


def test_split_eui_64_address():
    result = net.split_eui_64_address("2a02:6b8:b060:16b:e61d:2dff:fe00:83f0")
    assert result == ("2a02:6b8:b060:16b::/64", "e4:1d:2d:00:83:f0")


def test_split_hbf_ipv6_address():
    result = net.split_hbf_ipv6_address("2a02:6b8:c04::696:7d4b:4db8")
    assert result == ("2a02:6b8:c04::/64", int("696", 16), "00:00:7d:4b:4d:b8")
