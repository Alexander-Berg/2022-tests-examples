# -*- coding: utf-8 -*-

import decimal
import pytest
import datetime

from balance import muzzle_util as ut
from tests.balance_tests.core.core_common import (
    _init_invoice,
    _patch_discount,
    AUCTION_UNIT_ID,
    _create_price,
)

D = decimal.Decimal


@pytest.mark.parametrize(
    'old_price, new_price, old_qty, new_qty, new_sum',
    [
        pytest.param(100, 100, 10, 10, 1000, id='simple-unchanged'),
        pytest.param(100, 100, D('6.666667'), D('6.666667'), D('666.67'), id='ounding-unchanged'),
        pytest.param(100, 200, 10, 5, 1000, id='simple-changed'),
        pytest.param(100, 200, D('6.666667'), D('3.333334'), D('666.67'), id='rounding-changed'),
    ]
)
def test_price(session, core_obj, client, paysys, old_price, new_price, old_qty, new_qty, new_sum):
    price_change_dt = ut.trunc_date(datetime.datetime.now()) + datetime.timedelta(5)
    new_dt = price_change_dt + datetime.timedelta(1)

    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=old_price,
        orders_qtys=[old_qty]
    )
    invoice_order, = invoice.invoice_orders
    _create_price(session, dt=price_change_dt,
                  product=invoice_order.order.product,
                  price=new_price)

    core_obj.patch_invoice_date(invoice.id, new_dt)
    session.expire_all()

    assert new_dt == invoice.dt
    assert new_sum == invoice.effective_sum
    assert new_sum == invoice_order.amount
    assert new_price == invoice_order.price
    assert new_qty == invoice_order.quantity
    assert new_qty == invoice_order.initial_quantity


@pytest.mark.parametrize(
    'price, pct, qty, sum_, new_pct, new_qty',
    [
        pytest.param(100, 10, 10, 900, 10, 10, id='rounding-unchanged'),
        pytest.param(100, 10, D('6.666667'), D('600'), 10, D('6.666667'), id='simple-unchanged'),
        pytest.param(100, 10, 10, 900, 20, D('11.25'), id='simple-changed'),
        pytest.param(100, 10, D('6.666667'), D('600'), 15, D('7.058824'), id='rounding-changed'),
    ]
)
def test_discount(session, core_obj, client, paysys, price, pct, qty, sum_, new_pct, new_qty):
    new_dt = ut.trunc_date(datetime.datetime.now()) + datetime.timedelta(1)

    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=price,
        orders_qtys=[qty],
        discount_pct=pct
    )

    with _patch_discount(new_pct):
        core_obj.patch_invoice_date(invoice.id, new_dt)
    session.expire_all()

    assert new_dt == invoice.dt
    assert sum_ == invoice.effective_sum

    invoice_order, = invoice.invoice_orders
    assert sum_ == invoice_order.amount
    assert price == invoice_order.price
    assert new_pct == invoice_order.discount_pct
    assert new_qty == invoice_order.quantity
    assert new_qty == invoice_order.initial_quantity


@pytest.mark.parametrize(
    'price, pct, qty, sum_, new_pct, req_init_qty, req_qty',
    [
        pytest.param(100, 10, 10, 1000, 10, 10, D('11.111111'), id='simple-unchanged'),
        pytest.param(100, 10, D('6.666667'), D('666.67'), 10, D('6.666667'), D('7.407408'), id='rounding-unchanged'),
        pytest.param(100, 10, 10, 1000, 20, 10, D('12.5'), id='simple-changed'),
        pytest.param(100, 10, D('6.666667'), D('666.67'), 15, D('6.666667'), D('7.843138'), id='rounding-changed'),
    ]
)
def test_quantity_discount(session, core_obj, client, paysys, price, pct, qty, sum_, new_pct,
                           req_init_qty, req_qty):
    new_dt = ut.trunc_date(datetime.datetime.now()) + datetime.timedelta(1)

    invoice = _init_invoice(
        session, client, paysys,
        unit_id=AUCTION_UNIT_ID,
        price=price,
        orders_qtys=[qty],
        discount_pct=pct,
        adjust_quantity=True
    )

    with _patch_discount(new_pct, adjust_quantity=True):
        core_obj.patch_invoice_date(invoice.id, new_dt)
    session.expire_all()

    assert new_dt == invoice.dt
    assert sum_ == invoice.effective_sum

    invoice_order, = invoice.invoice_orders
    assert sum_ == invoice_order.amount
    assert price == invoice_order.price
    assert new_pct == invoice_order.discount_pct
    assert req_qty == invoice_order.quantity
    assert req_init_qty == invoice_order.initial_quantity
