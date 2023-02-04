import sys
import requests
from datetime import datetime
from collections import namedtuple
import pytz
import pytest
import time
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request,
    env_patch_request,
    api_path_with_company_id,
    generate_numbers,
    actual_time_interval,
    cleanup_state,
    push_positions,
    create_depot,
    create_courier,
    create_route,
    create_orders,
    create_tmp_company,
    create_tmp_users,
)
from ya_courier_backend.models import UserRole

POSITION = (55.736294, 37.582708)
TIME_ZONE = pytz.timezone('Europe/Moscow')

EnvUserDepot = namedtuple("EnvUserDepot", "dbenv users depots couriers routes orders")


@pytest.fixture(scope='module')
def env_with_user_depot(system_env_with_db):
    """
    users[0]: manager without access to any depots
    users[1]: manager with access to depot2
    users[2]: admin from the same company as user1, user2 and depot1, depot2
    users[3]: admin from the other company
    users[4]: app from the same company
    users[5]: app from the other company
    users[6]: dispatcher without access to any depots
    users[7]: dispatcher with access to depot2
    """
    env = system_env_with_db
    with create_tmp_company(env, "Test company user_depot_access") as company_id2:
        user_roles = [
            UserRole.manager, UserRole.manager,
            UserRole.admin, UserRole.admin,
            UserRole.app, UserRole.app,
            UserRole.dispatcher, UserRole.dispatcher
        ]
        company_ids = [
            env.company_id, env.company_id,
            env.company_id, company_id2,
            env.company_id, company_id2,
            env.company_id, env.company_id
        ]
        with create_tmp_users(env, company_ids, user_roles) as users:
            try:
                courier_num1, depot_num1, route_num1, order_num1 = generate_numbers('user_depot_access')
                courier_num2, depot_num2, route_num2, order_num2 = generate_numbers('user_depot_access2')

                cleanup_state(env, courier_num1, route_num1, depot_num1)
                cleanup_state(env, courier_num2, route_num2, depot_num2)

                depot1 = create_depot(env, depot_num1, {'name': 'depot1'}, env.company_id)
                depot2 = create_depot(env, depot_num2, {'name': 'depot2'}, env.company_id)

                courier1 = create_courier(env, courier_num1)
                courier2 = create_courier(env, courier_num2)

                route1 = create_route(env, route_num1, courier1['id'], depot1['id'],
                                      route_date=datetime.now(TIME_ZONE).date().isoformat())
                route2 = create_route(env, route_num2, courier2['id'], depot2['id'],
                                      route_date=datetime.now(TIME_ZONE).date().isoformat())

                orders = []
                orders.extend(create_orders(
                    env,
                    order_num1, route_id=route1['id'],
                    order_locations=[{'lat': POSITION[0], 'lon': POSITION[1]}],
                    time_intervals=[actual_time_interval()]))
                orders.extend(create_orders(
                    env,
                    order_num2, route_id=route2['id'],
                    order_locations=[{'lat': POSITION[0], 'lon': POSITION[1]}],
                    time_intervals=[actual_time_interval()]))

                push_positions(env, courier1['id'], route1['id'], [(POSITION[0], POSITION[1], time.time())])
                push_positions(env, courier2['id'], route2['id'], [(POSITION[0], POSITION[1], time.time())])

                env_patch_request(
                    env,
                    '{}/user_depot/{}'.format(api_path_with_company_id(env), users[1]['id']),
                    data=[depot2['id']],
                    auth=env.auth_header_super)
                env_patch_request(
                    env,
                    '{}/user_depot/{}'.format(api_path_with_company_id(env), users[7]['id']),
                    data=[depot2['id']],
                    auth=env.auth_header_super)

                yield EnvUserDepot(dbenv=env, users=users, depots=[depot1, depot2], couriers=[courier1, courier2], routes=[route1, route2], orders=orders)

            finally:
                cleanup_state(env, courier_num1, route_num1, depot_num1)
                cleanup_state(env, courier_num2, route_num2, depot_num2)


FilterItems = namedtuple('FilterItems', ['known_items', 'filter_by'])


def get_filter_items(env):
    return {
        'depot': FilterItems(set([i['number'] for i in env.depots]), 'number'),
        'courier': FilterItems(set([i['number'] for i in env.couriers]), 'courier_number'),
        'courier_id': FilterItems(set([i['id'] for i in env.couriers]), 'courier_id'),
        'id': FilterItems(set([i['id'] for i in env.couriers]), 'id'),
        'route_number': FilterItems(set([i['number'] for i in env.routes]), 'route_number'),
        'route': FilterItems(set([i['number'] for i in env.routes]), 'number'),
        'order': FilterItems(set([i['number'] for i in env.orders]), ''),
    }


def perform_tests(env_with_user_depot, handler, tests, item_type):
    filter_items = get_filter_items(env_with_user_depot)[item_type]
    for test_index, test in enumerate(tests):
        for index, user in enumerate(env_with_user_depot.users):
            path = api_path_with_company_id(env_with_user_depot.dbenv, company_id=env_with_user_depot.dbenv.company_id, path=test['args'])
            test_params = f"handler: {handler}, args: {test['args']}, test_index={test_index}, user_index={index}, role={user['role']}"
            print(f"{test_params}", file=sys.stderr, flush=True)

            if test.get('pager', False):
                items = []
                for page in range(1, 100):
                    response = env_get_request(
                        env_with_user_depot.dbenv,
                        path=path + "&page={}".format(page),
                        caller=user)
                    assert response.status_code == test['status'][index], f"{test_params}"
                    if response.status_code != requests.codes.ok or len(response.json()) == 0:
                        break
                    items.extend(response.json())
            else:
                response = env_get_request(
                    env_with_user_depot.dbenv,
                    path=path,
                    caller=user)
                assert response.status_code == test['status'][index], f"{test_params}"
                items = response.json()

            print("response {}, total: {}".format(
                items[:5] if isinstance(items, list) else items,
                len(items)),
                file=sys.stderr
            )
            if 'length' in test and test['length'][index] is not None:
                # this filter is a hack to run test on the dirty database
                if isinstance(items, list):
                    items = [d for d in items if d[filter_items.filter_by] in filter_items.known_items]
                elif isinstance(items, dict):
                    items = dict([(k, v) for k, v in items.items() if v in filter_items.known_items])
                print("items {}".format(items), file=sys.stderr)
                sys.stderr.flush()
                assert len(items) == test['length'][index], f"{test_params}"


def test_courier_report(env_with_user_depot):
    today = datetime.now(TIME_ZONE).date().strftime("%F")
    tests = [
        {
            'args': {'date': today},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 2, None, None, None, 0, 1]
        },
        {
            'args': {'date': today, 'depot_id': env_with_user_depot.depots[0]['id']},
            'status': [requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden],
            'length': [None, None, 1, None, None, None, None, None]
        },
        {
            'args': {'date': today, 'depot_id': env_with_user_depot.depots[1]['id']},
            'status': [requests.codes.forbidden, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.ok],
            'length': [None, 1, 1, None, None, None, None, 1]
        },
    ]
    handler = 'courier-quality'
    for test in tests:
        test['args'] = handler + "?" + "&".join("{}={}".format(k, v) for k, v in test['args'].items())
    perform_tests(env_with_user_depot, handler, tests, item_type='courier')


def test_courier_position(env_with_user_depot):
    handler = 'courier-position'
    tests = [
        {
            'args': {'courier_id': env_with_user_depot.couriers[0]['id'], 'route_id': env_with_user_depot.routes[0]['id']},
            'status': [requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden],
            'length': [None, None, 1, None, None, None, None, None]
        },
        {
            'args': {'courier_id': env_with_user_depot.couriers[1]['id'], 'route_id': env_with_user_depot.routes[1]['id']},
            'status': [requests.codes.forbidden, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.ok],
            'length': [None, 1, 1, None, None, None, None, 1]
        },
    ]
    for test in tests:
        test['args'] = handler + "/{courier_id}/routes/{route_id}".format(**test['args'])
    perform_tests(env_with_user_depot, handler, tests, item_type='courier_id')


def test_courier_position_list(env_with_user_depot):
    handler = 'courier-position'
    tests = [
        {
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 2, None, None, None, 0, 1]
        },
    ]
    for test in tests:
        test['args'] = handler
    perform_tests(env_with_user_depot, handler, tests, item_type='id')


def test_depots_list(env_with_user_depot):
    handler = 'depots'
    tests = [
        {
            'args': {},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 2, None, None, None, None, None]
        },
        {
            'args': {'number': env_with_user_depot.depots[0]['number']},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 0, 1, None, None, None, None, None]
        },
        {
            'args': {'number': env_with_user_depot.depots[1]['number']},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 1, None, None, None, None, None]
        },
    ]
    for test in tests:
        test['args'] = handler + "?" + "&".join("{}={}".format(k, v) for k, v in test['args'].items())
    perform_tests(env_with_user_depot, handler, tests, item_type='depot')


def test_order_details(env_with_user_depot):
    handler = 'order-details'
    today = datetime.now(TIME_ZONE).date().strftime("%F")
    tests = [
        {
            'args': {'date': today},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 2, None, 2, None, 0, 1]
        },
        {
            'args': {'date': today, 'depot_id': env_with_user_depot.depots[0]['id']},
            'status': [requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden],
            'length': [None, None, 1, None, 1, None, None, None]
        },
        {
            'args': {'date': today, 'depot_id': env_with_user_depot.depots[1]['id']},
            'status': [requests.codes.forbidden, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.ok],
            'length': [None, 1, 1, None, 1, None, None, 1]
        },
        {
            'args': {'date': today, 'order_number': env_with_user_depot.orders[0]['number']},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 0, 1, None, 1, None, 0, 0]
        },
        {
            'args': {'date': today, 'order_number': env_with_user_depot.orders[1]['number']},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 1, None, 1, None, 0, 1]
        },
    ]
    for test in tests:
        test['args'] = handler + "?" + "&".join("{}={}".format(k, v) for k, v in test['args'].items())
    perform_tests(env_with_user_depot, handler, tests, item_type='route_number')


def test_route_details(env_with_user_depot):
    handler = 'route-details'
    today = datetime.now(TIME_ZONE).date().strftime("%F")
    tests = [
        {
            'args': {'date': today},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 2, None, None, None, 0, 1]
        },
        {
            'args': {'date': today, 'courier_number': env_with_user_depot.couriers[0]['number']},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 0, 1, None, None, None, 0, 0]
        },
        {
            'args': {'date': today, 'courier_number': env_with_user_depot.couriers[1]['number']},
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.forbidden, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 1, None, None, None, 0, 1]
        },
    ]
    for test in tests:
        test['args'] = handler + "?" + "&".join("{}={}".format(k, v) for k, v in test['args'].items())
    perform_tests(env_with_user_depot, handler, tests, item_type='route_number')


def test_routes(env_with_user_depot):
    handler = 'routes'
    today = datetime.now(TIME_ZONE).date().strftime("%F")
    tests = [
        {
            'args': {},
            'pager': True,
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 2, None, 0, None, 0, 1]
        },
        {
            'args': {'date': today},
            'pager': True,
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 2, None, 0, None, 0, 1]
        },
        {
            'args': {'date': today, 'number': env_with_user_depot.routes[0]['number']},
            'pager': True,
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 0, 1, None, 0, None, 0, 0]
        },
        {
            'args': {'date': today, 'number': env_with_user_depot.routes[1]['number']},
            'pager': True,
            'status': [requests.codes.ok, requests.codes.ok,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.forbidden,
                       requests.codes.ok, requests.codes.ok],
            'length': [0, 1, 1, None, 0, None, 0, 1]
        },
    ]
    for test in tests:
        test['args'] = handler + "?" + "&".join("{}={}".format(k, v) for k, v in test['args'].items())
    perform_tests(env_with_user_depot, handler, tests, item_type='route')
