# encoding: utf-8

import json

import msgpack

from collections import defaultdict

import infra.yasm.interfaces.internal.subscription_store_pb2 as subscription_store_pb2
import infra.yasm.interfaces.internal.tsdb_pb2 as tsdb_pb2
import infra.yasm.interfaces.internal.agent_pb2 as agent_pb2

from cppzoom import (
    ZTaggedMetricManager, ZRequesterPipeline, ZMiddlePipeline, ZServerPipeline,
    ZAggregationRules, ZCommonRules, ZInMemoryMessagePusher,
    loads_middle_message, loads_server_message,
    ZSubscriptionStoreProtobufRequestSerializer
)


def from_message(body):
    return [msgpack.unpackb(msg) for msg in body]


def get_middle_messages(pipeline):
    result = {}
    for x in pipeline.get_middle_messages():
        result.update(loads_middle_message(x).get_values())
    return result


def get_server_message(pipeline):
    return {host: tagged_record.get_values()
            for host, tagged_record in loads_server_message(pipeline.get_server_message()).items()}


def get_tsdb_message(pusher):
    result = []
    for message in pusher.get_messages():
        request = tsdb_pb2.TTsdbPushSignalRequest()
        request.ParseFromString(message)
        host_name_table = request.HostNameTable
        instance_key_table = request.InstanceKeyTable
        signal_name_table = request.SignalNameTable
        for host_record in request.HostRecords:
            values = {}
            for tag_record in host_record.TagRecords:
                signals = values[instance_key_table.Name[tag_record.InstanceKey.Index]] = {}
                for signal_value in tag_record.Record.Values:
                    signals[signal_name_table.Name[signal_value.SignalName.Index]] = signal_value.Value.FloatValue.Value
            result.append({
                'time': host_record.Timestamp,
                'host': host_name_table.Name[host_record.Host.Index] if not host_record.IsGroup else None,
                'group': host_name_table.Name[host_record.Group.Index],
                'values': values
            })
    return sorted(result, key=lambda x: x['host'])


def test_itype_validation(yasm_conf):
    pushers = [ZInMemoryMessagePusher(), ZInMemoryMessagePusher(), ZInMemoryMessagePusher()]
    aggr_rules = ZAggregationRules()
    common_rules = ZCommonRules()
    metric_manager = ZTaggedMetricManager()
    pipeline = ZRequesterPipeline(yasm_conf, pushers, aggr_rules, common_rules, "SAS.001", 1)
    pipeline.set_metric_manager(metric_manager)
    pipeline.set_time(17)

    agent_response = json.dumps({
        "status": "ok",
        "aggr": [
            ["correct", "|ctype,geo,prj,tier", {"signal_one_summ": 42}],
            ["incorrect@symbol", "|ctype,geo,prj,tier", {"signal_one_summ": 42}],
            ["x" * 55, "|ctype,geo,prj,tier", {"signal_one_summ": 42}],
        ],
    })
    pipeline.add_host_response("sas1-1234", agent_response, "application/json")
    stats_and_errors = pipeline.finish()
    assert 1 == stats_and_errors.hosts_success

    values_dict = loads_middle_message(pipeline.get_middle_messages()[0]).get_values()
    assert values_dict.keys() == ["correct|group=SAS.001|ctype,geo,host,prj,tier"]


def test_protobuf_instance_key_validation(yasm_conf):
    pushers = [ZInMemoryMessagePusher(), ZInMemoryMessagePusher()]
    aggr_rules = ZAggregationRules()
    common_rules = ZCommonRules()
    metric_manager = ZTaggedMetricManager()
    pipeline = ZRequesterPipeline(yasm_conf, pushers, aggr_rules, common_rules, "SAS.001", 1)
    pipeline.set_metric_manager(metric_manager)
    pipeline.set_time(17)

    proto_response = agent_pb2.TAgentResponse()
    proto_response.Status.Status = agent_pb2.STATUS_OK
    signals = ["signal_one_summ"]
    instance_keys = [
        "correct-itype|ctype=prod;geo=sas",
        "-incorrect-itype|ctype=prod;geo=sas",
        "correct-itype|correct_tag=correct-tag-value;ctype=prod;geo=sas",
        "correct-itype|-incorrect-tag=correct-tag-value;ctype=prod;geo=sas",
        "correct-itype|correct_tag=-incorrect-tag-value;ctype=prod;geo=sas",
        "correct-itype|-incorrect-tag=-incorrect-tag-value;ctype=prod;geo=sas"
    ]
    proto_response.AggregatedRecords.SignalNameTable.Name.extend(signals)
    proto_response.AggregatedRecords.InstanceKeyTable.Name.extend(instance_keys)
    for key_idx in xrange(len(instance_keys)):
        instance_key_record = proto_response.AggregatedRecords.Records.add()
        instance_key_record.InstanceKey.Index = key_idx
        for signal_idx in xrange(len(signals)):
            record_value = instance_key_record.Record.Values.add()
            record_value.SignalName.Index = signal_idx
            record_value.Value.FloatValue.Value = 42

    pipeline.add_host_response("sas1-1234", proto_response.SerializeToString(), "application/x-protobuf")
    stats_and_errors = pipeline.finish()
    assert 4 == stats_and_errors.ignored_signals
    assert 1 == stats_and_errors.hosts_success
    assert 0 == stats_and_errors.non_proto_responses
    values_dict = loads_middle_message(pipeline.get_middle_messages()[0]).get_values()
    assert set(values_dict.keys()) == {
        "correct-itype|ctype=prod;geo=sas;group=SAS.001|host",
        "correct-itype|correct_tag=correct-tag-value;ctype=prod;geo=sas;group=SAS.001|host"
    }


def test_requester_pipeline(yasm_conf):
    aggregation_rules = ZAggregationRules()
    common_rules = ZCommonRules()
    common_rules.add_rule("itype=apphost")
    common_rules.add_rule("itype=base", "signal=*_common_????")
    pusher = ZInMemoryMessagePusher()
    pipeline = ZRequesterPipeline(yasm_conf, [pusher], aggregation_rules, common_rules, "SAS.001", 2)
    metrics = ZTaggedMetricManager()
    pipeline.set_metric_manager(metrics)
    pipeline.set_subscriptions(
        [
            {"host": "sas1-1234", "tags": "itype=apphost", "signal": "signal_two_summ"},
            {"host": "sas1-1234", "tags": "itype=apphost", "signal": "cpu_common_summ"},
            {"host": "sas1-1234", "tags": "itype=apphost", "signal": "cpu_clashed_summ"}
        ]
    )
    pipeline.set_time(17)
    response = json.dumps({
        "status": "ok",
        "aggr": [
            ["qloud", "|ctype,geo,prj,tier", {"signal_one_summ": 42}],
            ["base", "|ctype,geo,prj,tier", {"ace_of_base_summ": 1337}],
            ["apphost", "|ctype,geo,prj,tier", {"signal_two_summ": 24, 'cpu_clashed_summ': 57}],
            ["common", "|ctype,geo,prj,tier", {"cpu_common_summ": 123, 'cpu_clashed_summ': 75, 'trashed_summ': 21}],
        ],
    })
    pipeline.add_host_response("sas1-1234", response, "no-protobuf")
    stats = pipeline.finish()

    assert stats.hosts_success == 1
    assert len(stats.deserialize_errors) == 0
    assert len(stats.aggregate_errors) == 0
    assert len(stats.invalid_response_hosts) == 0
    assert stats.ignored_signals == 0
    assert stats.non_proto_responses == 1

    assert get_tsdb_message(pusher) == [{
        'host': 'sas1-1234',
        'group': 'SAS.001',
        'time': 17,
        'values': {
            'apphost|group=SAS.001;host=sas1-1234|ctype,geo,prj,tier': {'signal_two_summ': 24.0, 'cpu_clashed_summ': 57.0, 'cpu_common_summ': 123.0},
            'qloud|group=SAS.001;host=sas1-1234|ctype,geo,prj,tier': {'signal_one_summ': 42.0},
            'common|group=SAS.001;host=sas1-1234|ctype,geo,prj,tier': {'cpu_common_summ': 123.0, 'cpu_clashed_summ': 75.0, 'trashed_summ': 21.0},
            'base|group=SAS.001;host=sas1-1234|ctype,geo,prj,tier': {"ace_of_base_summ": 1337.0, "cpu_common_summ": 123.0}
        }
    }]
    assert get_middle_messages(pipeline) == {
        'common|group=SAS.001|ctype,geo,host,prj,tier': {'cpu_common_summ': 123.0, 'cpu_clashed_summ': 75.0, 'trashed_summ': 21.0},
        'qloud|group=SAS.001|ctype,geo,host,prj,tier': {'signal_one_summ': 42.0},
        'apphost|group=SAS.001|ctype,geo,host,prj,tier': {'signal_two_summ': 24.0, 'cpu_common_summ': 123.0, 'cpu_clashed_summ': 57.0},
        'base|group=SAS.001|ctype,geo,host,prj,tier': {"ace_of_base_summ": 1337.0, "cpu_common_summ": 123.0}
    }
    assert get_server_message(pipeline) == {
        'sas1-1234': {
            'apphost|group=SAS.001;host=sas1-1234|ctype,geo,prj,tier': {
                'signal_two_summ': 24.0,
                'cpu_common_summ': 123.0,
                'cpu_clashed_summ': 57.0
            }
        }
    }

    assert {k: v['totalPtsMetric'] for k, v in metrics.to_dict().items()} == {
        'apphost': 3,
        'base': 2,
        'common': 3,
        'qloud': 1
    }

    pusher.clean()
    pipeline.clean()
    pipeline.finish()

    assert get_tsdb_message(pusher) == []
    assert get_middle_messages(pipeline) == {
        'common|group=SAS.001|ctype,geo,host,prj,tier': {'cpu_common_summ': 0.0, 'cpu_clashed_summ': 0.0, 'trashed_summ': 0},
        'qloud|group=SAS.001|ctype,geo,host,prj,tier': {'signal_one_summ': 0.0},
        'apphost|group=SAS.001|ctype,geo,host,prj,tier': {'signal_two_summ': 0.0, 'cpu_common_summ': 0.0, 'cpu_clashed_summ': 0.0},
        'base|group=SAS.001|ctype,geo,host,prj,tier': {"ace_of_base_summ": 0.0, "cpu_common_summ": 0.0}
    }
    assert get_server_message(pipeline) == {}


def test_middle_pipeline(yasm_conf):
    pusher = ZInMemoryMessagePusher()
    pipeline = ZMiddlePipeline(yasm_conf, pusher, 'SAS.001', 17)
    record = loads_middle_message(''.join(msgpack.dumps(x) for x in (
        ['apphost||ctype,geo,prj,tier', {'signal_two_summ': 24.0}],
        ['qloud||ctype,geo,prj,tier', {'signal_one_summ': 42.0}]
    )))
    metrics = ZTaggedMetricManager()

    for _ in xrange(2):
        pipeline.mul_tagged_record(record, metrics)
    pipeline.set_subscriptions([{"host": "SAS.001", "tags": "itype=apphost", "signal": "signal_two_summ"}])
    pipeline.finish()
    pipeline.send_tsdb_messages()

    assert get_tsdb_message(pusher) == [{
        'host': None,
        'group': 'SAS.001',
        'time': 17,
        'values': {
            'apphost||ctype,geo,prj,tier': {'signal_two_summ': 48.0},
            'qloud||ctype,geo,prj,tier': {'signal_one_summ': 84.0}
        }
    }]
    assert get_server_message(pipeline) == {
        'SAS.001': {'apphost||ctype,geo,prj,tier': {'signal_two_summ': 48.0}}
    }

    pusher.clean()

    pipeline.clean()
    pipeline.finish()
    pipeline.send_tsdb_messages()

    assert get_tsdb_message(pusher) == [{
        'host': None,
        'group': 'SAS.001',
        'time': 17,
        'values': {
            'apphost||ctype,geo,prj,tier': {'signal_two_summ': 0.0},
            'qloud||ctype,geo,prj,tier': {'signal_one_summ': 0.0}
        }
    }]
    assert get_server_message(pipeline) == {
        'SAS.001': {'apphost||ctype,geo,prj,tier': {'signal_two_summ': 0.0}}
    }


def test_server_pipeline():
    pipeline = ZServerPipeline('SAS.001')

    host_records = loads_server_message(''.join(msgpack.dumps(x) for x in (
        ['SAS.001', {
            'apphost|ctype=prod;geo=man;prj=project|tier': {'signal_one_summ': 7.0},
            'apphost|ctype=prod;geo=sas;prj=project|tier': {'signal_one_summ': 11.0}
        }],
        ['SAS.002', {
            'apphost|ctype=prod;geo=man;prj=project|tier': {'signal_one_summ': 7.0},
            'apphost|ctype=prod;geo=sas;prj=project|tier': {'signal_one_summ': 11.0},
            'apphost|ctype=prod|geo,prj,tier': {'signal_one_summ': 18.0}
        }],
        ['sas1-1234', {
            'apphost|ctype=prod;geo=man;prj=project|tier': {'signal_one_summ': 3.0},
            'apphost|ctype=prod;geo=sas;prj=project|tier': {'signal_one_summ': 5.0}
        }]
    )))
    for host, tagged_record in host_records.iteritems():
        pipeline.mul_tagged_record(host, tagged_record)

    pipeline.set_subscriptions([
        {"host": "SAS.001", "tags": "itype=apphost;ctype=prod", "signal": "signal_one_summ"},
        {"host": "SAS.001", "tags": "itype=apphost;ctype=prod", "signal": "signal_not_exists_summ"},
        {"host": "SAS.001", "tags": "itype=apphost;ctype=prod", "signal": "signal_not_exists_aeee"},
        {"host": "SAS.001", "tags": "itype=apphost;ctype=prod", "signal": "signal_is_old"},
        {"host": "SAS.001", "tags": "itype=apphost;prj=not-exists", "signal": "signal_one_summ"},
        {"host": "SAS.002", "tags": "itype=apphost;ctype=prod", "signal": "signal_one_summ"},
        {"host": "sas1-1234", "tags": "itype=apphost;ctype=prod", "signal": "signal_one_summ"},
        {"host": "sas1-4321-not-exists", "tags": "itype=apphost;ctype=prod", "signal": "signal_one_summ"}
    ])
    stats = pipeline.finish()
    assert 3 == stats["key_set_power_max"]  # SAS.002
    assert 7 == stats["key_set_power_sum"]  #
    assert 2 == stats["resolved_key_set_power_max"]
    assert 5 == stats["resolved_key_set_power_sum"]

    timestamp = 105
    unpacked = msgpack.unpackb(pipeline.get_requested_points_message(timestamp))
    assert "subscriptions" in unpacked
    unpacked["subscriptions"].sort()
    assert unpacked == {
        'timestamp': timestamp,
        'subscriptions': [
            {'host': 'SAS.001', 'signal': 'signal_is_old', 'value': None, 'tags': 'itype=apphost;ctype=prod'},
            {'host': 'SAS.001', 'signal': 'signal_not_exists_aeee', 'value': None, 'tags': 'itype=apphost;ctype=prod'},
            {'host': 'SAS.001', 'signal': 'signal_not_exists_summ', 'value': 0, 'tags': 'itype=apphost;ctype=prod'},
            {'host': 'SAS.001', 'signal': 'signal_one_summ', 'value': 18, 'tags': 'itype=apphost;ctype=prod'},
            {'host': 'SAS.001', 'signal': 'signal_one_summ', 'value': 0, 'tags': 'itype=apphost;prj=not-exists'},
            {'host': 'SAS.002', 'signal': 'signal_one_summ', 'value': 18, 'tags': 'itype=apphost;ctype=prod'},
            {'host': 'sas1-1234', 'signal': 'signal_one_summ', 'value': 8, 'tags': 'itype=apphost;ctype=prod'},
            {'host': 'sas1-4321-not-exists', 'signal': 'signal_one_summ', 'value': 0, 'tags': 'itype=apphost;ctype=prod'}
        ]
    }


def test_server_pipeline_many_subscriptions():
    host_count = 10
    values_count = 13
    signal_count = 255
    data_value = 7.0
    bad_count = 15
    signal_in_sub_count = signal_count/2
    host_in_sub_count = host_count/2

    pipeline = ZServerPipeline('SAS.001')
    data = [
        ['SAS.{}'.format(host_id), {
            'apphost|ctype=prod;t1=v{};prj=project|tier'.format(val): {
                'signal_{}_summ'.format(sig_num): data_value for sig_num in xrange(signal_count)
            } for val in xrange(values_count)
        }] for host_id in xrange(host_count)
    ]
    host_records = loads_server_message(''.join(msgpack.dumps(x) for x in data))
    for hostname, tagged_record in host_records.iteritems():
        pipeline.mul_tagged_record(hostname, tagged_record)

    def sub(host, tags, signal):
        return {"host": host, "tags": tags, "signal": signal}

    def sub_with_value(host, tags, signal, value):
        return {"host": host, "tags": tags, "signal": signal, "value": value}

    subs = [sub("SAS.{}".format(x), "itype=apphost;ctype=prod", "signal_{}_summ".format(sig_num))
            for x in xrange(host_in_sub_count) for sig_num in xrange(signal_in_sub_count)]
    subs += [sub("SAS.{}".format(x), "itype=apphost;t1=v0;ctype=prod", "signal_{}_summ".format(sig_num))
             for x in xrange(host_in_sub_count) for sig_num in xrange(signal_in_sub_count)]
    subs += [sub("SAS.{}".format(x), "itype=apphost;ctype=prod", "signal_not_exists_aeee") for x in xrange(bad_count)]
    subs += [sub("SAS-not-exists", "itype=apphost;ctype=prod", "signal_{}_summ".format(x)) for x in xrange(bad_count)]

    pipeline.set_subscriptions(subs)
    stats = pipeline.finish()
    resolved_key_set_sum = host_in_sub_count * signal_in_sub_count * (values_count + 1)
    key_set_sum = host_count * signal_count * values_count

    assert values_count == stats["key_set_power_max"]
    assert key_set_sum == stats["key_set_power_sum"]
    assert values_count == stats["resolved_key_set_power_max"]
    assert resolved_key_set_sum == stats["resolved_key_set_power_sum"]

    timestamp = 105
    unpacked = msgpack.unpackb(pipeline.get_requested_points_message(timestamp))
    assert "subscriptions" in unpacked
    unpacked["subscriptions"].sort()

    def get_expected_subs():
        for i in xrange(bad_count):
            yield sub_with_value("SAS-not-exists", "itype=apphost;ctype=prod", "signal_{}_summ".format(i), 0)
        for i in xrange(host_in_sub_count):
            for j in xrange(signal_in_sub_count):
                yield sub_with_value("SAS.{}".format(i), "itype=apphost;ctype=prod", "signal_{}_summ".format(j), int(data_value * values_count))
        for i in xrange(host_in_sub_count):
            for j in xrange(signal_in_sub_count):
                yield sub_with_value("SAS.{}".format(i), "itype=apphost;ctype=prod;t1=v0", "signal_{}_summ".format(j), int(data_value))
        for i in xrange(bad_count):
            yield sub_with_value("SAS.{}".format(i), "itype=apphost;ctype=prod", "signal_not_exists_aeee", None)

    subscriptions_expected = list(get_expected_subs())
    subscriptions_expected.sort()

    assert unpacked == {
        'timestamp': timestamp,
        'subscriptions': subscriptions_expected
    }


def test_bad_subscriptions():
    pipeline = ZServerPipeline('SAS.001')

    host_records = loads_server_message(''.join(msgpack.dumps(x) for x in (
        ['SAS.001', {
            'apphost|ctype=prod,geo=man,prj=project|tier': {'signal_one_summ': 1.0},
        }],
    )))
    for host, tagged_record in host_records.iteritems():
        pipeline.mul_tagged_record(host, tagged_record)

    pipeline.set_subscriptions([
        {"host": "SAS.001", "tags": "itype=apphost;ctype=prod", "signal": "signal_one_summ"},
        {"host": "SAS.001", "tags": "itype=apphost;prj=first;prj=second", "signal": "signal_one_summ"},
        {"host": "sas1-1234"},
        {"host": "sas1-1234", "tags": "itype=apphost;prj=first;prj=second"},
        {"host": "sas1-1234", "signal": "signal_one_summ"},
        {"tags": "itype=apphost;prj=first;prj=second", "signal": "signal_one_summ"},
        {"tags": "itype=apphost;prj=first;prj=second"},
        {"signal": "signal_one_summ"},
        {"host": 123, "tags": None, "signal": 88}
    ])
    stats = pipeline.finish()

    timestamp = 965
    assert msgpack.unpackb(pipeline.get_requested_points_message(timestamp)) == {
        'timestamp': timestamp,
        'subscriptions': [
            {'host': 'SAS.001', 'signal': 'signal_one_summ', 'value': 1, 'tags': 'itype=apphost;ctype=prod'}
        ]
    }
    assert 1 == stats["key_set_power_max"]  # SAS.002
    assert 1 == stats["key_set_power_sum"]  #
    assert 1 == stats["resolved_key_set_power_max"]
    assert 1 == stats["resolved_key_set_power_sum"]


def test_rules(yasm_conf):
    aggregation_rules = ZAggregationRules()
    aggregation_rules.add_custom_rule("itype=qloud", ["prj"])
    aggregation_rules.add_default_rule(["tier"])
    common_rules = ZCommonRules()
    common_rules.add_rule("itype=apphost")

    pusher = ZInMemoryMessagePusher()
    pipeline = ZRequesterPipeline(yasm_conf, [pusher], aggregation_rules, common_rules, 'SAS.000', 2)
    metrics = ZTaggedMetricManager()
    pipeline.set_time(17)
    pipeline.set_metric_manager(metrics)
    pipeline.set_subscriptions([
        {"host": "sas1-1234", "tags": "itype=apphost;ctype=prod,prestable", "signal": "signal_two_summ"},
        {"host": "sas1-1234", "tags": "itype=apphost;ctype=prod,prestable", "signal": "cpu_common_summ"},
    ])
    response = json.dumps({
        "status": "ok",
        "aggr": [
            ["qloud", "ctype=prod;geo=sas;prj=alice;tier=first", {"signal_one_summ": 42}],
            ["apphost", "ctype=prod;geo=sas;prj=kubr;tier=none", {"signal_two_summ": 24}],
            ["common", "|ctype,geo,prj,tier", {"cpu_common_summ": 123}],
        ],
    })
    pipeline.add_host_response("sas1-1234", response, "application/json")
    stats_and_errors = pipeline.finish()
    assert 1 == stats_and_errors.hosts_success
    assert 1 == stats_and_errors.non_proto_responses

    assert get_tsdb_message(pusher) == [{
        'host': 'sas1-1234',
        'group': 'SAS.000',
        'time': 17,
        'values': {
            'common|group=SAS.000;host=sas1-1234|ctype,geo,prj,tier': {'cpu_common_summ': 123.0},
            'qloud|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;prj=alice;tier=first': {'signal_one_summ': 42.0},
            'qloud|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;tier=first|prj': {'signal_one_summ': 42.0},
            'apphost|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;prj=kubr;tier=none': {'signal_two_summ': 24.0, 'cpu_common_summ': 123},
            'apphost|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;prj=kubr|tier': {'signal_two_summ': 24.0, 'cpu_common_summ': 123}
        }
    }]
    assert get_middle_messages(pipeline) == {
        'common|group=SAS.000|ctype,geo,host,prj,tier': {'cpu_common_summ': 123.0},
        'qloud|ctype=prod;geo=sas;group=SAS.000;prj=alice;tier=first|host': {'signal_one_summ': 42.0},
        'qloud|ctype=prod;geo=sas;group=SAS.000;tier=first|host,prj': {'signal_one_summ': 42.0},
        'apphost|ctype=prod;geo=sas;group=SAS.000;prj=kubr;tier=none|host': {'signal_two_summ': 24.0, 'cpu_common_summ': 123.0},
        'apphost|ctype=prod;geo=sas;group=SAS.000;prj=kubr|host,tier': {'signal_two_summ': 24.0, 'cpu_common_summ': 123.0}
    }
    assert get_server_message(pipeline) == {
        'sas1-1234': {
            'apphost|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;prj=kubr;tier=none': {'signal_two_summ': 24.0, 'cpu_common_summ': 123.0},
            'apphost|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;prj=kubr|tier': {'signal_two_summ': 24.0, 'cpu_common_summ': 123.0}
        }
    }


def test_tier_filtering(yasm_conf):
    aggregation_rules = ZAggregationRules()
    common_rules = ZCommonRules()
    common_rules.add_rule("itype=upper")
    pusher = ZInMemoryMessagePusher()
    metrics = ZTaggedMetricManager()
    pipeline = ZRequesterPipeline(yasm_conf, [pusher], aggregation_rules, common_rules, "SAS.000", 1)
    pipeline.set_metric_manager(metrics)
    pipeline.set_time(10)

    response = json.dumps({
        "status": "ok",
        "aggr": [
            ["upper", "ctype=prod;geo=sas;prj=alice;tier=none", {"instance_summ": 42}],
            ["upper", "ctype=prod;geo=sas;prj=alice|tier", {"instance_summ": 42}],
            ["common", "|ctype,geo,prj,tier", {"cpu_host_summ": 123}],
        ],
    })
    pipeline.add_host_response("sas1-1234", response, "application/json")
    pipeline.finish()

    assert get_tsdb_message(pusher) == [{
        'host': 'sas1-1234',
        'group': 'SAS.000',
        'time': 10,
        'values': {
            'common|group=SAS.000;host=sas1-1234|ctype,geo,prj,tier': {'cpu_host_summ': 123.0},
            'upper|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;prj=alice;tier=none': {'instance_summ': 42.0, 'cpu_host_summ': 123.0}
        }
    }]


def test_subscription_store_request():
    subscriptions_dict = {
        "ASEARCH": {
            "itype=app;ctype=prod;prj=some": {"signal1_summ", "signal2_dhhh", "signal3_dmmm"},
            "itype=app;ctype=prod;prj=some;geo=sas": {"signal1_summ", "signal2_dhhh", "signal3_dmmm"},
            "geo=sas;itype=app;ctype=prod;prj=some": {"signal1_summ", "signal2_dhhh", "signal3_dmmm"},
            "some..incorrect,,tag": {"signal4_summ"},
            "some=incorrect=tag": {"signal5_summ"},
        },
        u"some_host": {  # unicode encoded host and group names should be accepted
            "itype=app;ctype=prod;prj=some": {"signal1_summ", "signal2_dhhh", "signal3_dmmm"}
        },
        u"неправильный_host": {  # non-ascii unicode strings should be ignored
            "itype=app;ctype=prod;prj=some": {"signal1_summ", "signal2_dhhh", "signal3_dmmm"}
        }
    }

    request = ZSubscriptionStoreProtobufRequestSerializer.subscriptions_dict_to_list_request(subscriptions_dict)
    parsed_request = subscription_store_pb2.TSubscriptionListRequest()
    parsed_request.ParseFromString(request)

    assert 9 == len(parsed_request.Subscriptions)
    assert 3 == len(parsed_request.SignalNameTable.Name)
    assert 2 == len(parsed_request.HostNameTable.Name)
    assert 2 == len(parsed_request.RequestKeyTable.Name)

    actual_subscriptions = defaultdict(lambda: defaultdict(set))
    host_table = parsed_request.HostNameTable.Name
    tags_table = parsed_request.RequestKeyTable.Name
    signal_table = parsed_request.SignalNameTable.Name
    for subscription in parsed_request.Subscriptions:
        actual_subscriptions[host_table[subscription.HostName.Index]][tags_table[subscription.RequestKey.Index]].add(
            signal_table[subscription.SignalExpression.Index]
        )

    assert actual_subscriptions == {
        "ASEARCH": {
            "itype=app;ctype=prod;prj=some": {"signal1_summ", "signal2_dhhh", "signal3_dmmm"},
            "itype=app;ctype=prod;geo=sas;prj=some": {"signal1_summ", "signal2_dhhh", "signal3_dmmm"},
        },
        "some_host": {
            "itype=app;ctype=prod;prj=some": {"signal1_summ", "signal2_dhhh", "signal3_dmmm"}
        }
    }


def test_containers(yasm_conf):
    aggregation_rules = ZAggregationRules()
    common_rules = ZCommonRules()
    common_rules.add_rule("itype=apphost")
    pusher = ZInMemoryMessagePusher()
    metrics = ZTaggedMetricManager()
    pipeline = ZRequesterPipeline(yasm_conf, [pusher], aggregation_rules, common_rules, "SAS.000", 1)
    pipeline.set_metric_manager(metrics)
    pipeline.set_subscriptions([
        {"host": "bylcf2kcwkknnhq6", "tags": "itype=apphost", "signal": "signal_two_summ"},
        {"host": "bylcf2kcwkknnhq6", "tags": "itype=apphost", "signal": "cpu_common_summ"},
        {"host": "sas1-1234", "tags": "itype=qloud", "signal": "signal_one_summ"},
        {"host": "sas1-4321", "tags": "itype=apphost", "signal": "signal_two_summ"},
    ])
    pipeline.set_time(10)
    response = json.dumps({
        "status": "ok",
        "aggr": [
            ["qloud", "ctype=prod;geo=sas;prj=alice;tier=first", {"signal_one_summ": 42}],
            ["apphost", "yasm_container=bylcf2kcwkknnhq6|ctype,geo,prj,tier", {"signal_two_summ": 42}],
            ["common", "|ctype,geo,prj,tier", {"cpu_common_summ": 123}],
        ],
    })
    pipeline.add_host_response("sas1-1234", response, "application/json")
    pipeline.finish()

    assert get_tsdb_message(pusher) == [{
        'host': 'bylcf2kcwkknnhq6',
        'group': 'SAS.000',
        'time': 10,
        'values': {
            'apphost|group=SAS.000;host=bylcf2kcwkknnhq6|ctype,geo,prj,tier': {'signal_two_summ': 42.0, "cpu_common_summ": 123.0},
        },
    }, {
        'host': 'sas1-1234',
        'group': 'SAS.000',
        'time': 10,
        'values': {
            'common|group=SAS.000;host=sas1-1234|ctype,geo,prj,tier': {'cpu_common_summ': 123.0},
            'qloud|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;prj=alice;tier=first': {'signal_one_summ': 42.0}
        },
    }]
    assert get_middle_messages(pipeline) == {
        'common|group=SAS.000|ctype,geo,host,prj,tier': {'cpu_common_summ': 123},
        'qloud|ctype=prod;geo=sas;group=SAS.000;prj=alice;tier=first|host': {'signal_one_summ': 42},
        'apphost|group=SAS.000|ctype,geo,host,prj,tier': {'signal_two_summ': 42.0, "cpu_common_summ": 123.0}
    }
    assert get_server_message(pipeline) == {
        'sas1-1234': {
            'qloud|ctype=prod;geo=sas;group=SAS.000;host=sas1-1234;prj=alice;tier=first': {'signal_one_summ': 42}
        },
        'bylcf2kcwkknnhq6': {
            'apphost|group=SAS.000;host=bylcf2kcwkknnhq6|ctype,geo,prj,tier': {'signal_two_summ': 42, "cpu_common_summ": 123.0}
        },
    }
