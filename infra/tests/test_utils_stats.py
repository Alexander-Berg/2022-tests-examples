from __future__ import division

from ya.skynet.services.heartbeatserver.utils.stats import (
    TimeCounter, TimeFrameCounter, TimeSumCounter, TimeIncrementCounter
)

import os
import random


def test_base_counter_push():
    import pytest
    cnt = TimeCounter(10)
    with pytest.raises(NotImplementedError):
        cnt.push(1, 2)


def test_timeframe_no_stats():
    cnt = TimeFrameCounter(5)
    assert cnt.stat() == 0


def test_timeframe_negative_interval():
    cnt = TimeFrameCounter(5)
    cnt.start(1)
    assert cnt.stop(0) == 0


def test_timeframe_basic():
    cnt = TimeFrameCounter(3)
    cnt.start(0)                       # t:0   w:0
    assert cnt.stop(0.5) == 0.5 / 0.5  # t:0.5 w:0.5
    cnt.start(1)
    assert cnt.stop(1.5) == 1.0 / 1.5  # t:1.5 w:1.0
    cnt.start(2)
    assert cnt.stop(2.5) == 1.5 / 2.5  # t:2.5 w:1.5
    cnt.start(3)
    assert cnt.stop(3.5) == 1.5 / 2.5  # t:2.5 w:1.5
    cnt.start(3.7)
    assert cnt.stop(3.9) == 1.7 / 2.9  # t:2.9 w:1.7
    cnt.start(3.9)
    assert cnt.stop(4.0) == 1.8 / 3.0  # t:3.0 w:1.8
    cnt.start(5)
    assert cnt.stop(6.0) == 1.8 / 3.0  # t:3.0 w:1.8


def test_timeframe_long_periods():
    cnt = TimeFrameCounter(3)
    cnt.start(0)
    assert cnt.stop(6) == 1

    cnt.start(12)
    assert cnt.stop(13) == 1 / 3

    cnt.start(20)
    assert cnt.stop(30) == 1

    cnt.start(31)
    assert cnt.stop(32) == 2 / 3


def test_timeframe_micro_periods():
    cnt = TimeFrameCounter(30)
    cnt.start(0)
    assert cnt.stop(0.1) == 1
    cnt.start(20)
    assert int(cnt.stop(20.1) * 10 ** 6) == int(0.2 / 20.1 * 10 ** 6)
    cnt.start(20.1)
    assert int(cnt.stop(20.2) * 10 ** 6) == int(0.3 / 20.2 * 10 ** 6)


def test_timeframe_random():
    cnt = TimeFrameCounter(250)

    ts = 0
    itms = []
    for _ in range(300):
        wchoice = 1 / 3 + (1 / 6 * (random.random() - 0.5))  # 0.17..0.33..0.49
        itms.append(wchoice)
        ftime = 2 + random.random() * 2
        wtime = ftime * wchoice
        stime = ftime - wtime

        ts += stime
        cnt.start(ts)
        ts += wtime
        cnt.stop(ts)

    itms = itms[50:-1]
    avg = sum(itms) / len(itms)
    assert (avg - 0.02) <= cnt.stat() <= (avg + 0.02)


def test_timeframe_multiple_averages():
    cnt = TimeFrameCounter(5)
    cnt.addCounter(2)

    cnt.start(0)
    assert cnt.stop(0.5) == 1
    cnt.start(1)
    assert cnt.stop(1.5) == 1 / 1.5
    cnt.start(2)
    assert cnt.stop(2.5) == 1.5 / 2.5
    cnt.start(3)
    assert cnt.stop(3.75) == 2.25 / 3.75
    cnt.start(4.25)
    assert cnt.stop(5) == 3 / 5

    assert cnt.stat() == 3 / 5
    assert cnt.stat(2) == 1.5 / 2
    assert cnt.stat(1) == 0.75 / 1


def test_timeframe_double_averages():
    cnt = TimeFrameCounter(1)
    assert cnt.addCounter(1) is False
    assert cnt.addCounter(2) is True
    assert cnt.addCounter(2) is False

    assert cnt._counters.keys() == [1, 2]
    assert len(cnt._countersList) == 2


def test_timeframe_speed():
    import timeit
    import math

    def _check():
        cnt = TimeFrameCounter(30)
        step = 0.01
        for i in xrange(1, 10001, 2):
            value = i * step
            cnt.start(value)
            cnt.stop(value + step)
        assert math.fabs(0.5 - cnt.stat()) < 1e-6  # precision check

    tmr = timeit.Timer(_check, timer=lambda: sum(os.times()[:2]))
    time = min(tmr.repeat(15, 1))

    ustime = time * 200

    print('%dus per calc' % (ustime, ))
    assert ustime < 15


def test_sum_nostats():
    cnt = TimeSumCounter(5)
    assert cnt.stat() == 0


def test_sum_basic():
    cnt = TimeSumCounter(3)
    assert cnt.push(0, 0) == 0
    assert cnt.push(1, 1) == 1
    assert cnt.push(2, 1) == 2
    assert cnt.push(3, 1) == 3
    assert cnt.push(4, 0.5) == 2.5
    assert cnt.push(5, 0.5) == 2
    assert cnt.push(5.5, 1) == 3
    assert cnt.push(5.6, 1) == 4
    assert cnt.push(6, 0) == 3


def test_sum_long_periods():
    cnt = TimeSumCounter(3)
    assert cnt.push(6, 1) == 1
    assert cnt.push(30, 1) == 1
    assert cnt.push(31, 1) == 2


def test_sum_random():
    import math
    cnt = TimeSumCounter(250)

    intgr = random.randint(3, 30)

    ts = 0
    itms = []

    for _ in range(1000):
        wchoice = 1 / 3 + (1 / 6 * (random.random() - 0.5))  # 0.17..0.33..0.49

        ts += wchoice

        value = intgr * wchoice
        itms.append((ts, value))

        cnt.push(ts, value)

    assert math.fabs(cnt.stat() - sum(i[1] for i in itms if i[0] > int(ts - 250 + 1))) < 1e-6


def test_sum_multiple_averages():
    cnt = TimeSumCounter(5)
    cnt.addCounter(2)

    assert cnt.push(0.5, 1) == 1
    assert cnt.push(1.5, 1) == 2
    assert cnt.stat(2) == 2
    assert cnt.stat(1) == 1
    assert cnt.push(3.75, 1) == 3
    assert cnt.stat(2) == 1
    assert cnt.push(5, 1) == 3
    assert cnt.stat(2) == 1
    assert cnt.stat(1) == 1


def test_sum_speed():
    import timeit

    def _check():
        cnt = TimeSumCounter(300)
        step = 0.01
        summ = 0
        for i in xrange(1, 20001, 2):
            ts = step * i
            value = ts + step
            summ += value
            cnt.push(ts, value)
        assert cnt.stat() == summ

    tmr = timeit.Timer(_check, timer=lambda: sum(os.times()[:2]))
    time = min(tmr.repeat(15, 1))

    ustime = time * 100

    print('%dus per calc' % (ustime, ))
    assert ustime < 10


def test_increment_counter():
    cnt1 = TimeSumCounter(30)
    cnt2 = TimeIncrementCounter(30)

    for i in range(100):
        ts = 0.1 * i
        cnt1.push(ts, 1)
        cnt2.push(ts)

    assert cnt1.stat() == cnt2.stat()
