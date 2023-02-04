from flask import g
from flask_sqlalchemy import get_debug_queries
from http import HTTPStatus
import dateutil.tz
import pytest

from copy import deepcopy
from datetime import date, datetime, timedelta
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_delete, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage, add_user, add_user_depot, add_user_shared_company

from ya_courier_backend.logic.route_plan import get_route_plan_info
from ya_courier_backend.models import db, RoutePlan, RoutePlanNode, PlanTimeWindow, PlanNodeType, UserRole
from ya_courier_backend.util.oauth import UserAccount


def _route_datetime(hour, minute, seconds=0):
    return datetime(2019, 12, 13, hour, minute, seconds, tzinfo=dateutil.tz.gettz("Europe/Moscow"))


DEPOT_DATA = {
    "type": PlanNodeType.depot,
    "number": "0",
    "address": "test-street, 101",
    "lat": 55.799087,
    "lon": 37.729377,
    "customer_name": None,
    "multi_order": False,
    "arrival_time": _route_datetime(7, 0),
    "departure_time": _route_datetime(7, 3, 20),
    "service_duration": timedelta(seconds=200),
    "shared_service_duration": timedelta(seconds=0),
    "transit_distance_m": 0,
    "volume_cbm": None,
    "weight_kg": None,
    "amount": None,
}

GARAGE_DATA = {
    "type": PlanNodeType.garage,
    "number": "last-garage",
    "address": "test-address",
    "lat": 55.664695,
    "lon": 37.562443,
    "customer_name": None,
    "multi_order": False,
    "arrival_time": _route_datetime(15, 58, 46),
    "departure_time": _route_datetime(16, 40),
    "service_duration": timedelta(seconds=0),
    "shared_service_duration": timedelta(seconds=0),
    "transit_distance_m": 24001,
    "volume_cbm": None,
    "weight_kg": None,
    "amount": None,
}

FIRST_ORDER_DATA = {
    "type": PlanNodeType.order,
    "number": "126",
    "address": "221B Baker Street",
    "lat": 55.826326,
    "lon": 37.637686,
    "customer_name": "Sherlock",
    "multi_order": False,
    "arrival_time": _route_datetime(7, 26, 27),
    "departure_time": _route_datetime(7, 26, 40),
    "service_duration": timedelta(seconds=0),
    "shared_service_duration": timedelta(seconds=0),
    "transit_distance_m": 8723,
    "volume_cbm": None,
    "weight_kg": 1.0,
    "amount": None,
}

SECOND_ORDER_DATA = {
    "type": PlanNodeType.order,
    "number": "78",
    "address": "1 Baker Street",
    "lat": 55.8185462,
    "lon": 37.66126693,
    "customer_name": None,
    "multi_order": False,
    "arrival_time": _route_datetime(9, 0),
    "departure_time": _route_datetime(9, 1, 40),
    "service_duration": timedelta(seconds=0),
    "shared_service_duration": timedelta(seconds=0),
    "transit_distance_m": 9000,
    "volume_cbm": 0.004,
    "weight_kg": 1.0,
    "amount": 0.0,
}

THIRD_ORDER_DATA = {
    "type": PlanNodeType.order,
    "number": "115",
    "address": None,
    "lat": 55.8185462,
    "lon": 37.66126693,
    "customer_name": None,
    "multi_order": False,
    "arrival_time": _route_datetime(7, 17, 44),
    "departure_time": _route_datetime(7, 21, 40),
    "service_duration": timedelta(seconds=0),
    "shared_service_duration": timedelta(seconds=0),
    "transit_distance_m": 6832,
    "volume_cbm": 0.004,
    "weight_kg": 1.0,
    "amount": 0.0,
}


def _change_date(dt, route_date):
    return datetime.combine(route_date, dt.time(), tzinfo=dateutil.tz.gettz("Europe/Moscow"))


def _node_with_date(node, route_date):
    node = deepcopy(node)
    node["arrival_time"] = _change_date(node["arrival_time"], route_date)
    node["departure_time"] = _change_date(node["departure_time"], route_date)
    return node


def _load_route_plans():
    return list(map(RoutePlan.as_dict, db.session.query(RoutePlan).all()))


def _load_route_plan_nodes():
    return list(map(RoutePlanNode.as_dict, db.session.query(RoutePlanNode).all()))


def _load_plan_time_windows():
    return list(map(PlanTimeWindow.as_dict, db.session.query(PlanTimeWindow).all()))


@skip_if_remote
@pytest.mark.parametrize("import_depot_garage", [True, False])
def test_plan_with_depots_and_garage_is_created_after_import(env: Environment, import_depot_garage):
    set_company_import_depot_garage(env, env.default_company.id, import_depot_garage)

    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert _load_route_plans() == [
            {
                "id": 1,
                "route_id": route["id"],
                "orders_count": 2,
                "total_transit_distance_m": 41724,
                "total_duration": timedelta(seconds=34800),
            }
        ]

        assert _load_route_plan_nodes() == [
            {"id": 1, "route_plan_id": 1, "route_sequence_pos": 0, **DEPOT_DATA},
            {"id": 2, "route_plan_id": 1, "route_sequence_pos": 1, **FIRST_ORDER_DATA},
            {"id": 3, "route_plan_id": 1, "route_sequence_pos": 2, **SECOND_ORDER_DATA},
            {"id": 4, "route_plan_id": 1, "route_sequence_pos": 3, **GARAGE_DATA},
        ]

        assert _load_plan_time_windows() == [
            {"id": 1, "start": _route_datetime(7, 0), "end": _route_datetime(23, 59), "node_id": 1},
            {"id": 2, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 2},
            {"id": 3, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 3},
            {"id": 4, "start": _route_datetime(10, 0), "end": _route_datetime(14, 0), "node_id": 4},
        ]


@skip_if_remote
@pytest.mark.parametrize("import_depot_garage", [True, False])
def test_two_plans_are_created_for_two_routes_in_solution(env: Environment, import_depot_garage):
    set_company_import_depot_garage(env, env.default_company.id, import_depot_garage)

    task_id = "mock_task_uuid__generic"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [first_route, second_route] = local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert _load_route_plans() == [
            {
                "id": 1,
                "route_id": first_route["id"],
                "orders_count": 2,
                "total_transit_distance_m": 17723,
                "total_duration": timedelta(seconds=7300),
            },
            {
                "id": 2,
                "route_id": second_route["id"],
                "orders_count": 1,
                "total_transit_distance_m": 6832,
                "total_duration": timedelta(seconds=1300),
            },
        ]

        assert _load_route_plan_nodes() == [
            {"id": 1, "route_plan_id": 1, "route_sequence_pos": 0, **DEPOT_DATA},
            {"id": 2, "route_plan_id": 1, "route_sequence_pos": 1, **FIRST_ORDER_DATA},
            {"id": 3, "route_plan_id": 1, "route_sequence_pos": 2, **SECOND_ORDER_DATA},
            {"id": 4, "route_plan_id": 2, "route_sequence_pos": 0, **DEPOT_DATA},
            {"id": 5, "route_plan_id": 2, "route_sequence_pos": 1, **THIRD_ORDER_DATA},
        ]

        assert _load_plan_time_windows() == [
            {"id": 1, "start": _route_datetime(7, 0), "end": _route_datetime(23, 59), "node_id": 1},
            {"id": 2, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 2},
            {"id": 3, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 3},
            {"id": 4, "start": _route_datetime(7, 0), "end": _route_datetime(23, 59), "node_id": 4},
            {"id": 5, "start": _route_datetime(10, 0), "end": _route_datetime(22, 0), "node_id": 5},
        ]


@skip_if_remote
def test_route_plans_are_deleted_when_route_is_deleted(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__generic"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [first_route, second_route] = local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert db.session.query(RoutePlan).count() == 2
        assert db.session.query(RoutePlanNode).count() == 5
        assert db.session.query(PlanTimeWindow).count() == 5

    path_delete = f"/api/v1/companies/{env.default_company.id}/routes"
    local_delete(env.client, path_delete, data=[first_route["id"]], headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert db.session.query(RoutePlan).count() == 1
        assert db.session.query(RoutePlanNode).count() == 2
        assert db.session.query(PlanTimeWindow).count() == 2

    local_delete(env.client, path_delete, data=[second_route["id"]], headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert db.session.query(RoutePlan).count() == 0
        assert db.session.query(RoutePlanNode).count() == 0
        assert db.session.query(PlanTimeWindow).count() == 0


@skip_if_remote
def test_route_plan_is_changed_if_re_imported_route_is_changed(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__reduced_generic"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert _load_route_plans() == [
            {
                "id": 1,
                "route_id": route["id"],
                "orders_count": 2,
                "total_transit_distance_m": 17723,
                "total_duration": timedelta(seconds=7300),
            }
        ]

        assert _load_route_plan_nodes() == [
            {"id": 1, "route_plan_id": 1, "route_sequence_pos": 0, **DEPOT_DATA},
            {"id": 2, "route_plan_id": 1, "route_sequence_pos": 1, **FIRST_ORDER_DATA},
            {"id": 3, "route_plan_id": 1, "route_sequence_pos": 2, **SECOND_ORDER_DATA},
        ]

        assert _load_plan_time_windows() == [
            {"id": 1, "start": _route_datetime(7, 0), "end": _route_datetime(23, 59), "node_id": 1},
            {"id": 2, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 2},
            {"id": 3, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 3},
        ]

    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}&import-mode=replace-not-started"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert _load_route_plans() == [
            {
                "id": 2,
                "route_id": route["id"],
                "orders_count": 2,
                "total_transit_distance_m": 41724,
                "total_duration": timedelta(seconds=34800),
            }
        ]

        assert _load_route_plan_nodes() == [
            {"id": 4, "route_plan_id": 2, "route_sequence_pos": 0, **DEPOT_DATA},
            {"id": 5, "route_plan_id": 2, "route_sequence_pos": 1, **FIRST_ORDER_DATA},
            {"id": 6, "route_plan_id": 2, "route_sequence_pos": 2, **SECOND_ORDER_DATA},
            {"id": 7, "route_plan_id": 2, "route_sequence_pos": 3, **GARAGE_DATA},
        ]

        assert _load_plan_time_windows() == [
            {"id": 4, "start": _route_datetime(7, 0), "end": _route_datetime(23, 59), "node_id": 4},
            {"id": 5, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 5},
            {"id": 6, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 6},
            {"id": 7, "start": _route_datetime(10, 0), "end": _route_datetime(14, 0), "node_id": 7},
        ]


@skip_if_remote
def test_route_plan_is_created_if_update_orders_is_true(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__reduced_generic"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [first_route] = local_post(env.client, path, headers=env.user_auth_headers)

    # Import solution with changed time_window
    task_id = "mock_task_uuid__sms_time_window_test"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}&update-orders=true"
    [second_route] = local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert _load_route_plans() == [
            {
                "id": 1,
                "route_id": first_route["id"],
                "orders_count": 2,
                "total_transit_distance_m": 17723,
                "total_duration": timedelta(seconds=7300),
            },
            {
                "id": 2,
                "route_id": second_route["id"],
                "orders_count": 2,
                "total_transit_distance_m": 17723,
                "total_duration": timedelta(seconds=7300),
            },
        ]

        new_date = date(2020, 8, 31)

        assert _load_route_plan_nodes() == [
            {"id": 1, "route_plan_id": 1, "route_sequence_pos": 0, **DEPOT_DATA},
            {"id": 2, "route_plan_id": 1, "route_sequence_pos": 1, **FIRST_ORDER_DATA},
            {"id": 3, "route_plan_id": 1, "route_sequence_pos": 2, **SECOND_ORDER_DATA},
            {"id": 4, "route_plan_id": 2, "route_sequence_pos": 0, **_node_with_date(DEPOT_DATA, new_date)},
            {"id": 5, "route_plan_id": 2, "route_sequence_pos": 1, **_node_with_date(FIRST_ORDER_DATA, new_date)},
            {"id": 6, "route_plan_id": 2, "route_sequence_pos": 2, **_node_with_date(SECOND_ORDER_DATA, new_date)},
        ]

        new_node_time = datetime(2020, 8, 31, 10, 0, 0, tzinfo=dateutil.tz.gettz("Europe/Moscow"))
        new_node_time_window = {"start": new_node_time, "end": new_node_time + timedelta(days=1)}

        assert _load_plan_time_windows() == [
            {"id": 1, "node_id": 1, "start": _route_datetime(7, 0), "end": _route_datetime(23, 59)},
            {"id": 2, "node_id": 2, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0)},
            {"id": 3, "node_id": 3, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0)},
            {"id": 4, "node_id": 4, **new_node_time_window},
            {"id": 5, "node_id": 5, **new_node_time_window},
            {"id": 6, "node_id": 6, **new_node_time_window},
        ]


@skip_if_remote
def test_plan_with_multi_order_is_imported(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__with_multi_order"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert _load_route_plans() == [
            {
                "id": 1,
                "route_id": route["id"],
                "orders_count": 3,
                "total_transit_distance_m": 17723,
                "total_duration": timedelta(seconds=7300),
            }
        ]

        assert _load_route_plan_nodes() == [
            {"id": 1, "route_plan_id": 1, "route_sequence_pos": 0, **DEPOT_DATA},
            {"id": 2, "route_plan_id": 1, "route_sequence_pos": 1, **FIRST_ORDER_DATA},
            {"id": 3, "route_plan_id": 1, "route_sequence_pos": 2, **SECOND_ORDER_DATA},
            {
                "id": 4,
                "route_plan_id": 1,
                "route_sequence_pos": 3,
                **SECOND_ORDER_DATA,
                "number": "79",
                "multi_order": True,
                "transit_distance_m": 0,
            },
        ]

        assert _load_plan_time_windows() == [
            {"id": 1, "start": _route_datetime(7, 0), "end": _route_datetime(23, 59), "node_id": 1},
            {"id": 2, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 2},
            {"id": 3, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 3},
            {"id": 4, "start": _route_datetime(9, 0), "end": _route_datetime(14, 0), "node_id": 4},
        ]


@skip_if_remote
def test_plan_info_has_date_filtration(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__with_multi_order"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    path = f"/api/v1/companies/{env.default_company.id}/route-plan-info?start_date=2019-12-01&end_date=2019-12-12"
    info = local_get(env.client, path, headers=env.user_auth_headers)
    assert info == []

    path = f"/api/v1/companies/{env.default_company.id}/route-plan-info?start_date=2019-12-13&end_date=2019-12-13"
    info = local_get(env.client, path, headers=env.user_auth_headers)
    assert info == [
        {
            "id": route["id"],
            "number": route["number"],
            "date": route["date"],
            "plan_metrics": {
                "orders_count": 3,
                "total_transit_distance_m": 17723,
                "total_duration": {"value": 7300, "text": "2:01:40"},
            },
        }
    ]


@skip_if_remote
def test_plan_info_has_company_filtration(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__with_multi_order"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    test_user_id, auth = add_user(env, "own_user", UserRole.admin, env.default_shared_company.id)

    path = f"/api/v1/companies/{env.default_shared_company.id}/route-plan-info?start_date=2019-12-13&end_date=2019-12-13"
    info = local_get(env.client, path, headers=auth)
    assert info == []


@skip_if_remote
def test_plan_info_has_depot_filtration(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__with_multi_order"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    path = f"/api/v1/companies/{env.default_company.id}/route-plan-info?depot_id=101"
    local_get(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)

    path = f"/api/v1/companies/{env.default_company.id}/route-plan-info?depot_id={env.default_depot.id}"
    info = local_get(env.client, path, headers=env.user_auth_headers)
    assert info == [
        {
            "id": env.default_route.id,
            "number": env.default_route.number,
            "date": str(env.default_route.date),
            "plan_metrics": None,
        },
        {
            "id": route["id"],
            "number": route["number"],
            "date": route["date"],
            "plan_metrics": {
                "orders_count": 3,
                "total_transit_distance_m": 17723,
                "total_duration": {"value": 7300, "text": "2:01:40"},
            },
        },
    ]


@skip_if_remote
def test_plan_info_is_available_only_if_user_has_access_to_the_depot(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__with_multi_order"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    test_user_id, auth = add_user(env, "own_user", UserRole.dispatcher, env.default_company.id)
    path = f"/api/v1/companies/{env.default_company.id}/route-plan-info?end_date=2019-12-31"
    info = local_get(env.client, path, headers=auth)
    assert info == []

    add_user_depot(env, test_user_id)
    info = local_get(env.client, path, headers=auth)
    assert info == [
        {
            "id": route["id"],
            "number": route["number"],
            "date": route["date"],
            "plan_metrics": {
                "orders_count": 3,
                "total_transit_distance_m": 17723,
                "total_duration": {"value": 7300, "text": "2:01:40"},
            },
        }
    ]


@skip_if_remote
def test_plan_info_is_available_for_shared_companies(env: Environment):
    task_id = "mock_task_uuid__generic"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [first_route, second_route] = local_post(env.client, path, headers=env.user_auth_headers)

    path = f"/api/v1/companies/{env.default_company.id}/route-plan-info?end_date=2019-12-31"
    info = local_get(env.client, path, headers=env.user_auth_headers)

    # all routes are available for own company admin user
    assert info == [
        {
            "id": first_route["id"],
            "number": first_route["number"],
            "date": first_route["date"],
            "plan_metrics": {
                "orders_count": 2,
                "total_transit_distance_m": 17723,
                "total_duration": {"value": 7300, "text": "2:01:40"},
            },
        },
        {
            "id": second_route["id"],
            "number": second_route["number"],
            "date": second_route["date"],
            "plan_metrics": {
                "orders_count": 1,
                "total_transit_distance_m": 6832,
                "total_duration": {"value": 1300, "text": "0:21:40"},
            },
        },
    ]

    user_id, auth = add_user(env, "test_shared_user", UserRole.dispatcher, env.default_shared_company.id)
    local_get(env.client, path, headers=auth, expected_status=HTTPStatus.FORBIDDEN)
    add_user_shared_company(env, user_id, env.default_company.id)
    info = local_get(env.client, path, headers=auth)

    # only routes with shared orders are available for another company user
    assert info == [
        {
            "id": first_route["id"],
            "number": first_route["number"],
            "date": first_route["date"],
            "plan_metrics": {
                "orders_count": 2,
                "total_transit_distance_m": 17723,
                "total_duration": {"value": 7300, "text": "2:01:40"},
            },
        }
    ]


@skip_if_remote
def test_plan_info_returns_nulls_if_plan_is_not_created(env: Environment):
    routes_path = f"/api/v1/companies/{env.default_company.id}/routes"
    data = {
        "number": "1",
        "courier_number": env.default_courier.number,
        "depot_number": env.default_depot.number,
        "date": "2021-05-26",
    }
    route = local_post(env.client, routes_path, headers=env.user_auth_headers, data=data)

    plan_info_path = (
        f"/api/v1/companies/{env.default_company.id}/route-plan-info?start_date=2021-05-26&end_date=2021-05-26"
    )
    info = local_get(env.client, plan_info_path, headers=env.user_auth_headers)
    assert info == [
        {
            "id": route["id"],
            "number": route["number"],
            "date": route["date"],
            "plan_metrics": None,
        }
    ]


@skip_if_remote
def test_plan_info_requires_two_sql_queries(env_with_debug_queries: Environment):
    env = env_with_debug_queries
    with env.flask_app.app_context():
        g.user = UserAccount(
            id=env.default_user.id,
            login=env.default_user.login,
            uid=env.default_user.uid,
            company_ids=[env.default_company.id],
            is_super=env.default_user.is_super,
            confirmed_at=env.default_user.confirmed_at,
            role=UserRole.admin.value,
        )
        info = get_route_plan_info(env.default_company.id, 1, 100, env.default_route.date, env.default_route.date, None)
        # first query - actual data, second one - count(*) as part of paginate call
        assert len(get_debug_queries()) == 3
        assert len(info) == 1


@skip_if_remote
def test_add_route_plans_only_required_fields(env: Environment):
    route_plans_path = f"/api/v1/companies/{env.default_company.id}/route-plans"
    data = [
        {
            "number": "0",
            "nodes": [
                {
                    "type": PlanNodeType.garage.value,
                    "arrival_time_s": 26787,
                    "transit_distance_m": 24001,
                    "value": {
                        "number": "first-garage",
                        "point": {
                            "lat": 55.664695,
                            "lon": 37.562443
                        },
                    }
                },
                {
                    "type": PlanNodeType.depot.value,
                    "arrival_time_s": 25200,
                    "transit_distance_m": 0,
                    "value": {
                        "number": "0",
                        "point": {
                            "lat": 55.799087,
                            "lon": 37.729377
                        },
                        "service_duration_s": 200,
                    }
                },
                {
                    "type": PlanNodeType.order.value,
                    "arrival_time_s": 57526,
                    "departure_time_s": 57526,
                    "transit_distance_m": 8723,
                    "used_time_window": "10:00-12:00",
                    "value": {
                        "number": "126",
                        "point": {
                            "lat": 55.826326,
                            "lon": 37.637686
                        },
                        "service_duration_s": 0,
                    }
                }
            ]
        }
    ]
    local_post(env.client, route_plans_path, headers=env.user_auth_headers, data=data)

    plan_info_path = (
        f"/api/v1/companies/{env.default_company.id}/route-plan-info?start_date=2021-01-01&end_date=2021-01-02"
    )
    info = local_get(env.client, plan_info_path, headers=env.user_auth_headers)
    assert info == [
        {
            'id': 1,
            'number': '0',
            'date': '2021-01-01',
            'plan_metrics': {
                'orders_count': 1,
                'total_transit_distance_m': 32724,
                'total_duration': {'value': 30739, 'text': '8:32:19'}
            }
        }
    ]


@skip_if_remote
def test_add_route_plans_all_fields(env: Environment):
    route_plans_path = f"/api/v1/companies/{env.default_company.id}/route-plans"
    data = [
        {
            "number": "0",
            "nodes": [
                {
                    "type": PlanNodeType.garage.value,
                    "arrival_time_s": 26787,
                    "departure_time_s": 26800,
                    "transit_distance_m": 24001,
                    "used_time_window": "00:00-1.00:00",
                    "value": {
                        "number": "last-garage",
                        "address": "test-address",
                        "point": {
                            "lat": 55.664695,
                            "lon": 37.562443
                        },
                        "service_duration_s": 0,
                    }
                },
                {
                    "type": PlanNodeType.depot.value,
                    "arrival_time_s": 25200,
                    "departure_time_s": 25400,
                    "transit_distance_m": 0,
                    "used_time_window": "01:00-1.00:00",
                    "value": {
                        "number": "0",
                        "address": "test-street, 101",
                        "point": {
                            "lat": 55.799087,
                            "lon": 37.729377
                        },
                        "service_duration_s": 200,
                    }
                },
                {
                    "type": PlanNodeType.order.value,
                    "arrival_time_s": 57526,
                    "departure_time_s": 57526,
                    "transit_distance_m": 8723,
                    "used_time_window": "10:00-12:00",
                    "value": {
                        "number": "2345",
                        "address": "221B Baker Street",
                        "point": {
                            "lat": 55.826326,
                            "lon": 37.637686
                        },
                        "service_duration_s": 0,
                        "shared_service_duration_s": 0,
                        "multi_order": False,
                        "customer_name": "Sherlock",
                        "shipment_size": {
                            "volume_cbm": 1.2,
                            "weight_kg": 5.1,
                            "amount": 4.5
                        }
                    }
                }
            ]
        }
    ]
    local_post(env.client, route_plans_path, headers=env.user_auth_headers, data=data)


@skip_if_remote
def test_add_too_many_route_plans(env: Environment):
    route_plans_path = f"/api/v1/companies/{env.default_company.id}/route-plans"
    data = [{}] * 101
    local_post(env.client, route_plans_path, headers=env.user_auth_headers, data=data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_invalid_type(env: Environment):
    route_plans_path = f"/api/v1/companies/{env.default_company.id}/route-plans"
    data = [
        {
            "number": "0",
            "nodes": [
                {
                    "type": "invalid_type",
                    "number": "last-garage",
                    "point": {
                        "lat": 55.664695,
                        "lon": 37.562443
                    },
                    "arrival_time_s": 26787,
                    "transit_distance_m": 24001
                }
            ]
        }
    ]
    local_post(env.client, route_plans_path, headers=env.user_auth_headers, data=data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_new_route_plan_replaces_the_old_one(env: Environment):
    task_id = "mock_task_uuid__reduced_generic"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    route_plans_path = f"/api/v1/companies/{env.default_company.id}/route-plans"
    data = [{"number": route["number"], "nodes": []}]
    local_post(env.client, route_plans_path, headers=env.user_auth_headers, data=data)

    with env.flask_app.app_context():
        assert _load_route_plans() == [
            {
                "id": 2,
                "route_id": route["id"],
                "orders_count": 0,
                "total_transit_distance_m": 0,
                "total_duration": timedelta(seconds=0),
            }
        ]


@skip_if_remote
def test_invalid_route_plan_does_not_replace_the_old_one(env: Environment):
    task_id = "mock_task_uuid__reduced_generic"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, path, headers=env.user_auth_headers)

    route_plans_path = f"/api/v1/companies/{env.default_company.id}/route-plans"
    data = [
        {
            "number": route["number"],
            "nodes": [
                {
                    "type": PlanNodeType.order.value,
                    "arrival_time_s": 57526,
                    "departure_time_s": 57526,
                    "transit_distance_m": 8723,
                    "used_time_window": "12:00-10:00",  # invalid time window
                    "value": {
                        "number": "126",
                        "point": {"lat": 55.826326, "lon": 37.637686},
                        "service_duration_s": 0,
                    },
                }
            ],
        }
    ]
    local_post(
        env.client,
        route_plans_path,
        headers=env.user_auth_headers,
        data=data,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )

    with env.flask_app.app_context():
        assert _load_route_plans() == [
            {
                "id": 1,
                "route_id": route["id"],
                "orders_count": 2,
                "total_transit_distance_m": 17723,
                "total_duration": timedelta(seconds=7300),
            }
        ]
