from datetime import datetime, timedelta, time as dt_time
from freezegun import freeze_time
from dateutil.tz import gettz
import time
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_delete, local_post, local_get, local_patch, Environment
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_setting, set_company_import_depot_garage, update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import ROUTES_DATE

from ya_courier_backend.models import db
from ya_courier_backend.models.courier_idle_state import CourierIdleState
from ya_courier_backend.models.logistic_company import NodeIdleType, EtaType
from ya_courier_backend.util.position import Position

TEST_DATETIME = datetime.combine(ROUTES_DATE, dt_time(9), tzinfo=gettz('Europe/Moscow'))


def _get_courier_idle_state(env, route_id, courier_id):
    with env.flask_app.app_context():
        return CourierIdleState.get(route_id, courier_id)


@skip_if_remote
def test_courier_idle_db_deletion_on_route_delete(env: Environment):

    assert not _get_courier_idle_state(env, env.default_route.id, env.default_courier.id)

    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': 0}
    update_route(env, env.default_route.id, route_state_context)
    assert _get_courier_idle_state(env, env.default_route.id, env.default_courier.id)

    delete_path = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
    local_delete(env.client, delete_path, headers=env.user_auth_headers)
    assert not _get_courier_idle_state(env, env.default_route.id, env.default_courier.id)


@skip_if_remote
def test_courier_idle_db_deletion_on_courier_delete(env: Environment):
    assert not _get_courier_idle_state(env, env.default_route.id, env.default_courier.id)

    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': 0}
    update_route(env, env.default_route.id, route_state_context)
    assert _get_courier_idle_state(env, env.default_route.id, env.default_courier.id)

    courier_data = {
        'name':  'idle_test_courier_name',
        'number': 'idle_test_courier'
    }
    courier_add_path = f"/api/v1/companies/{env.default_company.id}/couriers"
    courier = local_post(env.client, courier_add_path, headers=env.user_auth_headers, data=courier_data)
    patch_route_path = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
    change_data = {'courier_id': courier['id']}
    local_patch(env.client, patch_route_path, headers=env.user_auth_headers, data=change_data)
    update_route(env, env.default_route.id, route_state_context)
    assert _get_courier_idle_state(env, env.default_route.id, courier['id'])

    courier_delete_path = f"/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}"
    local_delete(env.client, courier_delete_path, headers=env.user_auth_headers)
    assert not _get_courier_idle_state(env, env.default_route.id, env.default_courier.id)


@skip_if_remote
def test_courier_idle_db_unique_constraint(env: Environment):
    with env.flask_app.app_context():
        courier_id = env.default_courier.id
        route_id = env.default_route.id
        assert not CourierIdleState.get(route_id, courier_id)
        CourierIdleState.create(route_id, courier_id)
        db.session.commit()
        try:
            CourierIdleState.create(route_id, courier_id)
            db.session.commit()
        except Exception as ex:
            assert f'Key (route_id, courier_id)=({route_id}, {courier_id}) already exists' in str(ex)
            db.session.rollback()
        CourierIdleState.get(route_id, courier_id)


@skip_if_remote
def test_courier_idle_db_interaction_with_no_record_found(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)
    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route = local_post(env.client, import_path, headers=env.user_auth_headers)[0]
    route_id = route['id']
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']

    assert not _get_courier_idle_state(env, route_id, env.default_courier.id)

    route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': time}
    update_route(env, route_id, route_state_context)
    assert _get_courier_idle_state(env, route_id, env.default_courier.id)


@skip_if_remote
def test_courier_idle_state_update(env: Environment):
    # 0. Import a route
    set_company_import_depot_garage(env, env.default_company.id, True)
    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route = local_post(env.client, import_path, headers=env.user_auth_headers)[0]
    route_id = route['id']
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route_id}"
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)

    now = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    start_datetime = datetime.fromtimestamp(now).astimezone(gettz('Europe/Moscow'))
    assert not courier_idle_state
    with freeze_time(start_datetime) as freezed_time:
        now = time.time()
        # 1. Stay at the depot for some time
        locations = [(55.7447, 37.6727, now), (55.7447, 37.6727, now + 15 * 60), (55.7447, 37.6727, now + 30 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now + 30 * 60}
        update_route(env, route_id, route_state_context)
        courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)

        assert courier_idle_state
        assert not courier_idle_state.is_idle
        assert courier_idle_state.route_id == route_id
        assert courier_idle_state.courier_id == env.default_courier.id
        assert courier_idle_state.last_window_finish_timestamp == now + 30 * 60

        # 2. Move to another position and stay here for some time
        freezed_time.tick(delta=timedelta(minutes=45))
        now = time.time()
        locations = [(58.82, 37.73, now + 15 * 60), (58.82, 37.73, now + 30 * 60), (58.82, 37.73, now + 45 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 58.82, 'lon': 38.73, 'timestamp': now + 45 * 60}
        update_route(env, route_id, route_state_context)
        courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)

        assert courier_idle_state.is_idle
        assert courier_idle_state.last_window_finish_timestamp == now + 45 * 60
        assert courier_idle_state.idle_position == Position(37.73, 58.82, now + 15 * 60)

        # 3. Stay at the same location to check if idle start time didn't change
        freezed_time.tick(delta=timedelta(minutes=45))
        now = time.time()
        locations = [(58.82, 37.73, now + 15 * 60), (58.82, 37.73, now + 30 * 60), (58.82, 37.73, now + 45 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 58.82, 'lon': 38.73, 'timestamp': now + 45 * 60}
        update_route(env, route_id, route_state_context)
        courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)

        assert courier_idle_state.is_idle
        assert courier_idle_state.last_window_finish_timestamp == now + 45 * 60
        assert courier_idle_state.idle_position == Position(37.73, 58.82, now - 30 * 60)

        # 4. Continue to move
        freezed_time.tick(delta=timedelta(minutes=45))
        now = time.time()
        locations = [(58.82, 37.76, now + 15 * 60), (58.82, 37.77, now + 30 * 60), (58.82, 37.78, now + 45 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 58.82, 'lon': 38.78, 'timestamp': now + 45 * 60}
        update_route(env, route_id, route_state_context)
        courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)

        assert not courier_idle_state.is_idle

        # 5. Move to order location and stay for some time. Order will be marked as visited automatically
        freezed_time.tick(delta=timedelta(minutes=45))
        now = time.time()
        locations = [(55.826326, 37.637686, now + 15 * 60), (55.826326, 37.637686, now + 25 * 60), (55.826326, 35.637686, now + 45 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 55.826326, 'lon': 37.637686, 'timestamp': now + 35 * 60}
        update_route(env, route_id, route_state_context)
        courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)

        assert not courier_idle_state.is_idle

        # 6. Mark next order as visited, move to it and stay for some time
        courier_id = route["courier_id"]
        order_id = route_info['nodes'][2]['value']['id']
        path_patch = f'/api/v1/couriers/{courier_id}/routes/{route_id}/orders/{order_id}'
        local_patch(env.client, path_patch, headers=env.user_auth_headers, data={'status': 'finished'})

        freezed_time.tick(delta=timedelta(minutes=35))
        now = time.time()
        locations = [(55.8185462, 37.66126693, now + 15 * 60), (55.8185462, 37.66126693, now + 30 * 60), (55.8185462, 37.66126693, now + 45 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 55.8185462, 'lon': 37.66126693, 'timestamp': now + 45 * 60}
        update_route(env, route_id, route_state_context)
        courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)

        assert courier_idle_state.is_idle

        path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?" \
                               f"date={start_datetime.date()}&types=depot,order,garage"
        courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)
        # No idles for the first depot
        assert courier_quality[0]['transit_idle_duration'] == 0
        assert courier_quality[0]['location_idle_duration'] == 0
        # Idle for the first order 2 hours in transit and 0 at location
        assert courier_quality[1]['transit_idle_duration'] == 7200
        assert courier_quality[1]['location_idle_duration'] == 0
        # No idles for the second order
        assert courier_quality[2]['transit_idle_duration'] == 0
        assert courier_quality[2]['location_idle_duration'] == 0
        # Depot in the end is not visited
        assert courier_quality[3]['transit_idle_duration'] is None
        assert courier_quality[3]['location_idle_duration'] is None


@skip_if_remote
@freeze_time(TEST_DATETIME)
def test_idle_is_started_from_time_window_start_when_idle_type_is_time_window_end(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, False)
    set_company_setting(env, env.default_company.id, "node_idle_type", NodeIdleType.time_window_end)

    task_id = 'mock_task_uuid__generic'
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]['id']

    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    now = TEST_DATETIME.timestamp()
    time_window_end = now + 5 * 60 * 60
    locations = [
        (55.826326, 37.637686, now),
        (55.826326, 37.637686, now + 15 * 60),
        (55.826326, 37.637686, now + 30 * 60),
        (55.826326, 37.637686, time_window_end),
        (55.826326, 37.637686, time_window_end + 15 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 55.826326, "lon": 37.637686, "timestamp": time_window_end + 15 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert not courier_idle_state.is_idle

    locations = [(55.826326, 37.637686, time_window_end + 30 * 60)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 55.826326, "lon": 37.637686, "timestamp": time_window_end + 30 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert courier_idle_state.is_idle
    assert courier_idle_state.last_window_finish_timestamp == time_window_end + 30 * 60
    assert courier_idle_state.idle_position == Position(37.637686, 55.826326, time_window_end)


@skip_if_remote
@pytest.mark.parametrize("eta_type", [EtaType.arrival_time, EtaType.delivery_time])
@pytest.mark.parametrize("mark_delivered", [True, False])
@freeze_time(TEST_DATETIME)
def test_idle_is_started_from_real_visit_time_regardless_of_mark_delivered_option(
    env: Environment, eta_type, mark_delivered
):
    set_company_import_depot_garage(env, env.default_company.id, False)
    set_company_setting(env, env.default_company.id, "eta_type", eta_type)
    set_company_setting(env, env.default_company.id, "mark_delivered_enabled", mark_delivered)

    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]["id"]

    # Set order service duration to 10min
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    order_id = route_info["nodes"][0]["value"]["id"]
    path_patch = f"/api/v1/companies/{env.default_company.id}/orders/{order_id}"
    local_patch(env.client, path_patch, headers=env.user_auth_headers, data={"service_duration_s": 10 * 60})

    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    time_window_start = TEST_DATETIME.timestamp()
    locations = [
        (55.826326, 37.637686, time_window_start - 5 * 60),
        (55.826326, 37.637686, time_window_start),
        (55.826326, 37.637686, time_window_start + 5 * 60),
        (55.826326, 37.637686, time_window_start + 10 * 60),
        (55.826326, 37.637686, time_window_start + 20 * 60),
        (55.826326, 37.637686, time_window_start + 35 * 60),
        (55.826326, 37.637686, time_window_start + 40 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 55.826326, "lon": 37.637686, "timestamp": time_window_start + 40 * 60}
    update_route(env, route_id, route_state_context)

    start = time_window_start + (5 * 60 if eta_type == EtaType.arrival_time else 10 * 60)
    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert courier_idle_state.is_idle
    assert courier_idle_state.last_window_finish_timestamp == time_window_start + 40 * 60
    assert courier_idle_state.idle_position == Position(37.637686, 55.826326, start)


@skip_if_remote
@pytest.mark.parametrize("mark_delivered", [True, False])
@freeze_time(TEST_DATETIME)
def test_idle_is_started_from_visit_time_if_its_greater_than_time_window_end(env: Environment, mark_delivered):
    set_company_import_depot_garage(env, env.default_company.id, False)
    set_company_setting(env, env.default_company.id, "node_idle_type", NodeIdleType.time_window_end)
    set_company_setting(env, env.default_company.id, "mark_delivered_enabled", mark_delivered)

    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]["id"]

    # Set order service duration to 20min for visit to take 10min
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    order_id = route_info["nodes"][0]["value"]["id"]
    path_patch = f"/api/v1/companies/{env.default_company.id}/orders/{order_id}"
    local_patch(env.client, path_patch, headers=env.user_auth_headers, data={"service_duration_s": 20 * 60})

    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    time_window_end = TEST_DATETIME.timestamp() + 5 * 60 * 60
    locations = [
        (55.826326, 37.637686, time_window_end - 5 * 60),
        (55.826326, 37.637686, time_window_end),
        (55.826326, 37.637686, time_window_end + 5 * 60),
        (55.826326, 37.637686, time_window_end + 6 * 60),
        (55.826326, 37.637686, time_window_end + 15 * 60),
        (55.826326, 37.637686, time_window_end + 25 * 60),
        (55.826326, 37.637686, time_window_end + 45 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 55.826326, "lon": 37.637686, "timestamp": time_window_end + 45 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert courier_idle_state.is_idle
    assert courier_idle_state.last_window_finish_timestamp == time_window_end + 45 * 60
    assert courier_idle_state.idle_position == Position(37.637686, 55.826326, time_window_end + 5 * 60)


@skip_if_remote
@freeze_time(TEST_DATETIME)
def test_idle_is_started_after_order_departure(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, False)
    set_company_setting(env, env.default_company.id, "node_idle_type", NodeIdleType.time_window_end)

    task_id = 'mock_task_uuid__generic'
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]['id']

    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    now = TEST_DATETIME.timestamp()
    locations = [
        (55.826326, 37.637686, now),
        (55.826326, 37.637686, now + 15 * 60),
        (55.826326, 37.637686, now + 30 * 60),
        (58.82, 37.73, now + 45 * 60),
        (58.82, 37.73, now + 60 * 60),
        (58.82, 37.73, now + 75 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 58.82, "lon": 37.73, "timestamp": now + 75 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert courier_idle_state.is_idle
    assert courier_idle_state.last_window_finish_timestamp == now + 75 * 60
    assert courier_idle_state.idle_position == Position(37.73, 58.82, now + 45 * 60)


@skip_if_remote
@freeze_time(TEST_DATETIME)
def test_idle_is_not_started_when_courier_visiting_the_order(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, False)

    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]["id"]

    # Set order service duration to 4h, so that real is 2h
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route_id}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    order_id = route_info["nodes"][0]["value"]["id"]
    path_patch = f"/api/v1/companies/{env.default_company.id}/orders/{order_id}"
    local_patch(env.client, path_patch, headers=env.user_auth_headers, data={"service_duration_s": 4 * 60 * 60})

    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    time_window_start = TEST_DATETIME.timestamp()
    locations = [
        (55.826326, 37.637686, time_window_start),
        (55.826326, 37.637686, time_window_start + 30 * 60),
        (55.826326, 37.637686, time_window_start + 60 * 60),
        (55.826326, 37.637686, time_window_start + 90 * 60),
        (55.826326, 37.637686, time_window_start + 110 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 55.826326, "lon": 37.637686, "timestamp": time_window_start + 110 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert not courier_idle_state.is_idle


@skip_if_remote
@freeze_time(TEST_DATETIME)
def test_idle_is_not_started_when_courier_visiting_the_depot(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]["id"]

    # Set depot service duration to 4h, so that real is 2h
    path_patch = f"/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}"
    local_patch(env.client, path_patch, headers=env.user_auth_headers, data={"service_duration_s": 4 * 60 * 60})

    # Visit event is not yet set for depot
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    time_window_start = TEST_DATETIME.timestamp()
    locations = [
        (55.7447, 37.6727, time_window_start),
        (55.7447, 37.6727, time_window_start + 30 * 60),
        (55.7447, 37.6727, time_window_start + 60 * 60),
        (55.7447, 37.6727, time_window_start + 90 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 55.7447, "lon": 37.6727, "timestamp": time_window_start + 110 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert not courier_idle_state.is_idle

    # Visit event is set, but service duration is still going
    locations = [
        (55.7447, 37.6727, time_window_start + 110 * 60),
        (55.7447, 37.6727, time_window_start + 120 * 60),
        (55.7447, 37.6727, time_window_start + 140 * 60),
        (55.7447, 37.6727, time_window_start + 150 * 60),
        (55.7447, 37.6727, time_window_start + 180 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 55.7447, "lon": 37.6727, "timestamp": time_window_start + 180 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert not courier_idle_state.is_idle

    # Service duration is spent
    service_duration_end = time_window_start + 4 * 60 * 60
    locations = [
        (55.7447, 37.6727, service_duration_end - 10 * 60),
        (55.7447, 37.6727, service_duration_end),
        (55.7447, 37.6727, service_duration_end + 10 * 60),
        (55.7447, 37.6727, service_duration_end + 20 * 60),
        (55.7447, 37.6727, service_duration_end + 30 * 60),
        (55.7447, 37.6727, service_duration_end + 40 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 55.7447, "lon": 37.6727, "timestamp": service_duration_end + 40 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert courier_idle_state.is_idle
    assert courier_idle_state.last_window_finish_timestamp == service_duration_end + 40 * 60
    assert courier_idle_state.idle_position == Position(37.6727, 55.7447, service_duration_end)


@skip_if_remote
@freeze_time(TEST_DATETIME)
def test_idle_is_started_after_depot_departure(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]["id"]

    # Set depot service duration to 1h, so that real is 30min
    path_patch = f"/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}"
    local_patch(env.client, path_patch, headers=env.user_auth_headers, data={"service_duration_s": 1 * 60 * 60})

    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    now = TEST_DATETIME.timestamp()
    locations = [
        (55.7447, 37.6727, now),
        (55.7447, 37.6727, now + 15 * 60),
        (55.7447, 37.6727, now + 30 * 60),
        (58.82, 37.73, now + 45 * 60),
        (58.82, 37.73, now + 60 * 60),
        (58.82, 37.73, now + 75 * 60),
    ]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    route_state_context = {"lat": 58.82, "lon": 37.73, "timestamp": now + 75 * 60}
    update_route(env, route_id, route_state_context)

    courier_idle_state = _get_courier_idle_state(env, route_id, env.default_courier.id)
    assert courier_idle_state.is_idle
    assert courier_idle_state.last_window_finish_timestamp == now + 75 * 60
    assert courier_idle_state.idle_position == Position(37.73, 58.82, now + 45 * 60)
