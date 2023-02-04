from datetime import datetime
from http import HTTPStatus

import dateutil.tz
import pytest

import maps.b2bgeo.test_lib.apikey_values as apikey_values
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_patch, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data


TEST_TIMEZONE = 'Europe/Moscow'


def _patch_courier_position_sources(env: Environment, company_allowed_sources, courier_allowed_sources):
    company_path = f'/api/v1/companies/{env.default_company.id}'
    data = {
        'allowed_courier_position_sources': company_allowed_sources
    }
    local_patch(env.client, company_path, data=data, headers=env.user_auth_headers)
    j = local_get(env.client, company_path, headers=env.user_auth_headers)
    assert j['allowed_courier_position_sources'] == company_allowed_sources

    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    data = {
        'allowed_courier_position_sources': courier_allowed_sources
    }
    local_patch(env.client, courier_path, data=data, headers=env.user_auth_headers)
    j = local_get(env.client, courier_path, headers=env.user_auth_headers)
    assert j['allowed_courier_position_sources'] == courier_allowed_sources


def _create_route(env: Environment):
    task_id = "mock_task_uuid__ongoing_route"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, import_path, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    pos_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']

    return route['id'], pos_time


def _check_position_recorded(env: Environment, route_id, recorded):
    path_positions = f"/api/v1/companies/{env.default_company.id}/courier-position/{env.default_courier.id}/routes/{route_id}"
    positions = local_get(env.client, path_positions, headers=env.user_auth_headers)
    assert bool(positions) is recorded


@skip_if_remote
@pytest.mark.parametrize('company_allowed_sources, courier_allowed_sources, recorded',
                         [
                             [['app'], None, False],
                             [['app', 'gps_tracker'], None, True],
                             [[], ['s2s_api', 'gps_tracker'], True],
                             [['s2s_api', 'app'], ['s2s_api'], False]
                         ])
def test_gps_tracker_position_source(env: Environment, company_allowed_sources, courier_allowed_sources, recorded):
    _patch_courier_position_sources(env, company_allowed_sources, courier_allowed_sources)

    route_id, pos_time = _create_route(env)

    path_route = f'/api/v1/companies/{env.default_company.id}/routes/{route_id}'
    imei = 1234567890
    local_patch(env.client, path_route, headers=env.user_auth_headers, data={'imei': imei})

    path_push = f'/api/v1/gps-trackers/{imei}/push-positions'
    locations = [(55.77, 37.63,
                  datetime.fromtimestamp(pos_time + 50).replace(tzinfo=dateutil.tz.gettz(TEST_TIMEZONE)).isoformat())]

    local_post(env.client, path_push, headers=env.superuser_auth_headers, data=prepare_push_positions_data(locations))

    _check_position_recorded(env, route_id, recorded)


@skip_if_remote
@pytest.mark.parametrize('company_allowed_sources, courier_allowed_sources, recorded',
                         [
                             [['s2s_api'], None, False],
                             [['app', 'gps_tracker'], None, True],
                             [['gps_tracker'], ['s2s_api', 'app'], True],
                             [['s2s_api', 'app'], ['s2s_api', 'gps_tracker'], False]
                         ])
def test_app_position_source(env: Environment, company_allowed_sources, courier_allowed_sources, recorded):
    _patch_courier_position_sources(env, company_allowed_sources, courier_allowed_sources)

    route_id, pos_time = _create_route(env)

    path_push = f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions'
    locations = [(55.82, 37.63, pos_time)]
    expected_status = HTTPStatus.OK if recorded else HTTPStatus.UNAUTHORIZED
    local_post(env.client, path_push, headers=env.user_auth_headers,
               data=prepare_push_positions_data(locations), expected_status=expected_status)

    _check_position_recorded(env, route_id, recorded)


@pytest.mark.parametrize('company_allowed_sources, courier_allowed_sources, recorded',
                         [
                             [['s2s_api'], None, False],
                             [['app', 'gps_tracker'], None, True],
                             [['gps_tracker'], ['s2s_api', 'app'], True],
                             [['s2s_api', 'app'], [], False]
                         ])
def test_app_position_source_v2(env: Environment, company_allowed_sources, courier_allowed_sources, recorded):
    _patch_courier_position_sources(env, company_allowed_sources, courier_allowed_sources)

    route_id, pos_time = _create_route(env)

    path_push = f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions-v2'
    data_v2 = {
        'positions': [
            {
                'coords': {
                    'accuracy': 10,
                    'latitude': 55.82,
                    'longitude': 37.63
                },
                'timestampMeta': {
                    'systemTime': pos_time * 1000
                }
            }
        ]
    }
    expected_status = HTTPStatus.OK if recorded else HTTPStatus.UNAUTHORIZED
    local_post(env.client, path_push, headers=env.user_auth_headers, data=data_v2, expected_status=expected_status)

    _check_position_recorded(env, route_id, recorded)


@skip_if_remote
@pytest.mark.parametrize('company_allowed_sources, courier_allowed_sources,  recorded',
                         [
                             [['gps_tracker'], None, False],
                             [['s2s_api', 'gps_tracker'], None, True],
                             [['gps_tracker'], ['s2s_api', 'app'], True],
                             [['s2s_api'], [], False]
                         ])
def test_s2s_position_source(env: Environment, company_allowed_sources, courier_allowed_sources, recorded):
    _patch_courier_position_sources(env, company_allowed_sources, courier_allowed_sources)

    route_id, pos_time = _create_route(env)

    path_push_v3 = f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions-v3?apikey={apikey_values.ACTIVE}'
    data_v3 = {
        'positions': [
            {
                'accuracy': 15,
                'point': {'lat': 55.77, 'lon': 37.63},
                'timestamp': pos_time
            }
        ]
    }
    expected_status = HTTPStatus.OK if recorded else HTTPStatus.UNAUTHORIZED
    local_post(env.client, path_push_v3, headers=env.user_auth_headers, data=data_v3, expected_status=expected_status)

    _check_position_recorded(env, route_id, recorded)


@skip_if_remote
@pytest.mark.parametrize('company_allowed_sources, courier_allowed_sources,  recorded',
                         [
                             [['gps_tracker'], None, False],
                             [['s2s_api', 'yanavi'], None, True],
                             [['gps_tracker'], ['yanavi'], True],
                         ])
def test_navi_position_source(env: Environment, company_allowed_sources, courier_allowed_sources, recorded):
    _patch_courier_position_sources(env, company_allowed_sources, courier_allowed_sources)

    route_id, pos_time = _create_route(env)

    path_push = f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions?source=navi-ch'
    locations = [(55.82, 37.63, pos_time)]
    expected_status = HTTPStatus.OK if recorded else HTTPStatus.UNAUTHORIZED
    local_post(env.client, path_push, headers=env.user_auth_headers,
               data=prepare_push_positions_data(locations), expected_status=expected_status)

    _check_position_recorded(env, route_id, recorded)
