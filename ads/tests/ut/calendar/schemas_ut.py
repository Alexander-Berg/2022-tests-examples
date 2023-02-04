#! coding: utf-8

import collections
import json

import pytest
import yatest

import ads.watchman.timeline.api.lib.calendar.schemas as calendar_schemas
from ads.watchman.timeline.api.tests.helpers.model_generators import TestModelGenerator


HOLIDAYS_RESPONSE_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/calendar/holidays.json')


def mock_responses():
    with open(HOLIDAYS_RESPONSE_FILE, 'rb') as f:
        return json.load(f)


SCHEMAS = collections.OrderedDict([
    (u"HolidaySchema", (mock_responses()["holidays"][0], calendar_schemas.HolidaySchema(strict=True))),
    (u"HolidaysSchema", (mock_responses(), calendar_schemas.HolidaysSchema(strict=True)))
])


@pytest.mark.parametrize("json_obj, schema", SCHEMAS.values(), ids=SCHEMAS.keys())
def test_schema_serializing_deserializing(json_obj, schema):
    obj = schema.load(json_obj).data
    reverted_json_obj = schema.dump(obj).data
    assert json_obj == reverted_json_obj


def test_serializing_holiday_params():

    geo_type = TestModelGenerator.create_geo_type(geo_id=1)
    holiday_params = TestModelGenerator.create_holidays_params(geo_type=geo_type)

    json_obj = calendar_schemas.HolidayParamsSchema(strict=True).dump(holiday_params).data

    assert json_obj == {
        "from": holiday_params.start_day.isoformat(),
        "to": holiday_params.end_day.isoformat(),
        "for": holiday_params.geo_type.geo_id,
        "outMode": holiday_params.out_mode.name
    }
