# -*- coding: utf-8 -*-
from datacloud.dev_utils.data.data_utils import array_fromstring
from datacloud.features.locations.build_config import LocationsBuildConfig
from datacloud.features.locations.locations_features import step_1_gather_round_logs, step_2_calculate_user_statistics, \
    step_3_calculate_bandits_statistics, step_4_calculate_homework_statistics, step_5_merge_tables

ROOT = '//projects/scoring/test_partner/XPROD-000'
EXPECTED_SUFFIX = '_expected'


def test_step_1(yt_client, yql_client):
    build_config = LocationsBuildConfig(root=ROOT, days_to_take=5)
    step_1_gather_round_logs(yt_client, yql_client, build_config)
    expected_features = list(yt_client.read_table(build_config.locations_round_table + EXPECTED_SUFFIX))
    features_data = list(yt_client.read_table(build_config.locations_round_table))
    assert features_data == expected_features


def test_step_2(yt_client, yql_client):
    build_config = LocationsBuildConfig(root=ROOT, days_to_take=5,
                                        country_kmeans_top=1,
                                        country_kmeans_clusters=1,
                                        country_mlb_top=0,
                                        region_kmeans_top=1,
                                        region_kmeans_clusters=1,
                                        region_mlb_top=0,
                                        city_min_count=1,
                                        native_country_code=1,
                                        use_pretrain_transformer=False,
                                        )
    build_config.locations_round_table += EXPECTED_SUFFIX
    step_2_calculate_user_statistics(yt_client, yql_client, build_config)
    expected_features = list(yt_client.read_table(build_config.locations_stat_table + EXPECTED_SUFFIX))
    features_data = list(yt_client.read_table(build_config.locations_stat_table))
    assert features_data == expected_features

    expected_features = list(yt_client.read_table(build_config.locations_stat_table_map + EXPECTED_SUFFIX))
    features_data = list(yt_client.read_table(build_config.locations_stat_table_map))
    assert features_data == expected_features

    expected_features = list(yt_client.read_table(build_config.locations_stat_table_cat + EXPECTED_SUFFIX))
    features_data = list(yt_client.read_table(build_config.locations_stat_table_cat))
    assert features_data == expected_features


def test_step_3(yt_client, yql_client):
    build_config = LocationsBuildConfig(root=ROOT)
    build_config.locations_round_table += EXPECTED_SUFFIX
    step_3_calculate_bandits_statistics(yt_client, yql_client, build_config)
    expected_features = list(yt_client.read_table(build_config.locations_bandits_table + EXPECTED_SUFFIX))
    features_data = list(yt_client.read_table(build_config.locations_bandits_table))
    assert features_data == expected_features


def test_step_4(yt_client, yql_client):
    build_config = LocationsBuildConfig(root=ROOT)
    step_4_calculate_homework_statistics(yt_client, yql_client, build_config)
    expected_features = list(yt_client.read_table(build_config.locations_homework_table + EXPECTED_SUFFIX))
    features_data = list(yt_client.read_table(build_config.locations_homework_table))
    assert features_data == expected_features


def test_step_5(yt_client, yql_client):
    build_config = LocationsBuildConfig(root=ROOT)
    build_config.locations_stat_table_map += EXPECTED_SUFFIX
    build_config.locations_bandits_table += EXPECTED_SUFFIX
    build_config.locations_homework_table += EXPECTED_SUFFIX

    step_5_merge_tables(yt_client, yql_client, build_config)

    eid2features = dict()
    for row in yt_client.read_table(build_config.out_table):
        eid2features[row['external_id']] = list(array_fromstring(row['features']))

    eid2features_expected = {
        'eid_000': [5.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                    1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0, 1.0, 1.0,
                    1.0, 5.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                    0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, ],
        'eid_999': [-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0,
                    -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0,
                    -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0,
                    -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0,
                    -1.0, -1.0, -1.0, ],
    }
    assert eid2features == eid2features_expected
