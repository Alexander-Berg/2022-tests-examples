import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


SHARED_WITH_COMPANY_IDX = 0


def _get_courier_quality(sharing_env, user_kind, company_idx):
    return env_get_request(
        sharing_env['dbenv'],
        api_path_with_company_id(
            sharing_env['dbenv'],
            'courier-quality?date={}'.format(sharing_env['companies'][company_idx]['date']),
            company_id=sharing_env['companies'][company_idx]['id']
        ),
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )


def _get_order_numbers(courier_quality_response):
    assert courier_quality_response.status_code == requests.codes.ok
    j = courier_quality_response.json()
    assert isinstance(j, list)
    order_numbers = set()
    for order in j:
        order_numbers.add(order['order_number'])
        if order['suggested_order_number']:
            order_numbers.add(order['suggested_order_number'])
    assert len(j) == len(order_numbers)
    return order_numbers


def _get_env_order_numbers(sharing_env, company_idx):
    return set([order['number'] for order in sharing_env['companies'][company_idx]['all_orders']])


def _get_env_sharing_order_numbers(sharing_env, company_idx):
    return set([order['number'] for order in sharing_env['companies'][company_idx]['sharing_orders'][SHARED_WITH_COMPANY_IDX]])


@skip_if_remote
def test_sharing_courier_quality(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    company_idx = 0
    for user_kind in [UserKind.admin]:
        response = _get_courier_quality(sharing_env, user_kind, company_idx)
        order_numbers = _get_order_numbers(response)
        expected_order_numbers = _get_env_order_numbers(sharing_env, company_idx)
        assert order_numbers == expected_order_numbers
    for user_kind in [UserKind.trusted_manager, UserKind.manager, UserKind.trusted_dispatcher, UserKind.dispatcher]:
        response = _get_courier_quality(sharing_env, user_kind, company_idx)
        assert set() == _get_order_numbers(response)
    for user_kind in [UserKind.app]:
        assert _get_courier_quality(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden

    company_idx = 1
    for user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.trusted_dispatcher]:
        response = _get_courier_quality(sharing_env, user_kind, company_idx)
        order_numbers = _get_order_numbers(response)
        expected_order_numbers = _get_env_sharing_order_numbers(sharing_env, company_idx)
        assert order_numbers == expected_order_numbers
    for user_kind in [UserKind.manager, UserKind.dispatcher, UserKind.app]:
        assert _get_courier_quality(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden

    company_idx = 2
    for user_kind in UserKind:
        assert _get_courier_quality(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden
