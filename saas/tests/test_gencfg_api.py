# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import mock
import unittest
import requests_mock

from six import iteritems

from saas.library.python.gencfg import GencfgAPI, GencfgTag
from saas.library.python.gencfg.errors import GencfgGroupNotFound

TEST_HOSTS1 = ['sas1-1111.search.yandex.net', 'sas2-2222.search.yandex.net', 'sas3-3333.search.yandex.net', ]
TEST_HOSTS2 = ['sas4-1111.search.yandex.net', 'sas5-5555.search.yandex.net', 'sas6-6666.search.yandex.net', ]
TEST_GROUPS = ['TEST_GROUP_1', 'TEST_GROUP_2', ]
TEST_TAGS = ['stable-126-r1746', 'stable-126-r1745', 'stable-126-r1744', 'stable-1-r3', 'stable-1-r2', 'stable-1-r23', 'stable-2-r1', ]
COMMON_GROUPS = ['ALL_SEARCH', 'ALL_RTC', 'SAS_YASM_YASMAGENT_STABLE', 'SAS_SEARCH', 'SAS_SAAS_CLOUD', 'SAS_YASM_YASMAGENT_PRESTABLE', 'SAS_RUNTIME',
                 'ALL_RUNTIME', 'SAS_RTC_SLA_TENTACLES_PROD', 'SAS_JUGGLER_CLIENT_STABLE', 'SAS_KERNEL_UPDATE_3', ]
CUSTOM_GROUPS = {
    'SAS_SAAS_CLOUD_SIGNALS': ['sas3-3333.search.yandex.net'],
    'SAS_SAAS_CLOUD_VS_MODERATION_TELEPONY': ['sas1-1111.search.yandex.net'],
    'SAS_SAAS_CLOUD_TEST_SERVICE2': ['sas2-2222.search.yandex.net']
}


def wait_none():
    pass


@requests_mock.Mocker()
class TestGencfgApiClientRequests(unittest.TestCase):
    hosts_by_groups = {g: TEST_HOSTS1 for g in COMMON_GROUPS}
    hosts_by_groups.update(CUSTOM_GROUPS)
    groups_by_hosts = {
        'hosts_by_group': hosts_by_groups,
        'unknown_hosts': [],
        'hosts': TEST_HOSTS1,
        'groups': COMMON_GROUPS + list(CUSTOM_GROUPS.keys()),
        'groups_by_host': {h: COMMON_GROUPS + [k for k, v in iteritems(CUSTOM_GROUPS) if v == h] for h in TEST_HOSTS1}
    }
    hosts_data = [
        {
            'botdisk': 4000,
            'botmem': 128,
            'botprj': 'Search Portal > Runtime search > -',
            'botssd': 900,
            'dc': 'sas',
            'disk': 3458,
            'domain': '.search.yandex.net',
            'ffactor': '0.5U',
            'flags': 0,
            'golemowners': [],
            'hdd_count': 0,
            'hdd_models': [],
            'hdd_size': 0,
            'hwaddr': 'unknown',
            'invnum': '900902972',
            'ipmi': 0,
            'ipv4addr': 'unknown',
            'ipv6addr': '2a02:6b8:b000:1ba:225:90ff:fe88:369c',
            'issue': 'unknown',
            'kernel': 'unknown',
            'l3enabled': True,
            'lastupdate': 'unknown',
            'location': 'sas',
            'memory': 128,
            'model': 'E5-2650v2',
            'mtn_fqdn_version': 2,
            'n_disks': 0,
            'name': 'sas2-6939',
            'ncpu': 32,
            'net': 1000,
            'netcard': 'unknown',
            'os': 'unknown',
            'platform': 'unknown',
            'power': 1293.0,
            'queue': 'sas-1.1.1',
            'rack': '30',
            'raid': 'unknown',
            'shelves': [],
            'ssd': 825,
            'ssd_count': 0,
            'ssd_models': [],
            'ssd_size': 0,
            'storages': {},
            'switch': 'sas1-s28',
            'unit': '13',
            'vlan': 604,
            'vlan688ip': 'unknown',
            'vlans': {
                'vlan688': '2a02:6b8:c08:cf8d:0:0:0:1',
                'vlan761': '2a02:6b8:f000:ac4:0:0:0:1',
                'vlan788': '2a02:6b8:fc00:cd0d:0:0:0:1'
            },
            'vmfor': 'unknown',
            'walle_tags': [
                'rtc',
                'runtime'
            ]
        },
        {
            'botdisk': 4000,
            'botmem': 128,
            'botprj': 'Search Portal > Runtime search > -',
            'botssd': 1000,
            'dc': 'sas',
            'disk': 3458,
            'domain': '.search.yandex.net',
            'ffactor': '0.5U',
            'flags': 0,
            'golemowners': [],
            'hdd_count': 0,
            'hdd_models': [],
            'hdd_size': 0,
            'hwaddr': 'unknown',
            'invnum': '900909349',
            'ipmi': 0,
            'ipv4addr': 'unknown',
            'ipv6addr': '2a02:6b8:b000:6be:225:90ff:fe88:ba02',
            'issue': 'unknown',
            'kernel': 'unknown',
            'l3enabled': True,
            'lastupdate': 'unknown',
            'location': 'sas',
            'memory': 128,
            'model': 'E5-2650v2',
            'mtn_fqdn_version': 1,
            'n_disks': 0,
            'name': 'sas2-5663',
            'ncpu': 32,
            'net': 1000,
            'netcard': 'unknown',
            'os': 'unknown',
            'platform': 'unknown',
            'power': 1293.0,
            'queue': 'sas-1.1.1',
            'rack': '28',
            'raid': 'unknown',
            'shelves': [],
            'ssd': 938,
            'ssd_count': 0,
            'ssd_models': [],
            'ssd_size': 0,
            'storages': {},
            'switch': 'sas1-s153',
            'unit': '9',
            'vlan': 0,
            'vlan688ip': 'unknown',
            'vlans': {
                'vlan688': '2a02:6b8:c08:dd88:0:0:0:1',
                'vlan761': '2a02:6b8:f000:ac9:0:0:0:1',
                'vlan788': '2a02:6b8:fc00:db08:0:0:0:1'
            },
            'vmfor': 'unknown',
            'walle_tags': [
                'rtc',
                'runtime'
            ]
        }
    ]
    memory_guarantee = 32212254720.0
    memory_overcommit = 10737418240.0

    @classmethod
    def setUpClass(cls):
        cls.test_group = 'SAS_SAAS_TEST'
        cls.test_tags = ['stable-126-r1746', 'stable-126-r1745', 'stable-126-r1744', 'stable-1-r3', 'stable-1-r2', 'stable-1-r23', 'stable-2-r1', ]
        cls.gencfg_api_trunk = GencfgAPI()
        cls.gencfg_api_tag = GencfgAPI(tag=cls.test_tags[-1])
        cls.test_card = {
            'access': {'sshers': [], 'sudo_commands': [], 'sudoers': []},
            'audit': {
                'cpu': {
                    'class_name': 'greedy',
                    'extra_cpu': 0.0,
                    'greedy_limit': 560.0,
                    'last_modified': '2019-02-22 13:28:49',
                    'min_cpu': 0.0,
                    'service_coeff': 1.1333,
                    'service_groups': [],
                    'suggest': {'at': 'Jun 25, 2018', 'cpu': 351.0, 'msg': 'Do nothing: new power <360.00> is close to old power <350.15>'},
                    'traffic_coeff': 1.5},
                'instances': {'min_instances': None},
                'memory': {'last_modified': None}
            },
            'balancer': {'balancer_type': 'hashing', 'dnsname': None, 'enabled': False},
            'configs': {
                'balancer': {'module_name': None, 'output_file': None, 'params': None, 'sub_module_name': None},
                'basesearch': {'custom_name': None, 'jinja_params': [], 'template': None},
                'enabled': False, 'intmetasearch': {'config_power': None}
            },
            'description': 'Sas SaaS YMUSIC service',
            'dispenser': {'project_key': None},
            'guest': {'hbf_project_id': 0, 'owners': [], 'port': 8041, 'tags': {'ctype': 'none', 'itype': 'none', 'metaprj': 'unknown', 'prj': []}},
            'host_donor': None,
            'host_donors': [],
            'intlookups': [],
            'legacy': {'funcs': {'instanceCount': 'default', 'instanceMemory': None, 'instancePort': 'new19804', 'instancePower': 'exactly360'}},
            'master': 'SAS_SAAS_CLOUD',
            'name': cls.test_group,
            'nanny': {'services': [{'last_modified': '2018-06-26 18:43:02', 'name': 'saas_cloud_ymusic', 'status': 'ONLINE', 'tag': 'tags/stable-114-r75'}]},
            'on_update_trigger': None,
            'owners': ['saku', 'i024', 'yandex_search_tech_quality_robot_saas'],
            'owners_abc_roles': [],
            'properties': {
                'allow_background_groups': True,
                'allow_fast_download_queue': False,
                'background_group': False,
                'cpu_guarantee_set': True,
                'created_at': 'Dec 1, 2016',
                'created_from_portovm_group': None,
                'expires': None,
                'export_to_cauth': False,
                'extra_disk_shards': 2,
                'extra_disk_size': 0,
                'extra_disk_size_per_instance': 0,
                'fake_group': False,
                'full_host_group': False,
                'hbf_old_project_ids': [],
                'hbf_parent_macros': '_GENCFG_SAS_SAAS_CLOUD_YMUSIC_',
                'hbf_project_id': 16779212,
                'hbf_range': '_GENCFG_SEARCHPRODNETS_ROOT_',
                'hbf_resolv_conf': 'default',
                'ignore_cpu_audit': False,
                'internet_tunnel': False,
                'ipip6_ext_tunnel': False,
                'ipip6_ext_tunnel_pool_name': 'default',
                'ipip6_ext_tunnel_v2': False,
                'monitoring_golovan_port': None,
                'monitoring_juggler_port': None,
                'monitoring_ports_ready': True,
                'mtn': {
                    'export_mtn_to_cauth': False,
                    'port': None,
                    'portovm_mtn_addrs': False,
                    'tunnels': {
                        'hbf_decapsulator_anycast_address': '2a02:6b8:0:3400::aaaa', 'hbf_slb_ipv4_mtu': 1450, 'hbf_slb_ipv6_mtu': 1450, 'hbf_slb_name': []
                    },
                    'use_mtn_in_config': False
                },
                'mtn_fqdn_domain': None,
                'mtn_fqdn_version': 0,
                'nidx_for_group': None,
                'nonsearch': False,
                'security_segment': 'normal',
                'unraisable_group': False,
                'untouchable': False,
                'yasmagent_prestable_group': False,
                'yasmagent_production_group': True
            },
            'recluster': {'alloc_hosts': [], 'cleanup': [], 'generate_intlookups': [], 'next_stage': None},
            'reminders': [],
            'reqs': {
                'hosts': {
                    'cpu_models': [],
                    'except_cpu_models': [],
                    'location': {'dc': [], 'location': ['sas']},
                    'max_per_switch': 0,
                    'memory': 0.0,
                    'ndisks': 0,
                    'netcard_regexp': None
                },
                'instances': {
                    'affinity_category': 'default',
                    'disk': 151397597184.0,
                    'max_per_host': 0,
                    'memory_guarantee': cls.memory_guarantee,
                    'memory_overcommit': cls.memory_overcommit,
                    'min_instances': 0,
                    'min_per_host': 0,
                    'net_guarantee': 0.0,
                    'net_limit': 0.0,
                    'port': 8041,
                    'power': 0,
                    'ssd': 96636764160.0
                },
                'shards': {'equal_instances_power': False, 'max_replicas': 1000, 'min_power': 360, 'min_replicas': 1},
                'volumes': [
                    {'generate_deprecated_uuid': False, 'guest_mp': '', 'host_mp_root': '/place', 'mount_point_workdir': True, 'quota': 10737418240.0,
                     'shared': False, 'symlinks': [], 'uuid_generator_version': 2},
                    {'generate_deprecated_uuid': False, 'guest_mp': '/', 'host_mp_root': '/place', 'mount_point_workdir': False, 'quota': 10737418240.0,
                     'shared': False, 'symlinks': [], 'uuid_generator_version': 2},
                    {'generate_deprecated_uuid': False, 'guest_mp': '/cores', 'host_mp_root': '/place', 'mount_point_workdir': False, 'quota': 85899345920.0,
                     'shared': False, 'symlinks': [], 'uuid_generator_version': 2},
                    {'generate_deprecated_uuid': False, 'guest_mp': '/db/bsconfig/webstate', 'host_mp_root': '/place', 'mount_point_workdir': False,
                     'quota': 1073741824.0, 'shared': False, 'symlinks': ['/state'], 'uuid_generator_version': 2},
                    {'generate_deprecated_uuid': False, 'guest_mp': '/logs', 'host_mp_root': '/place', 'mount_point_workdir': False, 'quota': 42949672960.0,
                     'shared': False, 'symlinks': ['/usr/local/www/logs'], 'uuid_generator_version': 2},
                    {'generate_deprecated_uuid': False, 'guest_mp': '/ssd', 'host_mp_root': '/ssd', 'mount_point_workdir': False, 'quota': 96636764160.0,
                     'shared': False, 'symlinks': ['/data'], 'uuid_generator_version': 2}]
            },
            'resolved_owners': ['alexbykov', 'anikella', 'coffeeman', 'derrior', 'i024', 'jeannette', 'saku', 'salmin', 'varvar4ik', 'yrum'],
            'resources': {'ninstances': 40},
            'searcherlookup_postactions': {
                'aline_tag': {'enabled': False},
                'conditional_tags': [],
                'copy_on_ssd_tag': {'enabled': False},
                'custom_tier': {'enabled': False, 'tier_name': ''},
                'fixed_hosts_tags': [],
                'gen_by_code_tags': [],
                'host_memory_tag': {'enabled': False},
                'int_with_snippet_reqs_tag': {'enabled': False},
                'pre_in_pre_tags': [],
                'replica_tags': {'enabled': False, 'first_replica_tag': None, 'tag_format': None},
                'shardid_tag': {'enabled': False, 'tag_format': None, 'tag_prefix': 'a_shard_', 'tags_format': [], 'write_primus_name': False}
            },
            'slaves': [],
            'tags': {'ctype': 'prod', 'itag': [], 'itype': 'rtyserver', 'metaprj': 'web', 'prj': ['saas-ymusic']},
            'triggers': {'on_add_host': {'method': 'default'}, 'on_rename_group': {'method': 'default'}},
            'walle': {'reinstall': {'max_reinstall_count': 0, 'max_reinstall_percent': 0}},
            'watchers': [], 'yp': {'endpointset': None}
        }

    def test_get_tags(self, m):
        mock_response = {'displayed_tags': self.test_tags}
        m.get(
            'https://api.gencfg.yandex-team.ru/trunk/tags',
            json=mock_response
        )
        # self.assertEqual(GencfgAPI.get_tags(), self.test_tags)
        tags = GencfgAPI.get_tags()
        self.assertTrue(isinstance(tags[0], GencfgTag))
        self.assertListEqual(self.test_tags, tags)
        self.assertEqual(GencfgAPI.get_latest_tag(), GencfgTag(self.test_tags[0]))

    def test_get_tag_by_commit(self, m):
        test_commit = 123
        mock_response = {'commit': test_commit, 'tag': None}
        # no commit for tag yet
        m.get(
            'https://api.gencfg.yandex-team.ru/tags/tag_by_commit/{}'.format(test_commit),
            json=mock_response
        )
        tag = GencfgAPI.get_tag_by_commit(test_commit)
        self.assertIsInstance(tag, GencfgTag)
        self.assertEqual(tag, GencfgTag(None))
        self.assertEqual(tag, GencfgTag('trunk'))
        # and now there is a tag for this commit
        mock_response['tag'] = TEST_TAGS[0]
        self.assertIsInstance(tag, GencfgTag)
        self.assertEqual(TEST_TAGS[0], GencfgAPI.get_tag_by_commit(test_commit))

    def test_get_group_trunk_info(self, m):
        mock_response = {
            'group': self.test_group,
            'hosts': TEST_HOSTS2,
            'master': 'SAS_SAAS_CLOUD_TEST',
            'owners': ['yandex_search_tech_quality_robot_saas', 'coffeeman']
        }
        m.get(
            'https://api.gencfg.yandex-team.ru/trunk/groups/{}'.format(self.test_group),
            json=mock_response
        )
        self.assertDictEqual(mock_response, self.gencfg_api_trunk.get_group_info(self.test_group))

    def test_get_group_tag_info(self, m):
        mock_response = {
            'group': self.test_group,
            'hosts': TEST_HOSTS1,
            'master': 'SAS_SAAS_CLOUD_TEST',
            'owners': ['yandex_search_tech_quality_robot_saas', 'coffeeman']
        }
        m.get(
            'https://api.gencfg.yandex-team.ru/tags/{}/groups/{}'.format(self.test_tags[-1], self.test_group),
            json=mock_response
        )
        self.assertDictEqual(mock_response, self.gencfg_api_tag.get_group_info(self.test_group))

    def test_get_non_existing_group_info(self, m):
        m.get(
            'https://api.gencfg.yandex-team.ru/trunk/groups/{}'.format(self.test_group),
            text='group does not exist', status_code=404
        )
        with self.assertRaises(GencfgGroupNotFound):
            self.gencfg_api_trunk.get_group_info(self.test_group)

    def test_change_tag(self, m):
        tag_str = self.test_tags[-1]
        mock_response_trunk = {
            'group': self.test_group,
            'hosts': TEST_HOSTS1,
            'master': 'SAS_SAAS_CLOUD_TEST',
            'owners': ['yandex_search_tech_quality_robot_saas', 'coffeeman']
        }
        mock_response_tag = {
            'group': self.test_group,
            'hosts': TEST_HOSTS2,
            'master': 'SAS_SAAS_CLOUD_TEST',
            'owners': ['yandex_search_tech_quality_robot_saas', 'coffeeman']
        }
        m.get(
            'https://api.gencfg.yandex-team.ru/tags/{}/groups/{}'.format(tag_str, self.test_group),
            json=mock_response_tag
        )
        m.get(
            'https://api.gencfg.yandex-team.ru/trunk/groups/{}'.format(self.test_group),
            json=mock_response_trunk
        )
        gencfg = GencfgAPI()
        self.assertDictEqual(mock_response_trunk, gencfg.get_group_info(self.test_group))

        gencfg.tag = tag_str
        self.assertDictEqual(mock_response_tag, gencfg.get_group_info(self.test_group))

        gencfg.tag = 'trunk'
        self.assertDictEqual(mock_response_trunk, gencfg.get_group_info(self.test_group))

        gencfg.tag = GencfgTag(tag_str)
        self.assertDictEqual(mock_response_tag, gencfg.get_group_info(self.test_group))

        gencfg.tag = GencfgTag('trunk')
        self.assertDictEqual(mock_response_trunk, gencfg.get_group_info(self.test_group))

    def test_get_group_card(self, m):
        m.get(
            'https://api.gencfg.yandex-team.ru/trunk/groups/{}/card'.format(self.test_group),
            json=self.test_card
        )
        self.assertDictEqual(self.test_card, self.gencfg_api_trunk.get_group_info_card(self.test_group))

    def test_get_slaves(self, m):
        m.get(
            'https://api.gencfg.yandex-team.ru/trunk/groups/{}/card'.format(self.test_card['master']),
            json={'slaves': [self.test_group, ]}
        )
        res = self.gencfg_api_trunk.get_slave_groups(self.test_card['master'])
        self.assertEqual(1, len(res))
        self.assertEqual(self.test_group, res[0])

    def test_get_groups_by_hosts(self, m):
        m.post(
            'https://api.gencfg.yandex-team.ru/trunk/hosts/hosts_to_groups',
            json=self.groups_by_hosts
        )
        res = self.gencfg_api_trunk.get_groups_by_hostlist(TEST_HOSTS1)
        self.assertEqual(TEST_HOSTS1, res['hosts'])
        self.assertTrue('hosts_by_group' in res)
        self.assertTrue('groups' in res)
        self.assertTrue(isinstance(res['groups'], list))
        self.assertTrue('groups_by_host' in res)
        self.assertTrue(isinstance(res['groups_by_host'], dict))

    def test_get_hosts_data(self, m):
        m.post(
            'https://api.gencfg.yandex-team.ru/trunk/hosts_data',
            json={'hosts_data': self.hosts_data, 'notfound_hosts': []}
        )
        self.assertListEqual(self.hosts_data, self.gencfg_api_trunk.get_hosts_data('groups_by_host')['hosts_data'])

    def test_get_memory_limit(self, m):
        m.get(
            'https://api.gencfg.yandex-team.ru/trunk/groups/{}/card'.format(self.test_group),
            json=self.test_card
        )
        mem_limit = self.memory_guarantee + self.memory_overcommit
        self.assertEqual(self.memory_guarantee, self.gencfg_api_trunk.get_memory_guarantee(self.test_group))
        self.assertEqual(mem_limit, self.gencfg_api_trunk.get_memory_limit(self.test_group))


@mock.patch('saas.library.python.gencfg.api.wait_minute', wait_none)
class TestGencfgApiClient(unittest.TestCase):
    @staticmethod
    def get_group_card(group):
        return {
            'group': group,
            'hosts': TEST_HOSTS1,
            'master': 'SAS_SAAS_CLOUD_TEST',
            'owners': ['yandex_search_tech_quality_robot_saas', 'coffeeman']
        }

    @classmethod
    def setUpClass(cls):
        cls.test_group = 'SAS_SAAS_TEST'
        cls.group_info = {
            'group': cls.test_group,
            'hosts': TEST_HOSTS1,
            'master': 'SAS_SAAS_CLOUD_TEST',
            'owners': ['yandex_search_tech_quality_robot_saas', 'coffeeman']
        }

    def setUp(self):
        self.gencfg_api = GencfgAPI(wait_function=wait_none())

    def test_group_exists(self):
        self.gencfg_api.get_group_info = mock.MagicMock(return_value=self.group_info)
        self.assertTrue(self.gencfg_api.group_exists(self.test_group))
        self.gencfg_api.get_group_info.assert_called_once_with(self.test_group)

        self.gencfg_api.get_group_info = mock.MagicMock(side_effect=GencfgGroupNotFound(self.test_group, self.gencfg_api.tag))
        self.assertFalse(self.gencfg_api.group_exists(self.test_group))
        self.gencfg_api.get_group_info.assert_called_once_with(self.test_group)

    @mock.patch('saas.library.python.gencfg.GencfgAPI.get_tag_by_commit')
    def test_wait_for_tag_with_commit(self, mock_tag_by_commit):
        test_commit = 123
        mock_tag_by_commit.side_effect = [GencfgTag(x) for x in [None, None, None, TEST_TAGS[0]]]
        tag = GencfgAPI.wait_for_tag_with_commit(test_commit)
        self.assertIsInstance(tag, GencfgTag)
        self.assertEqual(tag, TEST_TAGS[0])

    def test_get_group_hostlist(self):
        self.gencfg_api.get_group_info = mock.MagicMock(side_effect=self.get_group_card)
        hostlist = self.gencfg_api.get_group_hostlist(self.test_group)
        self.assertIsInstance(hostlist, list)
        self.assertListEqual(hostlist, TEST_HOSTS1)

    @mock.patch('saas.library.python.gencfg.GencfgAPI.group_present_in_trunk')
    def test_wait_for_tag_with_groups_raise_exceptions_for_groups_not_present_in_trunk(self, group_present_in_trunk):
        group_present_in_trunk.return_value = False
        with self.assertRaises(GencfgGroupNotFound):
            GencfgAPI.wait_for_tag_with_groups([self.test_group, ])
        group_present_in_trunk.assert_called_once_with(self.test_group)

    def test_wait_for_tag_with_groups(self):
        test_tag_sequence = ['stable-1-r{}'.format(x) for x in range(1, 10)]
        test_tags = [GencfgTag(t) for t in test_tag_sequence]

        def group_exists_mock(_, tag):
            return tag == test_tags[-1]

        GencfgAPI.get_latest_tag = mock.Mock(side_effect=test_tags)
        GencfgAPI.group_present_in_trunk = mock.Mock(return_value=True)
        GencfgAPI.group_present_in_tag = mock.Mock(side_effect=group_exists_mock)

        result = GencfgAPI.wait_for_tag_with_groups([self.test_group, ])
        self.assertEqual(result, test_tags[-1])
