import datetime

from maps.b2bgeo.ya_courier.backend.test_lib import util
from maps.b2bgeo.ya_courier.backend.test_lib.util_deprecated import (
    DATE_TODAY_MSK,
    TIMEZONE_MSK,
)
from maps.b2bgeo.libs.py_flask_utils.format_util import parse_time
from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str


DEPOT_TIME_ZONE = TIMEZONE_MSK
ROUTE_DATETIME = datetime.datetime(DATE_TODAY_MSK.year, DATE_TODAY_MSK.month, DATE_TODAY_MSK.day, 0, 0, 0, tzinfo=DEPOT_TIME_ZONE)
POINTS = [
    {"lat": 55.733827, "lon": 37.588722},
    {"lat": 55.729299, "lon": 37.580116}
]
INTERVALS = ['08:00-23.00'] * len(POINTS)


def _push_position(system_env_with_db, location, courier_id, route_id):
    util.push_positions(
        system_env_with_db,
        courier_id,
        route_id,
        track=[(location['lat'], location['lon'], (ROUTE_DATETIME + parse_time(location['time_now'])).timestamp())])


def _get_order_history(system_env_with_db, order_track):
    return util.get_tracking_info(order_track, system_env_with_db)['order']['history']


def test_order_history(system_env_with_db):
    with util.create_route_env(
            system_env_with_db,
            'test_order_history',
            order_locations=POINTS,
            time_intervals=INTERVALS,
            depot_data={"mark_route_started_radius": 0},
            route_date=ROUTE_DATETIME.date().isoformat()) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        assert util.get_order_track_id(system_env_with_db, orders[0]['id']) is None
        assert util.get_order_track_id(system_env_with_db, orders[1]['id']) is None

        current_location = {
            'lat': depot['lat'],
            'lon': depot['lon'],
            'time_now': '07:00'
        }

        _push_position(system_env_with_db, current_location, courier_id, route_id)

        routed_orders = util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        orders = routed_orders['route']
        assert util.get_order_track_id(system_env_with_db, orders[0]['id']) is None
        assert util.get_order_track_id(system_env_with_db, orders[1]['id']) is None

        current_location['time_now'] = '08:01'
        route = util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        assert route['route'][0]['id'] == orders[0]['id']

        # check that tracks appeared because we sent notification sms for each order
        track0 = util.get_order_track_id(system_env_with_db, orders[0]['id'])
        track1 = util.get_order_track_id(system_env_with_db, orders[1]['id'])
        assert track0
        assert track1

        history0 = _get_order_history(system_env_with_db, track0)
        assert len(history0) == 3
        assert history0[0]['event'] == 'ORDER_CREATED'
        assert history0[0]['timestamp']
        assert history0[0]['time']
        assert history0[0]['time'] == get_isoformat_str(history0[0]['timestamp'], DEPOT_TIME_ZONE)
        assert history0[1]['event'] == 'START'
        assert history0[1]['timestamp']
        assert history0[1]['time']
        assert history0[1]['time'] == get_isoformat_str(history0[1]['timestamp'], DEPOT_TIME_ZONE)
        assert history0[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history0[2]['timestamp']
        assert history0[2]['time']

        history1 = _get_order_history(system_env_with_db, track1)
        assert len(history1) == 2
        assert history1[0]['event'] == 'ORDER_CREATED'
        assert history1[1]['event'] == 'START'

        util.confirm_order(system_env_with_db, orders[0])

        history0 = _get_order_history(system_env_with_db, track0)
        assert len(history0) == 4
        assert history1[0]['event'] == 'ORDER_CREATED'
        assert history0[1]['event'] == 'START'
        assert history0[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history0[2]['timestamp']
        assert history0[2]['time']
        assert history0[3]['event'] == 'STATUS_UPDATE'
        assert history0[3]['status'] == 'confirmed'

        assert history1 == _get_order_history(system_env_with_db, track1)

        util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        history0 = _get_order_history(system_env_with_db, track0)
        assert len(history0) == 4
        assert history0[0]['event'] == 'ORDER_CREATED'
        assert history0[1]['event'] == 'START'
        assert history0[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history0[2]['timestamp']
        assert history0[2]['time']
        assert history0[3]['event'] == 'STATUS_UPDATE'
        assert history0[3]['status'] == 'confirmed'

        assert history1 == _get_order_history(system_env_with_db, track1)

        current_location['lat'] = orders[0]['lat']
        current_location['lon'] = orders[0]['lon']
        util.finish_order(system_env_with_db, orders[0])

        history0 = _get_order_history(system_env_with_db, track0)
        assert len(history0) == 5
        assert history0[0]['event'] == 'ORDER_CREATED'
        assert history0[1]['event'] == 'START'
        assert history0[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history0[2]['timestamp']
        assert history0[2]['time']
        assert history0[3]['event'] == 'STATUS_UPDATE'
        assert history0[3]['status'] == 'confirmed'
        assert history0[4]['event'] == 'STATUS_UPDATE'
        assert history0[4]['status'] == 'finished'

        assert history1 == _get_order_history(system_env_with_db, track1)

        util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        assert route['route'][1]['id'] == orders[1]['id']
        assert history0 == _get_order_history(system_env_with_db, track0)
        history1 = _get_order_history(system_env_with_db, track1)
        assert len(history1) == 3
        assert history1[0]['event'] == 'ORDER_CREATED'
        assert history1[1]['event'] == 'START'
        assert history1[1]['timestamp']
        assert history1[1]['time']
        assert history1[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history1[2]['timestamp']
        assert history1[2]['time']

        util.confirm_order(system_env_with_db, orders[1])
        assert history0 == _get_order_history(system_env_with_db, track0)
        history1 = _get_order_history(system_env_with_db, track1)
        assert len(history1) == 4
        assert history1[0]['event'] == 'ORDER_CREATED'
        assert history1[1]['event'] == 'START'
        assert history1[1]['timestamp']
        assert history1[1]['time']
        assert history1[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history1[2]['timestamp']
        assert history1[2]['time']
        assert history1[3]['event'] == 'STATUS_UPDATE'
        assert history1[3]['status'] == 'confirmed'

        util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        assert history0 == _get_order_history(system_env_with_db, track0)
        history1 = _get_order_history(system_env_with_db, track1)
        assert len(history1) == 4
        assert history1[0]['event'] == 'ORDER_CREATED'
        assert history1[1]['event'] == 'START'
        assert history1[1]['timestamp']
        assert history1[1]['time']
        assert history1[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history1[2]['timestamp']
        assert history1[2]['time']
        assert history1[3]['event'] == 'STATUS_UPDATE'
        assert history1[3]['status'] == 'confirmed'

        current_location['lat'] = orders[1]['lat']
        current_location['lon'] = orders[1]['lon']
        util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        util.finish_order(system_env_with_db, orders[1])

        assert history0 == _get_order_history(system_env_with_db, track0)
        history1 = _get_order_history(system_env_with_db, track1)
        assert len(history1) == 5
        assert history1[0]['event'] == 'ORDER_CREATED'
        assert history1[1]['event'] == 'START'
        assert history1[1]['timestamp']
        assert history1[1]['time']
        assert history1[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history1[2]['timestamp']
        assert history1[2]['time']
        assert history1[3]['event'] == 'STATUS_UPDATE'
        assert history1[3]['status'] == 'confirmed'
        assert history1[4]['event'] == 'STATUS_UPDATE'
        assert history1[4]['status'] == 'finished'

        util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        assert history0 == _get_order_history(system_env_with_db, track0)
        assert history1 == _get_order_history(system_env_with_db, track1)


def test_history_after_batch_update(system_env_with_db):
    """
    Test the following workflow:
        - a route with 2 orders is created
        - the route is started and tracks for the orders are requested
        - the orders are modified using orders-batch request
        - track history for the tracks is requested
            * check: the track history corresponds to the orders-batch modification
    """
    with util.create_route_env(
            system_env_with_db,
            'test_history_after_batch_update',
            order_locations=POINTS,
            time_intervals=INTERVALS,
            depot_data={"mark_route_started_radius": 0},
            route_date=ROUTE_DATETIME.date().isoformat()) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        current_location = {
            'lat': depot['lat'],
            'lon': depot['lon'],
            'time_now': '07:00'
        }
        _push_position(system_env_with_db, current_location, courier_id, route_id)

        current_location['time_now'] = '08:01'
        util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        track0 = util.get_order_track_id(system_env_with_db, orders[0]['id'])
        track1 = util.get_order_track_id(system_env_with_db, orders[1]['id'])

        changes = [
            {
                'number': orders[0]['number'],
                'time_interval': '00:00 - 01:00',
                'status': 'confirmed'
            },
            {
                'number': orders[1]['number'],
                'time_interval': '00:00 - 01:00'
            }
        ]
        util.batch_orders(system_env_with_db, changes)

        history0 = _get_order_history(system_env_with_db, track0)
        assert len(history0) == 5
        assert history0[0]['event'] == 'ORDER_CREATED'
        assert history0[1]['event'] == 'START'
        assert history0[2]['event'] == 'ORDER_BECAME_NEXT'
        assert history0[2]['timestamp']
        assert history0[2]['time']
        assert history0[3]['event'] == 'INTERVAL_UPDATE'
        assert history0[3]['time_interval'] == '00:00 - 01:00'
        assert history0[4]['event'] == 'STATUS_UPDATE'
        assert history0[4]['status'] == 'confirmed'

        history1 = _get_order_history(system_env_with_db, track1)
        assert len(history1) == 3
        assert history1[0]['event'] == 'ORDER_CREATED'
        assert history1[1]['event'] == 'START'
        assert history1[1]['timestamp']
        assert history1[1]['time']
        assert history1[2]['event'] == 'INTERVAL_UPDATE'
        assert history1[2]['time_interval'] == '00:00 - 01:00'
