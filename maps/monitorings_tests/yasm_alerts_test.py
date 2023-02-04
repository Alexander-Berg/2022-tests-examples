import typing as tp
from dataclasses import dataclass, field
from operator import attrgetter

import pytest

from maps.infra.sedem.cli.lib.monitorings.monitorings import Monitorings, FinalMetric
from maps.infra.sedem.cli.lib.service import Resources
from maps.infra.sedem.cli.modules.balancer import Balancer
from maps.infra.sedem.cli.lib.monitorings.golovan import GolovanAPI, MetricType, RawMetric
from maps.infra.sedem.cli.tests.typing import ServiceFactory
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.matchers import Match


def test_add_s3_bucket_with_custom_abc(api_mock: ApiFixture, service_factory: ServiceFactory) -> None:
    api_mock.abc.add_service(1234, '1234')
    api_mock.abc.add_service(12345, '12345')
    api_mock.nanny.set_docker_tag(service_name='maps_core_mock_stable', docker_tag='maps/mock:1')
    service = service_factory(config={
        'main': {
            'name': 'mock',
            'abc_service': '1234'
        },
        'resources': {'stable': {
            's3': [
                {'buckets': ['1', '2']},
                {'abc_service': '12345', 'buckets': ['3']}
            ]
        }}
    })

    assert set(service.s3) == {'stable'}
    assert sorted(service.s3['stable']) == sorted([
        Resources.S3Bucket(abc_service='1234', buckets=['1', '2']),
        Resources.S3Bucket(abc_service='12345', buckets=['3'])
    ])


@dataclass
class MetricsCreationTest:
    name: str
    resources: dict
    signals: list
    expected_metrics: list
    balancers: dict = field(default_factory=dict)
    filters: list = field(default_factory=list)

    def __str__(self) -> str:
        return self.name


POSTGRES_TEST_CLUSTER_ID = '98b9062d-01fa-4250-ab59-a5e67a1edb9b'
MONGODB_TEST_CLUSTER_ID = 'abcdefgh-ijkl-mnop-qrst-uvwxyz123456'

METRICS_CREATION_TESTS = [
    # Service metrics test
    MetricsCreationTest(
        name='service_test',
        resources={'stable': {}},
        signals=[{
            'signal': signal_name,
            'type': MetricType.SERVICE,
            'tags': {'hosts': 'ASEARCH', 'itype': 'maps', 'ctype': 'stable', 'prj': 'maps-core-mock'}
        }
        for signal_name in (
            'roquefort-external_error_ammv',
            'roquefort-external_rps_ammv',
            'roquefort-total_error_ammv',
            'roquefort-total_rps_ammv',
            'roquefort-external_ping_error_ammv',
            'roquefort-external_ping_rps_ammv',
            'roquefort-mock_/ping_error_ammv',
            'roquefort-mock_/ping_ok_ammv',
            'roquefort-mock_/ping_rps_ammv',
        )],
        expected_metrics=[
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='total-5xx',
                signal=('mul(perc({numerator}, max(1,{denominator})), '
                        'min(floor(div({denominator}, {threshold})), 1))').format(
                    numerator=('diff(or(roquefort-external_error_ammv, roquefort-total_error_ammv), '
                               'max(roquefort-external_ping_error_ammv, 0))'),
                    denominator=('diff(or(roquefort-external_rps_ammv, roquefort-total_rps_ammv), '
                                 'max(roquefort-external_ping_rps_ammv, 0))'),
                    threshold=10,
                ),
                alert={'warn': 5, 'crit': 5, 'flaps': {'stable_time': 600}},
                graphic=None,
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='http-ping-error',
                signal='perc(diff(or(roquefort-external_ping_rps_ammv, roquefort-mock_/ping_rps_ammv),\n'
                       '          or(roquefort-external_ping_ok_ammv, roquefort-mock_/ping_ok_ammv)),\n'
                       '     max(1, or(roquefort-external_ping_rps_ammv, roquefort-mock_/ping_rps_ammv)))',
                alert={'warn': 34, 'crit': 50, 'flaps': {'stable_time': 60}, 'aggregation': {'window': 20, 'type': 'aver'}},
                graphic=None,
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='handle-for-panel-mock-_ping-error',
                signal='roquefort-mock_/ping_error_ammv',
                alert=None,
                graphic={
                    'handle': {
                        'metric': 'mock_/ping',
                        'name': 'mock: /ping',
                        'timings': [100, 250, 500, 1000, 2000]
                    },
                    'handle_code': 'error'
                },
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='handle-for-panel-mock-_ping-ok',
                signal='roquefort-mock_/ping_ok_ammv',
                alert=None,
                graphic={
                    'handle': {
                        'metric': 'mock_/ping',
                        'name': 'mock: /ping',
                        'timings': [100, 250, 500, 1000, 2000]
                    },
                    'handle_code': 'ok'
                },
                staging='stable',
            ),
        ]
    ),

    # Service metrics without /ping signals
    MetricsCreationTest(
        name='service_no_ping_test',
        resources={'stable': {}},
        signals=[{
            'signal': signal_name,
            'type': MetricType.SERVICE,
            'tags': {'hosts': 'ASEARCH', 'itype': 'maps', 'ctype': 'stable', 'prj': 'maps-core-mock'}
        }
        for signal_name in (
            'roquefort-total_error_ammv',
            'roquefort-total_rps_ammv',
        )],
        expected_metrics=[
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='total-5xx',
                signal=('mul(perc({numerator}, max(1,{denominator})), '
                        'min(floor(div({denominator}, {threshold})), 1))').format(
                    numerator='or(roquefort-external_error_ammv, roquefort-total_error_ammv)',
                    denominator='or(roquefort-external_rps_ammv, roquefort-total_rps_ammv)',
                    threshold=10,
                ),
                staging='stable',
            ),
        ]
    ),

    # Service metrics with filters test
    MetricsCreationTest(
        name='service_filters_test',
        resources={'stable': {}},
        filters=[
            {'filter': {'service_name': 'total-5xx'},
             'body': {'alert': {'warn': 10, 'crit': 10, 'flaps': {'stable_time': 100}}}}
        ],
        signals=[{
            'signal': signal_name,
            'type': MetricType.SERVICE,
            'tags': {'hosts': 'ASEARCH', 'itype': 'maps', 'ctype': 'stable', 'prj': 'maps-core-mock'}
        }
        for signal_name in (
            'roquefort-total_error_ammv',
            'roquefort-total_rps_ammv',
            'roquefort-mock_/ping_error_ammv',
            'roquefort-mock_/ping_ok_ammv',
            'roquefort-mock_/ping_rps_ammv',
        )],
        expected_metrics=[
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='total-5xx',
                alert={'warn': 10, 'crit': 10, 'flaps': {'stable_time': 100}},  # overridden by filter
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='http-ping-error',
                alert={'warn': 34, 'crit': 50, 'flaps': {'stable_time': 60}, 'aggregation': {'window': 20, 'type': 'aver'}},
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='handle-for-panel-mock-_ping-error',
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.SERVICE,
                name='handle-for-panel-mock-_ping-ok',
                staging='stable',
            ),
        ]
    ),

    # Balancer metrics test
    MetricsCreationTest(
        name='balancer_test',
        balancers={'stable': [
            {
                'name': 'default',
                'instances_count': 1
            }, {
                'name': 'l3',
                'fqdn': 'l3.maps.yandex.net',
                'l3_only': True
            }
        ]},
        resources={'stable': {}},
        signals=[{
            'signal': signal_name,
            'type': MetricType.BALANCER,
            'tags': {'hosts': 'ASEARCH', 'itype': 'balancer', 'ctype': 'prod', 'prj': 'core-mock-maps'}
        }
        for signal_name in (
            'balancer_report-report-service_total-outgoing_5xx_summ',
            'balancer_report-report-service_total-processing_time_hgram',
        )],
        expected_metrics=[
            Match.HasAttrs(
                type=MetricType.BALANCER,
                name='default-balancer-timings',
                signal='hperc(balancer_report-report-service_total-processing_time_hgram, 0.8, inf)',
                alert={'warn': 5, 'crit': 5, 'flaps': {'stable_time': 90}, 'aggregation': {'window': 30, 'type': 'aver'}},
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.BALANCER,
                name='default-balancer-5xx',
                signal='mul(\n'
                       '  perc(balancer_report-report-service_total-outgoing_5xx_summ,\n'
                       '       max(1, hcount(balancer_report-report-service_total-processing_time_hgram))),\n'
                       '  min(floor(div(hcount(balancer_report-report-service_total-processing_time_hgram), 50)), 1))',
                alert={'warn': 0.5, 'crit': 1, 'flaps': {'stable_time': 90}},
                staging='stable',
            ),
        ]
    ),

    # S3 metrics test
    MetricsCreationTest(
        name='s3_test',
        resources={'stable': {'s3': [{'buckets': ['1']}]}},
        signals=[{
            'signal': signal_settings['name'],
            'type': MetricType.S3,
            'tags': {
                'hosts': 'CON',
                'itype': signal_settings['itype'],
                'ctype': 'prestable, production',
                'prj': signal_settings['prj']
            }
        }
        for signal_settings in (
            {'name': 's3mds_service_stat-service_used_space_max', 'itype': 's3mdsstat', 'prj': '1234'},
            {'name': 's3mds_service_stat-service_max_size_max', 'itype': 's3mdsstat', 'prj': '1234'},
            {'name': 'mdsproxy_unistat-s3mds_nginx_dmmm', 'itype': 'mdsproxy', 'prj': '1'},
            {'name': 'mdsproxy_unistat-s3mds_nginx_404_dmmm', 'itype': 'mdsproxy', 'prj': '1'},
        )],
        expected_metrics=[
            Match.HasAttrs(
                type=MetricType.S3,
                name='s3-disk-quota-perc-usage-abc-1234',
                signal='perc(s3mds_service_stat-service_used_space_max, s3mds_service_stat-service_max_size_max)',
                alert={'warn': 75, 'crit': 85, 'flaps': {'stable_time': 60}},
                staging='stable'
            ),
            Match.HasAttrs(
                type=MetricType.S3,
                name='1234-1-s3-total-404',
                signal='mul(perc(mdsproxy_unistat-s3mds_nginx_404_dmmm, '
                       'max(1,mdsproxy_unistat-s3mds_nginx_dmmm)), '
                       'min(floor(div(mdsproxy_unistat-s3mds_nginx_dmmm, 10)), 1))',
                alert={'warn': 1, 'crit': 5, 'flaps': {'stable_time': 600}},
                staging='stable'
            )
        ]
    ),

    # MDS metrics test
    MetricsCreationTest(
        name='mds_test',
        resources={'stable': {
            'mds': [{'environment': 'production', 'buckets': ['1']}]
        }},
        signals=[{
            'signal': signal_name,
            'type': MetricType.MDS,
            'tags': {'hosts': 'CON', 'itype': 'mdscloud', 'ctype': 'prestable,production', 'prj': '1'}
        }
        for signal_name in (
            'mastermind-namespaces.stats.effective_used_space_axxx',
            'mastermind-namespaces.stats.space_limit_axxx',
        )],
        expected_metrics=[
            Match.HasAttrs(
                type=MetricType.MDS,
                name='mds-disk-quota-perc-usage-1',
                signal='perc(mastermind-namespaces.stats.effective_used_space_axxx, '
                       'mastermind-namespaces.stats.space_limit_axxx)',
                alert={'warn': 75, 'crit': 85, 'flaps': {'stable_time': 60}},
                staging='stable'
            ),
        ]
    ),

    # MDB metrics test
    MetricsCreationTest(
        name='mdb_test',
        resources={'stable': {
            'mdb': [
                {'type': 'postgres', 'cluster_id': POSTGRES_TEST_CLUSTER_ID},
                {'type': 'mongodb', 'cluster_id': MONGODB_TEST_CLUSTER_ID},
            ]
        }},
        signals=[{
            'signal': signal_settings['name'],
            'type': MetricType.MDB,
            'tags': {
                'hosts': 'CON',
                'itype': signal_settings['itype'],
                'ctype': signal_settings['ctype'],
                **({'tier': 'primary'} if signal_settings['itype'] != 'mdbdom0' else {})
            }
        }
        for signal_settings in (
            # net metrics from both mongo & postgres
            {'name': 'portoinst-net_mb_summ', 'itype': 'mdbdom0', 'ctype': POSTGRES_TEST_CLUSTER_ID},
            {'name': 'portoinst-net_limit_mb_summ', 'itype': 'mdbdom0', 'ctype': POSTGRES_TEST_CLUSTER_ID},
            {'name': 'portoinst-net_mb_summ', 'itype': 'mdbdom0', 'ctype': MONGODB_TEST_CLUSTER_ID},
            {'name': 'portoinst-net_limit_mb_summ', 'itype': 'mdbdom0', 'ctype': MONGODB_TEST_CLUSTER_ID},
            # mem metrics from postgres
            {'name': 'portoinst-anon_usage_slot_hgram', 'itype': 'mdbdom0', 'ctype': POSTGRES_TEST_CLUSTER_ID},
            {'name': 'portoinst-memory_limit_slot_hgram', 'itype': 'mdbdom0', 'ctype': POSTGRES_TEST_CLUSTER_ID},
            # disk metrics from mongo
            {'name': 'push-disk-used_bytes_/var/lib/mongodb_vmmv', 'itype': 'mdbmongodb', 'ctype': MONGODB_TEST_CLUSTER_ID},
            {'name': 'push-disk-total_bytes_/var/lib/mongodb_vmmv', 'itype': 'mdbmongodb', 'ctype': MONGODB_TEST_CLUSTER_ID},
            # replication lag metrics from both
            {'name': 'push-replset_status-replicationLag_vmmv', 'itype': 'mailpostgresql', 'ctype': POSTGRES_TEST_CLUSTER_ID},
            {'name': 'push-replset_status-replicationLag_vmmv', 'itype': 'mdbmongodb', 'ctype': MONGODB_TEST_CLUSTER_ID},
        )],
        expected_metrics=[
            Match.HasAttrs(
                type=MetricType.MDB,
                name='postgres-mdb-cluster-network-limit',
                signal='perc(portoinst-net_mb_summ, portoinst-net_limit_mb_summ)',
                alert={'warn': 75, 'crit': 90},
                staging='stable'
            ),
            Match.HasAttrs(
                type=MetricType.MDB,
                name='mongodb-mdb-cluster-network-limit',
                signal='perc(portoinst-net_mb_summ, portoinst-net_limit_mb_summ)',
                alert={'warn': 75, 'crit': 90},
                staging='stable'
            ),
            Match.HasAttrs(
                type=MetricType.MDB,
                name='mongodb-mdb-master-disk-limit',
                signal='perc(push-disk-used_bytes_/var/lib/mongodb_vmmv, push-disk-total_bytes_/var/lib/mongodb_vmmv)',
                alert={'warn': 75, 'crit': 90},
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.MDB,
                name='postgres-mdb-cluster-memory-limit',
                signal='perc(quant(portoinst-anon_usage_slot_hgram, 95),\n'
                       '     quant(portoinst-memory_limit_slot_hgram, 95))',
                alert={'warn': 75, 'crit': 90, 'flaps': {'stable_time': 60}},
                staging='stable',
            ),
            Match.HasAttrs(
                type=MetricType.MDB,
                name='postgres-mdb-replication-lag',
                signal='push-replset_status-replicationLag_vmmv',
                alert={'warn': 0.1, 'crit': 10},
                staging='stable'
            ),
            Match.HasAttrs(
                type=MetricType.MDB,
                name='mongodb-mdb-replication-lag',
                signal='push-replset_status-replicationLag_vmmv',
                alert={'warn': 0.1, 'crit': 10},
                staging='stable'
            ),
        ]
    ),

    # MDB metrics duplication test
    MetricsCreationTest(
        name='mdb_duplicated_metrics_test',
        resources={
            # same mongo for stable and prestable
            'stable': {'mdb': [{'type': 'mongodb', 'cluster_id': MONGODB_TEST_CLUSTER_ID}]},
            'prestable': {'mdb': [{'type': 'mongodb', 'cluster_id': MONGODB_TEST_CLUSTER_ID}]},
        },
        signals=[{
            'signal': 'portoinst-cpu_limit_usage_perc_hgram',
            'type': MetricType.MDB,
            'tags': {'hosts': 'CON', 'itype': 'mdbdom0', 'ctype': MONGODB_TEST_CLUSTER_ID}
        }],
        expected_metrics=[
            Match.HasAttrs(
                type=MetricType.MDB,
                name='mongodb-mdb-cluster-cpu-limit',
                signal='quant(portoinst-cpu_limit_usage_perc_hgram, 95)',
                alert={'warn': 75, 'crit': 90, 'flaps': {'stable_time': 60}},
                staging='stable'
            ),
            Match.HasAttrs(
                type=MetricType.MDB,
                name='mongodb-mdb-cluster-cpu-limit',
                signal='quant(portoinst-cpu_limit_usage_perc_hgram, 95)',
                alert={'warn': 75, 'crit': 90, 'flaps': {'stable_time': 60}},
                staging='prestable',
            ),
        ]
    ),
]


@pytest.mark.parametrize('case', METRICS_CREATION_TESTS, ids=str)
def test_metrics_creation(api_mock: ApiFixture,
                          service_factory: ServiceFactory,
                          case: MetricsCreationTest,
                          monkeypatch) -> None:
    def patched_nanny_services(self, staging: str, balancer_name: str) -> tp.Optional[list[str]]:
        if balancer_name == 'default':
            return ['balancer-nanny-service']
        raise RuntimeError(f'No nanny for {staging} {balancer_name} balancer')

    monkeypatch.setattr(Balancer, 'nanny_services', patched_nanny_services)

    api_mock.abc.add_service(1234, '1234')
    api_mock.nanny.set_docker_tag(service_name='maps_core_mock_stable', docker_tag='maps/mock:1')
    api_mock.nanny.set_docker_tag(service_name='maps_core_mock_prestable', docker_tag='maps/mock:1')
    api_mock.nanny.set_prj_tag(service_name='balancer-nanny-service', prj_tag='core-mock-maps')

    service_config = {
        'main': {
            'name': 'mock',
            'abc_service': '1234',
            'balancer': case.balancers,
        },
        'resources': case.resources,
    }
    if case.filters:
        service_config['filters'] = {'metrics_convertors': case.filters}
    service = service_factory(config=service_config)

    monitorings = Monitorings(service, [RawMetric(**signal) for signal in case.signals])
    actual_metrics = sorted(monitorings.final_metrics(), key=attrgetter('name'))
    expected_metrics = sorted(case.expected_metrics, key=attrgetter('name'))
    assert actual_metrics == expected_metrics


@dataclass
class AlertsCreationTest:
    name: str
    expected_alerts: tp.Optional[list] = None
    alert_settings: tp.Optional[dict] = None
    notification_staging: tp.Optional[str] = None
    alert_staging: tp.Optional[str] = None

    def __str__(self) -> str:
        return self.name


ALERTS_CREATION_TESTS = [
    AlertsCreationTest(
        name='simple_warn_crit',
        alert_settings={'warn': 75, 'crit': 90},
        expected_alerts=[Match.HasAttrs(
            name='maps_core_mock_stable.total-5xx',
            warn=[75, 90],
            crit=[90, None]
        )],
    ),
    AlertsCreationTest(
        name='aggregation_window',
        alert_settings={'warn': 50, 'crit': 100, 'aggregation': {'type': 'aver', 'window': 100}},
        expected_alerts=[Match.HasAttrs(
            name='maps_core_mock_stable.total-5xx',
            warn=[50, 100],
            crit=[100, None],
            value_modify={'type': 'aver', 'window': 100}
        )]
    ),
    AlertsCreationTest(
        name='aggregation_window',
        alert_settings={
            'warn': 50,
            'crit': 100,
            'trend': {
                'type': 'up',
                'interval': 42,
                'use_absolute': False,
                'aggregation': {'type': 'quant', 'quant': 90, 'offset': 60}
            }
        },
        expected_alerts=[Match.HasAttrs(
            name='maps_core_mock_stable.total-5xx',
            trend='up',
            interval=42,
            warn_perc=50,
            crit_perc=100,
            interval_modify={'type': 'quant', 'quant': 90, 'interval_end_offset': 60}
        )]
    ),
    AlertsCreationTest(
        name='keep_testing_alerts_without_notifications',
        alert_settings={'warn': 75, 'crit': 90},
        expected_alerts=[Match.HasAttrs(
            name='maps_core_mock_testing.total-5xx',
            warn=[75, 90],
            crit=[90, None]
        )],
        alert_staging='testing'
    ),
    AlertsCreationTest(
        name='drop_unstable_alerts_without_notifications',
        alert_settings={'warn': 75, 'crit': 90},
        expected_alerts=[],
        alert_staging='unstable'
    )
]


@pytest.mark.parametrize('case', ALERTS_CREATION_TESTS, ids=str)
def test_alerts_creation(api_mock: ApiFixture, service_factory: ServiceFactory, case: AlertsCreationTest) -> None:
    api_mock.abc.add_service(1234, '1234')
    api_mock.nanny.set_docker_tag(service_name='maps_core_mock_stable', docker_tag='maps/mock:1')
    api_mock.nanny.set_docker_tag(service_name='maps_core_mock_prestable', docker_tag='maps/mock:1')
    service = service_factory(config={
        'main': {
            'name': 'mock',
            'abc_service': '1234',
        },
        'resources': {'stable': {}, 'prestable': {}, 'testing': {}, 'unstable': {}},
        'filters': {
            'alerts': [{
                'filter': {
                    'staging': case.notification_staging or 'stable'
                },
                'body': {
                    'notifications': [{
                        'type': 'telegram',
                        'login': ['maps-infra-spam'],
                        'status': ['CRIT', 'WARN']
                    }],
                }
            }]
        }
    })

    alert_staging = case.alert_staging or 'stable'

    GolovanAPI(service).create_alerts([
        FinalMetric(
            type=MetricType.SERVICE,
            tags={'itype': 'maps', 'hosts': 'ASEARCH'},
            name='total-5xx',
            signal='perc(roquefort-total_error_ammv, roquefort-total_rps_ammv)',
            alert=case.alert_settings,
            graphic=None,
            staging=alert_staging
        )
    ])

    actual_alerts = api_mock.golovan.alerts(prefix=f'maps_core_mock_{alert_staging}')
    assert actual_alerts == case.expected_alerts
