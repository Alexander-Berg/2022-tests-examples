import pytest
import datetime
import dateutil
import requests
from urllib.parse import urlencode

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    create_route_env, env_get_request, env_patch_request,
    api_path_with_company_id, get_position_shifted_east
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import UserKind, create_sharing_env, create_sharing_depots_with_orders
from ya_courier_backend.models.order_status import OrderStatus

EXPECTED_FIELDS_SUBSET = {
    "arrival_time",
    "company_name",
    "courier_id",
    "courier_name",
    "depot_id",
    "failed_time_window",
    "route_date"
}


def test_expected_orders(system_env_with_db):
    """
    Test /expected-orders handler without orders sharing
    Check:
        - response is empty before ETA for orders is calculated initially
        - arrival time is present in response
        - radius parameters filters orders
        - finished orders are not present in response
        - 'from' and 'to' parameters checks
    """

    route_datetime = datetime.datetime.now(tz=dateutil.tz.gettz('Europe/Moscow'))

    coordinates_lst = [
        (55.733827, 37.588722)
    ]
    for shift in [300, 600]:
        shifted_lat, shifted_lon = get_position_shifted_east(coordinates_lst[0][0], coordinates_lst[0][1], shift)
        coordinates_lst.append((shifted_lat, shifted_lon))

    order_locations = [
        {
            "lat": coordinate[0],
            "lon": coordinate[1]
        } for coordinate in coordinates_lst
    ]

    with create_route_env(system_env_with_db,
                          "test_expected_orders",
                          order_locations=order_locations,
                          time_intervals=["00:00-10:00",
                                          "12:00-14:00",
                                          "16:00-18:00"],
                          route_date=route_datetime.date().isoformat()) as route_env:

        response = env_get_request(
            system_env_with_db,
            "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
                route_env["courier"]["id"],
                route_env["route"]["id"],
                order_locations[0]["lat"],
                order_locations[0]["lon"],
                "00:00"
            )
        )
        assert response.status_code == requests.codes.ok

        from_datetime = datetime.datetime.combine(
            route_datetime,
            datetime.time(),
            route_datetime.tzinfo
        )
        to_datetime = from_datetime + datetime.timedelta(days=1)
        parameters = {
            "lat": order_locations[0]["lat"],
            "lon": order_locations[0]["lon"],
            "from": from_datetime.isoformat(),
            "to": to_datetime.isoformat(),
            "radius": 500
        }

        resp = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, f"expected-orders?{urlencode(parameters)}")
        )
        assert resp.status_code == requests.codes.ok
        j = resp.json()
        assert len(j) == 2

        for order in j:
            assert EXPECTED_FIELDS_SUBSET < order.keys()
            arrival_time = dateutil.parser.parse(order["arrival_time"])
            assert from_datetime <= arrival_time <= to_datetime

        parameters.update({"radius": 200})
        resp = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, f"expected-orders?{urlencode(parameters)}")
        )
        assert resp.status_code == requests.codes.ok
        j = resp.json()
        assert len(j) == 1

        parameters.update({"radius": 500})
        first_order_id = route_env["orders"][0]["id"]
        patch_data = {
            "status": "finished"
        }
        resp = env_patch_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, f"orders/{first_order_id}"),
            data=patch_data
        )
        assert resp.status_code == requests.codes.ok
        resp = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, f"expected-orders?{urlencode(parameters)}")
        )
        assert resp.status_code == requests.codes.ok
        j = resp.json()
        assert len(j) == 1

        parameters.update({
            "from": to_datetime.isoformat(),
            "to": from_datetime.isoformat(),
        })
        resp = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, f"expected-orders?{urlencode(parameters)}")
        )
        assert resp.status_code == requests.codes.unprocessable_entity
        assert resp.json()["message"] == "Parameter 'to' value must be greater than 'from' value"

        parameters.update({
            "from": from_datetime.isoformat(),
            "to": (to_datetime + datetime.timedelta(minutes=1)).isoformat(),
        })
        resp = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, f"expected-orders?{urlencode(parameters)}")
        )
        assert resp.status_code == requests.codes.unprocessable_entity
        assert resp.json()["message"] == "Interval from-to is longer than 24 hours"


@skip_if_remote
def test_shared_expected_orders_sharing_2_companies(env_with_2_companies_sharing_setup):
    """
    Test /expected-orders handler with orders sharing
    Check:
        - shared_by_company's order is present in /expected-orders response
            for shared_with_company's admin
    """
    sharing_env = env_with_2_companies_sharing_setup
    db_env = sharing_env['dbenv']

    shared_with_company_idx = 0
    shared_with_company_id = sharing_env["companies"][shared_with_company_idx]["id"]
    shared_by_company_idx = 1
    shared_by_company_id = sharing_env["companies"][shared_by_company_idx]["id"]
    shared_by_company_env = sharing_env["companies"][shared_by_company_idx]
    assert len(shared_by_company_env["sharing_orders"][shared_with_company_idx]) == 1

    response = env_get_request(
        db_env,
        "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
            shared_by_company_env["sharing_couriers"][0][0]["id"],
            shared_by_company_env["sharing_routes"][0][0]["id"],
            55.73,
            37.58,
            "00:00"
        ),
        auth=db_env.auth_header_super
    )
    assert response.status_code == requests.codes.ok

    # These indices corresponds to not shared entities in sharing company env
    not_shared_courier = shared_by_company_env["all_couriers"][1]
    not_shared_route = shared_by_company_env["all_routes"][1]
    not_shared_order = shared_by_company_env["all_orders"][2]

    response = env_get_request(
        db_env,
        "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
            not_shared_courier["id"],
            not_shared_route["id"],
            55.73,
            37.58,
            "00:00"
        ),
        auth=db_env.auth_header_super
    )
    assert response.status_code == requests.codes.ok

    from_datetime = datetime.datetime.combine(
        datetime.date.today(),
        datetime.time()
    ).astimezone(dateutil.tz.gettz("UTC"))
    to_datetime = from_datetime + datetime.timedelta(days=1)

    parameters = {
        "lat": 55.73,
        "lon": 37.58,
        "from": from_datetime.isoformat(),
        "to": to_datetime.isoformat(),
        "radius": 500
    }

    resp = env_get_request(
        db_env,
        f"companies/{shared_with_company_id}/expected-orders?{urlencode(parameters)}",
        auth=db_env.get_user_auth(sharing_env['companies'][shared_with_company_idx]['users'][UserKind.admin])
    )
    assert resp.status_code == requests.codes.ok
    j = resp.json()
    assert len(j) == 1
    assert j[0]["id"] == shared_by_company_env["sharing_orders"][shared_with_company_idx][0]["id"]

    for order in j:
        assert EXPECTED_FIELDS_SUBSET < order.keys()
        arrival_time = dateutil.parser.parse(order["arrival_time"])
        assert from_datetime <= arrival_time <= to_datetime

    resp = env_get_request(
        db_env,
        f"companies/{shared_by_company_id}/expected-orders?{urlencode(parameters)}",
        auth=db_env.auth_header_super
    )
    assert resp.status_code == requests.codes.ok
    j = resp.json()
    assert len(j) == 2
    assert {j[0]["id"], j[1]["id"]} == {
        shared_by_company_env["sharing_orders"][shared_with_company_idx][0]["id"],
        not_shared_order["id"]
    }

    for order in j:
        assert EXPECTED_FIELDS_SUBSET < order.keys()
        arrival_time = dateutil.parser.parse(order["arrival_time"])
        assert from_datetime <= arrival_time <= to_datetime


@pytest.fixture()
def _env_with_4_companies_sharing_setup(system_env_with_db):
    with create_sharing_env(system_env_with_db, 'sharing_depots_', 4) as sharing_env:
        # Company 0 doesn't share first order from a route, shares second order from a route with company 1,
        # third order with company 2, fourth order with companies 1 and 2.
        # The orders are located on a line, 10 meters apart from each other.
        create_sharing_depots_with_orders(sharing_env, company_idx=0, order_locations_shift=10, depot_infos=[{
            'name': 'depot',
            'routes': [
                {
                    'orders': [
                        {'share_with': []},
                        {'share_with': [1]},
                        {'share_with': [2]},
                        {'share_with': [1, 2]},
                    ]
                }
            ]
        }])
        yield sharing_env


@skip_if_remote
def test_shared_expected_orders_sharing_4_companies(_env_with_4_companies_sharing_setup):
    """
    Test /expected-orders handler with orders from one route shared with 2 companies
    Check:
        - all shared_by_company's order is present in /expected-orders response for shared_by_company
        - for 1, 2 companies only shared with particular company orders are present /expected-orders
        - for 3 company /expected-orders returns empty list
    """
    sharing_env = _env_with_4_companies_sharing_setup
    db_env = sharing_env['dbenv']

    shared_by_company_env = sharing_env["companies"][0]
    shared_by_company_idx = shared_by_company_env["id"]

    response = env_get_request(
        db_env,
        "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
            shared_by_company_env["all_couriers"][0]["id"],
            shared_by_company_env["all_routes"][0]["id"],
            55.73,
            37.58,
            "00:00"
        ),
        auth=db_env.auth_header_super
    )
    assert response.status_code == requests.codes.ok

    from_datetime = datetime.datetime.combine(
        datetime.date.today(),
        datetime.time()
    ).astimezone(dateutil.tz.gettz("UTC"))
    to_datetime = from_datetime + datetime.timedelta(days=1)

    parameters = {
        "lat": 55.73,
        "lon": 37.58,
        "from": from_datetime.isoformat(),
        "to": to_datetime.isoformat(),
        "radius": 500
    }
    from_datetime = datetime.datetime.combine(
        datetime.date.today(),
        datetime.time()
    ).astimezone(dateutil.tz.gettz("UTC"))
    to_datetime = from_datetime + datetime.timedelta(days=1)

    # /expected-orders for shared_by_company should return all orders
    resp = env_get_request(
        db_env,
        f"companies/{shared_by_company_idx}/expected-orders?{urlencode(parameters)}",
        auth=db_env.auth_header_super
    )
    assert resp.status_code == requests.codes.ok, response.text
    j = resp.json()
    assert len(j) == 4

    # /expected-orders for companies 1, 2, 3 should return all orders shared with particular company
    company_ids = [sharing_env['companies'][company_idx]['id'] for company_idx in range(1, 4)]
    expected_orders_cnts = [2, 2, 0]

    for shared_with_company_id, expected_orders_cnt in zip(company_ids, expected_orders_cnts):
        resp = env_get_request(
            db_env,
            f"companies/{shared_with_company_id}/expected-orders?{urlencode(parameters)}",
            auth=db_env.auth_header_super
        )
        assert resp.status_code == requests.codes.ok, response.text
        j = resp.json()
        assert len(j) == expected_orders_cnt
        for order in j:
            assert shared_with_company_id in order["shared_with_company_ids"]


def test_expected_orders_after_order_status_change(system_env_with_db):
    """
        Test /expected-orders if order status was changed, but eta was not updated.
    """

    route_datetime = datetime.datetime.now(tz=dateutil.tz.gettz('Europe/Moscow'))

    coordinates_lst = [
        (50.828282, 39.121212)
    ]
    shift = 300
    shifted_lat, shifted_lon = get_position_shifted_east(coordinates_lst[0][0], coordinates_lst[0][1], shift)
    coordinates_lst.append((shifted_lat, shifted_lon))

    order_locations = [
        {
            "lat": coordinate[0],
            "lon": coordinate[1]
        } for coordinate in coordinates_lst
    ]

    with create_route_env(system_env_with_db,
                          "test_expected_orders_after_order_status_change",
                          order_locations=order_locations,
                          time_intervals=["00:00-10:00",
                                          "12:00-14:00",
                                          "16:00-18:00"],
                          route_date=route_datetime.date().isoformat(),
                          order_status=OrderStatus.cancelled.value) as route_env:

        resp = env_get_request(
            system_env_with_db,
            "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
                route_env["courier"]["id"],
                route_env["route"]["id"],
                order_locations[0]["lat"],
                order_locations[0]["lon"],
                "00:00"
            )
        )
        assert resp.ok, resp.text

        patch_data = {
            "status": OrderStatus.new.value
        }
        resp = env_patch_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, f"orders/{route_env['orders'][0]['id']}"),
            data=patch_data
        )
        assert resp.ok, resp.text

        from_datetime = datetime.datetime.combine(
            route_datetime,
            datetime.time(),
            route_datetime.tzinfo
        )
        to_datetime = from_datetime + datetime.timedelta(days=1)
        parameters = {
            "lat": order_locations[0]["lat"],
            "lon": order_locations[0]["lon"],
            "from": from_datetime.isoformat(),
            "to": to_datetime.isoformat(),
            "radius": 500
        }
        resp = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, f"expected-orders?{urlencode(parameters)}")
        )
        assert resp.ok, resp.text
        assert len(resp.json()) == 0
