# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import datetime
import pytest
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.payments import (
    create_card_payment,
    create_paypal_payment,
    create_rbs_payment,
    create_sw_payment,
    create_trust_payment,
    create_tur_payment,
    create_wm_payment,
    create_ym_payment,
)


@pytest.fixture(name='bank_payment_role')
def create_bank_payment_role():
    return create_role(
        (
            cst.PermissionCode.CREATE_BANK_PAYMENTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='manage_rbs_role')
def create_manage_rbs_role():
    return create_role(
        (
            cst.PermissionCode.MANAGE_RBS_PAYMENTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='view_payments_role')
def create_view_payments_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_PAYMENTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


class PaymentsTests(TestCaseApiAppBase):
    BASE_API = u'/v1/payments'

    def search(self, search_api, params):
        url = u'{}/{}'.format(self.BASE_API, search_api)
        response = self.test_client.get(url, params)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), u'response code must be OK')
        return response.get_json()['data']['items']


@pytest.mark.smoke
class TestRBSPayments(PaymentsTests):
    def test_rbs_payment_list(self, rbs_payment):
        params = {
            'client_id': rbs_payment.invoice.client_id,
            'amount': rbs_payment.amount.as_decimal(),
        }
        payments = self.search('rbs_card', params)
        hm.assert_that(
            payments,
            hm.contains(hm.has_entry('invoice_id', rbs_payment.invoice.id)),
            u'Exactly one payment should be found',
        )


@pytest.mark.smoke
class TestPayPalPayments(PaymentsTests):
    def test_paypal_payment_list(self, paypal_payment):
        params = {
            'payment_id': paypal_payment.id,
            'client_id': paypal_payment.invoice.client_id,
            'amount': paypal_payment.amount.as_decimal(),
            'date_type': 'CREATION',
            'from_dt': paypal_payment.dt.strftime("%Y-%m-%dT%H:%M:%S"),
            'to_dt': (paypal_payment.dt + datetime.timedelta(minutes=1)).strftime("%Y-%m-%dT%H:%M:%S"),
        }
        payments = self.search('paypal', params)
        hm.assert_that(
            payments,
            hm.contains(hm.has_entry('payment_id', paypal_payment.id)),
            u'Only one payment should be found',
        )


@pytest.mark.smoke
class TestYandexMoneyPayments(PaymentsTests):
    def test_ym_payment_list(self, ym_payment):
        params = {
            'payment_id': ym_payment.id,
            'amount': ym_payment.amount.as_decimal(),
            'date_type': 'CREATION',
            'from_dt': ym_payment.dt.strftime("%Y-%m-%dT%H:%M:%S"),
            'to_dt': (ym_payment.dt + datetime.timedelta(minutes=1)).strftime("%Y-%m-%dT%H:%M:%S"),
        }
        payments = self.search('yamoney', params)
        hm.assert_that(
            payments,
            hm.contains(hm.has_entry('payment_id', ym_payment.id)),
            u'Only one payment should be found',
        )


@pytest.mark.smoke
class TestPaymentList(PaymentsTests):
    @pytest.mark.incomplete
    def test_payment_list(self, card_payment):
        params = {
            'payment_id': card_payment.id,
            'amount': card_payment.amount.as_decimal(),
            'date_type': 'CREATION',
            'from_dt': card_payment.dt.strftime("%Y-%m-%dT%H:%M:%S"),
            'to_dt': (card_payment.dt + datetime.timedelta(minutes=1)).strftime("%Y-%m-%dT%H:%M:%S"),
        }
        self.search('list', params)


@pytest.mark.smoke
class TestTrustPayments(PaymentsTests):
    @pytest.mark.incomplete
    def test_trust_payment_list(self, trust_payment):
        params = {
            'amount': trust_payment.amount.as_decimal(),
            'date_type': 'CREATION',
            'from_dt': trust_payment.dt.strftime("%Y-%m-%dT%H:%M:%S"),
            'to_dt': (trust_payment.dt + datetime.timedelta(minutes=1)).strftime("%Y-%m-%dT%H:%M:%S"),
        }
        self.search('trust', params)


@pytest.mark.smoke
class TestWebMoneyPayments(PaymentsTests):
    def test_wm_payment_list(self, wm_payment):
        params = {
            'payment_id': wm_payment.id,
            'amount': wm_payment.amount.as_decimal(),
            'date_type': 'CREATION',
            'from_dt': wm_payment.dt.strftime("%Y-%m-%dT%H:%M:%S"),
            'to_dt': (wm_payment.dt + datetime.timedelta(minutes=1)).strftime("%Y-%m-%dT%H:%M:%S"),
        }
        payments = self.search('webmoney', params)
        hm.assert_that(
            payments,
            hm.contains(hm.has_entry('payment_id', wm_payment.id)),
            u'Only one payment should be found',
        )


@pytest.mark.smoke
class TestRegisterPayments(PaymentsTests):
    @pytest.mark.incomplete
    def test_register_payment_list(self, rbs_payment):
        params = {
            'client_id': rbs_payment.invoice.client_id,
            'amount': rbs_payment.amount.as_decimal(),
        }
        self.search('registry', params)


@pytest.mark.smoke
class TestSwedishCardPayments(PaymentsTests):
    def test_sw_payment_list(self, sw_payment):
        params = {
            'payment_id': sw_payment.id,
            'amount': sw_payment.amount.as_decimal(),
            'date_type': 'CREATION',
            'from_dt': sw_payment.dt.strftime("%Y-%m-%dT%H:%M:%S"),
            'to_dt': (sw_payment.dt + datetime.timedelta(minutes=1)).strftime("%Y-%m-%dT%H:%M:%S"),
        }
        payments = self.search('sw_card', params)
        hm.assert_that(
            payments,
            hm.contains(hm.has_entry('payment_id', sw_payment.id)),
            u'Only one payment should be found',
        )


@pytest.mark.smoke
class TestTurkishCardPayments(PaymentsTests):
    def test_tur_payment_list(self, tur_payment):
        params = {
            'payment_id': tur_payment.id,
            'amount': tur_payment.amount.as_decimal(),
            'date_type': 'CREATION',
            'from_dt': tur_payment.dt.strftime("%Y-%m-%dT%H:%M:%S"),
            'to_dt': (tur_payment.dt + datetime.timedelta(minutes=1)).strftime("%Y-%m-%dT%H:%M:%S"),
        }
        payments = self.search('tur_card', params)
        hm.assert_that(
            payments,
            hm.contains(hm.has_entry('payment_id', tur_payment.id)),
            u'Only one payment should be found',
        )


class AccessPoint(object):
    def __init__(self, payment_firm, role_firm, match_client, w_invoice=True):
        self.payment_firm = payment_firm
        self.role_firm = role_firm
        self.match_client = match_client
        self.w_invoice = w_invoice

    def get_role(self, in_role=None, client=None):
        role = None
        if self.role_firm is not cst.SENTINEL:
            role = in_role or create_view_payments_role()
            params = {}
            if self.role_firm is not None:
                params[cst.ConstraintTypes.firm_id] = self.role_firm
            if self.match_client is not None:
                client_batch_id = create_role_client(client if self.match_client else None).client_batch_id
                params[cst.ConstraintTypes.client_batch_id] = client_batch_id
            role = (role, params)
        return role


@pytest.mark.permissions
@pytest.mark.slow
class TestPermissions(PaymentsTests):

    @pytest.mark.parametrize(
        'has_role',
        [False, True],
    )
    def test_registry_access(self, admin_role, view_payments_role, rbs_payment, has_role):
        roles = [admin_role]
        if has_role:
            roles.append(view_payments_role)
        security.set_roles(roles)

        data = self.search('registry', {'register_id': rbs_payment.register_id})
        payment_match = hm.contains(hm.has_entry('register_id', rbs_payment.register_id)) if has_role else hm.empty()
        hm.assert_that(data, payment_match)

    @pytest.mark.parametrize(
        'path, field_name, allow_wo_inv, create_func',
        [
            pytest.param('tur_card', 'payment_id', True, create_tur_payment, id='tur_card'),
            pytest.param('sw_card', 'payment_id', True, create_sw_payment, id='sw_card'),
            pytest.param('webmoney', 'payment_id', False, create_wm_payment, id='webmoney'),
            pytest.param('yamoney', 'payment_id', False, create_ym_payment, id='yamoney'),
            pytest.param('paypal', 'payment_id', True, create_paypal_payment, id='paypal'),
            pytest.param('rbs_card', 'payment_id', False, create_rbs_payment, id='rbs'),
            pytest.param('trust', 'payment_id', True, create_trust_payment, id='trust'),
            pytest.param('list', 'balance_payment_id', True, create_card_payment, id='all_payments'),
        ],
    )
    @pytest.mark.parametrize(
        'access_point, ans',
        [
            pytest.param(AccessPoint(None, cst.SENTINEL, None), False, id='wo role'),
            pytest.param(AccessPoint(cst.FirmId.TAXI, None, None), True, id='wo constraint'),
            pytest.param(AccessPoint(None, cst.FirmId.TAXI, None), True, id='payment wo firm_id'),
            pytest.param(AccessPoint(cst.FirmId.TAXI, cst.FirmId.TAXI, None), True, id='valid firm'),
            pytest.param(AccessPoint(cst.FirmId.TAXI, cst.FirmId.DRIVE, None), False, id='invalid firm'),
            # pytest.param(AccessPoint(None, None, False), False, id='invalid client'),
            # pytest.param(AccessPoint(None, None, True), True, id='valid client'),
            pytest.param(AccessPoint(cst.FirmId.DRIVE, cst.FirmId.DRIVE, True), True, id='valid firm + valid client'),
            pytest.param(AccessPoint(cst.FirmId.DRIVE, cst.FirmId.DRIVE, False), False, id='valid firm + invalid client'),
            pytest.param(AccessPoint(cst.FirmId.TAXI, cst.FirmId.DRIVE, True), False, id='invalid firm + valid client'),
            pytest.param(AccessPoint(None, None, None, False), True, id='wo constraints wo invoice'),
            pytest.param(AccessPoint(None, None, True, False), False, id='w client wo invoice'),
        ],
    )
    def test_access(
        self,
        admin_role,
        client,
        access_point,
        ans,
        path,
        field_name,
        allow_wo_inv,
        create_func,
    ):
        if not (access_point.w_invoice or allow_wo_inv):
            # В некоторых вьюшках счета присоединяются по join => нет случая "без киента"
            return

        roles = [admin_role]
        role = access_point.get_role(client=client)
        if role is not None:
            roles.append(role)
        security.set_roles(roles)

        payment = create_func(client=client, firm_id=access_point.payment_firm)
        if not access_point.w_invoice:
            payment.invoice = None
        self.test_session.flush()

        data = self.search(path, {'register_id': payment.register_id})
        if ans:
            payments_match = hm.contains(hm.has_entries({
                field_name: payment.id,
            }))
        else:
            payments_match = hm.empty()
        hm.assert_that(data, payments_match)

    @pytest.mark.parametrize(
        'path, field_name, role_func, creating_func, allow_wo_inv',
        [
            pytest.param('trust', 'allowed_to_confirm', create_bank_payment_role, create_trust_payment, True, id='trust'),
            pytest.param('paypal', 'action_allowed', create_manage_rbs_role, create_paypal_payment, True, id='paypal'),
            pytest.param('rbs_card', 'action_allowed', create_manage_rbs_role, create_rbs_payment, False, id='rbs_card'),
            pytest.param('sw_card', 'action_allowed', create_manage_rbs_role, create_sw_payment, True, id='sw_card'),
        ],
    )
    @pytest.mark.parametrize(
        'access_point, has_role',
        [
            pytest.param(AccessPoint(None, cst.SENTINEL, None), False, id='wo role'),
            pytest.param(AccessPoint(cst.FirmId.TAXI, None, None), True, id='wo constraint'),
            pytest.param(AccessPoint(None, cst.FirmId.TAXI, None), True, id='payment wo firm_id'),
            pytest.param(AccessPoint(cst.FirmId.TAXI, cst.FirmId.TAXI, None), True, id='valid firm'),
            pytest.param(AccessPoint(cst.FirmId.TAXI, cst.FirmId.DRIVE, None), False, id='invalid firm'),
            pytest.param(AccessPoint(cst.FirmId.DRIVE, cst.FirmId.DRIVE, True), True, id='valid firm + valid client'),
            pytest.param(AccessPoint(cst.FirmId.DRIVE, cst.FirmId.DRIVE, False), False, id='valid firm + invalid client'),
            pytest.param(AccessPoint(cst.FirmId.TAXI, cst.FirmId.DRIVE, True), False, id='invalid firm + valid client'),
            pytest.param(AccessPoint(None, None, None, False), True, id='wo constraints wo invoice'),
            pytest.param(AccessPoint(None, None, True, False), False, id='w client wo invoice'),
        ],
    )
    def test_action_role_w_constraints(
        self,
        admin_role,
        view_payments_role,
        client,
        access_point,
        has_role,
        path,
        field_name,
        role_func,
        allow_wo_inv,
        creating_func,
    ):
        if not (access_point.w_invoice or allow_wo_inv):
            # В некоторых вьюшках счета присоединяются по join => нет случая "без киента"
            return

        roles = [admin_role, view_payments_role]
        role = access_point.get_role(role_func(), client)
        if role is not None:
            roles.append(role)
        security.set_roles(roles)

        payment = creating_func(client=client, firm_id=access_point.payment_firm)
        if not access_point.w_invoice:
            payment.invoice = None
            self.test_session.flush()

        data = self.search(path, {'register_id': payment.register_id})
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries({
                    'payment_id': payment.id,
                    field_name: has_role,
                }),
            ),
        )
