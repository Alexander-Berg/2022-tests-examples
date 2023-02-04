from copy import deepcopy
from http import HTTPStatus
from datetime import datetime, timedelta
from dateutil.tz import gettz

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    local_delete,
    local_post,
    create_test_order_data, create_empty_route,
    local_get,
)

from ya_courier_backend.models import db, Order


TEST_ORDER_DICT = create_test_order_data(1)
TEST_DATETIME = datetime(2019, 12, 13, 12, 10, 0, tzinfo=gettz('Europe/Moscow'))


def _setup_multiple_dropoff(env):
    delete_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    local_delete(env.client, delete_path, headers=env.user_auth_headers)

    route = create_empty_route(env, date=TEST_DATETIME.isoformat())
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    data = create_test_order_data(route_id=route['id'], time_interval='00:00-00:00')
    data['type'] = 'drop_off'
    order_number = data['number']
    order_first = local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)

    data['number'] = 'second_order'
    order_second = local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)

    with env.flask_app.app_context():
        order = db.session.query(Order).filter(Order.id == order_second['id']).first()
        order.number = order_number
        db.session.commit()

    return {
        'route_id': route['id'],
        'order_ids': [
            order_first['id'],
            order_second['id'],
        ],
    }


@skip_if_remote
def test_get_expected_orders(env):
    test_info = _setup_multiple_dropoff(env)
    now = TEST_DATETIME
    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now.timestamp() + 10}
    update_route(env, test_info['route_id'], route_state_context)

    path = f'/api/v1/companies/{env.default_company.id}/expected-orders'
    query = {
        'lat': TEST_ORDER_DICT['lat'],
        'lon': TEST_ORDER_DICT['lon'],
        'radius': 500,
        'from': now.isoformat(),
        'to': (now + timedelta(hours=23)).isoformat(),
    }
    assert len(local_get(env.client, path, headers=env.user_auth_headers, query=query)) == 2


@skip_if_remote
def test_get_route_details(env):
    test_info = _setup_multiple_dropoff(env)
    now = TEST_DATETIME
    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now.timestamp() + 10}
    update_route(env, test_info['route_id'], route_state_context)

    path = f'api/v1/companies/{env.default_company.id}/route-details'
    query = {
        'date': now.date().isoformat(),
        'route_id': test_info['route_id'],
    }
    resp = local_get(env.client, path, query=query, headers=env.user_auth_headers)
    assert len(resp[0]['route_state']['next_orders']) == 2


@skip_if_remote
def test_get_route_history(env):
    test_info = _setup_multiple_dropoff(env)
    now = TEST_DATETIME
    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now.timestamp() + 10}
    update_route(env, test_info['route_id'], route_state_context)

    path = f'api/v1/companies/{env.default_company.id}/route-history'
    query = {
        'date': now.date().isoformat(),
        'courier_number': env.default_courier.number,
    }
    resp = local_get(env.client, path, query=query, headers=env.user_auth_headers)
    for route_history in resp:
        if 'next_orders' in route_history:
            assert set(route_history['next_orders']) == set(test_info['order_ids'])


@skip_if_remote
def test_get_route_state(env):
    test_info = _setup_multiple_dropoff(env)
    now = TEST_DATETIME
    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now.timestamp() + 10}
    update_route(env, test_info['route_id'], route_state_context)

    path = f'api/v1/companies/{env.default_company.id}/route-state'
    query = {
        'date': now.date().isoformat(),
        'courier_number': env.default_courier.number,
    }
    resp = local_get(env.client, path, query=query, headers=env.user_auth_headers)
    resp['next_orders'] == set(test_info['order_ids'])


@skip_if_remote
def test_get_route_states(env):
    test_info = _setup_multiple_dropoff(env)
    now = datetime.now().astimezone(tz=gettz('Europe/Moscow'))
    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now.timestamp() + 10}
    update_route(env, test_info['route_id'], route_state_context)

    path = f'api/v1/companies/{env.default_company.id}/route-states'
    query = {
        'date': now.date().isoformat(),
        'courier_number': env.default_courier.number,
    }
    resp = local_get(env.client, path, query=query, headers=env.user_auth_headers)
    for route_history in resp:
        if 'next_orders' in route_history:
            assert set(route_history['next_orders']) == set(test_info['order_ids'])


@skip_if_remote
def test_get_predict_eta(env):
    test_info = _setup_multiple_dropoff(env)
    now = TEST_DATETIME
    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now.timestamp() + 10}
    update_route(env, test_info['route_id'], route_state_context)

    order_dict = create_test_order_data(env.default_route.id)
    path = f'/api/v1/couriers/{env.default_courier.id}/routes/{test_info["route_id"]}/predict-eta'
    query = {
        'lat': order_dict['lat'],
        'lon': order_dict['lon'],
        'time': now.isoformat(),
    }
    resp = local_get(env.client, path, headers=env.user_auth_headers, query=query)
    assert len(resp['route']) == 2
    assert resp['route'][0]['number'] == resp['route'][1]['number']


@skip_if_remote
def test_get_order_sequence(env):
    test_info = _setup_multiple_dropoff(env)
    path = f'/api/v1/companies/{env.default_company.id}/routes/{test_info["route_id"]}/order-sequence'
    resp = local_get(env.client, path, headers=env.user_auth_headers)
    assert len(set(resp['order_ids'])) == 2
    assert len(set(resp['order_numbers'])) == 1


@skip_if_remote
def test_post_order_sequence(env):
    test_info = _setup_multiple_dropoff(env)
    path = f'/api/v1/companies/{env.default_company.id}/routes/{test_info["route_id"]}/order-sequence'
    order_dict = create_test_order_data(env.default_route.id)
    data = [order_dict['number']] * 2
    local_post(env.client, path, headers=env.user_auth_headers, data=data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    data = test_info['order_ids']
    local_post(env.client, path, headers=env.user_auth_headers, data=data)


@skip_if_remote
def test_get_courier_quality(env):
    _setup_multiple_dropoff(env)
    now = TEST_DATETIME
    path = f'/api/v1/companies/{env.default_company.id}/courier-quality?date={now.date().isoformat()}'
    resp = local_get(env.client, path, headers=env.user_auth_headers)
    orders = [node for node in resp if node['type'] == 'order']

    assert len(orders) == 2
    assert orders[0]['order_number'] == orders[1]['order_number']


@skip_if_remote
def test_get_order_details(env):
    _setup_multiple_dropoff(env)
    path = f'/api/v1/companies/{env.default_company.id}/order-details?order_number=order_number'
    orders = local_get(env.client, path, headers=env.user_auth_headers)

    assert len(orders) == 2
    assert orders[0]['order_number'] == orders[1]['order_number']


@skip_if_remote
def test_get_orders(env):
    _setup_multiple_dropoff(env)
    path = f'/api/v1/companies/{env.default_company.id}/orders'
    orders = local_get(env.client, path, headers=env.user_auth_headers)

    assert len(orders) == 2
    assert orders[0]['number'] == orders[1]['number']


@skip_if_remote
def test_post_order_batch(env):
    test_info = _setup_multiple_dropoff(env)
    path = f'/api/v1/companies/{env.default_company.id}/orders-batch'
    order_dict = create_test_order_data(test_info['route_id'])
    order_dict_unique_number = deepcopy(order_dict)
    order_dict_unique_number['number'] += ' unique'

    local_post(env.client, path, headers=env.user_auth_headers, data=[order_dict_unique_number])

    local_post(env.client, path, headers=env.user_auth_headers,
               data=[order_dict], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_get_orders_by_id(env):
    test_info = _setup_multiple_dropoff(env)
    for order_id in test_info['order_ids']:
        path = f'/api/v1/companies/{env.default_company.id}/orders/{order_id}'
        resp = local_get(env.client, path, headers=env.user_auth_headers)
        assert resp['number'] == 'order_number'


@skip_if_remote
def test_get_order_info(env):
    test_info = _setup_multiple_dropoff(env)
    for order_id in test_info['order_ids']:
        path = f'/api/v1/companies/{env.default_company.id}/orders/{order_id}/order-info'
        local_get(env.client, path, headers=env.user_auth_headers)


@skip_if_remote
def test_get_tracking_with_routed_orders(env):
    test_info = _setup_multiple_dropoff(env)
    now = TEST_DATETIME

    routed_orders_path = f'/api/v1/couriers/{env.default_courier.id}/routes/{test_info["route_id"]}/routed-orders'
    create_track_path = f'/api/v1/couriers/{env.default_courier.id}/routes/{test_info["route_id"]}/create-track'
    query = {
        'lat': 55.7447,
        'lon': 37.6727,
        'timestamp': now.timestamp() + 10,
    }
    local_get(env.client, routed_orders_path, query=query, headers=env.user_auth_headers)

    tracks = []
    for order_id in test_info['order_ids']:
        query = {
            'order_id': order_id,
        }
        tracks.append(local_post(env.client, create_track_path, query=query, headers=env.user_auth_headers))

    tracking1 = local_get(env.client, f'/api/v1/tracking/{tracks[0]["track_id"]}', headers=env.user_auth_headers)
    tracking2 = local_get(env.client, f'/api/v1/tracking/{tracks[1]["track_id"]}', headers=env.user_auth_headers)
    assert tracking1['order']['id'] != tracking2['order']['id']
    assert tracking1['order']['number'] == tracking2['order']['number']

    for order_id in test_info['order_ids']:
        path = f'/api/v1/companies/{env.default_company.id}/orders/{order_id}/track-ids'
        assert len(local_get(env.client, path, headers=env.user_auth_headers)) == 1


@skip_if_remote
def test_get_verification(env):
    _setup_multiple_dropoff(env)
    order_dict = create_test_order_data(env.default_route.id)
    path = f'/api/v1/companies/{env.default_company.id}/verification?order_number={order_dict["number"]}'
    orders = local_get(env.client, path, headers=env.user_auth_headers)

    assert len(orders) == 2
    assert orders[0]['order_number'] == orders[1]['order_number']


@skip_if_remote
def test_post_order_batch_with_create_and_patch(env):
    #  in request: A B C
    #  in system: A A

    test_info = _setup_multiple_dropoff(env)
    path = f'/api/v1/companies/{env.default_company.id}/orders-batch'
    order_dict = create_test_order_data(test_info['route_id'])

    orders = []
    for _ in range(3):
        orders.append(deepcopy(order_dict))
    orders[1]['number'] += ' second'
    orders[2]['number'] += ' third'

    local_post(env.client, path, headers=env.user_auth_headers,
               data=orders, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_post_nodes_with_create_and_patch(env):
    _setup_multiple_dropoff(env)
    route_number = 'nodes_with_create_and_patch'
    route = create_empty_route(env, date=TEST_DATETIME.isoformat(), route_number=route_number)
    path = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    order_dict = create_test_order_data(route['id'])
    orders = []
    for _ in range(3):
        orders.append(deepcopy(order_dict))
    orders[1]['number'] += ' second'
    orders[2]['number'] += ' third'

    nodes = [
        {'type': 'order', 'value': order}
        for order in orders
    ]
    local_post(env.client, path, headers=env.user_auth_headers,
               data=nodes, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
