import pytest

from datetime import datetime
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_post,
    local_get,
    push_positions,
    prepare_push_positions_data
)

START = 1636794000  # 2021-11-13 12:00:00+3
TRACK = {
    (55.736294, 37.582708, START),
    (55.735834, 37.584918, START + 1)
}


def push_positions_another(env, route_id, positions):
    path_push = f"/api/v1/couriers/{env.default_another_courier.id}/routes/{route_id}/push-positions"
    local_post(env.client,
               path_push,
               headers=env.superuser_auth_headers,
               data=prepare_push_positions_data(positions))


@skip_if_remote
@freeze_time(datetime.fromtimestamp(START))
def test_filter_by_date_considers_route_finish_s(env: Environment):
    # 1. Import route with route_finish_s = 3.23:59:00 (2021-11-16T23:59:00)
    route = {
        'number': 'test_route',
        'courier_number': env.default_courier.number,
        'depot_number': env.default_depot.number,
        'date': '2021-11-13',
        'route_start_s': 0,
        'route_finish_s': 345540
    }
    path_add_route = f'/api/v1/companies/{env.default_company.id}/routes'
    route = local_post(env.client, path_add_route, headers=env.user_auth_headers, data=route)
    push_positions(env, route['id'], TRACK)

    # 2. Get /courier-position for 2021-11-16 and expect the route to be present in response
    path_positions = f'/api/v1/companies/{env.default_company.id}/courier-position?date=2021-11-16'
    positions = local_get(env.client, path_positions, headers=env.user_auth_headers)
    assert len(positions) == 1

    # 3. Get /courier-position for 2021-11-17 and expect no routes to be present in response
    path_positions = f'/api/v1/companies/{env.default_company.id}/courier-position?date=2021-11-17'
    positions = local_get(env.client, path_positions, headers=env.user_auth_headers)
    assert len(positions) == 0


@skip_if_remote
@freeze_time(datetime.fromtimestamp(START))
def test_filter_by_date_ignores_route_max_lateness(env: Environment):
    # 1. Import route with and order with time_window.end = 23:59
    route = {
        'number': 'test_route',
        'courier_number': env.default_courier.number,
        'depot_number': env.default_depot.number,
        'date': '2021-11-13'
    }
    path_add_route = f'/api/v1/companies/{env.default_company.id}/routes'
    route = local_post(env.client, path_add_route, headers=env.user_auth_headers, data=route)
    push_positions(env, route['id'], TRACK)

    order = {
        "number": "test_order",
        "time_interval": "11:00 - 23:59",
        "eta_type": "delivery_time",
        "address": "ул. Льва Толстого, 16",
        "lat": 55.7447,
        "lon": 37.6728,
        "route_number": "test_route",
    }
    path_add_orders = f"/api/v1/companies/{env.default_company.id}/orders"
    local_post(env.client, path_add_orders, headers=env.user_auth_headers, data=order)

    # 2. Get /courier-position for 2021-11-14 and expect no routes in response
    path_courier_position = f'/api/v1/companies/{env.default_company.id}/courier-position?date=2021-11-14'
    positions = local_get(env.client, path_courier_position, headers=env.user_auth_headers)
    assert len(positions) == 0


@skip_if_remote
@pytest.mark.parametrize('depot', ['default_santiago_depot', 'default_east_depot'])
@freeze_time(datetime.fromtimestamp(START))
def test_filter_by_date_works_fine_with_timezone_offset(env: Environment, depot):
    # 1. Import route with route_finish_s = 3.23:59:00 (2021-11-16T23:59:00)
    route = {
        'number': 'test_route',
        'courier_number': env.default_another_courier.number,
        'depot_number': getattr(env, depot).number,
        'date': '2021-11-13',
        'route_start_s': 0,
        'route_finish_s': 345540
    }
    path_add_route = f'/api/v1/companies/{env.default_another_company.id}/routes'
    route = local_post(env.client, path_add_route, headers=env.superuser_auth_headers, data=route)
    push_positions_another(env, route['id'], TRACK)

    # 2. Get /courier-position for 2021-11-16 and expect the route to be present in response
    path_courier_position = f'/api/v1/companies/{env.default_another_company.id}/courier-position?date=2021-11-16'
    positions = local_get(env.client, path_courier_position, headers=env.superuser_auth_headers)
    assert len(positions) == 1

    # 3. Get /courier-position for 2021-11-17 and expect no routes to be present in response
    path_courier_position = f'/api/v1/companies/{env.default_another_company.id}/courier-position?date=2021-11-17'
    positions = local_get(env.client, path_courier_position, headers=env.superuser_auth_headers)
    assert len(positions) == 0
