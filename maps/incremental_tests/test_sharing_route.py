import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request,
    env_patch_request,
    env_delete_request,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


SHARED_WITH_COMPANY_IDX = 0


def _get_route_auth(sharing_env, company_idx, route_id, auth):
    response = env_get_request(
        sharing_env['dbenv'],
        api_path_with_company_id(
            sharing_env['dbenv'],
            'routes/{}'.format(route_id),
            company_id=sharing_env['companies'][company_idx]['id']),
        auth=auth
    )
    if response.status_code == requests.codes.ok:
        j = response.json()
        assert j['id'] == route_id
    return response


def _get_route(sharing_env, user_kind, company_idx, route_id):
    auth = sharing_env['dbenv'].get_user_auth(sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind])
    return _get_route_auth(sharing_env, company_idx, route_id, auth)


def _get_route_as_super_user(sharing_env, company_idx, route_id):
    auth = sharing_env['dbenv'].auth_header_super
    return _get_route_auth(sharing_env, company_idx, route_id, auth)


def _patch_route(sharing_env, user_kind, company_idx, route_data):
    return env_patch_request(
        sharing_env['dbenv'],
        api_path_with_company_id(
            sharing_env['dbenv'],
            'routes/{}'.format(route_data['id']),
            company_id=sharing_env['companies'][company_idx]['id']),
        data=route_data,
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )


def _delete_route(sharing_env, user_kind, company_idx, route_id):
    return env_delete_request(
        sharing_env['dbenv'],
        api_path_with_company_id(
            sharing_env['dbenv'],
            'routes/{}'.format(route_id),
            company_id=sharing_env['companies'][company_idx]['id']),
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )


def _get_env_routes(sharing_env, company_idx):
    return sharing_env['companies'][company_idx]['all_routes']


def _get_env_sharing_routes(sharing_env, company_idx):
    return sharing_env['companies'][company_idx]['sharing_routes'][SHARED_WITH_COMPANY_IDX]


@skip_if_remote
def test_route_not_matching_company(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup
    for user_kind in UserKind:
        for company_idx in [0, 1, 2]:
            for other_company_idx in [c for c in [0, 1, 2] if c != company_idx]:
                for other_route in _get_env_routes(sharing_env, other_company_idx):
                    response = _get_route(sharing_env, user_kind, company_idx, other_route['id'])
                    assert response.status_code == requests.codes.unprocessable


@skip_if_remote
def test_sharing_route(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    company_idx = 0
    for route in _get_env_routes(sharing_env, company_idx):
        for user_kind in UserKind:
            response = _get_route(sharing_env, user_kind, company_idx, route['id'])
            if user_kind in [UserKind.admin, UserKind.app]:
                assert response.status_code == requests.codes.ok
            else:
                assert response.status_code == requests.codes.forbidden

    company_idx = 1
    sharing_routes = _get_env_sharing_routes(sharing_env, company_idx)
    for route in _get_env_routes(sharing_env, company_idx):
        for user_kind in UserKind:
            response = _get_route(sharing_env, user_kind, company_idx, route['id'])
            if (
                user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.trusted_dispatcher] and
                route in sharing_routes
            ):
                assert response.status_code == requests.codes.ok
            else:
                assert response.status_code == requests.codes.forbidden

    company_idx = 2
    for route in _get_env_routes(sharing_env, company_idx):
        for user_kind in UserKind:
            response = _get_route(sharing_env, user_kind, company_idx, route['id'])
            assert response.status_code == requests.codes.forbidden


@skip_if_remote
def test_cannot_patch_foreign_routes(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup
    for company_idx in [1, 2]:
        for route in _get_env_routes(sharing_env, company_idx):
            for user_kind in UserKind:
                response = _get_route_as_super_user(sharing_env, company_idx, route['id'])
                assert response.status_code == requests.codes.ok
                j = response.json()
                j['number'] += 'x'
                response = _patch_route(sharing_env, user_kind, company_idx, j)
                assert response.status_code == requests.codes.forbidden


@skip_if_remote
def test_cannot_delete_foreign_routes(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup
    for company_idx in [1, 2]:
        for route in _get_env_routes(sharing_env, company_idx):
            for user_kind in UserKind:
                response = _delete_route(sharing_env, user_kind, company_idx, route['id'])
                assert response.status_code == requests.codes.forbidden
