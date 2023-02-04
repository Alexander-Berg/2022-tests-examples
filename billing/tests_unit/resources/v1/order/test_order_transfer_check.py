# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
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
    get_client_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, create_order_w_firm
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.fixture(name='transfer_role')
def create_transfer_role():
    return create_role((cst.PermissionCode.TRANSFER_FROM_ORDER, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='view_order_role')
def create_view_order_role():
    return create_role((cst.PermissionCode.VIEW_ORDERS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='between_clients_role')
def create_between_clients_role():
    return create_role((cst.PermissionCode.TRANSFER_BETWEEN_CLIENTS, {cst.ConstraintTypes.firm_id: None}))


@pytest.mark.permissions
@pytest.mark.smoke
class TestOrderCheckTransferPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/order/transfer/check'

    @pytest.mark.parametrize(
        'checking_role, msg',
        [
            ('transfer_role', 'User {user_id} has no permission TransferFromOrder.'),
            ('view_order_role', 'User {user_id} has no permission ViewOrders.'),
        ],
    )
    @pytest.mark.parametrize(
        'role_firm_id, res',
        [
            pytest.param(cst.SENTINEL, False,
                         id='wo role'),
            pytest.param(None, True,
                         id='role wo constraints'),
            pytest.param(cst.FirmId.YANDEX_OOO, True,
                         id='role constraints matches firm_id'),
            pytest.param(cst.FirmId.CLOUD, False,
                         id='role constraints don\'t matches firm_id'),
        ],
    )
    def test_transfer_invoice_role(
            self,
            admin_role,
            transfer_role,
            between_clients_role,
            view_order_role,
            client,
            checking_role,
            role_firm_id,
            res,
            msg,
    ):
        """Проверяем права TransferFromOrder и ViewOrders
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

        order = create_order_w_firm(client=client, firm_ids=[cst.FirmId.YANDEX_OOO])
        dst_order = create_order_w_firm(client=client, firm_ids=[cst.FirmId.YANDEX_OOO])
        response = self.test_client.get(
            self.BASE_API,
            {
                'order_id': order.id,
                'dst_service_id': dst_order.service_id,
                'dst_service_order_id': dst_order.service_order_id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK))
        msg_params = {
            'user_id': self.test_session.passport.passport_id,
            'order_id': dst_order.id,
        }
        assert_that(
            response.get_json()['data'],
            has_entries({
                'result': res,
                'msg': msg.format(**msg_params) if not res else None,
            }),
        )

    @pytest.mark.parametrize(
        'with_between_clients_role, res, msg',
        [
            pytest.param(False, False, 'client should be the same class',
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
            roles.append((between_clients_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}))
        security.set_roles(roles)

        order = create_order(client=create_client())
        dst_order = create_order_w_firm(client=create_client(), firm_ids=[cst.FirmId.YANDEX_OOO])
        response = self.test_client.get(
            self.BASE_API,
            {
                'order_id': order.id,
                'dst_service_id': dst_order.service_id,
                'dst_service_order_id': dst_order.service_order_id,
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

    def test_order_not_found(self, admin_role, transfer_role, between_clients_role):
        order = create_order(client=create_client())
        dst_order = create_order(client=create_client())
        params = {
            'order_id': order.id,
            'dst_service_id': dst_order.service_id,
            'dst_service_order_id': dst_order.service_order_id,
        }
        wrong_params_map = [
            (('order_id',), order, 'Order 0--123 not found in DB'),
            (('dst_service_id', 'dst_service_order_id'), dst_order, 'Order -123--123 not found in DB'),
        ]

        for wrong_param, o, msg in wrong_params_map:
            cur_param = params.copy()
            for key in wrong_param:
                cur_param[key] = -123

            response = self.test_client.get(self.BASE_API, cur_param)
            assert_that(response.status_code, equal_to(http.OK))
            assert_that(
                response.get_json()['data'],
                has_entries({
                    'result': False,
                    'msg': msg,
                }),
            )
