import pytest
import logging
from tornado import web

from infra.yasm.gateway.lib.handlers import local
from infra.yasm.gateway.lib.util import yasmconf_lite, functions

log = logging.getLogger(__name__)


def test_extract_yasmconf_elements():
    conflist = {
        "qloud": {
            "tagkeys": {
                "required": ["prj"]
            }
        },
        "yasmagent": {
            "tagkeys": {}
        },
        "all": {
            "charts": {
                "porto_cpu": {
                    "chartTitle": "porto cpu",
                    "owner": [
                        "yoschi"
                    ],
                    "signalCommonOpt": {
                        "width": 2
                    },
                    "signalList": [
                        [
                            "aver~portoinst-cpu_usage_cores_tmmv",
                            {
                                "color": "#EE9900",
                                "title": "Used cores"
                            }
                        ]
                    ]
                },
                "porto_cpu_hgram": {
                    "chartTitle": "CPU statistics hgrams",
                    "signalList": [
                        [
                            "havg(portoinst-cpu_limit_cores_thhh)",
                            {
                                "color": "#37bff2",
                                "normalizable": False,
                                "yAxis": 0
                            }
                        ],
                        [
                            "havg(portoinst-cpu_guarantee_cores_thhh)",
                            {
                                "color": "#169833",
                                "normalizable": False
                            }
                        ]
                    ]
                }
            },
            "patterns": {}
        }
    }

    yasmconf = yasmconf_lite.YasmConfLite(conf={
        "conflist": conflist
    })

    assert sorted(local.extract_sub_response(yasmconf.get_conflist(), "all/charts/list")) == [
        "porto_cpu",
        "porto_cpu_hgram"
    ]

    assert sorted(local.extract_sub_response(yasmconf.get_conflist(), "list")) == [
        "all",
        "qloud",
        "yasmagent"
    ]

    assert local.extract_sub_response(yasmconf.get_conflist(), "all/charts/porto_cpu/chartTitle") == "porto cpu"
    assert local.extract_sub_response(yasmconf.get_conflist(), "all/charts/porto_cpu/chartTitle/ddd") == "porto cpu"
    assert local.extract_sub_response(yasmconf.get_conflist(), "all/charts/porto_cpu/owner/") == ["yoschi"]
    with pytest.raises(web.HTTPError) as ex:
        local.extract_sub_response(yasmconf.get_conflist(), "all/charts/porto_cpu/owner/something")
    assert 400 <= ex.value.status_code < 500

    assert local.extract_sub_response(yasmconf.get_conflist(), "") == conflist


def test_functions():
    assert local.FunctionsHandler.make_full_response_for_args({}) == {
        "result": functions.FUNCS,
        "total": len(functions.FUNCS)
    }

    assert local.FunctionsHandler.make_full_response_for_args({"limit": [2]}) == {
        "result": functions.FUNCS[0:2],
        "total": len(functions.FUNCS)
    }

    assert local.FunctionsHandler.make_full_response_for_args({"limit": [2], "offset": [2]}) == {
        "result": functions.FUNCS[2:4],
        "total": len(functions.FUNCS)
    }

    assert local.FunctionsHandler.make_full_response_for_args({"limit": [2], "offset": [2], "sorted": ["desc"]}) == {
        "result": list(reversed(functions.FUNCS))[2:4],
        "total": len(functions.FUNCS)
    }

    assert local.FunctionsHandler.make_full_response_for_args({"limit": [2], "offset": [2], "sorted": ["desc"]}) == {
        "result": list(reversed(functions.FUNCS))[2:4],
        "total": len(functions.FUNCS)
    }

    assert local.FunctionsHandler.make_full_response_for_args({
        "limit": [1],
        "offset": [2],
        "sorted": ["desc"],
        "function_pattern": ["h.*"]
    }) == {
        "total": 5,
        "result": [
            {
                "name": "hmerge",
                "normalizable": True,
                "args": [
                    {
                        "repeat": {"max": "inf", "min": 1},
                        "name": "hgram-signal",
                        "types": ["signal"]
                    }
                ]
            }
        ]
    }
