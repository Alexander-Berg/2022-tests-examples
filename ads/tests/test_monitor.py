import pytest

import ads.emily.storage.libs.monitor.lib.errors as errors
import ads.emily.storage.libs.monitor.tests.lib as monitor_ut
from ads.emily.storage.libs.monitor import EmilyMonitor
from ads.emily.storage.libs.monitor.lib.solomon import LocalEmilySolomon


@pytest.fixture(scope="module")
def solomon_client_with_data():
    solomon = LocalEmilySolomon()
    data = {
        (
            "test/api/v2/projects/default_project/sensors/data",
            '{{cluster="{cluster}", service="{service}", {labels}, sensor="{sensor}"}}'.format(
                cluster=solomon._solomon._cluster,
                service=monitor_ut.TEST_SERVICE_NAME,
                labels=", ".join(['{}="{}"'.format(k, v) for k, v in monitor_ut.TEST_METRIC.labels.items()]),
                sensor=monitor_ut.TEST_SENSOR,
            ),
            monitor_ut.TEST_CLIENT_TIME.isoformat(),
            monitor_ut.TEST_CLIENT_TIME.isoformat(),
        ): [monitor_ut.TEST_VALUE]
    }
    solomon._solomon.add_to_cache(data)
    yield solomon


def test_emily_monitor_push():
    solomon = LocalEmilySolomon()

    client = EmilyMonitor(
        solomon=solomon,
        service=monitor_ut.TEST_SERVICE,
        time=monitor_ut.TEST_CLIENT_TIME,
    )

    assert client._service == monitor_ut.TEST_SERVICE.value
    assert client._client.raw_client.service == monitor_ut.TEST_SERVICE_NAME

    client.push(monitor_ut.TEST_METRICS)

    monitor_ut.assert_result_solomon(solomon)


def test_emily_monitors_solomon_push():
    solomon = LocalEmilySolomon()

    monitor = EmilyMonitor(
        solomon=solomon,
        service=monitor_ut.TEST_SERVICE,
        time=monitor_ut.TEST_CLIENT_TIME,
    )

    for m in monitor_ut.TEST_MONITORS:
        monitor.push(m.get_metrics())

    monitor_ut.assert_result_solomon(solomon)


def test_emily_monitor_pull(solomon_client_with_data):
    client = EmilyMonitor(
        solomon=solomon_client_with_data,
        service=monitor_ut.TEST_SERVICE,
        time=monitor_ut.TEST_CLIENT_TIME,
    )

    vectors = client.pull(
        sensor=monitor_ut.TEST_SENSOR,
        ts_from=monitor_ut.TEST_CLIENT_TIME,
        ts_to=monitor_ut.TEST_CLIENT_TIME,
        **monitor_ut.TEST_METRIC.labels
    )
    assert vectors, "Not found any vectors, expected: [{}]".format(monitor_ut.TEST_VALUE)
    assert vectors[0].values == [monitor_ut.TEST_VALUE], "Pull | Got {}, expected: [{}]".format(vectors[0].values, monitor_ut.TEST_VALUE)


@pytest.mark.parametrize("service,error_type", [
    (monitor_ut.TEST_SERVICE, None),
    (monitor_ut.TEST_SERVICE.value, None),
    (monitor_ut.TEST_SERVICE.value.name, None),
    ("unknown", errors.EmilyMonitorError),
])
def test_emily_monitor_init(service, error_type):
    solomon = LocalEmilySolomon()

    try:
        EmilyMonitor(
            solomon=solomon,
            service=service,
            time=monitor_ut.TEST_CLIENT_TIME,
        )
    except Exception as e:
        if error_type is None:
            raise
        if not isinstance(e, error_type):
            raise ValueError("Expected error to raise {}, but raised {}".format(error_type, type(e)))
    else:
        if error_type:
            raise ValueError("Expected error to raise {}, but nothing happened".format(error_type))
