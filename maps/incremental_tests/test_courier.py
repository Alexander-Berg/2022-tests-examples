import copy
import json
import random

import requests

from maps.b2bgeo.ya_courier.backend.test_lib.config import PAGINATION_PAGE_SIZE
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    entity_equal, env_delete_request,
    env_patch_request, env_get_request, env_post_request,
    api_path_with_company_id,
    verify_schema_validation_create_entity,
    verify_schema_validation_modify_entity,
)

ID = None
ENTITY = {
    'number': f'TEST_COURIER_{str(random.randint(1000, 1000000))}',
    'name': 'Ваня',
    'phone': '+71234524423',
    "sms_enabled": True
}

NEW_ENTITY = {
    'number': ENTITY['number'],
    'phone': '+71234524424',
    'name': 'Иван',
    'sms_enabled': False
}

ENTITY2 = {
    'number': 'TEST_COURIER_COURIER',
    'phone': '+71234553425',
    'name': 'Олеся'
}

ENTITY_BAD_VALUES = {
    "name": ["Igor"],
    "phone": {},
    "sms_enabled": 1
}


class TestCourier(object):
    def test_wrong_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers-batch"),
            data=ENTITY
        )

        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_schema_create(self, system_env_with_db):
        verify_schema_validation_create_entity(
            system_env_with_db,
            type_name='couriers',
            entity=ENTITY,
            required_fields=[],
            bad_fields_dict=ENTITY_BAD_VALUES)

    def test_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers-batch"),
            data=[ENTITY, ENTITY2]
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert j['inserted'] + j['updated'] == 2

    def test_list(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert PAGINATION_PAGE_SIZE >= len(j) > 1

    def test_pagination_page_0(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers?page=0")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_pagination_page_1(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers?page=1")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert PAGINATION_PAGE_SIZE >= len(j) > 1

    def test_pagination_page_1m(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers?page=1000000")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert len(j) == 0

    def test_get_by_number(self, system_env_with_db):
        response = env_get_request(system_env_with_db, "couriers")
        assert response.status_code == requests.codes.unprocessable
        assert "Missing data for required field" in response.content.decode()

        response = env_get_request(system_env_with_db, f"couriers?number={ENTITY['number']}")
        assert response.status_code == requests.codes.ok
        j = response.json()
        assert len(j) == 1

    def test_courier_by_number(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "couriers?number={}".format(ENTITY['number'])
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert len(j) == 1
        assert entity_equal(j[0], ENTITY)

    def test_list_by_number(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "couriers?number={}".format(ENTITY['number'])
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert len(j) == 1
        assert entity_equal(j[0], ENTITY)

        global ID
        ID = j[0]['id']

        assert entity_equal(j[0], ENTITY)

    def test_get(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "couriers/{}".format(ID)
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, ENTITY)

    def test_schema_modify(self, system_env_with_db):
        verify_schema_validation_modify_entity(
            system_env_with_db,
            type_name='couriers',
            entity_id=ID,
            entity_number=ENTITY['number'],
            bad_fields_dict=ENTITY_BAD_VALUES)

    def test_patch(self, system_env_with_db):
        response = env_patch_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers/{}".format(ID)),
            data=NEW_ENTITY
        )

        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, NEW_ENTITY)

    def test_delete(self, system_env_with_db):
        response = env_delete_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers/{}".format(ID))
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "couriers/{}".format(ID)
            )
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.not_found

    def test_post(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers"),
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
            api_path_with_company_id(system_env_with_db, "couriers"),
            data=ENTITY
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_post_without_number(self, system_env_with_db):
        entity_without_number = copy.copy(ENTITY)
        del entity_without_number["number"]
        response = env_post_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers"),
            data=entity_without_number
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_post_with_empty_number(self, system_env_with_db):
        entity_with_empty_number = copy.copy(ENTITY)
        entity_with_empty_number["number"] = ""
        response = env_post_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers"),
            data=entity_with_empty_number
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_cleanup(self, system_env_with_db):
        response = env_delete_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "couriers/{}".format(ID))
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
