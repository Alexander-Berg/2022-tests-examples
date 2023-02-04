# -*- coding: utf-8 -*-
from datacloud.features.contact_actions.contact_actions_features import build_contac_actions_vectors
from datacloud.features.contact_actions.build_config import ContacActionsBuildConfig


ROOT = '//projects/scoring/test_partner/XPROD-000'


def test_build_vectors(yt_client, yql_client):
    build_config = ContacActionsBuildConfig(root=ROOT, min_retro='2019-01-01', max_retro='2019-01-01')
    build_contac_actions_vectors(yt_client, yql_client, build_config)
    expected_features = [
        {'external_id': '1_2019-01-01', 'features': [True, True] + [False] * 48},
        {'external_id': '2_2019-01-01', 'features': [False] * 50},
    ]
    features_data = list(yt_client.read_table(build_config.features_table_path))
    assert features_data == expected_features
