import datetime
import dateutil
import dateutil.tz
import dateutil.parser

from pytest import approx

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.util.time_zone import time_zone_offset_hours
from ya_courier_backend.util.order_time_windows import (
    seconds_from_midnight_to_utc,
    convert_time_window, convert_time_windows,
    convert_time_windows_to_time_window_iso,
    convert_time_window_timestamp, get_time_interval_seconds)


@skip_if_remote
def test_convert_time_zone():
    moscow_time = datetime.datetime(2018, 5, 26, 10, 0, 0, 0,
                                    dateutil.tz.gettz('Europe/Moscow'))
    assert str(moscow_time) == "2018-05-26 10:00:00+03:00"
    assert dateutil.parser.parse("2018-05-26 10:00:00+03:00") == moscow_time

    utc_time = moscow_time.astimezone(dateutil.tz.tzutc())
    assert str(utc_time) == "2018-05-26 07:00:00+00:00"
    assert dateutil.parser.parse("2018-05-26 07:00:00+00:00") == utc_time


@skip_if_remote
def test_seconds_from_midnight_to_utc():
    day = datetime.date(2018, 5, 26)
    time_zone = 'Europe/Moscow'
    seconds_from_midnight = datetime.timedelta(hours=10).total_seconds()

    utc = seconds_from_midnight_to_utc(seconds_from_midnight, day, time_zone)
    assert utc == datetime.datetime(
        2018, 5, 26, 7, 0, 0, 0, dateutil.tz.tzutc())


@skip_if_remote
def test_convert_time_window():
    time_window = {
        'start': datetime.datetime(2018, 5, 26, 7, 0, tzinfo=dateutil.tz.tzutc()),
        'end': datetime.datetime(2018, 5, 26, 8, 0, tzinfo=dateutil.tz.tzutc())
    }
    day = datetime.datetime(2018, 5, 26)
    time_zone = 'Europe/Moscow'

    time_interval = convert_time_window(time_window, day, time_zone)
    assert time_interval == "10:00 - 11:00"


@skip_if_remote
def test_convert_time_windows_to_time_window_iso():
    time_window = {
        'start': datetime.datetime(2019, 5, 27, 7, 0, tzinfo=dateutil.tz.tzutc()),
        'end': datetime.datetime(2019, 5, 27, 8, 0, tzinfo=dateutil.tz.tzutc())
    }

    time_zone = dateutil.tz.gettz('Europe/Moscow')

    expected_time_window_iso = {
        'start': '2019-05-27T10:00:00+03:00',
        'end': '2019-05-27T11:00:00+03:00',
    }

    assert convert_time_windows_to_time_window_iso([time_window], time_zone) == expected_time_window_iso
    assert convert_time_windows_to_time_window_iso([time_window] * 2, time_zone) == expected_time_window_iso


@skip_if_remote
def test_convert_time_windows_no_route():
    time_windows = [{
        'start': datetime.datetime(2018, 5, 26, 7, 0, tzinfo=dateutil.tz.tzutc()),
        'end': datetime.datetime(2018, 5, 26, 8, 0, tzinfo=dateutil.tz.tzutc())
    }]
    day = None
    time_zone = None

    time_interval = convert_time_windows(time_windows, day, time_zone)
    assert time_interval is None


@skip_if_remote
def test_convert_time_window_timestamp():
    day = datetime.datetime(2018, 5, 26)

    utc_timestamp = datetime.datetime(
        2018, 5, 26, 7, 0, tzinfo=dateutil.tz.tzutc())
    assert convert_time_window_timestamp(utc_timestamp, day,
                                         'Europe/Moscow') == "10:00"
    assert convert_time_window_timestamp(utc_timestamp, day,
                                         'Europe/Berlin') == "09:00"


@skip_if_remote
def test_convert_time_delivery_next_day():
    day = datetime.datetime(2018, 5, 25)

    utc_timestamp = datetime.datetime(
        2018, 5, 26, 7, 0, tzinfo=dateutil.tz.tzutc())
    assert convert_time_window_timestamp(utc_timestamp, day,
                                         'Europe/Moscow') == "1.10:00"
    assert convert_time_window_timestamp(utc_timestamp, day,
                                         'Europe/Berlin') == "1.09:00"


@skip_if_remote
def test_convert_time_delivery_day_before():
    day = datetime.datetime(2018, 5, 27)

    utc_timestamp = datetime.datetime(
        2018, 5, 26, 7, 0, tzinfo=dateutil.tz.tzutc())
    assert convert_time_window_timestamp(utc_timestamp, day,
                                         'Europe/Moscow') == "-1.10:00"
    assert convert_time_window_timestamp(utc_timestamp, day,
                                         'Europe/Berlin') == "-1.09:00"


@skip_if_remote
def test_get_time_interval_seconds():
    time_window = {
        'start': datetime.datetime(2018, 5, 26, 15, 0, tzinfo=dateutil.tz.tzutc()),
        'end': datetime.datetime(2018, 5, 26, 16, 0, tzinfo=dateutil.tz.tzutc())
    }
    day = datetime.datetime(2018, 5, 26)
    result = get_time_interval_seconds(time_window, day, 'UTC')
    assert result == (54000.0, 57600.0), time_window

    result = get_time_interval_seconds(time_window, day, 'Europe/Moscow')
    assert result == (64800.0, 68400.0), time_window


@skip_if_remote
def test_none_time_windows():
    time_windows = None
    day = datetime.datetime(2018, 5, 26)
    time_zone = 'Europe/Moscow'

    time_interval = convert_time_windows(time_windows, day, time_zone)
    assert time_interval is None


@skip_if_remote
def test_empty_time_windows():
    time_windows = []
    day = datetime.datetime(2018, 5, 26)
    time_zone = 'Europe/Moscow'

    time_interval = convert_time_windows(time_windows, day, time_zone)
    assert time_interval == ''


@skip_if_remote
def test_time_zone_offset():
    assert time_zone_offset_hours('Europe/Moscow') == approx(3.0)
    assert time_zone_offset_hours('Europe/Berlin') == approx(2.0) or time_zone_offset_hours('Europe/Berlin') == approx(1.0)
    assert time_zone_offset_hours('America/New_York') == approx(-4.0) or time_zone_offset_hours('America/New_York') == approx(-5.0)
    assert time_zone_offset_hours('Asia/Tehran') == approx(3.5) or time_zone_offset_hours('Asia/Tehran') == approx(4.5)
