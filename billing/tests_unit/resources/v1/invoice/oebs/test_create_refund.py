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

from balance.constants import (
    InvoiceRefundStatus,
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import (
    create_invoice,
)
from tests import object_builder as ob
from yb_snout_api.tests_unit.resources.v1.invoice.oebs.common import (
    create_refundable_cpf,
    create_refundable_payment_cpf,
)

D = decimal.Decimal


@pytest.mark.slow
@pytest.mark.invoice_refunds
class TestCreateOebsRefund(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/create-oebs-refund'

    def test_ok_without_reqs(self, invoice):
        invoice.create_receipt(100)
        cpf = create_refundable_cpf(invoice, 100)

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'cpf_id': cpf.id,
                'amount': '33.40',
                'refundable_amount': '100',
            },
        )
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')
        refund, = invoice.refunds

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'refunds_num': 1,
                'refundable_amount': '66.60',
            }),
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                amount=D('33.4'),
                status_code=InvoiceRefundStatus.not_exported,
                payment_id=cpf.id,
                payload=None,
            ),
        )

    def test_ok_add_reqs(self, invoice):
        cpf, payment = create_refundable_payment_cpf(invoice)
        payment.user_account = None
        payment.transaction_id = None
        invoice.create_receipt(100)

        transaction_num = ob.get_big_number()
        wallet_num = ob.get_big_number()
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'cpf_id': cpf.id,
                'amount': '33.40',
                'refundable_amount': '100',
                'transaction_num': transaction_num,
                'wallet_num': wallet_num,
            },
        )
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')
        refund, = invoice.refunds

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'refunds_num': 1,
                'refundable_amount': '66.60',
            }),
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                amount=D('33.4'),
                status_code=InvoiceRefundStatus.not_exported,
                payment_id=cpf.id,
                payload={
                    'wallet_num': str(wallet_num),
                    'transaction_num': str(transaction_num),
                },
            ),
        )

    def test_fail_amount(self, invoice):
        cpf = create_refundable_cpf(invoice, 100)
        invoice.create_receipt(100)

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'cpf_id': cpf.id,
                'amount': '666',
                'refundable_amount': '100',
            },
        )
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.BAD_REQUEST))
        data = response.get_json()
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'error': 'NOT_ENOUGH_FUNDS_FOR_REFUND',
            }),
        )

    def test_fail_availability(self, invoice):
        cpf = create_refundable_cpf(invoice, 100)
        cpf.source_id = None
        invoice.create_receipt(100)

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'cpf_id': cpf.id,
                'amount': '10',
                'refundable_amount': '100',
            },
        )
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.BAD_REQUEST))
        data = response.get_json()
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'error': 'INVALID_REFUND_REQUISITES',
            }),
        )
