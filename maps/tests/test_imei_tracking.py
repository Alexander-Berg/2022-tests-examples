import datetime
import dateutil.tz
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    cleanup_state,
    create_courier, create_orders, create_route, create_depot,
    push_imei_positions, get_courier_position_list
)
from ya_courier_backend.models.route import ACTIVE_ROUTE_MAX_EARLINESS_H, ACTIVE_ROUTE_MAX_LATENESS_H

TEST_ID = "test_imei_tracking_"

COURIER_NUMBER = TEST_ID + "test_courier"
DEPOT_NUMBER = TEST_ID + "test_depot"
ROUTE_NUMBER = TEST_ID + "test_route"
ORDERS_PREFIX = TEST_ID + "order"
IMEI1 = 12412427345543
IMEI2 = 89865227488303


class TestImeiTracking(object):
    # Test that positions sent with imei are registered only for
    # active routes. A route is active if the time of
    # the position is inside or near the time windows of
    # the orders in the route.
    def test_logic(self, system_env_with_db):
        current_hour = 15
        current_datetime = datetime.datetime(
            2018, 10, 26, current_hour, 30, 0, tzinfo=dateutil.tz.tzutc())
        routes = []

        def add_route(day, time_interval, imei, result):
            idx = len(routes)
            routes.append({
                'route_number': '{}{}'.format(ROUTE_NUMBER, idx),
                'order_number': '{}{}'.format(ORDERS_PREFIX, idx),
                'day': day,
                'time_interval': time_interval,
                'imei': imei,
                'result': result
            })
        # Day before yesterday orders
        add_route(-2, '00:00-23:59', IMEI2, False)
        add_route(-2, '00:00-23:59', IMEI1, False)
        # Day after tomorrow orders
        add_route(+2, '00:00-23:59', IMEI2, False)
        add_route(+2, '00:00-23:59', IMEI1, False)
        # Yesterday orders
        add_route(-1, '00:00-23:59', IMEI2, False)
        hour = 24 - ACTIVE_ROUTE_MAX_LATENESS_H - 1
        assert hour >= 1
        add_route(-1, '00:00-{}:00'.format(hour), IMEI1, False)
        # Tomorrow orders
        add_route(+1, '00:00-23:59', IMEI2, False)
        hour = ACTIVE_ROUTE_MAX_EARLINESS_H + 1
        assert hour <= 23
        add_route(+1, '{}:00-23:59'.format(hour), IMEI1, False)
        # Today order with wrong IMEI
        add_route(0, '00:00-23:59', IMEI2, False)
        # Today order's time interval is now
        add_route(0, '{0}:00-{1}:59'.format(current_hour, current_hour + 1), IMEI1, True)
        # Today order's time interval is slightly in the future
        add_route(0, '{0}:00-{0}:59'.format(current_hour + 1), IMEI1, True)
        # Today order's time interval is slightly in the past
        add_route(0, '{0}:00-{0}:59'.format(current_hour - 1), IMEI1, True)
        # Today order's time interval is deep in the past
        hour = current_hour - ACTIVE_ROUTE_MAX_LATENESS_H - 1
        assert hour >= 0
        add_route(0, '0:00-{0}:59'.format(hour), IMEI1, False)

        cleanup_state(system_env_with_db, COURIER_NUMBER, [x['route_number'] for x in routes], DEPOT_NUMBER)

        try:
            courier = create_courier(system_env_with_db, COURIER_NUMBER)
            courier_id = courier['id']

            depot = create_depot(system_env_with_db, DEPOT_NUMBER)
            depot_id = depot['id']

            for route in routes:
                route_date = (
                    current_datetime.date() +
                    datetime.timedelta(days=route['day'])
                )
                route_id = create_route(
                    system_env_with_db,
                    route['route_number'],
                    courier_id,
                    depot_id,
                    route_date=route_date.isoformat(),
                    imei=route['imei'])['id']
                create_orders(
                    system_env_with_db,
                    route['order_number'],
                    route_id,
                    time_intervals=[route['time_interval']]*2)
                route['route_id'] = route_id

            for route in routes:
                positions = get_courier_position_list(
                    system_env_with_db, courier_id, route['route_id'])
                assert isinstance(positions, list)
                assert len(positions) == 0

            push_imei_positions(system_env_with_db, IMEI1, current_datetime)

            for route in routes:
                print(route)
                positions = get_courier_position_list(
                    system_env_with_db, courier_id, route['route_id'])
                assert isinstance(positions, list)
                if route['result']:
                    assert len(positions) >= 1
                else:
                    assert len(positions) == 0

        finally:
            cleanup_state(system_env_with_db, COURIER_NUMBER, [x['route_number'] for x in routes], DEPOT_NUMBER)
