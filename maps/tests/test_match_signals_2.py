from maps.analyzer.pylibs.test_tools import schematize_table
from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables

import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.pylibs.schema as s

from maps.analyzer.pylibs.graphmatching.lib import match_signals
import maps.analyzer.pylibs.graphmatching.lib.schema as schema


TRACK_KEYS = [schema.CLID.name, schema.UUID.name, schema.VEHICLE_TYPE.name, schema.TRACK_START_TIME.name, schema.ENTER_TIME.name]


def test_match_signals_extra_data(ytc):
    expected = "//match_signals/travel_times_with_extra_data.out"
    schematize_table(ytc, "//match_signals/signals.in")
    result = match_signals(
        ytc,
        "//match_signals/signals.in",
        append_segment_data=True,
        append_vehicle_type=False
    )
    assert_equal_tables(
        ytc, expected, result,
        # TODO: Use `get_table_float_column_names`
        float_columns=[
            col.name for col in schema.TRAVEL_TIMES_TABLE.columns
            if col.type.unwrapped == s.Double
        ],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True, unordered=True,
        keys=TRACK_KEYS,
    )
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_signals_output_signals(ytc, schematize_match_signals):
    expected = "//match_signals/travel_times.out"
    expected_signals = "//match_signals/signals.out"
    result, signals = match_signals(
        ytc,
        "//match_signals/signals.in",
        output_signals=True,
        append_segment_data=False,
        append_vehicle_type=False
    )
    assert_equal_tables(
        ytc, expected, result,
        float_columns=[schema.TRAVEL_TIME.name],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True, unordered=True,
        keys=TRACK_KEYS,
    )
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION
    assert_equal_tables(
        ytc, expected_signals, signals,
        float_columns=[schema.LAT.name, schema.LON.name, schema.TIME_TO_NEXT.name, schema.LENGTH_TO_NEXT.name],
        null_equals_unexistant=True, unordered=True,
        keys=TRACK_KEYS,
    )

    rematched = match_signals(
        ytc,
        signals,
        match_all_signals=True,
        output_signals=False,
        append_segment_data=False,
        append_vehicle_type=False
    )
    assert envkit.graph.graph_version_attribute(ytc, rematched) == envkit.graph.VERSION
    assert_equal_tables(
        ytc, expected, rematched,
        float_columns=[schema.TRAVEL_TIME.name],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True,
        unordered=True, keys=TRACK_KEYS,
    )


def test_match_signals_vehicle_type(ytc, schematize_match_signals):
    expected = "//match_signals/travel_times_with_vehicle_type.out"
    result = match_signals(
        ytc,
        "//match_signals/signals.vehicle_type.in",
        append_segment_data=True,
        append_vehicle_type=True
    )
    assert_equal_tables(
        ytc, expected, result,
        # TODO: Use `get_table_float_column_names`
        float_columns=[
            col.name for col in schema.TRAVEL_TIMES_TABLE.columns
            if col.type.unwrapped == s.Double
        ],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True, unordered=True, keys=TRACK_KEYS,
    )
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_signals_no_vehicle_type(ytc, schematize_match_signals):
    expected = "//match_signals/travel_times_with_extra_data.out"
    result = match_signals(
        ytc,
        "//match_signals/signals.vehicle_type.in",
        append_segment_data=True,
        append_vehicle_type=False
    )
    assert_equal_tables(
        ytc, expected, result,
        # TODO: Use `get_table_float_column_names`
        float_columns=[
            col.name for col in schema.TRAVEL_TIMES_TABLE.columns
            if col.type.unwrapped == s.Double
        ],
        ignored_columns=[schema.LEAVE_TIME.name],
        null_equals_unexistant=True,
        unordered=True, keys=TRACK_KEYS,
    )
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION
