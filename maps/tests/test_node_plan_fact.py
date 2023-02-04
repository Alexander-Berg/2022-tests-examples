from datetime import timedelta

from ya_courier_backend.models import RoutePlanNode, PlanNodeType

from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.node_entities import (
    FactNode,
    NodePlanFact,
    PlanNode
)
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.node_plan_fact import build_plan_fact_nodes
from maps.b2bgeo.ya_courier.analytics_backend.lib.plan_fact.types import NodeType


def _mock_fact(type: NodeType, seq_pos: int) -> FactNode:
    return FactNode(
        id=0,
        type=type,
        route_sequence_pos=seq_pos,
        real_sequence_pos=seq_pos,
        number=None,
        status=None,
        lat=None,
        lon=None,
        address=None,
        point_radius=None,
        arrival_time=None,
        departure_time=None,
        service_duration=None,
        transit_duration=None,
        transit_distance_m=None,
        time_window=None,
        failed_time_window=None,
        location_idle_duration=None,
        transit_idle_duration=None,
        order_fact=None,
    )


def _mock_plan(type: PlanNodeType, seq_pos: int) -> RoutePlanNode:
    return RoutePlanNode(
        id=0,
        type=type,
        route_sequence_pos=seq_pos,
        arrival_time=None,
        time_windows=[]
    )


def _plan_res(type: NodeType, seq_pos: int) -> PlanNode:
    return PlanNode(
        id=0,
        type=type,
        route_sequence_pos=seq_pos,
        number=None,
        address=None,
        lat=None,
        lon=None,
        customer_name=None,
        arrival_time=None,
        departure_time=None,
        service_duration=None,
        shared_service_duration=None,
        transit_distance_m=None,
        volume_cbm=None,
        weight_kg=None,
        amount=None,
        used_time_window=None,
        failed_time_window=False,
        transit_duration=timedelta(),
    )


def test_node_plan_fact_is_empty_for_empty_input():
    assert build_plan_fact_nodes([], []) == []


def test_fact_is_unchanged_if_plan_is_empty():
    fact = [
        _mock_fact(NodeType.order, 0),
        _mock_fact(NodeType.order, 1),
    ]
    assert build_plan_fact_nodes([], fact) == [
        NodePlanFact(0, None, fact[0]),
        NodePlanFact(1, None, fact[1]),
    ]


def test_plan_is_unchanged_if_fact_is_empty():
    plan = [
        _mock_plan(PlanNodeType.order, 0),
        _mock_plan(PlanNodeType.order, 1),
    ]
    assert build_plan_fact_nodes(plan, []) == [
        NodePlanFact(0, _plan_res(NodeType.order, 0), None),
        NodePlanFact(1, _plan_res(NodeType.order, 1), None),
    ]


def test_fact_is_aligned_with_plan_orders():
    plan = [
        _mock_plan(PlanNodeType.garage, 0),
        _mock_plan(PlanNodeType.depot, 1),
        _mock_plan(PlanNodeType.order, 2),
    ]
    fact = [
        _mock_fact(NodeType.order, 0),
        _mock_fact(NodeType.order, 1),
    ]
    assert build_plan_fact_nodes(plan, fact) == [
        NodePlanFact(0, _plan_res(NodeType.garage, 0), None),
        NodePlanFact(1, _plan_res(NodeType.depot, 1), None),
        NodePlanFact(2, _plan_res(NodeType.order, 2), fact[0]),
        NodePlanFact(3, None, fact[1]),
    ]


def test_plan_is_aligned_with_fact_orders():
    plan = [
        _mock_plan(PlanNodeType.depot, 0),
        _mock_plan(PlanNodeType.order, 1),
    ]
    fact = [
        _mock_fact(NodeType.garage, 0),
        _mock_fact(NodeType.depot, 1),
        _mock_fact(NodeType.order, 2),
    ]
    assert build_plan_fact_nodes(plan, fact) == [
        NodePlanFact(0, None, fact[0]),
        NodePlanFact(1, _plan_res(NodeType.depot, 0), fact[1]),
        NodePlanFact(2, _plan_res(NodeType.order, 1), fact[2]),
    ]


def test_fact_is_not_aligned_when_no_orders_are_present():
    plan = [
        _mock_plan(PlanNodeType.garage, 0),
        _mock_plan(PlanNodeType.depot, 1),
    ]
    fact = [
        _mock_fact(NodeType.order, 0),
    ]
    assert build_plan_fact_nodes(plan, fact) == [
        NodePlanFact(0, _plan_res(NodeType.garage, 0), fact[0]),
        NodePlanFact(1, _plan_res(NodeType.depot, 1), None),
    ]
