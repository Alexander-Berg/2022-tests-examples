# -*- coding: utf-8 -*-
import sys
import pickle

from unittest.mock import Mock

from intranet.yandex_directory.src.yandex_directory.common.cache import (
    SimpleCache,
    UwsgiCache,
)

from hamcrest import (
    assert_that,
    equal_to,
)

from testutils import TestCase


class TestSimpleCache(TestCase):
    def setUp(self, *args, **kwargs):
        self.cache = SimpleCache()
        super(TestSimpleCache, self).setUp(*args, **kwargs)

    def test_cache_should_set_and_return_value(self):
        test_key = 'test_key'
        test_value = 'test_value'
        self.cache.set(test_key, test_value)

        assert_that(self.cache.get(test_key), equal_to(test_value))
        assert_that(self.cache.get('unknown_key'), equal_to(None))


class TestUwsgiCache(TestCase):
    def setUp(self, *args, **kwargs):
        # мокаем импорт uwsgi модуля, т.к. его нет в тестах
        sys.modules['uwsgi'] = Mock()

        self.cache = UwsgiCache()

        super(TestUwsgiCache, self).setUp(*args, **kwargs)

    def test_cache_set(self):
        test_key = 'test_key'
        test_value = 'test_value'
        self.cache.set(test_key, test_value)

        self.cache.uwsgi.cache_set.assert_called_once_with(self.cache._generate_key(test_key), pickle.dumps(test_value))

    def test_cache_get(self):
        test_key = 'test_key'
        self.cache.get(test_key)

        self.cache.uwsgi.cache_get.assert_called_once_with(self.cache._generate_key(test_key))

    def test_generate_cache_key(self):
        test_key = 'some_key'
        key = self.cache._generate_key(test_key)
        assert_that(key, equal_to('%s_%s' % (self.cache._cache_key_prefix, test_key)))
