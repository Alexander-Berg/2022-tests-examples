# coding: utf-8
from datetime import datetime, timedelta

import mock
import pytest

from infra.awacs.proto import internals_pb2
from awacs import resolver

from awtest import freeze_time


@pytest.fixture
def gencfg_cache(mongo_connection):
    return resolver.GencfgGroupInstancesCache()


def test_gencfg_cache(gencfg_cache):
    instances = [
        internals_pb2.Instance(
            host='yandex.ru',
            port=80,
            weight=5.5,
            ipv4_addr='77.88.55.55',
            ipv6_addr='2a02:6b8:a::a',
        ),
        internals_pb2.Instance(
            host='google.com',
            port=80,
            weight=5.5,
            ipv4_addr='74.125.232.195',
            ipv6_addr='2a00:1450:4012:801::100e',
        ),
    ]
    name = 'SEARCH'
    version = 'tags/stable-94-r139'
    use_mtn = False

    assert gencfg_cache._format_id(name, version, False) != gencfg_cache._format_id(name, version, True)

    now = datetime.utcnow().replace(microsecond=0)
    with freeze_time(now):
        gencfg_cache.cache_gencfg_group_instances(
            name=name,
            version=version,
            use_mtn=use_mtn,
            instances=instances
        )

    cache_entry = gencfg_cache.get_gencfg_group_instances_cache_entry(name, version, use_mtn)
    assert cache_entry['mtime'] == cache_entry['atime'] == now

    later = now + timedelta(days=1)
    with freeze_time(later):
        cached_instances = gencfg_cache.get_gencfg_group_instances(name, version, use_mtn)
    assert instances == cached_instances

    cache_entry = gencfg_cache.get_gencfg_group_instances_cache_entry(name, version, use_mtn)
    assert cache_entry['mtime'] == cache_entry['atime'] == now

    del gencfg_cache.mem_cache[gencfg_cache._format_id(name, version, use_mtn)]
    with freeze_time(later):
        cached_instances = gencfg_cache.get_gencfg_group_instances(name, version, use_mtn)
    assert instances == cached_instances

    cache_entry = gencfg_cache.get_gencfg_group_instances_cache_entry(name, version, use_mtn)
    assert cache_entry['mtime'] == now
    assert cache_entry['atime'] == later

    instances[0].port = 81

    later = now + timedelta(days=1)
    with freeze_time(later):
        gencfg_cache.cache_gencfg_group_instances(
            name=name,
            version=version,
            use_mtn=use_mtn,
            instances=instances
        )

    cache_entry = gencfg_cache.get_gencfg_group_instances_cache_entry(name, version, use_mtn)
    assert cache_entry['mtime'] == later
    assert cache_entry['atime'] == later

    cached_instances = gencfg_cache.get_gencfg_group_instances(name, version, use_mtn)
    assert instances == cached_instances

    cached_instances = gencfg_cache.get_gencfg_group_instances('XXX', version, use_mtn)
    assert cached_instances is None


def test_gencfg_client(gencfg_cache):
    client = resolver.GencfgClient(gencfg_client=mock.Mock(), cache=gencfg_cache)
    use_mtn = False

    def do_list_group_instances_data(name, version):
        return [
            {
                'dc': 'ugrb',
                'domain': '.search.yandex.net',
                'hostname': 'sg9-00.search.yandex.net',
                'location': 'msk',
                'port': 8082,
                'power': 341.5,
                'hbf': {
                    'hbf_project_id': '10b5fc6',
                    'interfaces': {
                        'backbone': {
                            'hostname': 'bb-man1-0888-MAN_PDB_VERTICAL__401-29030.gencfg-c.yandex.net',
                            'ipv6addr': '2a02:6b8:c0b:ef8:10b:5fc6:0:7166'
                        },
                        'fastbone': {
                            'hostname': 'fb-man1-0888-MAN_PDB_VERTICAL__401-29030.gencfg-c.yandex.net',
                            'ipv6addr': '2a02:6b8:fc13:ef8:10b:5fc6:0:7166'
                        }
                    },
                    'mtn_ready': True,
                },
            },
            {
                'dc': 'ugrb',
                'domain': '.search.yandex.net',
                'hostname': 'sg8-00.search.yandex.net',
                'location': 'msk',
                'port': 8083,
                'power': 300,
                'hbf': {
                    'hbf_project_id': '10b5fc6',
                    'interfaces': {
                        'backbone': {
                            'hostname': 'bb-man1-4868-MAN_PDB_VERTICAL__401-29030.gencfg-c.yandex.net',
                            'ipv6addr': '2a02:6b8:c0b:28c:10b:5fc6:0:7166'
                        },
                        'fastbone': {
                            'hostname': 'fb-man1-4868-MAN_PDB_VERTICAL__401-29030.gencfg-c.yandex.net',
                            'ipv6addr': '2a02:6b8:fc13:28c:10b:5fc6:0:7166'
                        }
                    },
                    'mtn_ready': True,
                }
            }
        ]

    name, version = 'MSK_SG_NANNY', 'tags/stable-93-r190'
    with mock.patch.object(client, 'do_list_group_instances_data',
                           side_effect=do_list_group_instances_data) as m:
        instances = client.list_group_instances(name, version, use_mtn)
        expected_instances = [
            internals_pb2.Instance(
                host='sg9-00.search.yandex.net',
                port=8082,
                weight=341.5
            ),
            internals_pb2.Instance(
                host='sg8-00.search.yandex.net',
                port=8083,
                weight=300
            ),
        ]
        assert instances == expected_instances

        m.assert_called_once_with(name, version)
        m.reset_mock()

        instances = client.list_group_instances(name, version, use_mtn)
        assert instances == expected_instances
        m.assert_not_called()

    use_mtn = True

    with mock.patch.object(client, 'do_list_group_instances_data',
                           side_effect=do_list_group_instances_data) as m:
        instances = client.list_group_instances(name, version, use_mtn)
        expected_instances = [
            internals_pb2.Instance(
                host='bb-man1-0888-MAN_PDB_VERTICAL__401-29030.gencfg-c.yandex.net',
                port=8082,
                ipv6_addr='2a02:6b8:c0b:ef8:10b:5fc6:0:7166',
                weight=341.5
            ),
            internals_pb2.Instance(
                host='bb-man1-4868-MAN_PDB_VERTICAL__401-29030.gencfg-c.yandex.net',
                port=8083,
                ipv6_addr='2a02:6b8:c0b:28c:10b:5fc6:0:7166',
                weight=300
            ),
        ]
        assert instances == expected_instances

        m.assert_called_once_with(name, version)
        m.reset_mock()

        instances = client.list_group_instances(name, version, use_mtn)
        assert instances == expected_instances
        m.assert_not_called()

    entry = gencfg_cache.get_gencfg_group_instances_cache_entry(name, version, False)
    assert entry['_id'] == 'MSK_SG_NANNY:tags/stable-93-r190'

    entry = gencfg_cache.get_gencfg_group_instances_cache_entry(name, version, True)
    assert entry['_id'] == 'MSK_SG_NANNY:tags/stable-93-r190:1'
