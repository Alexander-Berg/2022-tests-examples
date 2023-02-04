import os
from datetime import datetime

import mock
import json
import yaml
import msgpack
from werkzeug import parse_date

from genisys.web import api

from .base import ApiTestCase
from . import fixture


class GenisysApiAppTestCase(ApiTestCase):
    CONFIG = {}

    def setUp(self):
        super(GenisysApiAppTestCase, self).setUp()
        os.environ['GENISYS_API_CONFIG'] = 'test/api/config.py'
        with mock.patch('socket.gethostname') as mock_gethostname:
            mock_gethostname.return_value = 'testhost'
            self.app = api.make_app()
        self.config = self.app.config
        self.config.update(self.CONFIG)
        self.client = self.app.test_client()

    def tearDown(self):
        self.app.cleanup()
        super(GenisysApiAppTestCase, self).tearDown()


class TestNodeInfo(GenisysApiAppTestCase):
    def test(self):
        resp = self.client.get('/ping')
        self.assertEquals(resp.status_code, 200)
        self.assertEquals(dict(resp.headers), dict([
            ('Content-Type', 'text/html; charset=utf-8'),
            ('Content-Length', '0'),
            ('X-Genisys-Address', 'testing-wsgi-listen-address'),
            ('X-Genisys-Pid', str(os.getpid())),
            ('X-Genisys-Hostname', 'testhost')
        ]))

    def test_500(self):
        self.app.add_url_rule('/fail', view_func=lambda: 1/0)
        resp = self.client.get('/fail')
        self.assertEquals(resp.status_code, 500)
        expected_body = """\
Internal server error: division by zero
Node info: {"address": "testing-wsgi-listen-address", "hostname": "testhost", "pid": %d}\
""" % (os.getpid(), )
        self.assertEquals(resp.data.decode('latin1'), expected_body)
        self.assertEquals(dict(resp.headers), dict([
            ('Content-Type', 'text/html; charset=utf-8'),
            ('Content-Length', str(len(expected_body))),
            ('X-Genisys-Address', 'testing-wsgi-listen-address'),
            ('X-Genisys-Pid', str(os.getpid())),
            ('X-Genisys-Hostname', 'testhost')
        ]))

    def test_404(self):
        resp = self.client.get('/404')
        self.assertEquals(resp.status_code, 404)
        self.assertEquals(resp.headers['X-Genisys-Address'],
                          'testing-wsgi-listen-address')
        self.assertEquals(resp.headers['X-Genisys-Pid'], str(os.getpid()))
        self.assertEquals(resp.headers['X-Genisys-Hostname'], 'testhost')


class HivemindSkyversionTestCase(GenisysApiAppTestCase):
    def test(self):
        resp = self.client.get('/v1/hosts/h2/skyversion?fmt=json')
        self.assertEquals(resp.status_code, 200)
        self.assertEquals(resp.headers['Content-Type'], 'application/json')
        result = json.loads(resp.data.decode('utf8'))
        self.assertEquals(result, {
            'SKYNET_BINARY': {'attrs': fixture.SKYVERSION2['attributes'],
                              'http': fixture.SKYVERSION2['http']['links'],
                              'md5': fixture.SKYVERSION2['md5'],
                              'name': fixture.SKYVERSION2['description'],
                              'rsync': fixture.SKYVERSION2['rsync']['links'],
                              'size': fixture.SKYVERSION2['size'] >> 10,
                              'skynet': fixture.SKYVERSION2['skynet_id']},
            'conf_author': 'user1',
            'conf_id': '123',
            'conf_mtime': 1448831689.649433,
            'svn_url': fixture.SKYVERSION2['attributes']['svn_url'],
        })
        self.assertEquals(parse_date(resp.headers['Last-Modified']),
                          datetime.utcfromtimestamp(1448831689))

        resp = self.client.get(
            '/v1/hosts/h1/skyversion?fmt=json',
            headers={'If-Modified-Since': 'Sun, 29 Nov 2015 21:14:48 GMT'}
        )
        self.assertEquals(resp.status_code, 200)
        result = json.loads(resp.data.decode('utf8'))
        self.assertEquals(result, {
            'SKYNET_BINARY': {'attrs': fixture.SKYVERSION1['attributes'],
                              'http': fixture.SKYVERSION1['http']['links'],
                              'md5': fixture.SKYVERSION1['md5'],
                              'name': fixture.SKYVERSION1['description'],
                              'rsync': fixture.SKYVERSION1['rsync']['links'],
                              'size': fixture.SKYVERSION1['size'] >> 10,
                              'skynet': fixture.SKYVERSION1['skynet_id']},
            'conf_author': 'user1',
            'conf_id': '123',
            'conf_mtime': 1448831689.649433,
            'svn_url': fixture.SKYVERSION1['attributes']['svn_url'],
        })

    def test_not_modified(self):
        resp = self.client.get(
            '/v1/hosts/h1/skyversion',
            headers={'If-Modified-Since': 'Sun, 29 Nov 2015 21:14:49 GMT'}
        )
        self.assertEquals(resp.status_code, 304)
        self.assertEquals(resp.data, b'')


class HivemindLacmusAPITestCase(GenisysApiAppTestCase):
    def test(self):
        resp = self.client.get('/v1/skynet/version/rules?fmt=json')
        self.assertEquals(resp.status_code, 200)
        self.assertEquals(resp.headers['Content-Type'], 'application/json')
        result = json.loads(resp.data.decode('utf8'))
        self.assertEquals(result, [
            {'desc': '',
             'name': 'Torkve testing',
             'resolved': {
                 'hosts': ['h2'],
                 'resource': {
                     'SKYNET_BINARY': {'attrs': {
                         'backup_task': 43257645,
                         'svn_url': 'svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-14.11@1967321',
                         'version': '14.11.0a6'
                     }},
                     'svn_url': 'svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-14.11@1967321'}
             },
             'sandbox_task_name': 'skynet.bin (14.11.0a6 (tc:2100))'},
            {'desc': '',
             'name': 'Opl testing',
             'resolved': {
                 'hosts': ['h3', 'h4'],
                 'resource': {
                     'SKYNET_BINARY': {'attrs': {
                         'backup_task': 43011611,
                         'svn_url': 'svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-15.0@1964982',
                         'version': '15.0.0a17'
                     }},
                     'svn_url': 'svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-15.0@1964982'}
             },
             'sandbox_task_name': 'skynet.bin (15.0.0a17 (tc:2076))'},
            {'desc': '',
             'name': 'Default Rule',
             'resolved': {
                 'hosts': [],
                 'resource': {
                     'SKYNET_BINARY': {'attrs': {
                        'backup_task': 43244456,
                        'mds': '30724/90358745.tar.gz',
                        'released': 'stable',
                        'svn_url': 'svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-14.10@1967022',
                        'ttl': 'inf',
                        'version': '14.10.17'
                     }},
                 'svn_url': 'svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-14.10@1967022'}
             },
             'sandbox_task_name': 'skynet.bin (14.10.17 (tc:2095))'}
        ])


class HivemindSkynetTestCase(GenisysApiAppTestCase):
    def test(self):
        resp = self.client.get('/v1/hosts/h2/skynet?fmt=yaml')
        self.assertEquals(resp.status_code, 200)
        self.assertEquals(resp.headers['Content-Type'], 'text/plain')
        result = yaml.load(resp.data.decode('utf8'))
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
                'mtime': 1448831689.649433},
            'functest': {'author': 'robot-genisys',
                'data': {'foo': 'default'},
                'mtime': 1448876630.036458},
        })
        self.assertEquals(parse_date(resp.headers['Last-Modified']),
                          datetime.utcfromtimestamp(1448876630))

        resp = self.client.get(
            '/v1/hosts/h4/skynet',
            headers={'If-Modified-Since': 'Sun, 29 Nov 2015 21:14:49 GMT'}
        )
        self.assertEquals(resp.status_code, 200)
        self.assertEquals(resp.headers['Content-Type'], 'application/json')
        result = json.loads(resp.data.decode('utf8'))
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
        self.assertEquals(parse_date(resp.headers['Last-Modified']),
                          datetime.utcfromtimestamp(1448876630))

    def test_not_modified(self):
        resp = self.client.get(
            '/v1/hosts/h1/skynet',
            headers={'If-Modified-Since': 'Mon, 30 Nov 2015 09:43:55 GMT'}
        )
        self.assertEquals(resp.status_code, 304)
        self.assertEquals(resp.data, b'')


class NativeApiTestCase(GenisysApiAppTestCase):
    def test_whole_tree(self):
        resp = self.client.get('/v2/hosts/h3?fmt=msgpack')
        self.assertEquals(resp.status_code, 200)
        self.assertEquals(resp.headers['Content-Type'], 'application/msgpack')
        result = msgpack.loads(resp.data, encoding='utf8')
        self.assertEquals(result, {
            'changed_at': 1447324963.759573,
            'changed_by': 'user1',
            'config': None,
            'config_hash': '4915bf60855b65a29316e2fa6776dc1b60def6c5',
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
                   'config_hash': 'a569ef3b78f998574c8e63b793f69a021c814ce7',
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
                    'config_hash': '6479043d7d8c514db2b888bc56624143104650f3',
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
                            'config_hash': '4e8291e510fb90ca353513d4343604e622fac365',
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
        self.assertEquals(parse_date(resp.headers['Last-Modified']),
                          datetime.utcfromtimestamp(1448876630))

    def test_subtree(self):
        resp = self.client.get(
            '/v2/hosts/h2/skynet.services?fmt=msgpack',
            headers={'If-Modified-Since': 'Sun, 29 Nov 2015 21:14:49 GMT'}
        )
        self.assertEquals(resp.status_code, 200)
        result = msgpack.loads(resp.data, encoding='utf8')
        self.assertEquals(result, {
            'changed_at': 1447324964.485181,
            'changed_by': 'user6',
            'config': None,
            'config_hash': '01236f5e434b6361872822d280af0632dd1690c2',
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
            'utime': 1448869700.328099,
        })
        self.assertEquals(parse_date(resp.headers['Last-Modified']),
                          datetime.utcfromtimestamp(1448869828))

    def test_not_modified(self):
        resp = self.client.get(
            '/v2/hosts/h3/skynet',
            headers={'If-Modified-Since': 'Mon, 30 Nov 2015 09:43:55 GMT'}
        )
        self.assertEquals(resp.status_code, 304)
        self.assertEquals(resp.data, b'')

    def test_not_found(self):
        resp = self.client.get('/v2/hosts/h3/skynet.nonexistent')
        self.assertEquals(resp.status_code, 404)

    def test_wrong_format(self):
        resp = self.client.get('/v2/hosts/h3/skynet?fmt=speech')
        self.assertEquals(resp.status_code, 400)

    def test_config_hash_parameter(self):
        resp = self.client.get(
            '/v2/hosts/h2/skynet.services.service1?fmt=msgpack&'
            'config_hash=faa7a45ba0c30d7fa4bdcf1b58c2d6b5ab04c3eb'
        )
        self.assertEquals(resp.status_code, 304)
        resp = self.client.get(
            '/v2/hosts/h2/skynet.services.service1?fmt=msgpack&'
            'config_hash=faa7a45ba0c30d7fa4bdcf1b58c2d6b5ab04c3eb',
            headers={'If-Modified-Since': 'Mon, 11 Aug 2014 09:43:55 GMT'}
        )
        self.assertEquals(resp.status_code, 304)

        resp = self.client.get(
            '/v2/hosts/h1/skynet.services.service1?fmt=msgpack&'
            'config_hash=faa7a45ba0c30d7fa4bdcf1b58c2d6b5ab04c3eb'
        )
        self.assertEquals(resp.status_code, 200)
        result = msgpack.loads(resp.data, encoding='utf8')
        self.assertEquals(result, {
            'changed_at': 1447324964.823972,
            'changed_by': 'user1',
            'config': {'confignum': -1, 'service': 1},
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
            'utime': 1448870436.938978
        })

    def test_hosts_by_path_and_rulename(self):
        resp = self.client.get(
            '/v2/hosts-by-path-and-rulename?fmt=msgpack&'
            'path=skynet.versions&rulename=Torkve+testing'
        )
        self.assertEquals(resp.status_code, 200)
        result = msgpack.loads(resp.data, encoding='utf8')
        self.assertEquals(result, {'hosts': ['h2']})

        resp = self.client.get(
            '/v2/hosts-by-path-and-rulename?fmt=msgpack&'
            'path=skynet.services.service1&rulename=APRIMUS'
        )
        self.assertEquals(resp.status_code, 200)
        result = msgpack.loads(resp.data, encoding='utf8')
        self.assertEquals(list(result.keys()), ['hosts'])
        self.assertEquals(set(result['hosts']), set(['h3', 'h2']))

        resp = self.client.get(
            '/v2/hosts-by-path-and-rulename?fmt=json&'
            'path=skynet.services.service1&rulename=nonexistent+rule'
        )
        self.assertEquals(resp.status_code, 200)
        result = json.loads(resp.data.decode('latin1'))
        self.assertEquals(result, {'hosts': None})

        resp = self.client.get(
            '/v2/hosts-by-path-and-rulename?fmt=yaml&'
            'path=bzzzz&rulename=bbbbzz'
        )
        self.assertEquals(resp.status_code, 200)
        result = yaml.load(resp.data.decode('utf8'))
        self.assertEquals(result, {'hosts': None})
