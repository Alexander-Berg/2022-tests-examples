# coding: utf-8

from decimal import Decimal

import pytest

from balance import constants as cst
from tests import object_builder as ob


@pytest.fixture(name='order')
def create_order(session, client, service_id=cst.ServiceId.DIRECT,
                 product_id=cst.DIRECT_PRODUCT_RUB_ID, **kw):
    return ob.OrderBuilder.construct(
        session,
        client=client,
        service_id=service_id,
        product_id=product_id,
        **kw
    )


@pytest.fixture(name='invoice')
def create_invoice(session, client, person=None, request_=None, **kwargs):
    request_ = request_ or create_request(session, client,
                                          [(create_order(session, client), Decimal('100'))])
    return ob.InvoiceBuilder.construct(
        session,
        request=request_,
        person=person,
        **kwargs
    )


def create_request(session, client, orders):
    return ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(order=o, quantity=qty)
                for o, qty in orders
            ],
        ),
    )


def create_cashback(client, **kw):
    cb = ob.ClientCashbackBuilder.construct(
        client.session,
        client=client,
        **kw
    )
    client.session.expire(client)
    return cb
