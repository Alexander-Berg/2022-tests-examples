from http import HTTPStatus

from ya_courier_backend.models import ZoneVehicle, db

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_get, local_post, local_delete


TEST_ZONE_VEHICLE_REQUEST_SIZE = 101


def _get_zone_vehicle_table_size(env):
    with env.flask_app.app_context():
        return db.session.query(ZoneVehicle).count()


def _add_zones_and_vehicles(env):
    path_vehicle = f"/api/v1/reference-book/companies/{env.default_company.id}/vehicles"
    vehicle_data = [{"name": "Some vehicle 1", "number": "vehicle_id1", "routing_mode": "driving"},
                    {"name": "Some vehicle 2", "number": "vehicle_id2", "routing_mode": "driving"}]
    local_post(env.client, path_vehicle, headers=env.user_auth_headers, data=vehicle_data)
    vehicles = local_get(env.client, path_vehicle, headers=env.user_auth_headers)

    path_zone = f"/api/v1/reference-book/companies/{env.default_company.id}/zones"
    zone_data = [{"number": "zone 1", "polygon": {"coordinates": [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], "type": "Polygon"}},
                 {"number": "zone 2", "polygon": {"coordinates": [[[38, 56], [39, 56], [39, 57], [38, 57], [38, 56]]], "type": "Polygon"}}]
    local_post(env.client, path_zone, headers=env.user_auth_headers, data=zone_data)
    zones = local_get(env.client, path_zone, headers=env.user_auth_headers)

    return vehicles, zones


@skip_if_remote
def test_zone_vehicle_format(env):
    vehicles, zones = _add_zones_and_vehicles(env)
    vehicle = vehicles[0]
    vehicle['id'] = str(vehicle['id'])
    zone = zones[0]

    path_zone_vehicle = f'/api/v1/companies/{env.default_company.id}/zone-vehicles'
    data = {'allowed': [{'zone': zone['id'], 'vehicle': vehicle['id']}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=data, expected_status=HTTPStatus.OK)

    local_get(env.client, path_zone_vehicle, headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    resp_data = dict({"forbidden": []}, **data)
    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zone['id']}", headers=env.user_auth_headers) == resp_data
    assert local_get(env.client, f"{path_zone_vehicle}?vehicle_ids={vehicle['id']}", headers=env.user_auth_headers) == resp_data
    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zone['id']}&vehicle_ids={vehicle['id']}", headers=env.user_auth_headers) == resp_data


@skip_if_remote
def test_zone_vehicle(env):
    vehicles, zones = _add_zones_and_vehicles(env)
    vehicle = vehicles[0]
    vehicle['id'] = str(vehicle['id'])
    zone = zones[0]

    path_zone_vehicle = f'/api/v1/companies/{env.default_company.id}/zone-vehicles'

    query = {'allowed': [{'zone': zone['id'], 'vehicle': '337'}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.NOT_FOUND)

    query = {'allowed': [{'zone': zone['id'], 'vehicle': 'incorrect_id'}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.NOT_FOUND)

    query = {'allowed': [{'zone': '337', 'vehicle': vehicle['id']}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.NOT_FOUND)

    query = {'allowed': [{'zone': zone['id'], 'vehicle': vehicle['id']}] * TEST_ZONE_VEHICLE_REQUEST_SIZE}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zone['id']}", headers=env.user_auth_headers) \
        == {"allowed": [], "forbidden": []}

    query = {'allowed': [{'zone': zone['id'], 'vehicle': vehicle['id']}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.OK)
    assert _get_zone_vehicle_table_size(env) == 1
    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zone['id']}", headers=env.user_auth_headers) \
        == dict({"forbidden": []}, **query)

    query = {'allowed': [{'zone': zone['id'], 'vehicle': vehicle['id']}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.OK)
    assert _get_zone_vehicle_table_size(env) == 1
    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zone['id']}", headers=env.user_auth_headers) \
        == dict({"forbidden": []}, **query)

    query = {'forbidden': [{'zone': zone['id'], 'vehicle': vehicle['id']}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.OK)
    assert _get_zone_vehicle_table_size(env) == 1
    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zone['id']}", headers=env.user_auth_headers) \
        == dict({"allowed": []}, **query)

    query = {'allowed': [{'zone': zone['id'], 'vehicle': vehicle['id']}],
             'forbidden': [{'zone': zone['id'], 'vehicle': vehicle['id']}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert _get_zone_vehicle_table_size(env) == 1
    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zone['id']}", headers=env.user_auth_headers) \
        == dict({"allowed": []}, forbidden=query["forbidden"])

    query = [{'zone': zone['id'], 'vehicle': vehicle['id']}]
    local_delete(env.client, path_zone_vehicle,
                 headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.OK)
    assert _get_zone_vehicle_table_size(env) == 0
    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zone['id']}", headers=env.user_auth_headers) \
        == {"allowed": [], "forbidden": []}

    query = [{'zone': zone['id'], 'vehicle': vehicle['id']}]
    local_delete(env.client, path_zone_vehicle,
                 headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert _get_zone_vehicle_table_size(env) == 0


@skip_if_remote
def test_pagination(env):
    vehicles, zones = _add_zones_and_vehicles(env)
    path_zone_vehicle = f'/api/v1/companies/{env.default_company.id}/zone-vehicles'

    query = {
        'allowed': [
            {'zone': str(zones[0]['id']), 'vehicle': str(vehicles[0]['id'])},
            {'zone': str(zones[1]['id']), 'vehicle': str(vehicles[0]['id'])},
        ],
        'forbidden': [
            {'zone': str(zones[0]['id']), 'vehicle': str(vehicles[1]['id'])},
        ]
    }
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.OK)

    assert local_get(env.client, f"{path_zone_vehicle}?vehicle_ids={vehicles[0]['id']}&per_page=1", headers=env.user_auth_headers) \
        == {'allowed': [{'zone': str(zones[0]['id']), 'vehicle': str(vehicles[0]['id'])}], 'forbidden': []}
    assert local_get(env.client, f"{path_zone_vehicle}?vehicle_ids={vehicles[0]['id']}&per_page=1&page=2", headers=env.user_auth_headers) \
        == {'allowed': [{'zone': str(zones[1]['id']), 'vehicle': str(vehicles[0]['id'])}], 'forbidden': []}
    assert local_get(env.client, f"{path_zone_vehicle}?vehicle_ids={vehicles[0]['id']}&per_page=1&page=3", headers=env.user_auth_headers) \
        == {'allowed': [], 'forbidden': []}

    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={zones[0]['id']}&per_page=3", headers=env.user_auth_headers) \
        == {'allowed': [{'zone': str(zones[0]['id']), 'vehicle': str(vehicles[0]['id'])}],
            'forbidden': [{'zone': str(zones[0]['id']), 'vehicle': str(vehicles[1]['id'])}]}


@skip_if_remote
def test_limit(env):
    path_zone_vehicle = f'/api/v1/companies/{env.default_company.id}/zone-vehicles'

    object_ids = ",". join(str(i) for i in range(101))
    local_get(env.client, f"{path_zone_vehicle}?vehicle_ids={object_ids}", headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    local_get(env.client, f"{path_zone_vehicle}?zone_ids={object_ids}", headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_post_relation_with_public_zone(env):
    vehicles, _ = _add_zones_and_vehicles(env)
    path_zone = "/api/v1/reference-book/public/zones"
    path_zone_vehicle = f'/api/v1/companies/{env.default_company.id}/zone-vehicles'
    vehicle_id = str(vehicles[0]['id'])

    public_zones = local_get(env.client, path_zone, headers=env.user_auth_headers)
    assert public_zones
    public_zone_id = public_zones[0]['id']

    query = {'allowed': [{'zone': public_zone_id, 'vehicle': vehicle_id}]}
    local_post(env.client, path_zone_vehicle,
               headers=env.user_auth_headers, data=query, expected_status=HTTPStatus.OK)

    assert local_get(env.client, f"{path_zone_vehicle}?zone_ids={public_zone_id}", headers=env.user_auth_headers) \
        == dict({"forbidden": []}, **query)
