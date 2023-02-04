from maps.wikimap.stat.tasks_payment.dictionaries.staff_dump.lib.staff_dump import (
    describe_person,
    NMAPS_ACCOUNT_TYPE
)

import pytest


def test_no_accounts():
    result = describe_person({
        'name': {'last': {'ru': 'Фамилия'}, 'first': {'ru': 'Имя'}},
        'uid': '101',
        'login': 'staff-login',
        'department_group': {
            'name': 'Департамент 1',
            'url': 'dep1',
            'ancestors': [
                {'name': 'Департамент 3', 'url': 'dep3'},
                {'name': 'Департамент 2', 'url': 'dep2'}
            ]
        },
        'official': {'is_dismissed': False, 'quit_at': None},
    })
    assert result == {
        'last_name': 'Фамилия',
        'first_name': 'Имя',
        'uid': 101,
        'login': 'staff-login',
        'nmaps_logins': [],
        'primary_department': 'Департамент 1',
        'primary_department_url': 'dep1',
        'departments': ['Департамент 1', 'Департамент 2', 'Департамент 3'],
        'departments_urls': ['dep1', 'dep2', 'dep3'],
        'quit_at': None,
    }


def test_non_nmaps_and_private_accounts_are_ignored():
    result = describe_person({
        'name': {'last': {'ru': 'Фамилия'}, 'first': {'ru': 'Имя'}},
        'uid': '101',
        'login': 'staff-login',
        'accounts': [
            {'value': 'mail@email.com', 'type': 'email', 'private': False},
            {'value': 'login', 'type': NMAPS_ACCOUNT_TYPE, 'private': True},
        ],
        'department_group': {
            'name': 'Департамент 1',
            'url': 'dep1',
            'ancestors': [
                {'name': 'Департамент 3', 'url': 'dep3'},
                {'name': 'Департамент 2', 'url': 'dep2'}
            ]
        },
        'official': {'is_dismissed': False, 'quit_at': None},
    })
    assert result == {
        'last_name': 'Фамилия',
        'first_name': 'Имя',
        'uid': 101,
        'login': 'staff-login',
        'nmaps_logins': [],
        'primary_department': 'Департамент 1',
        'primary_department_url': 'dep1',
        'departments': ['Департамент 1', 'Департамент 2', 'Департамент 3'],
        'departments_urls': ['dep1', 'dep2', 'dep3'],
        'quit_at': None,
    }


def test_nmaps_work_accounts_are_accepted():
    result = describe_person({
        'name': {'last': {'ru': 'Фамилия'}, 'first': {'ru': 'Имя'}},
        'uid': '101',
        'login': 'staff-login',
        'accounts': [
            {'value': 'yndx-login-1@ya.ru', 'type': NMAPS_ACCOUNT_TYPE, 'private': False},
            {'value': 'yndx-login-2@yandex.kz', 'type': NMAPS_ACCOUNT_TYPE, 'private': False},
            {'value': 'Yndx.Login.3@yandex.com.tr', 'type': NMAPS_ACCOUNT_TYPE, 'private': False},
            {'value': 'yndx-login-4@yandex.net.ru', 'type': NMAPS_ACCOUNT_TYPE, 'private': False}
        ],
        'department_group': {
            'name': 'Департамент 1',
            'url': 'dep1',
            'ancestors': [
                {'name': 'Департамент 3', 'url': 'dep3'},
                {'name': 'Департамент 2', 'url': 'dep2'}
            ]
        },
        'official': {'is_dismissed': False, 'quit_at': None},
    })
    assert result == {
        'last_name': 'Фамилия',
        'first_name': 'Имя',
        'uid': 101,
        'login': 'staff-login',
        'nmaps_logins': ['yndx-login-1', 'yndx-login-2', 'yndx-login-3', 'yndx-login-4'],
        'primary_department': 'Департамент 1',
        'primary_department_url': 'dep1',
        'departments': ['Департамент 1', 'Департамент 2', 'Департамент 3'],
        'departments_urls': ['dep1', 'dep2', 'dep3'],
        'quit_at': None,
    }


def test_should_get_quit_at_for_dismissed_employees_only():
    employee = {
        'name': {'last': {'ru': 'Фамилия'}, 'first': {'ru': 'Имя'}},
        'uid': '101',
        'login': 'staff-login',
        'department_group': {
            'name': 'Департамент 1',
            'url': 'dep1',
            'ancestors': []
        },
        'official': {'is_dismissed': False, 'quit_at': '2022-03-04'},
    }
    assert describe_person(employee)['quit_at'] is None

    employee['official']['is_dismissed'] = True
    assert describe_person(employee)['quit_at'] == '2022-03-04'


def test_raise_if_nmaps_work_account_is_not_an_email():
    with pytest.raises(ValueError, match='Yandex e-mail .* does not match regexp'):
        describe_person({
            'name': {'last': {'ru': 'Фамилия'}, 'first': {'ru': 'Имя'}},
            'uid': '101',
            'login': 'staff-login',
            'accounts': [
                {'value': 'yndx-login', 'type': NMAPS_ACCOUNT_TYPE, 'private': False}
            ],
            'department_group': {
                'name': 'Департамент 1',
                'url': 'dep1',
                'ancestors': [
                    {'name': 'Департамент 3', 'url': 'dep3'},
                    {'name': 'Департамент 2', 'url': 'dep2'}
                ]
            },
            'official': {'is_dismissed': False, 'quit_at': None},
        })


def test_raise_if_nmaps_work_account_email_is_not_yandex():
    with pytest.raises(ValueError, match='Unexpected domain .* in yandex e-mail'):
        describe_person({
            'name': {'last': {'ru': 'Фамилия'}, 'first': {'ru': 'Имя'}},
            'uid': 101,
            'login': 'staff-login',
            'accounts': [
                {'value': 'yndx-login@not-yandex.ru', 'type': NMAPS_ACCOUNT_TYPE, 'private': False}
            ],
            'department_group': {
                'name': 'Департамент 1',
                'url': 'dep1',
                'ancestors': [
                    {'name': 'Департамент 3', 'url': 'dep3'},
                    {'name': 'Департамент 2', 'url': 'dep2'}
                ]
            },
            'official': {'is_dismissed': False, 'quit_at': None},
        })
