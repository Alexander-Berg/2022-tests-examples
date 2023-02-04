import pytest
import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request,
    env_patch_request,
    get_companies_list,
    create_route_env,
    create_tmp_companies,
    create_tmp_users,
    get_order,
    patch_order
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote, get_user_auth_header
from ya_courier_backend.models.user import UserRole


TEST_PREFIX = 'sharing_companies_test_'

LOCATIONS = [
    {"lat": 55.733827, "lon": 37.588722},
    {"lat": 55.729299, "lon": 37.580116}
]


def get_shared_companies(system_env_with_db, company_id, user_id, auth):
    return env_get_request(
        system_env_with_db,
        '{}/user_shared_company/{}'.format(
            api_path_with_company_id(system_env_with_db, company_id=company_id),
            user_id
        ),
        auth=auth
    )


def patch_shared_companies(system_env_with_db, company_id, user_id, company_ids, auth):
    return env_patch_request(
        system_env_with_db,
        '{}/user_shared_company/{}'.format(
            api_path_with_company_id(system_env_with_db, company_id=company_id),
            user_id,
        ),
        data=company_ids,
        auth=auth
    )


def check_shared_companies(response, expected_company_ids):
    assert response.ok, response.text
    company_ids = [company['id'] for company in response.json()]
    assert sorted(company_ids) == sorted(expected_company_ids)


def check_companies_list(system_env_with_db, auth, expected_company_ids):
    companies = get_companies_list(system_env_with_db, auth)
    company_ids = [c['id'] for c in companies]
    assert sorted(company_ids) == sorted(expected_company_ids)


@pytest.fixture(scope='module')
def sharing_companies_env(system_env_with_db):
    env = system_env_with_db
    with create_tmp_companies(
            env,
            [f'{TEST_PREFIX}_company_{i}' for i in range(3)],
            test_id=TEST_PREFIX + 'id') as company_ids:
        yield {f'company{i}_id': company_id for i, company_id in enumerate(company_ids)}


@pytest.fixture(scope='module')
def sharing_companies_users_env(system_env_with_db, sharing_companies_env):
    env = system_env_with_db
    env_c = sharing_companies_env
    with create_tmp_users(
            env,
            [env_c['company0_id'], env_c['company0_id'], env_c['company0_id'], env_c['company0_id']],
            [UserRole.admin, UserRole.manager, UserRole.dispatcher, UserRole.app]) as users:
        users_env = {'admin': users[0], 'manager': users[1], 'dispatcher': users[2], 'app': users[3]}
        env_c.update(users_env)
        yield env_c


@skip_if_remote
@pytest.mark.parametrize('user_role', [UserRole.admin, UserRole.manager, UserRole.dispatcher, UserRole.app])
def test_without_orders(system_env_with_db, sharing_companies_users_env, user_role):
    env = system_env_with_db
    env_cu = sharing_companies_users_env

    user = env_cu[user_role.value]
    user_auth = get_user_auth_header(env_cu[user_role.value])
    admin_auth = get_user_auth_header(env_cu[UserRole.admin.value])

    other_user_ids = [
        env_cu[r.value]["id"]
        for r in [UserRole.admin, UserRole.manager, UserRole.dispatcher, UserRole.app]
        if r != user_role
    ]

    check_companies_list(env, user_auth, [env_cu['company0_id']])
    response = get_shared_companies(env, env_cu['company0_id'], user['id'], user_auth)
    if user_role in [UserRole.app]:
        assert response.status_code == requests.codes.forbidden
    else:
        check_shared_companies(response, [])

    def test(assigned_company_ids):
        response = patch_shared_companies(env, env_cu['company0_id'], user['id'], assigned_company_ids, admin_auth)
        if user_role == UserRole.app:
            assert response.status_code == requests.codes.unprocessable
        else:
            assert response.status_code == requests.codes.ok
            if user_role != UserRole.dispatcher:
                response = get_shared_companies(env, env_cu['company0_id'], user['id'], user_auth)
                check_shared_companies(response, assigned_company_ids)

        check_companies_list(env, user_auth, [env_cu['company0_id']])
        for user_id in other_user_ids:
            response = get_shared_companies(env, env_cu['company0_id'], user_id, user_auth)
            if response.status_code == requests.codes.ok:
                check_shared_companies(response, [])
            else:
                assert response.status_code == requests.codes.forbidden
        # revert changes
        response = patch_shared_companies(env, env_cu['company0_id'], user['id'], [], admin_auth)
        if user_role == UserRole.app:
            assert response.status_code == requests.codes.unprocessable
        else:
            assert response.status_code == requests.codes.ok

    test([])
    test([env_cu['company1_id']])
    test([env_cu['company1_id'], env_cu['company2_id']])


@pytest.mark.parametrize('user_role', [UserRole.admin, UserRole.manager, UserRole.dispatcher, UserRole.app])
def test_with_orders(system_env_with_db, sharing_companies_users_env, user_role):
    env = system_env_with_db
    env_cu = sharing_companies_users_env

    user = env_cu[user_role.value]
    user_auth = get_user_auth_header(env_cu[user_role.value])
    admin_auth = get_user_auth_header(env_cu[UserRole.admin.value])

    check_companies_list(env, user_auth, [env_cu['company0_id']])
    response = get_shared_companies(env, env_cu['company0_id'], user['id'], user_auth)
    if user_role in [UserRole.app]:
        assert response.status_code == requests.codes.forbidden
    else:
        check_shared_companies(response, [])

    def test(shared_with_company_id, assigned_company_ids, expected_response_company_ids):
        with create_route_env(
                env,
                TEST_PREFIX + 'id',
                route_date='2018.12.12',
                order_locations=LOCATIONS) as route_env:
            order = route_env['orders'][0].copy()
            patch_order(env, order, {'shared_with_company_id': shared_with_company_id})
            order = get_order(env, route_env['orders'][0]['id'])
            assert order['shared_with_company_id'] == shared_with_company_id

            response = patch_shared_companies(env, env_cu['company0_id'], user['id'], assigned_company_ids, admin_auth)
            if user_role == UserRole.app:
                assert response.status_code == requests.codes.unprocessable
            else:
                assert response.status_code == requests.codes.ok
                if user_role != UserRole.dispatcher:
                    response = get_shared_companies(env, env_cu['company0_id'], user['id'], user_auth)
                    check_shared_companies(response, assigned_company_ids)

            check_companies_list(env, user_auth, expected_response_company_ids)

            response = patch_shared_companies(env, env_cu['company0_id'], user['id'], [], admin_auth)
            if user_role == UserRole.app:
                assert response.status_code == requests.codes.unprocessable
            else:
                assert response.status_code == requests.codes.ok
                if user_role != UserRole.dispatcher:
                    response = get_shared_companies(env, env_cu['company0_id'], user['id'], user_auth)
                    check_shared_companies(response, [])

    # Company env.company_id shares an order with user's company (env_cu['company0_id'])

    shared_with_company_id = env_cu['company0_id']

    expected_response_company_ids = [env_cu['company0_id'], env.company_id] if user_role == UserRole.admin else [env_cu['company0_id']]
    test(shared_with_company_id, [], expected_response_company_ids)
    test(shared_with_company_id, [env_cu['company1_id']], expected_response_company_ids)
    test(shared_with_company_id, [env_cu['company1_id'], env_cu['company2_id']], expected_response_company_ids)

    expected_response_company_ids = [env_cu["company0_id"], env.company_id] if user_role != UserRole.app else [env_cu["company0_id"]]
    test(shared_with_company_id, [env.company_id], expected_response_company_ids)
    test(shared_with_company_id, [env.company_id, env_cu['company1_id']], expected_response_company_ids)
    test(shared_with_company_id, [env.company_id, env_cu['company1_id'], env_cu['company2_id']], expected_response_company_ids)

    # Company env.company_id shares an order with some other company (not user's company)

    shared_with_company_id = env_cu['company1_id']

    expected_response_company_ids = [env_cu['company0_id']]
    test(shared_with_company_id, [], expected_response_company_ids)
    test(shared_with_company_id, [env.company_id], expected_response_company_ids)
    test(shared_with_company_id, [env.company_id, env_cu['company1_id']], expected_response_company_ids)
    test(shared_with_company_id, [env.company_id, env_cu['company1_id'], env_cu['company2_id']], expected_response_company_ids)
