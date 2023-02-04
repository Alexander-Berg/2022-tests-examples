from maps.b2bgeo.ya_courier.backend.test_lib import util


def test_confirm_first_order_with_interval_change(system_env_with_db):
    """
    Test the following workflow:
        - a route with 2 orders is created, current time is 7:01
        - the first order is confirmed with changing time interval
          from 8:00-18:00 to 7:00-18:00 (courier shift is now started)
            * check: the first order is also the first in the routed orders list
        - change current location close to the second order
            * check: the first order is still the first in the routed orders list
                     because it is confirmed
    """
    ORDERS_LOCS = [
        {
            'lat': 55.791928,
            'lon': 37.841492,
        },
        {
            'lat': 55.900931,
            'lon': 37.623044,
        }
    ]
    INTERVALS = ['08:00-18:00'] * len(ORDERS_LOCS)

    with util.create_route_env(
            system_env_with_db,
            'test_confirm_first_order_with_interval_change',
            order_locations=ORDERS_LOCS,
            time_intervals=INTERVALS) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        current_location = {
            'lat': depot['lat'],
            'lon': depot['lon'],
            'time_now': '07:01'
        }

        routed_orders = util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)['route']
        first_order_id = routed_orders[0]['id']
        first_order = orders[0] if first_order_id == orders[0]['id'] else orders[1]
        second_order_id = routed_orders[1]['id']
        second_order = orders[0] if second_order_id == orders[0]['id'] else orders[1]

        # Confirm the first order and modify its time window before courier shift start
        # Check that the first order is also the first in the routed orders list
        util.confirm_order(system_env_with_db, first_order, time_interval='07:00-18:00')
        routed_orders = util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)['route']
        assert routed_orders[0]['id'] == first_order_id

        # Move the current location close to the second order.
        # Verify that the first order is still the first in the routed order list
        # because it is confirmed (route should significantly change)

        current_location = {
            'lat': second_order['lat'],
            'lon': second_order['lon'],
            'time_now': '10:01'
        }
        solution = util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        routed_orders = solution['route']
        assert routed_orders[0]['id'] == first_order_id
        assert "matrix_statistics" in solution
        matrix_statistics = solution['matrix_statistics']
        assert len(matrix_statistics) == 1
        assert "geodesic_distances" in matrix_statistics['driving']
        assert "total_distances" in matrix_statistics['driving']
        assert 'refined_lat' in routed_orders[0]
        assert 'refined_lon' in routed_orders[0]
