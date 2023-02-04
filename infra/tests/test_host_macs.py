"""Tests working with host MACs info."""

import pytest

from sepelib.mongo.mock import ObjectMocker
from walle import host_macs
from walle.host_macs import HostMacs, save_macs_info
from walle.models import monkeypatch_timestamp, timestamp


@pytest.fixture
def test(database):
    return ObjectMocker(HostMacs)


def test_collection(test, monkeypatch):
    monkeypatch_timestamp(monkeypatch, 1)
    host1 = test.mock(
        dict(id="host1:a|b", name="host1", macs=["a", "b"], first_time=timestamp(), last_time=timestamp()), save=False
    )
    save_macs_info(host1.name, host1.macs)
    test.assert_equal()

    monkeypatch_timestamp(monkeypatch, 2)
    host2 = test.mock(
        dict(id="host2:c|d", name="host2", macs=["c", "d"], first_time=timestamp(), last_time=timestamp()), save=False
    )
    save_macs_info(host2.name, host2.macs)
    test.assert_equal()

    monkeypatch_timestamp(monkeypatch, 3)
    host1_dup = test.mock(
        dict(id="host1:e|f", name="host1", macs=["e", "f"], first_time=timestamp(), last_time=timestamp()), save=False
    )
    save_macs_info(host1_dup.name, host1_dup.macs)
    test.assert_equal()

    monkeypatch_timestamp(monkeypatch, 4)
    save_macs_info(host1.name, host1.macs)
    host1.last_time = timestamp()
    test.assert_equal()


def test_gc(test):
    expire_time = timestamp() - host_macs._INFO_EXPIRE_TIME

    test.mock(dict(id="host1", name="host1", macs=["a", "b"], first_time=0, last_time=expire_time), add=False)
    test.mock(dict(id="host2", name="host2", macs=["c", "d"], first_time=expire_time, last_time=expire_time), add=False)
    test.mock(dict(id="host3", name="host3", macs=["e", "f"], first_time=0, last_time=timestamp()))

    host_macs.host_macs_gc()
    test.assert_equal()
