from http import HTTPStatus
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user
from ya_courier_backend.models import db, User

from ya_courier_backend.models import UserRole


def _make_auth(login, uid):
    return {'Authorization': login + ':' + uid}


def _create_test_user_with_fixed_uid(env, role):
    user_id, user_auth = add_user(env, "auth_test_user", role)

    # First request fixes user uid in the database
    path = f"/api/v1/companies/{env.default_company.id}/users/{user_id}"
    result = local_get(env.client, path, headers=user_auth, expected_status=HTTPStatus.OK)
    assert result['login'] == 'auth_test_user'

    user_login, user_uid = user_auth['Authorization'].split(':')
    return user_id, user_login, user_uid


@skip_if_remote
def test_oauth_no_such_user(env: Environment):
    path = f"/api/v1/companies/{env.default_company.id}/users/{env.default_user.id}"
    result = local_get(
        env.client, path, headers=_make_auth('new_login', 'new_uid'),
        expected_status=HTTPStatus.UNAUTHORIZED
    )
    assert result['error'] == 'NoSuchUser'


@skip_if_remote
@pytest.mark.parametrize("role", [UserRole.app, UserRole.admin, UserRole.manager, UserRole.dispatcher])
def test_oauth_login_change(env: Environment, role: UserRole):
    user_id, user_login, user_uid = _create_test_user_with_fixed_uid(env, role)
    path = f"/api/v1/companies/{env.default_company.id}/users/{user_id}"

    result = local_get(
        env.client, path, headers=_make_auth('new_login', user_uid),
        expected_status=HTTPStatus.OK
    )
    assert result['login'] == 'new_login'


@skip_if_remote
@pytest.mark.parametrize("role", [UserRole.admin, UserRole.manager, UserRole.dispatcher])
def test_uid_mismatch_for_non_app_roles(env: Environment, role: UserRole):
    user_id, user_login, user_uid = _create_test_user_with_fixed_uid(env, role)
    path = f"/api/v1/companies/{env.default_company.id}/users/{user_id}"
    result = local_get(env.client, path, headers=_make_auth(user_login, "new_uid"), expected_status=HTTPStatus.UNAUTHORIZED)
    assert result["error"] == "UidMismatch"


@skip_if_remote
def test_uid_mismatch_is_ignored_and_updated_for_app(env: Environment):
    user_id, user_login, user_uid = _create_test_user_with_fixed_uid(env, UserRole.app)
    path = f"/api/v1/companies/{env.default_company.id}/users/{user_id}"
    result = local_get(env.client, path, headers=_make_auth(user_login, "new_uid"), expected_status=HTTPStatus.OK)
    assert result["login"] == "auth_test_user"

    with env.flask_app.app_context():
        user = db.session.query(User).filter(User.id == user_id).first()
        assert user.uid == "new_uid"
