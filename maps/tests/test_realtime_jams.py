import maps.analyzer.pylibs.realtime_jams.lib.schema as schema
from maps.analyzer.pylibs.realtime_jams.lib.build_jams import (
    build_realtime_jams, build_realtime_jams_pool, IntepolationConfig, DEFAULT_ENGINE_CONFIG
)
from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
from yatest.common import source_path


DEFAULT_CONFIG_JAMS_PATH = source_path(
    'maps/analyzer/pylibs/realtime_jams/tests/data/test_engine_config_jams.json'
)
DEFAULT_CONFIG_JAMS = DEFAULT_ENGINE_CONFIG.load(DEFAULT_CONFIG_JAMS_PATH).patch(model_path=None)


class TestBuildRealtimeJams(object):
    def test_1(self, ytc):
        expected = "//build_realtime_jams/table1.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_realtime_jams/table1.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_CONFIG_JAMS
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])

    def test_2(self, ytc):
        expected = "//build_realtime_jams/table2.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_realtime_jams/table2.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_CONFIG_JAMS
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])

    def test_3(self, ytc):
        expected = "//build_realtime_jams/table3.out"
        conf = DEFAULT_CONFIG_JAMS.patch(common_ratio=1)
        result = build_realtime_jams(
            ytc,
            travel_times="//build_realtime_jams/table3.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            max_category=None,
            engine_config=conf
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name], unordered=True)

    def test_8(self, ytc):
        expected = "//build_realtime_jams/table8.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_realtime_jams/table8.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_CONFIG_JAMS
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])

    def test_10(self, ytc):
        expected = "//build_realtime_jams/table10.out"
        conf = DEFAULT_CONFIG_JAMS.patch(common_ratio=1, max_signal_count_for_calculation_with_model=6)
        result = build_realtime_jams(
            ytc,
            travel_times="//build_realtime_jams/table10.in",
            interpolation_config=IntepolationConfig(
                window=1200, horizon=1200, window_by=schema.TIME.name
            ),
            time_step=60,
            max_category=8,
            engine_config=conf
        )
        assert_equal_tables(ytc, expected, result, precision=3, float_columns=[schema.TRAVEL_TIME.name])

    # Check with only_popular=True
    def test_11(self, ytc):
        expected = "//build_realtime_jams/table11.out"
        conf = DEFAULT_CONFIG_JAMS.patch(common_ratio=1, max_signal_count_for_calculation_with_model=6)
        result = build_realtime_jams(
            ytc,
            travel_times="//build_realtime_jams/table11.in",
            interpolation_config=IntepolationConfig(
                window=1200, horizon=1200, window_by=schema.TIME.name
            ),
            time_step=60,
            calculate_only_for_popular=True,
            engine_config=conf
        )
        assert_equal_tables(ytc, expected, result, precision=3, float_columns=[schema.TRAVEL_TIME.name])

    # Check with different reduce_by option
    def test_12(self, ytc):
        expected = "//build_realtime_jams/table12.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_realtime_jams/table12.in",
            reduce_by=[schema.PERSISTENT_ID.name, schema.SEGMENT_INDEX.name, schema.NEXT_EDGES.name],
            interpolation_config=IntepolationConfig(
                window=600, horizon=600, weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            max_category=7,
            engine_config=DEFAULT_CONFIG_JAMS
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])

    def test_zero_horizon(self, ytc):
        expected = "//build_realtime_jams/zero.horizon.out"
        result = build_realtime_jams(
            ytc,
            travel_times="//build_realtime_jams/zero.horizon.in",
            interpolation_config=IntepolationConfig(
                window=600, horizon=0, weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_CONFIG_JAMS
        )
        assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])


DEFAULT_CONFIG_POOL_PATH = source_path(
    'maps/analyzer/pylibs/realtime_jams/tests/data/test_engine_config_pool.json'
)
DEFAULT_CONFIG_POOL = DEFAULT_ENGINE_CONFIG.load(DEFAULT_CONFIG_POOL_PATH).patch(model_path=None)


class TestBuildRealtimeJamsPool(object):
    def test_1(self, ytc):
        expected = "//build_realtime_jams_pool/table1.out"
        result = build_realtime_jams_pool(
            ytc,
            travel_times="//build_realtime_jams_pool/table1.in",
            max_category=8,
            interpolation_config=IntepolationConfig(
                weighting_order_by=schema.LEAVE_TIME.name, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=DEFAULT_CONFIG_POOL,
            sampling_rate=1
        )
        float_columns = list(filter(
            lambda x: not x.startswith('_'),
            next(ytc.read_table(result))
        )) + ['_target']
        assert_equal_tables(
            ytc, expected, result, float_columns=float_columns, precision=4
        )
