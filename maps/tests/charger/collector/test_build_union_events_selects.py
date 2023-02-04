from maps_adv.stat_tasks_starter.lib.charger.collector.query_builders import (
    build_union_select_events,
)
from maps_adv.stat_tasks_starter.tests.tools import squash_whitespaces

from .test_build_select_events_stat import format_test_select


def test_returns_expected_query_one():
    select_sql_1 = format_test_select([1, 2])
    select_sql_2 = format_test_select([3, 4])

    got = build_union_select_events([select_sql_1, select_sql_2])

    expected = f"{select_sql_1} UNION ALL {select_sql_2}"
    assert squash_whitespaces(got) == squash_whitespaces(expected)
