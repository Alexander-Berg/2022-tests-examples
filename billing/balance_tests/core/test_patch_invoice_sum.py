# -*- coding: utf-8 -*-

import decimal

import pytest

from balance import exc

from tests.balance_tests.core.core_common import (
    _patch_discount,
    AUCTION_UNIT_ID,
    SHOWS_1000_UNIT_ID,
    UNITS_1000_UNIT_ID,
    _init_invoice,
)

D = decimal.Decimal


@pytest.mark.parametrize(
    'unit_id, price, discount_pct, old_qty, new_sum, new_qty',
    [
        [AUCTION_UNIT_ID, 100, 0, 10, 700, 7],
        [AUCTION_UNIT_ID, 100, 20, 10, 700, D('8.75')],
        [AUCTION_UNIT_ID, 321, 0, D('6.666667'), D('12345.67'), D('38.460031')],
        [SHOWS_1000_UNIT_ID, 666, 0, 123456, D('42.42'), 64]
    ],
    ids=['fish_simple', 'fish_discount', 'fish_rounding', 'shows_rounding']
)
def test_single(session, core_obj,
                client, paysys, unit_id,
                price, discount_pct, old_qty,
                new_sum, new_qty):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=unit_id,
        price=price,
        orders_qtys=[old_qty],
        discount_pct=discount_pct
    )

    with _patch_discount(discount_pct):
        core_obj.patch_invoice_sum(invoice.id, new_sum)
    session.expire_all()

    assert new_sum == invoice.effective_sum

    invoice_order, = invoice.invoice_orders
    assert new_sum == invoice_order.amount
    assert price == invoice_order.price
    assert discount_pct == invoice_order.discount_pct
    assert new_qty == invoice_order.quantity
    assert new_qty == invoice_order.initial_quantity


@pytest.mark.parametrize(
    'price, discount_pct, old_qtys, row_id, sums, new_qtys',
    [
        [
            100, 0,
            [1, 2, 3],
            1,
            [100, 100, 300],
            [1, 1, 3]
        ],
        [
            100, 14,
            [1, 2, 3],
            1,
            [86, 666, 258],
            [1, D('7.744186'), 3]
        ],
        [
            100, 0,
            [D('1.111111'), D('2.222222'), D('3.333333')],
            1,
            [D('111.11'), D('543.21'), D('333.33')],
            [D('1.111111'), D('5.4321'), D('3.333333')]
        ],
    ],
    ids=['simple', 'discount', 'rounding']
)
def test_multiple(session, core_obj,
                  client, paysys,
                  price, discount_pct, old_qtys,
                  row_id, sums, new_qtys):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=price,
        orders_qtys=old_qtys,
        discount_pct=discount_pct
    )

    new_sum = sums[row_id]
    invoice_order = invoice.invoice_orders[row_id]

    with _patch_discount(discount_pct):
        core_obj.patch_invoice_sum(invoice.id, new_sum, invoice_order.id)
    session.expire_all()

    assert sum(sums) == invoice.effective_sum
    assert sums == [io.amount for io in invoice.invoice_orders]
    assert {price} == {io.price for io in invoice.invoice_orders}
    assert {discount_pct} == {io.discount_pct for io in invoice.invoice_orders}
    assert new_qtys == [io.quantity for io in invoice.invoice_orders]
    assert new_qtys == [io.initial_quantity for io in invoice.invoice_orders]


def test_forbidden_unit(session, core_obj, client, paysys):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=UNITS_1000_UNIT_ID,
        price=10,
        orders_qtys=[5],
    )

    with pytest.raises(exc.CANNOT_PATCH_DAYS_AND_UNITS):
        core_obj.patch_invoice_sum(invoice.id, 666)


def test_quantity_discount_multiple(session, core_obj, client, paysys):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=100,
        orders_qtys=[10, 20],
        discount_pct=10,
        adjust_quantity=True
    )
    invoice_order = invoice.invoice_orders[-1]

    with _patch_discount(10, adjust_quantity=True):
        core_obj.patch_invoice_sum(invoice.id, 123, invoice_order.id)
    session.expire_all()

    assert 1123 == invoice.effective_sum

    assert [1000, 123] == [io.amount for io in invoice.invoice_orders]
    assert {100} == {io.price for io in invoice.invoice_orders}
    assert {10} == {io.discount_pct for io in invoice.invoice_orders}
    assert [D('11.111111'), D('1.366667')] == [io.quantity for io in invoice.invoice_orders]
    assert [D('10'), D('1.23')] == [io.initial_quantity for io in invoice.invoice_orders]


def test_turn_on(session, core_obj, client, paysys):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=100,
        orders_qtys=[1],
    )
    invoice.receipt_sum_1c = 90
    session.flush()

    core_obj.patch_invoice_sum(invoice.id, 90)

    assert invoice.effective_sum == 90
    assert invoice.receipt_sum == 90
    assert invoice.consume_sum == 90


def test_invoice_order_patch(session, core_obj, client, paysys):
    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=100,
        orders_qtys=[1, 2, 3],
    )

    invoice_order = invoice.invoice_orders[1]
    invoice_order.patch(100)
    session.expire_all()

    assert invoice.effective_sum == 500
    assert [io.amount for io in invoice.invoice_orders] == [100, 100, 300]
    assert {io.price for io in invoice.invoice_orders} == {100}
    assert {io.discount_pct for io in invoice.invoice_orders} == {0}
    assert [io.quantity for io in invoice.invoice_orders] == [1, 1, 3]
    assert [io.initial_quantity for io in invoice.invoice_orders] == [1, 1, 3]
