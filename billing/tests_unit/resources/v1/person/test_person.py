# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm

from balance import constants as cst, mapper

from brest.core.tests import security
from tests import object_builder as ob
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
from yb_snout_api.tests_unit.fixtures.client import create_role_client, create_client
from yb_snout_api.tests_unit.fixtures.common import not_existing_id
from yb_snout_api.tests_unit.fixtures.permissions import create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.edo import create_edo_offer


@pytest.fixture(name='view_person_role')
def create_view_person_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_PERSONS,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
    )


class TestCasePerson(TestCaseApiAppBase):
    BASE_API = u'/v1/person'

    def test_not_found(self, admin_role):
        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API, {'person_id':  not_existing_id(ob.PersonBuilder)})
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND))

    @pytest.mark.parametrize('has_edo', [True, False])
    def test_has_edo(self, admin_role, view_person_role, client, edo_offer, has_edo):
        security.set_roles([admin_role, view_person_role])
        person = create_person(client)
        if has_edo:
            person.inn = edo_offer.person_inn
            person.kpp = edo_offer.person_kpp
        response = self.test_client.get(self.BASE_API, {'person_id': person.id})
        assert person.has_edo == has_edo
        hm.assert_that(response.get_json()['data'], hm.has_entry('has_edo', person.has_edo))

    def test_has_purchase_order(self, admin_role, view_person_role, client):
        security.set_roles([admin_role, view_person_role])
        purchase_order = 'abc123!@#'
        person = create_person(
            client,
            purchase_order=purchase_order
        )
        response = self.test_client.get(self.BASE_API, {'person_id': person.id})
        assert response.get_json()['data']['purchase_order'] == purchase_order


@pytest.mark.permissions
class TestCasePersonPermission(TestCaseApiAppBase):
    BASE_API = u'/v1/person'

    def test_own_client(self, client_role, client):
        security.set_roles([client_role])
        security.set_passport_client(client)
        person = create_person(client)
        response = self.test_client.get(self.BASE_API, {'person_id': person.id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        hm.assert_that(response.get_json()['data'], hm.has_entry('id', person.id))

    @pytest.mark.parametrize(
        'w_role, ans',
        [
            (True, http.OK),
            (False, http.FORBIDDEN),
        ],
    )
    def test_has_role(self, client, admin_role, view_person_role, w_role, ans):
        roles = [admin_role]
        if w_role:
            roles.append(view_person_role)
        security.set_roles(roles)

        person = create_person(client)
        response = self.test_client.get(self.BASE_API, {'person_id': person.id})
        hm.assert_that(response.status_code, hm.equal_to(ans))

        if w_role:
            data = response.get_json()['data']
            hm.assert_that(data, hm.has_entry('id', person.id))

    @pytest.mark.parametrize(
        'match_client,ans',
        [
            (True, http.OK),
            (False, http.FORBIDDEN),
        ],
    )
    def test_client_constraint(self, client, admin_role, view_person_role, match_client, ans):
        client_batch_id = create_role_client(client if match_client else None).client_batch_id
        roles = [
            admin_role,
            (view_person_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        person = create_person(client)
        response = self.test_client.get(self.BASE_API, {'person_id': person.id})
        hm.assert_that(response.status_code, hm.equal_to(ans))

        if match_client:
            data = response.get_json()['data']
            hm.assert_that(data, hm.has_entry('id', person.id))

    @pytest.mark.parametrize(
        'perms',
        [
            [],
            [cst.PermissionCode.ADMIN_ACCESS, cst.PermissionCode.VIEW_PERSONS],
        ]
    )
    def test_fields_filter(self, client, perms):
        roles = [create_role(perm) for perm in perms]
        security.set_roles(roles)
        if not perms:
            security.set_passport_client(client)
        bank = self.test_session.query(mapper.Bank).filter(mapper.Bank.hidden == 0).first()
        person = create_person(client, bik=bank.bik)
        is_admin = (cst.PermissionCode.ADMIN_ACCESS in perms)

        response = self.test_client.get(self.BASE_API, {'person_id': person.id}, is_admin=is_admin)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        perm_required_fields = {
            cst.PermissionCode.ADMIN_ACCESS: [
                'dt', 'live_signature'
            ],
            cst.PermissionCode.VIEW_PERSONS: [
                'revise_act_period_type', 'vip'
            ],
        }
        for perm, keys in perm_required_fields.iteritems():
            for key in keys:
                hm.assert_that(data, hm.has_key(key) if perm in perms else hm.not_(hm.has_key(key)))
        perm = cst.PermissionCode.ADMIN_ACCESS
        hm.assert_that(data['bank_data'], hm.has_key('update_dt') if perm in perms else hm.not_(hm.has_key(key)))
