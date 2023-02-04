# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import mock
import pytest
import httpretty
import hamcrest as hm
import http.client as http

from balance.application import getApplication
from balance import constants as cst, mapper
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
from yb_snout_api.tests_unit.fixtures.common import not_existing_id
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.payments import create_trust_payment_form
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role


@pytest.fixture()
def httpretty_enabled_fixture():
    with httpretty.enabled(allow_net_connect=False):
        yield


def make_simpleapi_webxml_response(status_code=200, resp_text=''):
    url = getApplication().get_component_cfg('yb_balance_payments')['WebXmlURL']
    httpretty.register_uri(
        httpretty.POST,
        url + '/generate_ccard_form',
        status=status_code,
        body=resp_text,
    )


@pytest.mark.smoke
class TestPaystepPay(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/pay'

    def test_invoice_id_not_found(self, invoice):
        invoice_id = not_existing_id(ob.InvoiceBuilder)
        res = self.test_client.secure_post_json(self.BASE_API, {'invoice_id': invoice_id})
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'INVOICE_NOT_FOUND',
                'description': 'Invoice with ID %s not found in DB' % invoice_id,
            }),
        )

    def test_invoice_payment_form_not_found(self, invoice):
        invoice.payment_method_id = cst.PaymentMethodIDs.bank
        self.test_session.flush()
        res = self.test_client.secure_post_json(self.BASE_API, {'invoice_id': invoice.id})
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'NOT_FOUND',
                'description': 'Object not found: payment form for %s' % invoice,
            }),
        )

    @pytest.mark.parametrize('lang', ['ru', 'en', None])
    def test_invoice_trust_payment_form_good(self, invoice, trust_payment_form, lang):
        invoice.payment_method_id = cst.PaymentMethodIDs.credit_card
        self.test_session.flush()
        request_params = {'invoice_id': invoice.id}
        if lang:
            request_params['custom_params'] = {'lang': lang}
        with mock.patch('yb_snout_api.resources.v1.paystep.routes.pay.get_payment_form_data',
                        return_value=trust_payment_form):
            res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.get_json().get('data', {}),
            hm.has_entries({
                'method': 'POST',
                'form_url': 'https://trust-test.yandex.ru/web/payment/XXX',
                'params': {},
            }),
        )

    def test_w_additional_param(self):
        paysys = self.test_session.query(mapper.Paysys).getone(1052)
        hm.assert_that(paysys.payment_method_id, hm.equal_to(cst.PaymentMethodIDs.webmoney_wallet))
        invoice = ob.InvoiceBuilder.construct(self.test_session, paysys=paysys)

        trust_payment_form = create_trust_payment_form(invoice, {'a': 1}, additional_params={'accept_charset': 'utf-8'})
        with mock.patch('yb_snout_api.resources.v1.paystep.routes.pay.get_payment_form_data',
                        return_value=trust_payment_form):
            res = self.test_client.secure_post_json(self.BASE_API, {'invoice_id': invoice.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.has_entries({
                'method': 'POST',
                'form_url': 'https://trust-test.yandex.ru/web/payment/XXX',
                'params': {'a': 1},
                'accept_charset': 'utf-8',
            }),
        )

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_mobile_form_for_cards(self, invoice):
        invoice.payment_method_id = cst.PaymentMethodIDs.credit_card
        self.test_session.flush()

        request_params = {'invoice_id': invoice.id}
        make_simpleapi_webxml_response(resp_text='<payment-form _TARGET="https://trust-dev.yandex.ru/web/payment"/>')
        res = self.test_client.secure_post_json(
            self.BASE_API,
            request_params,
            headers={
                'User-Agent': 'Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N)'
                              ' AppleWebKit/537.36 (KHTML, like Gecko)'
                              ' Chrome/88.0.4324.111 Mobile Safari/537.36',
            },
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json().get('data', {}),
            hm.has_entries({
                'method': 'POST',
                'form_url': 'https://trust-dev.yandex.ru/web/payment?template_tag=mobile/form',
                'params': hm.empty(),
            }),
        )

@pytest.mark.smoke
@pytest.mark.permissions
class TestPaystepPayAccess(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/pay'

    def test_client_owns_invoice(self, invoice, client_role, trust_payment_form):
        invoice.payment_method_id = cst.PaymentMethodIDs.credit_card
        self.test_session.flush()
        security.set_passport_client(invoice.client)
        security.set_roles([client_role])
        with mock.patch('yb_snout_api.resources.v1.paystep.routes.pay.get_payment_form_data',
                        return_value=trust_payment_form):
            response = self.test_client.secure_post_json(self.BASE_API, {'invoice_id': invoice.id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

    def test_alien_invoice(self, client_role):
        security.set_roles([client_role])
        invoice = create_invoice()
        response = self.test_client.secure_post_json(self.BASE_API, {'invoice_id': invoice.id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'PERMISSION_DENIED',
                'description': 'User %s has no permission IssueInvoices.' % self.test_session.oper_id,
            }),
        )

    def test_w_role(self, admin_role, trust_payment_form):
        role = create_role((cst.PermissionCode.ISSUE_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        roles = [
            (role, {cst.ConstraintTypes.firm_id: None}),
        ]
        security.set_roles(roles)
        invoice = create_invoice()
        invoice.payment_method_id = cst.PaymentMethodIDs.credit_card
        self.test_session.flush()
        with mock.patch('yb_snout_api.resources.v1.paystep.routes.pay.get_payment_form_data',
                        return_value=trust_payment_form):
            response = self.test_client.secure_post_json(self.BASE_API, {'invoice_id': invoice.id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

    def test_wo_role(self, admin_role):
        security.set_roles([admin_role])
        invoice = create_invoice()
        response = self.test_client.secure_post_json(self.BASE_API, {'invoice_id': invoice.id})
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))
