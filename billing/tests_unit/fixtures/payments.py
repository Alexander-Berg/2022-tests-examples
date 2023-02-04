# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import allure
from decimal import Decimal as D
from tests import object_builder as ob

from brest.core.tests import utils as test_utils
import balance.mapper as mapper
from balance.constants import (
    OebsOperationType,
)
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


def create_payment(payment_type, inv=None, firm_id=None, client=None):
    session = test_utils.get_test_session()
    client = client or create_client()
    session.flush()
    inv = inv or create_invoice(client=client)
    register = ob.CardRegister.construct(session, register_dt=session.now(), amount=D('999'))
    payment = ob.GenericBuilder(payment_type, invoice=inv).build(session).obj
    payment.firm_id = firm_id
    payment.register_id = register.id
    session.flush()
    return payment


def create_payment_cash_payment_fact(payment):
    cpf = ob.OebsCashPaymentFactBuilder(
        amount=payment.amount,
        invoice=payment.invoice,
        operation_type=OebsOperationType.ACTIVITY,
        orig_id=payment.id,
    ).build(payment.session).obj
    payment.session.expire_all()  # триггер
    return cpf


@pytest.fixture(name='paypal_payment')
def create_paypal_payment(invoice=None, firm_id=None, client=None):
    with allure.step('create paypal money payment'):
        return create_payment(mapper.PayPalPayment, invoice, firm_id, client)


@pytest.fixture(name='ym_payment')
def create_ym_payment(invoice=None, firm_id=None, client=None):
    with allure.step('create yandex money payment'):
        session = test_utils.get_test_session()
        client = client or create_client()
        register = ob.CardRegister.construct(session, register_dt=session.now(), amount=D('999'))
        payment = ob.YandexMoneyPaymentBuilder(invoice=invoice or create_invoice(client=client)).build(session).obj
        payment.firm_id = firm_id
        payment.register_id = register.id
        session.flush()
        return payment


@pytest.fixture(name='card_payment')
def create_card_payment(invoice=None, firm_id=None, client=None):
    session = test_utils.get_test_session()
    with allure.step('create card payment'):
        client = client or create_client()
        session.flush()
        invoice = invoice or create_invoice(client=client, firm_id=firm_id)
        firm = firm_id and session.query(mapper.Firm).getone(firm_id)
        payment_method = ob.Getter(mapper.PaymentMethod, 1101)  # card
        processing = ob.Getter(mapper.Processing, 50501)  # trust api
        terminal = ob.TerminalBuilder.construct(
            session,
            firm_id=firm_id,
            currency='RUR',
            processing=processing,
            payment_method=payment_method,
        )
        register = ob.CardRegister.construct(session, register_dt=session.now(), amount=D('999'))
        payment = ob.CardPaymentBuilder.construct(
            session,
            invoice=invoice,
        )
        payment.firm = firm
        payment.register = register
        payment.set_terminal(terminal)
        session.flush()
        return payment


@pytest.fixture(name='trust_payment')
def create_trust_payment(invoice=None, firm_id=None, client=None):
    session = test_utils.get_test_session()
    with allure.step('create trust payment'):
        client = client or create_client()
        invoice = invoice or create_invoice(client=client, firm_id=firm_id)
        firm = firm_id and session.query(mapper.Firm).getone(firm_id)
        payment_method = ob.Getter(mapper.PaymentMethod, 1101)  # card
        processing = ob.Getter(mapper.Processing, 50501)  # trust api
        register = ob.CardRegister.construct(session, register_dt=session.now(), amount=D('999'))
        terminal = ob.TerminalBuilder.construct(
            session,
            firm=firm,
            currency='RUR',
            processing=processing,
            payment_method=payment_method,
        )
        payment = ob.TrustPaymentBuilder.construct(
            session,
            invoice=invoice,
        )
        payment.start_dt = payment.dt
        payment.firm = firm
        payment.set_terminal(terminal)
        payment.register_id = register.id
        payment.trust_payment_id = 'xtrust_%s' % ob.get_big_number()
        session.flush()
        return payment


@pytest.fixture(name='trust_api_payment')
def create_trust_api_payment(invoice):
    with allure.step('create trust_api payment'):
        payment = create_payment(mapper.TrustApiPayment, invoice)
        payment.start_dt = payment.dt
        return payment


@pytest.fixture(name='wm_payment')
def create_wm_payment(invoice=None, firm_id=None, client=None):
    with allure.step('create webmoney payment'):
        return create_payment(mapper.WebMoneyPayment, invoice, firm_id, client)


@pytest.fixture(name='rbs_payment')
def create_rbs_payment(invoice=None, firm_id=None, client=None):
    with allure.step('create rbs payment'):
        payment = create_payment(mapper.RBSPayment, invoice, firm_id, client)
        return payment


@pytest.fixture(name='sw_payment')
def create_sw_payment(invoice=None, firm_id=None, client=None):
    with allure.step('create swedish payment'):
        return create_payment(mapper.SixPaymentRUR, invoice, firm_id, client)


@pytest.fixture(name='tur_payment')
def create_tur_payment(invoice=None, firm_id=None, client=None):
    with allure.step('create turkish payment'):
        return create_payment(mapper.NestpayNatural, invoice, firm_id, client)


@pytest.fixture(name='trust_payment_form')
def create_trust_payment_form(invoice, custom_params=None, additional_params=None):
    with allure.step('mock trust payment form'):
        return (
            'POST',
            'https://trust-test.yandex.ru/web/payment/XXX',
            custom_params or {},
            additional_params or {}
        )
