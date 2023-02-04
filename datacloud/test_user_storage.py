# -*- coding: utf-8 -*-
import unittest
from datacloud.score_api.storage.users.generic import UserStorage, User
from datacloud.score_api.storage.users.simple import SimpleUserStorage


class TestUserStorage(unittest.TestCase):
    def test_simple_user_storage(self):
        users = {'token1': 'user1', 'token2': 'user2'}
        simple_storage = SimpleUserStorage(users)
        self.assertEqual(simple_storage.get_user_by_token('token1'), User('user1'))
        self.assertEqual(simple_storage.get_user_by_token('token2'), User('user2'))
        self.assertIsNone(simple_storage.get_user_by_token('unknown-token'))

    def test_generic_storage(self):
        generic_storage = UserStorage()
        with self.assertRaises(NotImplementedError):
            generic_storage.get_user_by_token('sample')

    def test_user_creation(self):
        user = User('sample_id')
        self.assertEqual(user.id, 'sample_id')
