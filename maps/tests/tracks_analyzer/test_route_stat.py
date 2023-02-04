import time
from http import HTTPStatus
import pytest
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get, local_patch, local_post
from ya_courier_backend.models import db, RouteStat

CONSTANT_FOR_TESTING_ORDER_BY = 2
CONSTANT_FOR_TESTING_META = 2


def _add_route_stat_instance(env: Environment):
    with env.flask_app.app_context():
        route_stat = RouteStat(
            route_id=env.default_route.id, company_id=env.default_company.id,
            route_number=env.default_route.number, routing_mode=env.default_route.routing_mode,
            run_number=1, depot_id=env.default_depot.id, depot_number=env.default_depot.number,
            courier_id=env.default_courier.id, courier_number=env.default_courier.number,
            courier_position_lat=env.default_depot.lat, courier_position_lon=env.default_depot.lon,
            courier_position_time=time.time(),
            orders_count=90, orders_arrived_late_count=0, orders_arrived_late_duration_s=0, orders_canceled_count=0,
            orders_arrived_on_time_count=0, orders_estimated_late_count=0, orders_estimated_late_duration_s=0,
            orders_estimated_on_time_count=0, orders_no_estimation_count=0,
            stops_count=13, idles_count=1, idles_duration_s=0
        )
        db.session.add(route_stat)
        db.session.commit()


def _add_route_stat_for_order_by(env: Environment):
    with env.flask_app.app_context():
        route_path = f'/api/v1/companies/{env.default_company.id}/routes'

        route_data = {
            'number': '1',
            'courier_number': env.default_courier.number,
            'depot_id': env.default_depot.id,
            'date': str(env.default_route.date),
            }
        route_post = local_post(env.client, route_path, headers=env.user_auth_headers, data=route_data, expected_status=HTTPStatus.OK)

        route_stat = RouteStat(
            route_id=route_post['id'],
            route_number=route_post['number'],
            company_id=route_post['company_id'],
            depot_id=route_post['depot_id'],
            courier_id=route_post['courier_id'],
            orders_count=10,
            stops_count=CONSTANT_FOR_TESTING_ORDER_BY,
            idles_count=CONSTANT_FOR_TESTING_ORDER_BY,
            idles_duration_s=CONSTANT_FOR_TESTING_ORDER_BY,
            orders_arrived_late_count=CONSTANT_FOR_TESTING_ORDER_BY,
            orders_arrived_late_duration_s=CONSTANT_FOR_TESTING_ORDER_BY,
            orders_canceled_count=CONSTANT_FOR_TESTING_ORDER_BY,
            orders_arrived_on_time_count=CONSTANT_FOR_TESTING_ORDER_BY,
            orders_estimated_late_count=CONSTANT_FOR_TESTING_ORDER_BY,
            orders_estimated_late_duration_s=CONSTANT_FOR_TESTING_ORDER_BY,
            orders_estimated_on_time_count=CONSTANT_FOR_TESTING_ORDER_BY,
            orders_no_estimation_count=CONSTANT_FOR_TESTING_ORDER_BY,
        )
        db.session.add(route_stat)
        db.session.commit()


def test_get_route_stat(env: Environment):
    path = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}"

    _add_route_stat_instance(env)
    resp_json = local_get(env.client, path, headers=env.superuser_auth_headers,
                          expected_status=HTTPStatus.OK)
    assert len(resp_json['routes']) == 1


@pytest.mark.parametrize('date,filter,courier_status,expected',
                         [('2022-11-30', '2022', 'online',  0),  # Wrong date
                          ('2020-11-30', '2022', 'offline', 0),  # Wrong courier_status
                          ('2020-11-30', '2022', 'online', 1),  # Correct filter on route_number
                          ('2020-11-30', '2019', 'online', 0),  # Wrong filter
                          ('2020-11-30', '2020', 'online', 1),  # Correct filter on depot_number
                          ('2020-11-30', 'Courier_name', 'online', 1),  # Correct filter on courier_name
                          ('2020-11-30', 'courier_name', 'online', 0),  # Wrong courier_name
                          ('2020-11-30', '2018', 'online', 1)  # Correct filter on order_number
                          ])
def test_filters(env: Environment, date, filter, courier_status, expected):
    path = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={date}&filter={filter}&courier_status={courier_status}" \
           f"&online_status_duration_s=3600"

    route_path = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
    depot_path = f"/api/v1/companies/{env.default_company.id}/depots"
    order_path = f"/api/v1/companies/{env.default_company.id}/orders"

    route_data = {
        "number": "2022",
        "courier_number": env.default_courier.number,
        "depot_number": env.default_depot.number,
        "date": "2020-11-30",
    }

    depot_data = {
        "number": "2020",
        "time_interval": "00:00-23:59",
        "address": "some address",
        "lat": 55.791928,
        "lon": 37.841492,
    }

    order_data = {
        "number": "2018",
        "time_interval": "00:00-23:59",
        "address": "some address",
        "lat": 55.791928,
        "lon": 37.841492,
        "route_id": env.default_route.id,
    }

    route_patch = local_patch(env.client, route_path, headers=env.user_auth_headers, data=route_data)
    depot_post = local_post(env.client, depot_path, headers=env.user_auth_headers, data=depot_data)
    local_post(env.client, order_path, headers=env.user_auth_headers, data=order_data)

    with env.flask_app.app_context():
        route_stat = RouteStat(
            route_id=route_patch['id'],
            route_number=route_patch['number'],
            company_id=route_patch['company_id'],
            depot_id=route_patch['depot_id'],
            depot_number=depot_post['number'],
            courier_id=route_patch['courier_id'],
            courier_position_time=time.time(),
            courier_name='Courier_name'
        )
        db.session.add(route_stat)
        db.session.commit()

    local_patch(
        client=env.client,
        headers=env.user_auth_headers,
        path=f"/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}",
        data={"name": "Courier_name"},
        expected_status=HTTPStatus.OK,
    )

    resp_json = local_get(env.client, path, headers=env.superuser_auth_headers,
                          expected_status=HTTPStatus.OK)
    assert len(resp_json['routes']) == expected


@pytest.mark.parametrize('order_by',
                         [('number,desc'),
                          ('number,asc'),
                          ('id,desc'),
                          ('id,asc'),
                          ('run_number,desc'),
                          ('run_number,asc')
                          ])
def test_order_by_parameter_first_level(env: Environment, order_by):
    path = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}&order_by={order_by}"

    _add_route_stat_for_order_by(env)
    _add_route_stat_instance(env)

    resp_json = local_get(env.client, path, headers=env.superuser_auth_headers,
                          expected_status=HTTPStatus.OK)

    if order_by.split(',')[1] == 'desc':
        assert resp_json['routes'][0][order_by.split(',')[0]] > resp_json['routes'][1][order_by.split(',')[0]]
    else:
        assert resp_json['routes'][0][order_by.split(',')[0]] < resp_json['routes'][1][order_by.split(',')[0]]


@pytest.mark.parametrize('order_by',
                         [('orders.count,desc'),
                          ('orders.count,asc'),
                          ('stops.count,desc'),
                          ('stops.count,asc'),
                          ('idles.count,desc'),
                          ('idles.count,asc'),
                          ('idles.duration_s,desc'),
                          ('idles.duration_s,asc')
                          ])
def test_order_by_second_level(env: Environment, order_by):
    path = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}&order_by={order_by}"

    _add_route_stat_for_order_by(env)
    _add_route_stat_instance(env)

    resp_json = local_get(env.client, path, headers=env.superuser_auth_headers,
                          expected_status=HTTPStatus.OK)

    field = order_by.split(',')[0]
    if order_by.split(',')[1] == 'desc':
        assert resp_json['routes'][0][field.split('.')[0]][field.split('.')[1]] > resp_json['routes'][1][field.split('.')[0]][field.split('.')[1]]
    else:
        assert resp_json['routes'][0][field.split('.')[0]][field.split('.')[1]] < resp_json['routes'][1][field.split('.')[0]][field.split('.')[1]]


@pytest.mark.parametrize('order_by',
                         [('orders.arrived_late.count,desc'),
                          ('orders.arrived_late.count,asc'),
                          ('orders.arrived_late.duration_s,desc'),
                          ('orders.arrived_late.duration_s,asc'),
                          ('orders.canceled.count,desc'),
                          ('orders.canceled.count,asc'),
                          ('orders.arrived_on_time.count,desc'),
                          ('orders.arrived_on_time.count,asc'),
                          ('orders.estimated_late.count,desc'),
                          ('orders.estimated_late.count,asc'),
                          ('orders.estimated_late.duration_s,desc'),
                          ('orders.estimated_late.duration_s,asc'),
                          ('orders.estimated_on_time.count,desc'),
                          ('orders.estimated_on_time.count,asc'),
                          ('orders.no_estimation.count,desc'),
                          ('orders.no_estimation.count,asc'),

                          ])
def test_order_by_third_level(env: Environment, order_by):
    path = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}&order_by={order_by}"

    _add_route_stat_for_order_by(env)
    _add_route_stat_instance(env)

    resp_json = local_get(env.client, path, headers=env.superuser_auth_headers,
                          expected_status=HTTPStatus.OK)

    field = order_by.split(',')[0]
    if order_by.split(',')[1] == 'desc':
        assert resp_json['routes'][0][field.split('.')[0]][field.split('.')[1]][field.split('.')[2]] > \
               resp_json['routes'][1][field.split('.')[0]][field.split('.')[1]][field.split('.')[2]]
    else:
        assert resp_json['routes'][0][field.split('.')[0]][field.split('.')[1]][field.split('.')[2]] < \
               resp_json['routes'][1][field.split('.')[0]][field.split('.')[1]][field.split('.')[2]]


def test_order_by_two_parameters(env: Environment):
    path_two_params = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}&order_by=depot.id,asc;orders.count,desc"

    _add_route_stat_for_order_by(env)
    _add_route_stat_instance(env)

    resp_json = local_get(env.client, path_two_params, headers=env.superuser_auth_headers,
                          expected_status=HTTPStatus.OK)

    assert resp_json['routes'][0]['orders']['count'] > resp_json['routes'][1]['orders']['count']


def test_pagination(env: Environment):
    path_first_page = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}&order_by=orders.count,desc&per_page=1&page=1"
    path_second_page = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}&order_by=orders.count,desc&per_page=1&page=2"

    _add_route_stat_for_order_by(env)
    _add_route_stat_instance(env)

    resp_json_first = local_get(env.client, path_first_page, headers=env.superuser_auth_headers,
                                expected_status=HTTPStatus.OK)

    resp_json_second = local_get(env.client, path_second_page, headers=env.superuser_auth_headers,
                                 expected_status=HTTPStatus.OK)

    assert len(resp_json_first['routes']) == 1
    assert resp_json_first['routes'][0]['orders']['count'] == 90
    assert resp_json_second['routes'][0]['orders']['count'] == 10


def test_meta(env: Environment):
    route_path = f'/api/v1/companies/{env.default_company.id}/routes'

    route_data = {
        'number': '1',
        'courier_number': env.default_courier.number,
        'depot_id': env.default_depot.id,
        'date': str(env.default_route.date),
    }
    route_post = local_post(env.client, route_path, headers=env.user_auth_headers, data=route_data,
                            expected_status=HTTPStatus.OK)

    expected_meta = {
        'max_values': {
            'idles': {
                'count': 10,
                'duration_s': 100,
            },
            'orders': {
                'arrived_late': {
                    'count': 20,
                    'duration_s': 200,
                },
                'canceled': {
                    'count': 30,
                },
                'arrived_on_time': {
                    'count': 40,
                },
                'estimated_late': {
                    'count': 50,
                    'duration_s': 500,
                },
                'estimated_on_time': {
                    'count': 60,
                },
                'no_estimation': {
                    'count': 70,
                }
            }
        }
    }
    with env.flask_app.app_context():
        route_stat = RouteStat(
            route_id=route_post['id'],
            route_number=route_post['number'],
            company_id=route_post['company_id'],
            depot_id=route_post['depot_id'],
            courier_id=route_post['courier_id'],
            orders_count=10,
            stops_count=CONSTANT_FOR_TESTING_META,
            idles_count=expected_meta['max_values']['idles']['count'],
            idles_duration_s=CONSTANT_FOR_TESTING_META,
            orders_arrived_late_count=expected_meta['max_values']['orders']['arrived_late']['count'],
            orders_arrived_late_duration_s=CONSTANT_FOR_TESTING_META,
            orders_canceled_count=expected_meta['max_values']['orders']['canceled']['count'],
            orders_arrived_on_time_count=CONSTANT_FOR_TESTING_META,
            orders_estimated_late_count=expected_meta['max_values']['orders']['estimated_late']['count'],
            orders_estimated_late_duration_s=CONSTANT_FOR_TESTING_META,
            orders_estimated_on_time_count=expected_meta['max_values']['orders']['estimated_on_time']['count'],
            orders_no_estimation_count=CONSTANT_FOR_TESTING_META,
        )
        db.session.add(route_stat)

        route_stat = RouteStat(
            route_id=env.default_route.id,
            route_number=env.default_route.number,
            company_id=env.default_company.id,
            depot_id=env.default_depot.id,
            courier_id=env.default_courier.id,
            orders_count=10,
            stops_count=CONSTANT_FOR_TESTING_META,
            idles_count=CONSTANT_FOR_TESTING_META,
            idles_duration_s=expected_meta['max_values']['idles']['duration_s'],
            orders_arrived_late_count=CONSTANT_FOR_TESTING_META,
            orders_arrived_late_duration_s=expected_meta['max_values']['orders']['arrived_late']['duration_s'],
            orders_canceled_count=CONSTANT_FOR_TESTING_META,
            orders_arrived_on_time_count=expected_meta['max_values']['orders']['arrived_on_time']['count'],
            orders_estimated_late_count=CONSTANT_FOR_TESTING_META,
            orders_estimated_late_duration_s=expected_meta['max_values']['orders']['estimated_late']['duration_s'],
            orders_estimated_on_time_count=CONSTANT_FOR_TESTING_META,
            orders_no_estimation_count=expected_meta['max_values']['orders']['no_estimation']['count'],
        )
        db.session.add(route_stat)
        db.session.commit()

    path = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}&per_page=1"
    resp = local_get(env.client, path, headers=env.superuser_auth_headers, expected_status=HTTPStatus.OK)
    assert resp['meta'] == expected_meta


def test_meta_no_stats(env: Environment):
    path = f"/api/v1/companies/{env.default_company.id}/routes/stats?date={env.default_route.date}"
    resp = local_get(env.client, path, headers=env.superuser_auth_headers, expected_status=HTTPStatus.OK)
    assert len(resp['routes']) == 0
    assert resp['meta']['max_values']['idles']['count'] == 0
    assert resp['meta']['max_values']['idles']['duration_s'] == 0
    assert resp['meta']['max_values']['orders']['arrived_late']['count'] == 0
    assert resp['meta']['max_values']['orders']['arrived_late']['duration_s'] == 0
    assert resp['meta']['max_values']['orders']['canceled']['count'] == 0
    assert resp['meta']['max_values']['orders']['arrived_on_time']['count'] == 0
    assert resp['meta']['max_values']['orders']['estimated_late']['count'] == 0
    assert resp['meta']['max_values']['orders']['estimated_late']['duration_s'] == 0
    assert resp['meta']['max_values']['orders']['estimated_on_time']['count'] == 0
    assert resp['meta']['max_values']['orders']['no_estimation']['count'] == 0


@pytest.mark.parametrize('courier_position_time_offset,courier_status,expected',
                         [(0, 'online',  1),
                          (0, 'offline', 0),
                          (-3601, 'online', 0),
                          (-3601, 'offline', 1),
                          ])
def test_courier_status(env: Environment, courier_position_time_offset, courier_status, expected):
    courier_position_time = time.time() + courier_position_time_offset

    route_stat_path = f"/api/v1/companies/{env.default_company.id}/routes/stats?date=2020-11-30&courier_status={courier_status}" \
                      "&online_status_duration_s=3600"
    route_path = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"

    route_data = {
        "number": "2022",
        "courier_number": env.default_courier.number,
        "depot_number": env.default_depot.number,
        "date": "2020-11-30",
    }

    route_patch = local_patch(env.client, route_path, headers=env.user_auth_headers, data=route_data)

    with env.flask_app.app_context():
        route_stat = RouteStat(
            route_id=route_patch['id'],
            route_number=route_patch['number'],
            company_id=route_patch['company_id'],
            depot_id=route_patch['depot_id'],
            courier_id=route_patch['courier_id'],
            courier_position_time=courier_position_time,
            courier_name='Courier_name',
        )
        db.session.add(route_stat)
        db.session.commit()

    local_patch(
        client=env.client,
        headers=env.user_auth_headers,
        path=f"/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}",
        data={"name": "Courier_name"},
        expected_status=HTTPStatus.OK,
    )

    resp_json = local_get(env.client, route_stat_path, headers=env.superuser_auth_headers,
                          expected_status=HTTPStatus.OK)
    assert len(resp_json['routes']) == expected
