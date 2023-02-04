# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division

import datetime

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.reconciliation import create_reconciliation_request
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_issue_inv_role


@pytest.mark.smoke
class TestHideRequest(TestCaseApiAppBase):
    BASE_API = '/v1/reconciliation/hide-request'

    def test_by_client(self, client):
        security.set_roles([])
        security.set_passport_client(client)

        reconciliation_request = create_reconciliation_request(client=client, hidden=0)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.assert_that(http.OK))

        self.test_session.refresh(reconciliation_request)
        assert reconciliation_request.hidden == 1

    @pytest.mark.parametrize(
        'can_issue_inv',
        [True, False],
    )
    def test_permission(self, client, admin_role, issue_inv_role, can_issue_inv):
        """Действие разрешено только для саппорта.
        """
        roles = [admin_role]
        if can_issue_inv:
            roles.append(issue_inv_role)
        security.set_roles(roles)

        reconciliation_request = create_reconciliation_request(client=client, hidden=False)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.assert_that(http.OK if can_issue_inv else http.FORBIDDEN))
