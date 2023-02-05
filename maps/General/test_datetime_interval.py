from datetime import datetime
from datetime import date
from maps.qa.common.lib.datetime_interval import (
    DatetimeInterval,
    datetime_intervals_overlap
)
import pytest
from freezegun import freeze_time


def test_dt_interval_constructor_success():
    dt_begin = datetime(2019, 07, 25, 0, 0, 0)
    dt_end = datetime(2019, 07, 26, 0, 0, 0)
    DatetimeInterval(dt_begin, dt_end)


def test_dt_interval_constructor_raise():
    dt_begin = datetime(2019, 07, 26, 0, 0, 0)
    dt_end = datetime(2019, 07, 25, 0, 0, 0)

    with pytest.raises(AssertionError):
        DatetimeInterval(dt_begin, dt_end)


def test_dt_interval_from_date_and_window():
    one_day_interval = DatetimeInterval.from_date_and_window(
        date(2019, 07, 26),
        1
    )

    assert one_day_interval.begin == date(2019, 07, 26)
    assert one_day_interval.end == date(2019, 07, 27)


@freeze_time("2020-02-02 00:00:00", tz_offset=0)
def test_dt_interval_from_timestamps_ms():
    dt = DatetimeInterval.from_timestamps_ms(1564099201000, 1564099202000)
    assert dt.begin == datetime(2019, 07, 26, 0, 00, 01)
    assert dt.end == datetime(2019, 07, 26, 0, 00, 02)


def test_datetime_intervals_overlap():
    def overlap_right():
        lhs = DatetimeInterval(date(2019, 01, 10), date(2019, 01, 12))
        rhs = DatetimeInterval(date(2019, 01, 11), date(2019, 01, 13))
        overlap = datetime_intervals_overlap(lhs, rhs)
        assert overlap is not None
        assert overlap.begin == date(2019, 01, 11)
        assert overlap.end == date(2019, 01, 12)

    def overlap_left():
        lhs = DatetimeInterval(date(2019, 01, 11), date(2019, 01, 13))
        rhs = DatetimeInterval(date(2019, 01, 10), date(2019, 01, 12))
        overlap = datetime_intervals_overlap(lhs, rhs)
        assert overlap is not None
        assert overlap.begin == date(2019, 01, 11)
        assert overlap.end == date(2019, 01, 12)

    def overlap_inside():
        lhs = DatetimeInterval(date(2019, 01, 10), date(2019, 01, 13))
        rhs = DatetimeInterval(date(2019, 01, 11), date(2019, 01, 12))
        overlap = datetime_intervals_overlap(lhs, rhs)
        assert overlap is not None
        assert overlap.begin == date(2019, 01, 11)
        assert overlap.end == date(2019, 01, 12)

    def donot_overlap():
        lhs = DatetimeInterval(date(2019, 01, 10), date(2019, 01, 11))
        rhs = DatetimeInterval(date(2019, 01, 12), date(2019, 01, 13))
        assert datetime_intervals_overlap(lhs, rhs) is None

    def single_point_touch():
        lhs = DatetimeInterval(date(2019, 01, 10), date(2019, 01, 11))
        rhs = DatetimeInterval(date(2019, 01, 11), date(2019, 01, 13))
        overlap = datetime_intervals_overlap(lhs, rhs)
        assert overlap is not None
        assert overlap.begin == date(2019, 01, 11)
        assert overlap.end == date(2019, 01, 11)

    overlap_right()
    overlap_left()
    overlap_inside()
    donot_overlap()
    single_point_touch()
