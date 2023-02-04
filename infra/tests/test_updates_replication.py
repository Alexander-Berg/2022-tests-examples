import helpers
import logging
import pytest

from infra.yp_dns_api.bridge.api import api_pb2


TEST_ZONE_NAME = 'test-zone.yandex.net'


@pytest.mark.usefixtures("yp_env")
def test_record_update_replication(yp_env):
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
        },
    })
    replicator = helpers.create_replicator(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': clusters,
        },
    })

    # must not fail: there is no data in YP
    assert replicator.sync()

    record_sets = helpers.generate_record_sets(1)
    record_set = record_sets[0]

    # add record set on first cluster
    helpers.add_records(client, [record_set], hints={'cluster': clusters[0]}, check_response={'clusters': [clusters[0]]})

    # check that record set added on first cluster
    record_set_in_first_cluster = helpers.list_record_sets(yp_clients[clusters[0]])[0]
    helpers.check_record_sets_equal(record_set_in_first_cluster, record_set)
    assert len(record_set_in_first_cluster['labels']['changelist']['changes']) == 1
    assert not record_set_in_first_cluster['labels']['changelist']['changes'][0]['replicated']

    # check that there are no record sets on second cluster
    assert len(helpers.list_record_sets(yp_clients[clusters[1]])) == 0

    # first sync should replicate updates to second cluster
    assert replicator.sync()
    for cluster in clusters:
        record_set_in_cluster = helpers.list_record_sets(yp_clients[cluster])[0]
        helpers.check_record_sets_equal(record_set_in_cluster, record_set)
        assert len(record_set_in_cluster['labels']['changelist']['changes']) == 1
        assert not record_set_in_cluster['labels']['changelist']['changes'][0]['replicated']

    # second sync sets "replicated" flag to true since updates are on all clusters
    assert replicator.sync()
    for cluster in clusters:
        record_set_in_cluster = helpers.list_record_sets(yp_clients[cluster])[0]
        helpers.check_record_sets_equal(record_set_in_cluster, record_set)
        assert len(record_set_in_cluster['labels']['changelist']['changes']) == 1
        assert record_set_in_cluster['labels']['changelist']['changes'][0]['replicated']

    # third sync deletes replicated updates
    assert replicator.sync()
    record_sets_after_third_sync = {}
    for cluster in clusters:
        record_sets_after_third_sync[cluster] = record_set_in_cluster = helpers.list_record_sets(yp_clients[cluster])[0]
        helpers.check_record_sets_equal(record_set_in_cluster, record_set)
        assert 'changes' not in record_set_in_cluster['labels']['changelist']

    # fourth sync does not change anything
    assert replicator.sync()
    for cluster in clusters:
        record_set_in_cluster = helpers.list_record_sets(yp_clients[cluster])[0]
        assert record_set_in_cluster == record_sets_after_third_sync[cluster]

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_versions_meta_update(yp_env):
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
        },
    })
    replicator = helpers.create_replicator(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': clusters,
        },
    })

    record_sets = helpers.generate_record_sets(1)
    record_set = record_sets[0]

    # add record set on first cluster
    helpers.add_records(client, [record_set], hints={'cluster': clusters[0]}, check_response={'clusters': [clusters[0]]})

    record_set_in_first_cluster = helpers.list_record_sets(yp_clients[clusters[0]])[0]
    # hash is not managing by Bridge, but by Replicator, so hash has not set yet
    assert 'record_set_hash' not in record_set_in_first_cluster['labels']['changelist']
    # each update on Bridge increases version by 1
    assert record_set_in_first_cluster['labels']['changelist']['version'] == 1
    # no base_versions info since no replications were held
    assert 'base_versions' not in record_set_in_first_cluster['labels']['changelist']

    # first sync
    assert replicator.sync()

    # check record set in first cluster
    record_set_in_first_cluster = helpers.list_record_sets(yp_clients[clusters[0]])[0]
    # hash is set
    assert 'record_set_hash' in record_set_in_first_cluster['labels']['changelist']
    hash_after_first_update = record_set_in_first_cluster['labels']['changelist']['record_set_hash']
    assert len(hash_after_first_update) > 0
    # version reset, since hash is updated (you may think that record set is recreating when hash is changing)
    assert record_set_in_first_cluster['labels']['changelist']['version'] == 0
    # base_versions is not set, because there were no record sets with new hash
    assert 'base_versions' not in record_set_in_first_cluster['labels']['changelist']

    # check record set in second cluster
    record_set_in_second_cluster = helpers.list_record_sets(yp_clients[clusters[1]])[0]
    # hashes are equal
    assert record_set_in_second_cluster['labels']['changelist']['record_set_hash'] == hash_after_first_update
    # version is zero, because no updates were applied by Bridge on this cluster
    assert record_set_in_second_cluster['labels']['changelist']['version'] == 0
    # base_versions is also empty
    assert 'base_versions' not in record_set_in_second_cluster['labels']['changelist']

    # to be continued...

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_update_soa_records(yp_env):
    yp_instances = yp_env
    yp_instances = {cluster: yp_instances[cluster] for cluster in list(yp_instances.keys())[:2]}
    assert len(yp_instances) == 2
    clusters = list(yp_instances.keys())
    yp_clients = {cluster: yp_instances[cluster].create_client() for cluster in clusters}
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': clusters,
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
        },
    })
    replicator = helpers.create_replicator(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': clusters,
        },
    })

    record_requests_by_cluster = {}

    def sync():
        assert replicator.sync(), "failed to run 1st sync: copy updates"
        assert replicator.sync(), "failed to run 2nd sync: set replicated flag"
        assert replicator.sync(), "failed to run 3rd sync: clear changelist"

        nonlocal record_requests_by_cluster
        record_requests_by_cluster = {cluster: api_pb2.TReqUpdateRecords() for cluster in clusters}

    sync()

    # (cluster id, SOA data)
    soa_datas = [
        (0, 'nsx.yp-dns.yandex.net. sysadmin.yandex-team.ru. 0 900 600 604800 120'),
        (0, 'nsx.dns.yandex.net. admin.yandex-team.ru. 1 999 666 1337 90'),
        (1, 'nsx.yandex.net. god.yandex-team.ru. 2 123 999 6000 300'),
        (1, 'nsx.yandex.ru. dog.yandex-team.ru. 3 321 1448 123 987'),
        (0, 'nsx.ydnx.ru. cat.yandex-team.ru. 4 909 112 911 281'),
    ]

    # run SOA record updates and run sync after step number `sync_step`
    for sync_step in range(len(soa_datas)):
        for step, (cluster_idx, soa_data) in enumerate(soa_datas):
            cluster_name = clusters[cluster_idx]
            cluster_record_requests = record_requests_by_cluster[cluster_name]

            record_set = helpers.make_dns_record_set(
                id=helpers.TEST_ZONE_NAME,
                rdtype='SOA',
                data=soa_data,
            )

            add_records_request, _ = helpers.add_records(client, [record_set], hints={'cluster': cluster_name},
                                                         check_response={'clusters': [cluster_name]})
            cluster_record_requests.requests.extend(add_records_request.requests)

            result_record_sets = helpers.list_record_sets(yp_clients[cluster_name])
            expected_record_sets = [record_set]
            helpers.check_record_sets(result_record_sets, expected_record_sets,
                                      check_func=helpers.check_record_sets_with_changelists,
                                      updates_request=cluster_record_requests)

            if step == sync_step:
                sync()

                for yp_client in yp_clients.values():
                    result_record_sets = helpers.list_record_sets(yp_client)
                    expected_record_sets = [record_set]
                    helpers.check_record_sets(result_record_sets, expected_record_sets,
                                              check_func=helpers.check_record_sets_with_changelists,
                                              updates_request=record_requests_by_cluster[cluster_name])

        # replicate changes, so record sets in both clusters are equal
        sync()

        # check that record sets in clusters contain last update
        for cluster_name, yp_client in yp_clients.items():
            result_record_sets = helpers.list_record_sets(yp_client)
            expected_record_sets = [record_set]
            helpers.check_record_sets(result_record_sets, expected_record_sets,
                                      check_func=helpers.check_record_sets_with_changelists,
                                      updates_request=record_requests_by_cluster[cluster_name])

        # now remove SOA record
        record_set = helpers.make_dns_record_set(
            id=helpers.TEST_ZONE_NAME,
            rdtype='SOA',
            data='',
        )
        remove_records_request, _ = helpers.remove_records(client, [record_set], hints={'cluster': cluster_name},
                                                           check_response={'clusters': [cluster_name]})

        result_record_sets = helpers.list_record_sets(yp_client)
        expected_record_sets = [{'meta': {'id': record_set['meta']['id']}, 'spec': {'records': []}}]
        helpers.check_record_sets(result_record_sets, expected_record_sets,
                                  check_func=helpers.check_record_sets_with_changelists,
                                  updates_request=remove_records_request)

        # synchronize, so there are no record sets
        sync()

        for cluster_name, yp_client in yp_clients.items():
            result_record_sets = helpers.list_record_sets(yp_client)
            helpers.check_record_sets(result_record_sets, [],
                                      check_func=helpers.check_record_sets_with_changelists,
                                      updates_request=record_requests_by_cluster[cluster_name])

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_update_cname_records(yp_env):
    yp_instances = yp_env
    yp_instances = {cluster: yp_instances[cluster] for cluster in list(yp_instances.keys())[:2]}
    assert len(yp_instances) == 2
    clusters = list(yp_instances.keys())
    yp_clients = {cluster: yp_instances[cluster].create_client() for cluster in clusters}
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': clusters,
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
        },
    })
    replicator = helpers.create_replicator(yp_instances, zones_config={
        TEST_ZONE_NAME: {
            'Clusters': clusters,
        },
    })

    record_requests_by_cluster = {}

    try:
        def sync():
            assert replicator.sync(), "failed to run 1st sync: copy updates"
            assert replicator.sync(), "failed to run 2nd sync: set replicated flag"
            assert replicator.sync(), "failed to run 3rd sync: clear changelist"

            nonlocal record_requests_by_cluster
            record_requests_by_cluster = {cluster: api_pb2.TReqUpdateRecords() for cluster in clusters}

        sync()

        fqdn = f"fqdn-x.{TEST_ZONE_NAME}"
        steps_params_set = [
            {
                "cluster": clusters[0],
                "update": {"type": "CNAME", "data": f"fqdn-a.{TEST_ZONE_NAME}"},
                "expected": {"type": "CNAME", "data": f"fqdn-a.{TEST_ZONE_NAME}"},
                "expected_in_cluster": {"type": "CNAME", "data": f"fqdn-a.{TEST_ZONE_NAME}"},
            },
            {
                "cluster": clusters[0],
                "update": {"type": "CNAME", "data": f"fqdn-b.{TEST_ZONE_NAME}"},
                "expected": {"type": "CNAME", "data": f"fqdn-b.{TEST_ZONE_NAME}"},
                "expected_in_cluster": {"type": "CNAME", "data": f"fqdn-b.{TEST_ZONE_NAME}"},
            },
            {
                "cluster": clusters[1],
                "update": {"type": "AAAA", "data": "2a02:6b8:c0c:b02:100:0:5d8a:0"},
                "expected": {"type": "AAAA", "data": "2a02:6b8:c0c:b02:100:0:5d8a:0"},
                "expected_in_cluster": {"type": "AAAA", "data": "2a02:6b8:c0c:b02:100:0:5d8a:0"},
            },
            {
                "cluster": clusters[0],
                "update": {"type": "A", "data": "127.0.0.1"},
                "expected": [
                    {"type": "AAAA", "data": "2a02:6b8:c0c:b02:100:0:5d8a:0"},
                    {"type": "A", "data": "127.0.0.1"},
                ]
            },
            {
                "cluster": clusters[1],
                "update": {"type": "CNAME", "data": f"fqdn-c.{TEST_ZONE_NAME}"},
                "expected": {"type": "CNAME", "data": f"fqdn-c.{TEST_ZONE_NAME}"},
                "expected_in_cluster": {"type": "CNAME", "data": f"fqdn-c.{TEST_ZONE_NAME}"},
            },
        ]

        # run record updates and run sync after step number `sync_step`
        for sync_step in range(len(steps_params_set)):
            logging.info(f"Run plan with sync after step {sync_step}")
            for step, step_params in enumerate(steps_params_set):
                logging.info(f"Run step {step}")

                cluster_name = step_params['cluster']
                cluster_record_requests = record_requests_by_cluster[cluster_name]

                update = helpers.make_dns_record_set(
                    id=fqdn,
                    records=step_params['update'],
                )
                expected_target_record_set = helpers.make_dns_record_set(
                    id=fqdn,
                    records=step_params['expected'],
                )
                expected_in_cluster_record_set = helpers.make_dns_record_set(
                    id=fqdn,
                    records=step_params['expected_in_cluster'],
                ) if 'expected_in_cluster' in step_params else None

                add_records_request, _ = helpers.add_records(client, [update], hints={'cluster': cluster_name},
                                                             check_response={'clusters': [cluster_name]})
                cluster_record_requests.requests.extend(add_records_request.requests)

                result_record_sets = helpers.list_record_sets(yp_clients[cluster_name])

                if expected_in_cluster_record_set is not None:
                    helpers.check_record_sets(result_record_sets, [expected_in_cluster_record_set],
                                              check_func=helpers.check_record_sets_with_changelists,
                                              updates_request=cluster_record_requests)

                if step == sync_step:
                    sync()

                    for yp_client in yp_clients.values():
                        result_record_sets = helpers.list_record_sets(yp_client)
                        helpers.check_record_sets(result_record_sets, [expected_target_record_set],
                                                  check_func=helpers.check_record_sets_with_changelists,
                                                  updates_request=record_requests_by_cluster[cluster_name])

            # replicate changes, so record sets in both clusters are equal
            sync()

            # check that record sets in clusters contain last update
            for cluster_name, yp_client in yp_clients.items():
                result_record_sets = helpers.list_record_sets(yp_client)
                helpers.check_record_sets(result_record_sets, [expected_target_record_set],
                                          check_func=helpers.check_record_sets_with_changelists,
                                          updates_request=record_requests_by_cluster[cluster_name])

            # now remove records
            remove = helpers.make_dns_record_set(
                id=fqdn,
                rdtype=steps_params_set[-1]["expected"]["type"],
                data=steps_params_set[-1]["expected"]["data"],
            )
            remove_records_request, _ = helpers.remove_records(client, [remove], hints={'cluster': cluster_name},
                                                               check_response={'clusters': [cluster_name]})

            result_record_sets = helpers.list_record_sets(yp_client)
            expected_record_set = {'meta': {'id': fqdn}, 'spec': {'records': []}}
            helpers.check_record_sets(result_record_sets, [expected_record_set],
                                      check_func=helpers.check_record_sets_with_changelists,
                                      updates_request=remove_records_request)

            # synchronize, so there are no record sets
            sync()

            for cluster_name, yp_client in yp_clients.items():
                result_record_sets = helpers.list_record_sets(yp_client)
                helpers.check_record_sets(result_record_sets, [],
                                          check_func=helpers.check_record_sets_with_changelists,
                                          updates_request=record_requests_by_cluster[cluster_name])
    finally:
        bridge.stop()
