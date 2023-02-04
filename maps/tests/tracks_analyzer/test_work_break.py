from datetime import datetime, timedelta
from http import HTTPStatus

import dateutil.tz
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_delete, local_get, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
from ya_courier_backend.models import WorkBreak, WorkBreakStatus

WORK_BREAKS_DATA = [
    {
        "work_time_range_till_rest": "00:20:00-00:30:00",
        "rest_duration_s": 100
    },
    {
        "work_time_range_till_rest": "00:10:00-00:20:00",
        "rest_duration_s": 200
    }
]
NEW_WORK_BREAKS_DATA = [
    {
        "work_time_range_till_rest": "02:50:00-03:00:00",
        "rest_duration_s": 300
    },
    {
        "work_time_range_till_rest": "23:00:00-1.00:00:00",
        "rest_duration_s": 400
    }
]
INVALID_WORK_BREAKS_DATA = [
    {
        "work_time_range_till_rest": "invalid window",
        "rest_duration_s": 300
    }
]
INVALID_WORK_BREAKS_EXTRA_PROPERTIES = [
    {
        "work_time_range_till_rest": "23:00:00-1.00:00:00",
        "rest_duration_s": 300,
        "sequence_pos": 0
    }
]

# Monday, August 31, 2020 10:00:00 AM GMT+03:00
TEST_TIMEZONE = dateutil.tz.gettz('Europe/Moscow')
TEST_DATETIME = datetime.fromtimestamp(1598857200).astimezone(TEST_TIMEZONE)


def _get_path(company_id, route_id):
    return f"/api/v1/companies/{company_id}/routes/{route_id}/work_breaks"


def _get_path_with_id(company_id, route_id, work_break_id):
    return f"/api/v1/companies/{company_id}/routes/{route_id}/work_breaks/{work_break_id}"


def _push_courier_position(env, courier_id, route_id, position_timestamp=None):
    if position_timestamp is None:
        position_timestamp = datetime.now().timestamp()
    path_push = f"/api/v1/couriers/{courier_id}/routes/{route_id}/push-positions"
    locations = [(55.82, 37.63, position_timestamp)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))


@skip_if_remote
def test_work_break(env: Environment):
    work_breaks = local_post(env.client, _get_path(env.default_company.id, env.default_route.id),
                             data=WORK_BREAKS_DATA, headers=env.user_auth_headers)
    assert len(work_breaks) == 2
    assert work_breaks[0].keys() == {'id', 'sequence_pos'}

    work_breaks = local_get(env.client, _get_path(
        env.default_company.id, env.default_route.id), headers=env.user_auth_headers)
    assert len(work_breaks) == 2
    assert work_breaks[0].keys() == {
        'id', 'sequence_pos', 'rest_duration_s', 'work_time_range_till_rest'}

    work_breaks = local_post(env.client, _get_path(env.default_company.id, env.default_route.id),
                             data=NEW_WORK_BREAKS_DATA, headers=env.user_auth_headers)
    assert len(work_breaks) == 2

    work_breaks = local_get(env.client, _get_path(
        env.default_company.id, env.default_route.id), headers=env.user_auth_headers)
    assert len(work_breaks) == 4
    for i in range(4):
        assert work_breaks[i]['sequence_pos'] == i
        assert work_breaks[i]['rest_duration_s'] == 100 * (i + 1)

    deleted_work_break = local_delete(env.client, _get_path_with_id(
        env.default_company.id, env.default_route.id, work_breaks[2]['id']), headers=env.user_auth_headers)
    assert deleted_work_break.keys() == {'id', 'sequence_pos'}
    assert deleted_work_break['sequence_pos'] == 2
    assert deleted_work_break['id'] == work_breaks[2]['id']

    work_breaks = local_get(env.client, _get_path(
        env.default_company.id, env.default_route.id), headers=env.user_auth_headers)
    assert len(work_breaks) == 3
    for i in range(3):
        assert work_breaks[i]['sequence_pos'] == i
        assert work_breaks[i]['rest_duration_s'] == 100 * (i + 1 + (int)(i == 2))


@skip_if_remote
def test_delete_non_existent_work_break(env: Environment):
    local_delete(env.client, _get_path_with_id(env.default_company.id, env.default_route.id, 12345),
                 headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_invalid_work_time_range_till_rest(env: Environment):
    local_post(env.client, _get_path(env.default_company.id, env.default_route.id), data=INVALID_WORK_BREAKS_DATA,
               headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_extra_fields(env: Environment):
    local_post(env.client, _get_path(env.default_company.id, env.default_route.id), data=INVALID_WORK_BREAKS_EXTRA_PROPERTIES,
               headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_eta(env: Environment):
    with freeze_time(TEST_DATETIME) as freezed_time:
        task_id = "mock_task_uuid__sms_time_window_test"
        import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
        [route] = local_post(env.client, import_path, headers=env.user_auth_headers)
        route_id = route['id']
        courier_id = route['courier_id']

        local_post(env.client, _get_path(env.default_company.id, route_id),
                   data=WORK_BREAKS_DATA, headers=env.user_auth_headers)

        _push_courier_position(env, courier_id, route_id)
        route_state_context = {'lat': 55.82, 'lon': 37.63, 'timestamp': datetime.now().timestamp()}
        update_route(env, route_id, route_state_context)

        with env.flask_app.app_context():
            work_breaks = WorkBreak.get_by_route_id(route_id)
            assert work_breaks[0].status == WorkBreakStatus.planned
            assert work_breaks[0].start is not None
            assert work_breaks[1].status == WorkBreakStatus.planned

        freezed_time.tick(delta=timedelta(seconds=work_breaks[0].start.timestamp() - datetime.now().timestamp()))
        _push_courier_position(env, courier_id, route_id)
        route_state_context = {'lat': 55.82, 'lon': 37.63, 'timestamp': datetime.now().timestamp()}
        update_route(env, route_id, route_state_context)

        with env.flask_app.app_context():
            work_breaks = WorkBreak.get_by_route_id(route_id)
            assert work_breaks[0].status == WorkBreakStatus.started
            assert work_breaks[0].start.timestamp() == datetime.now().timestamp()
            assert work_breaks[1].status == WorkBreakStatus.planned

        freezed_time.tick(delta=timedelta(seconds=work_breaks[0].rest_duration_s))
        _push_courier_position(env, courier_id, route_id)
        route_state_context = {'lat': 55.82, 'lon': 37.63, 'timestamp': datetime.now().timestamp()}
        update_route(env, route_id, route_state_context)

        with env.flask_app.app_context():
            work_breaks = WorkBreak.get_by_route_id(route_id)
            assert work_breaks[0].status == WorkBreakStatus.finished
            assert work_breaks[1].status == WorkBreakStatus.planned
            assert work_breaks[1].start is not None

        freezed_time.tick(delta=timedelta(
            seconds=work_breaks[1].start.timestamp() - datetime.now().timestamp() + work_breaks[1].rest_duration_s))
        route_state_context = {'lat': 55.82, 'lon': 37.63, 'timestamp': datetime.now().timestamp()}
        update_route(env, route_id, route_state_context)

        with env.flask_app.app_context():
            work_breaks = WorkBreak.get_by_route_id(route_id)
            assert work_breaks[0].status == WorkBreakStatus.finished
            assert work_breaks[1].status == WorkBreakStatus.finished

        another_courier_number = 'another_courier_number'
        couriers_path = f'/api/v1/companies/{env.default_company.id}/couriers'
        courier = local_post(env.client, couriers_path, data={'number': another_courier_number},
                             headers=env.user_auth_headers)
        courier_id = courier['id']

        route_path = f'/api/v1/companies/{env.default_company.id}/routes/{route_id}'
        local_patch(env.client, route_path, data={'courier_id': courier_id}, headers=env.user_auth_headers)

        _push_courier_position(env, courier_id, route_id)
        route_state_context = {'lat': 55.82, 'lon': 37.63, 'timestamp': datetime.now().timestamp()}
        update_route(env, route_id, route_state_context)

        with env.flask_app.app_context():
            work_breaks = WorkBreak.get_by_route_id(route_id)
            assert work_breaks[0].status == WorkBreakStatus.planned
            assert work_breaks[1].status == WorkBreakStatus.planned


@skip_if_remote
def test_predict_eta(env: Environment):
    task_id = "mock_task_uuid__sms_time_window_test"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, import_path, headers=env.user_auth_headers)
    route_id = route['id']
    courier_id = route['courier_id']

    local_post(env.client, _get_path(env.default_company.id, route_id),
               data=[WORK_BREAKS_DATA[0]], headers=env.user_auth_headers)

    # Test predict eta in the future
    _push_courier_position(env, courier_id, route_id, TEST_DATETIME.timestamp())
    predict_eta_path = f'/api/v1/couriers/{courier_id}/routes/{route_id}/predict-eta'
    local_get(env.client, predict_eta_path, {'lat': 55.82, 'lon': 37.63,
                                             'time': (TEST_DATETIME + timedelta(hours=5)).isoformat()},
              headers=env.user_auth_headers, expected_status=HTTPStatus.OK)

    # Test predict eta in the past
    _push_courier_position(env, courier_id, route_id, (TEST_DATETIME + timedelta(hours=5)).timestamp())
    route_state_context = {'lat': 55.82, 'lon': 37.63,
                           'timestamp': (TEST_DATETIME + timedelta(hours=5)).timestamp()}
    update_route(env, route_id, route_state_context)

    local_get(env.client, predict_eta_path, {'lat': 55.82, 'lon': 37.63,
                                             'time': TEST_DATETIME.isoformat()},
              headers=env.user_auth_headers, expected_status=HTTPStatus.OK)
