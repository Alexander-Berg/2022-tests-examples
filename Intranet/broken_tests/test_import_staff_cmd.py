from typing import Dict

import mock
import pytest

from tasha.constants import ACTION
from tasha.models import User, TelegramAccount, Action
from tasha.tests.factories import TgMembershipFactory

from tasha.management.commands.import_staff_users import Command, staff

pytestmark = [pytest.mark.django_db(transaction=True)]


class StaffIteratorMock(object):

    def __init__(self, update_people_dict: Dict[str, dict] = None):
        self.N = 0
        update_people_dict = update_people_dict or {}
        self.people = [
            {
                'id': 1,
                'uid': 1,
                'login': 'login1',
                'accounts': [{'type': 'telegram', 'value': 'prosche_pozvonit'}],
                'department_group': {
                    'url': 'url',
                    'ancestors': [{'url': 'url'}],
                },
                'official': {'is_dismissed': False, 'quit_at': None, 'join_at': '2013-12-09',
                             'organization': {'id': 1}
                             },
                'work_email': 'email',
            },
            {
                'id': 2,
                'uid': 2,
                'login': 'login2',
                'accounts': [{'type': 'GOTTA', 'value': 'GO_FAST'}],
                'department_group': {
                    'url': 'url',
                    'ancestors': [],
                },
                'official': {'is_dismissed': False, 'quit_at': None, 'join_at': '2013-12-09',
                             'organization': {'id': 1}
                             },
                'work_email': 'email',
            },
            {
                'id': 3,
                'uid': 3,
                'login': 'login3',
                'accounts': [],
                'department_group': {
                    'url': 'url',
                    'ancestors': [{'url': 'url'}],
                },
                'official': {'is_dismissed': True, 'quit_at': None, 'join_at': '2013-12-09',
                             'organization': {'id': 1}
                             },
                'work_email': 'email',
            },
            {
                'id': 4,
                'uid': 4,
                'login': 'login4',
                'accounts': [{'type': 'GOTTA', 'value': 'GO_FAST'},
                             {'type': 'telegram', 'value': '4'},
                             {'type': 'telegram', 'value': '5'}],
                'department_group': {
                    'url': 'ex_yandex_money',
                    'ancestors': [{'url': 'url'}],
                },
                'official': {'is_dismissed': False, 'quit_at': None, 'join_at': '2013-12-09',
                             'organization': {'id': 1}
                             },
                'work_email': 'email',
            },
            {
                'id': 5,
                'uid': 5,
                'login': 'login5',
                'accounts': [],
                'department_group': {
                    'url': 'url',
                    'ancestors': [{'url': 'ex_yandex_money'}],
                },
                'official': {'is_dismissed': False, 'quit_at': None, 'join_at': '2013-12-09',
                             'organization': {'id': 1}
                             },
                'work_email': 'email',
            },
            {
                'id': 6,
                'uid': 6,
                'login': 'login6',
                'accounts': [
                    {'type': 'telegram', 'value': '61'},
                    {'type': 'telegram', 'value': '62'}
                ],
                'department_group': {
                    'url': 'yandex_department',
                    'ancestors': [{'url': 'url'}],
                },
                'official': {'is_dismissed': False, 'quit_at': None, 'join_at': '2013-12-09',
                             'organization': {'id': 1}
                             },
                'work_email': 'email',
            },
            {
                'id': 7,
                'uid': 7,
                'login': 'login7',
                'accounts': [
                    {'type': 'telegram', 'value': '71'},
                    {'type': 'telegram', 'value': '72'}
                ],
                'department_group': {
                    'url': 'yandex_department',
                    'ancestors': [{'url': 'url'}],
                },
                'official': {'is_dismissed': False, 'quit_at': None, 'join_at': '2013-12-09',
                             'organization': {'id': 1}
                             },
                'work_email': 'email',
            }
        ]

        for staff_person in self.people:
            updates = update_people_dict.get(staff_person['login'], {})
            staff_person.update(updates)

    @property
    def pages(self):
        return self.get_pages()

    def get_pages(self):
        return [self.people[:2], self.people[2:], []]

    @property
    def first_page(self):
        page = self.pages[self.N]
        self.N += 1
        return page


def test_import_staff_cmd(django_assert_num_queries):
    mockfun = mock.MagicMock()
    mockfun.return_value = StaffIteratorMock()

    user1 = User.objects.create(username='login1', is_active=True)
    old_usernames = {'old_telegram1', 'old_telegram2'}

    user1.update_usernames(old_usernames)

    assert set(user1.get_usernames()) == old_usernames

    with mock.patch.object(staff.persons, 'getiter', new=mockfun), django_assert_num_queries(34):
        Command()._handle()  # import_staff_users

    user1.refresh_from_db()
    assert user1.get_usernames() == ['prosche_pozvonit']
    assert user1.is_active

    user2 = User.objects.get(username='login2')
    assert user2.get_usernames() == []
    assert user2.is_active

    user3 = User.objects.get(username='login3')
    assert user3.get_usernames() == []
    assert not user3.is_active

    user4 = User.objects.get(username='login4')
    assert set(user4.get_usernames()) == set()
    assert not user4.is_active  # ex yamoney

    user5 = User.objects.get(username='login5')
    assert user5.get_usernames() == []
    assert not user5.is_active  # ex yamoney

    user6 = User.objects.get(username='login6')
    assert set(user6.get_usernames()) == {'61', '62'}
    assert user6.is_active

    user7 = User.objects.get(username='login7')
    assert set(user7.get_usernames()) == {'71', '72'}
    assert user7.is_active

    account = TelegramAccount.objects.get(username='prosche_pozvonit')
    membership = TgMembershipFactory(account=account)
    mockfun.return_value = StaffIteratorMock(
        update_people_dict={
            'login1': {'accounts': []},  # удалил телегу из контактов
            'login4': {  # переместился внутрь Яндекса
                'department_group': {
                    'url': 'yandex_regular_department',
                    'ancestors': [{'url': 'yandex'}],
                }},
            'login6': {'official': {'is_dismissed': True, 'quit_at': '2015-04-20', 'join_at': '2013-12-09',
                                    'organization': {'id': 1}}},  # уволился
            'login7': {
                'accounts': [
                    {'type': 'telegram', 'value': '71'},
                    {'type': 'telegram', 'value': '72'},
                    {'type': 'telegram', 'value': '61'},  # взял телегу уволенного
                ],
            },
        }
    )

    with mock.patch.object(staff.persons, 'getiter', new=mockfun), django_assert_num_queries(26):
        Command()._handle()  # import_staff_users

    action = Action.objects.get()
    assert action.action == ACTION.USER_DELETED_TELEGRAM
    assert action.membership == membership

    user1.refresh_from_db()
    assert user1.get_usernames() == []

    user4.refresh_from_db()
    assert set(user4.get_usernames()) == {'4', '5'}
    assert user4.is_active

    user6.refresh_from_db()
    assert user6.get_usernames() == ['62']
    assert not user6.is_active

    user7.refresh_from_db()
    assert set(user7.get_usernames()) == {'61', '71', '72'}

    assert TelegramAccount.objects.filter(username='61', user=user7).count() == 1
