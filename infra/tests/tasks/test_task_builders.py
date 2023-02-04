"""Test task stage construction helpers."""
from walle._tasks.task_args import SwitchToMaintenanceTaskArgs
from walle._tasks.task_creator import get_switch_to_maintenance_stages
from walle.clients.juggler import JugglerDowntimeName
from walle.hosts import TaskType, HostOperationState, HostStatus
from walle.stages import Stages
from walle.util.tasks import StageBuilder


def test_set_maintenance_minimal_args():
    task_args = _task_args()

    sb = StageBuilder()
    sb.stage(Stages.ACQUIRE_PERMISSION, action="mock-action", comment="explanation")
    sb.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)
    sb.stage(
        Stages.SET_MAINTENANCE,
        ticket_key="MOCK-1001",
        timeout_status=HostStatus.READY,
        operation_state=HostOperationState.OPERATION,
        reason="explanation",
    )

    assert _stages(sb) == _stages(get_switch_to_maintenance_stages(task_args))


def test_set_maintenance_with_workdays():
    task_args = _task_args(workdays_only=True)

    sb = StageBuilder()
    sb.stage(Stages.ACQUIRE_PERMISSION, action="mock-action", comment="explanation", workdays=True)
    sb.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)
    sb.stage(
        Stages.SET_MAINTENANCE,
        ticket_key="MOCK-1001",
        timeout_status=HostStatus.READY,
        operation_state=HostOperationState.OPERATION,
        reason="explanation",
    )

    assert _stages(sb) == _stages(get_switch_to_maintenance_stages(task_args))


def test_set_maintenance_with_decommission():
    task_args = _task_args(operation_state=HostOperationState.DECOMMISSIONED)

    sb = StageBuilder()
    sb.stage(Stages.ACQUIRE_PERMISSION, action="mock-action", comment="explanation")
    sb.stage(Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)
    sb.stage(
        Stages.SET_MAINTENANCE,
        ticket_key="MOCK-1001",
        timeout_status=HostStatus.READY,
        operation_state=HostOperationState.DECOMMISSIONED,
        reason="explanation",
    )

    assert _stages(sb) == _stages(get_switch_to_maintenance_stages(task_args))


def _task_args(**kwargs):
    # hell, this is bollocks
    kwargs = dict(
        issuer="mock-issuer",
        task_type=TaskType.MANUAL,
        project="mock-project",
        host_inv=100001,
        host_name="mock-hostname",
        host_uuid="mock-host-uuid",
        ignore_cms=False,
        disable_admin_requests=False,
        power_off=False,
        ticket_key="MOCK-1001",
        cms_action="mock-action",
        reason="explanation",
        scenario_id=None,
        **kwargs
    )
    return SwitchToMaintenanceTaskArgs(**kwargs)


def _stages(sb):
    return [stage.to_mongo() for stage in sb.get_stages()]
