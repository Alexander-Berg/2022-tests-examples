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
    contains_inanyorder,
    equal_to,
    empty,
    has_entry,
    has_property,
    has_properties,
)

from balance import constants as cst

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.mark.smoke
class TestCaseInvoiceCorrectReceipt(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/correct-receipt'
    PAYMENT_SUM = D('123456.78')

    def test_set_correct_receipt(self, invoice):
        assert_that(invoice.receipts, empty(), 'There is not any receipts yet')
        invoice.manual_turn_on(self.PAYMENT_SUM)  # зачисляем деньги на счёт

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(
            invoice.receipts,
            contains_inanyorder(
                has_properties({'receipt_sum': self.PAYMENT_SUM}),  # поступление от manual_turn_on
                has_properties({
                    'receipt_sum': -1 * self.PAYMENT_SUM,  # поступление от /v1/invoice/correct-receipt
                    'operation': has_property('type_id', cst.OperationTypeIDs.correct_receipt),
                }),
            ),
        )

    def test_invoice_zero_sum(self, invoice):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
            },
        )
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'response code must be INTERNAL_SERVER_ERROR',
        )
        assert_that(
            response.get_json(),
            has_entry('error', 'CANNOT_CREATE_RECEIPT'),
        )

    @pytest.mark.parametrize(
        'attr_name, attr_val',
        [
            ('credit', 2),
            ('firm_id', cst.FirmId.CLOUD),
        ],
    )
    def test_cant_create_receipt(self, invoice, attr_name, attr_val):
        assert_that(invoice.receipts, empty(), 'There is not any receipts yet')
        invoice.manual_turn_on(self.PAYMENT_SUM)  # зачисляем деньги на счёт

        setattr(invoice, attr_name, attr_val)
        self.test_session.flush()
        self.test_session.expire_all()  # for reselect invoice.firm

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
            },
        )
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'response code must be INTERNAL_SERVER_ERROR',
        )
        assert_that(
            response.get_json(),
            has_entry('error', 'CANNOT_CREATE_RECEIPT'),
        )
