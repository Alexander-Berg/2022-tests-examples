# coding: utf-8

from __future__ import unicode_literals

import pytest

from easymeeting.core import schedules
from easymeeting.core import rooms
from easymeeting.core import resource_event
from easymeeting.core import combinations

from tests import helpers


DN = helpers.DatetimeNames()


ROOM_A = rooms.Room(
    id=1,
    display_name='A',
    exchange_name='a@dummy'
)
ROOM_B = rooms.Room(
    id=2,
    display_name='B',
    exchange_name='b@dummy'
)
ROOM_C = rooms.Room(
    id=3,
    display_name='C',
    exchange_name='c@dummy'
)
ROOM_D = rooms.Room(
    id=4,
    display_name='D',
    exchange_name='d@dummy'
)
ROOM_E = rooms.Room(
    id=5,
    display_name='E',
    exchange_name='e@dummy'
)

MEETING_IN_A = resource_event.ResourceEvent(interval=(DN.T_12_00, DN.T_13_00))
MEETING_IN_B = resource_event.ResourceEvent(interval=(DN.T_11_30, DN.T_12_30))
MEETING_IN_C = resource_event.ResourceEvent(interval=(DN.T_12_30, DN.T_13_30))
MEETING_IN_E = resource_event.ResourceEvent(interval=(DN.T_12_30, DN.T_13_00))

"""
    |11:30   |12:00   |12:30   |13:00   |
A   |        |########|########|        |
B   |########|########|        |        |
C   |        |        |########|########|
D   |        |        |        |        |
E   |        |        |########|        |
"""

OFFICE_SCHEDULE = schedules.OfficeSchedule(
    room_schedules=[
        schedules.RoomSchedule(
            room=ROOM_A,
            events=[
                MEETING_IN_A,
            ],
        ),
        schedules.RoomSchedule(
            room=ROOM_B,
            events=[
                MEETING_IN_B,
            ],
        ),
        schedules.RoomSchedule(
            room=ROOM_C,
            events=[
                MEETING_IN_C,
            ],
        ),
        schedules.RoomSchedule(
            room=ROOM_D,
            events=[],
        ),
        schedules.RoomSchedule(
            room=ROOM_E,
            events=[
                MEETING_IN_E,
            ],
        ),
    ],
)


EXPECTED_COMBINATIONS_12_TO_13 = [
    combinations.Combination(
        slots=[
            combinations.BookedSlot(
                room=ROOM_A,
                interval=(DN.T_12_00, DN.T_13_00),
                event=MEETING_IN_A,
            ),
        ],
    ),
    combinations.Combination(
        slots=[
            combinations.BookedSlot(
                room=ROOM_B,
                interval=(
                    DN.T_12_00,
                    DN.T_12_30,
                ),
                event=MEETING_IN_B,
            ),
            combinations.VacantSlot(
                room=ROOM_B,
                interval=(
                    DN.T_12_30,
                    DN.T_13_00
                ),
            ),
        ],
    ),
    combinations.Combination(
        slots=[
            combinations.VacantSlot(
                room=ROOM_C,
                interval=(
                    DN.T_12_00,
                    DN.T_12_30
                ),
            ),
            combinations.BookedSlot(
                room=ROOM_C,
                interval=(
                    DN.T_12_30,
                    DN.T_13_00,
                ),
                event=MEETING_IN_C,
            ),
        ],
    ),
    combinations.Combination(
        slots=[
            combinations.VacantSlot(
                room=ROOM_D,
                interval=(
                    DN.T_12_00,
                    DN.T_13_00
                ),
            ),
        ],
    ),
    combinations.Combination(
        slots=[
            combinations.VacantSlot(
                room=ROOM_E,
                interval=(
                    DN.T_12_00,
                    DN.T_12_30
                ),
            ),
            combinations.BookedSlot(
                room=ROOM_E,
                interval=(
                    DN.T_12_30,
                    DN.T_13_00,
                ),
                event=MEETING_IN_E,
            ),
        ],
    ),
    combinations.Combination(
        slots=[
            combinations.VacantSlot(
                room=ROOM_C,
                interval=(
                    DN.T_12_00,
                    DN.T_12_30
                ),
            ),
            combinations.VacantSlot(
                room=ROOM_B,
                interval=(
                    DN.T_12_30,
                    DN.T_13_00,
                ),
            ),
        ],
    ),
]

EXPECTED_COMBINATIONS_12_TO_1330 = [
    combinations.Combination(
        slots=[
            combinations.VacantSlot(
                room=ROOM_E,
                interval=(
                    DN.T_12_00,
                    DN.T_12_30
                ),
            ),
            combinations.VacantSlot(
                room=ROOM_D,
                interval=(
                    DN.T_12_30,
                    DN.T_13_00,
                ),
            ),
            combinations.VacantSlot(
                room=ROOM_E,
                interval=(
                    DN.T_13_00,
                    DN.T_13_30
                ),
            ),
        ],
    ),
    combinations.Combination(
        slots=[
            combinations.VacantSlot(
                room=ROOM_E,
                interval=(
                    DN.T_12_00,
                    DN.T_12_30
                ),
            ),
            combinations.VacantSlot(
                room=ROOM_B,
                interval=(
                    DN.T_12_30,
                    DN.T_13_00,
                ),
            ),
            combinations.VacantSlot(
                room=ROOM_E,
                interval=(
                    DN.T_13_00,
                    DN.T_13_30
                ),
            ),
        ],
    ),
]


@pytest.mark.parametrize(
    argnames='interval, expected',
    argvalues=[
        ((DN.T_12_00, DN.T_13_00), comb)
        for comb in EXPECTED_COMBINATIONS_12_TO_13
    ] + [
        ((DN.T_12_00, DN.T_13_30), comb)
        for comb in EXPECTED_COMBINATIONS_12_TO_1330
    ]
)
def test_expected_combinations_exist_full(interval, expected):
    fetched_combinations = combinations.prepare_office_combinations(
        interval=interval,
        office_schedule=OFFICE_SCHEDULE,
    )
    assert fetched_combinations.count(expected) == 1
