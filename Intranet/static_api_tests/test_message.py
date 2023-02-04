# coding: utf-8
from __future__ import unicode_literals

from static_api import message

from .base import MongoTestCase


def _m(id_, handled=True):
    return {'_id': id_, 'time':'', 'handled': handled}


class MessageTest(MongoTestCase):
    def test_last_message(self):
        manager = message.manager

        manager.collection._collection.insert([
            _m(1),
            _m(2),
            _m(3),
            _m(4, handled=False),
        ])

        last_message = manager.get_last_message()

        self.assertEqual(last_message['_id'], 3)

    def test_unhandled_messages(self):
        manager = message.manager

        manager.collection._collection.insert([
            _m(1),
            _m(2),
            _m(4, handled=False),
            _m(7, handled=False),
            _m(11, handled=False),
        ])

        missing = list(manager.get_unhandled_messages(10))

        self.assertEqual(missing, [message.MissingMessageRange((3,)),
                                   message.MessageRange([_m(4)]),
                                   message.MissingMessageRange((5, 6)),
                                   message.MessageRange([_m(7)]),
                                   message.MissingMessageRange((8, 10)),
                                   message.MessageRange([_m(11)]),
                                   message.MissingMessageRange((12, 12))])

    def test_unhandled_messages_empty(self):
        missing = list(message.manager.get_unhandled_messages(10))

        self.assertEqual(missing, [message.MissingMessageRange((1, 10))])

    def test_unhandled_messages_before_first_id(self):
        manager = message.manager
        manager.collection._collection.insert([
            _m(15, handled=False),
        ])

        missing = list(manager.get_unhandled_messages(10))

        self.assertEqual(missing, [message.MissingMessageRange((1, 10))])

    def test_unhandled_messages_after(self):
        manager = message.manager

        manager.collection._collection.insert([
            _m(15),
        ])

        missing = list(manager.get_unhandled_messages(10))

        self.assertEqual(missing, [message.MissingMessageRange((16, 25))])
