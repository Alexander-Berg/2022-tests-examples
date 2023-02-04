# -*- coding: utf-8 -*-
import unittest
from datacloud.score_api.storage.cookie_sync.simple import SimpleCookieSyncSet


class TestSimpleCookieSyncSet(unittest.TestCase):
    def test_empty_creation(self):
        sync_set = SimpleCookieSyncSet([])
        self.assertEqual(sync_set._vendors, frozenset())

    def test_creation(self):
        sync_set = SimpleCookieSyncSet(['one', 'two', 'one', 'three', 'three'])
        self.assertEqual(sync_set._vendors, frozenset(['one', 'two', 'three']))

    def test_if_cookie_support_sync(self):
        key = 'one'
        cookie = {'cookie_vendor': key, 'cookie': '1'}
        sync_set = SimpleCookieSyncSet([key, 'two', 'three'])
        self.assertTrue(sync_set.is_sync_cookie(cookie))

    def test_if_cookie_not_support_sync(self):
        key = 'one'
        cookie = {'cookie_vendor': key, 'cookie': '1'}
        sync_set = SimpleCookieSyncSet(['two', 'three', 'four'])
        self.assertFalse(sync_set.is_sync_cookie(cookie))

    def test_multiple_cookies_sync(self):
        cookies = [
            {'cookie_vendor': 'one', 'cookie': 1},
            {'cookie_vendor': 'two', 'cookie': 2},
            {'cookie_vendor': 'four', 'cookie': 3}]
        expected_result = [
            {'cookie_vendor': 'one', 'cookie': 1},
            {'cookie_vendor': 'four', 'cookie': 3}
        ]
        sync_set = SimpleCookieSyncSet(['one', 'three', 'four', 'five'])
        self.assertEqual(sync_set.check_multiple_cookies_for_sync(cookies), expected_result)


if __name__ == '__main__':
    unittest.main()
