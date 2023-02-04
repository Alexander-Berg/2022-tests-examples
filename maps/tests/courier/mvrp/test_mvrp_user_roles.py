import pytest
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import solver_request_by_task_id
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_auth import add_user, UserType


@skip_if_remote
@pytest.mark.parametrize(
    argnames="user_type, response_status",
    argvalues=[
        (UserType.admin_oauth, HTTPStatus.ACCEPTED),
        (UserType.admin_jwt, HTTPStatus.ACCEPTED),
        (UserType.manager_oauth, HTTPStatus.ACCEPTED),
        (UserType.manager_jwt, HTTPStatus.ACCEPTED),
        (UserType.dispatcher_oauth, HTTPStatus.ACCEPTED),
        (UserType.dispatcher_jwt, HTTPStatus.ACCEPTED),
        (UserType.app_oauth, HTTPStatus.FORBIDDEN),
        (UserType.app_jwt, HTTPStatus.FORBIDDEN),
    ],
)
def test_add_mvrp_task_permissions(env: Environment, user_type, response_status):
    task_id = "mock_task_uuid__generic"
    path_add_mvrp = "/api/v1/vrs/add/mvrp"
    _, user_auth = add_user(env, user_type)
    task_json = solver_request_by_task_id[task_id]
    local_post(env.client, path_add_mvrp, data=task_json, headers=user_auth, expected_status=response_status)


@skip_if_remote
@pytest.mark.parametrize("route", [
    "/api/v1/vrs/request/mvrp/mock_task_uuid__generic",
    "/api/v1/vrs/result/mvrp/mock_task_uuid__generic",
    "/api/v1/vrs/children?parent_task_id=mock_task_uuid__generic",
    "/api/v1/vrs/mvrp/mock_task_uuid__generic/rented-vehicle",
    "/api/v1/vrs/result/svrp/mock_task_uuid__svrp_generic"
])
@pytest.mark.parametrize(
    argnames="user_type, response_status",
    argvalues=[
        (UserType.admin_oauth, HTTPStatus.OK),
        (UserType.admin_jwt, HTTPStatus.OK),
        (UserType.manager_oauth, HTTPStatus.OK),
        (UserType.manager_jwt, HTTPStatus.OK),
        (UserType.dispatcher_oauth, HTTPStatus.OK),
        (UserType.dispatcher_jwt, HTTPStatus.OK),
        (UserType.app_oauth, HTTPStatus.FORBIDDEN),
        (UserType.app_jwt, HTTPStatus.FORBIDDEN),
    ],
)
def test_mvrp_get_routes_permissions(env: Environment, route, user_type, response_status):
    _, user_auth = add_user(env, user_type)
    local_get(env.client, route, headers=user_auth, expected_status=response_status)
