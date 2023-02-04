# -*- coding: utf-8 -*-

import decimal
import datetime

import pytest
import hamcrest

from balance import mapper
from balance.actions import consumption as a_c
from balance.constants import (
    DIRECT_PRODUCT_ID,
    ExportState,
    OrderLogTariffState,
)

from tests import object_builder as ob
from tests.base_routine import consumes_match

D = decimal.Decimal


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder(client=client).build(session).obj


@pytest.fixture
def order(request, session, client):
    params = getattr(request, 'param', {})
    product_id = params.get('product_id', DIRECT_PRODUCT_ID)
    return ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, product_id)
    ).build(session).obj


@pytest.fixture
def invoice(session, client, person, order):
    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[ob.BasketItemBuilder(order=order, quantity=666666)])
        )
    ).build(session).obj
    invoice.create_receipt(invoice.effective_sum)
    return invoice


def test_negative_reverse_broken_proportion(session, invoice, order):
    invoice.transfer(order, 2, 1)
    consume, = order.consumes
    consume.consume_sum += D('0.01')
    consume.current_sum += D('0.01')
    session.flush()

    a_c.consume_order(
        invoice=invoice,
        order=order,
        price_obj=consume.price_obj,
        discount_obj=mapper.DiscountObj(consume.static_discount_pct),
        qty=1, sum_=30
    )

    hamcrest.assert_that(
        order.consumes,
        consumes_match(
            [
                (D('1'), D('30.01'), D('1'), D('30.01')),
                (D('1'), D('30'), D('1'), D('30')),
            ],
            forced_params=['consume_qty', 'consume_sum', 'current_qty', 'current_sum']
        )
    )


class TestEnqueueProcessCompletions(object):
    @pytest.mark.parametrize(
        'flag_value',
        [
            pytest.param(False, id='dont_enqueue'),
            pytest.param(True, id='enqueue'),
        ]
    )
    def test_overcompletion(self, session, invoice, order, flag_value):
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})

        assert 'PROCESS_COMPLETION' not in order.exports

        a_c.consume_order(
            invoice=invoice,
            order=order,
            price_obj=invoice.invoice_orders[0].price_obj,
            discount_obj=mapper.DiscountObj(),
            qty=1, sum_=30,
            enqueue_pc_overcompletion=flag_value
        )

        if flag_value:
            export = order.exports['PROCESS_COMPLETION']
            hamcrest.assert_that(
                export,
                hamcrest.has_properties(state=ExportState.enqueued, input={'skip_deny_shipment': True})
            )
        else:
            assert 'PROCESS_COMPLETION' not in order.exports

    def test_wo_overcompletion(self, session, invoice, order):
        a_c.consume_order(
            invoice=invoice,
            order=order,
            price_obj=invoice.invoice_orders[0].price_obj,
            discount_obj=mapper.DiscountObj(),
            qty=1, sum_=30,
            enqueue_pc_overcompletion=True
        )

        assert 'PROCESS_COMPLETION' not in order.exports

    @pytest.mark.parametrize(
        'log_tariff_state',
        [
            pytest.param(OrderLogTariffState.INIT, id='init'),
            pytest.param(OrderLogTariffState.MIGRATED, id='migrated'),
        ]
    )
    def test_log_tariff_order(self, session, invoice, order, log_tariff_state):
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})
        order._is_log_tariff = log_tariff_state
        session.flush()

        assert 'PROCESS_COMPLETION' not in order.exports

        a_c.consume_order(
            invoice=invoice,
            order=order,
            price_obj=invoice.invoice_orders[0].price_obj,
            discount_obj=mapper.DiscountObj(),
            qty=1, sum_=30,
        )

        assert 'PROCESS_COMPLETION' not in order.exports
