from maps.analyzer.pylibs.realtime_jams.lib.build_jams import (
    build_realtime_jams, append_edge_properties, filter_similar_manoeuvres, IntepolationConfig, DEFAULT_ENGINE_CONFIG
)
import maps.analyzer.pylibs.realtime_jams.lib.schema as schema

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables


class TestCaseBuildJams(object):
    def test_build_jams_1(self, ytc):
        expected = "//build_jams/table1.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table1.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=0.9,
                max_signal_count_for_calculation_with_model=9,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])

    def test_build_jams_2(self, ytc):
        expected = "//build_jams/table2.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table2.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=0.9,
                max_signal_count_for_calculation_with_model=9,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])

    def test_build_jams_3(self, ytc):
        expected = "//build_jams/table3.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table3.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            max_category=None,
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=1,
                max_signal_count_for_calculation_with_model=9,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name], unordered=True)

    def test_build_jams_4(self, ytc):
        expected = "//build_jams/table4.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table4.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, window_by=schema.MATCH_TIME.name
            ),
            time_step=60,
            max_category=8,
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=1,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, precision=3, float_columns=[schema.TRAVEL_TIME.name])

    def test_build_jams_5(self, ytc):
        expected = "//build_jams/table5.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table5.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, window_by=schema.MATCH_TIME.name
            ),
            time_step=60,
            max_category=8,
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=1,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, precision=3, float_columns=[schema.TRAVEL_TIME.name])

    def test_build_jams_8(self, ytc):
        expected = "//build_jams/table8.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table8.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=0.9,
                max_signal_count_for_calculation_with_model=9,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])

    def test_build_jams_10(self, ytc):
        expected = "//build_jams/table10.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table10.in",
            interpolation_config=IntepolationConfig(
                window=1200, horizon=1200, window_by=schema.TIME.name
            ),
            time_step=60,
            max_category=8,
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=1,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, precision=3, float_columns=[schema.TRAVEL_TIME.name])

    # Check with only_popular=True
    def test_build_jams_11(self, ytc):
        expected = "//build_jams/table11.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table11.in",
            interpolation_config=IntepolationConfig(
                window=1200, horizon=1200, window_by=schema.TIME.name
            ),
            time_step=60,
            calculate_only_for_popular=True,
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=1,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, precision=3, float_columns=[schema.TRAVEL_TIME.name])

    # Check with different reduce_by option
    def test_build_jams_12(self, ytc):
        expected = "//build_jams/table12.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table12.in",
            reduce_by=[schema.PERSISTENT_ID.name, schema.SEGMENT_INDEX.name, schema.NEXT_EDGES.name],
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            max_category=7,
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=0.9,
                max_signal_count_for_calculation_with_model=9,
                model_path=None,
            ),
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])

    # Check drop_slow option
    def test_build_jams_13(self, ytc):
        expected = "//build_jams/table13.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_jams/table13.in",
            interpolation_config=IntepolationConfig(
                window=3600, horizon=3600, weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=0.9,
                model_path=None,
                drop_slow={'drop_k': 20, 'use_k': 10},
            ),
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])


def test_filter_similar_1(ytc):
    expected = "//filter_similar/table1.out"
    result = filter_similar_manoeuvres(
        ytc,
        "//filter_similar/table1.in",
        min_difference=1.1
    )
    assert_equal_tables(ytc, expected, result)


def test_append_edge_properties_1(ytc):
    expected = "//append_edge_properties/table1.out"
    result = append_edge_properties(
        ytc,
        "//append_edge_properties/table1.in",
        min_length=0
    )
    assert_equal_tables(
        ytc, expected, result,
        float_columns=[schema.TRAVEL_TIME.name]
    )


def test_append_edge_properties_2(ytc):
    expected = "//append_edge_properties/table2.out"
    result = append_edge_properties(
        ytc,
        "//append_edge_properties/table2.in",
        min_length=100
    )
    assert_equal_tables(
        ytc, expected, result,
        float_columns=[schema.TRAVEL_TIME.name]
    )
