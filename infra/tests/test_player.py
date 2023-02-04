# coding: utf-8

import time
import json

import msgpack
from protobuf_to_dict import protobuf_to_dict

import cppplayer

import infra.yasm.interfaces.internal.agent_pb2 as agent_pb2


def get_per_instance_data():
    return {
        'localhost_common:0': {
            'hostname': 'localhost_common',
            'tags': ['common||ctype,geo,prj,tier'],
            'type': 'common',
            'values': {
                'cpu-context_switches_hgram': [[70924], 1, None],
            }
        }
    }


def get_unicode_per_instance_data():
    return {
        u'localhost_common:0': {
            'hostname': 'localhost_common',
            'tags': ['common||ctype,geo,prj,tier'],
            'type': 'common',
            'values': {
                'cpu-context_switches_hgram': [[70924], 1, None],
            }
        }
    }


def get_aggregaged_data():
    return [
        ('rtcslatentacles', '|ctype,geo,prj,tier', {'counter-instance_tmmv': 1})
    ]


def aggregaged_to_msgpack(obj):
    itype, tail, values = obj
    return (itype, tail, tuple(values.iteritems()))


def per_instance_to_msgpack(obj):
    result = obj.copy()
    result['values'] = [[key, value] for key, value in obj['values'].iteritems()]
    return result


def test_player_data():
    per_instance_data = get_per_instance_data()
    aggregaged_data = get_aggregaged_data()
    player_data = cppplayer.ZPlayerData(time.time(), per_instance_data, aggregaged_data)

    assert json.loads(player_data.to_aggregated_json()) == [
        list(x) for x in aggregaged_data
    ]
    assert json.loads(player_data.to_per_instance_json()) == per_instance_data

    assert msgpack.unpackb(player_data.to_aggregated_msgpack(), use_list=False) == tuple(
        aggregaged_to_msgpack(x) for x in aggregaged_data
    )
    assert msgpack.unpackb(player_data.to_per_instance_msgpack()) == {
        key: per_instance_to_msgpack(value) for key, value in per_instance_data.items()
    }

    per_instance_records = agent_pb2.TPerInstanceRecords()
    per_instance_records.ParseFromString(player_data.to_per_instance_protobuf())
    assert protobuf_to_dict(per_instance_records) == {
        'InstanceKeyTable': {'Name': ['common||ctype,geo,prj,tier']},
        'SignalNameTable': {'Name': ['cpu-context_switches_hgram']},
        'Records': [{'HostName': 'localhost_common',
                     'InstanceKeys': [{}],
                     'InstanceName': 'localhost_common:0',
                     'InstanceType': 'common',
                     'Record': {'Values': [{'SignalName': {},
                                            'Value': {'HgramSmall': {'Values': [70924.0],
                                                                     'Zeros': 1L}}}]}}]}

    aggregated_records = agent_pb2.TAggregatedRecords()
    aggregated_records.ParseFromString(player_data.to_aggregated_protobuf())
    assert protobuf_to_dict(aggregated_records) == {
        'InstanceKeyTable': {'Name': ['rtcslatentacles||ctype,geo,prj,tier']},
        'Records': [{'InstanceKey': {},
                     'Record': {'Values': [{'SignalName': {},
                                            'Value': {'FloatValue': {'Value': 1.0}}}]}}],
        'SignalNameTable': {'Name': ['counter-instance_tmmv']}}


def test_player_data_unicode():
    per_instance_data = get_unicode_per_instance_data()
    player_data = cppplayer.ZPlayerData(time.time(), per_instance_data, [])

    assert json.loads(player_data.to_per_instance_json()) == per_instance_data
    assert msgpack.unpackb(player_data.to_per_instance_msgpack()) == {
        key: per_instance_to_msgpack(value) for key, value in per_instance_data.items()
    }
