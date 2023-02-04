from walle._tasks.task_args import RebootTaskArgs, PAYLOAD
from walle.audit_log import LogEntry
from walle.expert.decision import Decision
from walle.operations_log.constants import Operation


def test_get_task_params():
    test_values = {
        "issuer": "test_issuer",
        "task_type": "test_type",
        "operation_type": Operation.REBOOT.type,
        "operation_host_status": Operation.REBOOT.host_status,
        "cms_action": "some_cms_action",
        "type": "some_audit_log_type",
        "project": "test_project",
        "scenario_id": 1,
        "host_inv": 2,
        "host_name": "test_fqdn",
        "host_uuid": "test_uuid",
        "operation_restrictions": "restrictions",
        "checks_to_monitor": "checks",
        "reason": "test reason",
        "ignore_cms": True,
        "with_auto_healing": True,
        "monitor_on_completion": True,
        "ignore_maintenance": True,
        "disable_admin_requests": True,
        "from_current_task": True,
        "network": "some_network_value",
        "ssh": "FORBID",
        "check_post_code": True,
        "check_post_code_reason": "I wanted so",
        "decision": Decision.failure("some"),
    }
    task_args = RebootTaskArgs(**test_values)
    task_params = task_args.get_task_params()

    assert len(task_params) == len(LogEntry.task_fields) + 1  # count payload field

    for key, value in test_values.items():
        if isinstance(value, Decision):
            value = value.to_dict()
        if key in LogEntry.task_fields:
            assert task_params[key] == value
        else:
            assert task_params[PAYLOAD][key] == value
