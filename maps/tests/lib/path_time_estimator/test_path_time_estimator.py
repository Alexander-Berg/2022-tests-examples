import datetime

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
from maps.pylibs.yt.lib import unwrap_yt_error
import maps.analyzer.toolkit.lib.path_time_estimator as path_estimator
import maps.analyzer.toolkit.lib.schema as schema


def replace_index_with_track_key(ytc, t, index_to_track_start_time):
    def mapper(r):
        i = r.pop(path_estimator.INDEX)
        r[schema.CLID.name] = 'clid_{}'.format(i)
        r[schema.UUID.name] = 'uuid_{}'.format(i)
        r[schema.TRACK_START_TIME.name] = index_to_track_start_time[i]
        yield r

    result = ytc.create_temp_table()
    ytc.run_map(mapper, t, result)
    return result


def test_jams_builder(ytc):
    """
    Test of building reversed jams
    """
    expected = "//path_time_estimator/jams_builder1.out"
    with unwrap_yt_error():
        result = path_estimator.build_reversed_jams(
            ytc,
            travel_times="//path_time_estimator/jams_builder1.in",
            build_jams_kwargs={
                'max_category': None,
            },
            interpolation_config=path_estimator.IntepolationConfig(
                window=300, horizon=300, window_by=schema.LEAVE_TIME.name
            ),
            engine_config=path_estimator.DEFAULT_ENGINE_CONFIG.patch(
                common_ratio=0.9,
                max_signal_count_for_calculation_with_model=0,
                model_path=None,
            )
        )
    assert_equal_tables(ytc, expected, result, float_columns=[schema.TRAVEL_TIME.name])


def test_path_estimator(ytc):
    """
    test of path time estimating algorithm
    """
    expected = "//path_time_estimator/calculate_time_by_paths1.out"
    with unwrap_yt_error():
        result = path_estimator.calculate_time_by_paths(
            ytc,
            begin_time=None,
            end_time=None,
            routes="//path_time_estimator/routes.in",
            jams="//path_time_estimator/jams.in"
        )
    assert_equal_tables(ytc, expected, result, float_columns=[schema.JAMS_COVERAGE.name, schema.TOTAL_TIME_ESTIMATED.name])


def test_add_future_jams_to_routes(ytc):
    expected = "//path_time_estimator/add_future_jams_to_routes1.out"
    with unwrap_yt_error():
        flattened_routes = path_estimator.flatten_routes(
            ytc,
            "//path_time_estimator/routes.in",
            key_columns=(path_estimator.INDEX, schema.TRACK_START_TIME.name)
        )

        result = path_estimator.add_future_jams_to_routes(
            ytc,
            flattened_routes=flattened_routes,
            jams="//path_time_estimator/jams.in",
            max_jams_age=datetime.timedelta(seconds=61), max_route_duration=datetime.timedelta(hours=3)
        )
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_path_estimator_track_key(ytc):
    """
    test of path time estimating algorithm using (clid, uuid, track_start_time) as track key
    """
    src_routes = "//path_time_estimator/routes.in"
    index_to_track_start_time = {
        r[path_estimator.INDEX] : r[schema.TRACK_START_TIME.name]
        for r in ytc.read_table(src_routes)
    }
    expected = replace_index_with_track_key(
        ytc, "//path_time_estimator/calculate_time_by_paths1.out", index_to_track_start_time
    )
    with unwrap_yt_error():
        result = path_estimator.calculate_time_by_paths(
            ytc,
            begin_time=None,
            end_time=None,
            routes=replace_index_with_track_key(ytc, src_routes, index_to_track_start_time),
            jams="//path_time_estimator/jams.in",
            key_columns=(schema.CLID.name, schema.UUID.name, schema.TRACK_START_TIME.name),
        )
    assert_equal_tables(ytc, expected, result, float_columns=[schema.JAMS_COVERAGE.name, schema.TOTAL_TIME_ESTIMATED.name])
