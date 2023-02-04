# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import mock
from decimal import Decimal as D
from hamcrest import assert_that, equal_to, has_entries

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.resources import enums
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_role,
    get_client_role,
)


@pytest.fixture(name='transfer_role')
def create_transfer_role():
    return create_role((cst.PermissionCode.TRANSFER_FROM_INVOICE, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((cst.PermissionCode.VIEW_ORDERS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='between_client_role')
def create_between_client_role():
    return create_role((cst.PermissionCode.TRANSFER_BETWEEN_CLIENTS, {cst.ConstraintTypes.firm_id: None}))


@pytest.mark.smoke
class TestCaseInvoiceTransfer(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/transfer'
    PAYMENT_SUM = D('123456.78')
    QTY = D('123.32')

    def test_transfer(self, client, admin_role, between_client_role, transfer_role, view_order_role):
        """Основной положительный тест

        - есть права на трансфер с нужной фирмой
        - клиенты у счета и заказа не совпадают, но есть право BillingSupport
        - проверяется, что со счета списалась, а на заказ зачислилась правильная сумма
        """
        roles = [
            admin_role,
            view_order_role,
            between_client_role,
            (transfer_role, {cst.ConstraintTypes.firm_id: cst.FirmId.DRIVE}),
        ]

        security.set_roles(roles)

        order = create_order()
        invoice = create_invoice(client=client, firm_id=cst.FirmId.DRIVE)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        old_consume_sum = order.consume_sum
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'dst_order_id': '%s-%s' % (order.service_id, order.service_order_id),
                'invoice_id': invoice.id,
                'mode': enums.TransferType.SRC_PTS.name,
                'qty': self.QTY,
                'discount_pct': D('20'),
            },
        )
        assert_that(response.status_code, equal_to(http.OK))

        assert_that(invoice.unused_funds, self.PAYMENT_SUM - self.QTY)
        assert_that(order.consume_sum, old_consume_sum + self.QTY)

    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_order_not_found(self, client, admin_role, transfer_role, view_order_role):
        security.set_roles([admin_role, transfer_role, view_order_role])

        invoice = create_invoice(client=client)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        unique_service_order_id = ob.OrderBuilder().generate_unique_id(self.test_session, 'service_order_id')
        dst_order_id = '%s-%s' % (cst.ServiceId.DIRECT, unique_service_order_id)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'dst_order_id': dst_order_id,
                'invoice_id': invoice.id,
                'mode': enums.TransferType.ALL.name,
            },
        )
        assert_that(response.status_code, equal_to(http.BAD_REQUEST))
        assert_that(
            response.get_json(),
            has_entries({
                'description': 'Invalid parameter for function: Order %s not found in DB' % dst_order_id,
                'error': 'BAD_REQUEST',
            }),
        )

    def test_invalid_pct(self, client, admin_role, transfer_role, view_order_role):
        security.set_roles([admin_role, transfer_role, view_order_role])

        order = create_order(client=client)
        invoice = create_invoice(client=client)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'dst_order_id': '%s-%s' % (order.service_id, order.service_order_id),
                'invoice_id': invoice.id,
                'mode': enums.TransferType.ALL.name,
                'qty': self.QTY,
                'discount_pct': D('120'),
            },
        )
        assert_that(response.status_code, equal_to(http.BAD_REQUEST))
        assert_that(
            response.get_json(),
            has_entries({
                'description': 'Can\'t parse args.'
                               '\nСкидка в процентах Invalid parameter for function: invalid discount for a transfer',
                'error': 'SnoutParseArgsException',
            }),
        )


@pytest.mark.permissions
class TestInvoiceTransfer(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/transfer'
    PAYMENT_SUM = D('123456.78')

    @pytest.mark.parametrize(
        'w_extra_role',
        [False, True],
    )
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_ui_permission_denied_for_invoice(self, client, admin_role, transfer_role, w_extra_role):
        """Проверяется 2 случая:
        - права на трансфер нет
        - право на трансфер есть, но фирма неправильная
        """
        roles = [admin_role]
        if w_extra_role:
            roles.append((transfer_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD}))
        security.set_roles(roles)
        order = create_order(client)
        invoice = create_invoice(client=client)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'dst_order_id': '%s-%s' % (order.service_id, order.service_order_id),
                'invoice_id': invoice.id,
                'mode': enums.TransferType.ALL.name,
            },
        )
        assert_that(response.status_code, equal_to(http.FORBIDDEN))
        assert_that(
            response.get_json(),
            has_entries({
                'description': 'User %s has no permission %s.' % (
                    self.test_session.oper_id,
                    cst.PermissionCode.TRANSFER_FROM_INVOICE,
                ),
                'error': 'PERMISSION_DENIED',
            }),
        )

    def test_match_clients(self, client, admin_role, transfer_role, view_order_role):
        """Совпадают клиент у счета и заказа"""
        security.set_roles([admin_role, transfer_role, view_order_role])
        order = create_order(client)
        invoice = create_invoice(client=client)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'dst_order_id': '%s-%s' % (order.service_id, order.service_order_id),
                'invoice_id': invoice.id,
                'mode': enums.TransferType.ALL.name,
            },
        )
        assert_that(response.status_code, equal_to(http.OK))

    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_not_match_clients(self, client, admin_role, transfer_role, view_order_role):
        """Клиент у счета и заказа не совпадают и у пользователя нет права TransferBetweenClients"""
        security.set_roles([admin_role, transfer_role, view_order_role])
        order = create_order()
        invoice = create_invoice(client=client)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'dst_order_id': '%s-%s' % (order.service_id, order.service_order_id),
                'invoice_id': invoice.id,
                'mode': enums.TransferType.ALL.name,
            },
        )
        assert_that(response.status_code, equal_to(http.BAD_REQUEST))
        assert_that(
            response.get_json(),
            has_entries({
                'description': 'Invalid parameter for function: client should be the same class',
                'error': 'BAD_REQUEST',
            }),
        )

    @mock_client_resource('yb_snout_api.resources.v1.invoice.routes.transfer.Transfer')
    def test_client_owns_invoice(self, client, client_role):
        security.set_passport_client(client)
        security.set_roles([client_role])
        order = create_order(client)
        invoice = create_invoice(client=client)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'dst_order_id': '%s-%s' % (order.service_id, order.service_order_id),
                'invoice_id': invoice.id,
                'mode': enums.TransferType.ALL.name,
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK))

    @mock_client_resource('yb_snout_api.resources.v1.invoice.routes.transfer.Transfer')
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_alien_invoice(self, client_role, client):
        security.set_roles([client_role])
        order = create_order(client)
        invoice = create_invoice(client=client)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'dst_order_id': '%s-%s' % (order.service_id, order.service_order_id),
                'invoice_id': invoice.id,
                'mode': enums.TransferType.ALL.name,
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.FORBIDDEN))
