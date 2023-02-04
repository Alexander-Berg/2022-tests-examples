import datetime
from collections import namedtuple
from copy import deepcopy

import dateutil.tz
import pytest
import time

from maps.b2bgeo.ya_courier.backend.test_lib.config import COURIER_QUALITY_FIELDS
from ya_courier_backend.logic.courier_quality import (
    get_courier_quality,
    sort, get_suggested_order_id,
    get_value_or_infinity,
)
from ya_courier_backend.logic.segment_distance import (
    compute_route_segment_distances,
    get_order_completed_at,
)
from ya_courier_backend.logic.tracking import get_history_last_tracking_event_timestamp
from ya_courier_backend.models import (
    RoutingMode, Courier, Depot, TimeWindow, RouteStateHistory, Garage, Route, OrderHistoryEvent, Order,
    DepotInstanceHistoryEvent, DepotInstanceStatus, DepotInstance, OrderStatus, RouteNode
)
from ya_courier_backend.models.order import LATE_CALL_BEFORE_DELIVERY_LIMIT_S
from ya_courier_backend.models.route_node import ROUTE_NODE_TYPES

DEPOT_TIMEZONE_STR = 'Europe/Moscow'
DEPOT_TIMEZONE = dateutil.tz.gettz(DEPOT_TIMEZONE_STR)

NodeData = namedtuple('NodeData', ['RouteNode', 'Courier', 'Depot', 'Route'])


def _get_entity_ids(rows):
    return [row.RouteNode.entity_id for row in rows]


def get_datetime_in_depot_timezone_str(dt):
    return dt.astimezone(DEPOT_TIMEZONE).isoformat()


def _get_order_history_with_visit(timestamp):
    return [
        {
            "event": OrderHistoryEvent.visit,
            "position":
                {
                    "time": str(datetime.datetime.fromtimestamp(timestamp, dateutil.tz.tzutc()))
                }
        }
    ]


def _get_depot_instance_history_with_visit(timestamp):
    return [
        {
            "event": DepotInstanceHistoryEvent.visit.value,
            "timestamp": timestamp,
            "position":
                {
                    "time": str(datetime.datetime.fromtimestamp(timestamp, dateutil.tz.tzutc()))
                }
        }
    ]


def _get_order(courier_id, route_id, order_id, status, sequence_pos, timestamp=None, history_visit_timestamp=None):
    if history_visit_timestamp is None:
        history_visit_timestamp = timestamp
    return NodeData(
        RouteNode(
            entity=Order(
                id=order_id,
                status=status,
                status_log=[{"status": status.value, "timestamp": timestamp}],
                history=_get_order_history_with_visit(history_visit_timestamp) if history_visit_timestamp else None,
                route_sequence_pos=sequence_pos,
                route_id=route_id,
            ),
            route_id=route_id,
            route_sequence_pos=sequence_pos
        ),
        Courier(
            id=courier_id
        ),
        Depot(
        ),
        Route(
            id=route_id,
            courier_id=courier_id
        )
    )


def _get_depot(courier_id, route_id, depot_id, sequence_pos, timestamp=None):
    depot_instance = DepotInstance(id=depot_id, depot_id=depot_id, route_id=route_id)
    if timestamp is not None:
        depot_instance.history = _get_depot_instance_history_with_visit(timestamp)
        depot_instance.status = DepotInstanceStatus.visited

    return NodeData(
        RouteNode(
            entity=depot_instance,
            route_id=route_id,
            route_sequence_pos=sequence_pos
        ),
        Courier(
            id=courier_id
        ),
        Depot(
            id=depot_id
        ),
        Route(
            id=route_id,
            depot_id=depot_id,
            courier_id=courier_id
        )
    )


def _get_garage(courier_id, route_id, depot_id, garage_id, sequence_pos):
    return NodeData(
        RouteNode(
            entity=Garage(
                id=garage_id,
                route_id=route_id
            ),
            route_id=route_id,
            route_sequence_pos=sequence_pos
        ),
        Courier(
            id=courier_id
        ),
        Depot(
            id=depot_id
        ),
        Route(
            id=route_id,
            depot_id=depot_id,
            courier_id=courier_id
        )
    )


NODES_LISTS = [
    [
        _get_garage(courier_id=1, route_id=10, depot_id=1, garage_id=1000, sequence_pos=0),
        _get_depot(courier_id=1, route_id=10, depot_id=1, sequence_pos=1, timestamp=1533644390),
        _get_order(courier_id=1, route_id=10, order_id=106, status=OrderStatus.new, timestamp=1533644369,
                   sequence_pos=2),
        _get_order(courier_id=1, route_id=10, order_id=104, status=OrderStatus.new, timestamp=1533644369,
                   sequence_pos=3),
        _get_order(courier_id=2, route_id=12, order_id=107, status=OrderStatus.new, timestamp=1533644369,
                   sequence_pos=1),
        _get_order(courier_id=1, route_id=11, order_id=190, status=OrderStatus.new, timestamp=1533644369,
                   sequence_pos=1),
        _get_order(courier_id=1, route_id=10, order_id=103, status=OrderStatus.new, timestamp=1533644370,
                   sequence_pos=4),
        _get_order(courier_id=1, route_id=10, order_id=102, status=OrderStatus.new, timestamp=1533644371,
                   sequence_pos=5),
        _get_order(courier_id=1, route_id=10, order_id=100, status=OrderStatus.finished, timestamp=1533644390,
                   sequence_pos=6),
        _get_order(courier_id=1, route_id=10, order_id=105, status=OrderStatus.cancelled, timestamp=1533644380,
                   sequence_pos=7),
        _get_order(courier_id=1, route_id=10, order_id=101, status=OrderStatus.finished, timestamp=1533644370,
                   sequence_pos=8),
        _get_depot(courier_id=1, route_id=10, depot_id=2, sequence_pos=9),
        _get_garage(courier_id=1, route_id=10, depot_id=1, garage_id=1001, sequence_pos=10),
    ],
    [
        _get_order(courier_id=1, route_id=10, order_id=101, status=OrderStatus.finished, timestamp=1533644370,
                   sequence_pos=1),
        _get_order(courier_id=1, route_id=10, order_id=102, status=OrderStatus.finished, timestamp=1533644380,
                   sequence_pos=2),
        _get_order(courier_id=1, route_id=11, order_id=111, status=OrderStatus.finished, timestamp=1533644390,
                   sequence_pos=1),
        _get_order(courier_id=1, route_id=11, order_id=112, status=OrderStatus.finished, timestamp=1533644400,
                   sequence_pos=2),
    ],
]


def test_sorting_empty_list_does_nothing():
    rows = []
    sort(rows)
    assert rows == []


def test_visited_are_sorted_by_timestamp():
    rows = list(reversed(NODES_LISTS[0]))
    sort(rows)
    assert _get_entity_ids(rows) == [1000, 1, 101, 105, 100, 106, 104, 103, 102, 2, 1001, 190, 107]


def test_unvisited_are_sorted_by_sequence_pos():
    rows = list(reversed([
        _get_garage(courier_id=1, route_id=10, depot_id=1, garage_id=1000, sequence_pos=0),
        _get_depot(courier_id=1, route_id=10, depot_id=1, sequence_pos=1),
        _get_order(courier_id=1, route_id=10, order_id=106, status=OrderStatus.new, sequence_pos=2),
        _get_order(courier_id=1, route_id=10, order_id=104, status=OrderStatus.new, sequence_pos=3),
        _get_depot(courier_id=1, route_id=10, depot_id=2, sequence_pos=4),
        _get_garage(courier_id=1, route_id=10, depot_id=1, garage_id=1001, sequence_pos=5),
    ]))
    sort(rows)
    assert _get_entity_ids(rows) == [1000, 1, 106, 104, 2, 1001]


class SegmentDistanceParameters:
    """ Parameters for sort and segment distance testing """

    def __init__(self, orders, courier_positions, expected_segment_distances_compute):
        self.orders = orders
        self.courier_positions = courier_positions
        self.expected_segment_distances_compute = expected_segment_distances_compute


def _get_position(timestamp, lat, lon):
    return {
        "courier_id": 1,
        "route_id": 10,
        "lat": lat,
        "lon": lon,
        "time": timestamp
    }


COURIER_POSITIONS = [
    _get_position(timestamp=1533644350, lat=55.728033, lon=37.583119),
    _get_position(timestamp=1533644360, lat=55.729074, lon=37.584965),
    _get_position(timestamp=1533644371, lat=55.729764, lon=37.586831),
    _get_position(timestamp=1533644379, lat=55.730793, lon=37.589600),
    _get_position(timestamp=1533644385, lat=55.731629, lon=37.591638),
    _get_position(timestamp=1533644388, lat=55.734828, lon=37.585503),
    _get_position(timestamp=1533644390, lat=55.732423, lon=37.591991),
    _get_position(timestamp=1533644395, lat=55.735055, lon=37.593870),
    _get_position(timestamp=1533644400, lat=55.733475, lon=37.592452),
]

SEGMENT_DISTANCES_PARAMS_LIST = [
    SegmentDistanceParameters(
        [],
        COURIER_POSITIONS,
        {}
    ),
    SegmentDistanceParameters(
        NODES_LISTS[0],
        COURIER_POSITIONS,
        {
            100: pytest.approx(248.88494695803547),
            101: 0,
            102: pytest.approx(139.77743215630662),
            103: 0,
            104: 0,
            105: pytest.approx(207.7271448311294),
            106: pytest.approx(163.5835697265534),
            107: 0,
            190: 0,
        }
    ),
    SegmentDistanceParameters(
        NODES_LISTS[0],
        COURIER_POSITIONS[6:],
        {
            100: 0,
            101: 0,
            102: 0,
            103: 0,
            104: 0,
            105: 0,
            106: 0,
            107: 0,
            190: 0,
        }
    ),
    SegmentDistanceParameters(
        NODES_LISTS[1],
        COURIER_POSITIONS,
        {
            101: pytest.approx(163.5835697265534),
            102: pytest.approx(347.50457698743605),
            111: 0,
            112: 0
        }
    )
]


class MockOrder:
    def __init__(self, order_id, route_sequence_pos, history):
        self.order_id = order_id
        self.route_sequence_pos = route_sequence_pos
        self.history = history


class MockDepotInstance:
    def __init__(self, depot_id, route_sequence_pos, history):
        self.depot_id = depot_id
        self.route_sequence_pos = route_sequence_pos
        self.history = history


class MockRouteNode:
    def __init__(self, entity, route_sequence_pos):
        self.id = 1
        self.type = 'order' if isinstance(entity, MockOrder) else 'depot_instance'
        self.entity = entity
        self.route_sequence_pos = route_sequence_pos
        self.segment_distance_m = None

    def is_order(self):
        return self.type == 'order'

    def is_garage(self):
        return self.type == 'garage'

    @property
    def visit_event_name(self):
        if self.type == ROUTE_NODE_TYPES[Order]:
            return OrderHistoryEvent.visit
        elif self.type == ROUTE_NODE_TYPES[DepotInstance]:
            return DepotInstanceHistoryEvent.visit.value

        return None

    @property
    def visited_at(self):
        if visit_event := self.visit_event_name:
            return get_history_last_tracking_event_timestamp(self.entity.history,
                                                             visit_event)

        return None


def _compute_segment_distances(orders, positions):
    segment_distances = {}

    route_ids = {order.Route.id for order in orders}

    for route_id in route_ids:
        route_order_nodes = [
            deepcopy(order.RouteNode)
            for order in orders if order.RouteNode.is_order() and order.RouteNode.route_id == route_id
        ]

        route_positions = [p for p in positions if p['route_id'] == route_id]
        route_positions.sort(key=lambda p: p['time'])

        compute_route_segment_distances(route_order_nodes, route_positions)

        for route_order_node in route_order_nodes:
            assert route_order_node.entity_id not in segment_distances
            segment_distances[route_order_node.entity_id] = route_order_node.segment_distance_m

    return segment_distances


@pytest.mark.parametrize("params", SEGMENT_DISTANCES_PARAMS_LIST)
def test_segment_distances(params):
    orders = params.orders.copy()
    sort(orders)

    order_ids = {order.RouteNode.order.id for order in orders if order.RouteNode.is_order()}

    assert order_ids == set(params.expected_segment_distances_compute.keys())
    distances = _compute_segment_distances(orders, params.courier_positions)
    assert distances == params.expected_segment_distances_compute


def test_segment_distances_depot():
    depot = MockDepotInstance(1, 10, _get_depot_instance_history_with_visit(1533644370))
    route_node = MockRouteNode(depot, depot.route_sequence_pos)
    compute_route_segment_distances([route_node], COURIER_POSITIONS)
    assert route_node.segment_distance_m == pytest.approx(163.5835697265534)


# https://st.yandex-team.ru/BBGEO-2806
def test_segment_distances_bbgeo2806():
    now = time.time()
    orders = [
        _get_order(courier_id=1, route_id=2, order_id=1, status=OrderStatus.finished, timestamp=now, sequence_pos=1),
        _get_order(courier_id=2, route_id=1, order_id=2, status=OrderStatus.finished, timestamp=now, sequence_pos=1),
    ]
    positions = [
        {"route_id": 2, "courier_id": 1, "time": now - 1500, "lat": 55.685778, "lon": 37.459498 + 2e-3},
        {"route_id": 2, "courier_id": 1, "time": now - 10, "lat": 55.685778, "lon": 37.459498},
        {"route_id": 1, "courier_id": 2, "time": now - 1500, "lat": 55.685778, "lon": 37.459498 + 4e-3},
        {"route_id": 1, "courier_id": 2, "time": now - 10, "lat": 55.685778, "lon": 37.459498},
    ]

    distances = _compute_segment_distances(orders, positions)
    assert distances[1] == pytest.approx(125.36807)
    assert distances[2] == pytest.approx(250.73614)


def test_segment_distances_delayed_positions():
    now = time.time()
    orders = [
        _get_order(courier_id=1, route_id=2, order_id=2, status=OrderStatus.finished, timestamp=now, sequence_pos=2,
                   history_visit_timestamp=now - 200),
        _get_order(courier_id=1, route_id=2, order_id=1, status=OrderStatus.finished, timestamp=now, sequence_pos=1,
                   history_visit_timestamp=now - 900),
    ]
    positions = [
        {"route_id": 2, "courier_id": 1, "time": now - 1500, "lat": 55.685778, "lon": 37.459498 + 4e-2},
        {"route_id": 2, "courier_id": 1, "time": now - 1000, "lat": 55.685778, "lon": 37.459498 + 3e-3},
        {"route_id": 2, "courier_id": 1, "time": now - 300, "lat": 55.685778, "lon": 37.459498 + 1e-3},
    ]

    distances = _compute_segment_distances(orders, positions)
    assert distances[1] == pytest.approx(2319.30933)
    assert distances[2] == pytest.approx(125.36807)


def test_get_value_or_infinity():
    assert get_value_or_infinity(None) == float('inf')
    assert get_value_or_infinity(None) == get_value_or_infinity(None)
    assert get_value_or_infinity(0) == 0
    assert get_value_or_infinity(0) is not None
    assert get_value_or_infinity(123) == 123
    assert get_value_or_infinity(1234.567) == 1234.567


@pytest.mark.parametrize("finished_status", Order.all_finished_statuses())
def test_get_air_distance(finished_status):
    assert Order(lat=55.685778, lon=37.459498, status_log=[
        {
            "status": finished_status.value,
            "timestamp": 1533644370,
            "point": {
                "lat": "55.685778",
                "lon": "37.459498"
            }
        }
    ]).air_distance == pytest.approx(0.0)

    assert Order(lat=55.685778, lon=37.458498, status_log=[
        {
            "status": finished_status.value,
            "timestamp": 1533644370,
            "point": {
                "lat": 55.685778,
                "lon": 37.459498
            }
        }
    ]).air_distance == pytest.approx(62.684036)

    assert Order(lat=55.685778, lon=37.459498, status_log=[
        {
            "status": OrderStatus.confirmed.value,
            "timestamp": 1533644370,
            "point": {
                "lat": 55.685778,
                "lon": 37.459498
            }
        }
    ]).air_distance is None

    assert Order(lat=55.685778, lon=37.459498, status_log=[
        {
            "status": finished_status.value,
            "timestamp": 1533644370,
        }
    ]).air_distance is None

    assert Order(lat=55.685778, lon=37.459498, status_log=None).air_distance is None


@pytest.mark.parametrize("finished_status", Order.all_finished_statuses())
def test_one_order(finished_status):
    route_datetime = datetime.datetime(2019, 6, 16, tzinfo=dateutil.tz.tzutc())
    stored_visit_datetime = route_datetime.replace(hour=14, minute=30)
    stored_arrived_at = get_datetime_in_depot_timezone_str(stored_visit_datetime.replace(minute=15))
    stored_left_at = get_datetime_in_depot_timezone_str(stored_visit_datetime.replace(minute=45))

    status_log = [
        {
            "status": OrderStatus.confirmed.value,
            "timestamp": route_datetime.replace(hour=12).timestamp(),
            "point": {
                "lat": 55.685778,
                "lon": 37.459498
            }
        },
        {
            "status": finished_status.value,
            "timestamp": route_datetime.replace(hour=12, minute=40).timestamp(),
            "point": {
                "lat": 55.685778,
                "lon": 37.459498
            }
        }
    ]

    order_history = [
        {
            'event': OrderHistoryEvent.visit,
            'position': {'lat': 55.685778, 'lon': 37.459498,
                         'time': stored_visit_datetime.astimezone(DEPOT_TIMEZONE).isoformat()},
            'timestamp': stored_visit_datetime.timestamp()
        },
        {
            'event': OrderHistoryEvent.arrival,
            'position': {'lat': 55.684, 'lon': 37.459, 'time': stored_arrived_at},
            'timestamp': stored_visit_datetime.timestamp()
        },
        {
            'event': OrderHistoryEvent.status_update,
            'status': finished_status.value,
            'timestamp': stored_visit_datetime.timestamp(),
            'comment': 'Some comment'
        },
        {
            'event': OrderHistoryEvent.departure,
            'position': {'lat': 55.686, 'lon': 37.460, 'time': stored_left_at},
            'timestamp': stored_visit_datetime.timestamp() + 15 * 60
        }
    ]

    nodes_data = [
        NodeData(
            RouteNode=RouteNode(entity=Order(id=101, route_id=10, lat=55.685778, lon=37.459498,
                                             number="order_number_101", address="Some address 101",
                                             status=OrderStatus.new, phone="order_phone", status_log=[],
                                             time_windows=[], history=None, service_duration_s=600, volume=1,
                                             weight=1, route_sequence_pos=1, comments='', order_sharing=[],
                                             customer_name="customer_name"),
                                route_sequence_pos=1, segment_distance_m=None),
            Courier=Courier(id=1000, name="courier_name", number="courier_number", deleted=False),
            Depot=Depot(number="depot_number", time_zone=DEPOT_TIMEZONE_STR),
            Route=Route(id=1, number="route_number", imei=11111111111111111, routing_mode=RoutingMode.driving,
                        date=route_datetime.date())
        ),
        NodeData(
            RouteNode=RouteNode(entity=Order(id=100, route_id=10, lat=55.685778, lon=37.459498,
                                             number="order_number_100", address="Some address 100",
                                             status=finished_status, phone="order_phone", status_log=status_log,
                                             time_windows=[TimeWindow(start=route_datetime.replace(hour=13),
                                                                      end=route_datetime.replace(hour=14))],
                                             history=order_history, service_duration_s=600, volume=1,
                                             weight=1, route_sequence_pos=2, comments='', order_sharing=[],
                                             customer_name="customer_name"),
                                route_sequence_pos=2, segment_distance_m=123),
            Courier=Courier(id=1000, name="courier_name", number="courier_number", deleted=False),
            Depot=Depot(number="depot_number", time_zone=DEPOT_TIMEZONE_STR),
            Route=Route(id=10, number="route_number", imei=11111111111111111, routing_mode=RoutingMode.driving,
                        date=route_datetime.date())
        ),
    ]
    route_state_history_by_order_id = {100: RouteStateHistory(state={'next_orders': [101]})}

    positions_visit_datetime = route_datetime.replace(hour=16)
    assert abs(positions_visit_datetime.timestamp() - stored_visit_datetime.timestamp() > 1 * 60)
    expected_visited_at = get_datetime_in_depot_timezone_str(stored_visit_datetime)
    expected_arrived_at = stored_arrived_at
    expected_left_at = stored_left_at
    expected = [{
        'type': 'order',
        'air_distance': 0.0,
        'arrived_at': expected_arrived_at,
        'courier_name': 'courier_name',
        'courier_number': 'courier_number',
        'courier_deleted': False,
        'customer_name': 'customer_name',
        'depot_number': 'depot_number',
        'left_at': expected_left_at,
        'used_mark_delivered_radius': None,
        'order_number': 'order_number_100',
        "order_address": "Some address 100",
        'order_status': finished_status,
        'order_status_comments': [
            {
                "id": 2,
                "status": finished_status.value,
                "comment": "Some comment",
            }
        ],
        'route_number': 'route_number',
        "route_imei": 11111111111111111,
        'route_imei_str': '11111111111111111',
        'order_confirmed_at': get_datetime_in_depot_timezone_str(route_datetime.replace(hour=12)),
        'order_completed_at': get_datetime_in_depot_timezone_str(route_datetime.replace(hour=12, minute=40)),
        'far_from_point': False,
        'no_call_before_delivery': False,
        'late_call_before_delivery': False,
        'time_interval_error': -datetime.timedelta(minutes=20).total_seconds(),
        'delivery_not_in_interval': True,
        'not_in_order': True,
        'suggested_order_number': 'order_number_101',
        "order_interval": [{
            "start": route_datetime.replace(hour=13),
            "end": route_datetime.replace(hour=14)
        }],
        "segment_distance_m": 123,
        "order_visited_at": expected_visited_at,
        'route_routing_mode': RoutingMode.driving,
        'route_date': route_datetime.date(),
        'order_weight': 1,
        'order_volume': 1,
        'refined_lat': None,
        'refined_lon': None,
        'order_comments': '',
        'order_shared_with_companies': [],
        'delivery_lat': 55.685778,
        'delivery_lon': 37.459498,
        'lat': 55.685778,
        'lon': 37.459498,
        'transit_idle_duration': None,
        'location_idle_duration': None,
        'service_duration_s': 600,
        'shared_service_duration_s': None,
        'phone': 'order_phone'
    }, {
        'type': 'order',
        'air_distance': None,
        'arrived_at': None,
        'courier_name': 'courier_name',
        'courier_number': 'courier_number',
        'courier_deleted': False,
        'customer_name': 'customer_name',
        'delivery_not_in_interval': False,
        'depot_number': 'depot_number',
        'far_from_point': None,
        'late_call_before_delivery': None,
        'left_at': None,
        'used_mark_delivered_radius': None,
        'no_call_before_delivery': True,
        'not_in_order': None,
        'order_completed_at': None,
        'order_confirmed_at': None,
        'order_number': 'order_number_101',
        "order_address": "Some address 101",
        "route_imei": 11111111111111111,
        'route_imei_str': '11111111111111111',
        'order_status': OrderStatus.new,
        'order_status_comments': [],
        'route_number': 'route_number',
        'suggested_order_number': None,
        'time_interval_error': None,
        'order_interval': [],
        'segment_distance_m': None,
        "order_visited_at": None,
        'route_routing_mode': RoutingMode.driving,
        'route_date': route_datetime.date(),
        'order_weight': 1,
        'order_volume': 1,
        'refined_lat': None,
        'refined_lon': None,
        'order_comments': '',
        'order_shared_with_companies': [],
        'delivery_lat': None,
        'delivery_lon': None,
        'lat': 55.685778,
        'lon': 37.459498,
        'transit_idle_duration': None,
        'location_idle_duration': None,
        'service_duration_s': 600,
        'shared_service_duration_s': None,
        'phone': 'order_phone'
    }]

    received = get_courier_quality(nodes_data, route_state_history_by_order_id, {}, True)
    assert received == expected
    for what in ['start', 'end']:
        datetime_received = received[0]['order_interval'][0][what]
        datetime_expected = expected[0]['order_interval'][0][what]
        assert datetime_received.isoformat() == get_datetime_in_depot_timezone_str(datetime_expected)
    assert len(expected[0]) == len(COURIER_QUALITY_FIELDS['order'])
    assert set(expected[0]) == COURIER_QUALITY_FIELDS['order']


@pytest.mark.parametrize("order_status", OrderStatus)
def test_get_order_completed_at(order_status):
    result = get_order_completed_at({
        "order_status": order_status,
        "order_status_log": [{
            "status": order_status.value,
            "timestamp": 1533644370,
        }]
    })
    if order_status in Order.all_completed_statuses():
        assert result == 1533644370
    else:
        assert result is None


def test_get_order_completed_at_none():
    assert get_order_completed_at({
        "order_status": OrderStatus.new,
        "order_status_log": None
    }) is None


@pytest.mark.parametrize("order_status", OrderStatus)
def test_get_order_confirmed_at_last(order_status):
    result = Order(
        status_log=[{
            "status": order_status.value,
            "timestamp": 1533644370,
        }]).computed_confirmed_at

    if order_status in [OrderStatus.confirmed]:
        assert result == 1533644370
    else:
        assert result is None


def test_get_order_confirmed_at_not_last():
    assert Order(
        status_log=[
            {
                "status": OrderStatus.confirmed.value,
                "timestamp": 1533644370,
            },
            {
                "status": OrderStatus.new.value,
                "timestamp": 1533644371,
            }
        ]).computed_confirmed_at == 1533644370


def test_order_confirmed_at_none():
    assert Order(status=OrderStatus.new,
                 status_log=None).computed_confirmed_at is None


def test_get_far_from_point():
    assert Order(lat=55.685778, lon=37.458498, status=OrderStatus.new, status_log=[
        {
            "status": OrderStatus.new.value,
            "timestamp": 1533644370,
            "point": {
                "lat": 55.685778,
                "lon": 37.459498
            }
        }
    ]).is_far_from_point is None
    assert Order(lat=55.685778, lon=37.458498, status=OrderStatus.finished).is_far_from_point is None
    assert Order(lat=55.685778, lon=37.458498, status=OrderStatus.finished, status_log=[
        {
            "status": OrderStatus.finished.value,
            "timestamp": 1533644370,
            "point": {
                "lat": 55.685778,
                "lon": 37.458498
            }
        }
    ]).is_far_from_point is False
    assert Order(lat=55.685778, lon=37.458498, status=OrderStatus.partially_finished, status_log=[
        {
            "status": OrderStatus.partially_finished.value,
            "timestamp": 1533644370,
            "point": {
                "lat": 55.685778,
                "lon": 37.459498
            }
        }
    ]).is_far_from_point is False
    assert Order(lat=55.685778, lon=37.458498, status=OrderStatus.partially_finished, status_log=[
        {
            "status": OrderStatus.partially_finished.value,
            "timestamp": 1533644370,
            "point": {
                "lat": 55.687588,
                "lon": 37.458498
            }
        }
    ]).is_far_from_point is True


def test_get_no_call_before_delivery():
    assert Order(status=OrderStatus.new).no_call_before_delivery

    assert not Order(status=OrderStatus.cancelled).no_call_before_delivery

    assert not Order(status=OrderStatus.confirmed,
                     status_log=[{'status': OrderStatus.confirmed.value, 'timestamp': 1533644370}]
                     ).no_call_before_delivery
    assert not Order(status=OrderStatus.finished,
                     status_log=[{'status': OrderStatus.confirmed.value, 'timestamp': 1533644370}]
                     ).no_call_before_delivery
    assert Order(status=OrderStatus.partially_finished).no_call_before_delivery


def test_get_late_call_before_delivery():
    assert not Order(status=OrderStatus.new).late_call_before_delivery
    assert not Order(status=OrderStatus.confirmed).late_call_before_delivery
    assert not Order(status=OrderStatus.finished,
                     status_log=[
                         {'status': OrderStatus.finished.value, 'timestamp': 1533644370}]).late_call_before_delivery
    assert not Order(status=OrderStatus.finished,
                     status_log=[{'status': OrderStatus.confirmed.value, 'timestamp': 1533644371},
                                 {'status': OrderStatus.finished.value,
                                  'timestamp': 1533644371 + LATE_CALL_BEFORE_DELIVERY_LIMIT_S}]
                     ).late_call_before_delivery
    assert Order(status=OrderStatus.partially_finished,
                 status_log=[{'status': OrderStatus.confirmed.value, 'timestamp': 1533644371},
                             {'status': OrderStatus.finished.value,
                              'timestamp': 1533644371 + LATE_CALL_BEFORE_DELIVERY_LIMIT_S - 1}]
                 ).late_call_before_delivery
    assert Order(status=OrderStatus.finished,
                 status_log=[{'status': OrderStatus.confirmed.value, 'timestamp': 1533644371},
                             {'status': OrderStatus.finished.value, 'timestamp': 1533644371}]
                 ).late_call_before_delivery


def test_get_time_interval_error():
    assert Order(status=OrderStatus.new).time_interval_error is None
    assert Order(status=OrderStatus.confirmed).time_interval_error is None
    assert Order(status=OrderStatus.cancelled).time_interval_error is None
    assert Order(status=OrderStatus.finished,
                 time_windows=[TimeWindow(
                     start=datetime.datetime(2018, 7, 8, 5, 0, 0, 0, dateutil.tz.tzutc()),
                     end=datetime.datetime(2018, 7, 8, 7, 0, 0, 0, dateutil.tz.tzutc())
                 )],
                 status_log=[{'status': OrderStatus.finished.value,
                              'timestamp': datetime.datetime(2018, 7, 8, 6, 0, 0, 0, dateutil.tz.tzutc()).timestamp()}]
                 ).time_interval_error is None
    assert Order(status=OrderStatus.finished,
                 time_windows=[TimeWindow(
                     start=datetime.datetime(2018, 7, 8, 6, 30, 0, 0, dateutil.tz.tzutc()),
                     end=datetime.datetime(2018, 7, 8, 7, 0, 0, 0, dateutil.tz.tzutc())
                 )],
                 status_log=[{'status': OrderStatus.finished.value,
                              'timestamp': datetime.datetime(2018, 7, 8, 6, 0, 0, 0, dateutil.tz.tzutc()).timestamp()}]
                 ).time_interval_error == -30 * 60
    assert Order(status=OrderStatus.finished,
                 time_windows=[TimeWindow(
                     start=datetime.datetime(2018, 7, 8, 5, 0, 0, 0, dateutil.tz.tzutc()),
                     end=datetime.datetime(2018, 7, 8, 5, 45, 0, 0, dateutil.tz.tzutc())
                 )],
                 status_log=[{'status': OrderStatus.finished.value,
                              'timestamp': datetime.datetime(2018, 7, 8, 6, 0, 0, 0, dateutil.tz.tzutc()).timestamp()}]
                 ).time_interval_error == 15 * 60
    assert Order(status=OrderStatus.finished,
                 time_windows=[],
                 status_log=[{'status': OrderStatus.finished.value,
                              'timestamp': datetime.datetime(2018, 7, 8, 6, 0, 0, 0, dateutil.tz.tzutc()).timestamp()}]
                 ).time_interval_error is None
    assert Order(status=OrderStatus.finished,
                 time_windows=[
                     TimeWindow(
                         start=datetime.datetime(2018, 7, 8, 5, 0, 0, 0, dateutil.tz.tzutc()),
                         end=datetime.datetime(2018, 7, 8, 5, 45, 0, 0, dateutil.tz.tzutc())
                     ),
                     TimeWindow(
                         start=datetime.datetime(2018, 7, 8, 5, 0, 0, 0, dateutil.tz.tzutc()),
                         end=datetime.datetime(2018, 7, 8, 5, 45, 0, 0, dateutil.tz.tzutc())
                     )
                 ],
                 status_log=[{'status': OrderStatus.finished.value,
                              'timestamp': datetime.datetime(2018, 7, 8, 6, 0, 0, 0, dateutil.tz.tzutc()).timestamp()}]
                 ).time_interval_error is None


@pytest.mark.parametrize("finished_status", Order.all_finished_statuses())
def test_get_fixed_order(finished_status):
    assert get_suggested_order_id(Order(status=finished_status), set(), None) is None
    assert get_suggested_order_id(Order(status=finished_status), set(), []) is None
    assert get_suggested_order_id(Order(status=finished_status), set(), [1]) == 1
    assert get_suggested_order_id(Order(status=finished_status), set(), [1, 2]) == 1
    assert get_suggested_order_id(Order(status=finished_status), {1}, []) is None
    assert get_suggested_order_id(Order(status=finished_status), {1}, [1]) is None
    assert get_suggested_order_id(Order(status=finished_status), {1}, [1, 2]) == 2
    assert get_suggested_order_id(Order(status=finished_status), {2}, [1, 2]) == 1
    assert get_suggested_order_id(Order(status=finished_status), {1, 2}, [1, 2, 3]) == 3
    assert get_suggested_order_id(Order(status=finished_status), {2, 3}, [1, 2, 3]) == 1
    assert get_suggested_order_id(Order(status=finished_status), {1, 3}, [1, 2, 3]) == 2
