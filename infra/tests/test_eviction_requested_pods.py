import os
import pytest
import mock
import yatest
from sepelib.core import config as sepelibconfig
from infra.watchdog.src.app.watchdog import Application
from infra.watchdog.src.lib.metrics import Metrics
from infra.nanny.yp_lite_api.proto import admin_api_pb2


def source_path(path):
    try:
        return yatest.common.source_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], path)


def load_config():
    config_path = source_path("infra/watchdog/cfg_default.yml")
    print 'config_path-this', config_path
    sepelibconfig.load(config_path)


@pytest.fixture(autouse=True)
def config():
    load_config()
    return sepelibconfig


def test_metrics_lib():
    m = Metrics()
    assert m.dump() == []
    m.eviction_requested_24_hours.set(value=1)
    m.eviction_requested_48_hours.set(value=2)
    m.eviction_requested_72_hours.set(value=3)
    assert sorted(m.dump()) == [
        {u'name': u'eviction_requested_24_hours_attx', u'val': 1},
        {u'name': u'eviction_requested_48_hours_attx', u'val': 2},
        {u'name': u'eviction_requested_72_hours_attx', u'val': 3}
    ]


def test_run_eviction_requested_metrics():
    app = Application('test')
    admin_api_client_mock = mock.Mock()
    app.admin_api_client = admin_api_client_mock
    app.zk_client = mock.Mock()
    app.push_client = mock.Mock()
    resp = admin_api_pb2.GetEvictionRequestedPodsStatsResponse()
    admin_api_client_mock.get_eviction_requested_pods_stats.return_value = resp
    resp.pods.add(id='pod-1')

    metrics = app.run_eviction_requested_pods_metrics(run_once=True)
    assert metrics.eviction_requested_24_hours._counter == 1
    assert metrics.eviction_requested_48_hours._counter == 1
    assert metrics.eviction_requested_72_hours._counter == 1
