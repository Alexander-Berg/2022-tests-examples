# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from balance import constants as cst
from balance import muzzle_util as ut

from tests.tutils import mock_transactions

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReference
from yb_snout_api.tests_unit.fixtures.permissions import create_support_role

from tests import object_builder as ob


@pytest.mark.smoke
class TestCaseInvoiceRollback(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/rollback'

    @pytest.mark.parametrize('amount', ['1.23', None])
    @pytest.mark.parametrize('with_order', [True, False])
    def test_base(self, amount, invoice, with_order):
        invoice.turn_on_rows()

        order_id = None
        if with_order:
            order_id = invoice.consumes[0].order.id

        params = clean_dict({
            'invoice_id': invoice.id,
            'order_id': order_id,
            'amount': amount,
        })

        response = self.test_client.secure_post(
            self.BASE_API,
            data=params,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(
            invoice.operations,
            hm.has_item(hm.has_properties({'display_type_id': cst.OperationDisplayType.REMOVAL})),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perms, res',
        [
            ([cst.PermissionCode.ADMIN_ACCESS, cst.PermissionCode.WITHDRAW_CONSUMES_PREPAY], True),
            ([cst.PermissionCode.ADMIN_ACCESS], False),
            ([cst.PermissionCode.WITHDRAW_CONSUMES_PREPAY], False),
        ],
        ids=lambda x: str(x),
    )
    def test_access(self, perms, res):
        role = ob.create_role(self.test_session, *perms)
        security.set_roles([role])

        invoice = create_invoice()
        invoice.turn_on_rows()
        assert invoice.consume_sum > 0

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'amount': ut.decimal_to_str(invoice.effective_sum),
            },
        )

        if res:
            hm.assert_that(response.status_code, hm.equal_to(http.OK))
            hm.assert_that(invoice.consume_sum, hm.equal_to(0))
        else:
            hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))
            hm.assert_that(invoice.consume_sum, hm.equal_to(invoice.effective_sum))

    def test_fail(self, support_role):
        security.set_roles(support_role)

        invoice = create_invoice()
        invoice.turn_on_rows()
        assert invoice.consume_sum > 0

        with mock_transactions():
            res = self.test_client.secure_post(
                self.BASE_API,
                {'invoice_id': invoice.id, 'amount': ut.decimal_to_str(invoice.consume_sum)},
            )
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PERMISSION_DENIED',
                'description': 'User %s has no permission WithdrawConsumesPrepay.' % self.test_session.passport.passport_id,
            }),
        )
