from datetime import datetime, timedelta, time as dt_time
from dateutil.tz import gettz
from freezegun import freeze_time
import pytest
import time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import ROUTES_DATE
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_get, local_patch, local_post, add_courier
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data

from ya_courier_backend.tasks.finish_route_events import FinishRouteEventsTask, FinishRouteEventsMode
from ya_courier_backend.util.route_event import (
    FINISH_ROUTE_EVENTS_RETRY_TIME,
    route_events_with_time_courier_for_finish
)


TEST_DATETIME = datetime.combine(ROUTES_DATE, dt_time(9), tzinfo=gettz('Europe/Moscow'))


def _create_route_event_with_idle_event(env, freezed_time):
    task_id = 'mock_task_uuid__generic'
    import_path = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}'
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]['id']
    push_positions_path = f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions'
    now = time.time()

    route_path = f'api/v1/companies/{env.default_company.id}/routes/{route_id}'
    patch_data = {'courier_id': env.default_courier.id}
    local_patch(env.client, route_path, data=patch_data, headers=env.user_auth_headers)

    locations = [
        (55.7447, 37.6727, now),
        (55.7447, 37.6727, now + 15 * 60),
        (55.7447, 37.6727, now + 30 * 60),
    ]
    local_post(env.client, push_positions_path,
               headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now + 30 * 60}
    freezed_time.tick(delta=timedelta(seconds=30 * 60))
    update_route(env, route_id, route_state_context)

    now = time.time()

    locations = [
        (58.82, 37.73, now + 15 * 60),
        (58.82, 37.73, now + 30 * 60),
        (58.82, 37.73, now + 45 * 60),
        (58.82, 37.73, now + 60 * 60),
    ]
    local_post(env.client, push_positions_path,
               headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {'lat': 58.82, 'lon': 38.73, 'timestamp': now + 60 * 60}
    freezed_time.tick(delta=timedelta(seconds=60 * 60))
    update_route(env, env.default_route.id, route_state_context)

    return route_id


@skip_if_remote
@freeze_time(TEST_DATETIME)
def test_finish_route_events_select(env):
    with freeze_time(TEST_DATETIME) as freezed_time:
        _create_route_event_with_idle_event(env, freezed_time)
        freezed_time.tick(delta=timedelta(seconds=FINISH_ROUTE_EVENTS_RETRY_TIME + 1))
        now = time.time()
        with env.flask_app.app_context():
            assert len(route_events_with_time_courier_for_finish(now)) == 1

            task = FinishRouteEventsTask(env.flask_app)
            task.run()
            assert len(route_events_with_time_courier_for_finish(now)) == 0

            freezed_time.tick(delta=timedelta(seconds=FINISH_ROUTE_EVENTS_RETRY_TIME + 1))
            assert len(route_events_with_time_courier_for_finish(now + FINISH_ROUTE_EVENTS_RETRY_TIME + 1)) == 1


@skip_if_remote
def test_finish_route_events_route_in_progress(env):
    with freeze_time(TEST_DATETIME) as freezed_time:
        _create_route_event_with_idle_event(env, freezed_time)
        freezed_time.tick(delta=timedelta(seconds=FINISH_ROUTE_EVENTS_RETRY_TIME + 1))
        with env.flask_app.app_context():
            task = FinishRouteEventsTask(env.flask_app)
            task.run()

        route_event_path= f'/api/v1/companies/{env.default_company.id}/route-events'
        events = local_get(env.client, route_event_path, query={'from': TEST_DATETIME}, headers=env.user_auth_headers)

        assert len(events) == 1
        assert events[0]['finish'] is None


@skip_if_remote
def test_finish_route_events_route_finished(env):
    with freeze_time(TEST_DATETIME) as freezed_time:
        route_id = _create_route_event_with_idle_event(env, freezed_time)
        freezed_time.tick(delta=timedelta(seconds=FINISH_ROUTE_EVENTS_RETRY_TIME + 1))
        route_finish_s = 10 * 3600
        route_path = f'api/v1/companies/{env.default_company.id}/routes/{route_id}'
        patch_data = {'route_finish_s': route_finish_s}
        local_patch(env.client, route_path, data=patch_data, headers=env.user_auth_headers)
        with env.flask_app.app_context():
            task = FinishRouteEventsTask(env.flask_app)
            task.run()

        route_event_path= f'/api/v1/companies/{env.default_company.id}/route-events'
        events = local_get(env.client, route_event_path, query={'from': TEST_DATETIME}, headers=env.user_auth_headers)

        assert len(events) == 1
        assert events[0]['finish']['text'] == '2019-12-13T10:30:00+03:00'


@skip_if_remote
def test_finish_route_events_courier_changed(env):
    with freeze_time(TEST_DATETIME) as freezed_time:
        route_id = _create_route_event_with_idle_event(env, freezed_time)
        freezed_time.tick(delta=timedelta(seconds=FINISH_ROUTE_EVENTS_RETRY_TIME + 1))
        courier = add_courier(env, env.default_company.id, 'second_test_courier', phone=None)
        route_path = f'api/v1/companies/{env.default_company.id}/routes/{route_id}'
        local_patch(env.client, route_path, data={'courier_id': courier.id}, headers=env.user_auth_headers)
        with env.flask_app.app_context():
            task = FinishRouteEventsTask(env.flask_app)
            task.run()

        route_event_path= f'/api/v1/companies/{env.default_company.id}/route-events'
        events = local_get(env.client, route_event_path, query={'from': TEST_DATETIME}, headers=env.user_auth_headers)

        assert len(events) == 1
        assert events[0]['finish'] is not None


@skip_if_remote
def test_finish_route_events_route_finished_by_5h(env):
    with freeze_time(TEST_DATETIME) as freezed_time:
        _create_route_event_with_idle_event(env, freezed_time)
        route_event_path= f'/api/v1/companies/{env.default_company.id}/route-events'

        freezed_time.tick(delta=timedelta(seconds=8.5 * 3600))
        with env.flask_app.app_context():
            task = FinishRouteEventsTask(env.flask_app)
            task.run()
            events = local_get(env.client, route_event_path, query={'from': TEST_DATETIME}, headers=env.user_auth_headers)

            assert len(events) == 1
            assert events[0]['finish'] is None

            freezed_time.tick(delta=timedelta(seconds=FINISH_ROUTE_EVENTS_RETRY_TIME + 1))
            task = FinishRouteEventsTask(env.flask_app)
            task.run()
            events = local_get(env.client, route_event_path, query={'from': TEST_DATETIME}, headers=env.user_auth_headers)

            assert len(events) == 1
            assert events[0]['finish']['text'] == '2019-12-13T10:30:00+03:00'


@skip_if_remote
@pytest.mark.parametrize('task_mode', [FinishRouteEventsMode.normal.value, FinishRouteEventsMode.iterate.value])
def test_finish_route_events_periodic_task_run(env, task_mode):
    with freeze_time(TEST_DATETIME) as freezed_time:
        route_id = _create_route_event_with_idle_event(env, freezed_time)
        freezed_time.tick(delta=timedelta(seconds=FINISH_ROUTE_EVENTS_RETRY_TIME + 1))
        courier = add_courier(env, env.default_company.id, 'second_test_courier', phone=None)
        route_path = f'api/v1/companies/{env.default_company.id}/routes/{route_id}'
        local_patch(env.client, route_path, data={'courier_id': courier.id}, headers=env.user_auth_headers)

        with env.flask_app.app_context():
            task = FinishRouteEventsTask(env.flask_app)
            result = task.run({'task_mode': task_mode})
            assert result['iteration_count'] == 1
            assert result['task_mode'] == task_mode


@skip_if_remote
def test_finish_route_events_periodic_task_run_empty_mode(env):
    with env.flask_app.app_context():
        task = FinishRouteEventsTask(env.flask_app)
        assert task.run()['task_mode'] == FinishRouteEventsMode.normal.value
