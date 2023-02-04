from maps.analyzer.sandbox.masstransit.calculate_matched_signals_stats.lib.reducer import CalcStatsReducer
from yatest.common import source_path

import json


DATA_FILE_PATH = source_path('maps/analyzer/sandbox/masstransit/calculate_matched_signals_stats/tests/data/reducer_test_data.json')


def test_reducer():
    reducer = CalcStatsReducer()
    file = open(DATA_FILE_PATH, 'r')
    data = json.load(file)
    keys = data['keys']
    records = data['records']
    stats = reducer(keys, records)
    stats = next(stats)
    assert stats['clid'] == keys['clid']
    assert stats['uuid'] == keys['uuid']
    assert stats['signals_number'] == len(records)
    assert stats['distances_from_gps_to_bound_point_list'] == [0] * (len(records) - 2)
    assert stats['Bound_sequent_signals_numbers_list'] == [3, 2]
    assert stats['Bound_sequent_signals_lengths_list'] == [17, 5]
    assert stats['Bound_sequent_signals_durations_list'] == [5, 1]
    assert stats['FirstSignal_sequent_signals_numbers_list'] == [1]
    assert stats['FirstSignal_sequent_signals_durations_list'] == [0]
    assert stats['FirstSignal_sequent_signals_lengths_list'] is None
    assert stats['Rebound_sequent_signals_numbers_list'] == [1, 1]
    assert stats['Rebound_sequent_signals_durations_list'] == [0, 0]
    assert stats['Rebound_sequent_signals_lengths_list'] == [0, 0]
    assert stats['NotBound_sequent_signals_numbers_list'] == [1, 1]
    assert stats['ThreadChanged_sequent_signals_numbers_list'] == [1, 1]
    assert stats['WentOffThread_sequent_signals_numbers_list'] == [1]
    assert stats['SignalAfterLongAbsense_sequent_signals_numbers_list'] == [1]
