import requests
import pytest
import dateutil.parser
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_delete_request,
    env_get_request,
    env_patch_request,
    env_post_request,
    create_tmp_user,
    create_tmp_users,
    create_tmp_company,
    format_post_user_data_from_dict,
)
from ya_courier_backend.models import UserRole
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


@pytest.fixture(scope='module')
def env_with_users(system_env_with_db):
    env = system_env_with_db
    user_roles = [UserRole.app, UserRole.dispatcher, UserRole.manager, UserRole.admin]
    with create_tmp_company(env, "Test company test_current_user") as company_id:
        with create_tmp_users(env, [company_id] * len(user_roles), user_roles) as users:
            yield {
                'dbenv': env,
                'company_id': company_id,
                'users': dict(zip(user_roles, users)),
            }


def _get_current_user(env_with_users, user_role):
    response = env_get_request(
        env_with_users['dbenv'],
        'current_user',
        caller=env_with_users['users'][user_role]
    )
    assert response.status_code == requests.codes.ok
    return response.json()


def test_current_user(env_with_users):
    env = env_with_users
    for user_role in UserRole:
        j = _get_current_user(env, user_role)
        dateutil.parser.parse(j['confirmed_at'])  # throws ValueError if format is invalid
        assert isinstance(j['id'], int) and j['id'] > 0
        assert j['is_super'] is False
        assert isinstance(j['login'], str)
        assert j['role'] == user_role.value
        assert j.get('uid', None) is None
        assert 'company_users' in j


def test_current_user_multiple_companies(system_env_with_db):
    env = system_env_with_db
    with create_tmp_user(env, env.company_id, UserRole.app) as user:
        with create_tmp_company(env, "Test company test_current_user_multiple_companies") as company_id:
            resp = env_get_request(env, 'current_user', caller=user)
            assert resp.ok, resp.text
            assert len(resp.json()['company_users']) == 1
            assert resp.json()['company_users'][0]["company_id"] == system_env_with_db.company_id

            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, "users", company_id=company_id),
                data=format_post_user_data_from_dict(user),
                auth=system_env_with_db.auth_header_super)
            assert resp.ok, resp.text

            resp = env_get_request(env, 'current_user', caller=user)
            assert resp.ok, resp.text
            assert len(resp.json()['company_users']) == 2
            assert {user_company["company_id"] for user_company in resp.json()['company_users']} == {system_env_with_db.company_id, company_id}

            resp = env_get_request(
                system_env_with_db,
                path="companies",
                caller=user)
            assert resp.ok, resp.text
            assert len(resp.json()) == 2
            assert {company["id"] for company in resp.json()} == {system_env_with_db.company_id, company_id}

            resp = env_delete_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, "users", user['id'], company_id=company_id),
                data=user,
                auth=system_env_with_db.auth_header_super)
            assert resp.ok, resp.text

            resp = env_get_request(env, 'current_user', caller=user)
            assert resp.ok, resp.text
            assert len(resp.json()['company_users']) == 1
            assert {user_company["company_id"] for user_company in resp.json()['company_users']} == {system_env_with_db.company_id}


def test_current_user_after_patch(system_env_with_db):
    env = system_env_with_db
    with create_tmp_user(env, env.company_id, UserRole.admin) as user:

        resp = env_get_request(env, 'current_user', caller=user)
        assert resp.ok, resp.text
        cur_user = resp.json()

        assert len(cur_user['company_users']) == 1
        assert 'created_at' in cur_user['company_users'][0]
        assert env.company_id == cur_user['company_users'][0]['company_id']
        assert cur_user['company_users'][0]['company_name'] == 'Flash Logistics'
        assert cur_user['role'] == cur_user['company_users'][0]['role'] == UserRole.admin.value

        path = f"users/{cur_user['id']}"
        resp = env_patch_request(
            env,
            path=api_path_with_company_id(env, path, company_id=env.company_id),
            data={'role': UserRole.app.value}
        )
        assert resp.ok, resp.text

        resp = env_get_request(env, 'current_user', caller=user)
        assert resp.ok, resp.text
        cur_user_new = resp.json()

        cur_user['role'] = UserRole.app.value
        cur_user['company_users'][0]['role'] = UserRole.app.value

        assert cur_user == cur_user_new


@skip_if_remote
def test_cookie_unregistered(system_env_with_db):
    env = system_env_with_db
    cookies = {'Session_id': 'dummy_user', 'sessionid2': '123456'}
    response = requests.get(url=f"{env.url}/api/v1/current_user", cookies=cookies)
    assert response.status_code == 200


@skip_if_remote
def test_cookie_registered(system_env_with_db):
    env = system_env_with_db
    with create_tmp_user(env, env.company_id, UserRole.manager) as user:
        cookies = {'Session_id': user['login'], 'sessionid2': str(user['id'])}
        response = requests.get(url=f"{env.url}/api/v1/current_user", cookies=cookies)
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert 'passportUser' in j


def test_registered(system_env_with_db):
    env = system_env_with_db
    with create_tmp_user(env, env.company_id, UserRole.manager) as user:
        response = env_get_request(env, 'current_user', caller=user)
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert 'passportUser' in j
    assert 'uid' in j['passportUser']
    assert 'accounts' in j['passportUser']
