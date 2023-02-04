from datetime import timedelta

from ya_courier_backend.models import CourierPosition, Route, LogisticCompany, OrderStatus

from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.route_entities import RouteFactMetrics
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.route_fact_metrics import (
    calculate_route_fact_metrics,
)
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.node_fact import build_fact_nodes

from .utils import datetime_after, gen_fact_order, gen_fact_time_window, ROUTE_DATE


def test_fact_contains_zeros_for_empty_nodes():
    assert calculate_route_fact_metrics([], []) == RouteFactMetrics(
        failed_time_window_count=0,
        nodes_count=0,
        orders_count=0,
        orders_with_status_comments_count=0,
        delivered_orders_count=0,
        processed_orders_count=0,
        service_duration_orders=timedelta(),
        service_duration_depots=timedelta(),
        transit_distance_m=0,
        transit_duration=timedelta(),
        total_duration=timedelta(),
        start_time=None,
        end_time=None,
    )


def test_simple_fact_metrics_calculation():
    company = LogisticCompany(mark_delivered_radius=100)
    route = Route(
        date=ROUTE_DATE,
        route_nodes=[
            gen_fact_order(
                seq_pos=0,
                arrival_time=datetime_after(11),  # before time window
                departure_time=datetime_after(12),
                segment_distance_m=1,
                status=OrderStatus.finished,
                time_windows=[gen_fact_time_window(start_hour=12)],
            ),
            gen_fact_order(
                seq_pos=1,
                arrival_time=datetime_after(16),  # after time window
                departure_time=datetime_after(17),
                segment_distance_m=30,
                status=OrderStatus.finished,
                time_windows=[gen_fact_time_window(start_hour=12)],
            ),
            gen_fact_order(
                seq_pos=2,
                arrival_time=datetime_after(14),
                departure_time=datetime_after(15),
                segment_distance_m=200,
                status=OrderStatus.finished,
                time_windows=[gen_fact_time_window(start_hour=14)],
            ),
        ],
    )
    nodes = build_fact_nodes(route, company, None)

    assert calculate_route_fact_metrics(nodes, []) == RouteFactMetrics(
        failed_time_window_count=2,
        nodes_count=3,
        orders_count=3,
        orders_with_status_comments_count=0,
        delivered_orders_count=3,
        processed_orders_count=3,
        service_duration_orders=timedelta(hours=3),
        service_duration_depots=timedelta(),
        transit_distance_m=231,
        transit_duration=timedelta(hours=3),
        total_duration=timedelta(hours=6),
        start_time=datetime_after(11),
        end_time=datetime_after(17),
    )


def test_service_duration_does_not_overlap():
    company = LogisticCompany(mark_delivered_radius=100)
    route = Route(
        date=ROUTE_DATE,
        route_nodes=[
            gen_fact_order(
                seq_pos=0,
                arrival_time=datetime_after(11),
                departure_time=datetime_after(12),
                segment_distance_m=1,
                status=OrderStatus.finished,
                time_windows=[gen_fact_time_window(start_hour=11)],
            ),
            gen_fact_order(
                seq_pos=1,
                arrival_time=datetime_after(11),
                departure_time=datetime_after(13),
                segment_distance_m=30,
                status=OrderStatus.finished,
                time_windows=[gen_fact_time_window(start_hour=11)],
            ),
        ],
    )
    nodes = build_fact_nodes(route, company, None)

    assert calculate_route_fact_metrics(nodes, []) == RouteFactMetrics(
        failed_time_window_count=0,
        nodes_count=2,
        orders_count=2,
        orders_with_status_comments_count=0,
        delivered_orders_count=2,
        processed_orders_count=2,
        service_duration_orders=timedelta(hours=2),
        service_duration_depots=timedelta(),
        transit_distance_m=31,
        transit_duration=timedelta(),
        total_duration=timedelta(hours=2),
        start_time=datetime_after(11),
        end_time=datetime_after(13),
    )


def test_tail_positions_do_not_overlap_with_positions():
    company = LogisticCompany(mark_delivered_radius=100)
    route = Route(
        date=ROUTE_DATE,
        route_nodes=[
            gen_fact_order(
                seq_pos=0,
                arrival_time=datetime_after(11),
                departure_time=datetime_after(12),
                segment_distance_m=300,
                status=OrderStatus.finished,
                time_windows=[gen_fact_time_window(start_hour=11)],
            ),
        ],
    )
    positions = [
        CourierPosition(lat=9, lon=9, time=datetime_after(hours=10).timestamp()),
        CourierPosition(lat=10, lon=10, time=datetime_after(hours=12).timestamp()),
        CourierPosition(lat=10, lon=11.004515, time=datetime_after(hours=13).timestamp()),
    ]
    nodes = build_fact_nodes(route, company, positions[0])
    actual = calculate_route_fact_metrics(nodes, positions)
    assert actual == RouteFactMetrics(
        failed_time_window_count=0,
        nodes_count=1,
        orders_count=1,
        orders_with_status_comments_count=0,
        delivered_orders_count=1,
        processed_orders_count=1,
        service_duration_orders=timedelta(hours=1),
        service_duration_depots=timedelta(),
        transit_distance_m=110300.0012741555,
        transit_duration=timedelta(hours=2),
        total_duration=timedelta(hours=3),
        start_time=datetime_after(10),
        end_time=datetime_after(13),
    )
