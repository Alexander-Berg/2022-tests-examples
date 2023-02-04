from datetime import timedelta

import mock
from pymongo import errors
from tools_mongodb_cache import MongoDBCache
from tools_mongodb_cache.cache import _ensured_ttl_indexes

from wiki.utils import timezone as datetime
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class MongoCacheTest(WikiDjangoTestCase):
    def setUp(self):
        self.mock_storage = {}
        self.mock_mongo = mock.MagicMock()

        # mock mongo cache storage
        _ensured_ttl_indexes.add('fake')  # хак, чтобы не вызывалось навешивание TTL индексов

        self.cache = MongoDBCache(
            'mongodb://fake_location/fake_db',
            {
                'collection': 'fake',
                'TIMEOUT': 10,
                'OPTIONS': {
                    'WRITE_CONCERN': 0,
                },
            },
            mongodb=self.mock_mongo,
        )

        # mock mongo methods
        mock_collection = mock.MagicMock()
        mock_collection.update = lambda key, data, upsert, w: self.mock_storage.update({key['key']: data})
        mock_collection.find_one = lambda key: self.mock_storage.get(key['key'], None)

        self.cache.collection = lambda: mock_collection

    def tearDown(self):
        self.cache = None
        self.mock_storage = {}

    def test_custom_timeout_set(self):
        """
        test setting record with custom mongo cache timeout
        """
        self.cache.set('test_key', value='nyan-nyan-cat-to-the-sky', timeout=5)

        self.assertEqual(self.mock_storage['test_key']['record_timeout'], 5)

    def test_custom_timeout_get(self):
        """
        test getting record with custom mongo cache timeout
        """
        # record is too old
        now = datetime.now() - timedelta(seconds=15)
        self.mock_storage.update(
            {'test_key': {'value': 'nyan-nyan-cat-to-the-sky', 'status': now, 'record_timeout': 5}}
        )

        self.assertEqual(self.cache.get('test_key', default='default'), 'default')

        # all green
        now = datetime.now()
        self.mock_storage.update(
            {'test_key': {'value': 'nyan-nyan-cat-to-the-sky', 'status': now, 'record_timeout': 5}}
        )

        self.assertEqual(self.cache.get('test_key', default='default'), 'nyan-nyan-cat-to-the-sky')

        # record timeout in storage is greater than collection timeout, using collection timeout
        now = datetime.now() - timedelta(seconds=15)
        self.mock_storage.update(
            {'test_key': {'value': 'nyan-nyan-cat-to-the-sky', 'status': now, 'record_timeout': 20}}
        )

        self.assertEqual(self.cache.get('test_key', default='default'), 'default')

    def test_get_default_when_mongodb_is_dead(self):
        self.cache.set('key', 'hello')
        self.assertEqual(self.cache.get('key', 'nothing'), 'hello')
        with mock.patch(target='tools_mongodb_cache.MongoDBCache.give_mongodb_chance', new=False):
            self.assertEqual(self.cache.get('key', 'nothing'), 'nothing')

    def test_give_mongodb_chance(self):
        self.cache.mongodb_is_dead_since['dead_since'] = datetime.now()
        self.assertFalse(self.cache.give_mongodb_chance)

        self.cache.mongodb_is_dead_since['dead_since'] = datetime.now() - timedelta(seconds=2)
        self.assertTrue(self.cache.give_mongodb_chance)
        self.assertTrue(self.cache.give_mongodb_chance)
        self.assertEqual(self.cache.mongodb_is_dead_since, {})

    def test_hit_mongo_when_alive(self):
        with mock.patch(target='tools_mongodb_cache.MongoDBCache.give_mongodb_chance', new=True):

            class RaisedException(Exception):
                pass

            def raise_when_called(*args, **kwargs):
                raise RaisedException()

            with mock.patch(target='tools_mongodb_cache.MongoDBCache.read_from_mongo', new=raise_when_called):
                self.assertRaises(RaisedException, self.cache.get, 'some key')

            with mock.patch(target='tools_mongodb_cache.MongoDBCache.write_in_mongo', new=raise_when_called):
                self.assertRaises(RaisedException, self.cache.set, 'some key', 'some value')

    def test_dont_hit_mongo_when_dead(self):
        with mock.patch(target='tools_mongodb_cache.MongoDBCache.give_mongodb_chance', new=False):

            class RaisedException(Exception):
                pass

            def raise_when_called(*args, **kwargs):
                raise RaisedException()

            with mock.patch(target='tools_mongodb_cache.MongoDBCache.read_from_mongo', new=raise_when_called):
                try:
                    self.cache.get('some key')
                except RaisedException:
                    raise AssertionError('mongo was hit during the test')

            with mock.patch(target='tools_mongodb_cache.MongoDBCache.write_in_mongo', new=raise_when_called):
                try:
                    self.cache.set('some key', 'some value')
                except RaisedException:
                    raise AssertionError('mongo was hit during the test')

    def test_mongodb_becomes_dead_on_pymongo_errors(self):
        def raise_when_called(*args, **kwargs):
            raise errors.PyMongoError()

        with mock.patch(target='tools_mongodb_cache.MongoDBCache.read_from_mongo', new=raise_when_called):
            self.cache.get('some key')
            self.assertFalse(self.cache.give_mongodb_chance)

        with mock.patch(target='tools_mongodb_cache.MongoDBCache.write_in_mongo', new=raise_when_called):
            self.cache.set('some key', 'some value')
            self.assertFalse(self.cache.give_mongodb_chance)
