from typing import Any, Dict, List
import mock
import pytest
import datetime

from django.core.management import call_command
from django.test import override_settings
from freezegun import freeze_time

from intranet.crt.constants import AFFILIATION
from intranet.crt.tasks.sync_users import get_department_urls_to_affiliations_dict, staff_repo, groups_repo
from intranet.crt.users.models import CrtUser

pytestmark = pytest.mark.django_db


JSON = Dict[str, Any]


default_groups = [{
    'url': 'outstaff_dep123',
    'ancestors': [
        {'url': 'outstaff'},
        {'url': 'outstaff_dep123'},
    ]
}, {
    'url': 'as_dep321_dep654_dep987',
    'ancestors': [
        {'url': 'as'},
        {'url': 'as_dep321'},
        {'url': 'as_dep321_dep654'},
        {'url': 'as_dep321_dep654_dep987'},
    ]
}, {
    'url': 'yandex_dep123',
    'ancestors': [
        {'url': 'yandex'},
        {'url': 'yandex_dep123'},
    ]
}, {
    'url': 'yandex',
    'ancestors': [],
}]


offers = [{
    'username': 'tag_user',
    'first_name_en': 'Another',
    'last_name_en': 'User',
    'first_name': 'Другой',
    'last_name': 'Пользователь',
    'office': 500,
    'join_at': '2017-02-03',
    'department_url': default_groups[0]['url'],
}, {
    'username': 'in_hiring',
    'first_name_en': 'Hiring',
    'last_name_en': 'User',
    'first_name': 'Нанимаемый',
    'last_name': 'Пользователь',
    'office': 500,
    'join_at': '2017-02-03',
    'department_url': default_groups[1]['url'],
}, {
    'username': 'helpdesk_user',
    'first_name_en': 'Expired',
    'last_name_en': 'User',
    'first_name': 'Истёкший',
    'last_name': 'Пользователь',
    'office': 500,
    'join_at': '2017-01-01',
    'department_url': default_groups[2]['url'],
}]


def make_user(id, username, firstname, last_name, firstname_ru, last_name_ru,
              affiliation=AFFILIATION.YANDEX, is_active=True, is_robot=False, robot_owners=[]):
    data = {
        'id': id,
        'login': username,
        'official': {
            'is_dismissed': not is_active,
            'join_at': '2017-01-01',
            'is_robot': is_robot,
            'affiliation': affiliation,
        },
        'name': {
            'first': {'en': firstname, 'ru': firstname_ru},
            'last': {'en': last_name, 'ru': last_name_ru},
        },
        'language': {'ui': 'ru'},
        'work_email': '%s@yandex-team.ru' % username,
        'location': {
            'office': {
                'city': {
                    'country': {
                        'code': 'ru'
                    },
                    'name': {
                        'en': 'Moscow',
                    }
                }
            }
        },
        'department_group': {
            'department': {
                'kind': {
                    'slug': 'direction',
                },
                'url': 'yandex'
            }
        }
    }
    data.update({'robot_owners': [
        {'person': {
                'login': owner_username,
            },
        }
        for owner_username in robot_owners or []
    ]})
    return data


staff_person_iter = [
    make_user(1, 'normal_user', 'Normal', 'User', 'Обычный', 'Пользователь'),
    make_user(2, 'another_user', 'Another', 'User', 'Другой', 'Пользователь', is_active=False),
    make_user(3, 'tag_user', 'Tag', 'User', 'Теговый', 'Пользователь', is_active=False),
    make_user(4, 'helpdesk_user', 'Expired', 'User', 'Истёкший', 'Пользователь', is_active=False),
]


staff_person_new_iter = [
    make_user(5, 'normal_user', 'Simple', 'User', 'Обычный', 'Пользователь'),
    make_user(6, 'another_user', 'Another', 'User', 'Другой', 'Пользователь', is_active=False),
    make_user(7, 'tag_user', 'Tag', 'User', 'Таговый', 'Пользователь', is_active=False),
    make_user(8, 'helpdesk_user', 'Expired', 'User', 'Истёкший', 'Юзер', is_active=False),
]


class StaffIteratorMock(object):
    def __init__(self, get_user_func):
        self.N = 0
        self._get_user_func = get_user_func
        self._pages = self.get_pages()
    
    def __iter__(self):
        for page in self.get_pages():
            for obj in page:
                yield obj

    def get_pages(self):
        people = self._get_user_func(lookup=None)
        return [people[:2], people[2:], []]

    @property
    def first_page(self):
        page = self._pages[self.N]
        self.N += 1
        return page


class StaffOwnersMock(object):
    def __init__(self, robot_owners=None):
        self.robot_owners = robot_owners or []

    def ret_val(self, lookup):
        return [
            make_user(1, 'normal_user', 'Normal', 'User', 'Обычный', 'Пользователь'),
            make_user(2, 'another_user', 'Another', 'User', 'Другой', 'Пользователь'),
            make_user(3, 'tag_user', 'Tag', 'User', 'Теговый', 'Пользователь'),
            make_user(4, 'helpdesk_user', 'Helpdesk', 'User', 'Хелпдеска', 'Пользователь'),
            make_user(5, 'hypercube_user', 'Hypercube', 'Robot', 'Гипперкуб', 'Робот',
                      is_robot=True, robot_owners=self.robot_owners),
        ]


def run_sync_users(*,
    staff_persons: List[JSON],
    offers: List[JSON],
    when=datetime.datetime(2017, 2, 10),
    groups=default_groups,
):
    mocked_groups_getiter = mock.MagicMock()
    mocked_groups_getiter.return_value = StaffIteratorMock(lambda lookup: groups)

    mocked_staff_getiter = mock.MagicMock()
    mocked_staff_getiter.return_value = StaffIteratorMock(lambda lookup: staff_persons)

    with (
        mock.patch.object(staff_repo, 'getiter', new=mocked_staff_getiter),
        mock.patch.object(groups_repo, 'getiter', new=mocked_groups_getiter),
        mock.patch('intranet.crt.tasks.sync_users.get_offers', lambda: offers),
        override_settings(CRT_NEWHIRE_FRESHNESS_DAYS=14),
        freeze_time(when),
    ):
        call_command('sync_users')


def test_sync_users(users):
    def get_actual_user(username: str) -> CrtUser:
        user = users[username]
        user.refresh_from_db()
        return user

    # Протухший нанимаемый пользователь
    helpdesk_user = CrtUser.objects.get(username='helpdesk_user')
    helpdesk_user.in_hiring = True
    helpdesk_user.save()

    run_sync_users(staff_persons=staff_person_iter, offers=offers)

    # Обычный пользователь
    normal_user = get_actual_user('normal_user')
    assert normal_user.is_active is True
    assert normal_user.in_hiring is False
    assert normal_user.full_name == 'Normal User'
    assert normal_user.full_name_ru == 'Обычный Пользователь'
    assert normal_user.affiliation == AFFILIATION.YANDEX

    # Уволенный пользователь
    another_user = get_actual_user('another_user')
    assert another_user.is_active is False
    assert another_user.in_hiring is False
    assert another_user.full_name == 'Another User'
    assert another_user.full_name_ru == 'Другой Пользователь'
    assert another_user.affiliation == AFFILIATION.YANDEX

    # Уволенный пользователь - терминатор
    tag_user = get_actual_user('tag_user')
    assert tag_user.is_active is False
    assert tag_user.in_hiring is True
    assert tag_user.full_name == 'Tag User'
    assert tag_user.full_name_ru == 'Теговый Пользователь'
    assert tag_user.affiliation == AFFILIATION.EXTERNAL

    # Свежий нанимаемый пользователь
    in_hiring = CrtUser.objects.get(username='in_hiring')
    assert in_hiring.is_active is False
    assert in_hiring.in_hiring is True
    assert in_hiring.full_name == 'Hiring User'
    assert in_hiring.full_name_ru == 'Нанимаемый Пользователь'
    assert in_hiring.affiliation == AFFILIATION.EXTERNAL

    # Протухший нанимаемый
    helpdesk_user.refresh_from_db()
    assert helpdesk_user.is_active is False
    assert helpdesk_user.in_hiring is False
    assert helpdesk_user.full_name == 'Expired User'
    assert helpdesk_user.full_name_ru == 'Истёкший Пользователь'
    assert helpdesk_user.affiliation == AFFILIATION.YANDEX

    # Переименовываем пользователей
    run_sync_users(staff_persons=staff_person_new_iter, offers=offers)

    # Обычный пользователь
    normal_user.refresh_from_db()
    assert normal_user.full_name == 'Simple User'
    assert normal_user.full_name_ru == 'Обычный Пользователь'

    # Уволенный пользователь
    another_user = get_actual_user('another_user')
    assert another_user.full_name == 'Another User'
    assert another_user.full_name_ru == 'Другой Пользователь'

    # Уволенный пользователь - терминатор
    tag_user.refresh_from_db()
    assert tag_user.full_name == 'Tag User'
    assert tag_user.full_name_ru == 'Таговый Пользователь'

    # Протухший нанимаемый
    helpdesk_user.refresh_from_db()
    helpdesk_user.full_name == 'Expired User'
    helpdesk_user.full_name_ru == 'Истёкший Юзер'


def test_affiliation_changed(users):
    user: CrtUser = CrtUser.objects.get(username='normal_user')
    the_date = datetime.datetime(2017, 2, 10)

    hired_user = make_user(1, 'normal_user', 'Normal', 'User', 'Обычный', 'Пользователь', AFFILIATION.YANDEX, True)
    fired_user = make_user(1, 'normal_user', 'Normal', 'User', 'Обычный', 'Пользователь', AFFILIATION.YANDEX, False)
    fresh_outstaff_user = make_user(1, 'normal_user', 'Normal', 'User', 'Обычный', 'Пользователь', AFFILIATION.EXTERNAL, True)

    # initially
    assert user.is_active is True
    assert user.in_hiring is False
    assert user.affiliation == AFFILIATION.YANDEX

    # fire user
    the_date += datetime.timedelta(days=30)
    run_sync_users(staff_persons=[fired_user], offers=[], when=the_date)
    user.refresh_from_db()
    assert user.is_active is False
    assert user.in_hiring is False
    assert user.affiliation == AFFILIATION.YANDEX

    # begin hiring user to outstaff
    the_date += datetime.timedelta(days=30)
    join_at = datetime.datetime.strftime(the_date + datetime.timedelta(days=7), '%Y-%m-%d')
    run_sync_users(
        staff_persons=[fired_user],
        offers=[{
            'username': 'normal_user',
            'first_name_en': 'Normal',
            'last_name_en': 'User',
            'first_name': 'Обычный',
            'last_name': 'Пользователь',
            'office': 500,
            'join_at': join_at,
            'department_url': default_groups[0]['url'],
        }],
        when=the_date,
    )
    user.refresh_from_db()
    assert user.is_active is False
    assert user.in_hiring is True
    assert user.affiliation == AFFILIATION.EXTERNAL

    # hire user to outstaff
    the_date += datetime.timedelta(days=30)
    run_sync_users(staff_persons=[fresh_outstaff_user], offers=[], when=the_date)
    user.refresh_from_db()
    assert user.is_active is True
    assert user.in_hiring is False
    assert user.affiliation == AFFILIATION.EXTERNAL


def test_sync_robot_responsibilities(users):
    robot = users['hypercube_user']
    mocked_groups_getiter = mock.MagicMock()
    mocked_groups_getiter.return_value = StaffIteratorMock(lambda lookup: default_groups)

    mockfun = mock.MagicMock()

    for owners_ids in [
        [],
        ['normal_user'],
        ['tag_user', 'helpdesk_user'],
        ['normal_user', 'another_user', 'helpdesk_user'],
        [],
        ['helpdesk_user']
    ]:
        mockfun.return_value = StaffIteratorMock(StaffOwnersMock(owners_ids).ret_val)

        with (
            mock.patch.object(staff_repo, 'getiter', new=mockfun),
            mock.patch.object(groups_repo, 'getiter', new=mocked_groups_getiter),
            mock.patch('intranet.crt.tasks.sync_users.get_offers', lambda: []),
        ):
            call_command('sync_users')
        assert set(robot.responsibles.values_list('username', flat=True)) == set(owners_ids)

def test_groups_to_affiliations():
    mocked_groups_getiter = mock.MagicMock()
    mocked_groups_getiter.return_value = StaffIteratorMock(lambda lookup: default_groups)
    with mock.patch.object(groups_repo, 'getiter', new=mocked_groups_getiter):
        groups_to_affiliations = get_department_urls_to_affiliations_dict([
            'outstaff_dep123',
            'as_dep321_dep654_dep987',
            'yandex_dep123',
            'yandex',
        ])
    assert groups_to_affiliations['outstaff_dep123'] == AFFILIATION.EXTERNAL
    assert groups_to_affiliations['as_dep321_dep654_dep987'] == AFFILIATION.EXTERNAL
    assert groups_to_affiliations['yandex_dep123'] == AFFILIATION.YANDEX
    assert groups_to_affiliations['yandex'] == AFFILIATION.YANDEX


def test_incosistency_ignore(crt_client):
    brand_new_department = {
        'url': 'brand_new_dep',
        'ancestors': [
            {'url': 'yandex'},
            {'url': 'brand_new'},
        ]
    }
    user = {
        'username': 'mr_inconsistent',
        'first_name_en': 'Mister',
        'last_name_en': 'Inconsistent',
        'first_name': 'Мистер',
        'last_name': 'Неконсистентный',
        'office': 500,
        'join_at': '2017-02-03',
        'department_url': brand_new_department['url'],
    }
    the_date = datetime.datetime(2017, 2, 10)

    # initially
    assert CrtUser.objects.count() == 0

    # try to sync user, whose group is not yet created
    run_sync_users(staff_persons=[], offers=[user], when=the_date)
    assert CrtUser.objects.count() == 0

    # check monitoring shows invalid user
    response = crt_client.json.get('/monitorings/staff-sync-inconsistencies/')
    assert response.status_code == 412
    assert response.content.decode('utf-8') == 'user: "mr_inconsistent: department brand_new_dep was not found"'

    # create group
    new_groups = default_groups + [brand_new_department]

    # sync with created group
    run_sync_users(staff_persons=[], offers=[user], when=the_date, groups=new_groups)
    db_user = CrtUser.objects.get(username=user['username'])
    assert not db_user.is_active
    assert db_user.in_hiring
    assert db_user.affiliation == AFFILIATION.YANDEX

    # check monitoring shows no invalid users
    response = crt_client.json.get('/monitorings/staff-sync-inconsistencies/')
    assert response.status_code == 200
