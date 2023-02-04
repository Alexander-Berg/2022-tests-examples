import logging
import time

import mock
import msgpack

from genisys.web import api
from genisys.web.model import _serialize

from .base import ApiTestCase
from . import fixture


class HivemindTreeStructureTest(ApiTestCase):
    def setUp(self):
        super(HivemindTreeStructureTest, self).setUp()
        self.hts = api.TreeStructure(self.storage, logging.getLogger('hts'),
                                     (10, 20))
        self.hts.start()

    def tearDown(self):
        self.hts.stop()
        super(HivemindTreeStructureTest, self).tearDown()

    def test_sample(self):
        result, mtime = self.hts.sample("h1", from_path='skynet',
                                        hivemind_emulation=True)
        self.assertEquals(result, {
            'skynet.services.service1': {
                'author': 'user1',
                'data': {'confignum': -1, 'service': 1},
                'mtime': 1448869828.625905},
            'skynet.services.service2': {
                'author': 'user2',
                'data': {'confignum': -1, 'service': 2},
                'mtime': 1448869093.437578},
            'skynet.versions': {
                'author': 'user1',
                'data': fixture.SKYVERSION1,
                'mtime': 1448831689.649433}
        })
        self.assertEquals(mtime, 1448869828)

        result, mtime = self.hts.sample("h2", from_path='skynet',
                                        hivemind_emulation=True)
        self.assertEquals(result, {
            'skynet.services.service1': {
                'author': 'user1',
                'data': {'confignum': 1025},
                'mtime': 1448869828.625905},
            'skynet.services.service2': {
                'author': 'user2',
                'data': {'confignum': 130},
                'mtime': 1448869093.437578},
            'skynet.versions': {
                'author': 'user1',
                'data': fixture.SKYVERSION2,
                'mtime': 1448831689.649433}
        })
        self.assertEquals(mtime, 1448869828)

        result, mtime = self.hts.sample("h3", from_path='skynet.versions',
                                        hivemind_emulation=True)
        self.assertEquals(result, {
            'skynet.versions': {
                'author': 'user1',
                'data': fixture.SKYVERSION3,
                'mtime': 1448831689.649433}
        })
        self.assertEquals(mtime, 1448831689)

        result, mtime = self.hts.sample("h4", from_path='',
                                        hivemind_emulation=True)
        self.assertEquals(result, {
            'functest': {
                'author': 'robot-genisys',
                'data': {'foo': 'default'},
                'mtime': 1448876630.036458},
            'skynet.services.service1': {
                'author': 'user1',
                'data': {'confignum': 77},
                'mtime': 1448869828.625905},
            'skynet.services.service2': {
                'author': 'user2',
                'data': {'confignum': 458},
                'mtime': 1448869093.437578},
            'skynet.versions': {
                'author': 'user1',
                'data': fixture.SKYVERSION3,
                'mtime': 1448831689.649433}
        })
        self.assertEquals(mtime, 1448876630)


class NativeTreeStructureTest(ApiTestCase):
    def setUp(self):
        super(NativeTreeStructureTest, self).setUp()
        self.nts = api.TreeStructure(self.storage, logging.getLogger('hts'),
                                     (10, 20))
        self.nts.start()
        def fake_cfghash(hash_info):
            return hash_info
        self.mocked_cfghash = mock.patch.object(self.nts, '_get_combined_config_hash')
        self.mocked_cfghash.start().side_effect = fake_cfghash

    def tearDown(self):
        self.nts.stop()
        self.mocked_cfghash.stop()
        super(NativeTreeStructureTest, self).tearDown()

    def test_sample(self):
        result, mtime = self.nts.sample("h1", from_path='skynet')
        self.assertEquals(result, {
            'changed_at': 1447324964.412732,
            'changed_by': 'user1',
            'config': None,
            'config_hash': {
                'skynet': '0000000000000000000000000000000000000000',
                'skynet.services': '0000000000000000000000000000000000000000',
                'skynet.services.service1': '35f9ef717248c41a630f40a27d51a5f6329a0fdb',
                'skynet.services.service2': '27ad5f2d0ae9391971bb4a7f885feee732d3bdbd',
                'skynet.versions': '9e7ebd8de19beb956876346e132d2bfadb20693c'},
            'ctime': 1447324964.412732,
            'etime': 1448868682.328492,
            'last_status': 'same',
            'matched_rules': [],
            'mcount': 1,
            'mtime': 1447325115.690523,
            'name': 'skynet',
            'owners': [],
            'path': 'skynet',
            'revision': 9,
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {
                'services': {
                    'changed_at': 1447324964.485181,
                    'changed_by': 'user6',
                    'config': None,
                    'config_hash': {
                        'skynet.services': '0000000000000000000000000000000000000000',
                        'skynet.services.service1': '35f9ef717248c41a630f40a27d51a5f6329a0fdb',
                        'skynet.services.service2': '27ad5f2d0ae9391971bb4a7f885feee732d3bdbd'},
                    'ctime': 1447324964.485181,
                    'etime': 1448870000.328099,
                    'last_status': 'same',
                    'matched_rules': [],
                    'mcount': 1,
                    'mtime': 1447325116.249975,
                    'name': 'services',
                    'owners': ['user6'],
                    'path': 'skynet.services',
                    'revision': 20,
                    'stype': 'yaml',
                    'stype_options': None,
                    'subsections': {
                        'service1': {
                            'changed_at': 1447324964.823972,
                            'changed_by': 'user1',
                            'config': {'confignum': -1,
                                       'service': 1},
                            'config_hash': '35f9ef717248c41a630f40a27d51a5f6329a0fdb',
                            'ctime': 1447324964.823972,
                            'etime': 1448870736.938978,
                            'last_status': 'same',
                            'matched_rules': ['DEFAULT'],
                            'mcount': 461,
                            'mtime': 1448869828.625905,
                            'name': 'service1',
                            'owners': [],
                            'path': 'skynet.services.service1',
                            'revision': 47,
                            'stype': 'yaml',
                            'stype_options': None,
                            'subsections': {},
                            'tcount': 4926,
                            'ttime': 1448870436.938978,
                            'ucount': 4907,
                            'utime': 1448870436.938978},
                        'service2': {
                            'changed_at': 1447324965.181499,
                            'changed_by': 'user2',
                            'config': {'confignum': -1,
                                       'service': 2},
                            'config_hash': '27ad5f2d0ae9391971bb4a7f885feee732d3bdbd',
                            'ctime': 1447324965.181499,
                            'etime': 1448871720.171249,
                            'last_status': 'same',
                            'matched_rules': ['DEFAULT',
                                              'PACKAGE_DEFAULT'],
                            'mcount': 363,
                            'mtime': 1448869093.437578,
                            'name': 'service2',
                            'owners': [],
                            'path': 'skynet.services.service2',
                            'revision': 75,
                            'stype': 'yaml',
                            'stype_options': None,
                            'subsections': {},
                            'tcount': 4952,
                            'ttime': 1448871420.171249,
                            'ucount': 4901,
                            'utime': 1448871420.171249}},
                    'tcount': 4913,
                    'ttime': 1448869700.328099,
                    'ucount': 4913,
                    'utime': 1448869700.328099},
                'versions': {
                    'changed_at': 1447242679.507396,
                    'changed_by': 'user1',
                    'config': fixture.SKYVERSION1,
                    'config_hash': '9e7ebd8de19beb956876346e132d2bfadb20693c',
                    'ctime': 1447242679.507396,
                    'etime': 1448831989.649433,
                    'last_status': 'modified',
                    'matched_rules': ['Default Rule'],
                    'mcount': 10,
                    'mtime': 1448831689.649433,
                    'name': 'versions',
                    'owners': [],
                    'path': 'skynet.versions',
                    'revision': 123,
                    'stype': 'sandbox_resource',
                    'stype_options': {'resource_type': 'SKYNET_BINARY'},
                    'subsections': {},
                    'tcount': 63,
                    'ttime': 1448831689.649433,
                    'ucount': 59,
                    'utime': 1448831689.649433}},
            'tcount': 4909,
            'ttime': 1448868382.328492,
            'ucount': 4909,
            'utime': 1448868382.328492
        })
        self.assertEquals(mtime, 1448869828)

        result, mtime = self.nts.sample("h2", from_path='skynet')
        self.assertEquals(result, {
            'changed_at': 1447324964.412732,
            'changed_by': 'user1',
            'config': None,
            'config_hash': {
                'skynet': '0000000000000000000000000000000000000000',
                'skynet.services': '0000000000000000000000000000000000000000',
                'skynet.services.service1': 'faa7a45ba0c30d7fa4bdcf1b58c2d6b5ab04c3eb',
                'skynet.services.service2': 'd28da1f534c838e7af7b20a8bdbb74d13dfa536d',
                'skynet.versions': 'd52168b597af10253e3b2336914f045ed97dc03c'},
            'ctime': 1447324964.412732,
            'etime': 1448868682.328492,
            'last_status': 'same',
            'matched_rules': [],
            'mcount': 1,
            'mtime': 1447325115.690523,
            'name': 'skynet',
            'owners': [],
            'path': 'skynet',
            'revision': 9,
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {
                'services': {
                    'changed_at': 1447324964.485181,
                    'changed_by': 'user6',
                    'config': None,
                    'config_hash': {
                        'skynet.services': '0000000000000000000000000000000000000000',
                        'skynet.services.service1': 'faa7a45ba0c30d7fa4bdcf1b58c2d6b5ab04c3eb',
                        'skynet.services.service2': 'd28da1f534c838e7af7b20a8bdbb74d13dfa536d'},
                    'ctime': 1447324964.485181,
                    'etime': 1448870000.328099,
                    'last_status': 'same',
                    'matched_rules': [],
                    'mcount': 1,
                    'mtime': 1447325116.249975,
                    'name': 'services',
                    'owners': ['user6'],
                    'path': 'skynet.services',
                    'revision': 20,
                    'stype': 'yaml',
                    'stype_options': None,
                    'subsections': {
                        'service1': {
                            'changed_at': 1447324964.823972,
                            'changed_by': 'user1',
                            'config': {'confignum': 1025},
                            'config_hash': 'faa7a45ba0c30d7fa4bdcf1b58c2d6b5ab04c3eb',
                            'ctime': 1447324964.823972,
                            'etime': 1448870736.938978,
                            'last_status': 'same',
                            'matched_rules': ["PREALLOCATE_ENABLE", 'APRIMUS', 'DEFAULT'],
                            'mcount': 461,
                            'mtime': 1448869828.625905,
                            'name': 'service1',
                            'owners': [],
                            'path': 'skynet.services.service1',
                            'revision': 47,
                            'stype': 'yaml',
                            'stype_options': None,
                            'subsections': {},
                            'tcount': 4926,
                            'ttime': 1448870436.938978,
                            'ucount': 4907,
                            'utime': 1448870436.938978},
                        'service2': {
                            'changed_at': 1447324965.181499,
                            'changed_by': 'user2',
                            'config': {'confignum': 130},
                            'config_hash': 'd28da1f534c838e7af7b20a8bdbb74d13dfa536d',
                            'ctime': 1447324965.181499,
                            'etime': 1448871720.171249,
                            'last_status': 'same',
                            'matched_rules': ["service enabled out of SEARCH_ALL", "DEFAULT"],
                            'mcount': 363,
                            'mtime': 1448869093.437578,
                            'name': 'service2',
                            'owners': [],
                            'path': 'skynet.services.service2',
                            'revision': 75,
                            'stype': 'yaml',
                            'stype_options': None,
                            'subsections': {},
                            'tcount': 4952,
                            'ttime': 1448871420.171249,
                            'ucount': 4901,
                            'utime': 1448871420.171249}},
                    'tcount': 4913,
                    'ttime': 1448869700.328099,
                    'ucount': 4913,
                    'utime': 1448869700.328099},
                'versions': {
                    'changed_at': 1447242679.507396,
                    'changed_by': 'user1',
                    'config': fixture.SKYVERSION2,
                    'config_hash': 'd52168b597af10253e3b2336914f045ed97dc03c',
                    'ctime': 1447242679.507396,
                    'etime': 1448831989.649433,
                    'last_status': 'modified',
                    'matched_rules': ['Torkve testing'],
                    'mcount': 10,
                    'mtime': 1448831689.649433,
                    'name': 'versions',
                    'owners': [],
                    'path': 'skynet.versions',
                    'revision': 123,
                    'stype': 'sandbox_resource',
                    'stype_options': {'resource_type': 'SKYNET_BINARY'},
                    'subsections': {},
                    'tcount': 63,
                    'ttime': 1448831689.649433,
                    'ucount': 59,
                    'utime': 1448831689.649433}},
            'tcount': 4909,
            'ttime': 1448868382.328492,
            'ucount': 4909,
            'utime': 1448868382.328492
        })
        self.assertEquals(mtime, 1448869828)

        result, mtime = self.nts.sample("h3", from_path='')
        self.assertEquals(result, {
            'changed_at': 1447324963.759573,
            'changed_by': 'user1',
            'config': None,
            'config_hash': {
                '': '0000000000000000000000000000000000000000',
                'functest': '0c28268895fc67ef214b609e9356f28f3d1c7ab7',
                'newsection': '0000000000000000000000000000000000000000',
                'newsection.newsubsection': '0000000000000000000000000000000000000000',
                'skynet': '0000000000000000000000000000000000000000',
                'skynet.services': '0000000000000000000000000000000000000000',
                'skynet.services.service1': 'ee73579693c066f353e5c69c94c5d08c7443eb2f',
                'skynet.services.service2': '67f580833c75e276bca8ce0d165d7007ddb11e56',
                'skynet.versions': '6ef43dddc13428339ff119da384fc58c4e0d63e5'},
            'ctime': 1447324963.759573,
            'etime': 1448868682.318819,
            'last_status': 'same',
            'matched_rules': [],
            'mcount': 1,
            'mtime': 1447325114.681502,
            'name': 'genisys',
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 1,
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {
                'functest': {
                    'changed_at': 1448876628.418099,
                    'changed_by': 'robot-genisys',
                    'config': {'foo': 'default'},
                    'config_hash': '0c28268895fc67ef214b609e9356f28f3d1c7ab7',
                    'ctime': 1448876628.418099,
                    'etime': 1448880169.080212,
                    'last_status': 'same',
                    'matched_rules': ['DEFAULT'],
                    'mcount': 1,
                    'mtime': 1448876630.036458,
                    'name': 'functest',
                    'owners': [],
                    'path': 'functest',
                    'revision': 219,
                    'stype': 'yaml',
                    'stype_options': None,
                    'subsections': {},
                    'tcount': 10,
                    'ttime': 1448879869.080212,
                    'ucount': 10,
                    'utime': 1448879869.080212},
               'newsection': {
                   'changed_at': 1448884427.1181922,
                   'changed_by': 'user2',
                   'config': None,
                   'config_hash': {
                       'newsection': '0000000000000000000000000000000000000000',
                       'newsection.newsubsection': '0000000000000000000000000000000000000000'},
                   'ctime': 1448884427.1181922,
                   'etime': 1448884427.1181922,
                   'last_status': 'new',
                   'matched_rules': [],
                   'mcount': 0,
                   'mtime': None,
                   'name': 'newsection',
                   'owners': [],
                   'path': 'newsection',
                   'revision': 149,
                   'stype': 'yaml',
                   'stype_options': None,
                   'subsections': {
                       'newsubsection': {
                       'changed_at': 1448995121.4803107,
                       'changed_by': 'user1',
                       'config': None,
                       'config_hash': '0000000000000000000000000000000000000000',
                       'ctime': 1448995121.4803107,
                       'etime': 1448995121.4803107,
                       'last_status': 'new',
                       'matched_rules': [],
                       'mcount': 0,
                       'mtime': None,
                       'name': 'newsubsection',
                       'owners': [],
                       'path': 'newsection.newsubsection',
                       'revision': 150,
                       'stype': 'yaml',
                       'stype_options': None,
                       'subsections': {},
                       'tcount': 0,
                       'ttime': None,
                       'ucount': 0,
                       'utime': None
                   }},
                   'tcount': 0,
                   'ttime': None,
                   'ucount': 0,
                   'utime': None},
               'skynet': {
                    'changed_at': 1447324964.412732,
                    'changed_by': 'user1',
                    'config': None,
                    'config_hash': {
                        'skynet': '0000000000000000000000000000000000000000',
                        'skynet.services': '0000000000000000000000000000000000000000',
                        'skynet.services.service1': 'ee73579693c066f353e5c69c94c5d08c7443eb2f',
                        'skynet.services.service2': '67f580833c75e276bca8ce0d165d7007ddb11e56',
                        'skynet.versions': '6ef43dddc13428339ff119da384fc58c4e0d63e5'},
                    'ctime': 1447324964.412732,
                    'etime': 1448868682.328492,
                    'last_status': 'same',
                    'matched_rules': [],
                    'mcount': 1,
                    'mtime': 1447325115.690523,
                    'name': 'skynet',
                    'owners': [],
                    'path': 'skynet',
                    'revision': 9,
                    'stype': 'yaml',
                    'stype_options': None,
                    'subsections': {
                        'services': {
                            'changed_at': 1447324964.485181,
                            'changed_by': 'user6',
                            'config': None,
                            'config_hash': {
                                'skynet.services': '0000000000000000000000000000000000000000',
                                'skynet.services.service1': 'ee73579693c066f353e5c69c94c5d08c7443eb2f',
                                'skynet.services.service2': '67f580833c75e276bca8ce0d165d7007ddb11e56'},
                            'ctime': 1447324964.485181,
                            'etime': 1448870000.328099,
                            'last_status': 'same',
                            'matched_rules': [],
                            'mcount': 1,
                            'mtime': 1447325116.249975,
                            'name': 'services',
                            'owners': ['user6'],
                            'path': 'skynet.services',
                            'revision': 20,
                            'stype': 'yaml',
                            'stype_options': None,
                            'subsections': {
                                'service1': {
                                    'changed_at': 1447324964.823972,
                                    'changed_by': 'user1',
                                    'config': {'confignum': 63},
                                    'config_hash': 'ee73579693c066f353e5c69c94c5d08c7443eb2f',
                                    'ctime': 1447324964.823972,
                                    'etime': 1448870736.938978,
                                    'last_status': 'same',
                                    'matched_rules': ["APRIMUS", 'DEFAULT'],
                                    'mcount': 461,
                                    'mtime': 1448869828.625905,
                                    'name': 'service1',
                                    'owners': [],
                                    'path': 'skynet.services.service1',
                                    'revision': 47,
                                    'stype': 'yaml',
                                    'stype_options': None,
                                    'subsections': {},
                                    'tcount': 4926,
                                    'ttime': 1448870436.938978,
                                    'ucount': 4907,
                                    'utime': 1448870436.938978},
                                'service2': {
                                    'changed_at': 1447324965.181499,
                                    'changed_by': 'user2',
                                    'config': {'confignum': 465},
                                    'config_hash': '67f580833c75e276bca8ce0d165d7007ddb11e56',
                                    'ctime': 1447324965.181499,
                                    'etime': 1448871720.171249,
                                    'last_status': 'same',
                                    'matched_rules': ["CpuAffinity on SANDBOX", "DEFAULT"],
                                    'mcount': 363,
                                    'mtime': 1448869093.437578,
                                    'name': 'service2',
                                    'owners': [],
                                    'path': 'skynet.services.service2',
                                    'revision': 75,
                                    'stype': 'yaml',
                                    'stype_options': None,
                                    'subsections': {},
                                    'tcount': 4952,
                                    'ttime': 1448871420.171249,
                                    'ucount': 4901,
                                    'utime': 1448871420.171249}},
                            'tcount': 4913,
                            'ttime': 1448869700.328099,
                            'ucount': 4913,
                            'utime': 1448869700.328099},
                        'versions': {
                            'changed_at': 1447242679.507396,
                            'changed_by': 'user1',
                            'config': fixture.SKYVERSION3,
                            'config_hash': '6ef43dddc13428339ff119da384fc58c4e0d63e5',
                            'ctime': 1447242679.507396,
                            'etime': 1448831989.649433,
                            'last_status': 'modified',
                            'matched_rules': ['Opl testing'],
                            'mcount': 10,
                            'mtime': 1448831689.649433,
                            'name': 'versions',
                            'owners': [],
                            'path': 'skynet.versions',
                            'revision': 123,
                            'stype': 'sandbox_resource',
                            'stype_options': {'resource_type': 'SKYNET_BINARY'},
                            'subsections': {},
                            'tcount': 63,
                            'ttime': 1448831689.649433,
                            'ucount': 59,
                            'utime': 1448831689.649433}},
                    'tcount': 4909,
                    'ttime': 1448868382.328492,
                    'ucount': 4909,
                    'utime': 1448868382.328492
                }
            },
            'tcount': 4909,
            'ttime': 1448868382.318819,
            'ucount': 4909,
            'utime': 1448868382.318819
        })
        self.assertEquals(mtime, 1448876630)

    def test_hosts_by_path_and_rulename(self):
        result = self.nts.hosts_by_path_and_rulename('skynet.versions',
                                                     'Torkve testing')
        self.assertEquals(set(result), set(['h2']))
        result = self.nts.hosts_by_path_and_rulename('skynet.versions',
                                                     'Opl testing')
        self.assertEquals(set(result), set(['h3', 'h4']))

        result = self.nts.hosts_by_path_and_rulename(
            'skynet.services.service1', 'APRIMUS'
        )
        self.assertEquals(set(result), set(['h3', 'h2']))

        result = self.nts.hosts_by_path_and_rulename(
            'skynet.services.service1', 'IMGS_base'
        )
        self.assertEquals(set(result), set(['h4']))

        result = self.nts.hosts_by_path_and_rulename(
            'skynet.services.service1', 'nonexistent rule'
        )
        self.assertIs(result, None)

        result = self.nts.hosts_by_path_and_rulename(
            'skynet.nonexistent.service', 'whatever rule'
        )
        self.assertIs(result, None)


class TestRun(ApiTestCase):
    def test_recache(self):
        fake_recache_calls = 0
        def fake_recache():
            nonlocal fake_recache_calls
            fake_recache_calls += 1

        try:
            with mock.patch.object(api.TreeStructure, '_recache') as mrec:
                mrec.side_effect = fake_recache
                self.hts = api.TreeStructure(
                    self.storage, logging.getLogger('hts'), (10, 20)
                )
                self.hts.start()
                self.hts.CACHE_VALIDITY_RANGE = (0, 0)
            for i in range(100):
                if fake_recache_calls == 1:
                    break
                time.sleep(0.01)
            self.assertGreater(fake_recache_calls, 0)
        finally:
            self.hts.stop()


class TestRecache(ApiTestCase):
    def setUp(self):
        super(TestRecache, self).setUp()
        self.nts = api.TreeStructure(self.storage, logging.getLogger('nts'),
                                     (10, 20))
        self.nts.start()

    def tearDown(self):
        super(TestRecache, self).tearDown()
        self.nts.stop()

    def test_nothing_changed(self):
        old_cached_timestamp = self.nts._cached_timestamp
        self.nts._recache()
        new_cached_timestamp = self.nts._cached_timestamp
        self.assertGreater(new_cached_timestamp, old_cached_timestamp)

    def test_new_value_appeared(self):
        config = {
            "configs": {0: {
                'config': {'fiz': 'buz'},
                'config_hash': 'chash',
                'matched_rules': ['rule0'],
            }},
            "hosts": {"h1": 0}
        }
        self.storage.db.volatile.update(
            {'vtype': 'section', 'key': "hash('newsection')"},
            {'$set': {"value": _serialize(config),
                      'mtime': 1448885427.1181922,
                      'ttime': 1448885427.1181922,
                      'mcount': 441}}
        )
        self.nts._recache()
        self.assertEquals(self.nts._cached_configs['newsection'], config)
        tree = msgpack.loads(self.nts._cached_structure, encoding='utf8')
        newsection = tree['subsections']['newsection']
        self.assertEquals(newsection['mtime'], 1448885427.1181922)
        self.assertEquals(newsection['mcount'], 441)

    def test_mtime_increased(self):
        config = {
            "configs": {0: {
                'config': {'bzz': 'arrrgh'},
                'config_hash': 'chash',
                'matched_rules': ['rule0'],
            }},
            "hosts": {"h1": 0}
        }
        self.storage.db.volatile.update(
            {'vtype': 'section', 'key': "hash('skynet')"},
            {'$set': {
                'mtime': 1447325116.690523,
                'value': _serialize(config),
                'ttime': 1448868383.328492,
            }}
        )
        self.nts._recache()
        self.assertEquals(self.nts._cached_configs['skynet'], config)
        tree = msgpack.loads(self.nts._cached_structure, encoding='utf8')
        skynet = tree['subsections']['skynet']
        self.assertEquals(skynet['mtime'], 1447325116.690523)

    def test_mtime_decreased(self):
        config = {
            "configs": {0: {
                'config': {'whatever': 1},
                'config_hash': 'chash',
                'matched_rules': ['rule0'],
            }},
            "hosts": {"h1": 0}
        }
        self.storage.db.volatile.update(
            {'vtype': 'section', 'key': "hash('skynet')"},
            {'$set': {'mtime': 1447325114.690523}
        })
        self.nts._recache()
        self.assertEquals(self.nts._cached_configs['skynet'],
                          {'configs': {}, 'hosts': {}})
        tree = msgpack.loads(self.nts._cached_structure, encoding='utf8')
        skynet = tree['subsections']['skynet']
        self.assertEquals(skynet['mtime'], 1447325115.690523)

    def test_only_metadata_changed(self):
        self.storage.db.volatile.update(
            {'vtype': 'section', 'key': "hash('skynet.versions')"},
            {'$set': {
                "last_status": "same",
                "tcount": 75,
                "ttime": 1448832689.649433,
            }}
        )
        self.nts._recache()
        tree = msgpack.loads(self.nts._cached_structure, encoding='utf8')
        skyversions = tree['subsections']['skynet']['subsections']['versions']
        self.assertEquals(skyversions['tcount'], 75)
        self.assertEquals(skyversions['ttime'], 1448832689.649433)
        self.assertEquals(skyversions['last_status'], 'same')
