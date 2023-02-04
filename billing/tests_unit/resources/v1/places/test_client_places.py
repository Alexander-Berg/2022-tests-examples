# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst

from brest.core.tests import security
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
from yb_snout_api.tests_unit.fixtures.partners import create_place


@pytest.mark.smoke
class TestCasePlacesClientPlaces(TestCaseApiAppBase):
    BASE_API = u'/v1/places/client-places'

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(self.BASE_API, {'client_id': not_existing_id})
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')

    def test_getting_places(self, client):
        session = self.test_session
        places = [create_place(session, client) for _ in range(3)]
        session.flush()
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        data = response.get_json().get('data', [])
        entry_checkers = (hm.has_entry('id', place.id) for place in places)
        hm.assert_that(data, hm.contains_inanyorder(*entry_checkers))

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client, status_code',
        [
            (None, http.FORBIDDEN),
            (True, http.OK),
            (False, http.FORBIDDEN),
        ],
    )
    def test_permission(self, match_client, status_code, admin_role, view_client_role, client):
        roles = [admin_role]
        if match_client is not None:
            role_client = create_role_client(client if match_client else create_client())
            roles.append(
                (view_client_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
            )
        security.set_roles(roles)
        res = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(res.status_code, hm.equal_to(status_code))
