# coding: utf-8
import functools
from datetime import datetime, timedelta

import mock
import pytest

from awacs.lib import nannyclient
from infra.awacs.proto import internals_pb2
from awacs.model.balancer.generator import instances_cmp
from awacs import resolver

from awtest import freeze_time


@pytest.fixture
def nanny_cache(mongo_connection):
    return resolver.NannyInstancesCache()


@pytest.fixture
def instances():
    return [
        internals_pb2.Instance(
            host='google.com',
            port=80,
            weight=5.5,
            ipv4_addr='74.125.232.195',
            ipv6_addr='2a00:1450:4012:801::100e',
        ),
        internals_pb2.Instance(
            host='yandex.ru',
            port=80,
            weight=5.5,
            ipv4_addr='77.88.55.55',
            ipv6_addr='2a02:6b8:a::a',
        ),
        internals_pb2.Instance(
            host='rambler.ru',
            port=80,
            weight=1.0,
            ipv4_addr='81.19.82.9'
        ),
    ]


def sorted_instances(instances):
    return sorted(instances, key=functools.cmp_to_key(instances_cmp))


def test_nanny_cache(nanny_cache, instances):
    service_id = 'production_nanny'
    snapshot_id = 'qwerty'
    use_mtn = False

    now = datetime.utcnow().replace(microsecond=0)
    with freeze_time(now):
        nanny_cache.cache_nanny_instances(
            service_id=service_id,
            snapshot_id=snapshot_id,
            use_mtn=use_mtn,
            instances=instances
        )

    cache_entry = nanny_cache.get_nanny_instances_cache_entry(service_id, snapshot_id, use_mtn)
    assert cache_entry['mtime'] == cache_entry['atime'] == now

    later = now + timedelta(days=1)
    with freeze_time(later):
        cached_instances = nanny_cache.get_nanny_instances(service_id, snapshot_id, use_mtn)
    assert instances == cached_instances

    cache_entry = nanny_cache.get_nanny_instances_cache_entry(service_id, snapshot_id, use_mtn)
    assert cache_entry['mtime'] == cache_entry['atime'] == now

    del nanny_cache.mem_cache[nanny_cache._format_id(service_id, snapshot_id, use_mtn)]
    with freeze_time(later):
        cached_instances = nanny_cache.get_nanny_instances(service_id, snapshot_id, use_mtn)
    assert instances == cached_instances

    cache_entry = nanny_cache.get_nanny_instances_cache_entry(service_id, snapshot_id, use_mtn)
    assert cache_entry['mtime'] == now
    assert cache_entry['atime'] == later

    instances[0].port = 81

    later = now + timedelta(days=1)
    with freeze_time(later):
        nanny_cache.cache_nanny_instances(
            service_id=service_id,
            snapshot_id=snapshot_id,
            instances=instances,
            use_mtn=use_mtn
        )

    cache_entry = nanny_cache.get_nanny_instances_cache_entry(service_id, snapshot_id, use_mtn)
    assert cache_entry['mtime'] == later
    assert cache_entry['atime'] == later

    cached_instances = nanny_cache.get_nanny_instances(service_id, snapshot_id, use_mtn)
    assert instances == cached_instances

    cached_instances = nanny_cache.get_nanny_instances('XXX', snapshot_id, use_mtn)
    assert cached_instances is None


def test_nanny_client_with_gencfg_groups(nanny_cache, instances):
    gencfg_client = mock.Mock()
    client = resolver.NannyClient(nanny_client=mock.Mock(),
                                  gencfg_client=gencfg_client,
                                  yp_client_factory=mock.Mock(),
                                  cache=nanny_cache)

    def _do_get_snapshot_instances_section(service_id, snapshot_id):
        return nannyclient.ServiceInstances.from_dict({

            'chosen_type': 'EXTENDED_GENCFG_GROUPS',
            'extended_gencfg_groups': {
                'groups': [
                    {'release': 'tags/stable-90-r5', 'name': 'SAS_SG_NANNY', 'tags': []},
                    {'release': 'tags/stable-90-r5', 'name': 'MSK_SG_NANNY', 'tags': []},
                ],
                'tags': [],
            },
        })

    def _list_group_instances(name, version, use_mtn):
        if name == 'MSK_SG_NANNY':
            return instances[0:1]
        elif name == 'SAS_SG_NANNY':
            return instances[1:]

    service_id, snapshot_id = 'production_nanny', 'qwerty'
    with mock.patch.object(client, '_do_get_snapshot_instances_section',
                           side_effect=_do_get_snapshot_instances_section) as m:
        for use_mtn in (False, True):
            gencfg_client.reset_mock()
            gencfg_client.list_group_instances.side_effect = _list_group_instances
            actual_instances = client.list_nanny_snapshot_instances(service_id, snapshot_id, use_mtn)
            assert sorted_instances(actual_instances) == sorted_instances(instances)

            m.assert_called_once_with(service_id, snapshot_id)
            m.reset_mock()

            actual_instances = client.list_nanny_snapshot_instances(service_id, snapshot_id, use_mtn)
            assert sorted_instances(actual_instances) == sorted_instances(instances)
            m.assert_not_called()

            assert gencfg_client.list_group_instances.mock_calls == [
                mock.call('SAS_SG_NANNY', 'tags/stable-90-r5', use_mtn),
                mock.call('MSK_SG_NANNY', 'tags/stable-90-r5', use_mtn),
            ]


def test_nanny_client_with_instances(nanny_cache, instances):
    client = resolver.NannyClient(nanny_client=mock.Mock(),
                                  gencfg_client=mock.Mock(),
                                  yp_client_factory=mock.Mock(),
                                  cache=nanny_cache)
    use_mtn = False

    def _do_get_snapshot_instances_section(service_id, snapshot_id):
        return nannyclient.ServiceInstances.from_dict({
            'chosen_type': 'INSTANCE_LIST',
            'instance_list': [{'host': i.host,
                               'port': i.port,
                               'weight': i.weight,
                               'ipv4_address': i.ipv4_addr,
                               'ipv6_address': i.ipv6_addr} for i in instances],
        })

    with mock.patch.object(client, '_do_get_snapshot_instances_section',
                           side_effect=_do_get_snapshot_instances_section) as m:
        service_id, snapshot_id = 'production_nanny', 'qwerty'
        actual_instances = client.list_nanny_snapshot_instances(service_id, snapshot_id, use_mtn)
        assert sorted_instances(actual_instances) == sorted_instances(instances)

        m.assert_called_once_with(service_id, snapshot_id)
        m.reset_mock()

        actual_instances = client.list_nanny_snapshot_instances(service_id, snapshot_id, use_mtn)
        assert sorted_instances(actual_instances) == sorted_instances(instances)
        m.assert_not_called()
