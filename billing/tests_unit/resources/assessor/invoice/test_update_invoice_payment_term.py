# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_entry,
    greater_than,
    has_entries,
)

from balance.constants import ServiceId, DIRECT_PRODUCT_ID
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice, create_overdraft_invoice


@pytest.mark.smoke
class TestUpdateInvoicePaymentTerm(TestCaseApiAppBase):
    BASE_API = u'/assessor/invoice/update-payment-term'

    def test_update_payment_term(self, overdraft_invoice):

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': overdraft_invoice.id,
                'payment_term_dt': datetime.datetime.now(),
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

    def test_update_payment_term_prepayment(self, invoice):

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'payment_term_dt': datetime.datetime.now(),
            },
            is_admin=False,
        )

        assert_that(
            response.get_json(),
            has_entries({
                'error': 'BAD_REQUEST',
                'description': 'Invalid parameter for function: Payment term dt is not supported for this invoice',
            }),
        )
