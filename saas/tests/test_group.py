# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import copy
import mock
import unittest

from saas.library.python.gencfg import GencfgAPI, GencfgGroup
from saas.library.python.gencfg.errors import GencfgApiError


@mock.patch.object(GencfgAPI, 'group_exists', return_value=True, autospec=True)
class TestGencfgGroup(unittest.TestCase):
    TEST_CARD_INFO_REQS_INSTANCES_IO_LIMITS = {
        'hdd_io_ops_read_limit': 1000,
        'hdd_io_ops_write_limit': 1000,
        'hdd_io_read_limit': 10485760.0,
        'hdd_io_write_limit': 10485760.0,
        'ssd_io_ops_read_limit': 1000,
        'ssd_io_ops_write_limit': 1000,
        'ssd_io_read_limit': 524288000.0,
        'ssd_io_write_limit': 104857600.0
    }

    TEST_CARD_INFO = {
        'audit': {
            'cpu': {
                'class_name': 'greedy',
                'extra_cpu': 0.0,
                'greedy_limit': 240.0,
                'last_modified': '2019-08-17 13:03:03',
                'min_cpu': 0.0,
                'service_coeff': 1.1333,
                'service_groups': [],
                'suggest': {
                    'at': 'Jun 25, 2018',
                    'cpu': 83.0,
                    'msg': 'Modify power: <200.00> => <82.65> for group with instance power already set'
                },
                'traffic_coeff': 1.5
            },
        },
        'description': 'New service named SAS_SAAS_CLOUD_SNIPPETS_KV',
        'legacy': {
            'funcs': {
                'instanceCount': 'exactly1',
                'instanceMemory': None,
                'instancePort': 'new32596',
                'instancePower': 'exactly160'
            }
        },
        'master': 'SAS_SAAS_CLOUD',
        'name': 'SAS_SAAS_CLOUD_SNIPPETS_KV',
        'reqs': {
            'hosts': {},
            'instances': {
                'affinity_category': 'default',
                'disk': 272730423296.0,
                'max_per_host': 0,
                'memory_guarantee': 39728447488.0,
                'memory_overcommit': 21474836480.0,
                'min_instances': 0,
                'min_per_host': 0,
                'net_guarantee': 0.0,
                'net_limit': 0.0,
                'port': 8041,
                'power': 0,
                'ssd': 75161927680.0,
            },
            'shards': {
                'equal_instances_power': False,
                'max_replicas': 1000,
                'min_power': 160,
                'min_replicas': 1
            },
            'volumes': [
                {
                    'generate_deprecated_uuid': False,
                    'guest_mp': '',
                    'host_mp_root': '/place',
                    'mount_point_workdir': False,
                    'quota': 10737418240.0,
                    'shared': False,
                    'symlinks': [],
                    'uuid_generator_version': 2
                },
                {
                    'generate_deprecated_uuid': False,
                    'guest_mp': '/',
                    'host_mp_root': '/place',
                    'mount_point_workdir': False,
                    'quota': 10737418240.0,
                    'shared': False,
                    'symlinks': [],
                    'uuid_generator_version': 2
                },
                {
                    'generate_deprecated_uuid': False,
                    'guest_mp': '/cores',
                    'host_mp_root': '/place',
                    'mount_point_workdir': False,
                    'quota': 142807662592.0,
                    'shared': False,
                    'symlinks': [],
                    'uuid_generator_version': 2
                },
                {
                    'generate_deprecated_uuid': False,
                    'guest_mp': '/db/bsconfig/webstate',
                    'host_mp_root': '/place',
                    'mount_point_workdir': False,
                    'quota': 1073741824.0,
                    'shared': False,
                    'symlinks': [
                        '/state'
                    ],
                    'uuid_generator_version': 2
                },
                {
                    'generate_deprecated_uuid': False,
                    'guest_mp': '/logs',
                    'host_mp_root': '/place',
                    'mount_point_workdir': False,
                    'quota': 107374182400.0,
                    'shared': False,
                    'symlinks': [
                        '/usr/local/www/logs'
                    ],
                    'uuid_generator_version': 2
                },
                {
                    'generate_deprecated_uuid': False,
                    'guest_mp': '/ssd',
                    'host_mp_root': '/ssd',
                    'mount_point_workdir': False,
                    'quota': 75161927680.0,
                    'shared': False,
                    'symlinks': [
                        '/data'
                    ],
                    'uuid_generator_version': 2
                }
            ]
        },
        'slaves': [],
        'tags': {
            'ctype': 'prod',
            'itag': [],
            'itype': 'rtyserver',
            'metaprj': 'web',
            'prj': [
                'saas-test-prj'
            ]
        },
        'triggers': {
            'on_add_host': {
                'method': 'default'
            },
            'on_rename_group': {
                'method': 'default'
            }
        },
        'walle': {
            'reinstall': {
                'max_reinstall_count': 0,
                'max_reinstall_percent': 0
            }
        },
        'watchers': [],
        'yp': {
            'moved': False,
            'where': {
                'endpoint_set': None,
                'location': None
            }
        }
    }
    test_group = 'SAS_TEST_GROUP'
    test_tag = 'stable-123-r1234'
    test_tag2 = r'tags\/stable-123-r1234'

    @staticmethod
    def build_test_hosts_req(locations=None):
        locations = locations if locations else []
        return {
            'cpu_models': [],
            'except_cpu_models': [],
            'location': {
                'dc': [],
                'location': locations
            },
            'max_per_switch': 0,
            'memory': 0.0,
            'ndisks': 0,
            'netcard_regexp': None
        }

    @classmethod
    def build_test_card_info(cls, group_name=None, with_io_limits=False, req_hosts=None):
        group_name = group_name if group_name else cls.test_group
        req_hosts = req_hosts if req_hosts else cls.build_test_hosts_req()

        test_card_info = copy.deepcopy(cls.TEST_CARD_INFO)
        test_card_info['name'] = group_name
        test_card_info['reqs']['hosts'] = req_hosts
        if with_io_limits:
            test_card_info['reqs']['instances'].update(cls.TEST_CARD_INFO_REQS_INSTANCES_IO_LIMITS)
        return test_card_info

    def test_from_string(self, gencfg_group_exists):
        g1 = GencfgGroup(self.test_group)
        self.assertEqual(g1.name, self.test_group)
        gencfg_group_exists.assert_called_once_with(g1._gencfg_api, self.test_group)

    def test_groups_equal(self, _):
        g1 = GencfgGroup(self.test_group)
        g2 = GencfgGroup(self.test_group)
        self.assertEqual(g1, g2)
        g1 = GencfgGroup(self.test_group, self.test_tag)
        g2 = GencfgGroup(self.test_group, self.test_tag)
        self.assertEqual(g1, g2)
        g2 = GencfgGroup(self.test_group, self.test_tag2)
        self.assertEqual(g1, g2)

    def test_groups_with_different_tags_unequal(self, _):
        g1 = GencfgGroup(self.test_group)
        g2 = GencfgGroup(self.test_group, self.test_tag)
        self.assertNotEqual(g1, g2)
        g3 = GencfgGroup(self.test_group, 'stable-123-r6789')
        self.assertNotEqual(g2, g3)

    def test_tag_property(self, _):
        g = GencfgGroup(self.test_group, self.test_tag)
        self.assertEqual(self.test_tag, g._tag)
        self.assertEqual(g._tag, g.tag)

        new_tag = 'stable-123-r6789'
        g.tag = new_tag
        self.assertEqual(new_tag, g._tag)
        self.assertEqual(new_tag, g.tag)

    def test_group_without_io_limits_has_empty_io_limits(self, _):
        g = GencfgGroup(self.test_group, self.test_tag)
        g._gencfg_api.get_group_info_card = mock.MagicMock(return_value=self.build_test_card_info(with_io_limits=False))
        self.assertDictEqual(g.io_limits, {})

    def test_group_with_io_limits_has_correct_io_limits(self, _):
        updated_test_card_info = self.build_test_card_info(with_io_limits=True)
        g = GencfgGroup(self.test_group, self.test_tag)
        g._gencfg_api.get_group_info_card = mock.MagicMock(return_value=updated_test_card_info)

        self.assertDictEqual(g.io_limits, self.TEST_CARD_INFO_REQS_INSTANCES_IO_LIMITS)

    def test_location_parsed_from_name(self, _):
        g = GencfgGroup(self.test_group, self.test_tag)
        g._gencfg_api.get_group_info_card = mock.MagicMock(return_value=self.build_test_card_info(
            group_name=self.test_group, req_hosts=self.build_test_hosts_req([])
        ))
        self.assertEqual(g.location, 'SAS')

    def test_location_parsed_from_card(self, _):
        g = GencfgGroup(self.test_group, self.test_tag)
        g._gencfg_api.get_group_info_card = mock.MagicMock(return_value=self.build_test_card_info(
            group_name=self.test_group, req_hosts=self.build_test_hosts_req(locations=['man'])
        ))
        self.assertEqual(g.location, 'MAN')

    def test_multiple_locations_raise_exception(self, _):
        g = GencfgGroup(self.test_group, self.test_tag)
        g._gencfg_api.get_group_info_card = mock.MagicMock(return_value=self.build_test_card_info(
            group_name=self.test_group, req_hosts=self.build_test_hosts_req(locations=['sas', 'man'])
        ))
        with self.assertRaises(GencfgApiError):
            return g.location

    def test_prj_tags(self, _):
        g = GencfgGroup(self.test_group, self.test_tag)
        g._gencfg_api.get_group_info_card = mock.MagicMock(return_value=self.build_test_card_info(group_name=self.test_group))
        self.assertListEqual(g.prj_tags, ['saas-test-prj', ])
