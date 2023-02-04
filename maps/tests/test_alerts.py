import pytest
from pydantic import ValidationError

from maps.infra.sedem.lib.config.schema import ServiceConfig
from maps.infra.sedem.lib.config.schema.alerts import (
    Alert,
    AlertCheckOptions,
    AlertFilter,
    AlertFlaps,
    AlertType,
    BalancerAlertPingSettings,
    EmailNotification,
    FilterCondition,
    FiltersSection,
    HandleMetric,
    MetricConvertor,
    MetricConvertorFilter,
    MetricAggregation,
    MetricAlert,
    MetricDefinition,
    MetricGraphic,
    MetricTrend,
    MetricTrendAggregation,
    MetricType,
    PhoneEscalationNotification,
    PhoneEscalationTailNotification,
    PhoneNotification,
    ServiceDutyNotification,
    SlackChat,
    SlackNotification,
    StartrekNotification,
    TelegramNotification,
)
from maps.infra.sedem.lib.config.schema.tests.shared import DEFAULT_CONFIG_CONTENT, extract_errors


def test_valid_minimal_alert() -> None:
    alert = Alert.parse_obj({})

    assert alert == Alert.construct(
        type=AlertType.SERVICE,
        simple_ping=False,

        balancer_ping=None,
        l7_health_check=False,

        check_options=None,
        description=None,
        pronounce=None,
        ignore_on_deploy=False,
        custom_tag=[],
        degrade_warn=None,
        degrade_crit=None,
        warn=None,
        crit=None,
        flaps=None,
        ttl=900,
        nodata_mode='force_ok',
        disaster_environments=[],
        notifications=[],
    )


def test_valid_featured_alert() -> None:
    alert = Alert.parse_obj({
        'check_options': {'args': ['check', 'smth'], 'env': {'SOME_VAR': '1'}},
        'description': 'my fancy alert',
        'pronounce': 'faaancy',
        'ignore_on_deploy': True,
        'custom_tag': ['some_tag'],
        'degrade_warn': '30%',
        'degrade_crit': '60%',
        'flaps': {'stable_time': 300, 'critical_time': 1500},
        'ttl': 60,
        'nodata_mode': 'force_crit',
        'disaster_environments': ['stable'],
    })

    assert alert == Alert.construct(
        type=AlertType.SERVICE,
        simple_ping=False,

        balancer_ping=None,
        l7_health_check=False,

        check_options=AlertCheckOptions(args=['check', 'smth'], env={'SOME_VAR': '1'}),
        description='my fancy alert',
        pronounce='faaancy',
        ignore_on_deploy=True,
        custom_tag=['some_tag'],
        degrade_warn='30%',
        degrade_crit='60%',
        warn=None,
        crit=None,
        flaps=AlertFlaps(stable_time=300, critical_time=1500),
        ttl=60,
        nodata_mode='force_crit',
        disaster_environments=['stable'],
        notifications=[],
    )


def test_valid_service_ping_alert() -> None:
    alert = Alert.parse_obj({
        'simple_ping': True,
    })

    assert alert.simple_ping is True


def test_invalid_service_ping_alert_with_bad_type() -> None:
    with pytest.raises(ValidationError) as exc:
        Alert.parse_obj({
            'type': 'external',
            'simple_ping': True,
        })

    assert extract_errors(exc) == [
        'field requires alert type "service"',
    ]


def test_valid_balancer_ping_alert() -> None:
    alert = Alert.parse_obj({
        'type': 'balancer',
        'balancer_ping': {},
    })

    assert alert.type == AlertType.BALANCER
    assert alert.balancer_ping == BalancerAlertPingSettings.construct(
        https=False,
        fqdn=None,
        vhost=None,
    )


def test_invalid_balancer_ping_alert_with_bad_type() -> None:
    with pytest.raises(ValidationError) as exc:
        Alert.parse_obj({
            'balancer_ping': {},
        })

    assert extract_errors(exc) == [
        'field requires alert type "balancer"',
    ]


def test_valid_l7_health_check_alert() -> None:
    alert = Alert.parse_obj({
        'type': 'balancer',
        'l7_health_check': True,
    })

    assert alert.type == AlertType.BALANCER
    assert alert.l7_health_check is True


def test_invalid_l7_health_check_alert_with_bad_type() -> None:
    with pytest.raises(ValidationError) as exc:
        Alert.parse_obj({
            'l7_health_check': True,
        })

    assert extract_errors(exc) == [
        'field requires alert type "balancer"',
    ]


def test_valid_external_alert() -> None:
    alert = Alert.parse_obj({
        'type': 'external',
    })

    assert alert.type == AlertType.EXTERNAL


def test_invalid_active_alert_with_check_options() -> None:
    with pytest.raises(ValidationError) as exc:
        Alert.parse_obj({
            'simple_ping': True,
            'check_options': {'args': ['check', 'smth'], 'env': {'SOME_VAR': '1'}},
        })

    assert extract_errors(exc) == [
        'field is not allowed for ping checks',
    ]


def test_valid_minimal_alerts_section() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'alerts': {
            'test-alert': {},
        },
    })

    assert config.alerts == {
        'test-alert': Alert(),
    }


def test_valid_metric_signal() -> None:
    metric_definition = MetricDefinition.parse_obj({
        'signal': 'M[subst:roquefort-{appid}_/{handle}_rps_ammv]',
    })

    assert metric_definition == MetricDefinition.construct(
        signal='M[subst:roquefort-{appid}_/{handle}_rps_ammv]',
    )


def test_invalid_metric_signal() -> None:
    with pytest.raises(ValidationError) as exc:
        MetricDefinition.parse_obj({
            'signal': 'M[плохой сигнал]',
        })

    assert extract_errors(exc) == [
        'bad signal: плохой сигнал',
    ]


def test_invalid_metric_definition() -> None:
    with pytest.raises(ValidationError) as exc:
        MetricDefinition.parse_obj({
            'func': ['perc_min', 'subst:{SRC}', 'subst:roquefort-{appid}_{prefix}{handle}_rps_ammv', 10],
            'signal': 'M[subst:roquefort-{appid}_/{handle}_rps_ammv]',
        })

    assert extract_errors(exc) == [
        'one and only one of "signal" or "func" must be defined',
    ]


def test_valid_minimal_metric_convertor() -> None:
    metric_convertor = MetricConvertor.parse_obj({})

    assert metric_convertor == MetricConvertor.construct(
        type=MetricType.SERVICE,
        alternative_name=None,
        search_template=[],
        body=[],
        alert=None,
        graphic=None,
        kwargs={},
    )


def test_valid_featured_metric_convertor() -> None:
    metric_convertor = MetricConvertor.parse_obj({
        'alternative_name': 'http-{appid}-{handle}-{code}',
        'search_template': [
            r'roquefort-([-.\w]+)_(/?)(.*)_(404|429|4xx|499|error)_ammv',
            'appid',
            'prefix',
            'handle',
            'code',
        ],
        'body': [{
            'func': ['perc_min', 'subst:{SRC}', 'subst:roquefort-{appid}_{prefix}{handle}_rps_ammv', '{rps_thresold}'],
        }],
        'alert': {
            'warn': 5,
            'crit': 5,
            'aggregation': {'type': 'max', 'window': 60},
            'trend': {'type': 'up', 'interval': 60, 'aggregation': {'type': 'quant', 'quant': 95}},
        },
        'graphic': {
            'handle': {
                'name': '{appid}: {handle}',
                'metric': '{appid}_{handle}',
                'timings': [100, 250, 500, 1000, 2000],
            },
            'handle_code': '{code}',
        },
        'kwargs': {
            'rps_thresold': 10,
        },
    })

    assert metric_convertor == MetricConvertor.construct(
        type=MetricType.SERVICE,
        alternative_name='http-{appid}-{handle}-{code}',
        search_template=[
            r'roquefort-([-.\w]+)_(/?)(.*)_(404|429|4xx|499|error)_ammv',
            'appid',
            'prefix',
            'handle',
            'code',
        ],
        body=[MetricDefinition(
            func=['perc_min', 'subst:{SRC}', 'subst:roquefort-{appid}_{prefix}{handle}_rps_ammv', '{rps_thresold}'],
        )],
        alert=MetricAlert(
            warn=5,
            crit=5,
            aggregation=MetricAggregation(
                type='max',
                window=60,
            ),
            trend=MetricTrend(
                type='up',
                interval=60,
                aggregation=MetricTrendAggregation(
                    type='quant',
                    quant=95,
                ),
            ),
        ),
        graphic=MetricGraphic(
            handle=HandleMetric(
                name='{appid}: {handle}',
                metric='{appid}_{handle}',
                timings=[100, 250, 500, 1000, 2000],
            ),
            handle_code='{code}',
        ),
        kwargs={'rps_thresold': '10'},
    )


def test_valid_minimal_metric_convertors_section() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'metrics_convertors': {
            'test-metric': {},
        },
    })

    assert config.metrics_convertors == {
        'test-metric': MetricConvertor(),
    }


def test_valid_minimal_filters_section() -> None:
    filters = FiltersSection.parse_obj({
        'alerts': [{
            'body': {
                'disable': True,
            },
        }],
        'metrics_convertors': [{
            'body': {
                'disable': True,
            },
        }],
    })

    assert filters == FiltersSection.construct(
        alerts=[AlertFilter(body=AlertFilter.FilterBody(disable=True))],
        metrics_convertors=[MetricConvertorFilter(body=MetricConvertorFilter.FilterBody(disable=True))],
    )


def test_valid_featured_filters_section() -> None:
    filters = FiltersSection.parse_obj({
        'alerts': [{
            'filter': {
                'staging': 'load',
            },
            'body': {
                '$force notifications': [],
            },
        }],
        'metrics_convertors': [{
            'filter': {
                'staging': 'load',
            },
            'body': {
                '$force notifications': [],
            },
        }],
    })

    assert filters == FiltersSection.construct(
        alerts=[AlertFilter(
            filter=FilterCondition(staging='load'),
            body=AlertFilter.FilterBody(notifications=[]),
        )],
        metrics_convertors=[MetricConvertorFilter(
            filter=FilterCondition(staging='load'),
            body=MetricConvertorFilter.FilterBody(notifications=[]),
        )],
    )


def test_valid_notification_profiles_section() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'notification_profiles': {
            'all_types': [{
                'type': 'telegram',
                'login': ['some-chat'],
                'status': ['WARN', 'CRIT'],
            }, {
                'type': 'slack',
                'chats': [{'name': 'some', 'link': 'https://yndx-all.slack.com/archives/ABCDEFGHI'}],
            }, {
                'type': 'phone',
                'login': ['john-doe'],
                'delay': 60,
            }, {
                'type': 'email',
                'login': ['some-mailing-list@yandex-team.ru'],
            }, {
                'type': 'startrek',
                'queue': 'SOME-QUEUE',
                'components': ['some-component'],
            }, {
                'type': 'phone_escalation',
                'logins': ['john-doe', 'jane-doe'],
                'repeat': 3,
                'delay': 60,
                'call_tries': 2,
                'on_success_next_call_delay': 600,
            }, {
                'type': 'service_duty',
                'abc_service': 'some-service',
            }, {
                'type': 'phone_escalation_tail',
                'logins': ['extra-login'],
            }],
        },
    })

    assert config.notification_profiles == {
        'all_types': [
            TelegramNotification.construct(
                type='telegram',
                login=['some-chat'],
                status=['WARN', 'CRIT'],
            ),
            SlackNotification.construct(
                type='slack',
                chats=[SlackChat.construct(name='some', link='https://yndx-all.slack.com/archives/ABCDEFGHI')],
            ),
            PhoneNotification.construct(
                type='phone',
                login=['john-doe'],
                delay=60,
            ),
            EmailNotification.construct(
                type='email',
                login=['some-mailing-list@yandex-team.ru'],
            ),
            StartrekNotification.construct(
                type='startrek',
                queue='SOME-QUEUE',
                components=['some-component'],
            ),
            PhoneEscalationNotification.construct(
                type='phone_escalation',
                logins=['john-doe', 'jane-doe'],
                repeat=3,
                delay=60,
                call_tries=2,
                on_success_next_call_delay=600,
            ),
            ServiceDutyNotification.construct(
                type='service_duty',
                abc_service='some-service',
            ),
            PhoneEscalationTailNotification.construct(
                type='phone_escalation_tail',
                logins=['extra-login'],
            ),
        ],
    }


def test_valid_signal_functions_section() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'signal_functions': {
            'perc': 'perc(M[{0}], max(1, M[{1}]))',
        },
    })

    assert config.signal_functions == {
        'perc': 'perc(M[{0}], max(1, M[{1}]))',
    }


def test_signal_functions_consistent() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'metrics_convertors': {
            'metric': {
                'body': [{'func': ['perc', 1, 2]}],
            },
        },
        'signal_functions': {
            'perc': 'perc(M[{0}], max(1,M[{1}]))',
        },
    })

    assert config.metrics_convertors['metric'].body == [MetricDefinition(
        func=['perc', 1, 2],
    )]


def test_signal_functions_inconsistent() -> None:
    with pytest.raises(ValidationError) as exc:
        ServiceConfig.parse_obj({
            **DEFAULT_CONFIG_CONTENT,
            'metrics_convertors': {
                'metric': {
                    'body': [{'func': ['perc', 1, 2]}],
                },
            },
        })

    assert extract_errors(exc) == [
        'unknown signal function "perc"',
    ]
