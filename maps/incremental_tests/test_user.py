import requests
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    create_tmp_companies,
    create_tmp_company,
    create_tmp_user,
    create_tmp_users,
    env_delete_request,
    env_get_request,
    env_patch_request,
    env_post_request,
)
from ya_courier_backend.models import UserRole

URI = 'users'

TEST_USER = None


class TestUser(object):
    def test_init(self, system_env_with_db):
        response = env_get_request(system_env_with_db,
                                   path=api_path_with_company_id(system_env_with_db, URI))
        assert response.status_code == requests.codes.ok
        users = response.json()
        for x in users:
            if x['login'] in ['test-super-user', 'test-admin-user', ' test-manager-user', 'test_login_user']:
                d = env_delete_request(system_env_with_db, path=api_path_with_company_id(system_env_with_db, URI, x['id']))
                assert d.status_code == requests.codes.ok

    def test_create_user_schema(self, system_env_with_db):
        request_user = {'company_id': system_env_with_db.company_id}
        response = env_post_request(system_env_with_db,
                                    path=api_path_with_company_id(system_env_with_db, URI),
                                    data=request_user)
        assert response.status_code == requests.codes.unprocessable
        assert "UserPost: Additional properties are not allowed ('company_id' was unexpected)" in response.json()['message']

        request_user = {
            'login': '123',
            'role': 123
        }
        response = env_post_request(system_env_with_db,
                                    path=api_path_with_company_id(system_env_with_db, URI),
                                    data=request_user)
        assert response.status_code == requests.codes.unprocessable
        assert "123 is not of type 'string'" in response.json()['message']

    def test_add_admin_user(self, system_env_with_db):
        request_user = {'login': 'test-admin-user'}
        response = env_post_request(system_env_with_db,
                                    path=api_path_with_company_id(system_env_with_db, URI),
                                    data=request_user)

        assert response.status_code == requests.codes.ok
        user = response.json()
        assert user['login'] == request_user['login']
        assert 'id' in user
        assert user['role'] == 'admin'
        global TEST_USER
        TEST_USER = user

    def test_add_existing_user(self, system_env_with_db):
        request_user = {
            'login': TEST_USER['login'],
            'role': 'admin',
        }
        response = env_post_request(system_env_with_db,
                                    path=api_path_with_company_id(system_env_with_db, URI),
                                    data=request_user)
        assert response.status_code == requests.codes.unprocessable
        assert f"User '{TEST_USER['login']}' already registered. Choose another login." in response.json()['message']

    def test_list(self, system_env_with_db):
        response = env_get_request(system_env_with_db,
                                   path=api_path_with_company_id(system_env_with_db, URI))

        assert response.status_code == requests.codes.ok
        users = response.json()
        assert len(users) > 0

    def test_list_fail(self, system_env_with_db):
        response = env_get_request(system_env_with_db,
                                   path=api_path_with_company_id(system_env_with_db, f"{URI}?page=0"))

        assert response.status_code == requests.codes.unprocessable
        assert 'Must be greater than or equal to 1' in response.content.decode()

    def test_get_by_id(self, system_env_with_db):
        response = env_get_request(system_env_with_db,
                                   path=api_path_with_company_id(system_env_with_db, URI, TEST_USER['id']))

        assert response.status_code == requests.codes.ok
        user = response.json()
        assert user['login'] == TEST_USER['login']

    def test_get_by_login_fail(self, system_env_with_db):
        response = env_get_request(system_env_with_db,
                                   path="user-info",
                                   auth=system_env_with_db.auth_header_super)
        assert response.status_code == requests.codes.unprocessable
        assert 'Missing data for required field' in response.content.decode()

    def test_get_by_login(self, system_env_with_db):
        def get_expected_response(user_data):
            expected_resp = {**user_data, 'company_ids': [system_env_with_db.company_id]}
            return expected_resp
        for login in [TEST_USER['login'], "*" + TEST_USER['login'][2:], "*" + TEST_USER['login'][2:12] + "*",
                      TEST_USER['login'][:4] + "*", "*", "**", TEST_USER['login'].replace('t', '?')]:
            response = env_get_request(system_env_with_db,
                                       path="user-info?login={}".format(login),
                                       auth=system_env_with_db.auth_header_super)
            assert response.ok, response.text
            assert get_expected_response(TEST_USER) in response.json()
        # Add new user
        test_user = {'login': 'test_login_user'}
        response = env_post_request(system_env_with_db,
                                    path=api_path_with_company_id(system_env_with_db, URI),
                                    data=test_user)
        assert response.ok, response.text
        test_user = response.json()

        response = env_get_request(system_env_with_db,
                                   path="user-info?login={}".format((test_user['login'][:13] + "*").replace('t', '?')),
                                   auth=system_env_with_db.auth_header_super)
        assert response.ok, response.text
        assert get_expected_response(test_user) in response.json()
        # Check TEST_USER and test_user can be found by login "test*"
        response = env_get_request(system_env_with_db,
                                   path="user-info?login={}".format("test*"),
                                   auth=system_env_with_db.auth_header_super)
        assert response.ok, response.text
        assert get_expected_response(TEST_USER) in response.json()
        assert get_expected_response(test_user) in response.json()
        # Remove added user
        response = env_delete_request(system_env_with_db, path=api_path_with_company_id(system_env_with_db, URI, test_user['id']))
        assert response.ok, response.text
        # Check deleted user can not be found by its login
        response = env_get_request(system_env_with_db,
                                   path="user-info?login={}".format('test_login_user'),
                                   auth=system_env_with_db.auth_header_super)
        assert response.ok, response.text
        assert response.json() == []
        # Check only superusers have access
        response = env_get_request(system_env_with_db,
                                   path="user-info?login={}".format(TEST_USER['login']))
        assert response.status_code == requests.codes.forbidden

    def test_get_by_uppercase_login(self, system_env_with_db):
        test_user = {'login': 'test_login_user'}
        response = env_post_request(system_env_with_db,
                                    path=api_path_with_company_id(system_env_with_db, URI),
                                    data=test_user)
        assert response.ok, response.text
        test_user = response.json()

        response = env_get_request(system_env_with_db,
                                   path="user-info?login={}".format(test_user['login'].upper),
                                   auth=system_env_with_db.auth_header_super)
        assert response.ok, response.text

    def test_update_user_schema(self, system_env_with_db):
        def try_patch(data):
            r = env_patch_request(system_env_with_db,
                                  path=api_path_with_company_id(system_env_with_db, URI, TEST_USER['id']),
                                  data=data)
            assert r.status_code == requests.codes.unprocessable
            return r.json()['message']

        assert "schema validation failed: UserPatch: 4141 is not of type 'string'" in try_patch({
            'login': 4141
        })

        assert "schema validation failed: UserPatch: Additional properties are not allowed ('is_super' was unexpected)" in try_patch({
            'is_super': 'True'
        })

    def test_patch(self, system_env_with_db):
        patched_user = {
            'role': 'manager'
        }
        r = env_patch_request(system_env_with_db,
                              path=api_path_with_company_id(system_env_with_db, URI, TEST_USER['id']),
                              data=patched_user)
        assert r.status_code == requests.codes.ok
        updated_user = r.json()

        assert updated_user['login'] == TEST_USER['login']
        assert updated_user['role'] == 'manager'

    def test_remove(self, system_env_with_db):
        d = env_delete_request(system_env_with_db,
                               path=api_path_with_company_id(system_env_with_db, URI, TEST_USER['id']))
        assert d.status_code == requests.codes.ok

        response = env_get_request(system_env_with_db,
                                   path=api_path_with_company_id(system_env_with_db, URI))

        assert response.status_code == requests.codes.ok
        assert TEST_USER['id'] not in [x['id'] for x in response.json()]

    def test_add_user_role(self, system_env_with_db):
        user_data = {
            'login': 'test-manager-user',
            'role': 'manager'
        }
        response = env_post_request(system_env_with_db,
                                    path=api_path_with_company_id(system_env_with_db, URI),
                                    data=user_data)

        assert response.status_code == requests.codes.ok
        user = response.json()
        assert user['role'] == 'manager'

        d = env_delete_request(system_env_with_db, path=api_path_with_company_id(system_env_with_db, URI, user['id']))
        assert d.status_code == requests.codes.ok

    def test_user_deleting_himself(self, system_env_with_db):
        # Only user with app role is allowed to delete himself
        test_data = {
            UserRole.admin: requests.codes.forbidden,
            UserRole.manager: requests.codes.forbidden,
            UserRole.dispatcher: requests.codes.forbidden,
            UserRole.app: requests.codes.ok
        }
        user_roles = list(test_data.keys())
        with create_tmp_users(system_env_with_db, [system_env_with_db.company_id] * len(user_roles), user_roles) as users:
            user_by_role = dict(zip(user_roles, users))
            for role, expected_status_code in test_data.items():
                assert env_delete_request(
                    system_env_with_db,
                    path=api_path_with_company_id(system_env_with_db, URI, user_by_role[role]['id']),
                    auth=system_env_with_db.get_user_auth(user_by_role[role])
                ).status_code == expected_status_code

    def test_deleting_other_user(self, system_env_with_db):
        test_data = {
            # Admin can delete other user with any role
            UserRole.admin: {
                UserRole.admin: requests.codes.ok,
                UserRole.manager: requests.codes.ok,
                UserRole.dispatcher: requests.codes.ok,
                UserRole.app: requests.codes.ok
            },
            # Manager can delete other user with app role
            UserRole.manager: {
                UserRole.admin: requests.codes.forbidden,
                UserRole.manager: requests.codes.forbidden,
                UserRole.dispatcher: requests.codes.forbidden,
                UserRole.app: requests.codes.ok
            },
            # Dispatcher user cannot delete other users
            UserRole.dispatcher: {
                UserRole.admin: requests.codes.forbidden,
                UserRole.manager: requests.codes.forbidden,
                UserRole.dispatcher: requests.codes.forbidden,
                UserRole.app: requests.codes.forbidden
            },
            # App user cannot delete other users
            UserRole.app: {
                UserRole.admin: requests.codes.forbidden,
                UserRole.manager: requests.codes.forbidden,
                UserRole.dispatcher: requests.codes.forbidden,
                UserRole.app: requests.codes.forbidden
            }
        }
        for requester_role in test_data.keys():
            with create_tmp_user(system_env_with_db, system_env_with_db.company_id, requester_role) as requester:
                user_roles = list(test_data[requester_role].keys())
                with create_tmp_users(system_env_with_db, [system_env_with_db.company_id] * len(user_roles), user_roles) as users:
                    user_by_role = dict(zip(user_roles, users))
                    for role, expected_status_code in test_data[requester_role].items():
                        assert env_delete_request(
                            system_env_with_db,
                            path=api_path_with_company_id(system_env_with_db, URI, user_by_role[role]['id']),
                            auth=system_env_with_db.get_user_auth(requester)
                        ).status_code == expected_status_code

    def test_deleting_user_from_other_company(self, system_env_with_db):
        user_roles = [
            UserRole.admin,
            UserRole.manager,
            UserRole.dispatcher,
            UserRole.app
        ]
        with create_tmp_company(system_env_with_db, "Test company test_deleting_users") as company_id:
            with create_tmp_users(system_env_with_db, [company_id] * len(user_roles), user_roles) as users:
                for user in users:
                    assert env_delete_request(
                        system_env_with_db,
                        path=api_path_with_company_id(system_env_with_db, URI, user['id'], company_id=company_id)
                    ).status_code == requests.codes.forbidden


class TestUserMultipleCompanies(object):
    def test_one_app_in_3_companies(self, system_env_with_db):
        """
            Test the following workflow:
                - create app registered in 3 companies (user with id user_id_1 created)
                - fail to patch role in one of the companies
                - patch login in all 3 companies (user with id user_id_2 created, user_id_1 removed after third patch)
                - fail to delete user by id user_id_1
                - delete user in 3 companies by id user_id_2
        """
        request_user = {'login': 'test-app-in-3-companies', 'role': 'app'}
        with create_tmp_companies(system_env_with_db, [f'Test company MultipleCompaniesOneUser {i}' for i in range(3)]) as company_ids:
            user_id = None

            for i in range(3):
                resp = env_post_request(
                    system_env_with_db,
                    path=api_path_with_company_id(system_env_with_db, URI, company_id=company_ids[i]),
                    data=request_user,
                    auth=system_env_with_db.auth_header_super)
                assert resp.ok, resp.text
                j = resp.json()
                assert j["role"] == UserRole.app.value
                if i == 0:
                    user_id = j["id"]
                else:
                    assert j["id"] == user_id

            resp = env_patch_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, URI, user_id, company_id=company_ids[0]),
                data={"role": UserRole.manager.value},
                auth=system_env_with_db.auth_header_super)
            assert resp.status_code == requests.codes.unprocessable, resp.text

            new_user_id = None
            for i in range(3):
                resp = env_patch_request(
                    system_env_with_db,
                    path=api_path_with_company_id(system_env_with_db, URI, user_id, company_id=company_ids[i]),
                    data={"login": "new_login"},
                    auth=system_env_with_db.auth_header_super)
                assert resp.ok, resp.text
                j = resp.json()
                assert j["role"] == UserRole.app.value
                if i == 0:
                    new_user_id = j["id"]
                else:
                    assert j["id"] == new_user_id

            for i in range(3):
                resp = env_delete_request(
                    system_env_with_db,
                    path=api_path_with_company_id(system_env_with_db, URI, user_id, company_id=company_ids[i]),
                    auth=system_env_with_db.auth_header_super)
                assert resp.status_code == requests.codes.not_found, resp.text

                resp = env_delete_request(
                    system_env_with_db,
                    path=api_path_with_company_id(system_env_with_db, URI, new_user_id, company_id=company_ids[i]),
                    auth=system_env_with_db.auth_header_super)
                assert resp.ok, resp.text

    @pytest.mark.parametrize('non_app_role', [UserRole.admin, UserRole.manager])
    def test_fail_on_non_app_role(self, system_env_with_db, non_app_role):
        request_user = {'login': 'test-non-app-multiple-companies', 'role': 'app'}
        with create_tmp_companies(system_env_with_db, [f'Test company MultipleCompaniesOneUser {i}' for i in range(2)]) as company_ids:
            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, URI, company_id=company_ids[0]),
                data=request_user,
                auth=system_env_with_db.auth_header_super)
            assert resp.ok, resp.text
            assert resp.json()["role"] == UserRole.app.value

            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, URI, company_id=company_ids[1]),
                data=dict(
                    request_user,
                    role=non_app_role.value
                ),
                auth=system_env_with_db.auth_header_super)
            assert resp.status_code == requests.codes.unprocessable

    def test_fail_to_add_app_twice_to_one_company(self, system_env_with_db):
        request_user = {'login': 'test-app-one-company', 'role': 'app'}
        with create_tmp_company(system_env_with_db, 'Test company MultipleCompaniesOneUser') as company_id:
            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, URI, company_id=company_id),
                data=request_user,
                auth=system_env_with_db.auth_header_super)
            assert resp.ok, resp.text

            assert resp.json()["role"] == UserRole.app.value

            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, URI, company_id=company_id),
                data=request_user,
                auth=system_env_with_db.auth_header_super)
            assert resp.status_code == requests.codes.unprocessable


@skip_if_remote
class TestAppUser(object):
    def test_add_app_user(self, system_env_with_db):
        request_user = {'login': 'test-login-app-user'}
        with create_tmp_company(system_env_with_db, 'TestAppUser Company') as company_id:
            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, 'app-user', company_id=company_id),
                data=request_user,
                auth=system_env_with_db.auth_header_super)
            assert resp.ok, resp.text

    def test_add_existing_app_user(self, system_env_with_db):
        request_user = {'login': 'test-login-app-user'}
        with create_tmp_company(system_env_with_db, 'TestAppUser Company') as company_id:
            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, 'app-user', company_id=company_id),
                data=request_user,
                auth=system_env_with_db.auth_header_super)
            assert resp.ok, resp.text

            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, 'app-user', company_id=company_id),
                data=request_user,
                auth=system_env_with_db.auth_header_super)
            assert resp.ok, resp.text

    def test_missing_login(self, system_env_with_db):
        request_user = {}
        with create_tmp_company(system_env_with_db, 'TestAppUser Company') as company_id:
            resp = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, 'app-user', company_id=company_id),
                data=request_user,
                auth=system_env_with_db.auth_header_super)
            assert resp.status_code == requests.codes.unprocessable, resp.text
            assert "Json schema validation failed: AppUserCreate: 'login' is a required property" in resp.json()['message'], resp.text
