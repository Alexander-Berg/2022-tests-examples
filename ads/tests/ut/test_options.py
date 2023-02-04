import pytest
import math
from uuid import uuid4
from ads.quality.metric_eval.pylib import metric_eval
from ads.quality.metric_eval.tests.lib import check_result


def reverse_sigmoid(y):
    return -math.log(1.0 / float(y) - 1)


@pytest.fixture(scope='module')
def test_table(local_yt):
    data = [
        {"slice": 0, "p": 0.1, "t": 0, "w": 1, "q": 0},
        {"slice": 0, "p": 0.3, "t": 0, "w": 2, "q": 1},
        {"slice": 0, "p": 0.5, "t": 1, "w": 3, "q": 0},
        {"slice": 0, "p": 0.13, "t": 1, "w": 4, "q": 1},

        {"slice": 1, "p": 0.8, "t": 0, "w": 1, "q": 0},
        {"slice": 1, "p": 0.9, "t": 0, "w": 10, "q": 1},
        {"slice": 1, "p": 0.3, "t": 1, "w": 2, "q": 0},
        {"slice": 1, "p": 0.4, "t": 1, "w": 4, "q": 1},
    ]

    for row in data:
        row["p_logit"] = reverse_sigmoid(row["p"])
        row["p_log"] = math.log(row["p"])

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


@pytest.mark.parametrize('transform_prediction, prediction', [("Sigmoid", "p_logit"), ("Exp", "p_log")])
def test_transform_prediction(test_table, local_yt, transform_prediction, prediction):
    metrics1 = metric_eval(
        src_table=test_table,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        qid=None,
        metrics=["log_loss", "auc"],
        transform_prediction=None,
        memory_limit=2,
    )

    metrics2 = metric_eval(
        src_table=test_table,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction=prediction,
        qid=None,
        metrics=["log_loss", "auc"],
        transform_prediction=transform_prediction,
        memory_limit=2,
    )

    check_result(metrics1, metrics2)


def test_wrong_transform_argument(test_table, local_yt):
    with pytest.raises(RuntimeError):
        metric_eval(
            src_table=test_table,
            slices=None,
            weight=None,
            total=True,
            yt_token="",
            yt_proxy=local_yt.get_client().config['proxy']['url'],
            target="t",
            prediction="p",
            qid=None,
            metrics=["log_loss", "auc"],
            transform_prediction="BestTransform",
            memory_limit=2,
        )


@pytest.mark.parametrize('metrics', [[], ["SuperMetric"], ["SuperMetric", "log_loss"]])
def test_wrong_metrics(test_table, local_yt, metrics):
    with pytest.raises(RuntimeError):
        metric_eval(
            src_table=test_table,
            slices=None,
            weight=None,
            total=False,
            yt_token="",
            yt_proxy=local_yt.get_client().config['proxy']['url'],
            target="t",
            prediction="p",
            qid=None,
            metrics=metrics,
            transform_prediction=None,
            memory_limit=2,
        )


def test_dry_run_existed_params():
    metrics = metric_eval(
        src_table=None,
        yt_token="",
        yt_proxy="",
        target="t",
        prediction="p",
        qid=None,
        metrics=["log_loss", "auc"],
        transform_prediction=None,
        memory_limit=2,
        dry_run=True,
    )

    assert metrics == {"slices": []}


@pytest.mark.parametrize('transform_prediction, metrics', [(None, ["SuperMetric"]), ("WrongTransform", ["log_loss"])])
def test_dry_run_no_existed_params(transform_prediction, metrics):
    with pytest.raises(RuntimeError):
        metric_eval(
            src_table=None,
            yt_token="",
            yt_proxy="",
            target="t",
            prediction="p",
            qid=None,
            metrics=metrics,
            transform_prediction=transform_prediction,
            memory_limit=2,
            dry_run=True,
        )


@pytest.fixture(scope='module')
def test_no_binary_table(local_yt):
    data = [
        {"slice": 0, "p": 0.1, "t": 0.1, "w": 1, "q": 0},
        {"slice": 0, "p": 0.3, "t": 0.2, "w": 2, "q": 1},
        {"slice": 0, "p": 0.5, "t": 0.6, "w": 3, "q": 0},
        {"slice": 0, "p": 0.13, "t": 1.0, "w": 4, "q": 1},

        {"slice": 1, "p": 0.8, "t": 0.0, "w": 1, "q": 0},
        {"slice": 1, "p": 0.9, "t": 0.2, "w": 10, "q": 1},
        {"slice": 1, "p": 0.3, "t": 1.0, "w": 2, "q": 0},
        {"slice": 1, "p": 0.4, "t": 1.0, "w": 4, "q": 1},
    ]

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


@pytest.mark.parametrize('metrics', [["auc"], ["q_auc"]])
def test_no_binary_target_fails(test_no_binary_table, local_yt, metrics):
    with pytest.raises(RuntimeError):
        metric_eval(
            src_table=test_no_binary_table,
            yt_token="",
            yt_proxy=local_yt.get_client().config['proxy']['url'],
            target="t",
            prediction="p",
            qid="q",
            metrics=metrics,
            transform_prediction=None,
            memory_limit=2,
        )
