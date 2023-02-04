from datetime import datetime, timezone
from mock import patch
import pytest

from intranet.vconf.src.ext_api.calendar import (
    parse_event_id,
    get_events_by_time,
)


test_cases = [
    (None, None),
    (123, 123),
    ('123', 123),
    ('https://calendar.yandex-team.ru/event/1?applyToFuture=0', 1),
    ('https://calendar.yandex-team.ru/event/1', 1),
    ('https://calendar.yandex-team.ru/event/xcc', None),
    ('https://calendar.yandex-team.ru/event/?uid=1111&event_id=123', 123),
    ('https://calendar.yandex-team.ru/event/?event_id=123', 123),
    ('https://calendar.yandex-team.ru/event/?show_event_id=123', 123),
    ('https://calendar.yandex-team.ru/event/?uid=11', None),
]

FAKE_CALENDAR_GET_RESOURCES_SCHEDULE = {
    'offices': [
        {
            'id': 1,
            'staffId': 1,
            'name': 'Morozov',
            'tzId': 'Europe/Moscow',
            'resources': [
                {
                    'info': {
                        'id': 1,
                        'name': 'Test room 1',
                        'alterName': '',
                        'email': 'conf_rr_1@yandex-team.ru',
                        'type': 'protected-room',
                        'hasPhone': False,
                        'hasVideo': False,
                        'canBook': True,
                        'mapUrl': '',
                    },
                    'restrictions': [],
                    'events': [
                        {
                            'eventId': 10001,
                            'start': '2021-10-20T12:00:00',
                            'end': '2021-10-20T21:00:00',
                        },
                    ],
                },
                {
                    'info': {
                        'id': 2,
                        'name': 'Test room 2',
                        'alterName': '',
                        'email': 'conf_rr_2@yandex-team.ru',
                        'type': 'room',
                        'hasPhone': False,
                        'hasVideo': False,
                        'canBook': True,
                        'mapUrl': '',
                    },
                    'restrictions': [],
                    'events': [
                        {
                            'eventId': 10002,
                            'start': '2021-10-20T12:00:00',
                            'end': '2021-10-20T21:00:00',
                        },
                    ],
                },
            ],
        },
    ],
}


def fake_get_events_brief(*args, **kwargs):
    return {
        'events': [
            {
                'id': 10001,
                'name': 'Test meeting 1',
                'organizer': {
                    'uid': 20001,
                    'name': 'Uhura',
                    'email': 'uhura@yandex-team.ru',
                    'login': 'uhura',
                    'officeId': 1,
                    'decision': 'yes',
                },
                'attendees': [],
                'resources': [
                    {
                        'name': 'Test room 1',
                        'email': 'conf_rr_extropolis@yandex-team.ru',
                        'type': 'room',
                        'officeId': 1,
                        'officeStaffId': 1,
                    },
                ],
                'othersCanView': True,
            },
            {
                'id': 10002,
                'name': 'Test meeting 2',
                'organizer': {
                    'uid': 20002,
                    'name': 'Spock',
                    'email': 'spock@yandex-team.ru',
                    'login': 'spock',
                    'officeId': 1,
                    'decision': 'yes',
                },
                'attendees': [],
                'resources': [
                    {
                        'name': 'Test room 2',
                        'email': 'conf_rr_extropolis@yandex-team.ru',
                        'type': 'room',
                        'officeId': 1,
                        'officeStaffId': 1,
                    },
                ],
                'othersCanView': False,
            },
        ],
    }


def fake_get_events_for_offices(*args, **kwargs):
    return FAKE_CALENDAR_GET_RESOURCES_SCHEDULE['offices']


@pytest.mark.parametrize('test_case', test_cases)
def test_get_event_id_by_url(test_case):
    raw_event_id, expected_event_id = test_case
    assert parse_event_id(raw_event_id) == expected_event_id


@patch('intranet.vconf.src.ext_api.calendar.get_events_for_offices', fake_get_events_for_offices)
@patch('intranet.vconf.src.ext_api.calendar.get_events_brief', fake_get_events_brief)
def test_get_events_by_time():
    expected_result = {
        '10001': {
            'rooms': ['conf_rr_1@yandex-team.ru'],
            'start_time': datetime(2021, 10, 20, 12, 0, tzinfo=timezone.utc),
            'end_time': datetime(2021, 10, 20, 21, 0, tzinfo=timezone.utc),
            'organizer': 'Uhura',
            'name': 'Test meeting 1',
        },
        '10002': {
            'rooms': ['conf_rr_2@yandex-team.ru'],
            'start_time': datetime(2021, 10, 20, 12, 0, tzinfo=timezone.utc),
            'end_time': datetime(2021, 10, 20, 21, 0, tzinfo=timezone.utc),
            'organizer': 'Hidden',
            'name': 'Meeting name is hidden',
        },
    }

    assert get_events_by_time(['1']) == expected_result
