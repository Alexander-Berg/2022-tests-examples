import datetime

import dateutil.tz
import pytest
from flask import Flask

from maps.b2bgeo.ya_courier.backend.test_lib.util import get_position_shifted_east
from ya_courier_backend.logic.tracking import (
    detect_route_sequence_violation, detect_route_sequence_violation_by_statuses
)
from ya_courier_backend.models import LogisticCompany, Order, OrderHistoryEvent, OrderStatus, RouteNode
from ya_courier_backend.models.order import (
    add_order_history_event, has_order_history_event
)

_TEST_TIMEZONE = dateutil.tz.gettz("Europe/Moscow")
_TEST_COMPANY = LogisticCompany(
    mark_delivered_radius=500,
    mark_delivered_service_time_coefficient=1,
    mark_delivered_enabled=True
)


@pytest.fixture
def env_mock_app():
    app = Flask(__name__)
    with app.app_context() as ctx:
        yield ctx


def _get_orders_line(distances):
    """
    returns list of orders which stand on a line going from west to east x_i meters apart from the previous location.
    Example:
        _get_orders_line([100, 1000, 600]) returns 4 orders like:
        distance(order_0, order_1) ~= 100
        distance(order_1, order_2) ~= 1000
        distance(order_2, order_3) ~= 600
        distance(order_0, order_3) ~= 100+1000+600
    """
    distances = [0] + distances

    orders = []
    for i, offset in enumerate(distances):
        order = Order()
        order.id = i
        order.status = OrderStatus.new
        order.history = []
        order.route_sequence_pos = i

        if i == 0:
            order.lat = 55.740021
            order.lon = 37.478933
        else:
            lat, lon = get_position_shifted_east(orders[-1].lat, orders[-1].lon, offset)
            order.lat = lat
            order.lon = lon

        orders.append(order)
    return orders


def _route_nodes_from_orders(orders):
    return [
        RouteNode(entity=order, route_sequence_pos=pos)
        for pos, order in enumerate(orders)
    ]


def _mark_visited(order_nodes, finished_status):
    for order_node in order_nodes:
        order = order_node.entity
        order.status = finished_status
        if not has_order_history_event(order, OrderHistoryEvent.visit):
            dt = datetime.datetime.now(_TEST_TIMEZONE)

            add_order_history_event(order, OrderHistoryEvent.visit, details={
                'position': {
                    'lat': order.lat,
                    'lon': order.lon,
                    'time': dt.isoformat()
                }
            })


@pytest.mark.parametrize('finished_status', Order.all_finished_statuses())
def test_correct_route_sequence_basic(env_mock_app, finished_status):
    orders = _get_orders_line([600, 1500])
    order_nodes = _route_nodes_from_orders(orders)

    assert not detect_route_sequence_violation(_TEST_COMPANY, route_order_nodes=[], newly_visited_order_nodes=[])
    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[])

    _mark_visited([order_nodes[0]], finished_status=finished_status)

    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[0]])

    _mark_visited([order_nodes[1]], finished_status=finished_status)

    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[1]])
    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=order_nodes[:2])

    _mark_visited([order_nodes[2]], finished_status=finished_status)
    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[2]])
    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=order_nodes[1:])
    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=order_nodes)


def test_correct_route_sequence_closely_located_1(env_mock_app):
    orders = _get_orders_line([400, 1500])
    order_nodes = _route_nodes_from_orders(orders)

    _mark_visited([order_nodes[1]], finished_status=OrderStatus.finished)

    # the first order is skipped but that's ok because the second is located within the radius of the first.
    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[1]])

    orders = _get_orders_line([50, 50, 50, 0, 50, 100, 1500])  # all orders except the last have overlapping radius
    order_nodes = _route_nodes_from_orders(orders)
    assert len(order_nodes) == 8
    closely_located_order_nodes = order_nodes[:7]
    for order_node in closely_located_order_nodes[::-1]:
        _mark_visited([order_node], finished_status=OrderStatus.finished)
        assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_node])

    _mark_visited([order_nodes[-1]], finished_status=OrderStatus.partially_finished)
    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[-1]])


def test_correct_route_sequence_closely_located_2(env_mock_app):
    orders = _get_orders_line([400, 400])
    order_nodes = _route_nodes_from_orders(orders)
    _mark_visited([order_nodes[1]], finished_status=OrderStatus.finished)
    assert not detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[1]])


def test_violated_route_sequence_basic_1(env_mock_app):
    orders = _get_orders_line([510, 510])
    order_nodes = _route_nodes_from_orders(orders)
    _mark_visited([order_nodes[1]], finished_status=OrderStatus.finished)

    assert detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[1]]) == {
        'newly_visited_order': orders[1],
        'skipped_orders': [orders[0]]
    }


def test_violated_route_sequence_basic_2(env_mock_app):
    orders = _get_orders_line([1000] * 10)
    order_nodes = _route_nodes_from_orders(orders)
    _mark_visited(order_nodes[:3], finished_status=OrderStatus.finished)

    for i in range(4, 11):
        _mark_visited([order_nodes[i]], finished_status=OrderStatus.finished)
        assert detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[i]]) == {
            'newly_visited_order': orders[i],
            'skipped_orders': [orders[3]]
        }
        assert detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=order_nodes[4: i + 1]) == {
            'newly_visited_order': orders[4],
            'skipped_orders': [orders[3]]
        }


def test_violated_route_sequence_holes(env_mock_app):
    orders = _get_orders_line([510, 510, 50])
    order_nodes = _route_nodes_from_orders(orders)
    _mark_visited([order_nodes[0], order_nodes[-1]], finished_status=OrderStatus.finished)

    assert detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[-1]]) == {
        'newly_visited_order': orders[-1],
        'skipped_orders': [orders[1]]  # orders[2] is not considered as skipped because it's located in the radius of
                                       # the order[-1]
    }


def test_violated_route_sequence_holes_with_detection_by_statuses(env_mock_app):
    orders = _get_orders_line([510, 510, 50])
    order_nodes = _route_nodes_from_orders(orders)
    _mark_visited([order_nodes[0], order_nodes[-1]], finished_status=OrderStatus.finished)

    assert detect_route_sequence_violation_by_statuses(_TEST_COMPANY, order_nodes) == {
        'newly_visited_order': orders[-1],
        'skipped_orders': [orders[1]]  # orders[2] is not considered as skipped because it's located in the radius of
                                       # the order[-1]
    }


def test_violated_route_sequence_same_location(env_mock_app):
    # Orders 1 and 3 are very close to each other
    orders = _get_orders_line([510, 0])
    orders[2].lat = orders[0].lat
    orders[2].lon = orders[0].lon

    order_nodes = _route_nodes_from_orders(orders)

    # Complete order 1
    _mark_visited([order_nodes[0]], finished_status=OrderStatus.finished)
    # Mark the order 3 node visited but don't change its status to finished.
    # This is the case when 'visited' event is being added by tracking system,
    # but order statuses are controlled manually by couriers.
    _mark_visited([order_nodes[-1]], finished_status=order_nodes[-1].entity.status)

    assert detect_route_sequence_violation(_TEST_COMPANY, order_nodes, newly_visited_order_nodes=[order_nodes[0], order_nodes[-1]]) == {
        'newly_visited_order': orders[-1],
        'skipped_orders': [orders[1]]
    }


def test_violated_route_sequence_same_location_with_detection_by_statuses(env_mock_app):
    # Orders 1 and 3 are very close to each other
    orders = _get_orders_line([510, 0])
    orders[2].lat = orders[0].lat
    orders[2].lon = orders[0].lon

    order_nodes = _route_nodes_from_orders(orders)

    # Complete order 1
    _mark_visited([order_nodes[0]], finished_status=OrderStatus.finished)
    # Mark the order 3 node visited but don't change its status to finished.
    # This is the case when 'visited' event is being added by tracking system,
    # but order statuses are controlled manually by couriers.
    _mark_visited([order_nodes[-1]], finished_status=order_nodes[-1].entity.status)

    assert detect_route_sequence_violation_by_statuses(_TEST_COMPANY, order_nodes) is None
