from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request, env_patch_request,
    api_path_with_company_id, cleanup_state, create_courier, create_orders,
    create_route, create_depot, query_routed_orders, push_positions,
    create_tracking_tokens, get_tracking_info, query_track, get_order_details)
from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import (
    get_isoformat_str,
    get_unix_timestamp
)
import time
import datetime
import dateutil.tz
import iso8601

TEST_ID = "test_tracking_workflow_"

COURIER_NUMBER = TEST_ID + "test_courier"
DEPOT_NUMBER = TEST_ID + "test_depot"
ROUTE_NUMBER = TEST_ID + "test_route"
ORDERS_PREFIX = TEST_ID + "order"

_TRACK = [
    (55.736294, 37.582708),
    (55.735834, 37.584918),
    (55.734696, 37.584853),
    (55.733861, 37.585733)
]


def _get_routed_orders(courier_id, route_id, system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}'.format(
            courier_id, route_id,
            55.754096, 37.731182,
            '8:00'
        )
    )
    assert response.ok, response.text
    return response.json()


def _patch_order(order_id, data, system_env_with_db):
    response = env_patch_request(
        system_env_with_db,
        path=api_path_with_company_id(system_env_with_db, "orders", order_id),
        data=data
    )
    assert response.ok, response.text


class TestTrackingWorkflow(object):
    def test_workflow(self, system_env_with_db):
        test_time_zone_str = 'Asia/Vladivostok'
        test_time_zone = dateutil.tz.gettz(test_time_zone_str)
        test_datetime = datetime.datetime.now(test_time_zone)
        cleanup_state(system_env_with_db, COURIER_NUMBER, ROUTE_NUMBER, DEPOT_NUMBER)
        try:
            courier = create_courier(system_env_with_db, COURIER_NUMBER)
            courier_id = courier["id"]

            depot = create_depot(system_env_with_db, DEPOT_NUMBER, custom_data={'time_zone': test_time_zone_str})
            depot_id = depot["id"]

            route = create_route(system_env_with_db, ROUTE_NUMBER, courier_id, depot_id,
                                 test_datetime.date().isoformat())
            route_id = route["id"]

            orders = create_orders(system_env_with_db, ORDERS_PREFIX, route_id)

            tokens = create_tracking_tokens(system_env_with_db, courier_id, route_id, orders)

            # Tracks should work before any positions are send
            track = query_track(tokens[0], system_env_with_db)
            assert track["positions_type"] == "rough"
            # Some positions can be in the track from previous run of the test
            # assert len(track["positions"]) == 0
            assert "raw_positions" not in track

            track = query_track(tokens[1], system_env_with_db)
            assert track["positions_type"] == "rough"
            # Some positions can be in the track from previous run of the test
            # assert len(track["positions"]) == 0
            assert "raw_positions" not in track

            push_timestamp = time.time()
            track = [(x[0], x[1], push_timestamp + (i - len(_TRACK)) * 30) for i, x in enumerate(_TRACK)]
            push_positions(system_env_with_db, courier_id, route_id, track=track)

            def equal_tracks(track, positions, eps=0.0001):
                assert len(track) == len(positions)
                assert all([abs(t[0] - p['latitude']) < eps and abs(t[1] - p['longitude']) < eps
                            for t, p in zip(track, positions)]), f"{track=}, {positions=}, {eps=}"

            def positions_near_track(track, positions, eps=0.5):
                for p in positions:
                    assert any([abs(t[0] - p['latitude']) < eps and abs(t[1] - p['longitude']) < eps
                                for t in track]), f"point={p} is not on the {track=}"

            # Check that we can query track before routing was done
            track = query_track(tokens[1], system_env_with_db)
            assert track["positions_type"] == "rough"
            assert len(track["positions"]) == 1
            assert "time" in track["positions"][0]
            assert "raw_positions" not in track
            positions_near_track(_TRACK, track["positions"])
            assert track['eta_iso'] is None  # has not been computed yet.

            def trigger_eta_update():
                return query_routed_orders(
                    system_env_with_db, courier_id, route_id,
                    point={
                        'lat': _TRACK[-1][0],
                        'lon': _TRACK[-1][1],
                        'timestamp': datetime.datetime.now(test_time_zone).timestamp()
                    })
            routed_orders = trigger_eta_update()

            track = query_track(tokens[1], system_env_with_db)

            # Check ETA fields
            assert 'eta_iso' in track
            eta_timestamp = get_unix_timestamp(track['eta_iso'])
            assert push_timestamp < eta_timestamp, track['eta_iso']
            assert track['eta_iso'] == get_isoformat_str(eta_timestamp, test_time_zone)

            tracking = get_tracking_info(tokens[1], system_env_with_db)
            order_eta = iso8601.parse_date(tracking['order']['arrival_time'])
            assert order_eta == iso8601.parse_date(track['eta_iso'])
            assert test_datetime < order_eta and order_eta < test_datetime + datetime.timedelta(days=1)
            assert test_datetime.astimezone(order_eta.tzinfo).isoformat() == test_datetime.isoformat()

            order_details = get_order_details(system_env_with_db, orders[1]['number'])
            assert order_eta == iso8601.parse_date(order_details['arrival_time'])

            company_data = tracking['company']
            assert len(company_data) == 4
            assert company_data.keys() == {'id', 'name', 'logo_url', 'bg_color'}

            # Find the index in initial order list which correspond to the
            # second order in routed orders list
            second_routed_order = 1 \
                if orders[0]["number"] == routed_orders["route"][0]["number"] \
                else 0

            second_track_id = tokens[second_routed_order]
            first_track_id = tokens[1 - second_routed_order]

            track = query_track(second_track_id, system_env_with_db)

            assert track["positions_type"] == "rough"
            assert len(track["positions"]) == 1
            assert "raw_positions" not in track
            positions_near_track(_TRACK, track["positions"])

            # Deliver the first order
            first_order = routed_orders["route"][0]
            patch_data = {
                "status": "finished"
            }
            resp = env_patch_request(
                system_env_with_db,
                "couriers/{}/routes/{}/orders/{}".format(
                    courier["id"],
                    route["id"],
                    first_order["id"]
                ),
                data=patch_data
            )
            assert resp.ok, resp.text

            # We have to query routed orders again to update statuses
            track = query_track(second_track_id, system_env_with_db)
            assert track["positions_type"] == "rough"
            assert len(track["positions"]) == 1
            assert "raw_positions" not in track
            positions_near_track(_TRACK, track["positions"])
            trigger_eta_update()

            track = query_track(second_track_id, system_env_with_db)

            assert track["positions_type"] == "matched"
            assert len(track["positions"]) > 1
            # if tests was started several times in the row, we can have more
            # positions then pushed, because we are always creating company and route
            # with the same IDs
            assert len(track["raw_positions"]) > 1
            equal_tracks(_TRACK, track["raw_positions"], 0.0001)
            positions_near_track(_TRACK, track["positions"], 0.01)

            tracking = get_tracking_info(first_track_id, system_env_with_db)
            assert tracking['order']['arrival_time_s'] is None
            assert tracking['order']['arrival_time'] is None

        finally:
            cleanup_state(system_env_with_db, COURIER_NUMBER, ROUTE_NUMBER, DEPOT_NUMBER)

    def test_dropped_location(self, system_env_with_db):
        cleanup_state(system_env_with_db, COURIER_NUMBER, ROUTE_NUMBER, DEPOT_NUMBER)
        try:
            courier = create_courier(system_env_with_db, COURIER_NUMBER)
            courier_id = courier["id"]

            depot = create_depot(system_env_with_db, DEPOT_NUMBER)
            depot_id = depot["id"]

            route = create_route(system_env_with_db, ROUTE_NUMBER, courier_id, depot_id)
            route_id = route["id"]

            orders = create_orders(system_env_with_db, ORDERS_PREFIX, route_id)
            order_id = orders[0]["id"]

            tokens = create_tracking_tokens(system_env_with_db, courier_id, route_id, orders)
            track_id = tokens[0]

            _get_routed_orders(courier_id, route_id, system_env_with_db)

            tracking = get_tracking_info(track_id, system_env_with_db)
            assert not tracking['order'].get('dropped')
            assert not tracking['order'].get('failed_time_window')

            _patch_order(order_id, {'status': 'confirmed', 'time_interval': '01:00 - 02:01'}, system_env_with_db)

            _get_routed_orders(courier_id, route_id, system_env_with_db)

            tracking = get_tracking_info(track_id, system_env_with_db)
            assert tracking['order'].get('dropped') or tracking['order'].get('failed_time_window')

        finally:
            cleanup_state(system_env_with_db, COURIER_NUMBER, ROUTE_NUMBER, DEPOT_NUMBER)

    def test_failed_time_window(self, system_env_with_db):
        cleanup_state(system_env_with_db, COURIER_NUMBER, ROUTE_NUMBER, DEPOT_NUMBER)
        try:
            courier = create_courier(system_env_with_db, COURIER_NUMBER)
            courier_id = courier["id"]

            depot = create_depot(system_env_with_db, DEPOT_NUMBER)
            depot_id = depot["id"]

            route = create_route(system_env_with_db, ROUTE_NUMBER, courier_id, depot_id)
            route_id = route["id"]

            orders = create_orders(system_env_with_db, ORDERS_PREFIX, route_id)
            order_0 = orders[0]["id"]
            order_1 = orders[1]["id"]

            tokens = create_tracking_tokens(system_env_with_db, courier_id, route_id, orders)
            track_id = tokens[1]

            _get_routed_orders(courier_id, route_id, system_env_with_db)

            tracking = get_tracking_info(track_id, system_env_with_db)
            assert tracking['order'].get('failed_time_window') is None

            _patch_order(order_0, {'status': 'confirmed'}, system_env_with_db)
            _patch_order(order_1, {'time_interval': '01:00 - 02:00'}, system_env_with_db)

            _get_routed_orders(courier_id, route_id, system_env_with_db)

            tracking = get_tracking_info(track_id, system_env_with_db)
            assert not tracking['order'].get('dropped')
            assert tracking['order'].get('failed_time_window') is not None

        finally:
            cleanup_state(system_env_with_db, COURIER_NUMBER, ROUTE_NUMBER, DEPOT_NUMBER)
