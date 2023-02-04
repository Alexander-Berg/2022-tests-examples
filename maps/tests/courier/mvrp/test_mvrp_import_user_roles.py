from http import HTTPStatus
import json

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user, add_user_depot
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import solver_request_by_task_id

from ya_courier_backend.models import UserRole


@skip_if_remote
def test_manager_mvrp_task_post(env: Environment):
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)

    task_id = "mock_task_uuid__generic"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path, headers=manager_auth, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id)
    local_post(env.client, path, headers=manager_auth, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_manager_import_routes_post(env: Environment):
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)

    path = f"/api/v1/companies/{env.default_company.id}/import-routes"
    data = solver_request_by_task_id["mock_task_uuid__generic"]
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, manager_id)
    local_post(env.client, path, headers=manager_auth, data=data, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_manager_can_check_import_task_for_all_depots(env: Environment):
    manager_id, manager_auth = add_user(env, "test_manager", UserRole.manager)

    # Check by task id
    task_id = "mock_task_uuid__generic"
    path = f"/api/v1/companies/{env.default_company.id}/check-import-task?task_id={task_id}"
    local_post(env.client, path, headers=manager_auth, expected_status=HTTPStatus.OK)

    # Check by task json
    task = {
        "vehicles": [{"id": 0, "routing_mode": "driving"}],
        "locations": [{"id": 0, "point": {"lat": 55.61, "lon": 37.76}, "time_window": "07:00-23:00"}],
        "depots": [{"id": 0, "point": {"lat": 55.71, "lon": 37.86}, "time_window": "07:00-23:00"}],
    }
    path = f"/api/v1/companies/{env.default_company.id}/check-import-task"
    local_post(env.client, path, headers=manager_auth, data=json.dumps(task), expected_status=HTTPStatus.OK)
