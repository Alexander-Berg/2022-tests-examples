import requests
import pytest
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request, env_post_request,
    env_patch_request, env_delete_request,
    api_path_with_company_id,
    create_user,
    create_tmp_user,
    create_tmp_company,
    format_post_user_data_from_dict,
)
from maps.b2bgeo.ya_courier.backend.test_lib.config import PAGINATION_PAGE_SIZE
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.models.user import UserRole


TEST_USER = 'role-test-user'
TEST_USER_TMP = 'role-test-user-tmp'
TEST_ANOTHER_USER = 'role-test-another-user'
TEST_APP_USER = 'role-test-app-user'


class TestUserRoles(object):
    def user_path(self, env, user, company_id):
        return api_path_with_company_id(env, "users/{}".format(user["id"]), company_id=company_id)

    @pytest.mark.parametrize("role", ['admin', 'manager', 'dispatcher', 'app'])
    def test_user_role_access(self, system_env_with_db, role):
        env = system_env_with_db
        with create_tmp_company(env, 'Test user access') as company_id:
            with create_tmp_user(env, company_id, UserRole[role]) as user:
                with create_tmp_user(env, company_id, UserRole.manager) as another_user:
                    with create_tmp_user(env, company_id, UserRole.app) as app_user:
                        # Trying create user when login is missing
                        response = env_post_request(
                            env,
                            path=api_path_with_company_id(env, "users", company_id=company_id),
                            data={},
                            caller=user)
                        if role in ['app', 'dispatcher']:
                            assert response.status_code == requests.codes.forbidden
                        else:
                            assert response.status_code == requests.codes.unprocessable

                        # Trying create new user
                        for new_role in ['app', 'dispatcher', 'manager', 'admin', None]:
                            response = env_post_request(
                                env,
                                path=api_path_with_company_id(env, "users", company_id=company_id),
                                data={'login': TEST_USER_TMP} if new_role is None else {'login': TEST_USER_TMP, 'role': new_role},
                                caller=user)
                            if role == 'admin' or (role == 'manager' and new_role == 'app'):
                                assert response.ok
                                tmp_user = response.json()
                                assert tmp_user['login'] == TEST_USER_TMP
                                assert tmp_user['role'] == ('admin' if new_role is None else new_role)
                                response = env_delete_request(
                                    env,
                                    path=self.user_path(env, tmp_user, company_id=company_id),
                                    caller=user)
                                assert response.ok
                            else:
                                assert response.status_code == requests.codes.forbidden

                        # Getting list of users
                        response = env_get_request(
                            env,
                            path=api_path_with_company_id(env, "users", company_id=company_id),
                            caller=user)
                        assert response.ok
                        j = response.json()
                        if role == 'admin':
                            assert len(j) == 4
                        elif role in ['dispatcher', 'manager']:
                            assert len(j) == 2
                            assert j[0]['login'] == user['login']
                            assert j[1]['login'] == app_user['login']
                        else:
                            assert len(j) == 1
                            assert j[0]['login'] == user['login']

                        # Getting/modifying user
                        for test_user in [user, another_user, app_user]:
                            # Trying get user
                            response = env_get_request(
                                env,
                                path=self.user_path(env, test_user, company_id=company_id),
                                caller=user)
                            if test_user == user or role == 'admin' or (role in ['dispatcher', 'manager'] and test_user == app_user):
                                assert response.ok
                            else:
                                assert response.status_code == requests.codes.forbidden

                            # Trying modify user
                            response = env_patch_request(
                                env,
                                path=self.user_path(env, test_user, company_id=company_id),
                                data=format_post_user_data_from_dict(test_user),
                                caller=user)
                            if role == 'admin' or (role == 'manager' and test_user == app_user):
                                assert response.ok
                            else:
                                assert response.status_code == requests.codes.forbidden

                        # Trying create new company
                        response = env_post_request(
                            env,
                            path="companies",
                            data={},
                            caller=user)
                        assert response.status_code == requests.codes.forbidden

                        # Trying modify own company
                        response = env_patch_request(
                            env,
                            path=api_path_with_company_id(env, company_id=company_id),
                            data={},
                            caller=user)
                        if role == 'admin':
                            assert response.ok
                        else:
                            assert response.status_code == requests.codes.forbidden

                        # Trying delete own company
                        response = env_delete_request(
                            env,
                            path=api_path_with_company_id(env, company_id=company_id),
                            caller=user)
                        assert response.status_code == requests.codes.forbidden


def _create_app_users(system_env_with_db, company_id, login_prefix, count):
    for i in range(count):
        create_user(
            system_env_with_db,
            '{}{}'.format(login_prefix, i),
            company_id,
            UserRole.app,
            auth=system_env_with_db.auth_header_super)


def _get_users(system_env_with_db, company_id, user, page):
    response = env_get_request(
        system_env_with_db,
        path=api_path_with_company_id(system_env_with_db, "users?page={}".format(page), company_id=company_id),
        caller=user)
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert isinstance(j, list)
    return j


@skip_if_remote
def test_pagination(system_env_with_db):
    """
    Test that the list of users is properly paginated when the number of users is
    close to the page size.
    """

    env = system_env_with_db

    app_user_min_count = PAGINATION_PAGE_SIZE - 2
    app_user_max_count = PAGINATION_PAGE_SIZE + 1

    with create_tmp_company(env, "Test company test_pagination") as company_id:
        with create_tmp_user(env, company_id, UserRole.manager) as manager_user:

            # The manager should be the only user in the list
            users = _get_users(env, company_id, manager_user, page=1)
            assert len(users) == 1
            assert manager_user['id'] == users[0]['id']

            _create_app_users(env, company_id, "test_pagination_", app_user_min_count)

            for app_user_count in range(app_user_min_count, app_user_max_count + 1):

                # Manager will see himself and all apps
                expected_user_count = 1 + app_user_count

                expected_page_count = (expected_user_count - 1) // PAGINATION_PAGE_SIZE + 1

                received_users = []

                for page in range(1, 1 + expected_page_count):
                    users = _get_users(env, company_id, manager_user, page)

                    # All pages except the last one should be *full*
                    if page < expected_page_count:
                        assert len(users) == PAGINATION_PAGE_SIZE

                    received_users.extend(users)

                # Should get *no users* if the page number is bigger than needed
                assert len(_get_users(env, company_id, manager_user, expected_page_count + 1)) == 0

                # *All* users were received
                assert expected_user_count == len(received_users)

                # The manager himself should be in the list of users
                assert any(manager_user['id'] == user['id'] for user in received_users)

                # There are *no duplicate* users
                assert expected_user_count == len({user['id'] for user in received_users})

                _create_app_users(env, company_id, "test_pagination_{}_".format(app_user_count), count=1)
