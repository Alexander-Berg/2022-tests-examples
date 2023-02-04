# coding: utf-8

from __future__ import unicode_literals

import pytest
import json

from tests import helpers
from easymeeting.core import models

SOME_DATETIME_STR_FROM = '2018-03-14T12:00Z'
SOME_DATETIME_STR_TO = '2018-03-14T14:00Z'


@pytest.mark.vcr
def test_get_rooms(client):
    actual = helpers.get_json(
        client=client,
        path='/frontend/rooms/',
        query_params={'exchangeNames': 'conf_ekb_east,conf_nn_3_9'}
    )

    helpers.assert_is_substructure(
        {
            'rooms': [
                {
                    'name': {'exchange': 'conf_ekb_east'},
                },
                {
                    'name': {'exchange': 'conf_nn_3_9'},
                },
            ]
        },
        actual,
    )


@pytest.mark.vcr
@pytest.mark.django_db
def test_get_combinations_offices(client):
    actual = helpers.post_json(
        client=client,
        path='/frontend/combinations/',
        json_data={
            'dateFrom': SOME_DATETIME_STR_FROM,
            'dateTo': SOME_DATETIME_STR_TO,
            'participants': [
                {'login': 'volozh', 'officeId': 1},
                {'login': 'imperator', 'officeId': 2},
            ],
        },
    )

    assert 'offices' in actual
    assert {office['id'] for office in actual['offices']} == {1, 2}


@pytest.mark.vcr
def test_get_offices_by_ids(client):
    actual = helpers.get_json(
        client=client,
        path='/frontend/offices/',
        query_params={'officeIds': '1,3,8,4,121,55'}
    )
    expected = [
        {'cityName': 'Москва', 'id': 1, 'name': 'БЦ Морозов', 'tzOffset': 10800000},
        {'cityName': 'Екатеринбург', 'id': 3, 'name': 'БЦ Палладиум', 'tzOffset': 18000000},
        {'cityName': 'Новосибирск', 'id': 55, 'name': 'Академпарк', 'tzOffset': 25200000},
        {'cityName': 'Москва', 'id': 8, 'name': 'БЦ Мамонтов', 'tzOffset': 10800000},
        {'cityName': 'Москва', 'id': 121, 'name': 'БЦ Аврора', 'tzOffset': 10800000},
        {'cityName': 'Новосибирск', 'id': 4, 'name': 'БЦ Гринвич', 'tzOffset': 25200000},
    ]
    assert actual['offices'] == expected


@pytest.mark.vcr
def test_get_all_offices(client):
    actual = helpers.get_json(
        client=client,
        path='/frontend/offices/',
        query_params={}
    )
    assert len(actual['offices']) == 55


@pytest.mark.vcr
@pytest.mark.django_db
def test_get_combinations_rooms_format(client):
    actual = helpers.post_json(
        client=client,
        path='/frontend/combinations/',
        json_data={
            'dateFrom': SOME_DATETIME_STR_FROM,
            'dateTo': SOME_DATETIME_STR_TO,
            'participants': [
                {'login': 'volozh', 'officeId': 1},
            ],
        },
    )

    helpers.assert_is_substructure(
        {
            'offices': [
                {
                    'id': 1,
                    'name': "БЦ Морозов",
                    'combinations': [
                        {
                            'factors': {},
                            'slots': [
                                {
                                    'name': {
                                        'display': helpers.ANY(basestring),
                                        'exchange': helpers.ANY(basestring),
                                    },
                                },
                            ],
                        },
                    ]
                }
            ]
        },
        actual,
    )
    for office in actual['offices']:
        for combination in office['combinations']:
            for slot in combination['slots']:
                # conf_rr_7_6 unavailable for booking
                assert 'conf_rr_7_6' != slot['name']['exchange']


@pytest.mark.vcr
@pytest.mark.django_db
def test_get_combinations_with_persons_availability(client):
    res = helpers.post_json(
        client=client,
        path='/frontend/combinations/',
        json_data={
            'dateFrom': SOME_DATETIME_STR_FROM,
            'dateTo': SOME_DATETIME_STR_TO,
            'participants': [
                {'login': 'zhigalov', 'officeId': 3},
                {'login': 'sibirev', 'officeId': 3},
            ],
        },
    )
    actual = [
        combination
        for combination in res['offices'][0]['combinations']
        if combination['factors']['booked'] > 0
    ]
    expected = [
        {
            'slots': [
                {
                    'dateFrom': '2018-03-14T12:00:00Z',
                    'dateTo': '2018-03-14T13:00:00Z',
                    'name': {'display': '11.Атлантида', 'exchange': 'conf_ekb_atlantis'},
                    'factors': {
                        'personsAvailability': 50,
                        'booked': 100,
                    },
                    'info': {
                        'unavailablePersonsCount': 1,
                        'totalPersonsCount': 2,
                    },
                    'eventId': 11758,
                },
                {
                    'dateFrom': '2018-03-14T13:00:00Z',
                    'dateTo': '2018-03-14T14:00:00Z',
                    'name': {'display': '11.Атлантида', 'exchange': 'conf_ekb_atlantis'},
                    'factors': {
                        'personsAvailability': 0,
                        'booked': 100,
                    },
                    'info': {
                        'unavailablePersonsCount': 1,
                        'totalPersonsCount': 1,
                    },
                    'eventId': 11762,
                },
            ],
            'factors': {
                'personsAvailability': 25,  # 0.5 * 50 + 0.5 * 0
                'booked': 100,  # 0.5 * 100 + 0.5 * 100
                'hops': 8,  # 100 * (10 min) / (120 min)
            },
        }
    ]
    assert actual == expected


@pytest.mark.vcr
@pytest.mark.django_db
def test_get_combinations_visits(client):
    json_data = {
        'dateFrom': SOME_DATETIME_STR_FROM,
        'dateTo': SOME_DATETIME_STR_TO,
        'participants': [
            {'login': 'zhigalov', 'officeId': 3},
            {'login': 'sibirev', 'officeId': 3},
        ],
    }
    helpers.post_json(
        client=client,
        path='/frontend/combinations/',
        json_data=json_data,
    )
    visits = models.Visit.objects.values('params', 'uid', 'is_helpful')
    assert [{
        'params': json.dumps(json_data),
        'uid': '1120000000073516',
        'is_helpful': None,
    }] == [visit for visit in visits]
