# coding: utf-8
import time
import collections
from datetime import datetime, timedelta

import inject
import mock
import pytest

import awacs.lib.staffclient
from awacs.lib import zookeeper_client
from awacs.model import staffcache
from awtest import freeze_time


@pytest.fixture(autouse=True)
def deps(zk):
    def configure(binder):
        binder.bind(zookeeper_client.IZookeeperClient, zk)

    inject.clear_and_configure(configure)
    yield zk
    inject.clear()


def test_cache():
    cache = staffcache.StaffCache('/cache/staff/')

    bucket = 'test_cache'

    key_1 = '123'
    value_1 = ['romanovich', 'nekto0n']
    key_2 = '456'
    value_2 = ['imperator']

    # miss cache
    content, expired = cache.get(bucket, key_1)
    assert content is None
    assert expired

    # set a value in cache
    cache.set(bucket, key_1, value_1)

    # make sure it has been set
    content, expired = cache.get(bucket, key_1)
    assert content == value_1
    assert not expired

    # remove the value from zookeeper cache
    cache.zk_storage_clients[bucket].remove(key_1)

    # expect it to be still served from memory cache
    assert key_1 in cache.mem_caches[bucket]
    content, expired = cache.get(bucket, key_1)
    assert content == value_1
    assert not expired

    # set another value
    cache.set(bucket, key_2, value_2)

    # remove it from memory cache
    del cache.mem_caches[bucket][key_2]

    # expect it to be still served from zookeeper cache
    content, expired = cache.get(bucket, key_2)
    assert content == value_2
    assert not expired

    # memory should be updated after serving from zookeeper
    assert key_2 in cache.mem_caches[bucket]


class DummyCache(object):
    TTL = 600

    def __init__(self):
        self.cache = collections.defaultdict(dict)

    def set(self, bucket, key, value):
        self.cache[bucket][key] = (value, time.time())

    def get(self, bucket, key):
        content, expired = None, True
        if key in self.cache[bucket]:
            content, updated_at = self.cache[bucket][key]
            expired = updated_at + self.TTL < time.time()
        return content, expired


def test_staff_client():
    cache = DummyCache()
    client = awacs.lib.staffclient.StaffClient(staff_client=mock.Mock(), cache=cache)

    def _do_resolve_login_to_group_ids(login):
        return ['482', '123']

    with mock.patch.object(client, '_do_resolve_login_to_group_ids',
                           side_effect=_do_resolve_login_to_group_ids) as m:
        res = client.resolve_login_to_group_ids('romanovich')
        assert res == ['482', '123']

        m.assert_called_once_with('romanovich')
        m.reset_mock()

        res = client.resolve_login_to_group_ids('romanovich')
        assert res == ['482', '123']
        m.assert_not_called()

        future = datetime.utcnow() + timedelta(seconds=cache.TTL + 1)
        with freeze_time(future):
            res = client.resolve_login_to_group_ids('romanovich')
            assert res == ['482', '123']
            m.assert_called_once_with('romanovich')

    exception = awacs.lib.staffclient.StaffClient._USE_EXPIRED_CACHE_ERRORS[0]
    with mock.patch.object(client, '_do_resolve_login_to_group_ids',
                           side_effect=exception) as m:
        with pytest.raises(exception):
            client.resolve_login_to_group_ids('romanovich', use_cache=False)

        res = client.resolve_login_to_group_ids('romanovich')
        assert res == ['482', '123']

        future += timedelta(seconds=cache.TTL + 1)
        with freeze_time(future):
            with pytest.raises(exception):
                client.resolve_login_to_group_ids('romanovich')

            res = client.resolve_login_to_group_ids('romanovich', use_expired_cache=True)
            assert res == ['482', '123']


@pytest.mark.vcr
def test_get_staff_group_members():
    cache = DummyCache()
    client = awacs.lib.staffclient.StaffClient.from_config(
        {
            'api_url': 'https://staff-api.yandex-team.ru/v3/',
            # NOTE set your own oauth token if you're rebuilding vcr cassettes
            'req_timeout': 5,
            'verify_ssl': False,
        },
        cache=cache,
    )

    group_id = 75060  # svc_rclb in staff (not matches abc service id)

    # NOTE if rebuilding casette, you'd probably have to fix assertion: group contents may change over time
    assert list(sorted(client.get_group_members(group_id))) == [
        u'alonger',
        u'disafonov',
        u'dmitriyt',
        u'ferenets',
        u'i-dyachkov',
        u'idtv',
        u'max7255',
        u'pirogov',
        u'reddi',
        u'romanovich',
        u'torkve',
    ], "Not all users in list, or list contains robots"
