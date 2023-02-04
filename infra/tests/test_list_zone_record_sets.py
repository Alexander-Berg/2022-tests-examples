import pytest
import random
import string

import helpers

from infra.yp_dns_api.bridge.api import api_pb2


TEST_ZONE_NAME = 'test-zone.yandex.net'
ANOTHER_ZONE_NAME = 'another-zone.yandex.net'
UNKNOWN_ZONE_NAME = 'unknown.yandex.net'


@pytest.mark.usefixtures("yp_env")
def test_list_unknown_zone(yp_env):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
        },
    })
    yp_client = yp_instance.create_client()

    bridge.wait_update()

    client.list_zone_record_sets(UNKNOWN_ZONE_NAME)
    helpers.check_list_zone_record_sets(client, UNKNOWN_ZONE_NAME, [], [], expected_status=api_pb2.TRspListZoneRecordSets.UNKNOWN_ZONE)

    yp_client.create_object(
        'dns_record_set',
        helpers.make_dns_record_set('a.{}'.format(UNKNOWN_ZONE_NAME), 'AAAA', '2a02:6b8:c0c:b02:100:0:5d8a:0'),
        enable_structured_response=True
    )
    bridge.wait_update()

    # TODO: ensure update is received
    helpers.check_list_zone_record_sets(client, UNKNOWN_ZONE_NAME, [], [], expected_status=api_pb2.TRspListZoneRecordSets.UNKNOWN_ZONE)

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
@pytest.mark.parametrize("labeled_with_zone", [True, False])
def test_list_zone(yp_env, labeled_with_zone):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'RecordSetsLabeledWithZone': labeled_with_zone,
        },
    })
    yp_client = yp_instance.create_client()

    try:
        helpers.check_list_zone_record_sets(client, TEST_ZONE_NAME, [], timestamps={cluster_name: yp_client.generate_timestamp()})

        record_sets = helpers.make_dns_record_sets({
            'fqdn-{}.{}'.format(i, TEST_ZONE_NAME): [
                '2a02:6b8:c0c:b02:100:0:5d8a:{}'.format(i)
            ] for i in range(100)
        }, 'AAAA', zone=TEST_ZONE_NAME)
        create_resp = yp_client.create_objects(record_sets, enable_structured_response=True)

        helpers.check_list_zone_record_sets(client, TEST_ZONE_NAME, record_sets, [], timestamps={cluster_name: create_resp['commit_timestamp']})

        another_zone_record_sets = helpers.make_dns_record_sets({
            'fqdn-{}.{}'.format(i, ANOTHER_ZONE_NAME): [
                '2a02:6b8:c0c:b02:100:0:5d8a:{}'.format(i)
            ] for i in range(100)
        }, 'AAAA', zone=ANOTHER_ZONE_NAME)
        create_resp = yp_client.create_objects(another_zone_record_sets, enable_structured_response=True)

        helpers.check_list_zone_record_sets(client, TEST_ZONE_NAME, record_sets, record_sets, timestamps={cluster_name: create_resp['commit_timestamp']})
    finally:
        bridge.stop()


@pytest.mark.usefixtures("yp_env")
@pytest.mark.parametrize("labeled_with_zone", [True, False])
def test_paginating_list_zone(yp_env, labeled_with_zone):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'RecordSetsLabeledWithZone': labeled_with_zone,
        },
    })
    yp_client = yp_instance.create_client()

    try:
        another_zone_record_sets = helpers.make_dns_record_sets({
            'fqdn-{}.{}'.format(i, ANOTHER_ZONE_NAME): [
                '2a02:6b8:c0c:b02:100:0:5d8a:{}'.format(i)
            ] for i in range(100)
        }, 'AAAA', zone=ANOTHER_ZONE_NAME)
        yp_client.create_objects(another_zone_record_sets)

        record_sets = helpers.make_dns_record_sets({
            'fqdn-{}.{}'.format(i, TEST_ZONE_NAME): [
                '2a02:6b8:c0c:b02:100:0:5d8a:{}'.format(i)
            ] for i in range(100)
        }, 'AAAA', zone=TEST_ZONE_NAME)
        create_resp = yp_client.create_objects(record_sets, enable_structured_response=True)

        record_sets = helpers.sort_dns_record_sets(record_sets)

        result = []
        continuation_token = None
        random.seed(1337)
        while True:
            limit = random.randint(1, 10)
            while True:
                resp = client.list_zone_record_sets(TEST_ZONE_NAME, limit=limit, continuation_token=continuation_token)
                if resp.yp_timestamps[cluster_name] >= create_resp['commit_timestamp']:
                    break

            expected = record_sets[len(result):len(result) + limit]
            helpers.check_list_zone_record_sets_response(resp, expected)

            continuation_token = resp.continuation_token

            result.extend(resp.record_sets)

            if len(resp.record_sets) < limit:
                break

        assert len(result) == len(record_sets)
    finally:
        bridge.stop()


@pytest.mark.usefixtures("yp_env")
@pytest.mark.parametrize("labeled_with_zone", [True, False])
def test_list_multicluster_zone(yp_env, labeled_with_zone):
    yp_instances = yp_env
    yp_instances = {cluster: yp_instances[cluster] for cluster in list(yp_instances.keys())[:2]}
    assert len(yp_instances) == 2
    clusters = list(yp_instances.keys())
    default_ttl = 666
    yp_clients = {cluster: yp_instances[cluster].create_client() for cluster in clusters}
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'DefaultTtl': default_ttl,
            'Clusters': clusters,
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
            'RecordSetsLabeledWithZone': labeled_with_zone,
        },
    })

    try:
        for cluster in clusters:
            yp_client = yp_clients[cluster]

            another_zone_record_sets = helpers.generate_record_sets(100, zone=ANOTHER_ZONE_NAME, save_object_type=True)
            yp_client.create_objects(another_zone_record_sets)

        merged_record_sets = []
        for idx, cluster in enumerate(clusters):
            record_sets = helpers.generate_record_sets(100, seed=idx)
            helpers.add_records(client, record_sets, hints={'cluster': cluster},
                                check_response={'clusters': [cluster]})
            merged_record_sets = helpers.merge_record_set_lists([merged_record_sets, record_sets])

        record_sets = helpers.sort_dns_record_sets(merged_record_sets)

        # remove some record sets
        record_sets_to_remove = record_sets[::5]
        helpers.remove_records(client, record_sets_to_remove, check_response=True)

        commit_timestamps = {cluster: yp_clients[cluster].generate_timestamp() for cluster in clusters}

        # filter left record sets
        record_sets = [record_set for idx, record_set in enumerate(record_sets) if idx % 5 != 0]

        result = []
        continuation_token = None
        random.seed(1337)
        while True:
            limit = random.randint(1, 10)
            while True:
                resp = client.list_zone_record_sets(TEST_ZONE_NAME, limit=limit, continuation_token=continuation_token)
                if all(map(lambda cluster: resp.yp_timestamps[cluster] >= commit_timestamps[cluster], clusters)):
                    break

            expected = record_sets[len(result):len(result) + limit]
            helpers.check_list_zone_record_sets_response(resp, expected, options={'default_ttl': default_ttl})

            continuation_token = resp.continuation_token

            result.extend(resp.record_sets)

            if len(resp.record_sets) < limit:
                break

        assert len(result) == len(record_sets)
    finally:
        bridge.stop()


@pytest.mark.usefixtures("yp_env")
@pytest.mark.parametrize("labeled_with_zone", [True, False])
def test_paginating_list_multicluster_zone_one_by_one(yp_env, labeled_with_zone):
    yp_instances = yp_env
    yp_instances = {cluster: yp_instances[cluster] for cluster in list(yp_instances.keys())[:2]}
    assert len(yp_instances) == 2
    clusters = list(yp_instances.keys())
    yp_clients = {cluster: yp_instances[cluster].create_client() for cluster in clusters}
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': clusters,
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
            'RecordSetsLabeledWithZone': labeled_with_zone,
        },
    })

    try:
        # prepare data
        commit_timestamps = {}

        data = {}
        data[0] = {
            ch: {
                'fqdn': 'fqdn-{}.{}'.format(ch, TEST_ZONE_NAME),
                'data': '2a02:6b8:c0c:b02:100:0:5d8{}:0'.format(ch),
            }
            for ch in string.ascii_lowercase[:3]
        }
        record_sets = [
            {
                'meta': {'id': data[0][ch]['fqdn']},
                'spec': {'records': [{'type': 'AAAA', 'data': data[0][ch]['data']}]},
            }
            for ch in ['a', 'b', 'c']
        ]
        helpers.add_records(client, record_sets, hints={'cluster': clusters[0]},
                            check_response={'clusters': [clusters[0]]})
        commit_timestamps[clusters[0]] = yp_clients[clusters[0]].generate_timestamp()

        data[1] = {
            ch: {
                'fqdn': data[0][ch]['fqdn'],
                'data': '2a02:6b8:c0c:b02:100:0:5d8{}:1'.format(ch),
            }
            for ch in data[0].keys()
        }
        record_sets = [
            {
                'meta': {'id': data[1][ch]['fqdn']},
                'spec': {'records': [{'type': 'AAAA', 'data': data[0][ch]['data']}]},
            }
            for ch in ['a']
        ]
        helpers.remove_records(client, record_sets, hints={'cluster': clusters[1]},
                               check_response={'clusters': [clusters[1]]})
        commit_timestamps[clusters[1]] = yp_clients[clusters[1]].generate_timestamp()

        # check one by one
        continuation_token = None
        expected_record_sets = [
            {
                'meta': {'id': data[0]['b']['fqdn']},
                'spec': {'records': [
                    {'type': 'AAAA', 'class': 'IN', 'ttl': 600, 'data': data[0]['b']['data']},
                ]}
            },
        ]
        resp = helpers.check_list_zone_record_sets(client, TEST_ZONE_NAME, expected_record_sets=expected_record_sets,
                                                   limit=1, continuation_token=continuation_token,
                                                   timestamps=commit_timestamps, expected_status=api_pb2.TRspListZoneRecordSets.OK)

        continuation_token = resp.continuation_token
        expected_record_sets = [
            {
                'meta': {'id': data[0]['c']['fqdn']},
                'spec': {'records': [
                    {'type': 'AAAA', 'class': 'IN', 'ttl': 600, 'data': data[0]['c']['data']},
                ]}
            },
        ]
        resp = helpers.check_list_zone_record_sets(client, TEST_ZONE_NAME, expected_record_sets=expected_record_sets,
                                                   limit=1, continuation_token=continuation_token,
                                                   timestamps=commit_timestamps, expected_status=api_pb2.TRspListZoneRecordSets.OK)

        continuation_token = resp.continuation_token
        expected_record_sets = []
        resp = helpers.check_list_zone_record_sets(client, TEST_ZONE_NAME, expected_record_sets=expected_record_sets,
                                                   limit=1, continuation_token=continuation_token,
                                                   timestamps=commit_timestamps, expected_status=api_pb2.TRspListZoneRecordSets.OK)
    finally:
        bridge.stop()
