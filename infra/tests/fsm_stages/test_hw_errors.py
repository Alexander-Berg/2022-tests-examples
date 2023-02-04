"""Tests common logic for hardware error fixing."""

import pytest

import walle.admin_requests.request as admin_requests
import walle.expert.rules
import walle.util.host_health
from infra.walle.server.tests.lib.rules_util import limit_breached, limit_not_breached
from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_status_reasons,
    mock_commit_stage_changes,
    mock_decision,
    monkeypatch_check_rule,
    monkeypatch_automation_plot_id,
    mock_task_cancellation,
    mock_schedule_host_reboot,
    monkeypatch_function,
)
from sepelib.core.constants import HOUR_SECONDS
from walle.clients import bot
from walle.expert import juggler
from walle.expert.automation_plot import AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.expert.types import WalleAction, CheckType
from walle.fsm_stages import change_disk, hw_errors
from walle.fsm_stages.common import (
    ADMIN_REQUEST_CHECK_INTERVAL,
    MONITORING_PERIOD,
    get_current_stage,
    commit_stage_changes,
)
from walle.fsm_stages.constants import StageStatus
from walle.hosts import HostState
from walle.models import timestamp
from walle.operations_log import operations as operations_log
from walle.operations_log.constants import Operation
from walle.stages import Stages
from walle.util.misc import drop_none
from walle.clients import dmc


@pytest.fixture()
def test(request, mp, monkeypatch_timestamp, monkeypatch_check_percentage):
    monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)
    return TestCase.create(request)


class TestHandling:
    TICKET_KEY = "ITDC-00001"

    BOT_RESPONSE_FAULT = (
        """{
        "Status": "FAULT",
        "Initiator": "robot-walle",
        "TimeCreate": "10",
        "StNum": "%s"
    }"""
        % TICKET_KEY
    )

    BOT_RESPONSE_DELETED = (
        """{
        "Status": "DELETED",
        "Initiator": "robot-walle",
        "TimeCreate": "10",
        "StNum": "%s"
    }"""
        % TICKET_KEY
    )

    @pytest.fixture(
        autouse=True,
        params=(
            (
                Operation.CHANGE_DISK.host_status,
                Stages.CHANGE_DISK,
                CheckType.DISK,
                "disk change",
                change_disk.ChangeDiskStageHandler.handle_create,
                change_disk.ChangeDiskStageHandler,
                walle.expert.rules.CheckDisk,
            ),
        ),
    )
    def init(self, request):
        (
            self.status,
            self.stage_name,
            self.check_type,
            self.operation,
            self.retry_handler,
            self.retry_handler_obj,
            self.check_rule,
        ) = request.param

    @pytest.mark.parametrize("timed_out", (True, False))
    def test_pending_with_missing_health_status(self, test, mp, timed_out):
        reason = "All hw-watcher checks are missing, but it's probably some other failure: walle_meta is staled."
        error = "Unable to check actuality of {} operation: {}"

        reasons = mock_status_reasons(event_time=0)
        mp.function(juggler.get_host_health_reasons, return_value=reasons)
        monkeypatch_function(
            mp,
            dmc.get_decisions_from_handler,
            module=dmc,
            return_value=(None, mock_decision(WalleAction.WAIT, reason=reason)),
        )

        host = test.mock_host(
            dict(
                state=HostState.ASSIGNED,
                status=self.status,
                task=mock_task(stage=self.stage_name, stage_status=StageStatus.HW_ERRORS_PENDING),
            )
        )
        stage = get_current_stage(host)

        if timed_out:
            stage.set_temp_data("monitoring_start_time", timestamp() - hw_errors._MONITORING_TIMEOUT)
            host.save()

        handle_host(host)

        if timed_out:
            mock_task_cancellation(host)
        else:
            stage.set_temp_data("monitoring_start_time", timestamp())
            mock_commit_stage_changes(host, check_after=MONITORING_PERIOD, error=error.format(self.operation, reason))

        test.hosts.assert_equal()

    @pytest.mark.parametrize("timed_out", (True, False))
    def test_pending_with_missing_check_status(self, test, mp, timed_out):
        decision = mock_decision(WalleAction.WAIT)
        monkeypatch_check_rule(mp, decision, self.check_type, self.check_rule)
        monkeypatch_function(
            mp,
            dmc.get_decisions_from_handler,
            module=dmc,
            return_value=(None, decision),
        )

        host = test.mock_host(
            dict(
                state=HostState.ASSIGNED,
                status=self.status,
                task=mock_task(stage=self.stage_name, stage_status=StageStatus.HW_ERRORS_PENDING),
            )
        )
        stage = get_current_stage(host)

        if timed_out:
            stage.set_temp_data("monitoring_start_time", timestamp() - hw_errors._MONITORING_TIMEOUT)
            host.save()

        handle_host(host)

        if timed_out:
            mock_task_cancellation(host)
        else:
            stage.set_temp_data("monitoring_start_time", timestamp())
            mock_commit_stage_changes(
                host,
                check_after=MONITORING_PERIOD,
                error="Unable to check actuality of {} operation: {}".format(self.operation, decision.reason),
            )

        test.hosts.assert_equal()

    def test_pending_without_error(self, test, mp):
        reasons = mock_status_reasons()
        mp.function(juggler.get_host_health_reasons, return_value=reasons)
        monkeypatch_function(
            mp,
            dmc.get_decisions_from_handler,
            module=dmc,
            return_value=(None, mock_decision(WalleAction.HEALTHY)),
        )

        host = test.mock_host(
            dict(
                state=HostState.ASSIGNED,
                status=self.status,
                task=mock_task(stage=self.stage_name, stage_status=StageStatus.HW_ERRORS_PENDING),
            )
        )

        handle_host(host)

        mock_task_cancellation(host)
        test.hosts.assert_equal()

    def test_pending_without_error_op_limits_are_not_applied(self, test, mp):
        def check_limits(*args, **kwargs):
            return limit_not_breached

        check_limits_mock = mp.function(operations_log.check_limits, side_effect=check_limits)
        reasons = mock_status_reasons()
        mp.function(juggler.get_host_health_reasons, return_value=reasons)

        monkeypatch_function(
            mp,
            dmc.get_decisions_from_handler,
            module=dmc,
            return_value=(None, mock_decision(WalleAction.HEALTHY)),
        )

        host = test.mock_host(
            dict(
                state=HostState.ASSIGNED,
                status=self.status,
                restrictions=None,
                task=mock_task(
                    stage=self.stage_name,
                    stage_params={"slot": None},
                    stage_status=StageStatus.HW_ERRORS_PENDING,
                ),
            )
        )

        handle_host(host)
        mock_task_cancellation(host)

        assert check_limits_mock.called
        test.hosts.assert_equal()

    @pytest.mark.parametrize(
        ["response", "status_time", "retry"],
        [
            # admin request has gone
            ("UNKNOWN", None, False),
            ("UNKNOWN", 0, False),
            ("UNKNOWN", 2 * HOUR_SECONDS, False),
            ("UNKNOWN", 2 * HOUR_SECONDS + 1, True),
            ("FALSE", None, False),
            ("FALSE", 0, False),
            ("FALSE", 2 * HOUR_SECONDS, False),
            ("FALSE", 2 * HOUR_SECONDS + 1, True),
            # admin request was deleted
            (BOT_RESPONSE_FAULT, None, True),
            (BOT_RESPONSE_FAULT, 0, True),
            (BOT_RESPONSE_FAULT, 2 * HOUR_SECONDS + 1, True),
            (BOT_RESPONSE_DELETED, None, True),
            (BOT_RESPONSE_DELETED, 0, True),
            (BOT_RESPONSE_DELETED, 2 * HOUR_SECONDS + 1, True),
        ],
    )
    def test_waiting_missing_request(self, test, mp, response, status_time, retry):
        mp.function(bot.raw_request, return_value=response)
        retry_handler = mp.method(
            self.retry_handler,
            obj=self.retry_handler_obj,
            side_effect=lambda self: commit_stage_changes(
                self._host, status=StageStatus.HW_ERRORS_WAITING_DC, check_now=True
            ),
        )
        # effectively, just wrap it into a call logger.
        get_request_status = mp.function(
            admin_requests.get_request_status, side_effect=admin_requests.get_request_status
        )

        request_id = 666
        host = test.mock_host(
            dict(
                state=HostState.ASSIGNED,
                status=self.status,
                task=mock_task(
                    stage=self.stage_name,
                    stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                    stage_temp_data=drop_none(
                        {
                            hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                            "decision_params": {},
                            "decision_reason": "reason-mock",
                            "request_gone_timestamp": timestamp() - status_time if status_time is not None else None,
                        }
                    ),
                ),
            )
        )

        handle_host(host)

        stage = get_current_stage(host)
        if response in {"UNKNOWN", "FALSE"}:
            stage.setdefault_temp_data("request_gone_timestamp", timestamp())
        else:
            host.ticket = self.TICKET_KEY
            get_current_stage(host).set_data("tickets", [self.TICKET_KEY])
            host.task.revision += 1

        get_request_status.assert_called_once_with(request_id)
        if retry:
            mock_commit_stage_changes(host, check_now=True, status=StageStatus.HW_ERRORS_WAITING_DC)
            assert retry_handler.call_count == 1
        else:
            mock_commit_stage_changes(host, error="BOT request 666 does not exist (got error {})".format(response))
            assert not retry_handler.called

        test.hosts.assert_equal()

    def test_waiting_in_process_request(self, test, mp):
        request_id = 666
        get_request_status = mp.function(
            admin_requests.get_request_status,
            return_value={
                hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                "status": admin_requests.STATUS_IN_PROCESS,
            },
        )

        host = test.mock_host(
            dict(
                state=HostState.ASSIGNED,
                status=self.status,
                task=mock_task(
                    stage=self.stage_name,
                    stage_status=StageStatus.HW_ERRORS_WAITING_DC,
                    stage_temp_data={
                        hw_errors.REQUEST_ID_STAGE_FIELD_NAME: request_id,
                        "decision_params": {},
                        "decision_reason": "reason-mock",
                    },
                ),
            )
        )

        handle_host(host)

        get_request_status.assert_called_once_with(request_id)
        mock_commit_stage_changes(
            host,
            status_message="Waiting for #666 BOT admin request to complete.",
            check_after=ADMIN_REQUEST_CHECK_INTERVAL,
        )

        test.hosts.assert_equal()

    def test_healthy_with_limit_exceeded_executes_original_decision(self, test, mp, monkeypatch_audit_log):
        orig_decision = mock_decision(WalleAction.REBOOT, reason="Original reason")
        new_decision = mock_decision(WalleAction.HEALTHY)
        monkeypatch_check_rule(mp, new_decision, self.check_type, self.check_rule)

        monkeypatch_function(
            mp,
            dmc.get_decisions_from_handler,
            module=dmc,
            return_value=(None, mock_decision(WalleAction.HEALTHY)),
        )

        check_limits_mock = mp.function(operations_log.check_limits, return_value=limit_breached)

        host = test.mock_host(
            dict(
                state=HostState.ASSIGNED,
                status=self.status,
                task=mock_task(
                    stage=self.stage_name,
                    stage_status=StageStatus.HW_ERRORS_PENDING,
                    stage_data={"orig_decision": orig_decision.to_dict()},
                ),
            )
        )

        handle_host(host)

        assert check_limits_mock.called
        mock_schedule_host_reboot(host, manual=False, reason=orig_decision.reason)

        test.hosts.assert_equal()
