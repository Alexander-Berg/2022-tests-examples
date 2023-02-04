import json
from uuid import uuid4

import pytest
from pytest import approx
import time
from datetime import datetime, timedelta
import dateutil.parser
import dateutil.tz
import requests

from ya_courier_backend.config.common import COURIER_POSITION_HISTORY_DAYS
from ya_courier_backend.models.route import ACTIVE_ROUTE_MAX_EARLINESS_H, ACTIVE_ROUTE_MAX_LATENESS_H
from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str
from ya_courier_backend.util.tracking import ZERO_POSITION_EPSILON

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request, api_path_with_company_id, get_orders,
    push_positions, push_imei_positions, get_courier_position_list, request,
    create_route_env, get_last_courier_positions, change_order_time_interval,
    actual_time_interval, later_time_interval, _push_positions_v3, get_company, create_tmp_company,
    patch_order_by_order_id, create_courier, patch_route
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote

from ya_courier_backend.resources.courier_position import ACTIVE_ROUTE_MAX_ROUTE_AGE_D

TEST_ID = "test_courier_position"

COURIER_NUMBER = TEST_ID + "test_courier"
DEPOT_NUMBER = TEST_ID + "test_depot"
ROUTE_NUMBER = TEST_ID + "test_route"
ORDERS_PREFIX = TEST_ID + "order"

POSITION = (55.736294, 37.582708)
POSITION_2 = (55.836294, 37.682708)
START = int(time.time())
TRACK = {
    (55.736294, 37.582708, START),
    (55.735834, 37.584918, START + 1)
}
TIME_ZONE = dateutil.tz.gettz('Europe/Moscow')


def _query_active_courier_positions(system_env_with_db, depot_id=None):
    path = "{}/courier-position".format(api_path_with_company_id(system_env_with_db))
    if depot_id is not None:
        path += "?depot_id=" + str(depot_id)
    resp = env_get_request(system_env_with_db, path)
    assert resp.ok, resp.text

    j = resp.json()

    assert(isinstance(j, list))
    if len(j) > 0:
        expected_items = ["id", "lon", "lat", "timestamp", "server_time", "server_time_iso"]
        pos = j[0]
        for item in expected_items:
            assert(item in pos)
        assert len(expected_items) == len(pos.keys())

    return j


def _check_iso_times(time_zone_str, items, field_names):
    time_zone = dateutil.tz.gettz(time_zone_str)
    for item in items:
        for field_name in field_names:
            assert item[field_name + '_iso'] == get_isoformat_str(item[field_name], time_zone)


class TestCourierPosition(object):
    @pytest.mark.parametrize('push_positions_version', [1, 2])
    def test_courier_position(self, system_env_with_db, push_positions_version):
        env = system_env_with_db
        with create_route_env(env, f"courier_position-{push_positions_version}") as route_env:
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']

            push_positions(env, courier_id, route_id, track=TRACK, version=push_positions_version)
            positions = get_courier_position_list(env, courier_id, route_id)
            assert isinstance(positions, list)
            assert len(positions) == len(TRACK)

            end = time.time()

            for position in positions:
                assert {"id", "courier_id", "route_id", "time",
                        "lat", "lon", "accuracy", "server_time",
                        "time_iso", "server_time_iso", "imei", "imei_str"} == position.keys()
                assert START <= position["server_time"] <= end

            _check_iso_times(route_env['depot']['time_zone'], positions, ['server_time', 'time'])

    @pytest.mark.parametrize('push_positions_version', [1, 2])
    def test_courier_position_duplicates(self, system_env_with_db, push_positions_version):
        env = system_env_with_db
        with create_route_env(env, f"courier_position_duplicates-{push_positions_version}") as route_env:
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']

            push_positions(env, courier_id, route_id, track=TRACK, version=push_positions_version)
            positions = get_courier_position_list(env, courier_id, route_id)
            assert isinstance(positions, list)
            assert len(positions) == len(TRACK)

            push_positions(env, courier_id, route_id, track=TRACK, version=push_positions_version)
            positions = get_courier_position_list(env, courier_id, route_id)
            assert isinstance(positions, list)
            assert len(positions) == len(TRACK)

    @pytest.mark.parametrize('push_positions_version', [1, 2, 'gps'])
    def test_courier_position_filtering(self, system_env_with_db, push_positions_version):
        imei = 100200 if push_positions_version == 'gps' else None
        env = system_env_with_db
        current_datetime = datetime.now(tz=TIME_ZONE)
        with create_route_env(env, f"courier_position_filtering-{push_positions_version}", imei=imei,
                              route_date=current_datetime.date().isoformat()) as route_env:
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']

            def _push_position(lat, lon):
                if imei:
                    push_imei_positions(env, imei, start_datetime=current_datetime, track=[(lat, lon)])
                else:
                    push_positions(env, courier_id, route_id,
                                   track=[(pos[0], pos[1], current_datetime.timestamp())],
                                   version=push_positions_version)

            expected_position_count = 0
            for pos, expect_stored in [
                    [(44.4, 33.3), True],
                    [(0, 0), False],
                    [(ZERO_POSITION_EPSILON, ZERO_POSITION_EPSILON), True],
                    [(-ZERO_POSITION_EPSILON, -ZERO_POSITION_EPSILON), True],
                    [(0.9 * ZERO_POSITION_EPSILON, ZERO_POSITION_EPSILON), True],
                    [(ZERO_POSITION_EPSILON, 0.9 * ZERO_POSITION_EPSILON), True],
                    [(0.9 * ZERO_POSITION_EPSILON, 0.9 * ZERO_POSITION_EPSILON), False],
                    [(-0.9 * ZERO_POSITION_EPSILON, -0.9 * ZERO_POSITION_EPSILON), False],
                    [(40.5, -180.1), False],
                    [(40.5, 180.1), False],
                    [(-90.1, 50.1), False],
                    [(90.1, 50.1), False],
                    [(90, -180), True],
                    [(-90, 180), True],
                    # positions inside of forbidden area:
                    [(55.971, 37.41), False],
                    [(55.599, 37.2693), False],
                    [(55.407, 37.91), False],
                    # position near forbidden area:
                    [(55.95, 37.415), True]
            ]:
                _push_position(pos[0], pos[1])
                current_datetime += timedelta(milliseconds=100)
                expected_position_count += expect_stored
                positions = get_courier_position_list(env, courier_id, route_id)
                assert isinstance(positions, list)
                assert len(positions) == expected_position_count

    def test_courier_position_bad_page(self, system_env_with_db):
        env = system_env_with_db
        with create_route_env(env, "courier_position_bad_page") as route_env:
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']

            response = env_get_request(
                env,
                '{}/courier-position/{}/routes/{}?page=0'.format(
                    api_path_with_company_id(env),
                    courier_id, route_id
                )
            )
            assert response.status_code == requests.codes.unprocessable
            assert 'Must be greater than or equal to 1' in response.content.decode()

    def test_courier_position_old_route(self, system_env_with_db):
        env = system_env_with_db
        route_date = datetime.now(TIME_ZONE).date() - timedelta(days=COURIER_POSITION_HISTORY_DAYS+1)
        with create_route_env(env, "courier_position_old_route", route_date=route_date.isoformat()) as route_env:
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']

            positions = get_courier_position_list(env, courier_id, route_id)
            assert isinstance(positions, list)


def _route_date():
    return datetime.now(TIME_ZONE).date().isoformat()


class TestActiveCourierPositions(object):
    def test_active_courier_positions_one_active(self, system_env_with_db):
        """
        Test the following workflow:
            - a route with 1 order and active time_window is created
            - a position for this route is pushed
            - courier-position handler is requested
                * check: positions contain the position from the created route
        """
        with create_route_env(
            system_env_with_db,
            "test_active_courier_positions_one_active",
            order_locations=[{'lat': POSITION[0], 'lon': POSITION[1]}],
            time_intervals=[actual_time_interval()],
            route_date=_route_date()
        ) as env:
            courier = env["courier"]
            courier_id = courier["id"]
            route = env["route"]
            route_id = route["id"]

            position_time = time.time()
            push_positions(system_env_with_db, courier_id, route_id, [(POSITION[0], POSITION[1], position_time)])
            positions = _query_active_courier_positions(system_env_with_db)

            assert len(positions) >= 1
            current_courier_positions = [p for p in positions if p['id'] == courier_id]
            assert len(current_courier_positions) == 1
            pos = current_courier_positions[0]
            assert pos['lat'] == approx(POSITION[0])
            assert pos['lon'] == approx(POSITION[1])
            assert pos['timestamp'] == approx(position_time, 30)
            _check_iso_times(env['depot']['time_zone'], current_courier_positions, ['server_time'])

    def test_active_courier_positions_one_inactive(self, system_env_with_db):
        """
        Test the following workflow:
            - a route with 1 order and inactive time_window is created
            - a position for this route is pushed
            - courier-position handler is requested
                * check: positions don't contain the position from the created route
        """
        with create_route_env(
            system_env_with_db,
            "test_active_courier_positions_one_inactive",
            order_locations=[{'lat': POSITION[0], 'lon': POSITION[1]}],
            time_intervals=[later_time_interval()],
            route_date=_route_date()
        ) as env:
            courier = env["courier"]
            courier_id = courier["id"]
            route = env["route"]
            route_id = route["id"]

            push_positions(system_env_with_db, courier_id, route_id, [(POSITION[0], POSITION[1], time.time())])
            positions = _query_active_courier_positions(system_env_with_db)

            current_courier_positions = [p for p in positions if p['id'] == courier_id]
            assert len(current_courier_positions) == 0

    def test_active_courier_positions_one_active_one_not(self, system_env_with_db):
        """
        Test the following workflow:
            - a route with 1 order and active time_window is created
            - a position for the active route is pushed
            - a route with 1 order and inactive time_window is created
            - a position for the inactive route is pushed
            - courier-position handler is requested
                * check: positions contain the position from the active route
                         and don't contain the position from the inactive route
        """
        with create_route_env(
            system_env_with_db,
            "test_active_courier",
            order_locations=[{'lat': POSITION[0], 'lon': POSITION[1]}],
            time_intervals=[actual_time_interval()],
            route_date=_route_date()
        ) as active_env:
            active_courier = active_env["courier"]
            active_courier_id = active_courier["id"]
            active_route = active_env["route"]
            active_route_id = active_route["id"]

            position_time = time.time()
            push_positions(system_env_with_db, active_courier_id, active_route_id, [(POSITION[0], POSITION[1], position_time)])

            with create_route_env(
                system_env_with_db,
                "test_inactive_courier",
                order_locations=[{'lat': POSITION[0], 'lon': POSITION[1]}],
                time_intervals=[later_time_interval()],
                route_date=_route_date()
            ) as inactive_env:
                inactive_courier = inactive_env["courier"]
                inactive_courier_id = inactive_courier["id"]
                inactive_route = inactive_env["route"]
                inactive_route_id = inactive_route["id"]

                push_positions(system_env_with_db, inactive_courier_id, inactive_route_id, [(POSITION[0], POSITION[1], time.time())])

                positions = _query_active_courier_positions(system_env_with_db)
                assert len(positions) >= 1
                active_courier_positions = [p for p in positions if p['id'] == active_courier_id]
                assert len(active_courier_positions) == 1
                pos = active_courier_positions[0]
                assert pos['lat'] == approx(POSITION[0])
                assert pos['lon'] == approx(POSITION[1])
                assert pos['timestamp'] == approx(position_time, 30)
                _check_iso_times(active_env['depot']['time_zone'], active_courier_positions, ['server_time'])

                inactive_courier_positions = [p for p in positions if p['id'] == inactive_courier_id]
                assert len(inactive_courier_positions) == 0

    def test_show_only_last_position(self, system_env_with_db):
        """
        Test the following workflow:
            - a route with 1 order and active time_window is created
            - 2 positions for this route are pushed
            - courier-position handler is requested
                * check: positions contain only the last position from the created route
        """
        with create_route_env(
            system_env_with_db,
            "test_show_only_last_position",
            order_locations=[{'lat': POSITION[0], 'lon': POSITION[1]}],
            time_intervals=[actual_time_interval()],
            route_date=_route_date()
        ) as env:
            courier = env["courier"]
            courier_id = courier["id"]
            route = env["route"]
            route_id = route["id"]

            push_positions(system_env_with_db, courier_id, route_id, [(POSITION[0], POSITION[1], time.time())])
            push_positions(system_env_with_db, courier_id, route_id, [(POSITION_2[0], POSITION_2[1], time.time())])

            positions = _query_active_courier_positions(system_env_with_db)

            assert len(positions) >= 1
            current_courier_positions = [p for p in positions if p['id'] == courier_id]
            assert len(current_courier_positions) == 1
            pos = current_courier_positions[0]
            assert pos['lat'] != approx(POSITION[0])
            assert pos['lon'] != approx(POSITION[1])
            assert pos['lat'] == approx(POSITION_2[0])
            assert pos['lon'] == approx(POSITION_2[1])
            _check_iso_times(env['depot']['time_zone'], current_courier_positions, ['server_time'])

    def test_depot_id_filtering(self, system_env_with_db):
        """
        Test the following workflow:
            - 2 routes each with 1 order and active time_window are created for different depots
            - 1 position is pushed for every route
            - courier-position handler is requested with depot_id filtering
                * check: positions contain only the position from the corresponding route
                * check: positions don't contain positions from created routes for another depot_id
        """
        time_intervals = [actual_time_interval()]
        route_date = _route_date()
        with create_route_env(
            system_env_with_db,
            "test_depot_id_filtering_first_env",
            order_locations=[{"lat": POSITION[0], "lon": POSITION[1]}],
            time_intervals=time_intervals,
            route_date=route_date
        ) as env1:
            with create_route_env(
                system_env_with_db,
                "test_depot_id_filtering_second_env",
                order_locations=[{"lat": POSITION[0], "lon": POSITION[1]}],
                time_intervals=time_intervals,
                route_date=route_date,
                route_start="00:00:00"
            ) as env2:
                push_positions(system_env_with_db, env1["courier"]["id"], env1["route"]["id"], [(POSITION[0], POSITION[1], time.time())])
                push_positions(system_env_with_db, env2["courier"]["id"], env2["route"]["id"], [(POSITION_2[0], POSITION_2[1], time.time())])

                positions = _query_active_courier_positions(system_env_with_db, env1["depot"]["id"])
                assert len(positions) == 1
                pos = positions[0]
                assert pos["id"] == env1["courier"]["id"]
                assert pos["lat"] == approx(POSITION[0])
                assert pos["lon"] == approx(POSITION[1])
                _check_iso_times(env1["depot"]["time_zone"], positions, ["server_time"])

                positions = _query_active_courier_positions(system_env_with_db, env2["depot"]["id"])
                assert len(positions) == 1
                pos = positions[0]
                assert pos["id"] == env2["courier"]["id"]
                assert pos["lat"] == approx(POSITION_2[0])
                assert pos["lon"] == approx(POSITION_2[1])
                _check_iso_times(env2["depot"]["time_zone"], positions, ["server_time"])

                dummy_depot_id = 12345
                positions = _query_active_courier_positions(system_env_with_db, dummy_depot_id)
                assert len(positions) == 0


class TestCourierPositionActiveRoute(object):
    """
    Position is available in /courier-position if:
        - current time is greater than route_start_s if specified otherwise than min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H
        - current time is less than route_finish_s if specified otherwise than max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H
        - position’s time is greater than current time - ACTIVE_ROUTE_MAX_POSITIONS_AGE_D
    """
    now = datetime.now(tz=dateutil.tz.gettz('Europe/Moscow'))
    order_locations = [{"lat": 55.733827, "lon": 37.588722}]
    route_date = now - timedelta(days=1)

    def get_eninty_ids(self, env):
        return env['courier']['id'], env['depot']['id'], env['route']['id'], env['orders'][0]['id']

    def push_position_check_is_recorded(self, system_env_with_db, courier_id, route_id, position_time):
        position_time_str = position_time.isoformat()
        track = [(22.2, 33.3, position_time_str)]
        push_positions(system_env_with_db, courier_id, route_id, track)
        recorded_positions = get_courier_position_list(system_env_with_db, courier_id, route_id)
        assert len(recorded_positions) == 1
        assert recorded_positions[0]['lat'] == 22.2

    def get_last_courier_positions_check(self, system_env_with_db, depot_id, courier_id, must_be_recoded=True):
        response = get_last_courier_positions(system_env_with_db, depot_id)
        if must_be_recoded:
            assert len(response) == 1
            assert response[0]['lat'] == 22.2
            assert courier_id == response[0]['id']
        else:
            assert len(response) == 0

    def get_route_start_or_route_finish(self, route_time_s_datetime, route_date):
        route_datetime = datetime(route_date.year, route_date.month, route_date.day, tzinfo=dateutil.tz.gettz('Europe/Moscow'))
        diff = route_time_s_datetime - route_datetime
        hours_minutes = f"{diff.seconds//3600}:{(diff.seconds)//60%60}"
        if diff.days == 0:
            return hours_minutes
        return str(diff.days) + "." + hours_minutes

    def test_routes_not_older_than_max_positions_age_is_active(self, system_env_with_db):
        route_date = self.now - timedelta(days=ACTIVE_ROUTE_MAX_ROUTE_AGE_D - 1)
        route_start = self.now - timedelta(hours=1)
        route_finish = self.now + timedelta(hours=1)
        position_time = self.now

        with create_route_env(
            system_env_with_db,
            'test_routes_not_older_than_max_positions_age_is_active',
            route_date=route_date.isoformat(),
            route_finish=self.get_route_start_or_route_finish(route_finish, route_date),
            route_start=self.get_route_start_or_route_finish(route_start, route_date),
            time_intervals=[],
            order_locations=[]
        ) as env:

            courier_id, depot_id, route_id = env['courier']['id'], env['depot']['id'], env['route']['id']

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id, must_be_recoded=True)

    def test_routes_older_than_max_positions_age_is_not_active(self, system_env_with_db):
        route_date = self.now - timedelta(days=ACTIVE_ROUTE_MAX_ROUTE_AGE_D)
        route_start = self.now - timedelta(hours=1)
        route_finish = self.now + timedelta(hours=1)
        position_time = self.now

        with create_route_env(
            system_env_with_db,
            'test_routes_older_than_max_positions_age_is_not_active',
            route_date=route_date.isoformat(),
            route_finish=self.get_route_start_or_route_finish(route_finish, route_date),
            route_start=self.get_route_start_or_route_finish(route_start, route_date),
            time_intervals=[],
            order_locations=[]
        ) as env:

            courier_id, depot_id, route_id = env['courier']['id'], env['depot']['id'], env['route']['id']

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id, must_be_recoded=False)

    def test_current_time_in_timewindow_interval(self, system_env_with_db):
        """
        Make sure that recorded position is available in /courier-position when:
           1) current time is inside of
              [min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H, max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H]
              and route_start, route_finish are not defined
           2) position's time is outside of [min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H, max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H]
              and also greater than the current time - ACTIVE_ROUTE_MAX_POSITIONS_AGE_D
        """
        start = self.now
        end = start + timedelta(hours=ACTIVE_ROUTE_MAX_EARLINESS_H + 3)
        position_time = start - timedelta(hours=ACTIVE_ROUTE_MAX_EARLINESS_H - 1)

        with create_route_env(
            system_env_with_db,
            'test_courier_position_active_route_1',
            route_date=(self.route_date).isoformat(),
            time_intervals=[start.isoformat() + '/' + end.isoformat()],
            order_locations=self.order_locations
        ) as env:

            courier_id, depot_id, route_id, order_id = self.get_eninty_ids(env)

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)
            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id)

            start = self.now + timedelta(hours=ACTIVE_ROUTE_MAX_EARLINESS_H - 1)
            change_order_time_interval(system_env_with_db, {"id": order_id}, start.isoformat() + '/' + end.isoformat())

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id)

    def test_route_start(self, system_env_with_db):
        """
        Make sure that recorded position is available in /courier-position when:
           1) current time is inside of [route_start_s, min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H]
           2) position's time is greater than current time - ACTIVE_ROUTE_MAX_POSITIONS_AGE_D
        """
        route_start = self.now - timedelta(hours=1)
        start = self.now + timedelta(hours=ACTIVE_ROUTE_MAX_EARLINESS_H + 1)
        end = start + timedelta(hours=1)
        position_time = route_start + timedelta(minutes=1)

        with create_route_env(
            system_env_with_db,
            'test_courier_position_active_route_2',
            route_date=self.route_date.isoformat(),
            time_intervals=[start.isoformat() + '/' + end.isoformat()],
            route_start=self.get_route_start_or_route_finish(route_start, self.route_date),
            order_locations=self.order_locations
        ) as env:

            courier_id, depot_id, route_id, _ = self.get_eninty_ids(env)

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id)

    def test_route_finish(self, system_env_with_db):
        """
        Make sure that recorded position is available in /courier-position when:
           1) current time is inside of [max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H, route_finish_s]
           2) position's time is greater than current time - ACTIVE_ROUTE_MAX_POSITIONS_AGE_D
        """
        route_finish = self.now + timedelta(hours=1)
        end = self.now - timedelta(hours=ACTIVE_ROUTE_MAX_LATENESS_H + 1)
        start = end - timedelta(hours=1)
        position_time = route_finish - timedelta(minutes=1)

        with create_route_env(
            system_env_with_db,
            'test_courier_position_active_route_3',
            route_date=self.route_date.isoformat(),
            time_intervals=[start.isoformat() + '/' + end.isoformat()],
            route_finish=self.get_route_start_or_route_finish(route_finish, self.route_date),
            order_locations=self.order_locations
        ) as env:

            courier_id, depot_id, route_id, _ = self.get_eninty_ids(env)

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id)

    def test_unavailable_position_route_start(self, system_env_with_db):
        """
        Make sure that recorded position is not available in /courier-position when:
           1) current time is less than route_start_s
           2) position's time is greater than current time - ACTIVE_ROUTE_MAX_POSITIONS_AGE_D
        """
        route_start = self.now + timedelta(hours=1)
        start = route_start + timedelta(hours=1)
        end = start + timedelta(hours=1)
        position_time = route_start + timedelta(minutes=1)

        with create_route_env(
            system_env_with_db,
            'test_courier_position_active_route_4',
            route_date=self.route_date.isoformat(),
            time_intervals=[start.isoformat() + '/' + end.isoformat()],
            route_start=self.get_route_start_or_route_finish(route_start, self.route_date),
            order_locations=self.order_locations
        ) as env:

            courier_id, depot_id, route_id, _ = self.get_eninty_ids(env)

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id, False)

    def test_unavailable_position_route_finish(self, system_env_with_db):
        """
        Make sure that recorded position is not available in /courier-position when:
           1) current time is greater than route_finish_s
           2) position's time is greater than current time - ACTIVE_ROUTE_MAX_POSITIONS_AGE_D
        """
        route_finish = self.now - timedelta(hours=1)
        end = route_finish - timedelta(hours=1)
        start = end - timedelta(hours=1)
        position_time = route_finish - timedelta(minutes=1)

        with create_route_env(
            system_env_with_db,
            'test_courier_position_active_route_5',
            route_date=self.route_date.isoformat(),
            time_intervals=[start.isoformat() + '/' + end.isoformat()],
            route_finish=self.get_route_start_or_route_finish(route_finish, self.route_date),
            order_locations=self.order_locations
        ) as env:

            courier_id, depot_id, route_id, _ = self.get_eninty_ids(env)

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id, False)

    def test_unavailable_position(self, system_env_with_db):
        """
        Make sure that recorded position is not available in /courier-position if:
           1) current time is outside of [min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H, max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H]
              and route_start, route_finish are not defined
           2) position's time is greater than current time - ACTIVE_ROUTE_MAX_POSITIONS_AGE_D
        """
        start = self.now + timedelta(hours=ACTIVE_ROUTE_MAX_EARLINESS_H + 1)
        end = start + timedelta(hours=1)
        position_time = start

        with create_route_env(
            system_env_with_db,
            'test_courier_position_active_route_5',
            route_date=self.route_date.isoformat(),
            time_intervals=[start.isoformat() + '/' + end.isoformat()],
            order_locations=self.order_locations
        ) as env:

            courier_id, depot_id, route_id, order_id = self.get_eninty_ids(env)

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id, False)

            end = self.now - timedelta(hours=ACTIVE_ROUTE_MAX_LATENESS_H + 1)
            start = end - timedelta(hours=1)
            change_order_time_interval(system_env_with_db, {"id": order_id}, start.isoformat() + '/' + end.isoformat())

            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id, False)

    def test_route_with_no_orders(self, system_env_with_db):
        """
        Make sure that position is recorded and available in /courier-position if:
        1. route contains no orders
        2. route_start and route_finish are defined
        3. position's time inside [route_start, route_finish] (for /push-position)
        4. current time inside [route_start, route_finish] (for /courier-position)
        """

        route_start = self.now - timedelta(hours=1)
        route_finish = self.now + timedelta(hours=1)
        position_time = self.now

        with create_route_env(
            system_env_with_db,
            'test_courier_position_no_orders',
            route_date=self.route_date.isoformat(),
            route_finish=self.get_route_start_or_route_finish(route_finish, self.route_date),
            route_start=self.get_route_start_or_route_finish(route_start, self.route_date),
            time_intervals=[],
            order_locations=[]
        ) as env:

            courier_id, depot_id, route_id = env['courier']['id'], env['depot']['id'], env['route']['id']

            orders = get_orders(system_env_with_db, route_id)
            assert orders == []

            self.push_position_check_is_recorded(system_env_with_db, courier_id, route_id, position_time)
            self.get_last_courier_positions_check(system_env_with_db, depot_id, courier_id)


def _get_company_by_id(env, company_id, auth=None):
    response = env_get_request(env, f"companies/{company_id}", auth=auth)
    assert response.ok
    return response.json()


@skip_if_remote
class TestTrackingV3(object):
    shared_apikey = str(uuid4())

    def test_ok(self, system_env_with_db):
        with create_route_env(system_env_with_db, 'courier_position_v3') as route_env:
            apikey = get_company(system_env_with_db)['apikey']
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']

            resp = _push_positions_v3(system_env_with_db, courier_id, route_id, TRACK, apikey)
            assert resp.status_code == requests.codes.ok, resp.text

    def test_shared(self, system_env_with_db):
        contractor_name_1 = "Test company name 1"
        contractor_name_2 = "Test company name 2"
        with create_tmp_company(system_env_with_db, contractor_name_1, self.shared_apikey) as contractor_id_1, \
                create_tmp_company(system_env_with_db, contractor_name_2, self.shared_apikey) as contractor_id_2, \
                create_route_env(
                    system_env_with_db,
                    "courier_position_v3",
                    order_locations=[{"lat": 55.733827, "lon": 37.588722}]
                ) as route_env:
            route_id = route_env['route']['id']
            order_id = route_env["orders"][0]["id"]

            courier = create_courier(system_env_with_db, 'test_courier_')
            patch_data = {"shared_with_company_ids": [contractor_id_1]}
            patch_order_by_order_id(system_env_with_db, order_id, patch_data)
            patch_route(system_env_with_db, route_id, {'courier_id': courier['id']})

            apikey_1 = _get_company_by_id(system_env_with_db,
                                          contractor_id_1,
                                          auth=system_env_with_db.auth_header_super)['apikey']
            apikey_2 = _get_company_by_id(system_env_with_db,
                                          contractor_id_2,
                                          auth=system_env_with_db.auth_header_super)['apikey']

            resp = _push_positions_v3(system_env_with_db, courier['id'], route_id, TRACK, apikey_1)
            assert resp.status_code == requests.codes.ok, resp.text
            resp = _push_positions_v3(system_env_with_db, courier['id'], route_id, TRACK, apikey_2)
            assert resp.status_code == requests.codes.ok, resp.text

    def test_invalid_apikey(self, system_env_with_db):
        with create_route_env(system_env_with_db, 'courier_position_v3') as route_env:
            apikey = 'invalid_apikey'
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']

            resp = _push_positions_v3(system_env_with_db, courier_id, route_id, TRACK, apikey)
            assert resp.status_code == requests.codes.forbidden, resp.text

    @pytest.mark.parametrize('invalid_data', ['invalid_data', {'accuracy': 10, 'latitude': 48, 'longitude': 40, 'time': 1602270000}])
    def test_invalid_input(self, system_env_with_db, invalid_data):
        with create_route_env(system_env_with_db, f'courier_position_v3-{invalid_data == "invalid_data"}') as route_env:
            apikey = get_company(system_env_with_db)['apikey']
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']

            resp = request(
                method='post',
                url=f"{system_env_with_db.url}/api/v1/couriers/{courier_id}/routes/{route_id}/push-positions-v3?apikey={apikey}",
                data=json.dumps({'invalid_field': invalid_data}),
            )
            assert resp.status_code == requests.codes.unprocessable, resp.text
