# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import os
import mock
import unittest
from pathlib2 import Path

from saas.library.python.token_store import TokenStore, PersistentTokenStore


class TestPersistentTokenStore(unittest.TestCase):
    test_tokens = {
        'TEST_SERVICE_OAUTH': 'test_token',
        'OAUTH_T_TEST_SERVICE': 't_test_token',
        'ABC_TOKEN': 'fake_abc_token',
        'STARTREK_TOKEN': 'fake_startrek_token',
        'SANDBOX_TOKEN': 'fake_sandbox_token'
    }

    def setUp(self):
        os.environ.update(self.test_tokens)
        TokenStore._evn_loaded = False
        TokenStore._tokens = {}
        PersistentTokenStore.WELL_KNOWN_TOKEN_FILES['test_service'] = Path('~/.test_service')

    def tearDown(self):
        for k in self.test_tokens.keys():
            os.environ.pop(k)
        TokenStore._evn_loaded = False
        TokenStore._tokens = {}

    def test_get_token_from_env(self):
        self.assertEqual(TokenStore.get_token_from_store_or_env('test_service'), 'test_token')
        # token should be already loaded
        self.assertEqual(PersistentTokenStore.get_token('t_test_service'), 't_test_token')
        self.assertEqual(PersistentTokenStore.get_token_from_store_env_or_file('abc'), 'fake_abc_token')
        self.assertEqual(PersistentTokenStore.get_token_from_store_env_or_file('startrek'), 'fake_startrek_token')
        self.assertEqual(PersistentTokenStore.get_token_from_store_env_or_file('sandbox'), 'fake_sandbox_token')

    def test_load_token_from_file(self):
        opener = mock.mock_open(read_data='test_token')

        def mocked_open(self, *args, **kwargs):
            return opener(self, *args, **kwargs)

        with mock.patch.object(Path, 'open', mocked_open):
            PersistentTokenStore.load_token_from_file('yt')

        self.assertEqual(TokenStore.get_token('yt'), 'test_token')
        opener.assert_called_once_with(Path.home().joinpath('.yt/token'))
