import pytest
import json

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    create_route_env,
    create_tmp_company,
    create_tmp_users,
    env_get_request,
    env_patch_request,
    post_order_sequence,
    confirm_order,
    push_positions,
    query_routed_orders)
from datetime import datetime, timedelta
import dateutil.tz
import random
import requests
import dateutil.parser as dt_parser
from urllib.parse import urlencode
from ya_courier_backend.models import UserRole


def _get_test_datetime():
    tz = dateutil.tz.gettz("Europe/Moscow")
    now = datetime.now(tz=tz)
    today = datetime(now.year, now.month, now.day, 9, 0, 0, tzinfo=tz)
    return today + timedelta(hours=random.randint(0, 5))


PREDICT_ETA_URL_BASE = "couriers/{courier}/routes/{route}/predict-eta"


ORDERS_LOCS = [
    {
        'lat': 55.791928,
        'lon': 37.841492,
    },
    {
        'lat': 55.900931,
        'lon': 37.623044,
    }
]


def _set_optimal_order_sequence_enabled(system_env_with_db, optimal_order_sequence_enabled):
    assert env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data={'optimal_order_sequence_enabled': optimal_order_sequence_enabled},
        auth=system_env_with_db.auth_header_super).status_code == requests.codes.ok


def _predict_eta(
        system_env_with_db,
        courier_id,
        route_id,
        time,
        position,
        sequence=None,
        use_optimal_sequence=False,
        expected_status_code=requests.codes.ok):

    params = {
        'lat': position['lat'],
        'lon': position['lon'],
        'time': time,
        'find-optimal': json.dumps(use_optimal_sequence),
    }
    if sequence:
        params['order-sequence'] = ','.join(str(x) for x in sequence)

    url = (PREDICT_ETA_URL_BASE + "?{params}").format(
        courier=courier_id,
        route=route_id,
        params=urlencode(params),
    )
    resp = env_get_request(system_env_with_db, url)
    assert resp.status_code == expected_status_code, (resp.status_code, resp.text)

    eta_data = resp.json()
    return eta_data


def _check_route_eta_is_sane(start_time, eta):
    route_eta = eta['route']
    last_order_eta_dt = dt_parser.parse(route_eta[-1]['arrival_time'])
    assert last_order_eta_dt > start_time
    depot_arrival_time = dt_parser.parse(eta['route_end'])
    assert depot_arrival_time > last_order_eta_dt


def test_predict_eta_validate_schema(system_env_with_db):
    _set_optimal_order_sequence_enabled(system_env_with_db, True)

    order_locs = [
        {
            'lat': 55.900931,
            'lon': 37.623044,
        }
    ]
    INTERVALS = ['08:00-18:00'] * len(order_locs)

    test_date = _get_test_datetime()
    with create_route_env(system_env_with_db, 'test_predict_eta_validate_schema', order_locations=order_locs,
                          time_intervals=INTERVALS, route_date=test_date.date().isoformat()) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        url_base = PREDICT_ETA_URL_BASE.format(courier=courier_id, route=route_id)

        def get_eta_predict_url(params):
            return url_base + '?' + urlencode(params)

        # Case 1: Not enough arguments
        resp = env_get_request(system_env_with_db, url_base)
        assert resp.status_code == requests.codes.unprocessable

        time = test_date.isoformat()

        # Case 2: Invalid location format
        resp = env_get_request(system_env_with_db,
                               path=get_eta_predict_url({'lat': 50.1, 'lon': 'Moscow', 'time': time}))
        assert resp.status_code == requests.codes.unprocessable
        assert "Not a valid float 'Moscow'" in resp.json()['message']

        # Case 3: Partially provided position
        resp = env_get_request(system_env_with_db, path=get_eta_predict_url({'lat': 55.7, 'time': time}))
        assert resp.status_code == requests.codes.unprocessable

        # Case 4: Time provided in an invalid format
        pos = {
            'lat': 55.791928,
            'lon': 37.841492,
        }
        wrong_time = "15:00"
        resp = env_get_request(system_env_with_db,
                               path=get_eta_predict_url({'lat': pos['lat'], 'lon': pos['lon'], 'time': wrong_time}))
        assert resp.status_code == requests.codes.unprocessable
        assert "Not a valid datetime '15:00'" in resp.json()['message']

        # Case 5: Wrong order sequence format
        resp = env_get_request(system_env_with_db,
                               path=get_eta_predict_url({'lat': pos['lat'], 'lon': pos['lon'], 'time': time,
                                                         'order-sequence': 'null'}))
        assert resp.status_code == requests.codes.unprocessable
        assert "Not a valid list of integer values." in resp.json()['message']

        # Case 6: Unknown order ids
        resp = env_get_request(system_env_with_db,
                               path=get_eta_predict_url({'lat': pos['lat'], 'lon': pos['lon'], 'time': time,
                                                         'order-sequence': "4,8, 15, 16, 23,42"}))
        assert resp.status_code == requests.codes.unprocessable
        msg = resp.json()['message']
        assert "Error in query parameter 'order-sequence': Incorrect set of orders provided. Missing route orders:" \
               in msg
        assert f'Missing route orders: {[o["id"] for o in orders]}' in msg
        assert 'non route orders: [4, 8, 42, 15, 16, 23]' in msg

        # Case 7: both 'order-sequence' and 'find-optimal' are specified
        resp = env_get_request(system_env_with_db,
                               path=get_eta_predict_url({'lat': pos['lat'], 'lon': pos['lon'], 'time': time,
                                                         'order-sequence': ','.join([str(o['id']) for o in orders]),
                                                         'find-optimal': 'true'}))
        assert resp.status_code == requests.codes.unprocessable
        assert "the 'order-sequence'' cannot be specified if 'find-optimal' is requested" in resp.json()['message']

        # Case 7.2: both 'order-sequence' and 'find-optimal' are specified, but find-optimal is set to False.
        resp = env_get_request(system_env_with_db,
                               path=get_eta_predict_url({'lat': pos['lat'], 'lon': pos['lon'], 'time': time,
                                                         'order-sequence': ','.join([str(o['id']) for o in orders]),
                                                         'find-optimal': 'false'}))
        assert resp.status_code == requests.codes.ok

        # Case 8: invalid value is specified for 'find-optimal'
        resp = env_get_request(system_env_with_db,
                               path=get_eta_predict_url({'lat': pos['lat'], 'lon': pos['lon'], 'time': time,
                                                         'order-sequence': ','.join([str(o['id']) for o in orders]),
                                                         'find-optimal': 10}))
        assert resp.status_code == requests.codes.unprocessable
        assert "Not a valid boolean '10'" in resp.json()['message']


def test_predict_eta_default_order_sequence(system_env_with_db):
    INTERVALS = ['08:00-18:00'] * len(ORDERS_LOCS)

    request_time = _get_test_datetime()
    with create_route_env(system_env_with_db, 'test_predict_eta_default_order_sequence', order_locations=ORDERS_LOCS,
                          time_intervals=INTERVALS,
                          route_date=request_time.date().isoformat()) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        order_sequence = [o['id'] for o in orders]
        random.shuffle(order_sequence)
        post_order_sequence(system_env_with_db, route_id, order_sequence)
        eta = _predict_eta(system_env_with_db, courier_id, route_id, time=request_time.isoformat(),
                           position={'lat': depot['lat'], 'lon': depot['lon']})

        route_eta = eta['route']
        assert order_sequence == [item['id'] for item in route_eta]
        _check_route_eta_is_sane(request_time, eta)


def test_predict_eta_provided_sequence(system_env_with_db):
    INTERVALS = ['08:00-18:00'] * len(ORDERS_LOCS)

    request_time = _get_test_datetime()
    with create_route_env(system_env_with_db, 'test_predict_eta_provided_sequence', order_locations=ORDERS_LOCS,
                          time_intervals=INTERVALS, route_date=request_time.date().isoformat()) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        order_sequence = [o['id'] for o in orders]
        random.shuffle(order_sequence)
        eta = _predict_eta(system_env_with_db, courier_id, route_id, time=request_time.isoformat(),
                           position={'lat': depot['lat'], 'lon': depot['lon']},
                           sequence=order_sequence)

        route_eta = eta['route']
        assert order_sequence == [item['id'] for item in route_eta]
        _check_route_eta_is_sane(request_time, eta)

        order_sequence.reverse()
        eta = _predict_eta(system_env_with_db, courier_id, route_id, time=request_time.isoformat(),
                           position={'lat': orders[1]['lat'], 'lon': orders[1]['lon']},
                           sequence=order_sequence)

        route_eta = eta['route']
        assert order_sequence == [item['id'] for item in route_eta]
        _check_route_eta_is_sane(request_time, eta)


@skip_if_remote
def test_predict_eta_searches_optimal_sequence(system_env_with_db):
    INTERVALS = ['08:00-18:00'] * len(ORDERS_LOCS)
    request_time = _get_test_datetime()
    with create_route_env(system_env_with_db, 'test_predict_eta_searches_optimal_sequence', order_locations=ORDERS_LOCS,
                          time_intervals=INTERVALS, route_date=request_time.date().isoformat()) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        for idx, order_loc in enumerate(ORDERS_LOCS):
            courier_position = {
                'lat': order_loc['lat'] + 1e-5,
                'lon': order_loc['lon']
            }

            eta = _predict_eta(system_env_with_db, courier_id, route_id, time=request_time.isoformat(),
                               position=courier_position, use_optimal_sequence=False)
            route_eta = eta['route']
            assert [o['id'] for o in orders] == [item['id'] for item in route_eta]
            _check_route_eta_is_sane(request_time, eta)

            # Requesting optimal sequence fails when company's optimal_order_sequence_enabled is False
            _set_optimal_order_sequence_enabled(system_env_with_db, False)
            _predict_eta(system_env_with_db, courier_id, route_id, time=request_time.isoformat(),
                         position=courier_position, use_optimal_sequence=True,
                         expected_status_code=requests.codes.forbidden)

            # Requesting optimal sequence succeeds when company's optimal_order_sequence_enabled is True
            _set_optimal_order_sequence_enabled(system_env_with_db, True)
            eta = _predict_eta(system_env_with_db, courier_id, route_id, time=request_time.isoformat(),
                               position=courier_position, use_optimal_sequence=True)
            expected_order_sequence = [orders[(idx + i) % len(ORDERS_LOCS)]['id'] for i in range(len(ORDERS_LOCS))]
            route_eta = eta['route']
            assert expected_order_sequence == [item['id'] for item in route_eta]
            _check_route_eta_is_sane(request_time, eta)


@skip_if_remote
def test_predict_eta_check_access_rules(env_with_two_companies_and_routes):
    env = env_with_two_companies_and_routes['env']
    users = env_with_two_companies_and_routes['users']
    courier_route_env = env_with_two_companies_and_routes['courier_route_env']
    other_route_env = env_with_two_companies_and_routes['other_route_env']

    courier_id = courier_route_env['courier']['id']
    other_route_id = other_route_env['route']['id']

    url = (PREDICT_ETA_URL_BASE + "?{params}").format(
        courier=courier_id,
        route=other_route_id,
        params=urlencode({
            'lat': ORDERS_LOCS[0]['lat'],
            'lon': ORDERS_LOCS[0]['lon'],
            'time': _get_test_datetime().isoformat(),
        })
    )
    for user in users:
        resp = env_get_request(env, url, caller=user)
        assert resp.status_code == requests.codes.unprocessable


@pytest.fixture(scope='module')
def env_with_two_companies_and_routes(system_env_with_db):
    INTERVALS = ['08:00-18:00'] * len(ORDERS_LOCS)

    company_id = system_env_with_db.company_id
    user_roles = [UserRole.app, UserRole.dispatcher, UserRole.manager, UserRole.admin]
    with create_tmp_users(system_env_with_db, [company_id] * len(user_roles), user_roles) as users:

        # Courier's actual route
        with create_route_env(system_env_with_db, 'test_predict_eta_check_access_rules',
                              order_locations=[{'lat': 50, 'lon': 30}],
                              time_intervals=['08:00-18:00'],
                              route_date=_get_test_datetime().date().isoformat()) as courier_route_env:

            with create_tmp_company(system_env_with_db, "Test company test_predict_eta_check_access_rules")\
                    as other_company_id:
                auth = system_env_with_db.auth_header_super
                with create_route_env(system_env_with_db, "test_predict_eta_access_rules", order_locations=ORDERS_LOCS,
                                      time_intervals=INTERVALS, company_id=other_company_id, auth=auth)\
                        as other_route_env:

                    yield {
                        'env': system_env_with_db,
                        'users': users,
                        'courier_route_env': courier_route_env,
                        'other_company_id': other_company_id,
                        'other_route_env': other_route_env
                    }


def test_use_optimal_sequence_do_not_drop_locations(system_env_with_db):
    INTERVALS = ['08:00-08:10', '08:00-08:10']
    assert len(ORDERS_LOCS) == len(INTERVALS)

    request_time = _get_test_datetime()
    with create_route_env(system_env_with_db, 'test_use_optimal_sequence_do_not_drop_locations', order_locations=ORDERS_LOCS,
                          time_intervals=INTERVALS, route_date=request_time.date().isoformat()) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        # {"hard_window": true} is set for confirmed orders
        confirm_order(system_env_with_db, orders[0])
        confirm_order(system_env_with_db, orders[1])

        _set_optimal_order_sequence_enabled(system_env_with_db, True)
        eta = _predict_eta(system_env_with_db, courier_id, route_id, time=request_time.isoformat(),
                            position={'lat': depot['lat'], 'lon': depot['lon']}, use_optimal_sequence=True)
        assert len(eta["route"]) == len(ORDERS_LOCS)
        assert len([order for order in eta["route"] if 'dropped' in order and order['dropped']]) == 0


def test_no_orders_in_route(system_env_with_db):
    INTERVALS = []

    request_time = _get_test_datetime()
    with create_route_env(system_env_with_db, 'test_no_orders_in_route', order_locations=ORDERS_LOCS,
                          time_intervals=INTERVALS,
                          route_date=request_time.date().isoformat()) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']

        eta = _predict_eta(system_env_with_db, courier_id, route_id, time=request_time.isoformat(),
                           position={'lat': depot['lat'], 'lon': depot['lon']},
                           expected_status_code=requests.codes.ok)

        assert len(eta['route']) == 0


def test_driving_tomorrows_route_bug(system_env_with_db):
    time_zone = 'Europe/Moscow'
    driving_start_datetime = datetime.now(tz=dateutil.tz.gettz(time_zone)).replace(hour=22, minute=45)
    intervals = ['00:01 - 23:59'] * len(ORDERS_LOCS)

    with create_route_env(system_env_with_db, 'test_driving_tomorrows_route',
                          order_locations=ORDERS_LOCS,
                          time_intervals=intervals,
                          route_date=(driving_start_datetime + timedelta(days=1)).date().isoformat(),
                          depot_data={'time_zone': time_zone}) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']

        push_positions(system_env_with_db, courier_id, route_id, track=[
            (depot['lat'], depot['lon'], driving_start_datetime.timestamp()),
            (depot['lat'] + 0.01, depot['lon'] - 0.01, (driving_start_datetime + timedelta(minutes=2)).timestamp())
        ])
        _predict_eta(system_env_with_db, courier_id, route_id,
                     time=(driving_start_datetime + timedelta(minutes=3)).isoformat(),
                     position={'lat': depot['lat'], 'lon': depot['lon']},
                     expected_status_code=requests.codes.unprocessable_entity)

        query_routed_orders(system_env_with_db, courier_id, route_id,
                            expected_status_code=requests.codes.unprocessable_entity,
                            strict=False,
                            point={
                                "lat": depot['lat'] - 0.011,
                                "lon": depot['lon'] + 0.001,
                                "timestamp": (driving_start_datetime + timedelta(minutes=3)).timestamp()
                            })
