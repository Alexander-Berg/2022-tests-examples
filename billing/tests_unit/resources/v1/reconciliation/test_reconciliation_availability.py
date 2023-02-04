# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division

import datetime

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.mark.smoke
class TestReconciliationAvailability(TestCaseApiAppBase):
    BASE_API = '/v1/reconciliation/availability'

    @pytest.mark.parametrize(
        'has_limited_role',
        [True, False],
    )
    def test_settlements(self, has_limited_role, client):
        security.set_passport_client(client)
        if has_limited_role:
            security.update_limited_role(client)
        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'is_settlements_available': not has_limited_role,
                'is_reconciliation_reports_available': False,
            }),
        )

    @pytest.mark.parametrize(
        'match_firm',
        [True, False],
    )
    def test_reconciliation(self, invoice, match_firm):
        security.set_passport_client(invoice.client)
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [invoice.firm_id] if match_firm else [cst.FirmId.MARKET]
        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'is_settlements_available': True,
                'is_reconciliation_reports_available': match_firm,
            }),
        )
