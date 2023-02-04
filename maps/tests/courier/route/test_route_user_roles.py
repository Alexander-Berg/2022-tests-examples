import pytest
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_patch, local_post, local_delete, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user_depot, add_depot, remove_user_foreign_key

from maps.b2bgeo.ya_courier.backend.test_lib.util_auth import add_user, UserType


def _get_route(env):
    return {
        "number": "1",
        "courier_number": env.default_courier.number,
        "depot_number": env.default_depot.number,
        "date": "2020-11-30",
    }


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [
        UserType.manager_jwt,
        UserType.manager_oauth,
    ],
)
def test_manager_routes_post(env: Environment, user_type):
    remove_user_foreign_key(env)
    manager_id, manager_auth = add_user(env, user_type)

    path = f"/api/v1/companies/{env.default_company.id}/routes"
    data = _get_route(env)
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id)
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [
        UserType.manager_jwt,
        UserType.manager_oauth,
    ],
)
def test_manager_routes_delete(env: Environment, user_type):
    remove_user_foreign_key(env)
    path = f"/api/v1/companies/{env.default_company.id}/routes"
    route = local_post(env.client, path, headers=env.user_auth_headers, data=_get_route(env))

    manager_id, manager_auth = add_user(env, user_type)

    data = [route["id"]]
    local_delete(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id)
    local_delete(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [
        UserType.manager_jwt,
        UserType.manager_oauth,
    ],
)
def test_manager_route_patch(env: Environment, user_type):
    remove_user_foreign_key(env)
    path = f"/api/v1/companies/{env.default_company.id}/routes"
    route = local_post(env.client, path, headers=env.user_auth_headers, data=_get_route(env))

    manager_id, manager_auth = add_user(env, user_type)

    route_path = f"{path}/{route['id']}"
    data = _get_route(env)
    data["data"] = "2020-12-30"
    local_patch(env.client, route_path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id)
    local_patch(env.client, route_path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)

    another_depot = add_depot(env, '1')
    data['depot_number'] = another_depot.number
    local_patch(env.client, route_path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id, another_depot.id)
    local_patch(env.client, route_path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [
        UserType.manager_jwt,
        UserType.manager_oauth,
    ],
)
def test_manager_route_delete(env: Environment, user_type):
    remove_user_foreign_key(env)
    path = f"/api/v1/companies/{env.default_company.id}/routes"
    route = local_post(env.client, path, headers=env.user_auth_headers, data=_get_route(env))

    manager_id, manager_auth = add_user(env, user_type)

    route_path = f"{path}/{route['id']}"
    local_delete(env.client, route_path, headers=manager_auth, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id)
    local_delete(env.client, route_path, headers=manager_auth, expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [
        UserType.manager_jwt,
        UserType.manager_oauth,
    ],
)
def test_manager_routes_batch_post(env: Environment, user_type):
    remove_user_foreign_key(env)
    manager_id, manager_auth = add_user(env, user_type)

    path = f"/api/v1/companies/{env.default_company.id}/routes-batch"
    data = [_get_route(env)]
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id)
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)

    another_depot = add_depot(env, '1')
    data[0]['depot_number'] = another_depot.number
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id, another_depot.id)
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [
        UserType.manager_jwt,
        UserType.manager_oauth,
        UserType.dispatcher_jwt,
        UserType.dispatcher_oauth,
    ],
)
def test_manager_dispatcher_can_get_routes_only_for_own_depots(env: Environment, user_type):
    remove_user_foreign_key(env)
    local_delete(env.client, f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}", headers=env.user_auth_headers)
    path = f"/api/v1/companies/{env.default_company.id}/routes"
    route = local_post(env.client, path, headers=env.user_auth_headers, data=_get_route(env))
    user_id, user_auth = add_user(env, user_type)

    route_path = f"{path}/{route['id']}"
    order_sequence_path = f"{route_path}/order-sequence"
    local_get(env.client, route_path, headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, order_sequence_path, headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)
    assert local_get(env.client, path, headers=user_auth) == []

    add_user_depot(env, user_id)
    assert local_get(env.client, route_path, headers=user_auth) == route
    assert local_get(env.client, path, headers=user_auth) == [route]
    local_get(env.client, order_sequence_path, headers=user_auth, expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [
        UserType.dispatcher_jwt,
        UserType.dispatcher_oauth,
        UserType.app_jwt,
        UserType.app_oauth,
    ],
)
def test_write_requests_are_forbidden_for_dispatcher_and_app(env: Environment, user_type):
    remove_user_foreign_key(env)
    base_path = f"/api/v1/companies/{env.default_company.id}"
    user_id, user_auth = add_user(env, user_type)
    data = _get_route(env)
    add_user_depot(env, user_id)

    local_post(env.client, f"{base_path}/routes", headers=user_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, f"{base_path}/routes-batch", headers=user_auth, data=[data], expected_status=HTTPStatus.FORBIDDEN)

    route = local_post(env.client, f"{base_path}/routes", headers=env.user_auth_headers, data=data)
    route_path = f"{base_path}/routes/{route['id']}"

    local_patch(env.client, route_path, headers=user_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)
    local_delete(env.client, route_path, headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)
