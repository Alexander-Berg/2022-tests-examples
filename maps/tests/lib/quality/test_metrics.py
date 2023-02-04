from maps.pylibs.yt.lib.unwrap_yt_error import unwrap_yt_error

from maps.analyzer.toolkit.lib.quality.metrics import calculate_metrics_yql
from maps.analyzer.pylibs.test_tools import assert_equal_tables, dump_table
from maps.analyzer.toolkit.lib.quality.schema import (
    ETALON_TRAVEL_TIME, CHECKED_TRAVEL_TIME, RELATIVE_ERROR_TDIGEST,
)


def test_calculate_metrics_yql_1(ytc):
    with unwrap_yt_error():
        group_by = ['region']
        expected = "//calculate_metrics/table1.out"
        result = calculate_metrics_yql(
            ytc, "//calculate_metrics/table1.in", group_by, 0, 1.0,
            ETALON_TRAVEL_TIME.name, CHECKED_TRAVEL_TIME.name
        )
        columns = set(dump_table(ytc, expected)[0].keys()) - set(["region"])
        assert_equal_tables(
            ytc, expected, result,
            float_columns=list(columns),
            ignored_columns=group_by + [RELATIVE_ERROR_TDIGEST.name],
            bytestrings=True,
        )


def test_calculate_metrics_yql_2(ytc):
    with unwrap_yt_error():
        group_by = ['region']
        expected = "//calculate_metrics/table2.out"
        result = calculate_metrics_yql(
            ytc, "//calculate_metrics/table2.in", group_by, 0, 1.0,
            ETALON_TRAVEL_TIME.name, CHECKED_TRAVEL_TIME.name
        )
        columns = set(dump_table(ytc, expected)[0].keys()) - set(["region"])
        assert_equal_tables(
            ytc, expected, result,
            float_columns=list(columns),
            ignored_columns=group_by + [RELATIVE_ERROR_TDIGEST.name],
            precision=5,
            unordered=True,
            bytestrings=True,
        )
