from copy import deepcopy
from datetime import datetime, date, timedelta
from typing import Optional

from ya_courier_backend.models import (
    Courier,
    CourierPosition,
    Depot,
    LogisticCompany,
    OrderStatus,
    Route,
    RouteEvent,
    RoutePlan,
    PlanNodeType,
)

from maps.b2bgeo.ya_courier.backend.tools.export_analytics.lib.plan_fact.storages import CourierStorageInterface

from ..utils import (
    datetime_after,
    timestamp_after,
    gen_fact_order,
    gen_fact_time_window,
    gen_fact_garage,
    gen_plan_node,
    gen_plan_time_window,
    ROUTE_DATE,
)

COMPANIES=[
    LogisticCompany(id=1, mark_delivered_radius=300),
    LogisticCompany(id=2, mark_delivered_radius=200),
]

DEPOTS=[
    Depot(id=1, company_id=1, number='test_depot_12', name='depot_name'),
    Depot(id=3, company_id=2, number='test_depot_1'),
]

COURIERS=[
    Courier(id=1, company_id=1, number='test_courier_12', phone='courier_phone'),
    Courier(id=2, company_id=2, number='test_courier_2'),
]

ROUTES = [
    Route(
        id=124,
        company_id=2,
        depot_id=3,
        courier_id=2,
        date=ROUTE_DATE + timedelta(days=1),
        number='test_route',
        courier_violated_route=False,
        route_nodes=[
            gen_fact_order(
                seq_pos=0,
                number='test_order',
                status=OrderStatus.confirmed,
                time_windows=[gen_fact_time_window(start_hour=24 + 12)],
            ),
        ],
    ),
    Route(
        id=121,
        company_id=1,
        depot_id=1,
        courier_id=1,
        date=ROUTE_DATE,
        number='test_route',
        courier_violated_route=False,
        route_nodes=[
            gen_fact_garage(
                seq_pos=0,
                number='test_garage',
                visit_time=datetime_after(9),
                segment_distance_m=10,
            )
        ],
    ),
]

ROUTE_PLANS = [
    RoutePlan(
        route_id=121,
        nodes=[
            gen_plan_node(
                seq_pos=0,
                type=PlanNodeType.garage,
                arrival_time=datetime_after(9),
                time_windows=[gen_plan_time_window(start_hour=9)],
            )
        ]
    ),
]


POSITIONS = [
    CourierPosition(
        route_id=121,
        time=timestamp_after(8, 50),
        lat=8,
        lon=8,
    )
]


def _filter_by_id(objects: list, ids: list[int]) -> list:
    return [obj for obj in objects if obj.id in ids]


def _filter_by_route_id(objects: list, ids: list[int]) -> list:
    return [obj for obj in objects if obj.route_id in ids]


class MockCourierStorage(CourierStorageInterface):
    def __init__(self):
        self.companies = deepcopy(COMPANIES)
        self.depots = deepcopy(DEPOTS)
        self.couriers = deepcopy(COURIERS)
        self.routes = deepcopy(ROUTES)
        self.route_plans = deepcopy(ROUTE_PLANS)
        self.positions = deepcopy(POSITIONS)

    def load_routes(self, from_date: date, to_date: date) -> list[Route]:
        return [route for route in self.routes if route.date >= from_date and route.date <= to_date]

    def load_plans(self, route_ids: list[int]) -> list[RoutePlan]:
        return _filter_by_route_id(self.route_plans, route_ids)

    def load_depots(self, ids: list[int]) -> list[Depot]:
        return _filter_by_id(self.depots, ids)

    def load_couriers(self, ids: list[int]) -> list[Courier]:
        return _filter_by_id(self.couriers, ids)

    def load_companies(self, ids: list[int]) -> list[LogisticCompany]:
        return _filter_by_id(self.companies, ids)

    def load_first_positions(self, routes: list[Route]) -> list[CourierPosition]:
        res = []
        for route in routes:
            positions = _filter_by_route_id(self.positions, [route.id])
            if positions:
                res.append(positions[0])
        return res

    def load_positions_tail(self, route: Route, from_dt: Optional[datetime]) -> list[CourierPosition]:
        return []

    def load_route_events(self, route: Route) -> list[RouteEvent]:
        return []
