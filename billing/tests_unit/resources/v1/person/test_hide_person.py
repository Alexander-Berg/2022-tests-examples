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
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
from yb_snout_api.tests_unit.fixtures.common import not_existing_id


@pytest.fixture(name='edit_person_role')
def create_edit_person_role():
    return create_role(
        (cst.PermissionCode.EDIT_PERSONS, {cst.ConstraintTypes.client_batch_id: None}),
    )


class TestHidePerson(TestCaseApiAppBase):
    BASE_API = '/v1/person/hide-person'

    @pytest.mark.smoke
    def test_hide_person(self, person):
        assert person.hidden == 0
        res = self.test_client.secure_post(
            self.BASE_API,
            {'person_id': person.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        assert person.hidden == 1

    @pytest.mark.parametrize(
        'is_admin',
        [True, False],
    )
    def test_w_active_contracts(self, client, person, is_admin):
        """Из админки можно всё
        """
        create_general_contract(client=client, person=person)

        res = self.test_client.secure_post(
            self.BASE_API,
            {'person_id': person.id},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if is_admin else http.INTERNAL_SERVER_ERROR))

        if not is_admin:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'PERSON_HAS_ACTIVE_CONTRACTS',
                    'description': 'Person with ID %d has active contracts. Archivation is prohibited.' % person.id,
                }),
            )
        else:
            assert person.hidden == 1

    @pytest.mark.parametrize(
        'is_admin',
        [True, False],
    )
    def test_w_autooverdraft(self, client, person, is_admin):
        """Из админки можно всё
        """
        ob.OverdraftParamsBuilder.construct(
            self.test_session,
            client=client,
            person=person,
            client_limit=666,
        )

        res = self.test_client.secure_post(
            self.BASE_API,
            {'person_id': person.id},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if is_admin else http.INTERNAL_SERVER_ERROR))

        if not is_admin:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'PERSON_HAS_AUTO_OVERDRAFT',
                    'description': 'Person with ID %d has auto overdraft. Archivation is prohibited.' % person.id,
                }),
            )
        else:
            assert person.hidden == 1


@pytest.mark.permissions
class TestHidePersonPermission(TestCaseApiAppBase):
    BASE_API = '/v1/person/hide-person'

    @pytest.mark.parametrize(
        'match_client',
        [
            pytest.param(True, id='right client'),
            pytest.param(False, id='wrong client'),
            pytest.param(None, id='wo role'),
        ],
    )
    def test_role(self, admin_role, edit_person_role, client, match_client):
        roles = [admin_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append(
                (edit_person_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
            )
        security.set_roles(roles)

        person = create_person(client=client)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'person_id': person.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

        if match_client:
            assert person.hidden == 1

    @pytest.mark.parametrize(
        'match_client',
        [True, False],
    )
    def test_client_ui(self, client, match_client):
        security.set_roles([])
        security.set_passport_client(client if match_client else create_client())

        person = create_person(client=client)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'person_id': person.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

        if match_client:
            assert person.hidden == 1


class TestUnarchivePerson(TestCaseApiAppBase):
    BASE_API = '/v1/person/unhide-person'

    def test_unarchive_person(self, client):
        person = create_person(client=client, hidden=1)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'person_id': person.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        assert person.hidden == 0

    def test_not_found(self):
        person_id = not_existing_id(ob.PersonBuilder)
        res = self.test_client.secure_post(
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


@pytest.mark.permissions
class TestUnarchivePersonPermission(TestCaseApiAppBase):
    BASE_API = '/v1/person/unhide-person'

    @pytest.mark.parametrize(
        'match_client',
        [
            pytest.param(True, id='right client'),
            pytest.param(False, id='wrong client'),
            pytest.param(None, id='wo role'),
        ],
    )
    def test_role(self, admin_role, edit_person_role, client, match_client):
        roles = [admin_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append(
                (edit_person_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
            )
        security.set_roles(roles)

        person = create_person(client=client, hidden=1)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'person_id': person.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

        if match_client:
            assert person.hidden == 0
