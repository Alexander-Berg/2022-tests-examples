# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six
import mock
import unittest

from saas.library.python.token_store import OauthTokenMeta


class TestOauthTokenMeta(unittest.TestCase):

    def test_no_oauth_slug(self):

        class C(six.with_metaclass(OauthTokenMeta)):
            pass

        with self.assertRaises(RuntimeError):
            C()

    @mock.patch('saas.library.python.token_store.PersistentTokenStore.get_token_from_store_env_or_file', return_value='test_token')
    def test_token_added(self, m):

        class ClassTest(six.with_metaclass(OauthTokenMeta)):
            _OAUTH_SLUG = 'test'

        test_token = 'test_token'

        class_test_instance = ClassTest()
        self.assertEqual(class_test_instance._OAUTH_TOKEN, test_token)
        self.assertEqual(ClassTest._OAUTH_TOKEN, test_token)

        other_test_instance = ClassTest()
        self.assertEqual(other_test_instance._OAUTH_TOKEN, test_token)
        self.assertEqual(ClassTest._OAUTH_TOKEN, test_token)

        m.assert_called_once_with('test')

    @mock.patch('saas.library.python.token_store.PersistentTokenStore.get_token_from_store_env_or_file', return_value='wrong_token')
    def test_existing_token_intact(self, m):
        test_token = 'a_test_token_a'

        class ClassWithToken(six.with_metaclass(OauthTokenMeta)):
            _OAUTH_TOKEN = test_token

        class_test_instance = ClassWithToken()
        self.assertEqual(class_test_instance._OAUTH_TOKEN, test_token)
        self.assertEqual(ClassWithToken._OAUTH_TOKEN, test_token)

        m.assert_not_called()
