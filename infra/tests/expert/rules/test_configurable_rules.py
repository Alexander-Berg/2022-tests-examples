"""Tests for configurable check rules."""
import pytest

import walle.operations_log.operations
from infra.walle.server.tests.lib.rules_util import limit_breached, limit_not_breached
from infra.walle.server.tests.lib.util import mock_status_reasons, mock_task
from sepelib.core.constants import MINUTE_SECONDS
from walle.expert.automation_plot import AutomationPlot, Check
from walle.expert.checks import get_check_max_possible_delay
from walle.expert.decision import Decision
from walle.expert.decisionmakers import ModernDecisionMaker
from walle.expert.rules import escalation
from walle.expert.types import WalleAction, CheckStatus
from walle.hosts import Host, HostStatus, HealthStatus
from walle.models import timestamp
from walle.operations_log.constants import Operation
from walle.projects import Project


def get_modern_decision_maker(
    project, check_types, wait=False, reboot=False, profile=False, redeploy=False, report_failure=True
):
    automation_plot = AutomationPlot(
        id="automation-plot-mock",
        checks=[
            Check(
                name=check_type,
                enabled=True,
                wait=wait,
                reboot=reboot,
                profile=profile,
                redeploy=redeploy,
                report_failure=report_failure,
            )
            for check_type in check_types
        ],
    )

    decision_maker = ModernDecisionMaker.for_automation_plot(project, automation_plot)
    return decision_maker(project, enabled_checks=check_types)


def mock_host(status=HostStatus.READY, project=None):
    host = Host(status=status)

    if status in HostStatus.ALL_TASK:
        host.task = mock_task()

    if project is None:
        # Default host project does not support profile.
        # This is intentional, so that you don't forget to test this case.
        project = Project(id="project-id-mock")
    host.get_project = lambda fields: project

    return host


def mock_check_failure_reason(
    reason_str="this check always fails",
    status=CheckStatus.FAILED,
    status_mtime_age=get_check_max_possible_delay("check-mock"),
):

    return {
        "status_mtime": timestamp() - status_mtime_age,
        "timestamp": timestamp() - MINUTE_SECONDS,
        "effective_timestamp": timestamp() - MINUTE_SECONDS,
        "status": status,
        "metadata": {
            "timestamp": timestamp(),
            "reason": reason_str,
        },
    }


@pytest.mark.parametrize(
    "policy",
    [
        {},
        {
            "disk": {},
            "memory": {},
            "fsck": {},
        },
        {
            "disk": None,
            "memory": None,
            "fsck": {},
        },
        {
            "disk": {"non_existing_parameter": "mock"},
            "memory": {"non_existing_parameter": "mock"},
            "fsck": {"non_existing_parameter": "mock"},
        },
        {
            "disk": {
                "redeploy_on_bad_block": False,
                "redeploy_on_no_info": False,
                "redeploy_on_unreserved_disk": False,
                "redeploy_on_unknown_disk": False,
            },
            "memory": {"redeploy_on_ecc_failure": False},
            "fsck": {"enabled": False},
        },
    ],
)
def test_decision_maker_successfully_creates_with_rule_policy(mp, policy):
    mp.config("automation.plot_policy.automation-plot-mock", policy)
    host = mock_host()
    get_modern_decision_maker(host.get_project(()), [])


def test_decision_maker_produces_healthy_for_passed_custom_check(walle_test):
    check_type = "custom-check-type"
    host = mock_host()
    decision_maker = get_modern_decision_maker(host.get_project(()), [check_type], reboot=True)

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    reasons[check_type] = {"status": CheckStatus.PASSED}

    decision = decision_maker.make_decision(host, reasons)
    assert decision == Decision.healthy(reason="Host is healthy.")


@pytest.mark.parametrize(
    ["wait", "reboot", "profile", "redeploy", "report_failure", "action"],
    [
        (True, False, False, False, False, WalleAction.WAIT),
        (True, True, False, False, False, WalleAction.WAIT),
        (False, True, False, False, False, WalleAction.REBOOT),
        (False, True, False, True, False, WalleAction.REBOOT),
        (False, False, True, False, False, WalleAction.PROFILE),
        (False, False, True, True, False, WalleAction.PROFILE),
        (False, False, False, True, False, WalleAction.REDEPLOY),
        (False, False, False, True, True, WalleAction.REDEPLOY),
        (False, False, False, False, True, WalleAction.REPORT_FAILURE),
        (False, False, False, False, False, WalleAction.HEALTHY),
    ],
)
def test_decision_maker_produces_non_healthy_for_failed_custom_check(
    mp, wait, reboot, profile, redeploy, report_failure, action
):
    check_type = "custom-check-type"
    host = mock_host()
    decision_maker = get_modern_decision_maker(
        host.get_project(()),
        [check_type],
        wait=wait,
        reboot=reboot,
        profile=profile,
        redeploy=redeploy,
        report_failure=report_failure,
    )
    mp.function(walle.operations_log.operations.check_limits, module=escalation, return_value=limit_not_breached)

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    reasons[check_type] = mock_check_failure_reason()

    decision = decision_maker.make_decision(host, reasons)
    if action == WalleAction.HEALTHY:
        assert decision == Decision(action, reason="Host is healthy.")
    else:
        expected_reason = "custom-check-type check failed: this check always fails."
        assert decision == Decision(action, reason=expected_reason, checks=[check_type])


@pytest.mark.parametrize("previous_check_status", [CheckStatus.SUSPECTED, CheckStatus.FAILED])
def test_decision_maker_produces_wait_when_previous_check_is_failing(mp, previous_check_status):
    prev1_check_type = "prev1-check-type"
    prev2_check_type = "prev2-check-type"
    this_check_type = "this-check-type"
    checks = [prev1_check_type, prev2_check_type, this_check_type]

    host = mock_host()
    decision_maker = get_modern_decision_maker(host.get_project(()), checks, reboot=True)
    mp.function(walle.operations_log.operations.check_limits, module=escalation, return_value=limit_not_breached)

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    # prev1_check_type keeps decision maker from producing decision for prev2_check_type
    # using the same mechanism that is under test.
    reasons[prev1_check_type] = mock_check_failure_reason(status=CheckStatus.SUSPECTED)
    # prev2_check_type triggers our test case for this_check_type.
    reasons[prev2_check_type] = mock_check_failure_reason(status=previous_check_status)
    reasons[this_check_type] = mock_check_failure_reason(status=CheckStatus.FAILED)

    decision = decision_maker.make_decision(host, reasons)
    expected_reason = (
        "this-check-type check failed: this check always fails, but it can be a prev2-check-type failure. Skipping."
    )
    assert decision == Decision.wait(reason=expected_reason, checks=[this_check_type])


def test_decision_maker_produces_wait_when_previous_check_have_just_turned_ok(mp):
    prev_check_type = "prev-check-type"
    this_check_type = "this-check-type"

    host = mock_host()
    decision_maker = get_modern_decision_maker(host.get_project(()), [prev_check_type, this_check_type], reboot=True)
    mp.function(walle.operations_log.operations.check_limits, module=escalation, return_value=limit_not_breached)

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    reasons[prev_check_type] = mock_check_failure_reason(
        status=CheckStatus.PASSED, status_mtime_age=get_check_max_possible_delay(prev_check_type) - 1
    )
    reasons[this_check_type] = mock_check_failure_reason(status=CheckStatus.FAILED)

    decision = decision_maker.make_decision(host, reasons)
    expected_reason = (
        "this-check-type check failed: this check always fails,"
        " but it may be an echo of prev-check-type failure. Wait for check to catch up."
    )
    assert decision == Decision.wait(reason=expected_reason, checks=[this_check_type])


def test_decision_maker_produces_wait_when_this_check_have_just_failed(mp):
    prev_check_type = "prev-check-type"
    this_check_type = "this-check-type"
    host = mock_host()
    decision_maker = get_modern_decision_maker(host.get_project(()), [prev_check_type, this_check_type], reboot=True)
    mp.function(walle.operations_log.operations.check_limits, module=escalation, return_value=limit_not_breached)

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    reasons[prev_check_type] = mock_check_failure_reason(status=CheckStatus.PASSED)
    reasons[this_check_type] = mock_check_failure_reason(
        status=CheckStatus.FAILED, status_mtime_age=get_check_max_possible_delay(prev_check_type) - 1
    )

    decision = decision_maker.make_decision(host, reasons)
    expected_reason = (
        "this-check-type check failed: this check always fails,"
        " but it may be some other failure. Wait for other checks to catch up."
    )
    assert decision == Decision.wait(reason=expected_reason, checks=[this_check_type])


def test_decision_maker_ignores_previous_check_when_it_is_missing(mp):
    prev_check_type = "prev-check-type"
    this_check_type = "this-check-type"
    host = mock_host()
    decision_maker = get_modern_decision_maker(host.get_project(()), [prev_check_type, this_check_type], reboot=True)
    mp.function(walle.operations_log.operations.check_limits, module=escalation, return_value=limit_not_breached)

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    reasons[prev_check_type] = dict(status=CheckStatus.MISSING)  # no metadata for missing checks
    reasons[this_check_type] = mock_check_failure_reason(status=CheckStatus.FAILED)

    decision = decision_maker.make_decision(host, reasons)
    expected_reason = "this-check-type check failed: this check always fails."
    assert decision == Decision(WalleAction.REBOOT, reason=expected_reason, checks=[this_check_type])


class TestEscalation:
    check_type = "custom-check-type"
    check_failure_reason = "this check always fails"

    @pytest.fixture
    def monkeypatch_limits(self, mp):
        self._monkeypatch_limits(mp)

    @staticmethod
    def _monkeypatch_limits(mp, limits=lambda *a, **kw: limit_not_breached):
        """
        :type limits: Iterable | (*args, **kwargs) -> object
        """
        mp.function(walle.operations_log.operations.check_limits, module=escalation, side_effect=limits)

    def _mock_decision(self, action, reason_template):
        if action in {WalleAction.HEALTHY, WalleAction.WAIT, WalleAction.DEACTIVATE}:
            checks = None
        else:
            checks = [self.check_type]

        expected_reason = reason_template.format(check_type=self.check_type, reason=self.check_failure_reason)

        return Decision(action, reason=expected_reason, checks=checks)

    def _produce_decision_for_check(self, host_status, reboot, profile, redeploy):
        host = mock_host(status=host_status)
        project = host.get_project(())

        decision_maker = get_modern_decision_maker(
            project, [self.check_type], reboot=reboot, profile=profile, redeploy=redeploy
        )

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        reasons[self.check_type] = mock_check_failure_reason(self.check_failure_reason)

        return decision_maker.make_decision(host, reasons)

    @pytest.mark.usefixtures("monkeypatch_limits")
    @pytest.mark.parametrize(
        ["profile", "redeploy", "action"],
        [
            (True, False, WalleAction.PROFILE),
            (True, True, WalleAction.PROFILE),
            (False, True, WalleAction.REDEPLOY),
            (False, False, WalleAction.REPORT_FAILURE),
        ],
    )
    def test_escalates_reboot_if_host_is_powering_on(self, walle_test, profile, redeploy, action):
        decision = self._produce_decision_for_check(
            Operation.POWER_ON.host_status, reboot=True, profile=profile, redeploy=redeploy
        )

        reason_template = "Host powering on hasn't helped: {check_type} check failed: {reason}."
        assert self._mock_decision(action, reason_template=reason_template) == decision

    @pytest.mark.usefixtures("monkeypatch_limits")
    @pytest.mark.parametrize(
        ["profile", "redeploy", "action"],
        [
            (True, True, WalleAction.PROFILE),
            (True, False, WalleAction.PROFILE),
            (False, True, WalleAction.REDEPLOY),
            (False, False, WalleAction.REPORT_FAILURE),
        ],
    )
    def test_escalates_reboot_if_host_is_rebooting(self, walle_test, profile, redeploy, action):
        decision = self._produce_decision_for_check(
            Operation.REBOOT.host_status, reboot=True, profile=profile, redeploy=redeploy
        )

        reason_template = "Reboot hasn't helped: {check_type} check failed: {reason}."
        assert self._mock_decision(action, reason_template=reason_template) == decision

    @pytest.mark.usefixtures("monkeypatch_limits")
    @pytest.mark.parametrize("redeploy", [True, False])
    def test_escalates_reboot_if_host_is_deploying(self, walle_test, redeploy):
        decision = self._produce_decision_for_check(
            Operation.REDEPLOY.host_status, reboot=True, profile=False, redeploy=redeploy
        )

        reason_template = "Redeploying hasn't helped: {check_type} check failed: {reason}."
        assert self._mock_decision(WalleAction.REPORT_FAILURE, reason_template=reason_template) == decision

    @pytest.mark.usefixtures("monkeypatch_limits")
    @pytest.mark.parametrize("redeploy", [True, False])
    def test_escalates_reboot_if_host_is_profiling(self, walle_test, redeploy):
        decision = self._produce_decision_for_check(
            Operation.PROFILE.host_status, reboot=True, profile=True, redeploy=False
        )

        reason_template = "Host profiling hasn't helped: {check_type} check failed: {reason}."
        assert self._mock_decision(WalleAction.REPORT_FAILURE, reason_template=reason_template) == decision

    @pytest.mark.usefixtures("monkeypatch_limits")
    @pytest.mark.parametrize("profile", [True, False])
    @pytest.mark.parametrize("redeploy", [True, False])
    def test_escalates_reboot_if_host_is_being_reported(self, walle_test, profile, redeploy):
        decision = self._produce_decision_for_check(
            Operation.REPORT_FAILURE.host_status, reboot=True, profile=profile, redeploy=redeploy
        )

        reason_template = "People are not fixing it: {check_type} check failed: {reason}."
        assert self._mock_decision(WalleAction.DEACTIVATE, reason_template=reason_template) == decision

    @pytest.mark.parametrize(
        ["profile", "redeploy", "action"],
        [
            (True, False, WalleAction.PROFILE),
            (True, True, WalleAction.PROFILE),
            (False, True, WalleAction.REDEPLOY),
            (False, False, WalleAction.REPORT_FAILURE),
        ],
    )
    def test_escalates_reboot_if_reboot_limit_reached(self, walle_test, mp, profile, redeploy, action):
        self._monkeypatch_limits(mp, [limit_breached, limit_not_breached, limit_not_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=True, profile=profile, redeploy=redeploy)

        reason_template = "{check_type} check failed: {reason}. We tried to reboot the host (limits msg mock)."
        assert self._mock_decision(action, reason_template=reason_template) == decision

    def test_escalates_reboot_if_redeploy_limit_reached(self, walle_test, mp):
        # This escalation should happen only if redeploy is enabled.
        # See other tests for cases when redeploy is not enabled.
        self._monkeypatch_limits(mp, [limit_breached, limit_breached, limit_not_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=True, profile=False, redeploy=True)

        reason_template = (
            "{check_type} check failed: {reason}."
            " We tried to reboot the host (limits msg mock)."
            " We tried to redeploy the host (limits msg mock)."
        )
        assert self._mock_decision(WalleAction.REPORT_FAILURE, reason_template=reason_template) == decision

    def test_escalates_reboot_if_profile_limit_reached(self, walle_test, mp):
        # This escalation should happen only if profile is enabled.
        # See other tests for cases when redeploy is not enabled.
        self._monkeypatch_limits(mp, [limit_breached, limit_breached, limit_not_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=True, profile=True, redeploy=False)

        reason_template = (
            "{check_type} check failed: {reason}."
            " We tried to reboot the host (limits msg mock)."
            " We tried to profile the host (limits msg mock)."
        )
        assert self._mock_decision(WalleAction.REPORT_FAILURE, reason_template=reason_template) == decision

    def test_escalates_reboot_if_reports_limit_reached(self, walle_test, mp):
        # This escalation should happen only if redeploy is enabled.
        # See other tests for cases when redeploy is not enabled.
        self._monkeypatch_limits(mp, [limit_breached, limit_breached, limit_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=True, profile=False, redeploy=True)

        reason_template = (
            "{check_type} check failed: {reason}."
            " We tried to reboot the host (limits msg mock)."
            " We tried to redeploy the host (limits msg mock)."
            " We tried to report host failure (limits msg mock)."
        )
        assert self._mock_decision(WalleAction.DEACTIVATE, reason_template=reason_template) == decision

    @pytest.mark.usefixtures("monkeypatch_limits")
    @pytest.mark.parametrize(["profile", "redeploy"], ((True, False), (False, True)))
    def test_escalates_redeploy_or_redeploy_if_host_is_deploying(self, walle_test, profile, redeploy):
        decision = self._produce_decision_for_check(
            Operation.REDEPLOY.host_status, reboot=False, profile=profile, redeploy=redeploy
        )

        reason_template = "Redeploying hasn't helped: {check_type} check failed: {reason}."
        assert self._mock_decision(WalleAction.REPORT_FAILURE, reason_template=reason_template) == decision

    @pytest.mark.usefixtures("monkeypatch_limits")
    @pytest.mark.parametrize(["profile", "redeploy"], ((True, False), (False, True)))
    def test_escalates_redeploy_or_profile_if_host_is_being_reported(self, walle_test, profile, redeploy):
        decision = self._produce_decision_for_check(
            Operation.REPORT_FAILURE.host_status, reboot=False, profile=profile, redeploy=redeploy
        )

        reason_template = "People are not fixing it: {check_type} check failed: {reason}."
        assert self._mock_decision(WalleAction.DEACTIVATE, reason_template=reason_template) == decision

    def test_escalates_redeploy_if_redeploy_limit_reached(self, walle_test, mp):
        self._monkeypatch_limits(mp, [limit_breached, limit_not_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=False, profile=False, redeploy=True)

        reason_template = "{check_type} check failed: {reason}. We tried to redeploy the host (limits msg mock)."
        assert self._mock_decision(WalleAction.REPORT_FAILURE, reason_template=reason_template) == decision

    def test_escalates_profile_if_profile_limit_reached(self, walle_test, mp):
        self._monkeypatch_limits(mp, [limit_breached, limit_not_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=False, profile=True, redeploy=False)

        reason_template = "{check_type} check failed: {reason}. We tried to profile the host (limits msg mock)."
        assert self._mock_decision(WalleAction.REPORT_FAILURE, reason_template=reason_template) == decision

    def test_escalates_redeploy_if_report_limit_reached(self, walle_test, mp):
        self._monkeypatch_limits(mp, [limit_breached, limit_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=False, profile=False, redeploy=True)

        reason_template = (
            "{check_type} check failed: {reason}."
            " We tried to redeploy the host (limits msg mock)."
            " We tried to report host failure (limits msg mock)."
        )
        assert self._mock_decision(WalleAction.DEACTIVATE, reason_template=reason_template) == decision

    def test_escalates_profile_if_report_limit_reached(self, walle_test, mp):
        self._monkeypatch_limits(mp, [limit_breached, limit_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=False, profile=True, redeploy=False)

        reason_template = (
            "{check_type} check failed: {reason}."
            " We tried to profile the host (limits msg mock)."
            " We tried to report host failure (limits msg mock)."
        )
        assert self._mock_decision(WalleAction.DEACTIVATE, reason_template=reason_template) == decision

    @pytest.mark.usefixtures("monkeypatch_limits")
    def test_escalates_report_if_host_is_being_reported(self, walle_test):
        decision = self._produce_decision_for_check(
            Operation.REPORT_FAILURE.host_status, reboot=False, profile=False, redeploy=False
        )

        reason_template = "People are not fixing it: {check_type} check failed: {reason}."
        assert self._mock_decision(WalleAction.DEACTIVATE, reason_template=reason_template) == decision

    def test_escalates_report_if_limit_reached(self, walle_test, mp):
        self._monkeypatch_limits(mp, [limit_breached])
        decision = self._produce_decision_for_check(HostStatus.READY, reboot=False, profile=False, redeploy=False)

        reason_template = "{check_type} check failed: {reason}. We tried to report host failure (limits msg mock)."
        assert self._mock_decision(WalleAction.DEACTIVATE, reason_template=reason_template) == decision
