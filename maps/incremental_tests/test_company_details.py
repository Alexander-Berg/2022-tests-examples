import requests
from ya_courier_backend.resources.logistic_company import (
    WHITELIST_OTHER_COMPANY_DETAILS,
    WHITELIST_OWN_COMPANY_MINIMAL_DETAILS,
    WHITELIST_OWN_COMPANY_MEDIUM_DETAILS,
    WHITELIST_OWN_COMPANY_MAXIMAL_DETAILS,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request,
    get_companies_list,
    api_path_with_company_id,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


SHARED_WITH_COMPANY_IDX = 0


def _get_companies(sharing_env, user_kind, expected_company_ids):
    env = sharing_env['dbenv']
    auth = env.get_user_auth(sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind])
    companies = get_companies_list(env, auth)
    companies_dict = {x['id']: x for x in companies}
    assert sorted(companies_dict.keys()) == sorted(expected_company_ids)
    return companies_dict


def _get_company(sharing_env, user_kind, company_idx):
    company_id = sharing_env['companies'][company_idx]['id']
    response = env_get_request(
        sharing_env['dbenv'],
        api_path_with_company_id(sharing_env['dbenv'], company_id=company_id),
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )
    if response.status_code == requests.codes.ok:
        j = response.json()
        assert j['id'] == company_id
    return response


def _check_company_details(actual_details, expected_details):
    for x in expected_details:
        assert x in actual_details
    for x in actual_details:
        assert x in expected_details
    if 'services' in expected_details:
        assert len(actual_details['services']) >= 1


@skip_if_remote
def test_all_companies_details(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    company_ids = [sharing_env['companies'][company_idx]['id'] for company_idx in [0, 1, 2]]

    test_cases = [
        {
            'user_kind': UserKind.admin,
            'expected_result': {
                company_ids[0]: WHITELIST_OWN_COMPANY_MAXIMAL_DETAILS,
                company_ids[1]: WHITELIST_OTHER_COMPANY_DETAILS
            }
        },
        {
            'user_kind': UserKind.trusted_manager,
            'expected_result': {
                company_ids[0]: WHITELIST_OWN_COMPANY_MEDIUM_DETAILS,
                company_ids[1]: WHITELIST_OTHER_COMPANY_DETAILS
            }
        },
        {
            'user_kind': UserKind.manager,
            'expected_result': {
                company_ids[0]: WHITELIST_OWN_COMPANY_MEDIUM_DETAILS
            }
        },
        {
            'user_kind': UserKind.app,
            'expected_result': {
                company_ids[0]: WHITELIST_OWN_COMPANY_MINIMAL_DETAILS
            }
        }
    ]

    for test_case in test_cases:
        companies = _get_companies(sharing_env, test_case['user_kind'], expected_company_ids=test_case['expected_result'].keys())
        for company_id, details in test_case['expected_result'].items():
            _check_company_details(companies[company_id], expected_details=details)


@skip_if_remote
def test_single_company_details(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    test_cases = [
        {
            'company_idx': 0,
            'expected_result': {
                UserKind.admin: WHITELIST_OWN_COMPANY_MAXIMAL_DETAILS,
                UserKind.trusted_manager: WHITELIST_OWN_COMPANY_MEDIUM_DETAILS,
                UserKind.manager: WHITELIST_OWN_COMPANY_MEDIUM_DETAILS,
                UserKind.app: WHITELIST_OWN_COMPANY_MINIMAL_DETAILS
            }
        },
        {
            'company_idx': 1,
            'expected_result': {
                UserKind.admin: WHITELIST_OTHER_COMPANY_DETAILS,
                UserKind.trusted_manager: WHITELIST_OTHER_COMPANY_DETAILS,
                UserKind.manager: None,
                UserKind.app: None
            }
        },
        {
            'company_idx': 2,
            'expected_result': {
                UserKind.admin: None,
                UserKind.trusted_manager: None,
                UserKind.manager: None,
                UserKind.app: None
            }
        }
    ]

    for test_case in test_cases:
        for user_kind, details in test_case['expected_result'].items():
            response = _get_company(sharing_env, user_kind, test_case['company_idx'])
            if details is None:
                assert response.status_code == requests.codes.forbidden
            else:
                assert response.status_code == requests.codes.ok, '{} {}'.format(user_kind, test_case)
                _check_company_details(response.json(), expected_details=details)
