import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


def _get_couriers(companies_env, user_kind, company_idx):
    company_id = companies_env['companies'][company_idx]['id']
    caller = companies_env['companies'][0]['users'][user_kind]
    return env_get_request(
        companies_env['dbenv'],
        api_path_with_company_id(
            companies_env['dbenv'], 'couriers', company_id=company_id),
        caller=caller
    )


def _get_courier_ids(companies_env, user_kind, company_idx):
    response = _get_couriers(companies_env, user_kind, company_idx)
    assert response.status_code == requests.codes.ok
    return sorted([j['id'] for j in response.json()])


def _get_env_courier_ids(env_with_sharing_companies):
    result = []
    for courier in env_with_sharing_companies["companies"][0]["all_couriers"]:
        result.append(courier["id"])
    return sorted(result)


def _get_env_courier_sharing_ids(env_with_sharing_companies):
    result = []
    sharing_with_company_idx = 0
    for courier in env_with_sharing_companies["companies"][1]["sharing_couriers"][sharing_with_company_idx]:
        result.append(courier["id"])
    return sorted(result)


@skip_if_remote
def test_sharing_couriers(env_with_default_sharing_setup):
    companies_env = env_with_default_sharing_setup

    company_idx = 0
    expected_ids = _get_env_courier_ids(env_with_default_sharing_setup)
    assert len(expected_ids) == 32
    for user_kind in UserKind:
        if user_kind == UserKind.app:
            assert _get_couriers(companies_env, user_kind,
                                 company_idx).status_code == requests.codes.unprocessable
        else:
            assert _get_courier_ids(companies_env, user_kind,
                                    company_idx) == expected_ids

    company_idx = 1
    expected_ids = _get_env_courier_sharing_ids(env_with_default_sharing_setup)
    assert len(expected_ids) == 6
    for user_kind in [UserKind.admin, UserKind.trusted_manager]:
        assert _get_courier_ids(companies_env, user_kind,
                                company_idx) == expected_ids
    for user_kind in [UserKind.manager, UserKind.app]:
        assert _get_couriers(companies_env, UserKind.manager,
                             company_idx).status_code == requests.codes.forbidden

    company_idx = 2
    for user_kind in UserKind:
        assert _get_couriers(companies_env, user_kind,
                             company_idx).status_code == requests.codes.forbidden
