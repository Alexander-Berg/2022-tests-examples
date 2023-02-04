from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
from maps.analyzer.toolkit.lib.quality.schema import (
    ETALON_TRAVEL_LENGTH, ETALON_TRAVEL_TIME,
    CHECKED_TRAVEL_LENGTH, CHECKED_TRAVEL_TIME,
    JAMS, LAG, MANOEUVRE_JAMS, AVERAGE_TRAVEL_TIME, JAMS_TIME,
    JAMS_COVERED, MANOEUVRE_COVERED, PREDICTION_TYPE,
)
from maps.analyzer.toolkit.lib.schema import ROUTE_CONFIDENCE
import maps.analyzer.toolkit.lib.quality.operations as ops


class TestStatsWithStopwatch(object):
    def test_stats_with_stopwatch_1(self, ytc):
        expected = "//stats_with_stopwatch/table1.out"
        result = ops.stats_with_stopwatch(ytc, "//stats_with_stopwatch/table1.in", [], calculate_jams=True)
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[
                CHECKED_TRAVEL_TIME.name, CHECKED_TRAVEL_LENGTH.name,
                ETALON_TRAVEL_TIME.name, ETALON_TRAVEL_LENGTH.name,
                AVERAGE_TRAVEL_TIME.name, JAMS_TIME.name,
                JAMS_COVERED.name, MANOEUVRE_COVERED.name,
            ],
            ignored_columns=[
                PREDICTION_TYPE.name, ROUTE_CONFIDENCE.name, LAG.name
            ],
            precision=4
        )

    def test_stats_with_stopwatch_2(self, ytc):
        expected = "//stats_with_stopwatch/table2.out"
        result = ops.stats_with_stopwatch(ytc, "//stats_with_stopwatch/table2.in", [], calculate_jams=True)
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[
                CHECKED_TRAVEL_TIME.name, CHECKED_TRAVEL_LENGTH.name,
                ETALON_TRAVEL_TIME.name, ETALON_TRAVEL_LENGTH.name,
                AVERAGE_TRAVEL_TIME.name, JAMS_TIME.name,
                JAMS_COVERED.name, MANOEUVRE_COVERED.name,
            ],
            ignored_columns=[
                PREDICTION_TYPE.name, ROUTE_CONFIDENCE.name, LAG.name
            ],
            precision=4
        )

    def test_stats_with_stopwatch_3(self, ytc):
        expected = "//stats_with_stopwatch/table3.out"
        result = ops.stats_with_stopwatch(ytc, "//stats_with_stopwatch/table3.in", [], calculate_jams=True)
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[
                CHECKED_TRAVEL_TIME.name, CHECKED_TRAVEL_LENGTH.name,
                ETALON_TRAVEL_TIME.name, ETALON_TRAVEL_LENGTH.name,
                AVERAGE_TRAVEL_TIME.name, JAMS_TIME.name,
                JAMS_COVERED.name, MANOEUVRE_COVERED.name,
            ],
            ignored_columns=[
                PREDICTION_TYPE.name, ROUTE_CONFIDENCE.name, LAG.name
            ],
            precision=4
        )

    def test_stats_with_stopwatch_4(self, ytc):
        expected = "//stats_with_stopwatch/table3.out"
        result = ops.stats_with_stopwatch(
            ytc,
            "//stats_with_stopwatch/table3.in",
            [],
            calculate_jams=True,
            threads=5,
            op_spec={'reducer': {'cpu_limit': 1}},  # test cluster have no CPUs
        )
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[
                CHECKED_TRAVEL_TIME.name, CHECKED_TRAVEL_LENGTH.name,
                ETALON_TRAVEL_TIME.name, ETALON_TRAVEL_LENGTH.name,
                AVERAGE_TRAVEL_TIME.name, JAMS_TIME.name,
                JAMS_COVERED.name, MANOEUVRE_COVERED.name,
            ],
            ignored_columns=[
                PREDICTION_TYPE.name, ROUTE_CONFIDENCE.name, LAG.name
            ],
            precision=4
        )

    def test_stats_with_stopwatch_manoeuvres(self, ytc):
        expected = "//stats_with_stopwatch/table.manoeuvres.out"
        result = ops.stats_with_stopwatch(ytc, "//stats_with_stopwatch/table3.in", [], calculate_manoeuvre_jams=True)
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[
                CHECKED_TRAVEL_TIME.name, CHECKED_TRAVEL_LENGTH.name,
                ETALON_TRAVEL_TIME.name, ETALON_TRAVEL_LENGTH.name,
                AVERAGE_TRAVEL_TIME.name, JAMS_TIME.name,
                JAMS_COVERED.name, MANOEUVRE_COVERED.name,
            ],
            ignored_columns=[
                PREDICTION_TYPE.name, ROUTE_CONFIDENCE.name, LAG.name
            ],
            precision=4
        )

    def test_stats_with_stopwatch_preserve_user_column(self, ytc):
        expected = "//stats_with_stopwatch/table.user.column.out"
        result = ops.stats_with_stopwatch(ytc, "//stats_with_stopwatch/table.user.column.in", [], calculate_jams=True, group_by=['user_column'])
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[
                CHECKED_TRAVEL_TIME.name, CHECKED_TRAVEL_LENGTH.name,
                ETALON_TRAVEL_TIME.name, ETALON_TRAVEL_LENGTH.name,
                AVERAGE_TRAVEL_TIME.name, JAMS_TIME.name,
                JAMS_COVERED.name, MANOEUVRE_COVERED.name,
            ],
            ignored_columns=[
                PREDICTION_TYPE.name, ROUTE_CONFIDENCE.name, LAG.name
            ],
            precision=4
        )


class TestJoinTravelTimes(object):
    def test_join_travel_times_1(self, ytc):
        expected = "//join_travel_times/table1.out"
        result = ops.join_travel_times(
            ytc,
            etalon="//join_travel_times/table1.etalon.in",
            jams="//join_travel_times/table1.checked.in",
            expire_age=30
        )
        assert_equal_tables(
            ytc, expected, result, float_columns=[ETALON_TRAVEL_TIME.name, JAMS.name], unordered=True
        )

    def test_join_travel_times_2(self, ytc):
        expected = "//join_travel_times/table2.out"
        result = ops.join_travel_times(
            ytc,
            etalon="//join_travel_times/table2.etalon.in",
            jams="//join_travel_times/table2.checked.in",
            expire_age=10
        )
        assert_equal_tables(
            ytc, expected, result, float_columns=[ETALON_TRAVEL_TIME.name, JAMS.name],
            unordered=True,
        )

    def test_join_travel_times_3(self, ytc):
        expected = "//join_travel_times/table3.out"
        result = ops.join_travel_times(
            ytc,
            etalon="//join_travel_times/table3.etalon.in",
            jams="//join_travel_times/table3.jams.in",
            dead_jams="//join_travel_times/table3.dead_jams.in",
            expire_age=60,
        )

        assert_equal_tables(
            ytc, expected, result, float_columns=[ETALON_TRAVEL_TIME.name, JAMS.name],
            unordered=True,
        )

    def test_join_travel_times_4(self, ytc):
        expected = "//join_travel_times/table4.out"
        result = ops.join_travel_times(
            ytc,
            etalon="//join_travel_times/table4.etalon.in",
            jams="//join_travel_times/table4.jams.in",
            manoeuvre_jams="//join_travel_times/table4.manoeuvre_jams.in",
            expire_age=60,
        )
        assert_equal_tables(
            ytc, expected, result, float_columns=[ETALON_TRAVEL_TIME.name, JAMS.name, MANOEUVRE_JAMS.name],
            unordered=True,
        )

    def test_join_travel_times_bad_etalon(self, ytc):
        try:
            ops.join_travel_times(
                ytc,
                etalon="//join_travel_times/table3.etalon.in",
                jams="//join_travel_times/table4.jams.in",
                manoeuvre_jams="//join_travel_times/table4.manoeuvre_jams.in",
                expire_age=60,
            )
        except AssertionError:
            pass
        else:
            raise AssertionError('Should fail because etalon has no manoeuvre_id column')

    def test_join_travel_times_bad_manoeuvres(self, ytc):
        try:
            ops.join_travel_times(
                ytc,
                etalon="//join_travel_times/table4.etalon.in",
                jams="//join_travel_times/table4.jams.in",
                manoeuvre_jams="//join_travel_times/table4.jams.in",
                expire_age=60,
            )
        except AssertionError:
            pass
        else:
            raise AssertionError('Should fail because manoeuvre_jams has no manoeuvre_id column')

    def test_rejoin_travel_times(self, ytc):
        expected = "//join_travel_times/rejoin.out"
        expected_clear = "//join_travel_times/rejoin.clear.out"

        intermediate = ops.join_travel_times(
            ytc,
            etalon="//join_travel_times/rejoin.etalon.in",
            jams="//join_travel_times/rejoin.jams.1.in",
            expire_age=30,
        )

        rejoined = ops.join_travel_times(
            ytc,
            etalon=intermediate,
            jams="//join_travel_times/rejoin.jams.2.in",
            expire_age=30,
        )
        assert_equal_tables(
            ytc, expected, rejoined, float_columns=[ETALON_TRAVEL_TIME.name, JAMS.name],
            unordered=True,
        )

        rejoined_clear = ops.join_travel_times(
            ytc,
            etalon=intermediate,
            jams="//join_travel_times/rejoin.jams.2.in",
            expire_age=30,
            clear_existing=True,
        )
        assert_equal_tables(
            ytc, expected_clear, rejoined_clear, float_columns=[ETALON_TRAVEL_TIME.name, JAMS.name],
            unordered=True,
        )

    def test_join_travel_times_join_by(self, ytc):
        expected = "//join_travel_times/join_by.out"
        result = ops.join_travel_times(
            ytc,
            etalon="//join_travel_times/join_by.etalon.in",
            jams="//join_travel_times/join_by.jams.in",
            expire_age=30,
            join_by=["jams_tag"],
        )
        assert_equal_tables(
            ytc, expected, result, float_columns=[ETALON_TRAVEL_TIME.name, JAMS.name],
            unordered=True,
        )


class TestJoinTravelTimesWithFutureJams(object):
    def test_join_travel_times_with_future_jams_1(self, ytc):
        expected = "//join_travel_times_with_future_jams/table1.out"
        result = ops.join_travel_times_with_future_jams(
            ytc,
            etalon="//join_travel_times_with_future_jams/assessors1.in",
            jams="//join_travel_times_with_future_jams/jams1.in",
            expire_age=30
        )
        assert_equal_tables(
            ytc, expected, result, float_columns=[ETALON_TRAVEL_TIME.name, JAMS.name], unordered=True
        )


def test_leave_longest_1(ytc):
    expected = "//leave_longest/table1.out"
    result = ops.leave_longest(ytc, "//leave_longest/table1.in")
    assert_equal_tables(ytc, expected, result)
