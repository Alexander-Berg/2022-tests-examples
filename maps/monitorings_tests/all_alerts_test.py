import os
import typing as tp
from mock import Mock, patch
from operator import itemgetter

import yatest
from juggler_sdk import JugglerApi

from maps.infra.sedem.cli.lib.monitorings.monitorings_controller import MonitoringsController
from maps.infra.sedem.cli.lib.service import Service
from maps.infra.sedem.cli.modules.balancer import Balancer
from maps.infra.sedem.cli.lib.monitorings.golovan import GolovanAPI, RawMetric, MetricType
from maps.infra.sedem.cli.lib.monitorings.juggler import JugglerAPI, META_ALERT_NAME


def mock_raw_metrics(self) -> list[RawMetric]:
    balancer_tags = {'itype': 'balancer', 'ctype': 'prod', 'prj': 'core-teapot-maps', 'hosts': 'ASEARCH'}
    service_tags = {'itype': 'maps', 'prj': 'maps-core-teapot', 'hosts': 'ASEARCH'}
    return [
        RawMetric(
            signal='balancer_report-report-service_total-outgoing_5xx_summ',
            tags=balancer_tags,
            type=MetricType.BALANCER),
        RawMetric(
            signal='roquefort-total_rps_ammv',
            tags=service_tags,
            type=MetricType.SERVICE),
        RawMetric(
            signal='roquefort-mock_/ping_error_ammv',
            tags=service_tags,
            type=MetricType.SERVICE),
    ]


def mock_juggler_alerts(self, *args, **kwargs) -> list[dict[str, tp.Any]]:
    alerts = [
        {
            'service_name': 'mock-juggler-alert',
            'staging': deploy_unit,
            'degrade_warn': '34%',
            'degrade_crit': '67%',
        }
        for deploy_unit in ('testing', 'stable')
    ]
    alerts += [
        {
            'service_name': 'ignore-juggler-alert',
            'staging': 'stable',
            'ignore_on_deploy': True,
        }
    ]
    return alerts


def mock_golovan_alerts(self, *args, **kwargs) -> list[dict[str, tp.Any]]:
    alerts = [
        {
            'service_name': 'mock-golovan-alert',
            'staging': deploy_unit,
            'warn': 0.1,
            'crit': 1.0,
        }
        for deploy_unit in ('testing', 'stable')
    ]
    alerts += [
        {
            'service_name': 'ignore-golovan-alert',
            'staging': 'stable',
            'ignore_on_deploy': True,
        }
    ]
    return alerts


@patch.object(GolovanAPI, 'query_raw_metrics', mock_raw_metrics)
@patch.object(Balancer, 'nanny_services', Mock(return_value=['balancer-nanny-service']))
@patch.object(Balancer, 'yasm_prj', Mock(return_value='core-teapot-maps'))
@patch.object(Service, 'staging_list', Mock(return_value=['stable']))
def test_all_nanny_alerts():
    os.environ['SEDEM_YA_ROOT'] = yatest.common.test_source_path('../test-data')
    service = Service('maps/infra/teapot')
    monitorings_controller = MonitoringsController(service)
    return {
        'alerts': monitorings_controller.expanded_all_alerts,
        'metrics': monitorings_controller.expanded_all_metrics
    }


def test_all_garden_alerts():
    os.environ['SEDEM_YA_ROOT'] = yatest.common.test_source_path('../test-data')
    service = Service('maps/garden/modules/backa_export')
    monitorings_controller = MonitoringsController(service)
    return {
        'alerts': monitorings_controller.expanded_all_alerts,
        'metrics': monitorings_controller.expanded_all_metrics
    }


def test_all_sandbox_alerts():
    os.environ['SEDEM_YA_ROOT'] = yatest.common.test_source_path('../test-data')
    service = Service('maps/infra/ecstatic/sandbox')
    monitorings_controller = MonitoringsController(service)
    return {
        'alerts': monitorings_controller.expanded_all_alerts,
        'metrics': monitorings_controller.expanded_all_metrics
    }


@patch.object(GolovanAPI, 'query_raw_metrics', mock_raw_metrics)
@patch.object(Balancer, 'nanny_services', Mock(return_value=['balancer-nanny-service']))
@patch.object(Balancer, 'yasm_prj', Mock(return_value='core-teapot-maps'))
@patch.object(Service, 'staging_list', Mock(return_value=['testing', 'stable']))
@patch.object(JugglerAPI, 'create_alerts', mock_juggler_alerts)
@patch.object(GolovanAPI, 'create_alerts', mock_golovan_alerts)
@patch.object(JugglerAPI, 'api', Mock(spec=JugglerApi))
def test_meta_alerts():
    os.environ['SEDEM_YA_ROOT'] = yatest.common.test_source_path('../test-data')
    service = Service('maps/infra/teapot')
    alerts = MonitoringsController(service).create_alerts()
    actual_meta_alerts = sorted(
        (alert for alert in alerts.juggler if alert['service_name'] == META_ALERT_NAME),
        key=itemgetter('staging'),
    )
    assert actual_meta_alerts == [{
        'service_name': META_ALERT_NAME,
        'staging': 'stable',
        'description': 'Meta alert for "stable": used to check crits history before deploy',
        'aggregate_checks': ['mock-juggler-alert', 'mock-golovan-alert'],
        'notifications': [],
    }, {
        'service_name': META_ALERT_NAME,
        'staging': 'testing',
        'description': 'Meta alert for "testing": used to check crits history before deploy',
        'aggregate_checks': ['mock-juggler-alert', 'mock-golovan-alert'],
        'notifications': [],
    }]
