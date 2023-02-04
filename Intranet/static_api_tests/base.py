# coding: utf-8

import unittest

from static_api import storage


storage.manager._init('test', force=True)


class MongoTestCase(unittest.TestCase):
    def setUp(self):
        db = storage.manager.db

        for name in db.list_collection_names():
            if not name.startswith('system.'):
                db[name].delete_many({})
