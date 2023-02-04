# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import clean_dict

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_role,
    create_view_client_role,
)


class TestCaseClientEmails(TestCaseApiAppBase):
    BASE_API = '/v1/client/emails'

    def test_get(self, client):
        email = 'person_eitor_data_test_snout@yandex.ru'
        client.email = email
        self.test_session.flush()

        security.set_passport_client(client)
        response = self.test_client.get(
            self.BASE_API,
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.contains_inanyorder(email, 'test@test.ru'),
        )

    def test_get_as_admin(self, client, admin_role, view_client_role):
        email = 'person_eitor_data_test_snout@yandex.ru'
        client.email = email
        self.test_session.flush()

        roles = [
            admin_role,
            (view_client_role, {cst.ConstraintTypes.client_batch_id: create_role_client(client).client_batch_id}),
        ]

        security.set_roles(roles)
        response = self.test_client.get(
            self.BASE_API,
            params={'client_id': client.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(email),
        )

    def test_forbidden(self, client, admin_role):
        security.set_roles([admin_role])
        response = self.test_client.get(
            self.BASE_API,
            params=clean_dict({'client_id': client.id}),
        )
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))
