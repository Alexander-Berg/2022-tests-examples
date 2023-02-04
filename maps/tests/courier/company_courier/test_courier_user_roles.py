import pytest
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_delete, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_auth import add_user, UserType


def _get_courier(number):
    return {"name": "test_courier_name", "number": number, "sms_enabled": False}


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [UserType.company_jwt, UserType.admin_jwt, UserType.manager_jwt, UserType.admin_oauth, UserType.manager_oauth],
)
def test_admin_manager_can_post_and_delete(env: Environment, user_type):
    _, user_auth = add_user(env, user_type)
    path = f"/api/v1/companies/{env.default_company.id}/couriers"
    courier = local_post(env.client, path, headers=user_auth, data=_get_courier('1'))

    path = f"{path}/{courier['id']}"
    local_delete(env.client, path, headers=user_auth)


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [UserType.company_jwt, UserType.admin_jwt, UserType.manager_jwt, UserType.admin_oauth, UserType.manager_oauth],
)
def test_admin_manager_can_batch_post(env: Environment, user_type):
    _, user_auth = add_user(env, user_type)

    path = f"/api/v1/companies/{env.default_company.id}/couriers-batch"
    local_post(env.client, path, headers=user_auth, data=[_get_courier('1'), _get_courier('2')])


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [UserType.dispatcher_jwt, UserType.app_jwt, UserType.dispatcher_oauth, UserType.app_oauth],
)
def test_write_requests_are_forbidden_for_dispatcher_and_app(env: Environment, user_type):
    base_path = f"/api/v1/companies/{env.default_company.id}"
    _, user_auth = add_user(env, user_type)

    local_post(env.client, f"{base_path}/couriers", headers=user_auth, data=_get_courier('2'), expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, f"{base_path}/couriers-batch", headers=user_auth, data=[_get_courier('2')], expected_status=HTTPStatus.FORBIDDEN)
    local_delete(env.client, f"{base_path}/couriers/{env.default_courier.id}", headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)


@skip_if_remote
@pytest.mark.parametrize(
    "user_type",
    [
        UserType.company_jwt,
        UserType.admin_jwt,
        UserType.manager_jwt,
        UserType.dispatcher_jwt,
        UserType.admin_oauth,
        UserType.manager_oauth,
        UserType.dispatcher_oauth,
    ],
)
def test_admin_manager_dispatcher_can_get_couriers(env: Environment, user_type):
    path = f"/api/v1/companies/{env.default_company.id}/couriers"
    courier = local_post(env.client, path, headers=env.user_auth_headers, data=_get_courier('1'))
    _, user_auth = add_user(env, user_type)
    received_couriers = local_get(env.client, path, headers=user_auth)
    assert [courier['id'] for courier in received_couriers] == [env.default_courier.id, courier['id']]
    assert local_get(env.client, f"{path}/{courier['id']}", headers=user_auth) == courier

    path_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date=2020-12-04"
    assert local_get(env.client, path_quality, headers=user_auth) == []
