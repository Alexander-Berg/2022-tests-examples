from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
from maps.analyzer.toolkit.lib.schema import UUID, CLID, ROUTE_GEOMETRY
from maps.analyzer.toolkit.lib.quality.schema import (
    TIME_POINT, SESSION_COVERAGE, NEW_ROUTE_GEOMETRY, NEW_ROUTE_START_TIME, SESSION_START_TIME, SESSION_FINISH_TIME,
    GEODISTANCE_FROM_SESSION_START_POINT
)
from maps.pylibs.yt.lib import unwrap_yt_error
import maps.analyzer.toolkit.lib.quality.route_lost as route_lost


class TestCaseCalcRouteLost(object):
    def test_calc_route_lost_1(self, ytc):
        # Two separate situations:
        # 1. passed full route
        # 2. route lost, then reached the finish
        # 3. got reroute while track is absent
        expected = "//route_lost/calc_route_lost1.out"
        source = "//route_lost/calc_route_lost1.in"
        ytc.run_sort(source, sort_by=self.sort_by)
        with unwrap_yt_error():
            result, _, _ = route_lost.calc_route_lost(
                ytc, source, segments_tolerance_count=0, finish_tolerance=100
            )
        assert_equal_tables(ytc, expected, result, float_columns=[SESSION_COVERAGE.name, GEODISTANCE_FROM_SESSION_START_POINT.name])

    def test_calc_route_lost_2(self, ytc):
        # uuid1: Two session has been splitted because of reaching the end
        # uuid2: Session has been splitted because of changing finish point
        expected = "//route_lost/calc_route_lost2.out"
        source = "//route_lost/calc_route_lost2.in"
        ytc.run_sort(source, sort_by=self.sort_by)
        with unwrap_yt_error():
            result, _, _ = route_lost.calc_route_lost(
                ytc, source, segments_tolerance_count=0, finish_tolerance=100
            )
        assert_equal_tables(ytc, expected, result, float_columns=[SESSION_COVERAGE.name], ignored_columns=[ROUTE_GEOMETRY.name, GEODISTANCE_FROM_SESSION_START_POINT.name], unordered=True)

    def test_calc_route_lost_3(self, ytc):
        # real track, you can draw it with graph (17.09.17-1)
        expected = "//route_lost/calc_route_lost3.out"
        source = "//route_lost/calc_route_lost3.in"
        ytc.run_sort(source, sort_by=self.sort_by)
        with unwrap_yt_error():
            result, _, _ = route_lost.calc_route_lost(
                ytc, source, segments_tolerance_count=10, finish_tolerance=100
            )
        assert_equal_tables(
            ytc, expected, result, float_columns=[SESSION_COVERAGE.name], ignored_columns=[NEW_ROUTE_GEOMETRY.name, GEODISTANCE_FROM_SESSION_START_POINT.name]
        )

    def test_calc_route_lost_4(self, ytc):
        expected_routes = "//route_lost/calc_route_lost_routes4.out"
        expected_tracks = "//route_lost/calc_route_lost_tracks4.out"
        source = "//route_lost/calc_route_lost4.in"
        ytc.run_sort(source, sort_by=self.sort_by)
        with unwrap_yt_error():
            result, routes, tracks = route_lost.calc_route_lost(
                ytc, source, segments_tolerance_count=10, finish_tolerance=100
            )
        assert_equal_tables(ytc, expected_routes, routes)
        assert_equal_tables(ytc, expected_tracks, tracks)

    def test_calc_route_lost_at_finish(self, ytc):
        # uuid1: One route lost a finish
        # uuid2: Two route lost near finish, only one should be accounted
        expected = "//route_lost/calc_route_lost_at_finish.out"
        source = "//route_lost/calc_route_lost_at_finish.in"
        ytc.run_sort(source, sort_by=self.sort_by)
        with unwrap_yt_error():
            result, _, _ = route_lost.calc_route_lost(
                ytc, source, segments_tolerance_count=0, finish_tolerance=100
            )
            ytc.run_sort(
                expected, sort_by=[
                    CLID.name, UUID.name, SESSION_START_TIME.name,
                    SESSION_FINISH_TIME.name, SESSION_COVERAGE.name, NEW_ROUTE_START_TIME.name
                ]
            )
        assert_equal_tables(
            ytc, expected, result, float_columns=[SESSION_COVERAGE.name],
            ignored_columns=[ROUTE_GEOMETRY.name, NEW_ROUTE_GEOMETRY.name, GEODISTANCE_FROM_SESSION_START_POINT.name]
        )

    def test_calc_route_lost_to_carpark_alternative(self, ytc):
        # uuid1: Near finish, move to a carpark alternative
        # (that has different route and route finish point, but same target)
        expected = "//route_lost/calc_route_lost_to_carpark_alternative.out"
        source = "//route_lost/calc_route_lost_to_carpark_alternative.in"
        ytc.run_sort(source, sort_by=self.sort_by)
        with unwrap_yt_error():
            result, _, _ = route_lost.calc_route_lost(
                ytc, source, segments_tolerance_count=0, finish_tolerance=100
            )
        assert_equal_tables(
            ytc, expected, result, float_columns=[SESSION_COVERAGE.name],
            ignored_columns=[ROUTE_GEOMETRY.name, NEW_ROUTE_GEOMETRY.name, GEODISTANCE_FROM_SESSION_START_POINT.name]
        )

    sort_by = [CLID.name, UUID.name, TIME_POINT.name, 'row_index']


def test_get_route_proto(ytc):
    expected = "//route_lost/get_route_proto.out"
    with unwrap_yt_error():
        result = route_lost.get_route_proto(
            ytc,
            "//route_lost/get_route_proto.in",
            route_lost.Types.ALL,
        )
    assert_equal_tables(ytc, expected, result)


def test_get_route_proto_type_route(ytc):
    expected = "//route_lost/get_route_proto_type_route.out"
    with unwrap_yt_error():
        result = route_lost.get_route_proto(
            ytc,
            "//route_lost/get_route_proto.in",
            route_lost.Types.ROUTE,
        )
    assert_equal_tables(ytc, expected, result)


def test_get_route_proto_type_parking(ytc):
    expected = "//route_lost/get_route_proto_type_parking.out"
    with unwrap_yt_error():
        result = route_lost.get_route_proto(
            ytc,
            "//route_lost/get_route_proto.in",
            route_lost.Types.PARKING,
        )
    assert_equal_tables(ytc, expected, result)


def test_join_events_and_routes(ytc):
    expected = "//route_lost/join_events_and_routes.out"
    with unwrap_yt_error():
        result = route_lost.join_events_and_routes(
            ytc,
            "//route_lost/join_events_table.in",
            "//route_lost/join_routes_table.in",
        )
    assert_equal_tables(ytc, expected, result)
