# -*- coding: utf-8 -*-

import decimal

import pytest

from balance import exc
from balance import mapper
from balance.constants import *

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_refund,
    create_product,
)

D = decimal.Decimal


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture(name='order')
def create_order(client, product_id=DIRECT_PRODUCT_RUB_ID):
    return ob.OrderBuilder(
        client=client,
        product_id=product_id,
    ).build(client.session).obj


@pytest.fixture(name='invoice')
def create_invoice(session, client=None, order=None):
    client = client or create_client(session)
    order = order or create_order(client)
    return ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(
                        quantity=666,
                        order=order
                    )
                ]
            )
        )
    ).build(session).obj


def test_all(invoice, order):
    invoice.create_receipt(100)
    invoice.transfer(order, TransferMode.all, None)
    assert order.consume_qty == 100
    assert invoice.consume_sum == 100


def test_all_rounding(session, client, invoice):
    product = create_product(session, D('66.6'))
    order = create_order(client, product.id)

    invoice.create_receipt(120)
    invoice.transfer(order, TransferMode.all)

    assert order.consume_qty == 1
    assert invoice.consume_sum == D('66.6')


def test_all_rounding_zero(session, client, invoice):
    product = create_product(session, D('66.6'))
    order = create_order(client, product.id)

    invoice.create_receipt(D('66.5'))
    with pytest.raises(exc.INVALID_TRANSFER_SUM_FOR_PRICE):
        invoice.transfer(order, TransferMode.all)

    assert order.consume_qty == 0
    assert invoice.consume_sum == 0


def test_sum(invoice, order):
    invoice.create_receipt(100)
    invoice.transfer(order, TransferMode.src, D('66.6'))
    assert order.consume_qty == D('66.6')
    assert invoice.consume_sum == D('66.6')


def test_sum_rounding(session, client, invoice):
    product = create_product(session, D('66.6'))
    order = create_order(client, product.id)

    invoice.create_receipt(100)
    invoice.transfer(order, TransferMode.src, D('66.6'))

    assert order.consume_qty == 1
    assert invoice.consume_sum == D('66.6')


@pytest.mark.parametrize('transfer_sum', [D('66.5'), D('66.7')], ids=lambda v: str(v))
def test_sum_rounding_fail(session, client, invoice, transfer_sum):
    product = create_product(session, D('66.6'))
    order = create_order(client, product.id)

    invoice.create_receipt(100)
    with pytest.raises(exc.INVALID_TRANSFER_SUM_FOR_PRICE):
        invoice.transfer(order, TransferMode.src, transfer_sum)

    assert order.consume_qty == 0
    assert invoice.consume_sum == 0


def test_qty(invoice):
    invoice.create_receipt(100)
    order = create_order(invoice.client, DIRECT_PRODUCT_ID)
    invoice.transfer(order, TransferMode.dst, D('2.345678'))
    assert order.consume_qty == D('2.345678')
    assert invoice.consume_sum == D('70.37')


def test_not_enough_funds(invoice, order):
    invoice.create_receipt(100)
    with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER):
        invoice.transfer(order, TransferMode.src, D('100.01'))


@pytest.mark.invoice_refunds
def test_all_w_refund(invoice, order):
    invoice.create_receipt(100)
    create_refund(invoice, 66)

    invoice.transfer(order, TransferMode.all)

    assert invoice.consume_sum == 34
    assert order.consume_qty == 34


@pytest.mark.invoice_refunds
def test_not_enough_w_refund(invoice, order):
    invoice.create_receipt(100)
    create_refund(invoice, 66)

    with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER):
        invoice.transfer(order, TransferMode.src, 35)


def test_delete_lock(session, invoice, order):
    invoice.create_receipt(100)
    invoice.transfer(order, TransferMode.src, 25)

    invoice.unused_funds_lock = InvoiceReceiptLockType.REFUND
    session.flush()

    invoice.transfer(order, TransferMode.all)
    assert invoice.consume_sum == 100
    assert not invoice.unused_funds_lock


def test_cancelled_invoice_transfer(invoice, order):
    invoice.hidden = 2
    invoice.create_receipt(100)

    invoice.transfer(order, TransferMode.src, 100)

    assert order.consume_qty == 0
    assert invoice.consume_sum == 0


def test_cancelled_invoice_turn_on_rows(invoice, order):
    invoice.hidden = 2
    invoice.create_receipt(100)

    invoice.turn_on_rows()

    assert order.consume_qty == 0
    assert invoice.consume_sum == 0
