# coding: utf-8
from __future__ import print_function

import json
import socket

import pytest

from _netmon import (
    get_metrics,
    merge_histogram,
    Address,
    DnsCache,
    Histogram
)

from agent import utils


def test_get_metrics():
    json.loads(get_metrics())


def test_round_by_interval():
    assert utils.round_by_interval(60, 719) == 720
    assert utils.round_by_interval(60, 720) == 720 + 60
    assert utils.round_by_interval(300, 900, "hostname") >= 900 + 300

    first_ts = utils.round_by_interval(300, 900, "hostname")
    second_ts = utils.round_by_interval(300, first_ts, "hostname")
    assert second_ts - first_ts == 300, "interval between timestamps should be stable"


def test_is_address_excluded():
    assert utils.is_address_excluded(socket.AF_INET6, "fe80::1%lo")
    assert utils.is_address_excluded(socket.AF_INET6, "::1")
    assert utils.is_address_excluded(socket.AF_INET, "127.0.0.1")
    assert not utils.is_address_excluded(socket.AF_INET, "192.168.0.1")


def test_dns_cache():
    addr = Address(socket.AF_INET6, "::1", 0)
    cache = DnsCache()

    assert cache.get("localhost", 0, socket.AF_INET6) is None

    cache.set("localhost", 0, socket.AF_INET6, [addr])
    result = [(socket.AF_INET6, ("::1", 0, 0, 0))]
    assert cache.get("localhost", 0, socket.AF_INET6) == result

    cache.cleanup()
    assert cache.get("localhost", 0, socket.AF_INET6) == result

    cache.set("localhost", 0, socket.AF_INET6, [Address(socket.AF_INET6, "2a02:6b8:0:81f::1:2f", 0)])
    assert cache.get("localhost", 0, socket.AF_INET6) == result


def test_histogram():
    h1 = Histogram(10**6, 2)
    h2 = Histogram(10**6, 2)
    assert not h1.get_total_count() and not h2.get_total_count()

    h1.record_value(10000)
    h2.record_value(1000)

    merge_histogram(h1, h2)
    assert h1.get_total_count() == 2
    assert h2.get_total_count() == 1
    assert h1.get_count_at_value(10000) == 1
    assert h1.get_count_at_value(1000) == 1
    assert h2.get_count_at_value(10000) == 0
    assert h2.get_count_at_value(1000) == 1


def test_quanted_timestamp():
    assert utils.quanted_timestamp(5 * utils.US, ts=523 * utils.US) == 520 * utils.US


@pytest.mark.parametrize("addrs,ipv4,ipv6,expected",
                         [[(), "10.0.0.1", "2a02:6b8::1", [(socket.AF_INET, "10.0.0.1"), (socket.AF_INET6, "2a02:6b8::1")]],
                          [(), None, None, []],
                          [((socket.AF_INET, "10.0.0.2"), ), "10.0.0.1", None, [(socket.AF_INET, "10.0.0.2")]],
                          [((socket.AF_INET, "10.0.0.2"), ), "10.0.0.1", "2a02:6b8::1", [(socket.AF_INET, "10.0.0.2"), (socket.AF_INET6, "2a02:6b8::1")]],
                          [((socket.AF_INET, "10.0.0.2"), (socket.AF_INET6, "2a02:6b8::2")), "10.0.0.1", "2a02:6b8::1",
                           [(socket.AF_INET, "10.0.0.2"), (socket.AF_INET6, "2a02:6b8::2")]],
                          ])
def test_coalesce_addresses(addrs, ipv4, ipv6, expected):
    assert utils.coalesce_addresses(addrs, ipv4, ipv6) == expected
