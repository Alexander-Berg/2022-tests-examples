# coding: utf-8

from __future__ import unicode_literals

import pytest

from easymeeting.lib import datetimes
from easymeeting.core import schedules
from easymeeting.core import rooms
from easymeeting.core import resource_event
from easymeeting.core import persons_availability

from tests import helpers


DN = helpers.DatetimeNames(2018, 1, 1)


FIRST_PERSONS_AVAILABILITY_EXAMPLE = persons_availability.PersonsAvailability(
    available=['mokhov', 'm-smirnov'],
    unavailable=['zhigalov'],
)
SECOND_PERSONS_AVAILABILITY_EXAMPLE = persons_availability.PersonsAvailability(
    available=['kiparis', 'sibirev'],
)
RESOURCE_EVENTS_EXAMPLE = [
    {
        'eventId': 10000,
        'start': '2018-01-01T12:00:00',
        'end': '2018-01-01T13:00:00',
        'persons_availability': FIRST_PERSONS_AVAILABILITY_EXAMPLE,
    },
    {
        'eventId': 10001,
        'start': '2018-01-01T16:00:00',
        'end': '2018-01-01T18:00:00',
        'persons_availability': SECOND_PERSONS_AVAILABILITY_EXAMPLE,
    },
]

RESOURCE_EXAMPLE = {
    'info': {
        'id': 100,
        'name': '3.Гималаи',
        'email': 'conf_ekb_himalaya@yandex-team.ru',
    },
    'events': RESOURCE_EVENTS_EXAMPLE,
}


@pytest.mark.parametrize('interval, expected', [
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 14),
            datetimes.utc_datetime(2018, 1, 1, 15),
        ),
        True,
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 13),
            datetimes.utc_datetime(2018, 1, 1, 15),
        ),
        True,
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 15),
            datetimes.utc_datetime(2018, 1, 1, 16),
        ),
        True,
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 12),
            datetimes.utc_datetime(2018, 1, 1, 13),
        ),
        False,
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 12),
            datetimes.utc_datetime(2018, 1, 1, 18),
        ),
        False,
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 12, 30),
            datetimes.utc_datetime(2018, 1, 1, 13, 30),
        ),
        False,
    ),
])
def test_schedule_is_available_at(interval, expected):
    schedule = schedules.RoomSchedule.from_raw_resource(RESOURCE_EXAMPLE)
    assert schedule.is_available_at(interval) == expected


@pytest.mark.parametrize('interval, expected', [
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 12),
            datetimes.utc_datetime(2018, 1, 1, 18),
        ),
        [
            (
                schedules.RoomSlice(
                    interval=(
                        datetimes.utc_datetime(2018, 1, 1, 13),
                        datetimes.utc_datetime(2018, 1, 1, 16),
                    ),
                )
            ),
        ],
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 12),
            datetimes.utc_datetime(2018, 1, 1, 14),
        ),
        [
            schedules.RoomSlice(
                interval=(
                    datetimes.utc_datetime(2018, 1, 1, 13),
                    datetimes.utc_datetime(2018, 1, 1, 14),
                ),
            ),
        ],
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 12),
            datetimes.utc_datetime(2018, 1, 1, 13),
        ),
        [],
    ),
])
def test_schedule_get_available_intersections(interval, expected):
    schedule = schedules.RoomSchedule.from_raw_resource(RESOURCE_EXAMPLE)
    assert schedule.get_vacant_intersections(interval) == expected


@pytest.mark.parametrize('interval, expected', [
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 12),
            datetimes.utc_datetime(2018, 1, 1, 18),
        ),
        [
            schedules.RoomSlice(
                interval=(
                    datetimes.utc_datetime(2018, 1, 1, 12),
                    datetimes.utc_datetime(2018, 1, 1, 13),
                ),
                event=resource_event.ResourceEvent(
                    interval=(
                        datetimes.utc_datetime(2018, 1, 1, 12),
                        datetimes.utc_datetime(2018, 1, 1, 13),
                    ),
                    persons_availability=FIRST_PERSONS_AVAILABILITY_EXAMPLE,
                    event_id=10000,
                )
            ),
            schedules.RoomSlice(
                interval=(
                    datetimes.utc_datetime(2018, 1, 1, 16),
                    datetimes.utc_datetime(2018, 1, 1, 18),
                ),
                event=resource_event.ResourceEvent(
                    interval=(
                        datetimes.utc_datetime(2018, 1, 1, 16),
                        datetimes.utc_datetime(2018, 1, 1, 18),
                    ),
                    persons_availability=SECOND_PERSONS_AVAILABILITY_EXAMPLE,
                    event_id=10001,
                )
            ),
        ],
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 12),
            datetimes.utc_datetime(2018, 1, 1, 14),
        ),
        [
            schedules.RoomSlice(
                interval=(
                    datetimes.utc_datetime(2018, 1, 1, 12),
                    datetimes.utc_datetime(2018, 1, 1, 13),
                ),
                event=resource_event.ResourceEvent(
                    interval=(
                        datetimes.utc_datetime(2018, 1, 1, 12),
                        datetimes.utc_datetime(2018, 1, 1, 13),
                    ),
                    persons_availability=FIRST_PERSONS_AVAILABILITY_EXAMPLE,
                    event_id=10000,
                ),
            ),
        ],
    ),
    (
        (
            datetimes.utc_datetime(2018, 1, 1, 13),
            datetimes.utc_datetime(2018, 1, 1, 16),
        ),
        [],
    ),
])
def test_schedule_get_booked_intersections(interval, expected):
    schedule = schedules.RoomSchedule.from_raw_resource(RESOURCE_EXAMPLE)
    assert schedule.get_booked_intersections(interval) == expected


def test_room_data_get_vacant_coverage():
    room_data = schedules.RoomData(
        room=None,
        vacant=[
            schedules.RoomSlice(
                interval=(
                    DN.T_12_00,
                    DN.T_12_30,
                ),
            ),
            schedules.RoomSlice(
                interval=(
                    DN.T_12_30,
                    DN.T_12_45,
                ),
            ),
        ]
    )
    assert room_data.get_vacant_coverage(
        interval=(DN.T_12_00, DN.T_13_00)
    ) == 0.75


def test_office_schedule_get_rooms_with_intervals():
    room = rooms.Room(1, 'a', 'a@a')
    event_one, event_two = [
        resource_event.ResourceEvent.from_raw_event(**raw_event)
        for raw_event in RESOURCE_EVENTS_EXAMPLE
    ]
    office_schedule = schedules.OfficeSchedule(
        room_schedules=[
            schedules.RoomSchedule(
                room=room,
                events=[event_one, event_two]
            )
        ]
    )
    rooms_with_intervals = office_schedule.get_rooms_with_intervals(
        interval=(DN.T_12_00, DN.T_18_00)
    )
    assert rooms_with_intervals == [
        schedules.RoomData(
            room=room,
            vacant=[
                schedules.RoomSlice(
                    interval=(DN.T_13_00, DN.T_16_00)
                ),
            ],
            booked=[
                schedules.RoomSlice(
                    interval=(DN.T_12_00, DN.T_13_00),
                    event=event_one,
                ),
                schedules.RoomSlice(
                    interval=(DN.T_16_00, DN.T_18_00),
                    event=event_two,
                ),
            ]
        )
    ]


@pytest.mark.parametrize('string, obj', [
    ('-wat', schedules.Order('wat', reverse=True)),
    ('wat', schedules.Order('wat', reverse=False)),
    ('+wat', schedules.Order('wat', reverse=False)),
])
def test_order_class(string, obj):
    assert schedules.Order.from_str(string) == obj


@pytest.mark.parametrize('string, expected', [
    ('-wat', True),
    ('wat', True),
    ('+wat', True),
    (None, False),
])
def test_order_class_bool(string, expected):
    assert bool(schedules.Order.from_str(string)) == expected
