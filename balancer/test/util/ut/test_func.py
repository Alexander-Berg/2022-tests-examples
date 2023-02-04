# -*- coding: utf-8 -*-
import pytest
from balancer.test.util.func import _func as func


OK_INTERVALS = [
    (5, 10),
    (-10, -5),
    (42, 42),
]
OK_INTERVALS_IDS = [
    'positive',
    'negative',
    'equal',
]


@pytest.mark.parametrize(
    ['start', 'fin'],
    OK_INTERVALS,
    ids=OK_INTERVALS_IDS,
)
def test_interval(start, fin):
    interval = func.Interval(start, fin)

    assert interval.start == start
    assert interval.fin == fin
    assert interval.duration == fin - start
    assert repr(interval)


def test_interval_start_gt_fin():
    with pytest.raises(func.IntervalException):
        func.Interval(1, 0)


@pytest.mark.parametrize(
    ['start', 'fin'],
    OK_INTERVALS,
    ids=OK_INTERVALS_IDS,
)
def test_interval_eq(start, fin):
    left = func.Interval(start, fin)
    right = func.Interval(start, fin)

    assert left == right
    assert hash(left) == hash(right)


@pytest.mark.parametrize(
    ['lstart', 'lfin', 'rstart', 'rfin'],
    [
        (5, 10, 5, 11),
        (5, 10, 6, 10),
        (5, 10, 4, 11),
        (4, 11, 5, 10),
        (5, 8, 7, 10),
        (5, 7, 8, 10),
    ],
    ids=[
        'ne_fin',
        'ne_start',
        'left_subset',
        'right_subset',
        'intersect',
        'disjoint',
    ]
)
def test_interval_ne(lstart, lfin, rstart, rfin):
    left = func.Interval(lstart, lfin)
    right = func.Interval(rstart, rfin)

    assert left != right


def build_intervals(data):
    return [func.Interval(start, fin) for start, fin in data]


@pytest.mark.parametrize(
    ['orig', 'expected'],
    [
        ([], []),
        ([(5, 10), (4, 11)], [(4, 11)]),
        ([(5, 8), (7, 10)], [(5, 10)]),
        ([(5, 8), (7, 10), (9, 12)], [(5, 12)]),
        ([(5, 7), (8, 10)], [(5, 7), (8, 10)]),
    ],
    ids=[
        'empty',
        'subset',
        'intersect_two',
        'intersect_three',
        'disjoint',
    ],
)
def test_join_intervals(orig, expected):
    assert func.join_intervals(build_intervals(orig)) == build_intervals(expected)


@pytest.mark.parametrize(
    ['interval', 'break_intervals', 'expected'],
    [
        ((5, 10), [(7, 8)], [(5, 7), (8, 10)]),
        ((5, 10), [(4, 7)], [(7, 10)]),
        ((5, 10), [(5, 7)], [(7, 10)]),
        ((5, 10), [(8, 11)], [(5, 8)]),
        ((5, 10), [(8, 10)], [(5, 8)]),
        ((5, 10), [(4, 7), (8, 11)], [(7, 8)]),
        ((5, 10), [(4, 11)], []),
    ],
    ids=[
        'subset',
        'intersect_start',
        'intersect_start_eq',
        'intersect_fin',
        'intersect_fin_eq',
        'intersect_start_fin',
        'empty',
    ],
)
def test_break_interval(interval, break_intervals, expected):
    assert func.break_interval(
        func.Interval(interval[0], interval[1]),
        build_intervals(break_intervals)
    ) == build_intervals(expected)


def test_interval_holder_init():
    holder = func.IntervalHolder()

    assert holder.interval is None


def test_interval_holder_setter():
    value = func.Interval(0, 1)
    holder = func.IntervalHolder()
    holder.interval = value

    assert holder.interval == value


def test_interval_holder_setter_not_none():
    value_first = func.Interval(0, 1)
    value_second = func.Interval(2, 3)
    holder = func.IntervalHolder()
    holder.interval = value_first
    holder.interval = value_second

    assert holder.interval == value_first
