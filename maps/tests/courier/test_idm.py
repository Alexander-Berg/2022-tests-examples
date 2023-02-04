from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_post_urlencoded,
    local_get,
)
from ya_courier_backend.models import db, User, UserInternalData
from ya_courier_backend.util.idm import IDM_YCB_INFO


def _get_idm_post_data(env: Environment):
    return {
        'login': 'test_user',
        'role': {'group': 'superuser'},
        'path': '/group/superuser/',
        'fields': {'passport_login': env.default_user.login},
    }


def _local_post_idm(client, path, query=None, headers=None, data=None, expected_status=HTTPStatus.OK, expected_code=0):
    response = local_post_urlencoded(client, path, query, headers, data, expected_status)
    assert response['code'] == expected_code
    return response


def _current_user_is_super(env):
    path_current_user = '/api/v1/current_user'
    return local_get(env.client, path_current_user, headers=env.user_auth_headers)['is_super']


@skip_if_remote
def test_idm_valid_add_role(env: Environment):
    path_idm_add = '/api/v1/internal/idm/add-role/'
    data = _get_idm_post_data(env)

    _local_post_idm(env.client, path_idm_add, headers=env.tvm_auth_headers, data=data)
    assert _current_user_is_super(env)

    resp = _local_post_idm(env.client, path_idm_add, headers=env.tvm_auth_headers, data=data)
    assert _current_user_is_super(env)
    assert 'warning' in resp


@skip_if_remote
def test_idm_valid_remove_role(env: Environment):
    path_idm_add = '/api/v1/internal/idm/add-role/'
    data = _get_idm_post_data(env)
    _local_post_idm(env.client, path_idm_add, headers=env.tvm_auth_headers, data=data)
    assert _current_user_is_super(env)

    path_idm_remove = '/api/v1/internal/idm/remove-role/'
    _local_post_idm(env.client, path_idm_remove, headers=env.tvm_auth_headers, data=data)
    assert not _current_user_is_super(env)


@skip_if_remote
def test_idm_valid_remove_role_and_fire(env: Environment):
    path_idm_add = '/api/v1/internal/idm/add-role/'

    data = _get_idm_post_data(env)
    _local_post_idm(env.client, path_idm_add, headers=env.tvm_auth_headers, data=data)

    with env.flask_app.app_context():
        user_count = len(db.session.query(User).all())

    data['fired'] = 1
    path_idm_remove = '/api/v1/internal/idm/remove-role/'
    _local_post_idm(env.client, path_idm_remove, headers=env.tvm_auth_headers, data=data)

    with env.flask_app.app_context():
        assert len(db.session.query(User).all()) == user_count - 1


@skip_if_remote
def test_idm_get_all_roles(env: Environment):
    path_idm_add = '/api/v1/internal/idm/add-role/'
    data = _get_idm_post_data(env)
    _local_post_idm(env.client, path_idm_add, headers=env.tvm_auth_headers, data=data)

    path_idm_get = '/api/v1/internal/idm/get-all-roles/'
    roles = local_get(env.client, path_idm_get, headers=env.tvm_auth_headers)
    with env.flask_app.app_context():
        initial_data_count = len(db.session.query(UserInternalData).all())

    assert len(roles['users']) == initial_data_count

    path_idm_remove = '/api/v1/internal/idm/remove-role/'
    _local_post_idm(env.client, path_idm_remove, headers=env.tvm_auth_headers, data=data)

    roles = local_get(env.client, path_idm_get, headers=env.tvm_auth_headers)
    with env.flask_app.app_context():
        assert len(db.session.query(UserInternalData).all()) == initial_data_count - 1

    assert len(roles['users']) == initial_data_count - 1


@skip_if_remote
def test_idm_valid_get_info(env: Environment):
    path_idm = '/api/v1/internal/idm/info/'

    assert IDM_YCB_INFO == local_get(env.client, path_idm, headers=env.tvm_auth_headers)
