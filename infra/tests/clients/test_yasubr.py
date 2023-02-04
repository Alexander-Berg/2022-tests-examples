"""Tests ya.subr client."""

import pytest

from walle.clients import yasubr
from walle.errors import YaSubrRecordNotFoundError


def test_get_host_vlans():
    assert yasubr.get_host_vlans("178.154.147.51") == (604, 761)

    with pytest.raises(YaSubrRecordNotFoundError):
        yasubr.get_host_vlans("93.158.183.177")


def test_get_host_network():
    assert yasubr.get_host_network("178.154.133.183", 604) == "2a02:6b8:0:160b::/64"
    assert yasubr.get_host_network("178.154.133.183", 761) is None
    assert yasubr.get_host_network("178.154.223.136", 604) == "2a02:6b8:0:c28::/64"
    assert yasubr.get_host_network("178.154.223.136", 761) is None

    with pytest.raises(YaSubrRecordNotFoundError):
        yasubr.get_host_network("95.108.220.148", 762)
