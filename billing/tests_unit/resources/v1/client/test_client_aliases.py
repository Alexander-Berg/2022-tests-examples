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


@pytest.mark.smoke
class TestCaseClientAliases(TestCaseApiAppBase):
    BASE_API = '/v1/client/aliases'

    @staticmethod
    def _create_right_answer_for_client(client):
        attrs = (
            'id',
            'client_type_id',
            'name',
            'email',
            'phone',
            'fax',
            'url',
            'is_agency',
            'agency_id',
            'class_id',
            'full_repayment',
            'dt',
            'oper_id',
        )
        right_answer = {attr: getattr(client, attr) for attr in attrs}
        change_type = (
            ('is_agency', bool),
            ('full_repayment', bool),
            ('dt', lambda dt: dt.isoformat()),
        )
        for attr, mp in change_type:
            right_answer[attr] = mp(right_answer[attr])
        return right_answer

    def test_client_with_aliases(self, client):
        aliases = [create_client() for _i in range(2)]
        client.class_.aliases.extend(aliases)
        res = self.test_client.get(self.BASE_API, {'client_id': client.id}, is_admin=True)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', [])
        hm.assert_that(
            data,
            hm.contains_inanyorder(
                hm.has_entries(self._create_right_answer_for_client(client)),
                *(hm.has_entries(self._create_right_answer_for_client(alias)) for alias in aliases)  # noqa: C815
            ),
        )

    def test_client_without_aliases(self, client):
        res = self.test_client.get(self.BASE_API, {'client_id': client.id}, is_admin=True)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', [])
        hm.assert_that(
            data,
            hm.contains(hm.has_entries(self._create_right_answer_for_client(client))),
        )

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(self.BASE_API, {'client_id': not_existing_id}, is_admin=True)
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client',
        [
            None,
            True,
            False,
        ],
    )
    def test_permission(self, match_client, admin_role, view_client_role, client):
        roles = [admin_role]
        if match_client is not None:
            role_client = create_role_client(client if match_client else create_client())
            roles.append(
                (view_client_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
            )
        security.set_roles(roles)
        res = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', [])

        hm.assert_that(data, hm.has_item(hm.has_entry('id', client.id)) if match_client else hm.empty())
