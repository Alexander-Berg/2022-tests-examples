# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import json

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_properties,
)

from balance import mapper
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import (
    create_client,
    create_client_with_intercompany,
    create_agency,
)

from yb_snout_api.resources.assessor.person import enums


@pytest.mark.smoke
class TestCreatePerson(TestCaseApiAppBase):
    BASE_API = u'/assessor/person/create'
    PERSON_PARAMS = '{"fname": "Иван", "lname": "Иванов", "mname": "Иванович", "phone": "+7 905 1234567", "email": "testagpi2@yandex.ru"}'

    def test_create_person(self, client):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'client_id': client.id,
                'person_type': enums.PersonType.ph.name,
                'data': self.PERSON_PARAMS,

            },
            is_admin=False,
        )
        person = self.test_session.query(mapper.Person).getone(response.get_json()['data']['person_id'])

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(person, has_properties(json.loads(self.PERSON_PARAMS)))
