# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from decimal import Decimal as D
import hamcrest as hm

from balance import constants as cst, mapper
from balance.actions import promocodes as promo_actions
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice, create_custom_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.promocode import create_legacy_promocode
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource


@pytest.mark.slow
class TestCheckCanTearOff(TestCaseApiAppBase):
    BASE_API = u'/v1/promocode/check-can-tear-off'

    @staticmethod
    def _get_roles(session):
        return [
            create_admin_role(),
            ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None})),
            ob.create_role(session, (cst.PermissionCode.TEAR_PROMOCODE_OFF, {cst.ConstraintTypes.firm_id: None})),
        ]

    def _apply_promocode(self, legacy_promocode, invoice, apply_promocode):
        session = self.test_session

        promo_actions.reserve_promo_code(invoice.client, legacy_promocode)
        invoice.promo_code = legacy_promocode
        invoice.turn_on_rows(apply_promocode=apply_promocode)

    def test_can_tear_off(self, legacy_promocode):
        security.set_roles(self._get_roles(self.test_session))
        invoice = create_invoice()
        self._apply_promocode(legacy_promocode, invoice, True)

        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json()['data']
        hm.assert_that(data.get('can-tear-promocode-off', None), hm.equal_to(True))

    def test_can_not_tear_off(self, legacy_promocode):
        security.set_roles(self._get_roles(self.test_session))
        invoice = create_invoice()
        self._apply_promocode(legacy_promocode, invoice, False)

        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'can-tear-promocode-off': False,
                'reason': hm.has_entries({
                    'error': 'CANT_TEAR_PC_OFF_NO_PC',
                    'description': "Can't tear promocode off: PC_TEAR_OFF_NO_PC",
                }),
            })
        )

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(self.BASE_API, {'invoice_id': not_existing_id})
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'inv_firm_id, role_firm_id, res',
        (
            (cst.FirmId.YANDEX_OOO, None, False),
            (cst.FirmId.YANDEX_OOO, [], True),
            (cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, True),
            (cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET, False),
        ),
        ids=[
            'user does not have permission',
            'user has permission without constraints',
            'user has permission with right constraint',
            'user has permission with wrong constraint',
        ],
    )
    def test_check_permission(self, admin_role, client, legacy_promocode, inv_firm_id, role_firm_id, res):
        """Проверяем права с ограничениями"""
        session = self.test_session
        roles = [
            admin_role,
            ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None})),
        ]
        if role_firm_id is not None:
            role = ob.create_role(session, (cst.PermissionCode.TEAR_PROMOCODE_OFF, {cst.ConstraintTypes.firm_id: None}))
            pc_role = (role, {cst.ConstraintTypes.firm_id: role_firm_id}) if role_firm_id else role
            roles.append(pc_role)
        security.set_roles(roles)

        order = ob.OrderBuilder(client=client, firm_id=inv_firm_id).build(session)
        invoice = create_custom_invoice({order: D('100')}, firm_id=inv_firm_id, client=client)
        self._apply_promocode(legacy_promocode, invoice, True)
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(data.get('can-tear-promocode-off'), hm.equal_to(res))

    @pytest.mark.permissions
    @mock_client_resource('yb_snout_api.resources.v1.promocode.routes.check_can_tear_off.PromocodeCheckCanTearOff')
    def test_client_owns_invoice(self, client_role, legacy_promocode):
        """Счет не принадлежит клиенту"""
        session = self.test_session
        security.set_roles([client_role])

        order = ob.OrderBuilder().build(session)
        invoice = create_custom_invoice({order: D('100')}, client=order.client)
        self._apply_promocode(legacy_promocode, invoice, True)
        response = self.test_client.get(self.BASE_API, {'invoice_id': invoice.id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(data.get('can-tear-promocode-off'), hm.is_(False))
