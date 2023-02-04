
import pytest

from balance.muzzle_util import valid_id, dt_intersects
from collections import namedtuple
from datetime import datetime


@pytest.mark.parametrize(['value', 'expected'], [(0, False), (None, False), (-1, False), (1, True)])
def test_valid_id(value, expected):
    assert valid_id(value) == expected


p1 = datetime.strptime('2019-01-01', '%Y-%m-%d')
p2 = datetime.strptime('2019-02-02', '%Y-%m-%d')
p3 = datetime.strptime('2019-02-10', '%Y-%m-%d')
p4 = datetime.strptime('2019-03-03', '%Y-%m-%d')


Interval = namedtuple('Interval', ['start', 'end'])


@pytest.mark.parametrize(['interval_1', 'interval_2', 'expected'], [
    [Interval(p1, p1), Interval(p1, p1), True],
    [Interval(p1, p1), Interval(p1, p3), True],
    [Interval(p3, p3), Interval(p1, p3), True],
    [Interval(p1, p4), Interval(p1, p4), True],
    [Interval(p3, p3), Interval(p1, p4), True],
    [Interval(p1, p4), Interval(p2, p3), True],
    [Interval(None, None), Interval(None, None), True],
    [Interval(None, None), Interval(p1, None), True],
    [Interval(None, None), Interval(None, p1), True],
    [Interval(None, p4), Interval(None, p1), True],
    [Interval(p1, None), Interval(p4, None), True],
    [Interval(None, p4), Interval(p1, None), True],
    [Interval(None, None), Interval(p1, p3), True],
    [Interval(p1, p4), Interval(p3, None), True],
    [Interval(p1, p4), Interval(None, p3), True],
    [Interval(p1, p2), Interval(p3, p4), False],
    [Interval(None, p1), Interval(p2, p3), False],
    [Interval(None, p1), Interval(p2, None), False],
    [Interval(p3, None), Interval(p1, p2), False],
    [Interval(p3, None), Interval(None, p1), False],
])
def test_dt_intersects(interval_1, interval_2, expected):
    assert dt_intersects(interval_1.start, interval_1.end, interval_2.start, interval_2.end) == expected