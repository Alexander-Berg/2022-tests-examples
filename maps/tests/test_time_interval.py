import requests
from datetime import date, datetime
from collections import namedtuple
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    create_route_env, create_route_envs, patch_order,
    get_order, get_orders, batch_orders,
    api_path_with_company_id, env_patch_request
)


from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote

POINTS = [{"lat": 55.663878, "lon": 37.482458}]

ROUTE_DATE_OBJECTS = [
    date(2018, 10, 7),
    date(2018, 10, 8),
    date(2018, 10, 6)
]

ROUTE_DATES = [date.isoformat() for date in ROUTE_DATE_OBJECTS]

TIME_INTERVALS = [
    ["07:00-12:00"],
    ["10:00-12:00"],
    ["14:00-17:00"],
]


def test_time_window_update(system_env_with_db):
    """
    Test the following workflow:
    - create route and order
    - change time_interval for the order
        * check time_interval
    """

    with create_route_env(system_env_with_db, "timewin", POINTS,
                          time_intervals=TIME_INTERVALS[0], route_date=ROUTE_DATES[0]) as route_env:

        order = route_env["orders"][0]
        route = route_env["route"]

        for interval in ["00:00 - 02:00", "12:00 - 16:00", "22:00 - 23:00"]:
            patch_order(system_env_with_db, order, {"time_interval": interval})
            updated_orders = get_orders(system_env_with_db, route["id"])
            assert len(updated_orders) == 1

            updated_order = updated_orders[0]
            assert updated_order['time_interval'] == interval


TimeIntervalParams = namedtuple('TimeIntervalParams', ['route_idx', 'time_interval', 'expected'])

TIME_INTERVAL_PARAMS_LIST = [
    TimeIntervalParams(1, None, '07:00 - 12:00'),
    TimeIntervalParams(1, '15-18', '15:00 - 18:00'),
    TimeIntervalParams(2, None, '07:00 - 12:00'),
    TimeIntervalParams(2, '15-18', '15:00 - 18:00'),
]


@skip_if_remote
@pytest.mark.parametrize("params", TIME_INTERVAL_PARAMS_LIST)
def test_route_and_timeinterval_patch(system_env_with_db, params):
    """
    Test the following workflow, for https://st.yandex-team.ru/BBGEO-1045:
    - create 3 routes for 3 different dates with 1 order in each route
    - move the order from the first route to the routes with the other dates (next and previous),
        with or without changing time_interval
        * check time_interval
    """

    with create_route_envs(system_env_with_db, "route_changes_timewin", order_locations=POINTS,
                           time_intervals_list=TIME_INTERVALS, route_dates=ROUTE_DATES) as route_envs:
        change = {"route_id": route_envs[params.route_idx]["route"]["id"]}
        if params.time_interval:
            change["time_interval"] = params.time_interval
        order = route_envs[0]['orders'][0]
        patch_order(system_env_with_db, order, change)
        changed_order = get_order(system_env_with_db, order['id'])
        assert changed_order["time_interval"] == params.expected


def _get_absolute_interval(date, start_hour, end_hour):
    assert end_hour > start_hour
    assert start_hour >= 0
    assert end_hour <= 23
    return '{}+03:00/{}+03:00'.format(
        datetime(date.year, date.month, date.day, start_hour).isoformat(),
        datetime(date.year, date.month, date.day, end_hour).isoformat())


ABSOLUTE_TIME_INTERVALS = [
    [_get_absolute_interval(date, 7, 12)] for date in ROUTE_DATE_OBJECTS
]

TIME_INTERVAL_PARAMS_LIST_ABSOLUTE = [
    TimeIntervalParams(1, None, '07:00 - 12:00'),
    TimeIntervalParams(1, _get_absolute_interval(ROUTE_DATE_OBJECTS[1], 15, 18), '15:00 - 18:00'),
    TimeIntervalParams(2, None, '07:00 - 12:00'),
    TimeIntervalParams(2, _get_absolute_interval(ROUTE_DATE_OBJECTS[2], 15, 18), '15:00 - 18:00'),
]


@skip_if_remote
@pytest.mark.parametrize("params", TIME_INTERVAL_PARAMS_LIST_ABSOLUTE)
def test_route_and_timeinterval_patch_absolute_intervals(system_env_with_db, params):
    """
    Test the following workflow, for https://st.yandex-team.ru/BBGEO-1045 with absolute time interval:
    - create 3 routes for 3 different dates with 1 order in each route
    - move the order from the first route to the routes with the other dates (next and previous),
        with or without changing time_interval
        * check time_interval

    ATTENTION: it could be unexpected behaviour that if route date is changes time absolute time
        interval will also change. However we assume that it is expectable behaviour for most use-cases.
    """

    with create_route_envs(system_env_with_db, "route_changes_timewin_absolute_intervals", order_locations=POINTS,
                           time_intervals_list=ABSOLUTE_TIME_INTERVALS, route_dates=ROUTE_DATES) as route_envs:
        change = {"route_id": route_envs[params.route_idx]["route"]["id"]}
        if params.time_interval:
            change["time_interval"] = params.time_interval
        order = route_envs[0]['orders'][0]
        patch_order(system_env_with_db, order, change)
        changed_order = get_order(system_env_with_db, order['id'])
        assert changed_order["time_interval"] == params.expected


@skip_if_remote
@pytest.mark.parametrize("params", TIME_INTERVAL_PARAMS_LIST)
def test_route_and_timeinterval_batch_patch(system_env_with_db, params):
    """
    Test the following workflow, for https://st.yandex-team.ru/BBGEO-1045:
    - create 3 routes for 3 different date with 1 order in each route
    - move order to routes with another dates (next and previous), with or without changing time_interval
      (orders-batch request)
        * check time_interval
    """

    with create_route_envs(system_env_with_db, "route_batch_changes_timewin", order_locations=POINTS,
                           time_intervals_list=TIME_INTERVALS, route_dates=ROUTE_DATES) as route_envs:
        order = route_envs[0]["orders"][0]
        batch_change = [{"number": order["number"], 'route_id': route_envs[params.route_idx]["route"]["id"]}]
        if params.time_interval:
            batch_change[0]['time_interval'] = params.time_interval

        batch_orders(system_env_with_db, batch_change)

        changed_order = get_order(system_env_with_db, order['id'])
        assert changed_order["time_interval"] == params.expected


@skip_if_remote
def test_bug_change_depot_time_zone(system_env_with_db):
    """
    Test the following workflow:
    - create depot with time zone Europe/Moscow
    - create route for a summer day
    - create order with time interval 08:00-21:00
    - change time_zone of the depot to Pacific/Honolulu (its UTC offset is -10:00)
    - check time_interval: it should start with a negative number (we are going to fix it, but
      the ticket is not yet created, see https://st.yandex-team.ru/BBGEO-2804).
    """

    with create_route_env(
            system_env_with_db,
            "bug_change_depot_time_zone",
            POINTS,
            time_intervals=['08:00-21:00'],
            route_date='2018-07-17') as route_env:

        route = route_env['route']

        depot = route_env['depot']
        assert depot['time_zone'] == 'Europe/Moscow'

        orders = get_orders(system_env_with_db, route['id'])
        assert len(orders) == 1
        assert orders[0]['time_interval'] == '08:00 - 21:00'

        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, 'depots/{}'.format(depot['id'])),
            data={'time_zone': 'Pacific/Honolulu'}
        )
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert j['time_zone'] == 'Pacific/Honolulu'

        orders = get_orders(system_env_with_db, route['id'])
        assert len(orders) == 1
        assert orders[0]['time_interval'] == '-1.19:00 - 08:00'
