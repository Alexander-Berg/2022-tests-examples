from http import HTTPStatus

from maps.b2bgeo.reference_book.lib.models import ZoneVehicle, db

TEST_ZONE_VEHICLE_REQUEST_SIZE = 101


def _get_zone_vehicle_table_size():
    return db.session.query(ZoneVehicle).count()


def _add_zones_and_vehicles(local_request, company_id):
    path_vehicle = f'/api/v1/reference-book/companies/{company_id}/vehicles'
    vehicle_data = [
        {'name': 'Some vehicle 1', 'number': 'vehicle_id1', 'routing_mode': 'driving'},
        {'name': 'Some vehicle 2', 'number': 'vehicle_id2', 'routing_mode': 'driving'},
    ]
    local_request('POST', path_vehicle, data=vehicle_data)
    vehicles = local_request('GET', path_vehicle)

    path_zone = f'/api/v1/reference-book/companies/{company_id}/zones'
    zone_data = [
        {
            'number': 'zone 1',
            'polygon': {'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], 'type': 'Polygon'},
        },
        {
            'number': 'zone 2',
            'polygon': {'coordinates': [[[38, 56], [39, 56], [39, 57], [38, 57], [38, 56]]], 'type': 'Polygon'},
        },
    ]
    local_request('POST', path_zone, data=zone_data)
    zones = local_request('GET', path_zone)

    return vehicles, zones


def test_pagination(local_request, company_id):
    vehicles, zones = _add_zones_and_vehicles(local_request, company_id)
    path_zone_vehicle = f'/api/v1/companies/{company_id}/zone-vehicles'
    query = {
        'allowed': [
            {'zone': str(zones[0]['id']), 'vehicle': str(vehicles[0]['id'])},
            {'zone': str(zones[1]['id']), 'vehicle': str(vehicles[0]['id'])},
        ],
        'forbidden': [
            {'zone': str(zones[0]['id']), 'vehicle': str(vehicles[1]['id'])},
        ],
    }
    local_request('POST', path_zone_vehicle, data=query, expected_status=HTTPStatus.OK)

    assert local_request('GET', f'{path_zone_vehicle}?vehicle_ids={vehicles[0]["id"]}&per_page=1') == {
        'allowed': [{'zone': str(zones[0]['id']), 'vehicle': str(vehicles[0]['id'])}],
        'forbidden': [],
    }
    assert local_request(
        'GET',
        f'{path_zone_vehicle}?vehicle_ids={vehicles[0]["id"]}&per_page=1&page=2',
    ) == {'allowed': [{'zone': str(zones[1]['id']), 'vehicle': str(vehicles[0]['id'])}], 'forbidden': []}
    assert local_request('GET', f'{path_zone_vehicle}?vehicle_ids={vehicles[0]["id"]}&per_page=1&page=3') == {
        'allowed': [],
        'forbidden': [],
    }

    assert local_request('GET', f'{path_zone_vehicle}?zone_ids={zones[0]["id"]}&per_page=3') == {
        'allowed': [{'zone': str(zones[0]['id']), 'vehicle': str(vehicles[0]['id'])}],
        'forbidden': [{'zone': str(zones[0]['id']), 'vehicle': str(vehicles[1]['id'])}],
    }


def test_limit(local_request, company_id):
    path_zone_vehicle = f'/api/v1/companies/{company_id}/zone-vehicles'

    object_ids = ','.join(str(i) for i in range(101))
    local_request(
        'GET', f'{path_zone_vehicle}?vehicle_ids={object_ids}', expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
    )
    local_request('GET', f'{path_zone_vehicle}?zone_ids={object_ids}', expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


def test_post_relation_with_public_zone(local_request, company_id):
    vehicles, _ = _add_zones_and_vehicles(local_request, company_id)
    path_zone = '/api/v1/reference-book/public/zones'
    path_zone_vehicle = f'/api/v1/companies/{company_id}/zone-vehicles'
    vehicle_id = str(vehicles[0]['id'])

    public_zones = local_request('GET', path_zone)
    assert public_zones
    public_zone_id = public_zones[0]['id']

    query = {'allowed': [{'zone': public_zone_id, 'vehicle': vehicle_id}]}
    local_request('POST', path_zone_vehicle, data=query, expected_status=HTTPStatus.OK)

    assert local_request('GET', f'{path_zone_vehicle}?zone_ids={public_zone_id}') == dict({'forbidden': []}, **query)
