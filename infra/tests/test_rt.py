import logging
import mock
import pytest

from solomon.services.gateway.api import yasm_gateway_service_pb2 as gateway_service_proto

from infra.yasm.gateway.lib.handlers import rt
from infra.yasm.gateway.lib.util import yasmconf_lite

from utils import MockedMultiAttemptRpc, fill_timeseries

log = logging.getLogger(__name__)


def test_handle_bad_format():
    data_processor = rt.RtRequestDataProcessor(
        cluster_provider=None,  # not needed in this case
        yasmconf=yasmconf_lite.YasmConfLite(conf={
            "conflist": {
                "qloud": {
                    "tagkeys": {
                        "required": ["prj"]
                    }
                }
            }
        }),
        options=rt.RtOptions(
            signals_limit=2,
            range_start_offset=55,
            range_end_offset=15
        ),
        unistat=mock.MagicMock(),
        log=log
    )

    keys_to_request, invalid_signals = data_processor.parse_requested_signals("oo")
    assert [] == keys_to_request
    assert len(invalid_signals) > 0

    keys_to_request, invalid_signals = data_processor.parse_requested_signals(["ASEARCH:itype=yasmagent"])
    assert [] == keys_to_request
    assert len(invalid_signals) > 0

    with pytest.raises(rt.SignalLimitError):
        data_processor.parse_requested_signals([
            "ASEARCH:itype=yasmagent:signal_summ",
            "ASEARCH:itype=yasmagent:signal_hgram",
            "ASEARCH:itype=yasmagent:signal_dxxx"
        ])

    keys_to_request, invalid_signals = data_processor.parse_requested_signals(["ASEARCH:itype=qloud;signal_summ"])
    assert [] == keys_to_request
    assert len(invalid_signals) > 0

    keys_to_request, invalid_signals = data_processor.parse_requested_signals(["ASEARCH:itype=qloud:signal_summ"])
    assert [] == keys_to_request
    assert len(invalid_signals) > 0

    keys_to_request, invalid_signals = data_processor.parse_requested_signals([":itype=qloud:signal_summ"])
    assert [] == keys_to_request
    assert len(invalid_signals) > 0

    keys_to_request, invalid_signals = data_processor.parse_requested_signals(["ASEARCH:itype=qloud:"])
    assert [] == keys_to_request
    assert len(invalid_signals) > 0


@pytest.fixture
def mocked_call(monkeypatch):
    monkeypatch.setattr(rt, "MultiAttemptRpc", MockedMultiAttemptRpc)


def test_prepare_call(mocked_call):
    cluster_provider = mock.MagicMock()
    cluster_provider.get_cluster_hosts.return_value = [
        ("sas", ["host1", "host2", "host3"])
    ]
    data_processor = rt.RtRequestDataProcessor(
        cluster_provider=cluster_provider,
        yasmconf=yasmconf_lite.YasmConfLite(conf={
            "conflist": {
                "qloud": {
                    "tagkeys": {
                        "required": ["prj"]
                    }
                }
            }
        }),
        options=rt.RtOptions(
            signals_limit=10,
            range_start_offset=55,
            range_end_offset=15
        ),
        unistat=mock.MagicMock(),
        log=log
    )
    keys_to_request, invalid_signals = data_processor.parse_requested_signals([
        "ASEARCH:itype=yasmagent:signal_summ",
        "ASEARCH:itype=yasmagent:signal_hgram",
        "ASEARCH:itype=yasmagent:signal_dxxx"
    ])
    assert invalid_signals == {}
    assert len(keys_to_request) == 3

    call = data_processor.prepare_call(keys_to_request, 10177)

    assert not call.host_sequence.empty()
    for i, proto_query in enumerate(call.request.queries):
        assert proto_query.hosts == "ASEARCH"
        assert proto_query.tags == "itype=yasmagent"
        assert proto_query.expression.startswith("signal_")
        assert proto_query.from_millis == (10175 - 55) * 1000
        assert proto_query.to_millis == 10175 * 1000
        assert proto_query.grid_millis == 5000


def test_pack_result():
    cluster_provider = mock.MagicMock()
    cluster_provider.get_cluster_hosts.return_value = [
        ("sas", ["host1", "host2", "host3"])
    ]
    data_processor = rt.RtRequestDataProcessor(
        cluster_provider=cluster_provider,
        yasmconf=yasmconf_lite.YasmConfLite(conf={
            "conflist": {
                "qloud": {
                    "tagkeys": {
                        "required": ["prj"]
                    }
                }
            }
        }),
        options=rt.RtOptions(
            signals_limit=10,
            range_start_offset=20,
            range_end_offset=5
        ),
        unistat=mock.MagicMock(),
        log=log
    )

    keys_to_request, invalid_signals = data_processor.parse_requested_signals([
        "ASEARCH:itype=yasmagent:signal_summ",
        "ASEARCH:yasmagent_self:signal_dxxx",
        "ASEARCH:itype=yasmagent"
    ])

    now = 10177
    proto_response = gateway_service_proto.ReadDataResponse()
    for key in keys_to_request:
        if key.signal_expr == "signal_summ":
            fill_timeseries(proto_response.timeseries.add(), [
                (10155, 0),
                (10160, 1)
            ], errors=["error1", "error2"])
        else:
            fill_timeseries(proto_response.timeseries.add(), [
                (10155, 0),
                (10165, 2),
                (10170, 3)
            ], errors=["error3"])

    result = data_processor.pack_result(proto_response, keys_to_request, invalid_signals, False, now)
    assert result == {
        "ASEARCH:itype=yasmagent": [
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10155,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10160,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10165,
                "value": None
            }
        ],
        "ASEARCH:itype=yasmagent:signal_summ": [
            {
                "errors": ["error1", "error2"],
                "timestamp": 10155,
                "value": 0.0
            },
            {
                "errors": ["error1", "error2"],
                "timestamp": 10160,
                "value": 1.0
            },
            {
                "errors": ["error1", "error2"],
                "timestamp": 10165,
                "value": None
            }
        ],
        "ASEARCH:yasmagent_self:signal_dxxx": [
            {
                "errors": ["error3"],
                "timestamp": 10155,
                "value": 0.0
            },
            {
                "errors": ["error3"],
                "timestamp": 10160,
                "value": None
            },
            {
                "errors": ["error3"],
                "timestamp": 10165,
                "value": 2.0
            }
        ]
    }

    result = data_processor.pack_result(proto_response, keys_to_request, invalid_signals, True, now)
    assert result == {
        "ASEARCH:itype=yasmagent": [
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10155,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10160,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10165,
                "value": None
            }
        ],
        "ASEARCH:itype=yasmagent:signal_summ": [
            {
                "timestamp": 10155,
                "value": 0.0
            },
            {
                "timestamp": 10160,
                "value": 1.0
            },
            {
                "timestamp": 10165,
                "value": None
            }
        ],
        "ASEARCH:yasmagent_self:signal_dxxx": [
            {
                "timestamp": 10155,
                "value": 0.0
            },
            {
                "timestamp": 10160,
                "value": None
            },
            {
                "timestamp": 10165,
                "value": 2.0
            }
        ]
    }
    result = data_processor.pack_result(None, [], invalid_signals, True, now)
    assert result == {
        "ASEARCH:itype=yasmagent": [
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10155,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10160,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10165,
                "value": None
            }
        ]
    }

    result = data_processor.pack_result(None, keys_to_request, invalid_signals, False, now,
                                        request_errors=["request error"])
    assert result == {
        "ASEARCH:itype=yasmagent": [
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10155,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10160,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10165,
                "value": None
            }
        ],
        "ASEARCH:itype=yasmagent:signal_summ": [
            {
                "errors": ["request error"],
                "timestamp": 10155,
                "value": None
            },
            {
                "errors": ["request error"],
                "timestamp": 10160,
                "value": None
            },
            {
                "errors": ["request error"],
                "timestamp": 10165,
                "value": None
            }
        ],
        "ASEARCH:yasmagent_self:signal_dxxx": [
            {
                "errors": ["request error"],
                "timestamp": 10155,
                "value": None
            },
            {
                "errors": ["request error"],
                "timestamp": 10160,
                "value": None
            },
            {
                "errors": ["request error"],
                "timestamp": 10165,
                "value": None
            }
        ]
    }

    result = data_processor.pack_result(None, keys_to_request, invalid_signals, True, now,
                                        request_errors=["request error"])
    assert result == {
        "ASEARCH:itype=yasmagent": [
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10155,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10160,
                "value": None
            },
            {
                "errors": ["Wrong request ASEARCH:itype=yasmagent, need more than 2 values to unpack"],
                "timestamp": 10165,
                "value": None
            }
        ],
        "ASEARCH:itype=yasmagent:signal_summ": [
            {
                "timestamp": 10155,
                "value": None
            },
            {
                "timestamp": 10160,
                "value": None
            },
            {
                "timestamp": 10165,
                "value": None
            }
        ],
        "ASEARCH:yasmagent_self:signal_dxxx": [
            {
                "timestamp": 10155,
                "value": None
            },
            {
                "timestamp": 10160,
                "value": None
            },
            {
                "timestamp": 10165,
                "value": None
            }
        ]
    }
