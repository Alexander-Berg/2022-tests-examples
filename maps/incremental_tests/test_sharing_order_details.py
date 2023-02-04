import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


FORBIDDEN_ACCESS = None
SHARED_WITH_COMPANY_IDX = 0


def _validate_sharing_info(sharing_env, requester_company_id, company_id, orders):
    company_id_to_name = {c['id']: c['name'] for c in sharing_env['companies']}

    for order in orders:
        response = env_get_request(
            sharing_env['dbenv'],
            api_path_with_company_id(sharing_env['dbenv'], 'orders/{}'.format(order['order_id']), company_id=company_id),
            auth=sharing_env['dbenv'].auth_header_super
        )
        assert response.status_code == requests.codes.ok
        shared_ids = response.json()['shared_with_company_ids']
        assert len(set(shared_ids)) == len(shared_ids)

        if company_id == requester_company_id:
            # Orders from own company are never shared with own company
            assert requester_company_id not in shared_ids
        else:
            assert requester_company_id in shared_ids
        for shared_id in shared_ids:
            assert {
                'id': shared_id,
                'name': company_id_to_name[shared_id],
                'number': None
            } in order['shared_with_companies']
        assert len(order['shared_with_companies']) == len(shared_ids)


def _get_order_details(sharing_env, user_kind, company_idx, query_params):
    company_id = sharing_env['companies'][company_idx]['id']
    shared_with_company_id = sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['id']
    assert len(query_params) >= 1
    path = 'order-details?{}'.format(
        '&'.join('{}={}'.format(k, v) for k, v in query_params.items())
    )
    response = env_get_request(
        sharing_env['dbenv'],
        api_path_with_company_id(sharing_env['dbenv'], path, company_id=company_id),
        caller=sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind]
    )
    if response.status_code == requests.codes.ok:
        orders = response.json()
        assert isinstance(orders, list)
        _validate_sharing_info(sharing_env, shared_with_company_id, company_id, orders)
    return response


def _get_orders(sharing_env, user_kind, company_idx, depot_id):
    return _get_order_details(
        sharing_env,
        user_kind,
        company_idx,
        {'date': sharing_env['companies'][company_idx]['date'], 'depot_id': depot_id}
    )


def _get_order_count(sharing_env, user_kind, company_idx, depot_id):
    response = _get_orders(sharing_env, user_kind, company_idx, depot_id)
    if response.status_code == requests.codes.forbidden:
        return FORBIDDEN_ACCESS
    assert response.status_code == requests.codes.ok
    orders = response.json()
    for order in orders:
        assert order['depot_id'] == depot_id
    return len(orders)


def _get_depot_ids_with_order_counts(sharing_env, user_kind, company_idx):
    response = _get_order_details(
        sharing_env,
        user_kind,
        company_idx,
        {'date': sharing_env['companies'][company_idx]['date']}
    )
    if response.status_code == requests.codes.forbidden:
        return {}
    assert response.status_code == requests.codes.ok
    orders = response.json()
    all_depot_ids = [depot['id'] for depot in sharing_env['companies'][company_idx]['depots'].values()]
    depot_ids_with_order_counts = {}
    for order in orders:
        assert order['depot_id'] in all_depot_ids
        if order['depot_id'] not in depot_ids_with_order_counts:
            depot_ids_with_order_counts[order['depot_id']] = 0
        depot_ids_with_order_counts[order['depot_id']] += 1
    return depot_ids_with_order_counts


def _check_orders(sharing_env, user_kind, company_idx, depot_names_with_order_counts):
    """
    Testing that a user of company SHARED_WITH_COMPANY_IDX can see exactly
    the orders specified by depot_names_with_order_counts.

    user_kind: user from company SHARED_WITH_COMPANY_IDX (this user requests orders).

    company_idx: index of a company from which the user requests orders.

    depot_names_with_order_counts: a dictionary with depot_name:order_count
        items, where depot_name is depot's name and order_count is the number of
        orders in the depot that are expected to be visible for the user.
        If order_count == FORBIDDEN_ACCESS then it is expected that the user
        has no permission to access the depot.
    """

    assert depot_names_with_order_counts is not None
    expected_depot_ids_with_order_counts = {}
    for depot_name, order_count in depot_names_with_order_counts.items():
        depot_id = sharing_env['companies'][company_idx]['depots'][depot_name]['id']
        expected_depot_ids_with_order_counts[depot_id] = order_count

    # Test depots one by one
    for depot in sharing_env['companies'][company_idx]['depots'].values():
        assert depot['id'] in expected_depot_ids_with_order_counts, depot['number']
        order_count = _get_order_count(sharing_env, user_kind, company_idx, depot['id'])
        assert expected_depot_ids_with_order_counts[depot['id']] == order_count, depot['number']

    # Test all depots at once
    expected_depot_ids_with_order_counts = {
        k: v for k, v in expected_depot_ids_with_order_counts.items() if v not in [0, FORBIDDEN_ACCESS]
    }
    depot_ids_with_order_counts = _get_depot_ids_with_order_counts(sharing_env, user_kind, company_idx)
    assert depot_ids_with_order_counts == expected_depot_ids_with_order_counts


@skip_if_remote
def test_depot_not_belonging_to_company(env_with_default_sharing_setup):
    """
    Test that specifying depot that does not belong to the company always fails
    """
    sharing_env = env_with_default_sharing_setup
    for user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.manager]:
        for company_idx in [0, 1, 2]:
            for depot_company_idx in [c for c in [0, 1, 2] if c != company_idx]:
                for depot in sharing_env['companies'][depot_company_idx]['depots'].values():
                    assert _get_orders(sharing_env, user_kind, company_idx, depot['id']) \
                        .status_code == requests.codes.unprocessable


def _all_depots_forbidden(sharing_env, company_idx):
    return {depot_name: FORBIDDEN_ACCESS for depot_name in sharing_env['companies'][company_idx]['depots']}


def test_sharing_orders(env_with_default_sharing_setup):
    """
    Test that requesting orders handles depot and company sharing correctly.
    """
    sharing_env = env_with_default_sharing_setup

    # Own company:
    # Admin can request and see all orders in a depot of his company even if
    # access to the depot is not granted to him.
    # Manager/dispatcher has no permission to request orders from a specific depot of his
    # company if access to the depot is not granted to him.
    # Manager/dispatcher has no permission to request orders from his company if access
    # to any depot is not granted to him.
    #
    # Other company:
    # User (dispatcher, manager or admin) has no permission to request orders from another
    # company if the company does not have orders shared with the user's company.
    # User (dispatcher, manager or admin) has no permission to request orders from a specific depot
    # of another company if the depot has no orders shared with the user's company.
    # Manager/dispatcher has no permission to request orders from another company if the
    # access to the company is not granted to him.
    # App user has no permission to request order details.

    # See create_sharing_depots_with_orders() for details regarding number and type of
    # orders created per depot. See comments in _check_orders() regarding its parameters.
    # Note: sharings_with_counts={} means that user has "Forbidden access" to every depot.

    # Requesting orders from depots of company 0 by users of company SHARED_WITH_COMPANY_IDX
    company_idx = 0
    all_forbidden = _all_depots_forbidden(sharing_env, company_idx)
    _check_orders(sharing_env, UserKind.admin, company_idx, depot_names_with_order_counts={
        '': 0,
        '0': 4,
        '1': 4,
        '2': 4,
        '0;1': 8,
        '0;2': 8,
        '1;2': 8,
        '0;1;2': 12,
        '1,2': 4,
        '0,1,2': 4,
        '0;1,2': 8
    })
    _check_orders(sharing_env, UserKind.trusted_manager, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.manager, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.trusted_dispatcher, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.dispatcher, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.app, company_idx, depot_names_with_order_counts={
        '': 0,
        '0': 4,
        '1': 4,
        '2': 4,
        '0;1': 8,
        '0;2': 8,
        '1;2': 8,
        '0;1;2': 12,
        '1,2': 4,
        '0,1,2': 4,
        '0;1,2': 8
    })

    # Requesting orders from depots of company 1 by users of company SHARED_WITH_COMPANY_IDX
    company_idx = 1
    all_forbidden = _all_depots_forbidden(sharing_env, company_idx)
    _check_orders(sharing_env, UserKind.admin, company_idx, depot_names_with_order_counts={
        '': FORBIDDEN_ACCESS,
        '0': 1,
        '1': FORBIDDEN_ACCESS,
        '2': FORBIDDEN_ACCESS,
        '0;1': 1,
        '0;2': 1,
        '1;2': FORBIDDEN_ACCESS,
        '0;1;2': 1,
        '1,2': FORBIDDEN_ACCESS,
        '0,1,2': 1,
        '0;1,2': 1
    })
    for user_kind in [UserKind.trusted_manager, UserKind.trusted_dispatcher]:
        _check_orders(sharing_env, user_kind, company_idx, depot_names_with_order_counts={
            '': FORBIDDEN_ACCESS,
            '0': 1,
            '1': FORBIDDEN_ACCESS,
            '2': FORBIDDEN_ACCESS,
            '0;1': 1,
            '0;2': 1,
            '1;2': FORBIDDEN_ACCESS,
            '0;1;2': 1,
            '1,2': FORBIDDEN_ACCESS,
            '0,1,2': 1,
            '0;1,2': 1
        })
    _check_orders(sharing_env, UserKind.manager, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.app, company_idx, depot_names_with_order_counts=all_forbidden)

    # Requesting orders from depots of company 2 by users of company SHARED_WITH_COMPANY_IDX
    company_idx = 2
    all_forbidden = _all_depots_forbidden(sharing_env, company_idx)
    _check_orders(sharing_env, UserKind.admin, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.trusted_manager, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.manager, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.trusted_dispatcher, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.dispatcher, company_idx, depot_names_with_order_counts=all_forbidden)
    _check_orders(sharing_env, UserKind.app, company_idx, depot_names_with_order_counts=all_forbidden)
