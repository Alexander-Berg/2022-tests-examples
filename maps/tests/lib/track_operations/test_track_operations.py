import maps.analyzer.toolkit.lib.track_operations as tops
import maps.analyzer.toolkit.lib.quality.schema as quality_schema
from maps.analyzer.toolkit.lib.schema import TRAVEL_TIME

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables


def test_assemble_track_parts_1(ytc):
    expected = "//assemble_track_parts/table1.out"
    result = tops.assemble_track_parts(
        ytc,
        travel_times="//assemble_track_parts/table1.in"
    )
    assert_equal_tables(ytc, expected, result, float_columns=[TRAVEL_TIME.name])


def test_assemble_track_parts_2(ytc):
    expected = "//assemble_track_parts/table2.out"
    result = tops.assemble_track_parts(
        ytc,
        travel_times="//assemble_track_parts/table2.in",
        min_length=20000
    )
    assert_equal_tables(ytc, expected, result, float_columns=[TRAVEL_TIME.name])


def test_assemble_track_parts_3(ytc):
    expected = "//assemble_track_parts/table3.out"
    result = tops.assemble_track_parts(
        ytc,
        travel_times="//assemble_track_parts/table3.in",
        gap_tolerance=0.001
    )
    assert_equal_tables(ytc, expected, result, float_columns=[TRAVEL_TIME.name])


def test_crop_track_1(ytc):
    expected = "//crop_track/table1.out"
    result = tops.crop_track(
        ytc,
        travel_times="//crop_track/table1.in",
        length=20000
    )
    assert_equal_tables(ytc, expected, result, float_columns=[TRAVEL_TIME.name])


def test_cut_track_1(ytc):
    expected = "//cut_track/table1.out"
    result = tops.cut_track(
        ytc,
        "//cut_track/table1.in",
        cut_length=20000,
        min_length=19000
    )
    assert_equal_tables(ytc, expected, result, float_columns=[TRAVEL_TIME.name])


def test_cut_track_2(ytc):
    expected = "//cut_track/table2.out"
    result = tops.cut_track(
        ytc,
        "//cut_track/table2.in",
        cut_length=20000,
        min_length=19000
    )
    assert_equal_tables(ytc, expected, result, float_columns=[TRAVEL_TIME.name])


def test_filter_tracks_empty(ytc):
    expected = "//filter_tracks/table.in"
    result = tops.filter_tracks(
        ytc,
        "//filter_tracks/table.in",
    )
    assert_equal_tables(ytc, expected, result)


def test_filter_tracks_length(ytc):
    expected = "//filter_tracks/table1.out"
    result = tops.filter_tracks(
        ytc,
        "//filter_tracks/table.in",
        min_total_length=50,
        max_total_length=100,
    )
    assert_equal_tables(ytc, expected, result)


def test_filter_tracks_avg_speed(ytc):
    expected = "//filter_tracks/table2.out"
    result = tops.filter_tracks(
        ytc,
        "//filter_tracks/table.in",
        min_average_speed=3,
        max_average_speed=9,
    )
    assert_equal_tables(ytc, expected, result)


def test_filter_tracks_route_confidence(ytc):
    expected = "//filter_tracks/table3.out"
    result = tops.filter_tracks(
        ytc,
        "//filter_tracks/table.in",
        min_route_confidence=0.9,
    )
    assert_equal_tables(ytc, expected, result)


def test_filter_tracks_multiple(ytc):
    expected = ytc.create_temp_table()
    result = tops.filter_tracks(
        ytc,
        "//filter_tracks/table.in",
        min_total_length=50,
        max_total_length=100,
        min_average_speed=7,
        max_average_speed=10,
        min_route_confidence=0.9,
    )
    assert_equal_tables(ytc, expected, result)


class TestSplitTracksUsingBlacklist(object):
    def test_split_tracks_using_blacklist_1(self, ytc):
        expected = "//split_tracks_using_blacklist/table1.out"
        result = tops.split_tracks_using_blacklist(
            ytc,
            training_tracks="//split_tracks_using_blacklist/table1.tracks.in",
            blacklist="//split_tracks_using_blacklist/table1.blacklist.in"
        )
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[quality_schema.ETALON_TRAVEL_TIME.name, quality_schema.JAMS.name], unordered=True
        )


class TestFindSlowEdgesBlacklist(object):
    def test_find_slow_edges_blacklist_1(self, ytc):
        expected = "//find_slow_edges/table1.out"
        result = tops.find_slow_edges_blacklist(
            ytc,
            "//find_slow_edges/table1.in",
            drop_k=10, window=600, min_bound=200, single_penalty=2,
        )
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[quality_schema.ETALON_TRAVEL_TIME.name, quality_schema.JAMS.name], unordered=True
        )
