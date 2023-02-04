from http import HTTPStatus
from typing import List, Dict, Any

import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get, local_post, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage
from ya_courier_backend.models import DepotInstance, DepotInstanceStatus, DepotInstanceHistoryEvent, RouteNode
from ya_courier_backend.models.route_node import ROUTE_NODE_TYPES
from ya_courier_backend.util.util import get_by_id


def _get_depot(env: Environment, route_id: int) -> List[Dict[str, Any]]:
    return local_get(
        client=env.client,
        path=f"/api/v1/companies/{env.default_company.id}/routes/{route_id}/depot-visits",
        headers=env.user_auth_headers)


def _setup(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)
    task_id = "mock_task_uuid__generic_with_two_depots_and_garage"
    [route_with_depots] = local_post(
        client=env.client,
        path=f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}",
        headers=env.user_auth_headers)
    route_id = route_with_depots["id"]

    depots = _get_depot(env, route_id)
    assert len(depots) == 2
    return route_id, [depot["instance_id"] for depot in depots]


@pytest.mark.parametrize(
    argnames="patch_data_request, response_code, check_history_len, check_history_events, check_instance_status",
    argvalues=[
        ({}, HTTPStatus.OK, 1, (DepotInstanceHistoryEvent.created,), DepotInstanceStatus.unvisited),
        ({"status": "not_exist_status"}, HTTPStatus.UNPROCESSABLE_ENTITY, 1, (DepotInstanceHistoryEvent.created,), DepotInstanceStatus.unvisited),
        ({"status": "unvisited"}, HTTPStatus.OK, 1, (DepotInstanceHistoryEvent.created,), DepotInstanceStatus.unvisited),
        ({"status": "visited"}, HTTPStatus.OK, 4, (DepotInstanceHistoryEvent.created, DepotInstanceHistoryEvent.arrival, DepotInstanceHistoryEvent.visit), DepotInstanceStatus.visited),
    ],
    ids=[
        "empty_status",
        "not_existent_status",
        "unvisited",
        "visited",
    ],
)
@skip_if_remote
def test_patch_depot_visits_status(
    env: Environment,
    patch_data_request: Dict[str, Any],
    response_code: HTTPStatus,
    check_history_len: int,
    check_history_events: List[DepotInstanceHistoryEvent],
    check_instance_status: DepotInstanceStatus,
):
    # Setup
    route_id, depot_ids = _setup(env)

    for depot_id in depot_ids:
        # Update status of depot instance by api
        local_patch(
            client=env.client,
            headers=env.user_auth_headers,
            path=f"/api/v1/companies/{env.default_company.id}/routes/{route_id}/depot-visits/{depot_id}",
            data=patch_data_request,
            expected_status=response_code,
        )

        # Check status and history of the depot instance updated by api
        with env.flask_app.app_context():
            first_depot_inst: DepotInstance = get_by_id(DepotInstance, depot_id)
            assert len(first_depot_inst.history) == check_history_len
            assert all(map(first_depot_inst.has_history_event, check_history_events))
            assert first_depot_inst.status == check_instance_status


@skip_if_remote
def test_patch_non_existent_depot_visits(env: Environment):
    route_id, depot_ids = _setup(env)
    non_existent_depot_id = max(depot_ids) + 1
    local_patch(
        client=env.client,
        headers=env.user_auth_headers,
        path=f"/api/v1/companies/{env.default_company.id}/routes/{route_id}/depot-visits/{non_existent_depot_id}",
        data={"status": "visited"},
        expected_status=HTTPStatus.NOT_FOUND,
    )


@skip_if_remote
def test_rollback_depot_visits_status_forbidden(env: Environment):
    status_for_test = DepotInstanceStatus.visited
    status_rollback = DepotInstanceStatus.unvisited
    # Setup
    route_id, depot_ids = _setup(env)

    for depot_id in depot_ids:
        # Update status of depot instance by api
        local_patch(
            client=env.client,
            headers=env.user_auth_headers,
            path=f"/api/v1/companies/{env.default_company.id}/routes/{route_id}/depot-visits/{depot_id}",
            data={"status": status_for_test.value},
        )

        # Set values to check next
        with env.flask_app.app_context():
            first_depot_inst: DepotInstance = get_by_id(DepotInstance, depot_id)
            check_history_len = len(first_depot_inst.history)
            check_instance_status = first_depot_inst.status

        # Try to rollback status
        local_patch(
            client=env.client,
            headers=env.user_auth_headers,
            path=f"/api/v1/companies/{env.default_company.id}/routes/{route_id}/depot-visits/{depot_id}",
            data={"status": status_rollback.value},
            expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
        )

        # Check status and history of the depot instance
        with env.flask_app.app_context():
            first_depot_inst: DepotInstance = get_by_id(DepotInstance, depot_id)
            assert len(first_depot_inst.history) == check_history_len
            assert first_depot_inst.status == check_instance_status


@skip_if_remote
def test_patch_not_status_fields(env: Environment):
    # Setup
    route_id, depot_ids = _setup(env)

    for depot_id in depot_ids:
        # Patch depot instance
        patch_id = 42
        patch_segment_distance_m = 42.2
        path_history_event_class_with_departure = [{
            "event": "DEPARTURE",
            "position": {"lat": 55.7447, "lon": 37.6727, "time": "1970-01-01T03:00:00+03:00"},
            "timestamp": 1622561466.241483,
            "used_radius": 0
        }]
        patch_depot_id = 123
        patch_route_id = 321

        rlt = local_patch(
            client=env.client,
            headers=env.user_auth_headers,
            path=f"/api/v1/companies/{env.default_company.id}/routes/{route_id}/depot-visits/{depot_id}",
            data={
                "id": patch_id,
                "depot_id": patch_depot_id,
                "route_id": patch_route_id,
                "segment_distance_m": patch_segment_distance_m,
                "history_event_class": path_history_event_class_with_departure,
            },
        )
        assert rlt == {"instance_id": depot_id, "status": "unvisited"}

        # Check: fields are not modified
        with env.flask_app.app_context():
            depot_instance_nodes: list[RouteNode] = RouteNode.get(
                slave_session=False, route_node_type=ROUTE_NODE_TYPES[DepotInstance],
            )
            assert all(el.entity.id != patch_id for el in depot_instance_nodes)
            assert all(el.entity.depot_id != patch_depot_id for el in depot_instance_nodes)
            assert all(el.entity.route_id != patch_route_id for el in depot_instance_nodes)
            assert all(el.segment_distance_m != patch_segment_distance_m for el in depot_instance_nodes)
            assert all(not el.entity.has_history_event(DepotInstanceHistoryEvent.departure)
                       for el in depot_instance_nodes)
