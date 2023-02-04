"""Tests changing of a corrupted disk."""

from unittest import mock

import pytest

import walle.admin_requests.request as admin_requests
from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    monkeypatch_function,
    mock_commit_stage_changes,
    mock_decision,
    mock_schedule_disk_change,
    mock_host_deactivation,
    mock_schedule_host_redeployment,
    mock_schedule_host_reboot,
    mock_startrek_client,
    mock_complete_current_stage,
    mock_complete_parent_stage,
    check_stage_initialization,
    monkeypatch_check_rule,
    monkeypatch_automation_plot_id,
    monkeypatch_clients_for_host,
)
from walle.expert.automation_plot import AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.expert.failure_types import FailureType
from walle.expert.rules import CheckDisk
from walle.expert.types import WalleAction, CheckType
from walle.fsm_stages import hw_errors
from walle.fsm_stages.change_disk import restart_task_with_host_power_off
from walle.fsm_stages.common import get_current_stage, ADMIN_REQUEST_CHECK_INTERVAL, switch_to_stage
from walle.fsm_stages.constants import StageStatus
from walle.hosts import HostState, TaskType
from walle.operations_log.constants import Operation
from walle.stages import Stages, Stage
from walle.util.misc import drop_none
from walle.clients import dmc
from walle.expert.decision import Decision
from walle.expert import types


@pytest.fixture()
def test(request, mp, monkeypatch_timestamp, mp_juggler_source):
    monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.CHANGE_DISK), status=StageStatus.HW_ERRORS_PENDING)


def _monkeypatch_check_rule(mp, decision):
    return monkeypatch_check_rule(mp, decision, CheckType.DISK, CheckDisk)


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_pending_with_decision_changed_to_dead(test, mp):
    _monkeypatch_check_rule(mp, mock_decision(WalleAction.DEACTIVATE))
    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, mock_decision(WalleAction.DEACTIVATE)),
    )

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(stage=Stages.CHANGE_DISK, stage_status=StageStatus.HW_ERRORS_PENDING),
        )
    )

    handle_host(host)

    mock_host_deactivation(host, reason="Reason mock.")
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_pending_redeploy_request(test, mp):
    decision = mock_decision(WalleAction.REDEPLOY, {"slot": 666})
    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, decision),
    )

    _monkeypatch_check_rule(mp, decision)
    mock_create_admin_request = mp.function(admin_requests.create_admin_request, return_value=(666, "MOCK-1"))
    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_params={"slot": decision.params["slot"]},
                stage_status=StageStatus.HW_ERRORS_PENDING,
                parent_next_stage_name=Stages.LOG_COMPLETED_OPERATION,
            ),
        )
    )
    monkeypatch_clients_for_host(mp, host)
    handle_host(host)
    assert mock_create_admin_request.call_count == 0
    mock_schedule_host_redeployment(
        host,
        manual=False,
        reason="Reason mock.",
        task_type=TaskType.AUTOMATED_HEALING,
    )
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_pending_reboot_request(test, mp):
    decision = mock_decision(WalleAction.REBOOT, {"slot": 666})

    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, decision),
    )
    _monkeypatch_check_rule(mp, decision)
    mock_create_admin_request = mp.function(admin_requests.create_admin_request, return_value=(666, "MOCK-1"))
    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_params={"slot": decision.params["slot"]},
                stage_status=StageStatus.HW_ERRORS_PENDING,
                parent_next_stage_name=Stages.LOG_COMPLETED_OPERATION,
            ),
        )
    )
    monkeypatch_clients_for_host(mp, host)
    handle_host(host)
    assert mock_create_admin_request.call_count == 0
    mock_schedule_host_reboot(host, manual=False, reason="Reason mock.", oplog_params={"slot": 666})
    test.hosts.assert_equal()


def _set_log_operation_stage_uid(host):
    log_operation_stage = host.task.stages[-1]
    assert log_operation_stage.name == Stages.LOG_COMPLETED_OPERATION

    stage = get_current_stage(host)
    stage.set_param("log_operation_stage_uid", log_operation_stage.uid)
    host.save()

    return log_operation_stage


@pytest.mark.parametrize("redeploy", (True, False))
@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_pending_create_request(test, mp, redeploy):
    decision = mock_decision(WalleAction.CHANGE_DISK)
    assert decision.params["redeploy"]

    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, decision),
    )

    _monkeypatch_check_rule(mp, decision)
    mock_create_admin_request = mp.function(admin_requests.create_admin_request, return_value=(666, "MOCK-1"))

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_params={"redeploy": redeploy},
                stage_status=StageStatus.HW_ERRORS_PENDING,
                parent_next_stage_name=Stages.LOG_COMPLETED_OPERATION,
            ),
        )
    )

    stage = get_current_stage(host)
    log_operation_stage = _set_log_operation_stage_uid(host)

    monkeypatch_clients_for_host(mp, host)
    handle_host(host)

    if redeploy:
        mock_create_admin_request.assert_called_once_with(
            host, admin_requests.RequestTypes.CORRUPTED_DISK_BY_SLOT, decision.reason, **decision.params
        )

        log_operation_stage.set_param("operation", Operation.CHANGE_DISK.type)
        log_operation_stage.set_param("params", {"slot": decision.params["slot"]})

        stage.set_temp_data(hw_errors.REQUEST_ID_STAGE_FIELD_NAME, 666)
        stage.set_temp_data(hw_errors.TICKET_ID_STAGE_FIELD_NAME, "MOCK-1")
        stage.set_temp_data("redeploy", redeploy)
        stage.set_temp_data("decision_params", decision.params)
        stage.set_temp_data("decision_reason", decision.reason)
        mock_commit_stage_changes(host, status=StageStatus.HW_ERRORS_WAITING_DC, check_now=True)
    else:
        assert mock_create_admin_request.call_count == 0
        mock_schedule_disk_change(host, decision)

    test.hosts.assert_equal()


@pytest.mark.parametrize("eine_code", [None, [], "EINE_CODE_MOCK", ["EINE_CODE_MOCK"], ["EC_MOCK1", "EC_MOCK2"]])
@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_create_request_passes_eine_code(test, mp, eine_code):
    decision = mock_decision(WalleAction.CHANGE_DISK)
    decision.params["eine_code"] = eine_code or None

    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, decision),
    )

    _monkeypatch_check_rule(mp, decision)
    mock_create_admin_request = mp.function(admin_requests.create_admin_request, return_value=(666, "MOCK-1"))

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_params={"redeploy": decision.params["redeploy"]},
                stage_status=StageStatus.HW_ERRORS_PENDING,
                parent_next_stage_name=Stages.LOG_COMPLETED_OPERATION,
            ),
        )
    )

    _set_log_operation_stage_uid(host)

    monkeypatch_clients_for_host(mp, host)
    handle_host(host)

    mock_create_admin_request.assert_called_once_with(
        host,
        admin_requests.RequestTypes.CORRUPTED_DISK_BY_SLOT,
        decision.reason,
        eine_code=decision.params.pop("eine_code"),
        **decision.params
    )


@pytest.mark.parametrize("redeploy", (True, False))
def test_waiting_resolve_ticket(test, mp, redeploy):
    request_id = 666
    ticket_id = "MOCK-1"
    st_client = mock_startrek_client(mp)
    st_client.get_issue.return_value = {"resolution": "ok"}

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_status=StageStatus.HW_ERRORS_WAITING_RESOLVE_TICKET,
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                    hw_errors.TICKET_ID_STAGE_FIELD_NAME: ticket_id,
                    "redeploy": redeploy,
                    "decision_params": {},
                    "decision_reason": "reason-mock",
                },
                parent_stage_name=Stages.HEAL_DISK,
            ),
        )
    )
    handle_host(host)

    st_client.get_issue.assert_called_once_with(ticket_id)

    if redeploy:
        mock_complete_current_stage(host)
    else:
        # Skip redeploy stages when not needed
        mock_complete_parent_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize("redeploy", (True, False))
def test_waiting_request(test, mp, redeploy):
    request_id = 666
    get_request_status = mp.function(
        admin_requests.get_request_status,
        return_value={hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id, "status": admin_requests.STATUS_IN_PROCESS},
    )

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                    "redeploy": redeploy,
                    "decision_params": {},
                    "decision_reason": "reason-mock",
                },
                parent_stage_name=Stages.HEAL_DISK,
            ),
        )
    )
    handle_host(host)

    mock_commit_stage_changes(
        host,
        check_after=ADMIN_REQUEST_CHECK_INTERVAL,
        status_message="Waiting for #{} BOT admin request to complete.".format(request_id),
    )

    get_request_status.assert_called_once_with(request_id)
    test.hosts.assert_equal()


def test_waiting_save_ticket(test, mp):
    request_id = 666
    ticket_key = "BURNE-0001"

    get_request_status = mp.function(
        admin_requests.get_request_status,
        return_value={
            hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
            "status": admin_requests.STATUS_IN_PROCESS,
            "ticket": ticket_key,
        },
    )

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                    "decision_params": {},
                    "decision_reason": "reason-mock",
                },
                parent_stage_name=Stages.HEAL_DISK,
            ),
        )
    )
    handle_host(host)

    get_current_stage(host).set_data("tickets", [ticket_key])
    host.ticket = ticket_key

    mock_commit_stage_changes(
        host,
        inc_revision=1,
        check_after=ADMIN_REQUEST_CHECK_INTERVAL,
        status_message="Waiting for #{} BOT admin request to complete.".format(request_id),
    )

    get_request_status.assert_called_once_with(request_id)
    test.hosts.assert_equal()


def test_request_deleted_retry(test, mp):
    decision = mock_decision(WalleAction.CHANGE_DISK)
    assert decision.params["redeploy"]
    request_id = 666
    ticket_id = "MOCK-2"

    get_request_status = mp.function(
        admin_requests.get_request_status,
        return_value={
            hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
            "status": admin_requests.STATUS_DELETED,
            "info": "ERROR-MOCK",
        },
    )
    mock_create_admin_request = monkeypatch_function(
        mp, admin_requests.create_admin_request, return_value=(request_id + 1, ticket_id)
    )

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                stage_temp_data=drop_none(
                    {
                        hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                        hw_errors.TICKET_ID_STAGE_FIELD_NAME: "MOCK-1",
                        "redeploy": decision.params["redeploy"],
                        "decision_params": decision.params,
                        "decision_reason": decision.reason,
                    }
                ),
                parent_stage_name=Stages.HEAL_DISK,
                parent_next_stage_name=Stages.LOG_COMPLETED_OPERATION,
            ),
        )
    )

    current_stage = get_current_stage(host)
    log_operation_stage = _set_log_operation_stage_uid(host)

    log_operation_stage.set_param("operation", Operation.CHANGE_DISK.type)
    log_operation_stage.set_param("params", {"slot": decision.params["slot"]})

    handle_host(host)

    get_request_status.assert_called_once_with(request_id)

    current_stage.set_temp_data(hw_errors.REQUEST_ID_STAGE_FIELD_NAME, request_id + 1)
    current_stage.set_temp_data(hw_errors.TICKET_ID_STAGE_FIELD_NAME, ticket_id)
    mock_commit_stage_changes(host, check_now=True, status=StageStatus.HW_ERRORS_WAITING_DC)

    mock_create_admin_request.assert_called_once_with(
        host, admin_requests.RequestTypes.CORRUPTED_DISK_BY_SLOT, decision.reason, **decision.params
    )

    test.hosts.assert_equal()


def test_shutdown_while_waiting_itdc(test):
    decision = mock_decision(WalleAction.CHANGE_DISK, redeploy=False, failure_type=FailureType.DISK_BAD_BLOCKS.name)
    request_id = 666
    ticket_id = 911

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.CHANGE_DISK.host_status,
            task=mock_task(
                stage=Stages.CHANGE_DISK,
                stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                stage_data={"orig_decision": decision.to_dict()},
                stage_temp_data=drop_none(
                    {
                        hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                        hw_errors.TICKET_ID_STAGE_FIELD_NAME: ticket_id,
                        "redeploy": decision.params["redeploy"],
                        "decision_params": decision.params,
                        "decision_reason": decision.reason,
                    }
                ),
                parent_stage_name=Stages.HEAL_DISK,
                parent_next_stage_name=Stages.LOG_COMPLETED_OPERATION,
            ),
        )
    )
    scenario = mock.Mock()
    scenario.id = 911

    assert not any(s.name == Stages.POWER_OFF for s in host.task.stages)
    restart_task_with_host_power_off(host, scenario)

    assert host.task.stages[2].name == Stages.POWER_OFF
    heal_disk_stage = host.task.stages[3]
    assert heal_disk_stage.name == Stages.HEAL_DISK
    change_disk_stage = heal_disk_stage.stages[0]
    assert change_disk_stage.name == Stages.CHANGE_DISK

    switch_to_stage(host, heal_disk_stage)
    handle_host(host)

    host.reload()
    current_stage = get_current_stage(host)
    assert current_stage.status == StageStatus.HW_ERRORS_WAITING_DC
    assert current_stage.get_temp_data(hw_errors.TICKET_ID_STAGE_FIELD_NAME) == ticket_id
    assert current_stage.get_temp_data(hw_errors.REQUEST_ID_STAGE_FIELD_NAME) == request_id
