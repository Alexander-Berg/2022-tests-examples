import pytest

from infra.awacs.proto import model_pb2
from awacs.model import alerting


@pytest.fixture
def alerting_prefix():
    return 'test_awacs'


@pytest.mark.parametrize('config_version', alerting.get_versions())
def test_get_config(config_version):
    alerting_config = alerting.get_config(config_version)
    assert alerting_config


def test_build_yasm_alert_prefix(alerting_prefix):
    name_1 = 'namespace_1'
    name_2 = 'namespace_2'
    result_1 = alerting.AlertingConfig.build_yasm_alert_prefix(alerting_prefix, name_1)
    result_2 = alerting.AlertingConfig.build_yasm_alert_prefix(alerting_prefix, name_2)
    assert result_1 != result_2


def test_get_juggler_raw_notify_rules():
    alerting_setting = model_pb2.NamespaceSpec.AlertingSettings()

    result = alerting.AlertingConfig.get_juggler_raw_notify_rules(alerting_setting)
    assert not result

    raw_notify_rule_1 = alerting_setting.juggler_raw_notify_rules.balancer.add(
    )  # type: model_pb2.NamespaceSpec.AlertingSettings.JugglerRawNotifyRule
    raw_notify_rule_1.template_name = 'test'
    raw_notify_rule_1.template_kwargs = """
        status:
            - from: OK
              to: CRIT
        login:
            - i-dyachkov
        method:
            - sms
    """

    raw_notify_rule_2 = alerting_setting.juggler_raw_notify_rules.platform.add()
    raw_notify_rule_2.template_name = 'test'
    raw_notify_rule_2.template_kwargs = """
        status:
            - from: OK
              to: CRIT
        login:
            - i-dyachkov
        method:
            - telegram
        """
    result = alerting.AlertingConfig.get_juggler_raw_notify_rules(alerting_setting)
    assert list(result) == [
        (alerting.BALANCER_GROUP, raw_notify_rule_1),
        (alerting.PLATFORM_GROUP, raw_notify_rule_2)
    ]


def test_get_juggler_raw_downtimers():
    alerting_setting = model_pb2.NamespaceSpec.AlertingSettings()
    result = alerting.AlertingConfig.get_juggler_raw_downtimers(alerting_setting)
    assert not result.staff_group_ids and not result.staff_logins

    alerting_setting.juggler_raw_downtimers.staff_logins.extend(['1', '2', '2'])
    alerting_setting.juggler_raw_downtimers.staff_group_ids.extend([1, 2, 2, 2])

    result = alerting.AlertingConfig.get_juggler_raw_downtimers(alerting_setting)
    assert alerting_setting.juggler_raw_downtimers.staff_logins == ['1', '2']
    assert alerting_setting.juggler_raw_downtimers.staff_group_ids == [1, 2]
    assert alerting_setting.juggler_raw_downtimers == result


def test_gen_balancer_yasm_alerts(alerting_prefix):
    current_config = alerting.get_config(alerting.CURRENT_VERSION)
    balancer_id = 'testing'
    namespace_id = 'namespace_1'
    namespace_slug = current_config.build_yasm_alert_prefix(alerting_prefix, namespace_id)
    juggler_namespace = current_config.build_juggler_namespace(alerting_prefix, namespace_id)
    yasm_alert_prefix = current_config.build_yasm_alert_prefix(alerting_prefix, namespace_slug)
    balancer_pb = model_pb2.Balancer()
    result = current_config.gen_balancer_yasm_alerts(
        alerting_prefix='test_awacs',
        yasm_alert_prefix=yasm_alert_prefix,
        juggler_namespace=juggler_namespace,
        juggler_check_tags=[
            current_config.build_juggler_check_namespace_tag(alerting_prefix, namespace_id),
            current_config.build_juggler_check_balancer_tag(alerting_prefix, balancer_id)
        ],
        itype='prod',
        ctype='balancer',
        prj=namespace_id,
        location='sas',
        balancer_ui_url='any',
        balancer_pb=balancer_pb,
        namespace_id=namespace_id,
        abc_service_slug='test',
        yasm_alert_suffix='',
    )
    assert result
