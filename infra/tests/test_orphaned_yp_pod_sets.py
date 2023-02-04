import os
import logging
import yaml
import pytest
import mock
import yatest
from sepelib.core import config as sepelibconfig
import datetime
from infra.watchdog.src.app.watchdog import OrphanedYpPodSetsCollectorSettings, Application
from infra.watchdog.src.lib.yp_pod_set import PodSet
from infra.watchdog.src.lib.metrics import Metrics


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
    m.orphaned_pod_sets.set(value=0)
    m.deploy_engine_url_missing_pod_sets.set(value=0)
    m.not_processed_domains_pod_sets.set(value=0)
    m.last_mtime_skip_pod_sets.set(value=0)
    assert sorted(m.dump()) == [
        {u'name': u'deploy_engine_url_missing_pod_sets_attx', u'val': 0},
        {u'name': u'last_mtime_skip_pod_sets_attx', u'val': 0},
        {u'name': u'not_processed_domains_pod_sets_attx', u'val': 0},
        {u'name': u'orphaned_pod_sets_attx', u'val': 0}
    ]


def test_settings():
    raw_data = """
    mark_label_name: 'orphaned'
    sleep_before_first_run: true
    iteration_period:
        hours: 6
    max_marked_pod_sets_per_iteration: 100
    match_deploy_engine_url_domains:
        - name: 'nanny.yandex-team.ru'
          process: true
          last_modification_timedelta:
             days: 1
        - name: 'adm-nanny.yandex-team.ru'
          process: false
        - name: 'dev-nanny.yandex-team.ru'
          process: false
    unmatched_deploy_engine_url_domains:
        process: true
        last_modification_timedelta:
            days: 7
    """
    data = yaml.safe_load(raw_data)
    settings = OrphanedYpPodSetsCollectorSettings(data)
    settings.mark_label_name = 'orphaned'
    assert settings.iteration_period == 6 * 60 * 60
    assert settings.sleep_before_first_run
    assert settings.max_marked_pod_sets_per_iteration == 100

    prod_nanny_settings = settings.get_domain_settings('nanny.yandex-team.ru')
    assert prod_nanny_settings.process
    assert prod_nanny_settings.last_modification_timedelta == datetime.timedelta(days=1)

    adm_nanny_settings = settings.get_domain_settings('adm-nanny.yandex-team.ru')
    assert not adm_nanny_settings.process
    assert adm_nanny_settings.last_modification_timedelta == datetime.timedelta()

    dev_nanny_settings = settings.get_domain_settings('dev-nanny.yandex-team.ru')
    assert not dev_nanny_settings.process
    assert dev_nanny_settings.last_modification_timedelta == datetime.timedelta()

    other_nanny_settings = settings.get_domain_settings('unmatched_domain_name')
    assert other_nanny_settings.process
    assert other_nanny_settings.last_modification_timedelta == datetime.timedelta(days=7)


def test_run_orphaned_yp_pod_sets_metrics():
    app = Application('test')
    yp_client_mock = mock.Mock()  # type: YpClient
    app.yp_clients_by_clusters = {'test_sas': yp_client_mock}
    app.zk_client = mock.Mock()
    app.push_client = mock.Mock()
    app.orphaned_collector_settings.sleep_before_first_run = False
    app.orphaned_collector_settings.iteration_period = 0

    pod_set_mock = mock.Mock()  # type: PodSet
    yp_client_mock.list_objects.return_value = [pod_set_mock]
    yp_client_mock.get_object.return_value = pod_set_mock

    log = logging.getLogger('testing')

    # case orphaned yp pod set
    metrics = Metrics()

    app.zk_client.get_service_ids.return_value = ['nanny_service_id']
    pod_set_mock.obj_id = 'yp_pod_set_object_id'
    pod_set_mock.deploy_engine_url = 'http://nanny.yandex-team.ru/'
    pod_set_mock.nanny_service_id = 'removed_nanny_service'
    pod_set_mock.nanny_watchdog_marks = {}
    pod_set_mock.exists_nanny_watchdog_marks = False
    pod_set_mock.get_attr_last_modification_dt.return_value = datetime.datetime.utcnow() - datetime.timedelta(days=2)
    app._run_orphaned_yp_pod_sets_metrics(metrics, log)
    assert metrics.orphaned_pod_sets._counter == 1
    assert metrics.deploy_engine_url_missing_pod_sets._counter == 0
    assert metrics.not_processed_domains_pod_sets._counter == 0
    assert metrics.last_mtime_skip_pod_sets._counter == 0

    yp_client_mock.set_object_label.assert_called_with(
        PodSet, 'yp_pod_set_object_id', PodSet.NANNY_WATCHDOG_LABELS_PATH,
        {app.orphaned_collector_settings.mark_label_name: app.instance_id}
    )

    # case orphaned yp pod set with not called set_object_label
    metrics = Metrics()
    yp_client_mock.set_object_label.reset_mock()
    app.zk_client.get_service_ids.return_value = ['nanny_service_id']
    pod_set_mock.obj_id = 'yp_pod_set_object_id'
    pod_set_mock.deploy_engine_url = 'http://nanny.yandex-team.ru/'
    pod_set_mock.nanny_service_id = 'removed_nanny_service'
    pod_set_mock.nanny_watchdog_marks = {}
    pod_set_mock.get_attr_last_modification_dt.return_value = datetime.datetime.utcnow() - datetime.timedelta(days=2)
    app.orphaned_collector_settings.max_marked_pod_sets_per_iteration = 0
    app._run_orphaned_yp_pod_sets_metrics(metrics, log)
    assert metrics.orphaned_pod_sets._counter == 1
    assert metrics.deploy_engine_url_missing_pod_sets._counter == 0
    assert metrics.not_processed_domains_pod_sets._counter == 0
    assert metrics.last_mtime_skip_pod_sets._counter == 0

    yp_client_mock.set_object_label.assert_not_called()

    app.orphaned_collector_settings.max_marked_pod_sets_per_iteration = 100

    # case skip by modification time < 1 days
    metrics = Metrics()
    pod_set_mock.get_attr_last_modification_dt.return_value = datetime.datetime.utcnow() - datetime.timedelta(hours=1)
    app._run_orphaned_yp_pod_sets_metrics(metrics, log)
    assert metrics.orphaned_pod_sets._counter == 0
    assert metrics.deploy_engine_url_missing_pod_sets._counter == 0
    assert metrics.not_processed_domains_pod_sets._counter == 0
    assert metrics.last_mtime_skip_pod_sets._counter == 1

    # case deploy_engine_url is empty
    metrics = Metrics()
    pod_set_mock.deploy_engine_url = ''
    app._run_orphaned_yp_pod_sets_metrics(metrics, log)
    assert metrics.orphaned_pod_sets._counter == 0
    assert metrics.deploy_engine_url_missing_pod_sets._counter == 1
    assert metrics.not_processed_domains_pod_sets._counter == 0
    assert metrics.last_mtime_skip_pod_sets._counter == 0

    # case skip not processed domains
    metrics = Metrics()
    pod_set_mock.deploy_engine_url = 'http://adm-nanny.yandex-team.ru/'
    app._run_orphaned_yp_pod_sets_metrics(metrics, log)
    assert metrics.orphaned_pod_sets._counter == 0
    assert metrics.deploy_engine_url_missing_pod_sets._counter == 0
    assert metrics.not_processed_domains_pod_sets._counter == 1
    assert metrics.last_mtime_skip_pod_sets._counter == 0

    # case skip unmatched domains by modification time < 7 days
    metrics = Metrics()
    pod_set_mock.deploy_engine_url = 'http://nanny.local/'
    pod_set_mock.get_attr_last_modification_dt.return_value = datetime.datetime.utcnow() - datetime.timedelta(days=2)
    app._run_orphaned_yp_pod_sets_metrics(metrics, log)
    assert metrics.orphaned_pod_sets._counter == 0
    assert metrics.deploy_engine_url_missing_pod_sets._counter == 0
    assert metrics.not_processed_domains_pod_sets._counter == 0
    assert metrics.last_mtime_skip_pod_sets._counter == 1

    # case unmatched domains w modification time > 7 days
    metrics = Metrics()
    pod_set_mock.deploy_engine_url = 'http://nanny.local/'
    pod_set_mock.get_attr_last_modification_dt.return_value = datetime.datetime.utcnow() - datetime.timedelta(days=10)
    app._run_orphaned_yp_pod_sets_metrics(metrics, log)
    assert metrics.orphaned_pod_sets._counter == 1
    assert metrics.deploy_engine_url_missing_pod_sets._counter == 0
    assert metrics.not_processed_domains_pod_sets._counter == 0
    assert metrics.last_mtime_skip_pod_sets._counter == 0

    # case count already marked pod_set
    metrics = Metrics()
    yp_client_mock.set_object_label.reset_mock()
    pod_set_mock.deploy_engine_url = 'http://nanny.local/'
    pod_set_mock.get_attr_last_modification_dt.return_value = datetime.datetime.utcnow() - datetime.timedelta(days=2)
    pod_set_mock.nanny_watchdog_marks = {app.orphaned_collector_settings.mark_label_name: ''}
    app._run_orphaned_yp_pod_sets_metrics(metrics, log)
    assert metrics.orphaned_pod_sets._counter == 1
    assert metrics.deploy_engine_url_missing_pod_sets._counter == 0
    assert metrics.not_processed_domains_pod_sets._counter == 0
    assert metrics.last_mtime_skip_pod_sets._counter == 0
    yp_client_mock.set_object_label.assert_not_called()
