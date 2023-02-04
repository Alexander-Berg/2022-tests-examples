import pytest


def check_target_stats(true_stats, calc_stats):
    assert set(true_stats.keys()) == set(calc_stats.keys())
    for metric_name in true_stats:
        assert true_stats[metric_name] == pytest.approx(calc_stats[metric_name]), metric_name


def check_result(true_metrics, metrics):
    # assert true_metrics["total"]["target_stats"] == pytest.approx(metrics["total"]["target_stats"])
    check_target_stats(true_metrics["total"]["target_stats"], metrics["total"]["target_stats"])

    true_metrics["slices"].sort(key=lambda x: x["slice_value"])
    metrics["slices"].sort(key=lambda x: x["slice_value"])

    assert len(true_metrics) == len(metrics)
    for true_slice, slice in zip(true_metrics["slices"], metrics["slices"]):
        # assert true_slice["target_stats"] == pytest.approx(slice["target_stats"])
        check_target_stats(true_slice["target_stats"], slice["target_stats"])
