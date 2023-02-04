from solomon.services.gateway.api import yasm_gateway_service_pb2 as gateway_service_proto

from infra.yasm.gateway.lib.tags.request import RequestKey
from infra.yasm.gateway.lib.handlers import top


def test_prepare_request():
    request = top.TopHandler.build_request(
        "ASEARCH",
        "signal_summ",
        RequestKey.from_dict({
            "itype": "yasmagent",
            "ctype": "prod, prestable, t*",
            "prj": "yasm"
        }),
        7,
        "max"
    )
    assert request.query.hosts == "ASEARCH"
    assert request.query.tags == "itype=yasmagent;ctype=prestable,prod,t*;prj=yasm"
    assert request.query.expression == "signal_summ"
    assert request.query.from_millis % (top.TOP_PERIOD * 1000) == 0
    assert request.query.to_millis % (top.TOP_PERIOD * 1000) == 0
    assert request.query.from_millis < request.query.to_millis
    assert request.query.grid_millis == (top.TOP_PERIOD * 1000)
    assert request.limit == 7
    assert request.aggregation_function == gateway_service_proto.QueryTopRequest.MAX
    assert request.sort_direction == gateway_service_proto.QueryTopRequest.DESCENDING


def test_prepare_request_methods():
    host = "ASEARCH"
    signal = "signal_summ"
    request_key = RequestKey.from_dict({
        "itype": "yasmagent",
        "ctype": "prod, prestable, t*",
        "prj": "yasm"
    })

    request = top.TopHandler.build_request(host, signal, request_key, 10, "max")
    assert request.aggregation_function == gateway_service_proto.QueryTopRequest.MAX
    assert request.sort_direction == gateway_service_proto.QueryTopRequest.DESCENDING

    request = top.TopHandler.build_request(host, signal, request_key, 10, "min")
    assert request.aggregation_function == gateway_service_proto.QueryTopRequest.MIN
    assert request.sort_direction == gateway_service_proto.QueryTopRequest.ASCENDING

    request = top.TopHandler.build_request(host, signal, request_key, 10, "maxavg")
    assert request.aggregation_function == gateway_service_proto.QueryTopRequest.AVG
    assert request.sort_direction == gateway_service_proto.QueryTopRequest.DESCENDING

    request = top.TopHandler.build_request(host, signal, request_key, 10, "minavg")
    assert request.aggregation_function == gateway_service_proto.QueryTopRequest.AVG
    assert request.sort_direction == gateway_service_proto.QueryTopRequest.ASCENDING


def fill_timeseries(proto_ts, ts_value_pairs, errors=None):
    for ts, value in ts_value_pairs:
        proto_ts.values.append(value)
        proto_ts.timestamps_millis.append(ts * 1000)
    if errors:
        proto_ts.errors.extend(errors)


def test_pack_response():
    host = "ASEARCH"
    signal = "signal_summ"
    request_key = RequestKey.from_dict({
        "itype": "yasmagent",
        "ctype": "prod, prestable, t*",
        "prj": "yasm"
    })
    request_proto = top.TopHandler.build_request(host, signal, request_key, 2, "max")
    response_proto = gateway_service_proto.QueryTopResponse()

    points_count = (request_proto.query.to_millis - request_proto.query.from_millis) / request_proto.query.grid_millis
    assert points_count >= 240

    host = response_proto.top_hosts.add()
    host.host_name = "host1"
    host.aggregated_value = 13.078
    host.timeseries.timestamps_millis.append(request_proto.query.from_millis)
    host.timeseries.values.append(13.077)
    host.timeseries.timestamps_millis.append(request_proto.query.from_millis + 5000)
    host.timeseries.values.append(13.079)

    host = response_proto.top_hosts.add()
    host.host_name = "host2"
    host.aggregated_value = 10.1
    host.timeseries.timestamps_millis.append(request_proto.query.from_millis)
    host.timeseries.values.append(10.0)
    host.timeseries.timestamps_millis.append(request_proto.query.from_millis + 5000)
    host.timeseries.values.append(10.2)

    st = request_proto.query.from_millis / 1000
    assert top.TopHandler.pack_response(response_proto, request_proto) == {
        "top": [
            {
                "host": "host1",
                "scores": 13.078,
                "points": [13.077, 13.079] + [None] * (points_count - 2),
                "timeline": list(xrange(st, st + points_count * top.TOP_PERIOD, top.TOP_PERIOD))
            },
            {
                "host": "host2",
                "scores": 10.1,
                "points": [10.0, 10.2] + [None] * (points_count - 2),
                "timeline": list(xrange(st, st + points_count * top.TOP_PERIOD, top.TOP_PERIOD))
            }
        ]
    }
