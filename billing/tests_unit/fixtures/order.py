# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal as D
import pytest
import mock
import allure
from sqlalchemy.sql.expression import func

from balance import constants as cst, mapper, discounts
import tests.object_builder as ob

from brest.core.tests import utils as test_utils


DEFAULT_ORDER_QTY = D('1')
PRODUCT_CURRENCY = 'RUB'


def create_orders(order_qtys=[DEFAULT_ORDER_QTY], client=None, turn_on=True, firm_id=cst.FirmId.YANDEX_OOO):
    session = test_utils.get_test_session()

    service = ob.Getter(mapper.Service, cst.DIRECT_SERVICE_ID).build(session).obj
    client = client or ob.ClientBuilder().build(session).obj
    orders = [
        ob.OrderBuilder(service=service, client=client).build(session).obj
        for _i in range(len(order_qtys))
    ]
    basket = ob.BasketBuilder(
        client=client,
        rows=[ob.BasketItemBuilder(order=order, quantity=qty) for order, qty in zip(orders, order_qtys)],
    )
    request = ob.RequestBuilder(basket=basket, firm_id=firm_id)
    invoice = ob.InvoiceBuilder(request=request, firm_id=firm_id).build(session).obj

    if turn_on:
        invoice.manual_turn_on(
            sum([
                qty * order.product.price_by_date(session.now(), PRODUCT_CURRENCY).price
                for order, qty in zip(orders, order_qtys)
            ]),
        )
    session.flush()

    return orders


@pytest.fixture(name='order')
@allure.step('create order')
def create_order(client=None, qty=DEFAULT_ORDER_QTY, turn_on=True, firm_id=cst.FirmId.YANDEX_OOO):
    return create_orders(order_qtys=[qty], client=client, turn_on=turn_on, firm_id=firm_id)[0]


@pytest.fixture(name='not_existing_id')
def not_existing_order_id():
    session = test_utils.get_test_session()
    max_order_id = session.query(func.max(mapper.Order.id)).scalar() or 0
    return max_order_id + 1000


@pytest.fixture(name='order_w_firm')
def create_order_w_firm(client=None, firm_ids=None, service_id=None):
    session = test_utils.get_test_session()
    service_id = service_id or cst.ServiceId.DIRECT
    firm_ids = firm_ids or [cst.FirmId.YANDEX_OOO]
    order = ob.OrderBuilder.construct(
        session,
        client=client or ob.ClientBuilder(),
        service_id=service_id,
    )
    for firm_id in firm_ids:
        if firm_id is not None:
            order.add_firm(firm_id)
    session.flush()
    return order


def consume_order(order, qty, discount_pct=0, dt=None):
    session = order.session
    client = order.client
    person = ob.PersonBuilder(client=client).build(session).obj
    paysys = ob.Getter(mapper.Paysys, 1000).build(session).obj

    def mock_discounts(ns):
        return discounts.DiscountProof('mock', discount=discount_pct, adjust_quantity=False), \
               None, None

    def mock_update_taxes(self, qty, sum_):
        return self._unchanged(qty, sum_)

    patch_tax = mock.patch('balance.actions.taxes_update.TaxUpdater.calculate_updated_parameters', mock_update_taxes)
    patch_discounts = mock.patch('balance.discounts.calc_from_ns', mock_discounts)

    with patch_tax, patch_discounts:
        invoice = ob.InvoiceBuilder(
            person=person,
            paysys=paysys,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=client,
                    dt=dt,
                    rows=[ob.BasketItemBuilder(order=order, quantity=qty)]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(cut_agava=True)
        return invoice.consumes[0]
