# coding: utf-8
import unittest

import mongomock

from emission.mongodb.storage import ClientMongoDBStorage


class ClientMongoDBStorageTest(unittest.TestCase):
    def setUp(self):
        self.collection = mongomock.MongoClient().db.collection
        self.storage = ClientMongoDBStorage(self.collection, 'test')

    def test_get_one(self):
        self.collection.insert_one({'msg_id': 345, 'master_name': 'test', 'data': 'test data'})

        entry = self.storage.get_one(345)

        self.assertEqual(entry['msg_id'], 345)
        self.assertEqual(entry['data'], 'test data')

    def test_insert(self):
        self.storage.insert(1, 'test data', 'modify')

        entry = self.collection.find_one({'msg_id': 1})

        self.assertEqual(entry['msg_id'], 1)
        self.assertEqual(entry['data'], 'test data')

        self.storage.insert(1, 'test data new', 'modify')

        entry = self.collection.find_one({'msg_id': 1})

        self.assertEqual(entry['data'], 'test data new')

    def test_get_slice(self):
        for i in range(10):
            self.collection.insert_one({'msg_id': i, 'master_name': 'test', 'data': 'test data %d' % i})

        iterator = self.storage.get_slice(3, 5)

        self.assertEqual([e['msg_id'] for e in iterator], range(3, 6))

    def test_delete_one(self):
        self.collection.insert_one({'msg_id': 345, 'master_name': 'test', 'data': 'test data'})

        self.storage.delete_one(345)

        self.assertEqual(self.collection.count(), 0)
