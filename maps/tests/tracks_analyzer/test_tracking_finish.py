import copy
import dateutil

from datetime import datetime

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_patch, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data

from ya_courier_backend.models import db, Route

DEFAULT_ORDER = {
    'number': 'default_order_number',
    'time_interval': '10:00-12:00',
    'address': 'some address',
    'lat': 55.791928,
    'lon': 37.841492,
}


def push_position(env, route_id):
    locations = [(55.801, 37.621, '2019-12-13T18:00:00+03:00')]
    path_push_positions = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    local_post(env.client, path_push_positions, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))


def get_positions(env, route_id):
    path_positions = f"/api/v1/companies/{env.default_company.id}/courier-position/{env.default_courier.id}/routes/{route_id}"
    return local_get(env.client, path_positions, headers=env.user_auth_headers)


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
            'date': '2019-12-13',
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


@skip_if_remote
def test_change_time_window(env: Environment):
    route = create_route(env)

    # Position is not recorded after max(time_windows.finish) + 5h
    push_position(env, route['id'])
    assert len(get_positions(env, route['id'])) == 0

    # Modify time_interval of an order
    order = copy.deepcopy(DEFAULT_ORDER)
    order['time_interval'] = '10:00-20:00'
    path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}/nodes"
    nodes = [{
        'type': 'order',
        'value': order
    }]
    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)

    # Position is now recorded
    push_position(env, route['id'])
    assert len(get_positions(env, route['id'])) == 1


@skip_if_remote
def test_prolongation(env: Environment):
    route = create_route(env)

    # Position is not recorded after max(time_windows.finish) + 5h
    push_position(env, route['id'])
    assert len(get_positions(env, route['id'])) == 0

    # Prolong finish_time to 19:00
    with env.flask_app.app_context():
        route_obj = db.session.query(Route).get(route['id'])
        route_obj.prolonged_finish_time = datetime(2019, 12, 13, 19, 0, 0, tzinfo=dateutil.tz.gettz("Europe/Moscow"))
        db.session.commit()

    # Position is now recorded
    push_position(env, route['id'])
    assert len(get_positions(env, route['id'])) == 1


@skip_if_remote
def test_increase_finish_time_after_prolongation(env: Environment):
    route = create_route(env)

    # Prolong finish_time to 17:00
    with env.flask_app.app_context():
        route_obj = db.session.query(Route).get(route['id'])
        route_obj.prolonged_finish_time = datetime(2019, 12, 13, 17, 0, 0, tzinfo=dateutil.tz.gettz("Europe/Moscow"))
        db.session.commit()

    # Position is not recorded after max(max(time_windows.finish) + 5h, route.prolonged_finish_time)
    push_position(env, route['id'])
    assert len(get_positions(env, route['id'])) == 0

    # Set rooute_finish higher than prolongation
    path = f'/api/v1/companies/{env.default_company.id}/routes/{route["id"]}'
    data = {'route_finish': '18:20'}
    local_patch(env.client, path, headers=env.user_auth_headers, data=data)

    # Position is now recorded
    push_position(env, route['id'])
    assert len(get_positions(env, route['id'])) == 1
