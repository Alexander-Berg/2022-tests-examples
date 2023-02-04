"""Tests host healing rules."""

from unittest.mock import ANY

import pytest

import walle.operations_log.operations
from infra.walle.server.tests.expert.rules.util import check_decision, fast
from infra.walle.server.tests.lib.util import TestCase, monkeypatch_automation_plot_id, mock_task, monkeypatch_config
from sepelib.core.constants import DAY_SECONDS, HOUR_SECONDS
from walle import restrictions
from walle.admin_requests.constants import RequestTypes
from walle.clients.eine import ProfileMode
from walle.constants import HostType
from walle.expert import rules
from walle.expert.automation_plot import AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.expert.constants import NETMON_REACTION_TIMEOUT
from walle.expert.decision import Decision
from walle.expert.failure_types import FailureType
from walle.expert.rules import SingleCheckRule, escalation
from walle.expert.rules.missing_passive_checks_rule import MissingPassiveChecksRule
from walle.expert.rules.utils import _should_be_disabled_check, repair_hardware_params
from walle.expert.types import WalleAction, Failure, CheckType, CheckStatus, CheckSets, get_walle_check_type
from walle.hosts import Host, HostLocation
from walle.models import monkeypatch_timestamp, timestamp
from walle.operations_log.constants import Operation
from walle.projects import Project


@pytest.fixture(autouse=True)
def rules_test(mp, request):
    monkeypatch_timestamp(mp)

    # disable escalation by limits, it's being checked in separate test module.
    # Status based escalation is still enabled, though.
    mp.function(walle.operations_log.operations.check_limits, module=escalation, return_value=True)

    # don't trigger this check, all checks enabled for tests
    mp.function(
        _should_be_disabled_check, side_effect=lambda host, check_type: should_be_disabled_mock(host, check_type)
    )

    return TestCase.create(request)


_COMPLETE_REASONS = {
    check_type: {
        "status": CheckStatus.PASSED,
        "effective_timestamp": timestamp(),
        "status_mtime": timestamp() - HOUR_SECONDS,
    }
    for check_type in CheckType.ALL
}

_INCOMPLETE_REASONS = {
    check_type: {
        "status": CheckStatus.MISSING if check_type in CheckType.ALL_NETMON else CheckStatus.PASSED,
        "effective_timestamp": timestamp(),
        "status_mtime": timestamp() - HOUR_SECONDS,
    }
    for check_type in CheckType.ALL
}


def should_be_disabled_mock(host, check_type):
    return False


@pytest.fixture(params=(False, True))
def enabled(request):
    return request.param


def test_healthy(reasons, fast, hw_checks_enabled):
    check_decision(reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled)


@pytest.mark.parametrize("network_check_type", CheckType.ALL_NETMON)
@pytest.mark.parametrize("status", CheckStatus.ALL)
def test_healthy_with_netmon_check(reasons, fast, hw_checks_enabled, network_check_type, status):
    reasons[network_check_type]["status"] = status

    check_decision(reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled)


@pytest.mark.parametrize("network_check_type", CheckType.ALL_INFRASTRUCTURE)
@pytest.mark.parametrize("network_check_status", set(CheckStatus.ALL) - {CheckStatus.PASSED})
@pytest.mark.parametrize("failed_check_type", CheckType.ALL_AVAILABILITY)
def test_failed_with_broken_network_check(
    reasons, fast, hw_checks_enabled, network_check_type, network_check_status, failed_check_type
):
    reasons[network_check_type]["status"] = network_check_status
    reasons[failed_check_type]["status"] = CheckStatus.FAILED
    rule = rules.AvailabilityCheckRule()

    host = Host(type=HostType.SERVER, location=HostLocation(short_queue_name="queue-mock", rack="rack-mock"))

    if fast:
        reason = ANY
    else:
        reason = (
            "Host is suspected to be unavailable: {},"
            " which might be due to an infrastructure issue: {} {}.".format(
                CheckType.WALLE_MAPPING[failed_check_type],
                CheckType.WALLE_MAPPING[network_check_type],
                network_check_status,
            )
        )

    check_decision(
        reasons,
        fast,
        WalleAction.WAIT,
        hw_checks_enabled=hw_checks_enabled,
        checks=[failed_check_type],
        reason=reason,
        host=host,
        rule=rule,
        failure_type=FailureType.AVAILABILITY,
    )


@pytest.mark.parametrize("failed_check_type", CheckType.ALL_AVAILABILITY)
def test_failed_with_netmon_check_delay(reasons, fast, failed_check_type):
    host = Host(inv=999, name="hostname-mock", project="project-mock", type=HostType.SERVER)

    reasons[failed_check_type]["status"] = CheckStatus.FAILED
    reasons[failed_check_type]["status_mtime"] = timestamp()
    rule = rules.AvailabilityCheckRule()

    check_decision(
        reasons,
        fast,
        WalleAction.WAIT,
        host=host,
        checks=[failed_check_type],
        reason="Host is suspected to be unavailable: {},"
        " which might be due to an infrastructure issue: {} waiting."
        "".format(CheckType.WALLE_MAPPING[failed_check_type], CheckType.WALLE_MAPPING[CheckType.NETMON]),
        rule=rule,
        failure_type=FailureType.AVAILABILITY,
    )


@pytest.mark.parametrize("failed_check_type", CheckType.ALL_AVAILABILITY)
def test_dont_fail_with_netmon_check_too_fresh(reasons, fast, failed_check_type):
    reasons[CheckType.NETMON]["status_mtime"] = timestamp() - NETMON_REACTION_TIMEOUT
    reasons[failed_check_type]["effective_timestamp"] = timestamp() - NETMON_REACTION_TIMEOUT
    reasons[failed_check_type]["status"] = CheckStatus.FAILED
    rule = rules.AvailabilityCheckRule()

    check_decision(
        reasons,
        fast,
        WalleAction.REBOOT,
        checks=[failed_check_type],
        reason="Host is not available: {}.".format(CheckType.WALLE_MAPPING[failed_check_type]),
        rule=rule,
        failure_type=FailureType.AVAILABILITY,
    )


@pytest.mark.parametrize("check_type", CheckType.ALL_AVAILABILITY)
@pytest.mark.parametrize("check_status", [CheckStatus.FAILED, CheckStatus.PASSED])
def test_failed_with_dns_updated_recently(reasons, fast, check_type, check_status):
    reasons[check_type]["status"] = check_status
    reasons[check_type]["effective_timestamp"] = timestamp()
    rule = rules.AvailabilityCheckRule()

    host = Host(dns={"update_time": timestamp() + 1})

    check_decision(
        reasons,
        fast,
        WalleAction.WAIT,
        checks=CheckType.ALL_AVAILABILITY,
        host=host,
        reason="Host is suspected to be unavailable: {},"
        " which might be due to dns ttl after records update."
        "".format(", ".join(map(get_walle_check_type, CheckType.ALL_AVAILABILITY))),
        rule=rule,
        failure_type=FailureType.AVAILABILITY,
    )


@pytest.mark.parametrize("check_type", CheckType.ALL_ACTIVE)
def test_failed_active_check(reasons, fast, check_type):
    reasons[check_type]["status"] = CheckStatus.FAILED
    reasons[check_type]["effective_timestamp"] = timestamp()
    rule = rules.AvailabilityCheckRule()

    check_decision(
        reasons,
        fast,
        WalleAction.REBOOT,
        checks=[check_type],
        reason="Host is not available: {}.".format(get_walle_check_type(check_type)),
        rule=rule,
        failure_type=FailureType.AVAILABILITY,
    )


@pytest.mark.parametrize("check_type", CheckType.ALL_ACTIVE)
def test_suspected_active_check(reasons, fast, check_type):
    reasons[check_type]["status"] = CheckStatus.SUSPECTED
    rule = rules.AvailabilityCheckRule()

    check_decision(
        reasons,
        fast,
        WalleAction.WAIT,
        checks=[check_type],
        reason="Host is suspected to be unavailable: {}.".format(get_walle_check_type(check_type)),
        rule=rule,
        failure_type=FailureType.AVAILABILITY,
    )


@pytest.mark.parametrize("check_type", CheckType.ALL_ACTIVE)
def test_unsure_active_check(reasons, fast, check_type):
    reasons[check_type]["status"] = CheckStatus.UNSURE
    rule = rules.AvailabilityCheckRule()

    check_decision(
        reasons,
        fast,
        WalleAction.WAIT,
        checks=[check_type],
        reason="{} check is not fresh enough.".format(get_walle_check_type(check_type)),
        rule=rule,
    )


@pytest.mark.parametrize("check_type", CheckType.ALL_ACTIVE)
@pytest.mark.parametrize(
    ["operation", "action", "params", "reason", "failure_type", "decision_restrictions"],
    [
        (Operation.POWER_ON, WalleAction.PROFILE, {}, "Host powering on hasn't helped", FailureType.AVAILABILITY, []),
        (Operation.REBOOT, WalleAction.PROFILE, {}, "Reboot hasn't helped", FailureType.AVAILABILITY, []),
        (Operation.PROFILE, WalleAction.REDEPLOY, {}, "Profiling hasn't helped", FailureType.AVAILABILITY, []),
        (
            Operation.REDEPLOY,
            WalleAction.PROFILE,
            {"profile_mode": ProfileMode.DANGEROUS_HIGHLOAD_TEST},
            "Redeploying hasn't helped",
            None,
            [restrictions.AUTOMATED_PROFILE, restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP],
        ),
        # TODO Profile (extra) -> 2nd_time_node
        (
            Operation.CHANGE_MEMORY,
            WalleAction.PROFILE,
            {"final": True},
            "It seems that processed memory change operation killed the node",
            None,
            [],
        ),
    ],
)
def test_failed_active_check_escalate(
    reasons, fast, check_type, operation, action, params, reason, failure_type, decision_restrictions
):
    host = Host(status=operation.host_status, task=mock_task())
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    reasons[check_type]["status"] = CheckStatus.FAILED
    reasons[check_type]["effective_timestamp"] = timestamp()
    rule = rules.AvailabilityCheckRule()

    if action == WalleAction.DEACTIVATE:
        decision_checks = None
    else:
        decision_checks = [check_type]

    expected_reason = "{}: Host is not available: {}.".format(reason, get_walle_check_type(check_type))

    check_decision(
        reasons,
        fast,
        action,
        params=params,
        checks=decision_checks,
        host=host,
        reason=expected_reason,
        rule=rule,
        failure_type=failure_type,
        restrictions=decision_restrictions,
    )


RELEVANT_PASSIVE_CHECKS = set(CheckType.ALL_PASSIVE) & CheckSets.FULL_FEATURED


class TestMissingPassiveChecksRule:
    @pytest.fixture()
    def host(self):
        return Host(inv=999, name="hostname-mock", project="project-mock")

    @pytest.fixture()
    def missing_and_old_reasons(self, reasons):
        reasons = self._passive_checks_are_missing(reasons)
        reasons = self._passive_checks_are_old_enough(reasons)
        return reasons

    @pytest.mark.usefixtures("enable_hw_checks")
    def test_all_passive_checks_are_freshly_missing(self, reasons, host):
        reasons = self._passive_checks_are_missing(reasons)
        decision = MissingPassiveChecksRule.make_decision(host, reasons, CheckSets.FULL_FEATURED)
        assert decision.action == WalleAction.WAIT  # these checks are not wrinkled enough yet

    @pytest.mark.usefixtures("enable_hw_checks")
    def test_all_passive_checks_missing_and_old_enough(self, missing_and_old_reasons, host):
        decision = MissingPassiveChecksRule.make_decision(host, missing_and_old_reasons, CheckSets.FULL_FEATURED)
        assert decision == Decision(
            WalleAction.REBOOT,
            reason="All passive checks are missing",
            failures=[Failure.CHECKS_MISSING],
            checks=sorted(MissingPassiveChecksRule.check_type_descr.check_type),
            failure_type=FailureType.MISSING_PASSIVE_CHECKS,
        )

    @pytest.mark.parametrize(
        "enabled_checks, disabled_checks",
        [(set(), set(CheckType.ALL_AVAILABILITY)), (set(CheckType.ALL_AVAILABILITY), RELEVANT_PASSIVE_CHECKS)],
    )
    def test_disabled_source_checks(self, missing_and_old_reasons, enabled_checks, disabled_checks, host):
        decision = MissingPassiveChecksRule.make_decision(host, missing_and_old_reasons, enabled_checks)
        assert decision.action == WalleAction.HEALTHY

    def test_avail_check_returned_wait(self, mp, missing_and_old_reasons, host):
        mp.method(
            MissingPassiveChecksRule._get_availability_check_results,
            obj=MissingPassiveChecksRule,
            return_value=Decision.wait(reason="Reason mock"),
        )
        decision = MissingPassiveChecksRule.make_decision(host, missing_and_old_reasons, CheckSets.FULL_FEATURED)
        assert decision.action == WalleAction.WAIT

    @pytest.mark.parametrize("check_type", RELEVANT_PASSIVE_CHECKS.union(CheckType.ALL_ACTIVE))
    def test_suspected_checks_make_wait(self, mp, missing_and_old_reasons, check_type, host):
        mp.method(
            MissingPassiveChecksRule.get_failed_checks,
            obj=MissingPassiveChecksRule,
            return_value=({}, {check_type: {}}),
        )
        missing_and_old_reasons[check_type]["status"] = CheckStatus.SUSPECTED
        decision = MissingPassiveChecksRule.make_decision(host, missing_and_old_reasons, CheckSets.FULL_FEATURED)
        assert decision.action == WalleAction.WAIT

    @staticmethod
    def _passive_checks_are_missing(reasons):
        for passive_check in CheckType.ALL_PASSIVE:
            reasons[passive_check]["status"] = CheckStatus.MISSING
        return reasons

    @staticmethod
    def _passive_checks_are_old_enough(reasons):
        for passive_check in CheckType.ALL_PASSIVE:
            reasons[passive_check]["status_mtime"] = timestamp() - DAY_SECONDS
        return reasons

    @pytest.mark.parametrize(
        ["operation", "action", "params", "reason", "failure_type", "decision_restrictions"],
        [
            (
                Operation.POWER_ON,
                WalleAction.PROFILE,
                {},
                "Host powering on hasn't helped",
                FailureType.MISSING_PASSIVE_CHECKS,
                [],
            ),
            (Operation.REBOOT, WalleAction.PROFILE, {}, "Reboot hasn't helped", FailureType.MISSING_PASSIVE_CHECKS, []),
            (
                Operation.PROFILE,
                WalleAction.REDEPLOY,
                {},
                "Profiling hasn't helped",
                FailureType.MISSING_PASSIVE_CHECKS,
                [],
            ),
            (
                Operation.REDEPLOY,
                WalleAction.PROFILE,
                {"profile_mode": ProfileMode.DANGEROUS_HIGHLOAD_TEST},
                "Redeploying hasn't helped",
                {},
                [restrictions.AUTOMATED_PROFILE, restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP],
            ),
            # TODO Profile (extra) -> 2nd_time_node
            (
                Operation.CHANGE_MEMORY,
                WalleAction.PROFILE,
                {"final": True},
                "It seems that processed memory change operation killed the node",
                None,
                [],
            ),
        ],
    )
    def test_missing_passive_checks_escalate(
        self, mp, missing_and_old_reasons, operation, action, params, reason, failure_type, decision_restrictions
    ):
        monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)
        host = Host(status=operation.host_status, task=mock_task())
        host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

        rule = rules.MissingPassiveChecksRule()

        if action == WalleAction.DEACTIVATE:
            decision_checks = None
        else:
            decision_checks = sorted(rule.check_type_descr.check_type)

        expected_reason = "{}: All passive checks are missing".format(reason)

        check_decision(
            missing_and_old_reasons,
            fast,
            action,
            params=params,
            checks=decision_checks,
            host=host,
            reason=expected_reason,
            rule=rule,
            failures=[Failure.CHECKS_MISSING],
            failure_type=failure_type,
            restrictions=decision_restrictions,
        )


@pytest.mark.parametrize("check_type", set(CheckType.ALL_JUGGLER) - {CheckType.TOR_LINK, CheckType.IB_LINK})
@pytest.mark.parametrize(
    ["ignored_check_status", "decision_reason"],
    [
        (CheckStatus.UNSURE, "{check_type} check is not fresh enough."),
        (CheckStatus.STALED, "{check_type} check is {check_status}."),
        (CheckStatus.MISSING, "No data for {check_type} check result."),
        (CheckStatus.INVALID, "{check_type} check has invalid metadata."),
    ],
)
def test_ignored_check(mp, reasons, fast, check_type, ignored_check_status, decision_reason):
    monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)

    reasons[check_type]["status"] = ignored_check_status

    if check_type in CheckType.ALL_META:
        check_decision(reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=True)
    else:
        reason = decision_reason.format(check_type=get_walle_check_type(check_type), check_status=ignored_check_status)
        check_decision(reasons, fast, WalleAction.WAIT, checks=[check_type], hw_checks_enabled=True, reason=reason)


@pytest.mark.parametrize("check_type", CheckType.ALL_JUGGLER)
def test_per_host_check_disabling(mp, reasons, fast, check_type):
    monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)

    mp.function(
        _should_be_disabled_check, side_effect=lambda host, inspected_check_type: inspected_check_type == check_type
    )

    reasons[check_type] = {"status": CheckStatus.FAILED}
    check_decision(reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=True)


@pytest.mark.parametrize("status", (CheckStatus.SUSPECTED, CheckStatus.FAILED))
def test_broken_meta_check(reasons, fast, status):
    reasons[CheckType.META] = {"status": status}
    check_decision(reasons, fast, WalleAction.HEALTHY)


@pytest.mark.parametrize("incomplete_reasons", [_INCOMPLETE_REASONS, _COMPLETE_REASONS])
def test_failed_reboots_check(incomplete_reasons, fast, hw_checks_enabled):
    incomplete_reasons[CheckType.REBOOTS] = {"status": CheckStatus.FAILED, "metadata": {"result": {"count": 50}}}
    rule = SingleCheckRule(rules.CheckReboots())

    if hw_checks_enabled:
        check_decision(
            incomplete_reasons,
            fast,
            WalleAction.REPAIR_REBOOTS,
            checks=[CheckType.REBOOTS],
            reason="Host is rebooting too often.",
            rule=rule,
            failure_type=FailureType.MANY_REBOOTS,
        )
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize("incomplete_reasons", [_INCOMPLETE_REASONS, _COMPLETE_REASONS])
@pytest.mark.parametrize('rule_paratemers', ({"enabled": False}, {"enabled": True}, {}))
def test_failed_tainted_kernel_check(incomplete_reasons, fast, hw_checks_enabled, rule_paratemers):
    incomplete_reasons[CheckType.TAINTED_KERNEL] = {
        "status": CheckStatus.FAILED,
        "metadata": {"result": {"reason": ["something crashed"]}},
    }
    rule = SingleCheckRule(rules.CheckTaintedKernel(**rule_paratemers))

    if hw_checks_enabled:
        if not rule_paratemers.get("enabled", True):
            check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, reason="Host is healthy.", rule=rule)
        else:
            check_decision(
                incomplete_reasons,
                fast,
                WalleAction.REBOOT,
                checks=[CheckType.TAINTED_KERNEL],
                reason="Kernel on host has been tainted: something crashed.",
                rule=rule,
                failure_type=FailureType.KERNEL_TAINTED,
            )
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize("incomplete_reasons", [_INCOMPLETE_REASONS, _COMPLETE_REASONS])
@pytest.mark.parametrize("rule_paratemers", ({"enabled": False}, {"enabled": True}, {}))
def test_failed_fsck_check(incomplete_reasons, fast, hw_checks_enabled, rule_paratemers):
    incomplete_reasons[CheckType.FS_CHECK] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": {
                "device_list": [
                    {
                        'error_count': 10532085,
                        'status': 'failed',
                        'message': 'partition sdc4 has broken filesystem',
                        'name': 'sdc4',
                        'threshold': 10000,
                    },
                    {
                        'error_count': 0,
                        'message': 'errors not found',
                        'name': 'md5',
                        'status': 'ok',
                        'threshold': 10000,
                    },
                    {
                        'error_count': 0,
                        'message': 'errors not found',
                        'name': 'md5',
                        'threshold': 10000,
                    },
                ]
            }
        },
    }
    rule = SingleCheckRule(rules.FsckRule(**rule_paratemers))

    if hw_checks_enabled:
        if rule_paratemers.get("enabled", True):
            check_decision(
                incomplete_reasons,
                fast,
                WalleAction.REDEPLOY,
                checks=[CheckType.FS_CHECK],
                reason="Filesystem is broken: partition sdc4 has broken filesystem.",
                rule=rule,
                failure_type=FailureType.FS_CHECK,
            )
        else:
            check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, reason="Host is healthy.", rule=rule)
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


class TestRackOverheatRule:
    PROJECT_NAME = "project-mock"

    @staticmethod
    def _make_failed(reasons, check_types):
        for check_type in check_types:
            reasons[check_type] = {
                "status": CheckStatus.FAILED,
                "metadata": {"result": {"eine_code": ["CPU_OVERHEATING"]}},
            }

    @staticmethod
    def _make_rack_overheat_check(n_failed=40, n_total=40):
        return {
            "status": CheckStatus.FAILED,
            "metadata": {
                "suspected_timeout": 1200,
                "threshold_flapping": 20,
                "last_crit": 1539180662,
                "failed": n_failed,
                "threshold_failed": 32,
                "suspected": n_failed,
                "total": n_total,
            },
        }

    def test_decide_rack_rapair(self, reasons, fast, hw_checks_enabled):
        location = HostLocation(short_queue_name="queue-mock", rack="rack-mock")
        host = Host(project=self.PROJECT_NAME, location=location)
        self._make_failed(reasons, [CheckType.CPU_CACHES])
        reasons[CheckType.WALLE_RACK_OVERHEAT] = self._make_rack_overheat_check()
        rule = rules.RackOverheatRule()

        if hw_checks_enabled:
            reason = "Rack rack-mock in queue queue-mock is overheated: 40 of 40 hosts overheated (threshold is 32)."
            check_decision(
                reasons,
                fast,
                WalleAction.REPAIR_RACK_OVERHEAT,
                host=host,
                checks=[CheckType.WALLE_RACK_OVERHEAT],
                reason=reason,
                rule=rule,
                failure_type=FailureType.RACK_OVERHEAT,
            )
        else:
            check_decision(reasons, fast, WalleAction.HEALTHY, host=host, checks=None, reason=ANY, rule=rule)

    def test_produces_wait_if_host_is_healthy(self, reasons, enable_hw_checks):
        location = HostLocation(short_queue_name="queue-mock", rack="rack-mock")
        host = Host(project=self.PROJECT_NAME, location=location)
        reasons[CheckType.WALLE_RACK_OVERHEAT] = self._make_rack_overheat_check()
        rule = rules.RackOverheatRule()

        check_decision(
            reasons,
            fast,
            WalleAction.WAIT,
            host=host,
            checks=[CheckType.WALLE_RACK_OVERHEAT],
            reason="Rack overheat check for rack rack-mock queue queue-mock failed but host is healthy.",
            rule=rule,
            failure_type=FailureType.RACK_OVERHEAT,
        )


class TestRackRule:
    PROJECT_NAME = "project-mock"

    @pytest.fixture(autouse=True)
    def enable_rack_rule(self, mp):
        monkeypatch_config(mp, "automation.rack_rule_enabled_projects", {self.PROJECT_NAME})

    @staticmethod
    def _make_failed(reasons, check_types):
        for check_type in check_types:
            reasons[check_type] = {"status": CheckStatus.FAILED}

    @staticmethod
    def _make_rack_check(n_failed=40, n_total=40):
        return {
            "status": CheckStatus.FAILED,
            "metadata": {
                "suspected_timeout": 1200,
                "threshold_flapping": 20,
                "last_crit": 1539180662,
                "failed": n_failed,
                "threshold_failed": 32,
                "suspected": n_failed,
                "total": n_total,
            },
        }

    @staticmethod
    def _make_netmon_check(status=CheckStatus.PASSED, dc_status=CheckStatus.PASSED, queue_status=CheckStatus.PASSED):
        return {
            "type": CheckType.NETMON,
            "status": status,
            "status_mtime": timestamp(),
            "metadata": {
                level_name: {"status": level_status, "alive": 1.0, "connectivity": [1, 2, 3, 4]}
                for level_name, level_status in (('switch', status), ('queue', queue_status), ('datacenter', dc_status))
            },
        }

    def test_common_sense(self, reasons, fast, hw_checks_enabled):
        location = HostLocation(short_queue_name="queue-mock", rack="rack-mock")
        host = Host(project=self.PROJECT_NAME, location=location, type=HostType.SERVER)

        self._make_failed(reasons, CheckType.ALL_AVAILABILITY)
        reasons[CheckType.WALLE_RACK] = self._make_rack_check()
        reasons[CheckType.NETMON] = self._make_netmon_check()
        rule = rules.RackRule()

        if hw_checks_enabled:
            reason = "Rack rack-mock in queue queue-mock has failed: 40 of 40 hosts failed (threshold is 32)."
            check_decision(
                reasons,
                fast,
                WalleAction.REPAIR_RACK_FAILURE,
                host=host,
                checks=[CheckType.WALLE_RACK],
                reason=reason,
                rule=rule,
                failure_type=FailureType.RACK_COMMON,
            )
        else:
            # rule is not enabled for basic plot, unreachable and ssh checks failed...
            check_decision(
                reasons,
                fast,
                WalleAction.WAIT,
                host=host,
                checks=CheckType.ALL_AVAILABILITY,
                reason=ANY,
                rule=rule,
                failure_type=FailureType.AVAILABILITY,
            )

    @pytest.mark.parametrize("failed_level", ["datacenter", "queue"])
    @pytest.mark.parametrize("netmon_check_status", CheckStatus.ALL)
    def test_produces_wait_if_dc_or_queue_failed(self, reasons, enable_hw_checks, failed_level, netmon_check_status):
        reasons = reasons.copy()  # this stuff does not work wel with parametrized test

        location = HostLocation(short_queue_name="queue-mock", rack="rack-mock")
        host = Host(project=self.PROJECT_NAME, location=location)

        self._make_failed(reasons, CheckType.ALL_AVAILABILITY)
        reasons[CheckType.WALLE_RACK] = self._make_rack_check()
        reasons[CheckType.NETMON] = self._make_netmon_check(
            status=netmon_check_status,
            dc_status=CheckStatus.FAILED if failed_level == "datacenter" else CheckStatus.PASSED,
            queue_status=CheckStatus.FAILED if failed_level == "queue" else CheckStatus.PASSED,
        )
        rule = rules.RackRule()

        check_decision(
            reasons,
            fast,
            WalleAction.WAIT,
            host=host,
            checks=[CheckType.WALLE_RACK],
            reason="Rack check for rack rack-mock queue queue-mock failed but netmon check shows some other problems.",
            rule=rule,
            failure_type=FailureType.RACK_COMMON,
        )

    @pytest.mark.parametrize("netmon_status", set(CheckStatus.ALL) - {CheckStatus.PASSED, CheckStatus.FAILED})
    def test_produces_wait_if_netmon_check_is_not_failed(self, reasons, enable_hw_checks, netmon_status):
        reasons = reasons.copy()  # this stuff does not work wel with parametrized test

        location = HostLocation(short_queue_name="queue-mock", rack="rack-mock")
        host = Host(project=self.PROJECT_NAME, location=location)

        self._make_failed(reasons, CheckType.ALL_AVAILABILITY)
        reasons[CheckType.WALLE_RACK] = self._make_rack_check()
        reasons[CheckType.NETMON] = {"status": netmon_status, "metadata": {}}
        rule = rules.RackRule()

        check_decision(
            reasons,
            fast,
            WalleAction.WAIT,
            host=host,
            checks=[CheckType.WALLE_RACK],
            reason="Rack check for rack rack-mock queue queue-mock failed but netmon check shows some other problems.",
            rule=rule,
            failure_type=FailureType.RACK_COMMON,
        )

    @pytest.mark.parametrize("failed_active_checks", [] + [[check_type] for check_type in CheckType.ALL_AVAILABILITY])
    def test_produces_wait_if_host_is_available(self, reasons, enable_hw_checks, failed_active_checks):
        reasons = reasons.copy()  # this stuff does not work wel with parametrized test

        location = HostLocation(short_queue_name="queue-mock", rack="rack-mock")
        host = Host(project=self.PROJECT_NAME, location=location)

        self._make_failed(reasons, failed_active_checks)
        reasons[CheckType.WALLE_RACK] = self._make_rack_check()
        rule = rules.RackRule()

        check_decision(
            reasons,
            fast,
            WalleAction.WAIT,
            host=host,
            checks=[CheckType.WALLE_RACK],
            reason="Rack check for rack rack-mock queue queue-mock failed but host is available.",
            rule=rule,
            failure_type=FailureType.RACK_COMMON,
        )

    def test_produces_wait_if_some_hosts_are_available(self, reasons, enable_hw_checks):
        reasons = reasons.copy()  # this stuff does not work wel with parametrized test

        location = HostLocation(short_queue_name="queue-mock", rack="rack-mock")
        host = Host(project=self.PROJECT_NAME, location=location)

        self._make_failed(reasons, CheckType.ALL_AVAILABILITY)
        reasons[CheckType.WALLE_RACK] = self._make_rack_check(n_failed=39, n_total=40)
        reasons[CheckType.NETMON] = self._make_netmon_check()
        rule = rules.RackRule()

        check_decision(
            reasons,
            fast,
            WalleAction.WAIT,
            host=host,
            checks=[CheckType.WALLE_RACK],
            reason="Some host has failed for rack rack-mock queue queue-mock. Waiting until all hosts fail.",
            rule=rule,
            failure_type=FailureType.RACK_COMMON,
        )


@pytest.mark.parametrize("incomplete_reasons", [_INCOMPLETE_REASONS, _COMPLETE_REASONS])
@pytest.mark.parametrize("rule_parameters", ({"enabled": False}, {"enabled": True}, {}))
def test_failed_torlink_check(incomplete_reasons, fast, rule_parameters, enable_hw_checks):
    incomplete_reasons[CheckType.TOR_LINK] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": {
                "status": "CRC ERROR",
                "timestamp": timestamp() - 3,
                "reason": ["Last check: Wed Aug 19 12:04:04 2020"],
            }
        },
    }
    params = repair_hardware_params(
        request_type=RequestTypes.MALFUNCTIONING_LINK_RX_CRC_ERRORS.type,
        operation_type=Operation.REPAIR_LINK.type,
    )
    rule = SingleCheckRule(rules.TorLinkRule(**rule_parameters))
    if rule_parameters.get("enabled", True):
        check_decision(
            incomplete_reasons,
            fast,
            WalleAction.REPAIR_HARDWARE,
            checks=[CheckType.TOR_LINK],
            reason="Errors/Drops detected on uplink ToR port",
            rule=rule,
            restrictions=[restrictions.AUTOMATED_LINK_REPAIR],
            params=params,
            hw_checks_enabled=True,
            failure_type=FailureType.LINK_MALFUNCTION,
        )
    else:
        check_decision(
            incomplete_reasons, fast, WalleAction.HEALTHY, reason="Host is healthy.", rule=rule, hw_checks_enabled=True
        )


@pytest.mark.parametrize("incomplete_reasons", [_INCOMPLETE_REASONS, _COMPLETE_REASONS])
def test_failed_ib_link_check(incomplete_reasons, fast, enable_hw_checks):
    incomplete_reasons[CheckType.IB_LINK] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "failed_checks": [
                {
                    "metadata": "{\"status\":\"CRIT\",\"timestamp\":1636515859.4462387562,\"reason\":\"No errs\"}",
                    "port": "mlx5_0",
                    "status": "OK",
                    "status_mtime": 1632486299,
                },
                {
                    "metadata": "{\"status\":\"OK\",\"timestamp\":1636515502.1194529533,\"reason\":\"No errs\"}",
                    "port": "mlx5_1",
                    "status": "OK",
                    "status_mtime": 1632485980,
                },
            ],
            "missed_checks": [],
            "passed_checks": [],
            "reason": "Some ib_link checks are failed",
            "suspected_checks": [],
            "timestamp": 1636514509,
        },
    }
    params = repair_hardware_params(
        request_type=RequestTypes.MALFUNCTIONING_LINK_RX_CRC_ERRORS.type,
        operation_type=Operation.REPAIR_IB_LINK.type,
    )
    rule = SingleCheckRule(rules.IbLinkRule())
    check_decision(
        incomplete_reasons,
        fast,
        WalleAction.REPAIR_HARDWARE,
        checks=[CheckType.IB_LINK],
        reason="Errors detected on uplink infiniband ports: mlx5_0, mlx5_1",
        rule=rule,
        restrictions=[restrictions.AUTOMATED_LINK_REPAIR, restrictions.AUTOMATED_INFINIBAND_REPAIR],
        params=params,
        hw_checks_enabled=True,
        failure_type=FailureType.LINK_MALFUNCTION,
    )
