# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal as D
from hamcrest import assert_that, equal_to
import http.client as http
import pytest
import mock

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order, create_order_w_firm


@pytest.fixture(name='payment_role')
def create_payment_role():
    return create_role((cst.PermissionCode.CREATE_CERTIFICATE_PAYMENTS, {cst.ConstraintTypes.firm_id: None}))


class TestCasePayOrder(TestCaseApiAppBase):
    BASE_API = '/v1/order/pay'

    @pytest.mark.parametrize('paysys_cc', ['ce', 'co'])
    def test_pay_order(self, order, paysys_cc):
        """
        Оплата заказа по сертификату(ce) или компенсация(co)
        """
        qty_difference = D('100500.100500')

        initial_consume_qty = order.consume_qty
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'order_id': order.id,
                'paysys_cc': paysys_cc,
                'qty': qty_difference,
            },
        )

        assert_that(response.status_code, equal_to(http.OK), 'Response code should be OK')
        expected_new_qty = initial_consume_qty + qty_difference * order.product.unit.type_rate
        new_consume_qty = order.consume_qty
        assert_that(new_consume_qty, equal_to(expected_new_qty), 'Unexpected quantity')

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'firm_ids, role_firm_ids, ans',
        [
            ([None], cst.SENTINEL, http.FORBIDDEN),
            ([None], [], http.OK),
            ([cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [], http.OK),
            ([cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], http.OK),
            ([cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], [cst.FirmId.YANDEX_OOO], http.FORBIDDEN),
        ],
    )
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_permission(self, admin_role, payment_role, firm_ids, role_firm_ids, ans):
        if role_firm_ids is cst.SENTINEL:
            roles = []
        elif not role_firm_ids:
            roles = [payment_role]
        else:
            roles = [
                (payment_role, {cst.ConstraintTypes.firm_id: firm_id})
                for firm_id in role_firm_ids
            ]
        roles.append(admin_role)
        security.set_roles(roles)

        order = create_order_w_firm(firm_ids=firm_ids)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'order_id': order.id,
                'paysys_cc': 'ce',
                'qty': D('100500.100500'),
            },
        )
        assert_that(response.status_code, equal_to(ans))
