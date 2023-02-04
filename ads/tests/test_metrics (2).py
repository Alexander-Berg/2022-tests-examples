
import copy

import pytest

import ads.emily.storage.libs.monitor.lib.metric as metric
import ads.emily.storage.libs.monitor.tests.lib as monitor_ut


def test_metric_load():
    expected = copy.deepcopy(monitor_ut.TEST_METRIC_DICT)

    m = metric.BaseMetric(
        sensor=monitor_ut.TEST_SENSOR,
        value=monitor_ut.TEST_VALUE,
        labels=monitor_ut.TEST_LABELS,
        time=monitor_ut.TEST_METRIC_TIME
    )

    assert m.to_dict() == expected

    m_loaded = metric.BaseMetric.load(expected)

    assert m == m_loaded


@pytest.mark.parametrize("InitMetic,kwargs,labels", [
    (metric.ModelVersionMetric, {"key": monitor_ut.TEST_MODEL_KEY, "version": monitor_ut.TEST_MODEL_VERSION}, {"key": monitor_ut.TEST_MODEL_KEY, "version": monitor_ut.TEST_MODEL_VERSION}),
    (metric.LatestModelMetric, {"key": monitor_ut.TEST_MODEL_KEY}, {"key": monitor_ut.TEST_MODEL_KEY, "version": monitor_ut.LATEST_MODEL_VERSION}),
    (metric.AggregateModelMetric, {"key": monitor_ut.TEST_MODEL_KEY, "aggregate": "sum"}, {"key": monitor_ut.TEST_MODEL_KEY, "aggregate": "sum"}),
])
def test_metric_init(InitMetic, kwargs, labels):
    metric = InitMetic(
        sensor=monitor_ut.TEST_SENSOR,
        value=monitor_ut.TEST_VALUE,
        **kwargs
    )
    assert metric.labels == labels
