
import pytest

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval, Subinterval, check_new_partitions, get_next_stream_table_name
)

from billing.library.python.logfeller_utils.tests.utils import generate_tables


@pytest.mark.parametrize(
    ['target_log_subintervals', 'exception_match'],
    [
        pytest.param(
            [('c1', 't1', 1, 1, 2)],
            'Partitions are not found in tables',
            id='Partitions are not found in tables'
        ),
        pytest.param(
            [('c1', 't1', 0, 1, 2)],
            'Failed to find initial offsets for some partitions',
            id='Failed to find initial offsets for some partitions'
        )
    ]
)
def test_exceptions(target_log_subintervals, exception_match):
    tables = generate_tables([
        LogInterval([
            Subinterval('c1', 't1', 0, 45, 50),
        ]),
    ])
    target_interval = LogInterval([
        Subinterval(*log_subinterval) for log_subinterval in target_log_subintervals
    ])
    with pytest.raises(AssertionError, match=exception_match):
        check_new_partitions(tables, LogInterval([]).beginning, target_interval.end)


def test_missing_table():
    first_interval = LogInterval([
        Subinterval('c1', 't1', 0, 0, 10),
    ])
    tables = generate_tables([first_interval])
    next_interval = LogInterval([
        Subinterval('c1', 't1', 0, 10, 20),
        Subinterval('c1', 't1', 1, 15, 20),
    ])
    tables.extend(generate_tables([
        next_interval,
    ], first_table_name=get_next_stream_table_name(
        get_next_stream_table_name(tables[0])
    )))
    with pytest.raises(AssertionError, match="Missing stream table"):
        check_new_partitions(tables, first_interval.beginning, next_interval.end)


def test_success(caplog):
    first_interval = LogInterval([
        Subinterval('c1', 't1', 0, 0, 10),
        Subinterval('c1', 't1', 5, 10, 15),  # random unrelated partition
    ])
    next_interval = LogInterval([
        Subinterval('c1', 't1', 0, 10, 20),
        Subinterval('c1', 't1', 1, 15, 20),  # starts in second table
        Subinterval('c1', 't1', 5, 15, 20),
    ])
    tables = generate_tables([
        first_interval,
        next_interval,
    ])
    check_new_partitions(tables, first_interval.beginning, next_interval.end)
    assert "Partition's first offset != 0" in caplog.text
