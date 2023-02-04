import pytest

from walle._tasks import sb_helpers
from walle.hosts import HostStatus, HostOperationState
from walle.stages import Stages
from walle.util.tasks import StageBuilder


@pytest.mark.parametrize(
    ["sb_helper", "name", "params"],
    [
        (sb_helpers.cancel_admin_requests, Stages.CANCEL_ADMIN_REQUESTS, None),
        (sb_helpers.set_downtime, Stages.SET_DOWNTIME, {"juggler_downtime_name": "default"}),
    ],
)
def test_not_parametrized_sb_helpers(sb_helper, name, params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helper(sb)
    result = sb.get_stages()[0]
    assert result.name == name and result.params == params


@pytest.mark.parametrize(
    ["checks_to_monitor", "monitoring_timeout", "result_params"],
    [
        ((), None, {"checks": []}),
        (("test",), None, {"checks": ["test"]}),
        (("test", "test2"), None, {"checks": ["test", "test2"]}),
        ((), 100, {'checks': [], "monitoring_timeout": 100}),
        (("test",), 100, {'checks': ["test"], "monitoring_timeout": 100}),
    ],
)
def test_monitor(checks_to_monitor, monitoring_timeout, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.monitor(sb, checks_to_monitor=checks_to_monitor, monitoring_timeout=monitoring_timeout)
    result = sb.get_stages()[0]
    assert result.name == Stages.MONITOR and result.params == result_params


@pytest.mark.parametrize(
    ["health_status_accuracy", "result_params"], [(None, None), (1, {"health_status_accuracy": 1})]
)
def test_reset_health_status(health_status_accuracy, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.reset_health_status(sb, health_status_accuracy=health_status_accuracy)
    result = sb.get_stages()[0]
    assert result.name == Stages.RESET_HEALTH_STATUS and result.params == result_params


@pytest.mark.parametrize(
    ["action", "reason", "workdays", "result_params"],
    [
        (None, None, None, None),
        ("nothing", None, None, {"action": "nothing"}),
        (None, "explanation", None, {"comment": "explanation"}),
        (None, None, True, {"workdays": True}),
        ("nothing", "explanation", True, {"action": "nothing", "comment": "explanation", "workdays": True}),
    ],
)
def test_acquire_permission(action, reason, workdays, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.acquire_permission(sb, action=action, reason=reason, workdays=workdays)
    result = sb.get_stages()[0]
    assert result.name == Stages.ACQUIRE_PERMISSION and result.params == result_params


@pytest.mark.parametrize(
    ["network", "vlans", "native_vlan", "result_params"],
    [
        (None, None, None, None),
        (
            "network_test",
            "vlans_test",
            "native_vlan_test",
            {"network": "network_test", "vlans": "vlans_test", "native_vlan": "native_vlan_test"},
        ),
    ],
)
def test_switch_vlans(network, vlans, native_vlan, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.switch_vlans(sb, network=network, vlans=vlans, native_vlan=native_vlan)
    result = sb.get_stages()[0]
    assert result.name == Stages.SWITCH_VLANS and result.params == result_params


@pytest.mark.parametrize(
    ["operation_type", "params", "result_params"],
    [(None, None, None), ("test", {"slot": 1}, {"operation": "test", "params": {"slot": 1}})],
)
def test_log_completed_operation(operation_type, params, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.log_completed_operation(sb, operation_type=operation_type, params=params)
    result = sb.get_stages()[0]
    assert result.name == Stages.LOG_COMPLETED_OPERATION and result.params == result_params


@pytest.mark.parametrize(
    ["terminators", "check_post_code", "result_params"],
    [
        ([], None, None),
        ({"test": "test"}, None, None),
        (None, True, {"check_post_code": True}),
        ({"test": "test"}, True, {"check_post_code": True}),
    ],
)
def test_ssh_reboot(terminators, check_post_code, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.ssh_reboot(sb, terminators=terminators, check_post_code=check_post_code)
    result = sb.get_stages()[0]
    assert result.name == Stages.SSH_REBOOT and result.params == result_params and result.terminators == terminators


@pytest.mark.parametrize(["soft", "result_params"], [(False, {"soft": False}), (True, {"soft": True})])
def test_power_off(soft, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.power_off(sb, soft=soft)
    result = sb.get_stages()[0]
    assert result.name == Stages.POWER_OFF and result.params == result_params


@pytest.mark.parametrize(
    ["check_post_code", "upgrade_to_profile", "pxe", "result_params"],
    [
        (False, False, False, {"check_post_code": False, "upgrade_to_profile": False, "pxe": False}),
        (True, True, True, {"check_post_code": True, "upgrade_to_profile": True, "pxe": True}),
    ],
)
def test_power_on(check_post_code, upgrade_to_profile, pxe, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.power_on(sb, check_post_code=check_post_code, upgrade_to_profile=upgrade_to_profile, pxe=pxe)
    result = sb.get_stages()[0]
    assert result.name == Stages.POWER_ON and result.params == result_params


@pytest.mark.parametrize(
    ["ticket_key", "timeout_time", "timeout_status", "operation_state", "reason", "result_params"],
    [
        (
            "ticket-key-mock",
            100,
            HostStatus.READY,
            HostOperationState.OPERATION,
            "reason-mock",
            {
                'ticket_key': "ticket-key-mock",
                'operation_state': 'operation',
                'reason': 'reason-mock',
                'timeout_status': 'ready',
                'timeout_time': 100,
            },
        ),
        (
            "ticket-key-mock",
            1000,
            HostStatus.DEAD,
            HostOperationState.DECOMMISSIONED,
            "reason-mock",
            {
                'ticket_key': "ticket-key-mock",
                'operation_state': 'decommissioned',
                'reason': 'reason-mock',
                'timeout_status': 'dead',
                'timeout_time': 1000,
            },
        ),
    ],
)
def test_set_maintenance(ticket_key, timeout_time, timeout_status, operation_state, reason, result_params):
    sb = StageBuilder()
    assert sb.get_stages() == []

    sb_helpers.set_maintenance(sb, ticket_key, timeout_time, timeout_status, operation_state, reason=reason)
    result = sb.get_stages()[0]
    assert result.name == Stages.SET_MAINTENANCE and result.params == result_params
