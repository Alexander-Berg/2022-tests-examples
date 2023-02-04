# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm
from decimal import Decimal as D

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource


@pytest.mark.smoke
@pytest.mark.permissions
class TestInvoiceActsAccess(TestCaseApiAppBase):
    BASE_API = u'/v1/invoice/acts'

    @pytest.mark.parametrize(
        'role_firm_id, invoice_firm_id, res',
        [
            (None, cst.FirmId.YANDEX_OOO, http.OK),
            (cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, http.OK),
            (cst.FirmId.DRIVE, cst.FirmId.YANDEX_OOO, http.FORBIDDEN),
        ],
        ids=[
            'without firm_id constrain',
            'right firm_id',
            'wrong firm_id',
        ],
    )
    def test_permission_constraints(self, admin_role, role_firm_id, invoice_firm_id, res):
        """Проверяем подходят ли права пользователя для просмотра конкретного счета"""
        role = create_role((cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        roles = [
            admin_role,
            (role, {cst.ConstraintTypes.firm_id: role_firm_id}),
        ]
        security.set_roles(roles)
        act = create_act(firm_id=invoice_firm_id)
        response = self.test_client.get(self.BASE_API, {u'invoice_id': act.invoice.id})
        hm.assert_that(response.status_code, hm.equal_to(res))

    def test_wo_role(self, admin_role):
        """У пользователя нет права просматривать счета"""
        security.set_roles([admin_role])
        act = create_act()
        response = self.test_client.get(self.BASE_API, {'invoice_id': act.invoice.id})
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')

    @mock_client_resource('yb_snout_api.resources.v1.invoice.routes.acts.InvoiceActs')
    def test_owns_act(self, client, client_role):
        """Клиент владеет счетом"""
        security.set_passport_client(client)
        security.set_roles([client_role])
        act = create_act(client=client)
        response = self.test_client.get(self.BASE_API, {'invoice_id': act.invoice.id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

    @mock_client_resource('yb_snout_api.resources.v1.invoice.routes.acts.InvoiceActs')
    def test_alien_act(self, client, client_role):
        """Клиент пытаеся запросить чужой счет"""
        security.set_roles([client_role])
        act = create_act(client=client)
        response = self.test_client.get(self.BASE_API, {'invoice_id': act.invoice.id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')

    def test_totals(self, client):
        invoice = create_invoice(client=client)
        act1 = create_act(client=client)
        act1.amount, act1.paid_amount = D('10'), D('1')
        act2 = create_act(client=client)
        act2.amount, act2.paid_amount = D('100'), D('10')
        act2.hidden = 4
        act3 = create_act(client=client)
        act3.amount, act3.paid_amount = D('1000'), D('100')
        act4 = create_act(client=client)
        act4.amount, act4.paid_amount = D('1000'), D('10000')

        for act in [act1, act2, act3, act4]:
            act.invoice = invoice
        self.test_session.flush()

        response = self.test_client.get(self.BASE_API, {'invoice_id': act.invoice.id, 'get_totals': True})
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'acts': hm.has_length(3),
                'acts_totals': hm.has_entries({
                    'unpaid_sum': '909.00',
                }),
            }),
        )
