from maps.b2bgeo.ya_courier.backend.test_lib import util
import requests
import time
import pytest


def test_orders_batch_performance(system_env_with_db):
    """
    Test the following workflow:
        - a route with 50 orders is created
        - the batch request with status modification for these orders is performed
            * check: batch modification took less than 10 seconds
    """
    positions = [{'lat': 55.003212, 'lon': 82.969714}] * 50
    with util.create_route_env(
        system_env_with_db,
        "test_orders_batch_performance",
        order_locations=positions
    ) as env:
        import json
        print(json.dumps(env["orders"][0], indent=4))

        changes = [
            {
                'number': order['number'],
                'status': 'confirmed'
            } for order in env['orders']
        ]
        start = time.time()
        util.batch_orders(system_env_with_db, changes)
        end = time.time()

        assert end - start < 10

        print("Orders are batch updated {}".format(end - start))


def test_orders_batch_numbers_performance(system_env_with_db):
    """
    Test the following workflow:
        - a route with 50 orders is created
        - prepare order changes replacing route_id`s by numbers
        - the batch request with status modification for these orders is performed
            * check: batch modification took less than 10 seconds
    """
    positions = [{'lat': 55.003212, 'lon': 82.969714}] * 50
    with util.create_route_env(
        system_env_with_db,
        "test_orders_batch_numbers_performance",
        order_locations=positions
    ) as env:
        changes = [
            {
                'route_number': env['route']['number'],
                'number': order['number'],
                'status': 'confirmed'
            } for order in env['orders']
        ]
        start = time.time()
        util.batch_orders(system_env_with_db, changes)
        end = time.time()

        assert end - start < 10

        print("Orders are batch updated {}".format(end - start))


def test_orders_batch_simple(system_env_with_db):
    """
    Test the following workflow:
        - a route with 3 orders is created
        - prepare order changes for first 2 orders, changing different things
        - the batch request with changes is performed
        - orders for the route are rerequested
            * check: changes are applied properly
    """

    POSITION1 = {'lat': 55.003212, 'lon': 82.969714}
    POSITION1_NEW = {'lat': 56.0, 'lon': 83.0}
    POSITION2 = {'lat': 55, 'lon': 82}
    POSITION3 = POSITION2
    CUSTOM_TIME_INTERVAL = '13:00 - 14:00'

    with util.create_route_env(
        system_env_with_db,
        "test_orders_batch_simple",
        order_locations=[POSITION1, POSITION2, POSITION3]
    ) as env:
        orders = env["orders"]
        route = env["route"]

        changes = [
            {
                'number': orders[0]['number'],
                'lat': POSITION1_NEW['lat'],
                'lon': POSITION1_NEW['lon']
            },
            {
                'number': orders[1]['number'],
                'time_interval': CUSTOM_TIME_INTERVAL
            }
        ]
        orders[0].update(changes[0])
        orders[1].update(changes[1])

        util.batch_orders(system_env_with_db, changes)

        new_orders = util.get_orders(system_env_with_db, route['id'])
        assert len(orders) == len(new_orders)

        assert len(new_orders[0]['history']) == 1
        assert len(new_orders[1]['history']) == 2
        assert len(new_orders[2]['history']) == 1
        new_orders[1]['history'] = None
        orders[1]['history'] = None
        for i in range(len(orders)):
            if i == 1:
                # Make sure that changing interval to CUSTOM_TIME_INTERVAL
                # has changed values of interval-dependent fields
                for name in ['time_window', 'time_interval_secs']:
                    assert new_orders[i][name] != orders[i][name]
                    del new_orders[i][name]
                    del orders[i][name]
            del new_orders[i]['notifications']
            assert new_orders[i] == {**orders[i], 'type': 'order'}


@pytest.mark.parametrize("route_column", ['id', 'number'])
def test_orders_batch_replace_route(system_env_with_db, route_column):
    """
    Test the following workflow:
        - a first route with 1 order is created
        - a second route with 1 order is created
        - the batch request moving order from the first to the second route is performed
          using route_number instead of id
        - orders are requested for the first and second route
            * check: the first route has no orders and the second route has 2 orders
    """

    POSITION = {'lat': 55.003212, 'lon': 82.969714}
    with util.create_route_env(
        system_env_with_db,
        f"test_orders_batch_replace_route1-{route_column}",
        order_locations=[POSITION]
    ) as env1:
        route1 = env1['route']
        order1 = env1['orders'][0]

        with util.create_route_env(
            system_env_with_db,
            f"test_orders_batch_replace_route2-{route_column}",
            order_locations=[POSITION]
        ) as env2:
            route2 = env2['route']
            order2 = env2['orders'][0]

            change = {
                'number': order1['number']
            }
            if route_column == 'id':
                change['route_id'] = route2['id']
            elif route_column == 'number':
                change['route_number'] = route2['number']
            else:
                assert False, 'Unexpected route_column param {}'.format(route_column)
            util.batch_orders(system_env_with_db, [change])

            orders1 = util.get_orders(system_env_with_db, route1['id'])
            assert len(orders1) == 0

            orders2 = util.get_orders(system_env_with_db, route2['id'])
            assert len(orders2) == 2

            order1['route_id'] = route2['id']
            assert all('route_sequence_pos' not in order for order in [order1, order2])
            del orders2[0]['notifications']
            del orders2[1]['notifications']
            assert orders2 == [{**order2, 'type': 'order'}, {**order1, 'type': 'order'}]


def test_orders_batch_route_not_exist(system_env_with_db):
    """
    Test the following workflow:
        - a route with 1 order is created
        - the batch request trying to change the route to a not existing one is performed
            * check: batch request is not processable
    """
    POSITION = {'lat': 55.003212, 'lon': 82.969714}

    with util.create_route_env(
        system_env_with_db,
        "test_orders_batch_route_not_exist",
        order_locations=[POSITION]
    ) as env:
        order = env["orders"][0]

        change = {
            'number': order['number'],
            'route_number': 'NOT_EXISTING_ROUTE_NUMBER'
        }

        status_code, _ = util.batch_orders(system_env_with_db, [change], strict=False)
        assert status_code == requests.codes.unprocessable


def test_orders_batch_service_duration_s_none(system_env_with_db):
    with util.create_route_env(
        system_env_with_db,
        "test_orders_batch_service_duration_s"
    ) as env:
        orders = env["orders"]

        changes = [
            {
                'number': orders[0]['number'],
                'service_duration_s': None
            }
        ]

        status_code, j = util.batch_orders(system_env_with_db, changes, strict=False)
        assert status_code == requests.codes.unprocessable
        assert j['message'].startswith("Json schema validation failed: OrdersBatch: None is not of type 'integer'") \
            and j['message'].find('service_duration_s') >= 0
