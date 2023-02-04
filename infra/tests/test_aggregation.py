# encoding: utf-8
import sys
import collections

from infra.yasm.interfaces.internal import subscription_store_pb2
from cppzoom import (
    ZSubscriptionsWithValueSeries,
    ZGetSubscriptionValuesResponseLoader, ZSubscriptionSplitter
)

Subscription = collections.namedtuple("Subscription", "host tags tags_as_requested signal start_ts values")


def compose_get_values_response(subscriptions):
    host_name_table_lookup = {}
    request_keys_table_lookup = {}
    signal_table_lookup = {}
    host_name_table = []
    request_keys_table = []
    signal_table = []
    for sub in subscriptions:
        if sub.host not in host_name_table_lookup:
            host_name_table_lookup[sub.host] = len(host_name_table)
            host_name_table.append(sub.host)
        if sub.tags not in request_keys_table_lookup:
            request_keys_table_lookup[sub.tags] = len(request_keys_table)
            request_keys_table.append(sub.tags)
        if sub.signal not in signal_table_lookup:
            signal_table_lookup[sub.signal] = len(signal_table)
            signal_table.append(sub.signal)
    response = subscription_store_pb2.TGetValuesResponse()
    for host_name in host_name_table:
        response.HostNameTable.Name.append(host_name)
    for request_key in request_keys_table:
        response.RequestKeyTable.Name.append(request_key)
    for signal in signal_table:
        response.SignalNameTable.Name.append(signal)
    for subscription in subscriptions:
        response.RawRequestKeys.append(subscription.tags_as_requested)
        sub_proto = response.Subscriptions.add()
        sub_proto.Subscription.HostName.Index = host_name_table_lookup[subscription.host]
        sub_proto.Subscription.RequestKey.Index = request_keys_table_lookup[subscription.tags]
        sub_proto.Subscription.SignalExpression.Index = signal_table_lookup[subscription.signal]
        sub_proto.StartTimestamp = subscription.start_ts
        sub_proto.ValueSeries.ValuesCount = 3
        sub_proto.ValueSeries.ValueType = 0
        sub_proto.ValueSeries.Values = "any string is fine in this test" + str(subscription.values)
    return response.SerializeToString()


def test_parse():
    merger_test = compose_get_values_response([
        Subscription(host="GRP0",
                     tags="itype=app0",
                     tags_as_requested="common_app0",
                     signal="test-hdlr_signal0",
                     start_ts=100,
                     values=[10, None, None]),
        Subscription(host="GRP1",
                     tags="itype=app1",
                     tags_as_requested="common_app1",
                     signal="test-hdlr_signal1",
                     start_ts=105,
                     values=[10, None, None]),
        Subscription(host="GRP2",
                     tags="itype=app2",
                     tags_as_requested="common_app2",
                     signal="test-hdlr_signal2",
                     start_ts=110,
                     values=[10, None, None])
    ])

    response_loader = ZGetSubscriptionValuesResponseLoader()
    loaded_records = response_loader.load(merger_test)
    assert sys.getrefcount(loaded_records) == 2
    assert len(loaded_records) == 3

    for i in range(0, 3):
        host = "GRP{}".format(i)
        assert host in loaded_records
        host_subs_with_values = loaded_records[host]
        assert isinstance(host_subs_with_values, ZSubscriptionsWithValueSeries)

    splitter = ZSubscriptionSplitter(4)
    splitter.set_metagroup_for_group("GRP0", "METAGRP")
    splitter.set_metagroup_for_group("GRP1", "METAGRP")
    splitter.set_metagroup_for_group("GRP2", "METAGRP")
    response_parts, max_start_ts = ZGetSubscriptionValuesResponseLoader.split(merger_test, splitter)
    assert splitter.get_total() == len(response_parts)
    assert 110 == max_start_ts
    loaded_records = {}
    for i, response in enumerate(response_parts):
        cur_loaded_records = response_loader.load(response)
        for host, host_subs_with_values in cur_loaded_records.iteritems():
            assert isinstance(host_subs_with_values, ZSubscriptionsWithValueSeries)
        loaded_records.update(cur_loaded_records)

    assert len(loaded_records) == 3
    for i in range(0, 3):
        host = "GRP{}".format(i)
        assert host in loaded_records
        host_subs_with_values = loaded_records[host]
        assert isinstance(host_subs_with_values, ZSubscriptionsWithValueSeries)
