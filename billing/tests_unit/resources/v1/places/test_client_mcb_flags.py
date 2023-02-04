# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.utils import iteritems

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from brest.core.tests import utils as test_utils
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_role,
    create_view_client_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.partners import fixture_mcb_category, create_place


@pytest.fixture
def not_existing_client_id():
    session = test_utils.get_test_session()
    cb = ob.ClientBuilder()
    return cb.generate_unique_id(session, 'id')


@pytest.fixture
def not_existing_place_id():
    session = test_utils.get_test_session()
    pb = ob.PlaceBuilder()
    return pb.generate_unique_id(session, 'id')


@pytest.fixture
def not_existing_mcb_category_id():
    session = test_utils.get_test_session()
    mcb = ob.MkbCategoryBuilder()
    return mcb.generate_unique_id(session, 'id')


@pytest.fixture(name='set_mcb_flags_role')
def create_set_mcb_flags_role():
    return create_role(
        (
            cst.PermissionCode.SET_MCB_FLAGS,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCasePlacesClientMcbFlags(TestCaseApiAppBase):
    BASE_API = u'/v1/places/client-mcb-flags'

    @staticmethod
    def _get_payload(client_id, mcb_categories_dict):
        return {
            'client_id': client_id,
            'mcb_flags': [
                {'place_id': place_id, 'mcb_category': mcb_category}
                for place_id, mcb_category in iteritems(mcb_categories_dict)
            ],
        }

    def test_client_not_found(self, not_existing_client_id, mcb_category):
        another_client = create_client()
        place = create_place(self.test_session, another_client)
        response = self.test_client.secure_post_json(
            self.BASE_API,
            self._get_payload(not_existing_client_id, {place.id: mcb_category.id}),
        )
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND))

    def test_place_not_found(self, client, not_existing_place_id, mcb_category):
        response = self.test_client.secure_post_json(
            self.BASE_API,
            self._get_payload(client.id, {not_existing_place_id: mcb_category.id}),
        )
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND))

    def test_mcb_category_not_found(self, client, not_existing_mcb_category_id):
        place = create_place(self.test_session, client)
        response = self.test_client.secure_post_json(
            self.BASE_API,
            self._get_payload(client.id, {place.id: not_existing_mcb_category_id}),
        )
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND))

    def test_place_not_owned(self, client, mcb_category):
        another_client = create_client()
        place = create_place(self.test_session, another_client)
        response = self.test_client.secure_post_json(
            self.BASE_API,
            self._get_payload(client.id, {place.id: mcb_category.id}),
        )
        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        hm.assert_that(response.get_json().get('error'), hm.equal_to('INVALID_PARAM'))

    def test_set_mcb_flags(self, client, mcb_category):
        places = [create_place(self.test_session, client) for _ in range(3)]
        response = self.test_client.secure_post_json(
            self.BASE_API,
            self._get_payload(client.id, {place.id: mcb_category.id for place in places}),
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        hm.assert_that([place.mkb_category.id for place in places], hm.only_contains(mcb_category.id))

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client, status_code',
        [
            (None, http.FORBIDDEN),
            (True, http.OK),
            (False, http.FORBIDDEN),
        ],
    )
    def test_permission(self, match_client, status_code, admin_role, set_mcb_flags_role, client, mcb_category):
        roles = [admin_role]
        if match_client is not None:
            role_client = create_role_client(client if match_client else create_client())
            roles.append(
                (set_mcb_flags_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
            )
        security.set_roles(roles)
        place = create_place(self.test_session, client)
        response = self.test_client.secure_post_json(
            self.BASE_API,
            self._get_payload(client.id, {place.id: mcb_category.id}),
        )
        hm.assert_that(response.status_code, hm.equal_to(status_code))

    def test_invalid_params(self):
        response = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': -1,
                'mcb_flags': [
                    {'place_id': -1, 'mcb_category': 'Aaa'},
                    {'place_id': 'Aaaa', 'mcb_category': -1},
                ],
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'client_id': hm.contains(hm.has_entries({'error': 'POSITIVE_FIELD_VALIDATION_ERROR'})),
                    'mcb_flags': hm.contains(hm.has_entries({
                        '0': hm.contains(hm.has_entries({
                            'mcb_category': hm.contains(hm.has_entries({'error': 'INTEGER_INVALID_FIELD_VALIDATION_ERROR'})),
                            'place_id': hm.contains(hm.has_entries({'error': 'POSITIVE_FIELD_VALIDATION_ERROR'})),
                        })),
                        '1': hm.contains(hm.has_entries({
                            'mcb_category': hm.contains(hm.has_entries({'error': 'POSITIVE_FIELD_VALIDATION_ERROR'})),
                            'place_id': hm.contains(hm.has_entries({'error': 'INTEGER_INVALID_FIELD_VALIDATION_ERROR'})),
                        })),
                    })),
                }),
            }),
        )
