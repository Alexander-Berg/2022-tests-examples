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

from balance import muzzle_util as ut
from balance.constants import (
    InvoiceRefundStatus,
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import (
    create_invoice,
)
from yb_snout_api.tests_unit.resources.v1.invoice.oebs.common import (
    create_refundable_cpf,
    create_refund,
)

D = decimal.Decimal


@pytest.fixture
def cpf(invoice):
    return create_refundable_cpf(invoice, 666)


@pytest.mark.invoice_refunds
class TestOebsRefunds(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/oebs-refunds'

    @pytest.mark.smoke
    def test_list(self, cpf):
        refunds = [
            create_refund(cpf, 10),
            create_refund(cpf, 20),
            create_refund(cpf, 30),
        ]

        url = u'{}?cpf_id={}'.format(self.BASE_API, cpf.id)
        response = self.test_client.get(url)
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'total_row_count': 3,
                'items': hamcrest.contains_inanyorder(*[
                    hamcrest.has_entries({
                        'id': r.id,
                        'amount': str(ut.round00(r.amount)),
                        'dt': hamcrest.is_not(None),
                        'status_code': InvoiceRefundStatus.not_exported,
                        'status_descr': None,
                        'oebs_payment_num': None,
                        'unlock_allowed': False,
                    })
                    for r in refunds
                ]),
            }),
        )

    @pytest.mark.slow
    @pytest.mark.parametrize(
        'status, description, payload, is_unlockable', [
            (InvoiceRefundStatus.not_exported, None, None, False),
            (InvoiceRefundStatus.exported, None, None, False),
            (InvoiceRefundStatus.export_failed, 'ну блин', None, True),
            (InvoiceRefundStatus.oebs_transmitted, None, {'payment_num': 'num_payment'}, False),
            (InvoiceRefundStatus.oebs_reconciled, None, {'payment_num': 'num_payment'}, False),
            (InvoiceRefundStatus.failed, 'у меня лапки :(', {'payment_num': 'num_payment'}, True),
            (InvoiceRefundStatus.successful, None, {'payment_num': 'num_payment'}, False),
            (InvoiceRefundStatus.failed_unlocked, 'у меня лапки :(', {'payment_num': 'num_payment'}, False),
        ],
        ids=lambda x: str(x),
    )
    def test_status_payload(self, cpf, status, description, payload, is_unlockable):
        refund = create_refund(cpf, 666)
        refund.set_status(status, description)
        refund.payload = payload
        cpf.session.flush()

        url = u'{}?cpf_id={}'.format(self.BASE_API, cpf.id)
        response = self.test_client.get(url)
        hamcrest.assert_that(response.status_code, hamcrest.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'total_row_count': 1,
                'items': hamcrest.contains(
                    hamcrest.has_entries({
                        'id': refund.id,
                        'amount': '666.00',
                        'dt': hamcrest.is_not(None),
                        'status_code': status,
                        'status_descr': description,
                        'oebs_payment_num': payload and payload.get('payment_num'),
                        'unlock_allowed': is_unlockable,
                    }),
                ),
            }),
        )
