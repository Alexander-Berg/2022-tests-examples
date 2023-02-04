# coding: utf-8

from __future__ import unicode_literals

import pytest

from tests import helpers


# TODO: вынести функции сериализации/фильтрации из вьюх и тестить отдельно
@pytest.fixture
def event():
    """
    Это костыль. В этом модуле с тестами функции из вьюх тестятся
    напрямую, что приводит к импорту get_user_ticket
    в незапатченном виде.
    Поэтому импортим вьюху прямо внутри теста.
    """
    from easymeeting.frontend.views import event
    return event


@pytest.mark.vcr
def test_get_public_events(client):
    actual = helpers.get_json(
        client=client,
        path='/frontend/events/',
        query_params={
            'eventIds': '11758,11762'
        }
    )
    helpers.assert_is_substructure(
        {
            'events': [
                {
                    'id': 11758,
                    'name': 'lecture',
                    'description': '',
                    'dateFrom': '2018-03-14T12:00:00Z',
                    'dateTo': '2018-03-14T13:00:00Z',
                    'organizer': {
                        'login': 'zhigalov',
                        'name': 'Сергей Жигалов',
                        'gaps': [{
                            'date_from': '2018-03-14T00:00:00Z',
                            'date_to': '2018-03-15T00:00:00Z',
                            'to_notify': [],
                        }],
                        'officeId': 3,
                        'decision': 'yes',
                    },
                    'attendees': [{
                        'login': 'm-smirnov',
                        'name': 'Михаил Смирнов',
                        'gaps': [],
                        'officeId': 3,
                        'decision': 'yes',

                    }],
                },
                {
                    'id': 11762,
                    'name': 'test event',
                    'description': 'Тестовое событие',
                    'dateFrom': '2018-03-14T13:00:00Z',
                    'dateTo': '2018-03-14T14:00:00Z',
                    'organizer': {
                        'login': 'zhigalov',
                        'name': 'Сергей Жигалов',
                        'gaps': [{
                            'date_from': '2018-03-14T00:00:00Z',
                            'date_to': '2018-03-15T00:00:00Z',
                            'to_notify': [],
                        }],
                        'officeId': 3,
                        'decision': 'yes',
                    },
                    'attendees': [],
                },
            ]
        },
        actual
    )


@pytest.mark.vcr
def test_get_private_events(client):
    actual = helpers.get_json(
        client=client,
        path='/frontend/events/',
        query_params={
            'eventIds': '12058',
        }
    )
    expected = {
        'events': [
            {
                'attendees': [],
                'description': None,
                'dateTo': None,
                'dateFrom': None,
                'organizer': {
                    'login': 'zhigalov',
                    'name': 'Сергей Жигалов',
                    'officeId': 3,
                    'gaps': [],
                    'decision': 'yes',
                },
                'id': 12058,
                'name': None
            }
        ]
    }
    assert actual == expected


@pytest.mark.vcr
def test_get_events_with_maillist_attendees(client):
    actual = helpers.get_json(
        client=client,
        path='/frontend/events/',
        query_params={
            'eventIds': '14591',
        }
    )

    expected = {
        'events': [
            {
                'attendees': [],
                'description': '',
                'dateTo': '2018-09-18T18:30:00Z',
                'dateFrom': '2018-09-18T18:00:00Z',
                'organizer': {
                    'officeId': 1,
                    'decision': 'yes',
                    'gaps': [],
                    'login': 'qazaq',
                    'name': 'Кирилл Карташов',
                },
                'id': 14591,
                'name': 'Встреча с рассылкой-участником',
            },
        ],
    }

    assert actual == expected


def test_create_events_with_wrong_data(client):
    actual = helpers.post_json(
        client=client,
        path='/frontend/events/',
        json_data={
            'name': 'Нет логинов и слотов, только имя',
            'description': '',
        },
        expect_status=400
    )
    assert 'errors' in actual
    assert 'logins' in actual['errors']
    assert 'slots' in actual['errors']
    assert 'name' not in actual['errors']
    assert 'description' not in actual['errors']


@pytest.mark.vcr
def test_create_events_success(client):
    actual = helpers.post_json(
        client=client,
        path='/frontend/events/',
        json_data={
            'name': '',
            'description': 'Created by integration test of EASYMEET-53',
            'logins': ['zhigalov'],
            'slots': [
                {
                    'dateFrom': '2018-05-11T12:00Z',
                    'dateTo': '2018-05-11T13:00Z',
                    'exchange': 'conf_rr_7_6',
                },
                {
                    'dateFrom': '2018-05-11T12:00Z',
                    'dateTo': '2018-05-11T12:30Z',
                    'exchange': 'conf_ekb_east',
                },
                {
                    'dateFrom': '2018-05-11T12:30Z',
                    'dateTo': '2018-05-11T13:00Z',
                    'exchange': 'conf_ekb_ur',
                },
                {
                    'dateFrom': '2018-05-11T13:00Z',
                    'dateTo': '2018-05-11T13:30Z',
                }
            ],
        }
    )
    assert len(actual) == 3


DN = helpers.DatetimeNames()
SLOTS_EXAMPLE = [
    {
        'dateFrom': DN.T_14_00,
        'dateTo': DN.T_15_00,
        'exchange': 'conf_ekb_west',

    },
    {
        'dateFrom': DN.T_14_00,
        'dateTo': DN.T_14_45,
        'exchange': 'conf_rr_1_7',
    },
    {
        'dateFrom': DN.T_14_45,
        'dateTo': DN.T_15_00,
        'exchange': 'conf_rr_2_4',
    },
    {
        'dateFrom': DN.T_14_00,
        'dateTo': DN.T_14_30,
        'exchange': 'conf_spb_pacman',
    },
    {
        'dateFrom': DN.T_14_30,
        'dateTo': DN.T_15_00,
        'exchange': 'conf_spb_smolnyi',
    }
]


def test_get_intervals(event):
    actual = event.get_intervals(SLOTS_EXAMPLE)
    expected = [
        (DN.T_14_00, DN.T_14_30),
        (DN.T_14_30, DN.T_14_45),
        (DN.T_14_45, DN.T_15_00),
    ]
    assert actual == expected


def test_get_rooms_by_interval(event):
    interval = (DN.T_14_00, DN.T_14_30)
    actual = event.get_rooms_by_interval(interval, SLOTS_EXAMPLE)
    expected = [
        'conf_ekb_west',
        'conf_rr_1_7',
        'conf_spb_pacman',
    ]
    assert actual == expected


@pytest.mark.parametrize(
    'name, index, count, expected',
    [
        (
            '',
            1, 2,
            'Без названия [часть 1 из 2]',
        ),
        (
            'Регулярная встреча (руко)водителей',
            2, 4,
            'Регулярная встреча (руко)водителей [часть 2 из 4]',
        ),
        (
            'Хурал',
            0, 1,
            'Хурал',
        ),
    ]
)
def test_get_event_name(event, name, index, count, expected):
    actual = event.get_event_name(name, index, count)
    assert actual == expected
