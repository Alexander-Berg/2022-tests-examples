import os

import mock
from pymongo import MongoClient

from lacmus2.web import api
from lacmus2.web import model

from ..redis_test import BaseRedisTestCase
from . import config as test_config


class ApiTestCase(BaseRedisTestCase):
    maxDiff = None
    testdb_uri = test_config.MONGODB_URI
    testdb_name = test_config.MONGODB_DB_NAME
    MONGO_CONFIG = {'HISTORICAL_SIZE': 4096}

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def setUp(self):
        super(ApiTestCase, self).setUp()
        client = MongoClient(self.testdb_uri)
        self.database = client[self.testdb_name]
        self.storage = model.Lacmus2MongoStorage(client, self.testdb_name,
                                                 self.MONGO_CONFIG)
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
        self.mock_hash = mock.patch('genisys.web.model.volatile_key_hash')
        self.mock_hash.start().side_effect = fake_hash

    def tearDown(self):
        super(ApiTestCase, self).tearDown()
        self.clear_db()
        self.mock_ts.stop()
        self.mock_hash.stop()


class ApiWebTestCase(ApiTestCase):
    CONFIG = {}

    def setUp(self):
        super(ApiWebTestCase, self).setUp()
        os.environ['LACMUS2_API_CONFIG'] = 'test/api/config.py'
        with mock.patch('socket.gethostname') as mock_gethostname:
            mock_gethostname.return_value = 'testhost'
            self.app = api.make_app()
        self.config = self.app.config
        self.config.update(self.CONFIG)
        self.client = self.app.test_client()

    def tearDown(self):
        self.app.cleanup()
        super(ApiWebTestCase, self).tearDown()
