from datetime import datetime, timedelta
from dateutil.parser import parse
from http import HTTPStatus
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
from ya_courier_backend.models import db, RouteState
from ya_courier_backend.tasks.update_route_state import update_route_states


def _push_position(env, courier_id, route_id, locations, expected_pos_count):
    path_push_positions = f"/api/v1/couriers/{courier_id}/routes/{route_id}/push-positions"
    local_post(env.client, path_push_positions, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    path_positions = f"/api/v1/companies/{env.default_company.id}/courier-position/{courier_id}/routes/{route_id}"
    positions = local_get(env.client, path_positions, headers=env.user_auth_headers)
    assert len(positions) == expected_pos_count


@skip_if_remote
def test_tracking_start(env: Environment):
    # 0. Make default company not to import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, False)

    # 0. Import a route
    company_id = env.default_company.id
    task_id = "mock_task_uuid__ongoing_route"
    path_import = f"/api/v1/companies/{company_id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    cur_time = datetime.now()
    courier_id = env.default_courier.id
    route_id = routes[0]['id']

    # 1. Push position in [route_start - tracking_start_h, route_start) interval. tracking_start_h = 1 by default
    path_route = f"/api/v1/companies/{company_id}/routes/{route_id}"
    route = local_get(env.client, path_route, headers=env.user_auth_headers)
    assert route["route_start"] == "07:00:00"

    time = f"{route['date']}T06:00:00+03:00"
    locations = [(55.801, 37.621, time)]
    _push_position(env, courier_id, route_id, locations, expected_pos_count=1)

    # 2. Make sure dirty_level is 0
    with env.flask_app.app_context():
        route_states = db.session.query(RouteState).filter(RouteState.route_id == route_id).all()
        assert len(route_states) == 1
        assert route_states[0].as_dict()["dirty_level"] == 0

    # 3. Push order position and make sure order was marked as delivered
    locations = [(55.826326, 37.637686, time)]
    _push_position(env, courier_id, route_id, locations, expected_pos_count=2)

    path_route_info = f"/api/v1/companies/{company_id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    assert "delivery_time" in route_info["nodes"][0]["value"]

    # 4. Force route state update
    timestamp = datetime.strptime(time, "%Y-%m-%dT%H:%M:%S%z").timestamp()
    local_get(env.client,
              f'/api/v1/couriers/{courier_id}/routes/{route_id}/routed-orders',
              query={
                  'lat': env.default_depot.lat,
                  'lon': env.default_depot.lon + 0.0001,
                  'timestamp': timestamp
              },
              headers=env.user_auth_headers)

    # 5. Make sure no notifications were created
    path_notifications = f"api/v1/companies/{company_id}/order-notifications?from={cur_time - timedelta(minutes=10)}&to={cur_time + timedelta(minutes=10)}"
    notifications = local_get(env.client, path_notifications, headers=env.user_auth_headers)
    assert notifications == []


@skip_if_remote
def test_tracking_start_nullable_route_start(env: Environment):
    # 0. Import a route
    company_id = env.default_company.id
    task_id = "mock_task_uuid__generic"
    path_import = f"/api/v1/companies/{company_id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    courier_id = env.default_courier.id
    route_id = routes[0]['id']

    # 1. Set tracking_start and remove route_start
    path_route = f"/api/v1/companies/{company_id}/routes/{route_id}"
    patch_data = {"route_start": None}
    local_patch(env.client, path_route, data=patch_data, headers=env.user_auth_headers)
    path_company = f"/api/v1/companies/{company_id}"
    patch_data = {"tracking_start_h": 5}
    local_patch(env.client, path_company, data=patch_data, headers=env.user_auth_headers)

    # 2. Position is not recorded earlier than min(time_windows.start) - 5h:
    locations = [(55.801, 37.621, "2019-12-13T03:00:00+03:00")]
    _push_position(env, courier_id, route_id, locations, expected_pos_count=0)


@skip_if_remote
def test_route_tracking_start(env: Environment):
    company_id = env.default_company.id
    task_id = "mock_task_uuid__generic"
    path_import = f"/api/v1/companies/{company_id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    courier_id = env.default_courier.id
    route_id = routes[0]['id']

    path_route = f"/api/v1/companies/{company_id}/routes/{route_id}"
    route = local_get(env.client, path_route, headers=env.user_auth_headers)
    assert route["route_start"] == "07:00:00"

    time = f"{route['date']}T05:00:00+03:00"
    locations = [(55.801, 37.621, time)]
    _push_position(env, courier_id, route_id, locations, expected_pos_count=0)

    patch_data = {"tracking_start_h": 2}
    path_company = f"/api/v1/companies/{company_id}"
    local_patch(env.client, path_company, data=patch_data, headers=env.user_auth_headers)
    _push_position(env, courier_id, route_id, locations, expected_pos_count=1)

    time = f"{route['date']}T04:00:00+03:00"
    locations = [(55.801, 37.621, time)]
    _push_position(env, courier_id, route_id, locations, expected_pos_count=1)

    patch_data = {"tracking_start_h": 4}
    local_patch(env.client, path_route, data=patch_data, headers=env.user_auth_headers)
    _push_position(env, courier_id, route_id, locations, expected_pos_count=2)


@skip_if_remote
@pytest.mark.parametrize("tracking_start_h", [25, -1, 5.5])
def test_invalid_tracking_start(env: Environment, tracking_start_h):
    path_company = f"/api/v1/companies/{env.default_company.id}"
    patch_data = {"tracking_start_h": tracking_start_h}
    local_patch(env.client, path_company, data=patch_data, headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_negative_tracking_start(env: Environment):
    # 0. Make default company not to import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, False)

    # 0. Import a route
    company_id = env.default_company.id
    task_id = "mock_task_uuid__ongoing_route"
    path_import = f"/api/v1/companies/{company_id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    courier_id = env.default_courier.id
    route_id = routes[0]['id']

    # 1. Set tracking_start_h = 24 and push position with time = route_start - tracking_start_h
    path_route = f"/api/v1/companies/{company_id}/routes/{route_id}"
    route = local_get(env.client, path_route, headers=env.user_auth_headers)
    assert route["route_start"] == "07:00:00"

    patch_data = {"tracking_start_h": 24}
    local_patch(env.client, path_route, data=patch_data, headers=env.user_auth_headers)

    # 2. Visit an order to set dirty_level = 2
    route_start = parse(f"{route['date']}T07:00:00+00:00")
    time = route_start - timedelta(days=1)
    locations = [(55.826326, 37.637686, time.isoformat())]
    _push_position(env, courier_id, route_id, locations, expected_pos_count=1)

    path_route_info = f"/api/v1/companies/{company_id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    assert "delivery_time" in route_info["nodes"][0]["value"]

    with env.flask_app.app_context():
        route_states = db.session.query(RouteState).filter(RouteState.route_id == route_id).all()
        assert len(route_states) == 1
        assert route_states[0].as_dict()["dirty_level"] == 2

    # 3. Force background route state update
    with env.flask_app.app_context():
        result = update_route_states({}, 300, 0, 1, 2)
        assert result["task_state"]["error_routes"] == {route_id, }

    with env.flask_app.app_context():
        route_states = db.session.query(RouteState).filter(RouteState.route_id == route_id).all()
        assert len(route_states) == 1
        assert route_states[0].as_dict()["dirty_level"] == 0

    # 4. Test /routed-orders
    local_get(env.client, f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/routed-orders',
              query={
                  'lat': env.default_depot.lat,
                  'lon': env.default_depot.lon,
                  'timestamp': time.timestamp()
              },
              headers=env.user_auth_headers,
              expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # 5. Test /predict-eta
    local_get(env.client, f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/predict-eta',
              query={
                  'lat': env.default_depot.lat,
                  'lon': env.default_depot.lon,
                  'time': time.isoformat(),
                  'find-optimal': True
              },
              headers=env.user_auth_headers,
              expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
