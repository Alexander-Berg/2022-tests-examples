# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import decimal
import uuid

from balance.constants import (
    PaymentMethodIDs,
)

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import (
    create_cash_payment_fact,
)
from yb_snout_api.tests_unit.fixtures.payments import (
    create_ym_payment,
    create_trust_api_payment,
    create_payment_cash_payment_fact,
)
from tests import object_builder as ob

D = decimal.Decimal


def create_refundable_cpf(invoice, amount):
    cpf = create_cash_payment_fact(invoice, amount)
    cpf.source_id = ob.get_big_number()
    cpf.cash_receipt_number = ob.get_big_number()
    cpf.inn = ob.get_big_number()
    cpf.customer_name = ob.get_big_number()
    cpf.bik = ob.get_big_number()
    cpf.account_name = ob.get_big_number()
    invoice.session.flush()
    invoice.session.expire_all()
    return cpf


def create_refundable_payment_cpf(invoice):
    payment = create_ym_payment(invoice)
    cpf = create_payment_cash_payment_fact(payment)
    cpf.source_id = ob.get_big_number()
    cpf.cash_receipt_number = ob.get_big_number()
    payment.transaction_id = ob.get_big_number()
    payment.user_account = ob.get_big_number()
    payment.payment_method_id = PaymentMethodIDs.yamoney_wallet
    invoice.session.flush()
    invoice.session.expire_all()
    return cpf, payment


def create_refundable_trust_cpf(invoice):
    payment = create_trust_api_payment(invoice)
    cpf = create_payment_cash_payment_fact(payment)
    payment.transaction_id = uuid.uuid4().hex
    invoice.session.flush()
    invoice.session.expire_all()
    return cpf, payment


def create_refund(cpf, amount):
    return ob.InvoiceRefundBuilder(
        invoice=cpf.invoice,
        payment_id=cpf.id,
        amount=amount,
    ).build(cpf.session).obj
