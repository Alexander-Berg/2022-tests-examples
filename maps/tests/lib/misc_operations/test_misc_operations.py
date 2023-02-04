import maps.analyzer.pylibs.schema as s
import maps.analyzer.toolkit.lib.misc_operations as ops
from maps.analyzer.toolkit.lib.schema import LON, LAT, SEGMENT_DATA_COLUMNS, ROUTE_LENGTH, ROUTE_TRAVEL_TIME

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables


def test_append_region(ytc):
    expected = "//misc_operations/append_region/table.out"
    result = ops.append_region(
        ytc,
        "//misc_operations/append_region/table.in",
    )
    assert_equal_tables(ytc, expected, result, float_columns=[LAT.name, LON.name])


def test_diff_tables(ytc):
    expected = "//diff_tables/table1.out"
    result = ops.diff_tables(
        ytc,
        "//diff_tables/table1.first.in",
        "//diff_tables/table1.second.in",
        ["column1", "column2"],
        ["value"]
    )
    assert_equal_tables(ytc, expected, result)


class TestCaseDecodeRouteGeometry(object):
    def test_decode_route_geometry_1(self, ytc):
        expected = "//misc_operations/decode_route_geometry/table1.out"
        result = ops.decode_route_geometry(ytc, "//misc_operations/decode_route_geometry/table1.in")
        assert_equal_tables(ytc, expected, result)

    def test_decode_route_geometry_2(self, ytc):
        expected = "//misc_operations/decode_route_geometry/table2.out"
        result = ops.decode_route_geometry(ytc, "//misc_operations/decode_route_geometry/table2.in")
        assert_equal_tables(ytc, expected, result, float_columns=[ROUTE_TRAVEL_TIME.name, ROUTE_LENGTH.name])

    def test_decode_route_geometry_3(self, ytc):
        expected = "//misc_operations/decode_route_geometry/table3.out"
        result = ops.decode_route_geometry(ytc, "//misc_operations/decode_route_geometry/table3.in")
        assert_equal_tables(ytc, expected, result, float_columns=[ROUTE_TRAVEL_TIME.name, ROUTE_LENGTH.name])

    def test_decode_route_geometry_selected_driving_arrival_points(self, ytc):
        expected = "//misc_operations/decode_route_geometry/selected_driving_arrival_points.out"
        result = ops.decode_route_geometry(ytc, "//misc_operations/decode_route_geometry/selected_driving_arrival_points.in")
        assert_equal_tables(ytc, expected, result, float_columns=[ROUTE_TRAVEL_TIME.name, ROUTE_LENGTH.name])

    def test_decode_route_geometry_many_tables(self, ytc):
        expected_list = [
            "//misc_operations/decode_route_geometry/table2.out", "//misc_operations/decode_route_geometry/table3.out"
        ]
        result_list = ops.decode_route_geometry(
            ytc, ["//misc_operations/decode_route_geometry/table2.in", "//misc_operations/decode_route_geometry/table3.in"]
        )
        for expected, result in zip(expected_list, result_list):
            assert_equal_tables(ytc, expected, result, float_columns=[ROUTE_TRAVEL_TIME.name, ROUTE_LENGTH.name])


class TestSampleByUuid(object):
    def test_sample_by_uuid_1(self, ytc):
        expected = '//misc_operations/sample_rows_by_uuid/table_1.out'
        result = ops.sample_rows_by_uuid(
            ytc,
            '//misc_operations/sample_rows_by_uuid/table.in',
            1,
            "UUID"
        )
        assert_equal_tables(
            ytc, expected, result,
            # TODO: Use `get_table_float_column_names`
            float_columns=[col.name for col in SEGMENT_DATA_COLUMNS if col.type.unwrapped == s.Double],
        )

    def test_sample_by_uuid_05(self, ytc):
        expected = '//misc_operations/sample_rows_by_uuid/table_05.out'
        result = ops.sample_rows_by_uuid(
            ytc,
            '//misc_operations/sample_rows_by_uuid/table.in',
            0.5,
            "UUID"
        )
        assert_equal_tables(
            ytc, expected, result,
            # TODO: Use `get_table_float_column_names`
            float_columns=[col.name for col in SEGMENT_DATA_COLUMNS if col.type.unwrapped == s.Double],
        )
