import pytest
from datetime import datetime, timedelta
from freezegun import freeze_time
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_delete,
    local_patch,
    local_post,
    local_get, create_empty_route, create_order,
    set_company_routes_max_prolongation, push_positions,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util import MOSCOW_TZ
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import update_route

from ya_courier_backend.tasks.prolong_route_finish_time import ProlongRouteFinishTimeTask
from ya_courier_backend.models import db, DepotInstance, Route


def _get_route(env, route_number, depot_number, custom_fields=None):
    route = {
        'number': route_number,
        'courier_number': env.default_courier.number,
        'depot_number': depot_number,
        'date': '2020-11-30',
    }
    if custom_fields is not None:
        route['custom_fields'] = custom_fields

    return route


def _get_depot_with_number(number):
    return {'number': number}


def _create_route(env, route_number, depot_number):
    route = create_empty_route(env, route_number, depot_number)

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [{'type': 'depot', 'value': _get_depot_with_number(depot_number)}]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    return route


def add_route_custom_field_to_company(env, custom_fields):
    data = {'route_custom_fields': custom_fields}
    path = f'/api/v1/companies/{env.default_company.id}'
    local_patch(env.client,
                path,
                headers=env.user_auth_headers,
                data=data)


@skip_if_remote()
@pytest.mark.parametrize('has_rented_courier', [True, False])
@pytest.mark.parametrize('has_imei', [True, False])
@pytest.mark.parametrize('batch', [True, False])
def test_one_of_imei_and_rented_courier(env, has_imei, has_rented_courier, batch):
    data = {
        "provider": "yandex_taxi_cargo",
        "order_id": "rented courier order",
    }
    path = f"/api/v1/companies/{env.default_company.id}/rented-couriers"
    rented_courier_id = local_post(env.client, path, headers=env.user_auth_headers, data=data)['id']

    route = _get_route(env, '1', env.default_depot.number)
    if has_rented_courier:
        route['rented_courier_id'] = rented_courier_id
    if has_imei:
        route['imei'] = 123
    path = f'/api/v1/companies/{env.default_company.id}/routes'
    if batch:
        path += '-batch'

    if has_rented_courier and has_imei:
        expected_status = HTTPStatus.UNPROCESSABLE_ENTITY
    else:
        expected_status = HTTPStatus.OK

    local_post(
        env.client,
        path,
        headers=env.user_auth_headers,
        data=[route] if batch else route,
        expected_status=expected_status
    )


@skip_if_remote()
@pytest.mark.parametrize('has_rented_courier', [True, False])
@pytest.mark.parametrize('has_imei', [True, False])
@pytest.mark.parametrize('batch', [True, False])
def test_one_of_imei_and_rented_courier_update(env, has_imei, has_rented_courier, batch):
    data = {
        "provider": "yandex_taxi_cargo",
        "order_id": "rented courier order",
    }
    path = f"/api/v1/companies/{env.default_company.id}/rented-couriers"
    rented_courier_id = local_post(env.client, path, headers=env.user_auth_headers, data=data)['id']

    route = _get_route(env, '1', env.default_depot.number)
    if has_imei:
        route['imei'] = 1234
    path = f'/api/v1/companies/{env.default_company.id}/routes'
    response = local_post(
        env.client,
        path,
        headers=env.user_auth_headers,
        data=route,
    )

    route_update = _get_route(env, '1', env.default_depot.number)
    if has_rented_courier:
        route_update['rented_courier_id'] = rented_courier_id
    if batch:
        path += '-batch'
        method = local_post
    else:
        path += '/{}'.format(response['id'])
        method = local_patch

    if has_rented_courier and has_imei:
        expected_status = HTTPStatus.UNPROCESSABLE_ENTITY
    else:
        expected_status = HTTPStatus.OK

    method(
        env.client,
        path,
        headers=env.user_auth_headers,
        data=[route_update] if batch else route_update,
        expected_status=expected_status
    )


@skip_if_remote
@pytest.mark.parametrize('rented_courier_id', ['1234', 'not a number'])
def test_invalid_rented_courier(env, rented_courier_id):
    route = _get_route(env, '1', env.default_depot.number)
    route.update({'rented_courier_id': rented_courier_id})
    path = f'/api/v1/companies/{env.default_company.id}/routes'
    local_post(
        env.client,
        path,
        headers=env.user_auth_headers,
        data=route,
        expected_status=HTTPStatus.NOT_FOUND,
    )


@skip_if_remote
def test_depot_instance_depot_id_is_updated_when_route_depot_id_is_patched(env: Environment):
    # 1. create a route with a single depot
    route = _create_route(env, '1', env.default_depot.number)

    # 2. patch route with different depot_id
    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    local_patch(env.client,
                path_route,
                headers=env.user_auth_headers,
                data={'depot_id': env.default_second_depot.id})

    with env.flask_app.app_context():
        depot_instances = DepotInstance.get()
        assert depot_instances[0].depot_id == env.default_second_depot.id


@skip_if_remote
def test_depot_instance_depot_id_is_updated_when_route_depot_number_is_patched(env: Environment):
    # 1. create a route with a single depot
    route = _create_route(env, '1', env.default_depot.number)

    # 2. patch route with different depot_id
    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    local_patch(env.client,
                path_route,
                headers=env.user_auth_headers,
                data={'depot_number': env.default_second_depot.number})

    with env.flask_app.app_context():
        depot_instances = DepotInstance.get()
        assert depot_instances[0].depot_id == env.default_second_depot.id


@skip_if_remote
def test_depot_instance_depot_id_is_updated_when_routes_batch_depot_id_is_patched(env: Environment):
    # 1. create multiple routes with a single depot
    route_first = _create_route(env, '1', env.default_depot.number)
    route_second = _create_route(env, '2', env.default_second_depot.number)

    # 2. post same routes with changes depot_id's
    path_routes = f"/api/v1/companies/{env.default_company.id}/routes-batch"
    data = [
        {
            'number': route_first['number'],
            'depot_id': env.default_second_depot.id
        },
        {
            'number': route_second['number'],
            'depot_id': env.default_depot.id
        }
    ]
    local_post(env.client,
               path_routes,
               headers=env.user_auth_headers,
               data=data)

    with env.flask_app.app_context():
        depot_instances = DepotInstance.get(order_by=[DepotInstance.route_id])
        assert depot_instances[0].depot_id == env.default_second_depot.id
        assert depot_instances[1].depot_id == env.default_depot.id


@skip_if_remote
def test_depot_instance_depot_id_is_updated_when_routes_batch_depot_number_is_patched(env: Environment):
    # 1. create multiple routes with a single depot
    route_first = _create_route(env, '1', env.default_depot.number)
    route_second = _create_route(env, '2', env.default_second_depot.number)

    # 2. post same routes with changes depot_id's
    path_routes = f"/api/v1/companies/{env.default_company.id}/routes-batch"
    data = [
        {
            'number': route_first['number'],
            'depot_number': env.default_second_depot.number
        },
        {
            'number': route_second['number'],
            'depot_number': env.default_depot.number
        }
    ]
    local_post(env.client,
               path_routes,
               headers=env.user_auth_headers,
               data=data)

    with env.flask_app.app_context():
        depot_instances = DepotInstance.get(order_by=[DepotInstance.route_id])
        assert depot_instances[0].depot_id == env.default_second_depot.id
        assert depot_instances[1].depot_id == env.default_depot.id


@skip_if_remote
def test_post_route_with_custom_fields(env: Environment):
    custom_fields = [
        {
            'key': 'key1',
            'value': 'value1'
        },
        {
            'key': 'key2',
            'value': 'value2'
        }
    ]
    add_route_custom_field_to_company(env, [x['key'] for x in custom_fields])
    route = create_empty_route(env, '1', env.default_depot.number, custom_fields=custom_fields)
    assert route['custom_fields'] == custom_fields


@skip_if_remote
def test_get_route_has_custom_fields(env: Environment):
    # 1. Post route with custom_fields
    custom_fields = [
        {
            'key': 'key1',
            'value': 'value1'
        },
        {
            'key': 'key2',
            'value': 'value2'
        }
    ]
    add_route_custom_field_to_company(env, [x['key'] for x in custom_fields])
    route = create_empty_route(env, '1', env.default_depot.number, custom_fields=custom_fields)

    # 2. Get route and check custom_fields is present
    path_routes = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    response = local_get(env.client, path_routes, headers=env.user_auth_headers)

    assert response['custom_fields'] == custom_fields


@skip_if_remote
def test_patch_route_custom_fields(env: Environment):
    # 1. Post route with custom_fields
    custom_fields = [
        {
            'key': 'key1',
            'value': 'value1'
        },
        {
            'key': 'key2',
            'value': 'value2'
        }
    ]
    new_custom_fields = [{
        'key': 'key3',
        'value': 'value3'
    }]
    all_custom_fields = [x['key'] for x in (custom_fields + new_custom_fields)]
    add_route_custom_field_to_company(env, all_custom_fields)
    route = create_empty_route(env, '1', env.default_depot.number, custom_fields=custom_fields)

    # 2. Get route and check custom_fields is present
    path_routes = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    patched_route = local_patch(env.client,
                                path_routes,
                                headers=env.user_auth_headers,
                                data={'custom_fields': new_custom_fields})

    assert patched_route['custom_fields'] == new_custom_fields


@skip_if_remote
def test_post_route_with_unknown_custom_fields_not_allowed(env: Environment):
    # Try to post unknown custom field; expect 422
    custom_fields = [
        {
            'key': 'key1',
            'value': 'value1'
        }
    ]
    response = create_empty_route(env, '1', env.default_depot.number,
                                   custom_fields=custom_fields,
                                   expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "Unkown custom fields in request: {'key1'}" in response['message']


@skip_if_remote
def test_patch_route_with_unknown_custom_fields_not_allowed(env: Environment):
    # 1. create an empty route without custom fields
    route = create_empty_route(env, '1', env.default_depot.number)

    # 2. patch the route with custom fields; expect 422
    custom_fields = [
        {
            'key': 'key1',
            'value': 'value1'
        }
    ]
    path_routes = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    response = local_patch(env.client,
                           path_routes,
                           headers=env.user_auth_headers,
                           data={'custom_fields': custom_fields},
                           expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "Unkown custom fields in request: {'key1'}" in response['message']


@skip_if_remote
def test_non_unique_custom_fields_are_not_allowed(env: Environment):
    # Try to post non-unique custom fields and expect 422
    custom_fields = [
        {
            'key': 'key1',
            'value': 'value1'
        },
        {
            'key': 'key1',
            'value': 'value1'
        }
    ]
    add_route_custom_field_to_company(env, [x['key'] for x in custom_fields])
    response = create_empty_route(env, '1', env.default_depot.number,
                                   custom_fields=custom_fields,
                                   expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "Non unique custom fields in request: {'key1'}" in response['message']


@skip_if_remote
def test_delete_route(env: Environment):
    # 1. create a route with a single depot
    route = _create_route(env, '1', env.default_depot.number)

    # 2. delete route
    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    local_delete(env.client,
                 path_route,
                 headers=env.user_auth_headers)

    # 3. check that route is deleted
    local_get(env.client,
              path_route,
              headers=env.user_auth_headers,
              expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_delete_route_with_order(env: Environment):
    # 1. create a route with a single order
    route = create_empty_route(env, '1', env.default_depot.number)

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [
        {
            'type': 'order',
            'value': {
                'number': 'default_order_number',
                'time_interval': '00:00-23:59',
                'address': 'some address',
                'lat': 55.791928,
                'lon': 37.841492,
            }
        },
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # 2. try to delete the route
    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    resp = local_delete(env.client,
                        path_route,
                        headers=env.user_auth_headers,
                        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert f"Cannot delete route with id={route['id']}" in resp['message']

    # 3. check that route is not deleted
    local_get(env.client,
              path_route,
              headers=env.user_auth_headers)


def test_route_task_options_field(env):
    route = create_empty_route(env, '1', env.default_depot.number)
    with env.flask_app.app_context():
        route_db = db.session.query(Route).filter(Route.id == route['id']).all()[0]

    assert route_db.task_options == {}


def test_tracking_finish_time_prolonged(env):
    """
    Test that tracking_finish_time and prolongation_hours_left are computed correctly
    when prolongation is enabled for the company.
    """

    PROLONG_HOURS = 3
    MAX_WINDOW_TIME = datetime(2020, 11, 30, 7, 15, 0, tzinfo=MOSCOW_TZ)
    FINISH_TIME = MAX_WINDOW_TIME + timedelta(hours=5)

    route = create_empty_route(env)
    set_company_routes_max_prolongation(env, hours=PROLONG_HOURS)

    create_order(env, number='order_1', route_id=route['id'], time_interval='06:00-07:15')

    # add courier position and run update_route so route-details works
    position_time = int(datetime(2020, 11, 30, 6, 20).timestamp())
    route_state_context = {'lat': 55.8185462, 'lon': 37.66126693, 'timestamp': position_time}
    positions = [tuple(route_state_context.values())]
    push_positions(env, route['id'], positions)
    update_route(env, route['id'], route_state_context)

    path_route_details = f"/api/v1/companies/{env.default_company.id}/route-details"

    with freeze_time(FINISH_TIME) as frozen_time:
        # 1. no route_finish, finish_time should be max_time_window + 5h
        # time = max_time_window + 5h
        # tracking_finish_time = max_time_window + 5h, hours_left = max_prolong
        route_details = local_get(env.client, path_route_details, headers=env.superuser_auth_headers,
                                  query={"date": route['date'], "route_id": route['id']})[0]
        assert route_details['tracking_finish_time'] == '2020-11-30T09:15:00+00:00'  # FINISH_TIME.isoformat(timespec=)
        assert route_details['prolongation_hours_left'] == PROLONG_HOURS

        # 2. time = max_window + 5h + 1h, prolonged_finish_time = finish_time + 1h, hours_left = max_prolong-1
        with env.flask_app.app_context():
            for _ in range(2):  # pass 1h
                task = ProlongRouteFinishTimeTask(env.flask_app)
                task.run({})
                frozen_time.tick(delta=timedelta(minutes=30))

        route_details = local_get(env.client, path_route_details, headers=env.superuser_auth_headers,
                                  query={"date": route['date'], "route_id": route['id']})[0]
        assert route_details['tracking_finish_time'] == '2020-11-30T10:15:00+00:00'
        assert route_details['prolongation_hours_left'] == PROLONG_HOURS - 1

        # 3. time = max_window + 5h + max_prolong, prolonged_finish_time = finish_time + max_prolong, hours_left = 0
        with env.flask_app.app_context():
            for _ in range((PROLONG_HOURS - 1) * 2):  # pass max_prolong_h - 1
                task = ProlongRouteFinishTimeTask(env.flask_app)
                task.run({})
                frozen_time.tick(delta=timedelta(minutes=30))

        route_details = local_get(env.client, path_route_details, headers=env.superuser_auth_headers,
                                  query={"date": route['date'], "route_id": route['id']})[0]
        assert route_details['tracking_finish_time'] == '2020-11-30T12:15:00+00:00'
        assert route_details['prolongation_hours_left'] is None

        # 4. check that after we go beyond max_route_finish_prolongation_h values don't change anymore
        with env.flask_app.app_context():
            for _ in range(1):  # pass 1 more hour
                task = ProlongRouteFinishTimeTask(env.flask_app)
                task.run({})
                frozen_time.tick(delta=timedelta(minutes=30))

        route_details = local_get(env.client, path_route_details, headers=env.superuser_auth_headers,
                                  query={"date": route['date'], "route_id": route['id']})[0]
        assert route_details['tracking_finish_time'] == '2020-11-30T12:15:00+00:00'
        assert route_details['prolongation_hours_left'] is None


def test_tracking_finish_time(env):
    """
    Test that tracking_finish_time and prolongation_hours_left are computed correctly
    when prolongation is disabled for the company.
    """

    # create a route with finish time
    route = create_empty_route(env)
    path = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}'
    data = {'route_start': '00:00:00', 'route_finish': '10:20:00'}
    local_patch(env.client, path, headers=env.user_auth_headers, data=data)

    create_order(env, number='order_1', route_id=route['id'], time_interval='06:00-07:15')

    # add courier position and run update_route so route-details works
    position_time = int(datetime(2020, 11, 30, 6, 20).timestamp())
    route_state_context = {'lat': 55.8185462, 'lon': 37.66126693, 'timestamp': position_time}
    positions = [tuple(route_state_context.values())]
    push_positions(env, route['id'], positions)
    update_route(env, route['id'], route_state_context)

    path_route_details = f"/api/v1/companies/{env.default_company.id}/route-details"

    response = local_get(env.client, path_route_details, headers=env.superuser_auth_headers, query={"date": route['date'], "route_id": route['id']})
    route = response[0]

    # check that tracking_finish_time is 10:20:00 Moscow time or 7:20 UTC
    assert route['tracking_finish_time'] == '2020-11-30T07:20:00+00:00'
    assert route['prolongation_hours_left'] is None
