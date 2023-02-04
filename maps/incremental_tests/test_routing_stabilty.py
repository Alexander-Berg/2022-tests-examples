from datetime import timedelta

import pytest
import json
import requests

from maps.b2bgeo.ya_courier.backend.test_lib.util import entity_equal, env_get_request, env_post_request, api_path_with_company_id
from pylev import classic_levenshtein

COURIER_ID = None

ROUTE = {
    'number': 'TEST_ROUTE_STABILITY',
    'courier_number': 'TEST_ROUTE_STABILITY_COURIER',
    'depot_number': 'TEST_ROUTE_STABILITY_DEPOT',
    'date': '2017-09-14'
}

ORDERS = [
    {
        'number': 'TEST_ROUTE_STABILITY_1',
        'address': 'Ленинская слобода, 4',
        'lat': 55.712394,
        'lon': 37.662021,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_2',
        'address': 'Автозаводская улица, 17к1',
        'lat': 55.704422,
        'lon': 37.654146,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_3',
        'address': 'улица Трофимова, 4',
        'lat': 55.703137,
        'lon': 37.662665,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_4',
        'address': 'улица Ленинская Слобода, 30с1',
        'lat': 55.705440,
        'lon': 37.647495,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_5',
        'address': 'Велозаводская улица, 7',
        'lat': 55.712999,
        'lon': 37.666528,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_6',
        'address': 'Автозаводская улица, 6',
        'lat': 55.709086,
        'lon': 37.660670,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_7',
        'address': 'Сокольники',
        'lat': 55.791521,
        'lon': 37.661366,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_8',
        'address': 'Хамовники',
        'lat': 55.726377,
        'lon': 37.567124,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_9',
        'address': 'Тверской район',
        'lat': 55.756107,
        'lon': 37.627893,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_10',
        'address': 'Таганский район',
        'lat': 55.742940,
        'lon': 37.672010,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_11',
        'address': 'Якиманка',
        'lat': 55.734127,
        'lon': 37.614846,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_12',
        'address': 'МКАД, 107 км',
        'lat': 55.791928,
        'lon': 37.841492,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_13',
        'address': 'МКАД, 87 км',
        'lat': 55.900931,
        'lon': 37.623044,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_14',
        'address': 'МКАД, 70 км',
        'lat': 55.846988,
        'lon': 37.391617,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_15',
        'address': 'МКАД, 46 км',
        'lat': 55.649909,
        'lon': 37.446456,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_16',
        'address': 'МКАД, 29 км',
        'lat': 55.572369,
        'lon': 37.656781,
    },
    {
        'number': 'TEST_ROUTE_STABILITY_17',
        'address': 'МКАД, 17 км',
        'lat': 55.633290,
        'lon': 37.808849,
    },
]

for order in ORDERS:
    order.update({
        'route_number': 'TEST_ROUTE_STABILITY',
        'time_interval': '08:00 - 23:59',
        'status': 'new',
        'phone': '+79161111111',
    })

ORDERS_DICT = {order['number']: order for order in ORDERS}
ORDERS_ID_DICT = {}

LOCATION = {
    'lat': 55.811264,
    'lon': 37.503887,
    'time_now': '8:00'
}

SOLUTION_ID = None
NUMBER_OF_ROUTES = 50


def get_routed_orders(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        path='couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}'.format(
            COURIER_ID, ROUTE['id'], LOCATION['lat'], LOCATION['lon'], LOCATION['time_now']))

    if response.status_code != requests.codes.ok:
        print(response.text)

    assert response.status_code == requests.codes.ok

    j = response.json()

    global SOLUTION_ID
    assert 'solution_id' in j
    SOLUTION_ID = j['solution_id']
    assert 'metrics' in j
    assert 'route' in j

    return j


def get_route_state(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        path=api_path_with_company_id(system_env_with_db, "route-state") +
        '?courier_number={}&date={}'.format(ROUTE['courier_number'], ROUTE['date']))

    if response.status_code != requests.codes.ok:
        print(response.text)

    assert response.status_code == requests.codes.ok

    j = response.json()
    state = j

    assert 'location' in state
    assert 'solution' in state
    assert 'fixed_orders' in state
    assert 'routed_orders' in state
    assert entity_equal(state['location'], LOCATION)
    assert SOLUTION_ID == state['solution']['id']

    return state


class TestRoutingStability(object):
    #
    # Initialization
    #
    def _create_entity(self, system_env_with_db, path, data):
        response = env_post_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, path),
            data=data
        )
        j = response.json()
        print(json.dumps(j, indent=4))

        assert response.status_code == requests.codes.ok
        assert j['inserted'] + j['updated'] == 1

    def test_create_courier(self, system_env_with_db):
        # create initial courier
        COURIER = {
            'number': 'TEST_ROUTE_STABILITY_COURIER',
            'name': 'Ваня',
            'phone': '+71234524423'
        }
        self._create_entity(system_env_with_db, "couriers-batch", [COURIER])

    def test_create_depot(self, system_env_with_db):
        # create initial depot
        DEPOT = {
            'number': 'TEST_ROUTE_STABILITY_DEPOT',
            'name': 'Склад 1',
            'address': 'ул. Льва Толстого, 16',
            'time_interval': '10:00 - 22:00',
            'lat': 55.7447,
            'lon': 37.6728,
            'description': 'курьерский подъезд',
            'service_duration_s': 600,
            'order_service_duration_s': 10
        }
        self._create_entity(system_env_with_db, "depots-batch", [DEPOT])

    def test_routes_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, 'routes-batch'),
            data=[ROUTE, ])
        j = response.json()
        print(json.dumps(j, indent=4))

        assert response.status_code == requests.codes.ok
        assert j['inserted'] + j['updated'] == 1

    def test_get_route_id(self, system_env_with_db):
        global ROUTE, COURIER_ID
        response = env_get_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, 'routes') + '?number={}'.format(ROUTE['number']))
        j = response.json()
        print(json.dumps(j, indent=4))

        assert response.status_code == requests.codes.ok
        assert len(j) == 1
        assert entity_equal(j[0], ROUTE)
        ROUTE['id'] = j[0]['id']
        COURIER_ID = j[0]['courier_id']

    def test_orders_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, 'orders-batch'),
            data=ORDERS)
        j = response.json()
        print(json.dumps(j, indent=4))

        assert response.status_code == requests.codes.ok
        assert j['inserted'] + j['updated'] == len(ORDERS)

    def test_get_order_ids(self, system_env_with_db):
        global ORDERS_ID_DICT
        for order in ORDERS:
            response = env_get_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, 'orders') + '?number={}'.format(order['number']))
            j = response.json()
            print(json.dumps(j, indent=4))

            assert response.status_code == requests.codes.ok
            assert len(j) == 1
            assert entity_equal(j[0], order)
            order['id'] = j[0]['id']

        ORDERS_ID_DICT = {order['id']: order for order in ORDERS}

    #
    # Stability test
    #

    @pytest.mark.skip(reason='Test it too unstable')
    def test_stability(self, system_env_with_db):
        next_orders = None
        failed = False

        for i in range(NUMBER_OF_ROUTES):
            j = get_routed_orders(system_env_with_db)

            assert j['metrics']['dropped_locations_count'] == 0
            for order in j['route']:
                assert entity_equal(order, ORDERS_DICT[order['number']])

            state = get_route_state(system_env_with_db)

            eta = str(timedelta(seconds=int(state.get('location', {}).get('time_s_now', 0)) +
                                        int(state.get('solution', {}).get('metrics', {}).get('total_duration_s', 0))))

            print(
                '{:3d}'.format(i),
                eta,
                state['next_orders'],
                '{:3d}'.format(classic_levenshtein(next_orders, state['next_orders']) if next_orders else 0)
            )

            assert state['solution']['solver_status'] == 'SOLVED'
            assert set(state['next_orders']) == set(
                order['id']
                for order in ORDERS
                if ORDERS_ID_DICT[order['id']]['status'] in ('new', 'confirmed')
            )

            if next_orders:
                # assert next_orders == state['next_orders']
                if not failed:
                    failed = next_orders != state['next_orders']
            else:
                next_orders = state['next_orders']

        assert not failed
