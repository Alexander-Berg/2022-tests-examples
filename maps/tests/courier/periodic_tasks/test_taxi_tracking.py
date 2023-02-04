import copy
import time
from datetime import datetime, timedelta

from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_patch, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util import MOSCOW_TZ
from maps.b2bgeo.ya_courier.backend.test_lib.util_rented_vehicle import TEST_TAXI_VEHICLE
from ya_courier_backend.models import db, TaxiTracking, RentedCourier
from ya_courier_backend.tasks.taxi_tracking import TaxiTrackingTask

TEST_DATETIME = datetime(2019, 12, 13, 1, 1, 1, tzinfo=MOSCOW_TZ)


def _all_objects(obj):
    return list(map(obj.as_dict, db.session.query(obj).all()))


def _count_active_taxis(time):
    return TaxiTracking.count_active(time.timestamp())


def create_taxi(env, data_update=None):
    data = copy.deepcopy(TEST_TAXI_VEHICLE)
    if data_update:
        data.update(data_update)
    data.pop("ref")
    path = f"/api/v1/companies/{env.default_company.id}/rented-couriers"
    return local_post(env.client, path, headers=env.user_auth_headers, data=data)


def create_route_for_taxi(env, taxi):
    path = f"/api/v1/companies/{env.default_company.id}/couriers"
    courier = local_post(env.client, path, headers=env.user_auth_headers, data={"number": "1", "name": TEST_TAXI_VEHICLE["ref"]})
    path = f"/api/v1/companies/{env.default_company.id}/routes"
    local_post(
        env.client,
        path,
        headers=env.user_auth_headers,
        data={
            "number": "fake route",
            "courier_number": courier["number"],
            "depot_number": env.default_depot.number,
            "date": "2000-01-01",
            "rented_courier_id": taxi["id"],
        },
    )


@skip_if_remote
def test_taxi_tracking_is_added_for_new_rented_couriers_with_taxi_provider(env: Environment):
    create_taxi(env)

    with env.flask_app.app_context():
        actual_taxi_tracking = _all_objects(TaxiTracking)
        expected_taxi_tracking = [
            {
                "id": 1,
                "rented_courier_id": 1,
                "updated_at": actual_taxi_tracking[0]["updated_at"],
                "position_updated_at": None,
                "failed_attempts": 0,
            },
        ]
        assert actual_taxi_tracking == expected_taxi_tracking


@skip_if_remote
def test_active_taxi_are_bounded_by_rented_courier_created_at(env: Environment):
    taxi = create_taxi(env)

    create_route_for_taxi(env, taxi)

    with env.flask_app.app_context():
        rented_courier = db.session.query(RentedCourier).first()
        now = rented_courier.created_at
        assert _count_active_taxis(now + timedelta(minutes=4, seconds=59)) == 0
        assert _count_active_taxis(now + timedelta(minutes=5, seconds=1)) == 1
        assert _count_active_taxis(now + timedelta(days=13, hours=23, minutes=59)) == 1
        assert _count_active_taxis(now + timedelta(days=14)) == 0


@skip_if_remote
def test_active_taxi_are_bounded_by_last_position_update(env: Environment):
    taxi = create_taxi(env)
    create_route_for_taxi(env, taxi)

    with env.flask_app.app_context():
        db.session.execute(TaxiTracking.__table__.update().values(position_updated_at=TaxiTracking.updated_at))
        db.session.commit()

        [taxi_tracking] = _all_objects(TaxiTracking)
        now = datetime.fromtimestamp(taxi_tracking["position_updated_at"])
        assert _count_active_taxis(now + timedelta(seconds=29)) == 0
        assert _count_active_taxis(now + timedelta(seconds=31)) == 1
        assert _count_active_taxis(now + timedelta(days=1) - timedelta(seconds=1)) == 1
        assert _count_active_taxis(now + timedelta(days=1, seconds=1)) == 0


@skip_if_remote
def test_active_taxi_delay_is_increased_based_on_failed_attempts_when_pos_exists(env: Environment):
    taxi = create_taxi(env)
    create_route_for_taxi(env, taxi)

    with env.flask_app.app_context():
        db.session.execute(TaxiTracking.__table__.update().values(failed_attempts=3))
        db.session.commit()

        [taxi_tracking] = _all_objects(TaxiTracking)
        now = datetime.fromtimestamp(taxi_tracking["updated_at"])
        assert _count_active_taxis(now + timedelta(minutes=4, seconds=59)) == 0
        assert _count_active_taxis(now + timedelta(minutes=5, seconds=1)) == 1

        db.session.execute(TaxiTracking.__table__.update().values(position_updated_at=TaxiTracking.updated_at))
        db.session.commit()

        assert _count_active_taxis(now + timedelta(seconds=59)) == 0
        assert _count_active_taxis(now + timedelta(seconds=61)) == 1


@skip_if_remote
def test_taxi_is_active_only_if_it_has_corresponding_route(env: Environment):
    taxi = create_taxi(env)

    with env.flask_app.app_context():
        [taxi_tracking] = _all_objects(TaxiTracking)
        now = datetime.fromtimestamp(taxi_tracking["updated_at"]) + timedelta(days=1)
        assert _count_active_taxis(now) == 0

    create_route_for_taxi(env, taxi)

    with env.flask_app.app_context():
        assert _count_active_taxis(now) == 1


@skip_if_remote
def test_taxi_positions_are_added_for_active_routes_only(env: Environment):
    with freeze_time(TEST_DATETIME) as freezed_time:
        taxi = create_taxi(env)

        path = f"/api/v1/companies/{env.default_company.id}/couriers"
        courier = local_post(
            env.client, path, headers=env.user_auth_headers, data={"number": "1", "name": TEST_TAXI_VEHICLE["ref"]}
        )

        path = f"/api/v1/companies/{env.default_company.id}/routes"
        data = {
            "number": "test_route",
            "courier_number": courier["number"],
            "depot_number": env.default_depot.number,
            "date": "2019-12-13",
            "rented_courier_id": taxi["id"],
        }
        route = local_post(
            env.client,
            path,
            headers=env.user_auth_headers,
            data=data,
        )

        freezed_time.tick(delta=timedelta(minutes=5, seconds=1))
        with env.flask_app.app_context():
            task = TaxiTrackingTask(env.flask_app)
            task.run({})

            [taxi_tracking] = _all_objects(TaxiTracking)
            assert taxi_tracking["position_updated_at"] is None
            assert taxi_tracking["updated_at"] == time.time()
            assert taxi_tracking["failed_attempts"] == 1

        path = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
        local_patch(
            env.client, path, headers=env.user_auth_headers, data={"route_start": "00:00:00", "route_finish": "23:59:59"}
        )

        freezed_time.tick(delta=timedelta(minutes=5, seconds=1))
        with env.flask_app.app_context():
            task = TaxiTrackingTask(env.flask_app)
            task.run({})

            [taxi_tracking] = _all_objects(TaxiTracking)
            assert taxi_tracking["position_updated_at"] == time.time()
            assert taxi_tracking["updated_at"] == time.time()
            assert taxi_tracking["failed_attempts"] == 0

        path = f"/api/v1/companies/{env.default_company.id}/courier-position/{courier['id']}/routes/{route['id']}"
        positions = local_get(env.client, path, headers=env.user_auth_headers)

        assert positions == [
            {
                "accuracy": 50.0,
                "courier_id": courier["id"],
                "id": 1,
                "imei": None,
                "imei_str": None,
                "lat": 55.0,
                "lon": 37.0,
                "route_id": route["id"],
                "server_time": 1576188663.0,
                "server_time_iso": "2019-12-13T01:11:03+03:00",
                "time": 1576191600.0,
                "time_iso": "2019-12-13T02:00:00+03:00",
            }
        ]


@skip_if_remote
def test_errors_from_taxi_api_are_recorded_and_ignored(env: Environment):
    with freeze_time(TEST_DATETIME) as freezed_time:
        taxi = create_taxi(env, data_update={"order_id": "internal_error"})

        path = f"/api/v1/companies/{env.default_company.id}/couriers"
        courier = local_post(
            env.client, path, headers=env.user_auth_headers, data={"number": "1", "name": TEST_TAXI_VEHICLE["ref"]}
        )

        path = f"/api/v1/companies/{env.default_company.id}/routes"
        data = {
            "number": "test_route",
            "courier_number": courier["number"],
            "depot_number": env.default_depot.number,
            "date": "2019-12-13",
            "route_start": "00:00:00",
            "route_finish": "23:59:59",
            "rented_courier_id": taxi["id"],
        }
        local_post(
            env.client,
            path,
            headers=env.user_auth_headers,
            data=data,
        )

        freezed_time.tick(delta=timedelta(minutes=5, seconds=1))
        with env.flask_app.app_context():
            task = TaxiTrackingTask(env.flask_app)
            task.run({})

            [taxi_tracking] = _all_objects(TaxiTracking)
            assert taxi_tracking["position_updated_at"] is None
            assert taxi_tracking["updated_at"] == time.time()
            assert taxi_tracking["failed_attempts"] == 1

        freezed_time.tick(delta=timedelta(minutes=5, seconds=1))
        with env.flask_app.app_context():
            db.session.execute(RentedCourier.__table__.update().values(order_id="not_active"))
            db.session.commit()

            task = TaxiTrackingTask(env.flask_app)
            task.run({})

            [taxi_tracking] = _all_objects(TaxiTracking)
            assert taxi_tracking["position_updated_at"] is None
            assert taxi_tracking["updated_at"] == time.time()
            assert taxi_tracking["failed_attempts"] == 2

        freezed_time.tick(delta=timedelta(minutes=5, seconds=1))
        with env.flask_app.app_context():
            db.session.execute(RentedCourier.__table__.update().values(order_id="not_found"))
            db.session.commit()

            task = TaxiTrackingTask(env.flask_app)
            task.run({})

            [taxi_tracking] = _all_objects(TaxiTracking)
            assert taxi_tracking["position_updated_at"] is None
            assert taxi_tracking["updated_at"] == time.time()
            assert taxi_tracking["failed_attempts"] == 3

        freezed_time.tick(delta=timedelta(minutes=5, seconds=1))
        with env.flask_app.app_context():
            db.session.execute(RentedCourier.__table__.update().values(order_id="normal id"))
            db.session.commit()

            task = TaxiTrackingTask(env.flask_app)
            task.run({})

            [taxi_tracking] = _all_objects(TaxiTracking)
            assert taxi_tracking["position_updated_at"] == time.time()
            assert taxi_tracking["updated_at"] == time.time()
            assert taxi_tracking["failed_attempts"] == 0


@skip_if_remote
def test_taxi_trackings_are_reported_to_unistat(env: Environment):
    def _request_unistat(env):
        path = "/api/v1/unistat"
        signals = local_get(env.client, path, headers=env.user_auth_headers)
        return {signal[0]: signal[1] for signal in signals}

    with freeze_time(TEST_DATETIME) as freezed_time:
        taxi = create_taxi(env)

        path = f"/api/v1/companies/{env.default_company.id}/couriers"
        courier = local_post(
            env.client, path, headers=env.user_auth_headers, data={"number": "1", "name": TEST_TAXI_VEHICLE["ref"]}
        )

        path = f"/api/v1/companies/{env.default_company.id}/routes"
        data = {
            "number": "test_route",
            "courier_number": courier["number"],
            "depot_number": env.default_depot.number,
            "date": "2019-12-13",
            "route_start": "00:00:00",
            "route_finish": "23:59:59",
            "rented_courier_id": taxi["id"],
        }
        local_post(
            env.client,
            path,
            headers=env.user_auth_headers,
            data=data,
        )

        metrics = _request_unistat(env)
        assert "pushed_positions_taxi_summ" not in metrics
        assert metrics["taxi_trackings_to_update_count_axxx"] == 0

        freezed_time.tick(delta=timedelta(minutes=5, seconds=1))

        metrics = _request_unistat(env)
        assert "pushed_positions_taxi_summ" not in metrics
        assert metrics["taxi_trackings_to_update_count_axxx"] == 1

        with env.flask_app.app_context():
            task = TaxiTrackingTask(env.flask_app)
            task.run({})

        metrics = _request_unistat(env)
        assert metrics["pushed_positions_taxi_summ"] == 1
        assert metrics["taxi_trackings_to_update_count_axxx"] == 0
