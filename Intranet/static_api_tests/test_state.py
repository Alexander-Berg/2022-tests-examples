# coding: utf-8
from __future__ import unicode_literals

from static_api import storage

from .base import MongoTestCase


class StateTest(MongoTestCase):
    def setUp(self):
        super(StateTest, self).setUp()

        self.manager = storage.manager.get_state_manager()

    def test_set_init(self):
        new_state = self.manager.set_state('init')

        self.assertEqual(new_state['prefixes']['read'], '')
        self.assertTrue(len(new_state['prefixes']['write']))

    def test_set_update(self):
        init_state = self.manager.set_state('init')
        update_state = self.manager.set_state('update')

        self.assertEqual(init_state['prefixes']['write'], update_state['prefixes']['read'])
        self.assertEqual(update_state['prefixes']['write'], update_state['prefixes']['read'])
