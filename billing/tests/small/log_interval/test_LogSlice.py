
import pytest

from billing.library.python.logfeller_utils.log_interval import (
    LogSlice
)


@pytest.mark.parametrize(
    ['s1', 's2'],
    [
        pytest.param(
            LogSlice({}),
            LogSlice({}),
            id='empty'
        ),
        pytest.param(
            LogSlice({('c1', 't1', 0): 1}),
            LogSlice({('c1', 't1', 0): 1}),
            id='not empty'
        ),
    ]
)
def test_equals(s1, s2):
    assert s1 == s2
    assert not s1 != s2
    assert not s1 < s2
    assert not s2 < s1
    assert not s1 > s2
    assert not s2 > s1
    assert s1 <= s2
    assert s2 <= s1
    assert s1 >= s2
    assert s2 >= s1


def test_does_not_equal():
    # s1 < s2
    s1 = LogSlice({('c1', 't1', 0): 1})
    s2 = LogSlice({('c1', 't1', 0): 2})
    assert s1 != s2
    assert not s1 == s2
    assert s1 < s2
    assert s2 > s1
    assert not s2 < s1
    assert not s1 > s2
    assert s1 <= s2
    assert not s2 <= s1
    assert not s1 >= s2
    assert s2 >= s1

    assert LogSlice({}) < s2


@pytest.mark.parametrize(
    ['s1', 's2'],
    [
        pytest.param(
            LogSlice({('c1', 't1', 0): 1,
                      ('c1', 't1', 1): 2}),
            LogSlice({('c1', 't1', 0): 2,
                      ('c1', 't1', 1): 1}),
            id='same partitions overlapping'
        ),
        pytest.param(
            LogSlice({('c1', 't1', 0): 1,
                      ('c1', 't1', 1): 1}),
            LogSlice({('c1', 't1', 0): 2,
                      ('c1', 't1', 2): 1}),
            id='different partitions set'
        ),
    ]
)
def test_not_comparable(s1, s2):
    assert not s1 == s2
    assert s1 != s2
    assert not s1 < s2
    assert not s2 < s1
    assert not s1 > s2
    assert not s2 > s1
    assert not s1 <= s2
    assert not s2 <= s1
    assert not s1 >= s2
    assert not s2 >= s1


def test_precedes():
    s1 = LogSlice({})

    # Contains zero partition
    s2 = LogSlice({('c1', 't1', 0): 2,
                   ('c1', 't1', 1): 0})

    s3 = LogSlice({('c1', 't1', 0): 2,
                   ('c1', 't1', 1): 0,
                   ('c1', 't1', 2): 0})

    assert s1.precedes(s1)
    assert s2.precedes(s2)
    assert not s1.precedes(s2)  # New non-zero partition
    assert s1.precedes(LogSlice({('c1', 't1', 0): 0}))  # New zero partition
    assert s2.precedes(s3)  # New zero partition
    assert not s3.precedes(s2)
    assert not s2.precedes(s1)
    # New non-zero partition
    assert not s2.precedes(LogSlice({('c1', 't1', 0): 2,
                                     ('c1', 't1', 1): 0,
                                     ('c1', 't1', 2): 5}))


@pytest.mark.parametrize(
    'meta, res',
    [
        (
            [{'topic': 'b', 'cluster': 'a', 'partition': 666, 'offset': 14}],
            LogSlice({('b', 'a', 666): 14}),
        ),
        (
            [
                {'topic': 'b', 'cluster': 'a', 'partition': 666, 'offset': 14},
                {'topic': 'c', 'cluster': 'a', 'partition': 666, 'offset': 15},
                {'topic': 'a', 'cluster': 'b', 'partition': 1, 'offset': 1},
            ],
            LogSlice({
                ('b', 'a', 666): 14,
                ('c', 'a', 666): 15,
                ('a', 'b', 1): 1,
            }),
        ),
        (
            [],
            LogSlice({})
        )
    ]
)
def test_from_meta(meta, res):
    assert LogSlice.from_meta(meta) == res


@pytest.mark.parametrize(
    'log_slice, res',
    [
        (
            LogSlice({('b', 'a', 666): 14}),
            [{'topic': 'b', 'cluster': 'a', 'partition': 666, 'offset': 14}],
        ),
        (
            LogSlice({
                ('b', 'a', 666): 14,
                ('c', 'a', 666): 15,
                ('a', 'b', 1): 1,
            }),
            [
                {'topic': 'b', 'cluster': 'a', 'partition': 666, 'offset': 14},
                {'topic': 'c', 'cluster': 'a', 'partition': 666, 'offset': 15},
                {'topic': 'a', 'cluster': 'b', 'partition': 1, 'offset': 1},
            ],
        ),
        (
            LogSlice({}),
            [],
        )
    ]
)
def test_to_meta(log_slice, res):
    assert log_slice.to_meta() == res


def test_without_zeroes():
    assert LogSlice({
        ('a', 'a', 0): 0,
        ('a', 'a', 1): 1,
    }).without_zeroes == LogSlice({
        ('a', 'a', 1): 1,
    })
