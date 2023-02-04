# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import os
import unittest

from saas.library.python.token_store import TokenStore


class TestTokenStore(unittest.TestCase):
    test_tokens = {
        'TEST_SERVICE_OAUTH': 'test_token',
        'OAUTH_T_TEST_SERVICE': 't_test_token',
        'YT_TOKEN': 'yt_token',
        'OAUTH_STATBOX': 'statbox_token',
        'MONITORADO_OAUTH_TOKEN': 'monitorado_token'
    }

    def setUp(self):
        os.environ.update(self.test_tokens)
        TokenStore._evn_loaded = False
        TokenStore._tokens = {}

    def tearDown(self):
        for k in self.test_tokens.keys():
            os.environ.pop(k)
        TokenStore._evn_loaded = False
        TokenStore._tokens = {}

    def test_token_store_can_store_token(self):
        test_service = 'test_service'
        test_token = 'test_token'
        token_store = TokenStore
        token_store.add_token(test_service, test_token)
        self.assertEqual(test_token, TokenStore.get_token(test_service))

    def test_get_token_from_env(self):
        self.assertEqual(TokenStore.get_token_from_store_or_env('test_service'), 'test_token')
        # token should be already loaded
        self.assertEqual(TokenStore.get_token('t_test_service'), 't_test_token')
        self.assertEqual(TokenStore.get_token('yt'), 'yt_token')
        self.assertEqual(TokenStore.get_token('statbox'), 'statbox_token')
        self.assertEqual(TokenStore.get_token('monitorado'), 'monitorado_token')
