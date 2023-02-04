# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal as D

import http.client as http
import pytest
import mock
from hamcrest import assert_that, equal_to

from balance import constants as cst, muzzle_util as ut

from brest.core.tests import security
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_agency
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_custom_invoice, create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_role,
    get_client_role,
    get_agency_role,
)


@pytest.mark.smoke
class TestCaseInvoiceTransfetFromUnusedFunds(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/transfer/from-unused-funds'
    ORDER_QTY = D('45.67')
    PAYMENT_SUM = D('123456.78')
    COEFF = D(100)
    ZERO = D(0)

    @pytest.mark.skip('BALANCE-25143: logic has been changed')
    def test_transfer_from_unused_funds(self, order):
        invoice = create_custom_invoice({order: self.ORDER_QTY})
        invoice.manual_turn_on(self.PAYMENT_SUM)  # включаем счёт на большую суммы, чем создаем свободные средства

        params = {
            'invoice_id': invoice.id,
            'dst_order_id': order.id,
        }
        response = self.test_client.secure_post(
            self.BASE_API,
            data=params,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(ut.round00(invoice.unused_funds), equal_to(self.ZERO), u'Unused funds were transferred to order')

    def test_transfer_from_unused_fund_fail(self, invoice):
        params = {'invoice_id': invoice.id}

        response = self.test_client.secure_post(
            self.BASE_API,
            data=params,
        )
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),  # INVALID_PARAM
            'response code must be INTERNAL_SERVER_ERROR',
        )

    def test_transfer_from_unused_fund_for_invoice(self, invoice):
        invoice.manual_turn_on(self.PAYMENT_SUM)  # включаем счёт на большую суммы, чем создаем свободные средства

        params = {'invoice_id': invoice.id}
        response = self.test_client.secure_post(
            self.BASE_API,
            data=params,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')


@pytest.mark.permissions
@mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
class TestInvoiceTransferFromUnusedFundsPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/transfer/from-unused-funds'
    PAYMENT_SUM = D('123456.78')

    @pytest.mark.parametrize(
        'role_firm_id, res',
        [
            (-1, http.FORBIDDEN),
            (None, http.OK),
            (cst.FirmId.YANDEX_OOO, http.OK),
            (cst.FirmId.DRIVE, http.FORBIDDEN),
        ],
        ids=[
            'wo_role',
            'wo_role_constraints',
            'w_right_role_constraint',
            'w_wrong_role_constraint',
        ],
    )
    def test_ui_permissions(self, role_firm_id, res, admin_role):
        role = create_role((cst.PermissionCode.TRANSFER_UNUSED_FUNDS, {cst.ConstraintTypes.firm_id: None}))
        roles = [admin_role]
        if role_firm_id != -1:
            roles.append((role, {cst.ConstraintTypes.firm_id: role_firm_id}))
        security.set_roles(roles)

        invoice = create_invoice(firm_id=cst.FirmId.YANDEX_OOO)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={'invoice_id': invoice.id},
        )
        assert_that(response.status_code, equal_to(res))

    @pytest.mark.parametrize(
        'client_fun, role_fun, res',
        [
            (create_client, get_client_role, http.OK),
            (create_agency, get_agency_role, http.FORBIDDEN),
        ],
        ids=['client', 'agency'],
    )
    @mock_client_resource('yb_snout_api.resources.v1.invoice.routes.transfer_from_unused_funds.TransferFromUnusedFunds')
    def test_client_owns_invoice(self, client_fun, role_fun, res):
        client = client_fun()

        security.set_passport_client(client)
        security.set_roles([role_fun()])

        invoice = create_invoice(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={'invoice_id': invoice.id},
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(res))

    @mock_client_resource('yb_snout_api.resources.v1.invoice.routes.transfer_from_unused_funds.TransferFromUnusedFunds')
    def test_alien_invoice(self, client_role):
        security.set_roles([client_role])
        invoice = create_invoice(firm_id=cst.FirmId.YANDEX_OOO)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={'invoice_id': invoice.id},
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.FORBIDDEN))
