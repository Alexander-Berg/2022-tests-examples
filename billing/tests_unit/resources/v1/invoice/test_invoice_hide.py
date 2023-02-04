# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from hamcrest import assert_that, equal_to, is_
import http.client as http
import pytest

from balance import constants as cst

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReference


@pytest.mark.smoke
class TestCaseHideInvoice(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/hide'

    def test_hide(self, invoice):
        invoice.credit = 1
        assert_that(invoice.hidden, is_(False))

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'invoice_id': invoice.id},
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(invoice.hidden, equal_to(2))  # скрыть счёт полностью

    @pytest.mark.parametrize(
        'attr_name, val',
        [
            ('credit', False),
            ('total_act_sum', 1),
            ('receipt_sum', 1),
            ('receipt_sum_1c', 1),
            ('status_id', cst.InvoiceStatusId.PRELIMINARY),
            ('hidden', 2),
        ],
    )
    def test_hide_fail(self, invoice, attr_name, val):
        setattr(invoice, attr_name, val)  # нарушает условия удаления постоплатного счета

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'invoice_id': invoice.id},
        )
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'response code must be INTERNAL_SERVER_ERROR',
        )
        assert_that(response.get_json()['error'], equal_to('CANNOT_HIDE_INVOICE'))
