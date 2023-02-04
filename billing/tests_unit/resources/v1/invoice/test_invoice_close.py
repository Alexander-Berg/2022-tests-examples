# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import http.client as http
from decimal import Decimal as D
from hamcrest import assert_that, equal_to

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
from yb_snout_api.tests_unit.fixtures.client import create_role_client, create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice, create_custom_invoice


@pytest.fixture(name='close_inv_role')
def create_close_inv_role():
    return create_role(
        (
            cst.PermissionCode.CLOSE_INVOICE,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCaseInvoiceClose(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/close'

    @pytest.mark.parametrize(
        'func',
        [
            pytest.param(lambda inv: setattr(inv, 'total_act_sum', inv.total_act_sum + inv.total_sum),
                         id='total_act_sum < total_sum'),
            pytest.param(lambda inv: setattr(inv, 'receipt_sum', D('0')),
                         id='receipt_sum == 0'),
            pytest.param(lambda inv: setattr(inv.invoice_orders[0].order, 'service_id', cst.ServiceId.DIRECT),
                         id='service_id != ONE_TIME_SALE'),
        ],
    )
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_cannot_close_invoice(self, admin_role, close_inv_role, func):
        security.set_roles([admin_role, close_inv_role])

        invoice = create_custom_invoice(service_id=cst.ServiceId.ONE_TIME_SALE)
        invoice.manual_turn_on(D('1000'))
        func(invoice)  # нарушаем одно из условий закрытия счёта

        response = self.test_client.secure_post(
            self.BASE_API,
            data={'invoice_id': invoice.id, 'close_dt': self.test_session.now().strftime('%Y%m%d%H%M%S')},
        )
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'response code must be INTERNAL_SERVER_ERROR',
        )
        assert_that(response.get_json()['error'], 'CANNOT_CLOSE_INVOICE')

    def test_invalid_month(self, invoice):
        session = self.test_session
        invoice.turn_on_rows()

        with mock.patch('balance.mncloselib.get_task_last_status', return_value='resolved'):
            # Нельзя формировать акты, если закрыта вторая стадия
            response = self.test_client.secure_post(
                self.BASE_API,
                data={'invoice_id': invoice.id, 'close_dt': session.now().strftime('%Y%m%d%H%M%S')},
            )

        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'response code must be INTERNAL_SERVER_ERROR',
        )
        assert_that(response.get_json()['error'], equal_to('CLOSED_MONTH'))


@pytest.mark.permissions
@pytest.mark.smoke
class TestCaseInvoiceClosePermissions(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/close'

    @mock_client_resource('yb_snout_api.resources.v1.invoice.routes.close.InvoiceClose')
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_close_by_user(self, client):
        security.set_passport_client(client)
        security.set_roles([])
        invoice = create_invoice(client=client)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'close_dt': self.test_session.now().strftime('%Y%m%d%H%M%S'),
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.FORBIDDEN))

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
    def test_firm_id(self, admin_role, close_inv_role, role_firm_id, res):
        roles = [admin_role]
        if role_firm_id is not cst.SENTINEL:
            roles.append((close_inv_role, {cst.ConstraintTypes.firm_id: role_firm_id}))
        security.set_roles(roles)

        invoice = create_custom_invoice(firm_id=cst.FirmId.YANDEX_OOO, service_id=cst.ServiceId.ONE_TIME_SALE)
        invoice.manual_turn_on(D('1000'))

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'close_dt': self.test_session.now().strftime('%Y%m%d%H%M%S'),
            },
        )
        assert_that(response.status_code, equal_to(res))

    @pytest.mark.parametrize(
        'is_allowed',
        [True, False],
    )
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_client_id(self, client, admin_role, close_inv_role, is_allowed):
        firm_id = cst.FirmId.DRIVE

        client_batch_id = create_role_client(client if is_allowed else None).client_batch_id
        roles = [
            admin_role,
            (
                close_inv_role,
                {cst.ConstraintTypes.firm_id: firm_id, cst.ConstraintTypes.client_batch_id: client_batch_id},
            ),
        ]
        security.set_roles(roles)

        invoice = create_custom_invoice(service_id=cst.ServiceId.ONE_TIME_SALE, client=client, firm_id=firm_id)
        invoice.manual_turn_on(D('1000'))

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'close_dt': self.test_session.now().strftime('%Y%m%d%H%M%S'),
            },
        )
        assert_that(response.status_code, equal_to(http.OK if is_allowed else http.FORBIDDEN))
