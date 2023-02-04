# -*- coding: utf-8 -*-

import datetime
import decimal

import pytest

from balance import overdraft
from balance.actions.consumption import reverse_consume
from balance.constants import (
    InvoiceReceiptLockType,
)

from tests.balance_tests.overdraft.common import (
    create_invoice,
)

D = decimal.Decimal


@pytest.mark.parametrize(
    'receipt_sum, consume_sum, act_sum, req_consume_sum',
    [
        pytest.param(100, 10, 10, 100, id='part_consumed'),
        pytest.param(100, 10, 20, 100, id='part_overact_consumed'),
        pytest.param(200, 10, 10, 100, id='overpaid'),
        pytest.param(200, 100, 10, 100, id='overpaid_consumed'),
        pytest.param(100, 10, 0, 10, id='unacted'),
        pytest.param(100, 10, 100, 10, id='full_overact'),
        pytest.param(50, 60, 60, 60, id='underpaid'),
        pytest.param(70, 60, 60, 70, id='part_paid'),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_sums(session, client, receipt_sum, consume_sum, act_sum, req_consume_sum):
    invoice = create_invoice(client, quantity=100)
    if receipt_sum:
        invoice.receipt_sum_1c = receipt_sum
        invoice.create_receipt(receipt_sum)
    consume, = invoice.consumes
    if act_sum:
        order = consume.order
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: act_sum})
        invoice.generate_act(backdate=datetime.datetime.now(), force=1)
    if consume_sum != invoice.consume_sum:
        reverse_consume(consume, None, invoice.consume_sum.as_decimal() - consume_sum)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(1)
    session.flush()

    overdraft.Overdraft(session).refund_orders([client.id])
    session.expire_all()

    assert invoice.consume_sum == req_consume_sum


@pytest.mark.parametrize(
    'consume_qtys, act_qtys, receipt_sum, req_consume_sums',
    [
        pytest.param([60, 70], [10, 10], 130, [D('60.77'), D('69.23')], id='full'),
        pytest.param([60, 70], [10, 10], 100, [D('46.92'), D('53.08')], id='partial'),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_multiple_orders(session, client, consume_qtys, act_qtys, receipt_sum, req_consume_sums):
    invoice = create_invoice(client, quantity=consume_qtys)

    for consume, qty in zip(invoice.consumes, act_qtys):
        order = consume.order
        reverse_consume(consume, None, consume.current_qty - qty)
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})

    invoice.generate_act(backdate=datetime.datetime.now(), force=1)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(1)

    invoice.receipt_sum_1c = receipt_sum
    invoice.create_receipt(receipt_sum)

    overdraft.Overdraft(session).refund_orders([client.id])
    session.expire_all()

    assert [q.current_qty for q in invoice.consumes] == req_consume_sums


@pytest.mark.parametrize(
    'lock, term_delta, req_consume_sum',
    [
        pytest.param(None, 1, 100, id='lock_none'),
        pytest.param(InvoiceReceiptLockType.OFF, 1, 100, id='lock_off'),
        pytest.param(InvoiceReceiptLockType.OVERDRAFT, 1, 100, id='lock_overdraft'),
        pytest.param(InvoiceReceiptLockType.TRANSFER, 1, 10, id='lock_transfer'),
        pytest.param(InvoiceReceiptLockType.REFUND, 1, 10, id='lock_refund'),
        pytest.param(None, 0, 10, id='term'),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_conditions(session, client, invoice, lock, term_delta, req_consume_sum):
    invoice = create_invoice(client, quantity=100)
    invoice.unused_funds_lock = lock
    invoice.receipt_sum_1c = 100
    invoice.create_receipt(100)
    consume, = invoice.consumes
    order = consume.order
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})
    invoice.generate_act(backdate=datetime.datetime.now(), force=1)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(term_delta)
    reverse_consume(consume, None, 90)

    overdraft.Overdraft(session).refund_orders([client.id])
    session.expire_all()

    assert invoice.consume_sum == req_consume_sum
