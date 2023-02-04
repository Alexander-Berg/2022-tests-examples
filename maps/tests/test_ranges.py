import pytest

from itertools import permutations
from typing import List

import maps.analyzer.pylibs.ranges as r


TEST_SET: List[r.IntRanges] = [
    r.IntRanges.until(10),
    r.IntRanges.until(100),
    r.IntRanges.after(0),
    r.IntRanges.after(50),
    r.IntRanges.after(200),
    r.IntRanges.within(-5, 3),
    r.IntRanges.within(10, 100),
    r.IntRanges.within(150, 180),
    r.IntRanges.within(150, 250),
    r.IntRanges.empty(),
    r.IntRanges.inf(),
]


def test_unbounded():
    assert r.IntRanges.inf().unbounded
    assert r.IntRanges.until(10).unbounded
    assert r.IntRanges.after(10).unbounded
    assert not r.IntRanges.within(1, 10).unbounded
    assert (-r.IntRanges.within(1, 10)).unbounded

    assert not r.IntRanges.inf().bounded
    assert not r.IntRanges.until(10).bounded
    assert not r.IntRanges.after(10).bounded
    assert r.IntRanges.within(1, 10).bounded
    assert not (-r.IntRanges.within(1, 10)).bounded


def test_bounds():
    assert r.IntRanges.inf().lower_bound is None
    assert r.IntRanges.empty().lower_bound is None
    assert r.IntRanges.inf().upper_bound is None
    assert r.IntRanges.empty().upper_bound is None
    assert r.IntRanges.until(10).upper_bound == 10
    assert r.IntRanges.until(10).lower_bound is None
    assert r.IntRanges.after(100).lower_bound == 100
    assert r.IntRanges.after(100).upper_bound is None
    assert r.IntRanges.within(10, 100).lower_bound == 10
    assert r.IntRanges.within(10, 100).upper_bound == 100

    s = r.IntRanges.within(10, 100) | r.IntRanges.within(200, 500)
    assert s.lower_bound == 10
    assert s.upper_bound == 500
    p = r.IntRanges.at(50)
    assert p.lower_bound == 50
    assert p.upper_bound == 50


def test_inversed():
    assert -r.IntRanges.until(10) == r.IntRanges.after(11)
    assert -r.IntRanges.within(10, 20) == (r.IntRanges.until(9) | r.IntRanges.after(21))

    for t in TEST_SET:
        assert -(-t) == t

    for lhs, rhs in permutations(TEST_SET, 2):
        assert -(lhs & rhs) == -lhs | -rhs
        assert -(lhs | rhs) == -lhs & -rhs


def test_ops():
    # merges neighbour points
    assert r.IntRanges.within(1, 4) | r.IntRanges.within(5, 10) | r.IntRanges.within(11, 12) == r.IntRanges.within(1, 12)

    for t in TEST_SET:
        assert t | r.IntRanges.inf() == r.IntRanges.inf()
        assert t & r.IntRanges.empty() == r.IntRanges.empty()
        assert t - r.IntRanges.inf() == r.IntRanges.empty()
        assert t - r.IntRanges.empty() == t
        assert t - t == r.IntRanges.empty()
        assert t | t == t
        assert t & t == t

    for lhs, rhs in permutations(TEST_SET, 2):
        assert lhs | rhs == rhs | lhs
        assert lhs & rhs == rhs & lhs


def test_contains():
    for t in TEST_SET:
        bounded_range = t & r.IntRanges.within(-100, 100) if t.unbounded else t
        inv_range = -bounded_range

        for v in range(-100, 100):
            assert (v in bounded_range) == (v not in inv_range)

        assert r.IntRanges.from_list(bounded_range.to_list()) == bounded_range


def test_inf_iter():
    with pytest.raises(ValueError):
        for _v in r.IntRanges.inf():
            break

    with pytest.raises(ValueError):
        for _v in r.IntRanges.until(10):
            break

    with pytest.raises(ValueError):
        for _v in r.IntRanges.after(10):
            break


def test_shifted():
    assert r.IntRanges.until(10).shifted(-2) == r.IntRanges.until(8)
    assert r.IntRanges.until(10).shifted(2) == r.IntRanges.until(12)
    assert r.IntRanges.inf().shifted(100) == r.IntRanges.inf()
    assert r.IntRanges.empty().shifted(100) == r.IntRanges.empty()
    assert r.IntRanges.within(4, 10).shifted(100) == r.IntRanges.within(104, 110)


def test_extended():
    assert (r.IntRanges.within(2, 3) | r.IntRanges.within(9, 11)).extended(left=1, right=2) == r.IntRanges.within(1, 5) | r.IntRanges.within(8, 13)
    assert (r.IntRanges.within(2, 3) | r.IntRanges.within(5, 6)).extended(left=1, right=1) == r.IntRanges.within(1, 7)
    assert (r.IntRanges.until(3) | r.IntRanges.after(7)).extended(left=1, right=2) == r.IntRanges.until(5) | r.IntRanges.after(6)
    assert (r.IntRanges.until(3) | r.IntRanges.after(4)).extended(left=1, right=2) == r.IntRanges.inf()

    for t in TEST_SET:
        assert t.extended(left=1) == t | t.shifted(offset=-1)
        assert t.extended(right=1) == t | t.shifted(offset=1)
        assert t.extended(left=1, right=1) == t | t.shifted(-1) | t.shifted(1)


def test_shrinked():
    assert (r.IntRanges.until(3) | r.IntRanges.within(5, 10)).shrinked(left=2, right=1) == r.IntRanges.until(2) | r.IntRanges.within(7, 9)
    assert (r.IntRanges.until(3) | r.IntRanges.within(5, 6)).shrinked(left=2, right=1) == r.IntRanges.until(2)
    assert r.IntRanges.within(4, 6).shrinked(left=1, right=2) == r.IntRanges.empty()

    for t in TEST_SET:
        assert t.shrinked(left=1) == t & t.shifted(offset=1)
        assert t.shrinked(right=1) == t & t.shifted(offset=-1)
        assert t.shrinked(left=1, right=1) == t & t.shifted(1) & t.shifted(-1)
