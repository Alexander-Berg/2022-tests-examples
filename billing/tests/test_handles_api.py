from datetime import datetime, timedelta
from unittest import mock
import pytz
from bson.objectid import ObjectId

import pytest
import webtest
from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.handles_api import logic

from .utils import (
    to_midnight,
    mock_datetime,
)


@pytest.fixture()
def yesterday_delayed_hourly_stat(simple_key_delayed):
    yesterday_midnight = datetime(2019, 12, 31, 21, tzinfo=pytz.UTC)
    _, counter = simple_key_delayed.get_counters()
    assert mapper.Unit.getone(id=counter.unit_id).cc == 'hits_delayed'
    hs = mapper.HourlyStat(counter_id=counter.pk, dt=yesterday_midnight, value=10).save()
    hs_etag = mapper.DelayedCounterHoulyStatEtag(hourly_stat_id=hs.pk).save()
    yield hs, hs_etag
    hs.delete()
    hs_etag.delete()
    counter.delete()


@pytest.fixture()
def yesterday_online_hourly_stat(simple_key_delayed):
    msk_yesterday_midnight = to_midnight(
        datetime.now(pytz.timezone('Europe/Moscow')) - timedelta(days=1)
    )
    counter, _ = simple_key_delayed.get_counters()
    assert mapper.Unit.getone(id=counter.unit_id).cc == 'hits'
    hs = mapper.HourlyStat(counter_id=counter.pk, dt=msk_yesterday_midnight, value=10).save()
    yield hs
    hs.delete()


def test_project_service_link_export_incremental(mongomock):
    app = webtest.TestApp(logic)
    mapper.Service(id=1, cc='test', token='test_t', name='Test',
                   units=['hits'], unlock_reasons=[1], lock_reasons=[2]).save()
    list(map(
        lambda e: mapper.ProjectServiceLinkExport(**e).save(),
        [
            {'project_id': 'p1', 'service_id': 1, 'service': 'test', 'approved': True, 'uid': 101, 'keys': {}},
            {'project_id': 'p2', 'service_id': 1, 'service': 'test', 'approved': True, 'uid': 102},
            {'project_id': 'p3', 'service_id': 2, 'service': 'fake', 'approved': True, 'uid': 103},
        ]
    ))
    answer = app.get('/project_service_link_export', headers={'X-Service-Token': 'test_t'}).json
    update_dt = answer['max_update_dt']

    mapper.ProjectServiceLinkExport(
        **{'project_id': 'p4', 'service_id': 1, 'service': 'test', 'approved': True, 'uid': 104}).save()
    incremental_answer = app.get(
        '/project_service_link_export', params={'update_dt__gt': update_dt}, headers={'X-Service-Token': 'test_t'}).json

    assert answer['page']['items'] == 2
    assert len(answer['data']) == 2
    assert len(incremental_answer['data']) == 1


def test_get_link_info_by_key(mongomock, simple_service_delayed, simple_key_delayed):
    app = webtest.TestApp(logic)

    response = app.get(
        '/link_info_by_key/' + simple_key_delayed.key,
        {
            'include': 'tarifficator_state'
        },
        headers={'X-Service-Token': simple_service_delayed.token},
        status='3*'
    )
    assert response.status_int == 308
    assert 'include=tarifficator_state' in response.headers['Location']


def test_get_project_link_info(mongomock, simple_service_delayed, simple_key_delayed, simple_link_delayed):
    app = webtest.TestApp(logic)
    response = app.get(
        f'/service/{simple_service_delayed.id}/project/{simple_link_delayed.project_id}',
        headers={'X-Service-Token': simple_service_delayed.token},
        status='2*'
    )
    assert 'tarifficator_state' not in response.json['data']

    response = app.get(
        f'/service/{simple_service_delayed.id}/project/{simple_link_delayed.project_id}',
        {
            'include': 'tarifficator_state,__some_strage_field__'
        },
        headers={'X-Service-Token': simple_service_delayed.token},
        status='2*'
    )
    assert response.status_int == 200
    assert 'tarifficator_state' in response.json['data']


def test_get_project_link_info_forbidden(mongomock, simple_service_delayed, simple_key_delayed, simple_link_delayed,
                                         simple_service):
    app = webtest.TestApp(logic)
    response = app.get(
        f'/service/{simple_service.id}/project/{simple_link_delayed.project_id}',
        headers={'X-Service-Token': simple_service_delayed.token},
        status='4*'
    )
    assert response.status_int == 403


def test_get_project_link_statistic(mongomock, simple_service_delayed, simple_key_delayed, simple_link_delayed,
                                    simple_service):
    app = webtest.TestApp(logic)
    response = app.get(
        f'/service/{simple_service.id}/project/{simple_link_delayed.project_id}/statistic',
        headers={'X-Service-Token': simple_service_delayed.token},
        status='4*'
    )
    assert response.status_int == 403

    app = webtest.TestApp(logic)
    response = app.get(
        f'/service/{simple_service_delayed.id}/project/{simple_link_delayed.project_id}/statistic',
        {
            'date_from': (datetime.now(pytz.timezone('Europe/Moscow')) - timedelta(days=1)).isoformat(),
            'date_to': datetime.now(pytz.timezone('Europe/Moscow')).isoformat()
        },
        headers={'X-Service-Token': simple_service_delayed.token},
        status='2*'
    )
    assert response.status_int == 200
    assert isinstance(response.json['data'], list)
    assert 'statistic' in response.json['data'][0]


def test_read_existing_delayed_counter_correctly(mongomock, simple_service_delayed, simple_key_delayed, yesterday_delayed_hourly_stat):
    app = webtest.TestApp(logic)

    yesterday_midnight = datetime(2019, 12, 31, 21, 0, tzinfo=pytz.UTC)
    response = app.get(
        f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
        {
            'date': yesterday_midnight.isoformat(),
        },
        headers={'X-Service-Token': simple_service_delayed.token}
    )
    assert response.json['value'] == '10'
    assert response.headers['ETag'] != ''


def test_read_existing_online_counter_must_fail(mongomock, simple_service_delayed, simple_key_delayed, yesterday_online_hourly_stat):
    app = webtest.TestApp(logic)

    yesterday_midnight = datetime(2019, 12, 31, 21, 0, tzinfo=pytz.UTC)
    response = app.get(
        f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits/delayed',
        {
            'date': yesterday_midnight.isoformat(),
        },
        headers={'X-Service-Token': simple_service_delayed.token},
        status='4*'
    )
    assert response.status_int == 400


def test_specify_wrong_units_reading_delayed_counter(mongomock, simple_service_delayed, simple_key_delayed, yesterday_delayed_hourly_stat):
    app = webtest.TestApp(logic)
    yesterday_midnight = datetime(2019, 12, 31, 21, 0, tzinfo=pytz.UTC)
    response = app.get(
        f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/unit_not_exists/delayed',
        {
            'date': yesterday_midnight.isoformat(),
        },
        headers={'X-Service-Token': simple_service_delayed.token},
        status='4*')
    assert response.status_int == 404
    assert response.json['error'] == 'Object not found'


def test_not_specify_date_reading_delayed_counter(mongomock, simple_service_delayed, simple_key_delayed, yesterday_delayed_hourly_stat):
    app = webtest.TestApp(logic)
    response = app.get(
        f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
        headers={'X-Service-Token': simple_service_delayed.token},
        status='4*')
    assert response.status_int == 400
    assert response.json['error'] == 'Invalid params'


def test_create_delayed_counter_correctly(mongomock, simple_service_delayed, simple_key_delayed):
    app = webtest.TestApp(logic)

    yesterday_midnight = datetime(2019, 12, 31, 21, 0, tzinfo=pytz.UTC)

    with mock.patch('billing.apikeys.apikeys.handles_api.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 21, 30, tzinfo=pytz.UTC))):
        response = app.put(
            f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
            {
                'date': yesterday_midnight.isoformat(),
                'value': '10'
            },
            headers={'X-Service-Token': simple_service_delayed.token}
        )
    assert response.headers['ETag'] != ''
    hourly_stat_id = mapper.DelayedCounterHoulyStatEtag.getone(pk=ObjectId(response.headers['ETag'])).hourly_stat_id
    hs = mapper.HourlyStat.getone(pk=hourly_stat_id)
    assert hs.value == 10
    assert hs.dt == yesterday_midnight


def test_already_existing_delayed_counter_creation_must_fail(mongomock, simple_service_delayed, simple_key_delayed, yesterday_delayed_hourly_stat):
    app = webtest.TestApp(logic)

    yesterday_midnight = datetime(2019, 12, 31, 21, 0, tzinfo=pytz.UTC)
    _, prev_etag = yesterday_delayed_hourly_stat

    with mock.patch('billing.apikeys.apikeys.handles_api.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 21, 30, tzinfo=pytz.UTC))):
        response = app.put(
            f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
            {
                'date': yesterday_midnight.isoformat(),
                'value': '10'
            },
            headers={'X-Service-Token': simple_service_delayed.token},
            status='4*'
        )
    assert response.status_int == 409
    assert response.headers.get('ETag') == str(prev_etag.pk)


def test_delayed_counter_of_online_unit_creation_must_fail(mongomock, simple_service_delayed, simple_key_delayed):
    app = webtest.TestApp(logic)

    yesterday_midnight = to_midnight(datetime.now(pytz.utc) - timedelta(days=1))

    response = app.put(
        f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits/delayed',
        {
            'date': yesterday_midnight.isoformat(),
            'value': '10'
        },
        headers={'X-Service-Token': simple_service_delayed.token},
        status='4*'
    )
    assert response.status_int == 400


def test_future_or_very_late_existing_delayed_counter_creation_must_fail(mongomock, simple_service_delayed, simple_key_delayed):
    app = webtest.TestApp(logic)

    tomorrow = datetime.now(pytz.utc) + timedelta(days=1)
    response = app.put(
        f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
        {
            'date': tomorrow.isoformat(),
            'value': '10'
        },
        headers={'X-Service-Token': simple_service_delayed.token},
        status='4*'
    )
    assert response.status_int == 400
    assert response.json['error'] == 'Bucket is not writable'  # 'Dates from future prohibited'

    midnight_two_days_before = to_midnight(
        datetime.now(pytz.utc) - timedelta(days=2)
    )
    response = app.put(
        f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
        {
            'date': midnight_two_days_before.isoformat(),
            'value': '10'
        },
        headers={'X-Service-Token': simple_service_delayed.token},
        status='4*'
    )
    assert response.status_int == 400
    assert response.json['error'] == 'Bucket is not writable'  # 'Too late'


def test_update_delayed_counter_correctly(mongomock, simple_service_delayed, simple_key_delayed, yesterday_delayed_hourly_stat):
    app = webtest.TestApp(logic)

    prev_hs, prev_etag = yesterday_delayed_hourly_stat
    yesterday_midnight = datetime(2019, 12, 31, 21, 0, tzinfo=pytz.UTC)

    with mock.patch('billing.apikeys.apikeys.handles_api.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 21, 30, tzinfo=pytz.UTC))):
        response = app.post(
            f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
            {
                'date': yesterday_midnight.isoformat(),
                'value': '30'
            },
            headers={'X-Service-Token': simple_service_delayed.token, 'ETag': str(prev_etag.pk)}
        )
    assert response.headers['ETag'] != prev_etag.pk
    hs = mapper.HourlyStat.getone(pk=prev_hs.pk)
    assert hs.value == 30
    assert hs.dt == prev_hs.dt


def test_update_delayed_counter_with_conflict(mongomock, simple_service_delayed, simple_key_delayed, yesterday_delayed_hourly_stat):
    app = webtest.TestApp(logic)

    prev_hs, prev_etag = yesterday_delayed_hourly_stat
    wrong_etag = prev_etag.pk
    prev_etag.delete()
    etag = mapper.DelayedCounterHoulyStatEtag(hourly_stat_id=prev_hs.pk).save()
    yesterday_midnight = datetime(2019, 12, 31, 21, 0, tzinfo=pytz.UTC)

    with mock.patch('billing.apikeys.apikeys.handles_api.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 21, 30, tzinfo=pytz.UTC))):
        response = app.post(
            f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
            {
                'date': yesterday_midnight.isoformat(),
                'value': '30'
            },
            headers={'X-Service-Token': simple_service_delayed.token, 'ETag': str(wrong_etag)},
            status='4*'
        )
    assert response.status_int == 409
    assert response.headers.get('ETag') == str(etag.pk)


def test_update_delayed_counter_too_late(mongomock, simple_service_delayed, simple_key_delayed, yesterday_delayed_hourly_stat):
    app = webtest.TestApp(logic)

    midnight_two_days_before = to_midnight(datetime.now(pytz.utc) - timedelta(days=2))
    prev_hs, prev_etag = yesterday_delayed_hourly_stat
    prev_hs.dt -= timedelta(days=1)
    response = app.post(
        f'/service/{simple_service_delayed.id}/key/{simple_key_delayed.key}/unit/hits_delayed/delayed',
        {
            'date': midnight_two_days_before.isoformat(),
            'value': '30'
        },
        headers={'X-Service-Token': simple_service_delayed.token, 'ETag': str(prev_etag.pk)},
        status='4*'
    )
    assert response.status_int == 400
    assert response.json['error'] == 'Bucket is not writable'  # 'Too late'
