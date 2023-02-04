
import pytest

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval, Subinterval, LogSlice
)


def test_parsing():
    # Example taken from real table attribute
    meta = {
        "topics": [
            {
                "cluster": "kafka-bs",
                "topic": "yabs-sb/bs-billable-event-log",
                "logbroker_sync_timestamp_ms": 1586170769354,
                "last_step_with_topics_table": 37101,
                "lb_partitions": [],
                "partitions": [
                    {
                        "next_offset": 505990,
                        "first_offset": 505837,
                        "next_offset_write_timestamp_lower_bound_ms": 1586170491853,
                        "partition": 0
                    },
                    {
                        "next_offset": 504385,
                        "first_offset": 504140,
                        "next_offset_write_timestamp_lower_bound_ms": 1586170519466,
                        "partition": 1
                    },
                ]
            },
            {
                "cluster": "kafka-bx",
                "topic": "yabs-xb/bs-billable-event-log",
                "logbroker_sync_timestamp_ms": 1586170769354,
                "last_step_with_topics_table": 37101,
                "lb_partitions": [],
                "partitions": [
                    {
                        "next_offset": 505990,
                        "first_offset": 505837,
                        "next_offset_write_timestamp_lower_bound_ms": 1586170491853,
                        "partition": 0
                    },
                ]
            }
        ]
    }

    assert LogInterval.from_meta(meta) == LogInterval([
        Subinterval('kafka-bs', 'yabs-sb/bs-billable-event-log', 0, 505837, 505990),
        Subinterval('kafka-bs', 'yabs-sb/bs-billable-event-log', 1, 504140, 504385),
        Subinterval('kafka-bx', 'yabs-xb/bs-billable-event-log', 0, 505837, 505990),
    ])


def test_serialization():
    initial = LogInterval([
        Subinterval('c1', 't1', 0, 500, 501),
        Subinterval('c1', 't1', 1, 400, 401),  # second partition in topic
        Subinterval('c1', 't2', 0, 300, 301),  # second topic in cluster
        Subinterval('c2', 't1', 0, 600, 601),  # different cluster
    ])

    assert LogInterval.from_meta(initial.to_meta()) == initial


def test_no_intersection():
    left_interval = LogInterval([
        Subinterval('c1', 't1', 0, 50, 55),  # successive
        Subinterval('c1', 't1', 1, 50, 55),  # successive with empty
        Subinterval('c1', 't1', 2, 55, 55),  # empty
        Subinterval('c1', 't1', 3, 50, 55),  # different key
    ])
    right_interval = LogInterval([
        Subinterval('c1', 't1', 0, 55, 60),  # successive
        Subinterval('c1', 't1', 1, 55, 55),  # successive with empty
        Subinterval('c1', 't1', 2, 55, 55),  # empty
        Subinterval('c1', 't1', 4, 50, 55),  # different key
    ])
    assert not left_interval.intersects_with(right_interval)
    assert not right_interval.intersects_with(left_interval)


def test_intersects():
    left_interval = LogInterval([
        Subinterval('c1', 't1', 0, 55, 65),  # intersects
        Subinterval('c1', 't1', 1, 50, 55),  # not intersects, successive
        Subinterval('c1', 't2', 0, 50, 55),  # not intersects, different key
    ])
    right_interval = LogInterval([
        Subinterval('c1', 't1', 0, 60, 70),  # intersects
        Subinterval('c1', 't1', 1, 55, 60),  # not intersects, successive
        Subinterval('c1', 't3', 0, 50, 55),  # not intersects, different key
    ])
    assert left_interval.intersects_with(right_interval)
    assert right_interval.intersects_with(left_interval)


def test_intersects_empty_in_the_middle_case():
    left_interval = LogInterval([
        Subinterval('c1', 't1', 0, 55, 65),
    ])
    right_interval = LogInterval([
        Subinterval('c1', 't1', 0, 57, 57),  # empty, inside left interval
    ])
    assert left_interval.intersects_with(right_interval)
    assert right_interval.intersects_with(left_interval)


@pytest.mark.parametrize(
    ['target_subintervals', 'expected_result'],
    [
        pytest.param(
            [Subinterval('c1', 't1', 0, 57, 68),
             Subinterval('c1', 't1', 2, 65, 73)],
            False,
            id='Partition is not covered'
        ),
        pytest.param(
            [Subinterval('c1', 't1', 0, 37, 68),
             Subinterval('c1', 't1', 1, 60, 68)],
            False,
            id='Beginning of the interval is not covered'
        ),
        pytest.param(
            [Subinterval('c1', 't1', 0, 50, 78)],
            False,
            id='End of the interval is not covered'
        ),
        pytest.param(
            [Subinterval('c1', 't1', 0, 45, 70)],
            True,
            id='Exact coverage'
        ),
        pytest.param(
            [Subinterval('c1', 't1', 0, 50, 60)],
            True,
            id='Overcoverage'
        ),
    ]
)
def test_covers(target_subintervals, expected_result):
    assert LogInterval([
        Subinterval('c1', 't1', 0, 45, 70),
        Subinterval('c1', 't1', 1, 45, 70),
    ]).covers(
        LogInterval(target_subintervals)
    ) is expected_result


@pytest.mark.parametrize(
    ['target_subintervals', 'covers_kwargs'],
    [
        pytest.param(
            [Subinterval('c1', 't1', 0, 20, 60)],
            dict(check_beginning=False),
            id='check_beginning=False'
        ),
        pytest.param(
            [Subinterval('c1', 't1', 0, 40, 80)],
            dict(check_end=False),
            id='check_end=False'
        ),
    ]
)
def test_covers_partial(target_subintervals, covers_kwargs):
    assert LogInterval([
        Subinterval('c1', 't1', 0, 30, 70),
    ]).covers(
        LogInterval(target_subintervals),
        **covers_kwargs
    )


def test_slice_properties():
    interval = LogInterval([
        Subinterval('c1', 't1', 0, 30, 70),
        Subinterval('c1', 't1', 1, 10, 20),
    ])
    assert interval.beginning == LogSlice({
        ('c1', 't1', 0): 30,
        ('c1', 't1', 1): 10,
    })
    assert interval.end == LogSlice({
        ('c1', 't1', 0): 70,
        ('c1', 't1', 1): 20,
    })

    empty_interval = LogInterval([])
    empty_slice = LogSlice({})
    assert empty_interval.beginning == empty_slice
    assert empty_interval.end == empty_slice


class TestSlicesConstructor(object):
    def test_partition_disappear(self):
        with pytest.raises(AssertionError, match='Partition sets mismatch in slices'):
            LogInterval.from_slices(
                LogSlice({
                    ('c1', 't1', 0): 70,
                    ('c1', 't1', 1): 20,
                }),
                LogSlice({
                    ('c1', 't1', 0): 80,
                }),
            )

    def test_partition_addition_strict(self):
        with pytest.raises(AssertionError, match='Partition sets mismatch in slices'):
            LogInterval.from_slices(
                LogSlice({
                    ('c1', 't1', 0): 70,
                }),
                LogSlice({
                    ('c1', 't1', 0): 80,
                    ('c1', 't1', 1): 30,
                }),
                strict=True
            )

    def test_partition_addition(self):
        res = LogInterval.from_slices(
            LogSlice({
                ('c1', 't1', 0): 70,
            }),
            LogSlice({
                ('c1', 't1', 0): 80,
                ('c1', 't1', 1): 30,
            }),
            strict=False
        )
        assert res == LogInterval([
            Subinterval('c1', 't1', 0, 70, 80),
            Subinterval('c1', 't1', 1, 0, 30),
        ])

    def test_backward_partition(self):
        with pytest.raises(AssertionError, match='first_offset > next_offset'):
            LogInterval.from_slices(
                LogSlice({
                    ('c1', 't1', 0): 90,
                }),
                LogSlice({
                    ('c1', 't1', 0): 30,
                }),
            )

    def test_ok(self):
        assert LogInterval.from_slices(
            LogSlice({
                ('c1', 't1', 0): 70,
                ('c1', 't1', 1): 30,
            }),
            LogSlice({
                ('c1', 't1', 0): 80,
                ('c1', 't1', 1): 30,
            }),
        ) == LogInterval([
            Subinterval('c1', 't1', 0, 70, 80),
            Subinterval('c1', 't1', 1, 30, 30),
        ])


def test_without_zeroes():
    assert LogInterval([
        Subinterval('a', 'a', 1, 0, 0),
        Subinterval('a', 'a', 2, 0, 10),
        Subinterval('a', 'a', 3, 10, 20),
    ]).without_zeroes == LogInterval([
        Subinterval('a', 'a', 2, 0, 10),
        Subinterval('a', 'a', 3, 10, 20),
    ])
