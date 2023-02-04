from copy import deepcopy
from http import HTTPStatus

COURIER_ALL_FIELDS = {'name': 'Some courier', 'number': 'some number', 'phone': '+79999999999'}

COURIER_ONLY_REQUIRED_FIELDS = {'number': 'some other number', 'phone': '+79999999999'}

VEHICLE_ALL_FIELDS = {
    'name': 'Some vehicle',
    'number': 'a111aa750',
    'routing_mode': 'driving',
    'parameters': {
        'max_weight': 2000.0,
        'width': 2.0,
        'height': 1.5,
        'length': 4.5,
    },
    'capacity': {
        'width': 1.5,
        'height': 0.5,
        'depth': 2.0,
        'weight': 500.0,
        'units': 0.0,
    },
    'cost': {
        'fixed': 2000.0,
        'hour': 0.0,
        'km': 15.0,
        'location': 0.0,
        'run': 0.5,
        'tonne_km': 150.0,
    },
    'garage': {
        'start': {
            'lat': 55.66206,
            'lon': 37.556774,
            'address': 'Leo Tolstoy Str, 16',
        },
        'end': {
            'lat': 55.66206,
            'lon': 37.556774,
            'address': 'Leo Tolstoy Str, 16',
        },
    },
    'imei': '12345',
    'start_from_depot': False,
    'return_to_depot': False,
    'max_runs': 1,
    'tags': ['tag1', 'tag2'],
    'shifts': [
        {
            'time_window': {
                'start': '07:00:00',
                'end': '19:00:00',
            },
            'hard_window': True,
            'max_duration_s': 6000.0,
            'maximal_stops': 10,
            'minimal_stops': 1,
            'penalty': {
                'stop_excess': {
                    'per_stop': 100.0,
                },
                'stop_lack': {
                    'per_stop': 100.0,
                },
            },
            'balanced_group_id': 'test_group',
        },
    ],
}

VEHICLE_ONLY_REQUIRED_FIELDS = {'name': 'Some vehicle', 'number': 'a111aa751', 'routing_mode': 'driving'}


def test_courier_reference(local_request, company_id):
    path_courier = f'/api/v1/reference-book/companies/{company_id}/couriers'

    local_request('POST', path_courier, data=[COURIER_ALL_FIELDS])
    courier = deepcopy(COURIER_ALL_FIELDS)

    resp = local_request('GET', path_courier)
    courier['id'] = resp[0]['id']
    assert resp[0] == courier

    path_courier_with_id = f'/api/v1/reference-book/companies/{company_id}/couriers/{courier["id"]}'
    courier = deepcopy(COURIER_ALL_FIELDS)
    courier['name'] = 'Test courier'

    local_request('PATCH', path_courier_with_id, data=courier)

    resp = local_request('GET', path_courier_with_id)
    courier['id'] = resp['id']
    assert resp == courier

    resp = local_request('GET', path_courier)
    assert len(resp) == 1
    assert resp[0] == courier

    local_request('POST', path_courier, data=[COURIER_ONLY_REQUIRED_FIELDS])

    resp = local_request('GET', path_courier)
    assert len(resp) == 2

    local_request('DELETE', path_courier_with_id)

    resp = local_request('GET', path_courier)
    assert len(resp) == 1

    local_request(
        'POST',
        path_courier,
        data=[COURIER_ONLY_REQUIRED_FIELDS, COURIER_ALL_FIELDS],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )


def test_too_many_objects_couriers(app, local_request, company_id):
    app.config['MAX_POST_OBJECT_COUNT'] = 3
    path_courier = f'/api/v1/reference-book/companies/{company_id}/couriers'
    resp = local_request(
        'POST',
        path_courier,
        data=[COURIER_ALL_FIELDS] * 4,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert 'The number of objects must not exceed' in resp['message']


def test_vehicle(local_request, company_id):
    path_vehicle = f'/api/v1/reference-book/companies/{company_id}/vehicles'

    local_request('POST', path_vehicle, data=[VEHICLE_ALL_FIELDS])
    vehicle = deepcopy(VEHICLE_ALL_FIELDS)

    resp = local_request('GET', path_vehicle)
    vehicle['id'] = resp[0]['id']
    assert len(resp) == 1
    assert resp[0] == vehicle

    path_vehicle_with_id = f'/api/v1/reference-book/companies/{company_id}/vehicles/{vehicle["id"]}'
    vehicle_patch = deepcopy(VEHICLE_ALL_FIELDS)
    vehicle_patch['shifts'] = [
        {
            'time_window': {'start': '08:00:00', 'end': '18:00:00'},
            'hard_window': False,
            'max_duration_s': 12000.0,
            'maximal_stops': 5,
            'minimal_stops': 2,
            'balanced_group_id': 'some_other_test_group',
            'penalty': {
                'stop_excess': {
                    'per_stop': 10.0,
                },
                'stop_lack': {
                    'per_stop': 10.0,
                },
            },
        },
    ]
    local_request('PATCH', path_vehicle_with_id, data=vehicle_patch)
    vehicle_patch['id'] = vehicle['id']
    resp = local_request('GET', path_vehicle_with_id)
    assert resp == vehicle_patch

    local_request('POST', path_vehicle, data=[VEHICLE_ONLY_REQUIRED_FIELDS])

    resp = local_request('GET', path_vehicle)
    assert len(resp) == 2

    local_request('DELETE', path_vehicle_with_id)

    resp = local_request('GET', path_vehicle)
    assert len(resp) == 1

    local_request(
        'POST',
        path_vehicle,
        data=[VEHICLE_ALL_FIELDS, VEHICLE_ONLY_REQUIRED_FIELDS],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )


def test_too_many_objects_vehicle(app, local_request, company_id):
    app.config['MAX_POST_OBJECT_COUNT'] = 3
    path_vehicle = f'/api/v1/reference-book/companies/{company_id}/vehicles'
    resp = local_request(
        'POST', path_vehicle, data=[VEHICLE_ALL_FIELDS] * 4, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
    )
    assert 'The number of objects must not exceed' in resp['message']


def test_courier_vehicle_link(local_request, company_id):
    path_vehicle = f'/api/v1/reference-book/companies/{company_id}/vehicles'
    local_request('POST', path_vehicle, data=[VEHICLE_ALL_FIELDS])
    resp = local_request('GET', path_vehicle)
    vehicle_id = resp[0]['id']

    path_courier = f'/api/v1/reference-book/companies/{company_id}/couriers'
    local_request('POST', path_courier, data=[COURIER_ALL_FIELDS])

    resp = local_request('GET', path_courier)
    courier_id = resp[0]['id']

    path_make_link = f'/api/v1/reference-book/companies/{company_id}/link-courier-vehicle'
    local_request('POST', path_make_link, data={'vehicle_id': vehicle_id, 'courier_id': courier_id})

    path_get_link = f'/api/v1/reference-book/companies/{company_id}/vehicles/linked-couriers?vehicle_ids={vehicle_id}'
    resp = local_request('GET', path_get_link)
    assert resp[0]['vehicle_id'] == vehicle_id
    assert resp[0]['couriers'][0] == {**COURIER_ALL_FIELDS, 'id': courier_id}

    local_request(
        'DELETE',
        path_make_link,
        data={'vehicle_id': vehicle_id, 'courier_id': courier_id},
    )

    resp = local_request('GET', path_get_link)
    assert resp[0]['vehicle_id'] == vehicle_id
    assert resp[0]['couriers'] == []
