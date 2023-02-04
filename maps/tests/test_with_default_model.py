from datetime import date

from test_common import run_impl


def test_with_default_model(ytc, unset_graph_version):
    run_impl(ytc, 'statistical_features', date(2017, 10, 20), date(2017, 10, 20), ['2017-10-20'])
