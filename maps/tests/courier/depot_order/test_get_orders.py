import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage, add_user, set_order_history
from ya_courier_backend.models import UserRole


@skip_if_remote
@pytest.mark.parametrize("types", ['order', 'depot', 'garage', 'order,depot,garage'])
def test_get_orders(env: Environment, types):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import routes
    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    # 2. Get orders with types
    path_get = f"/api/v1/companies/{env.default_company.id}/orders?types={types}"
    response = local_get(env.client, path_get, headers=env.user_auth_headers)

    # 3. Check if all types in response
    response_types = {item['type'] for item in response}
    assert response_types == set(types.split(','))


@skip_if_remote
def test_get_orders_with_none_history(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import routes and set history to None
    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)
    path_get = f"/api/v1/companies/{env.default_company.id}/orders"
    response = local_get(env.client, path_get, headers=env.user_auth_headers)
    order_id = response[0]['id']
    set_order_history(env, order_id, None)

    # 2. Get orders
    path_get = f"/api/v1/companies/{env.default_company.id}/orders"
    response = local_get(env.client, path_get, headers=env.user_auth_headers)

    # 3. Check if history is None
    assert response[0]['history'] is None


@skip_if_remote
def test_shared_depot(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import routes
    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    # 2. Get orders with types
    _, auth = add_user(env, 'test_shared_user', UserRole.admin, env.default_shared_company.id)
    path_get = f"/api/v1/companies/{env.default_company.id}/orders?types=depot"
    response = local_get(env.client, path_get, headers=auth)

    # 3.Check if response is empty
    assert response == []


@skip_if_remote
@pytest.mark.parametrize("types", ['depot', 'garage'])
def test_depot_garage_with_number(env: Environment, types):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import routes
    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    # 2. Get orders with types
    path_get = f"/api/v1/companies/{env.default_company.id}/orders?types={types}"
    order = local_get(env.client, path_get, headers=env.user_auth_headers)[0]
    path_get = f"/api/v1/companies/{env.default_company.id}/orders?types={types}&number={order['number']}"
    response = local_get(env.client, path_get, headers=env.user_auth_headers)

    # 3. Check if response is not empty
    assert response
