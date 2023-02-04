import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_patch, local_post, local_get, local_delete
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage, update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data


@skip_if_remote
def test_courier_quality_get_depot(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    # 2. Get courier-quality with types=depot
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}&types=depot"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    # 3. Check if depot received
    assert courier_quality == [
        {
            'type': 'depot',
            'courier_name': 'car-1',
            'courier_number': '2020',
            'courier_deleted': False,
            'depot_number': '0',
            'lat': 55.7447,
            'lon': 37.6727,
            'route_date': '2019-12-13',
            'route_number': '2020-1-2019-12-13',
            'segment_distance_m': None,
            'arrived_at': None,
            'left_at': None,
            'location_idle_duration': None,
            'transit_idle_duration': None,
            'service_duration_s': 600,
        }, {
            'type': 'depot',
            'courier_name': 'car-1',
            'courier_number': '2020',
            'courier_deleted': False,
            'depot_number': '0',
            'lat': 55.7447,
            'lon': 37.6727,
            'route_date': '2019-12-13',
            'route_number': '2020-1-2019-12-13',
            'segment_distance_m': None,
            'arrived_at': None,
            'left_at': None,
            'location_idle_duration': None,
            'transit_idle_duration': None,
            'service_duration_s': 600,
        }, {
            'type': 'depot',
            'courier_name': 'car-1',
            'courier_number': '2020',
            'courier_deleted': False,
            'depot_number': '0',
            'lat': 55.7447,
            'lon': 37.6727,
            'route_date': '2019-12-13',
            'route_number': '2020-2-2019-12-13',
            'segment_distance_m': None,
            'arrived_at': None,
            'left_at': None,
            'location_idle_duration': None,
            'transit_idle_duration': None,
            'service_duration_s': 600,
        }
    ]


@skip_if_remote
def test_courier_quality_get_garage(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    # 2. Get courier-quality with types=garage
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}&types=garage"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    # 3. Check if garage received
    assert courier_quality == [
        {
            'type': 'garage',
            'courier_name': 'car-1',
            'courier_number': '2020',
            'courier_deleted': False,
            'garage_number': '2',
            'lat': 55.664695,
            'lon': 37.562443,
            'route_date': '2019-12-13',
            'route_number': '2020-1-2019-12-13',
            'segment_distance_m': None,
            'location_idle_duration': None,
            'transit_idle_duration': None,
        }
    ]


@skip_if_remote
def test_courier_quality_get_visited_garage(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route, _ = local_post(env.client, path_import, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    # 2. Visit first depot, order and last garage
    arrival_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
    locations = [(55.753693, 37.6727, arrival_time - 600)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.7447, 37.6727, arrival_time), (55.7447, 37.6727, arrival_time + 600 * 1)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    order_lat = route_info['nodes'][1]['value']['point']['lat']
    order_lon = route_info['nodes'][1]['value']['point']['lon']
    locations = [(order_lat, order_lon, arrival_time + 600 * 2), (order_lat, order_lon, arrival_time + 600 * 3)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.664695, 37.562443, arrival_time + 600 * 4), (55.664695, 37.562443, arrival_time + 600 * 5)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # 3. Get courier-quality with types=garage
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}&types=garage"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    # 4. Check if garage received
    assert courier_quality == [
        {
            'type': 'garage',
            'courier_name': 'car-1',
            'courier_number': '2020',
            'courier_deleted': False,
            'garage_number': '2',
            'lat': 55.664695,
            'lon': 37.562443,
            'route_date': '2019-12-13',
            'route_number': '2020-1-2019-12-13',
            'segment_distance_m': pytest.approx(18579.2925289641),
            'location_idle_duration': 0.0,
            'transit_idle_duration': 0.0,
        }
    ]


@skip_if_remote
def test_first_depot_has_segment_distance_after_visit(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route, _ = local_post(env.client, path_import, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    # 2. Visit first depot
    arrival_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
    locations = [(55.753693, 37.6727, arrival_time - 600)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.7447, 37.6727, arrival_time)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.7447, 37.6727, arrival_time + 600)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # 3. Get courier-quality with types=depot
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}&types=depot"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    # 4. Check first depot has segment_distance_m
    assert courier_quality[0]['segment_distance_m'] == pytest.approx(1000.0, abs=0.1)
    assert courier_quality[1]['segment_distance_m'] is None


@skip_if_remote
def test_first_depot_segment_distance_is_zero_when_order_is_visited_first(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route, _ = local_post(env.client, path_import, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    # 2. Visit first order
    arrival_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
    locations = [(55.835319, 37.637686, arrival_time - 600)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.826326, 37.637686, arrival_time)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # 3. Get courier-quality with types=depot,order
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}&types=depot,order"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    # 4. Check first depot has segment_distance_m = 0
    assert courier_quality[0]['segment_distance_m'] == pytest.approx(0.0)
    assert courier_quality[0]['location_idle_duration'] == 0
    assert courier_quality[0]['transit_idle_duration'] == 0
    assert courier_quality[1]['segment_distance_m'] == pytest.approx(1000.0, abs=0.1)


@skip_if_remote
def test_first_depot_segment_distance_is_zero_when_order_is_finished_first(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route, _ = local_post(env.client, path_import, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    # 2. Make order finished
    order = route_info['nodes'][1]['value']
    path_order = f"/api/v1/companies/{env.default_company.id}/orders/{order['id']}"
    local_patch(env.client, path_order, headers=env.user_auth_headers, data={"status": "finished"})

    # 3. Check first depot has segment_distance_m as None
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}&types=depot,order"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)
    assert courier_quality[0]['segment_distance_m'] is None
    assert courier_quality[1]['segment_distance_m'] is None

    # 4. Update route state
    update_route(env, route['id'], {'lat': 55.8, 'lon': 37.6, 'time_now': '12:00'})

    # 5. Check first depot has segment_distance_m = 0
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)
    assert courier_quality[0]['segment_distance_m'] == pytest.approx(0.0)
    assert courier_quality[1]['segment_distance_m'] is None


@skip_if_remote
def test_fake_visited_depot_has_null_arrival_and_leaving_times(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route, _ = local_post(env.client, path_import, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    # 2. add depot visit event
    path_depot_visits = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/depot-visits"
    depots = local_get(
        client=env.client,
        path=path_depot_visits,
        headers=env.user_auth_headers)
    local_patch(
        client=env.client,
        headers=env.user_auth_headers,
        path=path_depot_visits + f"/{depots[0]['instance_id']}",
        data={"status": "visited"})

    # 3. check arrived_at and left_at is null, not 1970-01-01 midnight
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}&types=depot"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)
    assert courier_quality[0]['arrived_at'] is None
    assert courier_quality[0]['left_at'] is None


@skip_if_remote
def test_courier_quality_get_deleted_courier(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    # 1. Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route, _ = local_post(env.client, path_import, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    # 2. Visit first depot, order and last garage
    arrival_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
    locations = [(55.753693, 37.6727, arrival_time - 600)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.7447, 37.6727, arrival_time), (55.7447, 37.6727, arrival_time + 600 * 1)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    order_lat = route_info['nodes'][1]['value']['point']['lat']
    order_lon = route_info['nodes'][1]['value']['point']['lon']
    locations = [(order_lat, order_lon, arrival_time + 600 * 2), (order_lat, order_lon, arrival_time + 600 * 3)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # 3. Delete courier
    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_delete(env.client, courier_path, headers=env.user_auth_headers, query={'shallow_delete': True})

    # 4. Check deleted courier in resp
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}&types=order&with_deleted_couriers=True"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    assert courier_quality[0]['courier_deleted']
