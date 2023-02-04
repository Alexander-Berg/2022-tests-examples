import copy
import pytest
import time
from datetime import datetime, timedelta

from dateutil.tz import gettz
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import solver_request_by_task_id
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    local_delete, local_patch, local_post, local_get, add_courier,
    edit_routes_in_batch_mode,
    set_default_route_time_interval, Environment
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import (
    add_route_to_db, get_courier_by_number_and_company_id, update_route, set_company_import_depot_garage,
    set_dirty_level,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
from ya_courier_backend.models import (
    db, RouteEvent, LogisticCompany, Order, Route, PushNotification, PushNotificationLog, PushNotificationStatus,
    PushNotificationType, RouteEventType
)
from ya_courier_backend.tasks.check_courier_connection_loss import CheckCourierConnectionLossTask
from ya_courier_backend.tasks.send_push_notifications import SendPushNotificationsTask
from ya_courier_backend.util.push_notification import Push
from ya_courier_backend.util.util import get_by_id, get_by_number

TESTING_COURIER_PHONE = '01112223344'
TESTING_COURIER_NUMBER = '2021'
TESTING_ROUTE_NUMBER = '11'
ROUTE_EVENT_FIELDS = {
    'id': 1,
    'type': RouteEventType.COURIER_CONNECTION_LOSS,
    'start_timestamp': 1628073350.0,
    'finish_timestamp': 1628073351.0,
    'lat': 58.82,
    'lon': 37.73
}

TEST_ORDER = {
    'number': 'default_order_number',
    'time_interval': '00:00-23:59',
    'address': 'some address',
    'lat': 55.791928,
    'lon': 37.841492,
}


def _prepare_route(env: Environment, route_id, nodes=None):
    path_node = f'/api/v1/companies/{env.default_company.id}/routes/{route_id}/nodes'
    if nodes is None:
        nodes = [{'type': 'order', 'value': TEST_ORDER}]

    local_post(env.client,
               path_node,
               headers=env.user_auth_headers,
               data=nodes)


def enable_push_notifications(env: Environment, push_notification_types):
    with env.flask_app.app_context():
        env.default_company = db.session.query(LogisticCompany) \
            .filter(LogisticCompany.id == env.default_company.id) \
            .first()
        env.default_company.enabled_push_notifications = push_notification_types
        db.session.flush()
        db.session.expunge(env.default_company)
        db.session.commit()


def _get_request(env: Environment, courier_number, number=None):
    random_date = '2021-01-01'
    request = {'courier_number': courier_number, 'depot_number': env.default_depot.number, 'date': random_date}
    if number is not None:
        request['number'] = number
    return request


def _get_notifications(env: Environment):
    with env.flask_app.app_context():
        notifications = db.session.query(PushNotification).all()
        return notifications


def _get_notifications_by_courier_id(env: Environment, courier_id):
    with env.flask_app.app_context():
        notifications = db.session.query(PushNotification) \
            .filter(PushNotification.courier_id == courier_id) \
            .all()

    return notifications


def _get_idle_events(env: Environment, route_id):
    with env.flask_app.app_context():
        events = db.session.query(RouteEvent) \
            .filter(
                RouteEvent.route_id == route_id,
                RouteEvent.courier_id == env.default_courier.id,
                RouteEvent.type == RouteEventType.IDLE
            ) \
            .all()

    return events


def _update_courier_on_route(env: Environment, courier_number, route_id):
    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route_id}"
    route_data = _get_request(env, courier_number)
    return local_patch(env.client, path_route, headers=env.user_auth_headers, data=route_data)


def _sorted_key(notification):
    return notification.courier_id, notification.phone, notification.message_text


def _check_notifications(env, prev_notifications, expected_notifications):
    cur_notifications = _get_notifications(env)
    assert len(cur_notifications) == len(expected_notifications) + len(prev_notifications)

    notifications = list(set(cur_notifications) - set(prev_notifications))

    expected_notifications = sorted(expected_notifications, key=_sorted_key)
    notifications = sorted(notifications, key=_sorted_key)
    for i in range(len(notifications)):
        assert notifications[i].courier_id == expected_notifications[i].courier_id
        assert notifications[i].phone == expected_notifications[i].phone
        assert notifications[i].message_text == expected_notifications[i].message_text


def _create_push_by_type(env: Environment, push_type, order):
    if push_type == 'CallClient':
        return Push.CallClient(env.default_courier, order, env.default_company)
    elif push_type == 'AssignmentCourier_long':
        return Push.AssignmentCourier(env.default_courier, env.default_route, env.default_company)
    elif push_type == 'AssignmentCourier_short':
        env.default_company.name = ''
        return Push.AssignmentCourier(env.default_courier, env.default_route, env.default_company)
    elif push_type == 'CourierConnectionLoss':
        route_event = RouteEvent(**ROUTE_EVENT_FIELDS,
                                 route_id=env.default_route.id,
                                 courier_id=env.default_courier.id,
                                 company_id=env.default_company.id)
        return Push.CourierConnectionLoss(env.default_courier, env.default_company, route_event)

    raise RuntimeError(f'Unexpected type {push_type}')


def _add_order(env: Environment, number="test_order"):
    order = {
        "number": number,
        "time_interval": "11:00 - 11:00",
        "eta_type": "delivery_time",
        "address": "ул. Льва Толстого, 16",
        "lat": 55.7447,
        "lon": 37.6728,
        "route_number": env.default_route.number,
        "route_id": env.default_route.id,
    }
    path_add_orders = f"/api/v1/companies/{env.default_company.id}/orders"
    return local_post(env.client, path_add_orders, headers=env.user_auth_headers, data=order)


def _get_route(env, route_id):
    with env.flask_app.app_context():
        route = db.session.query(Route).filter(Route.id == route_id).one()
    return route


@skip_if_remote
def test_push_notification_build(env: Environment):
    enable_push_notifications(env, [PushNotificationType.call_client])
    env.default_courier.phone = TESTING_COURIER_PHONE
    order_data = _add_order(env)

    with env.flask_app.app_context():
        order = get_by_id(Order, order_data['id'])
        push = Push.CallClient(env.default_courier, order, env.default_company)
        push.schedule()
        notification = push.to_db_notification()
        db.session.commit()
        assert notification.status == PushNotificationStatus.not_sent
        assert notification.message_text == push.get_text()
        assert notification.courier_id == env.default_courier.id
        assert notification.phone == env.default_courier.phone

        notifications = db.session.query(PushNotification).filter(PushNotification.courier_id == env.default_courier.id).all()
        assert notifications == [notification]


@skip_if_remote
@pytest.mark.parametrize('locale', ['ru_RU', 'en_EN', 'es_CL', 'zz_ZZ', 'zzzzzzz', None])
@pytest.mark.parametrize('push_type', ['CallClient', 'AssignmentCourier_long', 'AssignmentCourier_short', 'CourierConnectionLoss'])
def test_locale_basic(env: Environment, locale, push_type):
    enable_push_notifications(env, [PushNotificationType.call_client])
    env.default_courier.phone = TESTING_COURIER_PHONE
    env.default_courier.locale = locale
    order_data = _add_order(env)

    title_by_language = {
        'ru_RU': 'Уведомление',
        'en_EN': 'Notification',
        'es_CL': 'Notificación',
    }

    with env.flask_app.app_context():
        order = get_by_id(Order, order_data['id'])
        push = _create_push_by_type(env, push_type, order)
        notification = push.to_db_notification()

        expected_title = title_by_language.get(locale or env.default_company.locale, 'Notification')
        assert notification.title == expected_title
        assert notification.message_text is not None
        assert notification.message_text != ''


@skip_if_remote
@pytest.mark.parametrize('locale', ['ru_RU', 'es_CL'])
@pytest.mark.parametrize('push_type', ['CallClient', 'AssignmentCourier_long', 'AssignmentCourier_short', 'CourierConnectionLoss'])
def test_locale_message_text(env: Environment, locale, push_type):
    enable_push_notifications(env, [
        PushNotificationType.call_client,
        PushNotificationType.assignment_courier,
        PushNotificationType.courier_connection_loss
    ])
    env.default_courier.phone = TESTING_COURIER_PHONE
    env.default_courier.locale = 'en_US'
    order_data = _add_order(env)

    with env.flask_app.app_context():
        order = get_by_id(Order, order_data['id'])
        push = _create_push_by_type(env, push_type, order)
        notification = push.to_db_notification()
        en_text = notification.message_text

        env.default_courier.locale = locale
        push = _create_push_by_type(env, push_type, order)
        notification = push.to_db_notification()
        localized_text = notification.message_text

        assert localized_text != en_text


@skip_if_remote
def test_push_notification_without_phone(env: Environment):
    prev_notifications = _get_notifications(env)
    with env.flask_app.app_context():
        order_data = _add_order(env)
        order = get_by_id(Order, order_data['id'])
        notification = Push.CallClient(env.default_courier, order, env.default_company).to_db_notification()
        assert notification is None

    expected_notifications = []
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_push_notification_log(env: Environment):
    enable_push_notifications(env, [PushNotificationType.call_client])
    courier = add_courier(env, env.default_company.id, TESTING_COURIER_NUMBER, TESTING_COURIER_PHONE)

    with env.flask_app.app_context():
        order_data = _add_order(env)
        order = get_by_id(Order, order_data['id'])
        len_old_log = db.session.query(PushNotificationLog).count()

        Push.CallClient(env.default_courier, order, env.default_company).schedule()
        len_new_log = db.session.query(PushNotificationLog).count()
        assert len_old_log == len_new_log

        Push.CallClient(courier, order, env.default_company).schedule()
        len_new_log = db.session.query(PushNotificationLog).count()
        assert len_old_log + 1 == len_new_log


@skip_if_remote
def test_push_notification_periodic_task(env: Environment):
    enable_push_notifications(env, [PushNotificationType.call_client])
    env.default_courier.phone = TESTING_COURIER_PHONE
    with env.flask_app.app_context():
        assert len(
            db.session.query(PushNotification).filter(PushNotification.courier_id == env.default_courier.id).all()) == 0

        order_data = _add_order(env, 'test_order_1')
        order = get_by_id(Order, order_data['id'])
        push_incorrect_number = Push.CallClient(env.default_courier, order, env.default_company)
        push_incorrect_number.schedule()
        pn_incorrect_number = push_incorrect_number.to_db_notification()
        pn_incorrect_number.phone = 'INCORRECT_PHONE123'

        order_data = _add_order(env, 'test_order_2')
        order = get_by_id(Order, order_data['id'])
        Push.CallClient(env.default_courier, order, env.default_company).schedule()

        order_data = _add_order(env, 'test_order_3')
        order = get_by_id(Order, order_data['id'])
        push_internal_error = Push.CallClient(env.default_courier, order, env.default_company)
        push_internal_error.schedule()
        pn_internal_error = push_internal_error.to_db_notification()
        pn_internal_error.phone = 'internal_error'
        pn_internal_error.expires_at = pn_internal_error.timestamp + 1

        db.session.commit()
        assert db.session.query(PushNotification).filter(PushNotification.courier_id == env.default_courier.id).count() == 3

        task = SendPushNotificationsTask(env.flask_app)
        task.run('')
        assert db.session.query(PushNotification).filter(PushNotification.status == PushNotificationStatus.sent).count() == 1  # test_order_2
        assert db.session.query(PushNotification).filter(PushNotification.status == PushNotificationStatus.fail).count() == 1  # test_order_1
        assert db.session.query(PushNotification).filter(PushNotification.status == PushNotificationStatus.not_sent).count() == 1  # test_order_3


@skip_if_remote
def test_assignment_notification_for_creating_route(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    prev_notifications = _get_notifications(env)
    courier = add_courier(env, env.default_company.id, TESTING_COURIER_NUMBER, TESTING_COURIER_PHONE)

    path_route = f"/api/v1/companies/{env.default_company.id}/routes"
    route_data = _get_request(env, courier.number, TESTING_ROUTE_NUMBER)
    local_post(env.client, path_route, headers=env.user_auth_headers, data=route_data)

    expected_notifications = []
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_assignment_notification_for_updating_route(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    _prepare_route(env, env.default_route.id)
    prev_notifications = _get_notifications(env)
    courier = add_courier(env, env.default_company.id, TESTING_COURIER_NUMBER, TESTING_COURIER_PHONE)
    _update_courier_on_route(env, courier.number, env.default_route.id)

    with env.flask_app.app_context():
        expected_notifications = [
            Push.AssignmentCourier(courier, env.default_route, env.default_company).to_db_notification()
        ]
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_assignment_notification_for_updating_route_with_the_same_courier(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    prev_notifications = _get_notifications(env)
    courier = add_courier(env, env.default_company.id, TESTING_COURIER_NUMBER, TESTING_COURIER_PHONE)
    route = add_route_to_db(env, courier.id)
    _update_courier_on_route(env, courier.number, route.id)

    expected_notifications = []
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_assignment_notification_for_batch_creating(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    prev_notifications = _get_notifications(env)
    courier = add_courier(env, env.default_company.id, TESTING_COURIER_NUMBER, TESTING_COURIER_PHONE)
    route_data = [
        _get_request(env, courier.number, TESTING_ROUTE_NUMBER)
    ]
    edit_routes_in_batch_mode(env, env.default_company.id, route_data)

    expected_notifications = []
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_assignment_notification_for_batch_with_different_couriers(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    prev_notifications = _get_notifications(env)
    _prepare_route(env, env.default_route.id)
    courier = add_courier(env, env.default_company.id, TESTING_COURIER_NUMBER, TESTING_COURIER_PHONE)

    route_data = [
        _get_request(env, courier.number, env.default_route.number)
    ]

    edit_routes_in_batch_mode(env, env.default_company.id, route_data)

    with env.flask_app.app_context():
        expected_notifications = [
            Push.AssignmentCourier(courier, env.default_route, env.default_company).to_db_notification()
        ]
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_assignment_notification_for_batch_with_the_same_courier(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    prev_notifications = _get_notifications(env)
    courier = add_courier(env, env.default_company.id, TESTING_COURIER_NUMBER, TESTING_COURIER_PHONE)
    route = add_route_to_db(env, courier.id)
    route_data = [
        _get_request(env, courier.number, route.number)
    ]
    edit_routes_in_batch_mode(env, env.default_company.id, route_data)

    expected_notifications = []
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_assignment_notification_for_import_routes(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    prev_notifications = _get_notifications(env)
    task_json = copy.deepcopy(solver_request_by_task_id['mock_task_uuid__task_for_import_routes'])
    task_json['vehicles'][1]['shifts'][0]['route_numbers'] = [TESTING_ROUTE_NUMBER]

    path_import = f"/api/v1/companies/{env.default_company.id}/import-routes"
    local_post(env.client, path_import, headers=env.user_auth_headers, data=task_json)

    courier = get_courier_by_number_and_company_id(env, str(task_json['vehicles'][1]['id']), env.default_company.id)

    with env.flask_app.app_context():
        route = get_by_number(Route, TESTING_ROUTE_NUMBER, env.default_company.id)

        expected_notifications = [
            Push.AssignmentCourier(courier, route, env.default_company).to_db_notification()
        ]
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_assignment_notification_for_getting_mvrp_result_by_task_id(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    prev_notifications = _get_notifications(env)
    path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id=mock_task_uuid__task_for_import_routes"
    local_post(env.client, path, headers=env.user_auth_headers)

    notifications = set(_get_notifications(env)) - set(prev_notifications)
    assert len(notifications) == 1


@skip_if_remote
def test_create_same_notification(env: Environment):
    enable_push_notifications(env, [PushNotificationType.call_client])
    env.default_courier.phone = TESTING_COURIER_PHONE
    with env.flask_app.app_context():
        assert db.session.query(PushNotification).filter(
            PushNotification.courier_id == env.default_courier.id).count() == 0

        order_data = _add_order(env, 'test_order_1')
        order = get_by_id(Order, order_data['id'])
        push = Push.CallClient(env.default_courier, order, env.default_company)
        push.schedule()
        # I made second push notification here to make test more stable if someone will change some logic to
        # constructor/schedule/add some caching
        push = Push.CallClient(env.default_courier, order, env.default_company)
        push.schedule()
        db.session.commit()
        assert db.session.query(PushNotification).filter(
            PushNotification.courier_id == env.default_courier.id).count() == 1


@skip_if_remote
def test_push_notification_with_deleted_courier(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    env.default_courier.phone = TESTING_COURIER_PHONE
    patch_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_patch(env.client, patch_path, data={'phone': TESTING_COURIER_PHONE}, headers=env.user_auth_headers)
    prev_notification = _get_notifications(env)
    _prepare_route(env, env.default_route.id)

    with env.flask_app.app_context():
        expected_notifications = [
            Push.AssignmentCourier(env.default_courier, env.default_route, env.default_company).to_db_notification()
        ]
    _check_notifications(env, prev_notification, expected_notifications)

    _prepare_route(env, env.default_route.id, nodes=[])
    route_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    local_delete(env.client, route_path, headers=env.user_auth_headers)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, headers=env.user_auth_headers)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_second_depot.id}'
    local_delete(env.client, depot_path, headers=env.user_auth_headers)

    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'

    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)
    _check_notifications(env, prev_notification, expected_notifications)

    local_delete(env.client, courier_path, headers=env.user_auth_headers)
    _check_notifications(env, prev_notification, [])


@skip_if_remote
def test_connection_loss_push_notification(env: Environment):
    enable_push_notifications(env, [PushNotificationType.courier_connection_loss])
    prev_notifications = _get_notifications(env)
    route = set_default_route_time_interval(env)

    path_courier = f"/api/v1/companies/{env.default_company.id}/couriers-batch"
    courier_data = [
        {
            'number': env.default_courier.number,
            'phone': TESTING_COURIER_PHONE
        }
    ]
    local_post(env.client, path_courier, headers=env.user_auth_headers, data=courier_data)

    start_datetime = (datetime.combine(route.date, datetime.min.time()) + timedelta(seconds=route.route_start_s)) \
        .astimezone(gettz(env.default_depot.time_zone))

    task = CheckCourierConnectionLossTask(env.flask_app)

    with freeze_time(start_datetime) as freezed_time:
        path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}/push-positions"
        locations = [(58.82, 37.73, time.time())]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

        freezed_time.tick(delta=timedelta(seconds=env.default_company.courier_connection_loss_s + 1))

        with env.flask_app.app_context():
            task.run('')
            event = db.session.query(RouteEvent).filter(RouteEvent.courier_id == env.default_courier.id).one_or_none()
            db.session.expunge(event)

            env.default_courier.phone = TESTING_COURIER_PHONE
            expected_notifications = [
                Push.CourierConnectionLoss(env.default_courier, env.default_company, event).to_db_notification()
            ]
    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_disable_notification(env: Environment):
    enable_push_notifications(env,
                              [PushNotificationType.assignment_courier, PushNotificationType.courier_connection_loss])
    prev_notifications = _get_notifications(env)
    env.default_courier.phone = TESTING_COURIER_PHONE
    _prepare_route(env, env.default_route.id)
    notifications = []
    route_event = RouteEvent(**ROUTE_EVENT_FIELDS,
                             route_id=env.default_route.id,
                             courier_id=env.default_courier.id,
                             company_id=env.default_company.id)

    notifications.append(Push.CourierConnectionLoss(env.default_courier, env.default_company, route_event))
    with env.flask_app.app_context():
        expected_notifications = list(map(lambda notification: notification.to_db_notification(), notifications))

    order_data = _add_order(env)
    with env.flask_app.app_context():
        order = get_by_id(Order, order_data['id'])
    notifications.append(Push.CallClient(env.default_courier, order, env.default_company))

    with env.flask_app.app_context():
        for notification in notifications:
            notification.schedule()
            db.session.flush()
        for push_notification in expected_notifications:
            db.session.expunge(push_notification)
        db.session.commit()

    _check_notifications(env, prev_notifications, expected_notifications)


@skip_if_remote
def test_push_assignment_notification_from_order_batch(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    env.default_courier.phone = TESTING_COURIER_PHONE
    patch_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_patch(env.client, patch_path, data={'phone': TESTING_COURIER_PHONE}, headers=env.user_auth_headers)
    prev_notification = _get_notifications(env)

    path = f'/api/v1/companies/{env.default_company.id}/orders-batch'
    data = [TEST_ORDER]
    data[0]['route_id'] = env.default_route.id
    local_post(env.client, path, headers=env.user_auth_headers, data=data)

    with env.flask_app.app_context():
        expected_notifications = [
            Push.AssignmentCourier(env.default_courier, env.default_route, env.default_company).to_db_notification()
        ]
    _check_notifications(env, prev_notification, expected_notifications)


@skip_if_remote
def test_push_assignment_notification_when_patch_order(env: Environment):
    env.default_courier.phone = TESTING_COURIER_PHONE
    patch_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_patch(env.client, patch_path, data={'phone': TESTING_COURIER_PHONE}, headers=env.user_auth_headers)
    prev_notification = _get_notifications(env)
    path = f'/api/v1/companies/{env.default_company.id}/orders'
    data = TEST_ORDER
    data['route_id'] = env.default_route.id
    data['route_number'] = env.default_route.number
    resp = local_post(env.client, path, headers=env.user_auth_headers, data=data)

    path_update = f'api/v1/companies/{env.default_company.id}/orders/{resp["id"]}'
    new_route = add_route_to_db(env, env.default_courier.id)
    local_patch(
        env.client,
        path_update,
        headers=env.user_auth_headers,
        data={'route_id': new_route.id, 'route_number': new_route.number}
    )

    with env.flask_app.app_context():
        expected_notifications = [
            Push.AssignmentCourier(env.default_courier, env.default_route, env.default_company).to_db_notification(),
            Push.AssignmentCourier(env.default_courier, new_route, env.default_company).to_db_notification()
        ]
    _check_notifications(env, prev_notification, expected_notifications)


@skip_if_remote
def test_push_assignment_notification_when_post_order(env: Environment):
    enable_push_notifications(env, [PushNotificationType.assignment_courier])
    env.default_courier.phone = TESTING_COURIER_PHONE
    patch_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_patch(env.client, patch_path, data={'phone': TESTING_COURIER_PHONE}, headers=env.user_auth_headers)

    prev_notification = _get_notifications(env)
    path = f'/api/v1/companies/{env.default_company.id}/orders'
    data = TEST_ORDER
    data['route_id'] = env.default_route.id
    data['route_number'] = env.default_route.number
    local_post(env.client, path, headers=env.user_auth_headers, data=data)

    with env.flask_app.app_context():
        expected_notifications = [
            Push.AssignmentCourier(env.default_courier, env.default_route, env.default_company).to_db_notification()
        ]
    _check_notifications(env, prev_notification, expected_notifications)


@skip_if_remote
def test_idle_push_notification(env: Environment):
    enable_push_notifications(env, [PushNotificationType.courier_idle])
    env.default_courier.phone = TESTING_COURIER_PHONE
    patch_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_patch(env.client, patch_path, data={'phone': TESTING_COURIER_PHONE}, headers=env.user_auth_headers)
    prev_notification = _get_notifications(env)
    set_company_import_depot_garage(env, env.default_company.id, True)
    task_id = "mock_task_uuid__generic"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    route = local_post(env.client, import_path, headers=env.user_auth_headers)[0]
    route_id = route['id']
    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route_id}"
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route_id}/push-positions"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    now = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    start_datetime = datetime.fromtimestamp(now).astimezone(gettz('Europe/Moscow'))
    with freeze_time(start_datetime) as freezed_time:
        now = time.time()
        # 1. Stay at the depot for some time
        locations = [(55.7447, 37.6727, now), (55.7447, 37.6727, now + 15 * 60), (55.7447, 37.6727, now + 30 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers,
                   data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now + 30 * 60}
        update_route(env, route_id, route_state_context)

        # 2. Move to another position and stay here for some time
        freezed_time.tick(delta=timedelta(minutes=45))
        now = time.time()
        locations = [(58.82, 37.73, now + 15 * 60), (58.82, 37.73, now + 30 * 60), (58.82, 37.73, now + 45 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers,
                   data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 58.82, 'lon': 38.73, 'timestamp': now + 45 * 60}
        update_route(env, route_id, route_state_context)

    idle_events = _get_idle_events(env, route_id)

    with env.flask_app.app_context():
        expected_notifications = [
            Push.CourierIdle(env.default_courier, env.default_company, idle_event, 80 * 60).to_db_notification()
            for idle_event in idle_events
        ]
    _check_notifications(env, prev_notification, expected_notifications)


@skip_if_remote
def test_route_end_push_notification(env: Environment):
    enable_push_notifications(env, [PushNotificationType.route_end])
    env.default_courier.phone = TESTING_COURIER_PHONE
    patch_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_patch(env.client, patch_path, data={'phone': TESTING_COURIER_PHONE}, headers=env.user_auth_headers)
    prev_notifications = _get_notifications(env)
    route = set_default_route_time_interval(env, route_finish='02:00:00')
    set_dirty_level(env, route.id)
    _prepare_route(env, route.id)

    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route.id}/push-positions"

    path_courier = f"/api/v1/companies/{env.default_company.id}/couriers-batch"
    courier_data = [
        {
            'number': env.default_courier.number,
            'phone': TESTING_COURIER_PHONE
        }
    ]
    local_post(env.client, path_courier, headers=env.user_auth_headers, data=courier_data)

    start_datetime = (datetime.combine(route.date, datetime.min.time()) + timedelta(seconds=route.route_start_s)) \
        .astimezone(gettz(env.default_depot.time_zone))
    with freeze_time(start_datetime):
        now = time.time()
        locations = [(55.7447, 37.6727, now), (55.7447, 37.6727, now + 15 * 60), (55.7447, 37.6727, now + 30 * 60)]
        local_post(env.client, path_push, headers=env.user_auth_headers,
                   data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now + 30 * 60}
        update_route(env, route.id, route_state_context)

        _check_notifications(env, prev_notifications, [])

        locations = [
            (55.7447, 37.6727, now + 45 * 60),
            (55.7447, 37.6727, now + 60 * 60),
            (55.7447, 37.6727, now + 75 * 60),
        ]
        local_post(env.client, path_push, headers=env.user_auth_headers,
                   data=prepare_push_positions_data(locations))
        route_state_context = {'lat': 55.7447, 'lon': 37.6727, 'timestamp': now + 75 * 60}
        update_route(env, route.id, route_state_context)

    with env.flask_app.app_context():
        expected_notifications = [
            Push.RouteEnd(env.default_courier, route, env.default_company).to_db_notification()
        ]
    _check_notifications(env, prev_notifications, expected_notifications)
