# -*- coding: utf-8 -*-

import collections
import uuid

from balance import mapper
from balance.constants import *

from tests import object_builder as ob


def create_order(client):
    return ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID),
    ).build(client.session).obj


def create_invoice(client, orders, paysys_id, quantity=100, overdraft=0):
    if not isinstance(orders, collections.Iterable):
        orders = [orders]

    return ob.InvoiceBuilder(
        overdraft=overdraft,
        paysys=ob.Getter(mapper.Paysys, paysys_id),
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(quantity=quantity, order=order)
                    for order in orders
                ]
            )
        )
    ).build(client.session).obj


def create_ym_payment(invoice):
    payment = ob.YandexMoneyPaymentBuilder(invoice=invoice).build(invoice.session).obj
    payment.receipt_sum = 0
    invoice.session.flush()
    return payment


def create_wm_payment(invoice):
    session = invoice.session
    terminal = (
        session.query(mapper.Terminal)
            .filter_by(firm_id=invoice.firm_id,
                       payment_method_id=PaymentMethodIDs.webmoney_wallet,
                       currency=invoice.currency)
            .first()
    )
    payment = ob.WebMoneyPaymentBuilder(invoice=invoice, terminal=terminal).build(invoice.session).obj
    payment.receipt_sum = 0
    invoice.session.flush()
    return payment


def create_trust_payment(invoice):
    payment = ob.TrustApiPaymentBuilder(invoice=invoice).build(invoice.session).obj
    payment.receipt_sum = 0
    invoice.session.flush()
    return payment


def create_payment_ocpf(payment, amount=None):
    cpf = ob.OebsCashPaymentFactBuilder(
        amount=amount or payment.amount,
        invoice=payment.invoice,
        operation_type=OebsOperationType.ACTIVITY,
        orig_id=payment.id
    ).build(payment.session).obj
    payment.session.expire_all()  # триггер
    return cpf


def create_ocpf(invoice, amount, operation_type=OebsOperationType.ONLINE, dt=None):
    cpf = ob.OebsCashPaymentFactBuilder(
        amount=amount,
        invoice=invoice,
        operation_type=operation_type,
        dt=dt
    ).build(invoice.session).obj
    invoice.session.expire_all()  # триггер
    return cpf


def create_bank_cpf(invoice, amount):
    cpf = create_ocpf(invoice, amount)
    cpf.inn = 'inn'
    cpf.customer_name = 'customer_name'
    cpf.bik = 'bik'
    cpf.account_name = 'account_name'
    cpf.source_id = uuid.uuid4().int
    cpf.cash_receipt_number = 'cash_receipt_number'
    invoice.session.flush()
    return cpf
