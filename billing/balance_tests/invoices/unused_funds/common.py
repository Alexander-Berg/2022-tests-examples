# -*- coding: utf-8 -*-

import collections

from balance import mapper
from balance.constants import *
from balance.mapper import ClientUnusedFunds
from tests import object_builder as ob

PAYSYS_ID_BANK = 1001
PAYSYS_ID_CARD = 1002


def create_order(client, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_RUB_ID, firm_id=FirmId.YANDEX_OOO):
    return ob.OrderBuilder(
        client=client,
        firm_id=firm_id,
        service_id=service_id,
        product=ob.Getter(mapper.Product, product_id),
    ).build(client.session).obj


def create_request(client, orders, quantity=666):
    session = client.session
    if not isinstance(orders, collections.Iterable):
        orders = [orders]

    return ob.RequestBuilder(
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(quantity=quantity, order=order)
                for order in orders
            ]
        )
    ).build(session).obj


def create_invoice(
    session=None,
    client=None,
    orders=None,
    quantity=666,
    overdraft=0,
    paysys_id=PAYSYS_ID_BANK,
    person=None,
    firm_id=FirmId.YANDEX_OOO,
):  # type: (...) -> mapper.Invoice
    session = session or client.session
    client = client or ob.ClientBuilder().build(session).obj
    orders = orders or [create_order(client, firm_id=firm_id)]
    request = create_request(client, orders, quantity)
    paysys = ob.Getter(mapper.Paysys, paysys_id).build(session).obj

    return ob.InvoiceBuilder(
        overdraft=overdraft,
        paysys=paysys,
        person=person or ob.PersonBuilder(client=client, type=paysys.category),
        request=request
    ).build(session).obj


def get_unused_funds_cache(
    session,
    invoice_id,  # type: int
):  # type: (...) -> list[ClientUnusedFunds]
    return session.query(ClientUnusedFunds).filter_by(invoice_id=invoice_id).all()
