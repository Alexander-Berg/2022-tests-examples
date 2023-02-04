import datetime as dt
import pytz
import pytest

from maps.garden.sdk.module_autostart import time_utils

MOSCOW_TZ = pytz.timezone("Europe/Moscow")


def test_work_hours():
    work_hours = time_utils.WorkHours(8, 17, tzinfo=MOSCOW_TZ)

    assert dt.datetime(2020, 11, 29, 10, 0, 0, tzinfo=pytz.utc) in work_hours
    assert dt.datetime(2020, 11, 29, 4, 0, 0, tzinfo=pytz.utc) not in work_hours
    assert dt.datetime(2020, 11, 29, 16, 0, 0, tzinfo=pytz.utc) not in work_hours

    assert MOSCOW_TZ.localize(dt.datetime(2020, 11, 29, 13, 0, 0)) in work_hours
    assert MOSCOW_TZ.localize(dt.datetime(2020, 11, 29, 4, 0, 0)) not in work_hours
    assert MOSCOW_TZ.localize(dt.datetime(2020, 11, 29, 17, 0, 0)) not in work_hours


@pytest.mark.parametrize(
    ("current_time", "expected_time"),
    [
        (dt.datetime(2020, 11, 29, 4, 0, 0), dt.datetime(2020, 11, 29, 8, 0, 0)),
        (dt.datetime(2020, 11, 29, 13, 0, 0), None),
        (dt.datetime(2020, 11, 29, 18, 0, 0), dt.datetime(2020, 11, 30, 8, 0, 0)),
    ],
)
def test_find_closest_work_time(freezer, current_time, expected_time):
    freezer.move_to(MOSCOW_TZ.localize(current_time))

    expected_time = MOSCOW_TZ.localize(expected_time) if expected_time else None

    work_hours = time_utils.WorkHours(8, 17, tzinfo=MOSCOW_TZ)

    assert work_hours.find_closest_work_time() == expected_time
