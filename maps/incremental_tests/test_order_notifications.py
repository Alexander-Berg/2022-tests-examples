import contextlib
import datetime
import dateutil.tz
from dateutil.parser import parse
import re
from urllib.parse import urlencode
import pytest
import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    create_tmp_company,
    create_tmp_user,
    env_get_request,
    env_patch_request,
    patch_company,
    patch_order,
    query_routed_orders, check_response_with_db_error,
)
from maps.b2bgeo.ya_courier.backend.test_lib import util
from test_sms_logic import TRACKING_URL_PATTERN
from ya_courier_backend.models import UserRole, EtaType
from ya_courier_backend.resources.order_notifications import NOTIFICATIONS_MAX_TIME_INTERVAL_M
from ya_courier_backend.util.db_errors import CONSISTENCY_ERROR_MESSAGE


@contextlib.contextmanager
def _create_notification_env(env, timezone_str):
    timezone_tz = dateutil.tz.gettz(timezone_str)
    now = datetime.datetime.now(tz=timezone_tz)
    route_date = datetime.datetime(now.year, now.month, now.day, 0, 0, 0, tzinfo=timezone_tz).date()

    with create_tmp_company(env, "Test company test notifications") as owner_company_id:
        with create_tmp_user(env, owner_company_id, UserRole.admin) as owner_user:
            owner_auth = env.get_user_auth(owner_user)
            with util.create_route_env(
                    env,
                    'test_notifications_route_env',
                    order_locations=[{'lat': 55.791928, 'lon': 37.841492}],
                    time_intervals=['08:00-23:59'],
                    route_date=route_date.isoformat(),
                    depot_data={'time_zone': timezone_str},
                    company_id=owner_company_id,
                    auth=owner_auth) as route_env:

                route = route_env['route']
                courier = route_env['courier']
                assert len(route_env['orders']) == 1
                order = route_env['orders'][0]

                current_location = util.get_location_nearby(order, '08:30')
                query_routed_orders(env, courier['id'], route['id'], current_location, auth=owner_auth)

                yield {
                    'dbenv': env,
                    'owner_company_id': owner_company_id,
                    'owner_auth': owner_auth,
                    'order': order,
                    'route': route,
                    'timezone_tz': timezone_tz,
                }


@contextlib.contextmanager
def _create_notification_type_env(env, timezone_str, order_locations, time_intervals):
    timezone_tz = dateutil.tz.gettz(timezone_str)
    now = datetime.datetime.now(tz=timezone_tz)
    route_date = datetime.datetime(now.year, now.month, now.day, 0, 0, 0, tzinfo=timezone_tz).date()

    with create_tmp_company(env, "Test company test notifications") as owner_company_id:
        with create_tmp_user(env, owner_company_id, UserRole.admin) as owner_user:
            owner_auth = env.get_user_auth(owner_user)
            with util.create_route_env(
                    env,
                    'test_notifications_route_env',
                    order_locations=order_locations,
                    time_intervals=time_intervals,
                    route_date=route_date.isoformat(),
                    depot_data={'time_zone': timezone_str},
                    company_id=owner_company_id,
                    auth=owner_auth) as route_env:

                route = route_env['route']
                courier = route_env['courier']
                assert len(route_env['orders']) == 3
                orders = route_env['orders']

                yield {
                    'dbenv': env,
                    'courier': courier,
                    'owner_company_id': owner_company_id,
                    'owner_auth': owner_auth,
                    'orders': orders,
                    'route': route,
                    'timezone_tz': timezone_tz,
                }


def _validate_notification_item_format(item, expected_timezone_tz):
    assert set(item.keys()) == {
        'eta',
        'order_id',
        'order_number',
        'owner_company_id',
        'owner_company_name',
        'route_id',
        'route_number',
        'time',
        'type',
        'widget_url',
    }

    assert item['type'] in ['shift_start', 'nearby']

    def validate_datetime(datetime_str):
        dt = parse(datetime_str)
        assert isinstance(dt, datetime.datetime)
        assert dt.tzinfo is not None
        assert dt.astimezone(expected_timezone_tz).isoformat() == datetime_str

    validate_datetime(item['time'])
    validate_datetime(item['eta'])

    assert re.match(TRACKING_URL_PATTERN + '$', item['widget_url']) is not None


def _get_notifications(env, company_id, from_datetime, to_datetime, page, auth, expected_status_code):
    params = {
        'from': from_datetime,
        'to': to_datetime
    }
    if page is not None:
        params['page'] = page
    response = env_get_request(
        env['dbenv'],
        api_path_with_company_id(env['dbenv'], f'order-notifications?{urlencode(params)}', company_id=company_id),
        auth=auth
    )
    assert response.status_code == expected_status_code, response.text

    j = response.json()

    if response.status_code == requests.codes.ok:
        assert isinstance(j, list)
        for item in j:
            _validate_notification_item_format(item, env['timezone_tz'])

    return j


@pytest.fixture(scope='function')
def notifications_env(system_env_with_db):
    with _create_notification_env(system_env_with_db, 'Europe/Moscow') as e:
        yield e


def parameters_notifications_time_intervals():
    now = datetime.datetime.now(tz=datetime.timezone.utc)
    result = []

    # Make sure that no notification items are returned if we specify a time interval outside of notifications events
    from_datetime = now - datetime.timedelta(minutes=5)
    to_datetime = from_datetime + datetime.timedelta(minutes=1)
    result.append((from_datetime, to_datetime, requests.codes.ok, [], None))
    from_datetime = now + datetime.timedelta(hours=10)  # test can be launched much later than it was parametrized by pytest
    to_datetime = from_datetime + datetime.timedelta(minutes=1)
    result.append((from_datetime, to_datetime, requests.codes.ok, [], None))

    # Make sure that empty time interval works and returns empty list of notifications
    result.append((now, now, requests.codes.ok, [], None))

    # Make sure that it's not allowed to use bad time intervals
    result.append((now, now - datetime.timedelta(minutes=1), requests.codes.unprocessable, None,
                   'Specified time interval is invalid'))
    result.append((now - datetime.timedelta(minutes=NOTIFICATIONS_MAX_TIME_INTERVAL_M + 1), now,
                   requests.codes.unprocessable, None, 'Specified time interval is longer than 30 minutes'))
    result.append(('24.06.2020', now, requests.codes.unprocessable, None, 'Not a valid datetime'))

    return result


@pytest.mark.parametrize('from_datetime, to_datetime, expected_status, result, substring',
                         parameters_notifications_time_intervals())
def test_notifications_time_intervals(notifications_env, from_datetime, to_datetime, expected_status, result, substring):
    e = notifications_env

    response = _get_notifications(
        e, e['owner_company_id'], from_datetime, to_datetime, page=None, auth=e['owner_auth'], expected_status_code=expected_status
    )
    if result is not None:
        assert response == result
    if substring is not None:
        assert substring in response['message']


def test_notifications_paging(system_env_with_db):
    env = system_env_with_db

    with _create_notification_env(env, 'Europe/Moscow') as e:

        now = datetime.datetime.now(tz=datetime.timezone.utc)

        # Make sure that it's not allowed to use bad page numbers

        from_datetime = now
        to_datetime = now - datetime.timedelta(minutes=1)
        response = _get_notifications(
            e, e['owner_company_id'], from_datetime, to_datetime, page=0, auth=e['owner_auth'], expected_status_code=requests.codes.unprocessable
        )
        assert 'Must be greater than or equal to 1' in response['message']

        # Make sure that big page numbers work and return empty list of notifications

        from_datetime = now - datetime.timedelta(minutes=1)
        to_datetime = now
        assert _get_notifications(
            e, e['owner_company_id'], from_datetime, to_datetime, page=2, auth=e['owner_auth'], expected_status_code=requests.codes.ok
        ) == []


def test_notifications_access(system_env_with_db):
    env = system_env_with_db

    with _create_notification_env(env, 'Europe/Moscow') as e:
        now = datetime.datetime.now(tz=datetime.timezone.utc)

        from_datetime = now - datetime.timedelta(minutes=1)
        to_datetime = now

        # Make sure that only admin can request list of notifications

        for user_role in [UserRole.admin, UserRole.manager, UserRole.dispatcher, UserRole.app]:
            with create_tmp_user(env, e['owner_company_id'], user_role) as user:
                expected_status_code = requests.codes.ok if user_role == UserRole.admin else requests.codes.forbidden
                _get_notifications(
                    e, e['owner_company_id'], from_datetime, to_datetime, page=None, auth=env.get_user_auth(user), expected_status_code=expected_status_code
                )

        # Make sure that a user from a foreign company cannot request list of notifications

        with create_tmp_company(env, "Test foreign company test notifications") as company_id:
            with create_tmp_user(env, company_id, UserRole.admin) as user:
                _get_notifications(
                    e, e['owner_company_id'], from_datetime, to_datetime, page=None, auth=env.get_user_auth(user), expected_status_code=requests.codes.forbidden
                )


def test_notifications_common(system_env_with_db):
    env = system_env_with_db

    for timezone_str in ['Europe/Moscow', 'Asia/Vladivostok']:
        with _create_notification_env(env, timezone_str) as e:

            now = datetime.datetime.now(tz=datetime.timezone.utc)

            # Make sure that list of notifications contains proper data when the time interval and the page are specified correctly

            from_datetime = now - datetime.timedelta(minutes=NOTIFICATIONS_MAX_TIME_INTERVAL_M)
            to_datetime = now
            for page in [None, 1]:
                notifications = _get_notifications(
                    e, e['owner_company_id'], from_datetime, to_datetime, page=page, auth=e['owner_auth'], expected_status_code=requests.codes.ok
                )
                assert len(notifications) == 2
                for i, notification in enumerate(notifications):
                    assert notification['type'] == ['shift_start', 'nearby'][i]
                    assert notification['order_id'] == e['order']['id']
                    assert notification['order_number'] == e['order']['number']
                    assert notification['route_id'] == e['route']['id']
                    assert notification['route_number'] == e['route']['number']
                    assert notification['owner_company_id'] == e['owner_company_id']


def test_notifications_time_interval_edge(system_env_with_db):
    env = system_env_with_db

    for timezone_str in ['Europe/Moscow', 'Asia/Vladivostok']:
        with _create_notification_env(env, timezone_str) as e:

            now = datetime.datetime.now(tz=datetime.timezone.utc)

            # Make sure that we see a notification only once if the notification is created at the interval edge

            for page in [None, 1]:
                from_datetime = now - datetime.timedelta(minutes=NOTIFICATIONS_MAX_TIME_INTERVAL_M)
                to_datetime = now
                notifications = _get_notifications(
                    e, e['owner_company_id'], from_datetime, to_datetime, page=page, auth=e['owner_auth'], expected_status_code=requests.codes.ok
                )
                assert len(notifications) == 2

                to_datetime = parse(notifications[1]['time'])
                from_datetime = to_datetime - datetime.timedelta(minutes=NOTIFICATIONS_MAX_TIME_INTERVAL_M)
                notifications = _get_notifications(
                    e, e['owner_company_id'], from_datetime, to_datetime, page=page, auth=e['owner_auth'], expected_status_code=requests.codes.ok
                )
                assert len(notifications) == 1
                assert notifications[0]['type'] == 'shift_start'

                from_datetime = to_datetime
                to_datetime = from_datetime + datetime.timedelta(minutes=NOTIFICATIONS_MAX_TIME_INTERVAL_M)
                notifications = _get_notifications(
                    e, e['owner_company_id'], from_datetime, to_datetime, page=page, auth=e['owner_auth'], expected_status_code=requests.codes.ok
                )
                assert len(notifications) == 1
                assert notifications[0]['type'] == 'nearby'


def test_eta_types(system_env_with_db):
    env = system_env_with_db

    with _create_notification_type_env(env,
                                       'Europe/Moscow',
                                       [{'lat': 55.791928, 'lon': 37.841492}, {'lat': 55.791928 + 0.015, 'lon': 37.841492}, {'lat': 55.791928, 'lon': 37.841492}],
                                       ['08:00-23:59', '09:00-23:59', '10:00-23:59']) as e:

        patch_company(env, {'eta_type': EtaType.delivery_time.value}, company_id=e['owner_company_id'],
                      auth=e['owner_auth'])

        patch_order(env, e['orders'][0], {'eta_type': EtaType.delivery_time.value}, company_id=e['owner_company_id'],
                    auth=e['owner_auth'])
        patch_order(env, e['orders'][1], {'eta_type': EtaType.arrival_time.value}, company_id=e['owner_company_id'],
                    auth=e['owner_auth'])
        patch_order(env, e['orders'][2], {'eta_type': None}, company_id=e['owner_company_id'], auth=e['owner_auth'])

        # 1st order has eta about 5m, waiting_duration == 0
        # 2nd order has eta about 16m, waiting_duration about 44m
        # 3rd order has eta more than 1h
        current_location = util.get_location_nearby(e['orders'][0], '08:00')
        query_routed_orders(env, e['courier']['id'], e['route']['id'], current_location, auth=e['owner_auth'])

        now = datetime.datetime.now(tz=datetime.timezone.utc)
        from_datetime = now - datetime.timedelta(minutes=NOTIFICATIONS_MAX_TIME_INTERVAL_M)
        to_datetime = now
        notifications = _get_notifications(
            e, e['owner_company_id'], from_datetime, to_datetime, page=None, auth=e['owner_auth'], expected_status_code=requests.codes.ok
        )

        assert len(notifications) == 5
        assert notifications[0]['type'] == 'shift_start'
        assert notifications[1]['type'] == 'shift_start'
        assert notifications[2]['type'] == 'shift_start'

        # company - delivery_time / order - delivery_time
        assert notifications[3]['type'] == 'nearby'

        # company - delivery_time / order - arrival_time
        assert notifications[4]['type'] == 'nearby'

        patch_order(env, e['orders'][0], {'status': 'finished'}, company_id=e['owner_company_id'], auth=e['owner_auth'])
        patch_order(env, e['orders'][1], {'status': 'finished'}, company_id=e['owner_company_id'], auth=e['owner_auth'])

        # eta + waiting_duration is small enough for the 3rd order
        current_location = util.get_location_nearby(e['orders'][2], '09:30')
        query_routed_orders(env, e['courier']['id'], e['route']['id'], current_location, auth=e['owner_auth'])

        from_datetime = to_datetime
        to_datetime = datetime.datetime.now(tz=datetime.timezone.utc)
        notifications = _get_notifications(
            e, e['owner_company_id'], from_datetime, to_datetime, page=None, auth=e['owner_auth'], expected_status_code=requests.codes.ok
        )
        assert len(notifications) == 1

        # company - delivery_time / order - None
        assert notifications[0]['type'] == 'nearby'


def test_invalid_eta_type_for_company(system_env_with_db):
    env = system_env_with_db

    with create_tmp_company(env, "Test foreign company test notifications") as company_id:
        with create_tmp_user(env, company_id, UserRole.admin) as owner_user:
            owner_auth = env.get_user_auth(owner_user)
            response = env_patch_request(
                system_env_with_db,
                api_path_with_company_id(system_env_with_db, company_id=company_id),
                data={'eta_type': 'shift_start'},
                auth=owner_auth
            )
            assert response.status_code == requests.codes.unprocessable
            check_response_with_db_error(response.json()['message'], CONSISTENCY_ERROR_MESSAGE)


def test_invalid_eta_type_for_order(system_env_with_db):
    env = system_env_with_db

    with _create_notification_env(env, 'Europe/Moscow') as e:
        path = f"orders/{e['order']['id']}"
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, path, company_id=e['owner_company_id']),
            data={'eta_type': 'shift_start'},
            auth=e['owner_auth']
        )
        assert response.status_code == requests.codes.unprocessable
        check_response_with_db_error(response.json()['message'], CONSISTENCY_ERROR_MESSAGE)
