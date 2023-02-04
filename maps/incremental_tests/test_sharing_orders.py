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


def _get_orders(sharing_env, user_kind, company_idx, route_id=None):
    return env_get_request(
        sharing_env['dbenv'],
        api_path_with_company_id(
            sharing_env['dbenv'],
            'orders{}'.format('' if route_id is None else '?route_id={}'.format(route_id)),
            company_id=sharing_env['companies'][company_idx]['id']
        ),
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )


def _get_route_ids(orders_response):
    assert orders_response.status_code == requests.codes.ok
    j = orders_response.json()
    assert isinstance(j, list)
    return set([x['route_id'] for x in j])


def _get_order_ids(orders_response):
    assert orders_response.status_code == requests.codes.ok
    j = orders_response.json()
    assert isinstance(j, list)
    return set([x['id'] for x in j])


def _get_env_order_ids(sharing_env, company_idx):
    return set([order['id'] for order in sharing_env['companies'][company_idx]['all_orders']])


def _get_env_sharing_order_ids(sharing_env, company_idx):
    return set([order['id'] for order in sharing_env['companies'][company_idx]['sharing_orders'][SHARED_WITH_COMPANY_IDX]])


def _get_env_route_ids(sharing_env, company_idx):
    return set([route['id'] for route in sharing_env['companies'][company_idx]['all_routes']])


def _get_env_sharing_route_ids(sharing_env, company_idx):
    return set([route['id'] for route in sharing_env['companies'][company_idx]['sharing_routes'][SHARED_WITH_COMPANY_IDX]])


@skip_if_remote
def test_route_not_matching_company(env_with_default_sharing_setup):
    """
    Test that specifying route that does not belong to the company always fails
    """
    sharing_env = env_with_default_sharing_setup
    for user_kind in [
        UserKind.admin,
        UserKind.trusted_manager, UserKind.manager,
        UserKind.trusted_dispatcher, UserKind.dispatcher,
    ]:
        for company_idx in [0, 1, 2]:
            for other_company_idx in [c for c in [0, 1, 2] if c != company_idx]:
                for other_route_id in _get_env_route_ids(sharing_env, other_company_idx):
                    response = _get_orders(sharing_env, user_kind, company_idx, other_route_id)
                    assert response.status_code == requests.codes.unprocessable


@skip_if_remote
def test_sharing_orders(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    # Own company.
    #
    # admin:
    #     should see all orders.
    # trusted manager and manager: should see no orders because
    #     env_with_default_sharing_setup does not enable access
    #     to own company depots for managers.
    # app:
    #     should see all orders.

    company_idx = 0
    assert company_idx == SHARED_WITH_COMPANY_IDX
    for user_kind in [UserKind.admin, UserKind.app]:
        response = _get_orders(sharing_env, user_kind, company_idx)

        route_ids = _get_route_ids(response)
        expected_route_ids = _get_env_route_ids(sharing_env, company_idx)
        assert route_ids == expected_route_ids

        order_ids = _get_order_ids(response)
        expected_order_ids = _get_env_order_ids(sharing_env, company_idx)
        assert order_ids == expected_order_ids

    for user_kind in [UserKind.trusted_manager, UserKind.manager, UserKind.trusted_dispatcher, UserKind.dispatcher]:
        response = _get_orders(sharing_env, user_kind, company_idx)

        route_ids = _get_route_ids(response)
        expected_route_ids = set()
        assert route_ids == expected_route_ids

    # Company that shares some orders with user's company.
    #
    # admin and trusted manager:
    #     should see all shared orders.
    # manager and app:
    #     should get 'forbidden access' code.

    company_idx = 1
    for user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.trusted_dispatcher]:
        response = _get_orders(sharing_env, user_kind, company_idx)

        route_ids = _get_route_ids(response)
        expected_route_ids = _get_env_sharing_route_ids(sharing_env, company_idx)
        assert route_ids == expected_route_ids

        order_ids = _get_order_ids(response)
        expected_order_ids = _get_env_sharing_order_ids(sharing_env, company_idx)
        assert order_ids == expected_order_ids

    for user_kind in [UserKind.manager, UserKind.dispatcher, UserKind.app]:
        assert _get_orders(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden

    # Company that doesn't share orders with user's company.
    #
    # admin, trusted manager, manager, trusted_dispatcher, dispatcher, app:
    #     should get 'forbidden access' code.

    company_idx = 2
    for user_kind in UserKind:
        assert _get_orders(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden
