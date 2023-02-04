# coding: utf-8

import freezegun
import pytest


@pytest.fixture()
def cache_storage():
    from intranet.webauth.lib.cache_storage import CacheClient

    return CacheClient()


def test_not_expired_yet(cache_storage):
    with freezegun.freeze_time('2001-01-01 12:00:00'):
        cache_storage.set('key', 'value', 3600)
    with freezegun.freeze_time('2001-01-01 12:59:59'):
        assert cache_storage.get('key') == 'value'


def test_expired(cache_storage):
    with freezegun.freeze_time('2001-01-01 12:00:00'):
        cache_storage.set('key', 'value', 3600)
    with freezegun.freeze_time('2001-01-01 13:00:01'):
        assert cache_storage.get('key') is None


def test_never_expire(cache_storage):
    with freezegun.freeze_time('2001-01-01 12:00:00'):
        cache_storage.set('key', 'value')
    with freezegun.freeze_time('2100-01-02 12:00:01'):
        assert cache_storage.get('key') == 'value'
