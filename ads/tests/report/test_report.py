import yatest.common
import pytest
import vh
import json
from ads.ml_engine.lib.report import compose_report, TEMPLATES_RESOURCE_ID
from ads.ml_engine.lib.vh_utils import get_sandbox_resource


TEST_METRICS = [
    {
        "fold": 0,
        "pool": "test",
        "total": {
            "target_stats": {
                "ll_p": 0.5
            }
        },
        "slices": [
            {
                "slice_key": ["test_slice", "page"],
                "slice_value": ["0", "1"],
                "target_stats": {
                    "ll_p": 0.4
                }
            },
            {
                "slice_key": ["test_slice", "page"],
                "slice_value": ["1", "2"],
                "target_stats": {
                    "ll_p": 0.6
                }
            }
        ]
    },
    {
        "fold": 1,
        "pool": "test",
        "total": {
            "target_stats": {
                "ll_p": 0.2
            }
        },
        "slices": [
            {
                "slice_key": ["test_slice", "page"],
                "slice_value": ["0", "1"],
                "target_stats": {
                    "ll_p": 0.1
                }
            },
            {
                "slice_key": ["test_slice", "page"],
                "slice_value": ["1", "2"],
                "target_stats": {
                    "ll_p": 0.3
                }
            }
        ]
    }
]

TEST_META = {
    "owner": "test_owner",
    "workflowURL": "test_workflow_url",
    "description": "test_description"
}

TEST_DUMP_INFO = {
    "http": {
        "proxy": "resource_link"
    }
}

TEST_FSTR = "pos\tfactor\tefficiency\n1\tF1\t100\n2\tF2\t50\n3\tF3\t10\n1\tфактор\t10"


@vh.lazy(object, x=object, out=vh.mkoutput(vh.File))
def dump_json(x, out):
    with open(out, "w") as f:
        json.dump(x, f)


@vh.lazy(object, x=object, out=vh.mkoutput(vh.File))
def dump_str(x, out):
    with open(out, "w") as f:
        f.write(x)


@pytest.mark.xfail(reason="FIXME: 'VhError: Some targets failed, build unsuccessful'", run=False)
def test_run():
    with vh.Graph() as graph, vh.LocalBackend():
        report = vh.File("report.html")
        pools = vh.File("pools")
        dump_json(TEST_METRICS, pools)
        fstr = vh.File("fstr")
        dump_str(TEST_FSTR, fstr)
        meta = vh.File("meta")
        dump_json(TEST_META, meta)
        dump_info = vh.File("dump_info")
        dump_json(TEST_DUMP_INFO, dump_info)
        templates = vh.File("templates")
        get_sandbox_resource(TEMPLATES_RESOURCE_ID, templates)
        compose_report(pools, fstr, meta, dump_info, templates, "nirvana_report.html", report)
    vh.run(graph)
    return yatest.common.canonical_file(report.path())
