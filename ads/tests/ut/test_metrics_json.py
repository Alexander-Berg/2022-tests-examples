import pytest
from uuid import uuid4
from ads.quality.metric_eval.pylib import metric_eval
from ads.quality.metric_eval.tests.lib import check_result


@pytest.fixture(scope='module')
def test_table(local_yt):
    data = [
        {"slice": 0, "p": 0.1, "t": 0, "w": 1, "q": 0, "q0": "hello"},
        {"slice": 0, "p": 0.3, "t": 0, "w": 2, "q": 1, "q0": "hello"},
        {"slice": 0, "p": 0.5, "t": 1, "w": 3, "q": 0, "q0": "hello"},
        {"slice": 0, "p": 0.13, "t": 1, "w": 4, "q": 1, "q0": "hello"},

        {"slice": 1, "p": 0.8, "t": 0, "w": 1, "q": 0, "q0": "hello"},
        {"slice": 1, "p": 0.9, "t": 0, "w": 10, "q": 1, "q0": "hello"},
        {"slice": 1, "p": 0.3, "t": 1, "w": 2, "q": 0, "q0": "hello"},
        {"slice": 1, "p": 0.4, "t": 1, "w": 4, "q": 1, "q0": "hello"},
    ]

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


@pytest.fixture(scope='module')
def true_weighted_noq_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                "log_loss": 1.546941865,
                "ll_p": -1.77469071,
                "shows": 27,
                "clicks": 13,
                "ctr_factor": 0.8831521841,
                "ctr": 0.4814814815,
                "uplift@50": 0.3846153846,
                "uplift@10": 0,
                "auc": 0.1593406593,
                "non_normalized_auc": 0.1593406593,
            }
        },
        "slices": [
            {"slice_key": "slice", "slice_value": "1", "target_stats":
                {
                    "log_loss": 1.806376171,
                    "ll_p": -3.278528814,
                    "shows": 17,
                    "clicks": 6,
                    "ctr_factor": 0.5000000074,
                    "ctr": 0.3529411765,
                    "uplift@50": 0,
                    "uplift@10": 0,
                    "auc": 0,
                    "non_normalized_auc": 0,
                }
             },
            {"slice_key": "slice", "slice_value": "0", "target_stats":
                {
                    "log_loss": 1.105903544,
                    "ll_p": -0.7071989173,
                    "shows": 10,
                    "clicks": 7,
                    "ctr_factor": 2.573529405,
                    "ctr": 0.7,
                    "uplift@50": 0.8571428571,
                    "uplift@10": 1.428571429,
                    "auc": 0.619047619,
                    "non_normalized_auc": 0.619047619,
                }
            }
        ]
    }
    yield true_metrics


@pytest.fixture(scope='module')
def true_unweighted_noq_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                "log_loss": 1.153461229,
                "ll_p": -0.9206280969,
                "shows": 8,
                "clicks": 4,
                "ctr_factor": 1.166180753,
                "ctr": 0.5,
                "uplift@50": 1,
                "uplift@10": 0,
                "auc": 0.40625,
                "non_normalized_auc": 0.40625,
            }
        },
        "slices": [
            {"slice_key": "slice", "slice_value": "1", "target_stats":
                {
                    "log_loss": 1.508071577,
                    "ll_p": -1.629848793,
                    "shows": 4,
                    "clicks": 2,
                    "ctr_factor": 0.8333333311,
                    "ctr": 0.5,
                    "uplift@50": 0,
                    "uplift@10": 0,
                    "auc": 0,
                    "non_normalized_auc": 0,
                }
             },
            {"slice_key": "slice", "slice_value": "0", "target_stats":
                {
                    "log_loss": 0.798850881,
                    "ll_p": -0.2114074009,
                    "shows": 4,
                    "clicks": 2,
                    "ctr_factor": 1.941747556,
                    "ctr": 0.5,
                    "uplift@50": 1,
                    "uplift@10": 2.00000003,
                    "auc": 0.75,
                    "non_normalized_auc": 0.75,
                }
             }
        ]
    }
    yield true_metrics


@pytest.mark.parametrize('qid', [None, ["q"]])
def test_weighted_noq_metrics(local_yt, test_table, true_weighted_noq_metrics, qid):
        metrics = metric_eval(
            src_table=test_table,
            yt_token="",
            yt_proxy=local_yt.get_client().config['proxy']['url'],
            target="t",
            prediction="p",
            weight="w",
            qid=qid,
            slices=[["slice"]],
            metrics=["log_loss", "ctr_factor", "uplift@10", "uplift@50", "auc", "non_normalized_auc"],
            transform_prediction=None,
            memory_limit=2,
        )

        check_result(true_weighted_noq_metrics, metrics)


@pytest.mark.parametrize('qid', [None, ["q"]])
def test_unweighted_noq_metrics(local_yt, test_table, true_unweighted_noq_metrics, qid):
    metrics = metric_eval(
            src_table=test_table,
            yt_token="",
            yt_proxy=local_yt.get_client().config['proxy']['url'],
            target="t",
            prediction="p",
            weight=None,
            qid=qid,
            slices=[["slice"]],
            metrics=["log_loss", "ctr_factor", "uplift@10", "uplift@50", "auc", "non_normalized_auc"],
            transform_prediction=None,
            memory_limit=2,
        )

    check_result(true_unweighted_noq_metrics, metrics)


@pytest.mark.parametrize('qid', [None, ["q"]])
def test_transaction(local_yt, test_table, true_unweighted_noq_metrics, qid):
    client = local_yt.get_client()
    with client.Transaction():
        metrics = metric_eval(
                src_table=test_table,
                target="t",
                prediction="p",
                weight=None,
                qid=qid,
                slices=[["slice"]],
                metrics=["log_loss", "ctr_factor", "uplift@10", "uplift@50", "auc", "non_normalized_auc"],
                transform_prediction=None,
                memory_limit=2,
                client=client,
            )

    check_result(true_unweighted_noq_metrics, metrics)


@pytest.fixture(scope='module')
def true_weighted_q_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                "q_uplift@50": 0.2592592593,
                "q_softmax": 4.434946909,
                "q_softmax_const": 2.591954533,
                "q_softmax_gain": -71.10434818,
                "q_auc": 0.1913580247,
                "q_uplift@10": 0,
                "q_non_normalized_auc": 0.1226415094,
                "q_pt_ratio_stdev": 0.3790672862,
                "q_pt_ratio_v2_mean": -0.006599195577,
                "q_p_group_stat_v2_mean": 0.5674765596,
                "q_p_group_stat_v2_coverage": 1,
                "q_pt_ratio_v2_coverage": 1,
                "q_p_group_stat_coverage": 1,
                "q_p_group_stat_mean": 0.5451851609,
                "q_pt_ratio_mean": 1.240740683,
                "q_p_group_stat_stdev": 0.06898961188,
                "q_pt_ratio_coverage": 1,
                "q_pt_ratio_v2_stdev": 0.203723594,
                "q_p_group_stat_v2_stdev": 0.09046988857,
                "q_softfmax2": 2.832398711,
                "q_softfmax2_const": 2.591954533,
                "q_softfmax2_gain": -9.27655848,
                "rmse": 0.7485195153
            }
        },
        "slices": [
            {"slice_key": "slice", "slice_value": "1", "target_stats":
                {
                    "q_uplift@50": 0.08823529412,
                    "q_softmax": 4.098898597,
                    "q_softmax_const": 2.125575649,
                    "q_softmax_gain": -92.83710739,
                    "q_auc": 0,
                    "q_uplift@10": 0,
                    "q_non_normalized_auc": 0,
                    "q_p_group_stat_v2_stdev": 0.132539003,
                    "q_pt_ratio_v2_stdev": 0.247395181,
                    "q_pt_ratio_coverage": 1,
                    "q_p_group_stat_stdev": 0.1107353387,
                    "q_pt_ratio_mean": 2.305882233,
                    "q_p_group_stat_mean": 0.7058823407,
                    "q_p_group_stat_coverage": 1,
                    "q_pt_ratio_v2_coverage": 1,
                    "q_p_group_stat_v2_coverage": 1,
                    "q_p_group_stat_v2_mean": 0.6905673836,
                    "q_pt_ratio_v2_mean": 0.2668166722,
                    "q_pt_ratio_stdev": 0.7433790301,
                    "q_softfmax2": 2.444659521,
                    "q_softfmax2_const": 2.125575649,
                    "q_softfmax2_gain": -15.01164504,
                    "rmse": 0.8102287074
                }
             },
            {"slice_key": "slice", "slice_value": "0", "target_stats":
                {
                    "q_uplift@50": 0.8333333333,
                    "q_softmax": 1.786909972,
                    "q_softmax_const": 1.617988709,
                    "q_softmax_gain": -10.44020038,
                    "q_auc": 0.4,
                    "q_uplift@10": 0.5333333413,
                    "q_non_normalized_auc": 0.2727272727,
                    "q_p_group_stat_v2_stdev": 0.1114512886,
                    "q_pt_ratio_v2_stdev": 0.07811047301,
                    "q_pt_ratio_coverage": 1,
                    "q_p_group_stat_stdev": 0.1045115649,
                    "q_pt_ratio_mean": 0.3813333452,
                    "q_p_group_stat_mean": 0.2720000029,
                    "q_p_group_stat_coverage": 1,
                    "q_pt_ratio_v2_coverage": 1,
                    "q_p_group_stat_v2_coverage": 1,
                    "q_p_group_stat_v2_mean": 0.2906433515,
                    "q_pt_ratio_v2_mean": -0.5096698165,
                    "q_pt_ratio_stdev": 0.1241074933,
                    "q_softfmax2": 1.615373913,
                    "q_softfmax2_const": 1.617988709,
                    "q_softfmax2_gain": 0.1616077568,
                    "rmse": 0.6298888837
                }
             }
        ]
    }
    yield true_metrics


@pytest.fixture(scope='module')
def true_unweighted_q_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                "q_uplift@50": 1,
                "q_softmax": 2.807777443,
                "q_softmax_const": 1.386294361,
                "q_softmax_gain": -102.5383296,
                "q_auc": 0.375,
                "q_uplift@10": 0,
                "q_non_normalized_auc": 0.375,
            }
        },
        "slices": [
            {"slice_key": "slice", "slice_value": "1", "target_stats":
                {
                    "q_uplift@50": 0,
                    "q_softmax": 2.504761722,
                    "q_softmax_const": 0.6931471805,
                    "q_softmax_gain": -261.3607314,
                    "q_auc": 0,
                    "q_uplift@10": 0,
                    "q_non_normalized_auc": 0,
                }
             },
            {"slice_key": "slice", "slice_value": "0", "target_stats":
                {
                    "q_uplift@50": 1,
                    "q_softmax": 0.7290661616,
                    "q_softmax_const": 0.6931471805,
                    "q_softmax_gain": -5.182012592,
                    "q_auc": 0.5,
                    "q_uplift@10": 1.000000015,
                    "q_non_normalized_auc": 0.5,
                }
             }
        ]
    }
    yield true_metrics


@pytest.mark.parametrize('qid', [["q"], ["q", "q0"]])
def test_weighted_q_metrics(local_yt, test_table, true_weighted_q_metrics, qid):
    metrics = metric_eval(
        src_table=test_table,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        weight="w",
        qid=qid,
        slices=[["slice"]],
        metrics=[
            "q_uplift@50", "q_softmax", "q_softfmax2@q_softmax@func=none", "q_auc", "q_uplift@10", "q_non_normalized_auc",
            "q_pt_ratio", "q_pt_ratio_v2@q_pt_ratio@func=exp:group_func=log:group_weight=no",
            "q_p_group_stat", "q_p_group_stat_v2@q_p_group_stat@func=exp:group_func=log:group_weight=sqrt", "rmse",
        ],
        transform_prediction=None,
        memory_limit=2,
    )

    check_result(true_weighted_q_metrics, metrics)


def test_unweighted_q_metrics(local_yt, test_table, true_unweighted_q_metrics):
    metrics = metric_eval(
        src_table=test_table,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        weight=None,
        qid=["q"],
        slices=[["slice"]],
        metrics=["q_uplift@50", "q_softmax", "q_auc", "q_uplift@10", "q_non_normalized_auc"],
        transform_prediction=None,
        memory_limit=2,
    )

    check_result(true_unweighted_q_metrics, metrics)


@pytest.fixture(scope='module')
def test_table_with_zero_target(local_yt):
    data = [
        {"slice": 0, "p": 0.1, "t": 0, "w": 1, "q": 0},
        {"slice": 0, "p": 0.3, "t": 0, "w": 2, "q": 1},
        {"slice": 0, "p": 0.5, "t": 0, "w": 3, "q": 0},
        {"slice": 0, "p": 0.13, "t": 0, "w": 4, "q": 1},

        {"slice": 1, "p": 0.8, "t": 0, "w": 1, "q": 0},
        {"slice": 1, "p": 0.9, "t": 0, "w": 10, "q": 1},
        {"slice": 1, "p": 0.3, "t": 0, "w": 2, "q": 0},
        {"slice": 1, "p": 0.4, "t": 0, "w": 4, "q": 1},
    ]

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


def test_none_result(local_yt, test_table_with_zero_target):
    metrics = metric_eval(
        src_table=test_table_with_zero_target,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        weight=None,
        qid=["q"],
        slices=[["slice"]],
        metrics=["q_uplift@10", "q_auc", "uplift@10", "auc", "log_loss"],
        transform_prediction=None,
        memory_limit=2,
    )

    def is_target_stats_nan(target_stats):
        for key, value in target_stats.iteritems():
            if key == "log_loss":
                assert value is not None
            else:
                assert value is None

    is_target_stats_nan(metrics["total"]["target_stats"])
    for slice in metrics["slices"]:
        is_target_stats_nan(slice["target_stats"])


@pytest.fixture(scope='module')
def test_table_for_llmax(local_yt):
    data = [
        {"slice": 0, "p": 0.1, "t": 0, "w": 1, "q": 0},
        {"slice": 0, "p": 0.3, "t": 0, "w": 2, "q": 1},
        {"slice": 0, "p": 0.5, "t": 0, "w": 3, "q": 0},
        {"slice": 0, "p": 0.13, "t": 0, "w": 4, "q": 1},
        {"slice": 0, "p": 0.8, "t": 0, "w": 1, "q": 0},
        {"slice": 0, "p": 0.9, "t": 0, "w": 10, "q": 1},
        {"slice": 0, "p": 0.3, "t": 1, "w": 2, "q": 0},
        {"slice": 0, "p": 0.4, "t": 1, "w": 4, "q": 1},
        {"slice": 0, "p": 0.5, "t": 1, "w": 4, "q": 0},
        {"slice": 0, "p": 0.8, "t": 1, "w": 4, "q": 1},
    ]

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


@pytest.fixture(scope='module')
def true_llmax_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                 "ll_max_corrected_ll":-31.03675067,
                 "log_loss":1.080821657,
                 "llmax":-0.233751686,
                  "ll_p":-1.019524976,
                  "ll_max_group_ll":-22.85544166,
            }
        },
        "slices": [
        ]
    }
    yield true_metrics


def test_llmax(local_yt, test_table_for_llmax, true_llmax_metrics):
    metrics = metric_eval(
        src_table=test_table_for_llmax,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        weight="w",
        qid=["q"],
        slices=[["slice"]],
        metrics=["log_loss", "llmax"],
        transform_prediction=None,
        memory_limit=2,
    )

    check_result(true_llmax_metrics, metrics)


@pytest.fixture(scope='module')
def test_table_for_q_m_uplift(local_yt):
    data = [
        {"slice": 0, "p": 0.9, "t": 1, "w": 4, "q": 0},
        {"slice": 0, "p": 0.5, "t": 0, "w": 1, "q": 0},
        {"slice": 0, "p": 0.3, "t": 0, "w": 9, "q": 0},
        {"slice": 0, "p": 0.1, "t": 1, "w": 2, "q": 0},

        {"slice": 1, "p": 0.9, "t": 1, "w": 1, "q": 1},
        {"slice": 1, "p": 0.5, "t": 0, "w": 1, "q": 1},
        {"slice": 1, "p": 0.3, "t": 0, "w": 1, "q": 1},
        {"slice": 1, "p": 0.1, "t": 1, "w": 1, "q": 1},
    ]

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


@pytest.fixture(scope='module')
def true_weighted_q_m_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                "q_m_uplift@20": 3.19444439
            }
        },
        "slices": [
            {"slice_key": "slice", "slice_value": "1", "target_stats":
                {
                    "q_m_uplift@20": 2.5
                }
             },
            {"slice_key": "slice", "slice_value": "0", "target_stats":
                {
                    "q_m_uplift@20": 3.3333333,
                }
             }
        ]
    }
    yield true_metrics


def test_weighted_q_m_uplift(local_yt, test_table_for_q_m_uplift, true_weighted_q_m_metrics):
    metrics = metric_eval(
        src_table=test_table_for_q_m_uplift,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        weight="w",
        qid=["q"],
        slices=[["slice"]],
        metrics=["q_m_uplift@20"],
        transform_prediction=None,
        memory_limit=2,
    )

    check_result(true_weighted_q_m_metrics, metrics)


@pytest.fixture(scope='module')
def test_table_for_q_auc_sid(local_yt):
    data = [
        {"slice": 0, "p": 0.9, "t": 1, "w": 0.8, "q": 1, "sid": 0},
        {"slice": 0, "p": 0.9, "t": 0, "w": 0.2, "q": 1, "sid": 0},
        {"slice": 0, "p": 0.1, "t": 0, "w": 0.5, "q": 1, "sid": 1},
        {"slice": 0, "p": 0.1, "t": 1, "w": 0.5, "q": 1, "sid": 1},

        {"slice": 1, "p": 0.3, "t": 0, "w": 0.4, "q": 2, "sid": 2},
        {"slice": 1, "p": 0.3, "t": 1, "w": 0.6, "q": 2, "sid": 2},
    ]

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


@pytest.fixture(scope='module')
def true_weighted_sid_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                "auc": 0.7083333,
                "q_auc": 0.8,
            }
        },
        "slices": [
            {"slice_key": "slice", "slice_value": "0", "target_stats":
                {
                    "auc": 0.8,
                    "q_auc": 0.8
                }
             }
        ]
    }
    yield true_metrics


def test_weighted_sid_metrics(local_yt, test_table_for_q_auc_sid, true_weighted_sid_metrics):
    metrics = metric_eval(
        src_table=test_table_for_q_auc_sid,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        weight="w",
        qid=["q"],
        sid="sid",
        slices=[["slice"]],
        metrics=["auc", "q_auc"],
        transform_prediction=None,
        memory_limit=2,
    )

    for s in metrics["slices"]:
        if s["slice_value"] == "1":
            assert s["target_stats"]["auc"] is None and s["target_stats"]["auc"] is None
            del s

    check_result(true_weighted_sid_metrics, metrics)


@pytest.fixture(scope='module')
def test_different_sid_mode_table(local_yt):
    data = [
        {"slice": 0, "p": 0.9, "t": 1, "w": 0.8, "q": 1, "sid": 0},
        {"slice": 0, "p": 0.9, "t": 0, "w": 0.2, "q": 1, "sid": 0},
        {"slice": 0, "p": 0.1, "t": 0, "w": 0.5, "q": 1, "sid": 1},
        {"slice": 0, "p": 0.1, "t": 1, "w": 0.5, "q": 1, "sid": 1},

        {"slice": 1, "p": 0.3, "t": 0, "w": 0.4, "q": 2, "sid": 2},
        {"slice": 1, "p": 0.3, "t": 1, "w": 0.6, "q": 2, "sid": 2},
    ]

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


@pytest.fixture(scope='module')
def true_different_sid_mode_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                "q_auc": 0.8,
                "clicks": 1.900000036,
                "ctr": 0.6333333358,
                "shows": 3.000000045,
                "ctr_factor": 1.461538475
            }
        },
        "slices": [
            {"slice_key": "slice", "slice_value": "0", "target_stats":
                {
                    "q_auc": 0.8,
                    "clicks": 1.300000012,
                    "ctr": 0.6500000011,
                    "shows": 2.000000015,
                    "ctr_factor": 1.300000023
                }
             }
        ]
    }
    yield true_metrics


def test_different_sid_mode(local_yt, test_different_sid_mode_table, true_different_sid_mode_metrics):
    metrics = metric_eval(
        src_table=test_different_sid_mode_table,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        weight="w",
        qid="q",
        sid="sid",
        slices=[["slice"]],
        metrics=["q_auc", "ctr_factor"],
        transform_prediction=None,
        memory_limit=2,
    )

    for s in metrics["slices"]:
        if s["slice_value"] == "1":
            assert s["target_stats"]["auc"] is None and s["target_stats"]["auc"] is None
            del s

    check_result(true_different_sid_mode_metrics, metrics)


@pytest.fixture(scope='module')
def test_regression_metrics_table(local_yt):
    data = [
        {"p": 0.1, "t": 0, "w": 2, "q": 1},
        {"p": 0.2, "t": 1, "w": 3, "q": 1},
        {"p": 0.4, "t": 2, "w": 4, "q": 1},
        {"p": 7, "t": 4, "w": 3, "q": 1},
        {"p": 8, "t": 8, "w": 2, "q": 2},
        {"p": 9, "t": 9, "w": 1, "q": 2},
    ]

    yt = local_yt.get_client()
    table_path = "//home/{}".format(str(uuid4()))
    yt.create("table", table_path)
    yt.write_table(table_path, data)

    yield table_path


@pytest.fixture(scope='module')
def true_regression_metrics():
    true_metrics = {
        "total": {
            "target_stats": {
                "mse": 2.612000000,
                "rmse": 1.61616830,
                "mae": 1.2,
                "avg_target": 3.2,
                "avg_prediction": 3.226666,
                "total_events": 15.0,
                "r2": 0.67990196078,
            }
        },
        "slices": []
    }
    yield true_metrics


def test_different_sid_mode(local_yt, test_regression_metrics_table, true_regression_metrics):
    metrics = metric_eval(
        src_table=test_regression_metrics_table,
        yt_token="",
        yt_proxy=local_yt.get_client().config['proxy']['url'],
        target="t",
        prediction="p",
        weight="w",
        qid="q",
        metrics=["mse", "rmse", "mae", "avg_target", "avg_prediction", "total_events", "r2"],
        transform_prediction=None,
        memory_limit=2,
    )

    check_result(true_regression_metrics, metrics)
