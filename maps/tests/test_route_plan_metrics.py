from datetime import timedelta

from ya_courier_backend.models import PlanNodeType

from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.route_plan_metrics import (
    calculate_route_plan_metrics,
)
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.route_entities import RoutePlanMetrics

from .utils import datetime_after, gen_plan_node, gen_plan_time_window


def test_plan_contains_zeros_for_empty_nodes():
    assert calculate_route_plan_metrics([]) == RoutePlanMetrics(
        transit_distance_m=0,
        transit_duration=timedelta(),
        service_duration=timedelta(),
        total_duration=timedelta(),
        failed_time_window_count=0,
        nodes_count=0,
        orders_count=0,
        start_time=None,
        end_time=None,
    )


def test_failed_time_window_is_counted():
    nodes = [
        gen_plan_node(
            seq_pos=0,
            type=PlanNodeType.garage,
            shared_service_duration=timedelta(seconds=0),
            arrival_time=datetime_after(12),
            transit_distance_m=0,
            time_windows=[gen_plan_time_window(start_hour=12)],
        ),
        gen_plan_node(
            seq_pos=1,
            type=PlanNodeType.order,
            shared_service_duration=timedelta(seconds=10),
            arrival_time=datetime_after(14),
            transit_distance_m=101,
            time_windows=[gen_plan_time_window(start_hour=12)],
        ),
    ]

    assert calculate_route_plan_metrics(nodes) == RoutePlanMetrics(
        transit_distance_m=101,
        transit_duration=timedelta(hours=1, minutes=50),
        service_duration=timedelta(seconds=210),
        total_duration=timedelta(hours=2, minutes=10),
        failed_time_window_count=1,
        nodes_count=2,
        orders_count=1,
        start_time=datetime_after(12),
        end_time=datetime_after(14, 10),
    )


def test_multi_order_shared_service_counted_once():
    nodes = [
        gen_plan_node(
            seq_pos=0,
            type=PlanNodeType.depot,
            shared_service_duration=timedelta(seconds=0),
            multi_order=False,
            arrival_time=datetime_after(11),
            transit_distance_m=0,
            time_windows=[gen_plan_time_window(start_hour=11)],
        ),
        gen_plan_node(
            seq_pos=0,
            type=PlanNodeType.order,
            shared_service_duration=timedelta(seconds=20),
            multi_order=False,
            arrival_time=datetime_after(12),
            transit_distance_m=102,
            time_windows=[gen_plan_time_window(start_hour=12)],
        ),
        gen_plan_node(
            seq_pos=1,
            type=PlanNodeType.order,
            shared_service_duration=timedelta(seconds=10),
            multi_order=True,
            arrival_time=datetime_after(12),
            transit_distance_m=0,
            time_windows=[gen_plan_time_window(start_hour=12)],
        ),
    ]

    assert calculate_route_plan_metrics(nodes) == RoutePlanMetrics(
        transit_distance_m=102,
        transit_duration=timedelta(minutes=50),
        service_duration=timedelta(seconds=320),
        total_duration=timedelta(hours=1, minutes=10),
        failed_time_window_count=0,
        nodes_count=3,
        orders_count=2,
        start_time=datetime_after(11),
        end_time=datetime_after(12, 10),
    )
