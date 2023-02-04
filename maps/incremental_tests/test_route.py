import random

import pytest
import requests

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.config import PAGINATION_PAGE_SIZE
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    entity_equal, create_company_entity,
    env_patch_request, env_get_request, env_post_request, env_delete_request,
    api_path_with_company_id, verify_schema_validation_create_entity,
    verify_schema_validation_modify_entity,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import (
    UserKind,
    create_sharing_env,
    create_sharing_depots_with_orders)


URI = 'routes'
ID = None
COURIER_ID = None
DEPOT_ID = None

TEST_COURIER = {
    "name": "Flash",
    "number": "TEST_ROUTE_COURIER",
    "sms_enabled": False
}

TEST_DEPOT = {
    'number': 'TEST_ROUTE_DEPOT',
    'name': 'Склад 1',
    'address': 'ул. Льва Толстого, 16',
    'time_interval': '10:00 - 22:00',
    'lat': 55.7447,
    'lon': 37.6728,
    'description': 'курьерский подъезд',
    'service_duration_s': 600,
    'order_service_duration_s': 10
}

ENTITY = {
    'number': str(random.randint(1000, 10000)),
    'courier_number': 'TEST_ROUTE_COURIER',
    'depot_number': 'TEST_ROUTE_DEPOT',
    'date': '2017-07-22',
}

ENTITY_BAD_VALUES = {
    'date': 12,
    'imei': 'Some',
    'route_start': 100,
}

NEW_ENTITY = ENTITY.copy()

ENTITY2 = ENTITY.copy()
ENTITY2.update({'number': '23423-2000'})

EXTERNAL_FK = ['courier_number', 'depot_number']
INTERNAL_FK = ['courier_id', 'depot_id']


@skip_if_remote
class TestRoute(object):
    def test_prepare(self, system_env_with_db):
        create_company_entity(system_env_with_db, "couriers", TEST_COURIER)
        create_company_entity(system_env_with_db, "depots", TEST_DEPOT)
        global DEPOT_ID
        DEPOT_ID = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "depots?number={}".format(ENTITY['depot_number']))
        ).json()[0]['id']
        global COURIER_ID
        COURIER_ID = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers?number={}".format(ENTITY['courier_number']))
        ).json()[0]['id']

    def _get_route_id_by_number(self, system_env_with_db, route_number):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "routes?number={}".format(route_number))
        )
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert len(j) == 1
        return j[0]['id']

    def test_create_route_with_depot_number_and_courier_number(self, system_env_with_db):
        assert 'depot_id' not in ENTITY
        assert 'depot_number' in ENTITY
        assert 'courier_id' not in ENTITY
        assert 'courier_number' in ENTITY

        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, URI),
            data=ENTITY
        )
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert DEPOT_ID == j['depot_id']
        assert COURIER_ID == j['courier_id']

        route_id = self._get_route_id_by_number(system_env_with_db, ENTITY['number'])
        response_ = env_patch_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, route_id),
            data=ENTITY)
        assert response_.status_code == requests.codes.ok
        j = response_.json()
        assert DEPOT_ID == j['depot_id']
        assert COURIER_ID == j['courier_id']

        response = env_delete_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, route_id))
        j = response.json()
        print(j)
        assert response.status_code == requests.codes.ok

    def test_create_route_with_depot_id_and_courier_id(self, system_env_with_db):
        data = ENTITY.copy()
        del data['depot_number']
        data['depot_id'] = DEPOT_ID
        del data['courier_number']
        data['courier_id'] = COURIER_ID

        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, URI),
            data=data
        )
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert DEPOT_ID == j['depot_id']
        assert COURIER_ID == j['courier_id']

        route_id = self._get_route_id_by_number(system_env_with_db, data['number'])
        response_ = env_patch_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, route_id),
            data=ENTITY)
        assert response_.status_code == requests.codes.ok
        j = response_.json()
        assert DEPOT_ID == j['depot_id']
        assert COURIER_ID == j['courier_id']

        response = env_delete_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, route_id))
        j = response.json()
        print(j)
        assert response.status_code == requests.codes.ok

    def test_create_route_without_depot_id_and_depot_number_fail(self, system_env_with_db):
        data = ENTITY.copy()
        del data['depot_number']

        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, URI),
            data=data
        )
        assert response.status_code == requests.codes.unprocessable
        assert response.json()['message'] == 'Missing depot id'

        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, 'routes-batch'),
            data=[data]
        )
        assert response.status_code == requests.codes.unprocessable
        assert response.json()['message'] == 'Missing depot id'

    def test_wrong_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            '{}/{}-batch'.format(api_path_with_company_id(system_env_with_db), URI),
            data=ENTITY)
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.unprocessable

    def test_schema_create(self, system_env_with_db):
        verify_schema_validation_create_entity(
            system_env_with_db,
            type_name='routes',
            entity=ENTITY,
            required_fields=['date'],
            bad_fields_dict=ENTITY_BAD_VALUES)

    def test_batch(self, system_env_with_db):
        entity1 = ENTITY.copy()
        entity2 = ENTITY2.copy()
        response = env_post_request(
            system_env_with_db,
            '{}/{}-batch'.format(api_path_with_company_id(system_env_with_db), URI),
            data=[entity1, entity2])
        assert response.ok, response.text

        j = response.json()
        assert j['inserted'] + j['updated'] == 2

    def test_batch_duplicate(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            '{}/{}-batch'.format(api_path_with_company_id(system_env_with_db), URI),
            data=[ENTITY, ENTITY])
        assert response.status_code == requests.codes.unprocessable

    def test_list(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/{}'.format(api_path_with_company_id(system_env_with_db), URI))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert PAGINATION_PAGE_SIZE >= len(j) > 1

    def test_pagination_page_0(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/{}?page=0'.format(api_path_with_company_id(system_env_with_db), URI))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.unprocessable
        assert 'Must be greater than or equal to 1' in response.content.decode()

    def test_pagination_page_1(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/{}?page=1'.format(api_path_with_company_id(system_env_with_db), URI))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert PAGINATION_PAGE_SIZE >= len(j) > 1

    def test_pagination_page_1m(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/{}?page=1000000'.format(api_path_with_company_id(system_env_with_db), URI))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert len(j) == 0

    def test_route_by_number(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}?number={}'.format(URI, ENTITY['number']))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert len(j) == 1
        assert entity_equal(j[0], ENTITY)

    def test_route_by_number_fail(self, system_env_with_db):
        response = env_get_request(system_env_with_db, URI)
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.unprocessable
        assert 'Missing data for required field' in response.content.decode()

    def test_list_by_number(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/{}?number={}'.format(
                api_path_with_company_id(system_env_with_db),
                URI, ENTITY['number']))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert len(j) == 1
        assert entity_equal(j[0], ENTITY)

        global ID
        ID = j[0]['id']

        for key in INTERNAL_FK:
            ENTITY[key] = j[0][key]

        for key in EXTERNAL_FK:
            del ENTITY[key]

    def test_get(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, ID))
        j = response.json()
        print(j)

        global COURIER_ID
        COURIER_ID = j['courier_id']

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, ENTITY)

    def test_schema_modify(self, system_env_with_db):
        verify_schema_validation_modify_entity(
            system_env_with_db,
            type_name='routes',
            entity_id=ID,
            entity_number=ENTITY['number'],
            bad_fields_dict=ENTITY_BAD_VALUES)

    def test_get_by_courier(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            'couriers/{}/routes?date={}'.format(COURIER_ID, ENTITY['date']))
        j = response.json()

        assert response.status_code == requests.codes.ok
        assert j == []

    def test_get_by_courier_fail(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            f'couriers/{COURIER_ID}/routes?page=0')
        assert response.status_code == requests.codes.unprocessable
        assert 'Must be greater than or equal to 1' in response.content.decode()

        response = env_get_request(
            system_env_with_db,
            f'couriers/{COURIER_ID}/routes?date=24.06.2020')
        assert response.status_code == requests.codes.unprocessable
        assert 'Not a valid date ' in response.content.decode()

    def test_patch(self, system_env_with_db):
        response = env_patch_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, ID),
            data=NEW_ENTITY)
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert entity_equal(j, NEW_ENTITY)

    def test_delete(self, system_env_with_db):
        response = env_delete_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, ID))
        j = response.json()
        print(j)
        assert response.status_code == requests.codes.ok

        response = env_get_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, ID))
        j = response.json()
        print(j)
        assert response.status_code == requests.codes.not_found

    def test_post(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            '{}/{}'.format(api_path_with_company_id(system_env_with_db), URI),
            data=ENTITY)
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, ENTITY)

        global ID
        ID = j['id']

    def test_post_duplicate(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            '{}/{}'.format(api_path_with_company_id(system_env_with_db), URI),
            data=ENTITY)
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.unprocessable

    def test_imei(self, system_env_with_db):
        entity_with_imei = ENTITY.copy()
        entity_with_imei.update({
            'number': str(int(ENTITY['number']) + 1),
            'imei': 12345678901234567
        })

        response = env_post_request(
            system_env_with_db,
            '{}/{}'.format(api_path_with_company_id(system_env_with_db), URI),
            data=entity_with_imei)
        j = response.json()
        print(j)

        # Response will also contain other IMEI-related fields
        imei = entity_with_imei['imei']
        entity_with_imei['imei_str'] = str(imei)

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, entity_with_imei)

        response = env_get_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, j['id']))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, entity_with_imei)

    def test_imei_str(self, system_env_with_db):
        imei = 12345678901234567
        entity_with_imei = ENTITY.copy()
        entity_with_imei.update({
            'number': str(int(ENTITY['number']) + 2),
            'imei_str': str(imei)
        })

        response = env_post_request(
            system_env_with_db,
            '{}/{}'.format(api_path_with_company_id(system_env_with_db), URI),
            data=entity_with_imei)
        j = response.json()
        print(j)

        # Response will contain both IMEI representations
        entity_with_imei['imei'] = imei
        entity_with_imei['imei_str'] = str(imei)

        assert response.status_code == requests.codes.ok, response.text
        assert entity_equal(j, entity_with_imei)

        # Test patching route with another IMEI
        another_imei = imei + 1
        del entity_with_imei['imei']
        entity_with_imei['imei_str'] = str(another_imei)

        response = env_patch_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, j['id']),
            data=entity_with_imei)
        j = response.json()
        print(j)

        entity_with_imei['imei'] = another_imei
        entity_with_imei['imei_str'] = str(another_imei)

        assert response.status_code == requests.codes.ok, response.text
        assert entity_equal(j, entity_with_imei)

        # Test nullable imei_str
        del entity_with_imei['imei']
        entity_with_imei['imei_str'] = None

        response = env_patch_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, j['id']),
            data=entity_with_imei)
        j = response.json()

        entity_with_imei['imei'] = None
        entity_with_imei['imei_str'] = None

        assert response.status_code == requests.codes.ok, response.text
        assert entity_equal(j, entity_with_imei)

    def test_imei_str_routes_batch(self, system_env_with_db):
        imei = 12345678901234567
        entity_with_imei = ENTITY.copy()
        entity_with_imei.update({
            'number': str(int(ENTITY['number']) + 3),
            'imei_str': str(imei)
        })

        # Prepare a route which we will patch later
        response = env_post_request(
            system_env_with_db,
            '{}/{}'.format(api_path_with_company_id(system_env_with_db), URI),
            data=entity_with_imei)
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok, response.text

        # Patch the route's IMEI using routes-batch method
        another_imei = imei + 1
        entity_with_imei['imei_str'] = str(another_imei)

        response = env_post_request(
            system_env_with_db,
            '{}/routes-batch'.format(api_path_with_company_id(system_env_with_db)),
            data=[entity_with_imei])
        assert response.status_code == requests.codes.ok

        entity_with_imei['imei'] = another_imei
        entity_with_imei['imei_str'] = str(another_imei)

        response = env_get_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, j['id']))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, entity_with_imei)

    def test_cleanup(self, system_env_with_db):
        response = env_delete_request(
            system_env_with_db,
            '{}/{}/{}'.format(api_path_with_company_id(system_env_with_db), URI, ID))
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.ok


@pytest.fixture()
def _env_with_2_companies_sharing_setup(system_env_with_db):
    with create_sharing_env(system_env_with_db, 'sharing_depots_', 2) as sharing_env:

        create_sharing_depots_with_orders(sharing_env, company_idx=1, depot_infos=[{
            'name': '0',
            'routes': [
                {
                    'orders': [
                        {'share_with': []},
                        {'share_with': []}
                    ]
                },
                {
                    'orders': [
                        {'share_with': []},
                        {'share_with': []}
                    ]
                },
            ]
        }])

        yield sharing_env


@skip_if_remote
def test_delete_multiple_routes_access(_env_with_2_companies_sharing_setup):
    sharing_env = _env_with_2_companies_sharing_setup
    system_env_with_db = sharing_env['dbenv']
    routes1 = sharing_env["companies"][1]["all_routes"]
    assert len(routes1) == 2

    # users from other companies can not delete route
    for user_kind in UserKind:
        response = env_delete_request(
            system_env_with_db,
            f'{api_path_with_company_id(system_env_with_db, company_id=sharing_env["companies"][0]["id"])}/routes',
            data=[routes1[0]['id']],
            auth=system_env_with_db.get_user_auth(sharing_env['companies'][0]['users'][user_kind]))
        if user_kind in [UserKind.admin, UserKind.trusted_manager, UserKind.manager]:
            assert response.status_code == requests.codes.unprocessable_entity
            assert "Specified route id" in response.text and "does not exist" in response.text
        else:
            assert response.status_code == requests.codes.forbidden
