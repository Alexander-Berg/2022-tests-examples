
import pytest

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval, Subinterval, find_covering_tables_for_interval,
    get_next_stream_table_name
)

from billing.library.python.logfeller_utils.tests.utils import generate_tables


def test_basic():
    target_interval = LogInterval([
        Subinterval('c1', 't1', 0, 57, 68),
        Subinterval('c1', 't1', 1, 65, 73),
    ]).to_meta()

    tables = generate_tables([
        # Tables before intersection may contain inconsistent metadata.
        LogInterval([
            # not successive with next table
            Subinterval('c1', 't1', 0, 10, 15),
            # this partition is gone after this table
            Subinterval('c6', 't1', 1, 15, 20),
        ]),

        LogInterval([
            Subinterval('c1', 't1', 0, 50, 55),
            Subinterval('c1', 't1', 1, 60, 65),
        ]),
        LogInterval([
            Subinterval('c1', 't1', 0, 55, 60),
            Subinterval('c1', 't1', 1, 65, 70),
        ]),

        # Empty table inside intersection
        LogInterval([
            Subinterval('c1', 't1', 0, 60, 60),
            Subinterval('c1', 't1', 1, 70, 70),
        ]),

        LogInterval([
            Subinterval('c1', 't1', 0, 60, 68),
            Subinterval('c1', 't1', 1, 70, 75),
            Subinterval('c1', 't1', 2, 60, 65),
        ]),
        LogInterval([
            Subinterval('c1', 't1', 0, 68, 68),
            Subinterval('c1', 't1', 1, 75, 85),
            Subinterval('c1', 't1', 2, 65, 75),
        ]),
    ])

    assert find_covering_tables_for_interval(
        target_interval, tables
    ) == (2, 4)


def test_not_intersects():
    target_interval = LogInterval([
        Subinterval('c1', 't1', 0, 57, 68),
        Subinterval('c1', 't1', 1, 65, 73),
    ]).to_meta()

    tables = generate_tables([
        LogInterval([
            Subinterval('c1', 't1', 0, 45, 50),
            Subinterval('c1', 't1', 1, 55, 60),
        ]),
        LogInterval([
            Subinterval('c1', 't1', 0, 50, 55),
            Subinterval('c1', 't1', 1, 60, 65),
        ]),
    ])

    with pytest.raises(AssertionError, match='Target interval is not covered by intersecting tables'):
        find_covering_tables_for_interval(
            target_interval, tables
        )


def test_not_successive_intervals():
    target_interval = LogInterval([
        Subinterval('c1', 't1', 0, 47, 68),
        Subinterval('c1', 't1', 1, 58, 73),
    ]).to_meta()

    tables = generate_tables([
        LogInterval([
            Subinterval('c1', 't1', 0, 45, 50),
            Subinterval('c1', 't1', 1, 55, 60),
        ]),
        LogInterval([
            Subinterval('c1', 't1', 0, 50, 55),
            # Second sub-interval is missing
        ]),
    ])

    with pytest.raises(AssertionError, match='Not successive intervals'):
        find_covering_tables_for_interval(
            target_interval, tables
        )


def test_gap_in_tables():
    target_interval = LogInterval([
        Subinterval('c1', 't1', 0, 47, 53),
    ]).to_meta()

    tables = generate_tables([
        LogInterval([
            Subinterval('c1', 't1', 0, 45, 50),
        ]),
    ])

    tables.extend(generate_tables([
        LogInterval([
            Subinterval('c1', 't1', 0, 50, 55),
        ]),
    ], first_table_name=get_next_stream_table_name(
        get_next_stream_table_name(str(tables[0]))
    )))

    with pytest.raises(AssertionError, match='The table has unexpected name'):
        find_covering_tables_for_interval(
            target_interval, tables
        )


def test_gap_in_offsets(caplog):
    target_interval = LogInterval([
        Subinterval('c1', 't1', 0, 47, 53),
    ]).to_meta()

    tables = generate_tables([
        LogInterval([
            Subinterval('c1', 't1', 0, 45, 50),
        ]),
        LogInterval([
            Subinterval('c1', 't1', 0, 51, 55),  # here is the gap
        ]),
    ])

    find_covering_tables_for_interval(target_interval, tables)

    assert 'A gap in offsets in successive stream log tables' in caplog.text


def test_not_covered():
    target_interval = LogInterval([Subinterval('c1', 't1', 0, 37, 68)]).to_meta()

    tables = generate_tables([
        LogInterval([Subinterval('c1', 't1', 0, 45, 70)]),
    ])

    with pytest.raises(AssertionError, match='Target interval is not covered by intersecting tables'):
        find_covering_tables_for_interval(
            target_interval, tables
        )
