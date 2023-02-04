import copy
import datetime
from http import HTTPStatus
from threading import Thread

from freezegun import freeze_time
from dateutil.tz import gettz
import time
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    create_empty_route,
    local_get,
    local_post,
    local_patch,
    local_delete,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
import pytest

from ya_courier_backend.models.model import TrackingRouteStatUpdatesMixin
from ya_courier_backend.tasks.update_route_stat_task import UpdateRouteStatTask
from ya_courier_backend.tasks.check_courier_connection_loss import CheckCourierConnectionLossTask
from ya_courier_backend.models.routing_mode import RoutingMode
from ya_courier_backend.models import (
    Courier, db,
    Depot, DepotInstance, DepotInstanceHistoryEvent, LastCourierPosition, OrderStatus, RouteStatUpdateQueue,
    Route,
    RouteStat,
    CourierPosition,
    RouteEvent,
    RouteEventType,
    Order,
    get_route_state_for_update
)
from ya_courier_backend.models.route_stat_update_queue import (
    SIGNAL_ROUTE_STAT_UPDATE_FAILED_COUNT,
    SIGNAL_ROUTE_STAT_UPDATE_QUEUE_SIZE,
)
from ya_courier_backend.util.order_time_windows import convert_time_offset_to_date
from ya_courier_backend.util.tasks.unistat import init_tasks_unistat_signals
from ya_courier_backend.util.courier_idle import DEFAULT_COURIER_IDLE_CONFIG

SFX_SIGNAL_ROUTE_STAT_UPDATE_FAILED_COUNT = SIGNAL_ROUTE_STAT_UPDATE_FAILED_COUNT + '_summ'
SFX_SIGNAL_ROUTE_STAT_UPDATE_QUEUE_SIZE = SIGNAL_ROUTE_STAT_UPDATE_QUEUE_SIZE + '_axxx'
DEFAULT_COURIER_IDLE_TIME = DEFAULT_COURIER_IDLE_CONFIG['time_window']


def _get_unistat_response_value(env, signal_name):
    path_get = '/api/v1/unistat'
    unistat_response = local_get(env.client, path_get, headers=env.user_auth_headers)
    unistat_dict = {item[0]: item[1] for item in unistat_response}

    return unistat_dict[signal_name]


def create_custom_route(env, route_number='1', depot_number=None, custom_fields=None, expected_status=HTTPStatus.OK):
    path_route = f'/api/v1/companies/{env.default_company.id}/routes'
    route_data = {
        'number': route_number,
        'courier_number': env.default_courier.number,
        'depot_number': depot_number or env.default_depot.number,
        'date': datetime.date.today().isoformat(),
        'route_start': '1',
        'route_finish': '2',
    }
    if custom_fields is not None:
        route_data['custom_fields'] = custom_fields
    return local_post(
        env.client, path_route, headers=env.user_auth_headers, data=route_data, expected_status=expected_status
    )


def create_depot(env, depot_number=None, expected_status=HTTPStatus.OK):
    depot_data = {
        'number': depot_number or env.default_depot.number,
        'time_interval': '00:00-23:59',
        'address': 'some address',
        'lat': 55.791928,
        'lon': 37.841492,
        'time_zone': 'Europe/Moscow'
    }
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots'
    return local_post(
        env.client, depot_path, headers=env.user_auth_headers, data=depot_data, expected_status=expected_status
    )


def test_route_stat_update_task_is_created(env: Environment):
    with env.flask_app.app_context():
        route_data = create_empty_route(env)

        RouteStatUpdateQueue.put(route_ids=[route_data['id']])
        route_task = db.session.query(RouteStatUpdateQueue) \
            .filter(RouteStatUpdateQueue.route_id == route_data['id']).first()
        assert route_task


def test_route_stat_update_courier_is_created(env: Environment):
    with env.flask_app.app_context():
        path = f"/api/v1/companies/{env.default_company.id}/couriers"
        courier = local_post(
            env.client,
            path,
            headers=env.user_auth_headers,
            data={'name': 'courier_name', 'number': '1', 'sms_enabled': False},
        )
        route = create_custom_route(env)

        local_patch(
            client=env.client,
            headers=env.user_auth_headers,
            path=f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}",
            data={'courier_id': courier['id']},
            expected_status=HTTPStatus.OK,
        )

        db.session.query(RouteStatUpdateQueue).delete()  # Delete all previous objects

        local_patch(
            client=env.client,
            headers=env.user_auth_headers,
            path=f"/api/v1/companies/{env.default_company.id}/couriers/{courier['id']}",
            data={'number': '3'},
            expected_status=HTTPStatus.OK,
        )

        route_task = db.session.query(RouteStatUpdateQueue).first()
        assert route_task.route_id == route['id']


def test_route_stat_update_depot_is_created(env: Environment):
    with env.flask_app.app_context():
        path= f"/api/v1/companies/{env.default_company.id}/depots"

        depot_data = {
            'number': '2020',
            'time_interval': '00:00-23:59',
            'address': 'some address',
            'lat': 55.791928,
            'lon': 37.841492,
        }

        depot = local_post(env.client, path, headers=env.user_auth_headers, data=depot_data)

        route = create_custom_route(env)

        local_patch(
            client=env.client,
            headers=env.user_auth_headers,
            path=f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}",
            data={'depot_id': depot['id']},
            expected_status=HTTPStatus.OK,
        )

        db.session.query(RouteStatUpdateQueue).delete()  # Delete all previous objects

        local_patch(
            client=env.client,
            headers=env.user_auth_headers,
            path=f"/api/v1/companies/{env.default_company.id}/depots/{depot['id']}",
            data={'number': '3'},
            expected_status=HTTPStatus.OK,
        )

        route_task = db.session.query(RouteStatUpdateQueue).first()
        assert route_task.route_id == route['id']


def test_route_stat_update_courier_position(env: Environment):
    with env.flask_app.app_context():
        create_custom_route(env)
        db.session.query(RouteStatUpdateQueue).delete()

        values = [{
            'route_id': 2,
            'courier_id': 1,
            'point': 'SRID=4326;POINT(55.753693 37.6727)',
            'lat': 55.753693,
            'lon': 37.6727,
            'time': 1576216800.0,
            'accuracy': 20,
            'server_time': 1654175372.4251769,
            'imei': None
        }]
        CourierPosition.insert_courier_positions(values)
        db.session.commit()

        route_task = db.session.query(RouteStatUpdateQueue).all()
        assert route_task


def test_route_stat_update_last_courier_position(env: Environment):
    task_id = "mock_task_uuid__result_with_with_depot_and_garage_in_the_end"
    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route, _ = local_post(env.client, path_import, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    with env.flask_app.app_context():
        db.session.query(RouteStatUpdateQueue).delete()

    arrival_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
    locations = [(55.753693, 37.6727, arrival_time)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    with env.flask_app.app_context():
        route_task = db.session.query(RouteStatUpdateQueue).all()
        assert route_task


def test_handler_func_is_called_on_route_stat_update_task_processing(env: Environment):
    with env.flask_app.app_context():
        route_data = create_empty_route(env)

        RouteStatUpdateQueue.put(route_ids=[route_data['id']])
        func_is_called = False

        def process_func(_):
            nonlocal func_is_called
            func_is_called = True

        RouteStatUpdateQueue.process_queued_task(process_func)
        RouteStatUpdateQueue.process_queued_task(process_func)  # checking that we don't delete task if it's None

        assert func_is_called


def test_route_queue_task_is_added_on_route_add(env: Environment):
    with env.flask_app.app_context():
        db.session.query(RouteStatUpdateQueue).delete()
        create_empty_route(env)
        assert db.session.query(RouteStatUpdateQueue).all()


def test_route_stat_update_task_is_removed_after_processing(env: Environment):
    with env.flask_app.app_context():
        create_empty_route(env)

        new_task = RouteStatUpdateQueue.query.order_by(RouteStatUpdateQueue.created_at) \
            .first()
        processed_task_id = None

        def store_task(task):
            nonlocal processed_task_id
            processed_task_id = task.id

        RouteStatUpdateQueue.process_queued_task(store_task)
        assert new_task.id == processed_task_id
        assert not RouteStatUpdateQueue.query.filter(RouteStatUpdateQueue.id == new_task.id).first()


def test_route_queue_task_is_added_on_route_violated(env: Environment):
    with env.flask_app.app_context():
        db.session.query(RouteStatUpdateQueue).delete()
        Route.set_route_as_violated(env.default_route.id)
        assert db.session.query(RouteStatUpdateQueue).all()


def test_route_stat_update_queue_insert(env: Environment):
    with env.flask_app.app_context():
        route_data = create_custom_route(env)

        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stat = db.session.query(RouteStat).filter(RouteStat.route_id == route_data['id']).first()

        assert route_stat.route_id == route_data['id']
        assert route_stat.company_id == route_data['company_id']
        assert route_stat.route_number == route_data['number']
        assert route_stat.routing_mode.value == route_data['routing_mode']
        assert route_stat.depot_id == route_data['depot_id']
        assert route_stat.courier_id == route_data['courier_id']


def test_route_stat_update_queue_update(env: Environment):
    courier_number = 'test_courier_number'
    courier_name = 'test_courier_name'
    depot_number = 'test_depot_number'

    with env.flask_app.app_context():
        create_depot(env, depot_number=depot_number)

        route_data = create_custom_route(env, depot_number=depot_number)

        courier = db.session.query(Courier).filter(Courier.id == route_data['courier_id']).one()
        courier.number = courier_number
        courier.name = courier_name
        db.session.add(courier)
        db.session.commit()

        route_stat = RouteStat(
            route_id=route_data['id'],
            company_id=2,
            route_number='0',
            routing_mode=RoutingMode.truck,
            depot_id=2,
            courier_id=2,
        )

        db.session.add(route_stat)
        db.session.commit()

        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stat: RouteStat = db.session.query(RouteStat).filter(RouteStat.route_id == route_data['id']).first()

        assert route_stat.route_id == route_data['id']
        assert route_stat.company_id == route_data['company_id']
        assert route_stat.route_number == route_data['number']
        assert route_stat.routing_mode.value == route_data['routing_mode']
        assert route_stat.depot_id == route_data['depot_id']
        assert route_stat.courier_id == route_data['courier_id']
        assert route_stat.courier_name == courier_name
        assert route_stat.courier_number == courier_number
        assert route_stat.depot_number == depot_number


def test_route_stat_update_failed_count_unistat_signal(env: Environment):
    init_tasks_unistat_signals()

    with env.flask_app.app_context():
        route_data = create_empty_route(env)

        RouteStatUpdateQueue.put(route_ids=[route_data['id']])

        def process_func(_):
            raise RuntimeError

        RouteStatUpdateQueue.process_queued_task(process_func)

    assert _get_unistat_response_value(env, SFX_SIGNAL_ROUTE_STAT_UPDATE_FAILED_COUNT) == 1


def test_route_stat_update_queue_size_unistat_signal(env: Environment):
    with env.flask_app.app_context():
        route_data = create_empty_route(env)
        RouteStatUpdateQueue.put(route_ids=[route_data['id']])
        db.session.commit()

    queue_size = _get_unistat_response_value(env, SFX_SIGNAL_ROUTE_STAT_UPDATE_QUEUE_SIZE)

    with env.flask_app.app_context():
        def process_func(_):
            raise RuntimeError

        RouteStatUpdateQueue.process_queued_task(process_func)

        db.session.commit()

    assert _get_unistat_response_value(env, SFX_SIGNAL_ROUTE_STAT_UPDATE_QUEUE_SIZE) == queue_size - 1


def test_route_not_added_twice(env: Environment):
    with env.flask_app.app_context():
        RouteStatUpdateQueue.put(route_ids=[env.default_route.id])
        db.session.commit()
        RouteStatUpdateQueue.put(route_ids=[env.default_route.id])
        db.session.commit()

        assert db.session.query(RouteStatUpdateQueue).count() == 1


def test_route_stat_queue_updated_on_route_events(env: Environment):
    import_path = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id=mock_task_uuid__generic'
    route_id = local_post(env.client, import_path, headers=env.user_auth_headers)[0]['id']
    path_route_info = f'/api/v1/companies/{env.default_company.id}/route-info?route_id={route_id}'
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    now = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    start_datetime = datetime.datetime.fromtimestamp(now).astimezone(gettz('Europe/Moscow'))

    with env.flask_app.app_context(), freeze_time(start_datetime) as freezed_time:
        now = time.time()
        task = CheckCourierConnectionLossTask(env.flask_app)
        db.session.query(RouteStatUpdateQueue).filter(RouteStatUpdateQueue.route_id == route_id).delete()

        locations = [
            (58.82, 37.73, now),
            (58.82, 37.73, now + DEFAULT_COURIER_IDLE_TIME / 2),
            (58.82, 37.73, now + DEFAULT_COURIER_IDLE_TIME)
        ]
        local_post(env.client, f'/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions',
                   headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        update_route(env, route_id, {'lat': 58.82, 'lon': 37.73, 'timestamp': now})

        route_events = db.session.query(RouteEvent).all()
        assert len(route_events) == 1
        assert route_events[0].type == RouteEventType.IDLE
        assert 0 < db.session.query(RouteStatUpdateQueue).filter(RouteStatUpdateQueue.route_id == route_id).count()
        db.session.query(RouteStatUpdateQueue).filter(RouteStatUpdateQueue.route_id == route_id).delete()

        freezed_time.tick(delta=datetime.timedelta(seconds=env.default_company.courier_connection_loss_s + 1))
        task.run('')

        route_events = db.session.query(RouteEvent).all()
        assert any(event.type == RouteEventType.COURIER_CONNECTION_LOSS for event in route_events)
        assert 0 < db.session.query(RouteStatUpdateQueue).filter(RouteStatUpdateQueue.route_id == route_id).count()


DEFAULT_ORDER = {
    'time_interval': '19:00-20:00',
    'address': 'some address',
    'lat': 55.791928,
    'lon': 37.841492,
}


class OrdersFabric:
    _next_order_number = 1

    @staticmethod
    def _generate_order_number():
        number = f'order_{OrdersFabric._next_order_number}'
        OrdersFabric._next_order_number += 1
        return number

    @staticmethod
    def get_order(status, lat=None, lon=None):
        order = copy.deepcopy(DEFAULT_ORDER)
        order['number'] = OrdersFabric._generate_order_number()
        order['status'] = status

        if lat:
            order['lat'] = lat
        if lon:
            order['lon'] = lon

        return order

    @staticmethod
    def get_orders(count, *args, **kwargs):
        return [OrdersFabric.get_order(*args, **kwargs) for _ in range(count)]


@pytest.mark.parametrize('orders_config', [
    {
        'categories': {
            'arrived_late': {'finished': 2},
            'arrived_on_time': {'finished': 3},
            'cancelled': {'cancelled': 1, 'postponed': 4},
            'estimated_late': {'confirmed': 2},
            'estimated_on_time': {'confirmed': 2},
            'no_estimation': {'confirmed': 1},
        },

        'delivery_time': '2020-11-30T19:30:00',
        'late_delivery_time': '2020-11-30T21:30:00',
        'estimated_delivery_time': '2020-11-30T19:30:00',
        'estimated_late_delivery_time': '2020-11-30T21:30:00'
    },
    {
        'categories': {
            'arrived_late': {'finished': 2},
            'arrived_on_time': {'finished': 3},
            'cancelled': {'cancelled': 1, 'postponed': 4},
            'estimated_late': {'confirmed': 2},
            'estimated_on_time': {'confirmed': 2},
            'no_estimation': {'confirmed': 1},
        },

        'delivery_time': '2020-11-30T11:30:00',
        'late_delivery_time': '2020-11-30T21:30:00',
        'estimated_delivery_time': '2020-11-30T11:30:00',
        'estimated_late_delivery_time': '2020-11-30T21:30:00'
    }
])
def test_processing_order_nodes(env: Environment, orders_config):
    orders_by_categories = orders_config['categories']

    with env.flask_app.app_context():
        route_data = create_empty_route(env)

        db.session.add(env.default_depot)
        db.session.commit()

        orders = []
        arrived_on_time_order_numbers = []
        arrived_late_order_numbers = []
        estimated_late_order_numbers = []
        estimated_on_time_order_numbers = []

        # Generating orders with different statuses
        for order_category, quantity_by_status in orders_by_categories.items():
            cur_orders = []
            for order_status, orders_quantity in quantity_by_status.items():
                cur_orders += OrdersFabric.get_orders(count=orders_quantity, status=order_status)
            orders += cur_orders

            if order_category == 'arrived_late':
                arrived_late_order_numbers += [order['number'] for order in cur_orders]
            elif order_category == 'arrived_on_time':
                arrived_on_time_order_numbers += [order['number'] for order in cur_orders]
            elif order_category == 'estimated_late':
                estimated_late_order_numbers += [order['number'] for order in cur_orders]
            elif order_category == 'estimated_on_time':
                estimated_on_time_order_numbers += [order['number'] for order in cur_orders]

        # Adding nodes to the route
        nodes = [{'type': 'order', 'value': order} for order in orders]
        path_node = f"/api/v1/companies/{env.default_company.id}/routes/{route_data['id']}/nodes"
        local_post(env.client, path_node, headers=env.user_auth_headers, data=nodes)

        # Adding delivery time to completed orders

        # In this case, the delivery is completed on time
        for order_number in arrived_on_time_order_numbers:
            order: Order = db.session.query(Order).filter(Order.number == order_number).first()
            order.delivered_at = datetime.datetime.fromisoformat(orders_config['delivery_time'])
            db.session.add(order)

        # And in this case, the courier was late
        arrived_late_duration_s = (
            datetime.datetime.fromisoformat(orders_config['late_delivery_time']) -
            (datetime.datetime.fromisoformat(route_data['date']) + datetime.timedelta(hours=20))
        ).total_seconds()
        arrived_late_duration_s *= len(arrived_late_order_numbers)
        for order_number in arrived_late_order_numbers:
            order: Order = db.session.query(Order).filter(Order.number == order_number).first()
            order.delivered_at = datetime.datetime.fromisoformat(orders_config['late_delivery_time'])
            db.session.add(order)

        db.session.commit()

        # Adding the estimated delivery time to incomplete orders
        routed_orders = []

        # Here the courier is late
        estimated_late_duration_s = (
            datetime.datetime.fromisoformat(orders_config['estimated_late_delivery_time']) -
            (datetime.datetime.fromisoformat(route_data['date']) + datetime.timedelta(hours=20))
        ).total_seconds()
        estimated_late_duration_s *= len(estimated_late_order_numbers)
        for order_number in estimated_late_order_numbers:
            order: Order = db.session.query(Order).filter(Order.number == order_number).first()
            routed_orders.append({
                'arrival_time_s': (
                    datetime.datetime.fromisoformat(orders_config['estimated_late_delivery_time']) -
                    datetime.datetime.fromisoformat(route_data['date'])
                ).total_seconds(),
                'id': order.id}
            )

        # And here the delivery goes according to plan
        for order_number in estimated_on_time_order_numbers:
            order: Order = db.session.query(Order).filter(Order.number == order_number).first()
            routed_orders.append({
                'arrival_time_s': (
                    datetime.datetime.fromisoformat(orders_config['estimated_delivery_time']) -
                    datetime.datetime.fromisoformat(route_data['date'])
                ).total_seconds(),
                'id': order.id}
            )

        route_state = get_route_state_for_update(route_data['id'])
        route_state.state = {
            'routed_orders': routed_orders,
            'fixed_orders': []
        }
        db.session.add(route_state)
        db.session.commit()

        # Generating statistics for the route
        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stat: RouteStat = db.session.query(RouteStat).filter(RouteStat.route_id == route_data['id']).first()

        assert route_stat
        assert route_stat.orders_count == len(orders)
        assert sorted(route_stat.orders_numbers) == sorted([order['number'] for order in orders])

        assert route_stat.orders_arrived_late_count == sum(orders_by_categories['arrived_late'].values())
        assert route_stat.orders_arrived_late_duration_s == arrived_late_duration_s
        assert route_stat.orders_arrived_on_time_count == sum(orders_by_categories['arrived_on_time'].values())
        assert route_stat.orders_canceled_count == sum(orders_by_categories['cancelled'].values())
        assert route_stat.orders_estimated_late_count == sum(orders_by_categories['estimated_late'].values())
        assert route_stat.orders_estimated_late_duration_s == estimated_late_duration_s
        assert route_stat.orders_estimated_on_time_count == sum(orders_by_categories['estimated_on_time'].values())
        assert route_stat.orders_no_estimation_count == sum(orders_by_categories['no_estimation'].values())


def test_processing_courier_position(env: Environment):
    courier_position_lat = 55.753693
    courier_position_lon = 37.6727

    with env.flask_app.app_context():
        create_custom_route(env)

        values = [{
            'route_id': 2,
            'courier_id': 1,
            'point': 'SRID=4326;POINT(55.753693 37.6727)',
            'lat': courier_position_lat,
            'lon': courier_position_lon,
            'time': 1576216800.0,
            'accuracy': 20,
            'server_time': 1654175372.4251769,
            'imei': None,
        }]
        courier_position_id = CourierPosition.insert_courier_positions(values)[0].id
        last_courier_position = LastCourierPosition(route_id=2, position_id=courier_position_id)
        db.session.add(last_courier_position)
        db.session.commit()

        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stat: RouteStat = db.session.query(RouteStat).filter(RouteStat.route_id == 2).one()

        assert route_stat.courier_position_lat == courier_position_lat
        assert route_stat.courier_position_lon == courier_position_lon


def test_thread_local_storage(env: Environment):
    thread_storage = set()

    def put_task_to_queue_in_thread():
        TrackingRouteStatUpdatesMixin.changed_routes().add(env.default_route.id)
        thread_storage.update(TrackingRouteStatUpdatesMixin.changed_routes())

    thread = Thread(target=put_task_to_queue_in_thread)
    thread.start()
    thread.join()
    assert thread_storage == {env.default_route.id}
    assert TrackingRouteStatUpdatesMixin.changed_routes() == set()


def test_processing_transit_params_and_stops(env: Environment):
    depot_number = '2020'

    with env.flask_app.app_context():
        depot_data = create_depot(env, depot_number=depot_number)
        depot = db.session.query(Depot).filter(Depot.number == depot_data['number']).one()

        route_data = {
            'number': '1',
            'courier_number': env.default_courier.number,
            'depot_number': depot_number,
            'date': '2020-11-30',
            'route_start': '1',
            'route_finish': '2',
        }
        route_path = f'/api/v1/companies/{env.default_company.id}/routes'
        route_data = local_post(env.client, route_path, headers=env.user_auth_headers, data=route_data)
        route = db.session.query(Route).filter(Route.id == route_data['id']).one()

        finished_orders = OrdersFabric.get_orders(3, status=OrderStatus.finished.value, lat=1, lon=2)
        confirmed_orders = OrdersFabric.get_orders(2, status=OrderStatus.confirmed.value, lat=2, lon=3)
        cancelled_orders = OrdersFabric.get_orders(1, status=OrderStatus.cancelled.value, lat=3, lon=4)

        nodes = [{'type': 'order', 'value': order} for order in finished_orders + confirmed_orders + cancelled_orders]
        nodes += [{'type': 'depot', 'value': {'number': depot.number, 'status': 'visited'}}]
        path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route_data["id"]}/nodes'
        local_post(env.client, path_node, headers=env.user_auth_headers, data=nodes)

        depot_instance = db.session.query(DepotInstance).filter(DepotInstance.route_id == route.id).one()
        depot_instance.add_history_event(DepotInstanceHistoryEvent.visit, details={
            'position': {'time': convert_time_offset_to_date(12, route.date).isoformat()}
        })

        for order in finished_orders:
            order: Order = db.session.query(Order).filter(Order.number == order['number']).first()
            order.delivered_at = datetime.datetime.fromisoformat('2020-11-30T19:30:00')
            db.session.add(order)
        db.session.commit()

        db.session.query(RouteStatUpdateQueue).delete()
        db.session.commit()
        db.session.add(RouteStatUpdateQueue(route_id=route.id))
        db.session.commit()

        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stat: RouteStat = db.session.query(RouteStat) \
            .filter(RouteStat.route_id == route_data['id']).one()

        assert route_stat.route_start == convert_time_offset_to_date(
            route.route_start_s, route.date, tzinfo=gettz(depot.time_zone)
        )
        assert route_stat.route_finish == convert_time_offset_to_date(
            route.route_finish_s, route.date, tzinfo=gettz(depot.time_zone)
        )
        assert route_stat.route_real_start.timestamp() == convert_time_offset_to_date(12, route.date).timestamp()
        assert route_stat.route_real_finish.timestamp() == convert_time_offset_to_date(route.route_finish_s, route.date).timestamp()
        assert route_stat.route_transit_duration_s == \
               convert_time_offset_to_date(route.route_finish_s, route.date).timestamp() - \
               convert_time_offset_to_date(12, route.date).timestamp()

        assert route_stat.stops_count == 4


def test_processing_idles(env: Environment):
    IDLES_COUNT = 9
    time_now = time.time()

    with env.flask_app.app_context():
        route_data = create_empty_route(env)

        for i in range(IDLES_COUNT):
            route_event = RouteEvent(
                type=RouteEventType.IDLE, route_id=route_data['id'], courier_id=route_data['courier_id'],
                start_timestamp=time_now - 3 ** i, finish_timestamp=time_now,
                company_id=route_data['company_id'], lon=i, lat=i ** 2)
            db.session.add(route_event)
        for i in range(2):
            route_event = RouteEvent(
                type=RouteEventType.COURIER_CONNECTION_LOSS, route_id=route_data['id'],
                courier_id=route_data['courier_id'],
                start_timestamp=time_now - 2 ** i, company_id=route_data['company_id'], lon=i, lat=i ** 2)
            db.session.add(route_event)
        db.session.commit()

        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stat: RouteStat = db.session.query(RouteStat) \
            .filter(RouteStat.route_id == route_data['id']).one()

        assert route_stat.idles_count == IDLES_COUNT

        # Idles duration is the sum of the time differences between the finish and start of each route event
        # with the IDLE type. Since the finish time of each event in the test is time_now, and the start time is
        # time_now - 3^i, the sum of such differences for i from 0 to IDLES_COUNT - 1 will be equal to
        # 3^0 + 3^1 + ... + 3^(IDLES_COUNT-1)
        assert route_stat.idles_duration_s == sum([3 ** i for i in range(IDLES_COUNT)])


def test_processing_route_run_numbers(env: Environment):
    create_custom_route(env, route_number='1')
    create_custom_route(env, route_number='2')

    route_third = create_custom_route(env, route_number='3')
    route_fourth = create_custom_route(env, route_number='4')

    path = f'/api/v1/companies/{env.default_company.id}/couriers'

    data = {'number': '2023'}

    local_post(env.client, path, headers=env.user_auth_headers, data=data)

    path_third = f"/api/v1/companies/{env.default_company.id}/routes/{route_third['id']}"
    path_fourth = f"/api/v1/companies/{env.default_company.id}/routes/{route_fourth['id']}"

    data = {'courier_id': 3}

    local_patch(env.client, path_third, headers=env.user_auth_headers, data=data)
    local_patch(env.client, path_fourth, headers=env.user_auth_headers, data=data)

    with env.flask_app.app_context():
        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stats = db.session.query(RouteStat).all()

        # with courier_id = 1
        assert route_stats[0].run_number == 1
        assert route_stats[1].run_number == 2
        assert route_stats[2].run_number == 3

        # with courier_id = 3
        assert route_stats[3].run_number == 1
        assert route_stats[4].run_number == 2


def test_processing_route_run_numbers_after_deleting_route(env: Environment):
    # Initially we had route_ids 1, 2 and 3 associated with courier_id 1
    # After deleting we have only route_ids 1 and 3 associated with courier_id 1
    route_first = create_custom_route(env, route_number='1')
    create_custom_route(env, route_number='2')

    with env.flask_app.app_context():
        for route in db.session.query(Route).all():
            route.prolonged_finish_time = datetime.datetime.now() + datetime.timedelta(hours=1)
        db.session.commit()

    with env.flask_app.app_context():
        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

    path = f"/api/v1/companies/{env.default_company.id}/routes/{route_first['id']}"
    local_delete(env.client, path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stats = db.session.query(RouteStat).all()
        assert len(route_stats) == 2
        assert route_stats[0].run_number == 1
        assert route_stats[1].run_number == 2


def test_processing_route_run_numbers_after_patching_courier_id(env: Environment):
    route_first = create_custom_route(env, route_number='1')
    create_custom_route(env, route_number='2')

    with env.flask_app.app_context():
        for route in db.session.query(Route).all():
            route.prolonged_finish_time = datetime.datetime.now() + datetime.timedelta(hours=1)
        db.session.commit()

    with env.flask_app.app_context():
        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

    path = f'/api/v1/companies/{env.default_company.id}/couriers'

    data = {'number': '2023'}

    local_post(env.client, path, headers=env.user_auth_headers, data=data)
    path_first = f"/api/v1/companies/{env.default_company.id}/routes/{route_first['id']}"

    data = {'courier_id': 3}

    local_patch(env.client, path_first, headers=env.user_auth_headers, data=data)

    with env.flask_app.app_context():
        task = UpdateRouteStatTask(env.flask_app)
        task.run({})

        route_stats = db.session.query(RouteStat).all()

        # with courier_id = 1
        assert route_stats[0].run_number == 1
        assert route_stats[2].run_number == 2

        # with courier_id = 3
        assert route_stats[1].run_number == 1
