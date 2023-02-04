# encoding: utf-8
import json
import sys

import pytest

import cppzoom


def is_correct_ugram(ugram):
    bounds = [bucket for bucket in ugram[1]]
    assert bounds == sorted(bounds, key=lambda b: b[0])


def test_ugram_trivial():
    ugram = cppzoom.debug_value(['ugram', []])
    assert sys.getrefcount(ugram) == 2
    is_correct_ugram(ugram)


def test_ugram_1():
    ugram = cppzoom.debug_value(['ugram', [[0.0, 1.0]]])
    assert sys.getrefcount(ugram) == 2
    is_correct_ugram(ugram)


def test_ugram_deserialization():
    ugram = cppzoom.debug_value(["ugram", [[0, 6398.0], [1, 88.0], [2, 1.0], [3, 0.0]]])
    assert sys.getrefcount(ugram) == 2
    is_correct_ugram(ugram)
    assert ugram == ["ugram", [(0, 6398), (1, 88), (2, 1), (3, 0)]]


def test_ugram_deserialization_2():
    ugram = cppzoom.debug_value([u'ugram', [[1, 1], [1, 0]]])
    assert sys.getrefcount(ugram) == 2
    is_correct_ugram(ugram)
    assert ugram == ['ugram', [(1, 1), (1, 0)]]


def as_agent_response(ugram):
    return json.dumps({
        "status": "ok",
        "aggr": [["apphost", "|ctype", {"signal_hgram": ugram}]],
    })


@pytest.mark.parametrize("hgrams", [
    [[[1.0], 0, 0], ['ugram', [[0.0, 1.0]]], [[1.0], 0, None]],
    [[[1.0], 0, None], [[8.988465674311579e+307, 8.98846567431158e+307], 0, -100], ['ugram', []]],
    [[[1.0], 0, None], [[0, 1, 2], 0, -100], ['ugram', []]]
])
def test_accumulate(yasm_conf, hgrams):
    aggregation_rules = cppzoom.ZAggregationRules()
    common_rules = cppzoom.ZCommonRules()
    pushers = [cppzoom.ZInMemoryMessagePusher(), cppzoom.ZInMemoryMessagePusher()]
    pipeline = cppzoom.ZRequesterPipeline(yasm_conf, pushers, aggregation_rules, common_rules, "SAS.001", 1)

    metric_manager = cppzoom.ZTaggedMetricManager()
    pipeline.set_metric_manager(metric_manager)
    pipeline.set_time(17)

    for i, hgram in enumerate(hgrams):
        pipeline.add_host_response("host{}".format(i), as_agent_response(hgram), "not-protobuf")

    stats = pipeline.finish()
    assert stats.hosts_success == len(hgrams)
    assert len(stats.deserialize_errors) == 0
    assert len(stats.aggregate_errors) == 0
    assert len(stats.invalid_response_hosts) == 0
    assert stats.ignored_signals == 0
    assert stats.non_proto_responses == len(hgrams)
    middle_message = pipeline.get_middle_messages()[0]
    values_dict = cppzoom.loads_middle_message(middle_message).get_values()
    assert 1 == len(values_dict)
    is_correct_ugram(values_dict["apphost|group=SAS.001|ctype,host"]["signal_hgram"])
