import datetime
import dateutil
import dateutil.tz
from freezegun import freeze_time
import pytest
import time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_get, local_post
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage, update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data

from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str
from ya_courier_backend.util.position import Position, MIN_POS_TIME
from ya_courier_backend.models import db, CourierIdleState, Courier
from ya_courier_backend.models.route_event import RouteEvent, RouteEventType


def _route_event(id, type, route_id, courier_id, courier_name, start, finish, time_zone):
    event_dict = {
        'id': str(id),
        'route_id': str(route_id),
        'courier_id': str(courier_id),
        'courier_name': courier_name,
        'type': type.value,
        'start': {
            'value': start,
            'text': get_isoformat_str(start, time_zone),
        },
        'point': {
            'lat': 58.82,
            'lon': 37.73,
        },
        'finish': None,
    }
    if finish:
        event_dict['finish'] = {
            'value': finish,
            'text': get_isoformat_str(finish, time_zone),
        }
    return event_dict


@skip_if_remote
@pytest.mark.parametrize('is_null_name', [True, False])
def test_route_event_resource(env, is_null_name):
    now = MIN_POS_TIME
    time_zone = dateutil.tz.gettz(env.default_depot.time_zone)
    with env.flask_app.app_context():
        if is_null_name:
            courier_id = env.default_company.id
            courier_name = None
        else:
            courier_name = 'test_route_event courier'
            courier = Courier(company_id=env.default_company.id, number='1', name=courier_name)
            db.session.add(courier)
            courier_id = db.session.query(Courier).filter(Courier.name == courier_name).first().id

        courier_idle = CourierIdleState.create(env.default_route.id, courier_id)
        courier_idle.is_idle = True
        courier_idle.idle_position = Position(37.73, 58.82, now)
        db.session.add(RouteEvent.from_courier_idle(courier_idle, env.default_company.id))
        db.session.flush()
        RouteEvent.finish(courier_idle.route_id, courier_idle.courier_id,
                          RouteEventType.IDLE, now + 10)
        db.session.commit()

    path_route_event= f'/api/v1/companies/{env.default_company.id}/route-events'
    test_event = _route_event(
        id='1',
        type=RouteEventType.IDLE,
        route_id=env.default_route.id,
        courier_id=courier_id,
        courier_name=courier_name,
        start=now,
        finish=now + 10,
        time_zone=time_zone,
    )
    test_response = [test_event]
    assert local_get(env.client, path_route_event, headers=env.user_auth_headers,
                     query={'from': get_isoformat_str(0, time_zone)}) == test_response

    assert len(local_get(env.client, path_route_event, headers=env.user_auth_headers,
                         query={'from': get_isoformat_str(0, time_zone)})) == 1
    assert len(local_get(env.client, path_route_event, headers=env.user_auth_headers,
                         query={'from': get_isoformat_str(now, time_zone)})) == 1
    assert len(local_get(env.client, path_route_event, headers=env.user_auth_headers,
                         query={'from': get_isoformat_str(now + 15, time_zone)})) == 0

    assert len(local_get(env.client, path_route_event, headers=env.user_auth_headers,
                         query={'from': get_isoformat_str(0, time_zone), 'to': get_isoformat_str(10, time_zone)})) == 0
    assert len(local_get(env.client, path_route_event, headers=env.user_auth_headers,
                         query={'from': get_isoformat_str(now, time_zone), 'to': get_isoformat_str(now + 10, time_zone)})) == 1
    assert len(local_get(env.client, path_route_event, headers=env.user_auth_headers,
                         query={'from': get_isoformat_str(now + 15, time_zone), 'to': get_isoformat_str(now + 20, time_zone)})) == 0


@skip_if_remote
def test_route_event(env):
    # 0. Import two routes
    set_company_import_depot_garage(env, env.default_company.id, True)
    task_id = 'mock_task_uuid__generic'
    import_path = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}'
    routes = local_post(env.client, import_path, headers=env.user_auth_headers)

    route_ids = [route['id'] for route in routes]
    push_paths = [f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions' for route_id in route_ids]

    path_route_info = f'/api/v1/companies/{env.default_company.id}/route-info?route_id={route_ids[0]}'
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    route_start_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    start_datetime = datetime.datetime.fromtimestamp(route_start_time).astimezone(datetime.timezone.utc)

    with freeze_time(start_datetime) as freezed_time:
        now = time.time()
        path_route_event= f'/api/v1/companies/{env.default_company.id}/route-events'
        query={'from': get_isoformat_str(0, datetime.timezone.utc)}

        test_event = _route_event(
            id='1',
            type=RouteEventType.IDLE,
            route_id=route_ids[0],
            courier_id=env.default_courier.id,
            courier_name='car-1',
            start=now + 60 * 60,
            finish=None,
            time_zone=dateutil.tz.gettz(env.default_depot.time_zone),
        )

        # 1. No events at both routes
        route_events = local_get(env.client, path_route_event, query=query, headers=env.user_auth_headers)

        assert route_events == []

        # 2. Event at first route, no event at second route
        freezed_time.tick(delta=datetime.timedelta(minutes=45))
        now = time.time()
        locations = [(58.82, 37.73, now + 15 * 60), (58.82, 37.73, now + 30 * 60), (58.82, 37.73, now + 45 * 60)]
        local_post(env.client, push_paths[0], headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 58.82, 'lon': 38.73, 'timestamp': now + 45 * 60}
        update_route(env, route_ids[0], route_state_context)
        route_events = local_get(env.client, path_route_event, query=query, headers=env.user_auth_headers)

        assert len(route_events) == 1
        assert route_events[0] == test_event

        # 3. Events at both routes
        local_post(env.client, push_paths[1], headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        update_route(env, route_ids[1], route_state_context)
        route_events = local_get(env.client, path_route_event, query=query, headers=env.user_auth_headers)

        assert len(route_events) == 2
        for event in route_events:
            assert event['start']['value'] == now + 15 * 60

        # 4. Events at both routes are finished
        freezed_time.tick(delta=datetime.timedelta(minutes=45))
        now = time.time()
        locations = [(58.82, 37.76, now + 15 * 60), (58.82, 37.77, now + 30 * 60), (58.82, 37.78, now + 45 * 60)]
        route_state_context = {'lat': 58.82, 'lon': 38.78, 'timestamp': now + 45 * 60}
        local_post(env.client, push_paths[0], headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        update_route(env, route_ids[0], route_state_context)
        local_post(env.client, push_paths[1], headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        update_route(env, route_ids[1], route_state_context)
        route_events = local_get(env.client, path_route_event, query=query, headers=env.user_auth_headers)

        assert len(route_events) == 2
        for event in route_events:
            assert event['finish']['value'] == now + 45 * 60
