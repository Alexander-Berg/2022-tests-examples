import helpers
import local_yp
import pytest
import time

from infra.yp_dns_api.bridge.api import api_pb2


@pytest.mark.usefixtures("yp_env")
def test_update_records(yp_env):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'MaxReplicaAgeForLookup': '1ms',
        },
    })
    yp_client = yp_instance.create_client()

    record_sets = helpers.generate_record_sets(record_sets_num=10)

    def check_func(actual, expected):
        helpers.check_record_sets_equal(actual, expected)
        assert actual['labels']['zone'] == helpers.TEST_ZONE_NAME
        assert 'changelist' not in actual['labels']

    batch_size = 4
    for iter in range(2):
        for i in range(0, len(record_sets), batch_size):
            batch = record_sets[i:i + batch_size]

            _, add_records_response = helpers.add_records(client, batch,
                                                          check_response={'clusters': [cluster_name]})
            result_record_sets = helpers.list_record_sets(yp_client)
            helpers.check_record_sets(result_record_sets, record_sets[:i + batch_size] if iter == 0 else record_sets, check_func=check_func)

    batch_size = 3
    for iter in range(2):
        for i in range(0, len(record_sets), batch_size):
            batch = record_sets[i:i + batch_size]
            _, remove_records_response = helpers.remove_records(client, batch,
                                                                check_response={'clusters': [cluster_name]})

            result_record_sets = helpers.list_record_sets(yp_client)
            helpers.check_record_sets(result_record_sets, record_sets[i + batch_size:] if iter == 0 else [], check_func=check_func)

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_set_owners(yp_env):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    yp_client = yp_instance.create_client()

    users = ['root'] + ['user{}'.format(i) for i in range(5)]

    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'Owners': users,
            'MaxReplicaAgeForLookup': '1ms',
        },
    })

    for user in users:
        if user == 'root':
            continue
        yp_client.create_object('user', attributes={
            'meta': {
                'id': user,
            },
        })
    local_yp.sync_access_control(yp_instance)

    record_sets = helpers.generate_record_sets(record_sets_num=10)

    def check_func(actual, expected):
        assert actual['meta']['acl'] == [
            {
                'action': 'allow',
                'subjects': users,
                'permissions': [
                    'read',
                    'write',
                ],
            },
        ]

    batch_size = 4
    for iter in range(2):
        for i in range(0, len(record_sets), batch_size):
            batch = record_sets[i:i + batch_size]

            _, add_records_response = helpers.add_records(client, batch,
                                                          check_response={'clusters': [cluster_name]})
            result_record_sets = helpers.list_record_sets(yp_client)
            helpers.check_record_sets(result_record_sets, record_sets[:i + batch_size] if iter == 0 else record_sets, check_func=check_func)

    batch_size = 3
    for iter in range(2):
        for i in range(0, len(record_sets), batch_size):
            batch = record_sets[i:i + batch_size]
            _, remove_records_response = helpers.remove_records(client, batch,
                                                                check_response={'clusters': [cluster_name]})

            result_record_sets = helpers.list_record_sets(yp_client)
            helpers.check_record_sets(result_record_sets, record_sets[i + batch_size:] if iter == 0 else [], check_func=check_func)

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_write_updates_to_changelist(yp_env):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'DefaultTtl': helpers.DEFAULT_TTL,
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
        },
    })
    yp_client = yp_instance.create_client()

    records_num = 2
    record_sets = helpers.generate_record_sets(record_sets_num=10, records_num=records_num)

    batch_size = 4
    all_record_requests = api_pb2.TReqUpdateRecords()
    for iter in range(2):
        for i in range(0, len(record_sets), batch_size):
            batch = record_sets[i:i + batch_size]

            add_records_request, add_records_response = helpers.add_records(client, batch,
                                                                            check_response={'clusters': [cluster_name]})
            all_record_requests.requests.extend(add_records_request.requests)

            result_record_sets = helpers.list_record_sets(yp_client)
            helpers.check_record_sets(result_record_sets, record_sets[:i + batch_size] if iter == 0 else record_sets,
                                      check_func=helpers.check_record_sets_with_changelists, updates_request=all_record_requests)

    batch_size = 3
    for iter in range(2):
        for i in range(0, len(record_sets), batch_size):
            batch = record_sets[i:i + batch_size]
            remove_records_request, remove_records_response = helpers.remove_records(client, batch,
                                                                                     check_response={'clusters': [cluster_name]})
            all_record_requests.requests.extend(remove_records_request.requests)

            result_record_sets = helpers.list_record_sets(yp_client)
            helpers.check_record_sets(result_record_sets, record_sets[i + batch_size:] if iter == 0 else [],
                                      check_func=helpers.check_record_sets_with_changelists, updates_request=all_record_requests)

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_remove_record_with_enabled_changelists(yp_env):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'DefaultTtl': helpers.DEFAULT_TTL,
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
        },
    })
    yp_client = yp_instance.create_client()

    record_sets = helpers.generate_record_sets(record_sets_num=2, records_num=2)
    remove_records_request, _ = helpers.remove_records(client, record_sets, check_response={'clusters': [cluster_name]})

    result_record_sets = helpers.list_record_sets(yp_client)
    expected_record_sets = [{'meta': {'id': record_set['meta']['id']}, 'spec': {'records': []}} for record_set in record_sets]
    helpers.check_record_sets(result_record_sets, expected_record_sets, check_func=helpers.check_record_sets_with_changelists,
                              updates_request=remove_records_request)

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_update_records_in_zone_disabled(yp_env):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'DefaultTtl': helpers.DEFAULT_TTL,
            'MaxReplicaAgeForLookup': '1ms',
            'AllowUpdateRecords': False,
        },
    })
    yp_client = yp_instance.create_client()

    record_sets = helpers.generate_record_sets(record_sets_num=2, records_num=2)
    update_records_request, update_records_response = helpers.add_records(client, record_sets)

    for response in update_records_response.responses:
        assert response.update.status == api_pb2.TRspUpdateRecord.EUpdateRecordStatus.ZONE_UPDATES_DISABLED, response.update.error_message

    result_record_sets = helpers.list_record_sets(yp_client)
    helpers.check_record_sets(result_record_sets, [], check_func=helpers.check_record_sets_equal)

    bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_update_records_changelist_overflow(yp_env):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 0,
            'MaxChangelistSize': 2
        },
    })
    yp_client = yp_instance.create_client()

    try:
        def check_func(actual, expected):
            helpers.check_record_sets_equal(actual, expected)
            assert actual['labels']['zone'] == helpers.TEST_ZONE_NAME
            assert 'changelist' in actual['labels']

        record_sets = helpers.generate_record_sets(record_sets_num=2, records_num=3)
        update_records_request, update_records_response = helpers.add_records(client, record_sets)
        for response in update_records_response.responses:
            assert response.update.status == api_pb2.TRspUpdateRecord.EUpdateRecordStatus.OK, response.update.error_message

        helpers.check_record_sets(helpers.list_record_sets(yp_client), record_sets, check_func=check_func)

        remove_records_request, remove_records_response = helpers.remove_records(client, record_sets)
        for response in remove_records_response.responses:
            assert response.remove.status == api_pb2.TRspRemoveRecord.ERemoveRecordStatus.CHANGELIST_OVERFLOW, response.remove.error_message

        helpers.check_record_sets(helpers.list_record_sets(yp_client), record_sets, check_func=check_func)
    finally:
        bridge.stop()


@pytest.mark.usefixtures("yp_env")
def test_update_records_throttled(yp_env):
    yp_instances = yp_env
    cluster_name, yp_instance = list(yp_instances.items())[0]
    bridge, client = helpers.create_bridge(yp_instances, zones_config={
        helpers.TEST_ZONE_NAME: {
            'Clusters': [
                cluster_name,
            ],
            'MaxReplicaAgeForLookup': '1ms',
            'WriteToChangelist': True,
            'MaxNumberChangesInRecordSetPerMinute': 2
        },
    })
    yp_client = yp_instance.create_client()

    try:
        def check_func(actual, expected):
            helpers.check_record_sets_equal(actual, expected)
            assert actual['labels']['zone'] == helpers.TEST_ZONE_NAME
            assert 'changelist' in actual['labels']

        record_sets = helpers.generate_record_sets(record_sets_num=2, records_num=3)
        update_records_request, update_records_response = helpers.add_records(client, record_sets)
        for response in update_records_response.responses:
            assert response.update.status == api_pb2.TRspUpdateRecord.EUpdateRecordStatus.OK, response.update.error_message

        helpers.check_record_sets(helpers.list_record_sets(yp_client), record_sets, check_func=check_func)

        remove_records_request, remove_records_response = helpers.remove_records(client, record_sets)
        for response in remove_records_response.responses:
            assert response.remove.status == api_pb2.TRspRemoveRecord.ERemoveRecordStatus.REQUEST_THROTTLED, response.remove.error_message

        helpers.check_record_sets(helpers.list_record_sets(yp_client), record_sets, check_func=check_func)

        time.sleep(55)

        remove_records_request, remove_records_response = helpers.remove_records(client, record_sets)
        for response in remove_records_response.responses:
            assert response.remove.status == api_pb2.TRspRemoveRecord.ERemoveRecordStatus.REQUEST_THROTTLED, response.remove.error_message

        helpers.check_record_sets(helpers.list_record_sets(yp_client), record_sets, check_func=check_func)

        time.sleep(6)

        remove_records_request, remove_records_response = helpers.remove_records(client, record_sets)
        for response in remove_records_response.responses:
            assert response.remove.status == api_pb2.TRspRemoveRecord.ERemoveRecordStatus.OK, response.remove.error_message

        result_record_sets = helpers.list_record_sets(yp_client)
        helpers.check_record_sets(result_record_sets, [], check_func=check_func)
    finally:
        bridge.stop()
