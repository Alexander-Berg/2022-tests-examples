import helpers
import logging
import pytest

from infra.yp_dns_api.bridge.api import api_pb2


@pytest.mark.usefixtures("yp_env")
def test_banning_clusters(yp_env):
    yp_instances = yp_env
    yp_instances = {cluster: yp_instances[cluster] for cluster in list(yp_instances.keys())[:3]}
    assert len(yp_instances) == 3
    clusters = list(yp_instances.keys())
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': clusters,
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
        },
    })

    try:
        banned = set([])
        left = set(clusters)

        for _ in range(len(clusters)):
            logging.info(f"Allowed clusters: {', '.join(left)}. Banned clusters: {', '.join(banned)}")
            record_sets = helpers.generate_record_sets(1, zone=helpers.TEST_ZONE_NAME)
            _, resp = helpers.add_records(client, record_sets,
                                          check_response={'clusters': left})

            used_cluster = resp.responses[0].update.cluster

            left.remove(used_cluster)
            banned.add(used_cluster)

            logging.info(f"Ban cluster {used_cluster}")
            helpers.apply_patch(bridge, {
                "BannedClusters": {
                    "UpdateRecords": list(banned),
                },
            })

        _, resp = helpers.add_records(client, record_sets)
        for response in resp.responses:
            assert response.update.status == api_pb2.TRspUpdateRecord.EUpdateRecordStatus.CLUSTER_BANNED

    finally:
        bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_hint_on_banned_cluster(yp_env):
    yp_instances = yp_env
    yp_instances = {cluster: yp_instances[cluster] for cluster in list(yp_instances.keys())[:3]}
    assert len(yp_instances) == 3
    clusters = list(yp_instances.keys())
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': clusters,
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
        },
    })

    try:
        for banned_cluster in clusters:
            logging.info(f"Ban cluster {banned_cluster}")
            helpers.apply_patch(bridge, {
                "BannedClusters": {
                    "UpdateRecords": [banned_cluster],
                },
            })

            record_sets = helpers.generate_record_sets(1, zone=helpers.TEST_ZONE_NAME)
            _, resp = helpers.add_records(client, record_sets, hints={'cluster': banned_cluster})

            for response in resp.responses:
                assert response.update.status == api_pb2.TRspUpdateRecord.EUpdateRecordStatus.CLUSTER_BANNED
                assert response.update.cluster == banned_cluster

    finally:
        bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_list_banned_clusters(yp_env):
    yp_instances = yp_env
    yp_instances = {cluster: yp_instances[cluster] for cluster in list(yp_instances.keys())[:2]}
    yp_clients = {cluster: yp_instance.create_client() for cluster, yp_instance in yp_instances.items()}
    assert len(yp_instances) == 2
    clusters = list(yp_instances.keys())
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': clusters,
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
        },
    })

    try:
        # add records to cluster[0]
        record_sets = helpers.generate_record_sets(100)
        helpers.add_records(client, record_sets, hints={'cluster': clusters[0]},
                            check_response={'clusters': [clusters[0]]})

        ban_params_set = [
            {
                "banned_clusters": [clusters[1]],
                "expected_record_sets": record_sets,
                "status": api_pb2.TRspListZoneRecordSets.OK
            },
            {
                "banned_clusters": [clusters[0]],
                "expected_record_sets": [],
                "status": api_pb2.TRspListZoneRecordSets.OK
            },
            {
                "banned_clusters": clusters,
                "expected_record_sets": [],
                "status": api_pb2.TRspListZoneRecordSets.DATA_UNAVAILABLE
            },
        ]

        for ban_params in ban_params_set:
            not_banned_clusters = set(clusters) - set(ban_params['banned_clusters'])
            logging.info(f"Ban clusters {', '.join(ban_params['banned_clusters'])}")
            helpers.apply_patch(bridge, {
                "BannedClusters": {
                    "ListZoneRecordSets": ban_params['banned_clusters'],
                },
            })

            resp = helpers.check_list_zone_record_sets(
                client,
                helpers.TEST_ZONE_NAME,
                ban_params['expected_record_sets'],
                timestamps={
                    cluster: yp_clients[cluster].generate_timestamp()
                    for cluster in not_banned_clusters
                },
                expected_status=ban_params['status'])

            assert set(resp.yp_timestamps.keys()) == set(not_banned_clusters)
            assert set(resp.banned_clusters) == set(ban_params['banned_clusters'])

    finally:
        bridge.stop()
