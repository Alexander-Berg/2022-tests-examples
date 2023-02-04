from maps.b2bgeo.libs.time.py.time_utils import (
    parse_time_interval, format_str_time_relative, parse_time,
)
import time
import datetime


def test_parse_timeinterval():
    route_date_s = time.mktime(datetime.datetime.now().timetuple())
    try:
        # Here used invalid character '–' (long dash U+2013) which cannot be parsed
        parse_time_interval(u"10:00:00 \u2013 11:00:00", route_date_s, 0)
    except Exception as ex:
        assert "Invalid time window format (time_window parameter) '10:00:00 \\xE2\\x80\\x93 11:00:00'" in str(ex)


def test_format_str_time_relative():
    assert format_str_time_relative(10) == "00:00:10"


def test_format_str_time_relative2():
    assert format_str_time_relative(87000) == "1.00:10:00"


def test_parse_time():
    assert parse_time("10:00") == 36000


def test_parse_time2():
    assert parse_time("1.00:10:00") == 87000
