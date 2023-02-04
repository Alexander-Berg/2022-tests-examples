# -*- coding: utf-8 -*-
from datacloud.features.time_hist.time_hist_grep import run_grep
from datacloud.config.yt import GREP_ROOT


def test_grep(yt_client, yql_client):
    log_date = '2020-01-21'
    run_grep(log_date, yql_client, yt_client, use_cloud_nodes=False)

    grep_table_list = list(yt_client.read_table(GREP_ROOT + '/region_log/' + log_date))
    expected_features_table = [
        {'yuid': '1', 'user_region': 10313, 'log_date': '2020-01-21'},
        {'yuid': '2', 'user_region': 10313, 'log_date': '2020-01-21'},
    ]
    assert grep_table_list == expected_features_table
