# -*- coding: utf-8 -*-

import datetime
import decimal

import pytest
import mock

from balance import mapper
import balance.muzzle_util as ut
from balance.actions.transfers_qty.interface import (
    TransferMultiple,
    SrcItem,
    DstItem,
)
from balance.constants import *
from butils import decimal_unit

from tests import object_builder as ob

pytestmark = [
    pytest.mark.ua_mode_transfers,
]

D = decimal.Decimal
DU = decimal_unit.DecimalUnit

PRESENT = ut.trunc_date(datetime.datetime.now())
NEAR_PAST = PRESENT - datetime.timedelta(1)

PAYSYS_BANK_USD_NONRES = 1013
PAYSYS_BANK_RUB_NONRES = 1014


@pytest.fixture(autouse=True)
def patch_currency():
    dt_rates = [
        {
            'RUR': DU(1),
            'RUB': DU(1),
            'USD': DU('30', 'RUB', 'USD'),
        },
        (
            PRESENT,
            {
                'RUR': DU(1),
                'RUB': DU(1),
                'USD': DU('60', 'RUB', 'USD'),
            }
        )
    ]
    with ob.patched_currency(dt_rates):
        yield


@pytest.fixture
def tax_policy(session):
    return ob.Getter(mapper.TaxPolicy, TaxPolicyId.RUS_STANDARD_NDS).build(session).obj


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder.construct(session, client=client, person_type='yt')


def create_order(client, product):
    return ob.OrderBuilder.construct(client.session, product=product, client=client)


def consume_order(order, person, qty, paysys_id, on_dt=None):
    invoice = ob.InvoiceBuilder(
        paysys=ob.Getter(mapper.Paysys, paysys_id),
        person=person,
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=person.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=qty)]
            )
        ),
        dt=on_dt or PRESENT
    ).build(order.session).obj
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=on_dt)
    return invoice


def test_different_currencies(session, client, person, tax_policy):
    product = ob.ProductBuilder.construct(
        session,
        taxes=tax_policy,
        unit=ob.Getter(mapper.ProductUnit, USD_UNIT_ID),
        prices=[]
    )

    src_order = create_order(client, product)
    dst_order1 = create_order(client, product)
    dst_order2 = create_order(client, product)

    consume_order(src_order, person, 3, PAYSYS_BANK_RUB_NONRES)
    consume_order(src_order, person, 10, PAYSYS_BANK_USD_NONRES)

    TransferMultiple(
        session,
        [SrcItem(7, src_order.consume_qty, src_order)],
        [
            DstItem(3, dst_order1),
            DstItem(4, dst_order2),
        ],
        old_consumes_first=True,
        ua_mode=True
    ).do()

    assert dst_order1.consume_qty == 3
    assert dst_order2.consume_qty == 4


def test_same_currency_different_rates(session, client, person, tax_policy):
    product = ob.ProductBuilder.construct(
        session,
        taxes=tax_policy,
        unit=ob.Getter(mapper.ProductUnit, USD_UNIT_ID),
        prices=[]
    )

    src_order = create_order(client, product)
    dst_order1 = create_order(client, product)
    dst_order2 = create_order(client, product)

    invoice1 = consume_order(src_order, person, 2, PAYSYS_BANK_RUB_NONRES, NEAR_PAST)
    invoice2 = consume_order(src_order, person, 2, PAYSYS_BANK_USD_NONRES, PRESENT)
    invoice3 = consume_order(src_order, person, 10, PAYSYS_BANK_RUB_NONRES, PRESENT)

    TransferMultiple(
        session,
        [SrcItem(7, src_order.consume_qty, src_order)],
        [
            DstItem(3, dst_order1),
            DstItem(4, dst_order2),
        ],
        old_consumes_first=True,
        ua_mode=True
    ).do()

    assert dst_order1.consume_qty == 3
    assert dst_order2.consume_qty == 4

    assert invoice1.consume_sum == 60
    assert sum(q.current_qty for q in invoice1.consumes) == 2

    assert invoice2.consume_sum == 2
    assert sum(q.current_qty for q in invoice2.consumes) == 2

    assert invoice3.consume_sum == 600
    assert sum(q.current_qty for q in invoice3.consumes) == 10


def test_different_products(session, client, person, tax_policy):
    src_product = ob.ProductBuilder.construct(
        session,
        taxes=tax_policy,
        unit=ob.Getter(mapper.ProductUnit, AUCTION_UNIT_ID),
        prices=[(NEAR_PAST, 'RUR', 1, tax_policy.taxes[-1])]
    )
    dst_product = ob.ProductBuilder.construct(
        session,
        taxes=tax_policy,
        unit=ob.Getter(mapper.ProductUnit, AUCTION_UNIT_ID),
        prices=[(NEAR_PAST, 'RUR', 1, tax_policy.taxes[-1])]
    )

    src_order = create_order(client, src_product)
    dst_order1 = create_order(client, dst_product)
    dst_order2 = create_order(client, dst_product)

    invoice1 = consume_order(src_order, person, 200, PAYSYS_BANK_RUB_NONRES, NEAR_PAST)
    invoice2 = consume_order(src_order, person, 200, PAYSYS_BANK_USD_NONRES, NEAR_PAST)
    invoice3 = consume_order(src_order, person, 1000, PAYSYS_BANK_RUB_NONRES, PRESENT)

    TransferMultiple(
        session,
        [SrcItem(700, src_order.consume_qty, src_order)],
        [
            DstItem(300, dst_order1),
            DstItem(400, dst_order2),
        ],
        old_consumes_first=True,
        ua_mode=True
    ).do()

    assert dst_order1.consume_qty == 300
    assert all(q.price_mapper.product == dst_product for q in dst_order1.consumes)

    assert dst_order2.consume_qty == 400
    assert all(q.price_mapper.product == dst_product for q in dst_order1.consumes)

    assert invoice1.consume_sum == 200
    assert sum(q.current_qty for q in invoice1.consumes) == 200

    assert invoice2.consume_sum == D('6.67')
    assert sum(q.current_qty for q in invoice2.consumes) == 200

    assert invoice3.consume_sum == 1000
    assert sum(q.current_qty for q in invoice3.consumes) == 1000


def test_different_currency_products(session, client, person, tax_policy):
    src_product = ob.ProductBuilder.construct(
        session,
        taxes=tax_policy,
        unit=ob.Getter(mapper.ProductUnit, USD_UNIT_ID),
        prices=[]
    )
    dst_product = ob.ProductBuilder.construct(
        session,
        taxes=tax_policy,
        unit=ob.Getter(mapper.ProductUnit, USD_UNIT_ID),
        prices=[]
    )

    src_order = create_order(client, src_product)
    dst_order1 = create_order(client, dst_product)
    dst_order2 = create_order(client, dst_product)

    invoice1 = consume_order(src_order, person, 2, PAYSYS_BANK_RUB_NONRES, NEAR_PAST)
    invoice2 = consume_order(src_order, person, 2, PAYSYS_BANK_USD_NONRES, PRESENT)
    invoice3 = consume_order(src_order, person, 10, PAYSYS_BANK_RUB_NONRES, PRESENT)

    TransferMultiple(
        session,
        [SrcItem(7, src_order.consume_qty, src_order)],
        [
            DstItem(3, dst_order1),
            DstItem(4, dst_order2),
        ],
        old_consumes_first=True,
        ua_mode=True
    ).do()

    assert dst_order1.consume_qty == 3
    assert dst_order2.consume_qty == 4

    assert invoice1.consume_sum == 60
    assert sum(q.current_qty for q in invoice1.consumes) == 2

    assert invoice2.consume_sum == 2
    assert sum(q.current_qty for q in invoice2.consumes) == 2

    assert invoice3.consume_sum == 600
    assert sum(q.current_qty for q in invoice3.consumes) == 10
