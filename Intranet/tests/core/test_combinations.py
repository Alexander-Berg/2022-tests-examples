# coding: utf-8

from __future__ import unicode_literals, division

from copy import deepcopy
from datetime import timedelta

import pytest
from django.conf import settings

from easymeeting.lib import datetimes
from easymeeting.core import combinations
from easymeeting.core import resource_event
from easymeeting.core import rooms
from easymeeting.core import persons_availability

from tests import helpers

DN = helpers.DatetimeNames()

PERSONS_AVAILABILITY_EXAMPLE = persons_availability.PersonsAvailability(
    available=['mokhov', 'm-smirnov'],
    unavailable=['kiparis', 'sibirev'],
)
RAW_RESOURCES_EXAMPLE = [
    {
        "info": {
            "id": 199,
            "name": "3.Гималаи",
            "email": "conf_ekb_himalaya@yandex-team.ru",
            "type": "room",
            "hasPhone": True,
            "hasVideo": True,
            "floor": 3,
            "canBook": True
        },
        "restrictions": [],
        "events": [
            {
                "eventId": 11637,
                "start": "2018-02-14T12:00:00",
                "end": "2018-02-14T13:00:00",
                "persons_availability": PERSONS_AVAILABILITY_EXAMPLE,
            }
        ]
    },
]

ROOM_EXAMPLE = rooms.Room(
    id=10,
    display_name='dummy_name',
    exchange_name='dummy_ex_name',
)

INTERVAL_EXAMPLE_ONE = (DN.T_12_00, DN.T_13_00)
INTERVAL_EXAMPLE_TWO = (DN.T_13_00, DN.T_14_00)

RESOURCE_EVENT_EXAMPLE = resource_event.ResourceEvent(
    interval=INTERVAL_EXAMPLE_ONE,
    persons_availability=PERSONS_AVAILABILITY_EXAMPLE,
    event_id=11637,
)


def test_combination_factors():
    room = rooms.Room.from_raw_resource(RAW_RESOURCES_EXAMPLE[0])
    slots = [
        combinations.BookedSlot(
            room=room,
            interval=(
                datetimes.utc_datetime(2018, 1, 1, 12),
                datetimes.utc_datetime(2018, 1, 1, 13),
            ),
            event=resource_event.ResourceEvent(
                interval=(
                    datetimes.utc_datetime(2018, 1, 1, 12),
                    datetimes.utc_datetime(2018, 1, 1, 13),
                ),
                event_id=1,
                persons_availability=persons_availability.PersonsAvailability(
                    available=['mokhov', 'm-smirnov', 'sibirev'],
                    unavailable=['zhigalov'],
                ),
            ),
        ),
        combinations.VacantSlot(
            room=room,
            interval=(
                datetimes.utc_datetime(2018, 1, 1, 13),
                datetimes.utc_datetime(2018, 1, 1, 13, 30),
            ),
        ),
    ]
    assert slots[0].factors == {
        'booked': 100,
        'personsAvailability': 75,
    }
    assert slots[1].factors == {
        'booked': 0,
        'personsAvailability': 0,
    }
    combination = combinations.Combination(slots=slots)
    assert combination.factors == {
        'booked': 66,  # 100 * 2 / 3 + 0 * 1 / 3
        'personsAvailability': 50,  # 75 * 2 / 3 * 0 * 1 / 3
        'hops': 11,  # 100 * (10 min) / (90 min)
    }


def test_combination_duration():
    combination = combinations.Combination(
        slots=[
            combinations.VacantSlot(
                room=None,
                interval=(
                    datetimes.utc_datetime(2018, 1, 1, 12),
                    datetimes.utc_datetime(2018, 1, 1, 13, 30),
                )
            )
        ]
    )
    assert combination.duration == 90


def test_combination_interval():
    combination = combinations.Combination(
        slots=[
            combinations.VacantSlot(
                room=None,
                interval=(
                    datetimes.utc_datetime(2018, 1, 1, 12),
                    datetimes.utc_datetime(2018, 1, 1, 12, 30),
                )
            ),
            combinations.VacantSlot(
                room=None,
                interval=(
                    datetimes.utc_datetime(2018, 1, 1, 12, 30),
                    datetimes.utc_datetime(2018, 1, 1, 13),
                )
            )
        ]
    )
    assert combination.interval == (
        datetimes.utc_datetime(2018, 1, 1, 12),
        datetimes.utc_datetime(2018, 1, 1, 13)
    )
    assert combination.date_from == datetimes.utc_datetime(2018, 1, 1, 12)
    assert combination.date_to == datetimes.utc_datetime(2018, 1, 1, 13)


def test_get_event_ids_by_offices():
    raw_schedule = {
        'offices': [
            {
                'id': 1111,
                'resources': [
                    {'events': [{'eventId': 2}, {'eventId': 3}]},
                    {'events': [{'eventId': 4}]},
                ],
            },
            {
                'id': 2222,
                'resources': [
                    {'events': [{'eventId': 5}]},
                    {'events': [{}]},
                    {},
                ],
            },
            {},
        ]
    }
    actual = combinations.get_event_ids_by_offices(raw_schedule)
    expected = {
        1111: [2, 3, 4],
        2222: [5],
    }
    assert actual == expected


@pytest.mark.parametrize('event, expected', [
    (
        {
            'organizer': {'login': 'm-smirnov'},
            'attendees': [
                {'login': 'zhigalov'},
                {'login': 'mokhov'},
            ],
        },
        [
            {'login': 'zhigalov'},
            {'login': 'mokhov'},
            {'login': 'm-smirnov'},
        ],
    ),
    (
        {
            'attendees': [
                {'login': 'zhigalov'},
                {'login': 'mokhov'},
            ],
        },
        [
            {'login': 'zhigalov'},
            {'login': 'mokhov'},
        ],
    ),
    (
        {'organizer': {'login': 'm-smirnov'}},
        [{'login': 'm-smirnov'}],
    ),
])
def test_get_persons_from_event(event, expected):
    actual = combinations.get_persons_from_event(event)
    assert actual == expected


def test_get_persons_by_events():
    events = [
        {
            'id': 102,
            'organizer': {'login': 'm-smirnov'},
            'attendees': [
                {'login': 'zhigalov'},
                {'login': 'mokhov'},
                {'login': 'kiparis'},
            ],
        },
        {
            'id': 103,
            'organizer': {'login': 'kiparis'},
        },
    ]
    actual = combinations.get_persons_by_events(events)
    expected = {
        102: [
            {'login': 'zhigalov'},
            {'login': 'mokhov'},
            {'login': 'kiparis'},
            {'login': 'm-smirnov'},
        ],
        103: [
            {'login': 'kiparis'},
        ],
    }
    assert actual == expected


def test_get_persons_logins():
    persons_by_events = {
        2: [
            {'login': 'm-smirnov'},
            {'login': 'zhigalov'},
            {'login': 'mokhov'},
            {}
        ],
        3: [
            {'login': 'kiparis'},
            {'login': 'mokhov'},
        ],
    }
    actual = combinations.get_persons_logins(persons_by_events)
    expected = ['m-smirnov', 'zhigalov', 'mokhov', 'kiparis']
    assert set(actual) == set(expected)


@pytest.mark.parametrize('staff_person, calendar_person, expected', [
    (
        {'location': {'office': {'id': 1}}},
        {'officeId': 2},
        1
    ),
    (
        {'location': {'office': {'id': None}}},
        {'officeId': 2},
        2
    ),
    (
        {'location': {'office': {}}},
        {'officeId': 2},
        2
    ),
    (
        {'location': {}},
        {'officeId': 2},
        2
    ),
    (
        {},
        {'officeId': 2},
        2
    ),
])
def test_get_person_office_id(staff_person, calendar_person, expected):
    actual = combinations.get_person_office_id(
        staff_person=staff_person,
        calendar_person=calendar_person,
    )
    assert actual == expected


@pytest.mark.parametrize('staff_person, person_gaps, expected', [
    (
        {'official': {'is_dismissed': False}},
        [],
        False
    ),
    (
        {'official': {'is_dismissed': True}},
        [],
        True
    ),
    (
        {'official': {'is_dismissed': False}},
        [{'workflow': 'trip'}],
        True
    ),
])
def test_is_person_unavailable(staff_person, person_gaps, expected):
    actual = combinations.is_person_unavailable(
        staff_person=staff_person,
        person_gaps=person_gaps,
    )
    assert actual == expected


def test_get_persons_availability():
    calendar_persons_by_events = {
        2222: [
            {'login': 'aleksundra', 'officeId': 1, 'decision': 'yes'},
            {'login': 'kiparis', 'officeId': 1, 'decision': 'yes'},
            {'login': 'mokhov', 'officeId': 1, 'decision': 'maybe'},
            {'login': 'm-smirnov', 'officeId': 3, 'decision': 'yes'},
        ],
        3333: [
            {'login': 'mokhov', 'officeId': 1, 'decision': 'yes'},
            {'login': 'm-smirnov', 'officeId': 3, 'decision': 'no'},
            {'login': 'sharovio', 'officeId': 3, 'decision': 'yes'},
            {},
        ],
    }
    staff_persons_by_logins = {
        'aleksundra': {'location': {'office': {'id': 1}}, 'official': {'is_dismissed': False}},
        'kiparis': {'location': {'office': {'id': 1}}, 'official': {'is_dismissed': False}},
        'mokhov': {'location': {'office': {'id': 3}}, 'official': {'is_dismissed': False}},
        'm-smirnov': {'location': {'office': {'id': 3}}, 'official': {'is_dismissed': False}},
        'sharovio': {'location': {'office': {'id': 3}}, 'official': {'is_dismissed': True}},
    }
    persons_gap_by_logins = {
        'aleksundra': [],
        'm-smirnov': [],
        'sharovio': [],
        'mokhov': [{'person_login': 'mokhov', 'workflow': 'trip'}],
        'kiparis': [{'person_login': 'kiparis', 'workflow': 'absence'}],
    }
    event_ids_by_offices = {
        1: [2222],
        3: [2222, 3333],
    }
    actual = combinations.get_persons_availability(
        calendar_persons_by_events=calendar_persons_by_events,
        persons_gap_by_logins=persons_gap_by_logins,
        staff_persons_by_logins=staff_persons_by_logins,
        event_ids_by_offices=event_ids_by_offices,
    )
    expected = {
        1: {
            2222: persons_availability.PersonsAvailability(
                available=['aleksundra'],
                unavailable=['kiparis'],
            ),
        },
        3: {
            2222: persons_availability.PersonsAvailability(
                available=['m-smirnov'],
                unavailable=['mokhov'],
            ),
            3333: persons_availability.PersonsAvailability(
                unavailable=['mokhov', 'sharovio'],
            ),
        },
    }
    assert actual == expected


def test_merge_persons_availability():
    raw_schedule = {
        'offices': [
            {
                'id': 1,
                'resources': [
                    {'events': [{'eventId': 2222}, {'eventId': 3333}]},
                    {'events': [{'eventId': 4444}]},
                ],
            },
            {
                'id': 2,
                'resources': [
                    {'events': [{'eventId': 2222}, {'eventId': 5555}]},
                    {'events': [{}]},
                    {},
                ],
            },
            {},
        ]
    }
    persons_availability_2222_1 = persons_availability.PersonsAvailability(
        available=['mokhov'],
    )
    persons_availability_2222_2 = persons_availability.PersonsAvailability(
        available=['kiparis'],
    )
    persons_availability_3333 = persons_availability.PersonsAvailability(
        available=['m-smirnov']
    )
    persons_availability_4444 = persons_availability.PersonsAvailability(
        available=['sibirev']
    )
    persons_availability_5555 = persons_availability.PersonsAvailability(
        available=['zhigalov']
    )
    availability_by_events = {
        1: {
            2222: persons_availability_2222_1,
            3333: persons_availability_3333,
            4444: persons_availability_4444,
        },
        2: {
            2222: persons_availability_2222_2,
            5555: persons_availability_5555,
        },
    }
    combinations.merge_persons_availability(raw_schedule, availability_by_events)
    expected = {
        'offices': [
            {
                'id': 1,
                'resources': [
                    {'events': [
                        {'eventId': 2222, 'persons_availability': persons_availability_2222_1},
                        {'eventId': 3333, 'persons_availability': persons_availability_3333}
                    ]},
                    {'events': [{'eventId': 4444, 'persons_availability': persons_availability_4444}]},
                ]
            },
            {
                'id': 2,
                'resources': [
                    {'events': [
                        {'eventId': 2222, 'persons_availability': persons_availability_2222_2},
                        {'eventId': 5555, 'persons_availability': persons_availability_5555},
                    ]},
                    {'events': [{}]},
                    {},
                ],
            },
            {},
        ]
    }
    assert raw_schedule == expected


def test_filter_bookable_rooms():
    bookable_rooms = [
        'conf_rr_tea@yandex-team.ru',
        'conf_rr_coffee@yandex-team.ru',
    ]
    raw_schedule = {
        'offices': [
            {
                'id': 1,
                'resources': [
                    {'info': {'email': 'conf_rr_excursion@yandex-team.ru'}},
                    {'info': {'email': 'conf_rr_tea@yandex-team.ru'}},
                ],
            },
            {
                'id': 2,
                'resources': [
                    {'info': {'email': 'conf_rr_coffee@yandex-team.ru'}},
                    {'info': {}},
                    {}
                ],
            },
            {},
        ]
    }
    combinations.filter_bookable_rooms(
        raw_schedule=raw_schedule,
        bookable_rooms=bookable_rooms,
        interval=INTERVAL_EXAMPLE_ONE,
    )
    expected = {
        'offices': [
            {
                'id': 1,
                'resources': [
                    {'info': {'email': 'conf_rr_tea@yandex-team.ru'}},
                ],
            },
            {
                'id': 2,
                'resources': [
                    {'info': {'email': 'conf_rr_coffee@yandex-team.ru'}},
                ],
            },
            {
                'resources': []
            },
        ]
    }
    assert raw_schedule == expected


def _shifted_start_str(start=INTERVAL_EXAMPLE_ONE[0], **kwargs):
    dt = start + timedelta(**kwargs)
    return dt.strftime(settings.CALENDAR_DATETIME_FORMAT)


@pytest.mark.parametrize('restrictions', (
    # Начался до
    [{'start': _shifted_start_str(days=-1)}],
    # Закончится после
    [{'end': _shifted_start_str(days=1)}],
    # Лежит внутри
    [{
        'start': _shifted_start_str(minutes=15),
        'end': _shifted_start_str(minutes=45),
    }],
    # Интервалов может быть больше одного. Один вне, один внутри
    [
        {'start': _shifted_start_str(days=1)},
        {
            'start': _shifted_start_str(minutes=15),
            'end': _shifted_start_str(minutes=45),
        },
    ],
    # Вечная недоступность
    [{}],
))
def test_filter_bookable_rooms_restrictions_failure(restrictions):
    raw_schedule = {
        'offices': [
            {
                'id': 1,
                'resources': [
                    {
                        'info': {'email': 'conf_rr_tea@yandex-team.ru'},
                        'restrictions': restrictions,
                    },
                ],
            },
        ]
    }
    expected = deepcopy(raw_schedule)
    expected['offices'][0]['resources'] = []

    combinations.filter_bookable_rooms(
        raw_schedule=raw_schedule,
        bookable_rooms=['conf_rr_tea@yandex-team.ru'],
        interval=INTERVAL_EXAMPLE_ONE,
    )

    assert raw_schedule == expected


def test_filter_bookable_rooms_restrictions_success():
    # Два интервала вне
    restrictions = [
        {
            'start': _shifted_start_str(days=-1),
            'end': _shifted_start_str(hours=-1),
        },
        {
            'start': _shifted_start_str(days=1),
            'end': _shifted_start_str(days=2),
        },
    ]
    raw_schedule = {
        'offices': [
            {
                'id': 1,
                'resources': [
                    {
                        'info': {'email': 'conf_rr_tea@yandex-team.ru'},
                        'restrictions': restrictions,
                    },
                ],
            },
        ]
    }
    expected = deepcopy(raw_schedule)

    combinations.filter_bookable_rooms(
        raw_schedule=raw_schedule,
        bookable_rooms=['conf_rr_tea@yandex-team.ru'],
        interval=INTERVAL_EXAMPLE_ONE,
    )

    assert raw_schedule == expected


def test_booked_slots_hashability():
    first = dict(
        room=ROOM_EXAMPLE,
        interval=INTERVAL_EXAMPLE_ONE,
        event=RESOURCE_EVENT_EXAMPLE,
    )
    second = dict(
        room=ROOM_EXAMPLE,
        interval=INTERVAL_EXAMPLE_TWO,
        event=RESOURCE_EVENT_EXAMPLE,
    )
    obj_one = combinations.BookedSlot(**first)
    obj_two_same = combinations.BookedSlot(**first)
    obj_three = combinations.BookedSlot(**second)

    obj_set = {obj_one, obj_two_same, obj_three}
    assert sorted(obj_set) == sorted([
        obj_one,
        obj_three
    ])


def test_vacant_slots_hashability():
    first = dict(
        room=ROOM_EXAMPLE,
        interval=INTERVAL_EXAMPLE_ONE,
    )
    second = dict(
        room=ROOM_EXAMPLE,
        interval=INTERVAL_EXAMPLE_TWO,
    )
    obj_one = combinations.VacantSlot(**first)
    obj_two_same = combinations.VacantSlot(**first)
    obj_three = combinations.VacantSlot(**second)

    obj_set = {obj_one, obj_two_same, obj_three}
    assert sorted(obj_set) == sorted([
        obj_one,
        obj_three
    ])


def test_combinations_hashability():
    first = dict(
        slots=(
            combinations.VacantSlot(
                room=ROOM_EXAMPLE,
                interval=INTERVAL_EXAMPLE_ONE,
            ),
            combinations.BookedSlot(
                room=ROOM_EXAMPLE,
                interval=INTERVAL_EXAMPLE_TWO,
                event=RESOURCE_EVENT_EXAMPLE,
            )
        )
    )
    second = dict(
        slots=(
            combinations.BookedSlot(
                room=ROOM_EXAMPLE,
                interval=INTERVAL_EXAMPLE_ONE,
                event=RESOURCE_EVENT_EXAMPLE,
            ),
            combinations.VacantSlot(
                room=ROOM_EXAMPLE,
                interval=INTERVAL_EXAMPLE_TWO,
            )
        )
    )
    obj_one = combinations.Combination(**first)
    obj_two_same = combinations.Combination(**first)
    obj_three = combinations.Combination(**second)

    obj_set = {obj_one, obj_two_same, obj_three}
    assert sorted(obj_set) == sorted([
        obj_one,
        obj_three
    ])
