"""Test escalation utils."""
from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_config, TestCase, mock_task
from walle.expert.automation import GLOBAL_DNS_AUTOMATION, PROJECT_DNS_AUTOMATION
from walle.expert.automation.global_automation import DnsAutomation
from walle.expert.automation.project_automation import _Automation
from walle.expert.decision import Decision
from walle.expert.rules.escalation import (
    EscalationRules,
    IEscalationPoint,
    EscalationPoint,
    Predicate,
    EscalationReason,
    limit_reached,
    platform_not_supported,
    dns_automation_off,
)
from walle.expert.types import WalleAction
from walle.hosts import Host, HostPlatform
from walle.operations_log.constants import Operation
from walle.operations_log.operations import on_completed_operation
from walle.restrictions import AUTOMATED_DNS, AUTOMATION


@pytest.fixture()
def test(monkeypatch_timestamp, request):
    return TestCase.create(request)


class TestEscalationRulesContainer:
    class MockEscalationPoint(IEscalationPoint):
        def __init__(self, input_decision, output_decision):
            self._input_decision = input_decision
            self._output_decision = output_decision

        def escalate(self, host, decision):
            if decision == self._input_decision:
                return self._output_decision
            else:
                return decision

    def test_runs_all_escalation_points_in_order(self):
        reboot = Decision(WalleAction.REBOOT, "reason-mock")
        profile = Decision(WalleAction.PROFILE, "reason-mock")
        redeploy = Decision(WalleAction.REDEPLOY, "reason-mock")
        deactivate = Decision.deactivate("reason-mock")

        escalation_rules = EscalationRules(
            self.MockEscalationPoint(redeploy, deactivate),
            self.MockEscalationPoint(reboot, profile),
            self.MockEscalationPoint(profile, redeploy),
        )

        assert redeploy == escalation_rules.escalate(Host(), reboot)

    def test_stops_when_decision_not_escalated(self):
        reboot = Decision(WalleAction.REBOOT, "reason-mock")
        profile = Decision(WalleAction.PROFILE, "reason-mock")
        deactivate = Decision.deactivate("reason-mock")

        escalation_rules = EscalationRules(
            self.MockEscalationPoint(reboot, profile),
            self.MockEscalationPoint(profile, profile),
            self.MockEscalationPoint(deactivate, deactivate),  # should not be called
        )

        assert profile == escalation_rules.escalate(Host(), reboot)

    def test_runs_escalation_from_any_position(self):
        # imagine that decision have already been escalated by host's status.
        # Now you need to continue escalation from this point up to check limits.
        reboot = Decision(WalleAction.REBOOT, "reason-mock")
        profile = Decision(WalleAction.PROFILE, "reason-mock")
        redeploy = Decision(WalleAction.REDEPLOY, "reason-mock")
        deactivate = Decision.deactivate("reason-mock")

        escalation_rules = EscalationRules(
            self.MockEscalationPoint(reboot, profile),
            self.MockEscalationPoint(profile, redeploy),
            self.MockEscalationPoint(redeploy, deactivate),
        )

        assert deactivate == escalation_rules.escalate(Host(), profile)

    def test_runs_escalation_for_same_action_until_escalated(self):
        reboot = Decision(WalleAction.REBOOT, "reason-mock")
        profile = Decision(WalleAction.PROFILE, "reason-mock")
        redeploy = Decision(WalleAction.REDEPLOY, "reason-mock")
        deactivate = Decision.deactivate("reason-mock")

        escalation_rules = EscalationRules(
            self.MockEscalationPoint(reboot, profile),
            self.MockEscalationPoint(profile, profile),
            self.MockEscalationPoint(profile, profile),
            self.MockEscalationPoint(profile, redeploy),
            self.MockEscalationPoint(profile, profile),
            self.MockEscalationPoint(redeploy, deactivate),
        )

        assert deactivate == escalation_rules.escalate(Host(), profile)


class TestEscalationPoint:
    def test_skips_escalation_if_predicate_is_false(self):
        reboot = Decision(WalleAction.REBOOT, "reason-mock")
        profile = Decision(WalleAction.PROFILE, "reason-mock")

        escalation_point = EscalationPoint(
            predicate=Predicate(lambda decision: False),
            reason=EscalationReason(lambda host, decision: "reason"),
            action=lambda decision, reason: profile,
        )
        assert reboot == escalation_point.escalate(Host(), reboot)

    def test_skips_escalation_if_condition_is_false(self):
        reboot = Decision(WalleAction.REBOOT, "reason-mock")
        profile = Decision(WalleAction.PROFILE, "reason-mock")

        escalation_point = EscalationPoint(
            predicate=Predicate(lambda decision: True),
            reason=EscalationReason(lambda host, decision: None),
            action=lambda decision, reason: profile,
        )
        assert reboot == escalation_point.escalate(Host(), reboot)

    def test_passes_reason_from_condition_to_escalation_function(self):
        mock_escalation_reason = "mock-escalation-reason"
        reboot = Decision(WalleAction.REBOOT, "reason-mock")
        profile = Decision(WalleAction.PROFILE, mock_escalation_reason)

        escalation_point = EscalationPoint(
            predicate=Predicate(lambda decision: True),
            reason=EscalationReason(lambda host, decision: mock_escalation_reason),
            action=lambda decision, reason: decision.escalate(WalleAction.PROFILE, reason),
        )

        assert profile == escalation_point.escalate(Host(), reboot)


class TestPredicate:
    def test_combined_is_only_true_when_both_predicates_true(self):
        reboot = Decision(WalleAction.REBOOT, "reason-mock")

        predicate = Predicate(lambda d: False) & Predicate(lambda d: False)
        assert predicate.evaluate(reboot) is False

        predicate = Predicate(lambda d: True) & Predicate(lambda d: False)
        assert predicate.evaluate(reboot) is False

        predicate = Predicate(lambda d: False) & Predicate(lambda d: True)
        assert predicate.evaluate(reboot) is False

        predicate = Predicate(lambda d: True) & Predicate(lambda d: True)
        assert predicate.evaluate(reboot) is True

    def test_combined_does_not_call_second_predicate_if_first_is_false(self):
        reboot = Decision(WalleAction.REBOOT, "reason-mock")

        mock_predicate_function = Mock(return_value=True)
        predicate = Predicate(lambda d: False) & Predicate(mock_predicate_function)

        assert predicate.evaluate(reboot) is False
        assert not mock_predicate_function.called

    def test_invert_produces_inverted_predicate(self):
        reboot = Decision(WalleAction.REBOOT, "reason-mock")

        predicate = ~Predicate(lambda d: False)
        assert predicate.evaluate(reboot) is True

        predicate = ~predicate
        assert predicate.evaluate(reboot) is False


def test_checks_limit_correctly(monkeypatch, test):
    """This is an integration test."""
    limit_name = "max_host_reboots"
    monkeypatch_config(monkeypatch, "automation.host_limits.{}".format(limit_name), [{"period": "1h", "limit": 2}])

    host = Host(inv=909, name="hostname-mock", task=mock_task())
    reboot = Decision(WalleAction.REBOOT, "reason-mock")

    reason_func = limit_reached(limit_name, Operation.REBOOT)

    # limit not reached, no operations in database
    assert reason_func.check(host, reboot) is None

    # limit not reached, have operations in database
    host.task.audit_log_id += "1"
    on_completed_operation(host, Operation.REBOOT.type)
    assert reason_func.check(host, reboot) is None

    # limit reached
    expected_reason = "{} We tried to reboot the host (2 times during the last 1 hour).".format(reboot.reason)

    host.task.audit_log_id += "1"
    on_completed_operation(host, Operation.REBOOT.type)
    assert reason_func.check(host, reboot) == expected_reason


@pytest.mark.parametrize(
    ["platforms", "exclude", "host_platform", "supported"],
    (
        ([], False, {}, False),
        ([{}], False, {}, True),
        ([{}], True, {}, False),
        ([{"system": "mock system"}], False, {"system": "mock system"}, True),
        ([{"system": "mock system"}], False, {"system": "other mock system"}, False),
        ([{"system": "mock system"}], True, {"system": "other mock system"}, True),
        ([{"board": "mock board"}], False, {"board": "mock board"}, True),
        ([{"board": "mock board"}], False, {"board": "other mock board"}, False),
        ([{"board": "mock board"}], True, {"board": "other mock board"}, True),
        (
            [{"system": "mock system", "board": "mock board"}],
            False,
            {"system": "mock system", "board": "mock board"},
            True,
        ),
        (
            [{"system": "mock system", "board": "mock board"}],
            False,
            {"system": "other mock system", "board": "mock board"},
            False,
        ),
        (
            [{"system": "mock system", "board": "mock board"}],
            True,
            {"system": "other mock system", "board": "mock board"},
            True,
        ),
    ),
)
def test_supported_platforms(monkeypatch, platforms, exclude, host_platform, supported):
    operation = Operation.REPAIR_MEMORY.type
    monkeypatch_config(monkeypatch, "automation.platform_support.{}.platforms".format(operation), platforms)
    monkeypatch_config(monkeypatch, "automation.platform_support.{}.exclude".format(operation), exclude)

    func = platform_not_supported(Operation.REPAIR_MEMORY)
    host = Host(inv=909, name="hostname-mock", platform=HostPlatform(**host_platform))
    decision = Decision(WalleAction.REPAIR_MEMORY, "Mock reason")

    message = func.check(host, decision)

    if supported:
        assert message is None
    else:
        assert message == "Mock reason Platform not supported {}".format(Operation.REPAIR_MEMORY.operation_name)


@pytest.mark.parametrize(
    ["global_automation", "project_automation", "restrictions", "message"],
    (
        (True, True, False, None),
        (False, False, False, "Global DNS automation is off"),
        (True, False, False, "Project DNS automation is off"),
        (True, True, True, "Host has restriction for DNS automation"),
    ),
)
def test_dns_automation_off(mp, global_automation, project_automation, restrictions, message):
    host = Host(inv=909, name="hostname-mock", project="mock_project")
    decision = Decision(WalleAction.REPAIR_MEMORY, "Mock reason")

    global_mock = mp.method(DnsAutomation.is_enabled, return_value=global_automation, obj=DnsAutomation)
    project_mock = mp.method(_Automation.enabled_for_project_id, return_value=project_automation, obj=_Automation)
    host_mock = mp.method(Host.applied_restrictions, return_value=({AUTOMATED_DNS} if restrictions else {}), obj=Host)

    expected_message = dns_automation_off.check(host, decision)

    assert expected_message == ("Mock reason {}".format(message) if message is not None else None)

    global_mock.assert_called_once_with(GLOBAL_DNS_AUTOMATION)

    if global_automation:
        project_mock.assert_called_once_with(PROJECT_DNS_AUTOMATION, "mock_project")
    else:
        assert not project_mock.called

    if global_automation and project_automation:
        host_mock.assert_called_once_with(host, AUTOMATED_DNS, AUTOMATION)
    else:
        assert not host_mock.called
