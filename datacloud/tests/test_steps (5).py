# -*- coding: utf-8 -*-
from datacloud.dev_utils.data.data_utils import array_tostring
from datacloud.features.time_hist.build_config import TimeHistBuildConfig
from datacloud.features.time_hist.time_hist_features import (
    step_1_calulate_timezone,
    step_2_aggregates_by_days_category,
    step_3_prepare_vectors
)


def test_steps_retro(yt_client, yql_client):
    expected_timezone_table = [
        {
            'external_id': '1_2017-07-01',
            'timezone_name': 'Asia/Yekaterinburg',
            'user_region': 52,
        },
        {
            'external_id': '2_2017-07-01',
            'timezone_name': 'Europe/Moscow',
            'user_region': 213,
        },
    ]

    expected_histogram_table = [
        {
            'external_id': '1_2017-07-01',
            'hist_activity_count': {'working': {'17': 1, '20': 1, '22': 1},
                                    'holiday': {'15': 1}},
            'hist_activity_rate': {'working': {'17': 1. / 3, '20': 1. / 3, '22': 1. / 3},
                                   'holiday': {'15': 1.}},
            'total_activity_days': {'working': 3, 'holiday': 1},
            'timezone_name': 'Asia/Yekaterinburg',
        },
        {
            'external_id': '2_2017-07-01',
            'hist_activity_count': {'working': {'14': 1, '16': 1, '18': 1, '19': 1, '20': 1}},
            'hist_activity_rate': {'working': {'14': 1. / 3, '16': 1. / 3, '18': 1. / 3, '19': 1. / 3, '20': 1. / 3}},
            'total_activity_days': {'working': 3},
            'timezone_name': 'Europe/Moscow',
        },
        {
            'external_id': '3_2017-07-01',
            'hist_activity_count': {'working': {'20': 1}},
            'hist_activity_rate': {'working': {'20': 1.}},
            'total_activity_days': {'working': 1},
            'timezone_name': None,
        }
    ]

    expected_features_table = [
        {
            'external_id': '1_2017-07-01',
            'features': array_tostring([
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 1. / 3,
                0.0, 0.0, 1. / 3, 0.0, 1. / 3, 0.0,
                3.
            ]),
        },
        {
            'external_id': '2_2017-07-01',
            'features': array_tostring([
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 1. / 3, 0.0, 1. / 3, 0.0,
                1. / 3, 1. / 3, 1. / 3, 0.0, 0.0, 0.0,
                3.
            ]),
        },
        {
            'external_id': '3_2017-07-01',
            'features': array_tostring([
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
                1.
            ]),
        },
    ]

    build_config = TimeHistBuildConfig(
        root='//projects/scoring/test_partner/XPROD-000',
        min_retro_date='2017-01-01',
        max_retro_date='2017-12-31'
    )

    step_1_calulate_timezone(build_config, yt_client, yql_client)
    timezone_data = list(yt_client.read_table(build_config.timezones_table))
    assert timezone_data == expected_timezone_table

    step_2_aggregates_by_days_category(build_config, yt_client, yql_client)
    histogram_table_list = list(yt_client.read_table(build_config.histogram_table))
    assert histogram_table_list == expected_histogram_table

    step_3_prepare_vectors(build_config, yt_client)
    features_table_list = list(yt_client.read_table(build_config.features_table))
    assert features_table_list == expected_features_table
