from __future__ import print_function


import vh
import pytest
import math
import tempfile
import json
import os
from copy import copy
from uuid import uuid4
from ads.quality.metric_eval.pylib import metric_eval as py_metric_eval_inner
from ads.quality.metric_eval.vh import metric_eval as vh_metric_eval
from ads.quality.metric_eval.tests.lib import check_result


def py_metric_eval(**kwargs):
    inner_kwargs = copy(kwargs)

    output_path = kwargs['output_path']
    inner_kwargs.pop('output_path', None)

    metrics = py_metric_eval_inner(**inner_kwargs)
    with open(output_path, "w") as f:
        json.dump(metrics, f)


def calc_logloss(data):
    return - sum(d["t"] * math.log(d["p"]) + (1 - d["t"]) * math.log(1 - d["p"]) for d in data) / len(data)


def calc_weighted_logloss(data):
    return - sum(d["w"] * (d["t"] * math.log(d["p"]) + (1 - d["t"]) * math.log(1 - d["p"])) for d in data) / sum(d["w"] for d in data)


def reverse_sigmoid(y):
    return - math.log(1.0 / float(y) - 1)


@pytest.fixture
def metric_eval_kwargs(local_yt):
    # connected with logloss_table
    # don't delete file - Valhalla automatically delete files in case of errors, and ya make launch
    # tests in isolated cwd's
    tmp = tempfile.NamedTemporaryFile(delete=False)
    yield dict(
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        qid=None,
        metrics=["log_loss"],
        transform_prediction="None",
        memory_limit=2,
        output_path=tmp.name
    )


@pytest.fixture(scope='module')
def logloss_table(local_yt):
    data = [
        {"x1": 0, "x2": 0, "p": 0.1, "p_logit": reverse_sigmoid(0.1), "t": 0, "w": 1},
        {"x1": 0, "x2": 0, "p": 0.2, "p_logit": reverse_sigmoid(0.2), "t": 0, "w": 2},
        {"x1": 0, "x2": 0, "p": 0.3, "p_logit": reverse_sigmoid(0.3), "t": 1, "w": 3},
        {"x1": 0, "x2": 0, "p": 0.7, "p_logit": reverse_sigmoid(0.7), "t": 1, "w": 4},

        {"x1": 0, "x2": 1, "p": 0.8, "p_logit": reverse_sigmoid(0.8), "t": 0, "w": 1},
        {"x1": 0, "x2": 1, "p": 0.9, "p_logit": reverse_sigmoid(0.9), "t": 0, "w": 2},
        {"x1": 0, "x2": 1, "p": 0.1, "p_logit": reverse_sigmoid(0.1), "t": 1, "w": 3},
        {"x1": 0, "x2": 1, "p": 0.2, "p_logit": reverse_sigmoid(0.2), "t": 1, "w": 4},

        {"x1": 1, "x2": 0, "p": 0.5, "p_logit": reverse_sigmoid(0.5), "t": 0, "w": 1},
        {"x1": 1, "x2": 0, "p": 0.5, "p_logit": reverse_sigmoid(0.5), "t": 0, "w": 2},
        {"x1": 1, "x2": 0, "p": 0.5, "p_logit": reverse_sigmoid(0.5), "t": 1, "w": 3},
        {"x1": 1, "x2": 0, "p": 0.5, "p_logit": reverse_sigmoid(0.5), "t": 1, "w": 4},

        {"x1": 1, "x2": 1, "p": 0.1, "p_logit": reverse_sigmoid(0.1), "t": 0, "w": 1},
        {"x1": 1, "x2": 1, "p": 0.1, "p_logit": reverse_sigmoid(0.1), "t": 0, "w": 2},
        {"x1": 1, "x2": 1, "p": 0.9, "p_logit": reverse_sigmoid(0.9), "t": 1, "w": 3},
        {"x1": 1, "x2": 1, "p": 0.9, "p_logit": reverse_sigmoid(0.9), "t": 1, "w": 4}
    ]

    def calc_reference(fn):
        return {
            "total": round(fn(data), 5),
            "x1": {
                0: round(fn(data[:8]), 5),
                1: round(fn(data[8:]), 5)
            },
            "x2": {
                0: round(fn(data[:4] + data[8:12]), 5),
                1: round(fn(data[4:8] + data[12:]), 5)
            },
            "x1,x2": {
                (0, 0): round(fn(data[:4]), 5),
                (0, 1): round(fn(data[4:8]), 5),
                (1, 0): round(fn(data[8:12]), 5),
                (1, 1): round(fn(data[12:]), 5)
            }
        }

    reference = calc_reference(calc_logloss)
    weighted_reference = calc_reference(calc_weighted_logloss)

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path, reference, weighted_reference


def vh_run_metric_eval(*args, **kwargs):
    with vh.Graph() as graph, vh.LocalBackend():
        res = vh_metric_eval(*args, **kwargs)
    keeper = vh.run_async(graph, always_make=True, yt_proxy=kwargs["yt_proxy"], secrets={'': '123'},
                          yt_token="123", yt_token_secret="")
    keeper.download(res)


@pytest.mark.parametrize('metric_eval', [vh_run_metric_eval, py_metric_eval], ids=["vh", "py"])
@pytest.mark.parametrize('slice_arg', [["x1"], [[0]]])
def test_improper_slice_argument(logloss_table, slice_arg, metric_eval_kwargs, metric_eval):
    table, unweighted_reference, weighted_reference = logloss_table
    with pytest.raises(AssertionError):
        metric_eval(
            src_table=table,
            slices=slice_arg,
            weight=None,
            total=False,
            **metric_eval_kwargs
        )


@pytest.mark.parametrize('metric_eval', [vh_run_metric_eval, py_metric_eval], ids=["vh", "py"])
def test_improper_transform_argument(logloss_table, metric_eval, local_yt):
    table, unweighted_reference, weighted_reference = logloss_table
    with pytest.raises(RuntimeError):
        metric_eval(
            src_table=table,
            slices=None,
            weight=None,
            total=True,
            yt_token="",
            yt_proxy=local_yt.get_client().config['proxy']['url'],
            target="t",
            prediction="p",
            qid=None,
            metrics=["log_loss"],
            transform_prediction="SOMEWEIRDTRANSFORM",
            memory_limit=2,
            output_path="./metrics.json"
        )


@pytest.mark.parametrize('metric_eval', [vh_run_metric_eval, py_metric_eval], ids=["vh", "py"])
def test_transform_sigmoid(logloss_table, local_yt, metric_eval):
    table, unweighted_reference, weighted_reference = logloss_table
    with tempfile.NamedTemporaryFile() as tmp1, tempfile.NamedTemporaryFile() as tmp2:
        metric_eval(
            src_table=table,
            yt_token="",
            yt_proxy=local_yt.get_client().config['proxy']['url'],
            target="t",
            prediction="p",
            qid=None,
            metrics=["log_loss"],
            transform_prediction=None,  # usual None is also tested here
            memory_limit=2,
            output_path=tmp1.name
        )

        metric_eval(
            src_table=table,
            yt_token="",
            yt_proxy=local_yt.get_client().config['proxy']['url'],
            target="t",
            prediction="p_logit",
            qid=None,
            metrics=["log_loss"],
            transform_prediction="Sigmoid",
            memory_limit=2,
            output_path=tmp2.name
        )

        with open(tmp1.name) as f:
            dct1 = json.load(f)

        with open(tmp2.name) as f:
            dct2 = json.load(f)

        check_result(dct1, dct2)


@pytest.mark.parametrize('metric_eval', [vh_run_metric_eval, py_metric_eval], ids=["vh", "py"])
def test_only_total(logloss_table, metric_eval_kwargs, metric_eval):
    table, unweighted_reference, weighted_reference = logloss_table
    metric_eval(
        src_table=table,
        slices=None,
        total=True,
        weight=None,
        **metric_eval_kwargs
    )
    reference = unweighted_reference
    with open(metric_eval_kwargs["output_path"], 'rt') as f:
        res = json.load(f)
    assert round(res["total"]["target_stats"]["log_loss"], 5) == reference["total"]
    assert res["slices"] == []


@pytest.mark.parametrize('metric_eval', [vh_run_metric_eval, py_metric_eval], ids=["vh", "py"])
def test_slice_without_total(logloss_table, metric_eval_kwargs, metric_eval):
    table, unweighted_reference, weighted_reference = logloss_table
    metric_eval(
        src_table=table,
        slices=[["x1"]],
        weight=None,
        total=False,
        **metric_eval_kwargs
    )
    reference = unweighted_reference
    with open(metric_eval_kwargs["output_path"], 'rt') as f:
        res = json.load(f)
    assert "total" not in res
    assert len(res["slices"]) == 2
    for cur_slice in res["slices"]:
        assert cur_slice["slice_key"] == ["x1"]
        slice_value = int(cur_slice["slice_value"][0])
        assert round(cur_slice["target_stats"]["log_loss"], 5) == reference["x1"][slice_value]


@pytest.mark.parametrize('metric_eval', [vh_run_metric_eval, py_metric_eval], ids=["vh", "py"])
def test_run_with_qid(logloss_table, metric_eval_kwargs, local_yt, metric_eval):
    table, unweighted_reference, weighted_reference = logloss_table
    metric_eval(
        src_table=table,
        slices=[["x1"]],
        weight=None,
        total=False,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        qid=["x2"],
        metrics=["log_loss", "q_auc"],
        transform_prediction="None",
        memory_limit=2,
        output_path=metric_eval_kwargs["output_path"]
    )
    # FIXME: check result value


@pytest.mark.parametrize('metric_eval', [vh_run_metric_eval, py_metric_eval], ids=["vh", "py"])
@pytest.mark.parametrize('weight', [None, "w"], ids=["Usual", "Weighted"])
def test_all_slices(logloss_table, metric_eval_kwargs, weight, metric_eval):
    table, unweighted_reference, weighted_reference = logloss_table
    metric_eval(
        src_table=table,
        slices=[["x1"], ["x2"], ["x1", "x2"]],
        weight=weight,
        total=True,
        **metric_eval_kwargs
    )
    reference = unweighted_reference if weight is None else weighted_reference
    with open(metric_eval_kwargs["output_path"], 'rt') as f:
        res = json.load(f)
    assert round(res["total"]["target_stats"]["log_loss"], 5) == reference["total"]
    assert len(res["slices"]) == 8
    for cur_slice in res["slices"]:
        slice_name = ",".join(cur_slice["slice_key"])
        if slice_name == "x1,x2":
            slice_value = (int(cur_slice["slice_value"][0]), int(cur_slice["slice_value"][1]))
        else:
            slice_value = int(cur_slice["slice_value"][0])
        assert round(cur_slice["target_stats"]["log_loss"], 5) == reference[slice_name][slice_value]


# Test the environment context


@pytest.mark.parametrize('metric_eval', [vh_run_metric_eval, py_metric_eval], ids=["vh", "py"])
def test_metric_eval_gives_clean_environment(logloss_table, metric_eval_kwargs, metric_eval):
    table, unweighted_reference, weighted_reference = logloss_table

    os.environ["VALUE1"] = "123"
    os.environ["VALUE4"] = "456"
    os.environ["YT_POOL"] = "some_auto_ml_pool"
    os.environ["YT_TOKEN"] = "POFD"
    os.environ["YT_PROXY"] = "dead_banach"

    prev_environ = os.environ.copy()

    metric_eval(
        src_table=table,
        slices=None,
        total=True,
        weight=None,
        **metric_eval_kwargs
    )

    assert os.environ == prev_environ
