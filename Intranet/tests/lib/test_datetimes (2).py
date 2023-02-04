# coding: utf-8
import datetime
import pytz
import pytest

from review.lib import datetimes


@pytest.mark.parametrize('dt, expected', [
    (
        '2018-01-01',
        datetime.date(2017, 12, 27),
    ),
    (
        datetime.date(2018, 1, 1),
        datetime.date(2017, 12, 27),
    ),
    (
        datetime.datetime(2018, 1, 1, 12, 0, 0),
        datetime.datetime(2017, 12, 27, 12, 0, 0),
    ),
    (
        datetime.datetime(2018, 1, 1, 12, 0, 0, tzinfo=pytz.utc),
        datetime.datetime(2017, 12, 27, 12, 0, 0, tzinfo=pytz.utc),
    ),
    (
        '2018-01-01 12:00:00',
        datetime.datetime(2017, 12, 27, 12, 0, 0),
    ),
    (
        '2018-01-01 12:00:00+0000',
        datetime.datetime(2017, 12, 27, 12, 0, 0, tzinfo=pytz.utc),
    ),
])
def test_shifted_five_days_ago(dt, expected):
    assert datetimes.shifted(dt, days=-5) == expected
