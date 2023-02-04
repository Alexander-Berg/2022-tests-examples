import pytest
import requests
from datetime import date, datetime, timezone

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    create_env_with_users, generate_numbers, cleanup_state,
    api_path_with_company_id,
    env_get_request, env_patch_request, env_post_request, env_delete_request,
    create_courier, create_depot
)


def _perform_action(
        request_fn,
        system_env_with_db,
        users_env,
        path,
        data=None,
        manager_expected_status_code=requests.codes.forbidden,
        app_expected_status_code=requests.codes.forbidden,
        admin_expected_status_code=requests.codes.ok):
    args = [system_env_with_db, path]
    kwargs = {
        'auth': users_env['auth_role_manager']
    }
    if request_fn == env_post_request or request_fn == env_patch_request:
        kwargs['data'] = data

    # Trying perform action as manager
    response = request_fn(*args, **kwargs)
    assert response.status_code == manager_expected_status_code

    # Perform action as app
    kwargs['auth'] = users_env['auth_role_app']

    response = request_fn(*args, **kwargs)
    assert response.status_code == app_expected_status_code

    # Perform action as admin
    kwargs['auth'] = users_env['auth_role_admin']

    response = request_fn(*args, **kwargs)
    assert response.status_code == admin_expected_status_code

    return response.json() if response.status_code == requests.codes.ok else None


@pytest.mark.parametrize(
    "role",
    [
        ("auth_role_app", requests.codes.forbidden),
        ("auth_role_dispatcher", requests.codes.forbidden),
        ("auth_role_manager", requests.codes.ok),
        ("auth_role_admin", requests.codes.ok),
    ],
)
def test_courier_handlers(system_env_with_db, role):
    with create_env_with_users(system_env_with_db) as users_env:
        auth_key, expected_code = role
        auth = users_env[auth_key]

        courier_num = 'test_courier_handlers_courier'
        cleanup_state(system_env_with_db, courier_num, None, None)
        courier_data = {
            'name': courier_num + '_name',
            'number': courier_num
        }

        # Create courier
        path = api_path_with_company_id(system_env_with_db, "couriers")
        response = env_post_request(system_env_with_db, path, courier_data, auth=auth)
        assert response.status_code == expected_code
        courier = response.json()

        # Batch update orders
        path = api_path_with_company_id(system_env_with_db, "couriers-batch")
        response = env_post_request(system_env_with_db, path, [courier_data], auth=auth)
        assert response.status_code == expected_code

        # Delete courier
        path=api_path_with_company_id(system_env_with_db, 'couriers/{}'.format(courier.get('id', '1')))
        response = env_delete_request(system_env_with_db, path, auth=auth)
        assert response.status_code == expected_code
        deleted_courier = response.json()

        assert courier == deleted_courier


def test_depot_handlers(system_env_with_db):
    with create_env_with_users(system_env_with_db) as users_env:
        depot_num = 'test_depot_handlers_depot'
        cleanup_state(system_env_with_db, None, None, depot_num)
        depot_data = {
            'number': depot_num,
            'name': 'Склад 1',
            'address': 'ул. Льва Толстого, 16',
            'time_interval': '0.00:00-23:59',
            'lat': 55.7447,
            'lon': 37.6727,
            'description': 'курьерский подъезд',
            'service_duration_s': 600,
            'order_service_duration_s': 10
        }

        # Create depot
        depot = _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'depots'),
            data=depot_data
        )

        # Batch update depots
        _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'depots-batch'),
            data=[depot_data]
        )

        depot_path = api_path_with_company_id(system_env_with_db, 'depots/{}'.format(depot['id']))

        # Get depot
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=depot_path,
            app_expected_status_code=requests.codes.ok
        )

        depot_patch = {
            'name': 'Склад 2',
            'allow_route_editing': True,
        }

        # Patch depot
        _perform_action(
            env_patch_request,
            system_env_with_db,
            users_env,
            path=depot_path,
            data=depot_patch
        )

        # Delete depot
        _perform_action(
            env_delete_request,
            system_env_with_db,
            users_env,
            path=depot_path
        )


def test_route_and_order_handlers(system_env_with_db):
    with create_env_with_users(system_env_with_db) as users_env:
        courier_num, depot_num, route_num, order_num = generate_numbers('test_order')
        cleanup_state(system_env_with_db, courier_num, route_num, depot_num)

        route_env = {
            'courier': create_courier(system_env_with_db, courier_num),
            'depot': create_depot(system_env_with_db, depot_num)
        }

        current_date = date.today().isoformat()

        route_data = {
            'courier_id': route_env['courier']['id'],
            'depot_id': route_env['depot']['id'],
            'date': current_date,
            'number': route_num
        }

        # Create route
        route_env['route'] = _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'routes'),
            data=route_data
        )

        # Batch update orders
        _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'routes-batch'),
            data=[route_data]
        )

        route_path = api_path_with_company_id(system_env_with_db, 'routes/{}'.format(route_env['route']['id']))

        # Get route
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path='routes?number={}'.format(route_env['route']['number'])
        )

        route_patch = {
            'imei': 11111111111111111
        }

        # Patch route
        _perform_action(
            env_patch_request,
            system_env_with_db,
            users_env,
            path=route_path,
            data=route_patch
        )

        route_state_path = api_path_with_company_id(
            system_env_with_db,
            'route-state?courier_number={}&date={}'.format(route_env['courier']['number'],
                                                           current_date)
        )
        # Get route_state
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=route_state_path
        )

        route_states_path = api_path_with_company_id(
            system_env_with_db,
            'route-states?date={}'.format(current_date)
        )
        # Get route states
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=route_states_path
        )

        route_history_path = api_path_with_company_id(
            system_env_with_db,
            'route-history?courier_number={}&date={}'.format(route_env['courier']['number'],
                                                             current_date)
        )
        # Get route history
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=route_history_path
        )

        order_data = {
            'address': 'Leo Tolstoy Str, 16',
            'amount': 1,
            'comments': 'Do not call',
            'customer_name': 'golovasteek',
            'description': 'Test order',
            'lat': 55.733827,
            'lon': 37.588722,
            'number': order_num,
            'payment_type': 'cash',
            'phone': '+79161111111',
            'route_id': route_env['route']['id'],
            'service_duration_s': 300,
            'time_interval': '00:00-1.01:00',
            'volume': 1,
            'weight': 1
        }

        # Create order
        order = _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'orders'),
            data=order_data
        )
        route_env['orders'] = [order]

        # Get orders
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'orders'),
            manager_expected_status_code=requests.codes.ok,
            app_expected_status_code=requests.codes.ok
        )

        # Batch update orders
        _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'orders-batch'),
            data=[order_data]
        )

        order_path = api_path_with_company_id(system_env_with_db, 'orders/{}'.format(route_env['orders'][0]['id']))

        # Get order
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=order_path
        )

        # Get track ids
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=order_path + '/track-ids'
        )

        # Delete order
        _perform_action(
            env_delete_request,
            system_env_with_db,
            users_env,
            path=order_path
        )

        _perform_action(
            env_delete_request,
            system_env_with_db,
            users_env,
            path=route_path
        )

        cleanup_state(system_env_with_db, courier_num, route_num, depot_num)


def test_mvrp_task_import(system_env_with_db):
    with create_env_with_users(system_env_with_db) as users_env:

        mvrp_task_import_path = api_path_with_company_id(system_env_with_db, 'mvrp_task?task_id=unknown-task-id')

        _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=mvrp_task_import_path,
            manager_expected_status_code=requests.codes.not_found,
            app_expected_status_code=requests.codes.forbidden,
            admin_expected_status_code=requests.codes.not_found
        )


def test_sms_report(system_env_with_db):
    with create_env_with_users(system_env_with_db) as users_env:
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'sms-report?date={}'.format(date.today().isoformat()))
        )


# TODO: this function should be removed in the scope of the task BBGEO-446
def test_route_tracking(system_env_with_db):
    with create_env_with_users(system_env_with_db) as users_env:
        vehicle_id = 100

        # Check that vehicles/{}/create-track handler no longer exists
        _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'vehicles/{}/create-track'.format(vehicle_id)),
            manager_expected_status_code=requests.codes.not_found,
            admin_expected_status_code=requests.codes.not_found,
            app_expected_status_code=requests.codes.not_found
        )

        # Check that vehicles/{}/push-positions handler no longer exists
        _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'vehicles/{}/push-positions'.format(vehicle_id)),
            data={},
            manager_expected_status_code=requests.codes.not_found,
            admin_expected_status_code=requests.codes.not_found,
            app_expected_status_code=requests.codes.not_found
        )


def test_gps_trackers(system_env_with_db):
    with create_env_with_users(system_env_with_db) as users_env:
        positions_data = [
            {
                "latitude": 55.754096,
                "longitude": 37.731182,
                "time": datetime.now(tz=timezone.utc).isoformat()
            }
        ]

        _perform_action(
            env_post_request,
            system_env_with_db,
            users_env,
            path='gps-trackers/{}/push-positions'.format(11111111111111111),
            data={'positions': positions_data},
            manager_expected_status_code=requests.codes.forbidden,
            admin_expected_status_code=requests.codes.forbidden
        )


def test_verification(system_env_with_db):
    with create_env_with_users(system_env_with_db) as users_env:
        _perform_action(
            env_get_request,
            system_env_with_db,
            users_env,
            path=api_path_with_company_id(system_env_with_db, 'verification?date={}'.format(date.today().isoformat()))
        )
