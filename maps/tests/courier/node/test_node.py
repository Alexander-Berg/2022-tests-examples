import copy
import json
from datetime import datetime
from http import HTTPStatus

import dateutil.parser as dp
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_get,
    local_post, create_empty_route,
)
from ya_courier_backend.models import (
    DepotInstance,
    DepotInstanceStatus,
    db,
    Route,
    RouteNode,
)

DEFAULT_ORDER = {
    'number': 'default_order_number',
    'time_interval': '00:00-23:59',
    'address': 'some address',
    'lat': 55.791928,
    'lon': 37.841492,
}

DEFAULT_GARAGE = {
    'address': 'test-address',
    'lat': 55.664695,
    'lon': 37.562443,
    'number': 'default_garage_number',
}


def _get_route_with(courier_number, depot_number, number):
    return {
        'number': number,
        'courier_number': courier_number,
        'depot_number': depot_number,
        'date': '2020-11-30',
    }


def _get_garage_with_number(number):
    garage = copy.deepcopy(DEFAULT_GARAGE)
    garage['number'] = number
    return garage


def _get_depot_with_number(number):
    return {'number': number}


def _get_order_with_number(number):
    order = copy.deepcopy(DEFAULT_ORDER)
    order['number'] = number
    return order


def _create_empty_route(env, number='1'):
    path_route = f'/api/v1/companies/{env.default_company.id}/routes'
    route_data = _get_route_with(env.default_courier.number, env.default_depot.number, number)
    return local_post(env.client, path_route, headers=env.user_auth_headers, data=route_data)


def _nodes_from_orders(orders):
    return [
        {'type': 'order', 'value': order}
        for order in orders
    ]


@skip_if_remote
@pytest.mark.parametrize(('mode'), ['default', 'using_ids'])
def test_only_order_nodes_post(env: Environment, mode):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode={mode}'

    # 2. prepare and post order nodes
    orders = [_get_order_with_number('order_1'), _get_order_with_number('order_2')]
    nodes = _nodes_from_orders(orders)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. request orders and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(orders)
    for original_order, received_order in zip(orders, response):
        assert original_order['number'] == received_order['number']


@skip_if_remote
def test_only_garage_node_post(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post garage node
    nodes = [{'type': 'garage', 'value': _get_garage_with_number('garage_1')}]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. request garage and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_only_depot_node_post(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post depot node
    nodes = [{'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. request depots and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
@pytest.mark.parametrize(('mode'), ['default', 'using_ids'])
def test_post_nodes_with_first_garage_and_depot(env: Environment, mode):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode={mode}'

    # 2. prepare and post order nodes and first depot and garage
    orders = [_get_order_with_number('order_1'), _get_order_with_number('order_2')]
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
@pytest.mark.parametrize(('mode'), ['default', 'using_ids'])
def test_post_nodes_with_last_garage_and_depot(env: Environment, mode):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode={mode}'

    # 2. prepare and post order nodes and last depot and garage
    orders = [_get_order_with_number('order_1'), _get_order_with_number('order_2')]
    nodes = [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
@pytest.mark.parametrize(('mode'), ['default', 'using_ids'])
def test_post_nodes_with_first_and_last_garage_and_depot(env: Environment, mode):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode={mode}'

    # 2. prepare and post nodes of all types
    orders = [_get_order_with_number('order_1'), _get_order_with_number('order_2')]
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_post_first_garage_and_depot_to_existing_route_with_only_orders(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes only
    nodes = _nodes_from_orders([_get_order_with_number('order_1'), _get_order_with_number('order_2')])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. prepare and post same nodes but with first garage and depot
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + nodes
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_post_last_garage_and_depot_to_existing_route_with_only_orders(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes only
    nodes = _nodes_from_orders([_get_order_with_number('order_1'), _get_order_with_number('order_2')])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. prepare and post same nodes but with last garage and depot
    nodes = nodes + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_post_first_and_last_garages_and_depots_to_existing_route_with_only_orders(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes only
    nodes = _nodes_from_orders([_get_order_with_number('order_1'), _get_order_with_number('order_2')])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. prepare and post same nodes but with first and last garage and depot
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + nodes + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_post_last_garages_and_depots_to_existing_route_with_orders_and_first_garage_and_depot(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes and first garage and depot nodes
    orders = [_get_order_with_number('order_1'), _get_order_with_number('order_2')]
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. prepare and post same nodes but with last garage and depot
    nodes = nodes + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_post_first_garages_and_depots_to_existing_route_with_orders_and_last_garage_and_depot(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes and last garage and depot nodes
    nodes = [
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')}
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. prepare and post same nodes but with first garage and depot
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + nodes
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_order_sequence_change_with_only_order_nodes(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes
    orders = [
        _get_order_with_number('order_1'),
        _get_order_with_number('order_2'),
        _get_order_with_number('order_3'),
    ]
    nodes = _nodes_from_orders(orders)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. change first and last orders
    nodes[0], nodes[2] = nodes[2], nodes[0]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check new sequence
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']

    # 5. shift orders by one
    nodes[0], nodes[1], nodes[2] = nodes[1], nodes[2], nodes[0]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 6. check new sequence
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_order_sequence_change_with_all_node_types(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post nodes of all types
    orders = [
        _get_order_with_number('order_1'),
        _get_order_with_number('order_2'),
        _get_order_with_number('order_3')
    ]
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. change first and last orders
    nodes[2], nodes[4] = nodes[4], nodes[2]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check new sequence
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']

    # 5. shift orders by one
    nodes[2], nodes[3], nodes[4] = nodes[3], nodes[4], nodes[2]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 6. check new sequence
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_deleting_nodes_from_the_end_one_by_one(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post nodes of all types
    orders = [
        _get_order_with_number('order_1'),
        _get_order_with_number('order_2'),
        _get_order_with_number('order_3')
    ]
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. delete nodes one by one from the end
    while nodes:
        del nodes[-1]
        local_post(env.client,
                   path_node,
                   headers=env.user_auth_headers,
                   data=nodes)

        # 4. check that node is deleted
        path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
        response = local_get(env.client, path_orders, headers=env.user_auth_headers)
        assert len(response) == len(nodes)
        for original_node, received_node in zip(nodes, response):
            assert original_node['type'] == received_node['type']
            assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_deleting_nodes_from_the_beginning_one_by_one(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post nodes of all types
    orders = [
        _get_order_with_number('order_1'),
        _get_order_with_number('order_2'),
        _get_order_with_number('order_3')
    ]
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. delete nodes one by one from the beginning
    while nodes:
        del nodes[0]
        local_post(env.client,
                   path_node,
                   headers=env.user_auth_headers,
                   data=nodes)

        # 4. check that node is deleted
        path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
        response = local_get(env.client, path_orders, headers=env.user_auth_headers)
        assert len(response) == len(nodes)
        for original_node, received_node in zip(nodes, response):
            assert original_node['type'] == received_node['type']
            assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_deleting_node_from_the_middle(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post nodes of all types
    orders = [
        _get_order_with_number('order_1'),
        _get_order_with_number('order_2'),
        _get_order_with_number('order_3')
    ]
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. delete node in the middle
    del nodes[3]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that node is deleted
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_deleting_all_nodes_at_once(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post nodes of all types
    orders = [
        _get_order_with_number('order_1'),
        _get_order_with_number('order_2'),
        _get_order_with_number('order_3')
    ]
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. delete all nodes
    nodes = []
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that all nodes are deleted
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == 0


@skip_if_remote
def test_add_delete_modify_and_reorder_nodes(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post some nodes
    orders = [
        _get_order_with_number('order_1'),
        _get_order_with_number('order_2'),
        _get_order_with_number('order_3')
    ]
    nodes = [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. prepare new order sequence:
    #    - add new orders
    #    - delete 'order_1'
    #    - modify 'order_2' change address
    orders = [
        _get_order_with_number('order_4'),
        _get_order_with_number('order_3'),
        _get_order_with_number('order_5'),
        _get_order_with_number('order_2'),
        _get_order_with_number('order_6'),
    ]
    orders[3]['address'] = 'new address'

    # 4. prepare and post nodes with:
    #    - new order sequence
    #    - new garage at the beginning
    #    - new depot at the end
    #    - new garage at the end
    #    - without depot at the beginning
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ] + [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_3')}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 5. check that changes applied
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)

    for entity, node in zip(response, nodes):
        assert entity['type'] == node['type']
        assert entity['number'] == node['value']['number']

    assert response[4]['address'] == nodes[4]['value']['address']


REQUIRED_FIELDS_MISSING_CASES = [
    {
        'id': 'order',
        'nodes': [{'type': 'order', 'value': {'number': 'order_1'}}],
        'index': 0,
    },
    {
        'id': 'order without number',
        'nodes': [{'type': 'order', 'value': {}}],
        'index': 0,
    },
    {
        'id': 'order with index 2',
        'nodes': [
            {'type': 'garage', 'value': DEFAULT_GARAGE},
            {'type': 'order', 'value': DEFAULT_ORDER},
            {'type': 'order', 'value': {'number': 'order_1'}},
        ],
        'index': 2,
    },
    {
        'id': 'depot',
        'nodes': [{'type': 'depot', 'value': {}}],
        'index': 0,
    },
    {
        'id': 'garage',
        'nodes': [{'type': 'order', 'value': {'number': 'order_1'}}],
        'index': 0,
    },
    {
        'id': 'garage without number',
        'nodes': [{'type': 'order', 'value': {}}],
        'index': 0,
    },
]


@skip_if_remote
@pytest.mark.parametrize(
    ('nodes', 'index'),
    [(case['nodes'], case['index']) for case in REQUIRED_FIELDS_MISSING_CASES],
    ids=[case['id'] for case in REQUIRED_FIELDS_MISSING_CASES])
def test_post_nodes_without_required_fields_fails(env: Environment, nodes, index):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    response = local_post(env.client,
                          path_node,
                          headers=env.user_auth_headers,
                          data=nodes,
                          expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert f'at $[{index}].value' in response['message']
    value_str = json.dumps(nodes[index]['value'], sort_keys=True, indent=4)
    assert f'context: {value_str}' in response['message']


@skip_if_remote
def test_post_nodes_without_required_fields_utf_8(env: Environment):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'
    nodes = [{'type': 'order', 'value': {'number': 'order_1', 'address': 'ул. Озерская, д.1'}}]

    response = local_post(env.client,
                          path_node,
                          headers=env.user_auth_headers,
                          data=nodes,
                          expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert 'ул. Озерская, д.1' in response['message']


@skip_if_remote
def test_unknown_type(env: Environment):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'
    nodes = [{'type': 'bad type', 'value': '{}'}]

    response = local_post(env.client,
                          path_node,
                          headers=env.user_auth_headers,
                          data=nodes,
                          expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert 'at $[0]' in response['message']


@skip_if_remote
def test_post_non_existing_depot_fails(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post non existing depot
    nodes = [
        {'type': 'depot', 'value': _get_depot_with_number('non existing depot')}
    ]
    response = local_post(env.client,
                          path_node,
                          headers=env.user_auth_headers,
                          data=nodes,
                          expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "'non existing depot' does not exist" in response['message']


@skip_if_remote
def test_correct_errors_for_wrong_input_format(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. Try post dict instead of array
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data={},
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 3. Try post string istead of array
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data='hello',
               expected_status=HTTPStatus.BAD_REQUEST)


@skip_if_remote
def test_add_node_to_non_existing_route_fails(env: Environment):
    # 1. Post node to non existing route
    non_existing_route_id = 123
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{non_existing_route_id}/nodes'
    nodes = _nodes_from_orders([_get_order_with_number('order_1')])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_deleting_completed_nodes_fails(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post nodes of all types
    order = _get_order_with_number('order_1')
    garage = _get_garage_with_number('garage_1')
    nodes = [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'order', 'value': order},
        {'type': 'garage', 'value': garage}
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    path_push = f'/api/v1/couriers/{env.default_courier.id}/routes/{route["id"]}/push-positions'
    timestamp = dp.parse('2020-11-30 12:00:00').timestamp()

    # 3.1 visit depot
    locations = [
        (env.default_depot.lat, env.default_depot.lon, timestamp),
        (env.default_depot.lat, env.default_depot.lon, timestamp + 1800),
    ]
    local_post(env.client,
               path_push,
               headers=env.user_auth_headers,
               data=prepare_push_positions_data(locations))

    # 3.2 try to post request without visited depot
    nodes_without_depot = copy.copy(nodes)
    del nodes_without_depot[0]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes_without_depot,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 4.1 visit order
    locations = [
        (order['lat'], order['lon'], timestamp + 3600),
        (order['lat'], order['lon'], timestamp + 5400),
    ]
    local_post(env.client,
               path_push,
               headers=env.user_auth_headers,
               data=prepare_push_positions_data(locations))

    # 4.2 try to post request without visited order
    nodes_without_order = copy.copy(nodes)
    del nodes_without_order[1]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes_without_order,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 5.1 visit garage
    locations = [
        (garage['lat'], garage['lon'], timestamp + 7200),
        (garage['lat'], garage['lon'], timestamp + 9000),
    ]
    local_post(env.client,
               path_push,
               headers=env.user_auth_headers,
               data=prepare_push_positions_data(locations))

    # 5.2 try to post request without visited garage
    nodes_without_garage = copy.copy(nodes)
    del nodes_without_garage[2]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes_without_garage,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_route_with_depot_or_garage_between_orders_not_supported(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. try post nodes with depot between orders
    nodes =[
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 3. try post nodes with garage between orders
    nodes =[
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 4. try post nodes with depot and garage between orders
    nodes =[
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_multiple_depots_or_garages_at_the_beginning_not_supported(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. try post nodes with multiple depot at the beginning
    nodes =[
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 3. try post nodes with multiple garage at the beginning
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_multiple_depots_or_garages_at_the_end_not_supported(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. try post nodes with multiple depot at the end
    nodes =[
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 3. try post nodes with multiple garage at the end
    nodes =[
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_route_of_first_and_last_depot_and_garages_only_succeeds(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. try post nodes garage-depot-depot-garage
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.OK)


@skip_if_remote
def test_request_fails_on_wrong_depot_garage_order(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. try post nodes with wrong depot/garage order in the beginning
    nodes =[
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 3. try post nodes with wrong depot/garage order in the end
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_request_fails_if_few_orders_have_the_same_number(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. try post nodes with wrong depot/garage order in the beginning
    nodes =[
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_single_depot_route_does_not_change_if_depot_posted_again(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single depot
    nodes =[
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post single depot again
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check if the depot still in the route
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)


@skip_if_remote
def test_delete_single_depot_success(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single depot
    nodes =[
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. delete depot
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=[])

    # 4. check that there is no depot anymore
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == 0


@skip_if_remote
def test_post_route_with_last_depot_to_route_with_single_depot(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single depot
    nodes =[
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post route with last depot
    nodes = [
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that route is imported correctly
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_nodes in zip(nodes, response):
        assert original_node['type'] == received_nodes['type']
        assert original_node['value']['number'] == received_nodes['number']


@skip_if_remote
def test_post_route_with_first_depot_to_route_with_single_depot(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single depot
    nodes =[
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post route with last depot
    nodes = [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that route is imported correctly
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_nodes in zip(nodes, response):
        assert original_node['type'] == received_nodes['type']
        assert original_node['value']['number'] == received_nodes['number']


@skip_if_remote
def test_post_route_with_two_depots_to_route_with_single_depot(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single depot
    nodes =[
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post route with first and last depot
    nodes = [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that route is imported correctly
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']

    # ensure that new depot_instance was created
    with env.flask_app.app_context():
        depot_nodes = RouteNode.get(route_node_type='depot_instance')
        assert len({node.entity_id for node in depot_nodes})


@skip_if_remote
def test_post_route_with_two_depots_to_route_with_single_depot_of_another_number(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single depot
    nodes =[
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post route with first and last depot and different number
    nodes = [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_second_depot.number)},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_second_depot.number)},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that route is imported correctly
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_single_garage_route_does_not_change_if_garage_posted_again(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single garage
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post single garage again
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check if the garage still in the route
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)


@skip_if_remote
def test_delete_single_garage_success(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single garage
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. delete garage
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=[])

    # 4. check that there is no garage anymore
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == 0


@skip_if_remote
def test_post_route_with_last_garage_to_route_with_single_garage(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single garage
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post route with last garage
    nodes = [
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that route is imported correctly
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_nodes in zip(nodes, response):
        assert original_node['type'] == received_nodes['type']
        assert original_node['value']['number'] == received_nodes['number']


@skip_if_remote
def test_post_route_with_first_garage_to_route_with_single_garage(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single garage
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post route with last garage
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that route is imported correctly
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_nodes in zip(nodes, response):
        assert original_node['type'] == received_nodes['type']
        assert original_node['value']['number'] == received_nodes['number']


@skip_if_remote
def test_post_route_with_two_garages_to_route_with_single_garage(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single garage
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post route with first and last garage
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that route is imported correctly
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']

    # ensure that new garage_instance was created
    with env.flask_app.app_context():
        garage_nodes = RouteNode.get(route_node_type='garage')
        assert len({node.entity_id for node in garage_nodes})


@skip_if_remote
def test_post_route_with_two_garages_to_route_with_single_garage_of_another_number(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. post single garage
    nodes =[
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. post route with first and last garage and different number
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'garage', 'value': _get_garage_with_number('garage_2')},
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. check that route is imported correctly
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@skip_if_remote
def test_depot_from_another_company_results_in_error(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)

    # 2. create another depot in shared_company
    depot_data = {
        'number': 'another_depot',
        'address': 'ул. Льва Толстого, 16',
        'lat': 55.7447,
        'lon': 37.6728,
    }
    path_depots = f'/api/v1/companies/{env.default_shared_company.id}/depots'
    depot = local_post(env.client, path_depots, headers=env.superuser_auth_headers, data=depot_data)

    # 3. post single depot
    nodes = [{'type': 'depot', 'value': _get_depot_with_number(depot['number'])}]
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'
    local_post(
        env.client,
        path_node,
        headers=env.user_auth_headers,
        data=nodes,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )

    # 4. check that no nodes were added
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert response == []


@skip_if_remote
def test_same_depot_from_another_company_is_ignored(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)

    # 2. create another depot in shared_company with the same number
    depot_data = {
        'number': env.default_depot.number,
        'address': 'ул. Льва Толстого, 16',
        'lat': 55.7447,
        'lon': 37.6728,
    }
    path_depots = f'/api/v1/companies/{env.default_shared_company.id}/depots'
    local_post(env.client, path_depots, headers=env.superuser_auth_headers, data=depot_data)

    # 3. post single depot
    nodes = [{'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}]
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'
    local_post(
        env.client,
        path_node,
        headers=env.user_auth_headers,
        data=nodes,
    )

    # 4. check that no nodes were added
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,depot,garage'
    [added_depot] = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert added_depot['id'] == env.default_depot.id


@skip_if_remote
def test_post_route_with_two_depots_to_route_with_single_depot_and_updating_status_of_first_depot(env: Environment):
    # 1. setup - empty route, data to post
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'
    base_data = [
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
        {'type': 'order', 'value': _get_order_with_number('order_1')},
        {'type': 'order', 'value': _get_order_with_number('order_2')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)},
    ]

    # 2. post a single depot - add a first depot
    data = [(base_data[0])]     # just first depot

    local_post(
        client=env.client,
        path=path_node,
        headers=env.user_auth_headers,
        data=data
    )

    with env.flask_app.app_context():
        depot_instances = DepotInstance.get(order_by=[DepotInstance.id])
        assert len(depot_instances) == 1
        first_depot_instance_id = depot_instances[0].id
        assert depot_instances[0].status == DepotInstanceStatus.unvisited

    # 3. update status of the first depot, add orders and add a last depot
    data = copy.deepcopy(base_data)
    data[0]['value']['status'] = DepotInstanceStatus.visited.value  # first depot - status visited

    local_post(
        client=env.client,
        path=path_node,
        headers=env.user_auth_headers,
        data=data
    )

    with env.flask_app.app_context():
        depot_instances = DepotInstance.get(order_by=[DepotInstance.id])
        assert len(depot_instances) == 2
        assert depot_instances[0].id == first_depot_instance_id
        assert depot_instances[0].status == DepotInstanceStatus.visited
        assert depot_instances[1].status == DepotInstanceStatus.unvisited

    # 4. try to change status of the first depot to unvisited - this will not happen, but without raising error
    data = copy.deepcopy(base_data)
    data[0]['value']['status'] = DepotInstanceStatus.unvisited.value    # first depot - status unvisited

    local_post(
        client=env.client,
        path=path_node,
        headers=env.user_auth_headers,
        data=data,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )

    with env.flask_app.app_context():
        depot_instances = DepotInstance.get(order_by=[DepotInstance.id])
        assert depot_instances[0].status == DepotInstanceStatus.visited

    # 5. try to delete visited depot instance - will raise errors.DeletingCompletedNode
    data = copy.deepcopy(base_data)
    data[0]['value'] = _get_depot_with_number(env.default_second_depot.number)  # first depot - replace to another depot

    local_post(
        client=env.client,
        path=path_node,
        headers=env.user_auth_headers,
        data=data,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )


@skip_if_remote
def test_time_windows_basic(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes
    order1 = _get_order_with_number('order_1')
    order1['time_interval'] = '06:00-07:15'
    order2 = _get_order_with_number('order_2')
    order2['time_interval'] = '07:00-12:30'
    nodes = _nodes_from_orders([order1, order2])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. get route and check that both times are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T06:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T12:30:00+03:00')


@skip_if_remote
def test_time_windows_modify_order(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes
    order1 = _get_order_with_number('order_1')
    order1['time_interval'] = '06:00-07:15'
    order2 = _get_order_with_number('order_2')
    order2['time_interval'] = '07:00-12:30'
    nodes = _nodes_from_orders([order1, order2])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. modify time_interval of an order
    order2['time_interval'] = '07:00-15:00'
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. get route and check that both times are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T06:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T15:00:00+03:00')


@skip_if_remote
def test_time_windows_delete_order(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes
    order1 = _get_order_with_number('order_1')
    order1['time_interval'] = '06:00-07:15'
    order2 = _get_order_with_number('order_2')
    order2['time_interval'] = '07:00-12:30'
    nodes = _nodes_from_orders([order1, order2])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. delete one order
    nodes = [nodes[0]]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. get route and check that both times are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T06:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T07:15:00+03:00')


@skip_if_remote
def test_time_windows_add_order(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes
    order1 = _get_order_with_number('order_1')
    order1['time_interval'] = '06:00-07:15'
    order2 = _get_order_with_number('order_2')
    order2['time_interval'] = '07:00-12:30'
    nodes = _nodes_from_orders([order1, order2])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. add one order
    order3 = _get_order_with_number('order_3')
    order3['time_interval'] = '03:00-04:00'
    nodes = _nodes_from_orders([order1, order2, order3])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 4. get route and check that both times are correct
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route['id']).first()
        assert route.time_window_min_start == datetime.fromisoformat('2020-11-30T03:00:00+03:00')
        assert route.time_window_max_end == datetime.fromisoformat('2020-11-30T12:30:00+03:00')


@skip_if_remote
def test_post_nodes_with_first_visited_depot(env: Environment):
    # 1. create an empty route
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes and first depot
    orders = [_get_order_with_number('order_1'), _get_order_with_number('order_2')]
    nodes = [
        {'type': 'depot', 'value': {**_get_depot_with_number(env.default_depot.number), 'status': 'visited'}}
    ] + [
        {'type': 'order', 'value': order}
        for order in orders
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 3. request nodes and check that all of them are presented in response
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']


@pytest.mark.parametrize(
    ('type'),
    [
        'delivery',
        'pickup',
        'drop_off',
    ]
)
@skip_if_remote
def test_post_nodes_with_order_of_different_types(env: Environment, type):
    # 1. create an empty route
    route = _create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes'

    # 2. prepare and post order nodes
    orders = [{**_get_order_with_number('order_1'), 'type': type}, {**_get_order_with_number('order_1'), 'type': type}]
    nodes = _nodes_from_orders(orders)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=[nodes[0]])

    route2 = _create_empty_route(env, '2')
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route2["id"]}/nodes'

    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=[nodes[0]],
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_post_all_type_of_nodes_using_ids(env: Environment):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode=using_ids'

    nodes = _nodes_from_orders([_get_order_with_number('order_1'), _get_order_with_number('order_2')])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    for order in response:
        order.pop('type')
    existing_nodes = _nodes_from_orders(response)
    for node in existing_nodes:
        node['value']['id'] = str(node['value']['id'])
    nodes = [
        {'type': 'garage', 'value': _get_garage_with_number('garage_1')},
        {'type': 'depot', 'value': _get_depot_with_number(env.default_depot.number)}
    ] + existing_nodes
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert existing_nodes
    assert len(response) == len(nodes)
    for original_node, received_node in zip(nodes, response):
        assert original_node['type'] == received_node['type']
        assert original_node['value']['number'] == received_node['number']
        if original_node['type'] == 'order':
            assert original_node['value']['id'] == str(received_node['id'])


@skip_if_remote
@pytest.mark.parametrize(
    ('type', 'status'),
    [
        ('delivery', HTTPStatus.UNPROCESSABLE_ENTITY),
        ('pickup', HTTPStatus.UNPROCESSABLE_ENTITY),
        ('drop_off', HTTPStatus.OK),
    ]
)
def test_post_same_order_using_ids(env: Environment, type, status):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode=using_ids'

    nodes = _nodes_from_orders([{**_get_order_with_number('order_1'), 'type': type}])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=status)


@skip_if_remote
@pytest.mark.parametrize(
    ('type', 'status'),
    [
        ('delivery', HTTPStatus.UNPROCESSABLE_ENTITY),
        ('pickup', HTTPStatus.UNPROCESSABLE_ENTITY),
        ('drop_off', HTTPStatus.OK),
    ]
)
def test_post_duplicated_order_using_ids(env: Environment, type, status):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode=using_ids'

    orders = [{**_get_order_with_number('order_1'), 'type': type}, {**_get_order_with_number('order_1'), 'type': type}]
    nodes = _nodes_from_orders(orders)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=status)


@skip_if_remote
@pytest.mark.parametrize(
    ('mode'),
    [
        'default',
        'using_ids',
    ]
)
def test_post_order_with_id_not_in_route_using_ids(env: Environment, mode):
    route = create_empty_route(env)
    first_route_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}/nodes?mode={mode}'
    second_route_path = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode={mode}'

    nodes = _nodes_from_orders([_get_order_with_number('order_1')])
    local_post(env.client,
               first_route_path,
               headers=env.user_auth_headers,
               data=nodes)

    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    nodes[0]['value']['id'] = str(response[0]['id'])
    local_post(env.client,
               second_route_path,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
@pytest.mark.parametrize(
    ('mode'),
    [
        'default',
        'using_ids',
    ]
)
def test_same_number_is_unprocessable_for_different_types(env: Environment, mode):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode={mode}'

    orders = [
        {**_get_order_with_number('order_1'), 'type': 'delivery'},
        {**_get_order_with_number('order_1'), 'type': 'drop_off'},
    ]
    nodes = _nodes_from_orders(orders)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


def test_route_nodes_order_sequence_for_new_orders_using_ids(env: Environment):
    # o1 o2 -> o3 o1 o4
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode=using_ids'

    orders = [
        {**_get_order_with_number('order_1'), 'type': 'drop_off'},
        {**_get_order_with_number('order_1'), 'type': 'drop_off'},
    ]
    nodes = _nodes_from_orders(orders)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    order_ids = [item['id'] for item in response]

    nodes.append({'type': 'order', 'value': _get_order_with_number('order_1')})
    nodes[2]['value']['type'] = 'drop_off'
    nodes[1]['value']['id'] = str(order_ids[0])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    new_order_ids = [item['id'] for item in response]
    assert set(order_ids) - set(new_order_ids) == {order_ids[1]}
    assert set(new_order_ids) - set(order_ids) == {new_order_ids[0], new_order_ids[2]}


def test_route_nodes_order_sequence_for_duplicate_numbers_using_ids(env: Environment):
    # o0 o1 o2 -> o2 o3 o0 o4
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode=using_ids'

    orders = [
        {**_get_order_with_number('order_1'), 'type': 'drop_off'},
        {**_get_order_with_number('order_1'), 'type': 'drop_off'},
        {**_get_order_with_number('order_1'), 'type': 'drop_off'},
    ]
    nodes = _nodes_from_orders(orders)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    order_ids = [item['id'] for item in response]

    nodes.extend(_nodes_from_orders([{**_get_order_with_number('order_1'), 'type': 'drop_off'}]))

    nodes[0]['value']['id'] = str(order_ids[2])
    nodes[2]['value']['id'] = str(order_ids[0])
    for ix, node in enumerate(nodes):
        node['value']['comments'] = str(ix)
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    new_order_comments = [item['comments'] for item in response]
    assert new_order_comments == ['0', '1', '2', '3']


def test_post_nodes_with_incorrect_mode(env: Environment):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode=using_numbers'

    nodes = _nodes_from_orders([_get_order_with_number('order_1')])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_post_and_patch_order_using_ids(env: Environment):
    route = create_empty_route(env)
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}/nodes?mode=using_ids'

    nodes = _nodes_from_orders([_get_order_with_number('order_1')])
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?types=order,garage,depot'
    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    nodes[0]['value']['id'] = str(response[0]['id'])
    new_number = 'New order number in test'
    nodes[0]['value']['number'] = new_number
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    response = local_get(env.client, path_orders, headers=env.user_auth_headers)
    assert len(response) == 1
    assert response[0]['number'] == new_number
