import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote

SHARED_WITH_COMPANY_IDX = 0


def _get_positions(sharing_env, user_kind, company_idx, courier_id, route_id):
    response = env_get_request(
        sharing_env['dbenv'],
        api_path_with_company_id(
            sharing_env['dbenv'],
            'courier-position/{}/routes/{}'.format(courier_id, route_id),
            company_id=sharing_env['companies'][company_idx]['id']),
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )
    if response.status_code == requests.codes.ok:
        j = response.json()
        assert isinstance(j, list)
        for route in j:
            assert route['courier_id'] == courier_id
            assert route['route_id'] == route_id
    return response


def _get_env_routes(sharing_env, company_idx):
    return sharing_env['companies'][company_idx]['all_routes']


def _get_env_sharing_routes(sharing_env, company_idx):
    return sharing_env['companies'][company_idx]['sharing_routes'][SHARED_WITH_COMPANY_IDX]


@skip_if_remote
def test_route_not_matching_courier(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup
    for user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.manager, UserKind.trusted_dispatcher, UserKind.dispatcher]:
        for company_idx in [0, 1, 2]:
            all_routes = _get_env_routes(sharing_env, company_idx)
            for idx, route in enumerate(all_routes):
                other_route = all_routes[(idx + 1) % len(all_routes)]
                response = _get_positions(sharing_env, user_kind, company_idx, courier_id=route['courier_id'], route_id=other_route['id'])
                assert response.status_code == requests.codes.unprocessable


@skip_if_remote
def test_route_and_courier_not_matching_company(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup
    for user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.manager, UserKind.trusted_dispatcher, UserKind.dispatcher]:
        for company_idx in [0, 1, 2]:
            for other_company_idx in [c for c in [0, 1, 2] if c != company_idx]:
                for route in _get_env_routes(sharing_env, other_company_idx):
                    response = _get_positions(sharing_env, user_kind, company_idx, courier_id=route['courier_id'], route_id=route['id'])
                    assert response.status_code == requests.codes.unprocessable


@skip_if_remote
def test_sharing_courier_positions(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    company_idx = 0
    for route in _get_env_routes(sharing_env, company_idx):
        for user_kind in UserKind:
            response = _get_positions(sharing_env, user_kind, company_idx, courier_id=route['courier_id'], route_id=route['id'])
            if user_kind == UserKind.admin:
                assert response.status_code == requests.codes.ok
                assert len(response.json()) == 1
            else:
                assert response.status_code == requests.codes.forbidden

    company_idx = 1
    sharing_routes = _get_env_sharing_routes(sharing_env, company_idx)
    for route in _get_env_routes(sharing_env, company_idx):
        for user_kind in UserKind:
            response = _get_positions(sharing_env, user_kind, company_idx, courier_id=route['courier_id'], route_id=route['id'])
            if user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.trusted_dispatcher] and route in sharing_routes:
                assert response.status_code == requests.codes.ok
                assert len(response.json()) == 1
            else:
                assert response.status_code == requests.codes.forbidden

    company_idx = 2
    for route in _get_env_routes(sharing_env, company_idx):
        for user_kind in UserKind:
            response = _get_positions(sharing_env, user_kind, company_idx, courier_id=route['courier_id'], route_id=route['id'])
            assert response.status_code == requests.codes.forbidden
