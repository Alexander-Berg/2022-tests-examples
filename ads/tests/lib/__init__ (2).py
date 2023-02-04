import typing as tp
from datetime import datetime

from ads.emily.storage.libs.monitor.lib import metric, monitor, services
from ads.emily.storage.utils import parse_sandbox_time

TEST_SERVICE = services.Services.JUNK_SERVICE
TEST_SERVICE_NAME = TEST_SERVICE.value.name
TEST_SENSORS = ["test-sensor1", "test-sensor2", "test-sensor3"]
TEST_SENSOR = TEST_SENSORS[0]
TEST_VALUES = [12.21, 42.12, 16.65]
TEST_VALUE = TEST_VALUES[0]
TEST_LABELS = {
    "test-label-key-1": "test-label-val-1",
    "test-label-key-2": "test-label-val-2",
}
TEST_METRIC_TIME = parse_sandbox_time("2021-05-15T12:24:30Z")
TEST_CLIENT_TIME = parse_sandbox_time("2021-06-16T12:30:30Z")

TEST_METRIC_DICT = {
    "sensor": TEST_SENSOR,
    "value": TEST_VALUE,
    "labels": TEST_LABELS,
    "time": TEST_METRIC_TIME,
}

TEST_MODEL_KEYS = ["model_key1", "model_key2", "model_key3"]
TEST_MODEL_KEY = TEST_MODEL_KEYS[0]
TEST_MODEL_VERSION = "model_version"
LATEST_MODEL_VERSION = "latest"

TEST_METRICS = [
    metric.ModelVersionMetric(
        sensor=TEST_SENSORS[0],
        value=TEST_VALUES[0],
        key=TEST_MODEL_KEYS[0],
        version=TEST_MODEL_VERSION,
    ),
    metric.LatestModelMetric(
        sensor=TEST_SENSORS[1],
        value=TEST_VALUES[1],
        key=TEST_MODEL_KEYS[1],
    ),
    metric.AggregateModelMetric(
        sensor=TEST_SENSORS[2],
        value=TEST_VALUES[2],
        key=TEST_MODEL_KEYS[2],
        aggregate="sum",
        time=TEST_METRIC_TIME,
    )
]
TEST_METRIC = TEST_METRICS[0]
TEST_SERICE_METRICS = [
    metric.ServiceMetrics(
        service=TEST_SERVICE.value,
        metrics=TEST_METRICS
    )
]


class TestMonitor(monitor.BaseMonitor):
    def get_metrics(
            self,                # type: TestMonitor
            relative_time=None,  # type: datetime
    ):  # type: (...) -> tp.List[metric.ModelMetric]
        return TEST_METRICS


TEST_MONITORS = [TestMonitor()]


def solomon_metrics_from(
        metrics,            # type: tp.List[metric.ModelMetric]
        time                # type: datetime
):  # type: (...) -> tp.List[tp.Tuple[str, tp.List[str]]]
    return [
        ("Sensor", [m.sensor for m in metrics]),
        ("Value", [m.value for m in metrics]),
        ("Labels", [m.labels for m in metrics]),
        ("Time", [m.time or time for m in metrics]),
    ]


def assert_result_solomon(solomon, service=None, time=None, metrics=None):
    cache = solomon.cache(service or TEST_SERVICE.value)

    d = [
        ("Sensor", TEST_SENSORS),
        ("Value", TEST_VALUES),
        ("Labels", [
            dict(
                key=TEST_MODEL_KEYS[0],
                version=TEST_MODEL_VERSION,
            ),
            dict(
                key=TEST_MODEL_KEYS[1],
                version=LATEST_MODEL_VERSION,
            ),
            dict(
                key=TEST_MODEL_KEYS[2],
                aggregate="sum",
            )
        ]),
        ("Time", [TEST_CLIENT_TIME, TEST_CLIENT_TIME, TEST_METRIC_TIME]),
    ] if metrics is None else solomon_metrics_from(metrics, time=time)

    for j, (key, test_results) in enumerate(d):
        for i, result in enumerate(test_results):
            assert cache[i][j] == result, "Solomon | {} {}: expected: {}, got {}".format(key, i, cache[i][j], result)
