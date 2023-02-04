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
from yb_snout_api.utils import clean_dict, context_managers as ctx_util
from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_support_role,
    create_role,
    get_client_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, create_orders


@pytest.fixture(name='transfer_role')
def create_transfer_role():
    return create_role((cst.PermissionCode.TRANSFER_FROM_ORDER, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((cst.PermissionCode.VIEW_ORDERS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='between_client_role')
def create_between_client_role():
    return create_role((cst.PermissionCode.TRANSFER_BETWEEN_CLIENTS, {cst.ConstraintTypes.firm_id: None}))


@pytest.mark.smoke
class TestOrderTransfer(TestCaseApiAppBase):
    BASE_API = '/v1/order/transfer'
    TRANSFER_QTY = D('12.34')
    SRC_QTY = D('123456.78')
    DST_QTY = D('45.67')
    ZERO = D('0')

    @pytest.mark.parametrize(
        'service_param',
        ['service_id', 'service.cc'],
    )
    def test_transfer_from_order_to_dst_order(
            self,
            service_param,
            admin_role,
            between_client_role,
            transfer_role,
            view_order_role,
    ):
        """Основной положительный тест на трансфер между заказами

        - есть права на трансфер с нужной фирмой
        - клиенты у счета и заказа не совпадают, но есть право BillingSupport
        - проверяется, правильная сумма зачислилась на заказ
        """
        firm_id = cst.FirmId.DRIVE
        roles = [
            admin_role,
            between_client_role,
            (view_order_role, {cst.ConstraintTypes.firm_id: firm_id}),
            (transfer_role, {cst.ConstraintTypes.firm_id: firm_id}),
        ]
        security.set_roles(roles)

        order = create_order(qty=self.SRC_QTY, firm_id=firm_id)
        dst_order = create_order(qty=self.DST_QTY, firm_id=firm_id)
        data = clean_dict({
            'order_id': order.id,
            'dst_service_cc': dst_order.service.cc if service_param == 'service.cc' else None,
            'dst_service_id': dst_order.service_id if service_param == 'service_id' else None,
            'dst_service_order_id': dst_order.service_order_id,
            'qty': self.TRANSFER_QTY,
            'mode': enums.TransferType.SRC_PTS.name,
        })

        response = self.test_client.secure_post(self.BASE_API, data)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        assert_that(order.consume_qty, equal_to(self.SRC_QTY - self.TRANSFER_QTY))
        assert_that(dst_order.consume_qty, equal_to(self.DST_QTY + self.TRANSFER_QTY))

    def test_transfer_to_unused_funds(self, admin_role, transfer_role):
        """Средства с заказа зачисляются на беззаказье"""
        security.set_roles([admin_role, transfer_role])
        order = create_order(qty=self.SRC_QTY)
        data = clean_dict({
            'order_id': order.id,
            'qty': self.TRANSFER_QTY,
            'mode': enums.TransferType.SRC_PTS.name,
        })
        response = self.test_client.secure_post(self.BASE_API, data)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        invoice = order.consumes[0].invoice
        assert_that(order.consume_qty, equal_to(self.SRC_QTY - self.TRANSFER_QTY))
        assert_that(invoice.unused_funds, equal_to(((self.SRC_QTY - self.TRANSFER_QTY) / D('100')).quantize(D('0'))))

    def test_transfer_all(self):
        order = create_order(qty=self.SRC_QTY)
        data = {
            'order_id': order.id,
            'qty': self.TRANSFER_QTY,
            'mode': enums.TransferType.ALL.name,
        }
        response = self.test_client.secure_post(self.BASE_API, data)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(order.consume_qty, equal_to(self.ZERO))

    def test_invalid_mode(self):
        order, = create_orders([self.SRC_QTY])
        data = {
            'order_id': order.id,
            'mode': enums.TransferType.DEST_PTS.name,
        }
        response = self.test_client.secure_post(self.BASE_API, data)
        assert_that(
            response.status_code,
            equal_to(http.INTERNAL_SERVER_ERROR),
            'response code must be INTERNAL_SERVER_ERROR',
        )
        assert_that(
            response.get_json(),
            has_entries({
                u'description': u'Invalid parameter for function: invalid mode for a transfer',
                u'error': u'INVALID_PARAM',
            }),
        )

    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_order_not_found(self, client, admin_role, transfer_role):
        security.set_roles([admin_role, transfer_role])
        order = create_order(qty=self.SRC_QTY)
        unique_service_order_id = ob.OrderBuilder().generate_unique_id(self.test_session, 'service_order_id')
        dst_order_id = '%s-%s' % (cst.ServiceId.DIRECT, unique_service_order_id)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'order_id': order.id,
                'dst_service_id': cst.ServiceId.DIRECT,
                'dst_service_order_id': unique_service_order_id,
                'qty': self.TRANSFER_QTY,
                'mode': enums.TransferType.SRC_PTS.name,
            },
        )
        assert_that(response.status_code, equal_to(http.NOT_FOUND))
        assert_that(
            response.get_json(),
            has_entries({
                'description': 'Order %s not found in DB' % dst_order_id,
                'error': 'ORDER_NOT_FOUND',
            }),
        )

    def test_invalid_pct(self, client, admin_role, transfer_role, view_order_role):
        security.set_roles([admin_role, transfer_role, view_order_role])
        order = create_order(qty=self.SRC_QTY)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'order_id': order.id,
                'mode': enums.TransferType.ALL.name,
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

    def test_none_pct_and_qty(self, client, admin_role, transfer_role, view_order_role):
        security.set_roles([admin_role, transfer_role, view_order_role])
        order = create_order(qty=self.SRC_QTY)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'order_id': order.id,
                'mode': enums.TransferType.ALL.name,
            },
        )
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(order.consume_qty, equal_to(self.ZERO))


@pytest.mark.permissions
@pytest.mark.smoke
@mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
class TestOrderTransferPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/order/transfer'
    TRANSFER_QTY = D('12.34')
    SRC_QTY = D('123456.78')
    DST_QTY = D('45.67')

    def test_ui_permission_denied(self, admin_role, between_client_role):
        """Для доступа из админки должно быть право TransferFromOrder
        """
        security.set_roles([admin_role, between_client_role])
        order = create_order()
        data = clean_dict({
            'order_id': order.id,
            'mode': enums.TransferType.ALL.name,
        })
        response = self.test_client.secure_post(self.BASE_API, data)
        assert_that(response.status_code, equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')
        assert_that(
            response.get_json(),
            has_entries({
                'description': 'User %s has no permission %s.' % (
                    self.test_session.oper_id,
                    cst.PermissionCode.TRANSFER_FROM_ORDER,
                ),
                'error': 'PERMISSION_DENIED',
            }),
        )

    def test_permit_wo_dst_order(self, admin_role, transfer_role):
        """Если не указан dst_order, то хватит права TransferFromOrder
        """
        security.set_roles([admin_role, transfer_role])
        order = create_order()
        data = clean_dict({
            'order_id': order.id,
            'mode': enums.TransferType.ALL.name,
        })
        response = self.test_client.secure_post(self.BASE_API, data)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

    def test_to_dst_order_match_clients(self, admin_role, transfer_role, view_order_role):
        """Одинаковые клиенты у order и dst_order => всё ок
        """
        security.set_roles([admin_role, transfer_role, view_order_role])
        order, dst_order = create_orders([self.SRC_QTY, self.DST_QTY])
        data = clean_dict({
            'order_id': order.id,
            'dst_service_id': dst_order.service_id,
            'dst_service_order_id': dst_order.service_order_id,
            'qty': self.TRANSFER_QTY,
            'mode': enums.TransferType.SRC_PTS.name,
        })
        response = self.test_client.secure_post(self.BASE_API, data)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

    @pytest.mark.parametrize(
        'has_support, res',
        [
            pytest.param(True, http.OK, id='has_between_client_role'),
            pytest.param(False, http.BAD_REQUEST, id='has_between_client_role'),
        ],
    )
    def test_to_dst_order_w_different_clients(
            self,
            admin_role,
            between_client_role,
            transfer_role,
            view_order_role,
            has_support,
            res,
    ):
        """Клиенты у order и dst_order не совпадают и ответ зависит от наличия TransferBetweenClients
        """
        roles = [admin_role, transfer_role, view_order_role]
        if has_support:
            roles.append(between_client_role)
        security.set_roles(roles)

        order = create_order(qty=self.SRC_QTY)
        dst_order = create_order(qty=self.DST_QTY)
        assert order.client != dst_order.client

        data = clean_dict({
            'order_id': order.id,
            'dst_service_id': dst_order.service_id,
            'dst_service_order_id': dst_order.service_order_id,
            'mode': enums.TransferType.ALL.name,
        })
        response = self.test_client.secure_post(self.BASE_API, data)
        assert_that(response.status_code, equal_to(res))

        if not has_support:
            assert_that(
                response.get_json(),
                has_entries({
                    'description': 'client should be the same class',
                    'error': 'CLIENTS_NOT_MATCH',
                }),
            )
