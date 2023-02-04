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
    has_entry,
)

from balance.constants import OebsOperationType, PermissionCode
from tests import object_builder as ob

from brest.core.tests import security

from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_cash_payment_fact, create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_view_inv_role, create_do_inv_trans_role


@pytest.mark.smoke
class TestCaseInvoiceTransfer(TestCaseApiAppBase):
    BASE_API = '/v1/invoice'

    @pytest.mark.parametrize('turn_on, expected_sum', [(False, u'0.00'), (True, u'3000.00')])
    def test_available_invoice_transfer_sum(self, turn_on, expected_sum):
        src_invoice = create_invoice(turn_on=turn_on)
        create_cash_payment_fact(src_invoice, 3000, OebsOperationType.INSERT)

        response = self.test_client.get(self.BASE_API, {'invoice_id': src_invoice.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(
            response.get_json()['data'],
            has_entry('available-invoice-transfer-sum', expected_sum),
        )

    @pytest.mark.parametrize(
        'who_is, res',
        [
            ['owner', 'success'],
            ['stranger', '403'],
            ['admin', '403'],
            ['admin_view_inv', 'zero-sum'],
            ['admin_view_inv_do_inv_trans', 'success'],
        ]
    )
    def test_access(self, admin_role, view_inv_role, do_inv_trans_role, client, who_is, res):
        security.set_passport_client(client if who_is == 'owner' else create_client())
        roles = []
        is_admin = ('admin' in who_is)
        if is_admin:
            roles.append(admin_role)
        if 'view_inv' in who_is:
            roles.append(view_inv_role)
        if 'do_inv_trans' in who_is:
            roles.append(do_inv_trans_role)
        security.set_roles(roles)

        src_invoice = create_invoice(client, turn_on=True)
        create_cash_payment_fact(src_invoice, 3000, OebsOperationType.INSERT)

        response = self.test_client.get(self.BASE_API, {'invoice_id': src_invoice.id}, is_admin=is_admin)
        assert_that(response.status_code, equal_to(http.FORBIDDEN if '403' in res else http.OK))
        if res == http.OK:
            assert_that(
                response.get_json()['data'],
                has_entry('available-invoice-transfer-sum', u'3000.00' if res == 'success' else u'0.00'),
            )
