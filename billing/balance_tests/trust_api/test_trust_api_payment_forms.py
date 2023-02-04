# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from contextlib import contextmanager
import xmlrpclib
import urllib

import hamcrest as hm
import httpretty
import pytest

from balance import exc
from balance import mapper
from balance.trust_api import payment_forms
from balance.application import getApplication
from balance.constants import PaymentMethodIDs
from balance.utils.path import get_balance_url

from tests import object_builder as ob


@contextmanager
def does_not_raise():
    yield


@pytest.fixture()
def httpretty_enabled_fixture():
    with httpretty.enabled(allow_net_connect=False):
        yield


def make_simpleapi_webxml_response(status_code=200, params=None, resp_text=''):
    url = getApplication().get_component_cfg('yb_balance_payments')['WebXmlURL']
    httpretty.register_uri(
        httpretty.POST,
        url + '/generate_ccard_form?' + urllib.urlencode(params or []),
        status=status_code,
        content_type='application/xml',
        match_querystring=True,
        body=resp_text
    )


def make_proxy_old_paysys_response(status_code=200, resp_text=''):
    url = getApplication().get_component_cfg('yb_balance_payments')['PaysysProxy'] + '/corba'
    httpretty.register_uri(
        httpretty.POST,
        url,
        status=status_code,
        content_type='application/xml',
        match_querystring=True,
        body=resp_text
    )


def test_inapplicable(session):
    paysys = session.query(mapper.Paysys).getone(1003)
    hm.assert_that(paysys.payment_method_id, hm.equal_to(PaymentMethodIDs.bank))

    invoice = ob.InvoiceBuilder(paysys=paysys).build(session).obj

    with pytest.raises(exc.NOT_FOUND) as e:
        payment_forms.get_payment_form_data(invoice)

    assert 'payment form' in str(e)


@pytest.mark.parametrize('payment_method_id, paysys_id, expected_form_type', [
    (PaymentMethodIDs.bank, 1003, type(None)),
    (PaymentMethodIDs.credit_card, 1002, payment_forms.CardPaymentForm),
    (PaymentMethodIDs.credit_card, 1033, payment_forms.CardPaymentForm),
    (PaymentMethodIDs.paypal, 1103, payment_forms.PayPalPaymentForm),
    # (PaymentMethodIDs.webmoney_wallet, 1022, payment_forms.WebMoneyPMPaymentForm),
    (PaymentMethodIDs.webmoney_wallet, 1052, payment_forms.WebMoneyPMPaymentForm),
    (PaymentMethodIDs.webmoney_wallet, 1001052, payment_forms.WebMoneyPMPaymentForm),
    # (PaymentMethodIDs.webmoney_wallet, 2301053, payment_forms.WebMoneyPMPaymentForm),
    (PaymentMethodIDs.yamoney_wallet, 11150, payment_forms.YooMoneyPaymentForm),
    (PaymentMethodIDs.yamoney_wallet, 11151, payment_forms.YooMoneyPaymentForm),
    (PaymentMethodIDs.yamoney_wallet, 1000, payment_forms.YooMoneyPaymentForm),
    (PaymentMethodIDs.yamoney_wallet, 12001000, payment_forms.YooMoneyPaymentForm),
    # (PaymentMethodIDs.yamoney_wallet, 1032, payment_forms.YooMoneyPaymentForm),
    # (PaymentMethodIDs.yamoney_wallet, 2301032, payment_forms.YooMoneyPaymentForm),
])
def test_match(session, payment_method_id, paysys_id, expected_form_type):
    paysys = session.query(mapper.Paysys).getone(paysys_id)
    hm.assert_that(paysys.payment_method_id, hm.equal_to(payment_method_id))

    invoice = ob.InvoiceBuilder(paysys=paysys).build(session).obj
    form = payment_forms.PaymentForm.create(invoice)
    hm.assert_that(form, hm.instance_of(expected_form_type))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('status_code, resp_text, expectation, expected_output', [
    (404, '', pytest.raises(xmlrpclib.ProtocolError), None),
    (500, '', pytest.raises(exc.TRUST_API_CONNECT_EXCEPTION), None),
    (200, '', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '</>', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '<payment-form/>', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '<payment-form _TARGET=""/>', does_not_raise(), ('POST', '', {}, {})),
    (200, '<payment-form _TARGET="https://trust-dev.yandex.ru/web/payment"/>',
     does_not_raise(), ('POST', 'https://trust-dev.yandex.ru/web/payment', {}, {})),
    (200, '''
<payment-form _TARGET="https://trust-dev.yandex.ru/web/payment"
  action="start" token="4815162342" description="Оплата счета"
/>''',
     does_not_raise(),
     ('POST', 'https://trust-dev.yandex.ru/web/payment',
      {'action': 'start', 'token': '4815162342', 'description': 'Оплата счета'}, {})),
    (200, '<payment-form _METHOD="GET" _TARGET="https://web.rbsuat.com/ab/merchants/yandex_team/payment_ru.html?mdOrder=062e9d99-0a15-75d7-8401-642e00003aa6" />',
     does_not_raise(), ('GET', 'https://web.rbsuat.com/ab/merchants/yandex_team/payment_ru.html?mdOrder=062e9d99-0a15-75d7-8401-642e00003aa6', {}, {}))
])
def test_get_form_data_trust(session, status_code, resp_text, expectation, expected_output):
    paysys = session.query(mapper.Paysys).getone(1033)
    hm.assert_that(paysys.payment_method_id, hm.equal_to(PaymentMethodIDs.credit_card))

    invoice = ob.InvoiceBuilder(paysys=paysys).build(session).obj
    form = payment_forms.PaymentForm.create(invoice)
    hm.assert_that(form, hm.instance_of(payment_forms.CardPaymentForm))

    make_simpleapi_webxml_response(status_code, form.get_input_params(), resp_text)

    method, payment_url, params, additional_params = None, None, None, None
    with expectation:
        method, payment_url, params, additional_params = form.get_form_data()

    if expected_output is None:
        expected_output = (None,) * 4

    hm.assert_that((method, payment_url, params, additional_params), hm.equal_to(expected_output))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('status_code, resp_text, expectation, expected_output', [
    (404, '', pytest.raises(xmlrpclib.ProtocolError), None),
    (500, '', pytest.raises(exc.TRUST_API_CONNECT_EXCEPTION), None),
    (200, '', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '</>', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '<payment-form/>', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '<payment-form _TARGET=""/>', does_not_raise(), ('POST', '', {}, {})),
    (200, '<payment-form _TARGET="https://money-dev.yandex.ru/eshop.xml"/>',
     does_not_raise(), ('POST', 'https://money-dev.yandex.ru/eshop.xml', {}, {})),
    (200, '''
<payment-form _TARGET="https://money-dev.yandex.ru/eshop.xml" _METHOD="POST"
  customerNumber="12345678" external_id="Б-12345678-9" scid="1000"
/>''',
     does_not_raise(),
     ('POST', 'https://money-dev.yandex.ru/eshop.xml',
      {'customerNumber': '12345678', 'external_id': 'Б-12345678-9', 'scid': '1000'}, {})),
    (200, '''
<payment-form _TARGET="https://money-dev.yandex.ru/eshop.xml" _METHOD="GET"
  customerNumber="12345678" external_id="Б-12345678-9" scid="1000">
<param name="merchant_id" value="5678"/>
<param name="desc" value="Оплата счета"/>
</payment-form>''',
     does_not_raise(),
     ('GET', 'https://money-dev.yandex.ru/eshop.xml?customerNumber=12345678'\
      '&desc=%D0%9E%D0%BF%D0%BB%D0%B0%D1%82%D0%B0%20%D1%81%D1%87%D0%B5%D1%82%D0%B0&external_id=%D0%91-12345678-9'\
      '&merchant_id=5678&scid=1000',
      {}, {})),
])
def test_get_form_data_yoomoney(session, status_code, resp_text, expectation, expected_output):
    paysys = session.query(mapper.Paysys).getone(1000)
    hm.assert_that(paysys.payment_method_id, hm.equal_to(PaymentMethodIDs.yamoney_wallet))

    invoice = ob.InvoiceBuilder(paysys=paysys).build(session).obj
    form = payment_forms.PaymentForm.create(invoice)
    hm.assert_that(form, hm.instance_of(payment_forms.YooMoneyPaymentForm))

    make_proxy_old_paysys_response(status_code, resp_text)

    method, payment_url, params, additional_params = None, None, None, None
    with expectation:
        method, payment_url, params, additional_params = form.get_form_data()

    if expected_output is None:
        expected_output = (None,) * 4

    hm.assert_that((method, payment_url, params, additional_params), hm.equal_to(expected_output))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('status_code, resp_text, expectation, expected_output', [
    (404, '', pytest.raises(xmlrpclib.ProtocolError), None),
    (500, '', pytest.raises(exc.TRUST_API_CONNECT_EXCEPTION), None),
    (200, '', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '</>', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '<payment-form/>', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '<payment-form _TARGET=""/>', does_not_raise(), ('POST', '', {}, {})),
    (200, '<payment-form _TARGET="https://www-dev.paypal.com/cgi-bin/webscr"/>',
     does_not_raise(), ('POST', 'https://www-dev.paypal.com/cgi-bin/webscr', {}, {})),
    (200, '''
<payment-form _TARGET="https://www-dev.paypal.com/cgi-bin/webscr" cmd="_express-checkout" token="ECDEV-12345678"
/>''',
     does_not_raise(),
     ('POST', 'https://www-dev.paypal.com/cgi-bin/webscr',
      {'cmd': '_express-checkout', 'token': 'ECDEV-12345678'}, {})),

    (200, '''
<payment-form _TARGET="https://www-dev.paypal.com/cgi-bin/webscr" cmd="_express-checkout" token="ECDEV-12345678">
<param name="payer_id" value="1234"/>
<param name="merchant_id" value="5678"/>
</payment-form>''',
     does_not_raise(),
     ('POST', 'https://www-dev.paypal.com/cgi-bin/webscr',
      {'cmd': '_express-checkout', 'token': 'ECDEV-12345678', 'payer_id': '1234', 'merchant_id': '5678'}, {})),
])
def test_get_form_data_paypal(session, status_code, resp_text, expectation, expected_output):
    paysys = session.query(mapper.Paysys).getone(1103)
    hm.assert_that(paysys.payment_method_id, hm.equal_to(PaymentMethodIDs.paypal))

    invoice = ob.InvoiceBuilder(paysys=paysys).build(session).obj
    form = payment_forms.PaymentForm.create(invoice)
    hm.assert_that(form, hm.instance_of(payment_forms.PayPalPaymentForm))

    make_proxy_old_paysys_response(status_code, resp_text)

    method, payment_url, params, additional_params = None, None, None, None
    with expectation:
        method, payment_url, params, additional_params = form.get_form_data()

    if expected_output is None:
        expected_output = (None,) * 4

    hm.assert_that((method, payment_url, params, additional_params), hm.equal_to(expected_output))


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('status_code, resp_text, expectation, expected_output', [
    (404, '', pytest.raises(xmlrpclib.ProtocolError), None),
    (500, '', pytest.raises(exc.TRUST_API_CONNECT_EXCEPTION), None),
    (200, '', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '</>', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '<payment-form/>', pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION), None),
    (200, '<payment-form _TARGET=""/>', does_not_raise(), ('POST', '', {}, {})),
    (200, '<payment-form _TARGET="https://dev.paymaster.ru/Payment/Init"/>',
     does_not_raise(), ('POST', 'https://dev.paymaster.ru/Payment/Init', {}, {})),
    (200, '''
<payment-form _TARGET="https://dev.paymaster.ru/Payment/Init"
  LMI_CURRENCY="RUB" LMI_MERCHANT_ID="R0" LMI_PAYMENT_AMOUNT="1000" LMI_PAYMENT_DESC="Оплата счета" _CHARSET="utf-8"
  _WITHHOST_LMI_FAIL_URL="result-wmi.xml" _WITHHOST_LMI_SUCCESS_URL="result-wmi.xml"
/>''',
     does_not_raise(),
     ('POST', 'https://dev.paymaster.ru/Payment/Init',
      {'LMI_CURRENCY': 'RUB', 'LMI_MERCHANT_ID': 'R0', 'LMI_PAYMENT_AMOUNT': '1000', 'LMI_PAYMENT_DESC': 'Оплата счета',
       'LMI_SUCCESS_URL': 'result-wmi.xml', 'LMI_FAIL_URL': 'result-wmi.xml'}, {'accept_charset': 'utf-8'})),
    (200, '''
<payment-form _TARGET="https://dev.paymaster.ru/Payment/Init"
  LMI_CURRENCY="RUB" LMI_MERCHANT_ID="R0" LMI_PAYMENT_AMOUNT="1000" LMI_PAYMENT_DESC="Оплата счета" _CHARSET="utf-8"
  _WITHHOST_LMI_FAIL_URL="result-wmi.xml" _WITHHOST_LMI_SUCCESS_URL="result-wmi.xml" _WITHHOST_SOMETHING="0" _ATTR=""
/>''',
     does_not_raise(),
     ('POST', 'https://dev.paymaster.ru/Payment/Init',
      {'LMI_CURRENCY': 'RUB', 'LMI_MERCHANT_ID': 'R0', 'LMI_PAYMENT_AMOUNT': '1000', 'LMI_PAYMENT_DESC': 'Оплата счета',
       'LMI_SUCCESS_URL': 'result-wmi.xml', 'LMI_FAIL_URL': 'result-wmi.xml',
       '_WITHHOST_SOMETHING': '0', '_ATTR': ''}, {'accept_charset': 'utf-8'})),
])
def test_get_form_data_webmoney_pm(session, status_code, resp_text, expectation, expected_output):
    paysys = session.query(mapper.Paysys).getone(1052)
    hm.assert_that(paysys.payment_method_id, hm.equal_to(PaymentMethodIDs.webmoney_wallet))

    invoice = ob.InvoiceBuilder(paysys=paysys).build(session).obj
    form = payment_forms.PaymentForm.create(invoice)
    hm.assert_that(form, hm.instance_of(payment_forms.WebMoneyPMPaymentForm))

    make_proxy_old_paysys_response(status_code, resp_text)

    method, payment_url, params, additional_params = None, None, None, None
    with expectation:
        method, payment_url, params, additional_params = form.get_form_data()

    if expected_output is None:
        expected_output = (None,) * 4
    else:
        balance_url = get_balance_url()
        _, _, expected_params, _ = expected_output
        for attr in expected_params.keys():
            if expected_params[attr].endswith('.xml'):
                expected_params[attr] = balance_url + '/' + expected_params[attr]

    hm.assert_that((method, payment_url, params, additional_params), hm.equal_to(expected_output))
