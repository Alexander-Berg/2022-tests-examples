# -*- coding: utf-8 -*-

import decimal

import hamcrest
import pytest

from balance import exc
from balance.constants import (
    TransferMode,
    DIRECT_PRODUCT_RUB_ID,
)

from tests.balance_tests.transfers.common import (
    create_order,
    complete_act,
)

D = decimal.Decimal


def assert_consumes(src_order, src_qtys, src_price, dst_order, dst_qtys, dst_price, init_qtys):
    hamcrest.assert_that(
        src_order.consumes,
        hamcrest.contains(*[
            hamcrest.has_properties(
                current_qty=qty,
                current_sum=qty * src_price,
                invoice=hamcrest.has_properties(consume_sum=init_qty * src_price)
            )
            for qty, init_qty in zip(src_qtys, init_qtys)
        ])
    )
    hamcrest.assert_that(src_order, hamcrest.has_properties(consume_qty=sum(src_qtys)))

    hamcrest.assert_that(
        dst_order.consumes,
        hamcrest.contains_inanyorder(*[
            hamcrest.has_properties(
                current_qty=qty,
                current_sum=qty * dst_price,
            )
            for qty in dst_qtys
        ])
    )
    hamcrest.assert_that(dst_order, hamcrest.has_properties(consume_qty=sum(dst_qtys)))


@pytest.mark.parametrize(
    'consumes_qtys, completion_qty, act_qty, src_qtys, dst_qtys',
    [
        pytest.param([10, 5, 6], 0, 0, [0, 0, 0], {10, 5, 6}, id='free'),
        pytest.param([10, 20], 13, 0, [10, 3], {17}, id='completed'),
        pytest.param([10, 5], 3, 12, [10, 2], {3}, id='overacted'),
    ]
)
def test_all(client, consumes_qtys, completion_qty, act_qty, src_qtys, dst_qtys):
    dst_order = create_order(client)
    src_order = create_order(client, consumes_qtys)
    complete_act(src_order, completion_qty, act_qty)

    src_order.transfer(dst_order, TransferMode.all)

    assert_consumes(
        src_order, src_qtys, 30,
        dst_order, dst_qtys, 30,
        consumes_qtys
    )


@pytest.mark.parametrize(
    'consumes_qtys, completion_qty, act_qty, req_qty, src_qtys, dst_qtys',
    [
        pytest.param([10, 5, 6], 0, 0, 10, [10, 1, 0], {6, 4}, id='free'),
        pytest.param([10, 5, 6], 12, 0, 9, [10, 2, 0], {6, 3}, id='eq_free'),
    ]
)
def test_src_qty(client, consumes_qtys, completion_qty, act_qty, req_qty, src_qtys, dst_qtys):
    dst_order = create_order(client)
    src_order = create_order(client, consumes_qtys)
    complete_act(src_order, completion_qty, act_qty)

    src_order.transfer(dst_order, TransferMode.src, req_qty)

    assert_consumes(
        src_order, src_qtys, 30,
        dst_order, dst_qtys, 30,
        consumes_qtys
    )


def test_dst(client):
    src_order = create_order(client, [10])
    dst_order = create_order(client)

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        src_order.transfer(dst_order, TransferMode.dst, 1)
    assert 'Transfer with mode = TransferMode.dst is outdated.' in exc_info.value.msg


@pytest.mark.parametrize(
    'consumes_qtys, completion_qty, act_qty, transfer_mode, req_qty, exc_cls',
    [
        pytest.param([10, 5, 6], 21, 0, TransferMode.all, None, exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER, id='all_completed'),
        pytest.param([10, 5, 6], 0, 21, TransferMode.all, None, exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER, id='all_acted'),
        pytest.param([10, 5, 6], 14, 0, TransferMode.src, 8, exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER, id='qty_completed'),
        pytest.param([10, 5, 6], 14, 0, TransferMode.src, D('7.000001'), exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER, id='qty_completed_penny'),
        pytest.param([10, 5, 6], 10, 13, TransferMode.src, 9, exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER, id='qty_acted'),
    ]
)
def test_not_enough(client, consumes_qtys, completion_qty, act_qty, transfer_mode, req_qty, exc_cls):
    dst_order = create_order(client)
    src_order = create_order(client, consumes_qtys)
    complete_act(src_order, completion_qty, act_qty)

    with pytest.raises(exc_cls):
        src_order.transfer(dst_order, transfer_mode, req_qty)


@pytest.mark.parametrize(
    'consumes_qtys, completion_qty, act_qty, transfer_mode, req_qty, src_qtys, dst_qtys',
    [
        pytest.param([10, 5], 9, 11, TransferMode.all, None, [9, 0], {1, 5}, id='all'),
        pytest.param([10, 5], 9, 11, TransferMode.src, 5, [10, 0], {5}, id='qty'),
    ]
)
def test_force_transfer_acted(client, consumes_qtys, completion_qty, act_qty, transfer_mode, req_qty,
                              src_qtys, dst_qtys):
    dst_order = create_order(client)
    src_order = create_order(client, consumes_qtys)
    complete_act(src_order, completion_qty, act_qty)

    src_order.transfer(dst_order, transfer_mode, req_qty, force_transfer_acted=True)

    assert_consumes(
        src_order, src_qtys, 30,
        dst_order, dst_qtys, 30,
        consumes_qtys
    )


def test_discount(client):
    dst_order = create_order(client)
    src_order = create_order(client, [10])

    src_order.transfer(dst_order, TransferMode.all, discount_pct=10)

    consume, = dst_order.consumes
    assert dst_order.consume_qty == D('11.111111')
    assert consume.current_sum == D('300')
    assert consume.discount_pct == 10


@pytest.mark.parametrize(
    'src_flag, dst_flag',
    [
        pytest.param(1, 0, id='src'),
        pytest.param(0, 1, id='dst'),
    ]
)
def test_unmoderated(session, client, src_flag, dst_flag):
    dst_order = create_order(client)
    src_order = create_order(client, [10])
    src_order.unmoderated = src_flag
    dst_order.unmoderated = dst_flag
    session.flush()

    with pytest.raises(exc.UNMODERATED_TRANSFER):
        src_order.transfer(dst_order, TransferMode.all)


def test_consecutive(session, client):
    dst_order = create_order(client, product_id=DIRECT_PRODUCT_RUB_ID)
    src_order = create_order(client, [1200], product_id=DIRECT_PRODUCT_RUB_ID)
    src_consume, = src_order.consumes

    transfer_qtys = [
        D('100.1528'),
        D('584.2031'),
        D('1.6219'),
        D('9.2298'),
        D('0.8852'),
        D('33.0745'),
        D('0.7054'),
        D('364.397'),
        D('2.6141'),
        D('1.318'),
        D('4.3927'),
        D('47.9882'),
        D('38.7911'),
    ]

    for qty in transfer_qtys:
        src_order.transfer(dst_order, TransferMode.src, qty)

    assert src_consume.current_qty == D('10.6262')
    assert src_consume.current_sum == D('10.63')

