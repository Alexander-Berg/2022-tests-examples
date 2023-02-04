# -*- coding: utf-8 -*-
from collections import defaultdict
from itertools import izip
import numpy as np
from yt.wrapper import ypath_join

from datacloud.dev_utils.data.data_utils import array_fromstring
from datacloud.features.geo.build_config import GeoBuildConfig
from datacloud.features.geo.geo_features import (
    step_0_grep_logs, step_1_filter_logs, step_2_calculate_distances,
    step_3_filter_distances, step_4_calc_features, step_5_compact_features,
    step_6_add_binary_features
)

ROOT = '//projects/scoring/test_partner/XPROD-000'
GEO_AGGS = ypath_join(ROOT, 'datacloud/aggregates/geo')
EPS = 1e-4


def assert_exists_sorted(yt_client, path, sorted_by):
    assert yt_client.exists(path)
    assert yt_client.get_attribute(path, 'sorted')
    assert yt_client.get_attribute(path, 'sorted_by') == sorted_by


def assert_grep_logs(yt_client):
    fetched_logs = ypath_join(ROOT, 'datacloud/grep/geo/geo')
    assert_exists_sorted(yt_client, fetched_logs, ['external_id', 'timestamp_of_log'])

    eid2points_num = defaultdict(int)
    for row in yt_client.read_table(fetched_logs):
        eid2points_num[row['external_id']] += 1

    eid2points_num_expected = {
        '3207936_2018-08-10': 7,
        '3282822_2018-01-11': 4,
        '3283480_2018-01-11': 8
    }
    assert eid2points_num == eid2points_num_expected


def assert_filter_logs1(yt_client):
    filtered_logs1 = ypath_join(GEO_AGGS, 'filtered_logs1')
    assert_exists_sorted(yt_client, filtered_logs1, ['external_id'])

    eid2points_num = defaultdict(int)
    eid2points_ts = defaultdict(set)
    for row in yt_client.read_table(filtered_logs1):
        external_id = row['external_id']
        eid2points_num[external_id] += 1
        eid2points_ts[external_id].add(row['timestamp_of_log'])

    eid2points_num_expected = {
        '3207936_2018-08-10': 3,
        '3282822_2018-01-11': 2,
        '3283480_2018-01-11': 4
    }
    assert eid2points_num == eid2points_num_expected

    eid2points_ts_expected = {
        '3207936_2018-08-10': set([-1523664000L]),
        '3282822_2018-01-11': set([-1515369600L]),
        '3283480_2018-01-11': set([-1515369600L]),
    }
    assert eid2points_ts == eid2points_ts_expected


def assert_filter_logs2(yt_client):
    filtered_logs2 = ypath_join(GEO_AGGS, 'filtered_logs2')
    assert_exists_sorted(yt_client, filtered_logs2, ['external_id'])

    eid2points_num = defaultdict(int)
    for row in yt_client.read_table(filtered_logs2):
        external_id = row['external_id']
        eid2points_num[external_id] += 1

    eid2points_num_expected = {
        '3207936_2018-08-10': 3,
        '3282822_2018-01-11': 2,
        '3283480_2018-01-11': 1
    }
    assert eid2points_num == eid2points_num_expected


def check_distances(yt_client, table_path, eid2distances_expected, eps=EPS):
    eid2distances = defaultdict(list)
    for row in yt_client.read_table(table_path):
        eid2distances[row['external_id']].append(row['distance'])

    assert set(eid2distances.keys()) == set(eid2distances_expected.keys())
    for eid, distances in eid2distances.iteritems():
        distances_e = eid2distances_expected[eid]
        assert len(distances) == len(distances_e)
        for dist, dist_e in izip(sorted(distances), distances_e):
            assert abs(dist - dist_e) < EPS, (eid, table_path)


def assert_calculate_distances(yt_client):
    distances = ypath_join(GEO_AGGS, 'distances')
    assert_exists_sorted(yt_client, distances, ['external_id', 'type', 'distance'])

    eid2distances_expected = {
        '3207936_2018-08-10': [837093.2308960505, 1434132.0853727437, 3113684.8520823177],
        '3283480_2018-01-11': [636193.5193623917],
    }
    check_distances(yt_client, distances, eid2distances_expected)


def assert_filter_distances(yt_client):
    distances_filtered = ypath_join(GEO_AGGS, 'distances_filtered')
    assert_exists_sorted(yt_client, distances_filtered, ['external_id', 'type', 'distance'])

    eid2distances_expected = {
        '3207936_2018-08-10': [837093.2308960505, 1434132.0853727437],
        '3283480_2018-01-11': [636193.5193623917],
    }
    check_distances(yt_client, distances_filtered, eid2distances_expected)


def assert_calc_features(yt_client):
    features_flatten = ypath_join(GEO_AGGS, 'features_flatten')
    assert_exists_sorted(yt_client, features_flatten, ['external_id', 'type', 'feature'])

    eid2features = defaultdict(list)
    for row in yt_client.read_table(features_flatten):
        eid2features[row['external_id']].append(row['feature'])
    eid2features = {
        eid: sorted(features) for eid, features in eid2features.iteritems()
    }

    eid2features_expected = {
        '3207936_2018-08-10': [0., 0.],
        '3283480_2018-01-11': [1.],
    }
    assert eid2features == eid2features_expected


def assert_compact_features(yt_client):
    features = ypath_join(GEO_AGGS, 'features')
    assert_exists_sorted(yt_client, features, ['external_id'])

    eid2features = dict()
    for row in yt_client.read_table(features):
        eid2features[row['external_id']] = list(array_fromstring(row['features']))

    eid2features_expected = {
        '3207936_2018-08-10': [0., 0., 0., 0., 0., 0.],
        '3283480_2018-01-11': [0., 0., 0., 0., 1., 0.],
    }
    assert eid2features == eid2features_expected


def assert_add_binary_features(yt_client):
    features_geo = ypath_join(GEO_AGGS, 'features_geo')
    assert_exists_sorted(yt_client, features_geo, ['external_id'])

    eid2features = dict()
    for row in yt_client.read_table(features_geo):
        eid2features[row['external_id']] = list(array_fromstring(row['features']))

    eid2features_expected = {
        '3207936_2018-08-10': [0., 0., 0., 0., 0., 0., 0., 0., 1.],
        '3282378_2018-01-05': [0., 0., 0., 0., 0., 0., 0., 0., 1.],
        '3282822_2018-01-11': [0., 0., 0., 0., 0., 0., 0., 0., 1.],
        '3283480_2018-01-11': [0., 0., 0., 0., 1., 0., 0., 0., 1.],
    }
    assert eid2features == eid2features_expected


def test_steps(yt_client):
    build_config = GeoBuildConfig(
        root='//projects/scoring/test_partner/XPROD-000',
        min_date='1900-01-01',
        max_date='2018-08-10',
        max_distances_in_category=2,
        distance_thresh=700000.
    )

    step_checks_pairs = [
        (step_0_grep_logs, [assert_grep_logs]),
        (step_1_filter_logs, [assert_filter_logs1, assert_filter_logs2]),
        (step_2_calculate_distances, [assert_calculate_distances]),
        (step_3_filter_distances, [assert_filter_distances]),
        (step_4_calc_features, [assert_calc_features]),
        (step_5_compact_features, [assert_compact_features]),
        (step_6_add_binary_features, [assert_add_binary_features])
    ]

    for step, checks in step_checks_pairs:
        step(build_config=build_config, yt_client=yt_client)
        for check in filter(callable, checks):
            check(yt_client=yt_client)
