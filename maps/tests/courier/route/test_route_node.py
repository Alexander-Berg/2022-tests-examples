import copy

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage

DEFAULT_ORDER = {
    "number": "",
    "time_interval": "00:00-23:59",
    "address": "some address",
    "lat": 55.791928,
    "lon": 37.841492,
    "route_id": 1,
}


def _get_order_with(number, route_id=1):
    order = copy.deepcopy(DEFAULT_ORDER)
    order["number"] = number
    order["route_id"] = route_id
    return order


@skip_if_remote
def test_added_order_sequence_pos(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    # 2. Remember number of nodes in the route
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={routes[0]['id']}"
    route_info = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    original_node_count = len(route_info[0]['nodes'])

    # 3. Add order with number 'added-order-0'
    added_order_number = 'added-order-0'
    order = _get_order_with(added_order_number, routes[0]['id'])
    path_orders = f"/api/v1/companies/{env.default_company.id}/orders"
    local_post(env.client, path_orders, headers=env.user_auth_headers, data=order)

    # 4. Check that order was added before depot and route in the end
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={routes[0]['id']}"
    route_info = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    route_nodes = route_info[0]['nodes']
    assert len(route_nodes) == original_node_count + 1
    assert route_nodes[-3]['value']['number'] == added_order_number
