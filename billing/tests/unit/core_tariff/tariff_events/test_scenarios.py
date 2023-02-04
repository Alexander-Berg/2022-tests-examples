# -*- coding: utf-8 -*-

from billing.log_tariffication.py.lib.logic.tarifficator import (
    Tarifficator,
)
from billing.log_tariffication.py.tests.utils import (
    OrderDataBuilder,
)

from .common import (
    tariff_order,
    get_result,
)


def test_completion_eq():
    order = OrderDataBuilder()
    order.add_consume(3)
    order.add_consume(8)
    order.add_batched_event(3)
    order.add_batched_event(8)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_split():
    order = OrderDataBuilder()
    order.add_consume(10)
    order.add_consume(12)
    order.add_batched_event(1)
    order.add_batched_event(7)
    order.add_batched_event(5)
    order.add_batched_event(9)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_rollback():
    order = OrderDataBuilder()
    order.add_consume(-10)
    order.add_consume(-12)
    order.add_batched_event(-1)
    order.add_batched_event(-7)
    order.add_batched_event(-5)
    order.add_batched_event(-9)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_full_untariffed():
    order = OrderDataBuilder()
    order.add_batched_event(1)
    order.add_batched_event(5)
    order.add_batched_event(7)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_rollback_full_untariffed():
    order = OrderDataBuilder()
    order.add_batched_event(-6)
    order.add_batched_event(-66)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_zero_consume_compensated():
    order = OrderDataBuilder()
    order.add_consume(0)
    order.add_batched_event(6)
    order.add_batched_event(60)
    order.add_batched_event(-66)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_no_consume_compensated():
    order = OrderDataBuilder()
    order.add_batched_event(6)
    order.add_batched_event(60)
    order.add_batched_event(-66)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_rollback_zero_consume_compensated():
    order = OrderDataBuilder()
    order.add_consume(0)
    order.add_batched_event(-6)
    order.add_batched_event(-60)
    order.add_batched_event(66)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_rollback_no_consume_compensated():
    order = OrderDataBuilder()
    order.add_batched_event(-6)
    order.add_batched_event(-60)
    order.add_batched_event(66)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_untariffed():
    order = OrderDataBuilder()
    order.add_consume(3)
    order.add_consume(8)
    order.add_batched_event(1)
    order.add_batched_event(5)
    order.add_batched_event(7)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_untariffed_eq():
    order = OrderDataBuilder()
    order.add_consume(6)
    order.add_batched_event(6)
    order.add_batched_event(3)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_rollback_untariffed():
    order = OrderDataBuilder()
    order.add_consume(-2)
    order.add_consume(-3)
    order.add_batched_event(-3)
    order.add_batched_event(-4)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_overflow_pos():
    order = OrderDataBuilder()
    order.add_consume(7)
    order.add_consume(3)
    order.add_batched_event(4)
    order.add_batched_event(-3)
    order.add_batched_event(11)
    order.add_batched_event(-3)
    order.add_batched_event(1)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_overflow_neg():
    order = OrderDataBuilder()
    order.add_consume(3)
    order.add_consume(7)
    order.add_batched_event(3)
    order.add_batched_event(-4)
    order.add_batched_event(11)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_rollback_overflow_neg():
    order = OrderDataBuilder()
    order.add_consume(-3)
    order.add_consume(-2)
    order.add_batched_event(-5)
    order.add_batched_event(3)
    order.add_batched_event(-4)
    order.add_batched_event(1)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_rollback_overflow_pos():
    order = OrderDataBuilder()
    order.add_consume(-4)
    order.add_consume(-2)
    order.add_batched_event(-4)
    order.add_batched_event(6)
    order.add_batched_event(-1)
    order.add_batched_event(-7)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completions_overflow_both():
    order = OrderDataBuilder()
    order.add_consume(10)
    order.add_batched_event(12)
    order.add_batched_event(-13)
    order.add_batched_event(11)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_completion_untariffed_overflow():
    order = OrderDataBuilder()
    order.add_consume(10)
    order.add_batched_event(3)
    order.add_batched_event(-4)
    order.add_batched_event(13)
    order.add_batched_event(-1)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_rollback_untariffed_overflow():
    order = OrderDataBuilder()
    order.add_consume(-10)
    order.add_batched_event(-3)
    order.add_batched_event(4)
    order.add_batched_event(-14)
    order.add_batched_event(1)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_untariffed_rollback():
    order = OrderDataBuilder()
    order.add_consume(10)
    order.add_batched_event(11)
    order.add_batched_event(-1)
    order.add_batched_event(-3)
    order.add_batched_event(5)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_state_tariff_dt():
    order = OrderDataBuilder(state='123213', tariff_dt=666)
    order.add_consume(222)
    order.add_consume(444)
    order.add_batched_event(333)
    order.add_batched_event(333)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_null_consume():
    order = OrderDataBuilder()
    order.add_consume(666, None, None, None, None)
    order.add_batched_untariffed_event(666)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)


def test_null_consume_overcompletion():
    order = OrderDataBuilder()
    order.add_consume(10, None, None, None, None)
    order.add_batched_event(5)
    order.add_batched_event(-1)
    order.add_batched_event(8)

    t = Tarifficator('test_run', order.sync_batched_info)
    tariffed_events = tariff_order(t, order)

    return get_result(t, tariffed_events)
