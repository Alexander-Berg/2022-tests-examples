import itertools
import unittest

import mock
from pymongo import MongoClient

from genisys.web import model as genisys_model
from lacmus2.web.model import Lacmus2MongoStorage


class BaseMongoTestCase(unittest.TestCase):
    maxDiff = None
    testdb_host = 'localhost'
    testdb_name = 'lacmus2_unittest'
    CONFIG = {'HISTORICAL_SIZE': 4096}

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def setUp(self):
        super(BaseMongoTestCase, self).setUp()
        client = MongoClient(self.testdb_host)
        self.database = client[self.testdb_name]
        self.storage = Lacmus2MongoStorage(client, self.testdb_name,
                                           self.CONFIG)
        self.clear_db()
        self.setup_mocks()

    def setup_mocks(self):
        self.mock_ts = mock.patch.object(self.storage, '_get_ts')
        self.mock_ts_return_value = 1444000000

        def fake_get_ts():
            self.mock_ts_return_value += 1
            return self.mock_ts_return_value
        self.mock_ts.start().side_effect = fake_get_ts

        def fake_hash(val):
            return "hash(%r)" % (val, )
        self.mock_hash = mock.patch.object(genisys_model, 'volatile_key_hash')
        self.mock_hash.start().side_effect = fake_hash

        self.mock_gen_oid = mock.patch.object(self.storage, '_generate_oid')
        mock_gen_oid_return_value = itertools.count(1)

        def fake_gen_oid():
            return '#{}'.format(next(mock_gen_oid_return_value))
        self.mock_gen_oid.start().side_effect = fake_gen_oid
        self.mock_check_oid = mock.patch.object(self.storage, '_check_oid')
        self.mock_check_oid.start()

    def tearDown(self):
        super(BaseMongoTestCase, self).tearDown()
        self.clear_db()
        self.mock_ts.stop()
        self.mock_gen_oid.stop()
        self.mock_hash.stop()
        self.mock_check_oid.stop()

    def assert_volatiles(self, expected, vtype=None, key=None, fields=None):
        filter_ = {}
        if vtype is not None:
            filter_['vtype'] = vtype
        if key is not None:
            filter_['key'] = key
        records = self.database.volatile.find(filter_, {'_id': False})
        records = list(records.sort([('vtype', 1), ('key', 1)]))
        for record in records:
            record['source'] = genisys_model._deserialize(record['source'])
            if record['value']:
                record['value'] = genisys_model._deserialize(record['value'])
        if fields:
            fields = set(fields)
            for record in records:
                for f in set(record) - fields:
                    del record[f]
        try:
            self.assertEquals(records, expected)
        except:
            import pprint
            pprint.pprint(records)
            raise


class SaveDashboardDeleteDashboardTestCase(BaseMongoTestCase):
    def test(self):
        self.storage.save_dashboard(
            username='u1', dashboard_name='db1',
            description='db1 description',
            charts=[
                {'signal': 'signal1', 'selector': 'selector1',
                 'filters': {'signal2': 'v2', 'signal3': 'v3'}},
                {'signal': 'signal2',
                 'selector': 'genisys@skynet.version/all_search',
                 'filters': {}},
                {'signal': 'signal3', 'selector': None, 'filters': {}},
            ]
        )
        [dashboard] = self.database.dashboard.find({}, {'_id': 0})
        self.assertEquals(dashboard, {
            'charts': [
                {'filters': {'signal2': 'v2', 'signal3': 'v3'},
                 'key': 'hash(\'["signal1", "selector1", {"signal2": "v2", '
                        '"signal3": "v3"}]\')',
                 'selector': 'selector1',
                 'selector_key': "hash('selector1')",
                 'selector_source': 'selector1',
                 'selector_vtype': 'selector',
                 'signal': 'signal1'},
                {'filters': {},
                 'key': 'hash(\'["signal2", "genisys@skynet.version/'
                        'all_search", {}]\')',
                 'selector': 'genisys@skynet.version/all_search',
                 'selector_key': "hash('genisys@skynet.version/all_search')",
                 'selector_source': {'path': 'skynet.version',
                                     'rule': 'all_search'},
                 'selector_vtype': 'genisys_selector',
                 'signal': 'signal2'},
                {'filters': {},
                 'key': 'hash(\'["signal3", null, {}]\')',
                 'selector': None,
                 'selector_key': None,
                 'selector_source': None,
                 'selector_vtype': None,
                 'signal': 'signal3'}
            ],
            'creator': 'u1',
            'desc': 'db1 description',
            'mtime': 1444000001,
            'name': 'db1'
        })

        self.assert_volatiles(vtype='dashboard', expected=[{
            'atime': 1444000001,
            'ctime': 1444000001,
            'ecount': 0,
            'etime': 1444000001,
            'key': "hash(('u1', 'db1'))",
            'last_status': 'new',
            'lock_id': None,
            'locked': False,
            'mcount': 0,
            'meta': {},
            'mtime': None,
            'pcount': 0,
            'proclog': [],
            'raw_key': ['u1', 'db1'],
            'source': {'charts': dashboard['charts']},
            'tcount': 0,
            'ttime': None,
            'ucount': 0,
            'utime': None,
            'value': None,
            'vtype': 'dashboard'
        }])

        self.assert_volatiles(vtype='selector', expected=[{
            'atime': 1444000001,
            'ctime': 1444000001,
            'ecount': 0,
            'etime': 1444000001,
            'key': "hash('selector1')",
            'last_status': 'new',
            'lock_id': None,
            'locked': False,
            'mcount': 0,
            'meta': {},
            'mtime': None,
            'pcount': 0,
            'proclog': [],
            'raw_key': 'selector1',
            'source': 'selector1',
            'tcount': 0,
            'ttime': None,
            'ucount': 0,
            'utime': None,
            'value': None,
            'vtype': 'selector'
        }])

        self.assert_volatiles(vtype='genisys_selector', expected=[{
            'atime': 1444000001,
            'ctime': 1444000001,
            'ecount': 0,
            'etime': 1444000001,
            'key': "hash('genisys@skynet.version/all_search')",
            'last_status': 'new',
            'lock_id': None,
            'locked': False,
            'mcount': 0,
            'meta': {},
            'mtime': None,
            'pcount': 0,
            'proclog': [],
            'raw_key': 'genisys@skynet.version/all_search',
            'source': {'path': 'skynet.version', 'rule': 'all_search'},
            'tcount': 0,
            'ttime': None,
            'ucount': 0,
            'utime': None,
            'value': None,
            'vtype': 'genisys_selector'
        }])

        self.storage.save_dashboard(
            username='u1', dashboard_name='db1',
            description='db1 description2',
            charts=[
                {'signal': 'signal2',
                 'selector': 'genisys@skynet.version/all_search',
                 'filters': {}},
            ]
        )
        [dashboard] = self.database.dashboard.find({}, {'_id': 0})
        self.assertEquals(dashboard, {
            'charts': [
                {'filters': {},
                 'key': 'hash(\'["signal2", "genisys@skynet.version/'
                        'all_search", {}]\')',
                 'selector': 'genisys@skynet.version/all_search',
                 'selector_key': "hash('genisys@skynet.version/all_search')",
                 'selector_source': {'path': 'skynet.version',
                                     'rule': 'all_search'},
                 'selector_vtype': 'genisys_selector',
                 'signal': 'signal2'},
            ],
            'creator': 'u1',
            'desc': 'db1 description2',
            'mtime': 1444000002,
            'name': 'db1'
        })

        self.assertEquals(self.storage.delete_dashboard('u1', 'db1'), True)
        self.assertEquals(self.database.dashboard.find().count(), 0)
        self.assert_volatiles(vtype='dashboard', expected=[])

    def test_selector_as_empty_string(self):
        self.storage.save_dashboard(
            username='u1', dashboard_name='db1',
            description='db1 description',
            charts=[{'signal': 'signal1', 'selector': '', 'filters': {}}]
        )
        [dashboard] = self.database.dashboard.find({}, {'_id': 0})
        self.assertEquals(dashboard, {
            'charts': [
                {'filters': {},
                 'key': 'hash(\'["signal1", null, {}]\')',
                 'selector': None,
                 'selector_key': None,
                 'selector_source': None,
                 'selector_vtype': None,
                 'signal': 'signal1'}
            ],
            'creator': 'u1',
            'desc': 'db1 description',
            'mtime': 1444000001,
            'name': 'db1'
        })

        self.assert_volatiles(vtype='selector', expected=[])


class ListDashboardsGetDashboardTestCase(BaseMongoTestCase):
    def test(self):
        self.storage.save_dashboard(
            username='u1', dashboard_name='db2',
            description='db2 description',
            charts=[
                {'signal': 'signal3', 'selector': None, 'filters': {}},
            ]
        )
        self.storage.save_dashboard(
            username='u2', dashboard_name='db1',
            description='db1 description',
            charts=[
                {'signal': 'signal2',
                 'selector': 'genisys@skynet.version/all_search',
                 'filters': {}},
                {'signal': 'signal3', 'selector': None, 'filters': {}},
            ]
        )
        self.storage.save_dashboard(
            username='u1', dashboard_name='db1',
            description='db1 description',
            charts=[
                {'signal': 'signal1', 'selector': 'selector1',
                 'filters': {'signal2': 'v2', 'signal3': 'v3'}},
                {'signal': 'signal3', 'selector': None, 'filters': {}},
            ]
        )

        db11 = self.storage.get_dashboard('u1', 'db1')
        self.assertEquals(db11, {
            'charts': [{'filters': {'signal2': 'v2', 'signal3': 'v3'},
                        'key': 'hash(\'["signal1", "selector1", '
                               '{"signal2": "v2", "signal3": "v3"}]\')',
                        'selector': 'selector1',
                        'selector_key': "hash('selector1')",
                        'selector_source': 'selector1',
                        'selector_vtype': 'selector',
                        'signal': 'signal1'},
                       {'filters': {},
                        'key': 'hash(\'["signal3", null, {}]\')',
                        'selector': None,
                        'selector_key': None,
                        'selector_source': None,
                        'selector_vtype': None,
                        'signal': 'signal3'}],
            'creator': 'u1',
            'desc': 'db1 description',
            'mtime': 1444000003,
            'name': 'db1'
        })
        db12 = self.storage.get_dashboard('u1', 'db2')
        self.assertEquals(db12, {
            'charts': [{'filters': {},
                        'key': 'hash(\'["signal3", null, {}]\')',
                        'selector': None,
                        'selector_key': None,
                        'selector_source': None,
                        'selector_vtype': None,
                        'signal': 'signal3'}],
            'creator': 'u1',
            'desc': 'db2 description',
            'mtime': 1444000001,
            'name': 'db2'
        })

        self.assertEquals(self.storage.list_dashboards('u1'), [db11, db12])

        db21 = self.storage.get_dashboard('u2', 'db1')
        self.assertEquals(db21, {
            'charts': [
                {'filters': {},
                 'key': 'hash(\'["signal2", "genisys@skynet.version/'
                        'all_search", {}]\')',
                 'selector': 'genisys@skynet.version/all_search',
                 'selector_key': "hash('genisys@skynet.version/all_search')",
                 'selector_source': {'path': 'skynet.version',
                                     'rule': 'all_search'},
                 'selector_vtype': 'genisys_selector',
                 'signal': 'signal2'},
                {'filters': {},
                 'key': 'hash(\'["signal3", null, {}]\')',
                 'selector': None,
                 'selector_key': None,
                 'selector_source': None,
                 'selector_vtype': None,
                 'signal': 'signal3'}],
            'creator': 'u2',
            'desc': 'db1 description',
            'mtime': 1444000002,
            'name': 'db1'
        })

        self.assertEquals(self.storage.list_dashboards('u2'), [db21])

        self.assertEquals(self.storage.list_dashboards('u5'), [])
        self.assertEquals(self.storage.get_dashboard('u1', 'db10'), None)
