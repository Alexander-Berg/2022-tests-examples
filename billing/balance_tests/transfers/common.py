# -*- coding: utf-8 -*-

import datetime

import mock

from balance import mapper
from balance.actions import acts as a_a
from balance.actions.transfers_qty.interface import (
    SrcItem,
    DstItem,
)
from balance.constants import (
    DIRECT_PRODUCT_ID,
    TransferMode,
)

from tests import object_builder as ob


def create_order(client, consume_qtys=None, product_id=DIRECT_PRODUCT_ID):
    session = client.session
    order = ob.OrderBuilder.construct(session, client=client, agency=client.agency, product_id=product_id)
    invoice_owner = client.agency or client

    for qty in consume_qtys or []:
        invoice = ob.InvoiceBuilder.construct(
            session,
            person=ob.PersonBuilder(client=invoice_owner, type='ur'),
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=invoice_owner,
                    rows=[ob.BasketItemBuilder(order=order, quantity=qty)]
                )
            )
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

    return order


def complete_act(order, completion_qty=0, act_qty=0):
    if act_qty:
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: act_qty})
        order.session.flush()
        a_a.ActAccounter(
            (order.agency or order.client),
            mapper.ActMonth(for_month=datetime.datetime.now()),
            force=1
        ).do()
        order.session.flush()

    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: completion_qty})
    order.session.flush()


@mock.patch('balance.mapper.invoices.Invoice.has_forced_discount', True)
def create_src(client, products, old_qty, new_qty, discounts=None, paysys_id=1000):
    session = client.session
    paysys = ob.Getter(mapper.Paysys, paysys_id).build(session).obj
    invoice_owner = client.agency or client

    if not isinstance(old_qty[0], list):
        old_qty = [old_qty]

    if not isinstance(products, list):
        products = [products] * len(old_qty[0])

    if discounts is None:
        discounts = [0] * len(old_qty[0])

    orders = [create_order(client, product_id=pr.id) for pr in products]

    for qty_parts in old_qty:
        invoice = ob.InvoiceBuilder(
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=invoice_owner,
                    rows=[
                        ob.BasketItemBuilder(
                            order=order,
                            quantity=qty,
                            forced_discount_pct=pct,
                        )
                        for qty, pct, order in zip(qty_parts, discounts, orders)
                        if qty and qty > 0
                    ]
                )
            ),
            paysys=paysys,
            person=ob.PersonBuilder(client=invoice_owner, type=paysys.category).build(session).obj,
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

    src_list = [
        SrcItem(order.consume_qty - qty, order.consume_qty, order)
        for qty, order in zip(new_qty, orders)
    ]

    return orders, src_list


def create_dst(client, products, qty_parts):
    if not isinstance(products, list):
        products = [products] * len(qty_parts)

    orders = [create_order(client, product_id=pr.id) for pr in products]

    dst_list = [
        DstItem(qty_part, order)
        for qty_part, order in zip(qty_parts, orders)
    ]

    return orders, dst_list


def consume_order(invoice, order, qty):
    return invoice.transfer(order, TransferMode.dst, qty, skip_check=True).consume
