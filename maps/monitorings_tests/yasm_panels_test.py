import os
import typing as tp
import yatest

from mock import Mock, patch

from maps.infra.sedem.cli.lib.monitorings.golovan import RawMetric, MetricType
from maps.infra.sedem.cli.lib.monitorings.golovan_panel import GolovanPanelGenerator
from maps.infra.sedem.cli.lib.monitorings.monitorings import Monitorings
from maps.infra.sedem.cli.lib.service import Service
from maps.infra.sedem.cli.modules.balancer import Balancer
from maps.pylibs.fixtures.api_fixture import ApiFixture


def raw_metrics() -> list[RawMetric]:
    tags = {'hosts': 'ASEARCH', 'itype': 'maps', 'prj': 'maps-core-ecstatic-coordinator'}

    signals = [
        RawMetric(
            signal=signal_name,
            type=MetricType.SERVICE,
            tags=tags
        ) for signal_name in (
            'roquefort-external_error_ammv',
            'roquefort-external_rps_ammv',
            'roquefort-total_error_ammv',
            'roquefort-total_rps_ammv',
            'roquefort-external_ping_error_ammv',
            'roquefort-external_ping_rps_ammv',
        )
    ]

    for handle in (
            '/current_versions',
            '/dataset/list',
            '/download',
            '/exists',
            '/is_adopted',
            '/is_managed',
            '/list_status',
            '/list_versions',
            '/locks/extend',
            '/locks/get',
            '/locks/list',
            '/locks/release',
            '/notify',
            '/ping',
            '/pkg/_/_/errlogs/_',
            '/pkg/_/_/status',
            '/pkg/_/versions',
            '/pkg/list',
            '/postdl',
            '/postdl_failed',
            '/postdl_started',
            '/remove',
            '/replication_config',
            '/reset_errors',
            '/retire',
            '/self_fqdn',
            '/step_in',
            '/switch_failed',
            '/torrents',
            '/tracker/announce',
            '/upload',
            '/versions'):
        for code in ('error', 'ok', '403', '404', '429', '3xx', '401', '4xx', '499'):
            signals.append(RawMetric(
                signal=f'roquefort-ecstatic-coordinator_{handle}_{code}_ammv',
                type=MetricType.SERVICE,
                tags=tags
            ))

    signals.append(RawMetric(
        signal='yacare-ecstatic-coordinator_mongo_connections_count_axxv',
        type=MetricType.SERVICE,
        tags=tags
    ))

    for volume in ('/cores', '/logs', 'root', 'cwd'):
        signals.append(RawMetric(
            signal=f'portoinst-volume_{volume}_usage_perc_txxx',
            type=MetricType.SERVICE,
            tags=tags
        ))

    return signals


@patch.object(Service, 'staging_list', Mock(return_value=['stable']))
def test_panel_creation(api_mock: ApiFixture, monkeypatch) -> str:
    def patched_nanny_services(self, staging: str, balancer_name: str) -> tp.Optional[list[str]]:
        if balancer_name == 'default':
            return ['balancer-nanny-service']
        raise RuntimeError(f'No nanny for {staging} {balancer_name} balancer')

    monkeypatch.setattr(Balancer, 'nanny_services', patched_nanny_services)
    api_mock.nanny.set_prj_tag(service_name='balancer-nanny-service', prj_tag='core-ecstatic-coordinator-maps')

    os.environ['SEDEM_YA_ROOT'] = yatest.common.test_source_path('../test-data')
    service = Service('maps/infra/ecstatic/coordinator')

    metrics = Monitorings(service, raw_metrics()).final_metrics()
    generator = GolovanPanelGenerator(service)

    return generator.generate(service.namer.yasm_panel_name(staging='stable'), 'stable', metrics, raw_metrics())
