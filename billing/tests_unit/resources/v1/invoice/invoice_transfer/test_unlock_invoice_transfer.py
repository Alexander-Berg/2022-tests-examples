# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import mock
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    has_entry,
)

from balance.constants import (
    InvoiceTransferStatus,
    OebsOperationType,
    PermissionCode,
)

from brest.core.tests import security

from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_cash_payment_fact, create_invoice, create_invoice_transfer
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_do_inv_trans_role


@pytest.mark.smoke
class TestUnlockInvoiceTransfer(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/unlock-invoice-transfer'

    def test_unlock(self):
        src_invoice = create_invoice()
        create_cash_payment_fact(src_invoice, 100, OebsOperationType.INSERT)
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice()

        dst_invoice.person = src_invoice.person
        self.test_session.flush()

        invoice_transfer = create_invoice_transfer(
            src_invoice, dst_invoice, 100, status=InvoiceTransferStatus.export_failed
        )

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_transfer_id': invoice_transfer.id
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(
            response.get_json()['data'],
            has_entries(
                {
                    "status": InvoiceTransferStatus.failed_unlocked,
                    "available_invoice_transfer_sum": u'100.00',
                }
            ),
        )

    def test_unlock_failed(self):
        src_invoice = create_invoice()
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice()

        dst_invoice.person = src_invoice.person
        self.test_session.flush()

        invoice_transfer = create_invoice_transfer(
            src_invoice, dst_invoice, 100
        )

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_transfer_id': invoice_transfer.id
            },
        )
        assert_that(response.status_code, equal_to(http.BAD_REQUEST), 'response code must be 400')
        assert_that(
            response.get_json(),
            has_entry(
                'error', u'INVOICE_TRANSFER_UNLOCK_FORBIDDEN'
            )
        )


    @pytest.mark.parametrize(
        'who_is, res',
        [
            ['admin', http.FORBIDDEN],
            ['admin_do_inv_trans', http.OK],
        ]
    )
    def test_access(self, admin_role, do_inv_trans_role, client, who_is, res):
        security.set_passport_client(create_client())
        roles = [admin_role]
        if 'do_inv_trans' in who_is:
            roles.append(do_inv_trans_role)
        security.set_roles(roles)

        src_invoice = create_invoice(client)
        src_invoice.manual_turn_on(100)
        dst_invoice = create_invoice(client, person=src_invoice.person)

        invoice_transfer = create_invoice_transfer(
            src_invoice, dst_invoice, 100, status=InvoiceTransferStatus.export_failed
        )

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'invoice_transfer_id': invoice_transfer.id},
        )

        assert_that(response.status_code, equal_to(res))
        if res == http.FORBIDDEN:
            assert_that(
                response.get_json(),
                has_entries({
                    'description': 'User %s has no permission %s.' % (
                        self.test_session.oper_id,
                        PermissionCode.DO_INVOICE_TRANSFER,
                    ),
                    'error': 'PERMISSION_DENIED',
                }),
            )
