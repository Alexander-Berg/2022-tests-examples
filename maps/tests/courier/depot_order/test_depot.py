from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_delete, local_get, local_post, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user, add_user_depot

from ya_courier_backend.models import UserRole


DEPOT_DATA_WITHOUT_NUMBER = {
    'address': 'Some address',
    'lat': 55,
    'lon': 33
}


def _get_route(env, route_number, depot_number):
    return {
        'number': route_number,
        'courier_number': env.default_courier.number,
        'depot_number': depot_number,
        'date': '2020-11-30',
    }


def _create_route(env, route_number, depot_number):
    path_route = f'/api/v1/companies/{env.default_company.id}/routes'
    route_data = _get_route(env, route_number, depot_number)
    route = local_post(env.client, path_route, headers=env.user_auth_headers, data=route_data)

    return route


@skip_if_remote
def test_depot_shallow_delete(env: Environment):
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    depots_path = f'/api/v1/companies/{env.default_company.id}/depots'

    depot_count = len(local_get(env.client, depots_path, headers=env.user_auth_headers))

    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    local_get(env.client, depot_path, headers=env.user_auth_headers, expected_status=HTTPStatus.GONE)
    resp = local_get(env.client, depots_path, headers=env.user_auth_headers)
    assert len(resp) == depot_count - 1


@skip_if_remote
def test_depot_shallow_delete_wrong(env: Environment):
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id + 5}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers,
                 expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_depot_shallow_delete_twice(env: Environment):
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    local_get(env.client, depot_path, headers=env.user_auth_headers, expected_status=HTTPStatus.GONE)
    add_depot_path = f'/api/v1/companies/{env.default_company.id}/depots'
    resp = local_post(env.client, add_depot_path, headers=env.user_auth_headers,
                      data={'number': env.default_depot.number, **DEPOT_DATA_WITHOUT_NUMBER})
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{resp["id"]}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)
    local_get(env.client, depot_path, headers=env.user_auth_headers, expected_status=HTTPStatus.GONE)


@skip_if_remote
def test_depot_shallow_delete_re_add(env: Environment):
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    add_depot_path = f'/api/v1/companies/{env.default_company.id}/depots'
    resp = local_post(env.client, add_depot_path, headers=env.user_auth_headers,
                      data={'number': env.default_depot.number, **DEPOT_DATA_WITHOUT_NUMBER})
    assert resp['id'] != env.default_depot.id


@skip_if_remote
def test_depots_shallow_delete_re_add_batch(env: Environment):
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    add_depots_batch_path = f'/api/v1/companies/{env.default_company.id}/depots-batch'
    resp = local_post(env.client, add_depots_batch_path, headers=env.user_auth_headers,
                      data=[{'number': env.default_depot.number, **DEPOT_DATA_WITHOUT_NUMBER}])
    assert resp == {'inserted': 1, 'updated': 0}


@skip_if_remote
def test_patch_route_with_depot_number(env: Environment):
    route = _create_route(env, '1', env.default_second_depot.number)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    add_depot_path = f'/api/v1/companies/{env.default_company.id}/depots'
    new_depot = local_post(env.client, add_depot_path, headers=env.user_auth_headers,
                             data={'number': env.default_depot.number, **DEPOT_DATA_WITHOUT_NUMBER})

    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    route = local_patch(env.client, path_route, headers=env.user_auth_headers,
                        data={'depot_number': env.default_depot.number})

    assert route['depot_id'] == new_depot['id']


@skip_if_remote
def test_patch_route_with_deleted_depot(env: Environment):
    route = _create_route(env, '1', env.default_depot.number)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    route = local_patch(env.client, path_route, headers=env.user_auth_headers,
                        data={'courier_number': env.default_courier.number})


@skip_if_remote
def test_courier_quality_for_deleted_depot(env: Environment):
    # Import route with orders, depot and garage in the end
    task_id = 'mock_task_uuid__result_with_with_depot_and_garage_in_the_end'
    path_import = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}'
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    # Get courier-quality
    date = '2019-12-13'
    path_courier_quality = f'/api/v1/companies/{env.default_company.id}/courier-quality?date={date}'
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    assert courier_quality != []

    # Delete depot
    depot_id = routes[0]['depot_id']
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{depot_id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    # Get courier-quality again
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    assert courier_quality != []


@skip_if_remote
def test_only_allowed_depots_are_shown_in_the_list_for_dispatcher_and_manager(env: Environment):
    admin_id, admin_auth = add_user(env, 'test_admin', UserRole.admin)
    manager_id, manager_auth = add_user(env, 'test_manager', UserRole.manager)
    dispatcher_id, dispatcher_auth = add_user(env, 'test_dispatcher', UserRole.dispatcher)

    depots_path = f'/api/v1/companies/{env.default_company.id}/depots'
    default_depot = local_get(env.client, f'{depots_path}/{env.default_depot.id}', headers=admin_auth)
    second_depot = local_get(env.client, f'{depots_path}/{env.default_second_depot.id}', headers=admin_auth)

    assert local_get(env.client, depots_path, headers=admin_auth) == [default_depot, second_depot]
    assert local_get(env.client, depots_path, headers=manager_auth) == []
    assert local_get(env.client, depots_path, headers=dispatcher_auth) == []

    add_user_depot(env, manager_id)
    add_user_depot(env, dispatcher_id)

    assert local_get(env.client, depots_path, headers=manager_auth) == [default_depot]
    assert local_get(env.client, depots_path, headers=dispatcher_auth) == [default_depot]


@skip_if_remote
def test_depot_long_name_post(env: Environment):

    add_depot_path = f'/api/v1/companies/{env.default_company.id}/depots'
    invalid_post_response = local_post(env.client, add_depot_path, headers=env.user_auth_headers,
                                       data={'number': 'invalid_depot', 'name': 'd' * 81, **DEPOT_DATA_WITHOUT_NUMBER},
                                       expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    assert "Failed validating 'maxLength' in schema['allOf'][0]['properties']['name']" in invalid_post_response['message']


@skip_if_remote
def test_depot_long_name_patch(env: Environment):
    patch_depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'

    invalid_patch_response = local_patch(env.client, patch_depot_path, headers=env.user_auth_headers,
                                         data={'name': 'd' * 81}, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    assert "Failed validating 'maxLength' in schema['properties']['name']" in invalid_patch_response['message']
