# -*- coding: utf-8 -*-
from datacloud.features.phone_range.phone_range_features import build_phone_range_vectors
from datacloud.features.phone_range.build_config import PhoneRangeBuildConfig


ROOT = '//projects/scoring/test_partner/XPROD-000'
GLUED_ROOT = '//projects/scoring/test_partner/XPROD-001'


def test_build_vectors(yt_client, yql_client):
    build_config = PhoneRangeBuildConfig(root=ROOT, pure_external_id=True)
    build_phone_range_vectors(yt_client, yql_client, build_config)
    expected_features = [
        {'external_id': '1_2019-01-01', 'region': 'Москва', 'operator': 'Yandex mobile', 'region_id': 1, 'change_region': False},
        {'external_id': '2_2019-01-01', 'region': 'Москва', 'operator': 'Cheep mobile', 'region_id': 1, 'change_region': None},
    ]
    features_data = list(yt_client.read_table(build_config.features_table))
    assert features_data == expected_features
    assert yt_client.list(build_config.data_dir) == ['features']


def test_build_vectors_with_glued_external_id(yt_client, yql_client):
    build_config = PhoneRangeBuildConfig(root=GLUED_ROOT)
    build_phone_range_vectors(yt_client, yql_client, build_config)
    expected_features = [
        {'external_id': '1_2019-01-01', 'region': 'Москва', 'operator': 'Yandex mobile', 'region_id': 1, 'change_region': False},
        {'external_id': '2_2019-01-01', 'region': 'Москва', 'operator': 'Cheep mobile', 'region_id': 1, 'change_region': None},
    ]
    features_data = list(yt_client.read_table(build_config.features_table))
    assert features_data == expected_features
    assert yt_client.list(build_config.data_dir) == ['features']


def test_build_vectors_prod(yt_client, yql_client):
    build_config = PhoneRangeBuildConfig(is_retro=False, snapshot_date='2020-01-21')
    build_phone_range_vectors(yt_client, yql_client, build_config)
    expected_features = [
        {'cid': '1', 'region': 'Москва', 'operator': 'Yandex mobile', 'region_id': 1, 'change_region': False},
        {'cid': '2', 'region': 'Москва', 'operator': 'Cheep mobile', 'region_id': 1, 'change_region': None},
    ]
    features_data = list(yt_client.read_table(build_config.features_table))
    assert features_data == expected_features
    assert yt_client.list(build_config.data_dir) == ['features']
