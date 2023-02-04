# -*- coding: utf-8 -*-

import pytest

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
    check_intervals_alignment
)


def test_ok():
    res = check_intervals_alignment([
        LogInterval([
            Subinterval('c', 't', 0, 0, 10),
            Subinterval('c', 't', 1, 10, 15),
        ]),
        LogInterval([
            Subinterval('c', 't', 0, 10, 20),
            Subinterval('c', 't', 1, 15, 15),
        ]),
        LogInterval([
            Subinterval('c', 't', 0, 20, 30),
            Subinterval('c', 't', 1, 15, 20),
        ]),
        LogInterval([
            Subinterval('c', 't', 0, 30, 40),
            Subinterval('c', 't', 1, 20, 21),
            Subinterval('c1', 't1', 0, 0, 20),
        ]),
        LogInterval([
            Subinterval('c', 't', 0, 40, 41),
            Subinterval('c', 't', 1, 21, 22),
            Subinterval('c1', 't1', 0, 20, 21),
        ]).to_meta(),
    ])
    assert res is True


def test_split():
    with pytest.raises(AssertionError) as exc_info:
        check_intervals_alignment([
            LogInterval([
                Subinterval('c', 't', 0, 0, 10),
                Subinterval('c', 't', 1, 10, 15),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 10, 20),
                Subinterval('c', 't', 1, 16, 17),
            ]),
        ])

    assert 'Intervals misaligned:' in exc_info.value.args[0]


def test_intersection():
    with pytest.raises(AssertionError) as exc_info:
        check_intervals_alignment([
            LogInterval([
                Subinterval('c', 't', 0, 0, 10),
                Subinterval('c', 't', 1, 10, 15),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 10, 20),
                Subinterval('c', 't', 1, 14, 17),
            ]),
        ])

    assert 'Intervals misaligned:' in exc_info.value.args[0]


def test_split_new_partition():
    with pytest.raises(AssertionError) as exc_info:
        check_intervals_alignment([
            LogInterval([
                Subinterval('c', 't', 0, 0, 10),
                Subinterval('c', 't', 1, 10, 15),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 10, 20),
                Subinterval('c', 't', 1, 15, 17),
                Subinterval('c1', 't1', 0, 1, 666),
            ]),
        ])

    assert 'Intervals misaligned:' in exc_info.value.args[0]
