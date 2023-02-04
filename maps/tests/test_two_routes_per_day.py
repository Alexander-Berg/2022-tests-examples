from maps.b2bgeo.ya_courier.backend.test_lib import util

TEST_ID = "test_two_routes_"

COURIER_NUMBER = TEST_ID + "test_courier"
DEPOT_NUMBER = TEST_ID + "test_depot"
ROUTE_1_NUMBER = TEST_ID + "test_route_1"
ROUTE_2_NUMBER = TEST_ID + "test_route_2"
ORDERS_1_PREFIX = TEST_ID + "order_1"
ORDERS_2_PREFIX = TEST_ID + "order_2"


class TestTwoRoutesPerDay(object):
    def test_smoke(self, system_env_with_db):
        """
        Small sanity check for two routes for 1 courier for 1 day
            * create courier, depo
            * crate two routes for the couriler
            * call routed_orders for both routes
                - check: both routes have expected orders
            * deliver the first order of one of the routes
                - check: set of orders is still the same in both routes
                - check: first order of corresponding route is marked as finished
        """
        env = system_env_with_db
        util.cleanup_state(env, COURIER_NUMBER, [ROUTE_1_NUMBER, ROUTE_2_NUMBER], DEPOT_NUMBER)
        try:
            courier = util.create_courier(env, COURIER_NUMBER)
            courier_id = courier["id"]

            depot = util.create_depot(env, DEPOT_NUMBER)
            depot_id = depot["id"]

            route_1 = util.create_route(env, ROUTE_1_NUMBER, courier_id, depot_id)
            route_1_id = route_1["id"]

            route_2 = util.create_route(env, ROUTE_2_NUMBER, courier_id, depot_id)
            route_2_id = route_2["id"]

            orders_1 = util.create_orders(env, ORDERS_1_PREFIX, route_1_id)
            orders_2 = util.create_orders(env, ORDERS_2_PREFIX, route_2_id)
            routed_1_orders = util.query_routed_orders(
                env, courier_id, route_1_id)
            assert set(order["id"] for order in orders_1) == \
                set(order["id"] for order in routed_1_orders["route"])

            routed_2_orders = util.query_routed_orders(
                env, courier_id, route_2_id)
            assert set(order["id"] for order in orders_2) == \
                set(order["id"] for order in routed_2_orders["route"])

            resp = util.env_patch_request(
                env,
                "couriers/{}/routes/{}/orders/{}".format(
                    courier["id"],
                    route_1_id,
                    routed_1_orders["route"][0]["id"]
                ),
                data={"status": "finished"}
            )
            assert resp.ok, resp.text

            routed_1_orders = util.query_routed_orders(
                env, courier_id, route_1_id)
            assert set(order["id"] for order in orders_1) == \
                set(order["id"] for order in routed_1_orders["route"])
            assert routed_1_orders["route"][0]["status"] == "finished"

            routed_2_orders = util.query_routed_orders(
                env, courier_id, route_2_id)
            assert set(order["id"] for order in orders_2) == \
                set(order["id"] for order in routed_2_orders["route"])

        finally:
            util.cleanup_state(env, COURIER_NUMBER, [ROUTE_1_NUMBER, ROUTE_2_NUMBER], DEPOT_NUMBER)
