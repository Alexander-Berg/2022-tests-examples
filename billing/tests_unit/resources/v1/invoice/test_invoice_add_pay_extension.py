# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import http.client as http
import pytest
import mock
from hamcrest import assert_that, equal_to

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import (
    create_invoice,
    create_overdraft_invoice,
)


@pytest.fixture(name='pay_extension_role')
def create_pay_extension_role():
    return create_role((cst.PermissionCode.PAY_INVOICE_EXTENSION, {cst.ConstraintTypes.firm_id: None}))


@pytest.mark.smoke
class TestCaseInvoiceAddInvoicePayExtension(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/add-pay-extension'
    now = datetime.datetime.now()

    def test_add_pay_extension(self, overdraft_invoice):
        session = self.test_session

        invoice = overdraft_invoice

        # Согласно с условием: invoice.receipt_sum_1c < invoice.total_act_sum
        invoice.receipt_sum_1c = 1000
        invoice.total_act_sum = 1100

        session.flush()

        assert_that(len(invoice.pay_extensions), equal_to(0))

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'pay_dt': self.now,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(len(invoice.pay_extensions), equal_to(1))

    def test_add_pay_extension_not_overdraft_invoice(self, invoice):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'pay_dt': self.now,
            },
        )
        assert_that(response.status_code, equal_to(http.INTERNAL_SERVER_ERROR))
        assert_that(response.get_json()['error'], 'NOT_OVERDRAFT_INVOICE')

    def test_add_pay_extension_invalid_parameters(self, overdraft_invoice):
        session = self.test_session
        invoice = overdraft_invoice

        # Нарушаем условие: invoice.receipt_sum_1c < invoice.total_act_sum
        invoice.receipt_sum_1c = 1100
        invoice.total_act_sum = 100

        session.flush()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'pay_dt': self.now,
            },
        )
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'response code must be INTERNAL_SERVER_ERROR',
        )
        assert_that(len(invoice.pay_extensions), equal_to(0))

    @pytest.mark.parametrize(
        'role_firm_id, res',
        [
            pytest.param(cst.SENTINEL, http.FORBIDDEN,
                         id='wo role'),
            pytest.param(None, http.OK,
                         id='role wo constraints'),
            pytest.param(cst.FirmId.YANDEX_OOO, http.OK,
                         id='role constraints matches firm_id'),
            pytest.param(cst.FirmId.CLOUD, http.FORBIDDEN,
                         id='role constraints don\'t matches firm_id'),
        ],
    )
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_permission(self, admin_role, pay_extension_role, role_firm_id, res):
        roles = [admin_role]
        if role_firm_id is not cst.SENTINEL:
            roles.append((pay_extension_role, {cst.ConstraintTypes.firm_id: role_firm_id}))
        security.set_roles(roles)

        invoice = create_overdraft_invoice(firm_id=cst.FirmId.YANDEX_OOO)
        # Согласно с условием: invoice.receipt_sum_1c < invoice.total_act_sum
        invoice.receipt_sum_1c = 1000
        invoice.total_act_sum = 1100
        self.test_session.flush()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'pay_dt': self.now,
            },
        )
        assert_that(response.status_code, equal_to(res))
