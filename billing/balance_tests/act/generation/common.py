# -*- coding: utf-8 -*-

from balance import mapper
from balance import core
import balance.actions.acts as a_a
from balance.constants import (
    DIRECT_PRODUCT_ID,
)

from tests import object_builder as ob


def generate_act(invoice, on_dt, split_act_creation=False):
    if isinstance(on_dt, mapper.ActMonth):
        kwargs = {'act_month': on_dt}
    else:
        kwargs = {'backdate': on_dt}
    res = a_a.ActAccounter(
        invoice.client,
        dps=[],
        invoices=[invoice.id],
        force=1,
        split_act_creation=split_act_creation,
        **kwargs
    ).do()
    invoice.session.flush()
    return res


def calculate_consumption(order, on_dt, qty):
    order.calculate_consumption(on_dt, {order.shipment_type: qty})
    order.session.flush()


def create_consume(invoice, order, qty, price_obj=None):
    res = invoice.transfer(order, 2, qty, price_obj, skip_check=True)
    return res.consume


def create_invoice(session, client, orders, paysys=1003, qty=9, turn_on=True):
    invoice = ob.InvoiceBuilder(
        paysys=ob.Getter(mapper.Paysys, paysys),
        person=ob.PersonBuilder(client=client),
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=qty) for order in orders]))
    ).build(session).obj
    if turn_on:
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(on_dt=invoice.dt)
    return invoice


def create_order(session, client, product=DIRECT_PRODUCT_ID, agency=None):
    return ob.OrderBuilder(
        client=client,
        agency=agency,
        product=ob.Getter(mapper.Product, product)
    ).build(session).obj


def consume_credit(contract, orders, paysys_id=1003, dt=None):
    session = contract.session

    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(
            client=contract.client,
            dt=None,
            rows=[
                ob.BasketItemBuilder(quantity=qty, order=order)
                for order, qty in orders
            ]
        )
    ).build(session).obj

    pa, = core.Core(session).pay_on_credit(request.id, paysys_id, contract.person_id, contract.id)
    session.flush()
    return pa


def get_act_act_enqueuer(session, month, force, op_type, client_ids=None):
    return a_a.ActEnqueuer(session, month, force=force, op_type=op_type, client_ids=client_ids)
