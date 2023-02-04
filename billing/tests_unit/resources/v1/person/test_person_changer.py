# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import (
    constants as cst,
)
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
from yb_snout_api.tests_unit.fixtures.common import not_existing_id


@pytest.fixture(name='edit_person_role')
def create_edit_person_role():
    return create_role(
        (cst.PermissionCode.EDIT_PERSONS, {cst.ConstraintTypes.client_batch_id: None}),
    )


class TestPersonChanger(TestCaseApiAppBase):
    BASE_API = '/v1/person/changer'

    @pytest.mark.smoke
    def test_get_by_person_id(self, person):
        res = self.test_client.get(
            self.BASE_API,
            {'person_id': person.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'id': person.id,
                'client_id': person.client_id,
                'type': 'ph',
            }),
        )

    def test_person_id_not_found(self):
        person_id = not_existing_id(ob.PersonBuilder)
        res = self.test_client.get(
            self.BASE_API,
            {'person_id': person_id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PERSON_NOT_FOUND',
                'description': 'Person with ID %s not found in DB' % person_id,
            }),
        )

    def test_not_enough_arguments(self, client):
        res = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '?': hm.contains(hm.has_entries({'error': 'PERSON_TYPE_REQUIRED_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    def test_client_id_not_found(self):
        client_id = not_existing_id(ob.ClientBuilder)
        res = self.test_client.get(
            self.BASE_API,
            {
                'client_id': client_id,
                'person_type': 'ph',
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'CLIENT_NOT_FOUND',
                'description': 'Client with ID %s not found in DB' % client_id,
            }),
        )

    def test_invalid_person_type(self, client):
        res = self.test_client.get(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_type': 'abc',
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'person_type': hm.contains(hm.has_entries({'error': 'INVALID_PERSON_TYPE_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'client_type',
        ['client', 'repr_client', 'no_client'],
    )
    def test_own_client(self, client, client_type):
        security.set_roles([])

        if client_type == 'client':
            security.set_passport_client(client)
        elif client_type == 'repr_client':
            ob.set_repr_client(
                self.test_session,
                self.test_session.passport,
                client,
            )

        res = self.test_client.get(
            self.BASE_API,
            {'person_type': 'ph'},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if client_type != 'no_client' else http.NOT_FOUND))

        if client_type != 'no_client':
            hm.assert_that(
                res.get_json()['data'],
                hm.has_entries({
                    'id': None,
                    'type': 'ph',
                    'client_id': None,
                }),
            )
        else:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'CLIENT_NOT_FOUND',
                    'description': 'Client with ID -1 not found in DB',
                }),
            )

    @pytest.mark.smoke
    @pytest.mark.parametrize(
        'is_partner',
        [True, False],
    )
    @pytest.mark.parametrize(
        'person_type',
        ['ph', 'ur'],
    )
    def test_client(self, client, is_partner, person_type):
        res = self.test_client.get(
            self.BASE_API,
            {
                'client_id': client.id,
                'person_type': person_type,
                'is_partner': is_partner,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'id': None,
                'client_id': None,
                'type': person_type,
                'is_partner': is_partner,
            }),
        )


@pytest.mark.permissions
class TestPersonChangerPermission(TestCaseApiAppBase):
    BASE_API = '/v1/person/changer'

    @pytest.mark.parametrize(
        'match_client',
        [
            pytest.param(None, id='wo role'),
            pytest.param(True, id='right client'),
            pytest.param(False, id='wrong client'),
        ],
    )
    @pytest.mark.parametrize(
        'req_type',
        [
            pytest.param(True, id='w person'),
            pytest.param(False, id='w client'),
        ],
    )
    def test_role_w_client(self, client, admin_role, edit_person_role, match_client, req_type):
        roles = [admin_role]
        if match_client is not None:
            client_batch_id = create_role_client(client if match_client else None).client_batch_id
            roles.append(
                (edit_person_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
            )
        security.set_roles(roles)

        if req_type:
            person = create_person(client=client, type='ph')
            params = {'person_id': person.id}
        else:
            params = {'client_id': client.id, 'person_type': 'ph', 'is_partner': False}

        res = self.test_client.get(self.BASE_API, params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

        if match_client:
            hm.assert_that(
                res.get_json()['data'],
                hm.has_entries({
                    'id': person.id if req_type else None,
                    'client_id': client.id if req_type else None,
                    'type': 'ph',
                }),
            )

    @pytest.mark.parametrize(
        'match_client',
        [True, False],
    )
    @pytest.mark.parametrize(
        'req_type',
        [
            pytest.param(True, id='w person'),
            pytest.param(False, id='w client'),
        ],
    )
    def test_client_ui(self, client, match_client, req_type):
        security.set_roles([])
        if match_client:
            security.set_passport_client(client)

        if req_type:
            person = create_person(client=client, type='ph')
            params = {'person_id': person.id}
        else:
            params = {'client_id': client.id, 'person_type': 'ph', 'is_partner': False}

        res = self.test_client.get(self.BASE_API, params, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

        if match_client:
            hm.assert_that(
                res.get_json()['data'],
                hm.has_entries({
                    'id': person.id if req_type else None,
                    'client_id': client.id if req_type else None,
                    'type': 'ph',
                }),
            )
