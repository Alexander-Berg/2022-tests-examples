from datetime import datetime
from http import HTTPStatus

import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_delete,
    local_post,
    local_patch,
    create_order,
    create_test_order_data, create_empty_route,
)
from ya_courier_backend.models import db, Route


@skip_if_remote
def test_time_windows_add_order(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)

    # 2. add an order
    create_order(env, number='order_1', route_id=route['id'], time_interval='06:00-07:15')

    # 4. get route and check that both times are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T06:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T07:15:00+03:00')


@skip_if_remote
def test_time_windows_modify_order(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)

    # 2. add an order
    order = create_order(env, number='order_1', route_id=route['id'], time_interval='06:00-07:15')

    # 3. modify time_interval of an order
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    order_patch = {'time_interval': '07:00-15:00'}
    local_patch(env.client,
                path_orders + f'/{order["id"]}',
                headers=env.user_auth_headers,
                data=order_patch)

    # 4. get route and check that both times are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T07:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T15:00:00+03:00')


@skip_if_remote
def test_time_windows_delete_order(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)

    # 2. add two orders
    order1 = create_order(env, number='order_1', route_id=route['id'], time_interval='06:00-07:15')
    create_order(env, number='order_2', route_id=route['id'], time_interval='07:00-12:00')

    # 3. delete one order
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    local_delete(env.client,
                 path_orders + f'/{order1["id"]}',
                 headers=env.user_auth_headers)

    # 4. get route and check that both times are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T07:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T12:00:00+03:00')


@skip_if_remote
def test_time_windows_delete_all_orders(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)

    # 2. add order
    order = create_order(env, number='order_1', route_id=route['id'], time_interval='06:00-07:15')

    # 3. delete order
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    local_delete(env.client,
                 path_orders + f'/{order["id"]}',
                 headers=env.user_auth_headers)

    # 4. get route and check that both times are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route['id']).first()
        assert route.time_window_min_start is None
        assert route.time_window_max_end is None


@skip_if_remote
def test_time_windows_change_route(env: Environment):
    # 1. create 2 empty routes
    route1 = create_empty_route(env)
    route2 = create_empty_route(env, route_number='2')

    # 2. post two orders to route1 and one order to route2
    create_order(env, number='order_1', route_id=route1['id'], time_interval='06:00-07:15')
    order2 = create_order(env, number='order_2', route_id=route1['id'], time_interval='07:00-12:30')
    create_order(env, number='order_3', route_id=route2['id'], time_interval='03:00-04:00')

    # 3. move order2 to route2
    path_order = f'/api/v1/companies/{env.default_company.id}/orders/{order2["id"]}'
    local_patch(env.client,
                path_order,
                headers=env.user_auth_headers,
                data={'route_id': route2['id']})

    # 4. get routes and check that times for both of them are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route1['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T06:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T07:15:00+03:00')

        route = db.session.query(Route).filter(Route.id == route2['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T03:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T12:30:00+03:00')


@pytest.mark.parametrize(
    ('test_field', 'max_length'),
    [
        ('customer_name', 1023),
        ('description', 1023),
        ('number', 80)
    ]
)
@skip_if_remote
def test_order_long_value_post(env: Environment, test_field, max_length):
    route = create_empty_route(env)
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'

    valid_params = {test_field: 'v' * max_length}
    invalid_params = {test_field: 'v' * (max_length + 1)}
    valid_data = create_test_order_data(route['id'], **valid_params)
    invalid_data = create_test_order_data(route['id'], **invalid_params)

    local_post(env.client, path_orders, headers=env.user_auth_headers, data=valid_data, expected_status=HTTPStatus.OK)

    invalid_post_response = local_post(env.client, path_orders, headers=env.user_auth_headers, data=invalid_data,
                                       expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert f"Failed validating 'maxLength' in schema['properties']['{test_field}']" in invalid_post_response['message']


@pytest.mark.parametrize(
    ('test_field', 'max_length'),
    [
        ('customer_name', 1023),
        ('description', 1023),
        ('number', 80)
    ]
)
@skip_if_remote
def test_order_long_value_patch(env: Environment, test_field, max_length):
    route = create_empty_route(env)
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    data = create_test_order_data(route['id'])

    new_order = local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)
    path_order = f'/api/v1/companies/{env.default_company.id}/orders/{new_order["id"]}'

    invalid_params = {test_field: 'v' * (max_length + 1)}
    invalid_data = create_test_order_data(route['id'], **invalid_params)

    invalid_patch_response = local_patch(env.client, path_order, headers=env.user_auth_headers, data=invalid_data,
                                         expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    assert f"Failed validating 'maxLength' in schema['properties']['{test_field}']" in invalid_patch_response['message']


@pytest.mark.parametrize(
    ('type'),
    [
        'delivery',
        'pickup',
        'drop_off',
    ]
)
@skip_if_remote
def test_duplicate_order_post(env: Environment, type):
    route = create_empty_route(env)
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    data = create_test_order_data(route['id'])
    data['type'] = type

    local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)
    local_post(env.client, path_orders, headers=env.user_auth_headers, data=data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@pytest.mark.parametrize(
    ('type'),
    [
        'delivery',
        'pickup',
        'drop_off',
    ]
)
@skip_if_remote
def test_duplicate_order_patch(env: Environment, type):
    route = create_empty_route(env)
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    data = create_test_order_data(route['id'])
    data['type'] = type
    order_number = data['number']
    local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)

    data['number'] = 'order_patch'
    order = local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)
    path_orders_patch = f'/api/v1/companies/{env.default_company.id}/orders/{order["id"]}'

    local_patch(env.client, path_orders_patch, headers=env.user_auth_headers,
                data={'number': order_number}, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
