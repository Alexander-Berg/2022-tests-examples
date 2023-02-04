import random

import pytest
import requests
import json

from maps.b2bgeo.ya_courier.backend.test_lib.config import PAGINATION_PAGE_SIZE
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    entity_equal, env_post_request, env_get_request,
    env_delete_request, env_patch_request, api_path_with_company_id,
    verify_schema_validation_create_entity,
    verify_schema_validation_modify_entity,
)

URI = 'depots'
ID = None
ENTITY = {
    'number': f'TEST_DEPOT_{str(random.randint(1000, 1000000))}',
    'name': 'Склад 1',
    'address': 'ул. Льва Толстого, 16',
    'time_interval': '10:00 - 22:00',
    'lat': 55.7447,
    'lon': 37.6728,
    'description': 'курьерский подъезд',
    'service_duration_s': 600,
    'order_service_duration_s': 10,
    'time_zone': "Europe/Moscow",
    'allow_route_editing': False,
    'mark_route_started_radius': 0
}

NEW_ENTITY = {
    'number': ENTITY['number'],
    'name': 'Склад 1а',
    'address': 'ул. Льва Толстого, 17',
    'time_interval': '11:00 - 23:00',
    'lat': 55.7448,
    'lon': 37.6729,
    'description': 'вход со двора',
    'service_duration_s': 700,
    'order_service_duration_s': 11,
    'time_zone': "Europe/Moscow",
    'allow_route_editing': True
}

ENTITY_BAD_VALUES = {
    'address': None,
    'allow_route_editing': 0,
    'description': ['string'],
    'lat': '0',
    'lon': True,
    'name': {},
    'order_service_duration_s': 0.001,
    'service_duration_s': 0.01,
    'time_interval': ['00:00', '01:00'],
    'time_zone': 9
}

ENTITY2 = {
    'number': '23453',
    'name': 'Склад 2',
    'address': 'ул. Льва Толстого, 18',
    'time_interval': '0.00:00-23:59',
    'lat': 55.7446,
    'lon': 37.6727,
    'description': 'курьерский вход',
    'service_duration_s': 500,
    'order_service_duration_s': 9,
    'time_zone': "Europe/Moscow"
}

ENTITY_TIMEZONE = {
    'number': str(random.randint(90000, 100000)),
    'name': 'Склад 3',
    'address': 'ул. Льва Толстого, 18',
    'time_interval': '0.00:00-23:59',
    'lat': 52.516667,
    'lon': 13.388889,
    'description': 'курьерский вход',
    'service_duration_s': 500,
    'order_service_duration_s': 9
}


def _get_depot_by_id(system_env_with_db, depot_id, expected_status_code=requests.codes.ok):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, 'depots/{}'.format(depot_id))
    )
    assert response.status_code == expected_status_code
    return response.json()


def _get_depot_by_number(system_env_with_db, depot_number):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, 'depots?number={}'.format(depot_number))
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    if len(j) == 1:
        assert j[0]['number'] == depot_number
        return j[0]
    assert len(j) == 0
    return None


def _add_depot(system_env_with_db, depot_data, use_batch):
    response = env_post_request(
        system_env_with_db,
        path=api_path_with_company_id(system_env_with_db, 'depots-batch' if use_batch else 'depots'),
        data=[depot_data] if use_batch else depot_data,
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    if use_batch:
        assert (j['inserted'], j['updated']) == (1, 0)
        j = _get_depot_by_number(system_env_with_db, depot_data['number'])
    assert j['number'] == depot_data['number']
    return j


def _modify_depot(system_env_with_db, depot_id, depot_data, use_batch, expected_status_code=requests.codes.ok):
    if use_batch:
        depot_data_with_number = depot_data.copy()
        depot_data_with_number['number'] = _get_depot_by_id(system_env_with_db, depot_id)['number']
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, 'depots-batch'),
            data=[depot_data_with_number],
        )
    else:
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, 'depots/{}'.format(depot_id)),
            data=depot_data
        )
    assert response.status_code == expected_status_code
    if response.status_code != requests.codes.ok:
        return None
    j = response.json()
    if use_batch:
        assert (j['inserted'], j['updated']) == (0, 1)
        j = _get_depot_by_id(system_env_with_db, depot_id)
    return j


def _delete_depot(system_env_with_db, depot_id):
    path = "depots/{}".format(depot_id)
    response = env_delete_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, path)
    )
    assert response.status_code == requests.codes.ok


class TestDepot(object):
    def test_wrong_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "depots-batch"),
            data=ENTITY
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_schema_create(self, system_env_with_db):
        verify_schema_validation_create_entity(
            system_env_with_db,
            type_name='depots',
            entity=ENTITY,
            required_fields=['address', 'lat', 'lon'],
            bad_fields_dict=ENTITY_BAD_VALUES)

    def test_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "depots-batch"),
            data=[ENTITY, ENTITY2],
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert j['inserted'] + j['updated'] == 2

    def test_list(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "depots")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert PAGINATION_PAGE_SIZE >= len(j) > 1

    def test_pagination_page_0(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "depots?page=0")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable
        assert 'Must be greater than or equal to 1' in response.content.decode()

    def test_pagination_page_1(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "depots?page=1")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert PAGINATION_PAGE_SIZE >= len(j) > 1

    def test_pagination_page_1m(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "depots?page=1000000")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert len(j) == 0

    def test_list_by_number(self, system_env_with_db):
        depot = _get_depot_by_number(system_env_with_db, ENTITY['number'])
        assert entity_equal(depot, ENTITY)

        global ID
        ID = depot['id']

    def test_get(self, system_env_with_db):
        depot = _get_depot_by_id(system_env_with_db, ID)
        assert entity_equal(depot, ENTITY)
        assert depot['allow_route_editing'] is False

    def test_schema_modify(self, system_env_with_db):
        verify_schema_validation_modify_entity(
            system_env_with_db,
            type_name='depots',
            entity_id=ID,
            entity_number=ENTITY['number'],
            bad_fields_dict=ENTITY_BAD_VALUES)

    @pytest.mark.parametrize('time_interval', ["0.00:00-1.23:59", "8-1.20", "8:00:00 - 20", "6:00 - 1.21:30:00"])
    def test_patch(self, system_env_with_db, time_interval):
        data = NEW_ENTITY.copy()
        data['time_interval'] = time_interval
        depot = _modify_depot(system_env_with_db, ID, data, use_batch=False)
        assert entity_equal(depot, data)

    def test_delete(self, system_env_with_db):
        _delete_depot(system_env_with_db, ID)
        _get_depot_by_id(system_env_with_db, ID, expected_status_code=requests.codes.not_found)

    @pytest.mark.parametrize('incorrect_time_interval', [None, "a-b", "2018-09-06T10:15:00+03:00/2018-09-06T12:45:00+03:00", "1.21:30:00 - 6:00"])
    def test_incorrect_time_interval(self, system_env_with_db, incorrect_time_interval):
        data = ENTITY.copy()
        data["time_interval"] = incorrect_time_interval
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "depots"),
            data=data
        )
        assert response.status_code == requests.codes.unprocessable

    @pytest.mark.parametrize('optional_field', ["description", "name", "order_service_duration_s", "service_duration_s", "time_interval",
                                                "time_zone", "allow_route_editing", "mark_route_started_radius"])
    def test_optional_fields_absence(self, system_env_with_db, optional_field):
        data = ENTITY.copy()
        del data[optional_field]
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "depots"),
            data=data
        )
        assert response.status_code == requests.codes.ok
        depot_id = response.json()['id']
        _delete_depot(system_env_with_db, depot_id)
        _get_depot_by_id(system_env_with_db, depot_id, expected_status_code=requests.codes.not_found)

    @pytest.mark.parametrize('correct_time_interval', ["0.00:00-1.23:59:59", "8-0.20", "8:00:00 - 20", "6:00 - 1.21:30:00"])
    def test_time_interval(self, system_env_with_db, correct_time_interval):
        data = ENTITY.copy()
        data["time_interval"] = correct_time_interval
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "depots"),
            data=data
        )
        j = response.json()
        _delete_depot(system_env_with_db, j['id'])
        _get_depot_by_id(system_env_with_db, j['id'], expected_status_code=requests.codes.not_found)

    def test_post(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "depots"),
            data=ENTITY
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, ENTITY)

        global ID
        ID = j['id']

    def test_post_duplicate(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "depots"),
            data=ENTITY
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_prepare_for_time_zone_testing(self, system_env_with_db):
        depot = _get_depot_by_number(system_env_with_db, ENTITY_TIMEZONE['number'])
        if depot:
            _delete_depot(system_env_with_db, depot['id'])

    @pytest.mark.parametrize('use_batch', [False, True])
    def test_implicit_time_zone(self, system_env_with_db, use_batch):
        assert 'time_zone' not in ENTITY_TIMEZONE
        depot = _add_depot(system_env_with_db, ENTITY_TIMEZONE, use_batch)
        assert depot['time_zone'] == 'Europe/Berlin'
        assert (depot['lat'], depot['lon']) == (ENTITY_TIMEZONE['lat'], ENTITY_TIMEZONE['lon'])

        depot = _modify_depot(system_env_with_db, depot['id'], {'lat': 43.115543, 'lon': 131.885483}, use_batch)
        assert depot['time_zone'] == 'Asia/Vladivostok'
        assert (depot['lat'], depot['lon']) == (43.115543, 131.885483)

        _modify_depot(system_env_with_db, depot['id'], {'lon': 141.4}, use_batch, expected_status_code=requests.codes.unprocessable)

        _delete_depot(system_env_with_db, depot['id'])

    @pytest.mark.parametrize('use_batch', [False, True])
    def test_explicit_time_zone(self, system_env_with_db, use_batch):
        data = ENTITY_TIMEZONE.copy()
        data['time_zone'] = 'Asia/Tehran'
        depot = _add_depot(system_env_with_db, data, use_batch)
        assert depot['time_zone'] == 'Asia/Tehran'

        depot = _modify_depot(system_env_with_db, depot['id'], {'time_zone': 'Asia/Vladivostok'}, use_batch)
        assert depot['time_zone'] == 'Asia/Vladivostok'
        assert (depot['lat'], depot['lon']) == (ENTITY_TIMEZONE['lat'], ENTITY_TIMEZONE['lon'])

        depot = _modify_depot(system_env_with_db, depot['id'], {'lat': 1.0, 'lon': 1.0, 'time_zone': 'Europe/Berlin'}, use_batch)
        assert depot['time_zone'] == 'Europe/Berlin'
        assert (depot['lat'], depot['lon']) == (1.0, 1.0)

        _modify_depot(system_env_with_db, depot['id'], {'lat': 77.0, 'time_zone': 'Asia/Tehran'}, use_batch, expected_status_code=requests.codes.unprocessable)

        _modify_depot(system_env_with_db, depot['id'], {'time_zone': 'Europe/BadCity'}, use_batch, expected_status_code=requests.codes.unprocessable)
        _modify_depot(system_env_with_db, depot['id'], {'time_zone': 'UTC+07'}, use_batch, expected_status_code=requests.codes.unprocessable)
        _modify_depot(system_env_with_db, depot['id'], {'time_zone': 5}, use_batch, expected_status_code=requests.codes.unprocessable)

        _delete_depot(system_env_with_db, depot['id'])

    def test_cleanup(self, system_env_with_db):
        _delete_depot(system_env_with_db, ID)
