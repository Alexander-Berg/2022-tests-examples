# -*- coding: utf-8 -*-

import pytest
import hamcrest

from billing.log_tariffication.py.lib.logic.tarifficator import (
    Tarifficator,
)

from billing.log_tariffication.py.tests.utils import (
    FLOAT_PRECISION,
    OrderDataBuilder,
)
from .common import (
    tariff_order,
    get_result,
)


@pytest.mark.parametrize(
    'consume_qty, consume_sum, qty, sum_, res_qty, res_sum',
    [
        pytest.param(10, 10, 1, 1, 1, 1),
        pytest.param(10, 20, 1, 2, 1, 2),
        pytest.param(1, 30, 0.1, 3, 1.0 / 10, 3),
        pytest.param(1, 30, -0.1, -3, -1.0 / 10, -3),
        pytest.param(1, 30, 0.000001, 0.00, 0.000001, 0.00),
        pytest.param(1, 30, 0.000100, 0.00, 0.000100, 0.00),
        pytest.param(1, 30, 0.000166, 0.00, 0.000166, 0.00),
        pytest.param(1, 30, 0.000167, 0.01, 0.000167, 0.01),
        pytest.param(1, 30, 0.000200, 0.01, 0.000200, 0.01),
        pytest.param(1, 30, 0.000600, 0.02, 0.000600, 0.02),
        pytest.param(1, 30, 0.001000, 0.03, 0.001000, 0.03),
        pytest.param(1, 30, -0.001000, -0.03, -0.001000, -0.03),
        pytest.param(1, 0, 1, 0, 1, 0),
        pytest.param(1, 0, -1, 0, -1, 0),
    ]
)
def test_price(consume_qty, consume_sum, qty, sum_, res_qty, res_sum):
    order = OrderDataBuilder()
    order.add_consume(qty, sum_, consume_qty, consume_sum)
    order.add_batched_event(qty)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    assert tariffed_events == [
        {
            'UID': 'test_run@7@666@1@666@0@0@1',
            'ServiceID': 7,
            'EffectiveServiceOrderID': order.id,
            'LBMessageUID': '1',
            'tariff_dt': 2,
            'consume_id': 1,
            'tariffed_qty': res_qty,
            'tariffed_sum': res_sum,
            'coeff_qty': consume_qty,
            'coeff_sum': consume_sum,
        },
    ]

    assert t.process_order() == {
        'ServiceID': 7,
        'EffectiveServiceOrderID': order.id,
        'state': None,
    }


def test_aggregate_rounding():  # test_crawfish
    """
    https://femida.yandex-team.ru/problems/1657
    """

    order = OrderDataBuilder()
    order.add_consume(7, 1, 7, 1)
    for _ in range(7):
        order.add_batched_event(1)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_aggregate_rounding_overflow():
    order = OrderDataBuilder()
    order.add_consume(7, 1, 7, 1)
    for _ in range(10):
        order.add_batched_event(1)
    for _ in range(15):
        order.add_batched_event(-1)
    for _ in range(12):
        order.add_batched_event(1)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_broken_proportion():
    order = OrderDataBuilder()
    order.add_consume(1.001, 1, 1, 1)
    order.add_batched_event(+100001.001)
    order.add_batched_event(-100000)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_broken_price():
    order = OrderDataBuilder()
    order.add_consume(20.354, 20.36, 1, 1)
    order.add_batched_event(7.111)
    order.add_batched_event(7.222)
    order.add_batched_event(6.021)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_broken_price_proportion():
    order = OrderDataBuilder()
    order.add_consume(10.01, 3.33, 3, 1)
    order.add_batched_event(+66666)
    order.add_batched_event(-66655.99)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


@pytest.mark.parametrize(
    'event_qty, events_cnt, req_qty, req_sum',
    [
        (1, 100, 100, 14.29),
        (-1, 100, -100, -14.29),
        (0.000001, 34999, 0.034999, 0),
        (0.000001, 35000, 0.035000, 0.01),
        (-0.000001, 35000, -0.035000, -0.01),
        (0.000001, 104999, 0.104999, 0.01),
        (0.000001, 105000, 0.105000, 0.02),
    ]
)
def test_aggregate_rounding_long(event_qty, events_cnt, req_qty, req_sum):
    order = OrderDataBuilder()
    order.add_consume(req_qty, req_sum, 7, 1)
    for idx in range(events_cnt):
        order.add_batched_event(event_qty)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    hamcrest.assert_that(
        sum(e['tariffed_qty'] for e in tariffed_events),
        hamcrest.close_to(req_qty, FLOAT_PRECISION)
    )
    hamcrest.assert_that(
        sum(e['tariffed_sum'] for e in tariffed_events),
        hamcrest.close_to(req_sum, FLOAT_PRECISION)
    )

    assert t.process_order() == {
        'ServiceID': 7,
        'EffectiveServiceOrderID': order.id,
        'state': None,
    }


@pytest.mark.parametrize('consume_qty, events_qty', [(10, [2, 3, 5])])
def test_prefix_batch_cost(consume_qty, events_qty):
    order1 = OrderDataBuilder(effective_order_id=1, batch_cost=0)
    order1.add_consume(consume_qty, id_=1)
    order1.add_consume(consume_qty - events_qty[-1], id_=2)
    for event_qty in events_qty:
        order1.add_batched_event(event_qty)

    order2 = OrderDataBuilder(effective_order_id=1, batch_cost=consume_qty)
    order2.add_consume(consume_qty, id_=1)
    order2.add_consume(consume_qty - events_qty[-1], id_=2)
    for event_qty in events_qty[:2]:
        order2.add_batched_event(event_qty, batch=1)

    sync_info = order1.sync_batched_info
    sync_info['completion_qty_delta'] = consume_qty*2 - events_qty[-1]
    t1 = Tarifficator('test_run', sync_info)
    tariffed_events_1 = tariff_order(t1, order1)

    sync_info = order2.sync_batched_info
    sync_info['completion_qty_delta'] = consume_qty * 2 - events_qty[-1]
    t2 = Tarifficator('test_run', order2.sync_batched_info)
    tariffed_events_2 = tariff_order(t2, order2)

    for i, event in enumerate(tariffed_events_1):
        assert event['consume_id'] == 1
        assert event['tariffed_qty'] == events_qty[i]

    for i, event in enumerate(tariffed_events_2):
        assert event['consume_id'] == 2
        assert event['tariffed_qty'] == events_qty[i]


def test_inaccurate_sum_calculation():
    order = OrderDataBuilder()
    order.add_consume(24000, 240.5, 12000, 120.25)
    order.add_batched_event(+12000)
    order.add_batched_event(+3600)
    order.add_batched_event(+3360)
    order.add_batched_event(+3360)
    order.add_batched_event(+1680)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)
