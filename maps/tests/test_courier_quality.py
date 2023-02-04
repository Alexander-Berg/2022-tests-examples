import datetime
import dateutil.tz
import pytest
import time
import iso8601
import requests

from maps.b2bgeo.ya_courier.backend.test_lib import util
from maps.b2bgeo.ya_courier.backend.test_lib.config import COURIER_QUALITY_FIELDS
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    create_route_env,
    get_courier_quality,
    actual_time_interval,
    later_time_interval,
    set_partially_finished_status_enabled,
    set_mark_delivered_enabled)
from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_unix_timestamp


def _get_courier_quality_for_route(system_env_with_db, route_env, with_depot=False):
    courier_report = get_courier_quality(system_env_with_db, route_env['route']['date'], params={'types': 'order,depot'})
    assert len(courier_report) >= len(route_env['orders'])

    report_for_current_route = \
        [record for record in courier_report if record['route_number'] == route_env['route']['number']]
    assert len(report_for_current_route) == len(route_env['orders']) + int(with_depot)
    return report_for_current_route


def _check_fields(courier_report):
    for row in courier_report:
        assert set(row) == COURIER_QUALITY_FIELDS[row['type']]


@skip_if_remote
def test_courier_quality_cgi_validation(system_env_with_db):
    path = util.api_path_with_company_id(
        system_env_with_db,
        "courier-quality?date=2020-06-06",
    )
    response = util.env_get_request(system_env_with_db, path)
    assert response.status_code == requests.codes.ok

    path = util.api_path_with_company_id(
        system_env_with_db,
        "courier-quality?date=07.06.2020",
    )
    response = util.env_get_request(system_env_with_db, path)
    assert response.status_code == requests.codes.unprocessable
    assert response.content
    j = response.json()
    assert j['message'].startswith('CGI parameters validation failed')
    assert j['message'].find('Not a valid date') >= 0


@pytest.mark.parametrize("finished_order_status, imei", [('partially_finished', 532097527496293), ('finished', 90902549264333)])
def test_courier_quality(system_env_with_db, finished_order_status, imei):
    """
    Test the following workflow:
        - a route with 2 orders is created
        - the first order is marked as finished
        - routed-orders are requested for this route
        - courier-quality report is requested for this route
            * check: report contains one record per each route order
            * check: one record has status 'new' and the other - 'finished'
            * check: order fields
    """
    POINTS = [
        {"lat": 55.733827, "lon": 37.588722},
        {"lat": 55.729299, "lon": 37.580116}
    ]

    with create_route_env(
            system_env_with_db,
            f'test_courier_quality-{finished_order_status}',
            imei=imei,
            order_locations=POINTS) as env:
        courier = env['courier']
        courier_id = courier['id']
        route = env['route']
        route_id = route['id']
        orders = env['orders']

        current_location = {
            'lat': orders[1]['lat'],
            'lon': orders[1]['lon'],
            'time_now': '08:00'
        }
        assert len(orders) == 2
        routed_orders = util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)
        assert len(routed_orders["route"]) == 2
        assert routed_orders["route"][0]["id"] == orders[0]["id"]
        assert routed_orders["route"][1]["id"] == orders[1]["id"]

        now = time.time()
        positions = [
            (55.730689, 37.584746, now - 30),
            (55.729926, 37.582643, now - 20),
            (55.729083, 37.580385, now - 10),
            (55.729083, 37.580385, now)
        ]

        util.push_positions(system_env_with_db, courier_id, route_id, positions)

        util.confirm_order(system_env_with_db, orders[1])
        routed_orders = util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)
        assert len(routed_orders["route"]) == 2
        assert routed_orders["route"][0]["id"] == orders[0]["id"]
        assert routed_orders["route"][1]["id"] == orders[1]["id"]

        if finished_order_status == 'partially_finished':
            set_partially_finished_status_enabled(system_env_with_db, True)
        util.finish_order(system_env_with_db, orders[1], finished_order_status=finished_order_status)
        routed_orders = util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)
        assert len(routed_orders["route"]) == 2

        assert routed_orders["route"][0]["id"] == orders[1]["id"]
        assert routed_orders["route"][1]["id"] == orders[0]["id"]

        report_for_current_route = _get_courier_quality_for_route(system_env_with_db, env)
        report_order_numbers = set([record['order_number'] for record in report_for_current_route])
        expected_order_numbers = set([order['number'] for order in orders])
        assert report_order_numbers == expected_order_numbers

        _check_fields(report_for_current_route)

        assert report_for_current_route[0]['route_imei'] == imei
        assert report_for_current_route[0]['route_imei_str'] == str(imei)

        new_order_record = None
        finished_order_record = None
        for order_record in report_for_current_route:
            if order_record['order_status'] == 'new':
                new_order_record = order_record
            elif order_record['order_status'] == finished_order_status:
                finished_order_record = order_record

        assert new_order_record
        assert new_order_record['order_number'] == orders[0]['number']
        assert finished_order_record
        assert finished_order_record['order_number'] == orders[1]['number']

        now = time.time()
        assert get_unix_timestamp(finished_order_record['order_completed_at']) == pytest.approx(now, 30)
        assert new_order_record['order_completed_at'] is None
        assert get_unix_timestamp(finished_order_record['order_confirmed_at']) == pytest.approx(now, 30)
        assert new_order_record['order_confirmed_at'] is None
        assert new_order_record['order_visited_at'] is None
        assert finished_order_record['order_visited_at'] is None  # the courier has not spent enough time there yet.
        assert not finished_order_record['far_from_point']
        assert new_order_record['far_from_point'] is None
        assert not finished_order_record["no_call_before_delivery"]
        assert new_order_record["no_call_before_delivery"]
        assert finished_order_record["late_call_before_delivery"]
        assert new_order_record["late_call_before_delivery"] is None
        assert finished_order_record["time_interval_error"] is None
        assert new_order_record["time_interval_error"] is None
        assert not finished_order_record["delivery_not_in_interval"]
        assert not new_order_record["delivery_not_in_interval"]
        assert finished_order_record["suggested_order_number"] == orders[0]["number"]
        assert new_order_record["suggested_order_number"] is None
        assert finished_order_record["not_in_order"]
        assert new_order_record["not_in_order"] is None
        assert len(finished_order_record["order_interval"]) == 1
        assert len(new_order_record["order_interval"]) == 1
        assert finished_order_record["segment_distance_m"] is None
        assert new_order_record["segment_distance_m"] is None
        assert new_order_record["order_weight"]
        assert new_order_record["order_volume"]
        assert new_order_record["order_comments"]
        assert new_order_record["route_routing_mode"]
        assert new_order_record["route_date"]
        assert new_order_record["order_shared_with_companies"] == []
        assert finished_order_record["delivery_lat"] is None
        assert finished_order_record["delivery_lon"] is None
        assert new_order_record["lat"] == POINTS[0]["lat"]
        assert new_order_record["lon"] == POINTS[0]["lon"]
        assert finished_order_record["lat"] == POINTS[1]["lat"]
        assert finished_order_record["lon"] == POINTS[1]["lon"]


def _get_order_report(system_env_with_db, route, order, params=None):
    courier_report = get_courier_quality(system_env_with_db, route['date'], params=params)
    assert len(courier_report) >= 1

    report_for_current_order = \
        [record for record in courier_report if record['order_number'] == order['number']]
    assert len(report_for_current_order) == 1
    _check_fields(report_for_current_order)

    return report_for_current_order[0]


@skip_if_remote
class TestOneOrder(object):
    @pytest.mark.parametrize("order_status", ['confirmed', 'finished', 'partially_finished', 'cancelled'])
    def test_air_distance(self, system_env_with_db, order_status):
        """
        Test the following workflow:
            - a route with 1 order is created
            - push position for the order
            - the order is marked as confirmed / finished / partially_finished / cancelled
            - courier-quality is requested for this order
                * check: "air_distance" for the order is set properly
        """
        depot = {'lat': 56.311739, 'lon': 38.136341, 'address': 'Sergiev Posad'}
        order = {'lat': 56.302124, 'lon': 38.164122}
        with create_route_env(
            system_env_with_db,
            'test_air_distance',
            order_locations=[order],
            depot_data=depot
        ) as env:
            order = env['orders'][0]
            courier = env['courier']
            route = env['route']
            depot = env['depot']

            util.push_positions(
                system_env_with_db,
                courier['id'],
                route['id'],
                [(order['lat'], order['lon'], time.time())])

            if order_status == 'partially_finished':
                set_partially_finished_status_enabled(system_env_with_db, True)
            util.change_order_status(system_env_with_db, order, order_status)

            order_report = _get_order_report(system_env_with_db, route, order)
            if order_status in ['confirmed', 'cancelled']:
                assert order_report['air_distance'] is None
            else:
                assert order_report['air_distance'] == pytest.approx(0.0)

    @pytest.mark.parametrize("order_status", ['confirmed', 'finished', 'partially_finished', 'cancelled'])
    def test_air_distance_no_positions(self, system_env_with_db, order_status):
        """
        Test the following workflow:
            - a route with 1 order is created
            - the order is marked as confirmed / finished / partially_finished /canceled
            - courier-quality is requested for this order
                * check: "air_distance" for the order is empty because no positions were pushed
        """
        order = {'lat': 56.302124, 'lon': 38.164122}
        with create_route_env(
            system_env_with_db,
            'test_air_distance_no_positions',
            order_locations=[order]
        ) as env:
            order = env['orders'][0]
            route = env['route']

            if order_status == 'partially_finished':
                set_partially_finished_status_enabled(system_env_with_db, True)
            util.change_order_status(system_env_with_db, order, order_status)

            order_report = _get_order_report(system_env_with_db, route, order)
            assert order_report['air_distance'] is None


@skip_if_remote
@pytest.mark.parametrize("order_status", ['confirmed', 'finished', 'partially_finished', 'cancelled'])
def test_one_order_with_depot(system_env_with_db, order_status):
    depot = {'lat': 56.311739, 'lon': 38.136341, 'address': 'Sergiev Posad'}
    order = {'lat': 56.302124, 'lon': 38.164122}
    with create_route_env(
        system_env_with_db,
        'test_air_distance',
        order_locations=[order],
        depot_data=depot
    ) as env:
        order = env['orders'][0]
        courier = env['courier']
        route = env['route']
        depot = env['depot']

        util.push_positions(
            system_env_with_db,
            courier['id'],
            route['id'],
            [(order['lat'], order['lon'], time.time())])

        if order_status == 'partially_finished':
            set_partially_finished_status_enabled(system_env_with_db, True)
        util.change_order_status(system_env_with_db, order, order_status)

        order_report = _get_order_report(system_env_with_db, route, order, {'types': 'order,depot'})
        if order_status in ['confirmed', 'cancelled']:
            assert order_report['air_distance'] is None
        else:
            assert order_report['air_distance'] == pytest.approx(0.0)


def test_courier_quality_wrong_sequence(system_env_with_db):
    """
    Test the following workflow:
        - a route with 3 orders is created
        - the first order confirmed and then delivered
        - the second order is confirmed
        - the third order is delivered
            * check: verify that the third order was delivered not in order
            * check: "arrived_at", "left_at" fields
            * check: order fields
    """
    POINTS = [
        {"lat": 55.663878, "lon": 37.482458},
        {"lat": 55.683761, "lon": 37.518000},
        {"lat": 55.705491, "lon": 37.551859},
    ]

    with create_route_env(
            system_env_with_db,
            'test_courier_quality_wrong_sequence',
            order_locations=POINTS) as env:

        courier = env['courier']
        courier_id = courier['id']
        route = env['route']
        route_id = route['id']
        orders = env['orders']

        current_location = {
            'lat': orders[0]['lat'],
            'lon': orders[0]['lon'],
            'time_now': '08:00'
        }
        now = time.time()
        util.push_positions(system_env_with_db, courier_id, route_id, [
                            (POINTS[0]["lat"], POINTS[0]["lon"], now)])
        util.push_positions(system_env_with_db, courier_id, route_id, [
                            (POINTS[1]["lat"], POINTS[1]["lon"], now)])
        util.push_positions(system_env_with_db, courier_id, route_id, [
                            (POINTS[2]["lat"], POINTS[2]["lon"], now)])

        # Get list of orders
        routed_orders = util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)
        # Confirm the first
        confirmed_order = routed_orders['route'][0]
        util.confirm_order(system_env_with_db, confirmed_order)
        # Update list of orders
        routed_orders = util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)

        # Confirmed has to be the first one
        assert routed_orders['route'][0]['id'] == confirmed_order['id']
        # Deliver the first order
        util.finish_order(system_env_with_db, confirmed_order)

        routed_orders = util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)

        assert routed_orders['route'][0]['status'] == 'finished'
        assert routed_orders['route'][1]['status'] == 'new'
        assert routed_orders['route'][2]['status'] == 'new'

        confirmed_order = routed_orders['route'][1]
        finished_order = routed_orders['route'][2]

        # confirm now the next order
        util.confirm_order(system_env_with_db, confirmed_order)
        routed_orders = util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)

        assert routed_orders['route'][0]['status'] == 'finished'
        assert routed_orders['route'][1]['status'] == 'confirmed'
        assert routed_orders['route'][2]['status'] == 'new'

        # But deliver another one
        util.finish_order(system_env_with_db, finished_order)
        util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)
        report_for_current_route = _get_courier_quality_for_route(system_env_with_db, env)
        _check_fields(report_for_current_route)

        for field in ["arrived_at", "left_at"]:
            for courier_idx in [1, 2]:
                # courier has to spent at least mark_delivered_service_time_coefficient * (order.service_duration_s + order.shared_service_duration_s)
                # seconds within mark_delivered_radius meters from the order's location.
                assert report_for_current_route[courier_idx][field] is None

        assert report_for_current_route[1]['order_number'] == finished_order['number']
        assert report_for_current_route[2]['order_number'] == confirmed_order['number']
        finished = report_for_current_route[1]
        confirmed = report_for_current_route[2]
        now = time.time()
        assert get_unix_timestamp(finished['order_completed_at']) == pytest.approx(now, 30)
        assert confirmed['order_completed_at'] is None
        assert finished['order_confirmed_at'] is None
        assert get_unix_timestamp(confirmed['order_confirmed_at']) == pytest.approx(now, 30)
        assert finished['far_from_point'] or report_for_current_route[0]['far_from_point']
        assert confirmed['far_from_point'] is None
        assert finished["no_call_before_delivery"]
        assert not confirmed["no_call_before_delivery"]
        assert not finished["late_call_before_delivery"]
        assert confirmed["late_call_before_delivery"] is None
        assert finished["time_interval_error"] is None
        assert confirmed["time_interval_error"] is None
        assert not finished["delivery_not_in_interval"]
        assert not confirmed["delivery_not_in_interval"]

        assert finished["suggested_order_number"] == confirmed_order['number']
        assert confirmed["suggested_order_number"] is None
        assert finished["not_in_order"]
        assert confirmed["not_in_order"] is None


ROUTE_PARAMS = [
    {
        'time_zone': 'Europe/Moscow',
        'depot': {'lat': 56.311739, 'lon': 38.136341, 'address': 'Sergiev Posad'},
        'order': {'lat': 56.302124, 'lon': 38.164122}

    },
    {
        'time_zone': 'Asia/Vladivostok',
        'depot': {'lat': 43.144527, 'lon': 131.912754, 'address': 'Vladivostok'},
        'order': {'lat': 43.149594, 'lon': 131.909197}
    }
]


@pytest.mark.parametrize("actual_interval", [True, False])
@pytest.mark.parametrize("route_params", ROUTE_PARAMS)
def test_delivery_in_interval(system_env_with_db, actual_interval, route_params):
    """
    Test the following workflow:
        - a route with 1 order and actual / later time interval is created
        - the order is marked as finished
        - routed-orders are requested for this route
        - courier-quality is requested for this route
            * check: `delivery_not_in_interval` is set to false / true
    """
    time_zone = dateutil.tz.gettz(route_params['time_zone'])
    route_date = datetime.datetime.now(time_zone).date()
    if actual_interval:
        time_interval = actual_time_interval(time_zone, route_date)
    else:
        time_interval = later_time_interval(time_zone, route_date)
    with create_route_env(
        system_env_with_db,
        f'test_delivery_in_interval-{actual_interval}-{route_params["time_zone"][0]}',
        order_locations=[route_params['order']],
        time_intervals=[time_interval],
        route_date=route_date.isoformat(),
        depot_data=route_params['depot']
    ) as env:
        order = env['orders'][0]
        courier = env['courier']
        route = env['route']
        depot = env['depot']
        util.finish_order(system_env_with_db, order)
        current_location = {
            'lat': depot['lat'],
            'lon': depot['lon'],
            'timestamp': datetime.datetime.now(time_zone).timestamp()
        }
        util.query_routed_orders(
            system_env_with_db, courier['id'], route['id'], current_location)
        order_report = _get_order_report(system_env_with_db, route, order)
        assert order_report['delivery_not_in_interval'] != actual_interval


def test_BBGEO1917(system_env_with_db):
    with create_route_env(
            system_env_with_db,
            'test_courier_quality2', order_status='finished') as env:
        courier = env['courier']
        courier_id = courier['id']
        route = env['route']
        route_id = route['id']
        orders = env['orders']

        report_for_current_route = _get_courier_quality_for_route(system_env_with_db, env)
        assert len(report_for_current_route) == 2
        now = time.time()
        positions = [
            (55.730689, 37.584746, now - 30),
            (55.729926, 37.582643, now - 20),
            (55.729083, 37.580385, now - 10),
            (55.729083, 37.580385, now)
        ]

        util.push_positions(system_env_with_db, courier_id, route_id, positions)

        current_location = {
            'lat': orders[1]['lat'],
            'lon': orders[1]['lon'],
            'time_now': '08:00'
        }
        util.query_routed_orders(
            system_env_with_db, courier_id, route_id, current_location)

        report_for_current_route = _get_courier_quality_for_route(system_env_with_db, env)
        assert len(report_for_current_route) == 2


def test_order_visited_at_report_1(system_env_with_db):
    """
    Test the following workflow:
        [auto delivery is disabled]
        - a route with 2 orders is created
        - the second order is marked as finished
        - courier-quality report is requested for this route
            * check: visited_at is not computed if too early.
            * check: visited_at computed even for finished orders.
            * check: visited_at computed even before the order is marked as delivered.
            * check: a correct time is chosen (closest from the delivery formula point of view).
            * check: depot timezone is expected
    """
    POINTS = [
        {"lat": 55.733827, "lon": 37.588722},
        {"lat": 55.729299, "lon": 37.580116}
    ]

    with create_route_env(system_env_with_db, 'test_order_visited_at_report_1', order_locations=POINTS) as env:
        try:
            set_mark_delivered_enabled(system_env_with_db, mark_delivered_enabled=False)
            courier_id = env['courier']['id']
            route_id = env['route']['id']
            orders = env['orders']

            now = time.time()
            positions = [  # near the 2nd order
                (55.730689, 37.584746, now - 1300),
                (55.729926, 37.582643, now - 1290),
                (55.729083, 37.580385, now - 1280),
            ]

            util.push_positions(system_env_with_db, courier_id, route_id, positions)
            util.finish_order(system_env_with_db, orders[1])

            report = _get_courier_quality_for_route(system_env_with_db, env)
            _check_fields(report)

            for order_record in report:
                # the courier has not spent enough time there yet
                assert order_record['order_visited_at'] is None

            positions = [
                (55.729083, 37.580385, now - 700),
                (POINTS[0]['lat'], POINTS[0]['lon'], now - 610)
            ]
            util.push_positions(system_env_with_db, courier_id, route_id, positions)

            def check_firstly_delivered_order():
                report_for_current_route = _get_courier_quality_for_route(system_env_with_db, env)
                _check_fields(report_for_current_route)
                assert len(report_for_current_route) == 2
                finished_order_record = [r for r in report_for_current_route if r['order_number'] == orders[1]['number']][0]
                new_order_record = [r for r in report_for_current_route if r['order_number'] == orders[0]['number']][0]

                assert get_unix_timestamp(finished_order_record['order_visited_at']) == pytest.approx(now - 700)
                return new_order_record

            new_order = check_firstly_delivered_order()
            assert new_order['order_visited_at'] is None

            positions = [
                (POINTS[0]['lat'] + 1e-5, POINTS[0]['lon'], now - 300),
                (POINTS[0]['lat'], POINTS[0]['lon'], now - 10),
                (POINTS[0]['lat'] - 1e-5, POINTS[0]['lon'], now)
            ]
            util.push_positions(system_env_with_db, courier_id, route_id, positions)

            new_order = check_firstly_delivered_order()
            assert get_unix_timestamp(new_order['order_visited_at']) == pytest.approx(now - 10)

            # check the time zone
            test_time = datetime.datetime.now()
            visited_at_tz = iso8601.parse_date(new_order['order_visited_at']).tzinfo
            depot_tz = dateutil.tz.gettz(env['depot']['time_zone'])
            assert test_time.astimezone(visited_at_tz).isoformat() == test_time.astimezone(depot_tz).isoformat()
        finally:
            # restore previous state
            set_mark_delivered_enabled(system_env_with_db, mark_delivered_enabled=True)


def test_order_visited_at_report_2(system_env_with_db):
    """
    Check that the report is not updated in case the courier visited the order multiple times: only the first visit is
    counted.
    """
    other_pos = {"lat": 55.729299, "lon": 37.580116}
    with create_route_env(system_env_with_db, 'test_order_visited_at_report_2',
                          order_locations=[{"lat": 55.733827, "lon": 37.588722}]) as env:
        courier = env['courier']
        courier_id = courier['id']
        route = env['route']
        route_id = route['id']
        order = env['orders'][0]

        now = time.time()
        positions = [
            (order['lat'], order['lon'], now - 1900),
            (order['lat'], order['lon'], now - 1300),
            (order['lat'], order['lon'], now - 1250),
            (other_pos['lat'], other_pos['lon'], now - 1200)
        ]
        util.push_positions(system_env_with_db, courier_id, route_id, positions)

        report = _get_courier_quality_for_route(system_env_with_db, env)
        _check_fields(report)
        reported_order = report[0]
        assert len(report) == 1
        assert get_unix_timestamp(reported_order['order_visited_at']) == pytest.approx(now - 1300)
        assert reported_order['order_status'] == 'finished'  # auto-delivery

        positions = [
            (order['lat'], order['lon'], now - 1100),
            (order['lat'], order['lon'], now - 500),
            (other_pos['lat'], other_pos['lon'], now - 400),
            (order['lat'], order['lon'], now - 300),
            (order['lat'], order['lon'], now),
        ]
        util.push_positions(system_env_with_db, courier_id, route_id, positions)

        report = _get_courier_quality_for_route(system_env_with_db, env)
        _check_fields(report)
        reported_order = report[0]
        assert len(report) == 1
        assert get_unix_timestamp(reported_order['order_visited_at']) == pytest.approx(now - 1300)
        assert reported_order['order_status'] == 'finished'


def test_order_visited_at_report_3(system_env_with_db):
    """
    Test the following workflow:
        [auto delivery is disabled]
        - a route with 2 orders at the same location(!) is created
        - courier-quality report is requested for this route
            * check: visited_at is not computed if too early.
            * check: later visited_at is computed for both finished orders.
            * check: a correct time is chosen (closest from the delivery formula point of view).
    """
    POINT = {"lat": 55.733827, "lon": 37.588722}
    POINTS = [POINT, POINT]

    with create_route_env(system_env_with_db, 'test_order_visited_at_report_3', order_locations=POINTS) as env:
        try:
            set_mark_delivered_enabled(system_env_with_db, mark_delivered_enabled=False)
            courier_id = env['courier']['id']
            route_id = env['route']['id']

            now = time.time()

            positions = [
                (POINT['lat'], POINT['lon'] + 1.5e-5, now - 1300),
                (POINT['lat'], POINT['lon'] + 1.4e-5, now - 1290),
                (POINT['lat'], POINT['lon'] + 1.3e-5, now - 1280),
            ]
            util.push_positions(system_env_with_db, courier_id, route_id, positions)

            report = _get_courier_quality_for_route(system_env_with_db, env)
            _check_fields(report)
            for order_record in report:
                # the courier has not spent enough time there yet
                assert order_record['order_visited_at'] is None

            positions = [
                (POINT['lat'], POINT['lon'] + 1.2e-5, now - 700),
                (POINT['lat'], POINT['lon'] + 1.1e-5, now - 610),
                (POINT['lat'], POINT['lon'] + 1.0e-5, now - 300),
                (POINT['lat'], POINT['lon'], now - 10),
                (POINT['lat'], POINT['lon'] + 1.0e-5, now),
            ]
            util.push_positions(system_env_with_db, courier_id, route_id, positions)

            report = _get_courier_quality_for_route(system_env_with_db, env)
            _check_fields(report)
            for order_record in report:
                assert get_unix_timestamp(order_record['order_visited_at']) == pytest.approx(now - 10)
        finally:
            # restore previous state
            set_mark_delivered_enabled(system_env_with_db, mark_delivered_enabled=True)


def _combine_to_datetime(date, tz, **kwargs):
    return datetime.datetime.combine(date, datetime.time(**kwargs)).astimezone(tz)


def test_report_arrived_at_left_at(system_env_with_db):
    """
    Check that courier-quality report uses arrived_at/left_at from history events.
    Note that order history stores only the first (among all) visit.
    """
    depot = {'lat': 56.311739, 'lon': 38.136341, 'address': 'Sergiev Posad'}
    depot_hour = 12
    order_point = {"lat": 55.733827, "lon": 37.588722}
    order_hour = 10
    test_tz = dateutil.tz.gettz("Europe/Moscow")
    route_date = datetime.datetime.now(test_tz).date() - datetime.timedelta(days=1)

    with create_route_env(system_env_with_db, 'test_arrived_left_at_report', order_locations=[order_point],
                          depot_data=depot, route_date=route_date.isoformat(), add_depot_to_route=True) as env:

        def visit_point(courier_id, route_id, point, hour):
            ts = _combine_to_datetime(route_date, test_tz, hour=hour).timestamp()
            positions = [
                (point['lat'] - 0.1, point['lon'], ts - 800),
                (point['lat'], point['lon'], ts - 720),
                (point['lat'], point['lon'], ts - 60),
                (point['lat'] + 0.1, point['lon'], ts)
            ]
            util.push_positions(system_env_with_db, courier_id, route_id, positions)
            positions = [
                (point['lat'], point['lon'], ts + 60),
                (point['lat'], point['lon'], ts + 720),
                (point['lat'] + 0.1, point['lon'], ts + 800)
            ]
            util.push_positions(system_env_with_db, courier_id, route_id, positions)

        visit_point(env['courier']['id'], env['route']['id'], order_point, order_hour)
        visit_point(env['courier']['id'], env['route']['id'], depot, depot_hour)

        route_report = _get_courier_quality_for_route(system_env_with_db, env, with_depot=True)

        assert route_report[0]['type'] == 'order'
        assert route_report[0]['arrived_at'] == _combine_to_datetime(route_date, test_tz, hour=order_hour - 1, minute=48).isoformat()
        assert route_report[0]['left_at'] == _combine_to_datetime(route_date, test_tz, hour=order_hour - 1, minute=59).isoformat()

        assert route_report[1]['type'] == 'depot'
        assert route_report[1]['arrived_at'] == _combine_to_datetime(route_date, test_tz, hour=depot_hour - 1, minute=48).isoformat()
        assert route_report[1]['left_at'] == _combine_to_datetime(route_date, test_tz, hour=depot_hour - 1, minute=59).isoformat()


@skip_if_remote
def test_order_segment_distance_bug_1(system_env_with_db):
    # For details see https://st.yandex-team.ru/BBGEO-2806

    order_location_1 = {"lat": 55.733827, "lon": 37.588722}
    order_location_2 = {"lat": 55.729299, "lon": 37.580116}

    with create_route_env(system_env_with_db, 'test_order_segment_distance_bug_1_1', order_locations=[order_location_1]) as env_1:
        with create_route_env(system_env_with_db, 'test_order_segment_distance_bug_1_2', order_locations=[order_location_2]) as env_2:
            courier_id_1 = env_1['courier']['id']
            courier_id_2 = env_2['courier']['id']
            route_id_1 = env_1['route']['id']
            route_id_2 = env_2['route']['id']

            ######################
            # Setup: courier_id_1 < courier_id_2 && route_id of courier_1 > route_id of courier_2
            #
            assert courier_id_1 < courier_id_2
            assert route_id_1 < route_id_2
            assert env_1['route']['courier_id'] == courier_id_1
            assert env_2['route']['courier_id'] == courier_id_2
            change = {
                'courier_id': courier_id_2
            }
            util.patch_route(system_env_with_db, route_id_1, change)

            change = {
                'courier_id': courier_id_1
            }
            util.patch_route(system_env_with_db, route_id_2, change)

            # now courier_id_1 is assigned to the route_2 and courier_id_2 is assigned to route_id_1
            env_1['route']['courier_id'] = courier_id_2
            env_2['route']['courier_id'] = courier_id_1

            ######################
            # Execution
            #
            now = time.time()
            for idx, route_env in enumerate([env_1, env_2]):
                route = route_env['route']
                orders = route_env['orders']
                start_pos = util.get_location_nearby(orders[0], now - 1500)

                order_loc = {
                    'lat': orders[0]['lat'],
                    'lon': orders[0]['lon']
                }
                positions = [
                    (start_pos['lat'], start_pos['lon'], now - 1000),
                    (order_loc['lat'], order_loc['lon'] + 1.4e-5, now - 700),
                    (order_loc['lat'], order_loc['lon'] + 1.0e-5, now - 800),
                    (order_loc['lat'], order_loc['lon'], now - 10),
                ]
                util.push_positions(system_env_with_db, route['courier_id'], route['id'], positions)

                report = _get_courier_quality_for_route(system_env_with_db, route_env)
                for order_report in report:
                    assert order_report['segment_distance_m'] > 0.1, order_report

            report = _get_courier_quality_for_route(system_env_with_db, env_1)
            assert len(report) == 1
            assert report[0]['segment_distance_m'] == pytest.approx(1667.923899)

            report = _get_courier_quality_for_route(system_env_with_db, env_2)
            assert len(report) == 1
            assert report[0]['segment_distance_m'] == pytest.approx(1667.923899)
            ##################
            # Clean-up.
            # The usual clean-up inside create_route_env would try to delete courier_1 but it's not possible because he
            # is referenced in the courier_positions table for a different route, which is supposed to be deleted by
            # outer create_route_env. So we are manually breaking the circular dependency here.
            #
            for route_env in [env_1['route'], env_2['route']]:
                util.cleanup_route_orders(route_env['id'], system_env_with_db)
                resp = util.env_delete_request(
                    system_env_with_db, "{}/routes/{}".format(
                        util.api_path_with_company_id(system_env_with_db), route_env['id']),
                    auth=system_env_with_db.auth_header_super
                )
                resp.raise_for_status()
