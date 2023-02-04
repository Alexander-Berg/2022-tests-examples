from datetime import timedelta

from ya_courier_backend.models import RouteEvent, RouteEventType

from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.route_entities import (
    RouteFactEvents,
    EventMetrics,
)
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.route_fact_events import calculate_fact_events

from .utils import datetime_after, timestamp_after, gen_fact_depot, gen_fact_order


def test_empty_metrics_from_empty_lists():
    assert calculate_fact_events([], []) == RouteFactEvents(
        order_idle=EventMetrics(timedelta(), 0),
        depot_idle=EventMetrics(timedelta(), 0),
        transit_idle=EventMetrics(timedelta(), 0),
        no_connection=EventMetrics(timedelta(), 0),
    )


def test_idle_metrics_are_divided_into_depots_and_orders():
    route_nodes = [
        gen_fact_depot(
            seq_pos=0,
            number='test_depot',
            arrival_time=datetime_after(11),
            departure_time=datetime_after(12),
        ),
        gen_fact_order(
            seq_pos=1,
            number='test_order',
            arrival_time=datetime_after(16),
            departure_time=datetime_after(17),
        ),
    ]
    route_events = [
        RouteEvent(type=RouteEventType.IDLE, lat=9, lon=9, start_timestamp=timestamp_after(8), finish_timestamp=timestamp_after(10)),
        RouteEvent(type=RouteEventType.IDLE, lat=12, lon=12, start_timestamp=timestamp_after(11, 10), finish_timestamp=timestamp_after(12)),
        RouteEvent(type=RouteEventType.IDLE, lat=14, lon=14, start_timestamp=timestamp_after(14), finish_timestamp=timestamp_after(15)),
        RouteEvent(type=RouteEventType.IDLE, lat=10, lon=10, start_timestamp=timestamp_after(16, 10), finish_timestamp=timestamp_after(17)),
    ]
    assert calculate_fact_events(route_nodes, route_events) == RouteFactEvents(
        order_idle=EventMetrics(timedelta(minutes=50), 1),
        depot_idle=EventMetrics(timedelta(minutes=50), 1),
        transit_idle=EventMetrics(timedelta(hours=3), 2),
        no_connection=EventMetrics(timedelta(), 0),
    )


def test_no_connection_metrics_is_independent_from_nodes():
    route_nodes = [
        gen_fact_depot(
            seq_pos=0,
            number='test_depot',
        ),
    ]
    route_events = [
        RouteEvent(type=RouteEventType.COURIER_CONNECTION_LOSS, start_timestamp=timestamp_after(11), finish_timestamp=timestamp_after(12)),
        RouteEvent(type=RouteEventType.COURIER_CONNECTION_LOSS, start_timestamp=timestamp_after(8), finish_timestamp=timestamp_after(10)),
    ]
    assert calculate_fact_events(route_nodes, route_events) == RouteFactEvents(
        order_idle=EventMetrics(timedelta(), 0),
        depot_idle=EventMetrics(timedelta(), 0),
        transit_idle=EventMetrics(timedelta(), 0),
        no_connection=EventMetrics(timedelta(hours=3), 2),
    )


def test_idle_events_after_last_visit():
    route_nodes = [
        gen_fact_depot(
            seq_pos=0,
            number='test_depot',
            arrival_time=datetime_after(11),
            departure_time=datetime_after(12),
        ),
        gen_fact_order(
            seq_pos=1,
            number='test_order',
            arrival_time=datetime_after(16),
            departure_time=datetime_after(17),
        ),
    ]
    route_events = [
        RouteEvent(type=RouteEventType.IDLE, lat=1, lon=1, start_timestamp=timestamp_after(18), finish_timestamp=timestamp_after(19)),
        RouteEvent(type=RouteEventType.IDLE, lat=2, lon=2, start_timestamp=timestamp_after(20), finish_timestamp=timestamp_after(21)),
    ]
    assert calculate_fact_events(route_nodes, route_events) == RouteFactEvents(
        order_idle=EventMetrics(timedelta(), 0),
        depot_idle=EventMetrics(timedelta(), 0),
        transit_idle=EventMetrics(timedelta(hours=2), 2),
        no_connection=EventMetrics(timedelta(), 0),
    )
