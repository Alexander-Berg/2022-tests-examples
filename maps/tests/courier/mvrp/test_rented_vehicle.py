from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get, local_delete
from maps.b2bgeo.ya_courier.backend.test_lib.util_rented_vehicle import FAKE_TASK_ID, TEST_TAXI_VEHICLE, TEST_ANOTHER_TAXI_VEHICLE


@skip_if_remote
def test_rented_vehicle(env: Environment):
    local_post(env.client,
               f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
               headers=env.user_auth_headers,
               data=TEST_TAXI_VEHICLE)

    local_post(env.client,
               f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
               headers=env.user_auth_headers,
               data=TEST_ANOTHER_TAXI_VEHICLE)

    rented_vehicles = local_get(env.client,
                                f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
                                headers=env.user_auth_headers)

    for rented_vehicle in rented_vehicles:
        assert "date" in rented_vehicle
        del rented_vehicle["date"]
    assert rented_vehicles == [TEST_TAXI_VEHICLE, TEST_ANOTHER_TAXI_VEHICLE]

    local_delete(env.client,
                 f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
                 headers=env.user_auth_headers,
                 data={'ref': TEST_ANOTHER_TAXI_VEHICLE['ref']})

    rented_vehicles = local_get(env.client,
                                f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
                                headers=env.user_auth_headers)

    assert "date" in rented_vehicles[0]
    del rented_vehicles[0]["date"]
    assert rented_vehicles == [TEST_TAXI_VEHICLE]

    local_delete(env.client,
                 f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
                 headers=env.user_auth_headers,
                 data={'ref': TEST_ANOTHER_TAXI_VEHICLE['ref']},
                 expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_rented_vehicle_doesnt_exist(env: Environment):
    local_delete(env.client,
                 f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
                 headers=env.user_auth_headers,
                 data={'ref': TEST_TAXI_VEHICLE['ref']},
                 expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_rented_vehicle_already_exists(env: Environment):
    local_post(env.client,
               f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
               headers=env.user_auth_headers,
               data=TEST_TAXI_VEHICLE)

    local_post(env.client,
               f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
               headers=env.user_auth_headers,
               data=TEST_TAXI_VEHICLE,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_rented_vehicle_bad_request_post_wrong_data(env: Environment):
    local_post(env.client,
               f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
               headers=env.user_auth_headers,
               data={'some_key': 'some value'},
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_rented_vehicle_bad_request_delete_wrong_type(env: Environment):
    local_delete(env.client,
                 f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
                 headers=env.user_auth_headers,
                 data={'ref': 123},
                 expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_rented_vehicle_bad_request_delete_empty_data(env: Environment):
    local_delete(env.client,
                 f'/api/v1/vrs/mvrp/{FAKE_TASK_ID}/rented-vehicle',
                 headers=env.user_auth_headers,
                 data={},
                 expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
