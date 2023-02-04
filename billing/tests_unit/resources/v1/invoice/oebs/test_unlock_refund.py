# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import decimal

import http.client as http
import pytest
import hamcrest
import mock

from balance.constants import (
    InvoiceRefundStatus,
)

from brest.core.tests import security

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.utils import context_managers as ctx_util
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import (
    create_invoice,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role
from yb_snout_api.tests_unit.resources.v1.invoice.oebs.common import (
    create_refundable_cpf,
    create_refund,
)

D = decimal.Decimal


@pytest.fixture(name='cpf')
def create_cpf(invoice=None):
    invoice = invoice or create_invoice()
    res = create_refundable_cpf(invoice, 666)
    res.invoice.create_receipt(666)
    return res


@pytest.mark.slow
@pytest.mark.invoice_refunds
class TestUnlockOebsRefund(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/unlock-oebs-refund'

    @pytest.mark.parametrize(
        'status_code',
        [
            InvoiceRefundStatus.export_failed,
            InvoiceRefundStatus.failed,
        ],
    )
    def test_ok(self, cpf, status_code):
        refund = create_refund(cpf, 100)
        refund.set_status(status_code, 'some_descr')

        response = self.test_client.secure_post(self.BASE_API, data={'refund_id': refund.id})
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'refundable_amount': '666.00',
                'status_code': InvoiceRefundStatus.failed_unlocked,
                'status_descr': 'some_descr',
            }),
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.failed_unlocked,
            ),
        )

    @pytest.mark.parametrize(
        'status_code, expected_refundable_amount',
        [
            (InvoiceRefundStatus.not_exported, '566.00'),
            (InvoiceRefundStatus.exported, '566.00'),
            (InvoiceRefundStatus.oebs_reconciled, '566.00'),
            (InvoiceRefundStatus.oebs_transmitted, '566.00'),
            (InvoiceRefundStatus.successful, '666.00'),
            (InvoiceRefundStatus.failed_unlocked, '666.00'),
        ],
    )
    def test_fail_status(self, cpf, status_code, expected_refundable_amount):
        refund = create_refund(cpf, 100)
        refund.set_status(status_code)

        response = self.test_client.secure_post(self.BASE_API, data={'refund_id': refund.id})
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'refundable_amount': expected_refundable_amount,
                'status_code': status_code,
                'status_descr': None,
            }),
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(status_code=status_code),
        )

    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_fail_access(self, admin_role):
        security.set_roles([admin_role])
        cpf = create_cpf()
        refund = create_refund(cpf, 100)
        refund.set_status(InvoiceRefundStatus.failed)
        response = self.test_client.secure_post(self.BASE_API, data={'refund_id': refund.id})

        hamcrest.assert_that(
            response.status_code,
            hamcrest.equal_to(http.FORBIDDEN),
            'response code must be FORBIDDEN',
        )

        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(status_code=InvoiceRefundStatus.failed),
        )
