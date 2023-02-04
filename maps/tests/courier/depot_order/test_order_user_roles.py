import pytest
from copy import deepcopy
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get, local_patch, local_post, local_delete
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user, add_user_depot, add_depot

from ya_courier_backend.models import UserRole


TEST_ORDER = {
    "number": "test_order",
    "time_interval": "11:00 - 23:00",
    "address": "ул. Льва Толстого, 16",
    "lat": 55.7447,
    "lon": 37.6728,
    "customer_number": "224",
    "route_number": "test_route",
}


def _add_route(env, route_number=None, depot_number=None):
    if not route_number:
        route_number = "test_route"
    if not depot_number:
        depot_number = env.default_depot.number

    path = f"/api/v1/companies/{env.default_company.id}/routes"
    data = {
        "number": route_number,
        "courier_number": env.default_courier.number,
        "depot_number": depot_number,
        "date": "2020-11-30",
    }
    return local_post(env.client, path, headers=env.user_auth_headers, data=data)


@skip_if_remote
def test_manager_orders_post(env: Environment):
    _add_route(env)

    manager_login = "test_manager"
    manager_id, manager_auth = add_user(env, manager_login, UserRole.manager)

    path = f"/api/v1/companies/{env.default_company.id}/orders"
    msg = local_post(env.client, path, headers=manager_auth, data=TEST_ORDER, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {"message": f"You do not have access to this depot of your company: {env.default_depot.id}."}

    add_user_depot(env, manager_id)
    local_post(env.client, path, headers=manager_auth, data=TEST_ORDER, expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize("role", [UserRole.manager, UserRole.dispatcher])
def test_manager_dispatcher_can_get_orders_only_for_own_depots(env: Environment, role):
    _add_route(env)
    path = f"/api/v1/companies/{env.default_company.id}/orders"
    order = local_post(env.client, path, headers=env.user_auth_headers, data=TEST_ORDER)
    order["notifications"] = []

    user_id, user_auth = add_user(env, "new_test_user", role)
    path_order = f"{path}/{order['id']}"
    path_track = f"{path}/{order['id']}/track-ids"
    msg = local_get(env.client, path_order, headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {"message": f"You do not have access to this depot of your company: {env.default_depot.id}."}
    msg = local_get(env.client, path_track, headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {"message": f"You do not have access to this depot of your company: {env.default_depot.id}."}
    assert local_get(env.client, path, headers=user_auth) == []

    add_user_depot(env, user_id)
    local_get(env.client, path_order, headers=user_auth, expected_status=HTTPStatus.OK)
    local_get(env.client, path_track, headers=user_auth, expected_status=HTTPStatus.OK)
    assert local_get(env.client, path, headers=user_auth) == [{**order, 'type': 'order'}]


@skip_if_remote
def test_manager_orders_patch(env: Environment):
    _add_route(env)
    path = f"/api/v1/companies/{env.default_company.id}/orders"
    data = deepcopy(TEST_ORDER)
    response = local_post(env.client, path, headers=env.user_auth_headers, data=data)

    manager_login = "test_manager"
    manager_id, manager_auth = add_user(env, manager_login, UserRole.manager)
    path_order = f"{path}/{response['id']}"

    data["customer_number"] = "100"
    msg = local_patch(env.client, path_order, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {"message": f"You do not have access to this depot of your company: {env.default_depot.id}."}

    add_user_depot(env, manager_id)
    local_patch(env.client, path_order, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)

    another_depot = add_depot(env, "1")
    another_route = _add_route(env, "test_route2", "1")
    data["route_id"] = another_route["id"]
    del data["route_number"]
    msg = local_patch(env.client, path_order, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {"message": f"You do not have access to this depot of your company: {another_depot.id}."}

    add_user_depot(env, manager_id, another_depot.id)
    local_patch(env.client, path_order, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_manager_orders_delete(env: Environment):
    _add_route(env)
    path = f"/api/v1/companies/{env.default_company.id}/orders"
    response = local_post(env.client, path, headers=env.user_auth_headers, data=TEST_ORDER)

    manager_login = "test_manager"
    manager_id, manager_auth = add_user(env, manager_login, UserRole.manager)
    path_order = f"{path}/{response['id']}"

    msg = local_delete(env.client, path_order, headers=manager_auth, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {"message": f"You do not have access to this depot of your company: {env.default_depot.id}."}

    add_user_depot(env, manager_id)
    local_delete(env.client, path_order, headers=manager_auth, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_manager_orders_batch_post(env: Environment):
    _add_route(env)

    manager_login = "test_manager"
    manager_id, manager_auth = add_user(env, manager_login, UserRole.manager)

    path = f"/api/v1/companies/{env.default_company.id}/orders-batch"
    data = [deepcopy(TEST_ORDER)]
    msg = local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {"message": f"You do not have access to this depot of your company: {env.default_depot.id}."}

    add_user_depot(env, manager_id)
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)

    another_depot = add_depot(env, "1")
    _add_route(env, "test_route2", "1")
    data[0]["route_number"] = "test_route2"
    msg = local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {"message": f"You do not have access to this depot of your company: {another_depot.id}."}

    add_user_depot(env, manager_id, another_depot.id)
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_manager_orders_batch_post_multiple_depots(env: Environment):
    _add_route(env)
    another_depot = add_depot(env, "1")
    _add_route(env, "test_route2", "1")

    manager_login = "test_manager"
    manager_id, manager_auth = add_user(env, manager_login, UserRole.manager)

    path = f"/api/v1/companies/{env.default_company.id}/orders-batch"
    data = [deepcopy(TEST_ORDER), deepcopy(TEST_ORDER)]
    data[0]["number"] = "test_order2"
    data[0]["route_number"] = "test_route2"
    msg = local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)
    assert msg == {
        "message": f"You do not have access to this depot of your company: {env.default_depot.id}, {another_depot.id}."
    }

    add_user_depot(env, manager_id)
    add_user_depot(env, manager_id, another_depot.id)
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)
