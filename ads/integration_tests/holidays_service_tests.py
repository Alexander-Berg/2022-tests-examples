#! coding: utf-8

import json
import collections

from datetime import datetime, timedelta

import requests_mock
import pytest
import yatest
import schedule

import ads.watchman.timeline.api.lib.calendar.api as calendar_api
import ads.watchman.timeline.api.lib.calendar.dao as calendar_dao
import ads.watchman.timeline.api.lib.calendar.service as calendar_service

import ads.watchman.timeline.api.lib.modules.events.dao as events_dao
import ads.watchman.timeline.api.lib.modules.events.db as events_db

from ads.watchman.timeline.api.lib.modules.events import db_manager, resource_manager

import ads.watchman.timeline.api.tests.helpers as test_helpers
import ads.watchman.timeline.api.tests.helpers.model_generators as model_generators

HOLIDAYS_RESPONSE_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/calendar/holidays.json')


def mock_responses():
    with open(HOLIDAYS_RESPONSE_FILE, 'rb') as f:
        return json.load(f)


HOLIDAYS = collections.OrderedDict([
    (u"holiday", {"holidays": mock_responses()["holidays"][0:1]}),
    (u"weekend", {"holidays": mock_responses()["holidays"][1:2]})
])


@pytest.fixture()
def net_mock():
    with requests_mock.mock() as m:
        yield m


@pytest.fixture()
def calendarDao():
    calendarApi = calendar_api.CalendarApi("http://test_host")
    calendarDao = calendar_dao.CalendarDao(calendarApi)
    yield calendarDao


@pytest.fixture()
def timelineDao(db_session):
    yield events_dao.SqlDao(db_session)


@pytest.fixture()
def restored_geo_type_dbo(db_session):
    db_manager.TimelineDBManager(db_session).sync_enums(resource_manager.ResourceManager())
    restored_geo_type_dbo = list(db_session.query(events_db.GeoTypeDBO).filter(events_db.GeoTypeDBO.geo_id.isnot(None)))
    yield restored_geo_type_dbo


@pytest.mark.parametrize(u"holiday", HOLIDAYS.values(), ids=HOLIDAYS.keys())
def test_insert_event_if_holiday_exists_in_calendar(db_session, net_mock, holiday, calendarDao, timelineDao, restored_geo_type_dbo):
    net_mock.get("http://test_host/intapi/get-holidays?", json=holiday)
    date = datetime(2017, 11, 12)

    restored_event_dbo = list(db_session.query(events_db.EventDBO).all())
    assert len(restored_event_dbo) == 0, "sanity check failed"

    calendar_service.push_holidays_from_calendar_to_timeline(timelineDao, calendarDao, date, ["test_owner"])

    restored_event_dbo = list(db_session.query(events_db.EventDBO).all())
    assert len(restored_event_dbo) == len(restored_geo_type_dbo), "should be insert for all geo types except root geo type"

    calendar_service.push_holidays_from_calendar_to_timeline(timelineDao, calendarDao, date, ["test_owner"])
    restored_event_dbo = list(db_session.query(events_db.EventDBO).all())
    assert len(restored_event_dbo) == len(restored_geo_type_dbo), "should be upsert for all inserted geo types"


def test_does_not_insert_event_if_holiday_not_exists_in_calendar(db_session, net_mock, calendarDao, timelineDao):
    net_mock.get("http://test_host/intapi/get-holidays", json={"holidays": []})
    date = datetime(2017, 11, 12)

    restored_event_dbo = list(db_session.query(events_db.EventDBO).all())
    assert len(restored_event_dbo) == 0, "sanity check failed"

    calendar_service.push_holidays_from_calendar_to_timeline(timelineDao, calendarDao, date, ["test_owner"])
    assert len(restored_event_dbo) == 0, "should nothing insert"


BASE_DATETIME = datetime(2017, 6, 1, 0, 1)


@pytest.fixture()
def schedule_conf():
    with test_helpers.MockDatetime.from_datetime(BASE_DATETIME):
        conf = model_generators.TestingConfig()
        conf.CALENDAR_URL = "http://test_host"
        session = "test_session"
        calendar_service.set_schedule(conf, session)
        yield conf
        schedule.clear()


def test_scheduler_service_does_not_update_holidays_if_not_update_period_passed(net_mock, schedule_conf):
    net_mock.get("http://test_host/intapi/get-holidays", json=HOLIDAYS["holiday"])
    new_datetime = BASE_DATETIME + timedelta(minutes=schedule_conf.CALENDAR_SERVICE_UPDATE_PERIOD_MINUTES - 1)
    with test_helpers.MockDatetime.from_datetime(new_datetime):
        schedule.run_pending()
        assert schedule_conf.DAO_CLASS.COUNTERS["put_event"] == 0


def test_scheduler_service_update_holidays_if_update_period_passed(net_mock, schedule_conf):
    net_mock.get("http://test_host/intapi/get-holidays", json=HOLIDAYS["holiday"])
    new_datetime = BASE_DATETIME + timedelta(minutes=schedule_conf.CALENDAR_SERVICE_UPDATE_PERIOD_MINUTES)
    with test_helpers.MockDatetime.from_datetime(new_datetime):
        schedule.run_pending()
        assert schedule_conf.DAO_CLASS.COUNTERS["put_event"] == len(schedule_conf.DAO_CLASS.get_geo_types())
