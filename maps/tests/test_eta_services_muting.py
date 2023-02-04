import pytest

from maps.b2bgeo.ya_courier.backend.test_lib import util
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import UserKind
from datetime import datetime, timedelta
import dateutil.tz
from dateutil import parser
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote

# all orders and the starting position are 20+ km between each other
ORDER_LOC = [
    {
        'lat': 55.791928,
        'lon': 37.841492,
    },
    {
        'lat': 55.670455,
        'lon': 37.284573,
    },
    {
        'lat': 55.930423,
        'lon': 37.520571
    }
]

COURIER_START_POS = {
    'lat': 55.6447,
    'lon': 37.6727
}

TIME_ZONE = dateutil.tz.gettz('Europe/Moscow')
ROUTE_DATETIME = datetime.now(tz=TIME_ZONE)


def _push_position(system_env_with_db, courier_id, route_id, new_locations, auth=None):
    track = [
        (
            loc['lat'],
            loc['lon'],
            parser.parse(str(ROUTE_DATETIME.date()) + 'T' + loc['time_now']).replace(tzinfo=TIME_ZONE).timestamp()
        ) for loc in new_locations
    ]
    util.push_positions(system_env_with_db, courier_id, route_id, track, auth=auth)


class _ServiceCheckInterface(object):
    def __init__(self, system_env_with_db, courier_id, route_id):
        self.env = system_env_with_db
        self.courier_id = courier_id
        self.route_id = route_id

    def check_on_shift_start(self, courier_start_datetime):
        """
        This method is called exactly once: when courier starts his shift and is still far (30+ minutes) from any of
         the orders.
        """
        raise NotImplementedError

    def check_on_visit_complete(self, order_idx, expect_eta_service_enabled):
        """
        Called after each visit of an order.
        :param int order_idx: index of the visited order in the predefined sequence of the route.
        :param bool expect_eta_service_enabled: True if the service is expected to provide or utilize ETA info.
        """
        raise NotImplementedError


_ETA_FIELDS_MANDATORY = ['arrival_time_s', 'waiting_duration_s']
_ETA_FIELDS_OPTIONAL = ['failed_time_window']


def _check_eta_fields(obj, eta_should_be_present):
    assert all([(obj[field] is not None) == eta_should_be_present for field in _ETA_FIELDS_MANDATORY])
    assert all([(obj[field] is None) or eta_should_be_present for field in _ETA_FIELDS_OPTIONAL])

    assert not eta_should_be_present or obj['arrival_time_s'] > 60.


class _SmsChecker(_ServiceCheckInterface):
    def __init__(self, system_env_with_db, courier_id, route_id):
        super(_SmsChecker, self).__init__(system_env_with_db, courier_id, route_id)
        self.sms_count = util.current_sms_count(system_env_with_db, ROUTE_DATETIME)
        self.next_nearby_order_idx = 0

    def check_on_shift_start(self, courier_start_datetime):
        # send out shift-started sms
        self.sms_count += len(ORDER_LOC)
        assert util.current_sms_count(self.env, ROUTE_DATETIME) == self.sms_count

    def check_on_visit_complete(self, order_idx, nearby_sms_should_be_sent):
        if order_idx == self.next_nearby_order_idx:
            self.sms_count += 1 if nearby_sms_should_be_sent else 0
            self.next_nearby_order_idx += 1
        assert util.current_sms_count(self.env, ROUTE_DATETIME) == self.sms_count


class _TrackingInfoChecker(_ServiceCheckInterface):
    def __init__(self, system_env_with_db, courier_id, route_id, orders):
        super(_TrackingInfoChecker, self).__init__(system_env_with_db, courier_id, route_id)
        self.tokens = util.create_tracking_tokens(system_env_with_db, courier_id, route_id, orders)
        self.visited_order_idx = []

    def check_on_shift_start(self, courier_start_datetime):
        for token in self.tokens:
            tracking = util.get_tracking_info(token, self.env)
            assert tracking['order']['arrival_time_s'] > courier_start_datetime.hour * 3600.

            track = util.query_track(token, self.env)
            assert 'eta_error' not in track, track['eta_error']
            assert 'eta_iso' in track
            eta = parser.parse(track['eta_iso'])
            assert eta > courier_start_datetime + timedelta(minutes=15)

    def check_on_visit_complete(self, order_idx, eta_should_be_present):
        self.visited_order_idx.append(order_idx)  # there is no ETA for a completed order

        for idx, token in enumerate(self.tokens):
            if idx in self.visited_order_idx:
                continue

            tracking = util.get_tracking_info(token, self.env)
            _check_eta_fields(tracking['order'], eta_should_be_present)

            track = util.query_track(token, self.env)
            assert (track['eta_iso'] is not None) == eta_should_be_present


class _OrderDetailsChecker(_ServiceCheckInterface):
    def __init__(self, system_env_with_db, orders, company_id=None, auth=None):
        super(_OrderDetailsChecker, self).__init__(system_env_with_db, courier_id=None, route_id=None)
        self.orders = orders
        self.company_id = company_id
        self.auth = auth

    def check_on_shift_start(self, courier_start_datetime):
        for order in self.orders:
            detail = util.get_order_details(self.env, order['number'], company_id=self.company_id,
                                            auth=self.auth)
            _check_eta_fields(detail, eta_should_be_present=True)
            assert detail['arrival_time_s'] > 1000.0

    def check_on_visit_complete(self, order_idx, expect_eta_service_enabled):
        for order in self.orders:
            detail = util.get_order_details(self.env, order['number'], company_id=self.company_id,
                                            auth=self.auth)
            if detail['status'] == 'finished':
                continue
            _check_eta_fields(detail, expect_eta_service_enabled)


class _RouteDetailsChecker(_ServiceCheckInterface):
    def __init__(self, system_env_with_db, courier_number, orders, company_id=None, auth=None):
        super(_RouteDetailsChecker, self).__init__(system_env_with_db, courier_id=company_id, route_id=None)
        self.courier_number = courier_number
        self.orders = orders
        self.company_id = company_id
        self.auth = auth

    def check_on_shift_start(self, courier_start_datetime):
        route_details = util.get_route_details(self.env, ROUTE_DATETIME.date().isoformat(), self.courier_number,
                                               self.company_id, self.auth)
        assert len(route_details) == 1
        route_state = route_details[0]['route_state']

        _check_eta_fields(route_state['next_order'], eta_should_be_present=True)
        assert len(route_state['routed_orders']) == len(self.orders)
        for routed_order in route_state['routed_orders']:
            _check_eta_fields(routed_order, eta_should_be_present=True)
            assert routed_order['arrival_time_s'] > 1000.0

    def check_on_visit_complete(self, order_idx, expect_eta_service_enabled):
        route_details = util.get_route_details(self.env, ROUTE_DATETIME.date().isoformat(), self.courier_number,
                                               self.company_id, self.auth)
        assert len(route_details) == 1
        route_state = route_details[0]['route_state']
        if 'next_order' in route_state:
            _check_eta_fields(route_state['next_order'], expect_eta_service_enabled)
        assert len(route_state['routed_orders']) == len(self.orders)
        for routed_order in route_state['routed_orders']:
            if routed_order['status'] == 'finished':
                continue
            _check_eta_fields(routed_order, expect_eta_service_enabled)
            if expect_eta_service_enabled:
                assert routed_order['arrival_time_s'] > 10.0


@skip_if_remote
@pytest.mark.parametrize("route_sequence_violated", [False, True])
def test_eta_services_are_not_provided_if_route_sequence_violated(system_env_with_db, route_sequence_violated):
    """
    Test the following workflow:
        - a route with 3 orders is created
        - determine a predefined sequence and an actual route to follow based on the test's parameters.
        - courier shift starts
            * check: ETA info is available
        - courier visits next order
            * check: if such order of visits violates sequence then
              no ETA is provided (for this and follow-up orders).
    """
    with util.create_route_env(
            system_env_with_db,
            'test_eta_services_muting' + f'-{int(route_sequence_violated)}',
            order_locations=ORDER_LOC,
            time_intervals=['08:00-23:59'] * len(ORDER_LOC),
            route_date=ROUTE_DATETIME.date().isoformat()) as env:

        ################
        # Scenario setup
        #
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        orders = env['orders']
        util.post_order_sequence(system_env_with_db, route_id, payload=[o['id'] for o in orders])

        actual_visit_sequence = [0, 2, 1] if route_sequence_violated else [0, 1, 2]

        def _move_courier(new_locations):
            util.query_routed_orders(system_env_with_db, courier_id, route_id, new_locations[0])
            _push_position(system_env_with_db, courier_id, route_id, new_locations)

        checkers = [
            _SmsChecker(system_env_with_db, courier_id, route_id),
            _TrackingInfoChecker(system_env_with_db, courier_id, route_id, orders),
            _OrderDetailsChecker(system_env_with_db, orders),
            _RouteDetailsChecker(system_env_with_db, env['courier']['number'], orders)
        ]
        assert len(checkers) == len(_ServiceCheckInterface.__subclasses__())

        ####################
        # Scenario execution
        #
        time_now = ROUTE_DATETIME.replace(hour=8, minute=1)
        current_location = {
            'lat': COURIER_START_POS['lat'],
            'lon': COURIER_START_POS['lon'],
            'time_now': time_now.time().strftime("%H:%M")
        }
        _move_courier([current_location])
        for checker in checkers:
            checker.check_on_shift_start(time_now)

        violation_occurred = False
        for idx, order_idx in enumerate(actual_visit_sequence):
            violation_occurred = violation_occurred or order_idx != idx
            current_order = orders[order_idx]

            time_now += timedelta(minutes=31)
            approaching_duration = timedelta(minutes=15)
            visit_duration = timedelta(minutes=15)
            next_locations = [
                util.get_location_nearby(current_order, time_now.time().strftime("%H:%M")),
                {
                    'lat': current_order['lat'],
                    'lon': current_order['lon'],
                    'time_now': (time_now + approaching_duration).time().strftime("%H:%M")
                },
                {
                    'lat': current_order['lat'],
                    'lon': current_order['lon'],
                    'time_now': (time_now + approaching_duration + visit_duration).time().strftime("%H:%M")
                }
            ]
            time_now += approaching_duration + visit_duration
            _move_courier(next_locations)
            expect_eta_service_enabled = not violation_occurred
            for checker in checkers:
                checker.check_on_visit_complete(order_idx, expect_eta_service_enabled)


@skip_if_remote
def test_eta_services_shared_observing_on_violated_route(env_with_2_companies_sharing_setup):
    """
    Check that ETA info presence for violated routes is not provided for observer company.
    """
    sharing_env = env_with_2_companies_sharing_setup
    db_env = sharing_env['dbenv']
    delivery_company_idx = 1
    delivery_company_id = sharing_env['companies'][delivery_company_idx]['id']
    shared_with_company_idx = 0

    ################
    # Scenario setup
    #
    shared_route = sharing_env['companies'][delivery_company_idx]['sharing_routes'][shared_with_company_idx][0]
    all_route_orders = [
        order for order in sorted(sharing_env['companies'][delivery_company_idx]['all_orders'],
                                  key=lambda order: order['id'])
        if order['route_id'] == shared_route['id']
    ]
    assert len(all_route_orders) > 1
    shared_orders = [order for order in all_route_orders
                     if sharing_env['companies'][shared_with_company_idx]['id'] in order['shared_with_company_ids']]
    assert len(shared_orders) > 0

    courier_id = shared_route['courier_id']
    courier = sharing_env['companies'][delivery_company_idx]['sharing_couriers'][shared_with_company_idx][0]
    assert courier_id == courier['id']  # sanity check
    courier_number = courier['number']

    def _move_courier(new_locations):
        util.query_routed_orders(db_env, courier_id, shared_route['id'], new_locations[0],
                                 auth=db_env.auth_header_super)
        _push_position(db_env, courier_id, shared_route['id'], new_locations, auth=db_env.auth_header_super)

    shared_with_company_auth = db_env.get_user_auth(
        sharing_env['companies'][shared_with_company_idx]['users'][UserKind.admin])
    checkers = [
        _OrderDetailsChecker(db_env, shared_orders, delivery_company_id, shared_with_company_auth),
        _RouteDetailsChecker(db_env, courier_number, shared_orders, delivery_company_id, shared_with_company_auth)
    ]
    actual_visit_sequence = list(reversed(range(len(all_route_orders))))  # this violates the default route order

    ####################
    # Scenario execution
    #
    route_date = parser.parse(sharing_env['companies'][delivery_company_idx]['date'])
    time_now = datetime.combine(route_date, datetime.now(dateutil.tz.gettz('Europe/Moscow')).time())
    start_location = {
        'lat': COURIER_START_POS['lat'],
        'lon': COURIER_START_POS['lon'],
        'time_now': time_now.time().strftime("%H:%M")
    }
    _move_courier([start_location])
    for checker in checkers:
        checker.check_on_shift_start(time_now)

    for order_idx in actual_visit_sequence:
        current_order = all_route_orders[order_idx]

        time_now += timedelta(minutes=1)
        approaching_duration = timedelta(minutes=1)
        visit_duration = timedelta(minutes=10)
        next_locations = [
            util.get_location_nearby(current_order, time_now.time().strftime("%H:%M")),
            {
                'lat': current_order['lat'],
                'lon': current_order['lon'],
                'time_now': (time_now + approaching_duration).time().strftime("%H:%M")
            },
            {
                'lat': current_order['lat'],
                'lon': current_order['lon'],
                'time_now': (time_now + approaching_duration + visit_duration).time().strftime("%H:%M")
            }
        ]
        time_now += approaching_duration + visit_duration
        _move_courier(next_locations)
        for checker in checkers:
            checker.check_on_visit_complete(order_idx, False)
