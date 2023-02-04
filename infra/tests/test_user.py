from __future__ import absolute_import

import os
from unittest import main, skipIf, mock

import flask

from lib.users import StaffUsers, UserCache, User
from lib.groups import StaffGroups, AbcGroups
from common import FlaskTest


class TestUser(FlaskTest):
    def setUp(self):
        super().setUp()
        flask.current_app.OAUTH_YANDEX_TOKEN = os.getenv('STAFF_OAUTH')

    def tearDown(self):
        db = self.app.database
        db.session.execute("DELETE FROM users")
        super().tearDown()

    def test_cache_lookup(self):
        db = self.app.database
        db.session.execute("INSERT INTO users VALUES ('torkve', 'test@ya.ru')")
        db.session.flush()
        db.session.commit()

        cache = UserCache(self.app.database_pool, log=mock.MagicMock())
        self.assertIsNone(cache.find_user('kekeke'))
        user = cache.find_user("torkve")
        self.assertIsInstance(user, User)
        self.assertEqual(user.get_address_for("email"), {"email": "test@ya.ru"})

    @skipIf(not os.getenv('STAFF_OAUTH'), 'staff oauth is not available')
    def test_staff_lookup(self):
        source = StaffUsers.create({'oauth': flask.current_app.OAUTH_YANDEX_TOKEN}, log=mock.MagicMock())
        self.assertIsInstance(source, StaffUsers)

        user = source.find_user('torkve')
        self.assertIsInstance(user, User)

        self.assertEqual(user.get_address_for('telegram'), {'nickname': 'the_ook', 'login': 'torkve'})
        self.assertIn(user.get_address_for('sms'), ('+7 (905) 559-82-38', '+7 905 559-82-38'))
        self.assertIn(user.get_address_for('voice'), ('+7 (905) 559-82-38', '+7 905 559-82-38'))
        self.assertEqual(user.get_address_for('email'),
                         {'email': 'torkve@yandex-team.ru', 'first_name': 'Vsevolod', 'last_name': 'Velichko'})

    @skipIf(not os.getenv('STAFF_OAUTH'), 'staff oauth is not available')
    def test_staff_groups_resolution(self):
        source = StaffGroups.create({'oauth': flask.current_app.OAUTH_YANDEX_TOKEN}, log=mock.MagicMock())
        self.assertIsInstance(source, StaffGroups)
        users = source.find_users('yandex')
        self.assertTrue(users)
        self.assertGreater(len(users), 1000)

    @skipIf(not os.getenv('STAFF_OAUTH'), 'staff oauth is not available')
    def test_abc_groups_resolution(self):
        source = AbcGroups.create({'oauth': flask.current_app.OAUTH_YANDEX_TOKEN}, log=mock.MagicMock())
        self.assertIsInstance(source, AbcGroups)
        users = source.find_users('svc_drawio')
        self.assertEqual(users, {'torkve'})
        users = source.find_users('svc_drawio_services_management')
        self.assertEqual(users, {'torkve'})
        users = source.find_users('2035')
        self.assertEqual(users, {'torkve'})


if __name__ == '__main__':
    main()
