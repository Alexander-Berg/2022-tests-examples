# -*- coding: utf-8 -*-

import collections
import contextlib
import decimal

import datetime

from balance import mapper, core
from balance.actions import acts as a_a
from tests import object_builder as ob

D = decimal.Decimal
AUCTION_UNIT_ID = 0
SHOWS_1000_UNIT_ID = 799
UNITS_1000_UNIT_ID = 796


def _create_price(session, dt, product, price):
    return ob.PriceBuilder(
        dt=dt,
        product=product,
        price=price
    ).build(session)


def _create_person(client):
    return ob.PersonBuilder(client=client).build(client.session).obj


def _create_product(session, unit_id, price):
    return ob.ProductBuilder(
        unit=ob.Getter(mapper.ProductUnit, unit_id),
        price=price
    ).build(session).obj


def _create_orders(client, products):
    if not isinstance(products, collections.Iterable):
        products = [products]

    return [
        ob.OrderBuilder(client=client, product=product).build(client.session).obj
        for product in products
    ]


def _create_request(orders_qtys):
    session = orders_qtys[0][0].session

    basket = ob.BasketBuilder(
        rows=[
            ob.BasketItemBuilder(quantity=qty, order=order)
            for order, qty in orders_qtys
        ]
    )
    return ob.RequestBuilder(basket=basket).build(session).obj


def _create_invoice(request, paysys, person, contract, dt=None):
    if dt is None:
        dt = datetime.datetime.now()

    return ob.InvoiceBuilder(
        request=request,
        paysys=paysys,
        person=person,
        contract=contract,
        dt=dt
    ).build(request.session).obj


def _create_contract(session, person):
    return ob.ContractBuilder(
        client=person.client,
        person=person,
        services={7},
        firm_id=1,
        is_signed=datetime.datetime.now(),
    ).build(session).obj


@contextlib.contextmanager
def _patch_discount(pct, adjust_quantity=False):
    from balance import discounts
    old_func = discounts.calc_from_ns
    discounts.calc_from_ns = lambda ns: [
        discounts.DiscountProof('patch', discount=pct, adjust_quantity=adjust_quantity),
        None, None
    ]

    yield

    discounts.calc_from_ns = old_func


def _init_invoice(session,
                  client, paysys, contract=None,
                  unit_id=AUCTION_UNIT_ID, price=100, discount_pct=0, adjust_quantity=False,
                  orders_qtys=None):
    if orders_qtys is None:
        orders_qtys = [10]

    product = _create_product(session, unit_id, price)
    orders = _create_orders(client, [product] * len(orders_qtys))
    request = _create_request([(order, qty) for order, qty in zip(orders, orders_qtys)])
    person = contract and contract.person or _create_person(client)

    with _patch_discount(discount_pct, adjust_quantity):
        invoice = _create_invoice(request, paysys, person, contract)

    return invoice


def create_y_invoice(session, person, qty, paysys_id=1003):
    contract = ob.ContractBuilder(
        client=person.client,
        person=person,
        commission=0,
        firm=1,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=session.now(),
    ).build(session).obj
    contract.client.is_agency = 1
    subclient = ob.ClientBuilder.construct(session)
    order = ob.OrderBuilder(product=ob.Getter(mapper.Product, 1475),
                            service=ob.Getter(mapper.Service, 7),
                            client=subclient,
                            agency=contract.client
                            ).build(session).obj
    basket = ob.BasketBuilder(
        client=contract.client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=qty)
            for o, qty in [(order, 10)]
        ]
    )
    request = ob.RequestBuilder(basket=basket).build(session).obj
    coreobj = core.Core(request.session)
    pa, = coreobj.pay_on_credit(
        request_id=request.id,
        paysys_id=paysys_id,
        person_id=contract.person.id,
        contract_id=contract.id
    )
    request.session.flush()
    now = datetime.datetime.now()
    order.calculate_consumption(now, {order.shipment_type: 10})
    act, = a_a.ActAccounter(
        pa.client,
        mapper.ActMonth(for_month=now),
        invoices=[pa.id], dps=[],
        force=1
    ).do()
    invoice = act.invoice
    pa.session.flush()
    return invoice
