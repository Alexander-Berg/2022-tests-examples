# -*- coding: utf-8 -*-
import datetime
import pytest

from balance import mapper, core
from balance.actions import acts as a_a
from balance.processors.oebs_api.utils import convert_DU
from balance.processors.oebs_api.wrappers import TransactionWrapper
from balance.publisher import fetch as publisher_fetch
from butils import decimal_unit
from tests import object_builder as ob

DU = decimal_unit.DecimalUnit

NOW = datetime.datetime.now()


@pytest.fixture
def person(session):
    return ob.PersonBuilder.construct(session)


@pytest.fixture
def contract(session, person):
    return ob.ContractBuilder(
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


def create_y_invoice(session, contract, orders=None):
    if not orders:
        contract.client.is_agency = 1
        orders = [ob.OrderBuilder(product=ob.Getter(mapper.Product, 1475),
                                  service=ob.Getter(mapper.Service, 7),
                                  client=ob.ClientBuilder.construct(session),
                                  agency=contract.client
                                  ).build(session).obj]
    basket = ob.BasketBuilder(
        client=contract.client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=qty)
            for order in orders for o, qty in [(order, 10)]
        ]
    )
    request = ob.RequestBuilder(basket=basket).build(session).obj
    coreobj = core.Core(request.session)
    pa, = coreobj.pay_on_credit(
        request_id=request.id,
        paysys_id=1003,
        person_id=contract.person.id,
        contract_id=contract.id
    )
    request.session.flush()
    now = datetime.datetime.now()
    for order in orders:
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


def test_y_invoice_product_id(session, contract):
    contract.client.is_agency = 1
    orders = [ob.OrderBuilder(product=ob.Getter(mapper.Product, 1475),
                              service=ob.Getter(mapper.Service, 7),
                              client=ob.ClientBuilder.construct(session),
                              agency=contract.client
                              ).build(session).obj for _ in range(2)]

    y_invoice = create_y_invoice(session, contract, orders)
    act = y_invoice.acts[0]
    act.rows[0].product_id = 503162
    y_invoice._invoice_orders_ = None
    session.flush()
    xml = publisher_fetch.get_data_for_publisher(session, y_invoice.id, NOW, 'invoice')
    products = []

    for product in xml.findall('invoice/products/grouped-product'):
        product_attrs = {}
        for tag in product:
            if tag.tag in ['id', 'name', 'unit']:
                product_attrs[tag.tag] = tag.text
        products.append(product_attrs)
    assert sorted(products) == sorted([{'id': u'1475', 'unit': u'у.е.',
                                        'name': u'Услуги «Яндекс.Директ».'}])
