from datetime import datetime, timezone
import pytest
import requests

from yandex.maps.test_utils.common import wait_until
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request, env_patch_request, env_delete_request,
    api_path_with_company_id, get_order, create_route_env,
    patch_order, batch_orders, get_orders, get_order_sequence, post_order_sequence,
    create_tmp_users, get_route, push_positions, push_imei_positions, get_order_details_by_route_id,
    create_tmp_company, get_courier_quality
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
)
from ya_courier_backend.models.user import UserRole
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.ya_courier import DEFAULT_UPDATE_ROUTE_STATES_PERIOD_S


SHARED_WITH_COMPANY_IDX = 0

LOCATIONS = [
    {'lat': 55.663878, 'lon': 37.482458},
    {'lat': 55.683761, 'lon': 37.518000},
    {'lat': 55.705491, 'lon': 37.551859},
]


def _get_order_ids(route_env):
    return [order['id'] for order in route_env['orders']]


def _get_order_numbers(route_env):
    return [order['id'] for order in route_env['orders']]


def _patch_courier_order(system_env_with_db, route_env, order_id, patch_data):
    return env_patch_request(
        system_env_with_db,
        'couriers/{}/routes/{}/orders/{}'.format(
            route_env['route']['courier_id'], route_env['route']['id'], order_id
        ),
        data=patch_data
    )


def _fix_route_orders(system_env_with_db, route_env):
    assert len(get_order_sequence(system_env_with_db, route_env['route']['id'])['order_ids']) > 0
    order_ids = _get_order_ids(route_env)
    post_order_sequence(system_env_with_db, route_env['route']['id'], order_ids)
    assert get_order_sequence(system_env_with_db, route_env['route']['id'])['order_ids'] == order_ids


def _get_order_ids_from_order_details(system_env_with_db, request_date, depot_ids):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(
            system_env_with_db,
            "order-details?date={}".format(request_date)
        )
    )
    assert response.status_code == requests.codes.ok
    orders = response.json()
    assert isinstance(orders, list)
    return [order['order_id'] for order in orders if order['depot_id'] in depot_ids]


def _get_env_routes(sharing_env, company_idx):
    return sharing_env['companies'][company_idx]['all_routes']


def test_move_order_to_another_route(system_env_with_db):
    with create_route_env(system_env_with_db, "move_order_to_another_route_1", order_locations=LOCATIONS) as route_env1:
        with create_route_env(system_env_with_db, "move_order_to_another_route_2", order_locations=LOCATIONS) as route_env2:
            _fix_route_orders(system_env_with_db, route_env1)
            _fix_route_orders(system_env_with_db, route_env2)

            # move order to another route
            patch_order(system_env_with_db, route_env1['orders'][1], {
                        "route_id": route_env2['route']['id']})

            orders1 = _get_order_ids(route_env1)
            orders2 = _get_order_ids(route_env2)
            orders2.append(orders1[1])
            del orders1[1]

            assert get_order_sequence(system_env_with_db,
                                      route_env1['route']['id'])['order_ids'] == orders1
            assert get_order_sequence(system_env_with_db,
                                      route_env2['route']['id'])['order_ids'] == orders2
            # order-sequence should not change if no changes in route
            patch_order(system_env_with_db, route_env1['orders'][1], {
                        "route_id": route_env2['route']['id'],
                        "status": "confirmed"})
            assert get_order_sequence(system_env_with_db,
                                      route_env1['route']['id'])['order_ids'] == orders1
            assert get_order_sequence(system_env_with_db,
                                      route_env2['route']['id'])['order_ids'] == orders2


def test_patch_courier_order(system_env_with_db):
    with create_route_env(system_env_with_db, "move_order_to_another_route_1", order_locations=LOCATIONS) as route_env1:
        with create_route_env(system_env_with_db, "move_order_to_another_route_2", order_locations=LOCATIONS) as route_env2:
            _fix_route_orders(system_env_with_db, route_env1)
            _fix_route_orders(system_env_with_db, route_env2)

            orders1 = _get_order_ids(route_env1)
            orders2 = _get_order_ids(route_env2)

            # Patching order should not change order sequence

            order = route_env1['orders'][0].copy()
            order['comments'] += 'x'
            patch_data = {
                'comments': order['comments'] + 'x'
            }
            order.update(patch_data)
            response = _patch_courier_order(system_env_with_db, route_env1, order['id'], patch_data)
            assert response.status_code == requests.codes.ok

            tmp_order = get_order(system_env_with_db, order['id'])
            assert tmp_order['comments'] == order['comments']
            assert tmp_order['route_id'] == route_env1['route']['id']
            assert get_order_sequence(system_env_with_db, route_env1['route']['id'])['order_ids'] == orders1

            # Moving order to other route is not allowed

            order = route_env1['orders'][0].copy()
            patch_data = {
                'comments': order['comments'] + 'y',
                'route_id': route_env2['route']['id']
            }
            order.update(patch_data)
            response = _patch_courier_order(system_env_with_db, route_env1, order['id'], patch_data)
            assert response.status_code == requests.codes.unprocessable
            assert response.json()['message'] == 'Moving order to other route is not allowed'

            tmp_order = get_order(system_env_with_db, order['id'])
            assert tmp_order['comments'] != order['comments']
            assert tmp_order['route_id'] == route_env1['route']['id']
            assert get_order_sequence(system_env_with_db, route_env1['route']['id'])['order_ids'] == orders1
            assert get_order_sequence(system_env_with_db, route_env2['route']['id'])['order_ids'] == orders2


def test_delete_order(system_env_with_db):
    with create_route_env(system_env_with_db, "delete_order", order_locations=LOCATIONS) as route_env:
        _fix_route_orders(system_env_with_db, route_env)

        orders = _get_order_ids(route_env)

        assert env_delete_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, 'orders/{}'.format(orders[1]))
        ).status_code == requests.codes.ok

        del orders[1]

        assert get_order_sequence(system_env_with_db, route_env['route']['id'])['order_ids'] == orders


def test_orders_batch_update(system_env_with_db):
    with create_route_env(system_env_with_db, "order_sequence_orders_batch_update_route_1", order_locations=LOCATIONS) as route_env1:
        with create_route_env(system_env_with_db, "order_sequence_orders_batch_update_route_2", order_locations=LOCATIONS) as route_env2:
            _fix_route_orders(system_env_with_db, route_env1)
            _fix_route_orders(system_env_with_db, route_env2)

            # move order to another route
            changes = [{'number': order['number']} for order in route_env1['orders']]
            changes[1]['route_id'] = route_env2['route']['id']
            batch_orders(system_env_with_db, changes)

            orders1 = _get_order_ids(route_env1)
            orders2 = _get_order_ids(route_env2)
            orders2.append(orders1[1])
            del orders1[1]

            assert get_order_sequence(
                system_env_with_db, route_env1['route']['id'])['order_ids'] == orders1
            assert get_order_sequence(
                system_env_with_db, route_env2['route']['id'])['order_ids'] == orders2


def test_orders_batch_create(system_env_with_db):
    with create_route_env(system_env_with_db, "order_sequence_orders_batch_create_route_1", order_locations=LOCATIONS) as route_env1:
        with create_route_env(system_env_with_db, "order_sequence_orders_batch_create_route_2", order_locations=LOCATIONS) as route_env2:
            _fix_route_orders(system_env_with_db, route_env1)
            _fix_route_orders(system_env_with_db, route_env2)

            # add new order to route
            changes = {
                'route_id': route_env2['route']['id'],
                'number': 'order_sequence_orders_batch_create_route_2_created',
            }
            changes.update({name: route_env1['orders'][0][name] for name in ['lat', 'lon', 'address', 'phone', 'time_interval']})
            batch_orders(system_env_with_db, [changes])

            assert get_order_sequence(
                system_env_with_db, route_env1['route']['id'])['order_ids'] == _get_order_ids(route_env1)
            orders2 = get_order_sequence(system_env_with_db, route_env2['route']['id'])['order_ids']
            assert len(orders2) == len(_get_order_ids(route_env2)) + 1
            assert orders2[:-1] == _get_order_ids(route_env2)


def test_orders_batch_create_many(system_env_with_db):
    with create_route_env(system_env_with_db, "order_sequence_orders_batch_create_many_route", order_locations=LOCATIONS) as route_env:
        _fix_route_orders(system_env_with_db, route_env)
        initial_order_sequence_ids = get_order_sequence(
            system_env_with_db, route_env['route']['id'])['order_ids']
        assert len(initial_order_sequence_ids) == 3

        number_template = "order_sequence_orders_batch_create_many_new_{}"

        # add 6 new orders
        changes = []
        for i in range(6):
            changes.append({
                'number': number_template.format(i)
            })
            changes[-1].update({
                name: route_env['orders'][0][name] for name in ['route_id', 'lat', 'lon', 'address', 'phone', 'time_interval']
            })
        batch_orders(system_env_with_db, changes)

        orders = get_orders(system_env_with_db, route_env['route']['id'])
        assert len(orders) == 9
        order_sequence_ids = get_order_sequence(
            system_env_with_db, route_env['route']['id'])['order_ids']
        assert len(order_sequence_ids) == 9

        # first 3 orders should not change
        assert initial_order_sequence_ids == order_sequence_ids[0:3]

        # next 6 orders should be added according to order in json
        id_to_number = {order["id"]: order["number"] for order in orders}
        for i, order_id in enumerate(order_sequence_ids[3:]):
            assert number_template.format(i) == id_to_number[order_id]


def test_order_details_sorted_by_sequence(system_env_with_db):
    with create_route_env(system_env_with_db, "order_sequence_order_details_sorted_by_sequence_1", order_locations=LOCATIONS) as route_env1:
        with create_route_env(system_env_with_db, "order_sequence_order_details_sorted_by_sequence_2", order_locations=LOCATIONS) as route_env2:
            depot_ids = [route_env['depot']['id'] for route_env in [route_env1, route_env2]]
            assert route_env1['route']['date'] == route_env2['route']['date']
            request_date = route_env1['route']['date']

            # Orders received from order-details should be in the same order as orders in the
            # predefined sequence.

            assert route_env1['route']['id'] < route_env2['route']['id']

            # Normal sequence of orders
            order_ids1 = _get_order_ids(route_env1)
            order_ids2 = _get_order_ids(route_env2)
            post_order_sequence(system_env_with_db, route_env1['route']['id'], order_ids1)
            post_order_sequence(system_env_with_db, route_env2['route']['id'], order_ids2)
            order_ids = _get_order_ids_from_order_details(system_env_with_db, request_date, depot_ids)
            assert order_ids == order_ids1 + order_ids2

            # Reversed sequence of orders
            order_ids1 = order_ids1[::-1]
            order_ids2 = order_ids2[::-1]
            post_order_sequence(system_env_with_db, route_env1['route']['id'], order_ids1)
            post_order_sequence(system_env_with_db, route_env2['route']['id'], order_ids2)
            order_ids = _get_order_ids_from_order_details(system_env_with_db, request_date, depot_ids)
            assert order_ids == order_ids1 + order_ids2


def test_post_order_sequence(system_env_with_db):
    with create_route_env(system_env_with_db, "route_with_order_sequence", order_locations=LOCATIONS) as route_env:
        order_ids = _get_order_ids(route_env)
        post_order_sequence(system_env_with_db, route_env['route']['id'], order_ids)
        real_order_ids = get_order_sequence(system_env_with_db, route_env['route']['id'])['order_ids']
        assert order_ids == real_order_ids

        order_ids.reverse()
        payload = {'order_ids': order_ids}
        post_order_sequence(system_env_with_db, route_env['route']['id'], payload)
        real_order_ids = get_order_sequence(system_env_with_db, route_env['route']['id'])['order_ids']
        assert order_ids == real_order_ids

        order_ids.reverse()
        number_from_id = {order['id']: order['number'] for order in route_env['orders']}
        order_numbers = [number_from_id[oid] for oid in order_ids]
        payload = {'order_numbers': order_numbers}
        post_order_sequence(system_env_with_db, route_env['route']['id'], payload)
        real_order_numbers = get_order_sequence(system_env_with_db, route_env['route']['id'])['order_numbers']
        assert order_numbers == real_order_numbers

        # invalid payloads
        error_msg = post_order_sequence(system_env_with_db, route_env['route']['id'],
                                        payload={'order_numbers': order_numbers + ['non-existing-number']},
                                        expected_status_code=requests.codes.unprocessable)['message']
        assert "Can not find objects defined by order_numbers field: 'non-existing-number'" == error_msg
        error_msg = post_order_sequence(system_env_with_db, route_env['route']['id'],
                                        payload={'order_numbers': order_numbers + ['non-existing-number'] * 2},
                                        expected_status_code=requests.codes.unprocessable)['message']
        assert "Following numbers are specified for at least two items: non-existing-number" == error_msg
        post_order_sequence(system_env_with_db, route_env['route']['id'], {}, requests.codes.unprocessable)
        post_order_sequence(system_env_with_db,
                            route_env['route']['id'],
                            {"order_numbers": []},
                            requests.codes.unprocessable)
        post_order_sequence(system_env_with_db,
                            route_env['route']['id'],
                            {"order_ids": []},
                            requests.codes.unprocessable)
        # TODO: uncomment these tests after BBGEO-4003
        post_order_sequence(system_env_with_db,
                            route_env['route']['id'],
                            {"order_ids": order_numbers},
                            requests.codes.unprocessable)
        """
        post_order_sequence(system_env_with_db,
                            route_env['route']['id'],
                            {"order_numbers": order_ids},
                            requests.codes.unprocessable)
        """
        post_order_sequence(system_env_with_db,
                            route_env['route']['id'],
                            {"order_ids": order_ids, "order_numbers": order_numbers},
                            requests.codes.unprocessable)
        post_order_sequence(system_env_with_db,
                            route_env['route']['id'],
                            {"order_ids": [order_ids[0]] * 2},
                            requests.codes.unprocessable)


@pytest.mark.parametrize("push_method", ["push_positions_v1", "push_positions_v2", "push_imei_positions"])
def test_order_sequence_courier_violated_route(system_env_with_db, push_method):
    """
    Test the following workflow:
    - Courier violates route sequence.
    - Courier edits route with /order-sequence.
    - Route becomes non-violated.
    """
    route_datetime = datetime.now(timezone.utc)
    order_locations = LOCATIONS

    imei = 12345
    with create_route_env(
            system_env_with_db,
            'test_order_sequence_courier_violated_route',
            order_locations=order_locations,
            imei=imei,
            time_intervals=['00:00-23:59'] * len(order_locations),
            route_date=route_datetime.date().isoformat()) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        depot_id = env['depot']['id']
        order_ids = _get_order_ids(env)

        route_info = get_route(system_env_with_db, route_id)
        assert not route_info['courier_violated_route']

        order_ids.reverse()
        track = [(order_locations[-1]['lat'], order_locations[-1]['lon'], route_datetime.replace(hour=10)),
                 (order_locations[-1]['lat'], order_locations[-1]['lon'], route_datetime.replace(hour=11))]

        if push_method == 'push_positions_v1' or push_method == 'push_positions_v2':
            track = [(lat, lon, date.timestamp()) for lat, lon, date in track]
            version = 1 if push_method == 'push_positions_v1' else 2
            push_positions(system_env_with_db, courier_id, route_id, track=track, version=version)
        else:
            push_imei_positions(system_env_with_db, imei, route_datetime, track=track)

        route_info = get_route(system_env_with_db, route_id)
        assert route_info['courier_violated_route']

        payload = {'order_ids': order_ids}
        post_order_sequence(system_env_with_db, route_id, payload)

        route_info = get_route(system_env_with_db, route_id)
        assert not route_info['courier_violated_route']

        def order_details_updated():
            order_details = get_order_details_by_route_id(system_env_with_db, route_id)
            for order in order_details:
                if order['order_id'] in order_ids[-2:]:
                    if 'arrival_time' not in order or 'arrival_time_s' not in order:
                        return False
                    assert order['arrival_time']
                    assert order['arrival_time_s']
            return True

        assert wait_until(order_details_updated, timeout=DEFAULT_UPDATE_ROUTE_STATES_PERIOD_S * 2)

        courier_report = get_courier_quality(system_env_with_db, date=route_datetime.date().isoformat(), depot_id=depot_id)
        for order in courier_report:
            assert not order['not_in_order']


@skip_if_remote
def test_user_access_to_own_company(system_env_with_db):
    """
        admin:
            should be able to see and modify order sequences for all routes of his company
        manager:
            should be able to see and modify order sequences only for the routes of his
            company he has access to (checked by depot access)
        dispatcher:
            should be able to only see order sequences only for the routes of his
            company he has access to (checked by depot access)
        app:
            should be able to see and modify order sequences for all routes of its company
    """
    env = system_env_with_db
    user_types = {
        UserKind.admin: UserRole.admin,
        UserKind.trusted_manager: UserRole.manager,
        UserKind.manager: UserRole.manager,
        UserKind.trusted_dispatcher: UserRole.dispatcher,
        UserKind.dispatcher: UserRole.dispatcher,
        UserKind.app: UserRole.app,
    }
    with create_tmp_company(env, "Test company test_order_sequence") as company_id:
        with create_tmp_users(env, [company_id] * len(user_types), list(user_types.values())) as users:
            user_info = dict(zip(user_types.keys(), users))
            with create_route_env(
                    env,
                    'test_user_access_to_own_company',
                    order_locations=[{'lat': 55.791928, 'lon': 37.841492}],
                    time_intervals=['08:00-18:00'],
                    company_id=company_id,
                    auth=env.auth_header_super) as route_env:

                depot_id = route_env['depot']['id']
                route_id = route_env['route']['id']
                assert len(route_env['orders']) == 1
                order_id = route_env['orders'][0]['id']

                # Provide access to the depot to the trusted manager (the other manager has no access)
                env_patch_request(
                    env,
                    api_path_with_company_id(env, 'user_depot', user_info[UserKind.trusted_manager]['id'], company_id=company_id),
                    data=[depot_id],
                    auth=env.auth_header_super
                )
                env_patch_request(
                    env,
                    api_path_with_company_id(env, 'user_depot', user_info[UserKind.trusted_dispatcher]['id'], company_id=company_id),
                    data=[depot_id],
                    auth=env.auth_header_super
                )

                expected_status_codes = {
                    UserKind.admin: (requests.codes.ok,) * 2,
                    UserKind.trusted_manager: (requests.codes.ok,) * 2,
                    UserKind.manager: (requests.codes.forbidden,) * 2,
                    UserKind.trusted_dispatcher: (requests.codes.ok, requests.codes.forbidden),
                    UserKind.dispatcher: (requests.codes.forbidden,) * 2,
                    UserKind.app: (requests.codes.ok,) * 2
                }

                for user_kind, user in user_info.items():
                    auth = env.get_user_auth(user)
                    get_result, post_result = expected_status_codes[user_kind]
                    get_order_sequence(env, route_id, expected_status_code=get_result, company_id=company_id, auth=auth)
                    post_order_sequence(env, route_id, [order_id], expected_status_code=post_result, company_id=company_id, auth=auth)


@skip_if_remote
def test_no_access_to_foreign_company(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup
    env = sharing_env['dbenv']
    for user_kind in UserKind:
        auth = env.get_user_auth(sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind])
        for other_company_idx in [c for c in [0, 1, 2] if c != SHARED_WITH_COMPANY_IDX]:
            other_company_id = sharing_env['companies'][other_company_idx]['id']
            for other_route in _get_env_routes(sharing_env, other_company_idx):
                get_order_sequence(env, other_route['id'], expected_status_code=requests.codes.forbidden, company_id=other_company_id, auth=auth)
                post_order_sequence(env, other_route['id'], [0, 1], expected_status_code=requests.codes.forbidden, company_id=other_company_id, auth=auth)


@skip_if_remote
def test_route_not_matching_company(env_with_default_sharing_setup):
    sharing_env = env_with_default_sharing_setup
    env = sharing_env['dbenv']
    company_id = sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['id']
    for user_kind in UserKind:
        post_result = (
            requests.codes.unprocessable
            if user_kind not in [UserKind.trusted_dispatcher, UserKind.dispatcher]
            else requests.codes.forbidden
        )
        auth = env.get_user_auth(sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind])
        for other_company_idx in [c for c in [0, 1, 2] if c != SHARED_WITH_COMPANY_IDX]:
            for other_route in _get_env_routes(sharing_env, other_company_idx):
                get_order_sequence(env, other_route['id'], expected_status_code=requests.codes.unprocessable, company_id=company_id, auth=auth)
                post_order_sequence(env, other_route['id'], [0, 1], expected_status_code=post_result, company_id=company_id, auth=auth)


@skip_if_remote
def test_post_order_sequence_with_duplicate_number(system_env_with_db):
    with create_route_env(system_env_with_db, 'route_with_order_sequence', order_locations=LOCATIONS) as route_env:
        order_ids = _get_order_ids(route_env)
        order_numbers = _get_order_numbers(route_env)

        data = [order_ids[0]] * len(order_ids)
        post_order_sequence(system_env_with_db, route_env['route']['id'],
                            data, expected_status_code=requests.codes.unprocessable)

        data = {'order_numbers': [order_numbers[0]] * len(order_numbers)}
        post_order_sequence(system_env_with_db, route_env['route']['id'],
                            data, expected_status_code=requests.codes.unprocessable)


@skip_if_remote
def test_post_fix_route_with_duplicate_number(system_env_with_db):
    with create_route_env(system_env_with_db, 'route_with_order_sequence', order_locations=LOCATIONS) as route_env:
        order_numbers = _get_order_numbers(route_env)

        data = {'orders': [order_numbers[0]] * len(order_numbers)}
        post_order_sequence(system_env_with_db, route_env['route']['id'],
                            data, expected_status_code=requests.codes.unprocessable)
