import pytest
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_delete, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user

from ya_courier_backend.models import UserRole


def _prepare_user_data(env, login, role):
    return {"login": login, "role": role.value}


def _get_user_ids(users):
    return [user["id"] for user in users]


@skip_if_remote
@pytest.mark.parametrize("role", [UserRole.admin, UserRole.manager, UserRole.dispatcher])
def test_only_admin_can_add_users(env: Environment, role):
    admin_id, admin_auth = add_user(env, "test_admin", UserRole.admin)
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)
    dispatcher_id, dispatcher_auth = add_user(env, "test_dispatcher", UserRole.dispatcher)
    app_id, app_auth = add_user(env, "test_app", UserRole.app)

    path = f"/api/v1/companies/{env.default_company.id}/users"
    new_user = _prepare_user_data(env, "new_test_user", role)
    local_post(env.client, path, headers=app_auth, data=new_user, expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, path, headers=dispatcher_auth, data=new_user, expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, path, headers=manager_auth, data=new_user, expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, path, headers=admin_auth, data=new_user, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_only_admin_manager_can_add_app_users(env: Environment):
    admin_id, admin_auth = add_user(env, "test_admin", UserRole.admin)
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)
    dispatcher_id, dispatcher_auth = add_user(env, "test_dispatcher", UserRole.dispatcher)
    app_id, app_auth = add_user(env, "test_app", UserRole.app)

    path = f"/api/v1/companies/{env.default_company.id}/users"
    new_app = _prepare_user_data(env, "new_app", UserRole.app)
    new_app2 = _prepare_user_data(env, "new_app2", UserRole.app)
    local_post(env.client, path, headers=app_auth, data=new_app, expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, path, headers=dispatcher_auth, data=new_app, expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, path, headers=manager_auth, data=new_app, expected_status=HTTPStatus.OK)
    local_post(env.client, path, headers=admin_auth, data=new_app2, expected_status=HTTPStatus.OK)

    path_app = f"/api/v1/companies/{env.default_company.id}/app-user"
    app_login1 = {"login": "new_app3"}
    app_login2 = {"login": "new_app4"}
    local_post(env.client, path_app, headers=app_auth, data=app_login1, expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, path_app, headers=dispatcher_auth, data=app_login1, expected_status=HTTPStatus.FORBIDDEN)
    local_post(env.client, path_app, headers=manager_auth, data=app_login2, expected_status=HTTPStatus.OK)
    local_post(env.client, path_app, headers=admin_auth, data=app_login2, expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize("role", [UserRole.admin, UserRole.manager, UserRole.dispatcher])
def test_only_admin_can_delete_users(env: Environment, role):
    admin_id, admin_auth = add_user(env, "test_admin", UserRole.admin)
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)
    dispatcher_id, dispatcher_auth = add_user(env, "test_dispatcher", UserRole.dispatcher)
    app_id, app_auth = add_user(env, "test_app", UserRole.app)

    user_id, _ = add_user(env, "new_test_user", role)
    user_path = f"/api/v1/companies/{env.default_company.id}/users/{user_id}"
    local_delete(env.client, user_path, headers=app_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_delete(env.client, user_path, headers=dispatcher_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_delete(env.client, user_path, headers=manager_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_delete(env.client, user_path, headers=admin_auth, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_only_admin_manager_can_delete_app_users(env: Environment):
    admin_id, admin_auth = add_user(env, "test_admin", UserRole.admin)
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)
    dispatcher_id, dispatcher_auth = add_user(env, "test_dispatcher", UserRole.dispatcher)
    app_id1, app_auth = add_user(env, "test_app", UserRole.app)
    app_id2, _ = add_user(env, "test_app2", UserRole.app)

    path = f"/api/v1/companies/{env.default_company.id}/users"
    local_delete(env.client, f"{path}/{app_id2}", headers=app_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_delete(env.client, f"{path}/{app_id1}", headers=dispatcher_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_delete(env.client, f"{path}/{app_id1}", headers=manager_auth, expected_status=HTTPStatus.OK)
    local_delete(env.client, f"{path}/{app_id2}", headers=admin_auth, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_only_admin_manager_dispatcher_can_get_other_users(env: Environment):
    admin_id, admin_auth = add_user(env, "test_admin", UserRole.admin)
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)
    dispatcher_id, dispatcher_auth = add_user(env, "test_dispatcher", UserRole.dispatcher)
    app_id, app_auth = add_user(env, "test_app", UserRole.app)

    admin2, _ = add_user(env, "test_admin2", UserRole.admin)
    manager2, _ = add_user(env, "test_manager2", UserRole.manager)
    dispatcher2, _ = add_user(env, "test_dispatcher2", UserRole.dispatcher)
    app2, _ = add_user(env, "test_app2", UserRole.app)
    all_users = [env.default_user.id, admin_id, manager_id, dispatcher_id, app_id, admin2, manager2, dispatcher2, app2]

    path = f"/api/v1/companies/{env.default_company.id}/users"
    assert _get_user_ids(local_get(env.client, path, headers=app_auth)) == [app_id]
    assert _get_user_ids(local_get(env.client, path, headers=dispatcher_auth)) == [dispatcher_id, app_id, app2]
    assert _get_user_ids(local_get(env.client, path, headers=manager_auth)) == [manager_id, app_id, app2]
    assert _get_user_ids(local_get(env.client, path, headers=admin_auth)) == all_users

    local_get(env.client, f"{path}/{app2}", headers=app_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, f"{path}/{app2}", headers=dispatcher_auth, expected_status=HTTPStatus.OK)
    local_get(env.client, f"{path}/{app2}", headers=manager_auth, expected_status=HTTPStatus.OK)
    local_get(env.client, f"{path}/{app2}", headers=admin_auth, expected_status=HTTPStatus.OK)

    local_get(env.client, f"{path}/{dispatcher2}", headers=app_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, f"{path}/{dispatcher2}", headers=dispatcher_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, f"{path}/{dispatcher2}", headers=manager_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, f"{path}/{dispatcher2}", headers=admin_auth, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_only_admin_can_access_other_users_shared_companies(env: Environment):
    admin_id, admin_auth = add_user(env, "test_admin", UserRole.admin)
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)
    dispatcher_id, dispatcher_auth = add_user(env, "test_dispatcher", UserRole.dispatcher)
    app_id, app_auth = add_user(env, "test_app", UserRole.app)

    base_path = f"/api/v1/companies/{env.default_company.id}/user_shared_company"
    path_manager = f"{base_path}/{manager_id}"
    path_dispatcher = f"{base_path}/{dispatcher_id}"
    path_app = f"{base_path}/{app_id}"

    # Admin can access all users shared companies
    local_get(env.client, path_manager, headers=admin_auth, expected_status=HTTPStatus.OK)
    local_get(env.client, path_dispatcher, headers=admin_auth, expected_status=HTTPStatus.OK)
    local_get(env.client, path_app, headers=admin_auth, expected_status=HTTPStatus.OK)

    # App cannot access shared companies for all users
    local_get(env.client, path_app, headers=app_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, path_manager, headers=app_auth, expected_status=HTTPStatus.FORBIDDEN)

    # Manager and dispatcher can access themselves only
    local_get(env.client, path_manager, headers=manager_auth, expected_status=HTTPStatus.OK)
    local_get(env.client, path_dispatcher, headers=dispatcher_auth, expected_status=HTTPStatus.OK)
    local_get(env.client, path_dispatcher, headers=manager_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, path_manager, headers=dispatcher_auth, expected_status=HTTPStatus.FORBIDDEN)
