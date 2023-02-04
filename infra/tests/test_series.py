import copy
import collections
import logging
import mock
import pytest

from solomon.services.gateway.api import yasm_gateway_service_pb2 as gateway_service_proto

from infra.yasm.gateway.lib.handlers import series
from infra.yasm.gateway.lib.tags.request import RequestKey
from infra.yasm.gateway.lib.util import yasmconf_lite

from utils import MockedMultiAttemptRpc, fill_timeseries

log = logging.getLogger(__name__)


def test_handle_bad_format():
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=None,  # not needed in this case
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=10,
            max_request_points_signal=10,
            max_signals_per_request=10),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )

    filtered_request, errors = data_processor.check_srv_request(["something that is not a correct request"])
    assert [] == filtered_request
    assert len(errors["common"]) > 0

    filtered_request, errors = data_processor.check_srv_request({"ctxList": []})
    assert [] == filtered_request
    assert {} == errors

    filtered_request, errors = data_processor.check_srv_request({"ctxList": ["not a ctx"]})
    assert [] == filtered_request
    assert len(errors["common"]) > 0

    filtered_request, errors = data_processor.check_srv_request({"ctxList": {"not a ctx": True}})
    assert [] == filtered_request
    assert len(errors["common"]) > 0

    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [{"not a ctx": True}]
    })
    assert [] == filtered_request
    assert len(errors["common"]) > 0

    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [{"not a ctx": True}]
    })
    assert [] == filtered_request
    assert len(errors["common"]) > 0

    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [
            {
                "host",
                "st",
                "et",
                "signals",
                "period"
            }
        ]
    })
    assert [] == filtered_request
    assert len(errors["common"]) > 0


def test_filter_contexts_with_missing_fields():
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=None,  # not needed in this case
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=10,
            max_request_points_signal=10,
            max_signals_per_request=10),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )

    good_context = {
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487680200,
        "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
        "period": 300
    }
    good_context_processed = {
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487680200,
        "signals": {
            series.TaggedSignalExpr(
                raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                request_key=RequestKey.from_string("itype=mmeta"),
                signal_expr="sum(test_signal_ammm,normal())"
            )
        },
        "period": 300
    }
    for key in good_context:
        bad_context = copy.copy(good_context)
        del bad_context[key]
        filtered_request, errors = data_processor.check_srv_request({
            "ctxList": [
                bad_context,
                good_context
            ]
        })
        assert filtered_request == [good_context_processed]
        assert len(errors["common"]) > 0


def test_filter_contexts_with_bad_base_fields():
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=None,  # not needed in this case
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=100,
            max_request_points_signal=100,
            max_signals_per_request=10),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )

    good_context = {
        "id": "ASEARCH:1487679300_1487680200_300",
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487680200,
        "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
        "period": 300
    }
    good_context_processed = {
        "id": "ASEARCH:1487679300_1487680200_300",
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487680200,
        "signals": {
            series.TaggedSignalExpr(
                raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                request_key=RequestKey.from_string("itype=mmeta"),
                signal_expr="sum(test_signal_ammm,normal())"
            )
        },
        "period": 300
    }
    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [
            good_context,
            {
                "id": "0",
                "host": "ASEARCH",
                "st": 1487679300,
                "et": 1487679000,
                "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
                "period": 300
            },
            {
                "id": "1",
                "host": "ASEARCH",
                "st": 1487679300,
                "et": 1487680200,
                "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
                "period": 4
            },
            {
                "id": "2",
                "host": "ASEARCH",
                "st": 1487679300,
                "et": 1487680200,
                "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
                "period": 301
            },
            {
                "id": "3",
                "host": {"not_a_str"},
                "st": 1487679300,
                "et": 1487680200,
                "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
                "period": 300
            },
            {
                "id": "4",
                "host": "",
                "st": 1487679300,
                "et": 1487680200,
                "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
                "period": 300
            }
        ]
    })
    assert len(errors) == 5
    assert filtered_request == [good_context_processed]


def test_filter_signals():
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=None,  # not needed in this case
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=100,
            max_request_points_signal=100,
            max_signals_per_request=10),
        yasmconf=yasmconf_lite.YasmConfLite(conf={
            "conflist": {
                "qloud": {
                    "tagkeys": {
                        "required": ["prj"]
                    }
                }
            }
        }),
        log=log
    )

    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [
            {
                "id": "ctx",
                "host": "ASEARCH",
                "st": 1487679300,
                "et": 1487680200,
                "signals": [
                    "itype=mmeta:sum(test_signal_ammm,normal())",
                    "mmeta_self:sum(test_signal_ammm,normal())",
                    "itype:mmeta:sum(test_signal_ammm,normal())",
                    "itype=mmeta;prj:sum(test_signal_ammm,normal())",
                    "itype=qloud:sum(test_signal_ammm,normal())"
                ],
                "period": 300
            }
        ]
    })
    assert filtered_request == [
        {
            "id": "ctx",
            "host": "ASEARCH",
            "st": 1487679300,
            "et": 1487680200,
            "signals": {
                series.TaggedSignalExpr(
                    raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                    request_key=RequestKey.from_string("itype=mmeta"),
                    signal_expr="sum(test_signal_ammm,normal())"
                ),
                series.TaggedSignalExpr(
                    raw_tagged_signal_expr="mmeta_self:sum(test_signal_ammm,normal())",
                    request_key=RequestKey.from_string("itype=mmeta"),
                    signal_expr="sum(test_signal_ammm,normal())"
                )
            },
            "period": 300
        }
    ]
    assert len(errors) == 1
    assert len(errors["ctx"]) == 3


def test_filter_contexts_with_duplicated_ids():
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=None,  # not needed in this case
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=10,
            max_request_points_signal=10,
            max_signals_per_request=10),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )

    good_context = {
        "id": "ASEARCH:1487679300_1487680200_300",
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487680200,
        "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
        "period": 300
    }
    good_context_processed = {
        "id": "ASEARCH:1487679300_1487680200_300",
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487680200,
        "signals": {
            series.TaggedSignalExpr(
                raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                request_key=RequestKey.from_string("itype=mmeta"),
                signal_expr="sum(test_signal_ammm,normal())"
            )
        },
        "period": 300
    }
    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [
            good_context,
            {
                "id": "ASEARCH:1487679300_1487680200_300",
                "host": "ASEARCH",
                "st": 1487679300,
                "et": 1487680200,
                "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
                "period": 300
            },
            {
                "host": "ASEARCH",
                "st": 1487679300,
                "et": 1487680200,
                "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
                "period": 300
            }
        ]
    })
    assert filtered_request == [good_context_processed]
    assert len(errors["ASEARCH:1487679300_1487680200_300"]) > 0


def test_filter_too_large_contexts():
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=None,  # not needed in this case
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=100,
            max_request_points_signal=10,
            max_signals_per_request=1),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )

    good_context = {
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487680200,
        "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
        "period": 300
    }
    good_context_processed = {
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487680200,
        "signals": {
            series.TaggedSignalExpr(
                raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                request_key=RequestKey.from_string("itype=mmeta"),
                signal_expr="sum(test_signal_ammm,normal())"
            )
        },
        "period": 300
    }
    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [
            good_context,
            {
                "id": "1",
                "host": "ASEARCH",
                "st": 1487679300,
                "et": 1487680200,
                "signals": [
                    # 2 signals is too much here
                    "itype=mmeta:sum(test_signal_ammm,normal())",
                    "itype=mmeta:havg(test_signal_hgram)"
                ],
                "period": 300
            },
            {
                "id": "2",
                "host": "ASEARCH",
                "st": 1487679300,
                "et": 1487682300,  # 11 points is too much here
                "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
                "period": 300
            }
        ]
    })
    assert filtered_request == [good_context_processed]
    assert len(errors) == 2


def test_filter_too_many_points():
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=None,  # not needed in this case
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=15,
            max_request_points_signal=10,
            max_signals_per_request=1),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )

    good_context = {
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487682000,
        "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
        "period": 300
    }
    good_context_2 = copy.copy(good_context)
    good_context_2["id"] = "copy"
    good_context_processed = {
        "host": "ASEARCH",
        "st": 1487679300,
        "et": 1487682000,
        "signals": {
            series.TaggedSignalExpr(
                raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                request_key=RequestKey.from_string("itype=mmeta"),
                signal_expr="sum(test_signal_ammm,normal())"
            )
        },
        "period": 300
    }
    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [
            good_context,
        ]
    })
    assert filtered_request == [good_context_processed]
    assert errors == {}

    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": [
            good_context,
            good_context_2
        ]
    })
    assert filtered_request == []
    assert errors.keys() == ["common"]


@pytest.fixture
def mocked_call(monkeypatch):
    monkeypatch.setattr(series, "MultiAttemptRpc", MockedMultiAttemptRpc)


def test_prepare_call(mocked_call):
    cluster_provider = mock.MagicMock()
    cluster_provider.get_cluster_hosts.return_value = [
        ("sas", ["host1", "host2", "host3"])
    ]
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=cluster_provider,
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=40,
            max_request_points_signal=10,
            max_signals_per_request=10),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )
    ctx_list = [
        {
            "host": "ASEARCH",
            "st": 1487679300,
            "et": 1487682000,
            "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
            "period": 300
        },
        {
            "host": "ASEARCH",
            "st": 1487679600,
            "et": 1487679645,
            "signals": ["itype=mmeta:sum(test_signal_ammm,normal())"],
            "period": 5
        }
    ]
    filtered_request, errors = data_processor.check_srv_request({
        "ctxList": ctx_list
    })
    assert errors == {}
    assert len(filtered_request) == 2

    call, requested_context_signals = data_processor.prepare_call(filtered_request)
    ctx_wrappers = {ctx for ctx, _ in requested_context_signals}
    assert len(ctx_wrappers) == 2

    assert not call.host_sequence.empty()
    for i, proto_query in enumerate(call.request.queries):
        assert proto_query.hosts == "ASEARCH"
        assert proto_query.tags == "itype=mmeta"
        assert proto_query.expression == "sum(test_signal_ammm,normal())"
        assert proto_query.from_millis == ctx_list[i]["st"] * 1000
        assert proto_query.to_millis == (ctx_list[i]["et"] + ctx_list[i]["period"]) * 1000
        assert proto_query.grid_millis == ctx_list[i]["period"] * 1000


def test_pack_result():
    cluster_provider = mock.MagicMock()
    cluster_provider.get_cluster_hosts.return_value = [
        ("sas", ["host1", "host2", "host3"])
    ]
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=cluster_provider,
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=40,
            max_request_points_signal=10,
            max_signals_per_request=10),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )
    to_request = [
        {
            "host": "ASEARCH",
            "st": 1487679300,
            "et": 1487679900,
            "signals": {
                series.TaggedSignalExpr(
                    raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                    request_key=RequestKey.from_string("itype=mmeta"),
                    signal_expr="sum(test_signal_ammm,normal())"
                ),
                series.TaggedSignalExpr(
                    raw_tagged_signal_expr="mmeta_self:test_signal_ammm",
                    request_key=RequestKey.from_string("itype=mmeta"),
                    signal_expr="test_signal_ammm"
                )
            },
            "period": 300
        },
        {
            "host": "ASEARCH",
            "st": 1487679600,
            "et": 1487679615,
            "signals": {
                series.TaggedSignalExpr(
                    raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                    request_key=RequestKey.from_string("itype=mmeta"),
                    signal_expr="sum(test_signal_ammm,normal())"
                )
            },
            "period": 5
        }
    ]
    _, requested_context_signals = data_processor.prepare_call(to_request)
    assert len(requested_context_signals) == 3

    proto_response = gateway_service_proto.ReadDataResponse()
    for ctx, tagged_signal_expr in requested_context_signals:
        if ctx.period == 5:
            fill_timeseries(proto_response.timeseries.add(), [
                (1487679600, 0),
                (1487679610, 2)
            ], errors=["error1", "error2"])
        else:
            fill_timeseries(proto_response.timeseries.add(), [
                (1487679300, 0),
                (1487679600, 1),
                (1487679900, 2)
            ], errors=["error1", "error3"])
    errors = collections.defaultdict(list)
    errors["common"].append("common error")
    result = data_processor.pack_result(proto_response, requested_context_signals, errors)
    assert result == {
        "ASEARCH:1487679300_1487679900_300": {
            "content": {
                "timeline": [1487679300, 1487679600, 1487679900],
                "values": {
                    "itype=mmeta:sum(test_signal_ammm,normal())": [0.0, 1.0, 2.0],
                    "mmeta_self:test_signal_ammm": [0.0, 1.0, 2.0]
                }
            }
        },
        "ASEARCH:1487679600_1487679615_5": {
            "content": {
                "timeline": [1487679600, 1487679605, 1487679610, 1487679615],
                "values": {
                    "itype=mmeta:sum(test_signal_ammm,normal())": [0.0, None, 2.0, None]
                }
            }
        },
        "errors": ["common error", "error1", "error2", "error3"],
        "request_id": "aaa-aaa-aaa"
    }


def test_pack_empty_result():
    cluster_provider = mock.MagicMock()
    cluster_provider.get_cluster_hosts.return_value = [
        ("sas", ["host1", "host2", "host3"])
    ]
    data_processor = series.SeriesRequestDataProcessor(
        request_id="aaa-aaa-aaa",
        cluster_provider=cluster_provider,
        unistat=mock.MagicMock(),
        limits=series.HistSeriesLimits(
            max_request_points_total=40,
            max_request_points_signal=10,
            max_signals_per_request=10),
        yasmconf=yasmconf_lite.YasmConfLite(conf={}),
        log=log
    )
    to_request = [
        {
            "host": "ASEARCH",
            "st": 1487679300,
            "et": 1487679900,
            "signals": {
                series.TaggedSignalExpr(
                    raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                    request_key=RequestKey.from_string("itype=mmeta"),
                    signal_expr="sum(test_signal_ammm,normal())"
                ),
                series.TaggedSignalExpr(
                    raw_tagged_signal_expr="mmeta_self:test_signal_ammm",
                    request_key=RequestKey.from_string("itype=mmeta"),
                    signal_expr="test_signal_ammm"
                )
            },
            "period": 300
        },
        {
            "host": "ASEARCH",
            "st": 1487679600,
            "et": 1487679615,
            "signals": {
                series.TaggedSignalExpr(
                    raw_tagged_signal_expr="itype=mmeta:sum(test_signal_ammm,normal())",
                    request_key=RequestKey.from_string("itype=mmeta"),
                    signal_expr="sum(test_signal_ammm,normal())"
                )
            },
            "period": 5
        }
    ]
    _, requested_context_signals = data_processor.prepare_call(to_request)
    assert len(requested_context_signals) == 3

    errors = collections.defaultdict(list)
    errors["common"].append("common error")
    result = data_processor.pack_empty_result(requested_context_signals, errors)
    assert result == {
        "ASEARCH:1487679300_1487679900_300": {
            "content": {
                "timeline": [1487679300, 1487679600, 1487679900],
                "values": {
                    "itype=mmeta:sum(test_signal_ammm,normal())": [None, None, None],
                    "mmeta_self:test_signal_ammm": [None, None, None]
                }
            }
        },
        "ASEARCH:1487679600_1487679615_5": {
            "content": {
                "timeline": [1487679600, 1487679605, 1487679610, 1487679615],
                "values": {
                    "itype=mmeta:sum(test_signal_ammm,normal())": [None, None, None, None]
                }
            }
        },
        "errors": ["common error"],
        "request_id": "aaa-aaa-aaa"
    }
