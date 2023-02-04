from typing import Optional

import pytest

from ya_courier_backend.logic.segment_distance import compute_route_idles
from ya_courier_backend.models import RouteNode, RouteEvent, RouteEventType
from ya_courier_backend.models.route_node import Point


class MockRouteNode(RouteNode):
    _visited_at: Optional[int]
    _point: Point
    _visit_radius: int

    @property
    def visited_at(self):
        return self._visited_at

    @visited_at.setter
    def visited_at(self, var):
        self._visited_at = var

    @property
    def point(self):
        return self._point

    @point.setter
    def point(self, var):
        self._point = var

    @property
    def visit_radius(self):
        return self._visit_radius

    @visit_radius.setter
    def visit_radius(self, var):
        self._visit_radius = var


def _create_node(visited_at: Optional[int], point: Point, visit_radius: Optional[int] = 100) -> MockRouteNode:
    node = MockRouteNode()
    node.visited_at = visited_at
    node.point = point
    node.visit_radius = visit_radius
    return node


def _create_idle(start: int, finish: Optional[int], point: Point) -> RouteEvent:
    return RouteEvent(
        start_timestamp=start,
        finish_timestamp=finish,
        lat=point.lat,
        lon=point.lon,
        type=RouteEventType.IDLE
    )


def test_empty_lists():
    nodes = []
    idles = []

    compute_route_idles(nodes, idles)


@pytest.mark.parametrize('nodes', ([_create_node(None, Point(0, 0))],
                                   [_create_node(15, Point(0, 0))],
                                   [_create_node(15, Point(0, 0)), _create_node(None, Point(0, 0))],
                                   [_create_node(None, Point(0, 0)), _create_node(15, Point(0, 0))],
                                   ))
def test_empty_idles_list(nodes: list[MockRouteNode]):
    idles = []

    compute_route_idles(nodes, idles)

    for node in nodes:
        if node.visited_at:
            assert node.transit_idle_duration == 0
            assert node.location_idle_duration == 0
        else:
            assert node.transit_idle_duration is None
            assert node.location_idle_duration is None


def test_nothing_visited():
    nodes = [
        _create_node(None, Point(0, 0))
    ]
    idles = [
        _create_idle(0, 10, Point(10, 10)),
        _create_idle(20, 30, Point(0, 0)),
    ]

    compute_route_idles(nodes, idles)

    assert nodes[0].transit_idle_duration is None
    assert nodes[0].location_idle_duration is None


def test_visited_order():
    nodes = [
        _create_node(15, Point(0, 0))
    ]
    idles = [
        _create_idle(0, 10, Point(10, 10)),
        _create_idle(20, 30, Point(0, 0)),
    ]

    compute_route_idles(nodes, idles)

    assert nodes[0].transit_idle_duration == 10
    assert nodes[0].location_idle_duration == 10


def test_two_orders_first_visited():
    nodes = [
        _create_node(15, Point(0, 0)),
        _create_node(None, Point(10, 0))
    ]
    idles = [
        _create_idle(0, 10, Point(10, 10)),
        _create_idle(20, 30, Point(0, 0)),
    ]

    compute_route_idles(nodes, idles)

    assert nodes[0].transit_idle_duration == 10
    assert nodes[0].location_idle_duration == 10


def test_two_orders_all_visited():
    nodes = [
        _create_node(15, Point(0, 0)),
        _create_node(35, Point(10, 0))
    ]
    idles = [
        _create_idle(0, 10, Point(10, 10)),
        _create_idle(20, 30, Point(0, 0)),
    ]

    compute_route_idles(nodes, idles)

    assert nodes[0].transit_idle_duration == 10
    assert nodes[0].location_idle_duration == 10

    assert nodes[1].transit_idle_duration == 0
    assert nodes[1].location_idle_duration == 0


def test_two_orders_all_visited_more_idles():
    nodes = [
        _create_node(15, Point(0, 0)),
        _create_node(55, Point(10, 0))
    ]
    idles = [
        _create_idle(0, 10, Point(10, 10)),
        _create_idle(20, 30, Point(0, 0)),
        _create_idle(40, 50, Point(5, 0)),
        _create_idle(70, 80, Point(10, 0)),
    ]

    compute_route_idles(nodes, idles)

    assert nodes[0].transit_idle_duration == 10
    assert nodes[0].location_idle_duration == 10

    assert nodes[1].transit_idle_duration == 10
    assert nodes[1].location_idle_duration == 10


def test_visited_orders_more_idles():
    nodes = [
        _create_node(50, Point(0, 0)),
        _create_node(100, Point(5, 5))
    ]
    idles = [
        # Idles NOT at order point
        _create_idle(0, 10, Point(10, 10)),
        _create_idle(15, 25, Point(10, 0)),
        _create_idle(30, 40, Point(0, 10)),

        # Idles at order point
        _create_idle(55, 65, Point(0, 0)),
        _create_idle(70, 80, Point(0, 0)),
        _create_idle(85, 95, Point(0, 0)),
    ]

    compute_route_idles(nodes, idles)

    assert nodes[0].transit_idle_duration == 30
    assert nodes[0].location_idle_duration == 30

    assert nodes[1].transit_idle_duration == 0
    assert nodes[1].location_idle_duration == 0


def test_unfinished_idle():
    nodes = [
        _create_node(15, Point(0, 0))
    ]
    idles = [
        _create_idle(0, 10, Point(10, 10)),
        _create_idle(20, None, Point(0, 0)),
    ]

    compute_route_idles(nodes, idles)

    assert nodes[0].transit_idle_duration == 10
    assert nodes[0].location_idle_duration == 0


def test_no_visit_radius():
    nodes = [
        _create_node(15, Point(0, 0), visit_radius=None)
    ]
    idles = [
        _create_idle(0, 10, Point(10, 10)),
        _create_idle(20, 30, Point(0, 0.000001)),  # Point is near the node but distance is not 0
    ]

    compute_route_idles(nodes, idles)

    assert nodes[0].transit_idle_duration == 10
    assert nodes[0].location_idle_duration == 0
