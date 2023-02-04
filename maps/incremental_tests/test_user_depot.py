import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    cleanup_depot,
    cleanup_user,
    create_depot,
    create_user,
    create_tmp_user,
    env_delete_request,
    env_get_request,
    env_patch_request,
    env_post_request,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.models.user import UserRole


TEST_PREFIX = "user_depot_test"
TEST_USER = TEST_PREFIX + "_user"


def get_depots(system_env_with_db, user_id, caller=None):
    return env_get_request(
        system_env_with_db,
        '{}/user_depot/{}'.format(
            api_path_with_company_id(system_env_with_db),
            user_id
        ),
        caller=caller,
    )


def get_depots_json(system_env_with_db, user_id):
    response = get_depots(system_env_with_db, user_id)
    assert response.ok, response.text
    return response.json()


def post_depots(system_env_with_db, user_id, depot_ids, caller=None):
    return env_patch_request(
        system_env_with_db,
        '{}/user_depot/{}'.format(
            api_path_with_company_id(system_env_with_db),
            user_id,
        ),
        data=depot_ids,
        caller=caller,
    )


def post_depots_json(system_env_with_db, user_id, depot_ids):
    response = post_depots(system_env_with_db, user_id, depot_ids)
    assert response.ok, response.text
    return response.json()


def cleanup(system_env_with_db):
    cleanup_depot(system_env_with_db, TEST_PREFIX + "_1")
    cleanup_depot(system_env_with_db, TEST_PREFIX + "_2")
    cleanup_user(system_env_with_db, TEST_PREFIX + "_1")
    cleanup_user(system_env_with_db, TEST_PREFIX + "_2")


def create_users_depots(system_env_with_db):
    return {
        "users": [
            create_user(system_env_with_db, TEST_PREFIX +
                        "_1", system_env_with_db.company_id, UserRole.manager),
            create_user(system_env_with_db, TEST_PREFIX +
                        "_2", system_env_with_db.company_id, UserRole.manager)
        ],
        "depots": [
            create_depot(system_env_with_db, TEST_PREFIX + "_1"),
            create_depot(system_env_with_db, TEST_PREFIX + "_2"),
        ]
    }


def test_get_post(system_env_with_db):
    cleanup(system_env_with_db)
    try:
        env = create_users_depots(system_env_with_db)
        user1 = env["users"][0]
        depot1 = env["depots"][0]
        depot2 = env["depots"][1]

        assert get_depots_json(system_env_with_db, user1['id']) == []

        assert post_depots_json(
            system_env_with_db, user1['id'], [depot1['id']])
        assert get_depots_json(system_env_with_db, user1['id']) == [depot1]

        assert post_depots_json(system_env_with_db, user1['id'], [
            depot1['id'], depot2['id']])
        assert get_depots_json(system_env_with_db, user1['id']) == [
            depot1, depot2]

        assert post_depots_json(
            system_env_with_db, user1['id'], [depot2['id']])
        assert get_depots_json(system_env_with_db, user1['id']) == [depot2]

        assert post_depots_json(system_env_with_db, user1['id'], [])
        assert get_depots_json(system_env_with_db, user1['id']) == []
    finally:
        cleanup(system_env_with_db)


def test_user_depot_schema(system_env_with_db):
    cleanup(system_env_with_db)
    try:
        env = create_users_depots(system_env_with_db)

        def try_post_user_depot(data):
            response = post_depots(system_env_with_db, env["users"][0]['id'], data)
            assert response.status_code == requests.codes.unprocessable
            return response.json()['message']

        for payload in [None, {}, "depot-id", ["depot-one"], [123, None]]:
            assert 'Json schema validation failed: UserDepotIds' in try_post_user_depot(payload)
    finally:
        cleanup(system_env_with_db)


def test_invalid_id(system_env_with_db):
    cleanup(system_env_with_db)
    try:
        env = create_users_depots(system_env_with_db)
        user1 = env["users"][0]

        non_existing_id = 1234567890
        response = get_depots(system_env_with_db, non_existing_id)
        assert response.status_code == requests.codes.not_found

        response = post_depots(
            system_env_with_db, user1['id'], non_existing_id)
        assert response.status_code == requests.codes.unprocessable
    finally:
        cleanup(system_env_with_db)


def test_remove_depot(system_env_with_db):
    cleanup(system_env_with_db)
    try:
        env = create_users_depots(system_env_with_db)
        user1 = env["users"][0]
        depot1 = env["depots"][0]
        depot2 = env["depots"][1]
        assert post_depots_json(
            system_env_with_db, user1['id'], [depot1['id'], depot2['id']])
        assert get_depots_json(system_env_with_db, user1['id']) == [
            depot1, depot2]

        path = "depots/{}".format(depot1['id'])
        response = env_delete_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, path)
        )
        assert response.status_code == requests.codes.ok
        assert get_depots_json(system_env_with_db, user1['id']) == [depot2]
    finally:
        cleanup(system_env_with_db)


def test_remove_user(system_env_with_db):
    cleanup(system_env_with_db)
    try:
        env = create_users_depots(system_env_with_db)
        user1 = env["users"][0]
        depot1 = env["depots"][0]

        assert post_depots_json(
            system_env_with_db, user1['id'], [depot1['id']])
        assert get_depots_json(system_env_with_db, user1['id']) == [depot1]

        path = "users/{}".format(user1['id'])
        response = env_delete_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, path)
        )
        assert response.status_code == requests.codes.ok
        assert get_depots(system_env_with_db,
                          user1['id']).status_code == requests.codes.not_found
    finally:
        cleanup(system_env_with_db)


def test_user_role(system_env_with_db):
    cleanup(system_env_with_db)
    try:
        env = create_users_depots(system_env_with_db)
        depot1 = env["depots"][0]
        with create_tmp_user(system_env_with_db, system_env_with_db.company_id, UserRole.manager) as user:
            assert get_depots(system_env_with_db,
                              user['id'], caller=user).status_code == requests.codes.ok
            assert post_depots(system_env_with_db, user['id'], [
                               depot1['id']], caller=user).status_code == requests.codes.forbidden
    finally:
        cleanup(system_env_with_db)


def create_new_company(system_env_with_db):
    data = {
        'name': 'New Logistics Company test_user_depot',
        'logo_url': 'https://avatars.mds.yandex.net/get-switch/41639/flash_1c27fd050b06e13818aad698f15735d8.gif/orig',
        'bg_color': '#bc0000',
        'sms_enabled': False,
        'services': [
            {
                "name": "courier",
                "enabled": True
            },
            {
                "name": "mvrp",
                "enabled": False
            }
        ],
        'initial_login': TEST_USER
    }

    # Create company
    resp = env_post_request(
        system_env_with_db,
        "companies",
        data=data,
        auth=system_env_with_db.auth_header_super
    )
    resp.raise_for_status()
    company = resp.json()
    user = create_user(
        system_env_with_db,
        TEST_PREFIX + "_3",
        company['id'],
        UserRole.manager,
        auth=system_env_with_db.auth_header_super)
    depot = create_depot(
        system_env_with_db, TEST_PREFIX + "_3",
        company_id=company['id'],
        auth=system_env_with_db.auth_header_super)
    return company, user, depot


def cleanup_new_company(system_env_with_db, depot_id, user_id, company_id):
    # Delete a depot
    resp = env_delete_request(
        system_env_with_db,
        api_path_with_company_id(
            system_env_with_db,
            path="depots",
            object_id=depot_id,
            company_id=company_id,
        ),
        auth=system_env_with_db.auth_header_super
    )
    assert resp.status_code == requests.codes.ok

    # Delete a user
    resp = env_delete_request(
        system_env_with_db,
        api_path_with_company_id(
            system_env_with_db,
            path="users",
            object_id=user_id,
            company_id=company_id,
        ),
        auth=system_env_with_db.auth_header_super
    )
    assert resp.status_code == requests.codes.ok

    cleanup_user(system_env_with_db, TEST_USER, company_id,
                 auth=system_env_with_db.auth_header_super)

    # Delete a company
    resp = env_delete_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, company_id=company_id),
        auth=system_env_with_db.auth_header_super
    )
    assert resp.status_code == requests.codes.ok


@skip_if_remote
def test_assign_depot_from_different_company(system_env_with_db):
    cleanup(system_env_with_db)
    company, user, depot = create_new_company(system_env_with_db)
    try:
        env = create_users_depots(system_env_with_db)
        user1 = env["users"][0]
        response = post_depots(
            system_env_with_db,
            user1["id"],
            [depot['id']])
        assert response.status_code == requests.codes.unprocessable
        assert "Can not find object " in response.text
        assert str(depot['id']) in response.text
        assert get_depots_json(system_env_with_db, user1['id']) == []
    finally:
        cleanup(system_env_with_db)
        cleanup_new_company(
            system_env_with_db,
            depot['id'],
            user['id'],
            company['id'],
        )
