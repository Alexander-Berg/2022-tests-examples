# coding: utf-8
import logging

import flaky
import inject
import juggler_sdk
import mock
import monotonic
import pytest
import six
import yaml
from sepelib.core import config as app_config

from awacs.lib import yasm_client, juggler_client, staffclient
from awacs.model import alerting, objects
from awacs.model.dao import IDao
from awacs.model.namespace.ctl import NamespaceCtl
from awtest import wait_until, wait_until_passes, check_log
from infra.awacs.proto import model_pb2, modules_pb2
from infra.swatlib.auth import abc
from infra.swatlib.orly_client import OrlyBrakeApplied


NS_ID = 'namespace-id'
TAXI_NS_ID = 'taxi-namespace-id'
MAPS_NS_ID = 'maps-namespace-id'
TESTING_NS_ID = 'testing-namespace-id'
CERT_ID = 'cert-id'
DNS_RECORD_ID = 'dns-record-id'
BACKEND_ID = 'backend-id'

ALL_VERSIONS = [c.version for c in alerting._CONFIGS]


@pytest.fixture
def yasm_client_mock():
    m = mock.Mock()
    m.replace_alerts.return_value = yasm_client.YasmReplaceAlertsResult(
        updated=1, created=0, deleted=0
    )
    return m


@pytest.fixture
def juggler_client_mock():
    m = mock.Mock()
    m.JugglerApiError = juggler_client.JugglerClient.JugglerApiError
    m.BadRequestError = juggler_client.JugglerClient.BadRequestError
    m.SyncNotifyRulesError = juggler_client.JugglerClient.SyncNotifyRulesError
    m.create_or_update_namespace.return_value = (
        juggler_client.CreateOrUpdateNamespaceResult(created=True, updated=False)
    )
    m.sync_notify_rules.return_value = juggler_client.SyncNotifyRulesResult(
        add=1, remove=0
    )
    m.sync_checks.return_value = juggler_client.SyncChecksResult(
        changed=['item1'], removed=[]
    )
    m.cleanup_checks.return_value = juggler_client.SyncChecksResult(
        changed=[], removed=['item1']
    )
    return m


@pytest.fixture
def abc_client_mock():
    return mock.Mock()


@pytest.fixture
def staff_client_mock():
    return mock.Mock()


@pytest.fixture(autouse=True)
def deps(
    caplog,
    binder,
    yasm_client_mock,
    dao,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(abc.IAbcClient, abc_client_mock)
        b.bind(yasm_client.IYasmClient, yasm_client_mock)
        b.bind(juggler_client.IJugglerClient, juggler_client_mock)
        b.bind(staffclient.IStaffClient, staff_client_mock)
        binder(b)

    inject.clear_and_configure(configure)
    app_config.set_value('alerting.name_prefix', 'test_awacs')
    yield
    inject.clear()


@pytest.fixture
def alerting_version():
    return alerting.CURRENT_VERSION


@pytest.fixture
def namespace_pb(cache, zk_storage, alerting_version):
    _ns_pb = model_pb2.Namespace()
    _ns_pb.meta.id = NS_ID
    dao = IDao.instance()
    spec_pb = model_pb2.NamespaceSpec()
    if alerting_version is not None:
        spec_pb.alerting.version = str(alerting_version)
        spec_pb.alerting.juggler_raw_downtimers.staff_logins.append('staff_login')
        spec_pb.alerting.juggler_raw_downtimers.staff_group_ids.append(111)
    ns_pb = dao.create_namespace(meta_pb=_ns_pb.meta, login='test', spec_pb=spec_pb)
    assert wait_until(lambda: cache.get_namespace(NS_ID), timeout=1)
    return ns_pb


@pytest.fixture
def taxi_namespace_pb(cache, zk_storage, alerting_version):
    _ns_pb = model_pb2.Namespace()
    _ns_pb.meta.id = TAXI_NS_ID
    dao = IDao.instance()
    spec_pb = model_pb2.NamespaceSpec()
    if alerting_version is not None:
        spec_pb.alerting.version = str(alerting_version)
        spec_pb.alerting.juggler_raw_downtimers.staff_logins.append('staff_login')
        spec_pb.alerting.juggler_raw_downtimers.staff_group_ids.append(111)
        spec_pb.alerting.notify_rules_disabled = True
    ns_pb = dao.create_namespace(meta_pb=_ns_pb.meta, login='test', spec_pb=spec_pb)
    assert wait_until(lambda: cache.get_namespace(TAXI_NS_ID), timeout=1)
    return ns_pb


@pytest.fixture
def maps_namespace_pb(cache, zk_storage, alerting_version):
    _ns_pb = model_pb2.Namespace()
    _ns_pb.meta.id = MAPS_NS_ID
    dao = IDao.instance()
    spec_pb = model_pb2.NamespaceSpec()
    if alerting_version is not None:
        spec_pb.alerting.version = str(alerting_version)
        spec_pb.alerting.juggler_raw_downtimers.staff_logins.append('staff_login')
        spec_pb.alerting.juggler_raw_downtimers.staff_group_ids.append(111)
        spec_pb.alerting.balancer_checks_disabled = True
    ns_pb = dao.create_namespace(meta_pb=_ns_pb.meta, login='test', spec_pb=spec_pb)
    assert wait_until(lambda: cache.get_namespace(MAPS_NS_ID), timeout=1)
    return ns_pb


@pytest.fixture
def testing_namespace_pb(cache, zk_storage, alerting_version):
    _ns_pb = model_pb2.Namespace()
    _ns_pb.meta.id = TESTING_NS_ID
    dao = IDao.instance()
    spec_pb = model_pb2.NamespaceSpec()
    if alerting_version is not None:
        spec_pb.alerting.version = str(alerting_version)
        spec_pb.alerting.juggler_raw_downtimers.staff_logins.append('staff_login')
        spec_pb.alerting.juggler_raw_downtimers.staff_group_ids.append(111)
        spec_pb.env_type = model_pb2.NamespaceSpec.NS_ENV_TESTING
    ns_pb = dao.create_namespace(meta_pb=_ns_pb.meta, login='test', spec_pb=spec_pb)
    assert wait_until(lambda: cache.get_namespace(TESTING_NS_ID), timeout=1)
    return ns_pb


@pytest.fixture
def ctl(cache, zk_storage, namespace_pb):
    """
    rtype: NamespaceCtl
    """
    cfg = app_config.get_value('alerting')
    cfg['sync_delay_interval_from'] = 0
    cfg['sync_delay_interval_to'] = 1800
    ctl = NamespaceCtl(NS_ID, cfg)
    ctl.PROCESS_INTERVAL = 0.1
    ctl.SELF_DELETION_COOLDOWN_PERIOD = 0.1
    ctl.EVENTS_QUEUE_GET_TIMEOUT = 0.1
    ctl._alerting_processor.CHECK_INTERVAL_LOWER_BOUND = 0
    ctl._alerting_processor.CHECK_INTERVAL_UPPER_BOUND = 0
    ctl._alerting_processor.SLEEP_AFTER_CREATE_JUGGLER_NAMESPACE = 0.1
    ctl._pb = namespace_pb
    ctl._alerting_processor.set_pb(namespace_pb)
    ctl._alerting_processor._needs_alerting_sync = mock.Mock()
    return ctl


@pytest.fixture
def balancer_pb(namespace_pb):
    balancer = model_pb2.Balancer(
        meta=model_pb2.BalancerMeta(id='balancer-id', namespace_id=namespace_pb.meta.id)
    )
    balancer.spec.config_transport.nanny_static_file.instance_tags.itype = 'test'
    balancer.spec.config_transport.nanny_static_file.instance_tags.ctype = 'balancer'
    balancer.spec.config_transport.nanny_static_file.instance_tags.prj = (
        namespace_pb.meta.id
    )
    balancer.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    balancer.meta.location.yp_cluster = 'SAS'
    balancer.spec.config_transport.nanny_static_file.service_id = 'rtc_balancer_test'
    return balancer


@pytest.fixture
def gencfg_balancer_pb(namespace_pb):
    balancer = model_pb2.Balancer(
        meta=model_pb2.BalancerMeta(
            id='gencfg-balancer-id', namespace_id=namespace_pb.meta.id
        )
    )
    balancer.spec.config_transport.nanny_static_file.instance_tags.itype = 'test'
    balancer.spec.config_transport.nanny_static_file.instance_tags.ctype = 'balancer'
    balancer.spec.config_transport.nanny_static_file.instance_tags.prj = (
        namespace_pb.meta.id
    )
    balancer.meta.location.type = model_pb2.BalancerMeta.Location.GENCFG_DC
    balancer.meta.location.gencfg_dc = 'SAS'
    balancer.spec.config_transport.nanny_static_file.service_id = 'rtc_balancer_test'
    return balancer


@pytest.fixture
def health_check_upstream_uri_pb(namespace_pb, balancer_pb):
    upstream_id = 'upstream_id'
    upstream = model_pb2.Upstream(
        meta=model_pb2.UpstreamMeta(id=upstream_id, namespace_id=namespace_pb.meta.id)
    )
    upstream.spec.yandex_balancer.config.regexp_section.matcher.match_fsm.uri = '/ping'
    status = upstream.statuses.add()
    status.active[
        '{}:{}'.format(namespace_pb.meta.id, balancer_pb.meta.id)
    ].status = 'True'
    return upstream


@pytest.fixture
def health_check_upstream_pattern_pb(namespace_pb, balancer_pb):
    upstream_id = 'upstream_id'
    upstream = model_pb2.Upstream(
        meta=model_pb2.UpstreamMeta(id=upstream_id, namespace_id=namespace_pb.meta.id)
    )
    upstream.spec.yandex_balancer.config.regexp_path_section.pattern = '/ping'
    status = upstream.statuses.add()
    status.active[
        '{}:{}'.format(namespace_pb.meta.id, balancer_pb.meta.id)
    ].status = 'True'
    return upstream


@pytest.fixture
def health_check_upstream_prefix_pb(namespace_pb, balancer_pb):
    upstream_id = 'upstream_id'
    upstream = model_pb2.Upstream(
        meta=model_pb2.UpstreamMeta(id=upstream_id, namespace_id=namespace_pb.meta.id)
    )
    upstream.spec.yandex_balancer.config.prefix_path_router_section.route = '/ping'
    status = upstream.statuses.add()
    status.active[
        '{}:{}'.format(namespace_pb.meta.id, balancer_pb.meta.id)
    ].status = 'True'
    return upstream


@pytest.mark.parametrize('alerting_version', [None])
def test_process_w_disabled_alerting(
    caplog, cache, zk_storage, ctx, ctl, namespace_pb, alerting_version
):
    ctl._process(ctx)


def test_process_create_or_update_juggler_namespace(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}

    juggler_client_mock.create_or_update_namespace.return_value = (
        juggler_client.CreateOrUpdateNamespaceResult(created=True, updated=False)
    )
    ctl._process(ctx)

    juggler_client_mock.create_or_update_namespace.assert_called_with(
        name=mock.ANY,
        abc_service_slug='svc_rclb',
        inherit_downtimers=False,
        downtimers=[
            '@svc_maintcoord',
            '@svc_test_group',
            'robot-walle',
            'staff_login',
        ],
        owners=['robot-yasm-golovan', '@svc_rclb'],
    )

    # case: not sleep after update Juggler Namespace
    juggler_client_mock.create_or_update_namespace.return_value = (
        juggler_client.CreateOrUpdateNamespaceResult(created=False, updated=True)
    )
    ctl._process(ctx)


def test_process_w_balancer_wo_instance_tags(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    b_1 = model_pb2.Balancer()
    cache.list_all_balancers.return_value = [b_1]
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'does not have meta.location, skip' in log.records_text()
    b_1.meta.location.yp_cluster = 'sas'
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert (
            'does not have spec.config_transport.nanny_static_file.instance_tags, skip'
            in log.records_text()
        )

    yasm_client_mock.replace_alerts.assert_called_with(mock.ANY, [])


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_0_1])
def test_process_balancer_yasm_alerts_ver_0_0_1(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    ctl._process(ctx)
    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_guarantee_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.worker_cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'max(balancer_report-worker-cpu_usage_hgram)',
            'juggler_check': {
                'service': 'worker_cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#worker-cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [70, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
    ]
    assert actual_yasm_alerts == expected_yasm_alerts
    yasm_client_mock.replace_alerts.assert_called_with(mock.ANY, expected_yasm_alerts)

    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_0_2])
def test_process_balancer_yasm_alerts_ver_0_0_2(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    ctl._process(ctx)
    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.worker_cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'max(balancer_report-worker-cpu_usage_hgram)',
            'juggler_check': {
                'service': 'worker_cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#worker-cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [70, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_0_4])
def test_process_balancer_yasm_alerts_ver_0_0_4(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    ctl._process(ctx)
    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.worker_cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'max(balancer_report-worker-cpu_usage_hgram)',
            'juggler_check': {
                'service': 'worker_cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#worker-cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [70, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
            'value_modify': {'type': 'aver', 'window': 30},
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_0_5])
def test_process_balancer_yasm_alerts_ver_0_0_5(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    ctl._process(ctx)
    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.worker_cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'max(balancer_report-worker-cpu_usage_hgram)',
            'juggler_check': {
                'service': 'worker_cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#worker-cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [70, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
            'value_modify': {'type': 'aver', 'window': 30},
        },
        {
            'abc': 'rclb',
            'crit': [75, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, \u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#fd-usage',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'fd_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.fd_usage',
            'signal': 'or(perc(balancer_report-fd_size_ammv, balancer_report-no_file_limit_ammv), 0)',
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [50, 75],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 \u0441\u0440\u0430\u0431\u043e\u0442\u0430'
                                u'\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#frozen-threads',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'frozen_threads',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.frozen_threads',
            'signal': 'or(balancer_report-threads-froze_ammv, 0)',
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_0_3])
def test_process_balancer_juggler_checks_0_0_3(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    ctl._process(ctx)
    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                'test_awacs_namespace_id_namespace-id',
                'cplb',
                'test_awacs_notify_group_platform_group',
                'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 30 * 60,
            'crit': 60 * 60,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[expected_deploy_status_check],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_0_4])
def test_process_balancer_juggler_checks_0_0_4(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    ctl._process(ctx)
    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[expected_deploy_status_check],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_0_6])
def test_process_balancer_juggler_checks_0_0_6(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][1]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_0])
def test_process_balancer_juggler_checks_0_1_0(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][2]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_3])
def test_process_balancer_juggler_checks_0_1_3(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][2]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_0])
def test_process_balancer_juggler_checks_0_2_0_no_upstream(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        with pytest.raises(RuntimeError):
            ctl._process(ctx)


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_0])
def test_process_balancer_juggler_checks_0_2_0_l7_with_health_check(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    balancer_pb.spec.yandex_balancer.config.l7_macro.health_check_reply.SetInParent()
    balancer_pb.spec.yandex_balancer.config.l7_macro.http.SetInParent()
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    assert len(juggler_client_mock.sync_checks.call_args_list[0][1]['checks']) == 4
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_http_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        2
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][3]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_http_check = juggler_sdk.Check(
        service='http_check',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        active='http',
        active_kwargs={
            'ok_codes': [200],
            'path': '/awacs-balancer-health-check',
            'port': 80,
        },
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='http-check',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
            'ignore_nodata': True,
            'mode': 'percent',
            'warn_limit': '30',
            'crit_limit': '50',
        },
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='http_check',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    assert actual_http_check.__dict__ == expected_http_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_http_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_0])
def test_process_balancer_juggler_checks_0_2_0_l7_no_health_check(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    balancer_pb.spec.yandex_balancer.config.l7_macro.SetInParent()
    balancer_pb.spec.yandex_balancer.config.l7_macro.http.SetInParent()
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        with pytest.raises(RuntimeError):
            ctl._process(ctx)


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_0])
def test_process_balancer_juggler_checks_0_2_0_invalid_instance_macro(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    health_check_upstream_uri_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    section = balancer_pb.spec.yandex_balancer.config.instance_macro.sections.add()
    section.key = 'admin'
    port = section.value.ports.add()
    port.value = 84
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    cache.get_upstream = mock.Mock()
    cache.get_upstream.return_value = health_check_upstream_uri_pb
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        with pytest.raises(
            RuntimeError,
            match="instance_macro must have 'http_section' or 'https_section'",
        ):
            ctl._process(ctx)


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_0])
@pytest.mark.parametrize(
    'health_check_upstream',
    [
        'regexp_section',
        'regexp_path_section',
        'prefix_path_router_section',
    ],
)
def test_process_balancer_juggler_checks_0_2_0_with_upstream_default_port(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    health_check_upstream_uri_pb,
    health_check_upstream_pattern_pb,
    health_check_upstream_prefix_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
    health_check_upstream,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    section = balancer_pb.spec.yandex_balancer.config.instance_macro.sections.add()
    section.key = 'https_section'
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]

    upstreams = {
        'regexp_section': health_check_upstream_uri_pb,
        'regexp_path_section': health_check_upstream_pattern_pb,
        'prefix_path_router_section': health_check_upstream_prefix_pb,
    }
    upstream = upstreams[health_check_upstream]

    cache.get_upstream = mock.Mock()
    cache.get_upstream.return_value = upstream
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    assert len(juggler_client_mock.sync_checks.call_args_list[0][1]['checks']) == 4
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_http_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        2
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][3]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_http_check = juggler_sdk.Check(
        service='http_check',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        active='https',
        active_kwargs={
            'ok_codes': [200],
            'path': '/ping',
            'port': 443,
            'validate_hostname': False,
            'allow_self_signed': True,
        },
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='http-check',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
            'ignore_nodata': True,
            'mode': 'percent',
            'warn_limit': '30',
            'crit_limit': '50',
        },
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='http_check',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    assert actual_http_check.__dict__ == expected_http_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_http_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_0])
@pytest.mark.parametrize('port_kind', ['local_port', 'port'])
def test_process_balancer_juggler_checks_0_2_0_with_upstream_custom_port(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    health_check_upstream_uri_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
    port_kind,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    section = balancer_pb.spec.yandex_balancer.config.instance_macro.sections.add()
    section.key = 'https_section'
    if port_kind == 'port':
        port = section.value.ports.add()
        port.value = 84
    elif port_kind == 'local_port':
        port = section.value.ports.add()
        port.value = 84
        port = section.value.local_ports.add()
        port.value = 80

    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    cache.get_upstream = mock.Mock()
    cache.get_upstream.return_value = health_check_upstream_uri_pb
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    assert len(juggler_client_mock.sync_checks.call_args_list[0][1]['checks']) == 4
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_http_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        2
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][3]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_http_check = juggler_sdk.Check(
        service='http_check',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        active='https',
        active_kwargs={
            'ok_codes': [200],
            'path': '/ping',
            'port': 84,
            'validate_hostname': False,
            'allow_self_signed': True,
        },
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='http-check',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
            'ignore_nodata': True,
            'mode': 'percent',
            'warn_limit': '30',
            'crit_limit': '50',
        },
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='http_check',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    assert actual_http_check.__dict__ == expected_http_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_http_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_1])
@pytest.mark.parametrize('port_kind', ['local_port', 'port'])
def test_process_balancer_juggler_checks_0_2_1_with_upstream_custom_port(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    health_check_upstream_uri_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
    port_kind,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    section = balancer_pb.spec.yandex_balancer.config.instance_macro.sections.add()
    section.key = 'https_section'
    if port_kind == 'port':
        port = section.value.ports.add()
        port.value = 84
    elif port_kind == 'local_port':
        port = section.value.ports.add()
        port.value = 80
        port = section.value.local_ports.add()
        port.value = 84

    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    cache.get_upstream = mock.Mock()
    cache.get_upstream.return_value = health_check_upstream_uri_pb
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    assert len(juggler_client_mock.sync_checks.call_args_list[0][1]['checks']) == 4
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_http_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        2
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][3]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_http_check = juggler_sdk.Check(
        service='http_check',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        active='https',
        active_kwargs={
            'ok_codes': [200],
            'path': u'/ping',
            'port': 84,
            'validate_hostname': False,
            'allow_self_signed': True,
        },
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='http-check',
                padding_right=0,
            )
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=180,
            critical=900,
            boost=0,
        ),
        ttl=1800,
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
            'ignore_nodata': True,
            'mode': 'percent',
            'warn_limit': '30',
            'crit_limit': '50',
        },
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='http_check',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    assert actual_http_check.__dict__ == expected_http_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_http_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_2])
def test_process_balancer_juggler_checks_0_2_2(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    balancer_pb.spec.yandex_balancer.config.l7_macro.health_check_reply.SetInParent()
    balancer_pb.spec.yandex_balancer.config.l7_macro.http.SetInParent()
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    assert len(juggler_client_mock.sync_checks.call_args_list[0][1]['checks']) == 4
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_http_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        2
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][3]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        check_options={
            'env': {
                'CRIT_THRESHOLD': '2',
                'WARNING_THRESHOLD': '14',
            },
        },
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_http_check = juggler_sdk.Check(
        service='http_check',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        active='http',
        active_kwargs={
            'ok_codes': [200],
            'path': '/awacs-balancer-health-check',
            'port': 80,
        },
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='http-check',
                padding_right=0,
            )
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=180,
            critical=900,
            boost=0,
        ),
        ttl=1800,
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
            'ignore_nodata': True,
            'mode': 'percent',
            'warn_limit': '30',
            'crit_limit': '50',
        },
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='http_check',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    assert actual_http_check.__dict__ == expected_http_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_http_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_2])
def test_process_balancer_juggler_checks_0_2_2_only_local_ports(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    health_check_upstream_uri_pb,
    staff_client_mock,
    alerting_version,
):
    section = balancer_pb.spec.yandex_balancer.config.instance_macro.sections.add()
    section.key = 'https_section'
    port = section.value.ports.add()
    port.value = 80
    port = section.value.local_ports.add()
    port.f_value.type = modules_pb2.Call.GET_PORT_VAR
    port.f_value.get_port_var_params.var = "port1"

    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    cache.get_upstream = mock.Mock()
    cache.get_upstream.return_value = health_check_upstream_uri_pb
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    assert len(juggler_client_mock.sync_checks.call_args_list[0][1]['checks']) == 4
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_http_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        2
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][3]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        check_options={
            'env': {
                'CRIT_THRESHOLD': '2',
                'WARNING_THRESHOLD': '14',
            },
        },
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_http_check = juggler_sdk.Check(
        service='http_check',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        active='https',
        active_kwargs={
            'ok_codes': [200],
            'path': '/ping',
            'validate_hostname': False,
            'allow_self_signed': True,
        },
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='http-check',
                padding_right=0,
            )
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=180,
            critical=900,
            boost=0,
        ),
        ttl=1800,
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
            'ignore_nodata': True,
            'mode': 'percent',
            'warn_limit': '30',
            'crit_limit': '50',
        },
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='http_check',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    assert actual_http_check.__dict__ == expected_http_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_http_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_2_3])
def test_process_balancer_juggler_checks_0_2_3(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    health_check_upstream_uri_pb,
    staff_client_mock,
    alerting_version,
):
    section = balancer_pb.spec.yandex_balancer.config.instance_macro.sections.add()
    section.key = 'https_section'
    port = section.value.ports.add()
    port.value = 80
    port = section.value.local_ports.add()
    port.f_value.type = modules_pb2.Call.GET_PORT_VAR
    port.f_value.get_port_var_params.var = "port1"

    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    cache.get_upstream = mock.Mock()
    cache.get_upstream.return_value = health_check_upstream_uri_pb
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    assert len(juggler_client_mock.sync_checks.call_args_list[0][1]['checks']) == 4
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_http_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        2
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][3]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        check_options={
            'env': {
                'CRIT_THRESHOLD': '2',
                'WARNING_THRESHOLD': '14',
            },
        },
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_http_check = juggler_sdk.Check(
        service='http_check',
        host='test_awacs.namespace-id.sas',
        namespace=u'test_awacs.namespace-id',
        active='https',
        active_kwargs={
            'ok_codes': [200],
            'path': '/ping',
            'validate_hostname': False,
            'allow_self_signed': True,
            'warn_expire': 0,
            'crit_expire': 0,
        },
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='http-check',
                padding_right=0,
            )
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=180,
            critical=900,
            boost=0,
        ),
        ttl=1800,
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
            'ignore_nodata': True,
            'mode': 'percent',
            'warn_limit': '30',
            'crit_limit': '50',
        },
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='http_check',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    assert actual_http_check.__dict__ == expected_http_check.__dict__
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_http_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_3])
def test_process_prestable_balancer_juggler_checks_0_1_3(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    balancer_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    balancer_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_PRESTABLE
    balancer_pb.spec.config_transport.nanny_static_file.instance_tags.ctype = (
        'prestable'
    )

    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_deploy_status_check = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ][0]
    actual_meta_check = juggler_client_mock.sync_checks.call_args_list[0][1]['checks'][
        1
    ]
    actual_enddate_certificate_check = juggler_client_mock.sync_checks.call_args_list[
        0
    ][1]['checks'][2]
    expected_deploy_status_check = juggler_sdk.Check(
        service='deploy_status',
        host='test_awacs.namespace-id.sas_prestable',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_notify_group_balancer_group',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        active='nanny_deploy_status',
        active_kwargs={
            'service': 'rtc_balancer_test',
            'warn': 60 * 60,
            'crit': 60 * 60 * 24 * 365,
        },
        refresh_time=300,
        ttl=1500,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='deploy-status',
                padding_right=0,
            )
        },
    )
    expected_enddate_certificate_check = juggler_sdk.Check(
        service='check_enddate_certificate',
        host='test_awacs.namespace-id.sas_prestable',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=300,
        ttl=10000,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='check-enddate-certificate',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'normal',
            'crit_limit': 1,
            'warn_limit': 1,
            'unreach_mode': 'skip',
            'unreach_service': [
                {
                    'check': ':META',
                    'hold': 600,
                }
            ],
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=1200,
            critical=6000,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='check_enddate_certificate',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    expected_meta_check = juggler_sdk.Check(
        service='META',
        host='test_awacs.namespace-id.sas_prestable',
        namespace=u'test_awacs.namespace-id',
        tags=sorted(
            [
                'a_geo_sas',
                'namespace-id',
                u'test_awacs_namespace_id_namespace-id',
                'cplb',
                u'test_awacs_notify_group_platform_group',
                u'test_awacs_balancer_id_balancer-id',
            ]
        ),
        refresh_time=90,
        ttl=900,
        meta={
            'urls': [
                {
                    'url': 'https://nanny.yandex-team.ru/ui/#/services/catalog/rtc_balancer_test/',
                    'type': 'nanny',
                    'title': u'⚠️Сервис в nanny',
                },
            ]
            + alerting.make_juggler_check_meta_urls(
                balancer_ui_url='https://nanny.yandex-team.ru/ui/'
                '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                actions_wiki_page_anchor='META',
                padding_right=0,
            )
        },
        aggregator='more_than_limit_is_problem',
        aggregator_kwargs={
            'ignore_nodata': False,
            'mode': 'percent',
            'crit_limit': '50',
            'warn_limit': '20',
        },
        flaps_config=juggler_sdk.FlapOptions(
            stable=60,
            critical=600,
            boost=0,
        ),
        children=[
            juggler_sdk.Child(
                host='awacs-rtc_balancer_test@cluster=sas',
                service='META',
                group_type='YP_ENDPOINT',
            )
        ],
    )
    assert actual_deploy_status_check.__dict__ == expected_deploy_status_check.__dict__
    assert actual_meta_check.__dict__ == expected_meta_check.__dict__
    assert (
        actual_enddate_certificate_check.__dict__
        == expected_enddate_certificate_check.__dict__
    )
    juggler_client_mock.sync_checks.assert_called_with(
        namespace='test_awacs.namespace-id',
        checks=[
            expected_deploy_status_check,
            expected_meta_check,
            expected_enddate_certificate_check,
        ],
        mark='test_awacs_namespace-id_checks',
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_0_7])
def test_process_balancer_yasm_alerts_ver_0_0_7(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]

    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.worker_cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'max(balancer_report-worker-cpu_usage_hgram)',
            'juggler_check': {
                'service': 'worker_cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#worker-cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [70, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
            'value_modify': {'type': 'aver', 'window': 30},
        },
        {
            'abc': 'rclb',
            'crit': [75, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#fd-usage',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'fd_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.fd_usage',
            'signal': 'or(perc(balancer_report-fd_size_ammv, balancer_report-no_file_limit_ammv), 0)',
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [50, 75],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#frozen-threads',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'frozen_threads',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.frozen_threads',
            'signal': 'or(balancer_report-threads-froze_ammv, 0)',
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#coredumps-total',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'coredumps_total',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.coredumps_total',
            'signal': 'or(hsum(portoinst-cores_total_hgram), 0)',
            'value_modify': {
                'type': 'summ',
                'window': 30,
            },
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_1])
def test_process_balancer_yasm_alerts_ver_0_1_1(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]

    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'crit': [75, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#fd-usage',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'fd_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.fd_usage',
            'signal': 'or(perc(balancer_report-fd_size_ammv, balancer_report-no_file_limit_ammv), 0)',
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [50, 75],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#frozen-threads',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'frozen_threads',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.frozen_threads',
            'signal': 'or(balancer_report-threads-froze_ammv, 0)',
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#coredumps-total',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'coredumps_total',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.coredumps_total',
            'signal': 'or(hsum(portoinst-cores_total_hgram), 0)',
            'value_modify': {
                'type': 'summ',
                'window': 30,
            },
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_2])
def test_process_balancer_yasm_alerts_ver_0_1_2(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]

    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'crit': [75, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#fd-usage',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'fd_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.fd_usage',
            'signal': 'or(perc(balancer_report-fd_size_ammv, balancer_report-no_file_limit_ammv), 0)',
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [50, 75],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#frozen-threads',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'frozen_threads',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.frozen_threads',
            'signal': 'or(balancer_report-threads-froze_ammv, 0)',
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#coredumps-total',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'coredumps_total',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.coredumps_total',
            'signal': 'or(hsum(portoinst-cores_total_hgram), 0)',
            'value_modify': {
                'type': 'summ',
                'window': 30,
            },
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_4])
def test_process_balancer_yasm_alerts_ver_0_1_4(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]

    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#coredumps-total',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'coredumps_total',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.coredumps_total',
            'signal': 'or(hsum(portoinst-cores_total_hgram), 0)',
            'value_modify': {
                'type': 'summ',
                'window': 30,
            },
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_4])
def test_process_prestable_balancer_yasm_alerts_ver_0_1_4(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    balancer_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_PRESTABLE
    balancer_pb.spec.config_transport.nanny_static_file.instance_tags.ctype = (
        'prestable'
    )
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]

    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas_prestable.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'prestable'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas_prestable',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas_prestable.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'prestable'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas_prestable',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas_prestable.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'prestable'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas_prestable',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas_prestable.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'prestable'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas_prestable',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas_prestable',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#coredumps-total',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'coredumps_total',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas_prestable.coredumps_total',
            'signal': 'or(hsum(portoinst-cores_total_hgram), 0)',
            'value_modify': {
                'type': 'summ',
                'window': 30,
            },
            'tags': {
                'ctype': [u'prestable'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_4])
def test_process_mixed_schedulers_balancer_yasm_alerts_ver_0_1_4(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    balancer_pb,
    gencfg_balancer_pb,
    juggler_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [
        balancer_pb,
        gencfg_balancer_pb,
    ]

    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    expected_yasm_alerts = [
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas_yp.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas_yp',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas_yp.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas_yp',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas_yp.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_balancer_id_balancer-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas_yp',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas_yp.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas_yp',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas_yp',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#coredumps-total',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'coredumps_total',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas_yp.coredumps_total',
            'signal': 'or(hsum(portoinst-cores_total_hgram), 0)',
            'value_modify': {
                'type': 'summ',
                'window': 30,
            },
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_limit_usage_perc_hgram, 80)',
            'juggler_check': {
                'service': 'cpu_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        u'test_awacs_balancer_id_gencfg-balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/gencfg-balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 120 * 5, 'stable': 120},
            },
            'warn': [60, 80],
            'crit': [80, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.cpu_wait_cores',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-cpu_wait_slot_hgram, 90)',
            'juggler_check': {
                'service': 'cpu_wait_cores',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        u'test_awacs_balancer_id_gencfg-balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/gencfg-balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#cpu-wait-cores',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [0.3, 0.4],
            'crit': [0.4, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.mem_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'quant(portoinst-anon_limit_usage_perc_hgram, 90)',
            'juggler_check': {
                'service': 'mem_usage',
                'tags': sorted(
                    [
                        u'test_awacs_notify_group_balancer_group',
                        u'test_awacs_balancer_id_gencfg-balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/gencfg-balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#mem-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'name': u'test_awacs.namespace-id.sas.logs_vol_usage',
            'tags': {
                'geo': ['sas'],
                'itype': [u'test'],
                'ctype': [u'balancer'],
                'prj': [u'namespace-id'],
            },
            'signal': 'portoinst-volume_/logs_usage_perc_txxx',
            'juggler_check': {
                'service': 'logs_vol_usage',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_gencfg-balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
                'namespace': u'test_awacs.namespace-id',
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'url': 'https://nanny.yandex-team.ru/ui/'
                            '#/awacs/namespaces/list/namespace-id/balancers/list/gencfg-balancer-id/show/',
                            'type': 'nanny',
                            'title': u'⚠️Балансер в awacs',
                        },
                        {
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#logs-vol-usage',
                            'type': 'wiki',
                            'title': u'⚠️Что делать, если алерт сработал'
                            + u'\u00A0' * 30,
                        },
                    ]
                },
                'flaps': {'critical': 150, 'stable': 30},
            },
            'warn': [80, 90],
            'crit': [90, None],
            'mgroups': ['ASEARCH'],
        },
        {
            'abc': 'rclb',
            'crit': [1, None],
            'juggler_check': {
                'flaps': {'critical': 150, 'stable': 30},
                'host': 'test_awacs.namespace-id.sas',
                'meta': {
                    'urls': [
                        {
                            'title': u'\u26a0\ufe0f\u0411\u0430\u043b\u0430\u043d\u0441\u0435\u0440 \u0432 awacs',
                            'type': 'nanny',
                            'url': 'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/namespace-id/balancers/list/gencfg-balancer-id/show/',
                        },
                        {
                            'title': (
                                u'\u26a0\ufe0f\u0427\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c, '
                                u'\u0435\u0441\u043b\u0438 \u0430\u043b\u0435\u0440\u0442 '
                                u'\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                                u'\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0\xa0'
                            ),
                            'type': 'wiki',
                            'url': 'https://wiki.yandex-team.ru/cplb/awacs/monitoring/alerting/actions/#coredumps-total',
                        },
                    ]
                },
                'namespace': u'test_awacs.namespace-id',
                'service': 'coredumps_total',
                'tags': sorted(
                    [
                        u'test_awacs_balancer_id_gencfg-balancer-id',
                        'cplb',
                        u'test_awacs_namespace_id_namespace-id',
                        'namespace-id',
                        u'test_awacs_notify_group_platform_group',
                    ]
                ),
            },
            'mgroups': ['ASEARCH'],
            'name': u'test_awacs.namespace-id.sas.coredumps_total',
            'signal': 'or(hsum(portoinst-cores_total_hgram), 0)',
            'value_modify': {
                'type': 'summ',
                'window': 30,
            },
            'tags': {
                'ctype': [u'balancer'],
                'geo': ['sas'],
                'itype': [u'test'],
                'prj': [u'namespace-id'],
            },
            'warn': [None, None],
        },
    ]
    assert expected_yasm_alerts == actual_yasm_alerts
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id', []
    )


@pytest.mark.parametrize('alerting_version', ALL_VERSIONS)
def test_process_namespace_notify_rules(
    caplog,
    cache,
    zk_storage,
    ctx,
    ctl,
    namespace_pb,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.must_get_namespace = mock.Mock()
    cache.must_get_namespace.return_value = namespace_pb
    juggler_raw_notify_rule1 = (
        namespace_pb.spec.alerting.juggler_raw_notify_rules.balancer.add()
    )  # type: model_pb2.NamespaceSpec.AlertingSettings.JugglerRawNotifyRule
    juggler_raw_notify_rule1.template_name = 'on_status_change'
    juggler_raw_notify_rule1.template_kwargs = """
        status:
            - from: OK
              to: CRIT
        login:
            - i-dyachkov
        method:
            - sms
    """
    juggler_raw_notify_rule2 = (
        namespace_pb.spec.alerting.juggler_raw_notify_rules.platform.add()
    )  # type: model_pb2.NamespaceSpec.AlertingSettings.JugglerRawNotifyRule
    juggler_raw_notify_rule2.template_name = 'on_status_change'
    juggler_raw_notify_rule2.template_kwargs = """
            status:
                - from: OK
                  to: CRIT
            login:
                - i-dyachkov
            method:
                - sms
        """
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = []
    ctl._process(ctx)
    cache.list_all_balancers.assert_called_with(namespace_pb.meta.id)
    yasm_client_mock.replace_alerts.assert_called_with(mock.ANY, [])
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.namespace-id',
        sorted(
            [
                juggler_client.NotifyRule(
                    selector='tag=test_awacs_notify_group_platform_group & tag=test_awacs_namespace_id_{}'.format(NS_ID),
                    template_name=juggler_raw_notify_rule2.template_name,
                    template_kwargs=yaml.safe_load(
                        juggler_raw_notify_rule2.template_kwargs
                    ),
                    description='Generated by awacs. Details: https://nanny.yandex-team.ru/ui/'
                                '#/awacs/namespaces/list/{}/alerting/'.format(NS_ID),
                ),
                juggler_client.NotifyRule(
                    selector='tag=test_awacs_notify_group_balancer_group & tag=test_awacs_namespace_id_{}'.format(NS_ID),
                    template_name=juggler_raw_notify_rule1.template_name,
                    template_kwargs=yaml.safe_load(
                        juggler_raw_notify_rule1.template_kwargs
                    ),
                    description='Generated by awacs. Details: https://nanny.yandex-team.ru/ui/'
                                '#/awacs/namespaces/list/{}/alerting/'.format(NS_ID),
                ),
            ],
            key=lambda x: (x.selector, x.template_name, x.template_kwargs),
        ),
    )


@pytest.mark.parametrize('alerting_version', ALL_VERSIONS)
def test_process_taxi_namespace_notify_rules(
    caplog,
    cache,
    zk_storage,
    ctx,
    taxi_namespace_pb,
    ctl,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.must_get_namespace = mock.Mock()
    cache.must_get_namespace.return_value = taxi_namespace_pb
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = []

    ctl._alerting_processor._namespace_id = TAXI_NS_ID
    ctl._alerting_processor.set_pb(taxi_namespace_pb)
    ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(taxi_namespace_pb.meta.id)
    yasm_client_mock.replace_alerts.assert_called_with(mock.ANY, [])
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.{}'.format(TAXI_NS_ID), []
    )


@pytest.mark.parametrize('alerting_version', [alerting.VERSION_0_1_0])
@pytest.mark.parametrize(
    'pb_fixture,pb_id',
    [
        ('maps_namespace_pb', MAPS_NS_ID),
        ('testing_namespace_pb', TESTING_NS_ID),
    ],
)
def test_process_only_namespace_platform_checks(
    request,
    caplog,
    cache,
    zk_storage,
    ctx,
    pb_fixture,
    pb_id,
    balancer_pb,
    ctl,
    juggler_client_mock,
    yasm_client_mock,
    abc_client_mock,
    staff_client_mock,
    alerting_version,
):
    pb = request.getfixturevalue(pb_fixture)
    abc_client_mock.get_service_slug.return_value = 'rclb'
    staff_client_mock.get_groups_by_ids.return_value = {111: {'url': 'svc_test_group'}}
    cache.must_get_namespace = mock.Mock()
    cache.must_get_namespace.return_value = pb
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = [balancer_pb]

    ctl._alerting_processor._namespace_id = pb_id
    ctl._alerting_processor._pb = pb
    with mock.patch('awacs.model.balancer.endpoints.endpoint_set_exists') as m:
        m.side_effect = (
            lambda endpoint_set_id, *args, **kwargs: endpoint_set_id
            == 'awacs-rtc_balancer_test'
        )
        ctl._process(ctx)

    cache.list_all_balancers.assert_called_with(pb.meta.id)
    actual_juggler_checks = juggler_client_mock.sync_checks.call_args_list[0][1][
        'checks'
    ]
    assert len(actual_juggler_checks) == 2
    assert 2 == len(
        [
            check
            for check in actual_juggler_checks
            if u'test_awacs_notify_group_platform_group' in check.tags
        ]
    )
    assert 0 == len(
        [
            check
            for check in actual_juggler_checks
            if u'test_awacs_notify_group_balancer_group' in check.tags
        ]
    )

    actual_yasm_alerts = yasm_client_mock.replace_alerts.call_args[0][1]
    assert len(actual_yasm_alerts) == 4
    assert 4 == len(
        [
            alert
            for alert in actual_yasm_alerts
            if u'test_awacs_notify_group_platform_group'
            in alert['juggler_check']['tags']
        ]
    )
    assert 0 == len(
        [
            alert
            for alert in actual_yasm_alerts
            if u'test_awacs_notify_group_balancer_group'
            in alert['juggler_check']['tags']
        ]
    )


@pytest.mark.parametrize('alerting_version', ALL_VERSIONS)
def test_process_attempt_no_l7(caplog, cache, zk_storage, ctx, ctl, alerting_version):
    cache.list_all_balancers = mock.Mock()
    cache.list_all_balancers.return_value = []

    ctl._alerting_processor._sync_juggler_namespace = mock.Mock()
    ctl._alerting_processor._sync_juggler_namespace.return_value = (
        juggler_client.CreateOrUpdateNamespaceResult(created=True, updated=False)
    )
    ctl._alerting_processor._sync_yasm_alerts = mock.Mock()
    ctl._alerting_processor._sync_yasm_alerts.return_value = (
        yasm_client.YasmReplaceAlertsResult(updated=1, created=0, deleted=0)
    )
    ctl._alerting_processor._sync_juggler_notify_rules = mock.Mock()
    ctl._alerting_processor._sync_juggler_notify_rules.return_value = (
        juggler_client.SyncNotifyRulesResult(add=1, remove=1)
    )
    ctl._process(ctx)

    def check():
        status = cache.must_get_namespace(NS_ID).alerting_sync_status
        assert status.last_successful_attempt == status.last_attempt
        assert status.last_successful_attempt.succeeded.status == 'True'
        assert status.last_attempt.succeeded.status == 'True'

    wait_until_passes(check)


@pytest.mark.parametrize('alerting_version', ALL_VERSIONS)
def test_process_attempt(caplog, cache, zk_storage, ctx, ctl, alerting_version):
    # case: normal process
    ctl._alerting_processor._sync_juggler_namespace = mock.Mock()
    ctl._alerting_processor._sync_juggler_namespace.return_value = (
        juggler_client.CreateOrUpdateNamespaceResult(created=True, updated=False)
    )
    ctl._alerting_processor._sync_yasm_alerts = mock.Mock()
    ctl._alerting_processor._sync_yasm_alerts.return_value = (
        yasm_client.YasmReplaceAlertsResult(updated=1, created=0, deleted=0)
    )
    ctl._alerting_processor._sync_juggler_notify_rules = mock.Mock()
    ctl._alerting_processor._sync_juggler_notify_rules.return_value = (
        juggler_client.SyncNotifyRulesResult(add=1, remove=1)
    )
    ctl._process(ctx)

    def check():
        new_ns = cache.must_get_namespace(NS_ID)
        assert new_ns.HasField('alerting_sync_status')
        status = new_ns.alerting_sync_status
        assert status.last_successful_attempt == status.last_attempt
        assert status.last_successful_attempt.succeeded.status == 'True'

    wait_until_passes(check)

    # case: failed juggler_notify_rules
    ctl._alerting_processor._sync_juggler_notify_rules.side_effect = Exception(
        '_sync_juggler_notify_rules exc'
    )

    with pytest.raises(Exception):
        ctl._process(ctx)

    def check():
        new_ns = cache.must_get_namespace(NS_ID)
        assert new_ns.HasField('alerting_sync_status')
        status = new_ns.alerting_sync_status
        assert status.last_successful_attempt != status.last_attempt
        assert status.last_successful_attempt.succeeded.status == 'True'
        assert status.last_attempt.succeeded.status == 'False'
        assert (
            status.last_attempt.succeeded.message
            == 'Failed to sync juggler notify rules: _sync_juggler_notify_rules exc'
        )

    wait_until_passes(check)

    # case: failed sync_yasm_alerts
    ctl._alerting_processor._sync_yasm_alerts.side_effect = Exception(
        '_sync_yasm_alerts exc'
    )

    with pytest.raises(Exception):
        ctl._process(ctx)

    def check():
        new_ns = cache.must_get_namespace(NS_ID)
        assert new_ns.HasField('alerting_sync_status')
        status = new_ns.alerting_sync_status
        assert status.last_successful_attempt != status.last_attempt
        assert status.last_successful_attempt.succeeded.status == 'True'
        assert status.last_attempt.succeeded.status == 'False'
        assert (
            status.last_attempt.succeeded.message
            == 'Failed to sync yasm alerts: _sync_yasm_alerts exc'
        )

    wait_until_passes(check)

    # case: failed  sync juggler namespace
    ctl._alerting_processor._sync_juggler_namespace.side_effect = Exception(
        '_sync_juggler_namespace exc'
    )

    with pytest.raises(Exception):
        ctl._process(ctx)

    def check():
        new_ns = cache.must_get_namespace(NS_ID)
        assert new_ns.HasField('alerting_sync_status')
        status = new_ns.alerting_sync_status
        assert status.last_successful_attempt != status.last_attempt
        assert status.last_successful_attempt.succeeded.status == 'True'
        assert status.last_attempt.succeeded.status == 'False'
        assert (
            status.last_attempt.succeeded.message
            == 'Failed to sync juggler namespace: _sync_juggler_namespace exc'
        )

    wait_until_passes(check)


@flaky.flaky(max_runs=5, min_passes=1)
@pytest.mark.parametrize('alerting_version', ALL_VERSIONS)
def test_needs_alerting_sync(
    caplog,
    cache,
    zk_storage,
    ctx,
    alerting_version,
    create_default_namespace,
):
    create_default_namespace(NS_ID)
    with mock.patch('time.time') as time_mock:
        time_mock.return_value = 0

        ctl = NamespaceCtl(
            NS_ID,
            {
                'sync_delay_interval_from': 10,
                'sync_delay_interval_to': 1810,
                'name_prefix': 'test_awacs',
            },
        )
        assert 10 < ctl._alerting_processor._sync_delay_interval <= 1810

        ctl = NamespaceCtl(
            NS_ID,
            {'sync_delay_interval_from': 10, 'name_prefix': 'test_awacs'},
        )
        alerting_config = alerting.get_config(alerting_version)
        cur_alerting_setting = model_pb2.NamespaceSpec.AlertingSettings()
        alerting_setting = model_pb2.NamespaceSpec.AlertingSettings()

        sync_check_deadline = 10
        current_monotonic_time = 5
        with check_log(caplog) as log:
            res = ctl._alerting_processor._needs_alerting_sync(
                ctx,
                alerting_config,
                cur_alerting_setting,
                alerting_setting,
                sync_check_deadline,
                current_monotonic_time,
                False,
                False,
            )
            assert not res

        res = ctl._alerting_processor._needs_alerting_sync(
            ctx,
            alerting_config,
            cur_alerting_setting,
            alerting_setting,
            sync_check_deadline,
            current_monotonic_time,
            False,
            True,
        )
        assert res

        sync_check_deadline = 10
        current_monotonic_time = 20
        res = ctl._alerting_processor._needs_alerting_sync(
            ctx,
            alerting_config,
            cur_alerting_setting,
            alerting_setting,
            sync_check_deadline,
            current_monotonic_time,
            False,
            False,
        )
        assert res

        cur_alerting_setting.version = '0.0.1'
        alerting_setting.version = '0.0.2'
        alerting_setting.juggler_raw_downtimers.staff_logins.append('i-dyachkov')
        notify_rule = alerting_setting.juggler_raw_notify_rules.balancer.add()
        notify_rule.template_name = 'test'
        with check_log(caplog) as log:
            res = ctl._alerting_processor._needs_alerting_sync(
                ctx,
                alerting_config,
                cur_alerting_setting,
                alerting_setting,
                sync_check_deadline,
                current_monotonic_time,
                False,
                False,
            )
            assert res
            assert (
                'alerting version has been changed: 0.0.1 -> 0.0.2'
                in log.records_text()
            )
            assert 'alerting juggler downtimers has been changed' in log.records_text()
            assert (
                'alerting juggler notify rules has been changed' in log.records_text()
            )

        res = ctl._alerting_processor._needs_alerting_sync(
            ctx,
            alerting_config,
            cur_alerting_setting,
            alerting_setting,
            sync_check_deadline,
            current_monotonic_time,
            True,
            False,
        )

        assert res


@pytest.mark.parametrize('alerting_version', ALL_VERSIONS)
def test_cleanup_alerting_entities(
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    juggler_client_mock,
    alerting_version,
):
    ctl._alerting_processor.namespace_id = NS_ID
    ctl._alerting_processor._alerting_prefix = 'test_awacs'
    assert ctl._alerting_processor.maybe_self_delete(ctx)
    yasm_client_mock.replace_alerts.assert_called_with('test_awacs.{}.'.format(NS_ID), [])
    juggler_client_mock.sync_notify_rules.assert_called_with(
        'test_awacs.{}'.format(NS_ID), []
    )
    juggler_client_mock.remove_namespace_if_exists('test_awacs.{}'.format(NS_ID))
    juggler_client_mock.cleanup_checks.assert_called_with(
        'test_awacs.{}'.format(NS_ID), 'test_awacs_{}_checks'.format(NS_ID)
    )

    juggler_client_mock.remove_namespace_if_exists.side_effect = (
        juggler_client.JugglerClient.BadRequestError('test')
    )
    assert not ctl._alerting_processor.maybe_self_delete(ctx)


@pytest.mark.parametrize('alerting_version', ALL_VERSIONS)
def test_orly_forbidden(
    ctx,
    ctl,
    namespace_pb,
    yasm_client_mock,
    juggler_client_mock,
    alerting_version,
):
    with mock.patch.object(ctl._alerting_processor, '_sync_brake') as sync_brake:
        sync_brake.maybe_apply = mock.Mock(side_effect=OrlyBrakeApplied)
        original_sync_deadline = ctl._alerting_processor._sync_check_deadline
        ctl._alerting_processor._alerting_sync_attempt = mock.Mock()
        ctl._process(ctx)
        ctl._alerting_processor._alerting_sync_attempt.assert_not_called()
        assert (
            60
            <= (ctl._alerting_processor._sync_check_deadline - original_sync_deadline)
            <= 5 * 60
        )


def test_remove_namespace(caplog, ctx, ctl, namespace_pb, cache, dao, zk_storage):
    for ns_pb in zk_storage.update_namespace(NS_ID, namespace_pb):
        ns_pb.order.progress.state.id = u'FINALISING'
        ns_pb.spec.incomplete = True
        ns_pb.spec.deleted = True
        ns_pb.spec.ClearField(b'alerting')
    assert wait_until(lambda: cache.must_get_namespace(NS_ID).spec.deleted)

    # case: namespace has incomplete order
    ctl._self_deletion_check_deadline = monotonic.monotonic()
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'namespace marked for removal' in log.records_text()
        assert (
            u'In-progress namespace with incomplete order can not be removed.'
            in log.records_text()
        )

    # case: namespace has cert
    for ns_pb in zk_storage.update_namespace(NS_ID, namespace_pb):
        ns_pb.order.status.status = u'FINISHED'
    assert wait_until(
        lambda: cache.must_get_namespace(NS_ID).order.status.status == u'FINISHED'
    )
    dao.create_cert(
        meta_pb=model_pb2.CertificateMeta(id=CERT_ID, namespace_id=NS_ID),
        login=u'test',
        order_pb=model_pb2.CertificateOrder.Content(),
    )
    wait_until_passes(lambda: cache.must_get_cert(NS_ID, CERT_ID))

    ctl._self_deletion_check_deadline = monotonic.monotonic()
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'namespace marked for removal' in log.records_text()
        assert (
            u'Deleting this namespace is not possible: it has 1 certificate(s). '
            u'Please remove all certificates before removing the namespace.'
        ) in log.records_text()

    assert cache.get_namespace(namespace_pb.meta.id) is not None
    dao.delete_cert(NS_ID, CERT_ID)
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID) is None)

    # case: namespace has dns_record
    dao.create_dns_record(
        meta_pb=model_pb2.DnsRecordMeta(id=DNS_RECORD_ID, namespace_id=NS_ID),
        spec_pb=model_pb2.DnsRecordSpec(),
        login=u'test',
    )
    wait_until_passes(lambda: cache.must_get_dns_record(NS_ID, DNS_RECORD_ID))

    ctl._self_deletion_check_deadline = monotonic.monotonic()
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'namespace marked for removal' in log.records_text()
        assert (
            u'Deleting this namespace is not possible: it has 1 DNS record(s). '
            u'Please remove all DNS records before removing the namespace.'
        ) in log.records_text()

    assert cache.get_namespace(namespace_pb.meta.id) is not None
    dao.delete_dns_record(NS_ID, DNS_RECORD_ID)
    assert wait_until(lambda: cache.get_dns_record(NS_ID, DNS_RECORD_ID) is None)

    if objects.L7HeavyConfig.cache.get(NS_ID, NS_ID) is not None:
        ctl._self_deletion_check_deadline = monotonic.monotonic()
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'namespace marked for removal' in log.records_text()
            assert (
                u'Deleting this namespace is not possible: it has 1 L7Heavy config(s). '
                u'Please remove all L7Heavy configs before removing the namespace.'
            ) in log.records_text()

        assert cache.get_namespace(namespace_pb.meta.id) is not None
        objects.L7HeavyConfig.remove(NS_ID, NS_ID)
        print("THERE")
        assert wait_until(lambda: objects.L7HeavyConfig.cache.get(NS_ID, NS_ID) is None, timeout=5)

    for ns_id in (NS_ID, u'another_namespace'):

        def fill_meta(pb):
            pb.meta.namespace_id = ns_id
            if not isinstance(pb, model_pb2.NamespaceAspectsSet):
                pb.meta.id = u'id'
            return pb

        zk_storage.create_upstream(ns_id, u'id', fill_meta(model_pb2.Upstream()))
        zk_storage.create_knob(ns_id, u'id', fill_meta(model_pb2.Knob()))
        zk_storage.create_backend(ns_id, u'id', fill_meta(model_pb2.Backend()))
        zk_storage.create_endpoint_set(ns_id, u'id', fill_meta(model_pb2.EndpointSet()))
        zk_storage.create_namespace_aspects_set(
            ns_id, fill_meta(model_pb2.NamespaceAspectsSet())
        )
        objects.NamespaceOperation.zk.create(fill_meta(model_pb2.NamespaceOperation()))

        wait_until_passes(lambda: cache.must_get_upstream(ns_id, u'id'))
        wait_until_passes(lambda: cache.must_get_knob(ns_id, u'id'))
        wait_until_passes(lambda: cache.must_get_backend(ns_id, u'id'))
        wait_until_passes(lambda: cache.must_get_endpoint_set(ns_id, u'id'))
        wait_until_passes(lambda: cache.must_get_namespace_aspects_set(ns_id))
        wait_until_passes(
            lambda: objects.NamespaceOperation.cache.must_get(ns_id, u'id')
        )

    ctl._self_deletion_check_deadline = monotonic.monotonic()
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'namespace marked for removal' in log.records_text()

    assert wait_until(lambda: cache.get_namespace(NS_ID) is None)

    cache.must_get_upstream(u'another_namespace', u'id')
    cache.must_get_knob(u'another_namespace', u'id')
    cache.must_get_backend(u'another_namespace', u'id')
    cache.must_get_endpoint_set(u'another_namespace', u'id')
    cache.must_get_namespace_aspects_set(u'another_namespace')
    objects.NamespaceOperation.cache.must_get(u'another_namespace', u'id')

    assert cache.get_upstream(NS_ID, u'id') is None
    assert cache.get_knob(NS_ID, u'id') is None
    assert cache.get_backend(NS_ID, u'id') is None
    assert cache.get_endpoint_set(NS_ID, u'id') is None
    assert cache.get_namespace_aspects_set(NS_ID) is None
    assert objects.NamespaceOperation.cache.get(NS_ID, u'id') is None


@pytest.mark.parametrize('alerting_version', ALL_VERSIONS)
def test_remove_namespace_with_alerting(
    caplog,
    ctx,
    ctl,
    namespace_pb,
    cache,
    dao,
    zk_storage,
    alerting_version,
):
    for ns_pb in zk_storage.update_namespace(NS_ID, namespace_pb):
        ns_pb.spec.incomplete = False
        ns_pb.spec.deleted = True
        ns_pb.spec.alerting.version = six.text_type(alerting_version)
    assert wait_until(lambda: cache.must_get_namespace(NS_ID).spec.deleted)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'namespace marked for removal' in log.records_text()

    assert wait_until(lambda: cache.get_namespace(NS_ID) is None, timeout=5)
