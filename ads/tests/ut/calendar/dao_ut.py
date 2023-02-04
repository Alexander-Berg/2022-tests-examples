# coding: utf-8

import json
import collections

from datetime import date

import requests_mock
import pytest
import yatest

import ads.watchman.timeline.api.lib.calendar.api as calendar_api
import ads.watchman.timeline.api.lib.calendar.dao as calendar_dao
import ads.watchman.timeline.api.lib.modules.events.schemas as events_schemas

from ads.watchman.timeline.api.tests.helpers.model_generators import TestModelGenerator


HOLIDAYS_RESPONSE_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/calendar/holidays.json')


def mock_responses():
    with open(HOLIDAYS_RESPONSE_FILE, 'rb') as f:
        return json.load(f)


HOLIDAYS = collections.OrderedDict([
    (u"holiday", {"holidays": mock_responses()["holidays"][0:1]}),
    (u"weekend", {"holidays": mock_responses()["holidays"][1:2]})
])


@pytest.fixture
def net_mock():
    with requests_mock.mock() as m:
        yield m


@pytest.mark.parametrize(u"holiday", HOLIDAYS.values(), ids=HOLIDAYS.keys())
def test_dao_returns_holiday_event(holiday, net_mock):
    net_mock.get("http://test_host/intapi/get-holidays", json=holiday)

    calendarApi = calendar_api.CalendarApi("http://test_host")
    calendarDao = calendar_dao.CalendarDao(calendarApi)

    d = date(2017, 11, 12)
    geo_type = TestModelGenerator.create_geo_type(geo_id=1)
    event = calendarDao.get_holiday_event(d, ['test_owner'], geo_type)

    assert events_schemas.HolidayEventSchema().dump(event).errors == {}


def test_dao_returns_none_if_today_is_not_holiday(net_mock):
    net_mock.get("http://test_host/intapi/get-holidays", json={"holidays": []})

    calendarApi = calendar_api.CalendarApi("http://test_host")
    calendarDao = calendar_dao.CalendarDao(calendarApi)

    d = date(2017, 11, 12)
    geo_type = TestModelGenerator.create_geo_type(geo_id=1)

    assert calendarDao.get_holiday_event(d, ['test_owner'], geo_type) is None


def test_dao_returns_none_if_not_geo_id_presented(net_mock):
    net_mock.get("http://test_host/intapi/get-holidays", json=HOLIDAYS['holiday'])

    calendarApi = calendar_api.CalendarApi("http://test_host")
    calendarDao = calendar_dao.CalendarDao(calendarApi)

    d = date(2017, 11, 12)
    geo_type = TestModelGenerator.create_geo_type(geo_id=None)

    assert calendarDao.get_holiday_event(d, ['test_owner'], geo_type) is None
