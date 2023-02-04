# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_entry,
    has_entries,
)

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    get_client_role,
    create_role,
    create_view_client_role,
)


@pytest.mark.smoke
class TestCaseClient(TestCaseApiAppBase):
    BASE_API = '/v1/client'

    def test_get_client(self, client, admin_role, view_client_role):
        security.set_roles([admin_role, view_client_role])
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK')

    @pytest.mark.permissions
    def test_client_forbidden(self, client, client_role):
        client1, client2 = create_client(), create_client()
        security.set_passport_client(client1)
        security.set_roles([client_role])
        response = self.test_client.get(self.BASE_API, {'client_id': client2.id})
        assert_that(response.status_code, equal_to(http.FORBIDDEN), 'Response code must be FORBIDDEN')

    def test_not_found(self):
        not_existing_id = self.test_session.execute('select bo.S_CLIENT_ID.nextval from dual').scalar()
        response = self.test_client.get(self.BASE_API, {'client_id': not_existing_id})
        assert_that(response.status_code, equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')

    @pytest.mark.parametrize(
        'w_client',
        [True, False],
    )
    @pytest.mark.permissions
    def test_own_client(self, client, client_role, w_client):
        security.set_passport_client(client)
        security.set_roles([client_role])
        params = {'client_id': client.id} if w_client else {}
        response = self.test_client.get(self.BASE_API, params, is_admin=False)
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(response.get_json()['data'], has_entry('id', client.id))

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client, ans',
        [
            (None, http.FORBIDDEN),
            (True, http.OK),
            (False, http.FORBIDDEN),
        ],
    )
    def test_client_permission(self, match_client, ans, admin_role, view_client_role, client):
        roles = [admin_role]
        if match_client is not None:
            role_client = create_role_client(client if match_client else create_client())
            roles.append(
                (view_client_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
            )
        security.set_roles(roles)
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        assert_that(response.status_code, equal_to(ans))

        if match_client:
            assert_that(response.get_json()['data'], has_entry('id', client.id))

    def test_has_persons(self, client):
        _person = create_person(client=client)
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(response.get_json()['data'], has_entries({'id': client.id, 'has_persons': True}))
