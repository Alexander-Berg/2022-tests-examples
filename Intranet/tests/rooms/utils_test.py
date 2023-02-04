from intranet.vconf.src.rooms.utils import is_ipv6_address


def test_is_ipv6_address():
    assert is_ipv6_address('1:2:3:4:5:6:7:8')
    assert is_ipv6_address('ff::1:2:3:4')

    assert not is_ipv6_address('1:2:3:4')
    assert not is_ipv6_address('127.192.0.13')
