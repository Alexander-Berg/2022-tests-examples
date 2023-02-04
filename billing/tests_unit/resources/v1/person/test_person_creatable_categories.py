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
from balance.actions.single_account.availability import ALLOWED_INDIVIDUAL_PERSON_CATEGORIES
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role


@pytest.fixture(name='view_clients_role')
def create_view_clients_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_CLIENTS,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='use_admin_persons_role')
def create_use_admin_persons_role():
    return create_role(
        (
            cst.PermissionCode.USE_ADMIN_PERSONS,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCasePersonCreatableCategories(TestCaseApiAppBase):
    BASE_API = '/v1/person/creatable-categories'

    @pytest.mark.parametrize(
        'is_partner',
        [True, False, None],
    )
    def test_get(self, client, is_partner):
        security.set_passport_client(client)
        response = self.test_client.get(
            self.BASE_API,
            params=clean_dict({'client_id': client.id, 'is_partner': is_partner}),
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        partner_match = hm.has_entry('is_partner', True)
        not_partner_match = hm.has_entry('is_partner', False)

        if is_partner is True:
            not_partner_match = hm.not_(not_partner_match)
        elif is_partner is False:
            partner_match = hm.not_(partner_match)

        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.has_items(
                partner_match,
                not_partner_match,
            ),
        )

    def test_single_account(self, client):
        single_account_category = list(ALLOWED_INDIVIDUAL_PERSON_CATEGORIES)[0]
        client.single_account_number = ob.get_big_number()
        create_person(client=client, type=single_account_category)
        self.test_session.flush()

        response = self.test_client.get(
            self.BASE_API,
            params=clean_dict({'client_id': client.id, 'is_partner': False}),
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            response.get_json()['data'],
            hm.not_(hm.has_item(hm.has_entry('category', single_account_category))),
        )

    @pytest.mark.parametrize(
        'legal_entity',
        [True, False, None],
    )
    def test_legal_entity(self, client, legal_entity):
        response = self.test_client.get(
            self.BASE_API,
            params=clean_dict({'client_id': client.id, 'legal_entity': legal_entity}),
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        legal_entity_match = hm.has_entry('legal_entity', True)
        not_legal_entity_match = hm.has_entry('legal_entity', False)

        if legal_entity is True:
            not_legal_entity_match = hm.not_(not_legal_entity_match)
        elif legal_entity is False:
            legal_entity_match = hm.not_(legal_entity_match)

        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.has_items(
                legal_entity_match,
                not_legal_entity_match,
            ),
        )

    def test_no_required_fields(self):
        res = self.test_client.get(self.BASE_API)
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'client_id': hm.contains(
                        hm.has_entries({
                            'error': 'REQUIRED_FIELD_VALIDATION_ERROR',
                            'description': 'Missing data for required field.',
                        }),
                    ),
                }),
            }),
        )


@pytest.mark.permissions
class TestCasePersonCreatableCategoriesPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/person/creatable-categories'

    def test_owner(self, client):
        security.set_passport_client(client)
        response = self.test_client.get(
            self.BASE_API,
            params=clean_dict({'client_id': client.id}),
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

    @pytest.mark.parametrize(
        'match_client, ans',
        [
            (True, http.OK),
            (False, http.FORBIDDEN),
            (None, http.FORBIDDEN),
        ],
    )
    def test_view_client_role(self, client, admin_role, view_clients_role, match_client, ans):
        roles = [admin_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((view_clients_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}))
        security.set_roles(roles)

        response = self.test_client.get(
            self.BASE_API,
            params=clean_dict({'client_id': client.id}),
        )
        hm.assert_that(response.status_code, hm.equal_to(ans))

    @pytest.mark.parametrize(
        'match_client',
        [True, False, None],
    )
    def test_user_admin_persons_perm(
        self,
        admin_role,
        view_clients_role,
        use_admin_persons_role,
        client,
        match_client,
    ):
        roles = [admin_role, view_clients_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((use_admin_persons_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}))
        security.set_roles(roles)

        # она подходит под условие admin_only, а создавать новую сложно
        category = self.test_session.query(mapper.PersonCategory).filter_by(category='yt').one()

        response = self.test_client.get(
            self.BASE_API,
            params=clean_dict({
                'client_id': client.id,
                'is_partner': False,  # чтобы yt не попала в выдачу как is_partner
            }),
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        categories_match = hm.has_item(
            hm.has_entries({
                'name': category.name,
                'category': category.category,
                'is_partner': False,
                'caption': category.caption,
            }),
        )
        if not match_client:
            categories_match = hm.not_(categories_match)

        hm.assert_that(
            response.get_json()['data'],
            categories_match,
        )
