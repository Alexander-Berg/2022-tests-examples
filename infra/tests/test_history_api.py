# encoding: utf-8

import pytest

from infra.yasm.interfaces.internal import history_api_pb2
from cppzoom import ZHistoryApiRequestSerializer, ZHistoryApiResponseMerger, ZHistoryAggregator, ZHistoryResponse


def test_valid_requests():
    requests = [{
        'et': 1549528935,
        'tag_signals': {u'itype=upper;geo=sas': [u'accesslog-2xx_summ']},
        'hosts': ['IVA.005', 'VLA_VM.000', 'VLA_GENFG.000'],
        'period': 5,
        'st': 1549527445}]
    content = ZHistoryApiRequestSerializer.requests_to_proto(requests, deadline=100, request_id="req")

    history_api_request = history_api_pb2.THistoryReadAggregatedRequest()
    history_api_request.ParseFromString(content)

    combinations = set()
    for request in requests:
        for tags, signals in request['tag_signals'].iteritems():
            for signal in signals:
                for host in request['hosts']:
                    combinations.add((host, tags, signal))

    assert history_api_request.DeadlineTimestamp == 100
    assert history_api_request.RequestId == "req"
    assert len(history_api_request.Queries) == len(combinations)
    for query in history_api_request.Queries:
        assert query.StartTimestamp == requests[0]['st']
        assert query.EndTimestamp == requests[0]['et']
        assert query.Period == requests[0]['period']
        host_name = history_api_request.HostNameTable.Name[query.HostName.Index]
        request_key = history_api_request.RequestKeyTable.Name[query.RequestKey.Index]
        signal_name = history_api_request.SignalNameTable.Name[query.SignalName.Index]
        assert (host_name, request_key, signal_name) in combinations

    loaded_requests = ZHistoryApiRequestSerializer.load_requests(content)
    assert len(loaded_requests) == len(combinations)


@pytest.mark.parametrize("requests", [
    {},
    [[]],
    [{'hosts': [""]}],
    [{'st': None, 'period': 5}],
    [{'st': 200, 'et': 100, 'period': 5}],
    [{'st': 100, 'et': 200, 'period': 9}],
    [{'st': 100, 'et': 200, 'period': 0}]
])
def test_invalid_requests(requests):
    with pytest.raises(Exception):
        ZHistoryApiRequestSerializer.requests_to_proto(requests)


def test_empty_requests():
    requests = []
    history_api_request = history_api_pb2.THistoryReadAggregatedRequest()
    history_api_request.ParseFromString(ZHistoryApiRequestSerializer.requests_to_proto(requests))
    assert not len(history_api_request.Queries)


def test_valid_load_responses():
    history_api_response = history_api_pb2.THistoryReadAggregatedResponse()
    history_api_response.HostNameTable.Name.append("sas1-1234")
    history_api_response.RequestKeyTable.Name.append("itype=history")
    history_api_response.SignalNameTable.Name.append("signal_summ")
    series = history_api_response.Series.add()
    series.StatusCode = history_api_pb2.THistoryAggregatedSeries.NOT_FOUND
    series.Query.HostName.Index = 0
    series.Query.RequestKey.Index = 0
    series.Query.SignalName.Index = 0
    series.Query.StartTimestamp = 500
    series.Query.EndTimestamp = 500
    series.Query.Period = 5
    response_merger = ZHistoryApiResponseMerger()
    assert [series.StatusCode] == response_merger.load_responses(history_api_response.SerializeToString())

    # there is no api to check it's content
    assert len(response_merger.merge_loaded_responses()) == 1


def test_empty_load_responses():
    history_api_response = history_api_pb2.THistoryReadAggregatedResponse()
    response_merger = ZHistoryApiResponseMerger()
    assert [] == response_merger.load_responses(history_api_response.SerializeToString())
    assert 0 == len(response_merger.merge_loaded_responses())


def test_merge_multiple_responses():
    responses = [
        '\n\x13\n\x11SAS_KERNEL_TEST.2\x12\r\n\x0bitype=upper\x1a\x14\n\x12accesslog-2xx_summ" '
        '\n\x14\n\x00\x12\x00\x1a\x00 \xc5\x9b\xbf\xe3\x05(\x97\xa7\xbf\xe3\x050\x05\x10\xc5\x9b\xbf\xe3\x05\x1a\x00 \x01',
        '\n\x13\n\x11SAS_KERNEL_TEST.2\x12\r\n\x0bitype=upper\x1a\x14\n\x12accesslog-2xx_summ" '
        '\n\x14\n\x00\x12\x00\x1a\x00 \xc5\x9b\xbf\xe3\x05(\x97\xa7\xbf\xe3\x050\x05\x10\xc5\x9b\xbf\xe3\x05\x1a\x00 \x01'
    ]
    response_merger = ZHistoryApiResponseMerger()
    for response in responses:
        assert [history_api_pb2.THistoryAggregatedSeries.OK] == response_merger.load_responses(response)
    assert 1 == len(response_merger.merge_loaded_responses())


def test_history_aggregator(yasm_conf):
    aggregator = ZHistoryAggregator(yasm_conf)
    aggregator.add_requests([{
        'et': 1549528935,
        'tag_signals': {u'geo=sas;itype=upper': [u'accesslog-2xx_summ']},
        'hosts': ['ASEARCH'],
        'period': 5,
        'st': 1549527445,
        'single_replica': 0
    }])
    aggregator.add_metagroups({
        'ASEARCH': ['SAS.000']
    })
    aggregator.mul_response(ZHistoryResponse.create({
        'et': 1549528935,
        'tag': u'itype=upper;geo=sas',
        'signal': u'accesslog-2xx_summ',
        'host': 'SAS.000',
        'period': 5,
        'st': 1549527445,
        'values': [1, 2, 3],
        'single_replica': 0
    }))
    assert aggregator.get_responses() == ([{
        'host': 'ASEARCH',
        'tag': 'itype=upper;geo=sas',
        'signal': 'accesslog-2xx_summ',
        'status_codes': {},
        'st': 1549527445,
        'et': 1549528935,
        'period': 5,
        'series_st': 1549527445,
        'values': [1, 2, 3]
    }], [])


def test_history_aggregator_overwrite(yasm_conf):
    requests = [{
        'et': 1010,
        'tag_signals': {u'geo=sas;itype=upper': [u'accesslog-2xx_summ']},
        'hosts': ['SAS.000'],
        'period': 5,
        'st': 1000,
        'single_replica': 0
    }]

    aggregator = ZHistoryAggregator(yasm_conf)
    aggregator.add_requests(requests)
    aggregator.mul_response(ZHistoryResponse.create({
        'et': 1010,
        'tag': u'itype=upper;geo=sas',
        'signal': u'accesslog-2xx_summ',
        'host': 'SAS.000',
        'period': 5,
        'st': 1000,
        'values': [1, 2],
        'single_replica': 0
    }))

    otherAggregator = ZHistoryAggregator(yasm_conf)
    otherAggregator.add_requests(requests)
    otherAggregator.mul_response(ZHistoryResponse.create({
        'et': 1010,
        'tag': u'itype=upper;geo=sas',
        'signal': u'accesslog-2xx_summ',
        'host': 'SAS.000',
        'period': 5,
        'st': 1000,
        'series_st': 1005,
        'values': [4, 3],
        'single_replica': 0
    }))

    aggregator.overwrite_since(otherAggregator, 1010, 1010)
    assert aggregator.get_responses() == ([{
        'host': 'SAS.000',
        'tag': 'itype=upper;geo=sas',
        'signal': 'accesslog-2xx_summ',
        'status_codes': {},
        'st': 1000,
        'et': 1010,
        'period': 5,
        'series_st': 1000,
        'values': [1, 2, 3]
    }], [])


def test_history_aggregator_errors(yasm_conf):
    aggregator = ZHistoryAggregator(yasm_conf)
    aggregator.add_requests([{
        'et': 1549528935,
        'tag_signals': {u'geo=sas;itype=upper': [u'accesslog-2xx_summ']},
        'hosts': ['ASEARCH'],
        'period': 5,
        'st': 1549527445,
        'single_replica': 0
    }])
    aggregator.add_metagroups({
        'ASEARCH': ['SAS.000']
    })
    aggregator.mul_response(ZHistoryResponse.create({
        'et': 1549528935,
        'tag': u'itype=upper;geo=sas',
        'signal': u'accesslog-2xx_summ',
        'host': 'SAS.000',
        'period': 5,
        'st': 1549527445,
        'status_code': 4,
        'values': [],
        'single_replica': 0
    }))
    assert aggregator.get_responses() == ([{
        'host': 'ASEARCH',
        'tag': 'itype=upper;geo=sas',
        'signal': 'accesslog-2xx_summ',
        'status_codes': {
            'SAS.000': 4
        },
        'st': 1549527445,
        'et': 1549528935,
        'period': 5,
        'series_st': 1549527445,
        'values': []
    }], [])


def test_responses_to_proto():
    responses = [ZHistoryResponse.create({
        'et': 1549528935,
        'tag': u'itype=upper;geo=sas',
        'signal': u'accesslog-2xx_summ',
        'host': 'SAS.000',
        'period': 5,
        'st': 1549527445,
        'values': [1, 2, 3],
        'single_replica': 0
    })]
    assert ZHistoryApiRequestSerializer.responses_to_proto(responses)


@pytest.mark.parametrize("incoming,allow_legacy_types,expected_values,expected_converted_signals", [
    # conversion small to normal returns message
    ([[1, 2], [3]], True, [[[1, 2], 0, None], [[3], 0, None]], []),
    ([[1, 2], [3]], False, [[[1, 1], 0, 0], [[0, 0, 1], 0, 0]], [["itype=upper;geo=sas", "accesslog-2xx_hgram"]]),
    # no messages if converting empty small
    ([[], []], True, [[[], 0, None], [[], 0, None]], []),
    ([[], []], False, [[[], 0, 0], [[], 0, 0]], []),
    # no messages if converting empty normal
    ([[[], 0, 0], [[], 0, 0]], True, [[[], 0, 0], [[], 0, 0]], []),
    ([[[], 0, 0], [[], 0, 0]], False, [[[], 0, 0], [[], 0, 0]], []),
    # message if got normal hgram with small size, but no conversion
    ([[[1, 4], 0, 0], [[1], 2, 5]], True, [[[1, 4], 0, 0], [[1], 2, 5]], []),
    ([[[1, 4], 0, 0], [[1], 2, 5]], False, [[[1, 4], 0, 0], [[1], 2, 5]], [["itype=upper;geo=sas", "accesslog-2xx_hgram"]]),
])
def test_history_aggregator_allow_legacy_types(incoming, allow_legacy_types, expected_values, expected_converted_signals, yasm_conf):
    requests = [{
        'et': 1010,
        'tag_signals': {u'geo=sas;itype=upper': [u'accesslog-2xx_hgram']},
        'hosts': ['SAS.000'],
        'period': 5,
        'st': 1000,
        'single_replica': 0
    }]

    aggregator = ZHistoryAggregator(yasm_conf)
    aggregator.add_requests(requests)
    aggregator.mul_response(ZHistoryResponse.create({
        'et': 1010,
        'tag': u'itype=upper;geo=sas',
        'signal': u'accesslog-2xx_hgram',
        'host': 'SAS.000',
        'period': 5,
        'st': 1000,
        'values': incoming,
        'single_replica': 0
    }))

    assert aggregator.get_responses(allow_legacy_types) == ([{
        'host': 'SAS.000',
        'tag': 'itype=upper;geo=sas',
        'signal': 'accesslog-2xx_hgram',
        'status_codes': {},
        'st': 1000,
        'et': 1010,
        'period': 5,
        'series_st': 1000,
        'values': expected_values
    }], expected_converted_signals)


def test_history_aggregator_allow_legacy_types_many_requests(yasm_conf):
    incoming_values_array = [
        [[[], 0, 0], [[], 0, 0]],
        [[1, 2], [3]],
        [[[], 0, 0], [[], 0, 0]],
        [[1, 2], [3]],
    ]
    expected_values_array = [
        [[[], 0, 0], [[], 0, 0]],
        [[[1, 1], 0, 0], [[0, 0, 1], 0, 0]],
        [[[], 0, 0], [[], 0, 0]],
        [[[1, 1], 0, 0], [[0, 0, 1], 0, 0]],
    ]
    requests = [{
        'et': 1010,
        'tag_signals': {u'geo=sas;itype=upper': [u'signal-{}_hgram'.format(idx)]},
        'hosts': ['SAS.000'],
        'period': 5,
        'st': 1000,
        'single_replica': 0
    } for idx in xrange(len(incoming_values_array))]

    aggregator = ZHistoryAggregator(yasm_conf)
    aggregator.add_requests(requests)
    for idx in xrange(len(incoming_values_array)):
        aggregator.mul_response(ZHistoryResponse.create({
            'et': 1010,
            'tag': u'itype=upper;geo=sas',
            'signal': u'signal-{}_hgram'.format(idx),
            'host': 'SAS.000',
            'period': 5,
            'st': 1000,
            'values': incoming_values_array[idx],
            'single_replica': 0
        }))

    responses, messages = aggregator.get_responses(False)
    assert sorted(responses) == sorted([{
        'host': 'SAS.000',
        'tag': 'itype=upper;geo=sas',
        'signal': 'signal-{}_hgram'.format(idx),
        'status_codes': {},
        'st': 1000,
        'et': 1010,
        'period': 5,
        'series_st': 1000,
        'values': expected_values_array[idx]
    } for idx in xrange(len(incoming_values_array))])
    assert sorted(messages) == sorted([['itype=upper;geo=sas', 'signal-1_hgram'], ['itype=upper;geo=sas', 'signal-3_hgram']])
