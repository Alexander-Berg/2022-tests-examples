# -*- coding: utf-8 -*-
from balance.publisher import fetch as publisher_fetch
import pytest
from tests import object_builder as ob
import datetime
from balance import mapper
from balance.constants import DIRECT_PRODUCT_ID

NOW = datetime.datetime.now()


def create_act(session):
    client = ob.ClientBuilder()
    orders = [ob.OrderBuilder(product=ob.Getter(mapper.Product, DIRECT_PRODUCT_ID),
                              client=client).build(session).obj for _ in range(2)]
    basket_b = ob.BasketBuilder(rows=[ob.BasketItemBuilder(order=order, quantity=10) for order in orders])
    request_b = ob.RequestBuilder(basket=basket_b)
    invoice = ob.InvoiceBuilder(request=request_b).build(session).obj
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice.dt)
    for order in orders:
        order.calculate_consumption(NOW, {order.shipment_type: 10})
    act, = invoice.generate_act(backdate=NOW, force=1)

    session.flush()
    return act


def test_act_grouped_products(session):
    act = create_act(session)
    act.rows[0].product_id = 508587
    session.flush()
    xml = publisher_fetch.get_data_for_publisher(session, act.id, NOW, 'act')
    products = []
    for product in xml.findall('invoice/products/grouped-product'):
        product_attrs = {}
        for tag in product:
            if tag.tag in ['id', 'name', 'unit']:
                product_attrs[tag.tag] = tag.text
        products.append(product_attrs)
    assert sorted(products) == sorted([{'id': u'508587', 'unit': u'у.е.',
                                        'name': u'Размещение рекламных материалов Яндекс.Директ Медийная реклама.'},
                                       {'id': u'1475', 'unit': u'у.е.',
                                        'name': u'Услуги «Яндекс.Директ».'}])
