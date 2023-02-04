import maps.analyzer.toolkit.lib as tk

from .test_common import prepare_matched_data_test


def test_prepare_matched_data_with_logs_extra_columns(ytc):
    prepare_matched_data_test(ytc, '/prepare_matched_data_extra.json', tk.schema.SIGNALS_EXTRA_TABLE)
