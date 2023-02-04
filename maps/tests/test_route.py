import random
from datetime import datetime, timedelta, date
import dateutil.tz

import pytest
import requests

from ya_courier_backend.util.db_errors import CONSISTENCY_ERROR_MESSAGE
from maps.b2bgeo.libs.py_flask_utils.format_util import parse_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_patch_request, env_get_request, env_post_request, env_delete_request,
    api_path_with_company_id, cleanup_courier, cleanup_depot, cleanup_route,
    cleanup_state, create_route_env, create_route_envs,
    generate_numbers, create_courier, create_depot, query_routed_orders,
    check_response_with_db_error, current_sms_count, push_positions,
)


URI = 'routes'
ID = None
COURIER_ID = None
DEPOT_ID = None

TEST_COURIER = {
    "name": "Flash",
    "number": "TEST_ROUTE_COURIER",
    "sms_enabled": False
}

TEST_DEPOT = {
    'number': 'TEST_ROUTE_DEPOT',
    'name': 'Склад 1',
    'address': 'ул. Льва Толстого, 16',
    'time_interval': '10:00 - 22:00',
    'lat': 55.7447,
    'lon': 37.6728,
    'description': 'курьерский подъезд',
    'service_duration_s': 600,
    'order_service_duration_s': 10
}

ENTITY = {
    'number': str(random.randint(1000, 10000)),
    'courier_number': 'TEST_ROUTE_COURIER',
    'depot_number': 'TEST_ROUTE_DEPOT',
    'date': '2017-07-22',
}

ENTITY_BAD_VALUES = {
    'date': 12,
    'imei': 'Some',
    'route_start': 100,
}

NEW_ENTITY = ENTITY.copy()

ENTITY2 = ENTITY.copy()
ENTITY2.update({'number': '23423-2000'})

EXTERNAL_FK = ['courier_number', 'depot_number']
INTERNAL_FK = ['courier_id', 'depot_id']


def test_modify_route_date(system_env_with_db):
    env = system_env_with_db
    with create_route_env(env, "route_test") as route_env:
        route = route_env['route']
        data = {
            'number': route['number'],
            'date': '2017-01-01',
        }
        response = env_patch_request(
            env,
            '{}/{}/{}'.format(api_path_with_company_id(env), URI, route['id']),
            data=data)
        assert response.status_code == requests.codes.unprocessable

        data = {
            'number': route['number'],
            'date': route['date'],
        }
        response = env_patch_request(
            env,
            '{}/{}/{}'.format(api_path_with_company_id(env), URI, route['id']),
            data=data)
        assert response.ok


def test_modify_date_in_empty_route(system_env_with_db):
    env = system_env_with_db
    with create_route_env(env, "route_test") as route_env:
        route = route_env['route']
        orders = route_env['orders']
        for order in orders:
            env_delete_request(env, '{}/{}/{}'.format(api_path_with_company_id(env), "orders", order['id']))
        data = {
            'number': route['number'],
            'date': '2019-05-09',
        }
        response = env_patch_request(
            env,
            '{}/{}/{}'.format(api_path_with_company_id(env), URI, route['id']),
            data=data)
        assert response.ok

        response = env_get_request(
            env,
            '{}/{}/{}'.format(api_path_with_company_id(env), URI, route['id']))
        assert response.ok
        j = response.json()
        assert j['date'] == '2019-05-09'


@pytest.fixture(scope='module')
def company_courier_env(system_env_with_db):
    courier_num, depot_num, route_num, order_num = generate_numbers(f'test_route_start_finish_{random.randint(1000, 1000000000)}')

    env = {}
    try:
        cleanup_state(system_env_with_db, courier_num, route_num, depot_num)

        env['courier'] = create_courier(system_env_with_db, courier_num)
        env['depot'] = create_depot(system_env_with_db, depot_num)
        env['route_number'] = route_num

        yield env
    finally:
        cleanup_state(system_env_with_db, courier_num, route_num, depot_num)


@pytest.mark.parametrize("field", ['route_start', 'route_finish'])
def test_create_route_with_start_and_finish_fail(system_env_with_db, company_courier_env, field):
    """
    Try create route with invalid route_start, route_finish
    """
    route_data = {
        'number': f'{company_courier_env["route_number"]}-{field}_fail',
        'courier_id': f'{company_courier_env["courier"]["id"]}-{field}',
        'depot_id': f'{company_courier_env["depot"]["id"]}-{field}',
        'date': '2019-07-23',
        field: 'xxx1.10:11:12',
    }

    response = env_post_request(
        system_env_with_db,
        '{}/routes'.format(api_path_with_company_id(system_env_with_db)),
        data=route_data)
    assert response.status_code == requests.codes.unprocessable

    response = env_post_request(
        system_env_with_db,
        '{}/routes-batch'.format(api_path_with_company_id(system_env_with_db)),
        data=[route_data])
    assert response.status_code == requests.codes.unprocessable


def test_route_finish_is_greater_than_start(system_env_with_db, company_courier_env):
    first = '0.20:00:00'
    second = '1.10:00:00'
    route_data = {
        'date': '2020-03-03',
        'route_start': second,
        'route_finish': first,
        'courier_id': company_courier_env['courier']['id'],
        'depot_id': company_courier_env['depot']['id'],
        'number': f'{company_courier_env["route_number"]}-01',
    }

    response = env_post_request(
        system_env_with_db,
        '{}/routes'.format(api_path_with_company_id(system_env_with_db)),
        data=route_data)
    assert response.status_code == requests.codes.unprocessable
    check_response_with_db_error(response.json()['message'], CONSISTENCY_ERROR_MESSAGE)

    response = env_post_request(
        system_env_with_db,
        '{}/routes-batch'.format(api_path_with_company_id(system_env_with_db)),
        data=[route_data])
    assert response.status_code == requests.codes.unprocessable
    check_response_with_db_error(response.json()['message'], CONSISTENCY_ERROR_MESSAGE)

    route_data['route_start'] = first
    route_data['route_finish'] = second
    response = env_post_request(
        system_env_with_db,
        '{}/routes'.format(api_path_with_company_id(system_env_with_db)),
        data=route_data)
    assert response.ok, response.text
    route_id = response.json()['id']

    response = env_patch_request(
        system_env_with_db,
        '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route_id),
        data={'route_start': second, 'route_finish': first})
    assert response.status_code == requests.codes.unprocessable
    check_response_with_db_error(response.json()['message'], CONSISTENCY_ERROR_MESSAGE)

    response = env_patch_request(
        system_env_with_db,
        '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route_id),
        data={'route_start': first, 'route_finish': second})
    assert response.ok

    response = env_delete_request(
        system_env_with_db,
        '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), response.json()['id']))
    assert response.ok


def test_create_route_with_start_and_finish_ok(system_env_with_db, company_courier_env):
    """
    Create route with valid route_start and route_finish
    Delete route
    """
    route_data = {
        'number': f'{company_courier_env["route_number"]}-02',
        'courier_id': company_courier_env['courier']['id'],
        'depot_id': company_courier_env['depot']['id'],
        'date': '2019-07-23',
        'route_start': '1.10:11:12',
        'route_finish': '2.03:10:55',
    }

    response = env_post_request(
        system_env_with_db,
        '{}/routes'.format(api_path_with_company_id(system_env_with_db)),
        data=route_data)

    assert response.ok
    j = response.json()
    assert j['route_start'] == route_data['route_start']
    assert j['route_finish'] == route_data['route_finish']

    response = env_delete_request(
        system_env_with_db,
        '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), j['id']))
    assert response.ok
    j = response.json()
    assert j['route_start'] == route_data['route_start']
    assert j['route_finish'] == route_data['route_finish']


def test_get_route_with_start_and_finish(system_env_with_db):
    """
    Get route with route_start and route_finish by number and id; check route_start and route_finish
    """
    route_start = '1.10:11:12'
    route_finish = '2.03:10:55'
    with create_route_env(system_env_with_db, "test_get_route_with_start_and_finish", route_start=route_start, route_finish=route_finish) as route_env:
        route = route_env['route']
        response = env_get_request(
            system_env_with_db,
            '{}/routes?number={}'.format(api_path_with_company_id(system_env_with_db), route['number']))

        assert response.ok
        j = response.json()
        assert j[0]['route_start'] == route_start
        assert j[0]['route_finish'] == route_finish

        response = env_get_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']))

        assert response.ok
        j = response.json()
        assert j['route_start'] == route_start
        assert j['route_finish'] == route_finish


@pytest.mark.parametrize("field", ["route_start", "route_finish"])
def test_patch_route_with_start_and_finish(system_env_with_db, field):
    """
    Patch route with route_start and route_finish by number and id; check route_start and route_finish
    """
    with create_route_env(system_env_with_db, f"test_patch_route_with_start_and_finish-{field}") as route_env:
        route = route_env['route']

        # set invalid
        response = env_patch_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']),
            data={field: 'xxx'})
        j = response.json()
        assert response.status_code == requests.codes.unprocessable

        # set negative
        response = env_patch_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']),
            data={field: '-1.10:22:00'})
        assert response.status_code == requests.codes.unprocessable
        assert 'Invalid time format' in response.json()['message']

        # set valid
        new_value = '1.10:22:00'
        response = env_patch_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']),
            data={field: new_value})

        assert response.ok
        j = response.json()
        assert j[field] == new_value

        response = env_get_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']))

        assert response.ok
        j = response.json()
        assert j[field] == new_value

        # set other field
        response = env_patch_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']),
            data={'date': j['date']})

        assert response.ok
        j = response.json()
        assert j[field] == new_value

        response = env_get_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']))

        assert response.ok
        j = response.json()
        assert j[field] == new_value

        # set None
        response = env_patch_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']),
            data={field: None})

        assert response.ok
        j = response.json()
        assert j[field] is None

        response = env_get_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']))

        assert response.ok
        j = response.json()
        assert j[field] is None

        # set '00:00:00'
        new_route_start = '00:00:00'
        response = env_patch_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']),
            data={field: new_route_start})

        assert response.ok
        j = response.json()
        assert j[field] == new_route_start

        response = env_get_request(
            system_env_with_db,
            '{}/routes/{}'.format(api_path_with_company_id(system_env_with_db), route['id']))

        assert response.ok
        j = response.json()
        assert j[field] == new_route_start


def test_routes_filter_by_depot_id(system_env_with_db):
    """
    Test the following workflow:
        - create 2 route envs
        - get /routes with depot_id and date filter
            * check: response list consists of routes corresponding to depot_id and date filter
    """
    route_envs_number = 2
    route_date = date(2019, 12, 16)
    route_dates = [route_date.isoformat()] * route_envs_number
    next_day = route_date + timedelta(days=1)
    time_intervals_list = [None] * route_envs_number

    with create_route_envs(system_env_with_db,
                           "test_routes_filter_by_depot_id",
                           time_intervals_list=time_intervals_list,
                           route_dates=route_dates) as route_envs:
        non_existing_id = 99999999

        for depot_id, query_date, expected_routes_ids in [
                (route_envs[0]["depot"]["id"], route_date, [route_envs[0]["route"]["id"]]),
                (route_envs[1]["depot"]["id"], route_date, [route_envs[1]["route"]["id"]]),
                (route_envs[0]["depot"]["id"], next_day, []),
                (route_envs[1]["depot"]["id"], next_day, []),
                (non_existing_id, route_date, [])]:

            response = env_get_request(
                system_env_with_db,
                '{}/routes?depot_id={}&date={}'.format(
                    api_path_with_company_id(system_env_with_db),
                    depot_id,
                    query_date.isoformat()
                )
            )

            assert response.status_code == requests.codes.ok
            routes_ids = [route["id"] for route in response.json()]
            assert routes_ids == expected_routes_ids


def _create_smses_and_tracking_tokens(system_env_with_db, route_date, route_envs):
    sms_count1 = current_sms_count(system_env_with_db, route_date)
    current_location = {
        'lat': route_envs[0]['orders'][0]['lat'],
        'lon': route_envs[0]['orders'][0]['lat'],
        'time_now': '08:01'
    }
    for route_env in route_envs:
        push_positions(
            system_env_with_db,
            route_env["courier"]["id"],
            route_env["route"]["id"],
            track=[(current_location['lat'], current_location['lon'], (route_date + parse_time(current_location['time_now'])).timestamp())])
        query_routed_orders(system_env_with_db, route_env["courier"]["id"],
                            route_env["route"]["id"], current_location)

    sms_count2 = current_sms_count(system_env_with_db, route_date)
    assert sms_count1 + len(route_envs) * \
        len(route_envs[0]['orders']) == sms_count2


@skip_if_remote
def test_delete_multiple_routes(system_env_with_db):
    route_envs_number = 3
    route_date = date(2019, 12, 16)
    TIMEZONE_MSK = dateutil.tz.gettz('Europe/Moscow')
    route_date = datetime.now(tz=TIMEZONE_MSK)
    route_dates = [route_date.date().isoformat()] * route_envs_number
    time_intervals_list = [None] * route_envs_number

    with create_route_envs(system_env_with_db,
                           "test_remove_multiple_routes",
                           time_intervals_list=time_intervals_list,
                           route_dates=route_dates) as route_envs:
        _create_smses_and_tracking_tokens(
            system_env_with_db, route_date, route_envs)

        # No data
        response = env_delete_request(
            system_env_with_db,
            f'{api_path_with_company_id(system_env_with_db)}/routes',
            data=None)
        assert response.status_code == requests.codes.bad_request

        # invalid data
        response = env_delete_request(
            system_env_with_db,
            f'{api_path_with_company_id(system_env_with_db)}/routes',
            data={"id1": 1, "id2": 2})
        assert response.status_code == requests.codes.unprocessable_entity
        assert "Json schema validation failed: RouteDeleteIds" in response.text

        # empty list
        response = env_delete_request(
            system_env_with_db,
            f'{api_path_with_company_id(system_env_with_db)}/routes',
            data=[])
        assert response.status_code == requests.codes.ok
        assert response.json() == []

        # duplicates
        response = env_delete_request(
            system_env_with_db,
            f'{api_path_with_company_id(system_env_with_db)}/routes',
            data=[route_envs[0]["route"]["id"], route_envs[0]["route"]["id"]])
        assert response.status_code == requests.codes.unprocessable_entity
        assert f"Duplicate route IDs: [{route_envs[0]['route']['id']}]" in response.text

        # non_existing
        non_existing_id = 99999999
        response = env_delete_request(
            system_env_with_db,
            f'{api_path_with_company_id(system_env_with_db)}/routes',
            data=[non_existing_id, non_existing_id + 1])
        assert response.status_code == requests.codes.unprocessable_entity
        assert "Specified route id" in response.text and "does not exist" in response.text

        # delete one route
        response = env_delete_request(
            system_env_with_db,
            f'{api_path_with_company_id(system_env_with_db)}/routes',
            data=[route_envs[0]['route']['id']])
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert len(j) == 1
        assert j[0]['id'] == route_envs[0]['route']['id']

        # delete two routes
        response = env_delete_request(
            system_env_with_db,
            f'{api_path_with_company_id(system_env_with_db)}/routes',
            data=[route_envs[1]['route']['id'], route_envs[2]['route']['id']])
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert len(j) == 2
        assert {j[0]['id'], j[1]['id']} == {route_envs[1]['route']['id'], route_envs[2]['route']['id']}


@pytest.mark.parametrize('courier_id', [None, 'incorrect_courier_id', 'not_specified'])
@pytest.mark.parametrize('courier_number', [None, 'incorrect_courier_number', 'not_specified'])
def test_fail_on_incorrect_courier_id_and_courier_number(system_env_with_db, courier_id, courier_number):
    depot_number = f'test_fail-{courier_id}-{courier_number}'
    cleanup_depot(system_env_with_db, depot_number)
    depot = create_depot(system_env_with_db, depot_number)
    route = {
        'number': f'test_fail-{courier_id}-{courier_number}',
        'depot_number': depot['number'],
        'date': '2020-08-01'
    }

    if courier_id == 'not_specified':
        assert 'courier_id' not in route
    else:
        route['courier_id'] = courier_id

    if courier_number == 'not_specified':
        assert 'courier_number' not in route
    else:
        route['courier_number'] = courier_number

    cleanup_route(system_env_with_db, route['number'])
    if route.get('courier_number'):
        cleanup_courier(system_env_with_db, route['courier_number'])

    response = env_post_request(
        system_env_with_db,
        path=api_path_with_company_id(system_env_with_db, URI),
        data=route
    )
    assert response.status_code == requests.codes.unprocessable

    response = env_post_request(
        system_env_with_db,
        '{}/routes-batch'.format(api_path_with_company_id(system_env_with_db)),
        data=[route]
    )
    assert response.status_code == requests.codes.unprocessable

    with create_route_env(system_env_with_db, 'route_test_patch_incorrect_courier') as route_env:
        global COURIER_ID
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, URI, route_env['route']['id']),
            data={
                'courier_number': courier_number,
                'courier_id': courier_id,
            }
        )
        assert response.status_code == requests.codes.unprocessable


def test_routing_mode(system_env_with_db):
    depot_number = 'test_fail_on_incorrect_routing_mode-depot_number'
    cleanup_depot(system_env_with_db, depot_number)
    depot = create_depot(system_env_with_db, depot_number)
    route = {
        'number': 'test_fail_on_incorrect_routing_mode-route_number',
        'depot_number': depot['number'],
        'date': '2020-08-12',
        'routing_mode': 'incorrect_routing_mode'
    }
    cleanup_route(system_env_with_db, route['number'])

    response = env_post_request(
        system_env_with_db,
        path=api_path_with_company_id(system_env_with_db, URI),
        data=route
    )
    assert response.status_code == requests.codes.unprocessable
    assert "Invalid routing mode: 'incorrect_routing_mode'. Valid values: f['driving', 'truck', 'walking', 'transit']" in response.json()['message']

    with create_route_env(system_env_with_db, 'test_routing_mode', routing_mode='walking') as route_env:
        route = route_env['route']
        response = env_get_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, URI, route['id']))
        assert response.ok, response.text
        assert response.json()['routing_mode'] == 'walking'

        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, URI, route['id']),
            data={'routing_mode': 'incorrect_routing_mode'})
        assert response.status_code == requests.codes.unprocessable
        assert "Invalid routing mode: 'incorrect_routing_mode'. Valid values: f['driving', 'truck', 'walking', 'transit']" in response.json()['message']

        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, URI, route['id']),
            data={'routing_mode': 'truck'})
        assert response.ok, response.text
        assert response.json()['routing_mode'] == 'truck'
