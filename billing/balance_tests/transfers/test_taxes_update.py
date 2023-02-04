# -*- coding: utf-8 -*-

import datetime
import decimal
import collections

import hamcrest
import pytest

import balance.muzzle_util as ut
from balance.actions.transfers_qty.interface import (
    TransferMultiple,
    SrcItem,
    DstItem,
)
from butils import decimal_unit

from tests import base_routine
from tests import object_builder as ob

D = decimal.Decimal
DU = decimal_unit.DecimalUnit

PAST = datetime.datetime(2000, 1, 1)
PRESENT = ut.trunc_date(datetime.datetime.now())
NEAR_PAST = PRESENT - datetime.timedelta(1)

pytestmark = [
    pytest.mark.taxes_update,
]


@pytest.fixture
def tax_policy(session):
    return ob.TaxPolicyBuilder(
        tax_pcts=[
            (PAST, 18),
            (PRESENT, 20),
        ]
    ).build(session).obj


def consumes_match(consumes_states):
    return base_routine.consumes_match(
        consumes_states,
        forced_params=[
            'current_qty',
            'current_sum',
            'tax_policy_pct_id',
            'price_id',
            'price',
        ]
    )


def create_order(session, client, product, qty=None, on_dt=None):
    order = ob.OrderBuilder(
        product=product,
        client=client
    ).build(session).obj

    if qty:
        invoice = ob.InvoiceBuilder(
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=client,
                    rows=[ob.BasketItemBuilder(order=order, quantity=qty)]
                )
            ),
            dt=on_dt or PRESENT
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(on_dt=on_dt)

    return order


def create_product_incl_tax(session, tax_policy, prices=30):
    price1, price2 = prices if isinstance(prices, collections.Iterable) else [prices] * 2
    tpp1, tpp2 = tax_policy.taxes
    return ob.ProductBuilder(
        taxes=tax_policy,
        prices=[
            (PAST, 'RUR', price1, tpp1),
            (PRESENT, 'RUR', price2, tpp2),
        ]
    ).build(session).obj


def create_product_wo_tax(session, tax_policy, price=30):
    return ob.ProductBuilder(
        taxes=tax_policy,
        prices=[
            (PAST, 'RUR', price),
        ]
    ).build(session).obj


def do_transfer(src_order, dst_order, qty, on_dt=None):
    return TransferMultiple(
        src_order.session,
        [SrcItem(qty, src_order.consume_qty, src_order)],
        [DstItem(1, dst_order)],
        on_dt=on_dt
    ).do()


@pytest.mark.parametrize(
    'on_dt', [NEAR_PAST, None],
    ids=['before', 'before_after']
)
def test_same_product(session, tax_policy, client, on_dt):
    product = create_product_wo_tax(session, tax_policy)
    price, = product.prices
    tpp1, tpp2 = tax_policy.taxes

    src_order = create_order(session, client, product, 666, NEAR_PAST)
    dst_order = create_order(session, client, product)

    do_transfer(src_order, dst_order, 66, on_dt)

    hamcrest.assert_that(
        src_order.consumes,
        consumes_match(
            [
                (600, 600 * 30 * D('1.18'), tpp1.id, price.id, 30 * D('1.18'))
            ]
        )
    )
    hamcrest.assert_that(
        dst_order.consumes,
        consumes_match(
            [
                (66, 66 * 30 * D('1.18'), tpp1.id, price.id, 30 * D('1.18'))
            ]
        )
    )


def test_different_product_before_before(session, tax_policy, client):
    tpp1, tpp2 = tax_policy.taxes

    src_product = create_product_wo_tax(session, tax_policy)
    src_price, = src_product.prices

    dst_product = create_product_wo_tax(session, tax_policy)
    dst_price, = dst_product.prices

    src_order = create_order(session, client, src_product, 666, NEAR_PAST)
    dst_order = create_order(session, client, dst_product)

    do_transfer(src_order, dst_order, 66, NEAR_PAST)

    hamcrest.assert_that(
        src_order.consumes,
        consumes_match(
            [
                (600, 600 * 30 * D('1.18'), tpp1.id, src_price.id, 30 * D('1.18'))
            ]
        )
    )
    hamcrest.assert_that(
        dst_order.consumes,
        consumes_match(
            [
                (66, 66 * 30 * D('1.18'), tpp1.id, dst_price.id, 30 * D('1.18'))
            ]
        )
    )


def test_different_product_before_after_tax_added(session, tax_policy, client):
    tpp1, tpp2 = tax_policy.taxes

    src_product = create_product_wo_tax(session, tax_policy)
    src_price, = src_product.prices

    dst_product = create_product_wo_tax(session, tax_policy)
    dst_price, = dst_product.prices

    src_order = create_order(session, client, src_product, 666, NEAR_PAST)
    dst_order = create_order(session, client, dst_product)

    do_transfer(src_order, dst_order, 66)

    hamcrest.assert_that(
        src_order.consumes,
        consumes_match(
            [
                (600, 600 * 30 * D('1.18'), tpp1.id, src_price.id, 30 * D('1.18'))
            ]
        )
    )
    hamcrest.assert_that(
        dst_order.consumes,
        consumes_match(
            [
                (D('64.9'), D('64.9') * 30 * D('1.2'), tpp2.id, dst_price.id, 30 * D('1.2'))
            ]
        )
    )


def test_different_product_before_after_tax_included(session, tax_policy, client):
    tpp1, tpp2 = tax_policy.taxes

    src_product = create_product_incl_tax(session, tax_policy)
    src_price1, src_price2 = src_product.prices

    dst_product = create_product_incl_tax(session, tax_policy)
    dst_price1, dst_price2 = dst_product.prices

    src_order = create_order(session, client, src_product, 666, NEAR_PAST)
    dst_order = create_order(session, client, dst_product)

    do_transfer(src_order, dst_order, 66)

    hamcrest.assert_that(
        src_order.consumes,
        consumes_match(
            [
                (600, 600 * 30, tpp1.id, src_price1.id, 30)
            ]
        )
    )
    hamcrest.assert_that(
        dst_order.consumes,
        consumes_match(
            [
                (66, 66 * 30, tpp2.id, dst_price2.id, 30)
            ]
        )
    )


def test_different_product_before_after_price_change(session, tax_policy, client):
    tpp1, tpp2 = tax_policy.taxes

    src_product = create_product_incl_tax(session, tax_policy)
    src_price1, src_price2 = src_product.prices

    dst_product = ob.ProductBuilder(
        taxes=tax_policy,
        prices=[
            (PAST, 'RUR', 30, tpp1),
            (PRESENT, 'RUR', D('66.6'), None),
        ]
    ).build(session).obj
    dst_price1, dst_price2 = dst_product.prices

    src_order = create_order(session, client, src_product, 666, NEAR_PAST)
    dst_order = create_order(session, client, dst_product)

    do_transfer(src_order, dst_order, 66)

    hamcrest.assert_that(
        src_order.consumes,
        consumes_match(
            [
                (600, 600 * 30, tpp1.id, src_price1.id, 30)
            ]
        )
    )
    hamcrest.assert_that(
        dst_order.consumes,
        consumes_match(
            [
                (D('24.774775'), D('1980'), tpp2.id, dst_price2.id, D('66.6') * D('1.2'))
            ]
        )
    )


def test_different_product_from_after_on_before(session, tax_policy, client):
    tpp1, tpp2 = tax_policy.taxes

    src_product = create_product_incl_tax(session, tax_policy)
    src_price1, src_price2 = src_product.prices

    dst_product = create_product_incl_tax(session, tax_policy)
    dst_price1, dst_price2 = dst_product.prices

    src_order = create_order(session, client, src_product, 666, PRESENT)
    dst_order = create_order(session, client, dst_product)

    do_transfer(src_order, dst_order, 66, NEAR_PAST)

    hamcrest.assert_that(
        src_order.consumes,
        consumes_match(
            [
                (600, 600 * 30, tpp2.id, src_price2.id, 30)
            ]
        )
    )
    hamcrest.assert_that(
        dst_order.consumes,
        consumes_match(
            [
                (66, 66 * 30, tpp1.id, dst_price1.id, 30)
            ]
        )
    )


def test_from_mixed_tax_products(session, tax_policy, client):
    # продукты
    tpp1, tpp2 = tax_policy.taxes

    product1 = create_product_incl_tax(session, tax_policy)
    price1_old, price1_new = product1.prices

    product2 = create_product_incl_tax(session, tax_policy)
    price2_old, price2_new = product1.prices

    # заказы
    src_order1 = create_order(session, client, product1)
    src_order2 = create_order(session, client, product2)
    dst_order = create_order(session, client, product1)

    # счёт
    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=src_order1, quantity=666)]
            )
        ),
        dt=NEAR_PAST
    ).build(session).obj
    invoice.create_receipt(invoice.effective_sum)

    # зачисления
    invoice.transfer(src_order1, 2, 5, invoice.internal_price(src_order1, NEAR_PAST))
    invoice.transfer(src_order2, 2, 6, invoice.internal_price(src_order2, PRESENT))

    # перенос
    TransferMultiple(
        session,
        [
            SrcItem(5, src_order1.consume_qty, src_order1),
            SrcItem(6, src_order2.consume_qty, src_order2),
        ],
        [DstItem(1, dst_order)],
    ).do()

    hamcrest.assert_that(
        sorted(dst_order.consumes, key=lambda co: co.current_qty),
        consumes_match(
            [
                (5, 5 * 30, tpp1.id, price1_old.id, 30),
                (6, 6 * 30, tpp2.id, price2_new.id, 30),
            ]
        )
    )


def test_from_mixed_tax_consumes(session, tax_policy, client):
    # продукты
    tpp1, tpp2 = tax_policy.taxes

    src_product = create_product_incl_tax(session, tax_policy)

    dst_product = create_product_incl_tax(session, tax_policy)
    _, price_new = dst_product.prices

    # заказы
    src_order = create_order(session, client, src_product)
    dst_order = create_order(session, client, dst_product)

    # счёт
    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=src_order, quantity=666)]
            )
        ),
        dt=NEAR_PAST
    ).build(session).obj
    invoice.create_receipt(invoice.effective_sum)

    # зачисления
    invoice.transfer(src_order, 2, 6, invoice.internal_price(src_order, NEAR_PAST))
    invoice.transfer(src_order, 2, 7, invoice.internal_price(src_order, PRESENT))

    # перенос
    do_transfer(src_order, dst_order, 13)

    hamcrest.assert_that(
        dst_order.consumes,
        consumes_match(
            [
                (13, 13 * 30, tpp2.id, price_new.id, 30),
            ]
        )
    )


@pytest.mark.ua_mode_transfers
def test_from_mixed_tax_consumes_ua(session, tax_policy, client):
    # продукты
    tpp1, tpp2 = tax_policy.taxes

    src_product = create_product_incl_tax(session, tax_policy)

    dst_product = create_product_incl_tax(session, tax_policy)
    _, price_new = dst_product.prices

    # заказы
    src_order = create_order(session, client, src_product)
    dst_order1 = create_order(session, client, dst_product)
    dst_order2 = create_order(session, client, dst_product)

    # счёт
    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=src_order, quantity=666)]
            )
        ),
        dt=NEAR_PAST
    ).build(session).obj
    invoice.create_receipt(invoice.effective_sum)

    # зачисления
    invoice.transfer(src_order, 2, 6, invoice.internal_price(src_order, NEAR_PAST))
    invoice.transfer(src_order, 2, 7, invoice.internal_price(src_order, PRESENT))

    # перенос
    TransferMultiple(
        src_order.session,
        [SrcItem(13, src_order.consume_qty, src_order)],
        [
            DstItem(10, dst_order1),
            DstItem(3, dst_order2)
        ],
        ua_mode=True
    ).do()

    hamcrest.assert_that(
        dst_order1.consumes,
        consumes_match(
            [
                (10, 10 * 30, tpp2.id, price_new.id, 30),
            ]
        )
    )
    hamcrest.assert_that(
        dst_order2.consumes,
        consumes_match(
            [
                (3, 3 * 30, tpp2.id, price_new.id, 30),
            ]
        )
    )
