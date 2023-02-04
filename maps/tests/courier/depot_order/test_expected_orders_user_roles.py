import datetime
import pytest
from urllib.parse import urlencode

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user, add_user_depot, set_company_import_depot_garage

from ya_courier_backend.models import UserRole


@skip_if_remote
@pytest.mark.parametrize("role", [UserRole.manager, UserRole.dispatcher])
def test_manager_and_dispatcher_can_get_expected_orders_only_for_own_depots(env: Environment, role):
    set_company_import_depot_garage(env, env.default_company.id, False)

    task_id = "mock_task_uuid__generic"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route, _] = local_post(env.client, path_import, headers=env.user_auth_headers)
    user_id, user_auth = add_user(env, "new_test_user", role)

    routed_orders_params = {"lat": 55.826326, "lon": 37.637686, "time_now": "12:00"}
    path_routed_orders = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/routed-orders?"
    path_routed_orders += urlencode(routed_orders_params)
    local_get(env.client, path_routed_orders, headers=env.user_auth_headers)

    route_date = datetime.date.fromisoformat(route["date"])
    from_datetime = datetime.datetime.combine(route_date, datetime.time.min, datetime.timezone(datetime.timedelta(hours=3)))
    to_datetime = from_datetime + datetime.timedelta(days=1)
    parameters = {
        "lat": 55.826326,
        "lon": 37.637686,
        "from": from_datetime.isoformat(),
        "to": to_datetime.isoformat(),
        "radius": 500,
    }
    path = f"/api/v1/companies/{env.default_company.id}/expected-orders?{urlencode(parameters)}"
    assert local_get(env.client, path, headers=user_auth) == []

    add_user_depot(env, user_id)
    user_orders = local_get(env.client, path, headers=user_auth)
    assert len(user_orders) == 1

    all_orders = local_get(env.client, path, headers=env.user_auth_headers)
    assert user_orders == all_orders
