# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import itertools
import http.client as http
import hamcrest as hm
from decimal import Decimal as D

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role

RECEIPT_SUM = D('12345.67')


@pytest.fixture(name='issue_inv_role')
def create_issue_inv_role():
    return create_role((
        cst.PermissionCode.ISSUE_INVOICES,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


def make_deferpay(session, invoice):
    invoice.receipt_sum = RECEIPT_SUM
    invoice.repayments = []
    session.flush()
    invoice.turn_on_req(request=invoice.request)
    return invoice


class BaseTestCase(TestCaseApiAppBase):
    BASE_API = NotImplemented
    has_data = False

    def test_take_action(self, admin_role, issue_inv_role, client):
        session = self.test_session
        security.set_roles([
            admin_role,
            (issue_inv_role, {cst.ConstraintTypes.client_batch_id: create_role_client(client).client_batch_id}),
        ])

        invoices = [
            make_deferpay(session, create_invoice(client))
            for _i in range(1)
        ]
        deferpays = itertools.chain(*[i.deferpays for i in invoices])

        res = self.test_client.secure_post(
            self.BASE_API,
            data={
                'deferpay_ids': ', '.join([str(d.id) for d in deferpays]),
                'invoice_dt': session.now().strftime('%Y-%m-%dT%H:%M:%S'),
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        if self.has_data:
            data = res.get_json().get('data', {})
            assert 'invoice_id' in data


class TestCaseDeferpayRepayment(BaseTestCase):
    BASE_API = '/v1/deferpay/action/repayment-invoice'
    has_data = True

    def test_forbidden(self, admin_role, issue_inv_role, client):
        session = self.test_session
        security.set_roles([
            admin_role,
            (issue_inv_role, {cst.ConstraintTypes.client_batch_id: create_role_client().client_batch_id}),
        ])

        invoices = [make_deferpay(session, create_invoice(client))]
        deferpays = itertools.chain(*[i.deferpays for i in invoices])

        res = self.test_client.secure_post(
            self.BASE_API,
            data={
                'deferpay_ids': ', '.join([str(d.id) for d in deferpays]),
                'invoice_dt': session.now().strftime('%Y-%m-%dT%H:%M:%S'),
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))


class TestCaseDeferpayConfirm(BaseTestCase):
    BASE_API = '/v1/deferpay/action/confirm-invoices'


class TestCaseDeferpayDecline(BaseTestCase):
    BASE_API = '/v1/deferpay/action/decline-invoices'
