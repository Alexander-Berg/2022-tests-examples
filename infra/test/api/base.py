import unittest

import mock

from pymongo import MongoClient

from genisys.web import model

from .fixture import get_fixture


class ApiTestCase(unittest.TestCase):
    maxDiff = None
    testdb_host = 'localhost'
    testdb_name = 'genisys_api_unittest'

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def setUp(self):
        super(ApiTestCase, self).setUp()
        client = MongoClient(self.testdb_host)
        self.database = client[self.testdb_name]
        self.storage = model.MongoStorage(client, self.testdb_name)
        self.clear_db()
        self.setup_mocks()
        self.storage.init_db(['user1'])
        self.database.section.remove()
        self.database.volatile.remove()
        self.setup_fixture()

    def setup_fixture(self):
        for collection, document in get_fixture():
            self.database[collection].insert(document)

    def setup_mocks(self):
        self.mock_ts = mock.patch.object(self.storage, '_get_ts')
        self.mock_ts_return_value = 1444000000
        def fake_get_ts():
            self.mock_ts_return_value += 1
            return self.mock_ts_return_value
        self.mock_ts.start().side_effect = fake_get_ts

        def fake_hash(val):
            return "hash(%r)" % (val, )
        self.mock_hash = mock.patch.object(model, 'volatile_key_hash')
        self.mock_hash.start().side_effect = fake_hash

    def tearDown(self):
        super(ApiTestCase, self).tearDown()
        self.clear_db()
        self.mock_ts.stop()
        self.mock_hash.stop()
