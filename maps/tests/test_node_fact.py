from datetime import datetime, timedelta, timezone

from ya_courier_backend.models import (
    CourierPosition,
    DepotInstanceStatus,
    GarageStatus,
    LogisticCompany,
    OrderStatus,
    Route,
)

from maps.b2bgeo.ya_courier.analytics_backend.lib.plan_fact.types import NodeType
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.node_entities import (
    FactNode,
    TimeWindow,
    OrderSpecificFact,
)
from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.node_fact import build_fact_nodes

from .utils import datetime_after, gen_fact_garage, gen_fact_depot, gen_fact_order, gen_fact_time_window, ROUTE_DATE


def test_fact_nodes_does_nothing_with_empty_array():
    assert build_fact_nodes(None, None, None) == []
    assert build_fact_nodes(Route(route_nodes=[]), None, None) == []


def test_fact_nodes_are_sorted_by_arrival_time():
    company = LogisticCompany(mark_delivered_radius=123)
    route = Route(
        date=ROUTE_DATE,
        route_nodes=[
            gen_fact_order(
                seq_pos=0,
                number='first',
                arrival_time=datetime_after(11),
                departure_time=datetime_after(12),
                segment_distance_m=1,
                status=OrderStatus.finished,
                status_comment='test_comment2',
                time_windows=[gen_fact_time_window(start_hour=12)],
                amount=1,
            ),
            gen_fact_order(
                seq_pos=1,
                number='second',
                arrival_time=datetime_after(16),
                departure_time=datetime_after(17),
                segment_distance_m=30,
                status=OrderStatus.finished,
                time_windows=[gen_fact_time_window(start_hour=12)],
                weight=2,
            ),
            gen_fact_order(
                seq_pos=2,
                number='third',
                arrival_time=datetime_after(14),
                departure_time=datetime_after(15),
                segment_distance_m=200,
                status=OrderStatus.finished,
                time_windows=[gen_fact_time_window(start_hour=14)],
                volume=3,
            ),
        ],
    )
    actual = build_fact_nodes(route, company, None)
    expected = [
        FactNode(
            id=0,
            type=NodeType.order,
            route_sequence_pos=0,
            real_sequence_pos=0,
            number='first',
            status=OrderStatus.finished.value,
            lat=10,
            lon=10,
            address='test_address',
            point_radius=123,
            arrival_time=datetime_after(11),
            departure_time=datetime_after(12),
            service_duration=timedelta(hours=1),
            transit_duration=timedelta(),
            transit_distance_m=1,
            time_window=TimeWindow(start=datetime_after(12), end=datetime_after(13)),
            failed_time_window=True,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(),
            order_fact=OrderSpecificFact(
                customer_name='test_customer',
                volume_cbm=None,
                weight_kg=None,
                amount=1,
                refined_lat=9,
                refined_lon=9,
                comments='test_comment',
                status_comments=[{'id': 2, 'status': OrderStatus.finished.value, 'comment': 'test_comment2'}],
            ),
        ),
        FactNode(
            id=0,
            type=NodeType.order,
            route_sequence_pos=2,
            real_sequence_pos=1,
            number='third',
            status=OrderStatus.finished.value,
            lat=10,
            lon=10,
            address='test_address',
            point_radius=123,
            arrival_time=datetime_after(14),
            departure_time=datetime_after(15),
            service_duration=timedelta(hours=1),
            transit_duration=timedelta(hours=2),
            transit_distance_m=200,
            time_window=TimeWindow(start=datetime_after(14), end=datetime_after(15)),
            failed_time_window=False,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(),
            order_fact=OrderSpecificFact(
                customer_name='test_customer',
                volume_cbm=3,
                weight_kg=None,
                amount=None,
                refined_lat=9,
                refined_lon=9,
                comments='test_comment',
                status_comments=[{'id': 2, 'status': OrderStatus.finished.value}],
            ),
        ),
        FactNode(
            id=0,
            type=NodeType.order,
            route_sequence_pos=1,
            real_sequence_pos=2,
            number='second',
            status=OrderStatus.finished.value,
            lat=10,
            lon=10,
            address='test_address',
            point_radius=123,
            arrival_time=datetime_after(16),
            departure_time=datetime_after(17),
            service_duration=timedelta(hours=1),
            transit_duration=timedelta(hours=1),
            transit_distance_m=30,
            time_window=TimeWindow(start=datetime_after(12), end=datetime_after(13)),
            failed_time_window=True,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(),
            order_fact=OrderSpecificFact(
                customer_name='test_customer',
                volume_cbm=None,
                weight_kg=2,
                amount=None,
                refined_lat=9,
                refined_lon=9,
                comments='test_comment',
                status_comments=[{'id': 2, 'status': OrderStatus.finished.value}],
            ),
        ),
    ]
    assert actual == expected


def test_route_with_last_depot_visited_first_and_first_depot_fake_visited():
    fake_time = datetime.fromtimestamp(0, timezone.utc)
    company = LogisticCompany(mark_delivered_radius=123)
    route = Route(
        date=ROUTE_DATE,
        route_nodes=[
            gen_fact_depot(
                seq_pos=0,
                number='first_depot',
                arrival_time=fake_time,
                departure_time=fake_time,
                time_interval='10:00-12:00',
            ),
            gen_fact_order(
                seq_pos=1,
                number='middle_order',
                arrival_time=datetime_after(12),
                departure_time=datetime_after(13),
                segment_distance_m=1,
                status=OrderStatus.finished,
                status_comment='test_comment2',
                time_windows=[gen_fact_time_window(start_hour=12)],
                amount=1,
                transit_idle_duration=timedelta(minutes=14)
            ),
            gen_fact_depot(
                seq_pos=2,
                number='last_depot',
                arrival_time=datetime_after(10),
                departure_time=datetime_after(11),
                segment_distance_m=12,
            ),
        ],
    )
    actual = build_fact_nodes(route, company, None)
    expected = [
        FactNode(
            id=0,
            type=NodeType.depot,
            route_sequence_pos=0,
            real_sequence_pos=0,
            number='first_depot',
            status=DepotInstanceStatus.visited.value,
            lat=12,
            lon=12,
            address='depot_address',
            point_radius=321,
            arrival_time=None,
            departure_time=None,
            service_duration=timedelta(),
            transit_duration=timedelta(),
            transit_distance_m=0,
            time_window=TimeWindow(start=datetime_after(10), end=datetime_after(12)),
            failed_time_window=False,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(),
            order_fact=None,
        ),
        FactNode(
            id=0,
            type=NodeType.depot,
            route_sequence_pos=2,
            real_sequence_pos=1,
            number='last_depot',
            status=DepotInstanceStatus.visited.value,
            lat=12,
            lon=12,
            address='depot_address',
            point_radius=321,
            arrival_time=datetime_after(10),
            departure_time=datetime_after(11),
            service_duration=timedelta(hours=1),
            transit_duration=timedelta(),
            transit_distance_m=12,
            time_window=None,
            failed_time_window=False,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(),
            order_fact=None,
        ),
        FactNode(
            id=0,
            type=NodeType.order,
            route_sequence_pos=1,
            real_sequence_pos=2,
            number='middle_order',
            status=OrderStatus.finished.value,
            lat=10,
            lon=10,
            address='test_address',
            point_radius=123,
            arrival_time=datetime_after(12),
            departure_time=datetime_after(13),
            service_duration=timedelta(hours=1),
            transit_duration=timedelta(hours=1),
            transit_distance_m=1,
            time_window=TimeWindow(start=datetime_after(12), end=datetime_after(13)),
            failed_time_window=False,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(minutes=14),
            order_fact=OrderSpecificFact(
                customer_name='test_customer',
                volume_cbm=None,
                weight_kg=None,
                amount=1,
                refined_lat=9,
                refined_lon=9,
                comments='test_comment',
                status_comments=[{'id': 2, 'status': OrderStatus.finished.value, 'comment': 'test_comment2'}],
            ),
        ),
    ]
    assert actual == expected


def test_visiting_depot_on_another_day_results_in_another_time_window():
    company = LogisticCompany(mark_delivered_radius=123)
    route = Route(
        date=ROUTE_DATE,
        route_nodes=[
            gen_fact_depot(
                seq_pos=0,
                number='first_depot',
                arrival_time=datetime_after(hours=34),
                departure_time=datetime_after(hours=35),
                time_interval='10:00-12:00',
            )
        ],
    )
    actual = build_fact_nodes(route, company, None)
    expected = [
        FactNode(
            id=0,
            type=NodeType.depot,
            route_sequence_pos=0,
            real_sequence_pos=0,
            number='first_depot',
            status=DepotInstanceStatus.visited.value,
            lat=12,
            lon=12,
            address='depot_address',
            point_radius=321,
            arrival_time=datetime_after(hours=34),
            departure_time=datetime_after(hours=35),
            service_duration=timedelta(hours=1),
            transit_duration=timedelta(),
            transit_distance_m=0,
            time_window=TimeWindow(start=datetime_after(34), end=datetime_after(36)),
            failed_time_window=False,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(),
            order_fact=None,
        ),
    ]
    assert actual == expected


def test_first_position_is_considered_for_transit_duration_calculation():
    company = LogisticCompany(mark_delivered_radius=123)
    route = Route(
        date=ROUTE_DATE,
        route_nodes=[
            gen_fact_depot(
                seq_pos=0,
                number='first_depot',
                segment_distance_m=121,
                arrival_time=datetime_after(hours=2),
                departure_time=datetime_after(hours=3),
                time_interval='10:00-12:00',
                location_idle_duration=timedelta(minutes=10),
            )
        ],
    )
    first_position = CourierPosition(lat=9, lon=9, time=datetime_after(1).timestamp())
    actual = build_fact_nodes(route, company, first_position)
    expected = [
        FactNode(
            id=0,
            type=NodeType.depot,
            route_sequence_pos=0,
            real_sequence_pos=0,
            number='first_depot',
            status=DepotInstanceStatus.visited.value,
            lat=12,
            lon=12,
            address='depot_address',
            point_radius=321,
            arrival_time=datetime_after(hours=2),
            departure_time=datetime_after(hours=3),
            service_duration=timedelta(hours=1),
            transit_duration=timedelta(hours=1),
            transit_distance_m=121,
            time_window=TimeWindow(start=datetime_after(10), end=datetime_after(12)),
            failed_time_window=True,
            location_idle_duration=timedelta(minutes=10),
            transit_idle_duration=timedelta(),
            order_fact=None,
        ),
    ]
    assert actual == expected


def test_first_garage_is_not_sorted_because_courier_could_have_missed_it():
    company = LogisticCompany(mark_delivered_radius=123)
    route = Route(
        date=ROUTE_DATE,
        route_nodes=[
            gen_fact_garage(seq_pos=0, number='first_garage'),
            gen_fact_depot(
                seq_pos=1,
                number='first_depot',
                segment_distance_m=121,
                arrival_time=datetime_after(hours=2),
                departure_time=datetime_after(hours=3),
                time_interval='10:00-12:00',
            )
        ],
    )
    first_position = CourierPosition(lat=9, lon=9, time=datetime_after(1).timestamp())
    actual = build_fact_nodes(route, company, first_position)
    expected = [
        FactNode(
            id=0,
            type=NodeType.garage,
            route_sequence_pos=0,
            real_sequence_pos=0,
            number='first_garage',
            status=GarageStatus.unvisited.value,
            lat=11,
            lon=11,
            address='garage_address',
            point_radius=100,
            arrival_time=None,
            departure_time=None,
            service_duration=timedelta(),
            transit_duration=timedelta(),
            transit_distance_m=0,
            time_window=None,
            failed_time_window=False,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(),
            order_fact=None,
        ),
        FactNode(
            id=0,
            type=NodeType.depot,
            route_sequence_pos=1,
            real_sequence_pos=1,
            number='first_depot',
            status=DepotInstanceStatus.visited.value,
            lat=12,
            lon=12,
            address='depot_address',
            point_radius=321,
            arrival_time=datetime_after(hours=2),
            departure_time=datetime_after(hours=3),
            service_duration=timedelta(hours=1),
            transit_duration=timedelta(hours=1),
            transit_distance_m=121,
            time_window=TimeWindow(start=datetime_after(10), end=datetime_after(12)),
            failed_time_window=True,
            location_idle_duration=timedelta(),
            transit_idle_duration=timedelta(),
            order_fact=None,
        ),
    ]
    assert actual == expected
