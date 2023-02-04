# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from decimal import Decimal as D
from hamcrest import assert_that, equal_to, has_entries

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.fixture(name='transfer_role')
def create_transfer_role():
    return create_role((cst.PermissionCode.TRANSFER_FROM_INVOICE, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='between_clients_role')
def create_between_clients_role():
    return create_role((cst.PermissionCode.TRANSFER_BETWEEN_CLIENTS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((cst.PermissionCode.VIEW_ORDERS, {cst.ConstraintTypes.firm_id: None}))


@pytest.mark.permissions
@pytest.mark.smoke
class TestOrderCheckTransferPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/transfer/check'
    PAYMENT_SUM = D('123456.78')

    @pytest.mark.parametrize(
        'checking_role',
        ['transfer_role', 'view_order_role'],
    )
    @pytest.mark.parametrize(
        'role_firm_id, res, msg',
        [
            pytest.param(cst.SENTINEL, False, 'permission_denied',
                         id='wo role'),
            pytest.param(None, True, None,
                         id='role wo constraints'),
            pytest.param(cst.FirmId.YANDEX_OOO, True, None,
                         id='role constraints matches firm_id'),
            pytest.param(cst.FirmId.CLOUD, False, 'permission_denied',
                         id='role constraints don\'t matches firm_id'),
        ],
    )
    def test_transfer_invoice_role(
            self,
            admin_role,
            transfer_role,
            view_order_role,
            between_clients_role,
            client,
            checking_role,
            role_firm_id,
            res,
            msg,
    ):
        """Проверяем право TransferFromInvoice
        """
        role_map = {
            'transfer_role': transfer_role,
            'view_order_role': view_order_role,
        }
        roles = [admin_role, between_clients_role]
        for r_name, r in role_map.items():
            if r_name == checking_role:
                if role_firm_id is not cst.SENTINEL:
                    roles.append((r, {cst.ConstraintTypes.firm_id: role_firm_id}))
            else:
                roles.append(r)
        security.set_roles(roles)

        invoice = create_invoice(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        order = create_order(client=client)
        response = self.test_client.get(
            self.BASE_API,
            {
                'invoice_id': invoice.id,
                'dst_service_id': order.service_id,
                'dst_service_order_id': order.service_order_id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(
            response.get_json()['data'],
            has_entries({
                'result': res,
                'msg': msg,
            }),
        )

    @pytest.mark.parametrize(
        'with_between_clients_role, res, msg',
        [
            pytest.param(False, False, 'clients_do_not_match',
                         id='wo role'),
            pytest.param(True, True, None,
                         id='w role'),
        ],
    )
    def test_transfer_between_clients_role(
            self,
            admin_role,
            transfer_role,
            between_clients_role,
            view_order_role,
            with_between_clients_role,
            res,
            msg,
    ):
        """Проверяем право TransferBetweenClients
        """
        roles = [admin_role, transfer_role, view_order_role]
        if with_between_clients_role:
            roles.append(between_clients_role)
        security.set_roles(roles)

        invoice = create_invoice(client=create_client(), firm_id=cst.FirmId.YANDEX_OOO)
        order = create_order(client=create_client())
        response = self.test_client.get(
            self.BASE_API,
            {
                'invoice_id': invoice.id,
                'dst_service_id': order.service_id,
                'dst_service_order_id': order.service_order_id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(
            response.get_json()['data'],
            has_entries({
                'result': res,
                'msg': msg,
            }),
        )

    def test_match_clients(self, client, admin_role, transfer_role, view_order_role):
        """Совпадают клиент у счета и заказа"""
        security.set_roles([admin_role, transfer_role, view_order_role])
        order = create_order(client)
        invoice = create_invoice(client=client)
        invoice.manual_turn_on(self.PAYMENT_SUM)
        response = self.test_client.get(
            self.BASE_API,
            data={
                'dst_service_id': order.service_id,
                'dst_service_order_id': order.service_order_id,
                'invoice_id': invoice.id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(response.get_json()['data'], has_entries({'result': True}))
