import pytest
import requests
import contextlib
from enum import Enum, auto

import maps.b2bgeo.test_lib.apikey_values as apikey_values
from ya_courier_backend.interservice.apikeys.yandex import APIKEYS_SERVICE_TOKEN
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import (
    MOCK_APIKEYS_CONTEXT, skip_if_remote, APIKEYS_SERVICE_COUNTER
)
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    cleanup_courier,
    cleanup_depot,
    cleanup_route,
    create_courier,
    create_depot,
    env_patch_request,
    env_post_request,
    find_route,
    patch_company,
)


def _add_route(system_env_with_db, route_data, use_batch, expected_status_code=requests.codes.ok, expected_message=None):
    assert route_data['number']
    response = env_post_request(
        system_env_with_db,
        path=api_path_with_company_id(system_env_with_db, 'routes-batch' if use_batch else 'routes'),
        data=[route_data] if use_batch else route_data,
    )
    assert response.status_code == expected_status_code, response.text
    j = response.json()
    if response.status_code != requests.codes.ok:
        assert expected_message is None or expected_message in j['message']
        return
    assert expected_message is None
    if use_batch:
        assert (j['inserted'], j['updated']) == (1, 0)
        j = find_route(system_env_with_db, route_data['number'])
        assert j
    for k, v in route_data.items():
        assert j[k] == v


def _modify_route(system_env_with_db, route_data, use_batch, expected_status_code=requests.codes.ok, expected_message=None):
    assert route_data['number']
    if use_batch:
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, 'routes-batch'),
            data=[route_data],
        )
    else:
        route_id = find_route(system_env_with_db, route_data['number'])['id']
        route_data = route_data.copy()
        del route_data['number']
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, f'routes/{route_id}'),
            data=route_data
        )
    assert response.status_code == expected_status_code, response.text
    j = response.json()
    if response.status_code != requests.codes.ok:
        assert expected_message is None or expected_message in j['message']
        return
    assert expected_message is None
    if use_batch:
        assert (j['inserted'], j['updated']) == (0, 1)
        j = find_route(system_env_with_db, route_data['number'])
        assert j
    for k, v in route_data.items():
        assert j[k] == v


def _batch_routes(system_env_with_db, batch_data):
    response = env_post_request(
        system_env_with_db,
        path=api_path_with_company_id(system_env_with_db, 'routes-batch'),
        data=batch_data,
    )
    assert response.status_code == requests.codes.ok, response.text
    j = response.json()
    assert j['inserted'] + j['updated'] == len(batch_data)
    for route_data in batch_data:
        j = find_route(system_env_with_db, route_data['number'])
        assert j
        for k, v in route_data.items():
            assert j[k] == v


class ApikeysMode(Enum):
    active_apikey = auto()
    banned_apikey = auto()
    unknown_apikey = auto()
    service_error = auto()


@contextlib.contextmanager
def _create_billing_env(env, apikeys_mode):
    def _get_name(suffix):
        return 'test_courier_billing_' + suffix

    depot_number = _get_name('depot')
    courier0_number = _get_name('courier0')
    courier1_number = _get_name('courier1')
    route0_number = _get_name('route0')
    route1_number = _get_name('route1')

    def _cleanup():
        cleanup_route(env, route0_number)
        cleanup_route(env, route1_number)
        cleanup_depot(env, depot_number)
        cleanup_courier(env, courier0_number)
        cleanup_courier(env, courier1_number)

    billing_env = {}
    try:
        _cleanup()

        billing_env['dbenv'] = env

        billing_env['apikeys_mode'] = apikeys_mode

        billing_env['apikey'] = {
            ApikeysMode.active_apikey: apikey_values.ACTIVE,
            ApikeysMode.banned_apikey: apikey_values.BANNED,
            ApikeysMode.unknown_apikey: apikey_values.UNKNOWN,
            ApikeysMode.service_error: None if env.mock_apikeys_url is None else apikey_values.MOCK_SIMULATE_ERROR,
        }[apikeys_mode]

        if billing_env['apikey'] is not None:
            patch_company(env, {'apikey': billing_env['apikey']})

        billing_env['courier0'] = create_courier(env, courier0_number)
        billing_env['courier1'] = create_courier(env, courier1_number)
        billing_env['depot'] = create_depot(env, depot_number)
        billing_env['route0_number'] = route0_number
        billing_env['route1_number'] = route1_number

        yield billing_env
    finally:
        patch_company(env, {'apikey': apikey_values.ACTIVE})
        _cleanup()


def _get_counter_value(billing_env):
    if billing_env['dbenv'].mock_apikeys_url is None:
        return 0
    return MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['counters'][APIKEYS_SERVICE_COUNTER]


def _check_counter_value(billing_env, start_counter_value, potential_counter_increase):
    if billing_env['dbenv'].mock_apikeys_url is None:
        return
    if billing_env['apikeys_mode'] == ApikeysMode.active_apikey:
        assert _get_counter_value(billing_env) == start_counter_value
    else:
        assert _get_counter_value(billing_env) == start_counter_value


@pytest.mark.parametrize('apikeys_mode', [ApikeysMode.active_apikey, ApikeysMode.service_error])
@pytest.mark.parametrize('use_batch', [False, True])
def test_courier_billing(system_env_with_db, apikeys_mode, use_batch):
    """
    Testing courier-per-route_date billing (for details see https://st.yandex-team.ru/BBGEO-4006).

    This function tests various scenarios of creating/modifying a route,
    when courier is never None or omitted (it is tested by another function).
    """

    # Mock apikeys server needs to be present to simulate apikeys service errors.
    if apikeys_mode == ApikeysMode.service_error and system_env_with_db.mock_apikeys_url is None:
        return

    with _create_billing_env(system_env_with_db, apikeys_mode) as billing_env:

        start_counter_value = _get_counter_value(billing_env)

        date0 = '2019-05-25'
        date1 = '2019-05-26'
        date2 = '2019-05-27'

        # Creating a route and then changing its date should
        # increment the billing counter once for each date used.

        route_data = {
            'number': billing_env['route0_number'],
            'courier_id': billing_env['courier0']['id'],
            'depot_id': billing_env['depot']['id'],
            'date': date0
        }

        _add_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=1)

        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=1)

        route_data['date'] = date1
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        route_data['date'] = date0
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        # Creating another route for *the same* courier and then changing its
        # date a date that was already used by the first route: it should not
        # change the billing counter.

        route_data['number'] = billing_env['route1_number']

        _add_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        route_data['date'] = date1
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        route_data['date'] = date0
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        # Deleting the second route should not change the billing counter.

        cleanup_route(system_env_with_db, route_data['number'])
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        # Creating another route for *other* courier and then changing its date
        # to the dates already used by the first route (and the first courier)
        # should increment the billing counter once for each date used.

        route_data['courier_id'] = billing_env['courier1']['id']

        _add_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=3)

        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=3)

        route_data['date'] = date1
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=4)

        route_data['date'] = date0
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=4)

        # Changing courier to another courier on a route when the second courier
        # already used the date of the route should not change the billing
        # counter.

        route_data['courier_id'] = billing_env['courier0']['id']
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=4)

        route_data['courier_id'] = billing_env['courier1']['id']
        route_data['date'] = date1
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=4)

        # Changing courier to another courier on a route and also changig
        # the date to a date which was never used by the second courier
        # should increment the billing counter.

        route_data['courier_id'] = billing_env['courier0']['id']
        route_data['date'] = date2
        _modify_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=5)


@pytest.mark.parametrize('apikeys_mode', [ApikeysMode.active_apikey, ApikeysMode.service_error])
def test_courier_billing_two_routes_one_courier(system_env_with_db, apikeys_mode):
    """
    Testing courier-per-route_date billing (for details see https://st.yandex-team.ru/BBGEO-4006).

    This function tests various scenarios of creating/modifying routes,
    when two routes with the same courier are passed to routes-batch handler.
    """

    # Mock apikeys server needs to be present to simulate apikeys service errors.
    if apikeys_mode == ApikeysMode.service_error and system_env_with_db.mock_apikeys_url is None:
        return

    with _create_billing_env(system_env_with_db, apikeys_mode) as billing_env:

        start_counter_value = _get_counter_value(billing_env)

        date0 = '2019-05-25'
        date1 = '2019-05-26'
        date2 = '2019-05-27'
        date3 = '2019-05-28'
        date4 = '2019-05-29'

        # Creating two routes for the same date (and the same courier) should
        # increment the billing counter only once.

        batch_data = [
            {
                'number': billing_env['route0_number'],
                'courier_id': billing_env['courier0']['id'],
                'depot_id': billing_env['depot']['id'],
                'date': date0
            },
            {
                'number': billing_env['route1_number'],
                'courier_id': billing_env['courier0']['id'],
                'depot_id': billing_env['depot']['id'],
                'date': date0
            }
        ]

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=1)

        # Changing both routes to a new date should
        # increment the billing counter only once.

        batch_data[0]['date'] = date1
        batch_data[1]['date'] = date1

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        # Changing two routes to an already used date should
        # not change the billing counter.

        batch_data[0]['date'] = date0
        batch_data[1]['date'] = date0

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        # Changing one of two routes to a new date and another one to an
        # already used date should increment the billing counter only once.

        batch_data[0]['date'] = date2
        batch_data[1]['date'] = date1

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=3)

        # Changing two routes to two new different dates should
        # increase the billing counter by two.

        batch_data[0]['date'] = date3
        batch_data[1]['date'] = date4

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=5)


@pytest.mark.parametrize('apikeys_mode', [ApikeysMode.active_apikey, ApikeysMode.service_error])
def test_courier_billing_two_routes_two_couriers(system_env_with_db, apikeys_mode):
    """
    Testing courier-per-route_date billing (for details see https://st.yandex-team.ru/BBGEO-4006).

    This function tests various scenarios of creating/modifying routes,
    when two routes with two couriers are passed to routes-batch handler.
    """

    # Mock apikeys server needs to be present to simulate apikeys service errors.
    if apikeys_mode == ApikeysMode.service_error and system_env_with_db.mock_apikeys_url is None:
        return

    with _create_billing_env(system_env_with_db, apikeys_mode) as billing_env:

        start_counter_value = _get_counter_value(billing_env)

        date0 = '2019-05-25'
        date1 = '2019-05-26'
        date2 = '2019-05-27'
        date3 = '2019-05-28'

        # Creating two routes for the same date with two different
        # couriers should increase the billing counter by two.

        batch_data = [
            {
                'number': billing_env['route0_number'],
                'courier_id': billing_env['courier0']['id'],
                'depot_id': billing_env['depot']['id'],
                'date': date0
            },
            {
                'number': billing_env['route1_number'],
                'courier_id': billing_env['courier1']['id'],
                'depot_id': billing_env['depot']['id'],
                'date': date0
            }
        ]

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=2)

        # Changing both routes to a new date should
        # increase the billing counter by two.

        batch_data[0]['date'] = date1
        batch_data[1]['date'] = date1

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=4)

        # Changing two routes to an already used date should
        # not change the billing counter.

        batch_data[0]['date'] = date0
        batch_data[1]['date'] = date0

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=4)

        # Changing one of two routes to a new date and another one to an
        # already used date should increment the billing counter only once.

        batch_data[0]['date'] = date2
        batch_data[1]['date'] = date1

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=5)

        # Exchanging couriers of two routes and changing the dates
        # to already used date should not change the billing counter.

        batch_data[0]['courier_id'] = billing_env['courier1']['id']
        batch_data[1]['courier_id'] = billing_env['courier0']['id']
        batch_data[0]['date'] = date0
        batch_data[1]['date'] = date0

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=5)

        # Exchanging couriers of two routes and changing the dates to a
        # not yet used date should increase the billing counter by two.

        batch_data[0]['courier_id'] = billing_env['courier0']['id']
        batch_data[1]['courier_id'] = billing_env['courier1']['id']
        batch_data[0]['date'] = date3
        batch_data[1]['date'] = date3

        _batch_routes(system_env_with_db, batch_data)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=7)


@skip_if_remote
@pytest.mark.parametrize('apikeys_mode', [ApikeysMode.banned_apikey, ApikeysMode.unknown_apikey])
@pytest.mark.parametrize('use_batch', [False, True])
def test_fail_route_creation_and_modification_when_apikey_is_bad(system_env_with_db, apikeys_mode, use_batch):
    """
    This function tests that creating and modifying a route is impossible
    if company's apikey is banned or invalid.
    """

    with _create_billing_env(system_env_with_db, apikeys_mode) as billing_env:

        date0 = '2019-05-25'
        date1 = '2019-05-26'

        route_data = {
            'number': billing_env['route0_number'],
            'courier_id': billing_env['courier0']['id'],
            'depot_id': billing_env['depot']['id'],
            'date': date0
        }

        error_message = 'Company apikey is unknown or banned'

        # Creating a route of a company with an unknown/banned apikey should fail.
        start_counter_value = _get_counter_value(billing_env)
        _add_route(system_env_with_db, route_data, use_batch, expected_status_code=requests.codes.forbidden, expected_message=error_message)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=0)

        # Temporarily set a valid apikey and create a route (the route is needed for testing
        # route modification in the next code block).
        start_counter_value = _get_counter_value(billing_env)
        billing_env['apikeys_mode'] = ApikeysMode.active_apikey
        patch_company(system_env_with_db, {'apikey': apikey_values.ACTIVE})
        _add_route(system_env_with_db, route_data, use_batch)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=1)

        # Modification of a route of a company with an unknown/banned apikey should fail.
        start_counter_value = _get_counter_value(billing_env)
        billing_env['apikeys_mode'] = apikeys_mode
        patch_company(system_env_with_db, {'apikey': billing_env['apikey']})
        route_data['date'] = date1
        _modify_route(system_env_with_db, route_data, use_batch, expected_status_code=requests.codes.forbidden, expected_message=error_message)
        _check_counter_value(billing_env, start_counter_value, potential_counter_increase=0)
