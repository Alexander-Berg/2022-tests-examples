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


def _get_last_positions(sharing_env, user_kind, company_idx):
    return env_get_request(
        sharing_env['dbenv'],
        api_path_with_company_id(
            sharing_env['dbenv'],
            'courier-position',
            company_id=sharing_env['companies'][company_idx]['id']),
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )


def _get_courier_ids_from_last_positions(sharing_env, user_kind, company_idx):
    response = _get_last_positions(sharing_env, user_kind, company_idx)
    response.status_code == requests.codes.ok
    j = response.json()
    assert isinstance(j, list)
    courier_ids = sorted([item['id'] for item in j])
    assert len(set(courier_ids)) == len(courier_ids)
    return courier_ids


def _get_env_courier_ids(sharing_env, company_idx):
    return sorted([c['id'] for c in sharing_env['companies'][company_idx]['all_couriers']])


def _get_env_sharing_courier_ids(sharing_env, company_idx):
    return sorted([c['id'] for c in sharing_env['companies'][company_idx]['sharing_couriers'][SHARED_WITH_COMPANY_IDX]])


@skip_if_remote
def test_sharing_courier_last_positions(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    company_idx = 0
    expected_ids = _get_env_courier_ids(sharing_env, company_idx)
    assert len(expected_ids) == 32
    for user_kind in [UserKind.admin]:
        assert _get_courier_ids_from_last_positions(sharing_env, user_kind, company_idx) == expected_ids
    for user_kind in [UserKind.trusted_manager, UserKind.manager]:
        assert _get_courier_ids_from_last_positions(sharing_env, user_kind, company_idx) == []
    for user_kind in [UserKind.app]:
        assert _get_last_positions(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden

    company_idx = 1
    expected_ids = _get_env_sharing_courier_ids(sharing_env, company_idx)
    assert len(expected_ids) == 6
    for user_kind in [UserKind.admin, UserKind.trusted_manager]:
        assert _get_courier_ids_from_last_positions(sharing_env, user_kind, company_idx) == expected_ids
    for user_kind in [UserKind.manager, UserKind.app]:
        assert _get_last_positions(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden

    company_idx = 2
    for user_kind in UserKind:
        assert _get_last_positions(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden
