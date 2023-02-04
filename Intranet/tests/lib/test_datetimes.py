# coding: utf-8

from __future__ import unicode_literals

import pytz
from decimal import Decimal
from datetime import datetime as dt, time as t
from functools import partial

from easymeeting.lib import datetimes

import pytest


# UTC datetime & time
udt = partial(dt, tzinfo=pytz.utc)
ut = partial(t, tzinfo=pytz.utc)

MSK_TIMEZONE = pytz.timezone('Europe/Moscow')


@pytest.mark.parametrize('dt_str, expected', [
    (
        "2018-02-14T12:00:00",
        udt(2018, 2, 14, 12, 0)
    ),
    (None, None)
])
def test_parse_calendar_dt(dt_str, expected):
    return datetimes.parse_calendar_datetime_str(dt_str) == expected


@pytest.mark.parametrize('dt_str, expected', [
    (
        "2018-02-14T12:00:00",
        udt(2018, 2, 14, 12, 0)
    ),
    (None, None)
])
def test_parse_gap_datetime_str(dt_str, expected):
    return datetimes.parse_gap_datetime_str(dt_str) == expected


@pytest.mark.parametrize('first, second, expected', [
    (
        (
            udt(2018, 1, 1, 12, 0, 0),
            udt(2018, 1, 1, 16, 0, 0),
        ),
        (
            udt(2018, 1, 1, 18, 0, 0),
            udt(2018, 1, 1, 20, 0, 0),
        ),
        None,
    ),
    (
        (
            udt(2018, 1, 1, 12, 0, 0),
            udt(2018, 1, 1, 16, 0, 0),
        ),
        (
            udt(2018, 1, 1, 16, 0, 0),
            udt(2018, 1, 1, 20, 0, 0),
        ),
        None,
    ),
    (
        (
            udt(2018, 1, 1, 12, 0, 0),
            udt(2018, 1, 1, 18, 0, 0),
        ),
        (
            udt(2018, 1, 1, 16, 0, 0),
            udt(2018, 1, 1, 20, 0, 0),
        ),
        (
            udt(2018, 1, 1, 16, 0, 0),
            udt(2018, 1, 1, 18, 0, 0),
        ),
    ),
    (
        (
            udt(2018, 1, 1, 12, 0, 0),
            udt(2018, 1, 1, 20, 0, 0),
        ),
        (
            udt(2018, 1, 1, 16, 0, 0),
            udt(2018, 1, 1, 18, 0, 0),
        ),
        (
            udt(2018, 1, 1, 16, 0, 0),
            udt(2018, 1, 1, 18, 0, 0),
        ),
    ),
])
def test_get_intersection(first, second, expected):
    assert datetimes.get_intersection(first, second) == expected


BIG_INTERVAL = (
    udt(2018, 1, 1, 12, 0, 0),
    udt(2018, 1, 1, 20, 0, 0),
)

SUBSCTRACT_ONE_EXAMPLES = [
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 21, 0, 0),
            udt(2018, 1, 1, 22, 0, 0),
        ),
        [BIG_INTERVAL],
    ),
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 20, 0, 0),
            udt(2018, 1, 1, 22, 0, 0),
        ),
        [BIG_INTERVAL],
    ),
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 11, 0, 0),
            udt(2018, 1, 1, 12, 0, 0),
        ),
        [BIG_INTERVAL],
    ),
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 11, 0, 0),
            udt(2018, 1, 1, 13, 0, 0),
        ),
        [
            (
                udt(2018, 1, 1, 13, 0, 0),
                udt(2018, 1, 1, 20, 0, 0),
            ),
        ],
    ),
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 12, 0, 0),
            udt(2018, 1, 1, 13, 0, 0),
        ),
        [
            (
                udt(2018, 1, 1, 13, 0, 0),
                udt(2018, 1, 1, 20, 0, 0),
            ),
        ],
    ),
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 19, 0, 0),
            udt(2018, 1, 1, 21, 0, 0),
        ),
        [
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 19, 0, 0),
            ),
        ],
    ),
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 20, 0, 0),
            udt(2018, 1, 1, 21, 0, 0),
        ),
        [
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 20, 0, 0),
            ),
        ],
    ),
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 14, 0, 0),
            udt(2018, 1, 1, 16, 0, 0),
        ),
        [
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 14, 0, 0),
            ),
            (
                udt(2018, 1, 1, 16, 0, 0),
                udt(2018, 1, 1, 20, 0, 0),
            ),
        ],
    ),
    (
        BIG_INTERVAL,
        (
            udt(2018, 1, 1, 19, 0, 0),
            udt(2018, 1, 1, 21, 0, 0),
        ),
        [
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 19, 0, 0),
            ),
        ],
    ),
    (
        BIG_INTERVAL,
        BIG_INTERVAL,
        [datetimes.EMPTY_INTERVAL],
    ),
]


@pytest.mark.parametrize(
    'minuend, subtrahend, expected',
    SUBSCTRACT_ONE_EXAMPLES
)
def test_substract_interval(minuend, subtrahend, expected):
    assert datetimes.substract_interval(minuend, subtrahend) == expected


@pytest.mark.parametrize(
    'minuend, subtrahend, expected',
    SUBSCTRACT_ONE_EXAMPLES
)
def test_substract_intervals_one(minuend, subtrahend, expected):
    assert datetimes.substract_intervals(minuend, [subtrahend]) == expected


@pytest.mark.parametrize('minuend, subtrahends_list, expected', [
    (
        BIG_INTERVAL,
        [
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 14, 0, 0),
            ),
            (
                udt(2018, 1, 1, 16, 0, 0),
                udt(2018, 1, 1, 20, 0, 0),
            ),
        ],
        [
            (
                udt(2018, 1, 1, 14, 0, 0),
                udt(2018, 1, 1, 16, 0, 0),
            ),
        ],
    ),
    (
        BIG_INTERVAL,
        [
            (
                udt(2018, 1, 1, 14, 0, 0),
                udt(2018, 1, 1, 15, 0, 0),
            ),
            (
                udt(2018, 1, 1, 16, 0, 0),
                udt(2018, 1, 1, 17, 0, 0),
            ),
        ],
        [
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 14, 0, 0),
            ),
            (
                udt(2018, 1, 1, 15, 0, 0),
                udt(2018, 1, 1, 16, 0, 0),
            ),
            (
                udt(2018, 1, 1, 17, 0, 0),
                udt(2018, 1, 1, 20, 0, 0),
            ),
        ],
    ),
])
def test_subsctract_intervals(minuend, subtrahends_list, expected):
    assert datetimes.substract_intervals(minuend, subtrahends_list) == expected


@pytest.mark.parametrize(
    'first, second, expected',
    [
        (
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 13, 0, 0),
            ),
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 14, 0, 0),
            ),
            Decimal('0.5'),
        ),
        (
            (
                udt(2018, 1, 1, 12, 30, 0),
                udt(2018, 1, 1, 13, 0, 0),
            ),
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 14, 0, 0),
            ),
            Decimal('0.25'),
        ),
    ]
)
def test_get_part_of_interval(first, second, expected):
    assert datetimes.get_part_of_interval(first, second) == expected


@pytest.mark.parametrize(
    'inclusive, included, expected',
    [
        (
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 13, 0, 0),
            ),
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 14, 0, 0),
            ),
            False,
        ),
        (
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 13, 0, 0),
            ),
            (
                udt(2018, 1, 1, 12, 0, 0),
                udt(2018, 1, 1, 12, 30, 0),
            ),
            True,
        ),
    ]
)
def test_is_include_interval(inclusive, included, expected):
    assert datetimes.is_include_interval(inclusive, included) == expected


@pytest.mark.parametrize(
    'round_func, round_to, initial, expected',
    [
        (
            datetimes.round_time,
            30,
            udt(2018, 1, 1, 12, 10, 43, 0),
            udt(2018, 1, 1, 12, 10, 30, 0),
        ),
        (
            datetimes.round_time,
            30,
            udt(2018, 1, 1, 12, 10, 48, 0),
            udt(2018, 1, 1, 12, 11, 0, 0),
        ),
        (
            datetimes.round_time,
            5 * 60 * 60,  # 5 часов
            udt(2018, 1, 1, 23, 10, 32, 120),
            udt(2018, 1, 2, 1, 0, 0, 0),
        ),
        (
            datetimes.ceil_time,
            30 * 60,  # 30 минут
            udt(2018, 1, 1, 12, 10, 43, 0),
            udt(2018, 1, 1, 12, 30, 0, 0),
        ),
        (
            datetimes.floor_time,
            12 * 60 * 60,  # 12 часов
            udt(2018, 1, 1, 15, 10, 43, 0),
            udt(2018, 1, 1, 12, 0, 0, 0),
        ),
    ]
)
def test_round_time(round_func, round_to, initial, expected):
    assert round_func(initial, round_to) == expected


@pytest.mark.parametrize('data', [
    {
        'start': udt(2018, 1, 1, 0, 0),
        'end': udt(2018, 1, 1, 1, 0),
        'duration': 60,
        'busy_intervals': [],
        'result': [
            (
                udt(2018, 1, 1, 0, 0),
                udt(2018, 1, 1, 1, 0),
            ),
        ],
    },
    {
        'start': udt(2018, 1, 1, 0, 0),
        'end': udt(2018, 1, 1, 1, 0),
        'duration': 15,
        'busy_intervals': [],
        'result': [
            (
                udt(2018, 1, 1, 0, 0),
                udt(2018, 1, 1, 0, 15),
            ),
            (
                udt(2018, 1, 1, 0, 15),
                udt(2018, 1, 1, 0, 30),
            ),
            (
                udt(2018, 1, 1, 0, 30),
                udt(2018, 1, 1, 0, 45),
            ),
            (
                udt(2018, 1, 1, 0, 45),
                udt(2018, 1, 1, 1, 0),
            ),
        ],
    },
    {
        'start': udt(2018, 1, 1, 0, 0),
        'end': udt(2018, 1, 1, 1, 0),
        'duration': 20,
        'busy_intervals': [
            (
                udt(2018, 1, 1, 0, 15),
                udt(2018, 1, 1, 0, 45),
            )
        ],
        'result': [],
    },
    {
        'start': udt(2018, 1, 1, 0, 0),
        'end': udt(2018, 1, 1, 1, 0),
        'duration': 20,
        'busy_intervals': [
            (
                udt(2018, 1, 1, 0, 20),
                udt(2018, 1, 1, 0, 40),
            )
        ],
        'result': [
            (
                udt(2018, 1, 1, 0, 0),
                udt(2018, 1, 1, 0, 20),
            ),
        ],
    },
])
def test_generate_free_intervals(data):
    result = data.pop('result')
    assert list(datetimes.generate_free_intervals(step=15, **data)) == result


@pytest.mark.parametrize(
    'dt, t, expected',
    [
        (
            # Aware
            udt(2019, 2, 1, 12, 32, 31),
            ut(14, 0, 15),
            udt(2019, 2, 1, 14, 0, 15),
        ),
        (
            # Naive
            dt(2019, 2, 1, 12, 32, 31),
            t(14, 0, 15),
            dt(2019, 2, 1, 14, 0, 15),
        ),
        (
            # Разные часовые пояса
            udt(2019, 2, 1, 12, 32, 31).astimezone(MSK_TIMEZONE),
            ut(14, 1, 15),
            udt(2019, 2, 1, 14, 1, 15).astimezone(MSK_TIMEZONE),
        ),
    ],
)
def test_set_time(dt, t, expected):
    assert datetimes.set_time(dt, t) == expected


@pytest.mark.parametrize('data', [
    {
        'start': udt(2018, 1, 1, 0, 0),
        'end': udt(2018, 1, 3, 0, 0),
        'time_start': ut(16, 00),
        'time_end': ut(20, 00),
        'include_partial': True,
        'result': [
            (
                udt(2018, 1, 1, 16, 0),
                udt(2018, 1, 1, 20, 0),
            ),
            (
                udt(2018, 1, 2, 16, 0),
                udt(2018, 1, 2, 20, 0),
            ),
        ],
    },
    {
        'start': udt(2018, 1, 1, 0, 0),
        'end': udt(2018, 1, 3, 0, 0),
        'time_start': ut(20, 00),
        'time_end': ut(16, 00),
        'include_partial': True,
        'result': [
            (
                udt(2018, 1, 1, 0, 0),
                udt(2018, 1, 1, 16, 0),
            ),
            (
                udt(2018, 1, 1, 20, 0),
                udt(2018, 1, 2, 16, 0),
            ),
            (
                udt(2018, 1, 2, 20, 0),
                udt(2018, 1, 3, 0, 0),
            ),
        ],
    },
    {
        'start': udt(2018, 1, 1, 0, 0),
        'end': udt(2018, 1, 3, 0, 0),
        'time_start': ut(20, 00),
        'time_end': ut(16, 00),
        'include_partial': False,
        'result': [
            (
                udt(2018, 1, 1, 20, 0),
                udt(2018, 1, 2, 16, 0),
            ),
        ],
    },
    {
        'start': udt(2018, 1, 1, 15, 0),
        'end': udt(2018, 1, 3, 15, 0),
        'time_start': ut(10, 00),
        'time_end': ut(16, 00),
        'include_partial': False,
        'result': [
            (
                udt(2018, 1, 2, 10, 0),
                udt(2018, 1, 2, 16, 0),
            ),
        ],
    },
    {
        'start': udt(2018, 1, 1, 15, 0),
        'end': udt(2018, 1, 3, 15, 0),
        'time_start': ut(10, 00),
        'time_end': ut(16, 00),
        'include_partial': True,
        'result': [
            (
                udt(2018, 1, 1, 15, 0),
                udt(2018, 1, 1, 16, 0),
            ),
            (
                udt(2018, 1, 2, 10, 0),
                udt(2018, 1, 2, 16, 0),
            ),
            (
                udt(2018, 1, 3, 10, 0),
                udt(2018, 1, 3, 15, 0),
            ),
        ],
    },
])
def test_generate_datetime_intervals_by_time(data):
    expected = data.pop('result')
    actual = list(datetimes.generate_datetime_intervals_by_time(**data))
    assert actual == expected


@pytest.mark.parametrize('start,end,expected', [
    (
        udt(2018, 8, 23, 15, 0),
        udt(2018, 9, 1, 14, 0),
        [
            (udt(2018, 8, 25), udt(2018, 8, 26)),
            (udt(2018, 8, 26), udt(2018, 8, 27)),
            (udt(2018, 9, 1), udt(2018, 9, 2)),
            (udt(2018, 9, 2), udt(2018, 9, 3)),
        ],
    ),
    (
        udt(2018, 8, 24, 15, 0),
        udt(2018, 8, 25, 14, 0),
        [
            (udt(2018, 8, 25), udt(2018, 8, 26)),
            (udt(2018, 8, 26), udt(2018, 8, 27)),
        ],
    ),
    (
        udt(2018, 8, 26, 15, 0),
        udt(2018, 8, 28, 14, 0),
        [
            (udt(2018, 8, 26), udt(2018, 8, 27)),
        ],
    ),
])
def test_get_holidays(start, end, expected):
    actual = datetimes.get_holidays(start, end)
    assert actual == expected
