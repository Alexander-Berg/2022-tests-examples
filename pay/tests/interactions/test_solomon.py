import time

import pytest
from contextlib2 import suppress

from mock import patch

from yb_darkspirit.interactions.solomon import (BackgroundTaskMetricCollector, BackgroundProcessMetric, Metrics,
                                                clear_background_task_metrics
                                                )


def test_collect_bg_process_nested_process_error():
    clear_background_task_metrics()
    proc1, proc2 = 'proc1', 'proc2'
    with pytest.raises(RuntimeError):
        with BackgroundTaskMetricCollector.collect_process(proc1):
            with BackgroundTaskMetricCollector.collect_process(proc2):
                pass


def test_collect_bg_subprocess_outside_collect_process_error():
    clear_background_task_metrics()
    subproc = 'subproc'
    with pytest.raises(RuntimeError):
        with BackgroundTaskMetricCollector.collect_subprocess(subproc):
            pass


def test_collect_bg_process_error_outside_subprocess():
    clear_background_task_metrics()
    proc = 'proc'
    timestamp = int(time.time())
    err = ValueError('val err')
    with suppress(err.__class__):
        with BackgroundTaskMetricCollector.collect_process(proc):
            raise err
    expected_data = [
        {'kind': 'IGAUGE',
         'labels': {'process': proc, 'sensor': BackgroundProcessMetric.METRIC_NAME,
                    'subprocess': BackgroundTaskMetricCollector.NO_SUBPROCESS,
                    'error': err.__class__.__name__}, 'ts': timestamp, 'value': 1},
    ]
    with patch('yb_darkspirit.interactions.solomon.time.time', return_value=timestamp):
        data = list(Metrics.data())
    assert len(expected_data) == len(data)
    for item in expected_data:
        assert item in data


def test_collect_bg_process():
    clear_background_task_metrics()
    proc, subproc1, subproc2 = 'proc', 'subproc1', 'subproc2'
    timestamp = int(time.time())
    err = ValueError('val err')
    with BackgroundTaskMetricCollector.collect_process(proc):
        with BackgroundTaskMetricCollector.collect_subprocess(subproc1):
            pass
        with BackgroundTaskMetricCollector.collect_subprocess(subproc2):
            pass
        with suppress(err.__class__):
            with BackgroundTaskMetricCollector.collect_subprocess(subproc2):
                raise err
        with BackgroundTaskMetricCollector.collect_subprocess(subproc2):
            pass
    expected_data = [
            {'kind': 'IGAUGE', 'labels': {'process': proc, 'sensor': BackgroundProcessMetric.METRIC_NAME, 'subprocess': subproc1}, 'ts': timestamp, 'value': 1},
            {'kind': 'IGAUGE', 'labels': {'process': proc, 'sensor': BackgroundProcessMetric.METRIC_NAME, 'subprocess': subproc2}, 'ts': timestamp, 'value': 2},
            {'kind': 'IGAUGE', 'labels': {'process': proc, 'sensor': BackgroundProcessMetric.METRIC_NAME, 'subprocess': subproc2, 'error': err.__class__.__name__}, 'ts': timestamp, 'value': 1},
    ]
    with patch('yb_darkspirit.interactions.solomon.time.time', return_value=timestamp):
        data = list(Metrics.data())
    assert len(expected_data) == len(data)
    for item in expected_data:
        assert item in data


def test_background_process_collector_send_solomon():
    clear_background_task_metrics()
    proc, subproc = 'proc', 'subproc'
    solomon_url = 'http://url'
    with patch('yb_darkspirit.interactions.solomon.send_metric') as send_metric:
        with BackgroundTaskMetricCollector.send_to_solomon(solomon_url, proc):
            with BackgroundTaskMetricCollector.collect_subprocess(subproc):
                pass
        assert send_metric.called
    with patch('yb_darkspirit.interactions.solomon.send_metric') as send_metric:
        err = ValueError('Oh no')
        with suppress(err.__class__):
            with BackgroundTaskMetricCollector.send_to_solomon(solomon_url, proc):
                with BackgroundTaskMetricCollector.collect_subprocess(subproc):
                    raise err
        assert send_metric.called
