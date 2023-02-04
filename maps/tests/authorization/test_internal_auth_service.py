import pytest
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get

from ya_courier_backend.models import UserRole


def _auth_headers(env: Environment, user_auth: dict):
    return {**user_auth, **env.tvm_auth_headers}


@skip_if_remote
@pytest.mark.parametrize("role", [UserRole.manager, UserRole.dispatcher, UserRole.app])
def test_non_admin_cannot_access_full_company(env: Environment, role: UserRole):
    user_id, user_auth = add_user(env, "new_test_user", role)

    path = f'/api/v1/internal/auth/objects/search?permission=read&root_type=company&root_id={env.default_company.id}'
    assert local_get(env.client, path, headers=_auth_headers(env, user_auth)) == []


@skip_if_remote
def test_admin_has_access_to_full_company(env: Environment):
    user_id, user_auth = add_user(env, "new_test_user", UserRole.admin)

    path = f'/api/v1/internal/auth/objects/search?permission=read&root_type=company&root_id={env.default_company.id}'
    expected = [{'type': 'company', 'id': str(env.default_company.id)}]
    assert local_get(env.client, path, headers=_auth_headers(env, user_auth)) == expected


@skip_if_remote
def test_admin_has_no_access_to_another_company(env: Environment):
    user_id, user_auth = add_user(env, "new_test_user", UserRole.admin)

    path = f'/api/v1/internal/auth/objects/search?permission=read&root_type=company&root_id={env.default_shared_company.id}'
    local_get(env.client, path, headers=_auth_headers(env, user_auth), expected_status=HTTPStatus.FORBIDDEN)


@skip_if_remote
def test_required_param_absence_results_in_unprocessable_entity(env: Environment):
    user_id, user_auth = add_user(env, "new_test_user", UserRole.admin)

    path = f'/api/v1/internal/auth/objects/search?root_type=company&root_id={env.default_company.id}'
    local_get(env.client, path, headers=_auth_headers(env, user_auth), expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    path = f'/api/v1/internal/auth/objects/search?permission=read&root_id={env.default_company.id}'
    local_get(env.client, path, headers=_auth_headers(env, user_auth), expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    path = '/api/v1/internal/auth/objects/search?permission=read&root_type=company'
    local_get(env.client, path, headers=_auth_headers(env, user_auth), expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_superuser_has_access_to_any_company(env: Environment):
    path = f'/api/v1/internal/auth/objects/search?permission=read&root_type=company&root_id={env.default_company.id}'
    expected = [{'type': 'company', 'id': str(env.default_company.id)}]
    assert local_get(env.client, path, headers=_auth_headers(env, env.superuser_auth_headers)) == expected

    path = f'/api/v1/internal/auth/objects/search?permission=read&root_type=company&root_id={env.default_shared_company.id}'
    expected = [{'type': 'company', 'id': str(env.default_shared_company.id)}]
    assert local_get(env.client, path, headers=_auth_headers(env, env.superuser_auth_headers)) == expected


@skip_if_remote
def test_unauthorized_in_tvm(env: Environment):
    path = f'/api/v1/internal/auth/objects/search?permission=read&root_type=company&root_id={env.default_company.id}'
    local_get(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.UNAUTHORIZED)
    local_get(env.client, path, headers=env.superuser_auth_headers, expected_status=HTTPStatus.UNAUTHORIZED)


@skip_if_remote
def test_unauthorized_in_oauth(env: Environment):
    path = f'/api/v1/internal/auth/objects/search?permission=read&root_type=company&root_id={env.default_company.id}'
    local_get(env.client, path, headers=env.tvm_auth_headers, expected_status=HTTPStatus.UNAUTHORIZED)
