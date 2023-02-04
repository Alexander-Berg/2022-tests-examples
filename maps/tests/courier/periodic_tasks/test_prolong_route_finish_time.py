import copy
import itertools
from datetime import datetime, timedelta

import pytest
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import MOSCOW_TZ
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get, local_patch, local_post, set_company_routes_max_prolongation, push_positions
from ya_courier_backend.models import db, Route
from ya_courier_backend.tasks.prolong_route_finish_time import ProlongRouteFinishTimeTask


TEST_DATETIME = datetime(2019, 12, 13, 12, 10, 0, tzinfo=MOSCOW_TZ)

POSITIONS_START_TIMESTAMP = 1576206000  # 2019-12-13 06:00 +03:00

DEFAULT_ORDER = {
    'number': 'default_order_number',
    'time_interval': '00:00-07:20',
    'address': 'some address',
    'lat': 55.791928,
    'lon': 37.841492,
}

DEFAULT_GARAGE = {
    'address': 'test-address',
    'lat': 55.664695,
    'lon': 37.562443,
    'number': 'default_garage_number',
}


def push_some_position(env, route):
    locations = [
        (10, 10, POSITIONS_START_TIMESTAMP),
    ]
    push_positions(env, route['id'], locations)


def create_route(env, date='2019-12-13'):
    path = f'/api/v1/companies/{env.default_company.id}/routes'
    route = local_post(
        env.client,
        path,
        headers=env.user_auth_headers,
        data={
            'number': 'fake route',
            'courier_number': env.default_courier.number,
            'depot_number': env.default_depot.number,
            'date': date,
        },
    )

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [{
        'type': 'order',
        'value': DEFAULT_ORDER
    }]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    return route


def get_locations_for_visiting_point(lat, lon, ts):
    return [
        (lat, lon, ts),
        (lat, lon, ts + 900),
    ]


@skip_if_remote
def test_not_visited_order(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)
    push_some_position(env, route)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00')


@skip_if_remote
def test_staying_at_order(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    locations = get_locations_for_visiting_point(DEFAULT_ORDER['lat'], DEFAULT_ORDER['lon'], POSITIONS_START_TIMESTAMP)
    push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00')


@skip_if_remote
@pytest.mark.parametrize('visited', [True, False])
def test_first_garage_depot(env: Environment, visited):
    route = create_route(env)
    set_company_routes_max_prolongation(env)
    push_some_position(env, route)

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [
        {
            'type': 'garage',
            'value': DEFAULT_GARAGE,
        },
        {
            'type': 'depot',
            'value': {'number': env.default_depot.number},
        },
        {
            'type': 'order',
            'value': DEFAULT_ORDER,
        },
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    if visited:
        ts = POSITIONS_START_TIMESTAMP
        locations = get_locations_for_visiting_point(DEFAULT_ORDER['lat'], DEFAULT_ORDER['lon'], POSITIONS_START_TIMESTAMP + 100) + \
            [(DEFAULT_ORDER['lat'] + 1, DEFAULT_ORDER['lon'] + 1, ts + 2000)]
        push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            if visited:
                assert route.prolonged_finish_time is None
            else:
                assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00')


@skip_if_remote
@pytest.mark.parametrize('visited', [True, False])
@pytest.mark.parametrize('with_depot', [True, False])
def test_last_garage(env: Environment, visited, with_depot):
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [{
        'type': 'order',
        'value': DEFAULT_ORDER,
    }]
    if with_depot:
        nodes.append({
            'type': 'depot',
            'value': {'number': env.default_depot.number},
        })
    nodes.append({
        'type': 'garage',
        'value': DEFAULT_GARAGE,
    })
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    ts = POSITIONS_START_TIMESTAMP
    # visit order
    locations = get_locations_for_visiting_point(DEFAULT_ORDER['lat'], DEFAULT_ORDER['lon'], ts)
    push_positions(env, route['id'], locations)

    if with_depot:
        # visit depot
        locations = get_locations_for_visiting_point(env.default_depot.lat, env.default_depot.lon, ts + 2000)
        push_positions(env, route['id'], locations)

    if visited:
        # visit garage
        locations = get_locations_for_visiting_point(DEFAULT_GARAGE['lat'], DEFAULT_GARAGE['lon'], ts + 4000)
        push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            if visited:
                assert route.prolonged_finish_time is None
            else:
                assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00')


@skip_if_remote
@pytest.mark.parametrize('visited', [True, False])
def test_last_depot(env: Environment, visited):
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [
        {
            'type': 'order',
            'value': DEFAULT_ORDER,
        },
        {
            'type': 'depot',
            'value': {'number': env.default_depot.number},
        },
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    ts = POSITIONS_START_TIMESTAMP
    # visit and leave the only order
    locations = get_locations_for_visiting_point(DEFAULT_ORDER['lat'], DEFAULT_ORDER['lon'], ts) + \
        [(DEFAULT_ORDER['lat'] + 1, DEFAULT_ORDER['lon'] + 1, ts + 2000)]
    push_positions(env, route['id'], locations)

    if visited:
        # visit depot
        locations = get_locations_for_visiting_point(env.default_depot.lat, env.default_depot.lon, ts + 2100)
        push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            if visited:
                assert route.prolonged_finish_time is None
            else:
                assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00')


@skip_if_remote
@pytest.mark.parametrize('visit_order', list(itertools.permutations(['garage', 'order', 'depot'])))
def test_visit_order(env: Environment, visit_order):
    visit_order = list(visit_order)
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [
        {
            'type': 'order',
            'value': DEFAULT_ORDER,
        },
        {
            'type': 'depot',
            'value': {'number': env.default_depot.number},
        },
        {
            'type': 'garage',
            'value': DEFAULT_GARAGE,
        },
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    ts = POSITIONS_START_TIMESTAMP
    for entity in visit_order:
        if entity == 'order':
            locations = get_locations_for_visiting_point(DEFAULT_ORDER['lat'], DEFAULT_ORDER['lon'], ts)
            push_positions(env, route['id'], locations)

        elif entity == 'depot':
            locations = get_locations_for_visiting_point(env.default_depot.lat, env.default_depot.lon, ts)
            push_positions(env, route['id'], locations)

        elif entity == 'garage':
            locations = get_locations_for_visiting_point(DEFAULT_GARAGE['lat'], DEFAULT_GARAGE['lon'], ts)
            push_positions(env, route['id'], locations)

        ts += 2000

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            if visit_order.index('order') < visit_order.index('garage'):
                assert route.prolonged_finish_time is None, f'Wrong result for {visit_order} order'
            else:
                assert route.prolonged_finish_time is not None, f'Wrong result for {visit_order} order'
                assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00'), \
                    f'Wrong result for {visit_order} order'


@skip_if_remote
def test_revisit_garage(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [
        {
            'type': 'order',
            'value': DEFAULT_ORDER,
        },
        {
            'type': 'depot',
            'value': {'number': env.default_depot.number},
        },
        {
            'type': 'garage',
            'value': DEFAULT_GARAGE,
        },
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    ts = POSITIONS_START_TIMESTAMP
    # visit garage before order
    locations = get_locations_for_visiting_point(DEFAULT_GARAGE['lat'], DEFAULT_GARAGE['lon'], ts)
    push_positions(env, route['id'], locations)

    # visit order
    locations = get_locations_for_visiting_point(DEFAULT_ORDER['lat'], DEFAULT_ORDER['lon'], ts + 2000)
    push_positions(env, route['id'], locations)

    # visit depot
    locations = get_locations_for_visiting_point(env.default_depot.lat, env.default_depot.lon, ts + 4000)
    push_positions(env, route['id'], locations)

    # visit garage
    locations = get_locations_for_visiting_point(DEFAULT_GARAGE['lat'], DEFAULT_GARAGE['lon'], ts + 6000)
    push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time is None


@skip_if_remote
def test_revisit_depot(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [
        {
            'type': 'order',
            'value': DEFAULT_ORDER,
        },
        {
            'type': 'depot',
            'value': {'number': env.default_depot.number},
        },
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    ts = POSITIONS_START_TIMESTAMP
    # visit depot before order
    locations = get_locations_for_visiting_point(env.default_depot.lat, env.default_depot.lon, ts)
    push_positions(env, route['id'], locations)

    # visit order
    locations = get_locations_for_visiting_point(DEFAULT_ORDER['lat'], DEFAULT_ORDER['lon'], ts + 2000)
    push_positions(env, route['id'], locations)

    # visit depot
    locations = get_locations_for_visiting_point(env.default_depot.lat, env.default_depot.lon, ts + 4000)
    push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time is None


@skip_if_remote
def test_orders_visit_order(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    order1 = DEFAULT_ORDER
    order2 = copy.deepcopy(DEFAULT_ORDER)
    order2.update({
        'number': 'order2',
        'lat': DEFAULT_ORDER['lat'] + 1,
        'lon': DEFAULT_ORDER['lon'] + 1,
    })
    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [
        {
            'type': 'order',
            'value': order1,
        },
        {
            'type': 'order',
            'value': order2,
        },
        {
            'type': 'depot',
            'value': {'number': env.default_depot.number},
        },
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    ts = POSITIONS_START_TIMESTAMP
    # visit order2
    locations = get_locations_for_visiting_point(order2['lat'], order2['lon'], ts)
    push_positions(env, route['id'], locations)

    # visit depot
    locations = get_locations_for_visiting_point(env.default_depot.lat, env.default_depot.lon, ts + 2000)
    push_positions(env, route['id'], locations)

    # visit order1
    locations = get_locations_for_visiting_point(order1['lat'], order1['lon'], ts + 4000) + \
        [(order1['lat'] - 1, order1['lon'] - 1, ts + 5000)]
    push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00')


@skip_if_remote
def test_max_prolongation(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env, hours=3)
    push_some_position(env, route)

    with freeze_time(TEST_DATETIME) as frozen_time:
        with env.flask_app.app_context():
            # run the task for 5 hours, updates should stop after adding 3 hours
            for _ in range(10):
                task = ProlongRouteFinishTimeTask(env.flask_app)
                task.run({})

                frozen_time.tick(delta=timedelta(minutes=30))

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T15:20:00+03:00')


@skip_if_remote
def test_need_position(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    # mark the only order as finished
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?route_id={route["id"]}'
    orders = local_get(env.client, path_orders, headers=env.user_auth_headers)
    courier_id = route['courier_id']
    order_id = orders[0]['id']
    path_patch = f'/api/v1/couriers/{courier_id}/routes/{route["id"]}/orders/{order_id}'
    local_patch(env.client, path_patch, headers=env.user_auth_headers, data={'status': 'finished'})

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time is None


@skip_if_remote
def test_manual_finish(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)
    order1 = DEFAULT_ORDER
    order2 = copy.deepcopy(DEFAULT_ORDER)
    order2.update({
        'number': 'order2',
        'lat': DEFAULT_ORDER['lat'] + 1,
        'lon': DEFAULT_ORDER['lon'] + 1,
    })
    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [
        {
            'type': 'order',
            'value': order1,
        },
        {
            'type': 'order',
            'value': order2,
        },
        {
            'type': 'depot',
            'value': {'number': env.default_depot.number},
        },
    ]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    ts = POSITIONS_START_TIMESTAMP
    # visit order1
    locations = get_locations_for_visiting_point(order1['lat'], order1['lon'], ts) + \
        [(order1['lat'] - 1, order1['lon'] - 1, ts + 1500)]
    push_positions(env, route['id'], locations)

    # mark order2 as finished
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders?route_id={route["id"]}'
    orders = local_get(env.client, path_orders, headers=env.user_auth_headers)
    courier_id = route['courier_id']
    order_id = next(filter(lambda o: o['number'] == 'order2', orders))['id']
    path_patch = f'/api/v1/couriers/{courier_id}/routes/{route["id"]}/orders/{order_id}'
    local_patch(env.client, path_patch, headers=env.user_auth_headers, data={'status': 'finished'})

    # visit depot
    locations = get_locations_for_visiting_point(env.default_depot.lat, env.default_depot.lon, ts + 2000)
    push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time is None


@skip_if_remote
def test_too_old_date(env: Environment):
    route = create_route(env, '2019-12-02')
    set_company_routes_max_prolongation(env)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time is None


@skip_if_remote
def test_too_early_for_update(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)
    push_some_position(env, route)

    test_time = TEST_DATETIME.replace(hour=11)
    with freeze_time(test_time):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time is None


@skip_if_remote
def test_visited_and_left_all_orders(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)

    ts = POSITIONS_START_TIMESTAMP
    locations = get_locations_for_visiting_point(DEFAULT_ORDER['lat'], DEFAULT_ORDER['lon'], ts) + [
        (DEFAULT_ORDER['lat'] + 1, DEFAULT_ORDER['lon'] + 1, ts + 2000),
    ]
    push_positions(env, route['id'], locations)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time is None


@skip_if_remote
@pytest.mark.parametrize('new_finish_hour', [13, 22])
def test_increased_finish_time(env: Environment, new_finish_hour):
    route = create_route(env)
    set_company_routes_max_prolongation(env)
    push_some_position(env, route)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

    path = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}'
    data = {'route_finish': f'{new_finish_hour}:00'}
    local_patch(env.client, path, headers=env.user_auth_headers, data=data)

    with freeze_time(TEST_DATETIME) as frozen_time:
        # check that prolonged finish time didn't change
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route_obj = db.session.query(Route).get(route['id'])
            assert route_obj.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00')

        # set time to {new_finish_hour}:40
        frozen_time.tick(delta=timedelta(hours=new_finish_hour - 12, minutes=30))

        # check that prolongation continues after new finish time
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route_obj = db.session.query(Route).get(route['id'])
            assert route_obj.prolonged_finish_time == \
                   datetime.fromisoformat(f'2019-12-13T{new_finish_hour + 1}:00:00+03:00')


@skip_if_remote
def test_decreased_finish_time(env: Environment):
    route = create_route(env)
    set_company_routes_max_prolongation(env)
    push_some_position(env, route)

    with freeze_time(TEST_DATETIME):
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

    path = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}'
    data = {'route_finish': '01:00'}
    local_patch(env.client, path, headers=env.user_auth_headers, data=data)

    with freeze_time(TEST_DATETIME):
        # check that prolonged finish time didn't change
        with env.flask_app.app_context():
            task = ProlongRouteFinishTimeTask(env.flask_app)
            task.run({})

            route = db.session.query(Route).get(route['id'])
            assert route.prolonged_finish_time == datetime.fromisoformat('2019-12-13T13:20:00+03:00')


@skip_if_remote
@pytest.mark.parametrize("path_route_info, expected_count", [
    ('/api/v1/companies/{company_id}/route-info?date=2019-12-14&with_prolongation=false', 0),
    ('/api/v1/companies/{company_id}/route-info?date=2019-12-14', 0),
    ('/api/v1/companies/{company_id}/route-info?date=2019-12-14&with_prolongation=true', 1)
])
def test_route_info_with_prolongation(env: Environment, path_route_info, expected_count):
    route = create_route(env)
    set_company_routes_max_prolongation(env, 100)
    push_some_position(env, route)

    with freeze_time(TEST_DATETIME + timedelta(days=1)):
        with env.flask_app.app_context():
            route_from_db = db.session.query(Route).get(route['id'])
            while route_from_db.prolonged_finish_time is None or \
                    (route_from_db.prolonged_finish_time.astimezone(MOSCOW_TZ).date() <
                     datetime.now().astimezone(MOSCOW_TZ).date()):
                task = ProlongRouteFinishTimeTask(env.flask_app)
                task.run({})

                route_from_db = db.session.query(Route).get(route['id'])
            assert route_from_db.prolonged_finish_time == datetime.fromisoformat('2019-12-14T00:20:00+03:00')
            path_route_info = path_route_info.format(company_id=env.default_company.id)
            route_info = local_get(env.client, path_route_info, headers=env.superuser_auth_headers)
            assert len(route_info) == expected_count
