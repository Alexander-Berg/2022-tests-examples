import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


def _get_depots(system_env_with_db, company_id, caller=None):
    return env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, 'depots', company_id=company_id),
        caller=caller
    )


def _check_shared_depots(sharing_env, user_kind, company_idx, depot_names):
    """
    Testing that a user of company 0 (the user is specified by user_kind)
    can see exactly the depots specified by depot_names.

    depot_names is a list of names of depots of a company (specified by
    company_idx) expected to be visible by the user.
    depot_names equal to None means that the user is expected to have
    "Forbidden access" when requesting depots from the company company_idx.
    """

    response = _get_depots(
        sharing_env['dbenv'],
        sharing_env['companies'][company_idx]['id'],
        caller=sharing_env['companies'][0]['users'][user_kind])

    if depot_names is None:
        assert response.status_code == requests.codes.forbidden
    else:
        assert response.status_code == requests.codes.ok
        depot_ids = [j['id'] for j in response.json()]

        assert len(set(depot_names)) == len(depot_names)
        expected_depot_ids = []
        for depot_name in depot_names:
            expected_depot_ids.append(sharing_env['companies'][company_idx]['depots'][depot_name]['id'])

        assert sorted(depot_ids) == sorted(expected_depot_ids)


@skip_if_remote
def test_sharing_depots(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    # Requesting depots of company 0 by users of company 0

    company_idx = 0
    # Admin can see a depot of his own company even if access to the depot is not granted to him
    _check_shared_depots(sharing_env, UserKind.admin, company_idx, depot_names=['', '0', '1', '2', '0;1', '0;2', '1;2', '0;1;2', '1,2', '0,1,2', '0;1,2'])
    # Manager cannot see a depot of his own company if access to the depot is not granted to him
    _check_shared_depots(sharing_env, UserKind.trusted_manager, company_idx, depot_names=[])
    # Manager cannot see a depot of his own company if access to the depot is not granted to him
    _check_shared_depots(sharing_env, UserKind.manager, company_idx, depot_names=[])
    # App user has no permission to request depots even of his own company
    _check_shared_depots(sharing_env, UserKind.app, company_idx, depot_names=None)

    # Requesting depots of company 1 by users of company 0

    company_idx = 1
    # Admin can see a depot of an other company if the depot of the company has orders shared
    # with admin's company
    _check_shared_depots(sharing_env, UserKind.admin, company_idx, depot_names=['0', '0;1', '0;2', '0;1;2', '0,1,2', '0;1,2'])
    # Manager can see a depot of an other company if the depot of the company has orders shared
    # with manager's company and access to the company is granted to him
    _check_shared_depots(sharing_env, UserKind.trusted_manager, company_idx, depot_names=['0', '0;1', '0;2', '0;1;2', '0,1,2', '0;1,2'])
    # Manager has no permission to request depots of an other company if access to the company is not granted to him
    _check_shared_depots(sharing_env, UserKind.manager, company_idx, depot_names=None)
    # App user has no permission to request depots of an other company
    _check_shared_depots(sharing_env, UserKind.app, company_idx, depot_names=None)

    # Requesting depots of company 2 by users of company 0

    company_idx = 2
    # Admin has no permission to request depots of an other company if the company has no orders shared with admin's company
    _check_shared_depots(sharing_env, UserKind.admin, company_idx, depot_names=None)
    # Manager has no permission to request depots of an other company if the company has no orders shared with manager's company
    _check_shared_depots(sharing_env, UserKind.trusted_manager, company_idx, depot_names=None)
    # Manager has no permission to request depots of an other company if the company has no orders shared with manager's company
    _check_shared_depots(sharing_env, UserKind.manager, company_idx, depot_names=None)
    # App user has no permission to request depots of an other company
    _check_shared_depots(sharing_env, UserKind.app, company_idx, depot_names=None)
