# -*- coding: utf-8 -*-

import pytest

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
    find_covering_intervals,
    RangeBorderSelectType,
)
from billing.library.python.logfeller_utils.tests.utils import (
    mk_interval,
)


@pytest.mark.parametrize(
    'target_interval, beginning_type, end_type, intervals, res',
    [
        pytest.param(
            mk_interval(10, 20),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(10, 20),
            ],
            (0, 0),
            id='eq_single'
        ),
        pytest.param(
            mk_interval(10, 20),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(0, 5),
                mk_interval(5, 10),
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (2, 3),
            id='eq_multiple'
        ),
        pytest.param(
            mk_interval(0, 30),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(0, 5),
                mk_interval(5, 10),
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 4),
            id='eq_borders'
        ),
        pytest.param(
            LogInterval([
                Subinterval('c1', 't1', 0, 2, 10),
                Subinterval('c1', 't1', 1, 0, 30),
            ]),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                LogInterval([
                    Subinterval('c1', 't1', 0, 0, 2),
                ]),
                LogInterval([
                    Subinterval('c1', 't1', 0, 2, 5),
                ]),
                LogInterval([
                    Subinterval('c1', 't1', 0, 5, 10),
                    Subinterval('c1', 't1', 1, 0, 30),
                ]),
                LogInterval([
                    Subinterval('c1', 't1', 0, 10, 14),
                    Subinterval('c1', 't1', 1, 30, 35),
                ]),
            ],
            (1, 2),
            id='eq_add_partition'
        ),
        pytest.param(
            mk_interval(10, 20),
            RangeBorderSelectType.le, RangeBorderSelectType.le,
            [
                mk_interval(0, 5),
                mk_interval(5, 10),
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (2, 3),
            id='le_exact'
        ),
        pytest.param(
            mk_interval(10, 20),
            RangeBorderSelectType.ge, RangeBorderSelectType.ge,
            [
                mk_interval(0, 5),
                mk_interval(5, 10),
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (2, 3),
            id='ge_exact'
        ),
        pytest.param(
            mk_interval(10, 25),
            RangeBorderSelectType.eq, RangeBorderSelectType.le,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 1),
            id='end_le_middle'
        ),
        pytest.param(
            mk_interval(10, 35),
            RangeBorderSelectType.eq, RangeBorderSelectType.le,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 2),
            id='end_le_over_border'
        ),
        pytest.param(
            mk_interval(20, 35),
            RangeBorderSelectType.eq, RangeBorderSelectType.le,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (2, 2),
            id='end_le_over_border_single'
        ),
        pytest.param(
            mk_interval(10, 25),
            RangeBorderSelectType.eq, RangeBorderSelectType.ge,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 2),
            id='end_ge_middle'
        ),
        pytest.param(
            mk_interval(10, 11),
            RangeBorderSelectType.eq, RangeBorderSelectType.ge,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 0),
            id='end_ge_single'
        ),
        pytest.param(
            mk_interval(11, 30),
            RangeBorderSelectType.le, RangeBorderSelectType.eq,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (1, 2),
            id='beginning_le_middle'
        ),
        pytest.param(
            mk_interval(9, 20),
            RangeBorderSelectType.le, RangeBorderSelectType.eq,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 1),
            id='beginning_le_over_border'
        ),
        pytest.param(
            mk_interval(9, 15),
            RangeBorderSelectType.le, RangeBorderSelectType.eq,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 0),
            id='beginning_le_over_border_single'
        ),
        pytest.param(
            mk_interval(11, 30),
            RangeBorderSelectType.ge, RangeBorderSelectType.eq,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 2),
            id='beginning_ge_middle'
        ),
        pytest.param(
            mk_interval(14, 15),
            RangeBorderSelectType.ge, RangeBorderSelectType.eq,
            [
                mk_interval(10, 15),
                mk_interval(15, 20),
                mk_interval(20, 30),
            ],
            (0, 0),
            id='beginning_ge_single'
        ),
    ]
)
def test_ok(target_interval, intervals, res, beginning_type, end_type):
    assert find_covering_intervals(target_interval, intervals, beginning_type, end_type) == res


@pytest.mark.parametrize(
    'target_interval, beginning_type, end_type, intervals',
    [
        pytest.param(
            mk_interval(0, 11),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(0, 10),
                mk_interval(10, 20),
            ],
            id='end_eq_bigger'
        ),
        pytest.param(
            mk_interval(0, 9),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(0, 10),
                mk_interval(10, 20),
            ],
            id='end_eq_smaller'
        ),
        pytest.param(
            mk_interval(0, 11),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(0, 10),
            ],
            id='end_eq_border_bigger'
        ),
        pytest.param(
            mk_interval(9, 20),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(0, 10),
                mk_interval(10, 20),
            ],
            id='beginning_eq_bigger'
        ),
        pytest.param(
            mk_interval(11, 20),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(0, 10),
                mk_interval(10, 20),
            ],
            id='beginning_eq_smaller'
        ),
        pytest.param(
            mk_interval(9, 20),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(10, 20),
            ],
            id='beginning_eq_bigger_border'
        ),
        pytest.param(
            mk_interval(0, 9),
            RangeBorderSelectType.eq, RangeBorderSelectType.le,
            [
                mk_interval(0, 10),
                mk_interval(10, 20),
            ],
            id='end_le_smaller'
        ),
        pytest.param(
            mk_interval(0, 21),
            RangeBorderSelectType.eq, RangeBorderSelectType.ge,
            [
                mk_interval(0, 10),
                mk_interval(10, 20),
            ],
            id='end_ge_bigger'
        ),
        pytest.param(
            mk_interval(1, 10),
            RangeBorderSelectType.eq, RangeBorderSelectType.eq,
            [
                mk_interval(0, 10),
                mk_interval(10, 20),
            ],
            id='beginning_le_smaller'
        ),
        pytest.param(
            mk_interval(9, 20),
            RangeBorderSelectType.ge, RangeBorderSelectType.eq,
            [
                mk_interval(10, 20),
            ],
            id='beginning_ge_bigger'
        ),
    ]
)
def test_fail(target_interval, intervals, beginning_type, end_type):
    with pytest.raises(AssertionError, match='got wrong covering interval'):
        find_covering_intervals(target_interval, intervals, beginning_type, end_type)


def test_empty_interval():
    with pytest.raises(AssertionError) as exc_info:
        find_covering_intervals(
            mk_interval(17, 17),
            [mk_interval(15, 20)]
        )
    assert 'Empty target interval' in exc_info.value.args[0]


def test_no_coverage():
    with pytest.raises(AssertionError) as exc_info:
        find_covering_intervals(
            mk_interval(30, 40),
            [
                mk_interval(0, 10),
                mk_interval(10, 20),
                mk_interval(20, 30),
            ],
        )
    assert 'Not found any intersecting intervals' in exc_info.value.args[0]


def test_gap():
    with pytest.raises(AssertionError) as exc_info:
        find_covering_intervals(
            mk_interval(9, 15),
            [
                mk_interval(0, 10),
                mk_interval(11, 20),
            ],
        )

    assert 'gap in offsets at' in exc_info.value.args[0]
