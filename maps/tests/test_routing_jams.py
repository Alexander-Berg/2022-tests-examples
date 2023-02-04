from datetime import timedelta, datetime

import pytest
import requests

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    entity_equal,
    env_get_request, env_post_request,
    api_path_with_company_id,
    )

COURIER_ID = None

TEST_COURIER = {
    "name": "Flash",
    "number": "TEST_ROUTE_JAMS_COURIER",
    "sms_enabled": False
}

TEST_DEPOT = {
    'number': 'TEST_ROUTE_JAMS_DEPOT',
    'name': 'Склад 1',
    'address': 'ул. Льва Толстого, 16',
    'time_interval': '0.00:00-23:59',
    'lat': 55.7447,
    'lon': 37.6727,
    'description': 'курьерский подъезд',
    'service_duration_s': 600,
    'order_service_duration_s': 10
}

ROUTE = {
    'number': 'TEST_ROUTE_JAMS',
    'courier_number': 'TEST_ROUTE_JAMS_COURIER',
    'depot_number': 'TEST_ROUTE_JAMS_DEPOT',
    'date': '2018-01-12'
}

ORDERS = [
    {
        'number': 'TEST_ROUTE_JAMS_1',
        'address': 'Ленинская слобода, 4',
        'lat': 55.712394,
        'lon': 37.662021,
    },
    {
        'number': 'TEST_ROUTE_JAMS_2',
        'address': 'Автозаводская улица, 17к1',
        'lat': 55.704422,
        'lon': 37.654146,
    },
    {
        'number': 'TEST_ROUTE_JAMS_3',
        'address': 'улица Трофимова, 4',
        'lat': 55.703137,
        'lon': 37.662665,
    },
    {
        'number': 'TEST_ROUTE_JAMS_4',
        'address': 'улица Ленинская Слобода, 30с1',
        'lat': 55.705440,
        'lon': 37.647495,
    },
    {
        'number': 'TEST_ROUTE_JAMS_5',
        'address': 'Велозаводская улица, 7',
        'lat': 55.712999,
        'lon': 37.666528,
    },
    {
        'number': 'TEST_ROUTE_JAMS_6',
        'address': 'Автозаводская улица, 6',
        'lat': 55.709086,
        'lon': 37.660670,
    },
    {
        'number': 'TEST_ROUTE_JAMS_7',
        'address': 'Сокольники',
        'lat': 55.791521,
        'lon': 37.661366,
    },
    {
        'number': 'TEST_ROUTE_JAMS_8',
        'address': 'Хамовники',
        'lat': 55.726377,
        'lon': 37.567124,
    },
    {
        'number': 'TEST_ROUTE_JAMS_9',
        'address': 'Тверской район',
        'lat': 55.756107,
        'lon': 37.627893,
    },
    {
        'number': 'TEST_ROUTE_JAMS_10',
        'address': 'Таганский район',
        'lat': 55.742940,
        'lon': 37.672010,
    },
    {
        'number': 'TEST_ROUTE_JAMS_11',
        'address': 'Якиманка',
        'lat': 55.734127,
        'lon': 37.614846,
    },
    {
        'number': 'TEST_ROUTE_JAMS_12',
        'address': 'МКАД, 107 км',
        'lat': 55.791928,
        'lon': 37.841492,
    },
    {
        'number': 'TEST_ROUTE_JAMS_13',
        'address': 'МКАД, 87 км',
        'lat': 55.900931,
        'lon': 37.623044,
    },
    {
        'number': 'TEST_ROUTE_JAMS_14',
        'address': 'МКАД, 70 км',
        'lat': 55.846988,
        'lon': 37.391617,
    },
    {
        'number': 'TEST_ROUTE_JAMS_15',
        'address': 'МКАД, 46 км',
        'lat': 55.649909,
        'lon': 37.446456,
    },
    {
        'number': 'TEST_ROUTE_JAMS_16',
        'address': 'МКАД, 29 км',
        'lat': 55.572369,
        'lon': 37.656781,
    },
    {
        'number': 'TEST_ROUTE_JAMS_17',
        'address': 'МКАД, 17 км',
        'lat': 55.633290,
        'lon': 37.808849,
    },
]

for order in ORDERS:
    order.update({
        'route_number': 'TEST_ROUTE_JAMS',
        'time_interval': '00:00 - 23:59',
        'status': 'new',
        'phone': '+79161111111',
    })

ORDERS_DICT = {order['number']: order for order in ORDERS}
ORDERS_ID_DICT = {}

LOCATION = {
    'lat': 55.811264,
    'lon': 37.503887,
}

SOLUTION_ID = None

# {weekday: [hour0, hour1, ... hour23]} in seconds
REFERENCE = {
    0: [22748, 22687, 22876, 24333, 27577, 31128, 30501, 30966, 31981, 32624, 32434, 32966, 32722, 31884, 31551, 30937,
        30951, 30178, 29895, 29522, 28396, 28140, 28194, 28269],
    1: [23513, 24231, 23896, 25620, 29362, 30897, 30750, 32328, 33061, 32532, 32808, 34199, 33335, 32836, 32918, 32650,
        32186, 31256, 31060, 30451, 30011, 29495, 29495, 29495],
    2: [23786, 23533, 23862, 24618, 25890, 29392, 30885, 31587, 32574, 33545, 34450, 35106, 34991, 34562, 34659, 34492,
        34243, 32345, 31957, 31412, 30957, 30393, 30393, 30393],
    3: [24227, 23903, 24249, 26866, 27869, 29528, 31029, 31924, 32463, 33641, 34626, 35061, 34541, 34195, 34662, 34182,
        34008, 33032, 32629, 31732, 31014, 30451, 30451, 30451],
    4: [24288, 23937, 24245, 24729, 27693, 29095, 29969, 31036, 32566, 33351, 34226, 34913, 34481, 34178, 34515, 34359,
        34180, 32623, 31797, 31140, 30496, 29812, 29812, 29812],
    5: [24662, 24508, 24713, 24603, 24461, 24416, 24377, 24377, 24377, 24377, 24377, 24377, 24377, 24377, 24565, 24767,
        25251, 25666, 25579, 27067, 27499, 28023, 28023, 28023],
    6: [23505, 23524, 23555, 23892, 23859, 23874, 23860, 23860, 23860, 23860, 23860, 23860, 23860, 23860, 23959, 24085,
        24223, 24264, 24862, 25248, 25441, 25591, 25591, 25591],
}


def get_routed_orders(hour_of_day, system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}:00'.format(
            COURIER_ID, ROUTE['id'], LOCATION['lat'], LOCATION['lon'] + hour_of_day * 10 ** -6, hour_of_day
        )
    )

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
        '{}/route-state?courier_number={}&date={}'.format(
            api_path_with_company_id(system_env_with_db),
            ROUTE['courier_number'],
            ROUTE['date']))

    if response.status_code != requests.codes.ok:
        print(response.text)

    assert response.status_code == requests.codes.ok

    j = response.json()
    state = j

    assert 'location' in state
    assert 'solution' in state
    assert 'fixed_orders' in state
    assert 'routed_orders' in state
    assert SOLUTION_ID == state['solution']['id']

    return state


def format_time(seconds):
    return str(timedelta(seconds=int(seconds)))


def mse(x, y):
    assert len(x)
    assert len(x) == len(y)
    return sum([(x[i] - y[i]) ** 2 for i in range(len(x))]) / len(x)


@pytest.mark.skip(reason="Depends on current router state, too unstable")
class TestRoutingJams(object):
    #
    # Initialization
    #
    def test_prepare(self, system_env_with_db):
        if system_env_with_db.existing:
            return
        response = env_post_request(
            system_env_with_db,
            '{}/couriers'.format(api_path_with_company_id(system_env_with_db)),
            data=TEST_COURIER)
        assert response.ok, response.text

        response = env_post_request(
            system_env_with_db,
            '{}/depots'.format(api_path_with_company_id(system_env_with_db)),
            data=TEST_DEPOT)
        assert response.ok, response.text

    def test_routes_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            '{}/routes-batch'.format(api_path_with_company_id(system_env_with_db)),
            data=[ROUTE, ])
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert j['inserted'] + j['updated'] == 1

    def test_get_route_id(self, system_env_with_db):
        global ROUTE, COURIER_ID
        response = env_get_request(
            system_env_with_db,
            '{}/routes?number={}'.format(
                api_path_with_company_id(system_env_with_db), ROUTE['number']))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert len(j) == 1
        assert entity_equal(j[0], ROUTE)
        ROUTE['id'] = j[0]['id']
        COURIER_ID = j[0]['courier_id']

    def test_orders_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            '{}/orders-batch'.format(api_path_with_company_id(system_env_with_db)),
            data=ORDERS)
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert j['inserted'] + j['updated'] == len(ORDERS)

    def test_get_order_ids(self, system_env_with_db):
        global ORDERS_ID_DICT
        for o in ORDERS:
            response = env_get_request(
                system_env_with_db,
                '{}/orders?number={}'.format(api_path_with_company_id(system_env_with_db), o['number']))
            j = response.json()
            print(j)

            assert response.status_code == requests.codes.ok
            assert len(j) == 1
            assert entity_equal(j[0], o)
            o['id'] = j[0]['id']

        ORDERS_ID_DICT = {o['id']: o for o in ORDERS}

    #
    # Jams test
    #

    def test_jams(self, system_env_with_db):
        weekday = datetime.today().weekday()

        eta = [0] * len(REFERENCE[weekday])

        print('hour\tETA\treference\tdiff\tsolution_id')

        for i in range(len(REFERENCE[weekday])):
            j = get_routed_orders(i, system_env_with_db)

            assert j['metrics']['dropped_locations_count'] == 0
            for o in j['route']:
                assert entity_equal(o, ORDERS_DICT[o['number']])

            state = get_route_state(system_env_with_db)
            eta[i] = state.get('solution', {}).get('metrics', {}).get('total_duration_s', 0)

            print(
                '{:3d}\t{}\t{}\t{}\t{}'.format(
                    i,
                    format_time(eta[i]),
                    format_time(REFERENCE[weekday][i]),
                    ('-' if eta[i] - REFERENCE[weekday][i] < 0 else ' ')
                    + format_time(abs(eta[i] - REFERENCE[weekday][i])),
                    state.get('solution', {}).get('id')
                )
            )

            assert state['solution']['solver_status'] == 'SOLVED'
            assert set(state['next_orders']) == set(
                o['id']
                for o in ORDERS
                if ORDERS_ID_DICT[o['id']]['status'] in ('new', 'confirmed')
            )

        print('MSE: {:,.0f}'.format(mse(eta, REFERENCE[weekday])).replace(',', ' '))

        # MSE(no jams, jams) ~= 11 * 10 ** 6 (fail)
        # MSE(jams1, jams2) ~= 0.7 * 10 ** 6 (OK)
        assert mse(eta, REFERENCE[weekday]) < 4 * 10 ** 6
