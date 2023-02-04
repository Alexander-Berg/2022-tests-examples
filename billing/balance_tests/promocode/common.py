# -*- coding: utf-8 -*-

import mock
import datetime
import collections

from balance import mapper
from balance import constants as cst
from balance.actions import promocodes
from balance import discounts

from tests import object_builder as ob

NOW = datetime.datetime.now().replace(microsecond=0)


def create_order(session, client=None, product_id=cst.DIRECT_PRODUCT_RUB_ID, dt=NOW, service_id=cst.ServiceId.DIRECT):
    return ob.OrderBuilder.construct(
        session,
        dt=dt,
        client=client or create_client(session),
        product=ob.Getter(mapper.Product, product_id),
        service=ob.Getter(mapper.Service, service_id),
    )


def create_invoice(session, qty=1, client=None, orders=None, paysys=None, person=None, discount_pct=None,
                   adjust_qty=False, request=None, firm_id=cst.FirmId.YANDEX_OOO, dt=NOW):
    if request is None:
        if orders is None:
            orders = [create_order(session, client=client)]
        if not isinstance(orders, collections.Iterable):
            orders = [orders]
        if not isinstance(qty, collections.Iterable):
            qty = [qty] * len(orders)
        client = orders[0].client

        basket = ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(quantity=q, order=o) for o, q in zip(orders, qty)]
        )

    def _mock_discount(ns):
        return discounts.DiscountProof('mock', discount=discount_pct, adjust_quantity=adjust_qty), None, None

    with mock.patch('balance.discounts.calc_from_ns', _mock_discount):
        invoice = ob.InvoiceBuilder(
            request=request or ob.RequestBuilder(basket=basket, firm_id=firm_id),
            paysys=paysys or ob.Getter(mapper.Paysys, ob.PAYSYS_ID),
            person=person or ob.PersonBuilder(client=client),
            firm_id=firm_id,
            dt=dt,
        ).build(session).obj

    session.flush()
    return invoice


def create_client(session):
    return ob.ClientBuilder.construct(session, service=cst.ServiceId.DIRECT)


def create_multicurrency_client(session, service_id=7, currency='RUB', convert_type=None):
    client = create_client(session)
    client.set_currency(service_id, currency, NOW, convert_type)
    return client


def create_promocode(session, params=None):
    params = params if params is not None else {}
    group = ob.PromoCodeGroupBuilder(**params).build(session).obj
    session.add(group)
    session.flush()
    pc = group.promocodes[0]
    return pc


def reserve_promocode(session, promocode, client, dt=NOW, for_request=False):
    promocodes.reserve_promo_code(client, promocode, dt, for_request=for_request)
    session.flush()
    return promocode.reservations[0]


def complete(order, qty):
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
    order.session.flush()
