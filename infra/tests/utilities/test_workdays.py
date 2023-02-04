"""Test workday utility."""
import datetime

import pytest

from walle.util.workdays import (
    WEEKDAY_MON,
    WEEKDAY_TUE,
    WEEKDAY_WED,
    WEEKDAY_SUN,
    to_timestamp,
    next_working_hour_timestamp,
)

# 2 Jun 2020, Tuesday
TODAY_DATE = datetime.date(2020, 6, 2)


@pytest.mark.parametrize(
    "time",
    [
        datetime.time(12, 15),
        datetime.time(12, 15, 1),
        datetime.time(12, 15, 59),
        datetime.time(12, 16, 0),
    ],
)
@pytest.mark.parametrize(
    "working_days",
    [
        {WEEKDAY_MON, WEEKDAY_TUE},
        {WEEKDAY_MON, WEEKDAY_TUE, WEEKDAY_WED},
        {WEEKDAY_TUE, WEEKDAY_WED},
        {WEEKDAY_TUE},
    ],
)
def test_now_is_working_time(time, working_days):
    wh_start, wh_end = datetime.time(12, 15), datetime.time(12, 16)
    ts = to_timestamp(datetime.datetime.combine(TODAY_DATE, time))
    assert ts == next_working_hour_timestamp(ts, working_days, wh_start, wh_end)


def test_workday_afterhours_next_working_time_tomorrow():
    working_days = {WEEKDAY_MON, WEEKDAY_TUE, WEEKDAY_WED}
    wh_start, wh_end = datetime.time(9, 0), datetime.time(19, 0)

    ts = to_timestamp(datetime.datetime.combine(TODAY_DATE, datetime.time(19, 0, 1)))
    working_time = to_timestamp(datetime.datetime.combine(TODAY_DATE, wh_start) + datetime.timedelta(days=1))

    assert working_time == next_working_hour_timestamp(ts, working_days, wh_start, wh_end)


def test_workday_beforehours_next_working_time_today():
    working_days = {WEEKDAY_MON, WEEKDAY_TUE, WEEKDAY_WED}
    wh_start, wh_end = datetime.time(9, 0), datetime.time(19, 0)

    ts = to_timestamp(datetime.datetime.combine(TODAY_DATE, datetime.time(8, 15, 46)))
    working_time = to_timestamp(datetime.datetime.combine(TODAY_DATE, wh_start))

    assert working_time == next_working_hour_timestamp(ts, working_days, wh_start, wh_end)


@pytest.mark.parametrize(
    ["working_days", "next_working_day"],
    [
        # today is 2 Jun 2020, Tuesday
        ({WEEKDAY_SUN}, datetime.date(2020, 6, 7)),
        ({WEEKDAY_MON}, datetime.date(2020, 6, 8)),
        ({WEEKDAY_MON, WEEKDAY_WED}, datetime.date(2020, 6, 3)),
    ],
)
@pytest.mark.parametrize(
    "today_time",
    [datetime.time(8, 0), datetime.time(9, 0), datetime.time(11, 0), datetime.time(19, 0), datetime.time(20, 0)],
)
def test_today_is_holiday(working_days, today_time, next_working_day):
    wh_start, wh_end = datetime.time(9, 0), datetime.time(19, 0)

    ts = to_timestamp(datetime.datetime.combine(TODAY_DATE, today_time))
    working_time = to_timestamp(datetime.datetime.combine(next_working_day, wh_start))

    assert working_time == next_working_hour_timestamp(ts, working_days, wh_start, wh_end)
