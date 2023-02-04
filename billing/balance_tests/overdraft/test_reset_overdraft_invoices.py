# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance import overdraft
from balance.actions.consumption import reverse_consume
from balance.constants import (
    InvoiceReceiptLockType,
)

from tests.balance_tests.overdraft.common import (
    create_invoice,
)


@pytest.mark.parametrize(
    'receipt_sum, consume_sum, completion_sum, act_sum, req_consume_sum, is_overdraft, lock',
    [
        pytest.param(0, 100, 0, 0, 0, 0, InvoiceReceiptLockType.OVERDRAFT, id='uncompleted'),
        pytest.param(0, 100, 10, 0, 10, 1, InvoiceReceiptLockType.OVERDRAFT, id='completed'),
        pytest.param(0, 100, 0, 10, 10, 1, InvoiceReceiptLockType.OVERDRAFT, id='overacted'),
        pytest.param(0, 100, 5, 10, 100, 1, InvoiceReceiptLockType.OFF, id='overacted_w_compl'),
        pytest.param(0, 100, 10, 5, 10, 1, InvoiceReceiptLockType.OVERDRAFT, id='completed_acted'),
        pytest.param(10, 100, 0, 10, 10, 1, InvoiceReceiptLockType.OVERDRAFT, id='partial_paid'),
        pytest.param(100, 100, 10, 0, 100, 1, InvoiceReceiptLockType.OFF, id='full_paid'),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_sums(session, client, receipt_sum, consume_sum, completion_sum, act_sum, req_consume_sum, is_overdraft, lock):
    invoice = create_invoice(client, quantity=100)
    if receipt_sum:
        invoice.receipt_sum_1c = receipt_sum
        invoice.create_receipt(receipt_sum)
    consume, = invoice.consumes
    order = consume.order
    if act_sum:
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: act_sum})
        invoice.generate_act(backdate=datetime.datetime.now(), force=1)
    if completion_sum != consume.completion_sum:
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: completion_sum})
    if consume_sum != invoice.consume_sum:
        reverse_consume(consume, None, invoice.consume_sum.as_decimal() - consume_sum)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    session.flush()

    overdraft.Overdraft(session).reset_overdraft_invoices([client.id])

    hamcrest.assert_that(
        invoice,
        hamcrest.has_properties(
            consume_sum=req_consume_sum,
            overdraft=is_overdraft,
            unused_funds_lock=lock,
        )
    )


@pytest.mark.parametrize(
    'consume_qtys, completion_qtys, act_qtys, req_consume_qtys',
    [
        pytest.param([40, 60], [10, 10], [0, 0], [10, 10], id='completed'),
        pytest.param([40, 60], [0, 0], [7, 7], [0, 14], id='overact'),
        pytest.param([40, 60], [10, 10], [0, 15], [10, 10], id='overact_w_compl'),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_multiple_orders(session, client, consume_qtys, completion_qtys, act_qtys, req_consume_qtys):
    invoice = create_invoice(client, quantity=consume_qtys)

    for consume, act_qty in zip(invoice.consumes, act_qtys):
        order = consume.order
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: act_qty})
    invoice.generate_act(backdate=datetime.datetime.now(), force=1)

    for consume, completion_qty in zip(invoice.consumes, completion_qtys):
        order = consume.order
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: completion_qty})

    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    session.flush()

    overdraft.Overdraft(session).reset_overdraft_invoices([client.id])
    session.expire_all()

    assert [q.current_qty for q in invoice.consumes] == req_consume_qtys


@pytest.mark.parametrize(
    'term_delta, req_consume_sum',
    [
        (0, 100),
        (1, 0),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_term(session, client, term_delta, req_consume_sum):
    invoice = create_invoice(client, quantity=100)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(term_delta)
    session.flush()

    overdraft.Overdraft(session).reset_overdraft_invoices([client.id])

    assert invoice.consume_sum == req_consume_sum
