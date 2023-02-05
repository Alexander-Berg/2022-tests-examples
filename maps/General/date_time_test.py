import datetime
import pytz

from maps.poi.pylibs.util.date_time import (
    datetime_to_utctimestamp,
    moscow_datetime_by_str,
    utctimestamp_to_moscow_date,
)


def test_moscow_datetime_by_str():
    dt = moscow_datetime_by_str('2020-02-03')
    assert dt.year == 2020
    assert dt.month == 2
    assert dt.day == 3
    assert dt.tzinfo.zone == 'Europe/Moscow'


def test_datetime_to_utctimestamp():
    dt = pytz.timezone('Europe/Moscow').localize(datetime.datetime(1970, 1, 1, 3))
    assert datetime_to_utctimestamp(dt) == 0
    dt = pytz.timezone('Europe/Moscow').localize(datetime.datetime(1970, 1, 1, 3, 1))
    assert datetime_to_utctimestamp(dt) == 60
    dt = pytz.timezone('Asia/Yekaterinburg').localize(datetime.datetime(1970, 1, 1, 5))
    assert datetime_to_utctimestamp(dt) == 0
    dt = pytz.timezone('Asia/Yekaterinburg').localize(datetime.datetime(1970, 1, 1, 5, 1))
    assert datetime_to_utctimestamp(dt) == 60
    dt = datetime.datetime(1970, 1, 1, 0, 2)
    assert datetime_to_utctimestamp(dt) == 120


def test_utctimestamp_to_moscow_date():
    assert utctimestamp_to_moscow_date(0) == '1970-01-01'
    assert utctimestamp_to_moscow_date(21 * 60 * 60 - 1) == '1970-01-01'
    assert utctimestamp_to_moscow_date(21 * 60 * 60) == '1970-01-02'
