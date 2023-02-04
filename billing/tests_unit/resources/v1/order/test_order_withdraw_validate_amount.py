# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal as D
import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
)

from brest.core.tests import security
from yb_snout_api.resources.enums import Precision
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.utils import match_decimal_value
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.mark.smoke
class TestCaseOrderWithdrawValidateAmount(TestCaseApiAppBase):
    BASE_API = u'/v1/order/withdraw/validate-amount'

    AMOUNT = D(1000000)
    PRODUCT_PRICE = D(100)
    REAL_AMOUNT = D(0)

    def test_order_withdraw_validate_amount(self, invoice):
        security.set_passport_client(invoice.client)
        response = self.test_client.get(
            self.BASE_API,
            {
                'invoice_id': invoice.id,
                'order_id': invoice.invoice_orders[0].order_id,
                'amount': self.AMOUNT,
            },
        )

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        expected_data = {
            'product_price': has_entries({
                'value': match_decimal_value(
                    self.PRODUCT_PRICE,
                    Precision.PRICE.value,
                ),
                'multiples_amount': True,
            }),
            'real_amount': has_entries({
                'value': match_decimal_value(
                    self.REAL_AMOUNT,
                    Precision.MONEY.value,
                ),
                'constraint': 'MAX_AMOUNT',
            }),
            'invoice_currency': 'RUR',
        }
        data = response.get_json().get('data')

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(data, has_entries(expected_data))
