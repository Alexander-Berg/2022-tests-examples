from datetime import timedelta, datetime, date

import pytest
from flask import Flask

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.config.common import KNOWN_LOCALES, IGNORED_LOCALES

try:
    from maps.b2bgeo.libs.time.py.time_utils import (
        format_str_time_relative as format_str_time)
except ImportError:
    from time_utils import (
        format_str_time_relative as format_str_time)

from maps.b2bgeo.libs.py_flask_utils.format_util import (
    parse_time, parse_interval_sec,
    TimeIntervalWrongFormat, parse_intervals_sec,
    get_localized_eta_str,
    check_html_color, check_url)
from maps.b2bgeo.libs.py_flask_utils.i18n import Keysets


ROUTE_DATE = datetime.now()
MSK_TIME_ZONE = 'Europe/Moscow'  # +0300
CHILE_TIME_ZONE = 'Chile/Continental'  # -0300 summer / -0400 winter


@pytest.fixture
def app():
    app = Flask(__name__)
    app.config.from_object('ya_courier_backend.config.unit_test')
    return app


@skip_if_remote
def test_format_str_time():
    assert format_str_time(81000) == '22:30:00'
    assert format_str_time(126000) == '1.11:00:00'
    assert format_str_time(24 * 60 * 60) == '1.00:00:00'
    assert format_str_time(24 * 60 * 60 - 1) == '23:59:59'
    assert format_str_time(24 * 60 * 60 * 2 - 1) == '1.23:59:59'


@skip_if_remote
def test_parse_time():
    assert parse_time("15") == timedelta(0, 15*60*60)
    assert parse_time("15:40") == timedelta(0, 15*60*60 + 40*60)
    assert parse_time("15:40:01") == timedelta(0, 15*60*60 + 40*60 + 1)
    assert parse_time("5.15") == timedelta(5, 15*60*60)
    assert parse_time("5.15:40") == timedelta(5, 15*60*60 + 40*60)
    assert parse_time("5.15:40:01") == timedelta(5, 15*60*60 + 40*60 + 1)


@skip_if_remote
def test_parse_interval_sec():
    assert parse_interval_sec(
        "15-15:40", ROUTE_DATE, MSK_TIME_ZONE) == (
            timedelta(0, 15*60*60).total_seconds(),
            timedelta(0, 15*60*60 + 40*60).total_seconds())


@skip_if_remote
def test_parse_intervals_sec():
    assert parse_intervals_sec('11 - 12', ROUTE_DATE, MSK_TIME_ZONE) == [(39600, 43200)]
    assert parse_intervals_sec('11 - 11', ROUTE_DATE, MSK_TIME_ZONE) == [(39600, 39600)]
    assert parse_intervals_sec('11-12:01', ROUTE_DATE, MSK_TIME_ZONE) == [(39600, 43260)]
    assert parse_intervals_sec('11 - 12:01:01', ROUTE_DATE, MSK_TIME_ZONE) == [(39600, 43261)]
    assert parse_intervals_sec(
        '11-12:01, 13-14', ROUTE_DATE, MSK_TIME_ZONE) == [(39600, 43260), (46800, 50400)]

    assert parse_intervals_sec('1.11-1.12', ROUTE_DATE, MSK_TIME_ZONE) == [(126000, 129600)]


@skip_if_remote
@pytest.mark.parametrize(
    "interval,message",
    [
        ('11-12:01, 13-24', None),
        ('11-', None),
        ('11', None),
        ('11 - 12:01:01:01', None),
        ('11 - 12 - 13', None),
        ('13 - 12', 'Beginning of time window 13 - 12 can\'t be later than its end'),
        ('-1.22:50 - 10:13', None),
        ('2018-10-30T10:00:00+00:00 - 2018-10-30T12:00:00+00:00', None),
        (
            '2021-11-17T11:00:00+03:00/2021-11-17T12:00:00+03:00',
            'end of time window .* can\'t be earlier than start time of the route',
        ),
    ],
)
def test_parse_intervals_sec_fail(interval, message):
    if not message:
        message = "Invalid time window format"
    with pytest.raises(TimeIntervalWrongFormat, match=message):
        parse_intervals_sec(interval, ROUTE_DATE, MSK_TIME_ZONE)


@skip_if_remote
def test_parse_intervals_sec_negative_timezone_chile_summer():
    assert parse_intervals_sec('2021-11-17T11:00:00-03:00/2021-11-17T12:00:00-03:00', date(2021, 11, 17),
                               CHILE_TIME_ZONE) == [(39600, 43200)]


@skip_if_remote
def test_parse_intervals_sec_negative_timezone_chile_winter():
    assert parse_intervals_sec('2021-07-17T11:00:00-04:00/2021-07-17T12:00:00-04:00',
                               date(2021, 7, 17), CHILE_TIME_ZONE) == [(39600, 43200)]


@skip_if_remote
@pytest.mark.parametrize(('locale', 'd', 'h', 'm'), (
    ('ru_RU', 'д', 'ч', 'м'),
    ('en_US', 'd', 'h', 'm'),
    ('es_CL', 'd', 'h', 'm'),
    ('zz_ZZ', 'd', 'h', 'm'),
))
def test_get_localized_eta_str(app, locale, d, h, m):
    Keysets.init(KNOWN_LOCALES, IGNORED_LOCALES)
    with app.app_context():
        assert get_localized_eta_str(-1, locale) == f'-1{m}'
        assert get_localized_eta_str(0, locale) == f'0{m}'
        assert get_localized_eta_str(1.1, locale) == f'1{m}'
        assert get_localized_eta_str(10, locale) == f'10{m}'
        assert get_localized_eta_str(100, locale) == f'1{h} 40{m}'
        assert get_localized_eta_str(1000, locale) == f'16{h} 40{m}'
        assert get_localized_eta_str(10000, locale) == f'6{d} 22{h} 40{m}'
        assert get_localized_eta_str(100000, locale) == f'69{d} 10{h} 40{m}'
        assert get_localized_eta_str(1000000, locale) == f'694{d} 10{h} 40{m}'
        assert get_localized_eta_str(24 * 60 + 25, locale) == f'1{d} 0{h} 25{m}'


@skip_if_remote
def test_check_html_color():
    assert check_html_color('#1111aa') is True
    assert check_html_color('#1111AA') is True
    assert check_html_color('#1111AX') is False
    assert check_html_color('#11') is False
    assert check_html_color('#111') is True
    assert check_html_color('#111111') is True
    assert check_html_color('#111111 ') is False
    assert check_html_color(' #111111') is False
    assert check_html_color(' 111111') is False
    assert check_html_color('rgb(0)') is False
    assert check_html_color('rgb(0,)') is False
    assert check_html_color('rgb(0,0)') is False
    assert check_html_color('rgb(0,0,0)') is True
    assert check_html_color('rgb(a,0,0)') is False
    assert check_html_color('RGB(0,0,0)') is True
    assert check_html_color('RGB(100,0,0)') is True
    assert check_html_color('RGB(200,0,0)') is True
    assert check_html_color('RGB(300,0,0)') is False
    assert check_html_color(' RGB(200,0,0)') is False
    assert check_html_color('RGB(200,0,0) ') is False
    assert check_html_color('RGB(200, 0,0)') is True
    assert check_html_color('RGB(200, 0,    0)') is True
    assert check_html_color('RGB(200, 0,    00)') is True
    assert check_html_color('black') is True
    assert check_html_color('bllack') is False
    assert check_html_color(' black') is False
    assert check_html_color('black ') is False
    assert check_html_color('') is False
    assert check_html_color(None) is True


@skip_if_remote
def test_check_url():
    assert check_url('http://yandex.ru') is True
    assert check_url('https://yandex.ru') is True
    assert check_url('htt://yandex.ru') is False
    assert check_url('http:/yandex.ru') is False
    assert check_url(r'http:/\yandex.ru') is False
    assert check_url('http://yandex.ru/avatar.') is True
    assert check_url('http://yandex.ru/avatar.svg') is True
    assert check_url(' http://yandex.ru/avatar.svg') is False
    assert check_url('http://yandex.ru/avatar.svg ') is True
    assert check_url('http://yandex.ru/avatar.svg?') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big&') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big& >') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&">') is False
    assert check_url('http://yandex.ru/avatar.svg?mo') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big&') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big& ') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&=') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big&;') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&:') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&>') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&"') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&?') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&_') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big&.') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big&. ') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&.+') is True
    assert check_url('http://yandex.ru/avatar.svg?size=big&.<') is False
    assert check_url('http://yandex.ru/avatar.svg?size=big&./') is False
    assert check_url('://yandex.ru/avatar.svg?mode=big&./') is False
    assert check_url('yandex.ru/avatar.svg?mode=big&./') is False
    assert check_url('http://yandex.ru:80/avatar.svg?size=big') is True
    assert check_url(
        'http://username:password@yandex.ru:80/avatar.svg?size=big') is True
    assert check_url(
        'http://username:"@yandex.ru:80/avatar.svg;params?size=big') is False
    assert check_url(
        'http://username:password@yandex.ru:80/avatar.svg;params?size=big') is True
    assert check_url(
        'http://username:password@yandex.ru:80/avatar.svg;params">?size=big') is False
    assert check_url(
        'http://username:password@yandex.ru:80/avatar.svg;params?size=big#anchor') is True
    assert check_url(
        'http://username:password@yandex.ru:80/avatar.svg;params?size=big#anchor"/>') is False
    assert check_url('') is False
    assert check_url(None) is True
