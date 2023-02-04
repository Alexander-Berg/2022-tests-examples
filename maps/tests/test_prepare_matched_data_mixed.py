import maps.analyzer.toolkit.lib as tk

from .test_common import prepare_matched_data_test


def test_prepare_matched_data_mixed(ytc):
    day_signals_table = tk.sources.filter_signals_logs(ytc, '//logs/analyzer-dispatcher-signals-log/1d/2017-10-05', has_no_extra_columns=True)
    tk.utils.move_to_persistent_place(ytc, day_signals_table[0], '//ananlyzer/data/signals/2017-10-05')
    prepare_matched_data_test(ytc, '/prepare_matched_data_extra.json', tk.schema.SIGNALS_EXTRA_TABLE)
