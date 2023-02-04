import maps.analyzer.toolkit.lib as tk

from .test_common import prepare_matched_data_test


def test_prepare_matched_data(ytc):
    prepare_matched_data_test(ytc, '/prepare_matched_data.json', tk.schema.SIGNALS_TABLE)
