import copy
import functools
import json
import logging
import os
import time
import uuid
import yatest

from infra.yp_dns_api.bridge.api import api_pb2
from infra.yp_dns_api.bridge.daemon import YpDnsApiBridge
from infra.yp_dns_api.client.client import YpDnsApiBridgeClient

from infra.yp_dns_api.tests.helpers.replicator import Replicator

from infra.libs.yp_dns.changelist.proto import changelist_pb2

from library.python import resource


BRIDGE_CONFIG_RESOURCE_NAME = '/proto_config/bridge_config.json'
REPLICATOR_CONFIG_RESOURCE_NAME = '/proto_config/replicator_config.json'

TEST_ZONE_NAME = 'test-zone.yandex.net'

ZONES_CONFIG_EXAMPLE = {
    TEST_ZONE_NAME: {
        'Clusters': [
            'master-1',
        ]
    }
}

DEFAULT_TTL = 1337


class YpDnsApiBridgeTestWrapper(YpDnsApiBridge):
    def wait_update(self):
        def parse_time(value):
            if value.endswith('ms'):
                return float(value[:-2]) / 1000.0
            elif value.endswith('s'):
                return float(value[:-1])

        time.sleep(2 * max(map(
            lambda cluster_config: parse_time(cluster_config['UpdatingFrequency']),
            self.config['YpClusterConfigs']
        )))


def create_bridge(yp_instances, zones_config, start=True, workdir=None):
    config = json.loads(resource.find(BRIDGE_CONFIG_RESOURCE_NAME))

    port_manager = yatest.common.network.PortManager()
    config['AdminHttpServiceConfig']['Port'] = port_manager.get_port()
    bridge_http_port = config['BridgeHttpServiceConfig']['Port'] = port_manager.get_port()
    grpc_port = config['GrpcServiceConfig']['Port'] = port_manager.get_port()
    state_coordinator_port = port_manager.get_port()

    workdir = os.path.join(yatest.common.output_path(), '' if workdir is None else workdir, 'bridge-{}'.format(bridge_http_port))
    os.makedirs(workdir)

    config['LoggerConfig']['Path'] = os.path.join(workdir, 'bridge-service-eventlog')
    config['UpdatableConfigOptions']['ConfigUpdatesLoggerConfig']['Path'] = os.path.join(workdir, 'config-updates-eventlog')
    config['ReplicaLoggerConfig']['Path'] = os.path.join(workdir, 'bridge-replica-dns-record-sets-eventlog')
    config['DynamicZonesConfig']['ZonesManagerServiceConfig']['LoggerConfig']['Path'] = \
        os.path.join(workdir, 'zones-manager-service-eventlog')
    config['DynamicZonesConfig']['ZonesStateCoordinatorConfig']['StateServicesConfig']['BridgeStateServiceConfig']['LoggerConfig']['Path'] = \
        os.path.join(workdir, 'zm-state-service-bridge-eventlog')
    config['DynamicZonesConfig']['PollConfig']['LoggerConfig']['Path'] = \
        os.path.join(workdir, 'poll-dynamic-zones-eventlog')
    zones_manager_config = config['DynamicZonesConfig']['ZonesManagerServiceConfig']['ZonesManagerConfig']
    zones_manager_config['DnsZonesReplicasConfig']['LoggerConfig']['Path'] = os.path.join(workdir, 'bridge-replica-dns-zones-eventlog')
    zones_manager_config['DnsZonesReplicasConfig']['YpReplicaConfig']['StorageConfig']['Path'] = \
        os.path.join(workdir, zones_manager_config['DnsZonesReplicasConfig']['YpReplicaConfig']['StorageConfig'].get('Path', 'storage'))
    zones_manager_config['DnsZonesReplicasConfig']['YpReplicaConfig']['BackupConfig']['Path'] = \
        os.path.join(workdir, zones_manager_config['DnsZonesReplicasConfig']['YpReplicaConfig']['BackupConfig'].get('Path', 'backup'))
    zones_manager_config['ZonesStatesUpdateLoggerConfig']['Path'] = os.path.join(workdir, 'zm-zones-states-update-eventlog')

    config['YpClientConfigs'] = []
    config['ZoneConfigs'] = []
    config['YpClusterConfigs'] = []

    all_clusters = set()
    for zone, zone_config in zones_config.items():
        zone_config['Name'] = zone
        config['ZoneConfigs'].append(zone_config)

        all_clusters |= set(zone_config['Clusters'])

    for cluster in all_clusters:
        yp_instance = yp_instances[cluster]
        config['YpClientConfigs'].append({
            'Name': cluster,
            'Address': yp_instance.yp_client_grpc_address,
            'EnableSsl': False,
            'EnableBalancing': False,
            'Timeout': '80s',
        })
        config['YpClusterConfigs'].append({
            'Name': cluster,
            'Address': yp_instance.yp_client_grpc_address,
            'Balancing': False,
            'EnableSsl': False,
            'Timeout': '5s',
            'UpdatingFrequency': '10ms',
        })

    config['UpdatableConfigOptions']['WatchPatchConfig']['Path'] = os.path.join(workdir, config['UpdatableConfigOptions']['WatchPatchConfig'].get('Path', 'config_patch.json'))
    config['UpdatableConfigOptions']['WatchPatchConfig']['ValidPatchPath'] = os.path.join(workdir, config['UpdatableConfigOptions']['WatchPatchConfig'].get('ValidPatchPath', 'valid_patch.json'))
    config['UpdatableConfigOptions']['WatchPatchConfig']['Frequency'] = '1ms'

    config['YpReplicaConfig']['StorageConfig']['Path'] = os.path.join(workdir, config['YpReplicaConfig']['StorageConfig'].get('Path', 'storage'))
    config['YpReplicaConfig']['BackupConfig']['Path'] = os.path.join(workdir, config['YpReplicaConfig']['BackupConfig'].get('Path', 'backup'))

    # dynamic zones
    yt_proxy = next(iter(yp_instances.values())).yt_instance.get_proxy_address()
    yp_cluster = next(iter(yp_instances.keys()))
    yp_address = next(iter(yp_instances.values())).yp_client_grpc_address
    zones_manager_config['YpAddress'] = yp_address
    zones_manager_config['DnsZonesReplicasConfig']['YpClusterConfig']['Address'] = yp_address
    zones_manager_config['DnsZonesReplicasConfig']['YpClusterConfig']['Name'] = yp_cluster
    zones_manager_config['DnsZonesReplicasConfig']['YpClusterConfig']['EnableSsl'] = False
    config['DynamicZonesConfig']['ZonesStateCoordinatorConfig']['YtProxy'] = yt_proxy
    config['DynamicZonesConfig']['ZonesStateCoordinatorConfig']['GrpcServerConfig']['Port'] = state_coordinator_port
    config['DynamicZonesConfig']['BridgeServiceClientConfig']['StateServiceClientConfig']['Address'] = 'localhost:{}'.format(state_coordinator_port)
    config['DynamicZonesConfig']['BridgeServiceClientConfig']['StateServiceClientConfig']['Registration']['YtProxy'] = yt_proxy
    config['DynamicZonesConfig']['BridgeServiceClientConfig']['StateServiceClientConfig']['ReportConfiguration']['YtProxy'] = yt_proxy
    config['DynamicZonesConfig']['ZonesManagerClientConfig']['Address'] = 'localhost:{}'.format(grpc_port)

    bridge = YpDnsApiBridgeTestWrapper(config)
    if start:
        bridge.start()

    client = YpDnsApiBridgeClient('localhost:{}'.format(bridge.grpc_port))
    return bridge, client


def create_replicator(yp_instances, zones_config, workdir=None):
    config = json.loads(resource.find(REPLICATOR_CONFIG_RESOURCE_NAME))

    workdir = os.path.join(yatest.common.output_path(), '' if workdir is None else workdir, 'replicator-{}'.format(uuid.uuid4()))
    os.makedirs(workdir)

    all_clusters = set().union(*map(lambda zone_config: set(zone_config['Clusters']), zones_config.values()))

    config['Controller']['Logger']['Path'] = os.path.join(workdir, 'replicator-eventlog')
    config['Controller']['Logger']['RotatePath'] = os.path.join(workdir, 'replicator-eventlog.1')
    config['Controller']['LeadingInvader']['Path'] = '//home'
    any_cluster = next(iter(all_clusters))

    del config['ShardsZonesDistribution']

    config['Controller']['LeadingInvader']['Proxy'] = yp_instances[any_cluster].create_yt_client().config["proxy"]["url"]
    config['ZonesReplicationConfigs'] = [
        {
            'GroupName': 'test',
            'ClusterConfigs': [
                {
                    'ClusterName': cluster,
                    'Address': yp_instances[cluster].yp_client_grpc_address,
                    'EnableSsl': False,
                } for cluster in all_clusters
            ],
            'ZoneReplicatorConfigs': [
                zone_config | {'Zone': zone_name}
                for zone_name, zone_config in zones_config.items()
            ]
        }
    ]
    logging.info('Replicator config: {}'.format(json.dumps(config)))

    return Replicator(config)


def apply_patch(bridge, patch):
    patch_path = bridge.config["UpdatableConfigOptions"]["WatchPatchConfig"]["Path"]
    os.makedirs(os.path.dirname(patch_path), exist_ok=True)

    with open(patch_path, 'w') as patch_file:
        patch = {
            "dont_apply": False,
            "patch": patch,
        }
        json.dump(patch, patch_file)

    time.sleep(1)


def merge_record_set_lists(record_sets_lists):
    def iter_union(lhs, rhs):
        for key in set(lhs).union(rhs):
            yield key, lhs.get(key), rhs.get(key)

    def merge_rs(lhs, rhs):
        if lhs is None or rhs is None:
            return copy.deepcopy(lhs or rhs)
        result = copy.deepcopy(lhs)
        result['spec']['records'] = list({
            dict(sorted(rec.items())).values(): rec
            for rec in lhs['spec'].get('records', []) + rhs['spec'].get('records', [])
        }.values())
        return result

    def merge(lhs, rhs):
        result = []
        lhs_map = {rs['meta']['id']: rs for rs in lhs}
        rhs_map = {rs['meta']['id']: rs for rs in rhs}
        for key, lhs_rs, rhs_rs in iter_union(lhs_map, rhs_map):
            merged_rs = merge_rs(lhs_rs, rhs_rs)
            if merged_rs is not None:
                result.append(merged_rs)
        return result

    return functools.reduce(merge, record_sets_lists, [])


def make_dns_record_set(id, rdtype=None, data=None, rdclass='IN', records=[], zone=None):
    record_set = {
        'meta': {'id': id},
        'spec': {
            'records': [],
        },
        'labels': {
        },
    }
    if rdtype is not None:
        record_set['spec']['records'].extend(list(map(
            lambda value: {'class': rdclass, 'type': rdtype, 'data': value},
            [data] if not isinstance(data, list) else data
        )))

    if isinstance(records, list):
        record_set['spec']['records'].extend(map(lambda record: {'class': 'IN'} | record, records))
    else:
        record_set['spec']['records'].append({'class': 'IN'} | records)

    if zone is not None:
        record_set['labels']['zone'] = zone

    return record_set


def make_dns_record_sets(id_to_data, rdtype, rdclass='IN', zone=None):
    return tuple(map(
        lambda kv: ('dns_record_set', make_dns_record_set(kv[0], rdtype, kv[1], rdclass, zone=zone)),
        id_to_data.items()
    ))


def generate_record_sets(record_sets_num, records_num=1, zone=TEST_ZONE_NAME, save_object_type=False, seed='5d8a'):
    return list(map(lambda record_set: record_set if save_object_type else record_set[1],
                    make_dns_record_sets({
                        'fqdn-{}.{}'.format(i, zone): [
                            '2a02:6b8:c0c:b02:100:0:{}:{}'.format(seed, j)
                            for j in range(records_num)
                        ] for i in range(record_sets_num)
                    }, 'AAAA', zone=zone)))


def update_records(bridge_client, request):
    return bridge_client.update_records(request)


def add_records(bridge_client, record_sets, hints={}, check_response=None):
    request = api_pb2.TReqUpdateRecords()
    for record_set in record_sets:
        for record in record_set['spec']['records']:
            record_request = request.requests.add()
            record_request.update.fqdn = record_set['meta']['id']
            record_request.update.type = api_pb2.ERecordType.Value(record['type'])
            record_request.update.data = record['data']
            if 'ttl' in record:
                record_request.update.ttl = record['ttl']
            if 'class' in record:
                setattr(record_request.update, 'class', record['class'])

            if hints.get('cluster'):
                record_request.hints.cluster = hints['cluster']

    update_records_response = update_records(bridge_client, request)

    if check_response:
        for response in update_records_response.responses:
            assert response.WhichOneof('response') == 'update'
            assert response.update.status == api_pb2.TRspUpdateRecord.EUpdateRecordStatus.OK, response.update.error_message
            assert not response.update.error_message

            if not isinstance(check_response, dict):
                continue
            assert 'clusters' not in check_response or response.update.cluster in check_response['clusters']

    return request, update_records_response


def remove_records(bridge_client, record_sets, hints={}, check_response=None):
    request = api_pb2.TReqUpdateRecords()
    for record_set in record_sets:
        for record in record_set['spec']['records']:
            record_request = request.requests.add()
            record_request.remove.fqdn = record_set['meta']['id']
            record_request.remove.type = api_pb2.ERecordType.Value(record['type'])
            record_request.remove.data = record['data']

            if hints.get('cluster'):
                record_request.hints.cluster = hints['cluster']

    remove_records_response = update_records(bridge_client, request)

    if check_response:
        for response in remove_records_response.responses:
            assert response.WhichOneof('response') == 'remove'
            assert response.remove.status == api_pb2.TRspRemoveRecord.ERemoveRecordStatus.OK, response.remove.error_message
            assert not response.remove.error_message

            if not isinstance(check_response, dict):
                continue
            assert 'clusters' not in check_response or response.remove.cluster in check_response['clusters']

    return request, remove_records_response


def list_record_sets(yp_client, filter=None):
    continuation_token = None
    limit = 100
    timestamp = yp_client.generate_timestamp()

    result = []
    while True:
        chunk = yp_client.select_objects(
            'dns_record_set',
            selectors=[
                '/meta',
                '/spec',
                '/labels',
            ],
            filter=filter,
            limit=limit,
            timestamp=timestamp,
            options={'continuation_token': continuation_token},
            enable_structured_response=True,
        )
        continuation_token = chunk['continuation_token']

        result.extend(list(map(
            lambda r: {'meta': r[0]['value'], 'spec': r[1]['value'], 'labels': r[2]['value']},
            chunk['results']
        )))

        if len(chunk['results']) < limit:
            break

    return result


def records_equal(lhs, rhs):
    def unique_records(records):
        if not records:
            return []

        result = {}
        for record in records:
            result[(record['type'], record['class'], record['data'])] = record
        return list(result.values())

    def sort_records(records):
        return sorted(records, key=lambda record: (record.get('class', ''), record['type'], record['data']))

    if len(lhs) != len(rhs):
        return False

    lhs = sort_records(unique_records(lhs))
    rhs = sort_records(unique_records(rhs))
    for lhs_record, rhs_record in zip(lhs, rhs):
        for key, value in lhs_record.items():
            if key in rhs_record:
                if value != rhs_record[key]:
                    return False
    return True


def check_record_sets_equal(actual, expected):
    if expected is None:
        assert actual['spec'].get('records', []) == []
        return

    assert actual['meta']['id'] == expected['meta']['id']

    expected_records = expected['spec'].get('records', [])
    actual_records = actual['spec'].get('records', [])
    assert records_equal(expected_records, actual_records), \
        f"expected {expected_records}, found {actual_records}"


def find_changelist_entry(changes, record_request):
    if record_request.WhichOneof('request') == 'update':
        key = (
            changelist_pb2.TRecordUpdateRequest.EUpdateType.UPDATE,
            record_request.update.fqdn,
            record_request.update.type,
            record_request.update.data,
        )
    elif record_request.WhichOneof('request') == 'remove':
        key = (
            changelist_pb2.TRecordUpdateRequest.EUpdateType.REMOVE,
            record_request.remove.fqdn,
            record_request.remove.type,
            record_request.remove.data,
        )

    for idx, entry in enumerate(changes):
        entry_key = (
            entry['record_update_request']['type'],
            entry['record_update_request']['content']['fqdn'],
            entry['record_update_request']['content']['type'],
            entry['record_update_request']['content']['data'],
        )
        if key == entry_key:
            yield idx, entry


def check_record_sets(actual_record_sets, expected_record_sets, check_func, updates_request=None):
    expected_by_ids = {record_set['meta']['id']: record_set for record_set in expected_record_sets}
    actual_by_ids = {record_set['meta']['id']: record_set for record_set in actual_record_sets}

    assert set(expected_by_ids.keys()).issubset(set(actual_by_ids.keys()))

    for id, actual in actual_by_ids.items():
        expected = expected_by_ids.get(id)
        if updates_request is not None:
            check_func(actual, expected, updates_request)
        else:
            check_func(actual, expected)


def check_record_sets_with_changelists(actual, expected, update_records_request):
    check_record_sets_equal(actual, expected)

    requests = []
    for record_request in update_records_request.requests:
        fqdn = None
        if record_request.WhichOneof('request') == 'update':
            fqdn = record_request.update.fqdn
        elif record_request.WhichOneof('request') == 'remove':
            fqdn = record_request.remove.fqdn

        if fqdn == actual['meta']['id']:
            requests.append(record_request)

    assert 'changelist' in actual['labels']
    changelist = actual['labels']['changelist'].copy()
    changes = changelist.get('changes', [])
    assert len(requests) == len(changes)
    assert len(changes) == len(set(map(lambda entry: entry['uuid'], changes)))
    for request in requests:
        cl_entries = find_changelist_entry(changes, request)
        cl_entry = next(cl_entries, None)

        assert cl_entry is not None
        idx, cl_entry = cl_entry

        assert len(cl_entry['uuid']) > 0
        assert cl_entry['timestamp'] < time.time() * 1000 * 1000
        assert not cl_entry['replicated']

        change_value = cl_entry['record_update_request']
        if request.WhichOneof('request') == 'update':
            assert change_value['type'] == changelist_pb2.TRecordUpdateRequest.EUpdateType.UPDATE
            assert change_value['content']['fqdn'] == request.update.fqdn
            assert change_value['content']['type'] == request.update.type
            assert change_value['content']['data'] == request.update.data
            assert change_value['content']['class'] == getattr(request.update, 'class') or 'IN'
            assert change_value['content']['ttl'] == request.update.ttl or DEFAULT_TTL
        elif request.WhichOneof('request') == 'remove':
            assert change_value['type'] == changelist_pb2.TRecordUpdateRequest.EUpdateType.REMOVE
            assert change_value['content']['fqdn'] == request.remove.fqdn
            assert change_value['content']['type'] == request.remove.type
            assert change_value['content']['data'] == request.remove.data
            assert 'class' not in change_value['content']
            assert 'ttl' not in change_value['content']

        changes.pop(idx)

    assert len(changes) == 0


def sort_dns_record_sets(record_sets):
    return sorted(map(
        lambda v: v[1] if isinstance(v, tuple) else v, record_sets
    ), key=lambda value: value['meta']['id'])


def sort_records(records):
    if not records:
        return []
    if isinstance(records[0], dict):
        return sorted(records, key=lambda record: (record['class'], record['type'], record['data']))
    else:
        return sorted(records, key=lambda record: (getattr(record, 'class'), record.type, record.data))


def assert_records_equal(actual, expected, options={}):
    assert actual.ttl == expected.get('ttl', options.get('default_ttl', actual.ttl))
    assert getattr(actual, 'class') == expected['class']
    assert actual.type == expected['type']
    assert actual.data == expected['data']


def assert_record_sets_equal(actual, expected, options={}):
    assert actual.id == expected['meta']['id']
    assert len(actual.records) == len(expected['spec']['records'])
    actual_records = sort_records(actual.records) if options.get('sort_records', True) else actual.records
    expected_records = sort_records(expected['spec']['records']) if options.get('sort_records', True) else expected['spec']['records']
    for actual_record, expected_record in zip(actual_records, expected_records):
        assert_records_equal(actual_record, expected_record, options=options)


def check_list_zone_record_sets_response(response, expected, expected_status=api_pb2.TRspListZoneRecordSets.OK, options={}):
    expected = sort_dns_record_sets(expected)
    assert response.status == expected_status
    assert len(response.record_sets) == len(expected)

    if response.status == api_pb2.TRspListZoneRecordSets.OK:
        assert len(response.yp_timestamps) > 0 and all(map(lambda value: value > 0, dict(response.yp_timestamps).values()))
    else:
        assert len(response.yp_timestamps) == 0

    for actual, expected in zip(response.record_sets, expected):
        assert_record_sets_equal(actual, expected, options=options)

    if response.record_sets:
        assert response.continuation_token != ''
    else:
        assert response.continuation_token == ''


def check_list_zone_record_sets(bridge_client, zone, expected_record_sets,
                                expected_record_sets_before_update=None, limit=None, continuation_token=None,
                                timestamps={}, expected_status=api_pb2.TRspListZoneRecordSets.OK):
    while True:
        resp = bridge_client.list_zone_record_sets(zone, limit=limit, continuation_token=continuation_token)

        if (len(resp.yp_timestamps) > 0 or resp.status == api_pb2.TRspListZoneRecordSets.OK) and \
                any(map(lambda kv: kv[1] < timestamps.get(kv[0], 0), dict(resp.yp_timestamps).items())):
            if expected_record_sets_before_update is not None:
                check_list_zone_record_sets_response(resp, expected_record_sets_before_update, expected_status=expected_status)
        else:
            check_list_zone_record_sets_response(resp, expected_record_sets, expected_status=expected_status)
            return resp
