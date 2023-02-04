# -*- coding: utf-8 -*-
from datacloud.dev_utils.data.data_utils import array_tostring
from datacloud.features.time_hist.build_config import TimeHistBuildConfig
from datacloud.features.time_hist.time_hist_features import (
    step_1_calulate_timezone,
    step_2_aggregates_by_days_category,
    step_3_prepare_vectors
)


def test_steps_prod(yt_client, yql_client):
    expected_timezone_table = [
        {
            'cid': '100',
            'timezone_name': 'Asia/Yekaterinburg',
            'user_region': 52,
        },
        {
            'cid': '300',
            'timezone_name': 'Europe/Moscow',
            'user_region': 2,
        },
    ]

    expected_histogram_table = [
        {
            'cid': '100',
            'hist_activity_count': {'working': {'14': 1, '17': 1, '23': 1}},
            'hist_activity_rate': {'working': {'14': 1.0, '17': 1.0, '23': 1.0}},
            'timezone_name': 'Asia/Yekaterinburg',
            'total_activity_days': {'working': 1}
        },
        {
            'cid': '300',
            'hist_activity_count': {'working': {'17': 1}},
            'hist_activity_rate': {'working': {'17': 1.0}},
            'timezone_name': 'Europe/Moscow',
            'total_activity_days': {'working': 1}
        }
    ]

    expected_features_table = [
        {
            'cid': '100',
            'features': array_tostring([
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                1.
            ]),
        },
        {
            'cid': '300',
            'features': array_tostring([
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                1.
            ]),
        },
    ]

    build_config = TimeHistBuildConfig(is_retro=False, snapshot_date='2020-01-21')

    step_1_calulate_timezone(build_config, yt_client, yql_client)
    timezone_data = list(yt_client.read_table(build_config.timezones_table))
    assert timezone_data == expected_timezone_table

    step_2_aggregates_by_days_category(build_config, yt_client, yql_client)
    histogram_table_list = list(yt_client.read_table(build_config.histogram_table))
    assert histogram_table_list == expected_histogram_table

    step_3_prepare_vectors(build_config, yt_client)
    features_table_list = list(yt_client.read_table(build_config.features_table))
    assert features_table_list == expected_features_table
