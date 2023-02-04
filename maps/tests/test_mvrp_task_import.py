import contextlib

import pytest
import sys
from copy import deepcopy
from datetime import datetime

import requests

import maps.b2bgeo.test_lib.apikey_values as apikey_values
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import (
    system_env, EnvDB, get_passport_accounts, skip_if_remote, MOCK_APIKEYS_CONTEXT, APIKEYS_SERVICE_COUNTER
)
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import (
    mock_async_backend_server,
    get_solution_by_task_id,
    get_existing_depot_numbers,
    solver_request_by_task_id,
    get_all_courier_numbers,
    UNKNOWN_DEPOT_ID,
    init_solutions_by_task_id
)
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    create_company,
    create_courier_depot_env,
    create_depot,
    create_route,
    create_route_env,
    env_get_request,
    env_post_request,
    get_depot,
    get_order_details_by_route_id,
    get_orders,
    get_route_details,
    get_route_info,
    get_user_auth,
    mvrp_task_verify_orders,
    patch_company,
    patch_order_by_order_id,
    push_positions,
    get_order,
    patch_order,
    query_routed_orders
)
from ya_courier_backend.interservice.apikeys.yandex import APIKEYS_SERVICE_TOKEN
from ya_courier_backend.resources.mvrp_task_import import _extract_order_numbers
from maps.b2bgeo.libs.time.py.time_utils import format_str_time_relative


ROUTE_DATE = datetime.now()
ROUTE_TIME_ZONE = 'Europe/Moscow'
GARAGE_COORDINATES = {"lat": 55.664695, "lon": 37.562443}


def _get_type_items(type_name):
    return {'type': type_name, 'types': [type_name]}.items()


def _validate_last_garage(garage, must_be_estimated, is_visited):
    assert not (must_be_estimated and is_visited)
    assert garage['type'] == 'garage'
    assert must_be_estimated == ('estimated_visit_time' in garage['value'])
    assert is_visited == ('visit_time' in garage['value'])


@contextlib.contextmanager
def _create_default_depot_courier_env(env, task_id):
    depot_numbers = get_existing_depot_numbers(task_id)
    to_cleanup_courier_numbers = get_all_courier_numbers(task_id)
    to_create_courier_numbers = list(to_cleanup_courier_numbers)[0:1] if len(to_cleanup_courier_numbers) > 0 else []
    with create_courier_depot_env(env, to_cleanup_courier_numbers, to_create_courier_numbers, depot_numbers) as depot_env:
        assert len(_get_couriers(env)) == len(to_create_courier_numbers)
        yield depot_env


@pytest.fixture(scope='module')
def _system_env_with_db_mocked_mvrp_solver():
    with mock_async_backend_server() as mock_mvrp_uri:
        print(f" system_env_with_db_mocked_mvrp_solver: {mock_mvrp_uri}", file=sys.stderr, flush=True)
        with system_env(mvrp_solver_uri=mock_mvrp_uri) as instance_env:
            env = EnvDB(
                url=instance_env['url'],
                mock_apikeys_url=instance_env['mock_apikeys_url'],
                mock_sender_url=instance_env['mock_sender_url'],
                mock_pipedrive_url=instance_env['mock_pipedrive_url'],
                mock_blackbox_url=instance_env['mock_blackbox_url'],
                company_id=None,
                auth_header="test_user:test_uid",
                auth_header_super="test_user_super:test_uid_super",
                passport_accounts=get_passport_accounts(),
                verify_ssl=False,
                existing=False,
                alternative_url=None
            )
            company_id = create_company(env, "test_user")
            init_solutions_by_task_id(create_company(env, "test_user2", "test_user2"))
            env = env._replace(company_id=company_id)
            patch_company(env, {'apikey': apikey_values.ACTIVE})
            yield env


def _get_billing_counter_value():
    return MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['counters'][APIKEYS_SERVICE_COUNTER]


def _post_mvrp_task_import(
        env,
        task_id,
        extra_query_params=None,
        expected_status_code=requests.codes.ok,
        depot_env=None,
        company_id=None,
        auth=None):

    assert task_id is not None

    query_params = {'task_id': task_id}
    if extra_query_params:
        query_params.update(extra_query_params)
    query_params_str = '&'.join('{}={}'.format(k, v) for k, v in query_params.items())

    path = api_path_with_company_id(env, f'mvrp_task?{query_params_str}', company_id=company_id)
    response = env_post_request(env, path, data=None, auth=auth)
    assert response.status_code == expected_status_code, response.text
    output = response.json()

    if depot_env and response.status_code == requests.codes.ok and isinstance(output, list):
        # tracking created routes to be delete upon teardown
        depot_env['created_route_numbers'] += [route['number'] for route in output if route.get('number')]
    return output


def _verify_import(sys_env, depot_env, imported_routes, solution, expected_order_status):
    couriers = _get_couriers(sys_env)
    assert len(couriers) == len(solution['result']['vehicles'])
    courier_id_to_number = {courier['id']: courier['number'] for courier in couriers}
    vehicles_info = \
    {
        str(vehicle['id']): {
            'imei': vehicle.get('imei'),
            'routing_mode': vehicle.get('routing_mode') or solution['result']['options'].get('routing_mode')
        } for vehicle in solution['result']['vehicles']
    }

    assert isinstance(imported_routes, list)
    solution_routes = solution['result']['routes']
    assert len(imported_routes) == len(solution_routes)
    expected_date = solution['result']['options'].get('date')
    for idx, route in enumerate(imported_routes):
        route_start = solution_routes[idx]["route"][0]['arrival_time_s']
        solution_route = [point for point in solution_routes[idx]["route"] if
                          point['node']['type'] != 'location' or point['node']['value']['type'] != 'garage']

        assert route['date'] == expected_date
        assert route['number'] == solution_routes[idx]['__testing__expected_route_number']
        assert route['company_id'] == sys_env.company_id
        assert route['courier_id'] in courier_id_to_number.keys()
        assert courier_id_to_number[route['courier_id']] == str(solution_routes[idx]['vehicle_id'])
        assert route['depot_id'] in [depot['id'] for depot in depot_env['depots']]
        assert route['route_start'] == format_str_time_relative(route_start)
        assert route['imei'] == vehicles_info[str(solution_routes[idx]['vehicle_id'])]['imei']
        assert route['routing_mode'] == vehicles_info[str(solution_routes[idx]['vehicle_id'])]['routing_mode']
        orders = get_orders(sys_env, route['id'], company_id=sys_env.company_id)
        assert len(solution_route) - 1 == len(orders)
        mvrp_task_verify_orders(orders, solution_route[1:], sys_env.company_id, route['id'], ROUTE_DATE, ROUTE_TIME_ZONE, expected_order_status)


def _get_couriers(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, "couriers")
    )
    response.raise_for_status()
    return response.json()


@skip_if_remote
def test_import_general_task(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'

    for requested_order_status, expected_order_status, expected_status_code in [
        (None, 'new', requests.codes.ok),
        ('new', 'new', requests.codes.ok),
        ('confirmed', 'confirmed', requests.codes.ok),
        ('finished', None, requests.codes.unprocessable),
        ('blah-blah-blah', None, requests.codes.unprocessable),
    ]:
        start_billing_value = _get_billing_counter_value()
        with _create_default_depot_courier_env(system_env, task_id) as depot_env:
            imported_routes = _post_mvrp_task_import(
                system_env,
                task_id=task_id,
                extra_query_params=None if requested_order_status is None else {'initial-status': requested_order_status},
                expected_status_code=expected_status_code,
                depot_env=depot_env)
            if expected_status_code != requests.codes.ok:
                continue
            _verify_import(system_env, depot_env, imported_routes, get_solution_by_task_id(task_id), expected_order_status)
            assert _get_billing_counter_value() == start_billing_value


@skip_if_remote
@pytest.mark.parametrize("task_id", [
    'mock_task_uuid__generic_with_two_depots',
    'mock_task_uuid__generic_with_two_depots_and_garage',
])
def test_first_depot_has_estimated_time_only_after_first_push_positions(_system_env_with_db_mocked_mvrp_solver, task_id):
    system_env = _system_env_with_db_mocked_mvrp_solver
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        patch_company(_system_env_with_db_mocked_mvrp_solver, {'import_depot_garage': True})
        [route] = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        assert route_info['nodes'][0].items() >= _get_type_items('depot')
        assert route_info['nodes'][3].items() >= _get_type_items('depot')
        assert all('estimated_service_time' not in node['value'] for node in route_info['nodes'])

        if task_id == 'mock_task_uuid__generic_with_two_depots_and_garage':
            _validate_last_garage(route_info['nodes'][4], must_be_estimated=False, is_visited=False)

        # make ycb update eta
        locations = [(55.999, 37.729, route_info['nodes'][1]['value']['time_windows'][0]['start']['text'])]
        push_positions(system_env, route["courier_id"], route["id"], locations)
        query_routed_orders(system_env, route["courier_id"], route["id"])

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        assert route_info['nodes'][0].items() >= _get_type_items('depot')
        assert route_info['nodes'][3].items() >= _get_type_items('depot')
        assert set(route_info['nodes'][0]['value']) >= {'id', 'estimated_service_time', 'point'}
        assert all('estimated_service_time' not in node['value'] for node in route_info['nodes'][1:])


@skip_if_remote
@pytest.mark.parametrize("task_id", [
    'mock_task_uuid__generic_with_two_depots',
    'mock_task_uuid__generic_with_two_depots_and_garage',
])
def test_orders_and_last_depot_have_estimated_time_only_after_first_depot_departure(
    _system_env_with_db_mocked_mvrp_solver, task_id
):
    system_env = _system_env_with_db_mocked_mvrp_solver
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        patch_company(_system_env_with_db_mocked_mvrp_solver, {'import_depot_garage': True})
        [route] = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)
        depot = get_depot(system_env, route['depot_id'])

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        assert route_info['nodes'][0].items() >= _get_type_items('depot')
        assert route_info['nodes'][3].items() >= _get_type_items('depot')
        assert all('estimated_service_time' not in node['value'] for node in route_info['nodes'])

        # visit first depot
        locations = [(depot['lat'], depot['lon'], route_info['nodes'][1]['value']['time_windows'][0]['start']['value'])]
        push_positions(system_env, route["courier_id"], route["id"], locations)
        locations = [(depot['lat'], depot['lon'], route_info['nodes'][1]['value']['time_windows'][0]['start']['value'] + 600)]
        push_positions(system_env, route["courier_id"], route["id"], locations)

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        assert route_info['nodes'][0].items() >= _get_type_items('depot')
        assert set(route_info['nodes'][0]['value']) >= {'id', 'visit_time', 'point'}
        assert all('estimated_service_time' not in node['value'] for node in route_info['nodes'])

        # depart from first depot
        locations = [(55.999, 37.729, route_info['nodes'][1]['value']['time_windows'][0]['start']['value'] + 1200)]
        push_positions(system_env, route["courier_id"], route["id"], locations)

        query_routed_orders(system_env, route["courier_id"], route["id"])

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        assert route_info['nodes'][0].items() >= _get_type_items('depot')
        assert route_info['nodes'][3].items() >= _get_type_items('depot')
        assert set(route_info['nodes'][0]['value']) >= {'id', 'visit_time', 'point'}
        assert all('estimated_service_time' in node['value'] for node in route_info['nodes'][1:] if node['type'] != 'garage')


@skip_if_remote
@pytest.mark.parametrize("task_id", [
    'mock_task_uuid__generic_with_two_depots',
    'mock_task_uuid__generic_with_two_depots_and_garage',
])
def test_first_depot_is_visited_after_order_is_finished(_system_env_with_db_mocked_mvrp_solver, task_id):
    system_env = _system_env_with_db_mocked_mvrp_solver
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        patch_company(_system_env_with_db_mocked_mvrp_solver, {'import_depot_garage': True})
        [route] = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        order = get_order(system_env, route_info['nodes'][1]['value']['id'])
        patch_order(system_env, order, {'status': 'finished'})
        query_routed_orders(system_env, route["courier_id"], route["id"])

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        assert route_info['nodes'][0].items() >= _get_type_items('depot')
        assert route_info['nodes'][3].items() >= _get_type_items('depot')
        assert set(route_info['nodes'][0]['value']) >= {'id', 'visit_time', 'point'}
        assert all('estimated_service_time' in node['value'] for node in route_info['nodes'][2:] if node['type'] != 'garage')
        assert route_info['meta']['courier_position']['node'] == {'prev': 1, 'next': 2}

        if task_id == 'mock_task_uuid__generic_with_two_depots_and_garage':
            _validate_last_garage(route_info['nodes'][4], must_be_estimated=True, is_visited=False)


@skip_if_remote
def test_import_route_with_first_garage_first_depot(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__solution_with_garage_first'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        patch_company(_system_env_with_db_mocked_mvrp_solver, {'import_depot_garage': True})
        route, _ = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        assert route_info['meta']['courier_position']['node'] == {'prev': None, 'next': 1}

        locations = [(55.799, 37.7293, route_info['nodes'][2]['value']['time_windows'][0]['start']['text'])]
        push_positions(system_env, route["courier_id"], route["id"], locations)
        query_routed_orders(system_env, route["courier_id"], route["id"])

        _, [route_info] = get_route_info(system_env, route_id=route['id'])

        garage = route_info['nodes'][0]
        assert garage.items() >= _get_type_items('garage')
        assert set(garage['value']) >= {'id', 'point'}

        depot = route_info['nodes'][1]
        assert depot.items() >= _get_type_items('depot')
        assert set(depot['value']) >= {'id', 'estimated_service_time', 'point'}

        assert route_info['meta']['courier_position']['node'] == {'prev': None, 'next': 1}


@skip_if_remote
@pytest.mark.parametrize("task_id", [
    'mock_task_uuid__generic_with_two_depots',
    'mock_task_uuid__generic_with_two_depots_and_garage',
])
def test_import_route_with_two_depots_mark_visited(_system_env_with_db_mocked_mvrp_solver, task_id):
    system_env = _system_env_with_db_mocked_mvrp_solver
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        patch_company(_system_env_with_db_mocked_mvrp_solver, {'import_depot_garage': True})
        [route] = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        for idx in 0, 3:
            assert route_info['nodes'][idx].items() >= _get_type_items('depot')
        last_depot = route_info['nodes'][3]['value']
        assert 'estimated_service_time' not in last_depot
        assert 'visit_time' not in last_depot
        assert route_info['meta']['courier_position']['node'] == {'prev': None, 'next': 0}

        # visit orders
        orders = get_orders(system_env, route['id'])
        for node_idx in 1, 2:
            assert route_info['nodes'][node_idx]['type'] == 'order'
            assert str(orders[node_idx - 1]['id']) == route_info['nodes'][node_idx]['value']['id']
            for pos_idx in range(2):
                locations = [(orders[node_idx - 1]['lat'], orders[node_idx - 1]['lon'], route_info['nodes'][node_idx]['value']['time_windows'][0]['start']['value'] + pos_idx * 600)]
                push_positions(system_env, route["courier_id"], route["id"], locations)

            _, [route_info] = get_route_info(system_env, route_id=route['id'])
            assert route_info['meta']['courier_position']['node'] == {'prev': node_idx, 'next': node_idx + 1}
            assert 'delivery_time' in route_info['nodes'][node_idx]['value']

        query_routed_orders(system_env, route["courier_id"], route["id"])
        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        last_depot = route_info['nodes'][3]['value']
        assert 'estimated_service_time' in last_depot

        # visit depot
        depot = get_depot(system_env, route['depot_id'])
        for pos_idx in range(2):
            locations = [(depot['lat'], depot['lon'], route_info['nodes'][2]['value']['time_windows'][0]['end']['value'] + (pos_idx + 1) * 600)]
            push_positions(
                system_env,
                route["courier_id"],
                route["id"],
                locations
            )

        _, [route_info] = get_route_info(system_env, route_id=route['id'])
        assert route_info['nodes'][3].items() >= _get_type_items('depot')
        assert 'visit_time' in route_info['nodes'][3]['value']
        assert route_info['meta']['courier_position']['node'] == {
            'prev': 3,
            'next': 4 if task_id == 'mock_task_uuid__generic_with_two_depots_and_garage' else None
        }

        if task_id == 'mock_task_uuid__generic_with_two_depots_and_garage':
            _validate_last_garage(route_info['nodes'][4], must_be_estimated=True, is_visited=False)
            assert route_info['meta']['courier_position']['node'] == {'prev': 3, 'next': 4}

            # visit garage
            locations = [(GARAGE_COORDINATES['lat'], GARAGE_COORDINATES['lon'], route_info['nodes'][2]['value']['time_windows'][0]['end']['value'] + 3 * 600)]
            push_positions(system_env, route["courier_id"], route["id"], locations)

            _, [route_info] = get_route_info(system_env, route_id=route['id'])
            _validate_last_garage(route_info['nodes'][4], must_be_estimated=False, is_visited=True)
            assert route_info['meta']['courier_position']['node'] == {'prev': 4, 'next': None}


@skip_if_remote
def test_apikeys_service_error(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver

    patch_company(system_env, {'apikey': apikey_values.MOCK_SIMULATE_ERROR})

    start_billing_value = _get_billing_counter_value()
    task_id = 'mock_task_uuid__generic'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        imported_routes = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)
        _verify_import(system_env, depot_env, imported_routes, get_solution_by_task_id(task_id), expected_order_status='new')

        assert _get_billing_counter_value() == start_billing_value

    patch_company(system_env, {'apikey': apikey_values.ACTIVE})


@skip_if_remote
def test_bad_apikey(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver

    start_billing_value = _get_billing_counter_value()

    task_id = 'mock_task_uuid__generic'
    with _create_default_depot_courier_env(system_env, task_id):
        for apikey in [apikey_values.BANNED, apikey_values.UNKNOWN]:
            patch_company(system_env, {'apikey': apikey})

            response = _post_mvrp_task_import(system_env, task_id=task_id,
                                              expected_status_code=requests.codes.forbidden)
            assert "Company apikey is unknown or banned" in response['message']

            assert _get_billing_counter_value() == start_billing_value

    patch_company(system_env, {'apikey': apikey_values.ACTIVE})


@skip_if_remote
def test_import_non_existent_task(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    response = _post_mvrp_task_import(
        system_env,
        task_id='non-existent-task-id',
        expected_status_code=requests.codes.not_found)
    assert 'No task' in response['message']


@skip_if_remote
def test_import_unknown_entities(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__unknown_depot'
    with _create_default_depot_courier_env(system_env, task_id):
        response = _post_mvrp_task_import(system_env, task_id=task_id,
                                          expected_status_code=requests.codes.unprocessable)
        assert f"Can not find objects defined by depot_number field: '{UNKNOWN_DEPOT_ID}'" in response['message']


@skip_if_remote
def test_route_number_too_long(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    start_billing_value = _get_billing_counter_value()
    task_id = 'mock_task_uuid__route_number_too_long'
    with _create_default_depot_courier_env(system_env, task_id):
        response = _post_mvrp_task_import(system_env, task_id=task_id,
                                          expected_status_code=requests.codes.unprocessable)
        assert "Generated route number is too long" in response['message']

        assert _get_billing_counter_value() == start_billing_value


@skip_if_remote
@pytest.mark.parametrize("import_mode, keep_routes, expected_status_code", [
    (None, True, requests.codes.ok),
    (None, False, requests.codes.ok),
    (None, None, requests.codes.unprocessable),
    ('replace-all', None, requests.codes.ok),
    ('replace-not-started', None, requests.codes.ok),
    ('add-all', None, requests.codes.ok),
])
def test_route_number_matches_existing_route(_system_env_with_db_mocked_mvrp_solver, import_mode, keep_routes, expected_status_code):
    """
    This test:
    1) Creates an empty route;
    2) Imports a task solution with a route which number would match
        the number of the previous route;
    3) Depending on import-mode and keep-routes values checks the result.

    The following cases are possible:
        - both keep-routes and import-mode are None: raises an error.
        - keep_routes=true: skips the route with the conflicting number.
        - import-mode=replace-all (or keep-routes=false): replaces all routes.
        - import-mode=replace-not-started: also replaces all routes since
          no tracking is emulated in this test.
        - import-mode=add-all: only order conflicts have to be checked,
          route number should be generated in a way that avoids conflicts.
    """
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        solution = get_solution_by_task_id(task_id)
        route_number = solution['result']['routes'][0]['__testing__expected_route_number']

        route = create_route(system_env,
                             route_number,
                             courier_id=depot_env['couriers'][0]['id'],
                             depot_id=depot_env['depots'][0]['id'],
                             route_date=solution['result']['options']['date'])
        depot_env['created_route_numbers'].append(route['number'])  # tracking created routes for later clean-up

        extra_query_params = {}

        if keep_routes is not None:
            extra_query_params['keep-routes'] = keep_routes
        if import_mode is not None:
            extra_query_params['import-mode'] = import_mode

        response = _post_mvrp_task_import(system_env, task_id=task_id,
                                          depot_env=depot_env,
                                          extra_query_params=extra_query_params,
                                          expected_status_code=expected_status_code)

        if import_mode == 'add-all':
            # No orders in the original route, hence no orders conflict with import.
            # Therefore all routes should be added, maybe with different numbers.
            created_routes = sorted(route['number'] for route in response)
            expected_added_routes = sorted(r['__testing__expected_route_number'] for r in solution['result']['routes'])
            # As one route with this number already exists, we cannot expect route numbers to be equal
            assert len(created_routes) == len(expected_added_routes)
            assert all(created_number.startswith(expected_created_number) for created_number, expected_created_number in zip(created_routes, expected_added_routes))
        elif import_mode in ['replace-all', 'replace-not-started']:
            # No tracking has been added to the existing route, so all routes are expected to be replaced in both modes
            assert [route['number'] for route in response] == [r['__testing__expected_route_number'] for r in solution['result']['routes']]
        else:
            if keep_routes:
                assert [route['number'] for route in response] == [solution['result']['routes'][1]['__testing__expected_route_number']]
            elif keep_routes is None:
                assert f"Routes with the following numbers already exist: ['{route_number}']" in response['message']
            else:
                assert [route['number'] for route in response] == [route_number, solution['result']['routes'][1]['__testing__expected_route_number']]


@skip_if_remote
def test_import_routes_for_day_with_routes(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        solution = get_solution_by_task_id(task_id)
        date = solution['result']['options']['date']
        route_number = f"first_route_for_{date}"

        route = create_route(system_env,
                             route_number,
                             courier_id=depot_env['couriers'][0]['id'],
                             depot_id=depot_env['depots'][0]['id'],
                             route_date=date)
        depot_env['created_route_numbers'].append(route['number'])  # tracking created routes for later clean-up

        _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)

        routes_for_the_date = get_route_details(system_env, date)
        assert set(route['route_number'] for route in routes_for_the_date) == {route_number,
                                                                               solution['result']['routes'][0]['__testing__expected_route_number'],
                                                                               solution['result']['routes'][1]['__testing__expected_route_number']}


@skip_if_remote
def test_keep_routes_true(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'
    reduced_task_id = 'mock_task_uuid__reduced_generic'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        response = _post_mvrp_task_import(system_env, task_id=reduced_task_id, depot_env=depot_env)
        assert len(response) == len(get_solution_by_task_id(reduced_task_id)['result']['routes'])

        response = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env,
                                          extra_query_params={'keep-routes': True})
        assert len(response) == len(get_solution_by_task_id(task_id)['result']['routes']) - len(get_solution_by_task_id(reduced_task_id)['result']['routes'])


@skip_if_remote
def test_keep_routes_false(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'
    reduced_task_id = 'mock_task_uuid__reduced_generic'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        response = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)
        assert len(response) == len(get_solution_by_task_id(task_id)['result']['routes'])

        response = _post_mvrp_task_import(system_env, task_id=reduced_task_id, depot_env=depot_env,
                                          extra_query_params={'keep-routes': False})
        assert len(response) == len(get_solution_by_task_id(reduced_task_id)['result']['routes'])

        reduced_solution = get_solution_by_task_id(reduced_task_id)
        reduces_solution_route_numbers = {route['__testing__expected_route_number'] for route in reduced_solution['result']['routes']}
        date = reduced_solution['result']['options']['date']
        routes_for_the_date = get_route_details(system_env, date)
        assert {route['route_number'] for route in routes_for_the_date} == reduces_solution_route_numbers


@skip_if_remote
def test_route_with_no_date(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__solution_without_date'
    with _create_default_depot_courier_env(system_env, task_id):
        response = _post_mvrp_task_import(system_env, task_id=task_id,
                                          expected_status_code=requests.codes.unprocessable)
        assert "Date is missing in solution options" == response['message']


@skip_if_remote
def test_mvrp_solver_unavailable(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    response = _post_mvrp_task_import(
        system_env,
        task_id='mock_task_uuid__service_unavailable',
        expected_status_code=requests.codes.internal_server_error)
    assert "VRP Solver cannot process your request" in response['message']


@skip_if_remote
def test_solution_one_route_without_depot(_system_env_with_db_mocked_mvrp_solver):
    """
    This functionality is postponed to the follow-up commit.
    """
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__one_route_without_depot'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        _post_mvrp_task_import(system_env,
                               task_id=task_id,
                               depot_env=depot_env,
                               expected_status_code=requests.codes.ok)


@skip_if_remote
def test_importing_failed_task(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__errored_task'
    with _create_default_depot_courier_env(system_env, task_id):
        response = _post_mvrp_task_import(system_env, task_id=task_id,
                                          expected_status_code=requests.codes.unprocessable)
        assert 'Solution is not applicable:' in response['message']


def _get_user(system_env, login):
    response = env_get_request(
        system_env,
        path=f"user-info?login={login}",
        auth=system_env.auth_header_super)
    assert response.status_code == requests.codes.ok
    users = response.json()
    assert len(users) == 1
    return users[0]


@skip_if_remote
def test_import_different_companies(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'

    c1_id = create_company(system_env, 'test_import_different_companies_c1', 'test_import_different_companies_c1')
    c2_id = create_company(system_env, 'test_import_different_companies_c2', 'test_import_different_companies_c2')
    u1 = _get_user(system_env, 'test_import_different_companies_c1')
    u2 = _get_user(system_env, 'test_import_different_companies_c2')
    u1_auth = get_user_auth(u1, None)
    u2_auth = get_user_auth(u2, None)
    create_depot(system_env, depot_number='0', company_id=c1_id, auth=u1_auth)
    create_depot(system_env, depot_number='0', company_id=c2_id, auth=u2_auth)
    _post_mvrp_task_import(system_env, task_id=task_id, company_id=c1_id, auth=u1_auth)
    _post_mvrp_task_import(system_env, task_id=task_id, company_id=c2_id, auth=u2_auth)


@skip_if_remote
def test_import_routes(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'

    for requested_order_status, expected_order_status, expected_status_code in [
        (None, 'new', requests.codes.ok),
        ('new', 'new', requests.codes.ok),
        ('confirmed', 'confirmed', requests.codes.ok),
        ('finished', None, requests.codes.unprocessable),
        ('blah-blah-blah', None, requests.codes.unprocessable),
    ]:
        start_billing_value = _get_billing_counter_value()
        with _create_default_depot_courier_env(system_env, task_id) as depot_env:
            query_params_str = '' if requested_order_status is None else f'?initial-status={requested_order_status}'
            path = api_path_with_company_id(system_env, f'import-routes{query_params_str}')
            task_json = solver_request_by_task_id[task_id]
            response = env_post_request(system_env, path, data=task_json)
            assert response.status_code == expected_status_code, response.text

            if response.status_code != requests.codes.ok:
                continue

            j = response.json()
            task_id = j["task_id"]
            imported_routes = j["routes"]

            assert task_id == j["task_id"]

            # tracking created routes to be delete upon teardown
            depot_env['created_route_numbers'] += [route['number'] for route in imported_routes if route.get('number')]

            _verify_import(system_env, depot_env, imported_routes, get_solution_by_task_id(task_id), expected_order_status)

            assert _get_billing_counter_value() == start_billing_value


@skip_if_remote
def test_import_routes_timeout(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver

    path = api_path_with_company_id(system_env, "import-routes")
    task_json = solver_request_by_task_id["mock_task_uuid__queued_task"]
    response = env_post_request(system_env, path, data=task_json)

    assert response.status_code == requests.codes.unprocessable
    assert response.json()["message"] == "Routes were not imported within timeout"


@skip_if_remote
def test_import_routes_failed(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver

    path = api_path_with_company_id(system_env, "import-routes")
    task_json = solver_request_by_task_id["mock_task_uuid__errored_task"]
    response = env_post_request(system_env, path, data=task_json)

    assert response.status_code == requests.codes.unprocessable
    assert 'Solution is not applicable:' in response.json()['message']


@skip_if_remote
def test_import_routes_invalid_json(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver

    path = api_path_with_company_id(system_env, "import-routes")
    task_json = solver_request_by_task_id["mock_task_uuid__invalid_task"]
    response = env_post_request(system_env, path, data=task_json)

    assert response.status_code == requests.codes.unprocessable
    assert 'VRP Solver cannot process requested entity with the following error' in response.json()['message']


@skip_if_remote
@pytest.mark.parametrize("import_mode, keep_routes, expected_status_code", [
    (None, True, requests.codes.ok),
    (None, False, requests.codes.ok),
    (None, None, requests.codes.unprocessable),
    ('replace-all', None, requests.codes.ok),
    ('add-all', None, requests.codes.unprocessable),
])
def test_import_routes_keep_routes(_system_env_with_db_mocked_mvrp_solver, import_mode, keep_routes, expected_status_code):
    """
    1) Imports the task;
    2) Imports the same task again with certain import-mode and keep-routes settings.

    With import-mode=add-all an error should occur due to conflicting orders,
    with both import-mode and keep-routes being empty - due to conflicting route numbers.
    Other cases should be resolved successfully according to the respective settings.
    """
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'

    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        path = api_path_with_company_id(system_env, "import-routes")
        task_json = solver_request_by_task_id["mock_task_uuid__generic"]

        response = env_post_request(system_env, path, data=task_json)
        assert response.status_code == requests.codes.ok

        # tracking created routes to be delete upon teardown
        if response.json().get('routes'):
            depot_env['created_route_numbers'] += [route['number'] for route in response.json()['routes'] if route.get('number')]

        params = {}

        if keep_routes is not None:
            params['keep-routes'] = keep_routes
        if import_mode is not None:
            params['import-mode'] = import_mode

        response = env_post_request(system_env, path, data=task_json, params=params)
        assert response.status_code == expected_status_code

        # tracking created routes to be delete upon teardown
        if response.json().get('routes'):
            depot_env['created_route_numbers'] += [route['number'] for route in response.json()['routes'] if route.get('number')]

        if import_mode == 'add-all':
            expected_message = 'Orders with the following numbers already exist and cannot be ignored or modified according to the current import mode'
            assert expected_message in response.json()['message']
        elif import_mode is None and keep_routes is None:
            assert 'Routes with the following numbers already exist' in response.json()['message']


@skip_if_remote
def test_route_with_garage_first(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__solution_with_garage_first'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        patch_company(_system_env_with_db_mocked_mvrp_solver, {'import_depot_garage': True})
        imported_routes = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)
        _verify_import(system_env, depot_env, imported_routes, get_solution_by_task_id(task_id),
                       expected_order_status='new')

        _, route_info = get_route_info(system_env, route_id=imported_routes[0]['id'])
        first_node = route_info[0]['nodes'][0]
        assert first_node['type'] == 'garage'


@skip_if_remote
def test_route_with_garage_last(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__solution_with_garage_last'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        patch_company(_system_env_with_db_mocked_mvrp_solver, {'import_depot_garage': True})
        imported_routes = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)
        _verify_import(system_env, depot_env, imported_routes, get_solution_by_task_id(task_id),
                       expected_order_status='new')

        _, route_info = get_route_info(system_env, route_id=imported_routes[0]['id'])
        last_node = route_info[0]['nodes'][-1]
        assert last_node['type'] == 'garage'


@skip_if_remote
def test_route_with_garage_first_and_last(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__solution_with_garage_first_and_last'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        patch_company(_system_env_with_db_mocked_mvrp_solver, {'import_depot_garage': True})
        imported_routes = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)
        _verify_import(system_env, depot_env, imported_routes, get_solution_by_task_id(task_id),
                       expected_order_status='new')

        _, route_info = get_route_info(system_env, route_id=imported_routes[0]['id'])
        nodes = route_info[0]['nodes']
        first_node, last_node = nodes[0], nodes[-1]
        assert first_node['type'] == 'garage' and last_node['type'] == 'garage'
        assert first_node['types'] == ['garage'] and last_node['types'] == ['garage']


@skip_if_remote
def test_import_the_same_orders(_system_env_with_db_mocked_mvrp_solver):
    '''
    Task 'mock_task_uuid__reduced_generic' contains route A with orders o1, o2
    Task 'mock_task_uuid__generic' contains routes A (o1, o2), B (o3)
    Task 'mock_task_uuid__generic_changed_date' contains routes C (o1, o2), D (o3)
    '''
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_A = 'mock_task_uuid__reduced_generic'
    task_A_B = 'mock_task_uuid__generic'
    task_C_D = 'mock_task_uuid__generic_changed_date'

    with _create_default_depot_courier_env(system_env, task_C_D) as depot_env:
        response = _post_mvrp_task_import(system_env, task_id=task_A, depot_env=depot_env)
        assert len(response) == len(get_solution_by_task_id(task_A)['result']['routes'])
        route_A_id = response[0]['id']
        assert len(get_order_details_by_route_id(system_env, route_A_id)) == 2

        # add-all mode should fail if importing existing orders
        response = _post_mvrp_task_import(system_env, task_id=task_C_D, depot_env=depot_env,
                                          extra_query_params={'import-mode': 'add-all'},
                                          expected_status_code=requests.codes.unprocessable)
        assert 'Orders with the following numbers already exist and cannot be ignored or modified according to the current import mode' in response['message']

        response = _post_mvrp_task_import(system_env, task_id=task_C_D, depot_env=depot_env,
                                          expected_status_code=requests.codes.unprocessable)
        assert 'Orders with following numbers already exist: ' in response['message']

        response = _post_mvrp_task_import(system_env, task_id=task_C_D, depot_env=depot_env,
                                          extra_query_params={'update-orders': True})
        assert len(response) == len(get_solution_by_task_id(task_C_D)['result']['routes'])
        assert len(get_order_details_by_route_id(system_env, route_A_id)) == 0
        order_3 = get_order_details_by_route_id(system_env, response[1]['id'])[0]

        patch_order_by_order_id(system_env, order_3["order_id"], {"status": "finished"})
        response = _post_mvrp_task_import(system_env, task_id=task_A_B, depot_env=depot_env,
                                          extra_query_params={'update-orders': True, 'keep-routes': True},
                                          expected_status_code=requests.codes.unprocessable)
        assert f'Some orders can not be updated. Orders with numbers [\'{order_3["order_number"]}\'] are already finished.' in response['message']

        patch_order_by_order_id(system_env, order_3["order_id"], {"status": "confirmed"})
        track = [(55, 37, order_3['time_window']['start'])]
        push_positions(system_env, order_3['courier_id'], order_3['route_id'], track)
        response = _post_mvrp_task_import(system_env, task_id=task_A_B, depot_env=depot_env,
                                          extra_query_params={'update-orders': True, 'keep-routes': True},
                                          expected_status_code=requests.codes.unprocessable)
        assert f'Some orders can not be updated. Orders with numbers [\'{order_3["order_number"]}\'] are from started routes.' in response['message']


@skip_if_remote
def test_import_mode_replace_not_started(_system_env_with_db_mocked_mvrp_solver):
    '''
    - Task 'mock_task_uuid__generic' contains routes A (o1, o2), B (o3).
    - Task 'mock_task_uuid__changed_vehicles_generic' contains routes C (o1, o2), D (o3),
      with the same routes date but with changed vehicle IDs.

    In this test we:
    1) Import task_A_B
    2) Mark route B as started (by pushing a courier's position)
    3) Try importing task_C_D
    '''
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_A_B = 'mock_task_uuid__generic'
    task_C_D = 'mock_task_uuid__changed_vehicles_generic'

    with _create_default_depot_courier_env(system_env, task_C_D) as depot_env:
        # Import task_A_B solution.
        response = _post_mvrp_task_import(system_env, task_id=task_A_B, depot_env=depot_env)
        assert len(response) == len(get_solution_by_task_id(task_A_B)['result']['routes'])
        route_A_id, route_B_id = [r['id'] for r in response]
        assert len(get_order_details_by_route_id(system_env, route_A_id)) == 2
        assert len(get_order_details_by_route_id(system_env, route_B_id)) == 1

        order_3 = get_order_details_by_route_id(system_env, route_B_id)[0]

        # Let the courier start route B.
        patch_order_by_order_id(system_env, order_3["order_id"], {"status": "confirmed"})
        track = [(55, 37, order_3['time_window']['start'])]
        push_positions(system_env, order_3['courier_id'], order_3['route_id'], track)

        # Try importing task_C_D.
        # replace-not-started mode should ignore orders from routes with courier track.
        # Current state corresponds to task_A_B imported with route B(o3) being started.
        # Therefore if importing task_C_D with this mode, route C is expected to replace
        # route A, and task B is expected to be preserved.
        response = _post_mvrp_task_import(system_env, task_id=task_A_B, depot_env=depot_env,
                                          extra_query_params={'import-mode': 'replace-not-started'},
                                          expected_status_code=requests.codes.ok)

        # Check old routes.
        # Route A should have been deleted by import, route B should remain the same.
        route_details_A = get_order_details_by_route_id(system_env, route_A_id, expected_status_code=requests.codes.not_found)
        assert "Route doesn't exist" in route_details_A['message']
        assert len(get_order_details_by_route_id(system_env, route_B_id)) == 1

        # Check imported routes.
        assert len(response) == 1
        route_C_id = response[0]['id']
        assert len(get_order_details_by_route_id(system_env, route_C_id)) == 2


@skip_if_remote
@pytest.mark.parametrize("import_mode, update_orders", [
    ('replace-all', None),
    ('replace-all', True),
    ('replace-not-started', None),
    ('replace-not-started', True),
])
def test_replacing_import_with_different_dates(_system_env_with_db_mocked_mvrp_solver, import_mode, update_orders):
    '''
    - Task 'mock_task_uuid__reduced_generic' contains routes A (o1, o2)
    - Task 'mock_task_uuid__reduced_generic_changed_date' contains the same route
      but has a different date.
    - Task 'mock_task_uuid__reduced_generic_2_changed_date' contains route B (o3)
      with a different date (the same as the previous task).

    In this test we:
    1) Import route A for old date, route B for new date.
    2) Try importing route A for a new date.
    3) Check that:
       a) If update-orders=true, routes are getting updated;
       a) Otherwise, error is returned: no new routes are going to be added as
          replacing routes works only for the same date. Old routes and orders
          should remain untouched.
    '''
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_route_A_old = 'mock_task_uuid__reduced_generic'
    task_route_A_new = 'mock_task_uuid__reduced_generic_changed_date'
    task_route_B_new = 'mock_task_uuid__reduced_generic_2_changed_date'

    full_task_id = 'mock_task_uuid__generic'  # to have a full list of couriers, depots etc.
    with _create_default_depot_courier_env(system_env, full_task_id) as depot_env:
        # Import tasks.
        response = _post_mvrp_task_import(system_env, task_id=task_route_A_old, depot_env=depot_env)
        assert len(response) == len(get_solution_by_task_id(task_route_A_old)['result']['routes'])
        route_A_id = response[0]['id']
        assert len(get_order_details_by_route_id(system_env, route_A_id)) == 2

        response = _post_mvrp_task_import(system_env, task_id=task_route_B_new, depot_env=depot_env)
        assert len(response) == len(get_solution_by_task_id(task_route_B_new)['result']['routes'])
        route_B_id = response[0]['id']
        assert len(get_order_details_by_route_id(system_env, route_B_id)) == 1

        # Try importing task_route_A_new.
        params = {'import-mode': import_mode}
        if update_orders is not None:
            params['update-orders'] = update_orders

        expected_status_code = requests.codes.ok if update_orders else requests.codes.unprocessable

        response = _post_mvrp_task_import(system_env, task_id=task_route_A_new, depot_env=depot_env,
                                          extra_query_params=params,
                                          expected_status_code=expected_status_code)

        if update_orders:
            # Check that import is successful
            assert len(response) == len(get_solution_by_task_id(task_route_A_new)['result']['routes'])
            # Old route A is now empty
            assert len(get_order_details_by_route_id(system_env, route_A_id)) == 0
            # Route B should not exist after replacing routes
            route_details_B = get_order_details_by_route_id(system_env, route_B_id, expected_status_code=requests.codes.not_found)
            assert "Route doesn't exist" in route_details_B['message']
        else:
            # Check old routes - they should remain in the same state as before.
            assert len(get_order_details_by_route_id(system_env, route_A_id)) == 2
            assert len(get_order_details_by_route_id(system_env, route_B_id)) == 1


@skip_if_remote
def test_check_import_task(system_env_with_db):
    system_env = system_env_with_db

    task_json = solver_request_by_task_id["mock_task_uuid__generic"]

    path = api_path_with_company_id(system_env, "check-import-task")
    response = env_post_request(system_env, path, data=task_json)
    assert response.status_code == requests.codes.ok, response.text
    assert response.json() == []


@skip_if_remote
def test_check_import_task_fail(system_env_with_db):
    system_env = system_env_with_db
    order_number = 'check_import_task_order_1'

    task_json = deepcopy(solver_request_by_task_id["mock_task_uuid__generic"])
    task_json['locations'][0]['id'] = order_number

    path = api_path_with_company_id(system_env, "check-import-task")
    with create_route_env(system_env, "check_import_task") as route_env:
        patch_order(system_env, route_env['orders'][0], {'number': order_number})

        response = env_post_request(system_env, path, data=task_json)
        assert response.status_code == requests.codes.ok, response.text
        j = response.json()
        assert len(j) == 1
        assert j[0]['error'] == 'OrdersAlreadyExist'
        assert j[0]['item_type'] == 'order'
        assert j[0]['numbers'] == [order_number]

        order = route_env['orders'][0]
        patch_order_by_order_id(system_env, order['id'], {"status": "finished"})
        response = env_post_request(system_env, path, data=task_json)
        assert response.status_code == requests.codes.ok, response.text
        j = response.json()
        assert len(j) == 2
        assert j[0]['error'] == 'OrdersAlreadyExist'
        assert j[0]['item_type'] == 'order'
        assert j[0]['numbers'] == [order_number]
        assert j[1]['error'] == 'OrdersAlreadyFinished'
        assert j[1]['item_type'] == 'order'
        assert j[1]['numbers'] == [order_number]

        patch_order_by_order_id(system_env, order['id'], {"status": "confirmed"})
        track = [(55, 37, order['time_window']['start'])]
        push_positions(system_env, route_env['route']['courier_id'], order['route_id'], track)
        response = env_post_request(system_env, path, data=task_json)
        assert response.status_code == requests.codes.ok, response.text
        j = response.json()
        assert len(j) == 2
        assert j[0]['error'] == 'OrdersAlreadyExist'
        assert j[0]['item_type'] == 'order'
        assert j[0]['numbers'] == [order_number]
        assert j[1]['error'] == 'OrdersWithPositions'
        assert j[1]['item_type'] == 'order'
        assert j[1]['numbers'] == [order_number]


@skip_if_remote
def test_check_import_task_routes_fail(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'

    path_check = api_path_with_company_id(system_env, 'check-import-task')
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        task_json = deepcopy(solver_request_by_task_id[task_id])
        result_json = get_solution_by_task_id(task_id)['result']

        date = result_json['options']['date']
        vehicles = deepcopy(result_json['vehicles'])

        depot_id = depot_env['depots'][0]['id']
        courier_id = depot_env['couriers'][0]['id']

        # need more vehicles
        vehicles += [{'id': 3002}, {'id': 3003}, {'id': 3004}]

        # occupy all possible route names
        route_numbers = [create_route(system_env, f"{courier['id']}-{run}-{date}", courier_id, depot_id)["number"]
                         for courier in vehicles
                         for run in range(0, 5)]

        # delete created routes upon teardown
        depot_env['created_route_numbers'] += route_numbers

        task_json['vehicles'] = vehicles
        task_json['depots'] = [{'id': depot_id}]

        task_json['options']['date'] = date
        task_json['vehicles'][0]['max_runs'] = 2
        task_json['vehicles'][0]['planned_route'] = {'locations': [{'id': 1337}, {'id': depot_id}, {'id': 1338}]}
        task_json['vehicles'][1]['max_runs'] = 9
        task_json['vehicles'][1]['planned_route'] = {'locations': [{'id': depot_id}, {'id': 1339}, {'id': depot_id}]}
        task_json['vehicles'][2]['max_runs'] = 300
        task_json['vehicles'][2]['planned_route'] = {'locations': [{}]}
        task_json['vehicles'][3]['planned_route'] = {'locations': []}
        task_json['vehicles'][4]['planned_route'] = {}
        task_json['vehicles'].append({})

        response = env_post_request(system_env, path_check, data=task_json)
        assert response.status_code == requests.codes.ok, response.text
        j = response.json()
        assert len(j) == 1
        assert j[0]['error'] == 'RoutesAlreadyExist'
        assert j[0]['item_type'] == 'route'
        assert j[0]['numbers'] == ['2020-1-2019-12-13', '2020-2-2019-12-13', '3000-1-2019-12-13']


@skip_if_remote
def test_check_import_task_by_task_id(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'

    path = api_path_with_company_id(system_env, "check-import-task")
    path += f'?task_id={task_id}'
    response = env_post_request(system_env, path, data={})
    assert response.status_code == requests.codes.ok, response.text
    assert response.json() == []


@skip_if_remote
def test_check_import_task_by_task_id_fail(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__generic'

    path_check = api_path_with_company_id(system_env, "check-import-task")
    path_check += f'?task_id={task_id}'
    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        path_import = api_path_with_company_id(system_env, "import-routes")
        task_json = solver_request_by_task_id["mock_task_uuid__generic"]

        response = env_post_request(system_env, path_import, data=task_json)
        assert response.status_code == requests.codes.ok

        j = response.json()
        route_numbers = [route['number'] for route in j['routes'] if route.get('number')]
        order_numbers = _extract_order_numbers(get_solution_by_task_id(task_id)['result'])

        # tracking created routes to be delete upon teardown
        depot_env['created_route_numbers'] += route_numbers

        response = env_post_request(system_env, path_check, data=None)
        assert response.status_code == requests.codes.ok, response.text
        j = response.json()
        assert len(j) == 2
        assert j[0]['error'] == 'OrdersAlreadyExist'
        assert j[0]['item_type'] == 'order'
        assert set(j[0]['numbers']) == set(order_numbers)
        assert j[1]['error'] == 'RoutesAlreadyExist'
        assert j[1]['item_type'] == 'route'
        assert set(j[1]['numbers']) == set(route_numbers)


@skip_if_remote
def test_check_import_task_not_set(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver

    path = api_path_with_company_id(system_env, "check-import-task")
    response = env_post_request(system_env, path, data={})
    assert response.status_code == requests.codes.unprocessable_entity
    assert 'Both task_id and request body are missing or incorrect' in response.text


@skip_if_remote
def test_import_order_with_equal_numbers_for_entities(_system_env_with_db_mocked_mvrp_solver):
    system_env = _system_env_with_db_mocked_mvrp_solver
    task_id = 'mock_task_uuid__reduced_generic_with_garage_in_the_end'
    task_with_changes_id = 'mock_task_uuid__reduced_generic_with_garage_in_the_end_with_shuddled_numbers'

    with _create_default_depot_courier_env(system_env, task_id) as depot_env:
        response = _post_mvrp_task_import(system_env, task_id=task_id, depot_env=depot_env)
        _verify_import(system_env, depot_env, response, get_solution_by_task_id(task_id), expected_order_status='new')

        solution = get_solution_by_task_id(task_with_changes_id)
        solution['result']['routes'][0]['__testing__expected_route_number'] = '2020-1-2019-12-13-1'
        depot_number = str(solution['result']['routes'][0]['route'][0]['node']['value']['id'])
        create_depot(system_env, depot_number=depot_number, company_id=system_env.company_id, auth=system_env.auth_header_super)

        response = _post_mvrp_task_import(system_env, task_id=task_with_changes_id, depot_env=depot_env, extra_query_params={'import-mode': 'add-all'})
        _verify_import(system_env, depot_env, response, get_solution_by_task_id(task_with_changes_id), expected_order_status='new')
