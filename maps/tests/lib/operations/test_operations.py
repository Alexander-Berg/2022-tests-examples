from datetime import date
import pytest

from maps.analyzer.pylibs.schema.operations import schematize
from maps.analyzer.pylibs.schema import get_table, table
import maps.analyzer.pylibs.schema as s
import maps.analyzer.toolkit.lib.operations as ops
import maps.analyzer.toolkit.lib.schema as schema
import maps.analyzer.toolkit.lib.sources as sources

from maps.analyzer.pylibs.test_tools import schematize_table
from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables


class TestConvertStatInfos(object):
    def test_convert_stat_infos_1(self, ytc):
        expected = "//convert_stat_infos/table1.out"
        result = ops.convert_stat_infos(
            ytc,
            "//convert_stat_infos/table1.in",
            output_encoded=False,
            good_only=False
        )
        assert_equal_tables(ytc, expected, result, float_columns=['lon', 'lat', 'speed', 'length_to_next'])

    def test_convert_stat_infos_2(self, ytc):
        expected = "//convert_stat_infos/table2.out"
        result = ops.convert_stat_infos(
            ytc,
            "//convert_stat_infos/table2.in",
            output_encoded=True,
            good_only=True
        )
        assert_equal_tables(
            ytc, expected, result, float_columns=['lon', 'lat', 'speed', 'length_to_next'])


def test_find_consecutive_edges(ytc):
    with pytest.raises(Exception):
        ops.find_consecutive_edges(
            ytc,
            '//find_consecutive_edges/table1.in',
            edges_number=3
        )

    for n in range(2, 4):
        expected = '//find_consecutive_edges/table{0}.out'.format(n)
        result = ops.find_consecutive_edges(
            ytc,
            '//find_consecutive_edges/table{0}.in'.format(n),
            edges_number=3
        )
        assert_equal_tables(ytc, expected, result)


def test_filter_assessors_signals(ytc):
    expected = "//filter_assessors_signals/expected.out"

    signals = "//filter_assessors_signals/signals.in"
    schematize_table(ytc, signals)
    result = ops.filter_assessors_signals(ytc, signals)

    assert_equal_tables(ytc, expected, result, null_equals_unexistant=True, unordered=True)


def test_append_route_confidence(ytc):
    expected = "//append_route_confidence/result.out"

    route_tracks = "//append_route_confidence/route_tracks.in"
    route_stats = "//append_route_confidence/route_stats.in"

    schematize_table(ytc, route_tracks)
    schematize_table(ytc, route_stats)

    result = ops.append_route_confidence(ytc, route_tracks, route_stats)
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_edge_jams(ytc):
    expected = "//edge_jams/table1.out"
    jams = "//edge_jams/table1.in"
    schematize_table(ytc, jams)
    schematize_table(ytc, expected)
    result = ops.edge_jams(ytc, jams)
    assert_equal_tables(ytc, expected, result, unordered=True, float_columns=["travel_time"], precision=4)


def test_edge_jams_extra_keys(ytc):
    TAG = s.column("tag", s.String, None)

    expected = "//edge_jams/table2.out"
    jams = "//edge_jams/table2.in"
    schematize_table(ytc, jams, hints=[TAG])
    schematize_table(ytc, expected, hints=[TAG])
    result = ops.edge_jams(ytc, jams, extra_keys=[TAG.name])
    assert_equal_tables(ytc, expected, result, unordered=True, float_columns=["travel_time"], precision=4)


class TestSplitByDate(object):
    _dates = [date(2020, 6, 1), date(2020, 6, 3)]
    _source = '//split_by_date/source/table'

    def _check(self, ytc, t, ordered, target_schema=None):
        results = ops.split_by_dates(ytc, t, self._dates, 'time', '%Y%m%dT%H%M%S')
        assert len(results) == len(self._dates), 'result size mismatch'
        for d, res in zip(self._dates, results):
            exp = sources.path_for_date('//split_by_date/expected', d)
            assert_equal_tables(ytc, exp, res, unordered=not ordered)
            assert target_schema is None or get_table(ytc, res).schema == target_schema.schema, \
                'bad result schema'

    def test_simple(self, ytc):
        self._check(ytc, self._source, ordered=False)

    def test_sorted(self, ytc):
        sorted_source = ytc.create_temp_table()
        ytc.run_sort(self._source, sorted_source, sort_by=['time'])
        self._check(ytc, sorted_source, ordered=True)

    def test_schematized(self, ytc):
        target_schema = table([
            schema.TIME,
            schema.SPEED,
            schema.LAT,
            schema.LON,
            schema.REGION_ID,
        ], 'test')
        schematized_source = schematize(ytc, self._source, target_schema)
        self._check(ytc, schematized_source, ordered=False, target_schema=target_schema)
