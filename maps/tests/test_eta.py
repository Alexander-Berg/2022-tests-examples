import sys
import contextlib
from datetime import datetime, timedelta
import dateutil
import dateutil.tz
import itertools
import json
import pytest
import random
import requests
import pytz
from urllib.parse import urlencode
from yandex.maps.test_utils.common import wait_until
from ya_courier_backend.models import OrderStatus, EtaType
from ya_courier_backend.models.order import has_order_history_event
from maps.b2bgeo.libs.py_flask_utils.format_util import parse_time_range
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import is_auth_enabled, skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.ya_courier import (
    DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S,
    DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S,
    TESTING_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S,
    TESTING_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    batch_orders,
    create_route_env,
    create_tmp_company,
    env_get_request,
    get_order_details_by_route_id,
    get_position_shifted_east,
    post_order_sequence,
    push_imei_positions,
    query_routed_orders,
    set_mark_delivered_enabled,
    patch_order_by_order_id,
    patch_company,
)


_TIMEZONES_STR = ['Europe/Moscow', 'Asia/Novosibirsk']
_TIMEZONE_STR = _TIMEZONES_STR[0]
_ROUTE_DATETIME = datetime(2020, 4, 25, tzinfo=dateutil.tz.gettz(_TIMEZONE_STR))
_TIME_TO_UPDATE_ROUTE_S = 2
_START_ORDER_HOUR = 7
_START_TIME = _ROUTE_DATETIME + timedelta(hours=_START_ORDER_HOUR)


def _offset_datetime(offset_s, start_time=_START_TIME):
    return start_time + timedelta(seconds=offset_s)


def _push_position(env, offset_s, loc, start_time=_START_TIME):
    push_imei_positions(env['dbenv'], env['imei'], _offset_datetime(offset_s, start_time), [(loc['lat'], loc['lon'])])


def _get_shifted_loc(loc, distance):
    lat, lon = get_position_shifted_east(loc['lat'], loc['lon'], distance)
    return {'lat': lat, 'lon': lon}


@contextlib.contextmanager
def _create_env(system_env_with_db, name, order_locations, service_duration_s, shared_service_duration_s,
                routing_mode=None, eta_type=None, time_intervals=None,
                route_date=_ROUTE_DATETIME.date().isoformat(), timezone=_TIMEZONE_STR):
    imei = random.randint(1000, 100000000000)
    with create_tmp_company(system_env_with_db, f'Test company {name}') as company_id:
        set_mark_delivered_enabled(system_env_with_db, True, company_id)
        with create_route_env(
                system_env_with_db,
                name,
                route_date=route_date,
                order_locations=order_locations,
                routing_mode=routing_mode,
                depot_data={'time_zone': timezone},
                imei=imei,
                company_id=company_id,
                auth=system_env_with_db.auth_header_super,
                order_status=OrderStatus.new.value,
                time_intervals=time_intervals,
                eta_type=eta_type) as env:

            env['dbenv'] = system_env_with_db
            env['company_id'] = company_id
            env['imei'] = imei

            batch_orders(
                env['dbenv'],
                [
                    {
                        'number': order['number'],
                        'service_duration_s': service_duration_s,
                        'shared_service_duration_s': shared_service_duration_s
                    } for order in env['orders']
                ],
                company_id=env['company_id'],
                auth=env['dbenv'].auth_header_super)

            yield env


def _predict_eta(env, start_time, offset_s, loc, use_optimal_sequence):
    url = f"couriers/{env['courier']['id']}/routes/{env['route']['id']}/predict-eta"
    params = urlencode({
        'lat': loc['lat'],
        'lon': loc['lon'],
        'time': _offset_datetime(offset_s, start_time).isoformat(),
        'find-optimal': json.dumps(use_optimal_sequence)
    })
    response = env_get_request(env['dbenv'], f'{url}?{params}', auth=env['dbenv'].auth_header_super)
    assert response.status_code == requests.codes.ok, (url, response.text)
    orders = response.json()['route']
    assert [x['id'] for x in orders] == [x['id'] for x in env['orders']]
    return orders


def _get_route_updated_at(env, route_date):
    api_path = api_path_with_company_id(env['dbenv'], company_id=env['company_id'])
    response = env_get_request(
        env['dbenv'],
        f"{api_path}/route-states?date={route_date}&courier_number={env['courier']['number']}",
        auth=env['dbenv'].auth_header_super
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert len(j) == 1
    return j[0]['updated_at']


def _get_update_params():
    if is_auth_enabled():
        return TESTING_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S, TESTING_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S
    return DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_DELAY_S, DEFAULT_UPDATE_ROUTE_STATES_ON_STATUS_PERIOD_S


def _get_orders_with_eta(env, eta_method, offset_s, loc, start_time=_START_TIME):
    if eta_method == 'routed-orders':
        return query_routed_orders(
            env['dbenv'],
            env['courier']['id'],
            env['route']['id'],
            point={'lat': loc['lat'], 'lon': loc['lon'], 'timestamp': _offset_datetime(offset_s, start_time).timestamp()},
            auth=env['dbenv'].auth_header_super
        )['route']
    elif eta_method == 'predict-eta':
        return _predict_eta(env, start_time, offset_s, loc, use_optimal_sequence=False)
    elif eta_method == 'predict-eta-optimal':
        return _predict_eta(env, start_time, offset_s, loc, use_optimal_sequence=True)
    elif eta_method == 'background-threads':
        post_order_sequence(env['dbenv'], env['route']['id'], [x['id'] for x in env['orders']], company_id=env['company_id'], auth=env['dbenv'].auth_header_super)
        # Wait route state update by background threads
        route_date = start_time.date().isoformat()
        updated_at = _get_route_updated_at(env, route_date)
        delay_s, period_s = _get_update_params()
        wait_until(lambda: _get_route_updated_at(env, route_date) != updated_at,
                   timeout=delay_s + period_s + _TIME_TO_UPDATE_ROUTE_S)
        return get_order_details_by_route_id(env['dbenv'], env['route']['id'], company_id=env['company_id'], auth=env['dbenv'].auth_header_super)
    else:
        assert False


def _get_min_max_route_time_s(distance, routing_mode='driving'):
    speed_limits = {
        'driving': {'min': 0.5, 'max': 30.0},
        'walking': {'min': 0.25, 'max': 2.25},
    }

    min_possible_speed = speed_limits[routing_mode]['min']
    max_possible_speed = speed_limits[routing_mode]['max']

    min_route_time_s = distance / max_possible_speed
    max_route_time_s = distance / min_possible_speed

    return min_route_time_s, max_route_time_s


def _validate_eta(eta, start_time_s, distance, routing_mode='driving', start_time=_START_TIME):
    min_route_time_s, max_route_time_s = _get_min_max_route_time_s(distance, routing_mode)

    min_expected_eta = _offset_datetime(start_time_s + min_route_time_s, start_time)
    max_expected_eta = _offset_datetime(start_time_s + max_route_time_s, start_time)

    assert min_expected_eta <= eta <= max_expected_eta


@pytest.mark.parametrize('eta_method,eta_type,time_intervals',
                         [('routed-orders', EtaType.arrival_time, None),
                          ('predict-eta', EtaType.arrival_time, None),
                          ('predict-eta-optimal', EtaType.arrival_time, None),
                          pytest.param('background-threads', EtaType.arrival_time, None, marks=skip_if_remote),
                          ('routed-orders', EtaType.delivery_time, None),
                          ('predict-eta', EtaType.delivery_time, None),
                          pytest.param('background-threads', EtaType.delivery_time, None, marks=skip_if_remote),
                          ('routed-orders', EtaType.delivery_time, ["09:00-11:00", "09:00-23:00"]),
                          ('predict-eta', EtaType.delivery_time, ["09:00-11:00", "09:00-23:00"]),
                          pytest.param('background-threads', EtaType.delivery_time, ["09:00-11:00", "09:00-23:00"], marks=skip_if_remote)])
def test_eta_finished_and_visited_order(system_env_with_db, eta_method, eta_type, time_intervals):
    """
    Test the following workflow (https://st.yandex-team.ru/BBGEO-6339):
        - create two orders (A and B) with non-overlapping auto-delivery radiuses
        - arrive to A (not marked yet as auto-delivered)
        - compute ETA and check that ETA of order B is
             correct_arrival_time("current time") + "service duration" + "shared service duration" + "route time from A to B"
        - spend just enough time near order A to mark it as auto-delivered
        - compute ETA and check that ETA of order B is
             correct_arrival_time("arrival time in orderA delivery radius") + "service duration" + "shared service duration" + "route time from A to B"
        - spend more time near A, but still in total less than "service duration" + "shared service duration"
        - compute ETA and check that ETA of order B is still
             correct_arrival_time("arrival time in orderA delivery radius") + "service duration" + "shared service duration" + "route time from A to B"
        - spend more time near A, in total more than "service duration" + "shared service duration"
        - compute ETA and check that ETA of order B is
             correct_arrival_time("current time") + "route time from A to B"
        where correct_arrival_time(t) = t if eta_type=arrival_time or
              correct_arrival_time(t) = max(t, time_window_start) if eta_type=delivery_time
    """

    service_duration_s = timedelta(hours=2).seconds
    shared_service_duration_s = timedelta(hours=3).seconds
    total_service_duration_s = service_duration_s + shared_service_duration_s
    waiting_time_s = 0
    if eta_type == EtaType.delivery_time and time_intervals:
        waiting_time_s = parse_time_range(time_intervals[0])[0] - timedelta(hours=_START_ORDER_HOUR).seconds
    distance_between_orders = 1200

    orderA_loc = {"lat": 55.733827, "lon": 37.588722}
    orderB_loc = _get_shifted_loc(orderA_loc, distance_between_orders)

    with _create_env(
            system_env_with_db,
            f'ftest_eta_fvo-{eta_method}-{eta_type == EtaType.arrival_time}-{time_intervals is None}',
            [orderA_loc, orderB_loc],
            service_duration_s=service_duration_s,
            shared_service_duration_s=shared_service_duration_s,
            time_intervals=time_intervals,
            eta_type=eta_type.value) as env:

        def _get_eta(time_s, loc, expect_orderA_finished):
            orders = _get_orders_with_eta(env, eta_method, time_s, loc)
            assert [x['status'] for x in orders] == [OrderStatus.finished.value if expect_orderA_finished else OrderStatus.new.value, OrderStatus.new.value]
            assert [has_order_history_event(orders[0], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [expect_orderA_finished, expect_orderA_finished, False]
            assert [has_order_history_event(orders[1], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            return dateutil.parser.parse(orders[1]['arrival_time'])

        # Arrive to A (not marked yet as auto-delivered)
        current_time_s = 0
        _push_position(env, current_time_s, orderA_loc)
        eta = _get_eta(current_time_s, orderA_loc, expect_orderA_finished=False)
        _validate_eta(eta, total_service_duration_s + waiting_time_s, distance_between_orders)

        if waiting_time_s:
            current_time_s = waiting_time_s
            _push_position(env, current_time_s, orderA_loc)
            eta = _get_eta(current_time_s, orderA_loc, expect_orderA_finished=False)
            _validate_eta(eta, total_service_duration_s + waiting_time_s, distance_between_orders)

        # Spend just enough time near order A so the order is marked as auto-delivered
        current_time_s = waiting_time_s + total_service_duration_s * 0.5
        _push_position(env, current_time_s, orderA_loc)
        eta = _get_eta(current_time_s, orderA_loc, expect_orderA_finished=True)
        _validate_eta(eta, total_service_duration_s + waiting_time_s, distance_between_orders)

        # Spend more time near A, but still in total less than "service duration" + "shared service duration"
        current_time_s = waiting_time_s + total_service_duration_s * 0.75
        _push_position(env, current_time_s, orderA_loc)
        eta = _get_eta(current_time_s, orderA_loc, expect_orderA_finished=True)
        _validate_eta(eta, total_service_duration_s + waiting_time_s, distance_between_orders)

        # Spend more time near A, but still in total less than "service duration" + "shared service duration"
        current_time_s = waiting_time_s + total_service_duration_s * 0.99
        _push_position(env, current_time_s, orderA_loc)
        eta = _get_eta(current_time_s, orderA_loc, expect_orderA_finished=True)
        _validate_eta(eta, total_service_duration_s + waiting_time_s, distance_between_orders)

        # Spend more time near A, in total more than "service duration" + "shared service duration"
        current_time_s = waiting_time_s + total_service_duration_s * 2
        _push_position(env, current_time_s, orderA_loc)
        eta = _get_eta(current_time_s, orderA_loc, expect_orderA_finished=True)
        _validate_eta(eta, current_time_s, distance_between_orders)


@pytest.mark.parametrize('eta_method', ['routed-orders', 'predict-eta', 'predict-eta-optimal', 'background-threads'])
def test_eta_finished_and_visited_and_departed_order(system_env_with_db, eta_method):
    """
    Test the following workflow (https://st.yandex-team.ru/BBGEO-6339):
        - create two orders (A and B) with non-overlapping auto-delivery radiuses
        - spend just enough time near order A to mark it as auto-delivered
        - leave the area of A
        - compute ETA and check that ETA of order B is
             "current time" + "route time from current location to B"
        - enter the area of A
        - compute ETA and check that ETA of order B is still
             "current time" + "route time from current location to B"
    """

    service_duration_s = timedelta(hours=2).seconds
    shared_service_duration_s = timedelta(hours=3).seconds
    total_service_duration_s = service_duration_s + shared_service_duration_s
    distance_between_orders = 1200

    orderA_loc = {"lat": 55.733827, "lon": 37.588722}
    orderB_loc = _get_shifted_loc(orderA_loc, distance_between_orders)
    halfway_loc = _get_shifted_loc(orderA_loc, 0.5 * distance_between_orders)

    with _create_env(
            system_env_with_db,
            f'test_eta_fvdo-{eta_method}',
            [orderA_loc, orderB_loc],
            service_duration_s=service_duration_s,
            shared_service_duration_s=shared_service_duration_s) as env:

        def _get_eta(time_s, loc):
            orders = _get_orders_with_eta(env, eta_method, time_s, loc)
            assert [x['status'] for x in orders] == [OrderStatus.finished.value, OrderStatus.new.value]
            assert [has_order_history_event(orders[0], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [True, True, True]
            assert [has_order_history_event(orders[1], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            return dateutil.parser.parse(orders[1]['arrival_time'])

        _push_position(env, 0, orderA_loc)

        # Spend just enough time near order A so the order is marked as auto-delivered
        current_time_s = total_service_duration_s * 0.5
        _push_position(env, current_time_s, orderA_loc)

        # Leave order A's area
        current_time_s = total_service_duration_s * 0.51
        _push_position(env, current_time_s, halfway_loc)
        eta = _get_eta(current_time_s, halfway_loc)
        _validate_eta(eta, current_time_s, 0.5 * distance_between_orders)

        # Enter the area of A
        current_time_s = total_service_duration_s * 0.52
        _push_position(env, current_time_s, orderA_loc)
        eta = _get_eta(current_time_s, orderA_loc)
        _validate_eta(eta, current_time_s, distance_between_orders)


def _make_time_intervals(start_time, delay_hours):
    result = []
    for length_hours in [2, 20]:
        result.append("{}/{}".format((start_time + timedelta(hours=delay_hours)).isoformat(),
                                     (start_time + timedelta(hours=delay_hours+length_hours)).isoformat()))
    return result


def _select_start_time_test_eta_finished_and_not_visited(arrival_delay_hours):
    for timezone in _TIMEZONES_STR:
        start_time = datetime.now(pytz.timezone(timezone))
        if 0 <= start_time.hour + arrival_delay_hours <= 23:
            # start_time and start_time + arrival_delay_hours should be in the same day
            start_time = start_time.replace(minute=0).replace(second=0).replace(microsecond=0)
            print(f"Selected start_time: {start_time}, timezone: {timezone}", file=sys.stderr, flush=True)
            return start_time, timezone
    assert False, "Can't select start_time"


@pytest.mark.parametrize('eta_method,eta_type,window_delay_hours,arrival_delay_hours',
                         [('routed-orders', EtaType.arrival_time, 0, 0),
                          ('predict-eta', EtaType.arrival_time, 0, 0),
                          ('predict-eta-optimal', EtaType.arrival_time, 0, 0),
                          ('background-threads', EtaType.arrival_time, 0, 0),
                          ('routed-orders', EtaType.delivery_time, 0, 0),
                          ('routed-orders', EtaType.delivery_time, 1, 0),
                          ('predict-eta', EtaType.delivery_time, 1, 0),
                          ('predict-eta-optimal', EtaType.delivery_time, 1, 0),
                          ('background-threads', EtaType.delivery_time, 1, 0),
                          ('routed-orders', EtaType.delivery_time, 1, 2),
                          ('predict-eta', EtaType.delivery_time, 1, 2),
                          ('predict-eta-optimal', EtaType.delivery_time, 1, 2),
                          ('background-threads', EtaType.delivery_time, 1, 2),
                          ('routed-orders', EtaType.delivery_time, -1, 0),
                          ('predict-eta', EtaType.delivery_time, -1, 0),
                          ('predict-eta-optimal', EtaType.delivery_time, -1, 0),
                          ('background-threads', EtaType.delivery_time, -1, 0),
                          ('routed-orders', EtaType.delivery_time, -1, -2),
                          ('predict-eta', EtaType.delivery_time, -1, -2),
                          ('predict-eta-optimal', EtaType.delivery_time, -1, -2),
                          ('background-threads', EtaType.delivery_time, -1, -2)])
def test_eta_finished_and_not_visited(system_env_with_db, eta_method, eta_type, window_delay_hours, arrival_delay_hours):
    """
    Test the following workflow (https://st.yandex-team.ru/BBGEO-6340):
        - create two orders (A and B) with non-overlapping auto-delivery radiuses
        - enter the area of A
        - mark order A as finished
        - compute ETA and check that ETA of order B
            * for eta_type = arrival_time:
              "arrival time in orderA delivery radius" + "service duration" + "shared service duration" + "route time from A to B"
            * for eta_type = delivery_time 4 cases depending on arrival, finish time and time window order:
              1) (arrival, finish, window_start) - same as eta_type=arrival_time
              2) (finish, window_start, arrival) - same as eta_type=arrival_time
              3) (window_start, arrival, finish) -
                max("arrival time", "window start") + "service duration" + "shared service duration" + "route time from A to B"
              4) (arrival, window_start, finish) - same as 3), but in this case window_start > arrival
    """

    service_duration_s = timedelta(hours=2).seconds
    shared_service_duration_s = timedelta(hours=3).seconds
    total_service_duration_s = service_duration_s + shared_service_duration_s

    # As patch_order_by_order_id() sets current time as an order finish time, we use here
    # current day as route_date and start_time - as a start of the orders delivery.
    start_time, timezone = _select_start_time_test_eta_finished_and_not_visited(arrival_delay_hours)
    time_intervals = _make_time_intervals(start_time, window_delay_hours) if window_delay_hours else None

    # use total_seconds because arrival_delay_hours can be negative
    arrival_delay_s = timedelta(hours=arrival_delay_hours).total_seconds()
    waiting_time_s = 0
    if eta_type == EtaType.delivery_time:
        if window_delay_hours < 0 and arrival_delay_hours < window_delay_hours:
            waiting_time_s = timedelta(hours=window_delay_hours-arrival_delay_hours).seconds
    distance_between_orders = 1200

    prev_courier_loc = {"lat": 55.733827, "lon": 37.588722}
    orderA_loc = _get_shifted_loc(prev_courier_loc, distance_between_orders)
    orderB_loc = _get_shifted_loc(orderA_loc, distance_between_orders)

    with _create_env(
            system_env_with_db,
            f'test_eta_fnvo-{eta_method}-{eta_type}-{window_delay_hours}-{arrival_delay_hours}',
            [orderA_loc, orderB_loc],
            service_duration_s=service_duration_s,
            shared_service_duration_s=shared_service_duration_s,
            eta_type=eta_type.value,
            time_intervals=time_intervals,
            route_date=start_time.date().isoformat(),
            timezone=timezone) as env:

        def _get_eta(time_s, loc, start_time):
            orders = _get_orders_with_eta(env, eta_method, time_s, loc, start_time)
            assert [x['status'] for x in orders] == [OrderStatus.finished.value, OrderStatus.new.value]
            assert [has_order_history_event(orders[0], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            assert [has_order_history_event(orders[1], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            return dateutil.parser.parse(orders[1]['arrival_time'])

        orders = env['orders']
        _push_position(env, arrival_delay_s - 2 * 60, prev_courier_loc, start_time=start_time)
        _push_position(env, arrival_delay_s, orderA_loc, start_time=start_time)
        _push_position(env, arrival_delay_s + total_service_duration_s / 3, orderA_loc, start_time=start_time)
        patch_order_by_order_id(system_env_with_db,
                                orders[0]['id'],
                                {'status': OrderStatus.finished.value},
                                company_id=env['company_id'],
                                auth=system_env_with_db.auth_header_super)

        eta = _get_eta(arrival_delay_s, orderA_loc, start_time)
        _validate_eta(eta, arrival_delay_s + total_service_duration_s + waiting_time_s, distance_between_orders, start_time=start_time)

        if eta_method in ['routed-orders', 'predict-eta', 'predict-eta-optimal']:
            eta = _get_eta(arrival_delay_s, prev_courier_loc, start_time)
            _validate_eta(eta, arrival_delay_s, distance_between_orders, start_time=start_time)


@pytest.mark.parametrize('eta_method', ['routed-orders', 'predict-eta', 'predict-eta-optimal', 'background-threads'])
def test_eta_inside_delivery_radius_not_visited_yet(system_env_with_db, eta_method):
    """
    Test the following workflow (https://st.yandex-team.ru/BBGEO-6340):
        - create two orders (A and B) with non-overlapping auto-delivery radiuses
        - enter the area of A
        - compute ETA and check that ETA of order B is
             "arrival time in orderA delivery radius" + "service duration" + "shared service duration" + "route time from A to B"
    """

    service_duration_s = timedelta(hours=2).seconds
    shared_service_duration_s = timedelta(hours=3).seconds
    total_service_duration_s = service_duration_s + shared_service_duration_s
    distance_between_orders = 1200

    prev_courier_loc = {"lat": 55.733827, "lon": 37.588722}
    orderA_loc = _get_shifted_loc(prev_courier_loc, distance_between_orders)
    orderB_loc = _get_shifted_loc(orderA_loc, distance_between_orders)

    with _create_env(
            system_env_with_db,
            f'test_eta_idrnvo-{eta_method}',
            [orderA_loc, orderB_loc],
            service_duration_s=service_duration_s,
            shared_service_duration_s=shared_service_duration_s) as env:

        def _get_eta(time_s, loc):
            orders = _get_orders_with_eta(env, eta_method, time_s, loc)
            assert [x['status'] for x in orders] == [OrderStatus.new.value, OrderStatus.new.value]
            assert [has_order_history_event(orders[0], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            assert [has_order_history_event(orders[1], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            return dateutil.parser.parse(orders[1]['arrival_time'])

        current_time_s = 0
        _push_position(env, current_time_s, orderA_loc)
        _push_position(env, current_time_s + (total_service_duration_s / 3), orderA_loc)

        eta = _get_eta(current_time_s + (total_service_duration_s / 3), orderA_loc)
        _validate_eta(eta, current_time_s + total_service_duration_s, distance_between_orders)


@pytest.mark.parametrize('eta_method', ['routed-orders', 'predict-eta', 'predict-eta-optimal', 'background-threads'])
@pytest.mark.parametrize('arrival_before_window_start, window_start_before_current_time',
                         [(True, True), (True, False), (False, True)])
def test_eta_not_finished_delivery_time_eta_type(system_env_with_db, eta_method, arrival_before_window_start, window_start_before_current_time):
    """
    Test the following workflow (https://st.yandex-team.ru/BBGEO-6340):
        - create two orders (A and B) with non-overlapping auto-delivery radiuses
        - enter the area of A, do not mark orders as finished
        - check that ETA of order B is
            * "arrival time in orderA delivery radius" + "service duration" + "shared service duration" + "route time from A to B"
              if A.window_start <= A.arrival_time <= current_time
            * "time window start" + "service duration" + "shared service duration" + "route time from A to B"
              if A.arrival_time <= A.window_start
    """

    service_duration_s = timedelta(hours=3).seconds
    shared_service_duration_s = timedelta(hours=2).seconds
    total_service_duration_s = service_duration_s + shared_service_duration_s
    distance_between_orders = 1200
    prev_courier_loc = {"lat": 55.733827, "lon": 37.588722}
    orderA_loc = _get_shifted_loc(prev_courier_loc, distance_between_orders)
    orderB_loc = _get_shifted_loc(orderA_loc, distance_between_orders)
    with _create_env(
            system_env_with_db,
            f'test_eta_nfdtet-{eta_method}-{arrival_before_window_start}-{window_start_before_current_time}',
            [orderA_loc, orderB_loc],
            eta_type=EtaType.delivery_time.value,
            service_duration_s=service_duration_s,
            time_intervals=["11:00-14:00", "17:00-20:00"],
            shared_service_duration_s=shared_service_duration_s) as env:

        def _get_eta_and_time_widow_start(time_s, loc):
            orders = _get_orders_with_eta(env, eta_method, time_s, loc)
            assert [x['status'] for x in orders] == [OrderStatus.new.value, OrderStatus.new.value]
            assert [has_order_history_event(orders[0], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            assert [has_order_history_event(orders[1], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            return dateutil.parser.parse(orders[1]['arrival_time']), dateutil.parser.parse(orders[0]['time_window']['start'])

        if arrival_before_window_start:
            _push_position(env, 0, orderA_loc)  # 07:00
        else:
            arrival_time_shift_s = timedelta(hours=5).seconds
            _push_position(env, arrival_time_shift_s, orderA_loc)  # 12:00

        if window_start_before_current_time:
            current_time_shift_s = timedelta(hours=6).seconds  # 13:00
        else:
            current_time_shift_s = timedelta(hours=2).seconds  # 9:00

        _push_position(env, current_time_shift_s, orderA_loc)
        eta, tw_start = _get_eta_and_time_widow_start(current_time_shift_s, orderA_loc)
        min_route_time_s, max_route_time_s = _get_min_max_route_time_s(distance_between_orders)

        if arrival_before_window_start:
            assert tw_start + timedelta(seconds=total_service_duration_s + min_route_time_s) <= eta \
                <= tw_start + timedelta(seconds=total_service_duration_s + max_route_time_s)
        else:
            assert _offset_datetime(arrival_time_shift_s) + timedelta(seconds=total_service_duration_s + min_route_time_s) <= eta \
                <= _offset_datetime(arrival_time_shift_s) + timedelta(seconds=total_service_duration_s + max_route_time_s)


@pytest.mark.parametrize('eta_method,routing_mode', itertools.product(['routed-orders', 'predict-eta', 'predict-eta-optimal', 'background-threads'], ['driving', 'walking']))
def test_eta_routing_modes(system_env_with_db, eta_method, routing_mode):
    """
    Tests that specifying routing mode in route parameters
    does influence the predicted ETA values.
    """

    service_duration_s = timedelta(hours=2).seconds
    shared_service_duration_s = timedelta(hours=3).seconds
    total_service_duration_s = service_duration_s + shared_service_duration_s
    distance_between_orders = 1200

    orderA_loc = {"lat": 55.733827, "lon": 37.588722}
    orderB_loc = _get_shifted_loc(orderA_loc, distance_between_orders)

    with _create_env(
            system_env_with_db,
            f'test_eta_routing_modes-{eta_method}-{routing_mode}',
            [orderA_loc, orderB_loc],
            service_duration_s=service_duration_s,
            shared_service_duration_s=shared_service_duration_s,
            routing_mode=routing_mode) as env:

        def _get_eta(time_s, loc):
            orders = _get_orders_with_eta(env, eta_method, time_s, loc)
            assert [x['status'] for x in orders] == [OrderStatus.finished.value, OrderStatus.new.value]
            assert [has_order_history_event(orders[0], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [True, True, False]
            assert [has_order_history_event(orders[1], x) for x in ['ARRIVAL', 'ORDER_VISIT', 'DEPARTURE']] == [False, False, False]
            return dateutil.parser.parse(orders[1]['arrival_time'])

        _push_position(env, 0, orderA_loc)

        # Finish order A
        current_time_s = total_service_duration_s
        _push_position(env, current_time_s, orderA_loc)

        eta = _get_eta(current_time_s, orderA_loc)
        _validate_eta(eta, current_time_s, distance_between_orders, routing_mode=routing_mode)


@pytest.mark.parametrize('router_category', ['auto', 'main', 'global'])
def test_eta_router_category(system_env_with_db, router_category):
    """
    Tests that provided route category is handled properly.
    """
    patch_company(system_env_with_db, {'router_category': router_category})

    service_duration_s = timedelta(hours=2).seconds
    shared_service_duration_s = timedelta(hours=3).seconds
    total_service_duration_s = service_duration_s + shared_service_duration_s

    orderA_loc = {"lat": 55.733827, "lon": 37.588722}

    with _create_env(
            system_env_with_db,
            f'test_eta_router_category-{router_category}',
            [orderA_loc],
            service_duration_s=service_duration_s,
            shared_service_duration_s=shared_service_duration_s) as env:
        _push_position(env, 0, orderA_loc)

        current_time_s = total_service_duration_s
        _push_position(env, current_time_s, orderA_loc)

        assert _get_orders_with_eta(env, 'background-threads', current_time_s, orderA_loc)
