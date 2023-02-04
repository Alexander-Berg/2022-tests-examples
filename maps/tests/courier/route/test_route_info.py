from http import HTTPStatus
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage
from ya_courier_backend.util.route_info import NODE_FIELDS


@skip_if_remote
def test_route_info_fields(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    # 2. Remember number of nodes in the route
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={routes[0]['id']}"
    response = local_get(env.client, path_route_info, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)

    # 3. Check nodes
    nodes = response[0]['nodes']

    for node in nodes:
        point = node['value'].pop('point', None)
        assert set(node['value']).issubset(set(NODE_FIELDS[node['type']]))
        assert point and point['lat'] is not None and point['lon'] is not None


@skip_if_remote
def test_time_window_equal_to_arrival_time_is_selected(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, False)

    route = {
        "number": "test_route",
        "courier_number": env.default_courier.number,
        "depot_number": env.default_depot.number,
        "date": "2021-04-01",
    }
    order = {
        "number": "test_order",
        "time_interval": "11:00 - 11:00",
        "eta_type": "delivery_time",
        "address": "ул. Льва Толстого, 16",
        "lat": 55.7447,
        "lon": 37.6728,
        "route_number": "test_route",
    }

    path_add_route = f"/api/v1/companies/{env.default_company.id}/routes"
    route = local_post(env.client, path_add_route, headers=env.user_auth_headers, data=route)

    path_add_orders = f"/api/v1/companies/{env.default_company.id}/orders"
    local_post(env.client, path_add_orders, headers=env.user_auth_headers, data=order)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    local_get(env.client, path_route_info, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)

    path_routed_orders = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/routed-orders"
    position_data = {"lat": 55.7447, "lon": 37.6728, "time_now": "11:00"}
    local_get(env.client, path_routed_orders, query=position_data, headers=env.user_auth_headers)

    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)
    assert route_info["nodes"][0]["value"]["estimated_service_time"]["start"]["text"] == "2021-04-01 11:00:00+03:00"


@skip_if_remote
def test_filter_by_date_considers_route_finish_s(env: Environment):
    # 1. Import route with route_finish_s = 3.23:59:00 (2021-11-16T23:59:00)
    route = {
        'number': 'test_route',
        'courier_number': env.default_courier.number,
        'depot_number': env.default_depot.number,
        'date': '2021-11-13',
        'route_start_s': 56520,
        'route_finish_s': 345540
    }
    path_add_route = f'/api/v1/companies/{env.default_company.id}/routes'
    route = local_post(env.client, path_add_route, headers=env.user_auth_headers, data=route)

    # 2. Get /route-info for 2021-11-16 and expect the route to be present in response
    path_route_info = f'/api/v1/companies/{env.default_company.id}/route-info?date=2021-11-16'
    route_info = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    assert route_info[0]['meta']['id'] == str(route['id'])

    # 3. Get /route-info for 2021-11-17 and expect no routes to be present in response
    path_route_info = f'/api/v1/companies/{env.default_company.id}/route-info?date=2021-11-17'
    route_info = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    assert len(route_info) == 0


@skip_if_remote
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

    # 2. Get /route-info for 2021-11-14 and expect no routes in response
    path_route_info = f'/api/v1/companies/{env.default_company.id}/route-info?date=2021-11-14'
    route_info = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    assert len(route_info) == 0


@skip_if_remote
@pytest.mark.parametrize('depot', ['default_santiago_depot', 'default_east_depot'])
def test_filter_by_date_works_fine_with_timezone_offset(env: Environment, depot):
    # 1. Import route with route_finish_s = 3.23:59:00 (2021-11-16T23:59:00)
    route = {
        'number': 'test_route',
        'courier_number': env.default_another_courier.number,
        'depot_number': getattr(env, depot).number,
        'date': '2021-11-13',
        'route_start_s': 56520,
        'route_finish_s': 345540
    }
    path_add_route = f'/api/v1/companies/{env.default_another_company.id}/routes'
    route = local_post(env.client, path_add_route, headers=env.superuser_auth_headers, data=route)

    # 2. Get /route-info for 2021-11-16 and expect the route to be present in response
    path_route_info = f'/api/v1/companies/{env.default_another_company.id}/route-info?date=2021-11-16'
    route_info = local_get(env.client, path_route_info, headers=env.superuser_auth_headers)
    assert route_info[0]['meta']['id'] == str(route['id'])

    # 3. Get /route-info for 2021-11-17 and expect no routes to be present in response
    path_route_info = f'/api/v1/companies/{env.default_another_company.id}/route-info?date=2021-11-17'
    route_info = local_get(env.client, path_route_info, headers=env.superuser_auth_headers)
    assert len(route_info) == 0


@skip_if_remote
@pytest.mark.parametrize('depot', ['default_santiago_depot', 'default_east_depot'])
def test_filter_by_date2_works_fine_with_timezone_offset(env: Environment, depot):
    # 1. Import route without route_start/route_finish
    route = {
        'number': 'test_route',
        'courier_number': env.default_another_courier.number,
        'depot_number': getattr(env, depot).number,
        'date': '2021-11-13',
    }
    path_add_route = f'/api/v1/companies/{env.default_another_company.id}/routes'
    route = local_post(env.client, path_add_route, headers=env.superuser_auth_headers, data=route)

    # 2. Get /route-info for 2021-11-12 and expect no routes
    path_route_info = f'/api/v1/companies/{env.default_another_company.id}/route-info?date=2021-11-12'
    route_info = local_get(env.client, path_route_info, headers=env.superuser_auth_headers)
    assert len(route_info) == 0

    # 3. Get /route-info for 2021-11-13 and expect no routes to be present in response
    path_route_info = f'/api/v1/companies/{env.default_another_company.id}/route-info?date=2021-11-13'
    route_info = local_get(env.client, path_route_info, headers=env.superuser_auth_headers)
    assert route_info[0]['meta']['id'] == str(route['id'])

    # 4. Get /route-info for 2021-11-14 and expect no routes
    path_route_info = f'/api/v1/companies/{env.default_another_company.id}/route-info?date=2021-11-14'
    route_info = local_get(env.client, path_route_info, headers=env.superuser_auth_headers)
    assert len(route_info) == 0
