import re
import datetime
from dateutil.parser import parse
import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    create_tmp_companies,
    create_tmp_company,
    create_tmp_user,
    create_tmp_users,
    env_get_request,
    env_patch_request,
    get_order,
    get_orders,
    query_routed_orders,
)
from maps.b2bgeo.ya_courier.backend.test_lib import util
from maps.b2bgeo.ya_courier.backend.test_lib.util_deprecated import (
    DATE_TODAY_MSK,
    TIMEZONE_MSK
)
from urllib.parse import urlencode
from ya_courier_backend.models import UserRole
from ya_courier_backend.util.distance import distance
from maps.b2bgeo.libs.py_flask_utils.format_util import parse_time
from ya_courier_backend.util.order_time_windows import convert_time_window_timestamp


ROUTE_DATETIME = datetime.datetime(DATE_TODAY_MSK.year, DATE_TODAY_MSK.month, DATE_TODAY_MSK.day, 0, 0, 0, tzinfo=TIMEZONE_MSK)

ROUTABLE_STATII = ('new', 'confirmed')

COURIER_ID = None

ORDERS_LOCS = [
    {
        'lat': 55.791928,
        'lon': 37.841492,
    },
    {
        'lat': 55.670455,
        'lon': 37.284573,
    }
]
INTERVALS = ['08:00-23:59'] * len(ORDERS_LOCS)
COURIER_START_POSITION = {
    'lat': 55.6447,
    'lon': 37.6727
}


_TRACK_ID_PATTERN = '[0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12}'
TRACKING_URL_PATTERN = r'https://[0-9a-z.-]+\.yandex.ru/courier/tracking/(?P<track_id>' + _TRACK_ID_PATTERN + ')'


def _current_sms_count(system_env_with_db, company_id, auth):
    return util.current_sms_count(system_env_with_db, ROUTE_DATETIME, company_id=company_id, auth=auth)


def _last_sms(system_env_with_db):
    j = util.sms_state(system_env_with_db, ROUTE_DATETIME)
    return max(j, key=lambda x: x['created_at'])


def _push_position(system_env_with_db, location, courier_id, route_id, auth):
    util.push_positions(
        system_env_with_db,
        courier_id,
        route_id,
        track=[(location['lat'], location['lon'], (ROUTE_DATETIME + parse_time(location['time_now'])).timestamp())],
        auth=auth
    )


# ########################## TESTS ###########################
def test_location_nearby(system_env_with_db):
    """
    Make sure that utill.get_location_nearby() produces positions that are
    not too close and not too far (_send_nearby_smses() does not
    send SMSes if ETA is too small or too big).
    """

    loc = ORDERS_LOCS[0]
    loc_near = util.get_location_nearby(loc, '0:00')

    d = distance((loc['lat'], loc['lon']), (loc_near['lat'], loc_near['lon']))

    speeds_km_h = [4.5, 90.0]
    speeds_m_s = [s * (1000 / (60 * 60)) for s in speeds_km_h]
    for time_s in [d / s for s in speeds_m_s]:
        assert time_s > 60 and time_s < 1800


def _validate_notifications(env, notifications):
    for notification in notifications:
        assert len(notification) == 3
        assert 'type' in notification
        eta_datetime = parse(notification['eta_iso'])
        assert isinstance(eta_datetime, datetime.datetime)
        assert eta_datetime.tzinfo is not None

        match = re.match(TRACKING_URL_PATTERN + '$', notification['tracking_url'])
        assert match
        track_id = match['track_id']

        response = env_get_request(env, f'tracking/{track_id}')
        assert response.status_code == requests.codes.ok, response.text

        response = env_get_request(env, f'tracking/{track_id}/track')
        assert response.status_code in [requests.codes.ok, requests.codes.gone], response.text


def _get_notifications_by_get_order(env, orders, company_id, auth):
    result = []
    for order in orders:
        notifications = get_order(env, order['id'], company_id=company_id, auth=auth)['notifications']
        _validate_notifications(env, notifications)
        result.append(notifications)
    return result


def _get_notifications_by_get_orders(env, orders, company_id, auth):
    assert len({order['route_id'] for order in orders}) == 1
    db_orders = get_orders(env, orders[0]['route_id'], company_id=company_id, auth=auth)
    assert len(db_orders) == len(orders)
    result = []
    for db_order in db_orders:
        notifications = db_order['notifications']
        _validate_notifications(env, notifications)
        result.append(notifications)
    return result


def _get_notifications_by_order_notifications(env, orders, company_id, auth):
    now = datetime.datetime.now(tz=datetime.timezone.utc)
    params = urlencode({
        'from': now - datetime.timedelta(minutes=3),
        'to': now,
        'page': 1
    })
    response = env_get_request(
        env,
        api_path_with_company_id(env, f"order-notifications?{params}", company_id=company_id),
        auth=auth
    )
    assert response.status_code == requests.codes.ok, response.text
    j = response.json()

    assert len({order['route_id'] for order in orders}) == 1
    assert all(x['route_id'] == orders[0]['route_id'] for x in j)

    assert len({order['company_id'] for order in orders}) == 1
    assert all(x['owner_company_id'] == orders[0]['company_id'] for x in j)

    assert not ({x['order_id'] for x in j} - {x['id'] for x in orders})

    result = []
    for order in orders:
        notifications = [
            {
                'type': x['type'],
                'eta_iso': x['eta'],
                'tracking_url': x['widget_url']
            } for x in j if x['order_id'] == order['id']
        ]
        _validate_notifications(env, notifications)
        result.append(notifications)

    return result


def _check_notification_types(notifications, expected_notification_types):
    assert len(notifications) == len(expected_notification_types)
    for order_notifications, order_expected_notification_types in zip(notifications, expected_notification_types):
        assert [x['type'] for x in order_notifications] == order_expected_notification_types


def _do_test_sms_logic_normal_scenario(
        system_env_with_db,
        owner_company_id,
        owner_auth,
        preprocess_orders,
        check_notifications):

    """
    Test the following workflow:
        - a route with three orders is created (third order has no phone number)
        - courier shift starts (first time interval begins)
            * check: for first two orders notification sms is sent
        - first order is confirmed
            * check: next order sms for it is sent
        - first order is delivered
        - second order is confirmed
            * check: next order sms for it is sent
    """

    with util.create_route_env(
            system_env_with_db,
            'test_sms_logic_normal_scenario',
            order_locations=ORDERS_LOCS + [{
                'lat': 55.670455,
                'lon': 36.9,
            }],
            time_intervals=INTERVALS + [INTERVALS[0]],
            route_date=ROUTE_DATETIME.date().isoformat(),
            company_id=owner_company_id,
            auth=owner_auth) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        assert len(orders) == 3
        assert env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders/{}".format(orders[2]["id"]), company_id=owner_company_id),
            data={'phone': None},
            auth=owner_auth
        ).status_code == requests.codes.ok

        if preprocess_orders:
            preprocess_orders(orders)

        sms_count = _current_sms_count(system_env_with_db, company_id=owner_company_id, auth=owner_auth)

        current_location = {
            'lat': COURIER_START_POSITION['lat'],
            'lon': COURIER_START_POSITION['lon'],
            'time_now': '07:00'
        }

        _push_position(system_env_with_db, current_location, courier_id, route_id, auth=owner_auth)

        # assert that we didn't create any notifications and sms yet because
        # courier time is earlier than the start of a first location interval
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location, auth=owner_auth)
        assert _current_sms_count(system_env_with_db, company_id=owner_company_id, auth=owner_auth) == sms_count
        check_notifications(orders, [[], [], []])

        current_location['time_now'] = '08:01'
        routed_orders = query_routed_orders(system_env_with_db, courier_id, route_id, current_location, auth=owner_auth)
        first_order, second_order = (orders[0], orders[1]) if routed_orders['route'][0]['id'] == orders[0]['id'] \
            else (orders[1], orders[0])

        # check that we sent notification sms for two out of three orders
        sms_count += 2
        assert _current_sms_count(system_env_with_db, company_id=owner_company_id, auth=owner_auth) == sms_count
        check_notifications(orders, [["shift_start"], ["shift_start"], ["shift_start"]])

        util.confirm_order(system_env_with_db, first_order, company_id=owner_company_id, auth=owner_auth)
        current_location = util.get_location_nearby(first_order, "08:30")
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location, auth=owner_auth)

        # check that we sent one "next order" sms for order 0
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=owner_company_id, auth=owner_auth) == sms_count
        check_notifications(orders, [["shift_start", "nearby"], ["shift_start"], ["shift_start"]])

        current_location['lat'] = first_order['lat']
        current_location['lon'] = first_order['lon']
        util.finish_order(system_env_with_db, first_order, company_id=owner_company_id, auth=owner_auth)

        query_routed_orders(system_env_with_db, courier_id, route_id, current_location, auth=owner_auth)
        assert _current_sms_count(system_env_with_db, company_id=owner_company_id, auth=owner_auth) == sms_count
        check_notifications(orders, [["shift_start", "nearby"], ["shift_start"], ["shift_start"]])

        util.confirm_order(system_env_with_db, second_order, company_id=owner_company_id, auth=owner_auth)
        current_location = util.get_location_nearby(second_order, "08:40")
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location, auth=owner_auth)

        # check that we sent one "next order" sms for order 1
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=owner_company_id, auth=owner_auth) == sms_count
        check_notifications(orders, [["shift_start", "nearby"], ["shift_start", "nearby"], ["shift_start"]])

        current_location['lat'] = second_order['lat']
        current_location['lon'] = second_order['lon']
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location, auth=owner_auth)
        util.finish_order(system_env_with_db, second_order, company_id=owner_company_id, auth=owner_auth)
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location, auth=owner_auth)
        assert _current_sms_count(system_env_with_db, company_id=owner_company_id, auth=owner_auth) == sms_count
        check_notifications(orders, [["shift_start", "nearby"], ["shift_start", "nearby"], ["shift_start"]])


def test_sms_logic_normal_scenario(system_env_with_db):
    env = system_env_with_db

    with create_tmp_company(env, "Test owner company test_sms_logic_normal_scenario") as owner_company_id:
        with create_tmp_user(env, owner_company_id, UserRole.admin) as owner_user:
            owner_auth = env.get_user_auth(owner_user)

            def check_notifications(orders, expected_notification_types):
                notifications0 = _get_notifications_by_get_order(env, orders, company_id=owner_company_id, auth=owner_auth)
                notifications1 = _get_notifications_by_get_orders(env, orders, company_id=owner_company_id, auth=owner_auth)
                notifications2 = _get_notifications_by_order_notifications(env, orders, company_id=owner_company_id, auth=owner_auth)
                assert notifications0 == notifications1
                assert notifications0 == notifications2
                _check_notification_types(notifications0, expected_notification_types)
                # Check that URLs are unique
                urls = [y['tracking_url'] for x in notifications0 for y in x]
                assert len(set(urls)) == len(urls)

            _do_test_sms_logic_normal_scenario(
                env,
                owner_company_id,
                owner_auth,
                preprocess_orders=None,
                check_notifications=check_notifications)


def test_sms_logic_normal_scenario_shared(system_env_with_db):
    env = system_env_with_db

    company_names = [
        "Test owner company test_sms_logic_normal_scenario_shared",
        "Test other company test_sms_logic_normal_scenario_shared",
    ]
    with create_tmp_companies(env, company_names) as company_ids:
        owner_company_id = company_ids[0]
        company_id = company_ids[1]
        with create_tmp_users(env, company_ids, [UserRole.admin] * 2) as users:
            owner_auth = env.get_user_auth(users[0])
            auth = env.get_user_auth(users[1])

            def share_orders(orders):
                for order in orders:
                    assert env_patch_request(
                        env,
                        path=api_path_with_company_id(env, "orders/{}".format(order["id"]), company_id=owner_company_id),
                        data={'shared_with_company_ids': [company_id]},
                        auth=owner_auth
                    ).status_code == requests.codes.ok

            def check_notifications(orders, expected_notification_types):
                notifications0 = _get_notifications_by_get_order(env, orders, company_id=owner_company_id, auth=owner_auth)
                notifications1 = _get_notifications_by_get_orders(env, orders, company_id=owner_company_id, auth=owner_auth)
                notifications2 = _get_notifications_by_order_notifications(env, orders, company_id=owner_company_id, auth=owner_auth)
                assert notifications0 == notifications1
                assert notifications0 == notifications2
                _check_notification_types(notifications0, expected_notification_types)

                # We do not use _get_notifications_by_get_order() below because it calls /orders/<order_id> that
                # doesn't support access for non-owning company users
                notifications3 = _get_notifications_by_get_orders(env, orders, company_id=owner_company_id, auth=auth)
                notifications4 = _get_notifications_by_order_notifications(env, orders, company_id=company_id, auth=auth)
                assert notifications3 == notifications4
                _check_notification_types(notifications3, expected_notification_types)

                # Check that URLs are unique
                urls = [z['tracking_url'] for x in [notifications0, notifications3] for y in x for z in y]
                assert len(set(urls)) == len(urls)

            _do_test_sms_logic_normal_scenario(
                env,
                owner_company_id,
                owner_auth,
                preprocess_orders=share_orders,
                check_notifications=check_notifications)


def test_sms_logic_confirmed_before_shift_start(system_env_with_db):
    """
    Test the following workflow:
        - a route with 2 orders is created
        - first order is confirmed
            * check: next order sms for it is sent
        - courier shift starts (first time interval begins)
            * check: notification sms is sent only for the second order
        - first order is delivered
        - second order is confirmed
            * check: next order sms for it is sent
    """
    with util.create_route_env(
            system_env_with_db,
            'test_sms_logic_confirmed_before_shift_start',
            order_locations=ORDERS_LOCS,
            time_intervals=INTERVALS,
            route_date=ROUTE_DATETIME.date().isoformat()) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']

        sms_count = _current_sms_count(system_env_with_db, company_id=None, auth=None)

        current_location = {
            'lat': COURIER_START_POSITION['lat'],
            'lon': COURIER_START_POSITION['lon'],
            'time_now': '07:00'
        }
        _push_position(system_env_with_db, current_location, courier_id, route_id, auth=None)

        routed_orders = query_routed_orders(system_env_with_db, courier_id, route_id, current_location)

        first_order = routed_orders['route'][0].copy()
        second_order = routed_orders['route'][1].copy()

        # Confirm first order before courier shift start
        util.confirm_order(system_env_with_db, first_order)

        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count

        # courier shift started
        # check that notification smses are sent for both orders
        current_location['time_now'] = '08:01'
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        sms_count += 2
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count

        current_location = util.get_location_nearby(first_order, "08:30")
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count
        util.finish_order(system_env_with_db, first_order)

        # Confirm second order
        # and check that next order sms is sent
        util.confirm_order(system_env_with_db, second_order)
        current_location = util.get_location_nearby(second_order, "08:50")
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count


def test_sms_logic_delivered_before_shift_start(system_env_with_db):
    """
    Test the following workflow:
        - a route with 2 orders is created
        - first order is confirmed
            * check: next order sms for it is sent
        - first order is delivered
        - courier shift starts (first time interval begins)
            * check: notification sms is sent only for the second order
        - second order is confirmed
            * check: next order sms for it is sent
    """
    with util.create_route_env(
            system_env_with_db,
            'test_sms_logic_delivered_before_shift_start',
            order_locations=ORDERS_LOCS,
            time_intervals=INTERVALS,
            route_date=ROUTE_DATETIME.date().isoformat()) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        sms_count = _current_sms_count(system_env_with_db, company_id=None, auth=None)

        current_location = {
            'lat': depot['lat'],
            'lon': depot['lon'],
            'time_now': '07:00'
        }
        _push_position(system_env_with_db, current_location, courier_id, route_id, auth=None)

        # Confirm first order before courier shift start
        # and check that next order sms is sent
        routed_orders = query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        first_order, second_order = (orders[0], orders[1]) if routed_orders['route'][0]['id'] == orders[0]['id'] \
            else (orders[1], orders[0])
        util.confirm_order(system_env_with_db, first_order)
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count

        current_location['lat'] = first_order['lat']
        current_location['lon'] = first_order['lon']
        util.finish_order(system_env_with_db, first_order)

        # courier shift started
        # check that notification sms is sent only for second order
        current_location['time_now'] = '08:01'
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count
        assert _last_sms(system_env_with_db)["order_id"] == second_order["id"]

        # Confirm second order
        # and check that next order sms is sent
        util.confirm_order(system_env_with_db, second_order)
        current_location = util.get_location_nearby(second_order, "08:50")
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count


def test_sms_logic_interval_change(system_env_with_db):
    """
    Test the following workflow:
        - a route with 2 orders is created, current time is 7:01
        - first order is confirmed with changing time interval
          from 8:00-18:00 to 7:00-8:00 (courier shift is now started)
            * check: notification sms is sent for each order
            * check: next order sms is sent for the first order
        - first order is delivered
        - second order is confirmed
            * check: next order sms for it is sent
    """
    with util.create_route_env(
            system_env_with_db,
            'test_sms_logic_interval_change',
            order_locations=ORDERS_LOCS,
            time_intervals=INTERVALS,
            route_date=ROUTE_DATETIME.date().isoformat()) as env:
        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        sms_count = _current_sms_count(system_env_with_db, company_id=None, auth=None)

        current_location = {
            'lat': depot['lat'],
            'lon': depot['lon'],
            'time_now': '07:01'
        }
        _push_position(system_env_with_db, current_location, courier_id, route_id, auth=None)

        # Determine order of orders
        routed_orders = query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        first_order, second_order = (orders[0], orders[1]) if routed_orders['route'][0]['id'] == orders[0]['id'] \
            else (orders[1], orders[0])

        # Confirm first order and modify its time window before courier shift start
        util.confirm_order(system_env_with_db, first_order, time_interval='07:00-23:59')
        current_location = util.get_location_nearby(first_order, '07:01')
        routed_orders = query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        assert first_order['id'] == routed_orders['route'][0]['id']
        assert second_order['id'] == routed_orders['route'][1]['id']

        # Check that notification sms is sent for all orders
        # and next order sms is sent for the first one
        sms_count += 3
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count

        current_location['lat'] = first_order['lat']
        current_location['lon'] = first_order['lon']
        util.finish_order(system_env_with_db, first_order)

        # Confirm second order
        # and check that next order sms is sent
        util.confirm_order(system_env_with_db, second_order)
        current_location = util.get_location_nearby(second_order, '08:10')
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count


def test_sms_logic_order_time_window(system_env_with_db):
    """
    Test the following workflow:
        - a route with 2 orders is created
        - change "time_window_end < now" for first order
        - check that only one sms was sent for second order
    """
    route_date = ROUTE_DATETIME - datetime.timedelta(days=1)
    with util.create_route_env(
            system_env_with_db,
            'test_sms_logic_order_time_window',
            order_locations=ORDERS_LOCS,
            time_intervals=['00:00-2.23:59'] * len(ORDERS_LOCS),
            route_date=route_date.date().isoformat()) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        sms_count = _current_sms_count(system_env_with_db, company_id=None, auth=None)

        current_location = {
            'lat': COURIER_START_POSITION['lat'],
            'lon': COURIER_START_POSITION['lon'],
            'time_now': '07:01'
        }
        _push_position(system_env_with_db, current_location, courier_id, route_id, auth=None)

        # change time window end
        time_window_end = datetime.datetime.now(TIMEZONE_MSK) - datetime.timedelta(minutes=10)
        time_interval_end = convert_time_window_timestamp(time_window_end, route_date, 'Europe/Moscow')
        time_interval = '00:00-' + time_interval_end
        util.change_order_status(system_env_with_db, orders[0], "new", time_interval=time_interval)

        query_routed_orders(system_env_with_db, courier_id,
                            route_id, current_location)

        # Check that notification sms is sent to one order
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count


def test_sms_messages(system_env_with_db):
    """
    Test the following workflow:
        - a route with 1 order is created
        - courier shift starts (first time interval begins)
            * check: notification sms is sent in proper format
        - first order is confirmed
            * check: next order sms is sent in proper format
    """
    with util.create_route_env(
            system_env_with_db,
            'test_sms_messages',
            order_locations=ORDERS_LOCS[:1],
            time_intervals=INTERVALS[:1],
            route_date=ROUTE_DATETIME.date().isoformat()) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']
        current_location = {
            'lat': COURIER_START_POSITION['lat'],
            'lon': COURIER_START_POSITION['lon'],
            'time_now': '08:01'
        }
        _push_position(system_env_with_db, current_location, courier_id, route_id, auth=None)
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        order = orders[0]

        # check notification sms
        assert re.match(
            f'Заказ Flash Logistics {order["number"]} ' + r'выдан для доставки\. Статус ' + TRACKING_URL_PATTERN + '$',
            _last_sms(system_env_with_db)['text']) is not None

        util.confirm_order(system_env_with_db, order)
        current_location = util.get_location_nearby(order, '08:20')
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)

        # check next order sms
        assert re.match(
            f'Заказ Flash Logistics {order["number"]} ' r'приедет через [0-9]{1,2}м\. Статус ' + TRACKING_URL_PATTERN + '$',
            _last_sms(system_env_with_db)['text']) is not None


def test_sms_postponed_order(system_env_with_db):
    """
    Check start sms is sent for postponed order, but nearby sms is not
    """
    with util.create_route_env(
            system_env_with_db,
            'test_sms_postponed_order',
            order_locations=ORDERS_LOCS,
            time_intervals=INTERVALS,
            route_date=ROUTE_DATETIME.date().isoformat()) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']

        sms_count = _current_sms_count(system_env_with_db, company_id=None, auth=None)
        util.change_order_status(system_env_with_db, orders[0], "postponed")

        current_location = {
            'lat': COURIER_START_POSITION['lat'],
            'lon': COURIER_START_POSITION['lon'],
            'time_now': '08:01'
        }
        _push_position(system_env_with_db, current_location, courier_id, route_id, auth=None)

        # Check that "route-start" sms is sent for the both orders
        query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        sms_count += 2
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count

        # Check that only one sms got "nearby" sms
        for order in orders:
            current_location = util.get_location_nearby(order, '08:20')
            query_routed_orders(system_env_with_db, courier_id, route_id, current_location)
        sms_count += 1
        assert _current_sms_count(system_env_with_db, company_id=None, auth=None) == sms_count
