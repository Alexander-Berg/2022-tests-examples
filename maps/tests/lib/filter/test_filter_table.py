from yatest.common import source_path

from maps.analyzer.toolkit.lib.filter_table import filter_table
import maps.analyzer.pylibs.schema as schema
from maps.analyzer.toolkit.lib.schema import REGION_ID, CATEGORY

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables


def test_filter_table_1(ytc):
    expected = "//filter_table/table1.out"
    result = filter_table(
        ytc, "//filter_table/table1.in",
        vehicle_ids=["clid0 uuid0", "clid1 uuid1"],
        custom_files=[
            source_path("maps/analyzer/toolkit/tests/data/filter_table/custom_file1.txt"),
            source_path("maps/analyzer/toolkit/tests/data/filter_table/custom_file2.txt")
        ],
        begin_time="20160101T000000", end_time="20161231T235959",
        times_of_day=["00:00-00:10"],
        bbox="37.646608 55.778845 37.661285 55.771903",
        segment_ids=["0 0", "1 1"],
        weekdays=["fri"],
        regions=[213],
    )
    assert_equal_tables(ytc, expected, result)


def test_filter_table_2(ytc):
    # Can we apply schema within cypress directory in .meta file?
    # Setting 'schema' attribute doesn't work
    TABLE = schema.table([REGION_ID, CATEGORY], None)
    ytc.run_merge(
        "//filter_table/table2.in",
        ytc.TablePath("//filter_table/table2.in", schema=TABLE.schema),
    )

    expected = "//filter_table/table2.out"
    result = filter_table(
        ytc, "//filter_table/table2.in",
        regions=[213],
        categories=[1, 2],
    )
    assert_equal_tables(ytc, expected, result)
