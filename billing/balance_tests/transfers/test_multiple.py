# -*- coding: utf-8 -*-

import decimal
import itertools

import pytest

from balance import mapper
from balance import muzzle_util as ut
from balance import exc
from balance.actions.transfers_qty.interface import (
    TransferMultiple,
)
from balance.actions.consumption import reverse_consume
from balance.constants import (
    DAY_UNIT_ID,
)

from tests import object_builder as ob
from .common import (
    create_dst,
    create_src,
    complete_act,
    consume_order,
    create_order,
)

D = decimal.Decimal


@pytest.mark.parametrize('skip_invoice_update', [False, True])
def test_transfer(session, client, product, skip_invoice_update):
    src_orders, src_list = create_src(client, product, [D('6'), D('6'), D('6')], [D('3'), D('2'), D('1')])
    dst_orders, dst_list = create_dst(client, product, [D('1'), D('1.666')])

    TransferMultiple(session, src_list, dst_list, skip_invoice_update=skip_invoice_update).do()

    src_res = [D('3'), D('2'), D('1')]
    dst_res = [
        (D('12') / D('2.666')).quantize(D('0.1') ** 6),
        (D('1.666') * D('12') / D('2.666')).quantize(D('0.1') ** 6)
    ]
    dst_res_sums = [ut.round00(qty * 12) for qty in dst_res]

    assert [o.consume_qty for o in src_orders] == src_res
    assert [co.current_sum for o in src_orders for co in o.consumes] == [36, 24, 12]

    assert [o.consume_qty for o in dst_orders] == dst_res
    dst_gr_sums = itertools.groupby((co for o in dst_orders for co in o.consumes), lambda co: co.order)
    assert [sum(co.current_sum for co in cos) for _, cos in dst_gr_sums] == dst_res_sums

    assert [co.invoice.consume_sum for o in src_orders for co in o.consumes] == [216] * 3


@pytest.mark.parametrize('skip_invoice_update', [False, True])
@pytest.mark.parametrize('paysys_id', [None, 1001])
def test_withdraw(session, client, direct_product, skip_invoice_update, paysys_id):
    create_src_extra_kwargs = {}
    if paysys_id:
        create_src_extra_kwargs['paysys_id'] = paysys_id

    src_orders, src_list = create_src(
        client, direct_product,
        [D('6'), D('6'), D('6')],
        [D('3'), D('2'), D('1')],
        **create_src_extra_kwargs
    )

    TransferMultiple(session, src_list, [], skip_invoice_update=skip_invoice_update).do(forced=True)

    assert [o.consume_qty for o in src_orders] == [3, 2, 1]
    assert [co.current_sum for o in src_orders for co in o.consumes] == [90, 60, 30]
    assert [co.invoice.consume_sum for o in src_orders for co in o.consumes] == [180] * 3


@pytest.mark.parametrize('skip_invoice_update', [False, True])
def test_transfer_zero_sum(session, client, product, skip_invoice_update):
    src_orders, src_list = create_src(
        client, product,
        [D('6'), D('6'), D('6')],
        [D('5.999999'), D('5.999765'), D('5.999876')]
    )
    dst_orders, dst_list = create_dst(client, product, [D('1'), D('1.666')])

    TransferMultiple(session, src_list, dst_list, skip_invoice_update=skip_invoice_update).do()

    src_res = [D('5.999999'), D('5.999765'), D('5.999876')]
    dst_res = [
        (D('0.000360') / D('2.666')).quantize(D('0.1') ** 6),
        (D('1.666') * D('0.000360') / D('2.666')).quantize(D('0.1') ** 6)
    ]

    assert [o.consume_qty for o in src_orders] == src_res
    assert [o.consume_qty for o in dst_orders] == dst_res


def test_transfer_zero_qty(session, client, product):
    src_orders, src_list = create_src(client, product, [D('666')], [D('666')])
    dst_orders, dst_list = create_dst(client, product, [D('1'), D('1.666')])

    TransferMultiple(session, src_list, dst_list).do()

    assert [o.consume_qty for o in src_orders] == [666]
    assert [o.consume_qty for o in dst_orders] == [0, 0]


def test_different_products(session, client):
    product1 = ob.ProductBuilder.construct(session, price=315)
    product2 = ob.ProductBuilder.construct(session, price=630)
    product3 = ob.ProductBuilder.construct(session, price=630)

    src_orders, src_list = create_src(
        client,
        [product1, product2],
        [D('30'), D('30')],
        [D('0'), D('0')],
    )
    dst_orders, dst_list = create_dst(client, [product2, product3], [D('22.5'), D('22.5')])

    TransferMultiple(session, src_list, dst_list).do()

    assert [o.consume_qty for o in src_orders] == [0, 0]
    assert [o.consume_qty for o in dst_orders] == [D('22.5'), D('22.5')]


def test_different_products_ratio(session, client):
    product1 = ob.ProductBuilder.construct(session, price=123)
    product2 = ob.ProductBuilder.construct(session, price=321)
    product3 = ob.ProductBuilder.construct(session, price=666)

    src_orders, src_list = create_src(
        client,
        [product1, product2],
        [D('66'), D('6')],
        [D('0'), D('0')],
    )
    dst_orders, dst_list = create_dst(client, [product2, product3], [D('4'), D('2')])

    TransferMultiple(session, src_list, dst_list).do()

    total_sum = D('66') * D('123') + D('6') * D('321')
    dst_res = [
        (total_sum / D('321') * D('4') / D('6')).quantize(D('0.1') ** 6),
        (total_sum / D('666') * D('2') / D('6')).quantize(D('0.1') ** 6),
    ]
    assert [o.consume_qty for o in src_orders] == [0, 0]
    assert [o.consume_qty for o in dst_orders] == dst_res


def test_dst_rounding(session, client):
    src_product = ob.ProductBuilder.construct(session, price=123)
    dst_product = ob.ProductBuilder.construct(session, price=321, unit=ob.Getter(mapper.ProductUnit, DAY_UNIT_ID))

    src_orders, src_list = create_src(client, src_product, [D('66')], [D('56.666666')])
    dst_orders, dst_list = create_dst(client, dst_product, [D('1')])

    TransferMultiple(session, src_list, dst_list).do()

    assert [o.consume_qty for o in src_orders] == [D('56.666666')]
    assert [o.consume_qty for o in dst_orders] == [D('4')]


@pytest.mark.parametrize('skip_invoice_update', [False, True])
def test_dst_rounding_zero(session, client, direct_product, direct_rub_product, skip_invoice_update):
    src_orders, src_list = create_src(client, direct_product, [D('10')], [D('9.999999')])
    dst_orders, dst_list = create_dst(client, direct_rub_product, [D('1')])

    src_order, = src_orders
    dst_order, = dst_orders
    consume, = src_order.consumes
    reverse = reverse_consume(consume, None, D('3.983166'))
    src_list[0].qty_old = src_order.consume_qty

    TransferMultiple(session, src_list, dst_list, skip_invoice_update=skip_invoice_update).do()

    assert src_order.consume_qty == D('6.016833')
    assert consume.current_sum == D('180.50')
    assert consume.invoice.consume_sum == D('180.50')
    assert reverse.reverse_sum == D('119.49')

    assert dst_order.consume_qty == 0
    assert dst_order.consumes == []


def test_with_completions(session, client, direct_product):
    (src_order1, src_order2), src_list = create_src(
        client, direct_product,
        [
            [D('10'), D('5')],
            [D('6'), D('6')],
        ],
        [D('9'), D('4')]
    )
    complete_act(src_order1, 9)
    complete_act(src_order2, 4)

    dst_orders, dst_list = create_dst(client, direct_product, [D('1'), D('2')])

    TransferMultiple(session, src_list, dst_list).do()

    assert src_order1.consume_qty == 9
    assert src_order2.consume_qty == 4
    assert [o.consume_qty for o in dst_orders] == [D('4.666667'), D('9.333333')]


def test_fail_completion(session, client, direct_product):
    (src_order1, src_order2), src_list = create_src(
        client, direct_product,
        [
            [D('10'), D('5')],
            [D('6'), D('6')],
        ],
        [D('9'), D('4')]
    )
    complete_act(src_order1, 9)
    complete_act(src_order2, D('4.000001'))

    dst_orders, dst_list = create_dst(client, direct_product, [D('1'), D('2')])

    with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_REVERSE):
        TransferMultiple(session, src_list, dst_list).do()


def test_overact_force(session, client, direct_product):
    (src_order,), (src_item,) = create_src(client, direct_product, [D('10')], [D('4')], paysys_id=1003)
    complete_act(src_order, 4, 5)

    dst_orders, dst_list = create_dst(client, direct_product, [D('1')])

    src_item.force_transfer_acted = True
    TransferMultiple(session, [src_item], dst_list).do()

    assert src_order.consume_qty == 4
    assert [o.consume_qty for o in dst_orders] == [D('6')]


def test_overact_person_force(session, client, direct_product):
    (src_order,), (src_item,) = create_src(client, direct_product, [D('10')], [D('4')], paysys_id=1000)
    complete_act(src_order, 4, 5)

    dst_orders, dst_list = create_dst(client, direct_product, [D('1')])

    TransferMultiple(session, [src_item], dst_list).do()

    assert src_order.consume_qty == 4
    assert [o.consume_qty for o in dst_orders] == [D('6')]


@pytest.mark.parametrize(
    'feature, is_subclients, skip_agency, new_qty, is_ok',
    [
        pytest.param(False, False, False, 4, True, id='single_wo_check'),
        pytest.param(False, True, False, 4, True, id='multiple_wo_check'),
        pytest.param(True, False, False, 4, True, id='single_w_check'),
        pytest.param(True, True, False, 4, False, id='multiple_w_check'),
        pytest.param(True, True, True, 4, True, id='multiple_w_check_not_enough_skip_agency'),
        pytest.param(True, True, False, 6, True, id='multiple_w_check_enough'),
    ]
)
def test_overact_between_subclients(session, client, direct_product,
                                    feature, skip_agency, is_subclients, new_qty, is_ok):
    session.config.__dict__['CHECK_SUBCLIENTS_OVERACT_TRANSFER'] = feature

    agency = ob.ClientBuilder.construct(session, is_agency=1)
    src_client = ob.ClientBuilder.construct(session, agency=agency)
    if is_subclients:
        dst_client = ob.ClientBuilder.construct(session, agency=agency)
    else:
        dst_client = src_client

    session.config.__dict__['SKIP_CHECK_SUBCLIENTS_OVERACT_TRANSFER_CLIENTS'] = [agency.id] if skip_agency else []

    (src_order,), (src_item,) = create_src(src_client, direct_product, [10], [new_qty], paysys_id=1003)
    invoice = src_order.consumes[0].invoice
    invoice.create_receipt(6666666)
    overact_order = create_order(src_client)
    overact_consume = consume_order(invoice, overact_order, 10)
    complete_act(overact_order, 0, 6)
    reverse_consume(overact_consume, None, 10)

    (dst_order,), dst_list = create_dst(dst_client, direct_product, [D('1')])

    if is_ok:
        TransferMultiple(session, [src_item], dst_list).do()

        assert src_order.consume_qty == new_qty
        assert dst_order.consume_qty == 10 - new_qty
    else:
        with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_REVERSE):
            TransferMultiple(session, [src_item], dst_list).do()


def test_fail_overact(session, client, direct_product):
    (src_order,), (src_item,) = create_src(client, direct_product, [D('10')], [D('4')], paysys_id=1003)
    complete_act(src_order, 4, 5)

    dst_orders, dst_list = create_dst(client, direct_product, [D('1')])

    with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_REVERSE):
        TransferMultiple(session, [src_item], dst_list).do()


def test_with_discount(session, client):
    product1 = ob.ProductBuilder.construct(session, price=4)
    product2 = ob.ProductBuilder.construct(session, price=5)

    src_orders, src_list = create_src(
        client,
        product1,
        [D('666')],
        [D('10')],
        [15]
    )
    dst_orders, dst_list = create_dst(client, [product1, product2], [D('1.4'), D('1.6')])

    TransferMultiple(session, src_list, dst_list).do()

    assert [o.consume_qty for o in src_orders] == [D('10')]
    assert [o.consume_qty for o in dst_orders] == [D('306.133333'), D('279.893334')]
    assert [(co.current_sum, co.discount_obj) for o in dst_orders for co in o.consumes] == [
        (D('1040.85'), mapper.DiscountObj(15)),
        (D('1189.55'), mapper.DiscountObj(15)),
    ]


def test_force_discount(session, client):
    product1 = ob.ProductBuilder.construct(session, price=4)
    product2 = ob.ProductBuilder.construct(session, price=5)

    src_orders, src_list = create_src(
        client,
        product1,
        [D('666')],
        [D('10')],
        [15]
    )
    dst_orders, dst_list = create_dst(client, [product1, product2], [D('1.4'), D('1.6')])

    TransferMultiple(session, src_list, dst_list, discount_pct=10).do()

    assert [o.consume_qty for o in src_orders] == [D('10')]
    assert [o.consume_qty for o in dst_orders] == [D('289.125926'), D('264.343704')]
    assert [(co.current_sum, co.discount_obj) for o in dst_orders for co in o.consumes] == [
        (D('1040.85'), mapper.DiscountObj(10)),
        (D('1189.55'), mapper.DiscountObj(10)),
    ]


def test_with_promo_discount(session, client):
    product1 = ob.ProductBuilder.construct(session, price=4)
    product2 = ob.ProductBuilder.construct(session, price=5)

    src_orders, src_list = create_src(
        client,
        product1,
        [D('666')],
        [D('10')],
        [D('14.5')]
    )

    consume, = [co for o in src_orders for co in o.consumes]
    promo_code, = ob.PromoCodeGroupBuilder.construct(session).promocodes
    consume.promo_code = promo_code
    consume.base_discount_pct = 10
    consume.promo_code_discount_pct = 5
    session.flush()

    dst_orders, dst_list = create_dst(client, [product1, product2], [D('1.4'), D('1.6')])

    TransferMultiple(session, src_list, dst_list).do()

    assert [o.consume_qty for o in src_orders] == [D('10')]
    assert [o.consume_qty for o in dst_orders] == [D('306.133333'), D('279.893334')]
    assert [(co.current_sum, co.discount_obj) for o in dst_orders for co in o.consumes] == [
        (D('1046.98'), mapper.DiscountObj(10, 5, promo_code)),
        (D('1196.54'), mapper.DiscountObj(10, 5, promo_code)),
    ]


def test_with_promo_discount_force(session, client):
    product1 = ob.ProductBuilder.construct(session, price=4)
    product2 = ob.ProductBuilder.construct(session, price=5)

    src_orders, src_list = create_src(
        client,
        product1,
        [D('666')],
        [D('10')],
        [D('14.5')]
    )

    consume, = [co for o in src_orders for co in o.consumes]
    promo_code, = ob.PromoCodeGroupBuilder.construct(session).promocodes
    consume.promo_code = promo_code
    consume.base_discount_pct = 10
    consume.promo_code_discount_pct = 5
    session.flush()

    dst_orders, dst_list = create_dst(client, [product1, product2], [D('1.4'), D('1.6')])

    TransferMultiple(session, src_list, dst_list, discount_pct=10).do()

    assert [o.consume_qty for o in src_orders] == [D('10')]
    assert [o.consume_qty for o in dst_orders] == [D('290.826666'), D('265.898667')]
    assert [(co.current_sum, co.discount_obj) for o in dst_orders for co in o.consumes] == [
        (D('1046.98'), mapper.DiscountObj(10)),
        (D('1196.54'), mapper.DiscountObj(10)),
    ]


def test_with_dynamic_discount(session, client, direct_product):
    (src_order,), (src_item,) = create_src(
        client,
        direct_product,
        [D('666')],
        [D('0')],
        [15]
    )
    src_consume, = src_order.consumes

    ob.add_dynamic_discount(src_consume, 5)
    qty_w_dyn = ut.round(D('666') / D('0.95'), 6)

    src_item.qty_old = qty_w_dyn
    src_item.qty_delta = qty_w_dyn

    (dst_order,), dst_list = create_dst(client, direct_product, [D('66')])

    TransferMultiple(session, [src_item], dst_list).do()

    assert src_order.consume_qty == D('0')
    assert dst_order.consume_qty == 666
    assert [(co.current_sum, co.discount_obj) for co in dst_order.consumes] == [
        (D('16983'), mapper.DiscountObj(15)),
    ]


@pytest.mark.parametrize('skip_invoice_update', [False, True])
def test_services_cache(session, client, product, skip_invoice_update):
    src_orders, src_list = create_src(client, product, [D('10')], [D('0')])
    dst_orders, dst_list = create_dst(client, product, [D('1')])

    dst_order, = dst_orders
    dst_order.service_id = 77
    session.flush()

    TransferMultiple(session, src_list, dst_list, skip_invoice_update=skip_invoice_update).do(forced=True)

    consume, = dst_order.consumes
    invoice = consume.invoice
    assert invoice._service_ids == {'7': 3, '77': 2}


def test_no_free_funds(session, client, product):
    (src_order,), src_list = create_src(client, product, [D('10')], [D('0')])
    _, dst_list = create_dst(client, product, [D('1')])

    complete_act(src_order, 10)

    with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_REVERSE):
        TransferMultiple(session, src_list, dst_list).do()
