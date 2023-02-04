import pytest
import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    cleanup_user,
    create_user,
    env_get_request,
    env_patch_request,
    create_company,
    cleanup_company,
    find_company_by_id
)
from ya_courier_backend.models.user import UserRole


TEST_PREFIX = "user_shared_company_test"


def get_shared_companies(system_env_with_db, user_id, auth=None):
    return env_get_request(
        system_env_with_db,
        '{}/user_shared_company/{}'.format(
            api_path_with_company_id(system_env_with_db),
            user_id
        ),
        auth=auth,
    )


def get_shared_companies_json(system_env_with_db, user_id):
    response = get_shared_companies(system_env_with_db, user_id)
    assert response.ok, response.text
    return response.json()


def get_shared_companies_ids(system_env_with_db, user_id):
    j = get_shared_companies_json(system_env_with_db, user_id)
    return [company['id'] for company in j]


def post_shared_companies(system_env_with_db, user_id, company_ids, auth=None):
    return env_patch_request(
        system_env_with_db,
        '{}/user_shared_company/{}'.format(
            api_path_with_company_id(system_env_with_db),
            user_id,
        ),
        data=company_ids,
        auth=auth,
    )


def post_shared_companies_json(system_env_with_db, user_id, company_ids):
    response = post_shared_companies(system_env_with_db, user_id, company_ids)
    assert response.ok, response.text
    return response.json()


@pytest.fixture
def env_two_companies(system_env_with_db):
    try:
        user = create_user(system_env_with_db, TEST_PREFIX +
                           "_user1", system_env_with_db.company_id, UserRole.manager)

        company1 = create_company(system_env_with_db, TEST_PREFIX +
                                  "_login1", TEST_PREFIX + "_company1")
        company2 = create_company(system_env_with_db, TEST_PREFIX +
                                  "_login2", TEST_PREFIX + "_company2")
        yield {"user": user, "company1": company1, "company2": company2, "env": system_env_with_db}
    finally:
        cleanup_user(system_env_with_db, TEST_PREFIX + "_user1")
        cleanup_user(system_env_with_db, TEST_PREFIX + "_login1",
                     company_id=company1, auth=system_env_with_db.auth_header_super)
        cleanup_user(system_env_with_db, TEST_PREFIX + "_login2",
                     company_id=company2, auth=system_env_with_db.auth_header_super)
        if find_company_by_id(system_env_with_db, company1, auth=system_env_with_db.auth_header_super):
            cleanup_company(system_env_with_db, company1)
        if find_company_by_id(system_env_with_db, company2, auth=system_env_with_db.auth_header_super):
            cleanup_company(system_env_with_db, company2)


def test_post_shared_companies_schema(env_two_companies):
    system_env_with_db = env_two_companies['env']
    user_id = env_two_companies['user']['id']
    assert get_shared_companies_json(system_env_with_db, user_id) == []

    def try_post_shared_companies(data):
        response = post_shared_companies(system_env_with_db, user_id, data)
        assert response.status_code == requests.codes.unprocessable
        return response.json()['message']

    msg = try_post_shared_companies({"login": "123"})
    assert "Json schema validation failed: UserSharedCompanyIds" in msg
    assert "is not of type 'array'" in msg

    for payload in [None, {}, ["company-one"], [2, None]]:
        assert "Json schema validation failed: UserSharedCompanyIds" in try_post_shared_companies(payload)


def test_get_post(env_two_companies):
    system_env_with_db = env_two_companies["env"]
    user = env_two_companies["user"]
    company1 = env_two_companies["company1"]
    company2 = env_two_companies["company2"]

    assert get_shared_companies_json(system_env_with_db, user['id']) == []

    post_shared_companies_json(system_env_with_db, user['id'], [company1])
    assert get_shared_companies_ids(
        system_env_with_db, user['id']) == [company1]

    post_shared_companies_json(
        system_env_with_db, user['id'], [company1, company2])
    assert get_shared_companies_ids(system_env_with_db, user['id']) == [
        company1, company2]

    post_shared_companies_json(
        system_env_with_db, user['id'], [])
    assert get_shared_companies_ids(system_env_with_db, user['id']) == []


def test_invalid_user_id(env_two_companies):
    system_env_with_db = env_two_companies["env"]

    non_existing_user_id = 1234567890
    response = get_shared_companies(system_env_with_db, non_existing_user_id)
    assert response.status_code == requests.codes.not_found


def test_invalid_company_id(env_two_companies):
    system_env_with_db = env_two_companies["env"]
    user = env_two_companies["user"]

    non_existing_company_id = 1234567890
    response = post_shared_companies(
        system_env_with_db, user['id'], [non_existing_company_id])
    assert response.status_code == requests.codes.unprocessable


def test_remove_user(env_two_companies):
    system_env_with_db = env_two_companies["env"]
    user = env_two_companies["user"]
    company1 = env_two_companies["company1"]

    assert get_shared_companies_json(system_env_with_db, user['id']) == []

    post_shared_companies_json(system_env_with_db, user['id'], [company1])
    assert get_shared_companies_ids(
        system_env_with_db, user['id']) == [company1]

    cleanup_user(system_env_with_db, user['login'])

    response = get_shared_companies(system_env_with_db, user['id'])
    assert response.status_code == requests.codes.not_found


def test_remove_company(env_two_companies):
    system_env_with_db = env_two_companies["env"]
    user = env_two_companies["user"]
    company1 = env_two_companies["company1"]
    company2 = env_two_companies["company2"]

    assert get_shared_companies_json(system_env_with_db, user['id']) == []

    post_shared_companies_json(system_env_with_db, user['id'], [company1, company2])
    assert get_shared_companies_ids(
        system_env_with_db, user['id']) == [company1, company2]

    cleanup_company(system_env_with_db, company2)

    assert get_shared_companies_ids(
        system_env_with_db, user['id']) == [company1]
