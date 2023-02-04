from __future__ import absolute_import

import logging
from queue import Queue
from unittest import main, mock

from lib.users import UserCache
from lib.events import QloudSource, Event
from lib.notifiers.abstract import Notifier
from lib.subscriptions.abstract import Subscriptions
from lib.context import Context

from common import FlaskTest


class MockNotifier(Notifier):
    def __init__(self):
        self.notifications = []

    def name(self):
        return 'Mock'

    def key(self):
        return 'email'

    @classmethod
    def create(cls, params):
        return cls()

    def read_user_address(self, data):
        return data

    def notify_user(self, address, message, **options):
        self.notifications.append((address, message, options))


class MockSubscriptions(Subscriptions):
    def name(self):
        return 'Mock'

    def key(self):
        return 'mock'

    @classmethod
    def create(cls, params):
        return cls()

    def subscribers_for(self, tags):
        return {tag: {} for tag in tags}, {}

    def settings_for(self, login):
        return {}


class TestDispatching(FlaskTest):
    def setUp(self):
        super().setUp()

        db = self.app.database
        db.session.execute(
            self.users_table.insert().values(login="vasya", email="vasya@ya.ru"),
        )
        db.session.execute(
            self.users_table.insert().values(login="petya", email="petya@ya.ru"),
        )
        db.session.execute(
            self.users_table.insert().values(login="kolya", email="kolya@ya.ru"),
        )
        db.session.execute(
            self.users_table.insert().values(login="olya", email="olya@ya.ru"),
        )
        db.session.flush()
        db.session.commit()
        self.cache = UserCache(self.app.database_pool, log=logging.getLogger('users.cache'))
        self.notifier = MockNotifier()
        self.event_source = QloudSource()

        self.ctx = Context()
        self.ctx.event_sources.append(self.event_source)
        self.ctx.notifiers.append(self.notifier.key())
        self.ctx.user_sources.append(self.cache)
        self.ctx.subscriptions_source = MockSubscriptions()
        self.ctx.queue = Queue()
        self.ctx.queue.stop = lambda: None

    def tearDown(self):
        db = self.app.database
        db.session.execute(
            self.users_table.delete()
        )
        db.session.flush()
        db.session.commit()
        super().tearDown()

    def test_dispatch(self):
        event = Event(
            message='failfailfail',
            params={'subject': 'FAILFAILFAIL'},
            tags=['vasya', 'kolya'],
            source=self.event_source,
            forced_users=['olya'],
        )

        self.ctx.dispatch_event(event)
        self.assertEqual(3, self.ctx.queue.qsize())
        notifications = [self.ctx.queue.get_nowait() for _ in range(3)]
        self.assertIn(
            {'type': self.notifier.key(),
             'address': {'email': 'vasya@ya.ru'},
             'attempt': 0,
             'message': 'failfailfail',
             'options': {'subject': 'FAILFAILFAIL'}},
            notifications
        )
        self.assertIn(
            {'type': self.notifier.key(),
             'address': {'email': 'kolya@ya.ru'},
             'attempt': 0,
             'message': 'failfailfail',
             'options': {'subject': 'FAILFAILFAIL'}},
            notifications
        )
        self.assertIn(
            {'type': self.notifier.key(),
             'address': {'email': 'olya@ya.ru'},
             'attempt': 0,
             'message': 'failfailfail',
             'options': {'subject': 'FAILFAILFAIL'}},
            notifications
        )
        self.assertEqual(0, self.ctx.queue.qsize())


if __name__ == '__main__':
    main()
