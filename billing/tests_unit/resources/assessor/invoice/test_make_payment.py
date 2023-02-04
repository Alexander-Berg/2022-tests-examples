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
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.mark.smoke
class TestMakePayment(TestCaseApiAppBase):
    BASE_API = u'/assessor/invoice/make-payment'
    QTY = 5

    def test_make_payment_partially(self, invoice):
        invoice_data = {
            'invoice_external_id': invoice.external_id,
            'payment_sum': self.QTY,
        }

        response = self.test_client.secure_post(self.BASE_API, data=invoice_data, is_admin=False)

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(invoice.receipt_sum, equal_to(self.QTY))
        assert_that(invoice.receipt_sum_1c, equal_to(self.QTY))

    def test_make_payment_full(self, invoice):
        invoice_data = {'invoice_external_id': invoice.external_id}

        response = self.test_client.secure_post(self.BASE_API, data=invoice_data, is_admin=False)

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(invoice.receipt_sum, equal_to(invoice.effective_sum))
        assert_that(invoice.receipt_sum_1c, equal_to(invoice.total_sum))
