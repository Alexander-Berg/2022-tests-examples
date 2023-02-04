import copy
import datetime
import dateutil.tz
from http import HTTPStatus

import pytest
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import solver_request_by_task_id, get_solution_by_task_id
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_delete, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage
from maps.b2bgeo.ya_courier.backend.test_lib.util_rented_vehicle import TEST_TAXI_VEHICLE, TEST_NON_TAXI_VEHICLE
from ya_courier_backend.models import (
    Courier,
    DepotInstance,
    Garage,
    RentedCourier,
    Route,
    TaxiTracking,
    db,
    Order,
    OrderType,
    GarageStatus,
    RouteNode,
)

DEFAULT_GARAGE = {
    "address": "test-address",
    "company_id": 1,
    "id": 1,
    "lat": 55.664695,
    "lon": 37.562443,
    "number": "",
    "route_id": 1,
}

TIME_ZONE = dateutil.tz.gettz("Europe/Moscow")
TEST_DATETIME = datetime.datetime(2019, 12, 13, 1, 1, 1, tzinfo=TIME_ZONE)


def _get_garage_with(id, number, route_id=1, status=GarageStatus.unvisited, history=None):
    garage = copy.deepcopy(DEFAULT_GARAGE)
    garage["id"] = id
    garage["number"] = number
    garage["route_id"] = route_id
    garage["status"] = status
    garage["history"] = history
    return garage


def _set_garages_history(garages, history):
    for garage in garages:
        garage['history'] = history


def _get_existing_orders(env: Environment):
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    orders = local_get(env.client, path_orders, headers=env.user_auth_headers)
    return orders


def _check_orders_number_postfix(env: Environment, itter, original_orders_number):
    with env.flask_app.app_context():
        orders = db.session.query(Order).filter(Order.company_id == env.default_company.id).all()
        order_numbers = set(order.number for order in orders)

    for original_order in original_orders_number:
        assert f'{original_order} ({itter})' in order_numbers


def _create_test_order_data(route_id, number='order_number', time_interval='06:00-07:15', customer_name='customer_name'):
    return {
        'number': number,
        'time_interval': time_interval,
        'address': 'some address',
        'lat': 55.791928,
        'lon': 37.841492,
        'route_id': route_id,
        'customer_name': customer_name,
    }


def _create_empty_route(env, number='1'):
    path_route = f'/api/v1/companies/{env.default_company.id}/routes'
    route_data = {
        'number': number,
        'courier_number': env.default_courier.number,
        'depot_number': env.default_depot.number,
        'date': '2020-11-30',
    }
    return local_post(env.client, path_route, headers=env.user_auth_headers, data=route_data)


@skip_if_remote
@pytest.mark.parametrize("case", [
    ('mock_task_uuid__generic', None),
    ('mock_task_uuid__vehicle_phone', None),
    ('mock_task_uuid__vehicle_phone', '777-77-77'),
])
def test_vehicle_phone(env: Environment, case):

    task_id, phone_in_db = case

    if phone_in_db:
        with env.flask_app.app_context():
            courier_in_db = db.session.query(Courier).first()
            courier_in_db.phone = phone_in_db
            db.session.commit()

    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}&original_task_id={task_id}"
    local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        couriers = db.session.query(Courier).filter(Courier.company_id == env.default_company.id).all()
        if task_id == 'mock_task_uuid__vehicle_phone':
            assert couriers[0].phone == "314-159-26-53"     # courier_in_db & phone_in_mvrp (x2)
        else:
            assert couriers[0].phone is None                # courier_in_db & phone_not_in_mvrp
        assert couriers[1].phone == "271-828-18-28"         # ccourier_not_in_db & phone_in_mvrp
        assert couriers[2].phone is None                    # ccourier_not_in_db & phone_not_in_mvrp


@skip_if_remote
def test_first_garage_import(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__solution_with_garage_first"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        garages = Garage.get(slave_session=False)
        db_garages = list(map(Garage.as_dict, garages))
        _set_garages_history(db_garages, None)
        assert db_garages == [_get_garage_with(1, "first-garage", route_id=2)]


@skip_if_remote
def test_first_and_last_garage_import(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__solution_with_garage_first_and_last"
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        garages = Garage.get(slave_session=False)
        db_garages = list(map(Garage.as_dict, garages))
        _set_garages_history(db_garages, None)
        assert db_garages == [
            _get_garage_with(1, "first-garage", route_id=2),
            _get_garage_with(2, "last-garage", route_id=2),
        ]


@skip_if_remote
def test_duplicate_garage_import(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id="
    first_task_id = "mock_task_uuid__first_reduced_with_garage_last"
    local_post(env.client, path + first_task_id, headers=env.user_auth_headers)

    second_task_id = "mock_task_uuid__second_reduced_with_garage_last"
    local_post(env.client, path + second_task_id, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        garages = Garage.get(slave_session=False)
        db_garages = list(map(Garage.as_dict, garages))
        _set_garages_history(db_garages, None)
        assert db_garages == [
            _get_garage_with(1, "last-garage", route_id=2),
            _get_garage_with(2, "last-garage", route_id=3),
        ]


@skip_if_remote
def test_route_cascade_removal(env: Environment):
    # 0. Make default company import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    routes = local_post(env.client, path_import, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert db.session.query(Route).all()
        assert RouteNode.get()
        assert Order.get()
        assert DepotInstance.get()
        assert Garage.get()

    path_delete = f"/api/v1/companies/{env.default_company.id}/routes"
    local_delete(env.client, path_delete, data=[routes[0]["id"]], headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert not RouteNode.get()
        assert not Order.get()
        assert not DepotInstance.get()
        assert not Garage.get()


@skip_if_remote
def test_not_to_import_depot_garage(env: Environment):
    # 0. Make default company not to import depots and garages
    set_company_import_depot_garage(env, env.default_company.id, False)

    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert db.session.query(Route).all()
        assert RouteNode.get()
        assert Order.get()
        assert not DepotInstance.get()
        assert not Garage.get()


@skip_if_remote
def test_import_route_with_drop_off(env: Environment):
    task_id = "mock_task_uuid__result_with_drop_off"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert Order.get(order_type=OrderType.drop_off)


@skip_if_remote
def test_mvrp_task_import_with_two_shifts(env: Environment):
    task_id = "mock_task_uuid__task_for_import_routes_with_two_shifts"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_import_route_with_route_number(env: Environment):
    with env.flask_app.app_context():
        query = db.session.query(Route.number)
        existing_route_numbers = {row[0] for row in query.all()}

    # 1. prepare task with route_numbers
    task_json = copy.deepcopy(solver_request_by_task_id['mock_task_uuid__task_for_import_routes'])
    task_json['vehicles'][0]['shifts'][0]['route_numbers'] = ['vehicle_0_route']
    task_json['vehicles'][1]['shifts'][0]['route_numbers'] = ['vehicle_1_route']

    # 2. post routes using import-routes
    path_import = f"/api/v1/companies/{env.default_company.id}/import-routes"
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    # 3. check if created route numbers are set to given ones
    with env.flask_app.app_context():
        query = db.session.query(Route.number)
        route_numbers = {row[0] for row in query.all()} - existing_route_numbers
        assert route_numbers == {'vehicle_0_route', 'vehicle_1_route'}


@skip_if_remote
def test_import_route_with_existing_orders(env: Environment):
    task_json = copy.deepcopy(solver_request_by_task_id['mock_task_uuid__task_for_import_routes'])
    task_json['vehicles'][0]['shifts'][0]['route_numbers'] = ['vehicle_0_route']
    task_json['vehicles'][1]['shifts'][0]['route_numbers'] = ['vehicle_1_route']

    path_import = f'/api/v1/companies/{env.default_company.id}/import-routes'
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)
    existing_orders = _get_existing_orders(env)
    orders_number = set(order['number'] for order in existing_orders)

    task_json['vehicles'][0]['shifts'][0]['route_numbers'] = ['vehicle_3_route']
    task_json['vehicles'][1]['shifts'][0]['route_numbers'] = ['vehicle_4_route']

    path_import = f'/api/v1/companies/{env.default_company.id}/import-routes?rename_duplicate_orders=True'
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    _check_orders_number_postfix(env, 1, orders_number)

    current_orders = _get_existing_orders(env)
    added_orders_numbers = len(current_orders) - len(existing_orders)

    assert added_orders_numbers == 3

    existing_orders = current_orders

    path_import = f'/api/v1/companies/{env.default_company.id}/import-routes?rename_duplicate_orders=True&import-mode=add-all'

    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    _check_orders_number_postfix(env, 2, orders_number)

    current_orders = _get_existing_orders(env)
    added_orders_numbers = len(current_orders) - len(existing_orders)

    assert added_orders_numbers == 3


@skip_if_remote
def test_mvrp_task_with_existing_orders(env: Environment):
    task_id = 'mock_task_uuid__generic'
    path = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}'
    local_post(env.client, path, headers=env.user_auth_headers)

    existing_orders = _get_existing_orders(env)
    orders_number = set(order['number'] for order in existing_orders)

    path_with_param = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}&rename_duplicate_orders=True&import-mode=add-all'
    local_post(env.client, path_with_param, headers=env.user_auth_headers)

    _check_orders_number_postfix(env, 1, orders_number)

    current_orders = _get_existing_orders(env)
    added_orders_numbers = len(current_orders) - len(existing_orders)
    existing_orders = current_orders
    assert added_orders_numbers == 3

    path_with_param = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}&rename_duplicate_orders=True&import-mode=add-all'
    local_post(env.client, path_with_param, headers=env.user_auth_headers)

    _check_orders_number_postfix(env, 2, orders_number)

    current_orders = _get_existing_orders(env)
    added_orders_numbers = len(current_orders) - len(existing_orders)
    assert added_orders_numbers == 3


@skip_if_remote
def test_import_route_with_multiple_route_numbers_for_one_vehicle(env: Environment):
    with env.flask_app.app_context():
        query = db.session.query(Route.number)
        existing_route_numbers = {row[0] for row in query.all()}

    # 1. prepare task with route_numbers
    task_json = copy.deepcopy(solver_request_by_task_id['mock_task_uuid__task_for_import_routes_with_two_shifts'])
    task_json['vehicles'][0]['shifts'][0]['route_numbers'] = ['vehicle_0_route_0']
    task_json['vehicles'][0]['shifts'][1]['route_numbers'] = ['vehicle_0_route_1']

    # 2. post routes using import-routes
    path_import = f"/api/v1/companies/{env.default_company.id}/import-routes"
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    # 3. check if created route numbers are set to given ones
    with env.flask_app.app_context():
        query = db.session.query(Route.number)
        route_numbers = {row[0] for row in query.all()} - existing_route_numbers
        assert route_numbers == {'vehicle_0_route_0', 'vehicle_0_route_1'}


@skip_if_remote
def test_import_route_with_multiple_runs_in_one_shift(env: Environment):
    with env.flask_app.app_context():
        query = db.session.query(Route.number)
        existing_route_numbers = {row[0] for row in query.all()}

    # 1. prepare task with route_numbers
    task_json = copy.deepcopy(
        solver_request_by_task_id['mock_task_uuid__task_for_import_routes_with_two_runs_in_one_shift'])
    task_json['vehicles'][0]['shifts'][0]['route_numbers'] = ['vehicle_0_route_0', 'vehicle_0_route_1']

    # 2. post routes using import-routes
    path_import = f"/api/v1/companies/{env.default_company.id}/import-routes"
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    # 3. check if created route numbers are set to given ones
    with env.flask_app.app_context():
        query = db.session.query(Route.number)
        route_numbers = {row[0] for row in query.all()} - existing_route_numbers
        assert route_numbers == {'vehicle_0_route_0', 'vehicle_0_route_1'}


@skip_if_remote
def test_import_route_with_multiple_runs_in_one_shift_and_not_enough_numbers(env: Environment):
    with env.flask_app.app_context():
        query = db.session.query(Route.number)
        existing_route_numbers = {row[0] for row in query.all()}

    # 1. prepare task with route_numbers
    task_json = copy.deepcopy(
        solver_request_by_task_id['mock_task_uuid__task_for_import_routes_with_two_runs_in_one_shift'])
    task_json['vehicles'][0]['shifts'][0]['route_numbers'] = ['vehicle_0_route_0']

    # 2. post routes using import-routes
    path_import = f"/api/v1/companies/{env.default_company.id}/import-routes"
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    # 3. check if created route numbers are set to given ones
    with env.flask_app.app_context():
        query = db.session.query(Route.number)
        route_numbers = {row[0] for row in query.all()} - existing_route_numbers
        assert route_numbers == {'vehicle_0_route_0', '1-2-2019-12-13'}


@skip_if_remote
@freeze_time('2019-12-13')
def test_check_import_route_with_multiple_runs_in_one_shift(env: Environment):
    # 1. prepare task with route_numbers
    task_json = copy.deepcopy(
        solver_request_by_task_id['mock_task_uuid__task_for_import_routes_with_two_runs_in_one_shift'])
    task_json['vehicles'][0]['shifts'][0]['route_numbers'] = ['vehicle_0_route_0']

    # 2. post routes using import-routes
    path_import = f'/api/v1/companies/{env.default_company.id}/import-routes'
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    # 3. check-import-task for same task
    path_check_import = f'/api/v1/companies/{env.default_company.id}/check-import-task'
    for seq, location in enumerate(task_json['locations'], start=1):
        location['id'] = seq
    task_json['depot']['id'] = 0
    task_json['vehicles'][0]['planned_route'] = {'locations': [
        {
            'id': location['id'],
        } for location in task_json['locations']
    ]}
    task_json['vehicles'][0]['planned_route']['locations'][5]['id'] = 0
    response = local_post(env.client, path_check_import, headers=env.user_auth_headers, data=task_json)
    assert {
        'error': 'RoutesAlreadyExist',
        'item_type': 'route',
        'numbers': ['1-2-2019-12-13', 'vehicle_0_route_0']
    } in response


@skip_if_remote
def test_deleted_courier(env: Environment):
    # Delete default route
    route_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    local_delete(env.client, route_path, headers=env.user_auth_headers)

    # Import mvrp task
    task_id = "mock_task_uuid__first_reduced_with_garage_last"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task"
    local_post(env.client, path_import, query={'task_id': task_id}, headers=env.user_auth_headers)

    # Shallow delete all couriers
    path_couriers = f"/api/v1/companies/{env.default_company.id}/couriers"
    couriers = local_get(env.client, path_couriers, headers=env.user_auth_headers)
    for courier in couriers:
        path_courier = f"/api/v1/companies/{env.default_company.id}/couriers/{courier['id']}"
        local_delete(env.client, path_courier, query={'shallow_delete': True}, headers=env.user_auth_headers)

    couriers = local_get(env.client, path_couriers, headers=env.user_auth_headers)
    assert len(couriers) == 0

    # Re add same mvrp task
    local_post(env.client, path_import, query={'task_id': task_id, 'keep-routes': False}, headers=env.user_auth_headers)

    # Check new couriers assigned to imported routes
    couriers = local_get(env.client, path_couriers, headers=env.user_auth_headers)
    courier_ids = [courier['id'] for courier in couriers]

    path_routes = f"/api/v1/companies/{env.default_company.id}/routes"
    routes = local_get(env.client, path_routes, headers=env.user_auth_headers)

    for route in routes:
        assert route['courier_id'] in courier_ids


@skip_if_remote
def test_taxi_tracking(env: Environment):
    original_task_id = 'mock_original_task_uuid__without_imei'
    task_id = 'mock_task_uuid__without_imei'

    path = f"/api/v1/vrs/mvrp/{original_task_id}/rented-vehicle"
    data = copy.deepcopy(TEST_TAXI_VEHICLE)
    data['ref'] = 'car-1'
    local_post(env.client, path, headers=env.user_auth_headers, data=data)
    # Car 2 has no routes => no TaxiTracking is created
    data = copy.deepcopy(TEST_TAXI_VEHICLE)
    data['ref'] = 'car-2'
    local_post(env.client, path, headers=env.user_auth_headers, data=data)

    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}&original_task_id={original_task_id}"
    local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        routes = db.session.query(Route).all()
        rented_couriers = db.session.query(RentedCourier).all()
        taxi_tracking = db.session.query(TaxiTracking).all()

        # all routes are done by car-1
        rented_courier_id = None
        for route in routes:
            if rented_courier_id is None:
                rented_courier_id = route.rented_courier_id
            else:
                assert rented_courier_id == route.rented_courier_id

        assert len(rented_couriers) == 1
        courier_dict = rented_couriers[0].as_dict()
        del courier_dict['created_at']
        assert courier_dict == {
            'id': str(rented_courier_id),
            'company_id': env.default_company.id,
            'order_id': TEST_TAXI_VEHICLE['order_id'],
            'provider': TEST_TAXI_VEHICLE['provider']
        }

        assert len(taxi_tracking) == 1
        assert taxi_tracking[0].rented_courier_id == rented_courier_id


@skip_if_remote
def test_non_taxi_rented_vehicle(env: Environment):
    original_task_id = 'mock_original_task_uuid__without_imei'
    task_id = 'mock_task_uuid__without_imei'

    path = f"/api/v1/vrs/mvrp/{original_task_id}/rented-vehicle"
    data = copy.deepcopy(TEST_NON_TAXI_VEHICLE)
    data['ref'] = 'car-1'
    local_post(env.client, path, headers=env.user_auth_headers, data=data)

    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}&original_task_id={original_task_id}"
    local_post(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        routes = db.session.query(Route).all()
        rented_couriers = db.session.query(RentedCourier).all()
        taxi_tracking = db.session.query(TaxiTracking).all()

        for route in routes:
            assert route.rented_courier_id is None

        assert len(rented_couriers) == 0
        assert len(taxi_tracking) == 0


@skip_if_remote
def test_forbid_both_imei_and_rented_vehicle(env: Environment):
    original_task_id = 'mock_original_task_uuid__generic'
    task_id = 'mock_task_uuid__generic'

    path = f"/api/v1/vrs/mvrp/{original_task_id}/rented-vehicle"
    data = copy.deepcopy(TEST_TAXI_VEHICLE)
    data['ref'] = 'car-1'
    local_post(env.client, path, headers=env.user_auth_headers, data=data)

    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}&original_task_id={original_task_id}"
    local_post(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_time_windows(env: Environment):
    task_id = "mock_task_uuid__generic"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        routes = db.session.query(Route).all()

        checked_routes = 0
        for route in routes:
            if route.number == '2020-1-2019-12-13':
                assert route.time_window_min_start.astimezone(TIME_ZONE).isoformat() == '2019-12-13T09:00:00+03:00'
                assert route.time_window_max_end.astimezone(TIME_ZONE).isoformat() == '2019-12-13T14:00:00+03:00'
                checked_routes += 1
            elif route.number == '2020-2-2019-12-13':
                assert route.time_window_min_start.astimezone(TIME_ZONE).isoformat() == '2019-12-13T10:00:00+03:00'
                assert route.time_window_max_end.astimezone(TIME_ZONE).isoformat() == '2019-12-13T22:00:00+03:00'
                checked_routes += 1

        assert checked_routes == 2, 'One of routes is missing'


@skip_if_remote
@pytest.mark.parametrize(
    ('task_id', 'matrix_router'),
    [
        ('mock_task_uuid__generic', 'main'),
        ('mock_task_uuid__solution_with_auto_matrix_router', 'auto'),
        ('mock_task_uuid__solution_with_main_matrix_router', 'main'),
        ('mock_task_uuid__solution_with_global_matrix_router', 'global'),
        ('mock_task_uuid__solution_with_geodesic_matrix_router', 'geodesic'),
    ]
)
def test_mvrp_import_options(env, task_id, matrix_router):
    route_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    local_delete(env.client, route_path, headers=env.user_auth_headers)

    path_import = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}'
    local_post(env.client, path_import, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        routes = db.session.query(Route).all()
        for route in routes:
            assert route.task_options['matrix_router'] == matrix_router


@skip_if_remote
@pytest.mark.parametrize('matrix_router', ['auto', 'main', 'global', 'geodesic'])
def test_import_route_matrix_router_option(env, matrix_router):
    route_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    local_delete(env.client, route_path, headers=env.user_auth_headers)

    task_json = copy.deepcopy(solver_request_by_task_id['mock_task_uuid__task_for_import_routes'])
    task_json['options']['matrix_router'] = matrix_router

    path_import = f'/api/v1/companies/{env.default_company.id}/import-routes'
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    with env.flask_app.app_context():
        route_db = db.session.query(Route).first()
        assert route_db.task_options['matrix_router'] == matrix_router


@skip_if_remote
def test_import_route_matrix_router_not_provided(env):
    route_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    local_delete(env.client, route_path, headers=env.user_auth_headers)

    task_json = copy.deepcopy(solver_request_by_task_id['mock_task_uuid__task_for_import_routes'])
    task_json['options'].pop('matrix_router')

    path_import = f'/api/v1/companies/{env.default_company.id}/import-routes'
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    with env.flask_app.app_context():
        route_db = db.session.query(Route).first()
        assert route_db.task_options['matrix_router'] == 'auto'


def test_import_general_task_with_long_description_and_comments(env):
    task_id = 'mock_task_uuid__generic_with_long_description_and_comments'

    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        assert db.session.query(Route).all()
        assert db.session.query(RouteNode).all()
        assert db.session.query(Order).all()


@pytest.mark.parametrize(
    ('type'),
    [
        'delivery',
        'pickup',
        'drop_off',
    ]
)
@skip_if_remote
def test_import_routes_with_same_order_number(env: Environment, type: str):
    path_import = f'/api/v1/companies/{env.default_company.id}/import-routes'
    task_json = copy.deepcopy(solver_request_by_task_id['mock_task_uuid__task_for_import_routes_with_multiple_orders'])

    order_numbers = [str(item['id']) for item in task_json['locations']]

    route = _create_empty_route(env)
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'

    for order_number in order_numbers:
        data = _create_test_order_data(route['id'], order_number)
        data['type'] = type
        order = local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)

        local_post(env.client, path_import, headers=env.user_auth_headers,
                   data=task_json, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

        path_order_delete = f'/api/v1/companies/{env.default_company.id}/orders/{order["id"]}'
        local_delete(env.client, path_order_delete, headers=env.user_auth_headers)


@pytest.mark.parametrize(
    ('type'),
    [
        'delivery',
        'pickup',
        'drop_off',
    ]
)
@skip_if_remote
def test_mvrp_task_with_same_order_number(env: Environment, type: str):
    task_id = 'mock_task_uuid__with_multi_order_and_different_type'
    path_import = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}'
    solution = get_solution_by_task_id(task_id)

    order_numbers = [str(item['node']['value']['id']) for item in solution['result']['routes'][0]['route'][1:]]

    route = _create_empty_route(env)
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'

    for order_number in order_numbers:
        data = _create_test_order_data(route['id'], order_number)
        data['type'] = type
        order = local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)

        local_post(env.client, path_import, headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

        path_order_delete = f'/api/v1/companies/{env.default_company.id}/orders/{order["id"]}'
        local_delete(env.client, path_order_delete, headers=env.user_auth_headers)


@pytest.mark.parametrize(
    ('type'),
    [
        'delivery',
        'pickup',
        'drop_off',
    ]
)
@skip_if_remote
def test_check_task_import_with_same_order_number(env: Environment, type: str):
    task_id = 'mock_task_uuid__with_multi_order_and_different_type'
    path_import = f'/api/v1/companies/{env.default_company.id}/check-import-task?task_id={task_id}'
    solution = get_solution_by_task_id(task_id)

    order_numbers = [str(item['node']['value']['id']) for item in solution['result']['routes'][0]['route'][1:]]

    route = _create_empty_route(env)
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'

    for order_number in order_numbers:
        data = _create_test_order_data(route['id'], order_number)
        data['type'] = type
        order = local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)

        resp = local_post(env.client, path_import, headers=env.user_auth_headers)
        assert len(resp) == 1

        path_order_delete = f'/api/v1/companies/{env.default_company.id}/orders/{order["id"]}'
        local_delete(env.client, path_order_delete, headers=env.user_auth_headers)
