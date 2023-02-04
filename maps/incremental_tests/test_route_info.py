import contextlib
import datetime
import dateutil
import requests
import time

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request, api_path_with_company_id,
    create_route_env, create_route_envs, query_routed_orders,
    push_positions, get_route_info, patch_order,
    create_tmp_users, patch_company,
    env_patch_request
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
    create_sharing_env,
    create_sharing_depots_with_orders)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.models import EtaType, UserRole
from ya_courier_backend.util.route_info import META_FIELDS

TEST_PARAMS = {
    'points': [
        {"lat": 55.663878, "lon": 37.482458},
        {"lat": 55.683761, "lon": 37.518000},
        {"lat": 55.705491, "lon": 37.551859},
    ],
    'route_dates': [
        datetime.date(2018, 10, 7).isoformat(),
        datetime.date(2018, 10, 8).isoformat(),
        datetime.date(2018, 10, 8).isoformat(),
        datetime.date(2018, 10, 8).isoformat()
    ],
    'time_intervals': [
        ["07:00-12:00", "15:00-16:00", "22:00-1.02:00"],
        ["10:00-12:00", "14:00-16:00", "18:00-20:00"],
        ["1.10:00-1.12:00", "1.14:00-1.16:00", "1.18:00-1.20:00"],
        ["2.10:00-2.12:00", "2.14:00-2.16:00", "1.18:00-2.20:00"]
    ],
}

ROUTE_INFO_FIELDS = {
    'meta',
    'nodes'
}


@skip_if_remote
def test_route_info_fields(system_env_with_db):
    route_date = datetime.date(2019, 11, 27)
    time_intervals = ['00:00-1.23:59:59']

    depot_data = {
        'lat': 55,
        'lon': 37,
        'time_zone': "Europe/Moscow"
    }

    with create_route_env(
            system_env_with_db,
            "test_route_info_fields",
            time_intervals=time_intervals,
            route_date=route_date.isoformat(),
            depot_data=depot_data):
        _, route_info = get_route_info(system_env_with_db, route_date=route_date)

        assert isinstance(route_info, list)
        assert len(route_info) == 1

        assert set(route_info[0].keys()) == ROUTE_INFO_FIELDS
        assert set(route_info[0]['meta'].keys()) == META_FIELDS


@skip_if_remote
def test_route_info_by_route_id(system_env_with_db):
    """
    Test the following workflow:
        - two routes with one order in each are created
        - /route-info with route_id returns the requested route info
        - /route-info with route_id set to an unknown ID fails with code 404
    """

    order_location = {"lat": 55.663878, "lon": 37.482458}
    time_interval = "07:00-12:00"
    route_date = datetime.date(2018, 10, 7).isoformat()

    with create_route_envs(
            system_env_with_db,
            "test_route_info_by_route_id",
            order_locations=[order_location],
            time_intervals_list=[[time_interval], [time_interval]],
            route_dates=[route_date, route_date],
            reuse_depot=True) as route_envs:

        assert len(route_envs) == 2

        # Known route IDs

        for route_env in route_envs:
            assert len(route_env['orders']) == 1

            _, route_info = get_route_info(system_env_with_db, route_id=route_env['route']['id'])

            assert len(route_info) == 1
            assert route_info[0]['meta']['id'] == str(route_env['route']['id'])
            assert route_info[0]['nodes'][0]['value']['id'] == str(route_env['orders'][0]['id'])

        # Unknown route ID

        assert env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "route-info?route_id={}".format(999999999)
            )
        ).status_code == requests.codes.not_found


@skip_if_remote
def test_route_info_delivery_time_eta_type(system_env_with_db):
    with create_route_env(
            system_env_with_db,
            "test_route_info_delivery_time_eta_type",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0],
            eta_type=EtaType.delivery_time.value) as route_env:
        """
        Ensure that /route-info doesn't fail when EtaType is delivery_time.
        To trigger service time estimation, first we request routed orders.
        After that we just try to request /route-info with strict=True,
        so the test fails if response is not 200.
        """
        _, route_info = get_route_info(system_env_with_db, route_id=route_env['route']['id'])

        locations = [(55.799, 37.7293, route_info[0]['nodes'][0]['value']['time_windows'][0]['start']['text'])]
        push_positions(
            system_env_with_db,
            route_env['courier']['id'],
            route_env['route']['id'],
            locations
        )
        query_routed_orders(system_env_with_db, route_env['courier']['id'], route_env['route']["id"])

        _, response = get_route_info(system_env_with_db, route_id=route_env['route']['id'], strict=True)


@skip_if_remote
def test_route_info_delivery_time_for_delivery_before_visit(system_env_with_db):
    """
        Test the following workflow:
        0. Company allows couriers to mark orders as finished (company.mark_delivered_enabled = False).
        1. Order is delivered by API.
        2. Order is marked as visited.
        3. delivery_time from /route_info contains time of status update event.
    """
    DATETIME = datetime.datetime(2020, 12, 1, tzinfo=dateutil.tz.tzutc())
    ROUTE_DATE = DATETIME.date().isoformat()
    SERVICE_DURATION_S = 300
    LOCATIONS = [
        {"lat": 55.733827, "lon": 37.588722}
    ]

    def offset_datetime(offset_s):
        return DATETIME + datetime.timedelta(seconds=offset_s)

    patch_company(system_env_with_db, {"mark_delivered_enabled": False})

    with create_route_env(system_env_with_db, "test_route_info_delivery_time",
                          route_date=ROUTE_DATE,
                          order_locations=LOCATIONS) as route_env:
        patch_order(system_env_with_db, route_env['orders'][0], {'status': 'finished'})
        ts_after_delivery_before_visit = datetime.datetime.now().timestamp()
        time.sleep(1)

        locations = [
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(0).timestamp()
            ),
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            route_env["courier"]["id"],
            route_env["route"]["id"],
            locations
        )

        _, route_infos = get_route_info(system_env_with_db, route_date=ROUTE_DATE)

        assert len(route_infos) == 1
        assert len(route_infos[0]['nodes']) == 1
        assert route_infos[0]['nodes'][0]['type'] == 'order'
        order = route_infos[0]['nodes'][0]['value']
        assert 'delivery_time' in order
        assert order['delivery_time']['value'] < ts_after_delivery_before_visit


@skip_if_remote
def test_route_info_by_date(system_env_with_db):
    """
    Test the following workflow:
        - two routes with one order in each are created for different dates
        - /route-info with date returns the requested route info for correspondent date
    """

    order_location = {"lat": 55.663878, "lon": 37.482458}
    time_interval = "07:00-1.12:00"
    route_dates = [
        datetime.date(2018, 10, 7).isoformat(),
        datetime.date(2018, 10, 8).isoformat(),
    ]

    with create_route_envs(
            system_env_with_db,
            "test_route_info_by_date",
            order_locations=[order_location],
            time_intervals_list=[[time_interval], [time_interval]],
            route_dates=route_dates,
            reuse_depot=True) as route_envs:

        assert len(route_envs) == 2

        # One route was active on 07.10.2018
        assert len(route_envs[0]['orders']) == 1

        _, route_info = get_route_info(system_env_with_db, route_date=route_envs[0]['route']['date'])

        assert len(route_info) == 1
        assert route_info[0]['meta']['id'] == str(route_envs[0]['route']['id'])
        assert route_info[0]['nodes'][0]['value']['id'] == str(route_envs[0]['orders'][0]['id'])

        # Two routes were active on 08.10.2018
        _, route_info = get_route_info(system_env_with_db, route_date=route_envs[1]['route']['date'])

        assert len(route_info) == 2
        assert {route_info[0]['meta']['id'], route_info[1]['meta']['id']} == {str(route_envs[0]['route']['id']), str(route_envs[1]['route']['id'])}


def test_route_info_by_depot_id(system_env_with_db):
    """
    Test the following workflow:
        - one route with one order is created
        - get /route-info for depot_id
            * check: got error
    """

    with create_route_env(
            system_env_with_db,
            "test_route_info_by_depot_id",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0]) as route_env:
        status_code, response = get_route_info(system_env_with_db, depot_id=route_env['depot']['id'], strict=False)
        assert status_code == requests.codes.unprocessable
        assert response['message'] == "Required parameter missing: 'date or route_id'"


@skip_if_remote
def test_invalid_date_format(system_env_with_db):
    status_code, response = get_route_info(system_env_with_db, route_date="not a date", strict=False)
    assert status_code == requests.codes.unprocessable
    assert "Not a valid date " in response['message']


@skip_if_remote
def test_route_info_planning_area(system_env_with_db):
    """
    Test the following workflow:
        - two routes with one order in each are created for different dates
        - /route-info with date returns the requested route info for correspondent date
    """

    order_location = {"lat": 55.663878, "lon": 37.482458}
    time_interval = "07:00-12:00"
    route_dates = [
        datetime.date(2018, 10, 7).isoformat(),
        datetime.date(2018, 10, 10).isoformat(),
    ]

    with create_route_envs(
            system_env_with_db,
            "test_route_info_planning_area",
            order_locations=[order_location],
            time_intervals_list=[[time_interval], [time_interval]],
            route_dates=route_dates,
            reuse_depot=True) as route_envs:

        assert len(route_envs) == 2

        # Known route IDs

        for route_env in route_envs:
            assert len(route_env['orders']) == 1

            _, route_info = get_route_info(system_env_with_db, route_date=route_env['route']['date'])

            assert route_info[0]['meta']['planning_area'] == {'type': 'depot',
                                                              'id': str(route_env['depot']['id']),
                                                              'time_zone': route_env['depot']['time_zone']}


@skip_if_remote
def test_order_time_windows(system_env_with_db):
    """
        Test the following workflow:
        1. Create a route with two orders.
        2. Emulate visiting first order.
        3. Check that:
           - Both orders have 'time_windows' field.
           - First order has 'delivery_time' field
             and doesn't have 'estimated_service_time_window' field.
           - Second order has 'estimated_service_time_window' field
             and doesn't have 'delivery_time' field.

    """
    DATETIME = datetime.datetime(2020, 9, 30, tzinfo=dateutil.tz.tzutc())
    ROUTE_DATE = DATETIME.date().isoformat()
    SERVICE_DURATION_S = 300
    LOCATIONS = [
        {"lat": 55.733827, "lon": 37.588722},
        {"lat": 55.729299, "lon": 37.580116}
    ]

    LOCATION_490M_FROM_ORDER = {"lat": 55.73823, "lon": 37.588722}
    LOCATION_510M_FROM_ORDER = {"lat": 55.73841, "lon": 37.588722}

    def offset_datetime(offset_s):
        return DATETIME + datetime.timedelta(seconds=offset_s)

    patch_company(system_env_with_db, {"mark_delivered_enabled": True})

    with create_route_env(system_env_with_db, "test_order_time_windows",
                          route_date=ROUTE_DATE,
                          order_locations=LOCATIONS) as route_env:
        patch_order(system_env_with_db, route_env['orders'][1], {'type': 'pickup'})
        # estimated_service_time_window won't appear without routed-orders query
        query_routed_orders(system_env_with_db, route_env['courier']['id'], route_env['route']['id'])

        # Trigger ARRIVAL and ORDER_VISIT events
        locations = [
            (
                LOCATION_510M_FROM_ORDER["lat"],
                LOCATION_510M_FROM_ORDER["lon"],
                offset_datetime(-SERVICE_DURATION_S * 0.3).timestamp()
            ),
            (
                LOCATION_490M_FROM_ORDER["lat"],
                LOCATION_490M_FROM_ORDER["lon"],
                offset_datetime(0).timestamp()
            ),
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S * 0.6).timestamp()
            ),
            (
                LOCATION_510M_FROM_ORDER["lat"],
                LOCATION_510M_FROM_ORDER["lon"],
                offset_datetime(SERVICE_DURATION_S * 1.3).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            route_env["courier"]["id"],
            route_env["route"]["id"],
            locations
        )

        # update estimated_service_time_window
        query_routed_orders(system_env_with_db, route_env['courier']['id'], route_env['route']['id'])

        # request and check /route-info response
        _, route_infos = get_route_info(system_env_with_db, route_date=ROUTE_DATE)

        assert len(route_infos) == 1
        route_info = route_infos[0]

        assert len(route_info['nodes']) == 2
        assert route_info['nodes'][0]['type'] == 'order'
        assert route_info['nodes'][0]['types'] == ['order', 'delivery']
        assert route_info['nodes'][1]['type'] == 'order'
        assert route_info['nodes'][1]['types'] == ['order', 'pickup']

        order_1 = route_info['nodes'][0]['value']
        assert 'time_windows' in order_1
        assert 'delivery_time' in order_1
        assert 'estimated_service_time' not in order_1

        order_2 = route_info['nodes'][1]['value']
        assert 'time_windows' in order_2
        assert 'delivery_time' not in order_2
        assert 'estimated_service_time' in order_2


@skip_if_remote
def test_empty_route_by_date(system_env_with_db):

    with create_route_env(
            system_env_with_db,
            "test_empty_route_by_date",
            order_locations=[],
            time_intervals=[],
            route_date=datetime.date(2018, 10, 7).isoformat()) as route_env:

        assert len(route_env['orders']) == 0

        _, route_info = get_route_info(system_env_with_db, route_date=route_env['route']['date'])

        assert len(route_info) == 1
        assert route_info[0]['meta']['id'] == str(route_env['route']['id'])
        assert len(route_info[0]['nodes']) == 0


@skip_if_remote
def test_shared_orders(system_env_with_db):
    with create_sharing_env(system_env_with_db, 'sharing_depots_', 2) as sharing_env:
        share_with_company_idx = 0
        owner_company_idx=1
        create_sharing_depots_with_orders(sharing_env, company_idx=owner_company_idx, depot_infos=[{
            'name': '0',
            'routes': [
                {
                    'orders': [
                        {'share_with': []},
                        {'share_with': [share_with_company_idx]},
                        {'share_with': []}
                    ]
                }
            ]
        }])

        # get by date
        auth = system_env_with_db.get_user_auth(sharing_env['companies'][share_with_company_idx]['users'][UserKind.admin])
        _, route_info = get_route_info(system_env_with_db,
                                       route_date=sharing_env['companies'][owner_company_idx]['all_routes'][0]['date'],
                                       company_id=sharing_env['companies'][owner_company_idx]['id'],
                                       auth=auth)
        assert len(route_info) == 1
        assert len(route_info[0]['nodes']) == 1
        assert route_info[0]['nodes'][0]['type'] == 'order'
        assert route_info[0]['nodes'][0]['value']['id'] == str(sharing_env['companies'][owner_company_idx]['all_orders'][1]['id'])

        # get by route_id
        auth = system_env_with_db.get_user_auth(sharing_env['companies'][share_with_company_idx]['users'][UserKind.admin])
        _, route_info = get_route_info(system_env_with_db,
                                       route_id=sharing_env['companies'][owner_company_idx]['all_routes'][0]['id'],
                                       company_id=sharing_env['companies'][owner_company_idx]['id'],
                                       auth=auth)
        assert len(route_info) == 1
        assert len(route_info[0]['nodes']) == 1
        assert route_info[0]['nodes'][0]['type'] == 'order'
        assert route_info[0]['nodes'][0]['value']['id'] == str(sharing_env['companies'][owner_company_idx]['all_orders'][1]['id'])


@skip_if_remote
def test_shared_orders_for_manager(system_env_with_db):
    with create_sharing_env(system_env_with_db, 'sharing_depots_', 2) as sharing_env:
        share_with_company_idx = 0
        owner_company_idx=1
        create_sharing_depots_with_orders(sharing_env, company_idx=owner_company_idx, depot_infos=[{
            'name': '0',
            'routes': [
                {
                    'orders': [
                        {'share_with': []},
                        {'share_with': [share_with_company_idx]},
                        {'share_with': []}
                    ]
                }
            ]
        }])

        share_with_manager = sharing_env['companies'][share_with_company_idx]['users'][UserKind.manager]
        share_with_company = sharing_env['companies'][share_with_company_idx]
        owner_company = sharing_env['companies'][owner_company_idx]
        data = [owner_company['id']]

        env_patch_request(
            system_env_with_db,
            '{}/user_shared_company/{}'.format(
                api_path_with_company_id(system_env_with_db, company_id=share_with_company['id']),
                share_with_manager['id'],
            ),
            data=data,
            auth=system_env_with_db.auth_header_super
        )

        # get by date
        auth = system_env_with_db.get_user_auth(share_with_manager)
        _, route_info = get_route_info(system_env_with_db,
                                       route_date=owner_company['all_routes'][0]['date'],
                                       company_id=owner_company['id'],
                                       auth=auth)
        assert len(route_info) == 1
        assert len(route_info[0]['nodes']) == 1
        assert route_info[0]['nodes'][0]['type'] == 'order'
        assert route_info[0]['nodes'][0]['value']['id'] == str(owner_company['all_orders'][1]['id'])

        # get by route_id
        auth = system_env_with_db.get_user_auth(share_with_manager)
        _, route_info = get_route_info(system_env_with_db,
                                       route_id=owner_company['all_routes'][0]['id'],
                                       company_id=owner_company['id'],
                                       auth=auth)
        assert len(route_info) == 1
        assert len(route_info[0]['nodes']) == 1
        assert route_info[0]['nodes'][0]['type'] == 'order'
        assert route_info[0]['nodes'][0]['value']['id'] == str(owner_company['all_orders'][1]['id'])


@contextlib.contextmanager
def _prepare_access_test_data(system_env_with_db, route_env):
    test_data = [
        {
            'role': UserRole.admin,
            'code': requests.codes.ok,
            'route_cnt': 1
        },
        {
            'role': UserRole.manager,
            'code': requests.codes.ok,
            'route_cnt': 1
        },
        {
            'role': UserRole.dispatcher,
            'code': requests.codes.ok,
            'route_cnt': 1
        },
        {
            'role': UserRole.app,
            'code': requests.codes.ok,
            'route_cnt': 1
        },
        {
            'role': UserRole.manager,
            'code': requests.codes.forbidden,
            'route_cnt': 0
        },
        {
            'role': UserRole.dispatcher,
            'code': requests.codes.forbidden,
            'route_cnt': 0
        }
    ]
    user_roles = [x['role'] for x in test_data]
    with create_tmp_users(system_env_with_db, [system_env_with_db.company_id] * len(user_roles), user_roles) as users:
        for i in range(len(users)):
            test_data[i]['user'] = users[i]
        env_patch_request(
            system_env_with_db,
            '{}/user_depot/{}'.format(api_path_with_company_id(system_env_with_db), users[1]['id']),
            data=[route_env['route']['depot_id']]
        )
        env_patch_request(
            system_env_with_db,
            '{}/user_depot/{}'.format(api_path_with_company_id(system_env_with_db), users[2]['id']),
            data=[route_env['route']['depot_id']]
        )
        yield test_data


@skip_if_remote
def test_access_by_depot_id(system_env_with_db):
    """
    Check that users with different roles get correct response
    when requesting /route-info by depot_id:
        'admin', 'app' - always have access
        'manager', 'dispatcher' - have access only if there is corresponding user_depot record
    """

    with create_route_env(
            system_env_with_db,
            "test_route_info_by_depot_id",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0]) as route_env:
        with _prepare_access_test_data(system_env_with_db, route_env) as test_data:
            for test_item in test_data:
                status_code, _ = get_route_info(system_env_with_db,
                                                route_date=route_env['route']['date'],
                                                depot_id=route_env['route']['depot_id'],
                                                auth=system_env_with_db.get_user_auth(test_item['user']),
                                                strict=False)
                assert status_code == test_item['code']


@skip_if_remote
def test_access_by_date(system_env_with_db):
    """
    Check that users with different roles get correct response
    when requesting /route-info by date (without depot_id):
        'admin', 'app' - have access to all routes
        'manager' - has access only to routes with corresponding user_depot record
    """

    with create_route_env(
            system_env_with_db,
            "test_route_info_by_depot_id",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0]) as route_env:
        with _prepare_access_test_data(system_env_with_db, route_env) as test_data:
            for test_item in test_data:
                _, route_info = get_route_info(system_env_with_db,
                                                route_date=route_env['route']['date'],
                                                auth=system_env_with_db.get_user_auth(test_item['user']),
                                                strict=True)
                assert len(route_info) == test_item['route_cnt']


@skip_if_remote
def test_access_by_route_id(system_env_with_db):
    """
    Check that users with different roles get correct response
    when requesting /route-info by route_id:
        'admin', 'app' - always have access
        'manager' - have access only if there is corresponding user_depot record
    """

    with create_route_env(
            system_env_with_db,
            "test_route_info_by_depot_id",
            order_locations=TEST_PARAMS['points'],
            time_intervals=TEST_PARAMS['time_intervals'][0],
            route_date=TEST_PARAMS['route_dates'][0]) as route_env:
        with _prepare_access_test_data(system_env_with_db, route_env) as test_data:
            for test_item in test_data:
                status_code, _ = get_route_info(system_env_with_db,
                                                route_id=route_env['route']['id'],
                                                auth=system_env_with_db.get_user_auth(test_item['user']),
                                                strict=False)
                assert status_code == test_item['code']
