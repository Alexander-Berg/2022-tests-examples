# -*- coding: utf-8 -*-

import decimal

import pytest

from balance import mapper, muzzle_util as ut, constants as cst
from balance.actions import consumption
from balance.son_schema import operations

from tests import object_builder as ob

D = decimal.Decimal


def test_consume(session, invoice, order):
    tax_policy = ob.TaxPolicyBuilder.construct(session, tax_pcts=[66])
    product = ob.ProductBuilder.construct(session, taxes=[tax_policy])
    price, = product.prices
    tpp, = tax_policy.taxes
    promo = ob.PromoCodeBuilder.construct(session, code=ob.generate_character_string())
    operation = mapper.Operation(666)
    session.add(operation)
    session.flush()

    consume = consumption.consume_order(
        invoice,
        order,
        D('123.45'),
        mapper.PriceObject(D('13.1'), 31, tpp, price),
        mapper.DiscountObj(D('10.1'), D('20.1'), promo, D('10.1')),
        D('321.09'),
        operation,
    ).consume
    session.flush()
    session.expire_all()

    data = operations.ConsumeSchema().dump(consume).data
    assert data == {
        'id': consume.id,
        'invoice': {'id': invoice.id,
                    'client_id': invoice.client_id,
                    },
        'invoice_id': invoice.id,
        'operation_id': operation.id,
        'order': {
            'id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'client_id': order.client_id,
        },
        'dt': consume.dt.strftime('%Y-%m-%dT%H:%M:%S+03:00'),
        'consume_qty': D('123.45'),
        'consume_sum': D('321.09'),
        'cashback_initial_qty': D('0'),
        'promocode_initial_qty': D('24.81345'),
        'discount_initial_qty': D('18.912429010149'),
        'paid_initial_qty': D('79.724120989851'),
        'certificate_initial_qty': D('0'),
        'compensation_initial_qty': D('0'),
        'rollback_compensation_initial_qty': D('0'),

        'version': 0,
        'discount': {
            'base_pct': D('10.1'),
            'dynamic_pct': D('10.0933'),
            'promo_code_id': promo.id,
            'promo_code_pct': D('20.1'),
            'cashback_base': D('0'),
            'cashback_bonus': D('0'),
        },
        'price': {
            'price': D('13.1'),
            'price_id': price.id,
            'tax_policy_pct_id': tpp.id,
            'type_rate': 31
        },
    }


def test_consume_empty(session, invoice, order):
    consume = consumption.consume_order(
        invoice,
        order,
        D('123.45'),
        mapper.PriceObject(D('13.1'), 31),
        mapper.DiscountObj(),
        D('321.09'),
    ).consume
    consume.tax_policy_pct_id = None
    consume.tax = None
    session.flush()
    session.expire_all()

    data = operations.ConsumeSchema().dump(consume).data
    assert data == {
        'id': consume.id,
        'invoice': {'id': invoice.id,
                    'client_id': invoice.client_id,
                    },
        'invoice_id': invoice.id,
        'operation_id': None,
        'order': {
            'id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'client_id': order.client_id,
        },
        'dt': consume.dt.strftime('%Y-%m-%dT%H:%M:%S+03:00'),
        'consume_qty': D('123.45'),
        'consume_sum': D('321.09'),
        'cashback_initial_qty': D('0'),
        'promocode_initial_qty': D('0'),
        'discount_initial_qty': D('0'),
        'paid_initial_qty': D('123.45'),
        'certificate_initial_qty': D('0'),
        'compensation_initial_qty': D('0'),
        'rollback_compensation_initial_qty': D('0'),
        'version': 0,
        'discount': {
            'base_pct': D('0'),
            'dynamic_pct': D('0.0000'),
            'promo_code_pct': D('0'),
            'cashback_base': D('0'),
            'cashback_bonus': D('0'),
        },
        'price': {
            'price': D('13.1'),
            'tax_policy_pct_id': 281,  # вычисляется по счёту
            'type_rate': 31
        },
    }


@pytest.mark.parametrize('reverse_qty', [0, D('36.6')])
@pytest.mark.parametrize(
    'base_pct, dynamic_pct, promo_code_pct, cashback_pct',
    [
        pytest.param(0, 0, 0, 0, id='simple'),
        pytest.param(0, 0, 0, 15, id='cb'),
        pytest.param(0, 0, 11, 0, id='pc'),
        pytest.param(0, 0, 11, 15, id='pc+cb'),
        pytest.param(0, 7, 0, 0, id='d'),
        pytest.param(0, 7, 0, 15, id='d+cb'),
        pytest.param(0, 7, 11, 0, id='d+pc'),
        pytest.param(0, 7, 11, 15, id='d+pc+cb'),
        pytest.param(3, 0, 0, 0, id='b'),
        pytest.param(3, 0, 0, 15, id='b+cb'),
        pytest.param(3, 0, 11, 0, id='b+pc'),
        pytest.param(3, 0, 11, 15, id='b+pc+cb'),
        pytest.param(3, 7, 0, 0, id='b+d'),
        pytest.param(3, 7, 0, 15, id='b+d+cb'),
        pytest.param(3, 7, 11, 0, id='b+d+pc'),
        pytest.param(3, 7, 11, 15, id='b+d+pc+cb'),
    ]
)
def test_discounts(session, invoice, order, base_pct, dynamic_pct, promo_code_pct, cashback_pct, reverse_qty):
    tax_policy = ob.TaxPolicyBuilder.construct(session, tax_pcts=[66])
    product = ob.ProductBuilder.construct(session, taxes=[tax_policy])
    price, = product.prices
    tpp, = tax_policy.taxes
    promo = ob.PromoCodeBuilder.construct(session, code=ob.generate_character_string())
    operation = mapper.Operation(666)
    session.add(operation)
    session.flush()

    total_initial_qty = D('123.45')

    cashback = ob.ClientCashbackBuilder.construct(
        session,
        client=order.client,
        service_id=order.service_id,
        iso_currency=order.product_iso_currency,
        bonus=500,
    )
    cashback_usage = ob.CashbackUsageBuilder.construct(session, client_cashback=cashback)
    discount_obj = mapper.DiscountObj(
        base_pct, promo_code_pct, promo, dynamic_pct,
        total_initial_qty, total_initial_qty * cashback_pct / 100, cashback_usage.id if cashback_pct else None,
    )

    consume = consumption.consume_order(
        invoice,
        order,
        total_initial_qty,
        mapper.PriceObject(D('13.1'), 31, tpp, price),
        discount_obj,
        D('543.21'),
        operation,
    ).consume
    session.flush()
    if reverse_qty:
        consumption.reverse_consume(consume, operation, reverse_qty)
    session.flush()
    session.expire_all()

    data = operations.ConsumeSchema().dump(consume).data

    assert data['discount_initial_qty'] + data['promocode_initial_qty'] + data['cashback_initial_qty'] + data['paid_initial_qty'] == data['consume_qty']
    if base_pct or dynamic_pct:
        assert data['discount_initial_qty'] > 0
    if promo_code_pct:
        assert data['promocode_initial_qty'] > 0
    if cashback_pct:
        assert data['cashback_initial_qty'] > 0
    discount_pct = ut.round(data['discount_initial_qty'] * 100 / (data['paid_initial_qty'] + data['discount_initial_qty']), 4)
    real_discount_pct = ut.round(ut.mul_discounts(data['discount']['base_pct'], data['discount']['dynamic_pct']), 4)
    assert discount_pct == real_discount_pct
    promo_code_pct = ut.round(data['promocode_initial_qty'] * 100 / (data['paid_initial_qty'] + data['discount_initial_qty'] + data['promocode_initial_qty']), 6)
    real_promo_code_pct = data['discount']['promo_code_pct']
    assert promo_code_pct == real_promo_code_pct
    cashback_pct = ut.round(data['cashback_initial_qty'] * 100 / (data['paid_initial_qty'] + data['discount_initial_qty'] + data['promocode_initial_qty'] + data['cashback_initial_qty']), 6)
    real_cashback_pct = ut.round(data['discount']['cashback_bonus'] * 100 / (data['discount']['cashback_base'] + data['discount']['cashback_bonus']), 6)
    assert cashback_pct == real_cashback_pct


@pytest.mark.parametrize(
    'consume_type, attr_name',
    [
        (cst.PaymentMethodIDs.bank, 'paid_initial_qty'),
        (cst.PaymentMethodIDs.certificate, 'certificate_initial_qty'),
        (cst.PaymentMethodIDs.compensation, 'compensation_initial_qty'),
    ]
)
def test_consume_types(session, order, consume_type, attr_name):
    invoice = ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=order.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=666)]
            )
        ),
        paysys=ob.PaysysBuilder(currency='RUR', iso_currency=643, cc='paysys_cc', category='ur', firm_id=1, payment_method_id=consume_type)
    ).build(session).obj
    tax_policy = ob.TaxPolicyBuilder.construct(session, tax_pcts=[66])
    product = ob.ProductBuilder.construct(session, taxes=[tax_policy])
    price, = product.prices
    tpp, = tax_policy.taxes
    promo = ob.PromoCodeBuilder.construct(session, code=ob.generate_character_string())
    operation = mapper.Operation(666)
    session.add(operation)
    session.flush()

    consume = consumption.consume_order(
        invoice,
        order,
        D('123.45'),
        mapper.PriceObject(D('13.1'), 31, tpp, price),
        mapper.DiscountObj(D('10.1'), D('20.1'), promo, D('10.1')),
        D('321.09'),
        operation,
    ).consume
    session.flush()
    session.expire_all()

    data = operations.ConsumeSchema().dump(consume).data

    assert data['discount_initial_qty'] + data['promocode_initial_qty'] + data['cashback_initial_qty'] + data[attr_name] == data['consume_qty']


def test_maxed_cashback(session, invoice, order):
    tax_policy = ob.TaxPolicyBuilder.construct(session, tax_pcts=[66])
    product = ob.ProductBuilder.construct(session, taxes=[tax_policy])
    price, = product.prices
    tpp, = tax_policy.taxes
    promo = ob.PromoCodeBuilder.construct(session, code=ob.generate_character_string())
    operation = mapper.Operation(666)
    session.add(operation)
    session.flush()

    total_initial_qty = D('10001')

    cashback = ob.ClientCashbackBuilder.construct(
        session,
        client=order.client,
        service_id=order.service_id,
        iso_currency=order.product_iso_currency,
        bonus=10000,
    )
    cashback_usage = ob.CashbackUsageBuilder.construct(session, client_cashback=cashback)
    discount_obj = mapper.DiscountObj(
        0, D('13.37'), promo, 0,
        cashback_base=D('0.4815'), cashback_bonus=D('4814.2342'), cashback_usage_id=cashback_usage.id
    )

    consume = consumption.consume_order(
        invoice,
        order,
        total_initial_qty,
        mapper.PriceObject(D('13.1'), 31, tpp, price),
        discount_obj,
        total_initial_qty,
        operation,
    ).consume
    session.flush()
    session.expire_all()

    data = operations.ConsumeSchema().dump(consume).data
    assert data == {
        'id': consume.id,
        'invoice': {'id': invoice.id,
                    'client_id': invoice.client_id,
                    },
        'invoice_id': invoice.id,
        'operation_id': operation.id,
        'order': {
            'id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'client_id': order.client_id,
        },
        'dt': consume.dt.strftime('%Y-%m-%dT%H:%M:%S+03:00'),
        'consume_qty': D('10001'),
        'consume_sum': D('10001'),
        'cashback_initial_qty': D('9999.999840945957'),
        'promocode_initial_qty': D('0.133721265525'),
        'discount_initial_qty': D('0'),
        'paid_initial_qty': D('0.866437788518'),
        'certificate_initial_qty': D('0'),
        'compensation_initial_qty': D('0'),
        'rollback_compensation_initial_qty': D('0'),
        'version': 0,
        'discount': {
            'base_pct': D('0'),
            'dynamic_pct': D('0.0000'),
            'promo_code_id': promo.id,
            'promo_code_pct': D('13.37'),
            'cashback_base': D('0.4815'),
            'cashback_bonus': D('4814.2342'),
        },
        'price': {
            'price': D('13.1'),
            'price_id': price.id,
            'tax_policy_pct_id': tpp.id,
            'type_rate': 31
        },
    }

    assert data['discount_initial_qty'] + data['promocode_initial_qty'] + data['cashback_initial_qty'] + data['paid_initial_qty'] == data['consume_qty']
    cashback_pct = ut.round(data['cashback_initial_qty'] * 100 / (data['paid_initial_qty'] + data['discount_initial_qty'] + data['promocode_initial_qty'] + data['cashback_initial_qty']), 6)
    real_cashback_pct = ut.round(data['discount']['cashback_bonus'] * 100 / (data['discount']['cashback_base'] + data['discount']['cashback_bonus']), 6)
    assert cashback_pct == real_cashback_pct
