# -*- coding: utf-8 -*-

import datetime as dt

from balance import mapper
from balance.actions.consumption import (
    reverse_consume,
)
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    TransferMode,
)

from tests import object_builder as ob

PAYSYS_ID_BANK_PH = 1001
PAYSYS_ID_CARD_PH = 1002
PAYSYS_ID_BANK_UR = 1003


def create_client(session):
    return ob.ClientBuilder.construct(session)


def create_invoice(session, client, order, paysys_id=PAYSYS_ID_BANK_PH):
    paysys = ob.Getter(mapper.Paysys, paysys_id).build(session).obj
    person = ob.PersonBuilder.construct(session, client=client, type=paysys.category)

    basket = ob.BasketBuilder(
        client=client,
        rows=[ob.BasketItemBuilder(quantity=10000, order=order)]
    )
    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(basket=basket),
        paysys=paysys,
        person=person
    ).build(session).obj
    invoice.create_receipt(invoice.effective_sum)
    return invoice


def create_order(session, client, product_id=DIRECT_PRODUCT_RUB_ID):
    return ob.OrderBuilder(client=client, product=ob.Getter(mapper.Product, product_id)).build(session).obj


def extract_qtys(consumes):
    return [(co.id, qty) for co, qty in consumes]


def do_complete(order, qty):
    order.calculate_consumption(dt.datetime.now(), {order.shipment_type: qty})
    order.session.flush()


def do_consume(invoice, order, qty):
    return invoice.transfer(order, TransferMode.dst, qty).consume


def do_act(invoice):
    res = invoice.generate_act(force=1, backdate=dt.datetime.now())
    invoice.session.flush()
    return res


def do_consume_state(invoice, order, current_qty, completion_qty, act_qty):
    consume = do_consume(invoice, order, act_qty)
    do_complete(order, act_qty)
    do_act(invoice)
    do_complete(order, completion_qty)
    reverse_consume(consume, None, consume.current_qty - current_qty)
    return consume
