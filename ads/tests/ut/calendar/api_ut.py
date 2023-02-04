#! coding: utf-8

import json
from datetime import date

import requests_mock
import pytest
import yatest

import ads.watchman.timeline.api.lib.calendar.api as calendar_api
import ads.watchman.timeline.api.lib.calendar.schemas as calendar_schemas

from ads.watchman.timeline.api.tests.helpers.model_generators import TestModelGenerator


HOLIDAYS_RESPONSE_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/calendar/holidays.json')


def mock_responses():
    with open(HOLIDAYS_RESPONSE_FILE, 'rb') as f:
        return json.load(f)


@pytest.fixture
def net_mock():
    with requests_mock.mock() as m:
        yield m


def test_api_returns_holidays(net_mock):

    calendarApi = calendar_api.CalendarApi("http://test_host")
    geo_type = TestModelGenerator.create_geo_type(geo_id=1)
    d = date(2017, 11, 12)
    holiday_params = TestModelGenerator.create_holidays_params(start_day=d, end_day=d, geo_type=geo_type)
    net_mock.get(url="http://test_host/intapi/get-holidays", json=mock_responses())
    holidays = calendarApi.get_holidays(holiday_params)

    assert calendar_schemas.HolidaysSchema(strict=True).dump(holidays).data == mock_responses()
