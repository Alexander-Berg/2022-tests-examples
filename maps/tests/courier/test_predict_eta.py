from urllib.parse import urlencode

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get, local_delete
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data


ORDER_WITHOUT_ETA = {
    "arrival_time": None,
    "arrival_time_s": None,
    "waiting_duration_s": None,
    "failed_time_window": None,
}


def _get_predict_eta_path(route, lat=55.8, lon=37.6):
    path = f"/api/v1/couriers/{route['courier_id']}/routes/{route['id']}/predict-eta"
    params = urlencode({"lat": lat, "lon": lon, "time": "2019-12-13T11:11:00+03:00"})
    return f"{path}?{params}"


def _extract_eta(routed_order):
    eta_keys = {"arrival_time", "arrival_time_s", "waiting_duration_s", "failed_time_window"}
    return {key: routed_order[key] for key in eta_keys}


def _extract_eta_values(response):
    return {"route_end": response["route_end"], "route": list(map(_extract_eta, response["route"]))}


@skip_if_remote
def test_eta_is_calculated_only_after_first_depot_departure(env: Environment):
    # 0. Make default company not to import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, False)

    # Import route without depots and garages and calculate ETA to check against it later
    task_id = "mock_task_uuid__generic_with_two_depots_and_garage"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route_without_depots] = local_post(env.client, import_path, headers=env.user_auth_headers)
    eta_without_depots = local_get(env.client, _get_predict_eta_path(route_without_depots), headers=env.user_auth_headers)
    eta_without_depots = _extract_eta_values(eta_without_depots)

    route_path = f"/api/v1/companies/{env.default_company.id}/routes"
    local_delete(env.client, route_path, headers=env.user_auth_headers, data=[route_without_depots['id']])

    # Import route with depots and garages and calculate ETA
    set_company_import_depot_garage(env, env.default_company.id, True)
    [second_route] = local_post(env.client, import_path, headers=env.user_auth_headers)
    eta_with_depots = local_get(env.client, _get_predict_eta_path(second_route), headers=env.user_auth_headers)
    eta_with_depots = _extract_eta_values(eta_with_depots)

    # Check that ETA is not calculated if courier has not yet departed from first depot
    assert eta_with_depots["route"] == [ORDER_WITHOUT_ETA, ORDER_WITHOUT_ETA]
    assert eta_with_depots["route"] != eta_without_depots["route"]
    assert eta_with_depots["route_end"] != eta_without_depots["route_end"]
    assert eta_without_depots["route_end"] is not None

    # Visit and depart from first depot
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{second_route['id']}/push-positions"
    locations = [(55.7447, 37.6727, "2019-12-13T09:11:00+03:00")]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.7447, 37.6727, "2019-12-13T10:11:00+03:00")]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.8, 37.6, "2019-12-13T11:11:00+03:00")]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # Check that ETA values with and without depots are the same for the same location after first depot departure
    eta_with_depots = local_get(env.client, _get_predict_eta_path(second_route), headers=env.user_auth_headers)
    eta_with_depots = _extract_eta_values(eta_with_depots)

    assert eta_with_depots == eta_without_depots
