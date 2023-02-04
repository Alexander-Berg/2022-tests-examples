from datetime import datetime, time, timedelta
from freezegun import freeze_time
import dateutil

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_patch,
    local_post,
    local_get
)
from ya_courier_backend.logic.route_status import RouteStatus


@skip_if_remote
def test_route_without_orders_and_route_start_is_planned(env: Environment):
    path_courier_routes = f"/api/v1/couriers/{env.default_courier.id}/routes"
    path_courier_route = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}"

    route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
    routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
    assert route["status"] == RouteStatus.PLANNED.value
    assert routes == []


@skip_if_remote
def test_route_with_only_route_start_is_still_planned(env: Environment):
    route_datetime = datetime.combine(env.default_route.date, time(0, 0), tzinfo=dateutil.tz.gettz("Europe/Moscow"))
    with freeze_time(route_datetime) as freezed_time:
        path_route = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
        path_courier_routes = f"/api/v1/couriers/{env.default_courier.id}/routes"
        path_courier_route = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}"

        # route_start = 1h+1m, so that tracking_start is 1m
        local_patch(env.client, path_route, data={"route_start_s": 60 * 60 + 60}, headers=env.user_auth_headers)

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == []

        # exactly route_start
        freezed_time.tick(delta=timedelta(minutes=1))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == []


@skip_if_remote
def test_route_with_only_route_finish_is_still_finished(env: Environment):
    route_datetime = datetime.combine(env.default_route.date, time(0, 0), tzinfo=dateutil.tz.gettz("Europe/Moscow"))
    with freeze_time(route_datetime) as freezed_time:
        path_route = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
        path_courier_routes = f"/api/v1/couriers/{env.default_courier.id}/routes"
        path_courier_route = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}"

        # route_finish = 1m
        local_patch(env.client, path_route, data={"route_finish_s": 60}, headers=env.user_auth_headers)

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == []

        # just over route_finish
        freezed_time.tick(delta=timedelta(minutes=1, seconds=1))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == []


@skip_if_remote
def test_route_with_route_start_and_finish_changes_status_accordingly(env: Environment):
    route_datetime = datetime.combine(env.default_route.date, time(0, 0), tzinfo=dateutil.tz.gettz("Europe/Moscow"))
    with freeze_time(route_datetime) as freezed_time:
        path_route = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
        path_courier_routes = f"/api/v1/couriers/{env.default_courier.id}/routes"
        path_courier_route = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}"

        # route_start = 1h+1m, so that tracking_start is 1m
        local_patch(env.client, path_route, data={"route_start_s": 60 * 60 + 60, "route_finish_s": 60 * 60 + 120}, headers=env.user_auth_headers)

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == [route]

        # exactly route_finish
        freezed_time.tick(delta=timedelta(hours=1, minutes=2))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.IN_PROGRESS.value
        assert routes == [route]

        # just over route_finish
        freezed_time.tick(delta=timedelta(seconds=1))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.FINISHED.value
        assert routes == []


@skip_if_remote
def test_route_start_is_prioritized_before_time_window_start(env: Environment):
    route_datetime = datetime.combine(env.default_route.date, time(0, 0), tzinfo=dateutil.tz.gettz("Europe/Moscow"))
    with freeze_time(route_datetime) as freezed_time:
        path_route = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
        path_courier_routes = f"/api/v1/couriers/{env.default_courier.id}/routes"
        path_courier_route = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}"
        path_orders = f"/api/v1/companies/{env.default_company.id}/orders"

        # route_start = 2h, so that tracking_start is 1h
        local_patch(env.client, path_route, data={"route_start_s": 2 * 60 * 60}, headers=env.user_auth_headers)

        order_data = {
            "number": "first_order",
            "time_interval": "00:00-01:00",
            "address": "some address",
            "lat": 55.791928,
            "lon": 37.841492,
            "route_id": env.default_route.id,
        }
        local_post(env.client, path_orders, data=order_data, headers=env.user_auth_headers)

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == [route]

        # exactly route_start
        freezed_time.tick(delta=timedelta(hours=1))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.IN_PROGRESS.value
        assert routes == [route]

        # just over time_window.end + 5h
        freezed_time.tick(delta=timedelta(hours=5, seconds=1))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.FINISHED.value
        assert routes == []


@skip_if_remote
def test_route_with_order_time_window_changes_status_based_on_it_with_five_hours_offset(env: Environment):
    route_datetime = datetime.combine(env.default_route.date, time(0, 0), tzinfo=dateutil.tz.gettz("Europe/Moscow"))
    with freeze_time(route_datetime) as freezed_time:
        path_courier_routes = f"/api/v1/couriers/{env.default_courier.id}/routes"
        path_courier_route = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}"
        path_orders = f"/api/v1/companies/{env.default_company.id}/orders"

        # route starts at 10:00-5h=05:00, ends at 11:00+5h=16:00
        order_data = {
            "number": "first_order",
            "time_interval": "10:50-11:00",
            "address": "some address",
            "lat": 55.791928,
            "lon": 37.841492,
            "route_id": env.default_route.id,
        }
        local_post(env.client, path_orders, data=order_data, headers=env.user_auth_headers)
        order_data = {
            **order_data,
            "number": "second_order",
            "time_interval": "10:00-10:10",
        }
        local_post(env.client, path_orders, data=order_data, headers=env.user_auth_headers)

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == [route]

        # exactly start time = min(time_windows.start) - 5h
        freezed_time.tick(delta=timedelta(hours=5))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.IN_PROGRESS.value
        assert routes == [route]

        # exactly finish time = max(time_windows.end) + 5h
        freezed_time.tick(delta=timedelta(hours=11))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.IN_PROGRESS.value
        assert routes == [route]

        # just over finish time
        freezed_time.tick(delta=timedelta(seconds=1))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.FINISHED.value
        assert routes == []


@skip_if_remote
def test_planned_routes_are_shown_only_three_days_into_the_future_for_courier(env: Environment):
    route_datetime = datetime.combine(env.default_route.date, time(0, 0), tzinfo=dateutil.tz.gettz("Europe/Moscow"))
    test_datetime = route_datetime - timedelta(days=3)
    with freeze_time(test_datetime) as freezed_time:
        path_route = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
        path_courier_routes = f"/api/v1/couriers/{env.default_courier.id}/routes"
        path_courier_route = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}"

        # route_start = 1h+1m, so that tracking_start is 1m
        local_patch(env.client, path_route, data={"route_start_s": 60 * 60 + 60, "route_finish_s": 60 * 60 + 120}, headers=env.user_auth_headers)

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == []

        # exactly route_start-3d
        freezed_time.tick(delta=timedelta(minutes=1))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.PLANNED.value
        assert routes == [route]


@skip_if_remote
def test_finished_routes_are_not_shown_for_courier(env: Environment):
    route_datetime = datetime.combine(env.default_route.date, time(0, 0), tzinfo=dateutil.tz.gettz("Europe/Moscow"))
    with freeze_time(route_datetime) as freezed_time:
        path_route = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}"
        path_courier_routes = f"/api/v1/couriers/{env.default_courier.id}/routes"
        path_courier_route = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}"

        # route_start = 1h+1m, so that tracking_start is 1m
        local_patch(env.client, path_route, data={"route_start_s": 0, "route_finish_s": 10}, headers=env.user_auth_headers)

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.IN_PROGRESS.value
        assert routes == [route]

        # just over route_finish
        freezed_time.tick(delta=timedelta(seconds=11))

        route = local_get(env.client, path_courier_route, headers=env.user_auth_headers)
        routes = local_get(env.client, path_courier_routes, headers=env.user_auth_headers)
        assert route["status"] == RouteStatus.FINISHED.value
        assert routes == []
