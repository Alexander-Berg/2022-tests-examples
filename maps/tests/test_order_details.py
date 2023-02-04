import datetime
import dateutil.tz
import dateutil.parser
import pytest
import requests
import time

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request, api_path_with_company_id,
    patch_order,
    set_partially_finished_status_enabled,
    create_route_env, create_route_envs, query_routed_orders,
    get_order_details,
    push_positions)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.models import OrderStatus, Order
from maps.b2bgeo.libs.py_flask_utils.format_util import parse_interval_sec
from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str
from ya_courier_backend.util.order_time_windows import convert_time_offset_to_date


TEST_PARAMS = {
    'points': [
        {"lat": 55.663878, "lon": 37.482458},
        {"lat": 55.683761, "lon": 37.518000},
        {"lat": 55.705491, "lon": 37.551859},
    ],
    'route_dates': [
        datetime.date(2018, 10, 7).isoformat(),
        datetime.date(2018, 10, 8).isoformat(),
        datetime.date(2018, 10, 8).isoformat(),
        datetime.date(2018, 10, 8).isoformat()
    ],
    'time_intervals': [
        ["07:00-12:00", "15:00-16:00", "22:00-1.02:00"],
        ["10:00-12:00", "14:00-16:00", "18:00-20:00"],
        ["1.10:00-1.12:00", "1.14:00-1.16:00", "1.18:00-1.20:00"],
        ["2.10:00-2.12:00", "2.14:00-2.16:00", "1.18:00-2.20:00"]
    ],
}


ORDER_DETAILS_REQUIRED_FIELDS = {
    'address',
    'amount',
    'comments',
    'confirmed_at',
    'confirmed_at_time',
    'courier_id',
    'courier_name',
    'courier_number',
    'customer_name',
    'delivered_at',
    'delivered_at_time',
    'depot_id',
    'depot_number',
    'description',
    'history',
    'lat',
    'lon',
    'mark_delivered_radius',
    'order_id',
    'order_number',
    'order_status_comments',
    'payment_type',
    'phone',
    'route_date',
    'route_id',
    'route_number',
    'service_duration_s',
    'shared_service_duration_s',
    'shared_with_companies',
    'shared_with_company_id',
    'status',
    'time_interval',
    'time_window',
    'visited_at_time',
    'weight',
    'refined_lat',
    'refined_lon',
    'timezone'
}

ORDER_DETAILS_OPTIONAL_FIELDS = {
    'arrival_time',
    'arrival_time_s',
    'waiting_duration_s',
    'failed_time_window'
}


def _get_order_details(system_env_with_db, date, depot_id):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(
            system_env_with_db,
            "order-details?date={}&depot_id={}".format(
                date.isoformat(),
                depot_id
            )
        )
    )
    assert response.status_code == requests.codes.ok
    return response.json()


class OrderDetailsParameters:
    """ Parameters for order details testing """

    def __init__(self, request_date, expected_orders_count):
        self.request_date = request_date
        self.expected_orders_count = expected_orders_count

active_routes_params_list = [
    OrderDetailsParameters(datetime.date(2018, 10, 7), 3),
    OrderDetailsParameters(datetime.date(2018, 10, 8), 12),
    OrderDetailsParameters(datetime.date(2018, 10, 9), 6),
    OrderDetailsParameters(datetime.date(2018, 10, 10), 3)
]


@skip_if_remote
def test_order_details_fields(system_env_with_db):
    """
    Test the following workflow:
        - create a route with orders
        - request /order-details
            * check: response contains only fields from ORDER_DETAILS_REQUIRED_FIELDS
        - request /routed-orders for this route
            * check: response conttains only fields from ORDER_DETAILS_REQUIRED_FIELDS | ORDER_DETAILS_OPTIONAL_FIELDS
    """

    route_date = datetime.date(2019, 11, 27)
    time_intervals = ['00:00-1.23:59:59']

    depot_data = {
        'lat': 55,
        'lon': 37,
        'time_zone': "Europe/Moscow"
    }

    with create_route_env(
            system_env_with_db,
            "test_order_details_fields",
            time_intervals=time_intervals,
            route_date=route_date.isoformat(),
            depot_data=depot_data) as route_env:

        orders = _get_order_details(system_env_with_db, route_date, route_env['depot']['id'])
        assert isinstance(orders, list)
        assert len(orders) == 1
        assert set(orders[0]) == ORDER_DETAILS_REQUIRED_FIELDS

        query_routed_orders(system_env_with_db, route_env['courier']['id'], route_env['route']['id'])
        orders = _get_order_details(system_env_with_db, route_date, route_env['depot']['id'])
        assert isinstance(orders, list)
        assert len(orders) == 1
        all_fields = ORDER_DETAILS_REQUIRED_FIELDS | ORDER_DETAILS_OPTIONAL_FIELDS
        assert set(orders[0]) == all_fields


@pytest.mark.parametrize("params", active_routes_params_list)
def test_active_routes(system_env_with_db, params):
    """
    Test the following workflow:
        - orders for 4 routes on sequential days are created
        - routes has time_interval at the turn of days or first order on following days
        - get /order-details for each day
            * check: number of active orders
    """

    with create_route_envs(
            system_env_with_db,
            f'active_routes-{params.request_date.isoformat()}-{params.expected_orders_count}',
            order_locations=TEST_PARAMS['points'],
            time_intervals_list=TEST_PARAMS['time_intervals'],
            route_dates=TEST_PARAMS['route_dates'],
            reuse_depot=True) as route_envs:

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "order-details?date={}&depot_id={}".format(
                    params.request_date.isoformat(),
                    route_envs[0]['depot']['id']
                )
            )
        )
        assert response.status_code == requests.codes.ok

        orders = response.json()

        assert isinstance(orders, list)
        assert len(orders) == params.expected_orders_count
        assert 'weight' in orders[0]


class ActiveRoutesNearMidnightParameters:
    """
    Parameters for test_active_routes_near_day_change.
    Member expected_orders_count_list consists of expected orders count
    in response of /order-details for date of route and next 2 days
    """
    def __init__(self, time_zone, time_interval, expected_orders_count_list):
        self.time_zone = time_zone
        self.time_interval = time_interval
        self.expected_orders_count_list = expected_orders_count_list

active_routes_near_midnight_params_list = [
    ActiveRoutesNearMidnightParameters("Etc/GMT+10", "01:00-2:00", [1, 0, 0]),
    ActiveRoutesNearMidnightParameters("Etc/GMT+10", "22:00-23:00", [1, 0, 0]),
    ActiveRoutesNearMidnightParameters("Etc/GMT+10", "1.01:00-1.2:00", [1, 1, 0]),
    ActiveRoutesNearMidnightParameters("Etc/GMT+10", "1.22:00-1.23:00", [1, 1, 0]),
    ActiveRoutesNearMidnightParameters("Etc/GMT-10", "01:00-2:00", [1, 0, 0]),
    ActiveRoutesNearMidnightParameters("Etc/GMT-10", "22:00-23:00", [1, 0, 0]),
    ActiveRoutesNearMidnightParameters("Etc/GMT-10", "1.01:00-1.2:00", [1, 1, 0]),
    ActiveRoutesNearMidnightParameters("Etc/GMT-10", "1.22:00-1.23:00", [1, 1, 0])
]


@skip_if_remote
@pytest.mark.parametrize("params", active_routes_near_midnight_params_list)
def test_active_routes_near_midnight(system_env_with_db, params):
    """
    Test the following workflow:
        - create route env with depot in parametrized time_zone, a route, an order
        - order has end of its time interval near midnight (parametrized: the beginning of the first route day,
          the beginning of the second route day, the end of the second route day)
        - get /order-details for the start route day and the next two days
            * check: order is present in response only for relevant days
    """

    order_locations = [
        {"lat": 55.663878, "lon": 37.482458}
    ]
    route_date = datetime.date(2019, 11, 27)
    time_intervals = [params.time_interval]

    depot_data = {
        'lat': 55,
        'lon': 37,
        'time_zone': params.time_zone
    }

    with create_route_env(
            system_env_with_db,
            f"test_arnm-{params.time_interval}-{params.expected_orders_count_list}",
            order_locations=order_locations,
            time_intervals=time_intervals,
            route_date=route_date.isoformat(),
            depot_data=depot_data) as route_env:

        query_dates = [route_date + datetime.timedelta(days=n) for n in range(3)]

        for date, expected_orders_number in zip(query_dates, params.expected_orders_count_list):
            response = env_get_request(
                system_env_with_db,
                api_path_with_company_id(
                    system_env_with_db,
                    "order-details?date={}&depot_id={}".format(
                        date.isoformat(),
                        route_env['depot']['id']
                    )
                )
            )
            assert response.status_code == requests.codes.ok

            orders = response.json()
            assert isinstance(orders, list)
            assert len(orders) == expected_orders_number


def test_only_order_number(system_env_with_db):
    """
    Test the following workflow:
        - order for 1 route is created
        - get /order-details for order_number
            * check: the only order
    """

    with create_route_env(
            system_env_with_db,
            "test_only_order_number",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0]) as route_env:
        get_order_details(system_env_with_db, route_env['orders'][0]['number'])


def test_non_existing_order_number(system_env_with_db):
    """
    Test the following workflow:
        - order for 1 route is created
        - get /order-details for non-existing order_number
            * check: empty list is returned
    """

    with create_route_env(
            system_env_with_db,
            "non_existing_order_number",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0]):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "order-details?order_number=non-existing-number",
            )
        )
        assert response.status_code == requests.codes.ok, f"error {response.status_code}: {response.text}"
        j = response.json()
        assert isinstance(j, list)
        assert len(j) == 0


def test_only_depot_id(system_env_with_db):
    """
    Test the following workflow:
        - order for 1 route is created
        - get /order-details for depot_id
            * check: got error
    """

    with create_route_env(
            system_env_with_db,
            "only_depot_id",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0]) as route_env:
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "order-details?depot_id={}".format(route_env['depot']['id'])
            )
        )
        assert response.status_code == requests.codes.unprocessable
        assert response.json()['message'] == "Required parameter missing: 'date or route_id or order_number'"


def test_only_route_id(system_env_with_db):
    """
    Test the following workflow:
        - two routes with one order in each are created
        - /order-details with route_id returns the order of the requested route
        - /order-details with route_id set to an unknown ID fails with code 404
    """

    order_location = {"lat": 55.663878, "lon": 37.482458}
    time_interval = "07:00-12:00"
    route_date = datetime.date(2018, 10, 7).isoformat()

    with create_route_envs(
            system_env_with_db,
            "only_route_id",
            order_locations=[order_location],
            time_intervals_list=[[time_interval], [time_interval]],
            route_dates=[route_date, route_date],
            reuse_depot=True) as route_envs:

        assert len(route_envs) == 2

        # Known route IDs

        for route_env in route_envs:
            assert len(route_env['orders']) == 1

            response = env_get_request(
                system_env_with_db,
                api_path_with_company_id(
                    system_env_with_db,
                    "order-details?route_id={}".format(route_env['route']['id'])
                )
            )
            assert response.status_code == requests.codes.ok

            orders = response.json()
            assert len(orders) == 1
            assert orders[0]['route_id'] == route_env['route']['id']
            assert orders[0]['order_id'] == route_env['orders'][0]['id']

        # Unknown route ID

        assert env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "order-details?route_id={}".format(999999999)
            )
        ).status_code == requests.codes.not_found


def test_invalid_date_format(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(
            system_env_with_db,
            "order-details?date=not a date"
        )
    )
    assert response.status_code == requests.codes.unprocessable
    assert "Not a valid date " in response.json()['message']


def _get_route_env_orders(response, route_env):
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert len(j) > 2

    route_env_order_ids = [order['id'] for order in route_env["orders"]]
    j = [
        order
        for order in j
        if order["order_id"] in route_env_order_ids]
    assert len(j) == 3
    return j


def test_unreachable_location(system_env_with_db):
    points = TEST_PARAMS['points'].copy()
    unreachable_location = {"lat": 61.698653, "lon": 99.505405}
    points[0] = unreachable_location
    with create_route_env(
            system_env_with_db,
            "test_unreachable_location",
            order_locations=points,
            route_date=TEST_PARAMS['route_dates'][0]) as route_env:
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "order-details?date={}".format(TEST_PARAMS['route_dates'][0])
            )
        )
        j = _get_route_env_orders(response, route_env)
        assert "failed_time_window" not in j[0]
        assert "failed_time_window" not in j[1]
        assert "failed_time_window" not in j[2]

        query_routed_orders(system_env_with_db, route_env['courier']['id'], route_env['route']['id'])

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "order-details?date={}".format(
                    TEST_PARAMS['route_dates'][0]
                )
            )
        )
        j = _get_route_env_orders(response, route_env)
        assert len(j) == 3
        is_failed_or_dropped = False
        for order_detail in j:
            is_failed_or_dropped = is_failed_or_dropped or ('dropped' in order_detail and order_detail['dropped'] is True) or (
                order_detail["failed_time_window"] is not None)
        assert is_failed_or_dropped


def test_history_is_present(system_env_with_db):
    """
    Test that order history is present in order-details response.
    Push positions and check visited_at_time time.
    """

    with create_route_env(
            system_env_with_db,
            "test_history_is_present",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0]) as route_env:
        order_details = get_order_details(system_env_with_db, route_env['orders'][0]['number'])
        assert 'history' in order_details
        history = order_details['history']
        assert isinstance(history, list)
        assert len(history) > 0
        for item in history:
            assert 'event' in item
            assert 'timestamp' in item
            assert 'time' in item

        order = route_env['orders'][0]
        point = TEST_PARAMS['points'][0]
        position_time = datetime.datetime.strptime(
            TEST_PARAMS['route_dates'][0] + " 10:00:00", "%Y-%m-%d %X"
        ).timestamp()
        company_service_coefficient = 0.5
        visited_time = position_time + order['service_duration_s'] * company_service_coefficient
        positions = [
            (point['lat'], point['lon'], position_time),
            (point['lat'], point['lon'], visited_time),
        ]
        push_positions(system_env_with_db, route_env['courier']['id'], route_env['route']['id'], positions)
        order_details = get_order_details(system_env_with_db, route_env['orders'][0]['number'])
        depot_tz = dateutil.tz.gettz(route_env['depot']['time_zone'])
        assert order_details['visited_at_time'] == get_isoformat_str(visited_time, depot_tz)


def _check_time(expected_time, time_str, time_zone):
    dt = dateutil.parser.parse(time_str)
    assert dt.timestamp() == pytest.approx(expected_time, 10)
    assert dt.astimezone(time_zone).isoformat() == time_str


def _check_time_window(time_window, start_date, time_interval_str, time_zone_str):
    interval = parse_interval_sec(time_interval_str, start_date, time_zone_str)
    start_datetime = datetime.datetime(
        start_date.year, start_date.month, start_date.day, tzinfo=dateutil.tz.gettz(time_zone_str))
    for i, name in enumerate(['start', 'end']):
        assert time_window[name] == (start_datetime + datetime.timedelta(seconds=interval[i])).isoformat()


@pytest.mark.parametrize("finished_status", Order.all_finished_statuses())
def test_time_format(system_env_with_db, finished_status):
    """
    Test that confirmed_at_time, delivered_at_time, time_window are in the depot's timezone
    """

    route_date_str = TEST_PARAMS['route_dates'][0]
    route_date = dateutil.parser.parse(route_date_str).date()
    time_intervals = TEST_PARAMS['time_intervals'][0]

    with create_route_env(
            system_env_with_db,
            f'details_time_format-{finished_status}',
            order_locations=TEST_PARAMS['points'],
            time_intervals=time_intervals,
            route_date=route_date_str) as route_env:

        depot_tz_str = route_env['depot']['time_zone']
        depot_tz = dateutil.tz.gettz(depot_tz_str)

        for order_idx, order in enumerate(route_env['orders']):
            order_details = get_order_details(system_env_with_db, order['number'])
            for name in ['confirmed_at', 'confirmed_at_time', 'delivered_at', 'delivered_at_time', 'visited_at_time']:
                assert order_details[name] is None
            for name in ['arrival_time_s', 'arrival_time']:
                assert name not in order_details
            assert order_details['time_window']
            # Test that time_window is in the depot's timezone
            _check_time_window(order_details['time_window'], route_date, time_intervals[order_idx], depot_tz_str)

        order = route_env['orders'][0]

        status_change_time = time.time()
        patch_order(system_env_with_db, order, {'status': OrderStatus.confirmed.value})
        order_details = get_order_details(system_env_with_db, order['number'])
        for name in ['confirmed_at', 'confirmed_at_time']:
            assert order_details[name]
        for name in ['delivered_at', 'delivered_at_time', 'visited_at_time']:
            assert order_details[name] is None
        confirmed_at_time = order_details['confirmed_at_time']
        # Test that confirmed_at_time is in the depot's timezone
        _check_time(status_change_time, confirmed_at_time, depot_tz)

        status_change_time = time.time()
        if finished_status == OrderStatus.partially_finished:
            set_partially_finished_status_enabled(system_env_with_db, True)
        patch_order(system_env_with_db, order, {'status': finished_status.value})
        order_details = get_order_details(system_env_with_db, order['number'])
        for name in ['confirmed_at', 'confirmed_at_time', 'delivered_at', 'delivered_at_time']:
            assert order_details[name]
        assert order_details['visited_at_time'] is None
        assert order_details['confirmed_at_time'] == confirmed_at_time
        # Test that delivered_at_time is in the depot's timezone
        _check_time(status_change_time, order_details['delivered_at_time'], depot_tz)

        # Calculate arrival time
        query_routed_orders(system_env_with_db, route_env['courier']['id'], route_env['route']['id'])

        arrival_time_tested = False
        for order in route_env['orders']:
            order_details = get_order_details(system_env_with_db, order['number'])
            if order_details['status'] == finished_status.value:
                continue
            # Test that arrival_time is in the depot's timezone
            assert order_details['arrival_time'] == convert_time_offset_to_date(
                order_details['arrival_time_s'], route_date, depot_tz_str).isoformat()
            arrival_time_tested = True
        assert arrival_time_tested
