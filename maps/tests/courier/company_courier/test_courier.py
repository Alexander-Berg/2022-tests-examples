import pytest

from http import HTTPStatus

from ya_courier_backend.models import db, Courier
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_delete, local_get, local_post, local_patch


def _get_route(env, route_number, depot_number):
    return {
        'number': route_number,
        'courier_number': env.default_courier.number,
        'depot_number': depot_number,
        'date': '2020-11-30',
    }


def _create_route(env, route_number, depot_number):
    path_route = f"/api/v1/companies/{env.default_company.id}/routes"
    route_data = _get_route(env, route_number, depot_number)
    route = local_post(env.client, path_route, headers=env.user_auth_headers, data=route_data)

    return route


@skip_if_remote
def test_courier_shallow_delete(env: Environment):
    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    couriers_path = f'/api/v1/companies/{env.default_company.id}/couriers'

    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    local_get(env.client, courier_path, headers=env.user_auth_headers, expected_status=HTTPStatus.GONE)
    resp = local_get(env.client, couriers_path, headers=env.user_auth_headers)
    assert resp == []


@skip_if_remote
def test_courier_shallow_delete_wrong(env: Environment):
    non_existent_courier_id = 100500
    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{non_existent_courier_id}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers,
                 expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_courier_shallow_delete_twice(env: Environment):
    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    local_get(env.client, courier_path, headers=env.user_auth_headers, expected_status=HTTPStatus.GONE)
    add_courier_path = f'/api/v1/companies/{env.default_company.id}/couriers'
    resp = local_post(env.client, add_courier_path, headers=env.user_auth_headers,
                      data={'number': env.default_courier.number})
    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{resp["id"]}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)
    local_get(env.client, courier_path, headers=env.user_auth_headers, expected_status=HTTPStatus.GONE)


@skip_if_remote
def test_courier_shallow_delete_re_add(env: Environment):
    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    add_courier_path = f'/api/v1/companies/{env.default_company.id}/couriers'
    resp = local_post(env.client, add_courier_path, headers=env.user_auth_headers,
                      data={'number': env.default_courier.number})
    assert resp['id'] != env.default_courier.id


@skip_if_remote
def test_courier_shallow_delete_re_add_batch(env: Environment):
    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    add_couriers_batch_path = f'/api/v1/companies/{env.default_company.id}/couriers-batch'
    resp = local_post(env.client, add_couriers_batch_path, headers=env.user_auth_headers,
                      data=[{'number': env.default_courier.number}])
    assert resp == {'inserted': 1, 'updated': 0}


@skip_if_remote
def test_patch_route_with_courier_number(env: Environment):
    route = _create_route(env, '1', env.default_depot.number)

    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    add_courier_path = f'/api/v1/companies/{env.default_company.id}/couriers'
    new_courier = local_post(env.client, add_courier_path, headers=env.user_auth_headers,
                             data={'number': env.default_courier.number})

    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    route = local_patch(env.client, path_route, headers=env.user_auth_headers,
                        data={'courier_number': env.default_courier.number})

    assert route['courier_id'] == new_courier['id']


@skip_if_remote
def test_courier_quality_for_deleted_courier(env: Environment):
    # Import route with orders, depot and garage in the end
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    # Get courier-quality
    date = '2019-12-13'
    path_courier_quality = f"/api/v1/companies/{env.default_company.id}/courier-quality?date={date}"
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    assert courier_quality != []

    # Delete courier
    courier_id = routes[0]['courier_id']
    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{courier_id}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    # Get courier-quality again
    courier_quality = local_get(env.client, path_courier_quality, headers=env.user_auth_headers)

    assert courier_quality == []


@skip_if_remote
@pytest.mark.parametrize(('locale', 'db_locale'), [
    ('ru_RU', 'ru_RU'),
    ('ru-RU', 'ru_RU'),
    ('zzzzz', 'zzzzz'),
    (None, None),
])
def test_locale(env: Environment, locale, db_locale):
    path_settings = f"/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}/settings"
    local_post(env.client, path_settings, headers=env.user_auth_headers, data={'locale': locale})

    with env.flask_app.app_context():
        courier = db.session.query(Courier).filter(Courier.id == env.default_courier.id).first()
        assert courier.locale == db_locale
