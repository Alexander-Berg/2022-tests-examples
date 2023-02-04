from dateutil import parser

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables

import maps.analyzer.pylibs.envkit as envkit

from maps.analyzer.pylibs.graphmatching.lib import match_signals
import maps.analyzer.pylibs.graphmatching.lib.schema as schema


TRACK_KEYS = [schema.CLID.name, schema.UUID.name, schema.VEHICLE_TYPE.name, schema.TRACK_START_TIME.name, schema.ENTER_TIME.name]


def test_match_signals(ytc, schematize_match_signals):
    expected = "//match_signals/travel_times.out"
    result = match_signals(
        ytc,
        "//match_signals/signals.in",
        append_segment_data=False,
        append_vehicle_type=False
    )
    assert_equal_tables(
        ytc, expected, result,
        float_columns=[schema.TRAVEL_TIME.name],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True,
        unordered=True,
        keys=TRACK_KEYS,
    )
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_signals_extra_column(ytc, schematize_match_signals):

    expected = "//match_signals/travel_times.out"
    result = match_signals(
        ytc,
        "//match_signals/signals.extra_column.in",
        append_segment_data=False,
        append_vehicle_type=False
    )
    assert_equal_tables(
        ytc, expected, result,
        float_columns=[schema.TRAVEL_TIME.name],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True,
    )
    result = match_signals(
        ytc,
        ["//match_signals/signals.extra_column.in"],
        append_segment_data=False,
        append_vehicle_type=False
    )
    assert_equal_tables(
        ytc, expected, result,
        float_columns=[schema.TRAVEL_TIME.name],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True,
    )
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_signals_one_date(ytc, schematize_match_signals):
    expecteds = ["//match_signals/travel_times.out"]
    results = match_signals(
        ytc,
        ["//match_signals/signals.in", "//match_signals/signals2.in"],
        append_segment_data=False,
        dates=[parser.parse('1970-01-01').date()],
        append_vehicle_type=False
    )
    for result, expected in zip(results, expecteds):
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[schema.TRAVEL_TIME.name],
            ignored_columns=[schema.LEAVE_TIME.name],
            null_equals_unexistant=True,
            unordered=True,
            keys=TRACK_KEYS,
        )
        assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_signals_many_inputs(ytc, schematize_match_signals):
    expecteds = ["//match_signals/travel_times.out", "//match_signals/travel_times2.out"]
    results = match_signals(
        ytc,
        ["//match_signals/signals.in", "//match_signals/signals2.in"],
        append_segment_data=False,
        dates=[parser.parse('1970-01-01').date(), parser.parse('1970-01-02').date()],
        append_vehicle_type=False
    )
    for result, expected in zip(results, expecteds):
        assert_equal_tables(
            ytc, expected, result,
            float_columns=[schema.TRAVEL_TIME.name],
            ignored_columns=[schema.LEAVE_TIME.name],
            null_equals_unexistant=True,
            unordered=True,
            keys=TRACK_KEYS,
        )
        assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_signals_mt(ytc, schematize_match_signals):
    expected = "//match_signals/travel_times.mt.out"
    result = match_signals(
        ytc,
        "//match_signals/signals.mt.in",
        append_segment_data=False,
        append_vehicle_type=False,
        threads=5,
        op_spec={'reducer': {'cpu_limit': 1}},  # test cluster have no CPUs
    )
    assert_equal_tables(
        ytc, expected, result,
        float_columns=[schema.TRAVEL_TIME.name],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True,
        unordered=True,
        keys=TRACK_KEYS,
    )
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION
