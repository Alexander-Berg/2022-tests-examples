import datetime
import time

import dateutil.tz
import requests

from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str
from maps.b2bgeo.ya_courier.backend.test_lib import util
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    cleanup_route_orders,
    create_company_entity,
    entity_equal,
    env_delete_request,
    env_get_request,
    env_post_request,
    get_location_nearby,
    get_request,
    get_route_details,
    patch_order,
    set_partially_finished_status_enabled,
)

COURIER = {
    'number': 'routing_logic_courier_num',
    'phone': '+7999333666',
    'name': 'Danil',
    'sms_enabled': False
}

ROUTABLE_STATII = ('new', 'confirmed')

PHONE_NUMBER = '+79161111111'

COURIER_ID = None

DEPOT_TIME_ZONE = dateutil.tz.gettz('Europe/Moscow')

# route_start needed to accept all positions for route day - 'route_start': '00:00:00'
ROUTE = {
    'number': 'TEST_ROUTE',
    'courier_number': COURIER['number'],
    'depot_number': '23453',
    'date': datetime.datetime.now(tz=DEPOT_TIME_ZONE).date().isoformat(),
    'route_start': '00:00:00'
}

ORDERS = [
    {
        'number': 'TEST_ROUTE_1',
        'route_number': 'TEST_ROUTE',
        'address': 'МКАД, 107 км',
        'phone': PHONE_NUMBER,
        'time_interval': '09:00 - 1.01:00',
        'lat': 55.791928,
        'lon': 37.841492,
        'status': 'new'
    },
    {
        'number': 'TEST_ROUTE_2',
        'route_number': 'TEST_ROUTE',
        'address': 'МКАД, 87 км',
        'phone': PHONE_NUMBER,
        'time_interval': '12:00 - 1.01:00',
        'lat': 55.900931,
        'lon': 37.623044,
        'status': 'new'
    },
    {
        'number': 'TEST_ROUTE_3',
        'route_number': 'TEST_ROUTE',
        'address': 'МКАД, 70 км',
        'phone': PHONE_NUMBER,
        'time_interval': '15:00 - 1.01:00',
        'lat': 55.846988,
        'lon': 37.391617,
        'status': 'new'
    },
    {
        'number': 'TEST_ROUTE_4',
        'route_number': 'TEST_ROUTE',
        'address': 'МКАД, 46 км',
        'phone': PHONE_NUMBER,
        'time_interval': '18:00 - 1.01:00',
        'lat': 55.649909,
        'lon': 37.446456,
        'status': 'new'
    },
    {
        'number': 'TEST_ROUTE_5',
        'route_number': 'TEST_ROUTE',
        'address': 'МКАД, 29 км',
        'phone': PHONE_NUMBER,
        'time_interval': '21:00 - 22:00',
        'lat': 55.572369,
        'lon': 37.656781,
        'status': 'new'
    },
    {
        'number': 'TEST_ROUTE_6',
        'route_number': 'TEST_ROUTE',
        'address': 'МКАД, 17 км',
        'phone': PHONE_NUMBER,
        'time_interval': '23:30 - 1.01:00',
        'lat': 55.633290,
        'lon': 37.808849,
        'status': 'new'
    },
]

ORDERS_DICT = {order['number']: order for order in ORDERS}
ORDERS_ID_DICT = {}

DEPOT = {
    'number': '23453',
    'name': 'Склад 2',
    'address': 'ул. Льва Толстого, 18',
    'time_interval': '0.00:00-23:59',
    'lat': 55.7446,
    'lon': 37.6727,
    'description': 'курьерский вход',
    'service_duration_s': 500,
    'order_service_duration_s': 9
}

DEPOT_ID = None

LOCATION = {
    'lat': 55.754097,
    'lon': 37.731182,
    'time_now': '8:00'
}

SOLUTION_ID = None

SMS_COUNT_BEFORE = 0
TRACKING_TOKEN = None


def get_routed_orders(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}'.format(
            COURIER_ID, ROUTE['id'],
            LOCATION['lat'], LOCATION['lon'],
            LOCATION['time_now'],
        )
    )
    assert response.status_code == requests.codes.ok

    j = response.json()

    assert 'solution_id' in j
    global SOLUTION_ID
    SOLUTION_ID = j['solution_id']
    assert 'metrics' in j
    assert 'route' in j

    return j


def get_route_state(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(
            system_env_with_db,
            "route-state?courier_number={}&date={}".format(
                ROUTE['courier_number'],
                ROUTE['date']
            )
        )
    )
    assert response.status_code == requests.codes.ok

    state = response.json()

    assert 'location' in state
    assert 'solution' in state
    assert 'fixed_orders' in state
    assert 'routed_orders' in state
    assert entity_equal(state['location'], LOCATION)
    assert SOLUTION_ID == state['solution']['id']

    return state


class TestRoutingLogic:
    #
    # Initialization
    #

    def test_clean_route(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "routes?number={}".format(ROUTE['number'])
            )
        )
        assert response.status_code == requests.codes.ok

        j = response.json()

        if len(j) == 0:
            return

        assert len(j) == 1
        route_id = j[0]['id']
        cleanup_route_orders(route_id, system_env_with_db)
        response = env_delete_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "routes/{}".format(route_id)
            )
        )
        assert response.status_code == requests.codes.ok

    def test_setup(self, system_env_with_db):
        create_company_entity(system_env_with_db, "couriers", COURIER)
        create_company_entity(system_env_with_db, "depots", DEPOT)

    def test_get_depot_id(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "depots?number={}".format(DEPOT['number'])
            )
        )

        j = response.json()
        assert len(j) == 1
        global DEPOT_ID
        DEPOT_ID = j[0]["id"]

    def test_routes_batch(self, system_env_with_db):
        create_company_entity(
            system_env_with_db,
            "routes",
            ROUTE
        )

    def test_get_route_id(self, system_env_with_db):
        global ROUTE, COURIER_ID
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "routes?number={}".format(ROUTE['number'])
            )
        )
        assert response.status_code == requests.codes.ok

        j = response.json()

        assert len(j) == 1
        assert entity_equal(j[0], ROUTE)
        ROUTE['id'] = j[0]['id']
        COURIER_ID = j[0]['courier_id']

    def test_orders_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "orders-batch"
            ),
            data=ORDERS
        )
        assert response.status_code == requests.codes.ok

        j = response.json()

        assert j['inserted'] + j['updated'] == len(ORDERS)

    def test_route_tracking(self, system_env_with_db):
        for push_positions_version, position_time in [
                (1, datetime.datetime.now(tz=datetime.timezone.utc).isoformat()),
                (1, time.time()),
                (2, time.time())]:
            util.push_positions(
                system_env_with_db,
                COURIER_ID,
                ROUTE['id'],
                track=[(LOCATION['lat'], LOCATION['lon'], position_time)],
                version=push_positions_version)

    def test_route_tracking_missing_accuracy(self, system_env_with_db):
        assert env_post_request(
            system_env_with_db,
            "couriers/{}/routes/{}/push-positions".format(
                COURIER_ID, ROUTE['id']
            ),
            data={
                'positions': [{
                    'latitude': LOCATION['lat'],
                    'longitude': LOCATION['lon'],
                    'time': int(time.time()*1000)
                }]
            }
        ).status_code == requests.codes.unprocessable

        assert env_post_request(
            system_env_with_db,
            "couriers/{}/routes/{}/push-positions-v2".format(
                COURIER_ID, ROUTE['id']
            ),
            data={
                'positions': [
                    {
                        'timestampMeta': {
                            'systemTime': int(time.time()*1000)
                        },
                        'coords': {
                            'latitude': LOCATION['lat'],
                            'longitude': LOCATION['lon']
                        }
                    }
                ]
            }
        ).status_code == requests.codes.unprocessable

    def test_route_tracking_create_token(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            "couriers/{}/routes/{}/create-track".format(
                COURIER_ID, ROUTE['id']
            ),
            data=None
        )
        assert response.status_code == requests.codes.ok

        j = response.json()

        assert 'track_id' in j
        assert 'url' in j
        assert 'tinyurl' in j
        assert j['url'] == j['tinyurl']

        response = get_request(j['tinyurl'], headers=[])
        assert response.status_code == requests.codes.ok

    def test_get_order_ids(self, system_env_with_db):
        for order in ORDERS:
            response = env_get_request(
                system_env_with_db,
                api_path_with_company_id(
                    system_env_with_db,
                    "orders?number={}".format(order['number'])
                )
            )
            assert response.status_code == requests.codes.ok

            j = response.json()

            assert len(j) == 1
            assert entity_equal(j[0], order)
            order['id'] = j[0]['id']

        global ORDERS_ID_DICT
        ORDERS_ID_DICT = {order['id']: order for order in ORDERS}

    #
    # State 0. All new.
    #
    def test_routed_orders_0(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])
            assert 'arrival_time_s' in order
            assert 'route_sequence_pos' not in order
            assert ORDERS[i]['id'] == order['id']

    def test_route_state_0(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert state['solution']['solver_status'] == 'SOLVED'
        assert state['next_order']['id'] == ORDERS[0]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )
        assert not state['fixed_orders']
        for i, order in enumerate(state['routed_orders']):
            assert ORDERS[i]['id'] == order['id']

    def test_route_state_fail(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "route-state?courier_number={}".format(
                    ROUTE['courier_number']
                )
            )
        )
        assert response.status_code == requests.codes.unprocessable

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "route-state?date={}".format(
                    ROUTE['date']
                )
            )
        )
        assert response.status_code == requests.codes.unprocessable

    def test_route_state_fail_2(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "route-state?courier_number={}&date=16.06.2020".format(
                    ROUTE['courier_number']
                )
            )
        )
        assert response.status_code == requests.codes.unprocessable

    #
    # State 1. Confirmed order with fixation
    #

    def test_order1_patch_confirmed(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[0], {'status': 'confirmed'})

    def test_order6_patch_as_closest_one(self, system_env_with_db):
        patch_order(
            system_env_with_db,
            ORDERS[5],
            {
                'lat': LOCATION['lat'],
                'lon': LOCATION['lon'],
                'time_interval': '08:00 - 09:00'
            }
        )

    def test_routed_orders_1(self, system_env_with_db):
        global LOCATION
        LOCATION = get_location_nearby(ORDERS[0], '09:01')
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])

            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order

    def test_route_state_1(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert 'dropped_locations' in state['solution']

        assert state['solution']['solver_status'] == 'SOLVED'
        assert state['next_order']['id'] == ORDERS[0]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )

    def test_get_tracking_token(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "orders/{}/track-ids".format(ORDERS[0]['id'])
            )
        )
        assert response.status_code == requests.codes.ok

        global TRACKING_TOKEN
        TRACKING_TOKEN = response.json()[-1]

    def test_track(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            "tracking/{}/track".format(TRACKING_TOKEN)
        )
        assert response.status_code == requests.codes.ok

        j = response.json()

        assert 'raw_positions' in j
        assert j['raw_positions']
        assert 'positions' in j
        if j['positions']:
            assert 'eta_iso' in j

    def test_tracking(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            "tracking/{}".format(TRACKING_TOKEN)
        )
        assert response.status_code == requests.codes.ok

        j = response.json()

        assert 'order' in j
        assert 'status_log' in j['order']
        assert len(j['order']['status_log']) > 0
        assert 'orders_before' in j
        assert isinstance(j['orders_before'], int)

    def test_order6_revert_changes(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[5], {
            'time_interval': '23:30 - 23:59',
            'lat': 55.633290,
            'lon': 37.808849,
            'status': 'new'
        })

    #
    # State 2. Confirmed order 2
    #

    def test_order2_patch_confirmed(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[1], {'status': 'confirmed'})

    def test_routed_orders_2(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])

            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order

    def test_route_state_2(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert 'dropped_locations' in state['solution']

        assert state['solution']['solver_status'] == 'SOLVED'
        assert state['next_order']['id'] == ORDERS[0]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )

    #
    # State 3. Failed time window.
    #

    def test_order6_patch_time_interval(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[5], {'time_interval': '01:00 - 02:00'})

    def test_routed_orders_3(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order

            if order['id'] == ORDERS[5]['id']:
                assert 'failed_time_window' in order
                assert order['failed_time_window']
            else:
                assert entity_equal(order, ORDERS_DICT[order['number']])

    def test_route_state_3(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert state['solution']['solver_status'] == 'SOLVED'
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )

    def test_route_details_3(self, system_env_with_db):
        j = get_route_details(system_env_with_db, ROUTE['date'], ROUTE['courier_number'])
        assert len(j) > 0
        route_details = j[0]
        assert "route_state" in route_details
        state = route_details["route_state"]
        assert "routed_orders" in state
        assert len(state['routed_orders']) > 0
        routed_order = state['routed_orders'][0]

        expected_fields = [
            "id", "status", "time_interval", "order_number",
            "route_id", "route_number", "route_date", "courier_id",
            "courier_number", "depot_id", "depot_number", "phone",
            "address", "lat", "lon", "delivered_at", "confirmed_at",
            "customer_name", "comments", "description", "courier_name",
            "service_duration_s"
            ]
        for field in expected_fields:
            assert field in routed_order

    def test_route_details_fail(self, system_env_with_db):
        response = env_get_request(system_env_with_db,
                                   api_path_with_company_id(system_env_with_db, "route-details"))
        assert response.status_code == requests.codes.unprocessable
        assert "Missing data for required field" in response.content.decode()

        response = env_get_request(system_env_with_db,
                                   api_path_with_company_id(system_env_with_db, "route-details?date=24.06.2020"))
        assert response.status_code == requests.codes.unprocessable
        assert "Not a valid date " in response.content.decode()

    def test_verification_3(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "verification?date={}".format(
                    ROUTE['date']
                ),
            )
        )
        assert response.status_code == requests.codes.ok

        orders = response.json()
        assert isinstance(orders, list)
        assert len(orders) > 0

        order = next(
            order
            for order in orders
            if order["order_number"] == ORDERS[1]["number"])

        expected_fields = [
            "order_id", "status", "time_interval", "order_number",
            "route_id", "route_number", "route_date", "courier_id",
            "courier_number", "depot_id", "depot_number", "phone",
            "address", "lat", "lon", "delivered_at", "confirmed_at",
            "customer_name", "comments", "description", "courier_name",
            "service_duration_s"]

        for field in expected_fields:
            assert field in order

        assert "status_log" in order
        assert order["status_log"]

    def test_verification_with_depot_3(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "verification?date={}&depot_id={}".format(
                    ROUTE['date'],
                    DEPOT_ID
                )
            )
        )
        assert response.status_code == requests.codes.ok

        orders = response.json()
        assert isinstance(orders, list)
        assert len(orders) > 0

        order = next(
            order
            for order in orders
            if order["order_number"] == ORDERS[5]["number"])

        expected_fields = [
            "order_id", "status", "time_interval", "order_number",
            "route_id", "route_number", "route_date", "courier_id",
            "courier_number", "depot_id", "depot_number", "phone",
            "address", "lat", "lon", "delivered_at", "confirmed_at",
            "customer_name", "comments", "description", "courier_name",
            "service_duration_s"]

        for field in expected_fields:
            assert field in order

    def test_verification_no_date(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "verification")
        )
        assert response.status_code == requests.codes.unprocessable
        assert 'Required parameter missing' in response.content.decode()

    def test_verification_invalid_date(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "verification?date=16.06.2020"
            )
        )
        assert response.status_code == requests.codes.unprocessable
        assert "Not a valid date " in response.content.decode()

    def test_route_states_fail(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                'route-states?courier_number={}'.format(
                    ROUTE['courier_number']
                ),
            )
        )
        assert response.status_code == requests.codes.unprocessable
        assert 'Missing data for required field' in response.content.decode()

    def test_route_states_fail_2(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                'route-states?courier_number={}&date=16.06.2020'.format(
                    ROUTE['courier_number']
                ),
            )
        )
        assert response.status_code == requests.codes.unprocessable

    def test_route_states_3(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                'route-states?courier_number={}&date={}'.format(
                    ROUTE['courier_number'],
                    ROUTE['date']
                ),
            )
        )
        assert response.status_code == requests.codes.ok

        j = response.json()
        assert isinstance(j, list)
        assert len(j) > 0
        route_details = j[0]
        assert "route_state" in route_details
        state = route_details["route_state"]
        assert "routed_orders" in state
        assert len(state['routed_orders']) > 0
        routed_order = state['routed_orders'][0]

        expected_fields = [
            "id", "status", "time_interval"]
        for field in expected_fields:
            assert field in routed_order

    #
    # State 4. Dropped order - note that after BBGEO-2000 all orders are fixed,
    # and since we do not allow dropping fixed(!) orders we should have no drops.
    # But solution violates hard time window so it is unfeasible.
    #

    def test_order6_patch_confirmed(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[5], {'status': 'confirmed', 'time_interval': '01:00 - 02:01'})

    def test_routed_orders_4(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order

    def test_route_state_4(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert state['solution']['solver_status'] == 'UNFEASIBLE'
        assert state['next_order']['id'] == ORDERS[0]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )

    #
    # State 5. Postponed order
    # There is still location with failed hard time window.
    #

    def test_order5_patch_postponed(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[4], {'status': 'postponed'})

    def test_routed_orders_5(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order
        routed_orders_ids = [order['id'] for order in j['route']]
        assert ORDERS[4]['id'] == routed_orders_ids[0]

    def test_route_state_5(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert 'dropped_locations' in state['solution']
        assert 'cancelled_orders' in state

        assert state['solution']['solver_status'] == 'UNFEASIBLE'
        assert state['next_order']['id'] == ORDERS[0]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        ) - {ORDERS[4]['id']}

    def test_order5_patch_new(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[4], {'status': 'new'})

    #
    # State 6. Cancelled order
    # Since order is cancelled solution again become feasible.
    #

    def test_order6_patch_cancelled(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[5], {'status': 'cancelled'})

    def test_routed_orders_6(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order
        assert ORDERS[5]['id'] in [order['id'] for order in j['route']]

    def test_route_state_6(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert 'dropped_locations' in state['solution']
        assert 'cancelled_orders' in state

        assert state['solution']['solver_status'] == 'SOLVED'
        assert state['next_order']['id'] == ORDERS[0]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )

    #
    # State 7. Finished order 1
    #

    def test_order1_patch_finished(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[0], {'status': 'finished'})

    def test_finished_track(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            "tracking/{}/track".format(TRACKING_TOKEN)
        )
        assert response.status_code == requests.codes.gone

    def test_finished_tracking(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            "tracking/{}".format(TRACKING_TOKEN)
        )
        assert response.status_code == requests.codes.ok

    def test_routed_orders_7(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])

            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order

            if ORDERS_ID_DICT[order['id']]['status'] == 'finished':
                assert 'delivered_at' in order

    def test_route_state_7(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert 'dropped_locations' in state['solution']
        assert 'cancelled_orders' in state
        assert 'finished_orders' in state

        assert ORDERS[0]['id'] not in state.get('tracking_tokens', {})

        assert state['solution']['solver_status'] == 'SOLVED'
        assert state['next_order']['id'] == ORDERS[1]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )
        assert ORDERS[0]['id'] in state['finished_orders']
        assert len(state['finished_orders']) == 1

    #
    # State 8. Confirmed order 3 and fixation within 2 hour ETA
    #

    def test_order3_patch_lat_lon(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[2], {
            'lat': 55.904372,
            'lon': 37.608710,
            'time_interval': '12:30 - 13:30'
        })

    def test_order3_patch_confirmed_and_lat_lon(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[2], {
            'status': 'confirmed',
        })

    def test_routed_orders_8(self, system_env_with_db):
        global LOCATION
        LOCATION = {
            'lat': 55.897886,
            'lon': 37.640811,
            'time_now': '12:00'
        }

        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])

            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order

    # def test_order_prefix_8_check_cleaning(self, system_env_with_db):
    #     state = get_route_state(system_env_with_db)

    def test_route_state_8(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert 'dropped_locations' in state['solution']
        assert 'cancelled_orders' in state
        assert state['solution']['solver_status'] == 'SOLVED'
        assert state['next_order']['id'] == ORDERS[1]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )

    #
    # State 9. Confirmed and finished
    #

    def test_order2_3_finished(self, system_env_with_db):
        set_partially_finished_status_enabled(system_env_with_db, True)
        patch_order(system_env_with_db, ORDERS[1], {'status': 'partially_finished'})
        patch_order(system_env_with_db, ORDERS[2], {'status': 'finished'})
        get_routed_orders(system_env_with_db)

    def test_order4_confirmed_and_finished(self, system_env_with_db):
        patch_order(system_env_with_db, ORDERS[3], {'status': 'confirmed'})
        patch_order(system_env_with_db, ORDERS[3], {'status': 'confirmed'})
        patch_order(system_env_with_db, ORDERS[3], {'status': 'confirmed', 'time_interval': '20:00-21:00'})
        patch_order(system_env_with_db, ORDERS[3], {'status': 'confirmed', 'time_interval': '20:00 - 21:00'})
        patch_order(system_env_with_db, ORDERS[3], {'status': 'finished'})

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "orders?route_id={}".format(ROUTE['id'])
            )
        )
        j = response.json()
        order = next(
            order
            for order in j
            if order["number"] == ORDERS[3]["number"])

        assert "status_log" in order
        assert len(order["status_log"]) == 2
        assert order['status_log'][0]['status'] == 'confirmed'
        assert order['status_log'][1]['status'] == 'finished'

        assert "history" in order
        assert len(order["history"]) == 6
        for item in order["history"]:
            assert 'timestamp' in item
            assert 'time' in item
            assert item['time'] == get_isoformat_str(item['timestamp'], DEPOT_TIME_ZONE)

        assert order['history'][0]['event'] == 'ORDER_CREATED'
        assert order['history'][1]['event'] == 'START'
        assert order['history'][2]['event'] == 'ORDER_BECAME_NEXT'
        assert order['history'][3]['event'] == 'STATUS_UPDATE'
        assert order['history'][3]['status'] == 'confirmed'

        assert order['history'][4]['event'] == 'INTERVAL_UPDATE'
        assert order['history'][4]['time_interval'] == '20:00 - 21:00'

        assert order['history'][5]['event'] == 'STATUS_UPDATE'
        assert order['history'][5]['status'] == 'finished'

    def test_routed_orders_9(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])

            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order

    def test_route_state_9(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert 'dropped_locations' in state['solution']
        assert 'cancelled_orders' in state
        assert state['solution']['solver_status'] == 'SOLVED'
        assert state['next_order']['id'] == ORDERS[4]['id']
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )
        assert len(state['fixed_orders']) == 0

    #
    # State 10. After midnight
    #

    def test_routed_orders_10(self, system_env_with_db):
        global LOCATION
        LOCATION = {
            'lat': 55.633290,
            'lon': 37.808849,
            'time_now': '1.00:10'
        }

        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII:
                assert 'arrival_time_s' in order

            if order['id'] == ORDERS[4]['id']:
                assert 'failed_time_window' in order
                assert order['failed_time_window']
            else:
                assert entity_equal(order, ORDERS_DICT[order['number']])

    def test_route_state_10(self, system_env_with_db):
        state = get_route_state(system_env_with_db)

        assert 'next_order' in state
        assert 'next_orders' in state
        assert state['solution']['solver_status'] == 'SOLVED'
        assert set(state['next_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ROUTABLE_STATII
        )

    #
    # State 11. All finished
    #

    def test_all_finished(self, system_env_with_db):
        order_changes = {
            'status': 'finished'
        }

        for order in ORDERS:
            if order['status'] in ROUTABLE_STATII:
                patch_order(system_env_with_db, order, order_changes)

    def test_routed_orders_11(self, system_env_with_db):
        j = get_routed_orders(system_env_with_db)

        assert j['metrics']['dropped_locations_count'] == 0
        for i, order in enumerate(j['route']):
            assert entity_equal(order, ORDERS_DICT[order['number']])
            assert ORDERS_ID_DICT[order['id']]['status'] not in ROUTABLE_STATII
            assert 'delivered_at' in order

    def test_route_state_11(self, system_env_with_db):
        state = get_route_state(system_env_with_db, )

        assert 'dropped_locations' in state['solution']
        assert 'cancelled_orders' in state
        assert state['solution']['solver_status'] == 'SOLVED'
        assert 'next_order' not in state
        assert 'next_orders' not in state
        assert set(state['finished_orders']) == set(
            order['id']
            for order in ORDERS
            if ORDERS_ID_DICT[order['id']]['status'] in ['finished', 'partially_finished']
        )
        assert not state['fixed_orders']
