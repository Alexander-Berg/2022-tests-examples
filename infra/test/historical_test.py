import unittest

import mock
from pymongo import MongoClient

from lacmus2.historical import Lacmus2HistoricalStorage


class BaseHistoricalTestCase(unittest.TestCase):
    testdb_host = 'localhost'
    CONFIG = {
        'HISTORICAL_SIZE': 4096,
        'HISTORICAL_DB_NAME': 'lacmus2_historical_unittest',
        'HISTORICAL_MAX_POINTS': 6,
    }

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def setUp(self):
        super(BaseHistoricalTestCase, self).setUp()
        client = MongoClient(self.testdb_host)
        self.database = client[self.CONFIG['HISTORICAL_DB_NAME']]
        self.hstorage = Lacmus2HistoricalStorage(client, self.CONFIG)
        self.clear_db()
        self.setup_mocks()

    def setup_mocks(self):
        self.mock_ts = mock.patch.object(self.hstorage, '_get_ts')
        self.mock_ts_return_value = 144000

        def fake_get_ts():
            self.mock_ts_return_value += 1
            return self.mock_ts_return_value
        self.mock_ts.start().side_effect = fake_get_ts

    def tearDown(self):
        super(BaseHistoricalTestCase, self).tearDown()
        self.clear_db()
        self.mock_ts.stop()


class HistoricalTest(BaseHistoricalTestCase):
    def test_write_point(self):
        self.hstorage.write_point('ckey1', {'foo': 1, 'bar': 20, 'b.z': 30})
        self.hstorage.write_point('ckey1', {'foo': 2, 'bar': 20, 'b.z': 30})
        self.hstorage.write_point('ckey1', {'foo': 1, 'b.z': 30})
        self.hstorage.write_point('ckey1', {'foo': 3, 'b.z': 50})
        points = list(self.database.historical.find({}, {'_id': False}))
        self.assertEquals(points, [
            {'time': 144001, 'chart': 'ckey1',
             'values': [['b.z', 30], ['bar', 20], ['foo', 1]]},
            {'time': 144002, 'chart': 'ckey1',
             'values': [['b.z', 30], ['bar', 20], ['foo', 2]]},
            {'time': 144003, 'chart': 'ckey1',
             'values': [['b.z', 30], ['foo', 1]]},
            {'time': 144004, 'chart': 'ckey1',
             'values': [['b.z', 50], ['foo', 3]]}
        ])

    def test_get_graph(self):
        self.hstorage.write_point('ckey1', {'foo': 1, 'bar': 20, 'b.z': 30})
        self.hstorage.write_point('ckey1', {'foo': 2, 'bar': 20, 'b.z': 30})
        self.hstorage.write_point('ckey1', {'foo': 1, 'b.z': 30})
        self.hstorage.write_point('ckey1', {'foo': 3, 'b.z': 50})
        self.hstorage.write_point('ckey1', {'foo': 4, 'b.z': 50})
        self.hstorage.write_point('ckey1', {'foo': 5, 'b.z': 50, 'bar': 1})
        self.hstorage.write_point('ckey1', {'foo': 6, 'b.z': 50})
        self.hstorage.write_point('ckey1', {'foo': 7, 'b.z': 50, '': 100})

        graph, last_time = self.hstorage.get_graph('ckey1')
        self.assertEquals(graph, [
            {'data': [{'x': 144008, 'y': 100}],
             'name': ''},
            {'data': [{'x': 144003, 'y': 30},
                      {'x': 144004, 'y': 50},
                      {'x': 144005, 'y': 50},
                      {'x': 144006, 'y': 50},
                      {'x': 144007, 'y': 50},
                      {'x': 144008, 'y': 50}],
             'name': 'b.z'},
            {'data': [{'x': 144006, 'y': 1}],
             'name': 'bar'},
            {'data': [{'x': 144003, 'y': 1},
                      {'x': 144004, 'y': 3},
                      {'x': 144005, 'y': 4},
                      {'x': 144006, 'y': 5},
                      {'x': 144007, 'y': 6},
                      {'x': 144008, 'y': 7}],
             'name': 'foo'}
        ])
        self.assertEquals(last_time, 144008)

        graph, last_time = self.hstorage.get_graph('ckey1', from_time=144008)
        self.assertEquals(graph, [])
        self.assertEquals(last_time, None)

        graph, last_time = self.hstorage.get_graph('ckey1', from_time=144005)
        self.assertEquals(graph, [
            {'data': [{'x': 144008, 'y': 100}],
             'name': ''},
            {'data': [{'x': 144006, 'y': 50},
                      {'x': 144007, 'y': 50},
                      {'x': 144008, 'y': 50}],
             'name': 'b.z'},
            {'data': [{'x': 144006, 'y': 1}],
             'name': 'bar'},
            {'data': [{'x': 144006, 'y': 5},
                      {'x': 144007, 'y': 6},
                      {'x': 144008, 'y': 7}],
             'name': 'foo'}
        ])
        self.assertEquals(last_time, 144008)
