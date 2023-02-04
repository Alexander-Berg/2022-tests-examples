"""Tests hardware repairing stage handler."""

import pytest

import walle.admin_requests.request as admin_requests
from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    monkeypatch_function,
    mock_commit_stage_changes,
    mock_decision,
    mock_complete_current_stage,
    check_stage_initialization,
    monkeypatch_check_rule,
    monkeypatch_automation_plot_id,
    mock_startrek_client,
)
from walle.clients.startrek import StartrekClientRequestError
from walle.expert.automation_plot import AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.expert.failure_types import FailureType
from walle.expert.rules import CheckLink
from walle.expert.types import WalleAction, CheckType
from walle.fsm_stages import hw_errors
from walle.fsm_stages.common import get_current_stage, ADMIN_REQUEST_CHECK_INTERVAL
from walle.fsm_stages.constants import StageStatus
from walle.hosts import HostState
from walle.operations_log.constants import Operation
from walle.stages import Stages, Stage
from walle.util.misc import drop_none
from walle.clients import dmc


@pytest.fixture()
def test(request, mp, monkeypatch_timestamp):
    monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.CHANGE_DISK), status=StageStatus.HW_ERRORS_PENDING)


def _monkeypatch_check_rule(mp, decision):
    return monkeypatch_check_rule(mp, decision, CheckType.LINK, CheckLink)


def test_pending_create_request(test, mp):
    decision = mock_decision(
        WalleAction.REBOOT,
        params={
            "request_type": admin_requests.RequestTypes.MALFUNCTIONING_LINK.type,
        },
        failure_type=FailureType.LINK_MALFUNCTION,
    )
    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, decision),
    )
    _monkeypatch_check_rule(mp, decision)

    mock_create_admin_request = monkeypatch_function(
        mp, admin_requests.create_admin_request, return_value=(666, "MOCK-1")
    )

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_PENDING,
                stage_params={
                    "decision_params": decision.params,
                    "decision_reason": decision.reason,
                },
            ),
        )
    )

    handle_host(host)

    mock_create_admin_request.assert_called_once_with(
        host, admin_requests.RequestTypes.MALFUNCTIONING_LINK, decision.reason
    )

    stage = get_current_stage(host)
    stage.set_temp_data(hw_errors.REQUEST_ID_STAGE_FIELD_NAME, 666)
    stage.set_temp_data(hw_errors.TICKET_ID_STAGE_FIELD_NAME, "MOCK-1")
    stage.set_temp_data("decision_params", decision.params)
    stage.set_temp_data("decision_reason", decision.reason)
    mock_commit_stage_changes(host, status=StageStatus.HW_ERRORS_WAITING_DC, check_now=True)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    ["request_type", "failure_type"],
    [
        (admin_requests.RequestTypes.INFINIBAND_INVALID_STATE, FailureType.INFINIBAND_INVALID_STATE),
    ],
)
def test_pending_create_noc_request(test, mp, request_type, failure_type):
    ticket_id = "NOCREQESTS-0001"
    decision = mock_decision(
        WalleAction.REPAIR_HARDWARE, params={"request_type": request_type.type}, failure_type=failure_type
    )
    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, decision),
    )
    _monkeypatch_check_rule(mp, decision)
    mock_create_noc_request = monkeypatch_function(mp, admin_requests.create_noc_request, return_value=(ticket_id))
    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_PENDING,
                stage_params={
                    "decision_params": decision.params,
                    "decision_reason": decision.reason,
                },
            ),
        )
    )

    handle_host(host)

    mock_create_noc_request.assert_called_once_with(host, request_type, decision.reason)

    host.ticket = ticket_id
    stage = get_current_stage(host)
    stage.set_data("tickets", {ticket_id})
    mock_commit_stage_changes(host, extra_fields=["ticket"])
    stage.set_temp_data(hw_errors.TICKET_ID_STAGE_FIELD_NAME, ticket_id)
    stage.set_temp_data("decision_params", decision.params)
    stage.set_temp_data("decision_reason", decision.reason)
    mock_commit_stage_changes(host, status=StageStatus.HW_ERRORS_WAITING_RESOLVE_TICKET, check_now=True)

    test.hosts.assert_equal()


def test_waiting_ok_request(test, mp):
    request_id = 666
    get_request_status = mp.function(
        admin_requests.get_request_status,
        return_value={hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id, "status": admin_requests.STATUS_PROCESSED},
    )

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                    hw_errors.TICKET_ID_STAGE_FIELD_NAME: "MOCK-1",
                },
            ),
        )
    )

    handle_host(host)

    get_request_status.assert_called_once_with(request_id)

    mock_commit_stage_changes(host, status=StageStatus.HW_ERRORS_WAITING_RESOLVE_TICKET, check_now=True)
    test.hosts.assert_equal()


def test_waiting_request(test, mp):
    request_id = 666
    get_request_status = mp.function(
        admin_requests.get_request_status,
        return_value={hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id, "status": admin_requests.STATUS_IN_PROCESS},
    )

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                stage_temp_data={hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id},
            ),
        )
    )

    handle_host(host)

    get_request_status.assert_called_once_with(request_id)

    mock_commit_stage_changes(
        host,
        check_after=ADMIN_REQUEST_CHECK_INTERVAL,
        status_message="Waiting for #{} BOT admin request to complete.".format(request_id),
    )
    test.hosts.assert_equal()


def test_waiting_resolve_ticket(test, mp):
    ticket_id = "MOCK-1"
    st_client = mock_startrek_client(mp)

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_RESOLVE_TICKET,
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: 666,
                    hw_errors.TICKET_ID_STAGE_FIELD_NAME: ticket_id,
                },
            ),
        )
    )
    handle_host(host)

    st_client.get_issue.assert_called_once_with(ticket_id)

    mock_commit_stage_changes(
        host,
        check_after=hw_errors.TICKET_RESOLVE_CHECK_INTERVAL,
        status_message="Waiting for Startrek ticket {} to resolve.".format(ticket_id),
    )
    test.hosts.assert_equal()


def test_ticket_resolved(test, mp):
    ticket_id = "MOCK-1"
    st_client = mock_startrek_client(mp)
    st_client.get_issue.return_value = {"resolution": "ok"}

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_RESOLVE_TICKET,
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: 666,
                    hw_errors.TICKET_ID_STAGE_FIELD_NAME: ticket_id,
                },
            ),
        )
    )
    handle_host(host)

    st_client.get_issue.assert_called_once_with(ticket_id)

    mock_complete_current_stage(host)
    test.hosts.assert_equal()


@pytest.mark.parametrize(hw_errors.TICKET_ID_STAGE_FIELD_NAME, (None, ""))
def test_skip_resolve_ticket(test, mp, ticket_id):
    st_client = mock_startrek_client(mp)

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_RESOLVE_TICKET,
                stage_temp_data=drop_none(
                    {
                        hw_errors.REQUEST_ID_STAGE_FIELD_NAME: 666,
                        hw_errors.TICKET_ID_STAGE_FIELD_NAME: ticket_id,
                    }
                ),
            ),
        )
    )
    handle_host(host)

    st_client.get_issue.assert_not_called()

    mock_complete_current_stage(host)
    test.hosts.assert_equal()


def test_ticket_resolve_with_st_404(test, mp):
    class MockResponse:
        def __init__(self):
            self.status_code = 404

    ticket_id = "MOCK-1"
    st_client = mock_startrek_client(mp)
    st_client.get_issue.side_effect = StartrekClientRequestError(MockResponse(), "some st error")

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_RESOLVE_TICKET,
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: 666,
                    hw_errors.TICKET_ID_STAGE_FIELD_NAME: ticket_id,
                },
            ),
        )
    )
    handle_host(host)

    st_client.get_issue.assert_called_once_with(ticket_id)

    mock_complete_current_stage(host)
    test.hosts.assert_equal()


def test_ticket_resolve_with_st_error(test, mp):
    ticket_id = "MOCK-1"
    st_client = mock_startrek_client(mp)
    st_client.get_issue.side_effect = RuntimeError("some runtime error")

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_RESOLVE_TICKET,
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: 666,
                    hw_errors.TICKET_ID_STAGE_FIELD_NAME: ticket_id,
                },
            ),
        )
    )
    handle_host(host)

    st_client.get_issue.assert_called_once_with(ticket_id)

    error_message = "Failed to get status for Startrek ticket MOCK-1: some runtime error"
    mock_commit_stage_changes(host, check_after=hw_errors.TICKET_RESOLVE_CHECK_INTERVAL, error=error_message)
    test.hosts.assert_equal()


def test_waiting_save_ticket(test, mp):
    request_id = 666
    ticket_key = "BURNE-10001"
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
            status=Operation.REPAIR_HARDWARE.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                stage_temp_data={hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id},
            ),
        )
    )

    handle_host(host)

    host.ticket = ticket_key
    get_current_stage(host).set_data("tickets", [ticket_key])
    mock_commit_stage_changes(
        host,
        inc_revision=1,
        check_after=ADMIN_REQUEST_CHECK_INTERVAL,
        status_message="Waiting for #{} BOT admin request to complete.".format(request_id),
    )

    get_request_status.assert_called_once_with(request_id)
    test.hosts.assert_equal()


def test_request_disappears(test, mp):
    request_id = 666
    decision = mock_decision(
        WalleAction.REBOOT,
        params={
            "reboot": True,
            "request_type": admin_requests.RequestTypes.MALFUNCTIONING_LINK.type,
        },
        failure_type=FailureType.LINK_RX_CRC_ERRORS,
    )
    mock_create_admin_request = mp.function(
        admin_requests.create_admin_request, return_value=(request_id + 1, "MOCK-2")
    )
    get_request_status = mp.function(
        admin_requests.get_request_status,
        return_value={
            hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
            "status": admin_requests.STATUS_DELETED,
            "info": "ERROR-MOCK",
        },
    )

    host = test.mock_host(
        dict(
            name="mock-host-name",
            state=HostState.ASSIGNED,
            status=Operation.REPAIR_LINK.host_status,
            task=mock_task(
                stage=Stages.HW_REPAIR,
                stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                stage_params={
                    "decision_params": decision.params,
                    "decision_reason": decision.reason,
                },
                stage_temp_data={
                    hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                    hw_errors.TICKET_ID_STAGE_FIELD_NAME: "MOCK-1",
                },
            ),
        )
    )

    handle_host(host)

    stage = get_current_stage(host)
    stage.set_temp_data(hw_errors.REQUEST_ID_STAGE_FIELD_NAME, request_id + 1)
    stage.set_temp_data(hw_errors.TICKET_ID_STAGE_FIELD_NAME, "MOCK-2")
    mock_commit_stage_changes(host, status=StageStatus.HW_ERRORS_WAITING_DC, check_now=True)

    get_request_status.assert_called_once_with(request_id)
    mock_create_admin_request.assert_called_once_with(
        host, admin_requests.RequestTypes.MALFUNCTIONING_LINK, decision.reason
    )
    test.hosts.assert_equal()
