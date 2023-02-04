from datetime import date

from test_common import run_impl


def test_without_models(ytc, unset_graph_version):
    run_impl(ytc, None, date(2017, 10, 20), date(2017, 10, 20), ['2017-10-20'])
