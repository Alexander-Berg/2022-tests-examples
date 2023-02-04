from test_common import run_test
from cluster_helper import make_bins_folder
from yatest import common

import os

bin_list = ['rtyserver_test', 'rtyserver', 'searchproxy', 'indexerproxy']


def run_cluster_test(test_name, test_pars, test_key, timeout, metrics, links=None):
    bin_dir = make_bins_folder(bin_list)
    logs_dir = common.output_path('logs_' + test_key)
    os.makedirs(logs_dir)
    run_test(test_name, test_pars, test_key, timeout, metrics=metrics, links=links, bin_dir=bin_dir,
             extra_env={'LOG_PATH': logs_dir})
