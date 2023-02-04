
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get
from datetime import datetime
from ya_courier_backend.models import OrderStatus
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
from http import HTTPStatus
from ya_courier_backend.models import TrackingToken


def _push_position(env, courier_id, route_id, locations):
    path_push_positions = f'/api/v1/couriers/{courier_id}/routes/{route_id}/push-positions'
    local_post(
        env.client,
        path_push_positions,
        headers=env.user_auth_headers,
        data=prepare_push_positions_data(locations)
    )


def _force_route_state_update(env, courier_id, route_id, location):
    lat, lon, time_str = location
    timestamp = datetime.strptime(time_str, '%Y-%m-%dT%H:%M:%S%z').timestamp()

    path_routed_orders = f'/api/v1/couriers/{courier_id}/routes/{route_id}/routed-orders'
    query_routed_orders = {
        'lat': lat,
        'lon': lon,
        'timestamp': timestamp
    }
    local_get(env.client, path_routed_orders, query=query_routed_orders, headers=env.user_auth_headers)


def _request_order_info(env, order_id):
    path_order_info = f'/api/v1/companies/{env.default_company.id}/orders/{order_id}/order-info'
    return local_get(env.client, path_order_info, headers=env.user_auth_headers)


def _mvrp_mock_task_uuid__ongoing_route(env):
    path_import = f'/api/v1/companies/{env.default_company.id}/mvrp_task'
    query_import = {'task_id': 'mock_task_uuid__ongoing_route'}
    [route] = local_post(env.client, path_import, query=query_import, headers=env.user_auth_headers)

    path_route_info = f'/api/v1/companies/{env.default_company.id}/route-info'
    query_route_info = {'route_id': route['id']}
    [route_info] = local_get(env.client, path_route_info, query=query_route_info, headers=env.user_auth_headers)

    order_ids = [e['value']['id'] for e in route_info['nodes']]

    return route, order_ids


@skip_if_remote
def test_order_info_processing(env: Environment):
    path_import = f'/api/v1/companies/{env.default_company.id}/mvrp_task'
    query_import = {'task_id': 'mock_task_uuid__ongoing_route'}
    [route] = local_post(env.client, path_import, query=query_import, headers=env.user_auth_headers)

    route_id = route['id']
    locations_values = [
        [(55.801, 37.621, f'{route["date"]}T09:40:00+03:00')],
        [(55.826326, 37.637686, f'{route["date"]}T10:40:00+03:00')]  # location of the first order
    ]

    path_route_info = f'/api/v1/companies/{env.default_company.id}/route-info'
    query_route_info = {'route_id': route_id}
    [route_info] = local_get(env.client, path_route_info, query=query_route_info, headers=env.user_auth_headers)

    order_ids = [e['value']['id'] for e in route_info['nodes']]

    # first order - initial state
    order_info = _request_order_info(env, order_ids[0])
    assert order_info.get('delivery_queue_position') is None
    assert order_info.get('status') == OrderStatus.new.value
    assert order_info.get('courier', {}).get('number') == env.default_courier.number
    assert order_info.get('courier', {}).get('location') is None
    assert order_info.get('courier', {}).get('location_updated_at') is None
    assert order_info.get('estimated_delivery_time') is None

    # first order - update courier location
    _push_position(env, env.default_courier.id, route_id, locations_values[0])
    order_info = _request_order_info(env, order_ids[0])
    assert order_info.get('courier', {}).get('location', {}).get('lat') == locations_values[0][0][0]
    assert order_info.get('courier', {}).get('location', {}).get('lon') == locations_values[0][0][1]
    assert order_info.get('courier', {}).get('location_updated_at', {}).get('text') == locations_values[0][0][2]

    # first order - update route state
    _force_route_state_update(env, env.default_courier.id, route_id, locations_values[0][0])
    order_info = _request_order_info(env, order_ids[0])
    assert order_info.get('delivery_queue_position') == 0
    assert order_info.get('estimated_delivery_time') is not None
    assert order_info.get('estimated_delivery_time', {}).get('value') is not None
    assert order_info.get('estimated_delivery_time', {}).get('text') is not None

    # first order - delivered
    _push_position(env, env.default_courier.id, route_id, locations_values[1])
    order_info = _request_order_info(env, order_ids[0])
    assert order_info.get('status') == OrderStatus.finished.value

    _force_route_state_update(env, env.default_courier.id, route_id, locations_values[1][0])
    order_info = _request_order_info(env, order_ids[0])
    assert order_info.get('estimated_delivery_time') is None

    # second order - delivery_queue_position is 0 now
    order_info = _request_order_info(env, order_ids[1])
    assert order_info.get('delivery_queue_position') == 0


@skip_if_remote
def test_invalid_order_id(env: Environment):
    company_id = env.default_company.id
    order_id = 1234
    path_order_info = f'/api/v1/companies/{company_id}/orders/{order_id}/order-info'
    local_get(env.client, path_order_info, headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_auth_fails(env: Environment):
    company_id = env.default_company.id
    path_import = f'/api/v1/companies/{company_id}/mvrp_task'
    query_import = {'task_id': 'mock_task_uuid__generic'}
    routes = local_post(env.client, path_import, query=query_import, headers=env.user_auth_headers)

    route_id = routes.pop()['id']
    path_route_info = f'/api/v1/companies/{env.default_company.id}/route-info'
    query_route_info = {'route_id': route_id}
    [route_info] = local_get(env.client, path_route_info, query=query_route_info, headers=env.user_auth_headers)

    # bad header
    order_id = route_info['nodes'].pop()['value']['id']
    path_order_info = f'/api/v1/companies/{company_id}/orders/{order_id}/order-info'
    local_get(env.client, path_order_info, headers={'Authorization': 'test_user:bad_password'}, expected_status=HTTPStatus.UNAUTHORIZED)

    # bad company
    company_id = 3
    path_order_info = f'/api/v1/companies/{company_id}/orders/{order_id}/order-info'
    local_get(env.client, path_order_info, headers=env.user_auth_headers, expected_status=HTTPStatus.FORBIDDEN)


@skip_if_remote
def test_tracking_url_created_by_route_state(env: Environment):
    route, order_ids = _mvrp_mock_task_uuid__ongoing_route(env)

    with env.flask_app.app_context():
        initial_tokens = TrackingToken.query.all()

    location = (env.default_depot.lon, env.default_depot.lat, f'{route["date"]}T09:00:00+03:00')
    _force_route_state_update(env, env.default_courier.id, route['id'], location)

    with env.flask_app.app_context():
        tokens = TrackingToken.query.all()
        # There are 2 orders in this mock_tack.
        # One of the orders has len(shared_with_company_ids) == 1, so func 'update_route_state' created 3 tokens
        assert len(tokens) == 3 + len(initial_tokens)
        checker_tokens = {elem.tracking_url: elem.as_dict() for elem in tokens}

    order_info = _request_order_info(env, order_ids[0])

    tracking_url = order_info.get('tracking_url')
    assert tracking_url is not None
    assert checker_tokens.get(tracking_url).get('company_id') == env.default_company.id
    assert checker_tokens.get(tracking_url).get('order_id') == int(order_ids[0])


@skip_if_remote
def test_tracking_url_created_by_order_info(env: Environment):
    _, order_ids = _mvrp_mock_task_uuid__ongoing_route(env)

    with env.flask_app.app_context():
        initial_tokens = TrackingToken.query.all()

    order_info = _request_order_info(env, order_ids[0])
    assert order_info.get('tracking_url') is not None

    with env.flask_app.app_context():
        tokens = TrackingToken.query.all()
        assert len(tokens) == 1 + len(initial_tokens)

    token = tokens[0]
    assert token.company_id == env.default_company.id
    assert token.order_id == int(order_ids[0])
