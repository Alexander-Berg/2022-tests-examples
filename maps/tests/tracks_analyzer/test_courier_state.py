from datetime import datetime, timedelta
import dateutil
from freezegun import freeze_time
import pytest
import pytz
import time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_get, local_post
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage, update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data

from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str


def _empty_courier_state(courier_id, courier_number):
    return {
        'courier_id': str(courier_id),
        'number': courier_number,
        'last_position': None,
        'ongoing_events': []
    }


def _courier_idle_event(route_id, time, time_zone):
    return {
        "type": "idle",
        "route_id": str(route_id),
        "start": {
            "value": time + 15 * 60,
            "text": get_isoformat_str(time + 15 * 60, time_zone)
        },
        "point": {
            "lat": 58.82,
            "lon": 37.73
        },
    }


def _last_position(time, time_zone):
    return {
        'point': {
            'lat': 58.82,
            'lon': 37.73,
        },
        'server_time': {
            'value': time,
            'text': get_isoformat_str(time, time_zone),
        }
    }


@skip_if_remote
@pytest.mark.parametrize("route_id_pos", [0, 1])
def test_courier_state(env, route_id_pos):
    # 0. Import two routes
    set_company_import_depot_garage(env, env.default_company.id, True)
    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, import_path, headers=env.user_auth_headers)

    route_ids = [route['id'] for route in routes]
    push_pathes = [f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions" for route_id in route_ids]

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route_ids[route_id_pos]}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    route_start_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    start_datetime = datetime.fromtimestamp(route_start_time).astimezone(pytz.utc)
    with freeze_time(start_datetime) as freezed_time:
        now = time.time()
        depot_timezone = dateutil.tz.gettz(env.default_depot.time_zone)

        # 1. No events at both routes
        path_courier_state = f'/api/v1/companies/{env.default_company.id}/courier-states'
        query = {"courier_ids": f"{env.default_courier.id}"}
        courier_states = local_get(env.client, path_courier_state, query=query, headers=env.user_auth_headers)

        assert courier_states == [_empty_courier_state(env.default_courier.id, env.default_courier.number)]

        # 2. Event at first route, no event at second route
        freezed_time.tick(delta=timedelta(minutes=45))
        now = time.time()
        locations = [(58.82, 37.73, now + 15 * 60), (58.82, 37.73, now + 30 * 60), (58.82, 37.73, now + 45 * 60)]
        local_post(env.client, push_pathes[route_id_pos], headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 58.82, 'lon': 38.73, 'timestamp': now + 45 * 60}
        update_route(env, route_ids[route_id_pos], route_state_context)
        courier_states = local_get(env.client, path_courier_state, query=query, headers=env.user_auth_headers)

        assert courier_states[0]['ongoing_events'] == [_courier_idle_event(route_ids[route_id_pos], now, depot_timezone)]
        assert courier_states[0]['last_position'] == _last_position(now, depot_timezone)

        # 3. Events at both routes
        local_post(env.client, push_pathes[1 - route_id_pos], headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        update_route(env, route_ids[1 - route_id_pos], route_state_context)
        courier_states = local_get(env.client, path_courier_state, query=query, headers=env.user_auth_headers)

        assert len(courier_states[0]['ongoing_events']) == 2
        ongoing_events = [_courier_idle_event(route_id, now, depot_timezone) for route_id in route_ids]
        assert courier_states[0]['ongoing_events'] == ongoing_events
        assert courier_states[0]['last_position'] == _last_position(now, depot_timezone)

        # 4. No events at both routes because courier started to move and left idle state
        freezed_time.tick(delta=timedelta(minutes=45))
        now = time.time()
        locations = [(58.82, 37.76, now + 15 * 60), (58.82, 37.77, now + 30 * 60), (58.82, 37.78, now + 45 * 60)]
        route_state_context = {'lat': 58.82, 'lon': 38.78, 'timestamp': now + 45 * 60}
        local_post(env.client, push_pathes[route_id_pos], headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        update_route(env, route_ids[route_id_pos], route_state_context)
        local_post(env.client, push_pathes[1 - route_id_pos], headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        update_route(env, route_ids[1 - route_id_pos], route_state_context)
        courier_states = local_get(env.client, path_courier_state, query=query, headers=env.user_auth_headers)

        assert courier_states[0]['ongoing_events'] == []


@skip_if_remote
def test_courier_state_with_no_courier(env):
    path_courier_state = f'/api/v1/companies/{env.default_company.id}/courier-states'
    query = {"courier_ids": "41,4141"}
    courier_states = local_get(env.client, path_courier_state, query=query, headers=env.user_auth_headers)

    assert courier_states == []


@skip_if_remote
def test_courier_state_with_multiple_couriers(env):
    # 0. Add new courier without routes
    add_courier_path = f'/api/v1/companies/{env.default_company.id}/couriers'
    resp = local_post(env.client, add_courier_path, headers=env.user_auth_headers,
                      data={'number': 'test_courier_number'})
    courier_id = resp['id']
    courier_number = resp['number']

    # 1. Check that information about both couriers was returned
    path_courier_state = f'/api/v1/companies/{env.default_company.id}/courier-states'
    query = {"courier_ids": f"{env.default_courier.id},{courier_id}"}
    courier_states = local_get(env.client, path_courier_state, query=query, headers=env.user_auth_headers)

    assert len(courier_states) == 2
    assert courier_states == [_empty_courier_state(env.default_courier.id, env.default_courier.number), _empty_courier_state(courier_id, courier_number)]
