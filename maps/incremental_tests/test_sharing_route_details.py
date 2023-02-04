import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)


SHARED_WITH_COMPANY_IDX = 0


def _get_routed_orders(sharing_env, courier_id, route_id):
    response = env_get_request(
        sharing_env['dbenv'],
        'couriers/{}/routes/{}/routed-orders?lat=55.73&lon=37.58&time_now=13:30'.format(
            courier_id,
            route_id
        ),
        auth=sharing_env['dbenv'].auth_header_super
    )
    assert response.status_code == requests.codes.ok
    return response


def _get_route_details(sharing_env, user_kind, company_idx):
    return env_get_request(
        sharing_env['dbenv'],
        api_path_with_company_id(
            sharing_env['dbenv'],
            'route-details?date={}'.format(sharing_env['companies'][company_idx]['date']),
            company_id=sharing_env['companies'][company_idx]['id']
        ),
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )


def _get_route_ids(route_details_response):
    assert route_details_response.status_code == requests.codes.ok
    j = route_details_response.json()
    assert isinstance(j, list)
    return set([x['route_id'] for x in j])


def _get_order_ids(route_details_response):
    assert route_details_response.status_code == requests.codes.ok
    j = route_details_response.json()
    assert isinstance(j, list)
    order_ids = set()
    for state in [x['route_state'] for x in j]:
        for name in ['next_orders', 'fixed_orders', 'finished_orders']:
            for order_id in state.get(name, []):
                order_ids.add(order_id)
        for x in state.get('routed_orders', []):
            order_ids.add(x['id'])
        order_id = state.get('next_order', {}).get('id')
        if order_id is not None:
            order_ids.add(order_id)
    return order_ids


def _get_env_order_ids(sharing_env, company_idx):
    return set([order['id'] for order in sharing_env['companies'][company_idx]['all_orders']])


def _get_env_sharing_order_ids(sharing_env, company_idx):
    return set([order['id'] for order in sharing_env['companies'][company_idx]['sharing_orders'][SHARED_WITH_COMPANY_IDX]])


def _get_env_routes(sharing_env, company_idx):
    return sharing_env['companies'][company_idx]['all_routes']


def _get_env_route_ids(sharing_env, company_idx):
    return set([route['id'] for route in sharing_env['companies'][company_idx]['all_routes']])


def _get_env_sharing_route_ids(sharing_env, company_idx):
    return set([route['id'] for route in sharing_env['companies'][company_idx]['sharing_routes'][SHARED_WITH_COMPANY_IDX]])


def test_sharing_route_details(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup

    # We have to call routed-orders first, otherwise route-details response
    # will have no data related to orders.
    for company_idx in [0, 1, 2]:
        for route in _get_env_routes(sharing_env, company_idx):
            _get_routed_orders(sharing_env, route['courier_id'], route['id'])

    company_idx = 0
    for user_kind in [UserKind.admin]:
        response = _get_route_details(sharing_env, user_kind, company_idx)

        route_ids = _get_route_ids(response)
        expected_route_ids = _get_env_route_ids(sharing_env, company_idx)
        assert route_ids == expected_route_ids

        order_ids = _get_order_ids(response)
        expected_order_ids = _get_env_order_ids(sharing_env, company_idx)
        assert order_ids == expected_order_ids

    for user_kind in [UserKind.trusted_manager, UserKind.manager, UserKind.trusted_dispatcher, UserKind.dispatcher]:
        response = _get_route_details(sharing_env, user_kind, company_idx)
        assert set() == _get_route_ids(response)
        assert set() == _get_order_ids(response)

    for user_kind in [UserKind.app]:
        assert _get_route_details(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden

    company_idx = 1
    for user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.trusted_dispatcher]:
        response = _get_route_details(sharing_env, user_kind, company_idx)

        route_ids = _get_route_ids(response)
        expected_route_ids = _get_env_sharing_route_ids(sharing_env, company_idx)
        assert route_ids == expected_route_ids

        order_ids = _get_order_ids(response)
        expected_order_ids = _get_env_sharing_order_ids(sharing_env, company_idx)
        assert order_ids == expected_order_ids

    for user_kind in [UserKind.manager, UserKind.dispatcher, UserKind.app]:
        assert _get_route_details(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden

    company_idx = 2
    for user_kind in UserKind:
        assert _get_route_details(sharing_env, user_kind, company_idx).status_code == requests.codes.forbidden
