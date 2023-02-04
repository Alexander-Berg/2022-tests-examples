# coding: utf-8
from __future__ import absolute_import, unicode_literals

import mock

from uhura.management.commands.import_staff_users import Command, staff
from uhura.models import User


class StaffIteratorMock(object):

    def __init__(self):
        self.N = 0
        self.pages = self.get_pages()

    def get_pages(self):
        people = [
            {
                'id': 1,
                'uid': 1,
                'login': 'login1',
                'phones': [{'number': '88005553535', 'is_main': True}],
                'accounts': [{'type': 'telegram', 'value': 'prosche_pozvonit'}],
                'official': {'is_dismissed': False, 'quit_at': None},
                'work_email': 'email',
            },
            {
                'id': 2,
                'uid': 2,
                'login': 'login2',
                'phones': [{'number': 'SANIC', 'is_main': True}],
                'accounts': [{'type': 'GOTTA', 'value': 'GO_FAST'}],
                'official': {'is_dismissed': True, 'quit_at': None},
                'work_email': 'email',
            },
            {
                'id': 3,
                'uid': 3,
                'login': 'login3',
                'phones': [{'number': '+7 (800) 555-35-35', 'is_main': True}],
                'accounts': [],
                'official': {'is_dismissed': True, 'quit_at': None},
                'work_email': 'email',
            },
            {
                'id': 4,
                'uid': 4,
                'login': 'login4',
                'phones': [{'number': '333333', 'is_main': True}],
                'accounts': [{'type': 'GOTTA', 'value': 'GO_FAST'},
                             {'type': 'telegram', 'value': '4'},
                             {'type': 'telegram', 'value': '5'}],
                'official': {'is_dismissed': False, 'quit_at': None},
                'work_email': 'email',
            },
        ]
        return [people[:2], people[2:], []]

    @property
    def first_page(self):
        page = self.pages[self.N]
        self.N += 1
        return page


def test_import_staff_cmd(uid, tg_app):
    mockfun = mock.MagicMock()
    mockfun.return_value = StaffIteratorMock()

    user1 = User.objects.create(uid=1)
    old_usernames = {'old_telegram1', 'old_telegram_2'}
    user1.update_usernames(old_usernames)

    assert set(user1.get_usernames()) == old_usernames

    with mock.patch.object(staff.persons, 'getiter', new=mockfun):
        Command()._handle()

    user1.refresh_from_db()
    assert user1.phone == '88005553535'
    assert user1.get_usernames() == ['prosche_pozvonit']
    assert user1.is_active

    user2 = User.objects.get(uid=2)
    assert user2.phone == ''
    assert user2.get_usernames() == []
    assert not user2.is_active

    user3 = User.objects.get(uid=3)
    assert user3.phone == '+78005553535'
    assert user3.get_usernames() == []
    assert not user3.is_active

    user4 = User.objects.get(uid=4)
    assert user4.phone == '333333'
    assert set(user4.get_usernames()) == {'4', '5'}
    assert user4.is_active
