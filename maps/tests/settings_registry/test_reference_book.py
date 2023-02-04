from copy import deepcopy
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_get,
    local_post, local_patch, local_delete,
)

COURIER_ALL_FIELDS = \
    {
        "name": "Some courier",
        "number": "some number",
        "phone": "+79999999999"
    }

COURIER_ONLY_REQUIRED_FIELDS = \
    {
        "number": "some other number",
        "phone": "+79999999999"
    }

VEHICLE_ALL_FIELDS = \
    {
        "name": "Some vehicle",
        "number": "a111aa750",
        "routing_mode": "driving",
        "parameters": {
            "max_weight": 2000.0,
            "width": 2.0,
            "height": 1.5,
            "length": 4.5,
        },
        "capacity": {
            "width": 1.5,
            "height": 0.5,
            "depth": 2.0,
            "weight": 500.0,
            "units": 0.0,
        },
        "cost": {
            "fixed": 2000.0,
            "hour": 0.0,
            "km": 15.0,
            "location": 0.0,
            "run": 0.5,
            "tonne_km": 150.0,
        },
        "garage": {
            "start": {
                "lat": 55.66206,
                "lon": 37.556774,
                "address": "Leo Tolstoy Str, 16",
            },
            "end": {
                "lat": 55.66206,
                "lon": 37.556774,
                "address": "Leo Tolstoy Str, 16",
            },
        },
        "imei": "12345",
        "start_from_depot": False,
        "return_to_depot": False,
        "max_runs": 1,
        "tags": ["tag1", "tag2"],
        "shifts": [
            {
                "time_window": {
                    "start": "07:00:00",
                    "end": "19:00:00",
                },
                "hard_window": True,
                "max_duration_s": 6000.0,
                "maximal_stops": 10,
                "minimal_stops": 1,
                "penalty": {
                    "stop_excess": {
                        'per_stop': 100.0,
                    },
                    "stop_lack": {
                        'per_stop': 100.0,
                    },
                },
                "balanced_group_id": "test_group",
            },
        ],
    }

VEHICLE_ONLY_REQUIRED_FIELDS = \
    {
        "name": "Some vehicle",
        "number": "a111aa751",
        "routing_mode": "driving"
    }


@skip_if_remote
def test_courier_reference(env: Environment):
    path_courier = f"/api/v1/reference-book/companies/{env.default_company.id}/couriers"

    local_post(env.client,
               path_courier,
               headers=env.user_auth_headers,
               data=[COURIER_ALL_FIELDS])
    courier = deepcopy(COURIER_ALL_FIELDS)

    resp = local_get(env.client,
                     path_courier,
                     headers=env.user_auth_headers)
    courier["id"] = resp[0]["id"]
    assert resp[0] == courier

    path_courier_with_id = f"/api/v1/reference-book/companies/{env.default_company.id}/couriers/{courier['id']}"
    courier = deepcopy(COURIER_ALL_FIELDS)
    courier["name"] = "Test courier"

    local_patch(env.client,
                path_courier_with_id,
                headers=env.user_auth_headers,
                data=courier)

    resp = local_get(env.client,
                     path_courier_with_id,
                     headers=env.user_auth_headers)
    courier['id'] = resp['id']
    assert resp == courier

    resp = local_get(env.client,
                     path_courier,
                     headers=env.user_auth_headers)
    assert len(resp) == 1
    assert resp[0] == courier

    local_post(env.client,
               path_courier,
               headers=env.user_auth_headers,
               data=[COURIER_ONLY_REQUIRED_FIELDS])

    resp = local_get(env.client,
                     path_courier,
                     headers=env.user_auth_headers)
    assert len(resp) == 2

    local_delete(env.client,
                 path_courier_with_id,
                 headers=env.user_auth_headers)

    resp = local_get(env.client,
                     path_courier,
                     headers=env.user_auth_headers)
    assert len(resp) == 1

    local_post(env.client,
               path_courier,
               headers=env.user_auth_headers,
               data=[COURIER_ONLY_REQUIRED_FIELDS, COURIER_ALL_FIELDS],
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_invalid_courier(env: Environment):
    path_courier = f"/api/v1/reference-book/companies/{env.default_company.id}/couriers"

    courier_no_phone = deepcopy(COURIER_ALL_FIELDS)
    del courier_no_phone["phone"]
    resp = local_post(env.client,
                      path_courier,
                      headers=env.user_auth_headers,
                      data=[courier_no_phone],
                      expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "'phone' is a required property" in resp['message']

    courier_no_number = deepcopy(COURIER_ALL_FIELDS)
    del courier_no_number["number"]
    resp = local_post(env.client,
                      path_courier,
                      headers=env.user_auth_headers,
                      data=[courier_no_number],
                      expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "'number' is a required property" in resp['message']


@skip_if_remote
def test_too_many_objects_couriers(env: Environment):
    env.flask_app.config['MAX_POST_OBJECT_COUNT'] = 3
    path_courier = f"/api/v1/reference-book/companies/{env.default_company.id}/couriers"
    resp = local_post(env.client,
                      path_courier,
                      headers=env.user_auth_headers,
                      data=[COURIER_ALL_FIELDS] * 4,
                      expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert 'The number of objects must not exceed' in resp['message']


@skip_if_remote
def test_vehicle(env: Environment):
    path_vehicle = f"/api/v1/reference-book/companies/{env.default_company.id}/vehicles"

    local_post(env.client,
               path_vehicle,
               headers=env.user_auth_headers,
               data=[VEHICLE_ALL_FIELDS])
    vehicle = deepcopy(VEHICLE_ALL_FIELDS)

    resp = local_get(env.client,
                     path_vehicle,
                     headers=env.user_auth_headers)
    vehicle['id'] = resp[0]['id']
    assert len(resp) == 1
    assert resp[0] == vehicle

    path_vehicle_with_id = f"/api/v1/reference-book/companies/{env.default_company.id}/vehicles/{vehicle['id']}"
    vehicle_patch = deepcopy(VEHICLE_ALL_FIELDS)
    vehicle_patch['shifts'] = [
        {
            "time_window": {
                "start": "08:00:00",
                "end": "18:00:00"
            },
            "hard_window": False,
            "max_duration_s": 12000.0,
            "maximal_stops": 5,
            "minimal_stops": 2,
            "balanced_group_id": "some_other_test_group",
            "penalty": {
                "stop_excess": {
                    'per_stop': 10.0,
                },
                "stop_lack": {
                    'per_stop': 10.0,
                },
            },
        },
    ]
    local_patch(env.client,
                path_vehicle_with_id,
                headers=env.user_auth_headers,
                data=vehicle_patch)
    vehicle_patch["id"] = vehicle["id"]
    resp = local_get(env.client,
                     path_vehicle_with_id,
                     headers=env.user_auth_headers)
    assert resp == vehicle_patch

    local_post(env.client,
               path_vehicle,
               headers=env.user_auth_headers,
               data=[VEHICLE_ONLY_REQUIRED_FIELDS])

    resp = local_get(env.client,
                     path_vehicle,
                     headers=env.user_auth_headers)
    assert len(resp) == 2

    local_delete(env.client,
                 path_vehicle_with_id,
                 headers=env.user_auth_headers)

    resp = local_get(env.client,
                     path_vehicle,
                     headers=env.user_auth_headers)
    assert len(resp) == 1

    local_post(env.client,
               path_vehicle,
               headers=env.user_auth_headers,
               data=[VEHICLE_ALL_FIELDS, VEHICLE_ONLY_REQUIRED_FIELDS],
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_invalid_vehicle(env: Environment):
    path_vehicle = f"/api/v1/reference-book/companies/{env.default_company.id}/vehicles"

    vehicle_no_number = deepcopy(VEHICLE_ALL_FIELDS)
    del vehicle_no_number["number"]
    resp = local_post(env.client,
                      path_vehicle,
                      headers=env.user_auth_headers,
                      data=[vehicle_no_number],
                      expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "'number' is a required property" in resp['message']

    vehicle_shift_no_tw = deepcopy(VEHICLE_ALL_FIELDS)
    del vehicle_shift_no_tw["shifts"][0]["time_window"]
    resp = local_post(env.client,
                      path_vehicle,
                      headers=env.user_auth_headers,
                      data=[vehicle_shift_no_tw],
                      expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "'time_window' is a required property" in resp['message']

    vehicle_negative_cost = deepcopy(VEHICLE_ALL_FIELDS)
    vehicle_negative_cost["cost"]["fixed"] = -1
    local_post(env.client,
               path_vehicle,
               headers=env.user_auth_headers,
               data=[vehicle_negative_cost],
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    vehicle_invalid_tw = deepcopy(VEHICLE_ALL_FIELDS)
    vehicle_invalid_tw["shifts"][0]["time_window"]["start"] = "2021-02-19"
    local_post(env.client,
               path_vehicle,
               headers=env.user_auth_headers,
               data=[vehicle_invalid_tw],
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_too_many_objects_vehicle(env: Environment):
    env.flask_app.config['MAX_POST_OBJECT_COUNT'] = 3
    path_vehicle = f"/api/v1/reference-book/companies/{env.default_company.id}/vehicles"
    resp = local_post(env.client,
                      path_vehicle,
                      headers=env.user_auth_headers,
                      data=[VEHICLE_ALL_FIELDS] * 4,
                      expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert 'The number of objects must not exceed' in resp['message']


@skip_if_remote
def test_courier_vehicle_link(env: Environment):
    path_vehicle = f"/api/v1/reference-book/companies/{env.default_company.id}/vehicles"
    local_post(env.client,
               path_vehicle,
               headers=env.user_auth_headers,
               data=[VEHICLE_ALL_FIELDS])
    resp = local_get(env.client,
                     path_vehicle,
                     headers=env.user_auth_headers)
    vehicle_id = resp[0]['id']

    path_courier = f"/api/v1/reference-book/companies/{env.default_company.id}/couriers"
    local_post(env.client,
               path_courier,
               headers=env.user_auth_headers,
               data=[COURIER_ALL_FIELDS])

    resp = local_get(env.client,
                     path_courier,
                     headers=env.user_auth_headers)
    courier_id = resp[0]["id"]

    path_make_link = f"/api/v1/reference-book/companies/{env.default_company.id}/link-courier-vehicle"
    local_post(env.client,
               path_make_link,
               headers=env.user_auth_headers,
               data={"vehicle_id": vehicle_id, "courier_id": courier_id})

    path_get_link = f"/api/v1/reference-book/companies/{env.default_company.id}/vehicles/linked-couriers?vehicle_ids={vehicle_id}"
    resp = local_get(env.client,
                     path_get_link,
                     headers=env.user_auth_headers)
    assert resp[0]["vehicle_id"] == vehicle_id
    assert resp[0]["couriers"][0] == {**COURIER_ALL_FIELDS, 'id': courier_id}

    local_delete(env.client,
                 path_make_link,
                 headers=env.user_auth_headers,
                 data={"vehicle_id": vehicle_id, "courier_id": courier_id})

    resp = local_get(env.client,
                     path_get_link,
                     headers=env.user_auth_headers)
    assert resp[0]["vehicle_id"] == vehicle_id
    assert resp[0]["couriers"] == []
