from datetime import datetime
import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    create_orders,
    query_routed_orders, finish_order, confirm_order,
    cancel_order, change_order_time_interval, change_order_coordinates,
    create_route_env,
    fix_orders, get_order_sequence, post_order_sequence,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import UserKind
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


def _get_order_numbers(orders):
    return [order['number'] for order in orders]


def _get_order_ids(orders):
    return [order['id'] for order in orders]


def check_routed_orders(system_env_with_db, courier_id, route_id, expected_number_sequence, point=None):
    routed_orders = query_routed_orders(system_env_with_db, courier_id, route_id, point=point)
    routed_orders_numbers = [order["number"] for order in routed_orders["route"]]
    assert routed_orders_numbers == expected_number_sequence


def check_routed_orders_not_equal(system_env_with_db, courier_id, route_id, expected_number_sequence, point=None):
    routed_orders = query_routed_orders(system_env_with_db, courier_id, route_id, point=point)
    routed_orders_numbers = [order["number"] for order in routed_orders["route"]]
    assert routed_orders_numbers != expected_number_sequence


POINTS = [
    {"lat": 55.663878, "lon": 37.482458},
    {"lat": 55.683761, "lon": 37.518000},
    {"lat": 55.705491, "lon": 37.551859},
]


class TestFixOrders(object):
    def test_restrictions(self, system_env_with_db):
        """
        Test if restrictions work.
        """
        with create_route_env(system_env_with_db, "fix_order_restrictions_1_", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)

            # Should fail, orders are not specified
            fix_orders(system_env_with_db, route['id'], {}, expected_status_code=requests.codes.unprocessable)

            # Should fail, not all orders are specified
            fix_orders(system_env_with_db, route['id'], [], expected_status_code=requests.codes.unprocessable)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)

            query_routed_orders(system_env_with_db, courier['id'], route['id'])

            # It is allowed to add orders to the route if the route has predefined sequence of orders
            orders_added = create_orders(
                system_env_with_db,
                order_number="order_restrictions_should_success",
                route_id=route['id']
            )
            assert len(orders_added) == 2
            fixed_numbers += _get_order_numbers(orders_added)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            # It is allowed to add orders to other routes
            with create_route_env(system_env_with_db, "fix_order_restrictions_2_", POINTS):
                pass

            # Currently orders can be modified through PATCH-order request
            change_order_coordinates(
                system_env_with_db, orders[1],
                lat=orders[1]["lat"] + 0.000001,
                lon=orders[1]["lon"] + 0.000001
            )
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

    @skip_if_remote
    def test_user_cannot_use_foreign_company_route(self, env_with_default_sharing_setup):
        SHARED_WITH_COMPANY_IDX = 0
        sharing_env = env_with_default_sharing_setup
        env = sharing_env['dbenv']
        user_kind = UserKind.admin

        company_idx = 0
        other_company_idx = 1
        for other_route in sharing_env['companies'][other_company_idx]['all_routes']:
            auth = env.get_user_auth(sharing_env['companies'][SHARED_WITH_COMPANY_IDX]['users'][user_kind])

            wrong_company_text = 'is associated with some other company'

            assert wrong_company_text in get_order_sequence(
                env,
                other_route['id'],
                expected_status_code=requests.codes.unprocessable,
                company_id=sharing_env['companies'][company_idx]['id'],
                auth=auth
            )['message']

            route_orders = [order for order in sharing_env['companies'][other_company_idx]['all_orders']
                            if order['route_id'] == other_route['id']]

            assert wrong_company_text in post_order_sequence(
                env,
                other_route['id'],
                [order['id'] for order in route_orders],
                expected_status_code=requests.codes.unprocessable,
                company_id=sharing_env['companies'][company_idx]['id'],
                auth=auth
            )['message']

            assert wrong_company_text in fix_orders(
                env,
                other_route['id'],
                [order['number'] for order in route_orders],
                expected_status_code=requests.codes.unprocessable,
                company_id=sharing_env['companies'][company_idx]['id'],
                auth=auth
            )['message']

    def test_reschedule(self, system_env_with_db):
        """
        Test the following workflow:
            - orders are created
            - orders are fixed
              * check: orders are in predefined sequence
            - order time interval is changed
              * check: orders are in predefined sequence
        """
        with create_route_env(system_env_with_db, "fix_order_reschedule", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            change_order_time_interval(system_env_with_db, orders[1], "20:00-23:00")
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

    def test_delivered(self, system_env_with_db):
        """
        Test the following workflow:
            - orders are created
            - orders are fixed
            - the first order is delivered
              * check: we can get routed orders
              * check: we can get routed orders, when they don't have to be optimized
        """
        with create_route_env(system_env_with_db, "fix_order_delivered", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            finish_order(system_env_with_db, orders[0])
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

    def test_confirmed(self, system_env_with_db):
        """
        Test the following workflow:
            - orders are created
            - orders are fixed
            - the second order is confirmed
              * check: courier is able to get routed orders
            - the first order is delivered
              * check: courier is able to get routed orders
              * check: courier is able to get routed orders
        """
        with create_route_env(system_env_with_db, "fix_order_confirmed", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            confirm_order(system_env_with_db, orders[1])
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            finish_order(system_env_with_db, orders[0])
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

    @skip_if_remote
    def test_delete(self, system_env_with_db):
        """
        Test the following workflow:
            - orders are created
            - orders are fixed
                * check: orders are in predefined sequence
            - order fixation is deleted
                * check: orders are in the same sequence (ETA did not change, so don't reschedule)
            - change point of courier
                * check: orders are in the optimal sequence
        """
        with create_route_env(system_env_with_db, "fix_order_delete", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            fixed_numbers = _get_order_numbers(route_env['orders'])

            fix_orders(system_env_with_db, route['id'], fixed_numbers)

            point_1 = {
                "lat": 55.739421,
                "lon": 37.587081,
                "time_now": datetime.now().time().strftime("%H:%M:%S")
            }

            point_2 = {
                "lat": 56.290145,
                "lon": 38.383849,
                "time_now": datetime.now().time().strftime("%H:%M:%S")
            }

            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers, point=point_1)

            # If requesting routed orders from the same point, it should not change: ETA did not change
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers, point=point_1)

            # If requesting routed orders from other point, ETA will change, but the order of orders will
            # stay the same because orders are fixed.
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers, point=point_2)

    def test_cancel(self, system_env_with_db):
        """
        Test the following workflow:
            - orders are created
            - orders are fixed
                * check: orders are in predefined sequence
            - order in the middle is cancelled
                * check: orders are still in predefined state
        """
        with create_route_env(system_env_with_db, "fix_cancel", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            cancel_order(system_env_with_db, orders[1])
            # Request route from other point, ETA should change. And rescheduling should happen
            check_routed_orders_not_equal(system_env_with_db, courier['id'], route['id'], fixed_numbers)

    def test_wrong_first_orders(self, system_env_with_db):
        """
        Test the following workflow:
            - orders are created and routing is started
            - 2 first orders are finished
            - try to fix orders starting with the last order
                * check: orders can be fixed even when the sequence does not start with finished order
                * check: routing works and routes are in expected order
            - try to fix orders in the correct sequence
                * check: orders can be fixed after routing is started
                * check: routing works and routes are in expected order
        """
        with create_route_env(system_env_with_db, "wrong_first_orders", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)
            fixed_ids = _get_order_ids(orders)

            query_routed_orders(
                system_env_with_db, courier['id'], route['id'])

            for order in orders[:2]:
                finish_order(system_env_with_db, order)

            fix_orders(system_env_with_db, route['id'], [fixed_numbers[-1]] + fixed_numbers[:-1])
            assert get_order_sequence(system_env_with_db, route['id'])['order_ids'] == [fixed_ids[-1]] + fixed_ids[:-1]
            # Fixing orders (specyfying order sequence) doesn't change the order of orders in routed-orders
            # results because ordering is "hardcoded" in _join_orders_with_eta(), e.g. finished orders are
            # always first.
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            assert get_order_sequence(system_env_with_db, route['id'])['order_ids'] == fixed_ids
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

    def test_replace_fixed_order(self, system_env_with_db):
        """
        Test the following workflow:
            - orders are created and fixed
            - routing is started
            - first order is confirmed and routing-orders is queried
            - change fixed orders: swap first and second order
            - query routed-orders
                * check: verify that the change was applied
        """
        with create_route_env(system_env_with_db, "replace_fixed_order", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            confirm_order(system_env_with_db, orders[0])
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            fixed_numbers[0], fixed_numbers[1] = fixed_numbers[1], fixed_numbers[0]
            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

    def test_change_sequence_with_finished_route(self, system_env_with_db):
        """
        Test the following workflow:
            - orders are created and fixed
            - routing is started
            - first order is confirmed and finished
            - change fixed orders: swap second and third orders
            - query routed-orders
                * check: verify that the change was applied
        """
        with create_route_env(system_env_with_db, "change_sequence_with_finished_route", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            confirm_order(system_env_with_db, orders[0])
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            finish_order(system_env_with_db, orders[0])
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            fixed_numbers[1], fixed_numbers[2] = fixed_numbers[2], fixed_numbers[1]
            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

    def test_deliver_wrong_order(self, system_env_with_db):
        """
        Test the following workflow:
            - the route with 3 orders is created and the orders are fixed
            - routing is started
            - the second order is delivered
            - /eta is called
                * check: orders in the response are in the order: second, first, third
            - /routed_orders is called
                * check: orders in the response are in the order: second, first, third
        """
        with create_route_env(system_env_with_db, "test_deliver_wrong_order", POINTS) as route_env:
            courier = route_env['courier']
            route = route_env['route']
            orders = route_env['orders']
            fixed_numbers = _get_order_numbers(orders)

            fix_orders(system_env_with_db, route['id'], fixed_numbers)
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)

            finish_order(system_env_with_db, orders[1])

            fixed_numbers[0], fixed_numbers[1] = fixed_numbers[1], fixed_numbers[0]
            check_routed_orders(system_env_with_db, courier['id'], route['id'], fixed_numbers)
