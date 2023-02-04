# coding: utf-8

from __future__ import unicode_literals

import pytest
import pytz

from datetime import datetime

from mock import patch

from tests import helpers


def _fake_closest_rounded_datetime(*args, **kwargs):
    return datetime(2018, 8, 1, 17, 45, tzinfo=pytz.utc)


@pytest.mark.vcr
def test_get_persons_view(client):
    actual = helpers.get_json(
        client=client,
        path='/frontend/persons/',
        query_params={
            'uids': '1120000000015903,1120000000001234',
            'dateFrom': '2018-03-14T12:00Z',
            'dateTo': '2018-03-14T14:00Z',
        }
    )
    expected = [
        {
            'login': 'zhigalov',
            'uid': 1120000000015903,
            'name': 'Сергей Жигалов',
            'currentOfficeId': 3,
            'events': [
                {
                    'eventId': 11758,
                    'start': '2018-03-14T15:00:00',
                    'end': '2018-03-14T16:00:00',
                    'availability': 'busy',
                },
                {
                    'eventId': 11762,
                    'start': '2018-03-14T16:00:00',
                    'end': '2018-03-14T17:00:00',
                    'availability': 'busy'
                }
            ],
            'gaps': [
                {
                    'dateFrom': '2018-03-14T00:00Z',
                    'dateTo': '2018-03-15T00:00Z',
                    'fullDay': True,
                    'id': 434316,
                    'login': 'zhigalov',
                    'workflow': 'absence'
                }
            ],
        },
        {
            'login': 'jane-t',
            'uid': 1120000000001234,
            'name': 'Евгения Тимофеева',
            'currentOfficeId': 1,
            'events': [],
            'gaps': [],
        },
    ]
    assert actual['persons'] == expected


@patch(
    target='easymeeting.frontend.views.person.datetimes.closest_rounded_datetime',
    new=_fake_closest_rounded_datetime,
)
@pytest.mark.vcr
def test_get_persons_free_intervals_view(client):
    result = helpers.post_json(
        client=client,
        path='/frontend/personsFreeIntervals/',
        json_data={
            'participants': [
                {
                    'login': 'qazaq',
                },
            ],
            'duration': 30,
            'freeTimeFrom': '09:00',
            'freeTimeTo': '15:00',
        }
    )
    assert len(result['intervals']) == 36


@pytest.mark.vcr
def test_get_persons_free_intervals_with_date_limits_view(client):
    result = helpers.post_json(
        client=client,
        path='/frontend/personsFreeIntervals/',
        json_data={
            'participants': [
                {
                    'login': 'qazaq',
                },
            ],
            'duration': 60,
            'dateFrom': '2018-08-01T00:00Z',
            'dateTo': '2018-08-02T00:00Z',
        }
    )
    assert len(result['intervals']) == 80
