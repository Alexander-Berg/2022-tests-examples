import datetime
import requests
import dateutil.tz
import time
import json
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id, push_positions, push_imei_positions, batch_orders,
    query_routed_orders, env_get_request, env_patch_request,
    create_route_env, create_tmp_company,
    get_order, patch_order, set_mark_delivered_enabled, get_orders
)

from ya_courier_backend.models import OrderStatus, OrderHistoryEvent
from ya_courier_backend.models.order import (
    has_order_history_event, get_order_history_event_records
)
from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str, get_unix_timestamp
import pytest
from ya_courier_backend.util.distance import distance
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


LOCATIONS = [
    {"lat": 55.733827, "lon": 37.588722},
    {"lat": 55.729299, "lon": 37.580116}
]

LOCATION_490M_FROM_ORDER = {"lat": 55.73823, "lon": 37.588722}
LOCATION_510M_FROM_ORDER = {"lat": 55.73841, "lon": 37.588722}


def test_distance():
    assert distance((LOCATIONS[0]["lat"], LOCATIONS[0]["lon"]), (
        LOCATION_490M_FROM_ORDER["lat"], LOCATION_490M_FROM_ORDER["lon"])) == pytest.approx(490, abs=1)
    assert distance((LOCATIONS[0]["lat"], LOCATIONS[0]["lon"]), (
        LOCATION_510M_FROM_ORDER["lat"], LOCATION_510M_FROM_ORDER["lon"])) == pytest.approx(510, abs=1)


DATETIME = datetime.datetime(2018, 10, 19, tzinfo=dateutil.tz.tzutc())
ROUTE_DATE = DATETIME.date().isoformat()
SERVICE_DURATION_S = 300
TOTAL_SERVICE_DURATION_S = 600


def offset_datetime(offset_s):
    return DATETIME + datetime.timedelta(seconds=offset_s)


def push_locations(location):
    return [
        (location["lat"], location["lon"])
    ]


def check_status(system_env_with_db, status, order_id, company_id=None, auth=None):
    order = get_order(system_env_with_db, order_id, company_id, auth)
    assert order["status"] == status
    return order


PARAMS_LIST = [
    {
        "imei": 33458985560341,
        "items": [
            {
                "locations": push_locations(LOCATIONS[0]),
                "time": offset_datetime(0),
                "status": OrderStatus.new.value,
            },
            {
                "locations": push_locations(LOCATIONS[0]),
                "time": offset_datetime(TOTAL_SERVICE_DURATION_S/2-1),
                "status": OrderStatus.new.value,
            },
            {
                "locations": push_locations(LOCATIONS[0]),
                "time": offset_datetime(TOTAL_SERVICE_DURATION_S/2),
                "status": OrderStatus.finished.value,
            }
        ]
    },
    {
        "imei": 22346985560342,
        "items": [
            {
                "locations": push_locations(LOCATION_490M_FROM_ORDER),
                "time": offset_datetime(0),
                "status": OrderStatus.new.value,
            },
            {
                "locations": push_locations(LOCATION_490M_FROM_ORDER),
                "time": offset_datetime(TOTAL_SERVICE_DURATION_S/2-1),
                "status": OrderStatus.new.value,
            },
            {
                "locations": push_locations(LOCATION_490M_FROM_ORDER),
                "time": offset_datetime(TOTAL_SERVICE_DURATION_S/2),
                "status": OrderStatus.finished.value,
            }
        ]
    },
    {
        "imei": 13416515560343,
        "items": [
            {
                "locations": push_locations(LOCATION_510M_FROM_ORDER),
                "time": offset_datetime(0),
                "status": OrderStatus.new.value,
            },
            {
                "locations": push_locations(LOCATION_510M_FROM_ORDER),
                "time": offset_datetime(TOTAL_SERVICE_DURATION_S/2-1),
                "status": OrderStatus.new.value,
            },
            {
                "locations": push_locations(LOCATION_510M_FROM_ORDER),
                "time": offset_datetime(TOTAL_SERVICE_DURATION_S/2),
                "status": OrderStatus.new.value,
            }
        ]
    }
]


@skip_if_remote
@pytest.mark.parametrize("params", PARAMS_LIST)
@pytest.mark.parametrize("push_method", ["push_positions_v1", "push_positions_v2", "push_imei_positions"])
@pytest.mark.parametrize("mark_delivered_enabled", [True, False])
@pytest.mark.parametrize("service_duration_s", [0, 100, 300, 600])
def test_mark_delivered(system_env_with_db, params, push_method, mark_delivered_enabled, service_duration_s):
    assert service_duration_s <= TOTAL_SERVICE_DURATION_S
    with create_tmp_company(system_env_with_db, "Test company test_mark_delivered") as company_id:
        auth = system_env_with_db.auth_header_super
        set_mark_delivered_enabled(system_env_with_db, mark_delivered_enabled, company_id)
        with create_route_env(
                system_env_with_db,
                f"test_mark_delivered-{params['imei'] % 10}-{push_method[-1]}-{mark_delivered_enabled}-{service_duration_s}",
                route_date=params["items"][0]["time"].date().isoformat(),
                order_locations=LOCATIONS,
                imei=params["imei"] if push_method == "push_imei_positions" else None,
                company_id=company_id,
                auth=auth) as env:
            batch_orders(system_env_with_db,
                         [{"number": order['number'],
                           "shared_service_duration_s": TOTAL_SERVICE_DURATION_S - service_duration_s,
                           "service_duration_s": service_duration_s} for order in env['orders']],
                          company_id=company_id,
                          auth=auth)
            for item in params["items"]:
                print(item)
                if push_method == "push_imei_positions":
                    push_imei_positions(
                        system_env_with_db,
                        params["imei"],
                        item["time"],
                        item["locations"])
                else:
                    push_positions(
                        system_env_with_db,
                        env["courier"]["id"],
                        env["route"]["id"],
                        track=[(x[0], x[1], item["time"].timestamp()) for x in item["locations"]],
                        auth=auth,
                        version={"push_positions_v1": 1, "push_positions_v2": 2}[push_method])
                check_status(
                    system_env_with_db,
                    item["status"] if mark_delivered_enabled else OrderStatus.new.value,
                    env["orders"][0]['id'],
                    company_id=company_id, auth=auth)


def test_order_fields(system_env_with_db):
    with create_route_env(system_env_with_db, "test_order_fields", route_date=ROUTE_DATE, order_locations=LOCATIONS, imei=78249668562) as env:
        print(json.dumps(env))
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], offset_datetime(0).timestamp())])
        check_status(system_env_with_db, OrderStatus.new.value, env["orders"][0]['id'])

        next_pos_ts = offset_datetime(SERVICE_DURATION_S / 2).timestamp()
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], next_pos_ts)])

        order = check_status(system_env_with_db, OrderStatus.finished.value, env["orders"][0]['id'])

        now = pytest.approx(time.time(), abs=10)
        time_zone = dateutil.tz.gettz(env['depot']['time_zone'])
        expected_times_iso = [
            get_isoformat_str(item['timestamp'], time_zone) for item in order['history']
        ]

        assert order["delivered_at"]
        assert len(order["history"]) == 4
        assert order["history"] == [
            {
                'event': "ORDER_CREATED",
                'timestamp': now,
                'time': expected_times_iso[0],
            },
            {
                'event': "ARRIVAL",
                "timestamp": now,
                'time': expected_times_iso[1],
                'position': {
                    'time': datetime.datetime.fromtimestamp(offset_datetime(0).timestamp())
                         .astimezone(time_zone).isoformat(),
                    'lat': LOCATIONS[0]['lat'],
                    'lon': LOCATIONS[0]['lon']
                },
                'used_mark_delivered_radius': 500
            },
            {
                'event': "ORDER_VISIT",
                'timestamp': now,
                'time': expected_times_iso[2],
                'position': {
                    'time': datetime.datetime.fromtimestamp(next_pos_ts)
                         .astimezone(time_zone).isoformat(),
                    'lat': LOCATIONS[0]['lat'],
                    'lon': LOCATIONS[0]['lon']
                },
                'used_mark_delivered_radius': 500
            },
            {
                'event': "STATUS_UPDATE",
                'timestamp': now,
                'time': expected_times_iso[3],
                'status': "finished",
                'source': {
                    'initiator': 'yandex'
                }
            }
        ]
        assert order["status_log"] == [
            {
                'point': LOCATIONS[0],
                'status': 'finished',
                'timestamp': now
            }
        ]


def test_mark_delivered_with_app_usage(system_env_with_db):
    """
    Check that automatic delivery marking works even if the user of the app performs manual actions
    (e.g. sets an order's status to `confirmed`)
    """
    with create_route_env(
            system_env_with_db, "test_mark_delivered_with_app_usage", route_date=ROUTE_DATE,
            order_locations=LOCATIONS, imei=9988227763416) as env:
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], offset_datetime(0).timestamp())])
        check_status(system_env_with_db, OrderStatus.new.value, env["orders"][0]['id'])
        query_routed_orders(system_env_with_db, env["courier"]["id"], env["route"]["id"])

        response = env_patch_request(
            system_env_with_db,
            "couriers/{courier_id}/routes/{route_id}/orders/{order_id}".format(
                courier_id=env["courier"]["id"],
                route_id=env["route"]["id"],
                order_id=env["orders"][0]["id"]),
            data={"status": OrderStatus.confirmed.value}
        )
        assert response.status_code == requests.codes.ok

        order = get_order(system_env_with_db, env["orders"][0]['id'])
        assert order["status"] == OrderStatus.confirmed.value

        status_update_events = get_order_history_event_records(order, OrderHistoryEvent.status_update)
        assert len(status_update_events) == 1
        assert status_update_events[0]["source"] == {
            "initiator": "user_api",
            "user_role": "admin"
        }

        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], offset_datetime(SERVICE_DURATION_S/2).timestamp())])
        check_status(system_env_with_db, OrderStatus.finished.value, env["orders"][0]['id'])


def test_pass_order(system_env_with_db):
    """
    Automatically set 'delivered status' - do not mark order when courier only passed location
    https://st.yandex-team.ru/BBGEO-1292
    """
    with create_route_env(system_env_with_db, "test_pass_order", route_date=ROUTE_DATE, order_locations=LOCATIONS, imei=825731468) as env:
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], offset_datetime(0).timestamp())])
        check_status(system_env_with_db, OrderStatus.new.value, env["orders"][0]['id'])

        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            track=[(LOCATIONS[1]['lat'], LOCATIONS[1]['lon'], offset_datetime(SERVICE_DURATION_S/4).timestamp())])
        check_status(system_env_with_db, OrderStatus.new.value, env["orders"][0]['id'])

        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], offset_datetime(SERVICE_DURATION_S/2).timestamp())])
        check_status(system_env_with_db, OrderStatus.new.value, env["orders"][0]['id'])

        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], offset_datetime(SERVICE_DURATION_S).timestamp())])
        check_status(system_env_with_db, OrderStatus.finished.value, env["orders"][0]['id'])


def test_check_delivery(system_env_with_db):
    """
        A pack with multiple locations can be sent in a single push query
        Test the following workflow:
            - the first pack contains the following locations:
                1) close to the order without enough time for delivery;
                2) far from the order;
                3) again close to the order
            - the second pack contains:
                1) close to the order with enough time for delivery if summed with the previous location
                2) far from the order
        * check: After the first pack the order is not delivered, after the second it's delivered
    """
    with create_route_env(system_env_with_db, "test_check_delivery", route_date=ROUTE_DATE, order_locations=LOCATIONS, imei=191928283737) as env:
        first_locations_pack = [
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(0).timestamp()
            ),
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S / 4).timestamp()
            ),
            (
                LOCATIONS[1]["lat"],
                LOCATIONS[1]["lon"],
                offset_datetime(SERVICE_DURATION_S / 2).timestamp()
            ),
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S * 3 / 4).timestamp()
            )
        ]

        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            first_locations_pack
        )
        check_status(system_env_with_db, OrderStatus.new.value, env["orders"][0]['id'])

        second_locations_pack = [
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S).timestamp()
            ),
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S * 5 / 4).timestamp()
            ),
            (
                LOCATIONS[1]["lat"],
                LOCATIONS[1]["lon"],
                offset_datetime(SERVICE_DURATION_S * 3 / 2).timestamp()
            )
        ]

        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            second_locations_pack
        )
        check_status(system_env_with_db, OrderStatus.finished.value, env["orders"][0]['id'])


# https://st.yandex-team.ru/BBGEO-1752
def test_already_passed_order_not_delivered(system_env_with_db):
    """
        Test the following workflow:
            - a route with 1 order is created
            - 2 positions close to the order are pushed with enough time for the order to be delivered
                * check: the order is marked as delivered
            - an order is manually marked back as confirmed
            - 1 position far from the order is pushed
                * check: the order is still confirmed
            - 1 position close to the order is pushed
                * check: the order is still confirmed
            - 1 position close to the order is pushed with not enough time for the order to be delivered
                * check: the order is still confirmed
            - 1 position close to the order is pushed with enough time for the order to be delivered
                * check: the order is marked as delivered
    """
    location = LOCATIONS[0]
    close_location = location
    far_location = LOCATIONS[1]
    with create_route_env(
            system_env_with_db,
            "test_already_passed_order_not_delivered",
            route_date=ROUTE_DATE,
            order_locations=[location],
            imei=663124489291) as env:
        order = env["orders"][0]
        positions = [
            (
                close_location["lat"],
                close_location["lon"],
                offset_datetime(0).timestamp()
            ),
            (
                close_location["lat"],
                close_location["lon"],
                offset_datetime(SERVICE_DURATION_S).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            positions
        )
        check_status(system_env_with_db, OrderStatus.finished.value, order['id'])

        patch_order(system_env_with_db, order, {'status': 'confirmed'})
        positions = [
            (
                far_location["lat"],
                far_location["lon"],
                offset_datetime(2 * SERVICE_DURATION_S).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            positions
        )
        check_status(system_env_with_db, OrderStatus.confirmed.value, order['id'])

        positions = [
            (
                close_location["lat"],
                close_location["lon"],
                offset_datetime(3 * SERVICE_DURATION_S).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            positions
        )
        check_status(system_env_with_db, OrderStatus.confirmed.value, order['id'])

        positions = [
            (
                close_location["lat"],
                close_location["lon"],
                offset_datetime(3.1 * SERVICE_DURATION_S).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            positions
        )
        check_status(system_env_with_db, OrderStatus.confirmed.value, order['id'])

        positions = [
            (
                close_location["lat"],
                close_location["lon"],
                offset_datetime(4 * SERVICE_DURATION_S).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            positions
        )
        check_status(system_env_with_db, OrderStatus.finished.value, order['id'])


def test_routes_dont_influence_each_other(system_env_with_db):
    """
        Test the following workflow:
            - 2 routes with 1 order each (the same location) are created
            - 1 position is pushed for route 1
            - 1 position (after enough time for delivery from position of route 1) is pushed for route 2
                * check: orders in both routes are not marked as delivered
    """
    location = LOCATIONS[0]

    with create_route_env(system_env_with_db, "test_routes_dont_influence_each_other_1", route_date=ROUTE_DATE,
                          order_locations=[location], imei=62343786853) as env1:
        with create_route_env(system_env_with_db, "test_routes_dont_influence_each_other_2", route_date=ROUTE_DATE,
                              order_locations=[location], imei=66574382001) as env2:
            push_positions(
                system_env_with_db,
                env1["courier"]["id"],
                env1["route"]["id"],
                track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], offset_datetime(0).timestamp())])

            push_positions(
                system_env_with_db,
                env2["courier"]["id"],
                env2["route"]["id"],
                track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'], offset_datetime(SERVICE_DURATION_S).timestamp())])

            check_status(system_env_with_db, OrderStatus.new.value, env1["orders"][0]['id'])
            check_status(system_env_with_db, OrderStatus.new.value, env2["orders"][0]['id'])


def test_order_departure_multiple_packs_of_locations(system_env_with_db):
    """
        Test the following workflow:
        - Step 1:
            - push a pack of locations enough for marking order visited and delivered
            - check the order history:
                - ARRIVAL event with the first position in the delivery area is created
                - VISIT event with a position in the delivery area is created
                - there is no DEPARTURE event
        - Step 2:
            - push a pack of locations inside of the delivery area
            - check that the order history is the same as after the Step 1 (in the past
              this check was failing because of BBGEO-3114)
        - Step 3:
            - push a pack of locations (outside, then inside, then again outside of the delivery area)
            - check the order history:
                - ARRIVAL and VISIT events are still the same as after the Step 1
                - DEPARTURE event with the last position from the Step 2 is created
    """
    with create_route_env(system_env_with_db, "test_order_departure_multiple_packs_of_locations",
                          route_date=ROUTE_DATE, order_locations=LOCATIONS, imei=1010928374) as env:
        def _check_history_event(order, event_type, expected_location):
            events = get_order_history_event_records(order, event_type)
            assert len(events) == 1
            event = events[0]
            assert expected_location == (
                event["position"]["lat"],
                event["position"]["lon"],
                get_unix_timestamp(event["position"]["time"])
            )

        # Step 1

        first_locations_pack = [
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
            )
        ]
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            first_locations_pack
        )
        order = get_order(system_env_with_db, env["orders"][0]['id'])

        # Expected visit position: first_locations_pack[2] but with corrected time
        expected_visit_loc = (LOCATIONS[0]["lat"], LOCATIONS[0]["lon"], offset_datetime(SERVICE_DURATION_S * 0.5).timestamp())

        _check_history_event(order, OrderHistoryEvent.arrival, first_locations_pack[1])
        _check_history_event(order, OrderHistoryEvent.visit, expected_visit_loc)
        assert not has_order_history_event(order, OrderHistoryEvent.departure)

        # Step 2

        second_locations_pack = [
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S * 1.1).timestamp()
            ),
            (
                LOCATION_490M_FROM_ORDER["lat"],
                LOCATION_490M_FROM_ORDER["lon"],
                offset_datetime(SERVICE_DURATION_S * 1.2).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            second_locations_pack
        )
        order = get_order(system_env_with_db, env["orders"][0]['id'])

        _check_history_event(order, OrderHistoryEvent.arrival, first_locations_pack[1])
        _check_history_event(order, OrderHistoryEvent.visit, expected_visit_loc)
        assert not has_order_history_event(order, OrderHistoryEvent.departure)

        # Step 3

        third_locations_pack = [
            (
                LOCATION_510M_FROM_ORDER["lat"],
                LOCATION_510M_FROM_ORDER["lon"],
                offset_datetime(SERVICE_DURATION_S * 1.3).timestamp()
            ),
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S * 1.4).timestamp()
            ),
            (
                LOCATION_510M_FROM_ORDER["lat"],
                LOCATION_510M_FROM_ORDER["lon"],
                offset_datetime(SERVICE_DURATION_S * 1.5).timestamp()
            )
        ]
        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            third_locations_pack
        )
        order = get_order(system_env_with_db, env["orders"][0]['id'])

        _check_history_event(order, OrderHistoryEvent.arrival, first_locations_pack[1])
        _check_history_event(order, OrderHistoryEvent.visit, expected_visit_loc)
        _check_history_event(order, OrderHistoryEvent.departure, second_locations_pack[-1])


def test_order_departure_single_locations_pack(system_env_with_db):
    """
        Test the following workflow:
        - push single pack of locations:
          a location outside of the delivery area,
          locations enough for marking order visited and delivered,
          a location outside of the delivery area
        * check: one ARRIVAL event with the first position in the delivery area and
          one DEPARTURE event with the last position in the delivery area are created
    """
    with create_route_env(system_env_with_db, "test_order_departure_single_locations_pack",
                          route_date=ROUTE_DATE, order_locations=LOCATIONS, imei=88880102023) as env:
        locations_pack = [
            (
                LOCATION_510M_FROM_ORDER["lat"],
                LOCATION_510M_FROM_ORDER["lon"],
                offset_datetime(0).timestamp()
            ),
            (
                LOCATIONS[0]["lat"],
                LOCATIONS[0]["lon"],
                offset_datetime(SERVICE_DURATION_S / 2).timestamp()
            ),
            (
                LOCATION_490M_FROM_ORDER["lat"],
                LOCATION_490M_FROM_ORDER["lon"],
                offset_datetime(SERVICE_DURATION_S).timestamp()
            ),
            (
                LOCATION_510M_FROM_ORDER["lat"],
                LOCATION_510M_FROM_ORDER["lon"],
                offset_datetime(SERVICE_DURATION_S * 3 / 2).timestamp()
            )
        ]

        push_positions(
            system_env_with_db,
            env["courier"]["id"],
            env["route"]["id"],
            locations_pack
        )

        order = get_order(system_env_with_db, env["orders"][0]['id'])

        arrival_events = get_order_history_event_records(order, OrderHistoryEvent.arrival)
        assert len(arrival_events) == 1
        arrival_event = arrival_events[0]
        arrival_position = (
            arrival_event["position"]["lat"],
            arrival_event["position"]["lon"],
            get_unix_timestamp(arrival_event["position"]["time"])
        )
        assert arrival_position == locations_pack[1]

        departure_events = get_order_history_event_records(order, OrderHistoryEvent.departure)
        assert len(departure_events) == 1
        departure_event = departure_events[0]
        departure_position = (
            departure_event["position"]["lat"],
            departure_event["position"]["lon"],
            get_unix_timestamp(departure_event["position"]["time"])
        )
        assert departure_position == locations_pack[2]


@skip_if_remote()
@pytest.mark.parametrize('imei', [24455768724, None])
def test_filtering_out_forbidden_positions(system_env_with_db, imei):
    set_mark_delivered_enabled(system_env_with_db, True)
    forbidden_points = [
        {'lat': 55.971, 'lon': 37.41},
        {'lat': 55.599, 'lon': 37.2693},
        {'lat': 55.407, 'lon': 37.91}
    ]
    with create_route_env(
            system_env_with_db, f'filtering_out_forbidden_positions-{imei}',
            route_date=offset_datetime(0).date().isoformat(),
            order_locations=LOCATIONS, imei=imei) as env:

        def _push_positions(start_datetime, positions):
            if imei:
                push_imei_positions(
                    system_env_with_db, imei, start_datetime=start_datetime, track=positions, pos_timeshift_ms=1000)
            else:
                track = [
                    (pos[0], pos[1], (start_datetime + datetime.timedelta(seconds=i)).timestamp())
                    for i, pos in enumerate(positions)
                ]
                push_positions(system_env_with_db, env["courier"]["id"], env["route"]["id"], track=track)

        _push_positions(start_datetime=offset_datetime(0),
                        positions=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon']),
                                   (LOCATIONS[0]['lat'] + 1e-5, LOCATIONS[0]['lon'] + 1e-4),
                                   (forbidden_points[0]['lat'], forbidden_points[0]['lon'])])

        _push_positions(start_datetime=offset_datetime(15),
                        positions=[(forbidden_points[1]['lat'], forbidden_points[1]['lon'])])

        check_status(system_env_with_db, OrderStatus.new.value, env["orders"][0]['id'])

        _push_positions(start_datetime=offset_datetime(SERVICE_DURATION_S / 2),
                        positions=[(forbidden_points[2]['lat'], forbidden_points[2]['lon']),
                                   (LOCATIONS[0]['lat'], LOCATIONS[0]['lon']),
                                   (forbidden_points[2]['lat'], forbidden_points[2]['lon'])])

        check_status(system_env_with_db, OrderStatus.finished.value, env["orders"][0]['id'])


def test_multi_order(system_env_with_db):
    """
        Test the following workflow:
        - create 4 orders
        - spend (SERVICE_DURATION_S / 2) seconds in order location
        - check that all orders are marked as delivered
    """
    order_count = 4
    imei = 7353999881034
    with create_route_env(
            system_env_with_db, "test_multi_order",
            route_date=ROUTE_DATE, order_locations=[LOCATIONS[0]] * order_count, imei=imei) as env:
        push_imei_positions(
            system_env_with_db,
            imei,
            start_datetime=offset_datetime(0),
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'])])
        push_imei_positions(
            system_env_with_db,
            imei,
            start_datetime=offset_datetime(SERVICE_DURATION_S / 2),
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'])])
        for i in range(order_count):
            check_status(
                system_env_with_db,
                OrderStatus.finished.value,
                env["orders"][i]['id'])


def test_arrival_visited_departure_for_delivered_orders(system_env_with_db):
    """
        Test the following workflow:
        - create 4 orders with finished status
        - spend (SERVICE_DURATION_S / 2) seconds in orders location
        - departure orders location
        - check that all orders have arrival/visited/departure events
    """
    order_count = 4
    imei = 78787878242342
    with create_route_env(
            system_env_with_db, "test_multi_order",
            route_date=ROUTE_DATE, order_locations=[LOCATIONS[0]] * order_count, imei=imei, order_status="finished") as env:
        # enter orders radius
        push_imei_positions(
            system_env_with_db,
            imei,
            start_datetime=offset_datetime(0),
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'])])
        # spend (SERVICE_DURATION_S / 2) seconds in orders radius
        push_imei_positions(
            system_env_with_db,
            imei,
            start_datetime=offset_datetime(SERVICE_DURATION_S / 2),
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'])])
        # departure orders radius
        push_imei_positions(
            system_env_with_db,
            imei,
            start_datetime=offset_datetime(SERVICE_DURATION_S),
            track=[(LOCATIONS[0]['lat'], LOCATIONS[0]['lon'] + 1)])

        orders = get_orders(system_env_with_db, env["route"]["id"])
        assert len(orders) == order_count
        for order in orders:
            events = [item["event"] for item in order["history"]]
            assert "ARRIVAL" in events
            assert "ORDER_VISIT" in events
            assert "DEPARTURE" in events


def test_non_multi_orders_are_not_delivered_at_the_same_time(system_env_with_db):
    """
    Test the following workflow:
    - create 6 orders (A-B-AA-B-A)
    - first A in the middle has doubled SERVICE_DURATION_S
    - spend SERVICE_DURATION_S/2 seconds in A
    - spend SERVICE_DURATION_S/2 seconds in B
    - check that only first 2 orders are mark as delivered
    - spend SERVICE_DURATION_S seconds in A
    - check that next 2 orders are mark as delivered
    """
    sys_env = system_env_with_db
    imei = 67999987862524
    a_location, b_location = LOCATIONS
    a_pos = [(a_location["lat"], a_location["lon"])]
    b_pos = [(b_location["lat"], b_location["lon"])]

    with create_route_env(
        sys_env,
        "test_non_multi_orders_are_not_delivered_at_the_same_time",
        route_date=ROUTE_DATE,
        order_locations=[a_location, b_location, a_location, a_location, b_location, a_location],
        imei=imei,
    ) as env:
        patch_order(system_env_with_db, env["orders"][2], {'service_duration_s': SERVICE_DURATION_S * 2})

        push_imei_positions(sys_env, imei, start_datetime=offset_datetime(0), track=a_pos)
        push_imei_positions(sys_env, imei, start_datetime=offset_datetime(SERVICE_DURATION_S / 2), track=a_pos)
        push_imei_positions(sys_env, imei, start_datetime=offset_datetime(SERVICE_DURATION_S), track=b_pos)
        push_imei_positions(sys_env, imei, start_datetime=offset_datetime(SERVICE_DURATION_S * 3 / 2), track=b_pos)

        check_status(sys_env, OrderStatus.finished.value, env["orders"][0]["id"])
        check_status(sys_env, OrderStatus.finished.value, env["orders"][1]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][2]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][3]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][4]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][5]["id"])

        push_imei_positions(sys_env, imei, start_datetime=offset_datetime(SERVICE_DURATION_S * 2), track=a_pos)
        push_imei_positions(sys_env, imei, start_datetime=offset_datetime(SERVICE_DURATION_S * 3), track=a_pos)

        check_status(sys_env, OrderStatus.finished.value, env["orders"][0]["id"])
        check_status(sys_env, OrderStatus.finished.value, env["orders"][1]["id"])
        check_status(sys_env, OrderStatus.finished.value, env["orders"][2]["id"])
        check_status(sys_env, OrderStatus.finished.value, env["orders"][3]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][4]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][5]["id"])

        route = env_get_request(sys_env, f"{api_path_with_company_id(sys_env)}/routes/{env['route']['id']}").json()
        assert not route["courier_violated_route"]


def test_shared_service_duration_is_respected_for_orders_in_the_same_location(system_env_with_db):
    """
    Test the following workflow:
    - create orders - (AA-B-A-B-A)
    - spend shared_service_duration_s/2 + SERVICE_DURATION_S seconds on A
    - check that 3 A orders are marked as delivered
    """
    sys_env = system_env_with_db
    imei = 767897252435
    a_location, b_location = LOCATIONS

    shared_service_duration_s = 100
    with create_route_env(
        sys_env,
        "test_shared_service_duration_..._in_the_same_location",
        route_date=ROUTE_DATE,
        order_locations=[a_location, a_location, b_location, a_location, b_location, a_location],
        imei=imei,
    ) as env:
        batch_orders(
            sys_env,
            [
                {"number": order["number"], "shared_service_duration_s": shared_service_duration_s}
                for order in env["orders"]
            ]
        )

        a_pos = [(a_location["lat"], a_location["lon"])]
        final_time = shared_service_duration_s / 2 + SERVICE_DURATION_S
        push_imei_positions(sys_env, imei, start_datetime=offset_datetime(0), track=a_pos)
        push_imei_positions(sys_env, imei, start_datetime=offset_datetime(final_time), track=a_pos)

        check_status(sys_env, OrderStatus.finished.value, env["orders"][0]["id"])
        check_status(sys_env, OrderStatus.finished.value, env["orders"][1]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][2]["id"])
        check_status(sys_env, OrderStatus.finished.value, env["orders"][3]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][4]["id"])
        check_status(sys_env, OrderStatus.new.value, env["orders"][5]["id"])

        route = env_get_request(sys_env, f"{api_path_with_company_id(sys_env)}/routes/{env['route']['id']}").json()
        assert route["courier_violated_route"]


@pytest.mark.parametrize("eta_type, imei", [("arrival_time", 7196628592485), ("delivery_time", 6916385900022)])
def test_same_location_orders_with_delivery_before_next_order_time_window_including_shared_service_duration(
    system_env_with_db, eta_type, imei
):
    """
    Test the following workflow:
    - create orders - (A-B-A-B-A)
    - spend service_duration_s/2 seconds in order location before first_order.tw.start
    - spend (service_duration_s+shared_service_duration_s)/2 seconds in order location after first_order.tw.start
    - check that 2 orders are marked as delivered
    """
    service_duration_s = 60 * 60
    shared_service_duration_s = 10 * 60
    a_location, b_location = LOCATIONS
    with create_route_env(
        system_env_with_db,
        f"test_same_location_..._service_duration-{eta_type}",
        route_date=ROUTE_DATE,
        order_locations=[a_location, b_location, a_location, b_location, a_location],
        imei=imei,
    ) as env:
        common = {
            "eta_type": eta_type,
            "service_duration_s": service_duration_s,
            "shared_service_duration_s": shared_service_duration_s,
        }
        patch_order(system_env_with_db, env["orders"][0], {**common, "time_interval": "00:30-23:59"})
        patch_order(system_env_with_db, env["orders"][2], {**common, "time_interval": "00:00-23:59"})
        patch_order(system_env_with_db, env["orders"][4], {**common, "time_interval": "00:00-23:59"})

        time_zone = dateutil.tz.gettz(env["depot"]["time_zone"])
        now = datetime.datetime.combine(DATETIME.date(), datetime.time(00), tzinfo=time_zone)

        a_pos = [(a_location["lat"], a_location["lon"])]
        push_imei_positions(system_env_with_db, imei, start_datetime=now, track=a_pos)

        now += datetime.timedelta(seconds=service_duration_s / 2)
        push_imei_positions(system_env_with_db, imei, start_datetime=now, track=a_pos)

        now += datetime.timedelta(seconds=shared_service_duration_s/2)
        timeshift = service_duration_s / 2 * 1000
        push_imei_positions(system_env_with_db, imei, start_datetime=now, track=a_pos * 2, pos_timeshift_ms=timeshift)

        orders = get_orders(system_env_with_db, env["route"]["id"])
        assert len(orders) == 5

        order_events = lambda order: [item["event"] for item in order["history"]]
        assert order_events(orders[0]) == [
            "ORDER_CREATED",
            "INTERVAL_UPDATE",
            "ARRIVAL",
            "ORDER_VISIT",
            "STATUS_UPDATE",
        ]
        assert order_events(orders[2]) == [
            "ORDER_CREATED",
            "INTERVAL_UPDATE",
            "ARRIVAL",
            "ORDER_VISIT",
            "STATUS_UPDATE",
        ]
        assert order_events(orders[4]) == ["ORDER_CREATED", "INTERVAL_UPDATE"]
        assert (orders[0]["history"][-2]["position"]["time"] > orders[2]["history"][-2]["position"]["time"]) == (
            eta_type == "delivery_time"
        )
