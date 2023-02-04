"""Tests host monitoring after task completion."""

import random

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    patch,
    handle_host,
    mock_task,
    monkeypatch_expert,
    mock_fail_current_stage,
    mock_status_reasons,
    mock_commit_stage_changes,
    check_stage_initialization,
    mock_decision,
    mock_schedule_host_profiling,
    mock_schedule_host_reboot,
    monkeypatch_audit_log,
    monkeypatch_automation_plot_id,
    any_task_status,
    fail_check,
    mock_complete_current_stage,
)
from sepelib.core.constants import HOUR_SECONDS

# from walle import hbf_drills
from walle.clients.eine import ProfileMode
from walle.expert import constants as expert_constants, dmc, juggler, decisionmakers
from walle.expert.constants import MONITORING_TIMEOUT
from walle.expert.decision import Decision
from walle.expert.failure_types import FailureType
from walle.expert.types import WalleAction, CheckType, CheckStatus, get_walle_check_type
from walle.fsm_stages.common import MONITORING_PERIOD
from walle.hosts import HealthStatus, HostState

# from walle.models import timestamp, monkeypatch_timestamp
from walle.models import timestamp
from walle.stages import Stage, Stages
from walle.clients import dmc as dmc_client
from walle.fsm_stages import monitor as monitor_stage

CUSTOM_PLOT_NAME = "custom"
CUSTOM_CHECK_NAME = "mock"


@pytest.fixture
def mock_decision_handler(mp):
    mp.function(
        dmc_client.get_decisions_from_handler,
        return_value=(
            Decision(action=WalleAction.WAIT, reason='Mock.', restrictions=[]),
            Decision(action=WalleAction.WAIT, reason='Mock.', restrictions=[]),
        ),
    )


@pytest.fixture
def test(request, mp, monkeypatch_timestamp, monkeypatch_check_percentage, mp_juggler_source):
    monkeypatch_expert(mp, enabled=True)
    monkeypatch_automation_plot_id(mp, CUSTOM_PLOT_NAME)

    test_case = TestCase.create(request, healthdb=True)

    test_case.automation_plot.mock(
        {
            "id": CUSTOM_PLOT_NAME,
            "checks": [
                {
                    "name": CUSTOM_CHECK_NAME,
                    "enabled": True,
                    "reboot": False,
                    "redeploy": True,
                    "wait": True,
                    "start_time": timestamp() - 100,
                }
            ],
        }
    )

    return test_case


def _break_check(reasons, check_type):
    reasons[check_type] = {"status": CheckStatus.MISSING}


def test_stage_initialization(test, mock_decision_handler):
    check_stage_initialization(test, Stage(name=Stages.MONITOR))


def test_disabled_expert_system(test, mp, mock_decision_handler):
    monkeypatch_expert(mp, enabled=False)
    host = test.mock_host({"state": HostState.ASSIGNED, "task": mock_task(stage=Stages.MONITOR)})

    handle_host(host)
    mock_complete_current_stage(host)

    test.hosts.assert_equal()


def test_host_state_is_free(test, mock_decision_handler):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(stage=Stages.MONITOR),
        }
    )

    handle_host(host)
    mock_complete_current_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize("most_recent_timestamp", ("state_time", "rename_time"))
@patch("walle.expert.dmc.handle_monitoring")
def test_host_not_adopted(handle_monitoring, test, most_recent_timestamp, mock_decision_handler):
    adoption_timeout = timestamp() - expert_constants.HOST_ADOPTION_TIMEOUT
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "state_time": adoption_timeout + int(most_recent_timestamp == "state_time"),
            "rename_time": adoption_timeout + int(most_recent_timestamp == "rename_time"),
            "task": mock_task(stage=Stages.MONITOR),
        }
    )

    handle_host(host)
    mock_commit_stage_changes(
        host, check_after=MONITORING_PERIOD, status_message="Waiting for Juggler to catch the host."
    )

    assert handle_monitoring.call_count == 0
    test.hosts.assert_equal()


@pytest.mark.parametrize("has_rename_time", (True, False))
@patch("walle.expert.dmc.handle_monitoring")
def test_host_adoption_timeout(handle_monitoring, test, has_rename_time, mock_decision_handler):
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "state_time": timestamp() - expert_constants.HOST_ADOPTION_TIMEOUT,
            "rename_time": timestamp() - expert_constants.HOST_ADOPTION_TIMEOUT if has_rename_time else None,
            "task": mock_task(stage=Stages.MONITOR),
        }
    )

    handle_host(host)
    mock_commit_stage_changes(
        host,
        check_after=monitor_stage._ERROR_MONITORING_PERIOD,
        error="Juggler failed to catch the host.",
        status_message="Waiting for Juggler to catch the host.",
    )

    assert handle_monitoring.call_count == 0
    test.hosts.assert_equal()


class TestCurrentHealth:
    @pytest.mark.parametrize("has_non_required", (True, False))
    @patch("walle.expert.dmc.handle_monitoring", return_value=Decision.healthy("reason-mock"))
    def test_ok(self, handle_monitoring, test, mp, has_non_required):
        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "task": mock_task(stage=Stages.MONITOR, stage_params={"checks": CheckType.ALL}),
            }
        )

        reasons = mock_status_reasons()
        if not has_non_required:
            for check_type in CheckType.ALL_INFRASTRUCTURE:
                _break_check(reasons, check_type)
            mp.function(
                dmc_client.get_decisions_from_handler,
                return_value=(mock_decision(WalleAction.WAIT), mock_decision(WalleAction.WAIT)),
            )
        else:
            mp.function(
                dmc_client.get_decisions_from_handler,
                return_value=(mock_decision(WalleAction.WAIT), mock_decision(WalleAction.HEALTHY)),
            )

        mp.function(juggler.get_host_health_reasons, return_value=reasons)

        handle_host(host)
        mock_complete_current_stage(host)

        assert handle_monitoring.call_count == 1
        test.hosts.assert_equal()

    @pytest.mark.parametrize("check_type", set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - set(CheckType.ALL_META))
    def test_monitoring_wait_for_required_checks(self, test, mp, check_type):
        mp.function(
            dmc_client.get_decisions_from_handler,
            return_value=(mock_decision(WalleAction.WAIT), mock_decision(WalleAction.WAIT)),
        )

        handle_monitoring = mp.function(dmc.handle_monitoring, side_effect=dmc.handle_monitoring)

        start_time = timestamp()
        health_effective_timestamp = start_time - 1

        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "task": mock_task(
                    stage=Stage(name=Stages.MONITOR, params={"checks": CheckType.ALL}, status_time=start_time)
                ),
                "checks_min_time": start_time,
            }
        )

        reasons = mock_status_reasons(effective_timestamp=health_effective_timestamp, reasons=["all bad"])
        _break_check(reasons, check_type)
        mp.function(juggler.get_host_health_reasons, return_value=reasons)

        handle_host(host)

        mock_commit_stage_changes(
            host,
            check_after=monitor_stage.MONITORING_PERIOD,
            status_message="Waiting for the host to become alive.",
        )
        test.hosts.assert_equal()
        assert not handle_monitoring.called

    @pytest.mark.parametrize("check_type", set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - set(CheckType.ALL_META))
    def test_monitoring_wait_for_required_checks_while_other_passive_check_fails(
        self, test, mp, check_type, mock_decision_handler
    ):
        handle_monitoring = mp.function(dmc.handle_monitoring, side_effect=dmc.handle_monitoring)

        other_checks = list(set(CheckType.ALL_PASSIVE) - {CheckType.META, check_type})
        failed_check = random.choice(other_checks)
        stage_checks = list(set(CheckType.ALL) - {failed_check})

        start_time = timestamp() - 1
        health_effective_timestamp = timestamp()

        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "task": mock_task(
                    stage=Stage(name=Stages.MONITOR, params={"checks": stage_checks}, status_time=start_time)
                ),
                "checks_min_time": start_time,
            }
        )

        reasons = mock_status_reasons(effective_timestamp=health_effective_timestamp)
        _break_check(reasons, check_type)
        fail_check(reasons, failed_check)
        mp.function(juggler.get_host_health_reasons, return_value=reasons)

        handle_host(host)

        mock_commit_stage_changes(
            host,
            check_after=monitor_stage.MONITORING_PERIOD,
            status_message="Waiting for the host to become alive.",
        )
        test.hosts.assert_equal()
        assert not handle_monitoring.called

    # @pytest.mark.parametrize("check_type", set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - set(CheckType.ALL_META))
    # def test_monitoring_wait_for_hbf_drill_to_finish(
    #     self, test, mp, hbf_drills_mocker, check_type, mock_wait_alternate_decision
    # ):
    #     cur_ts = 1000000
    #     monkeypatch_timestamp(mp, cur_time=cur_ts)
    #
    #     drill_reason = "reason-mock"
    #     mp.method(
    #         hbf_drills.HbfDrillsCollection.get_host_inclusion_reason,
    #         return_value=drill_reason,
    #         obj=hbf_drills.HbfDrillsCollection,
    #     )
    #
    #     other_checks = list(set(CheckType.ALL_PASSIVE) - {CheckType.META, check_type})
    #     failed_check = random.choice(other_checks)
    #     stage_checks = list(set(CheckType.ALL) - {failed_check})
    #
    #     host = test.mock_host(
    #         {
    #             "state": HostState.ASSIGNED,
    #             "task": mock_task(
    #                 stage=Stage(name=Stages.MONITOR, params={"checks": stage_checks}, status_time=cur_ts - 1)
    #             ),
    #         }
    #     )
    #
    #     handle_host(host)
    #
    #     mock_commit_stage_changes(
    #         host,
    #         check_after=MONITORING_PERIOD,
    #         status_message="Waiting for HBF drill to finish: {}".format(drill_reason),
    #     )
    #     test.hosts.assert_equal()

    @pytest.mark.parametrize("check_type", set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - set(CheckType.ALL_META))
    def test_monitoring_wait_for_required_checks_while_other_active_check_fails(self, test, mp, check_type):
        monkeypatch_audit_log(mp)
        handle_monitoring = mp.function(dmc.handle_monitoring, side_effect=dmc.handle_monitoring)

        health_effective_timestamp = timestamp()
        status_mtime = timestamp() - HOUR_SECONDS

        other_checks = list(set(CheckType.ALL_ACTIVE) - {CheckType.META, check_type})
        failed_check = random.choice(other_checks)
        stage_checks = list(set(CheckType.ALL) - {failed_check})

        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "task": mock_task(
                    stage=Stage(name=Stages.MONITOR, params={"checks": stage_checks}, status_time=status_mtime)
                ),
                "checks_min_time": status_mtime,
            }
        )

        reasons = mock_status_reasons(
            effective_timestamp=health_effective_timestamp, status_mtime=status_mtime, reasons=["all bad"]
        )

        _break_check(reasons, check_type)
        fail_check(reasons, failed_check)
        walle_check = get_walle_check_type(failed_check)
        mp.function(juggler.get_host_health_reasons, return_value=reasons)
        decision = mock_decision(
            action=WalleAction.REBOOT,
            failure_type=FailureType.AVAILABILITY,
            reason="Host is not available: {}.".format(walle_check),
            checks=[failed_check],
        )
        mp.function(dmc_client.get_decisions_from_handler, return_value=(decision, decision))

        handle_host(host)

        mock_schedule_host_reboot(
            host,
            manual=False,
            extra_checks=[failed_check],
            reason="Host is not available: {}.".format(walle_check),
            failure_type=FailureType.AVAILABILITY,
        )
        test.hosts.assert_equal()
        assert handle_monitoring.called

    @pytest.mark.parametrize("check_type", set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - set(CheckType.ALL_META))
    def test_monitoring_timeout_while_missing_required_checks(self, mp, test, check_type, mock_decision_handler):
        handle_monitoring = mp.function(dmc.handle_monitoring, side_effect=dmc.handle_monitoring)

        start_time = timestamp() - MONITORING_TIMEOUT

        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "task": mock_task(
                    stage=Stage(name=Stages.MONITOR, params={"checks": CheckType.ALL}, status_time=start_time)
                ),
                "checks_min_time": start_time,
            }
        )

        reasons = mock_status_reasons(juggler_check_time=timestamp())
        _break_check(reasons, check_type)
        mp.function(juggler.get_host_health_reasons, return_value=reasons)

        handle_host(host)

        mock_commit_stage_changes(
            host,
            check_after=monitor_stage._ERROR_MONITORING_PERIOD,
            status_message="Waiting for the host to become alive.",
            error="Host health check failed: {}.missing. Monitoring timeout exceeded.".format(
                get_walle_check_type(check_type)
            ),
        )

        assert handle_monitoring.call_count == 1
        test.hosts.assert_equal()

    @pytest.mark.parametrize(
        "check_type",
        set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB)),
    )
    def test_monitoring_timeout_while_missing_required_check_and_other_check_fails(self, mp, test, check_type):
        monkeypatch_audit_log(mp)
        mp.function(
            dmc_client.get_decisions_from_handler,
            return_value=(
                Decision(action=WalleAction.WAIT, reason='Mock.', restrictions=[]),
                Decision(action=WalleAction.REBOOT, reason="Reason mock.", restrictions=[]),
            ),
        )
        original_schedule_action = dmc.schedule_action

        def schedule_action(host, decision, *args, **kwargs):
            assert decision.action not in (
                WalleAction.HEALTHY,
                WalleAction.WAIT,
                WalleAction.FAILURE,
                WalleAction.DEACTIVATE,
            )
            decision = mock_decision(WalleAction.REBOOT)  # yep, stub decision to always be reboot
            original_schedule_action(host, decision, *args, **kwargs)

        schedule_action_mock = mp.function(dmc.schedule_action, side_effect=schedule_action)
        start_time = timestamp() - MONITORING_TIMEOUT

        other_checks = list(
            set(CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB)) - {check_type}
        )

        failed_check = random.choice(other_checks)
        stage_checks = list(set(CheckType.ALL) - {failed_check})

        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "task": mock_task(
                    stage=Stage(name=Stages.MONITOR, params={"checks": stage_checks}, status_time=start_time)
                ),
                "checks_min_time": start_time,
            }
        )

        reasons = mock_status_reasons(juggler_check_time=timestamp(), reasons=["all bad"])
        _break_check(reasons, check_type)
        fail_check(reasons, failed_check)
        mp.function(juggler.get_host_health_reasons, return_value=reasons)

        handle_host(host)

        mock_schedule_host_reboot(host, manual=False, reason="Reason mock.")
        test.hosts.assert_equal()
        assert schedule_action_mock.call_count == 1


@pytest.mark.parametrize("dmc_action", set(WalleAction.ALL_MONITOR) - {WalleAction.WAIT})
def test_dmc_action(mp, test, dmc_action, mock_decision_handler):
    """Test host copy that retains at FSM is in consistent condition."""
    handle_monitoring = mp.function(dmc.handle_monitoring, return_value=mock_decision(dmc_action))
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "task": mock_task(
                stage=Stages.MONITOR,
                stage_params={"checks": CheckType.ALL},
                stage_temp_data={"host_info_adoption_start_time": timestamp()},
            ),
        }
    )
    reasons = mock_status_reasons()
    mp.function(juggler.get_host_health_reasons, return_value=reasons)

    handle_host(host)

    if dmc_action == WalleAction.FAILURE:
        mock_fail_current_stage(host, reason="Reason mock.")
    elif dmc_action == WalleAction.HEALTHY:
        mock_complete_current_stage(host)
    elif dmc_action == WalleAction.WAIT:
        # that's not happening
        pass
    else:
        # Do nothing. DMC should schedule new task for us.
        pass

    assert handle_monitoring.call_count == 1
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log", "monkeypatch_timestamp")
@pytest.mark.parametrize("check_type", (CheckType.UNREACHABLE,))
def test_custom_mandatory_wait_decision(test, mp, check_type, mock_decision_handler):
    handle_monitoring = mp.function(dmc.handle_monitoring, side_effect=dmc.handle_monitoring)

    start_time = timestamp() - MONITORING_TIMEOUT

    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "task": mock_task(
                stage=Stage(name=Stages.MONITOR, params={"checks": {check_type}}, status_time=start_time)
            ),
            "checks_min_time": start_time,
        }
    )

    reasons = mock_status_reasons(
        juggler_check_time=timestamp(), reasons=["all bad"], enabled_checks=set(CheckType.ALL) | {CUSTOM_CHECK_NAME}
    )
    fail_check(reasons, CUSTOM_CHECK_NAME)
    mp.function(juggler.get_host_health_reasons, return_value=reasons)

    handle_host(host)
    host.task.next_check = timestamp() + monitor_stage._ERROR_MONITORING_PERIOD
    host.task.revision += 1
    host.task.error = "Host health check failed: {}.failed. Monitoring timeout exceeded.".format(CUSTOM_CHECK_NAME)
    host.task.status_message = "Waiting for the host to become alive."

    test.hosts.assert_equal()

    assert handle_monitoring.call_count == 1


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_dmc_integration(mp, test):
    """Test DMC schedules action and leaves host in a consistent condition."""
    dmc_action = WalleAction.PROFILE
    decision = mock_decision(dmc_action)
    mp.function(dmc_client.get_decisions_from_handler, return_value=(decision, decision))
    mp.method(
        decisionmakers.AbstractDecisionMaker.make_availability_decision,
        obj=decisionmakers.AbstractDecisionMaker,
        return_value=decision,
    )

    reasons = mock_status_reasons(
        juggler_check_time=timestamp(),
        status_mtime=timestamp() - HOUR_SECONDS,
        effective_timestamp=timestamp() - HOUR_SECONDS,
        status=HealthStatus.STATUS_FAILURE,
    )
    mp.function(juggler.get_host_health_reasons, return_value=reasons)

    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(
                stage=Stages.MONITOR,
                stage_params={"checks": CheckType.ALL},
                stage_temp_data={"host_info_adoption_start_time": timestamp()},
            ),
        }
    )

    handle_host(host)

    mock_schedule_host_profiling(host, manual=False, profile_mode=ProfileMode.HIGHLOAD_TEST, reason="Reason mock.")
    test.hosts.assert_equal()
