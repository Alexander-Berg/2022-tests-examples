import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


def _get_routes(companies_env, user_kind, company_idx):
    company_id = companies_env['companies'][company_idx]['id']
    caller = companies_env['companies'][0]['users'][user_kind]
    return env_get_request(
        companies_env['dbenv'],
        api_path_with_company_id(
            companies_env['dbenv'], 'routes', company_id=company_id),
        caller=caller
    )


def _get_route_ids(companies_env, user_kind, company_idx):
    response = _get_routes(companies_env, user_kind, company_idx)
    assert response.status_code == requests.codes.ok
    return sorted([j['id'] for j in response.json()])


def _get_env_route_ids(env_with_sharing_companies):
    result = []
    for route in env_with_sharing_companies["companies"][0]["all_routes"]:
        result.append(route["id"])
    return sorted(result)


def _get_env_route_sharing_ids(env_with_sharing_companies):
    result = []
    sharing_with_company_idx = 0
    for route in env_with_sharing_companies["companies"][1]["sharing_routes"][sharing_with_company_idx]:
        result.append(route["id"])
    return sorted(result)


@skip_if_remote
def test_sharing_routes(env_with_default_sharing_setup):
    companies_env = env_with_default_sharing_setup

    company_idx = 0
    expected_ids = _get_env_route_ids(companies_env)
    assert len(expected_ids) == 32
    assert _get_route_ids(companies_env, UserKind.admin,
                          company_idx) == expected_ids
    for user_kind in [UserKind.trusted_manager, UserKind.manager,
                      UserKind.trusted_dispatcher, UserKind.dispatcher, UserKind.app]:
        assert _get_route_ids(companies_env, user_kind, company_idx) == []

    company_idx = 1
    expected_ids = _get_env_route_sharing_ids(companies_env)
    assert len(expected_ids) == 6
    for user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.trusted_dispatcher]:
        assert _get_route_ids(companies_env, user_kind,
                              company_idx) == expected_ids
    for user_kind in [UserKind.manager, UserKind.dispatcher, UserKind.app]:
        assert _get_routes(companies_env, user_kind,
                           company_idx).status_code == requests.codes.forbidden

    company_idx = 2
    for user_kind in UserKind:
        assert _get_routes(companies_env, user_kind,
                           company_idx).status_code == requests.codes.forbidden
