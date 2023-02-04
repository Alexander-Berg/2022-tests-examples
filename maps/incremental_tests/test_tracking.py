from datetime import datetime, timezone, timedelta
from dateutil import parser
import dateutil.tz
import pytest
import requests

from maps.b2bgeo.ya_courier.backend.test_lib import util
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_post_request, env_get_request,
    api_path_with_company_id, push_imei_positions,
    request_tracker_status, remove_microseconds, push_positions,
    get_position_shifted_east, create_route_env, confirm_order, get_courier_position_list,
    patch_route, patch_company
)
from ya_courier_backend.models.route import ACTIVE_ROUTE_MAX_EARLINESS_H, ACTIVE_ROUTE_MAX_LATENESS_H
from ya_courier_backend.models.order import OrderStatus
from maps.b2bgeo.libs.py_flask_utils.format_util import parse_interval_sec
from ya_courier_backend.util.tracking import ZERO_POSITION_EPSILON


class TestTracking(object):

    def test_tracker_status(self, system_env_with_db):

        track = [
            (55.736294, 37.582708),
            (55.735834, 37.584918)
        ]
        pos_timeshift_ms = 1000
        timeshift = timedelta(milliseconds=pos_timeshift_ms * (len(track) - 1))

        # Querying non-existing IMEI should return 404
        #
        invalid_imei = 99999999999999999

        response = request_tracker_status(system_env_with_db, [invalid_imei])
        assert response.status_code == requests.codes.not_found

        invalid_imei_str = "99999999999999999"

        response = request_tracker_status(system_env_with_db, [invalid_imei_str])
        assert response.status_code == requests.codes.not_found

        # Querying non-array IMEI should trigger 422
        #
        invalid_imei = 99999999999999999

        response = request_tracker_status(system_env_with_db, invalid_imei)
        assert response.status_code == requests.codes.unprocessable_entity

        # Querying array of IMEI in wrong format should trigger 422
        #
        invalid_imei = "not_an_imei"

        response = request_tracker_status(system_env_with_db, [invalid_imei])
        assert response.status_code == requests.codes.unprocessable_entity

        # Push positions by IMEI positions and check
        # if latest position is returned.
        #
        # During conversions and storage we somehow lose microseconds accuracy,
        # particularly 6-th decimal place.
        #
        # To make this test accurate we will also remove microseconds from timestamp.
        #
        imei1 = 33333333333333333
        start_datetime1 = remove_microseconds(datetime.now(timezone.utc))
        push_imei_positions(system_env_with_db, imei1, start_datetime1,
                            track=track, pos_timeshift_ms=pos_timeshift_ms)

        # For testing specify IMEI in text form this time
        response = request_tracker_status(system_env_with_db, [str(imei1)])
        assert response.status_code == requests.codes.ok
        assert response.json()[0]["imei"] == imei1
        assert response.json()[0]["imei_str"] == str(imei1)
        assert parser.parse(response.json()[0]["time"]) - timeshift == start_datetime1

        # Push positions by IMEI twice for same IMEI number and check
        # if latest position is returned (covers upsert)
        #
        imei2 = 44444444444444444
        start_datetime2 = remove_microseconds(datetime.now(timezone.utc))
        push_imei_positions(system_env_with_db, imei2, start_datetime2,
                            track=track, pos_timeshift_ms=pos_timeshift_ms)

        imei3 = 44444444444444444
        start_datetime3 = remove_microseconds(datetime.now(timezone.utc)) \
                            + timedelta(days=5)
        push_imei_positions(system_env_with_db, imei3, start_datetime3,
                            track=track, pos_timeshift_ms=pos_timeshift_ms)

        response = request_tracker_status(system_env_with_db, [imei2])
        assert response.status_code == requests.codes.ok
        assert response.json()[0]["imei"] == imei2

        assert parser.parse(response.json()[0]["time"]) - timeshift \
                == start_datetime3

        # Check querying for multiple IMEI's
        #
        imei_values = [imei1, imei2]

        response = request_tracker_status(system_env_with_db, imei_values)
        assert response.status_code == requests.codes.ok
        assert len(response.json()) == len(imei_values)
        assert sorted([row["imei"] for row in response.json()]) == imei_values

    def test_zero_imei(self, system_env_with_db):
        zero_imei = 0
        push_imei_positions(system_env_with_db, zero_imei, datetime.now(timezone.utc), track=[(55.7, 35.7)])

    def test_filtering_lat_lon(self, system_env_with_db):
        imei = 33333333333333333
        send_datetime = remove_microseconds(datetime.now(timezone.utc))
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
                [(-90, 180), True]]:
            push_imei_positions(system_env_with_db, imei, send_datetime, track=[pos])
            response = request_tracker_status(system_env_with_db, [imei])
            assert response.status_code == requests.codes.ok
            assert response.json()[0]["imei"] == imei
            if expect_stored:
                assert parser.parse(response.json()[0]["time"]) == send_datetime
            else:
                assert parser.parse(response.json()[0]["time"]) < send_datetime
            send_datetime += timedelta(milliseconds=10)

    def test_forbidden_access(self, system_env_with_db):
        imei = 33333333333333333
        response = env_post_request(
            system_env_with_db,
            "gps-trackers/{}/push-positions".format(imei),
            data={
                "positions": {
                    "latitude": 22.2,
                    "longitude": 33.3,
                    "time": (datetime.now(timezone.utc)).isoformat()
                }
            }
        )
        assert response.status_code == requests.codes.forbidden

    def test_push_positions(self, system_env_with_db):
        with create_route_env(
            system_env_with_db,
            "courier_position",
            imei=100000
        ) as route_env:
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']
            imei = route_env['route']['imei']

            positions = [
                {
                    "latitude": 22.2,
                    "longitude": 33.3,
                    "time": (datetime.now(timezone.utc)).isoformat()
                }
            ]
            response = env_post_request(
                system_env_with_db,
                "gps-trackers/{}/push-positions".format(imei),
                data={"positions": positions},
                auth=system_env_with_db.auth_header_super
            )
            assert response.status_code == requests.codes.ok
            resp = get_courier_position_list(system_env_with_db, courier_id, route_id)
            assert resp[0]['imei'] == imei

    def test_push_positions_with_zeroes(self, system_env_with_db):
        with create_route_env(
            system_env_with_db,
            "courier_position",
            imei=100000
        ) as route_env:
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']
            imei = route_env['route']['imei']

            positions = [
                {
                    "latitude": 0,
                    "longitude": 33.3,
                    "time": (datetime.now(timezone.utc)).isoformat()
                },
                {
                    "latitude": 22.2,
                    "longitude": 0,
                    "time": (datetime.now(timezone.utc)).isoformat()
                }
            ]
            response = env_post_request(
                system_env_with_db,
                "gps-trackers/{}/push-positions".format(imei),
                data={"positions": positions},
                auth=system_env_with_db.auth_header_super
            )
            assert response.status_code == requests.codes.ok
            resp = get_courier_position_list(system_env_with_db, courier_id, route_id)
            assert resp[0]['imei'] == imei

    def test_push_positions_no_orders(self, system_env_with_db):
        now = datetime.now(timezone.utc)
        with create_route_env(
            system_env_with_db,
            "courier_position",
            imei=200000,
            route_date=now.date().isoformat(),
            route_start="00:00:00",
            route_finish="1.10:00:00",
            time_intervals=[],
            order_locations=[]
        ) as route_env:
            courier_id = route_env['courier']['id']
            route_id = route_env['route']['id']
            imei = route_env['route']['imei']

            positions = [
                {
                    "latitude": 22.2,
                    "longitude": 33.3,
                    "time": now.isoformat()
                }
            ]
            response = env_post_request(
                system_env_with_db,
                "gps-trackers/{}/push-positions".format(imei),
                data={"positions": positions},
                auth=system_env_with_db.auth_header_super
            )
            assert response.status_code == requests.codes.ok
            resp = get_courier_position_list(system_env_with_db, courier_id, route_id)
            assert len(resp) == 1
            assert resp[0]['imei'] == imei

    def test_wrong_tracking(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            path='tracking/non-existing-track-id')
        assert response.status_code == requests.codes.gone


def _check_time_window(time_window, start_date, time_interval_str, time_zone_str):
    interval = parse_interval_sec(time_interval_str, start_date, time_zone_str)
    start_datetime = datetime(
        start_date.year, start_date.month, start_date.day, tzinfo=dateutil.tz.gettz(time_zone_str))
    for i, name in enumerate(['start', 'end']):
        assert time_window[name] == (start_datetime + timedelta(seconds=interval[i])).isoformat()


def test_tracking_time_window(system_env_with_db):
    ROUTE_DATE = datetime.now(tz=dateutil.tz.gettz('Europe/Moscow')).date()
    with util.create_route_env(
            system_env_with_db,
            'test_tracking_time_window',
            order_locations=[{"lat": 55.733827, "lon": 37.588722}],
            time_intervals=['08:00-23.00'],
            depot_data={"mark_route_started_radius": 0},
            route_date=ROUTE_DATE.isoformat()) as env:

        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        current_location = {
            'lat': depot['lat'],
            'lon': depot['lon'],
            'time_now': '08:01'
        }
        util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)

        order_id = env['orders'][0]['id']
        track = util.get_order_track_id(system_env_with_db, order_id)

        order_data = util.get_tracking_info(track, system_env_with_db)['order']

        assert 'time_interval' in order_data
        assert 'time_window' in order_data
        _check_time_window(order_data['time_window'], ROUTE_DATE, order_data['time_interval'], depot['time_zone'])


@pytest.mark.parametrize("obeys_route_sequence", [False, True])
def test_tracking_route_sequence_compliance(system_env_with_db, obeys_route_sequence):
    ROUTE_DATETIME = datetime.now()
    order_locations = [{"lat": 55.733827, "lon": 37.588722}]
    for i in range(2):
        lat, lon = get_position_shifted_east(order_locations[-1]['lat'], order_locations[-1]['lon'], 450)
        order_locations.append({'lat': lat, 'lon': lon})

    with create_route_env(
            system_env_with_db,
            'test_tracking_route_sequence_compliance_ok',
            order_locations=order_locations,
            time_intervals=['08:00-23.00'] * len(order_locations),
            route_date=ROUTE_DATETIME.date().isoformat()) as env:
        route_id = env['route']['id']
        courier_id = env['courier']['id']

        route_info = get_route(system_env_with_db, route_id)
        assert not route_info['courier_violated_route']

        track = []
        actual_visit_sequence = [1, 0, 2] if obeys_route_sequence else [2, 1, 0]
        for idx, order_idx in enumerate(actual_visit_sequence):
            track.extend([
                (
                    order_locations[order_idx]['lat'], order_locations[order_idx]['lon'],
                    ROUTE_DATETIME.replace(hour=9 + idx).timestamp()
                ),
                (
                    order_locations[order_idx]['lat'], order_locations[order_idx]['lon'],
                    ROUTE_DATETIME.replace(hour=9 + idx, minute=15).timestamp()
                ),
            ])
        push_positions(system_env_with_db, courier_id, route_id, track=track)
        route_info = get_route(system_env_with_db, route_id)
        assert not route_info['courier_violated_route'] == obeys_route_sequence


def get_route(env, route_id):
    response = env_get_request(env, '{}/routes/{}'.format(api_path_with_company_id(env), route_id))
    return response.json()


# https://st.yandex-team.ru/BBGEO-3687
def test_tracking_order_status(system_env_with_db):
    ROUTE_DATE = datetime.now(tz=dateutil.tz.gettz('Europe/Moscow')).date()
    with util.create_route_env(
            system_env_with_db,
            'test_tracking_order_status',
            order_locations=[{"lat": 55.733827, "lon": 37.588722}],
            time_intervals=['08:00-23.00'],
            depot_data={"mark_route_started_radius": 0},
            route_date=ROUTE_DATE.isoformat()) as env:

        depot = env['depot']
        route_id = env['route']['id']
        courier_id = env['courier']['id']
        current_location = {
            'lat': depot['lat'],
            'lon': depot['lon'],
            'time_now': '08:01'
        }
        util.query_routed_orders(system_env_with_db, courier_id, route_id, current_location)

        order_id = env['orders'][0]['id']
        track = util.get_order_track_id(system_env_with_db, order_id)
        assert util.get_tracking_info(track, system_env_with_db)['order']["status"] == OrderStatus.new.value
        confirm_order(system_env_with_db, env['orders'][0])
        assert util.get_tracking_info(track, system_env_with_db)['order']["status"] == OrderStatus.confirmed.value


@pytest.mark.parametrize("push_positions_version", [1, 2])
def test_filter_positions(system_env_with_db, push_positions_version):
    """
    Position is recorded if:
            - position's time is greater than route_start_s if specified otherwise than min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H
            - position's time is less than route_finish_s if specified otherwise than max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H
    """
    patch_company(system_env_with_db, {'tracking_start_h': 0})
    now = datetime.now(tz=dateutil.tz.gettz('Europe/Moscow'))
    start = now + timedelta(hours=1)
    end = now + timedelta(hours=3)
    # https://st.yandex-team.ru/BBGEO-5172
    # move route_date 1 day to ensure that all timestamps are before route_date
    route_date = (now - timedelta(days=1)).date()

    order_locations = [{"lat": 55.733827, "lon": 37.588722}]

    with create_route_env(
            system_env_with_db,
            'test_filter_positions',
            route_date=route_date.isoformat(),
            time_intervals=[start.isoformat() + '/' + end.isoformat()],
            order_locations=order_locations) as env:
        courier_id = env['courier']['id']
        route_id = env['route']['id']

        # Make sure that position is not recorded when:
        #    position's time is outside of [min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H, max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H]
        position_time = start - timedelta(hours=ACTIVE_ROUTE_MAX_EARLINESS_H + 1)
        track = [(21, 33.3, position_time.timestamp())]
        push_positions(system_env_with_db, courier_id, route_id, track, version=push_positions_version)

        response = get_courier_position_list(system_env_with_db, courier_id, route_id)
        assert len(response) == 0

        position_time = end + timedelta(hours=ACTIVE_ROUTE_MAX_LATENESS_H + 1)
        track = [(21, 33.3, position_time.timestamp())]
        push_positions(system_env_with_db, courier_id, route_id, track, version=push_positions_version)

        response = get_courier_position_list(system_env_with_db, courier_id, route_id)
        assert len(response) == 0

        # Make sure that position is recorded when:
        #    position's time is inside of [min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H, max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H]
        position_time = start - timedelta(hours=ACTIVE_ROUTE_MAX_EARLINESS_H - 1)
        track = [(22, 33.3, position_time.timestamp())]
        push_positions(system_env_with_db, courier_id, route_id, track, version=push_positions_version)

        response = get_courier_position_list(system_env_with_db, courier_id, route_id)
        assert len(response) == 1
        assert [x for x in response if x['lat'] == 22]

        position_time = end + timedelta(hours=ACTIVE_ROUTE_MAX_LATENESS_H - 1)
        track = [(23, 33.3, position_time.timestamp())]
        push_positions(system_env_with_db, courier_id, route_id, track, version=push_positions_version)

        response = get_courier_position_list(system_env_with_db, courier_id, route_id)
        assert len(response) == 2
        assert [x for x in response if x['lat'] == 23]

        # Make sure that only first two positions are recorded:
        #    time from the first position is inside of [route_start_s, min(TimeWindow.start) - ACTIVE_ROUTE_MAX_EARLINESS_H]
        #    time from the second position is inside of [max(TimeWindow.end) + ACTIVE_ROUTE_MAX_LATENESS_H, route_finish_s]
        #    time from the third position is less than route_start_s
        #    time from the fourth position is greater than route_finish_s
        route_start_s = start - timedelta(hours=ACTIVE_ROUTE_MAX_EARLINESS_H + 1)
        route_finish_s = end + timedelta(hours=ACTIVE_ROUTE_MAX_LATENESS_H + 1)

        def get_route_start_or_route_finish(route_time_s_datetime, route_date):
            route_datetime = datetime(route_date.year, route_date.month, route_date.day, tzinfo=dateutil.tz.gettz('Europe/Moscow'))
            diff = route_time_s_datetime - route_datetime
            hours_minutes = f"{diff.seconds//3600}:{(diff.seconds)//60%60}"
            if diff.days == 0:
                return hours_minutes
            return str(diff.days) + "." + hours_minutes

        change = {
            "route_start": get_route_start_or_route_finish(route_start_s, route_date),
            "route_finish": get_route_start_or_route_finish(route_finish_s, route_date)
        }
        patch_route(system_env_with_db, route_id, change)

        position_time = route_start_s + (start - route_start_s)/2
        track = [(25, 33.3, position_time.timestamp())]
        position_time = end + (route_finish_s - end)/2
        track.append((26, 33.3, position_time.timestamp()))
        position_time = route_start_s - timedelta(hours=1)
        track.append((27, 33.3, position_time.timestamp()))
        position_time = route_finish_s + timedelta(hours=1)
        track.append((28, 33.3, position_time.timestamp()))

        push_positions(system_env_with_db, courier_id, route_id, track, version=push_positions_version)
        response = get_courier_position_list(system_env_with_db, courier_id, route_id)

        assert len([x for x in response if x['lat'] == 25 or x['lat'] == 26]) == 2, f"response {response}"
        assert not [x for x in response if x['lat'] == 27 or x['lat'] == 28], f"response {response}"
