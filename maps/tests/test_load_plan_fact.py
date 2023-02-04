from datetime import timedelta

from maps.b2bgeo.ya_courier.analytics_backend.lib.plan_fact.types import NodeType
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.load import load_plan_fact
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.node_entities import (
    NodePlanFact,
    PlanNode,
    FactNode,
    TimeWindow,
    OrderSpecificFact,
)
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.route_entities import (
    EventMetrics,
    RouteFactEvents,
    RouteFactMetrics,
    RouteInfo,
    RoutePlanFact,
    RoutePlanMetrics,
)

from .mocks.courier_storage import MockCourierStorage
from .utils import ROUTE_DATE, datetime_after


def test_empty_storage_results_in_empty_plan_fact():
    courier_storage = MockCourierStorage()
    assert load_plan_fact(courier_storage, ROUTE_DATE + timedelta(days=2), ROUTE_DATE + timedelta(days=2)) == []


def test_no_plan_no_positions_results_in_just_basic_metrics():
    courier_storage = MockCourierStorage()
    assert load_plan_fact(courier_storage, ROUTE_DATE + timedelta(days=1), ROUTE_DATE + timedelta(days=1)) == [
        RoutePlanFact(
            route_info=RouteInfo(
                id=124,
                company_id=2,
                number='test_route',
                imei=None,
                date=ROUTE_DATE + timedelta(days=1),
                depot_id=3,
                depot_number='test_depot_1',
                depot_name=None,
                depot_address=None,
                depot_timezone=None,
                courier_id=2,
                courier_number='test_courier_2',
                courier_name=None,
                courier_phone=None,
            ),
            plan=RoutePlanMetrics(
                transit_distance_m=0,
                transit_duration=timedelta(),
                total_duration=timedelta(),
                service_duration=timedelta(),
                failed_time_window_count=0,
                nodes_count=0,
                orders_count=0,
                start_time=None,
                end_time=None,
            ),
            fact=RouteFactMetrics(
                failed_time_window_count=0,
                nodes_count=1,
                orders_count=1,
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
            ),
            fact_events=RouteFactEvents(
                EventMetrics(timedelta(), 0),
                EventMetrics(timedelta(), 0),
                EventMetrics(timedelta(), 0),
                EventMetrics(timedelta(), 0),
            ),
            has_positions=False,
            plan_violated=False,
            courier_violated_route=False,
            nodes=[
                NodePlanFact(
                    0,
                    plan=None,
                    fact=FactNode(
                        id=0,
                        type=NodeType.order,
                        route_sequence_pos=0,
                        real_sequence_pos=0,
                        number='test_order',
                        status='confirmed',
                        lat=10,
                        lon=10,
                        address='test_address',
                        point_radius=200,
                        arrival_time=None,
                        departure_time=None,
                        service_duration=timedelta(),
                        transit_duration=timedelta(),
                        transit_distance_m=0,
                        time_window=TimeWindow(
                            start=datetime_after(hours=24 + 12), end=datetime_after(hours=24 + 13)
                        ),
                        failed_time_window=False,
                        location_idle_duration=timedelta(),
                        transit_idle_duration=timedelta(),
                        order_fact=OrderSpecificFact(
                            customer_name='test_customer',
                            volume_cbm=None,
                            weight_kg=None,
                            amount=None,
                            refined_lat=9,
                            refined_lon=9,
                            comments='test_comment',
                            status_comments=[],
                        ),
                    ),
                ),
            ],
        )
    ]


def test_route_with_plan_and_fact():
    courier_storage = MockCourierStorage()
    assert load_plan_fact(courier_storage, ROUTE_DATE, ROUTE_DATE) == [
        RoutePlanFact(
            route_info=RouteInfo(
                id=121,
                company_id=1,
                number='test_route',
                imei=None,
                date=ROUTE_DATE,
                depot_id=1,
                depot_number='test_depot_12',
                depot_name='depot_name',
                depot_address=None,
                depot_timezone=None,
                courier_id=1,
                courier_number='test_courier_12',
                courier_name=None,
                courier_phone='courier_phone',
            ),
            plan=RoutePlanMetrics(
                transit_distance_m=0,
                transit_duration=timedelta(),
                total_duration=timedelta(minutes=10),
                service_duration=timedelta(seconds=100),
                failed_time_window_count=0,
                nodes_count=1,
                orders_count=0,
                start_time=datetime_after(9),
                end_time=datetime_after(9, 10),
            ),
            fact=RouteFactMetrics(
                failed_time_window_count=0,
                nodes_count=1,
                orders_count=0,
                orders_with_status_comments_count=0,
                delivered_orders_count=0,
                processed_orders_count=0,
                service_duration_orders=timedelta(),
                service_duration_depots=timedelta(),
                transit_distance_m=10,
                transit_duration=timedelta(minutes=10),
                total_duration=timedelta(minutes=10),
                start_time=datetime_after(8, 50),
                end_time=datetime_after(9),
            ),
            fact_events=RouteFactEvents(
                EventMetrics(timedelta(), 0),
                EventMetrics(timedelta(), 0),
                EventMetrics(timedelta(), 0),
                EventMetrics(timedelta(), 0),
            ),
            has_positions=True,
            plan_violated=True,
            courier_violated_route=False,
            nodes=[
                NodePlanFact(
                    0,
                    plan=PlanNode(
                        id=0,
                        type=NodeType.garage,
                        route_sequence_pos=0,
                        number='dummy_number',
                        address='dummy_address',
                        lat=56.,
                        lon=37.,
                        customer_name='dummy_customer_name',
                        arrival_time=datetime_after(9),
                        departure_time=datetime_after(9, minutes=10),
                        service_duration=timedelta(seconds=100),
                        shared_service_duration=timedelta(),
                        transit_distance_m=0,
                        volume_cbm=None,
                        weight_kg=None,
                        amount=None,
                        used_time_window=TimeWindow(start=datetime_after(9), end=datetime_after(10)),
                        failed_time_window=False,
                        transit_duration=timedelta(),
                    ),
                    fact=FactNode(
                        id=0,
                        type=NodeType.garage,
                        route_sequence_pos=0,
                        real_sequence_pos=0,
                        number='test_garage',
                        status='visited',
                        lat=11,
                        lon=11,
                        address='garage_address',
                        point_radius=100,
                        arrival_time=datetime_after(9),
                        departure_time=datetime_after(9),
                        service_duration=timedelta(),
                        transit_duration=timedelta(minutes=10),
                        transit_distance_m=10,
                        time_window=None,
                        failed_time_window=False,
                        location_idle_duration=timedelta(),
                        transit_idle_duration=timedelta(),
                        order_fact=None,
                    ),
                ),
            ],
        )
    ]
