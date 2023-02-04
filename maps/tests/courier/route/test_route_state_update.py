from datetime import datetime
from http import HTTPStatus
import pytest

from ya_courier_backend.tasks.update_route_state import DirtyRouteSelector
from ya_courier_backend.models import db, Courier, Depot

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage, set_dirty_level


@skip_if_remote
@pytest.mark.parametrize("task_id", ["mock_task_uuid__generic_with_two_depots",
                                     "mock_task_uuid__generic_with_two_depots_and_garage",
                                     "mock_task_uuid__solution_with_garage_first_and_last",
                                     "mock_task_uuid__solution_with_garage_first",
                                     "mock_task_uuid__solution_with_garage_last"])
def test_fail(env: Environment, task_id):
    company_id = env.default_company.id
    set_company_import_depot_garage(env, company_id, True)

    path_import = f"/api/v1/companies/{company_id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    courier_id = env.default_courier.id
    route_id = routes[0]['id']

    path_route_info = f"/api/v1/companies/{company_id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    for node in route_info['nodes']:
        if node['type'] == 'order':
            path_order = f"/api/v1/companies/{company_id}/orders/{node['value']['id']}"
            local_patch(env.client, path_order, data={"status": "cancelled"}, headers=env.user_auth_headers)

    timestamp = datetime.now().timestamp()
    resp = local_get(env.client,
                     f'/api/v1/couriers/{courier_id}/routes/{route_id}/routed-orders',
                     query={
                         'lat': env.default_depot.lat,
                         'lon': env.default_depot.lon,
                         'timestamp': timestamp
                     },
                     headers=env.user_auth_headers,
                     expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert 'Can not calculate ETA for not started route without orders.' in resp['message']

    path_route_info = f"/api/v1/companies/{company_id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)


@skip_if_remote
@pytest.mark.parametrize("task_id", ["mock_task_uuid__result_no_first_depot_with_last_depot_and_garage",
                                     "mock_task_uuid__generic", "mock_task_uuid__100_locs"])
def test_success(env: Environment, task_id):
    company_id = env.default_company.id
    set_company_import_depot_garage(env, company_id, True)

    path_import = f"/api/v1/companies/{company_id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    courier_id = env.default_courier.id
    route_id = routes[0]['id']

    path_route_info = f"/api/v1/companies/{company_id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    timestamp = datetime.now().timestamp()
    local_get(env.client,
              f'/api/v1/couriers/{courier_id}/routes/{route_id}/routed-orders',
              query={
                  'lat': env.default_depot.lat,
                  'lon': env.default_depot.lon,
                  'timestamp': timestamp
              },
              headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{company_id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    assert "estimated_service_time" in route_info["nodes"][0]["value"]


@skip_if_remote
def test_bad_points(env: Environment):
    company_id = env.default_company.id
    path_import = f'/api/v1/companies/{company_id}/mvrp_task?task_id=mock_task_uuid__generic'
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    courier_id = env.default_courier.id
    route_id = routes[0]['id']

    timestamp = datetime.now().timestamp()
    resp = local_get(env.client,
                     f'/api/v1/couriers/{courier_id}/routes/{route_id}/routed-orders',
                     query={
                         'lat': 1,
                         'lon': 1,
                         'timestamp': timestamp
                     },
                     headers=env.user_auth_headers,
                     expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    assert 'Solver could not build 55% routes between provided locations.' in resp['message']


@skip_if_remote
def test_deleted_depot(env: Environment):
    with env.flask_app.app_context():
        set_dirty_level(env, env.default_route.id)

        route_selector = DirtyRouteSelector(60, 0, 1)
        routes = route_selector.get_routes_for_update()
        assert len(routes) == 1

        depot = db.session.query(Depot).filter(Depot.id == env.default_depot.id).first()
        depot.deleted = True
        db.session.commit()

        route_selector = DirtyRouteSelector(60, 0, 1)
        routes = route_selector.get_routes_for_update()
        assert len(routes) == 0


@skip_if_remote
def test_deleted_courier(env: Environment):
    with env.flask_app.app_context():
        set_dirty_level(env, env.default_route.id)

        route_selector = DirtyRouteSelector(60, 0, 1)
        routes = route_selector.get_routes_for_update()
        assert len(routes) == 1

        depot = db.session.query(Courier).filter(Depot.id == env.default_courier.id).first()
        depot.deleted = True
        db.session.commit()

        route_selector = DirtyRouteSelector(60, 0, 1)
        routes = route_selector.get_routes_for_update()
        assert len(routes) == 0
