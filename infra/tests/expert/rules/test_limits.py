"""Tests automation limits."""
import random
from functools import partial
from itertools import count
from unittest.mock import call, ANY

import pytest

import walle.operations_log.operations as operations_log
from infra.walle.server.tests.lib.rules_util import limit_breached, limit_not_breached
from infra.walle.server.tests.lib.util import mock_status_reasons, monkeypatch_config, mock_task
from sepelib.core import config
from sepelib.core.constants import HOUR_SECONDS
from sepelib.mongo.mock import ObjectMocker
from walle import restrictions
from walle.admin_requests.constants import RequestTypes
from walle.clients.eine import ProfileMode
from walle.expert.automation import PROJECT_DNS_AUTOMATION
from walle.expert.automation.project_automation import _Automation
from walle.expert.decision import Decision
from walle.expert.decisionmakers import ModernDecisionMaker, BasicDecisionMaker
from walle.expert.failure_types import FailureType
from walle.expert.rules import escalation, CheckCpuThermalHWW, SingleCheckRule
from walle.expert.types import WalleAction, CheckType, CheckStatus, HwWatcherCheckStatus, CheckSets
from walle.hosts import Host, HostStatus, HealthStatus, HostLocation, TaskType
from walle.models import monkeypatch_timestamp, timestamp
from walle.operations_log.constants import Operation
from walle.projects import Project
from walle.util.limits import parse_timed_limits


def mock_host(status=HostStatus.READY, project=None, next_inv=partial(next, count(1)), **kwargs):
    kwargs.setdefault("inv", next_inv())
    kwargs.setdefault("name", "mock-host")
    kwargs.setdefault("location", HostLocation())
    host = Host(status=status, **kwargs)

    if project is None:
        # Default host project does not support profile.
        # This is intentional, so that you don't forget to test this case.
        project = Project(id="project-id-mock")
    host.project = project.id
    host.get_project = lambda fields: project

    return host


def mock_project_with_profile():
    return Project(id="mock-project", profile="profile-mock", vlan_scheme="vlan-scheme-mock")


def get_modern_decision_maker(project, enabled_checks=None):
    return ModernDecisionMaker.for_modern_automation_plot(project)(project, enabled_checks=enabled_checks)


def monkeypatch_check_limits(mp, operation_to_result_map):
    return mp.function(
        operations_log.check_limits,
        module=escalation,
        side_effect=lambda host, operation, limits, params: operation_to_result_map[operation.type](params)
        if callable(operation_to_result_map[operation.type])
        else operation_to_result_map[operation.type],
    )


def monkeypatch_limits(mp, operation_limit_map):
    parsed_limits = {}
    for limit_name, limit_config in operation_limit_map.items():
        mp.setitem(config.get_value("automation.host_limits"), limit_name, limit_config)
        parsed_limits[limit_name] = parse_timed_limits(limit_config)

    return parsed_limits


def _create_modern_automation_plot_decision_maker(project, checks):
    return ModernDecisionMaker.for_modern_automation_plot(project)(project, checks)


@pytest.mark.parametrize(
    "decisionmaker_class",
    [
        BasicDecisionMaker,
        _create_modern_automation_plot_decision_maker,
    ],
)
class TestAvailabilityEscalation:
    def test_too_many_reboots_escalate_to_profile(self, walle_test, mp, decisionmaker_class):
        monkeypatch_config(mp, "automation.host_limits.max_host_reboots", [{"period": "1h", "limit": 1}])

        monkeypatch_check_limits(
            mp,
            {
                Operation.REBOOT.type: limit_breached,
                Operation.PROFILE.type: limit_not_breached,
                Operation.REDEPLOY.type: limit_not_breached,
            },
        )

        project = mock_project_with_profile()
        decision_maker = decisionmaker_class(project, CheckSets.BASIC)

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(mock_host(project=project), reasons)

        expected_reason = "Host is not available: unreachable, ssh. We tried to reboot the host (limits msg mock)."
        assert decision == Decision(
            WalleAction.PROFILE,
            reason=expected_reason,
            checks=CheckType.ALL_AVAILABILITY,
            failure_type=FailureType.AVAILABILITY,
        )

    def test_too_many_profiles_escalate_to_redeploy(self, mp, decisionmaker_class):
        monkeypatch_config(mp, "automation.host_limits.max_host_reboots", [{"period": "1h", "limit": 1}])
        monkeypatch_config(mp, "automation.host_limits.max_host_profiles", [{"period": "1h", "limit": 1}])

        monkeypatch_check_limits(
            mp,
            {
                Operation.REBOOT.type: limit_breached,
                Operation.PROFILE.type: limit_breached,
                Operation.REDEPLOY.type: limit_not_breached,
            },
        )

        project = mock_project_with_profile()
        decision_maker = decisionmaker_class(project, CheckSets.BASIC)

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(mock_host(project=project), reasons)

        expected_reason = (
            "Host is not available: unreachable, ssh."
            " We tried to reboot the host (limits msg mock)."
            " We tried to profile the host (limits msg mock)."
        )
        assert decision == Decision(
            WalleAction.REDEPLOY,
            reason=expected_reason,
            checks=CheckType.ALL_AVAILABILITY,
            failure_type=FailureType.AVAILABILITY,
        )

    def test_automated_profile_disabled_escalate_to_redeploy(self, mp, decisionmaker_class):
        monkeypatch_config(mp, "automation.host_limits.max_host_reboots", [{"period": "1h", "limit": 1}])
        monkeypatch_config(mp, "automation.host_limits.max_host_profiles", [{"period": "1h", "limit": 1}])

        monkeypatch_check_limits(
            mp,
            {
                Operation.REBOOT.type: limit_breached,
                Operation.PROFILE.type: limit_not_breached,
                Operation.REDEPLOY.type: limit_not_breached,
            },
        )

        host = mock_host()
        decision_maker = decisionmaker_class(host.get_project(()), CheckSets.BASIC)

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "Host is not available: unreachable, ssh."
            " We tried to reboot the host (limits msg mock)."
            " 'project-id-mock' project doesn't have a default profile,"
            " so automatic profiling can't be used."
        )
        assert decision == Decision(
            WalleAction.REDEPLOY,
            reason=expected_reason,
            checks=CheckType.ALL_AVAILABILITY,
            failure_type=FailureType.AVAILABILITY,
        )

    def test_redeploy_escalate_to_extra_highload_if_task_has_not_helped(self, walle_test, mp, decisionmaker_class):
        project = mock_project_with_profile()
        decision_maker = decisionmaker_class(project, CheckSets.BASIC)

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        host = mock_host(
            project=project, status=Operation.REDEPLOY.host_status, task=mock_task(type=TaskType.AUTOMATED_HEALING)
        )
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = "Redeploying hasn't helped: Host is not available: unreachable, ssh."

        expected_decision = Decision(
            WalleAction.PROFILE,
            expected_reason,
            {"profile_mode": ProfileMode.DANGEROUS_HIGHLOAD_TEST},
            checks=CheckType.ALL_AVAILABILITY,
            restrictions=[restrictions.AUTOMATED_PROFILE, restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP],
        )
        assert decision == expected_decision

    @pytest.mark.parametrize("dns_automation_enabled", (True, False))
    def test_failed_extra_highload_escalates_to_exp(self, walle_test, mp, decisionmaker_class, dns_automation_enabled):
        enabled_automation_mock = mp.method(
            _Automation.enabled_for_project_id, return_value=dns_automation_enabled, obj=_Automation
        )
        monkeypatch_config(
            mp, "automation.host_limits.max_host_extra_highload_profiles", [{"period": "2w", "limit": 1}]
        )

        monkeypatch_check_limits(
            mp,
            {
                Operation.PROFILE.type: limit_breached,  # This breach affects any OperationType.PROFILE
            },
        )

        project = mock_project_with_profile()
        decision_maker = decisionmaker_class(project, CheckSets.BASIC)

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        host = mock_host(
            project=project, status=Operation.REDEPLOY.host_status, task=mock_task(type=TaskType.AUTOMATED_HEALING)
        )
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "Host passes profile successfully but the problem does not go away."
            " Redeploying hasn't helped: Host is not available: unreachable, ssh."
            " We tried to profile the host (limits msg mock)."
        )

        if dns_automation_enabled:
            expected_decision = Decision(
                WalleAction.REPAIR_HARDWARE,
                expected_reason,
                failure_type=FailureType.SECOND_TIME_NODE,
                params={
                    "request_type": RequestTypes.SECOND_TIME_NODE.type,
                    "operation": Operation.REPORT_SECOND_TIME_NODE.type,
                    "redeploy": True,
                    "power_on_before_repair": True,
                },
                checks=CheckType.ALL_AVAILABILITY,
                restrictions=[restrictions.AUTOMATED_PROFILE, restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP],
            )
        else:
            expected_decision = Decision(WalleAction.DEACTIVATE, expected_reason + " Project DNS automation is off")

        assert decision == expected_decision
        enabled_automation_mock.assert_called_once_with(PROJECT_DNS_AUTOMATION, project.id)

    def test_after_change_memory_escalate_to_profile_and_ignore_limits(self, mp, decisionmaker_class):
        monkeypatch_config(mp, "automation.host_limits.max_host_reboots", [{"period": "1h", "limit": 1}])
        monkeypatch_config(mp, "automation.host_limits.max_host_profiles", [{"period": "1h", "limit": 1}])

        monkeypatch_check_limits(
            mp,
            {
                Operation.REBOOT.type: limit_breached,
                Operation.PROFILE.type: limit_breached,
                Operation.REDEPLOY.type: limit_not_breached,
            },
        )

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)

        host = mock_host(status=Operation.CHANGE_MEMORY.host_status, project=mock_project_with_profile())
        decision_maker = decisionmaker_class(host.get_project(()), CheckSets.BASIC)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "It seems that processed memory change operation killed the node: "
            "Host is not available: unreachable, ssh."
        )
        assert decision == Decision(
            WalleAction.PROFILE, reason=expected_reason, checks=CheckType.ALL_AVAILABILITY, params={"final": True}
        )

    def test_redeploy_escalate_to_extra_profile_if_all_limits_reached(self, walle_test, mp, decisionmaker_class):
        monkeypatch_timestamp(mp, cur_time=1)
        monkeypatch_config(mp, "automation.host_limits.max_host_reboots", [{"period": "1h", "limit": 1}])
        monkeypatch_config(mp, "automation.host_limits.max_host_profiles", [{"period": "1h", "limit": 1}])

        def limit_breached_on_regular_profile(params):
            if params == {"modes": ProfileMode.DANGEROUS_HIGHLOAD_TEST}:
                return limit_not_breached
            else:
                return limit_breached

        monkeypatch_check_limits(
            mp,
            {
                Operation.REBOOT.type: limit_breached,
                Operation.PROFILE.type: limit_breached_on_regular_profile,
            },
        )

        project = mock_project_with_profile()
        host = mock_host(project=project)
        oplog = ObjectMocker(operations_log.OperationLog)

        oplog.mock(
            dict(
                id="1.1",
                type=Operation.REDEPLOY.type,
                time=1,
                audit_log_id="x",
                host_inv=host.inv,
                host_name=host.name,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        )

        decision_maker = decisionmaker_class(project, CheckSets.BASIC)

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            'Host is not available: unreachable, ssh. '
            'We tried to reboot the host (limits msg mock). '
            'We tried to profile the host (limits msg mock). '
            'We already tried to redeploy the host .'
        )

        expected_decision = Decision(
            WalleAction.PROFILE,
            expected_reason,
            {"profile_mode": ProfileMode.DANGEROUS_HIGHLOAD_TEST},
            checks=CheckType.ALL_AVAILABILITY,
            restrictions=[restrictions.AUTOMATED_PROFILE, restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP],
        )
        assert decision == expected_decision


def test_link_repair_escalate_to_deactivate(mp):
    parsed_limits = monkeypatch_limits(mp, {"max_repaired_links": [{"period": "1h", "limit": 1}]})
    mock_check_limit = mp.function(operations_log.check_limits, module=escalation, return_value=limit_breached)

    host = mock_host()
    decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.LINK})

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    decision = decision_maker.make_decision(host, reasons)

    expected_reason = "Link check failed. hw-watcher: Reason mock. We tried to repair link (limits msg mock)."
    assert decision == Decision.deactivate(expected_reason)
    mock_check_limit.assert_called_once_with(
        host, Operation.REPAIR_LINK, parsed_limits["max_repaired_links"], params=None
    )


class TestChangeMemoryEscalation:
    def test_escalate_change_memory_to_profile_when_limit_reached(self, mp):
        mem_change_fail_limits = [{"period": "1h", "limit": 1}]
        mem_change_limits = [{"period": "1h", "limit": 2}]
        profile_limits = [{"period": "1h", "limit": 3}]

        monkeypatch_config(mp, "automation.host_limits.max_change_memory_failures", mem_change_fail_limits)
        monkeypatch_config(mp, "automation.host_limits.max_changed_memory", mem_change_limits)
        monkeypatch_config(mp, "automation.host_limits.max_memory_profiles", profile_limits)

        check_limits_mock = monkeypatch_check_limits(
            mp,
            {
                Operation.CHANGE_MEMORY.type: limit_breached,
                Operation.PROFILE.type: limit_not_breached,
            },
        )

        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.MEMORY})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "hw-watcher: Reason mock ecc error"
            " Platform not supported repair memory"
            " We tried to change memory (limits msg mock)."
        )
        assert decision == Decision(
            WalleAction.PROFILE,
            reason=expected_reason,
            checks=[CheckType.MEMORY],
            restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE, restrictions.AUTOMATED_REDEPLOY],
            params={"profile_mode": ProfileMode.HIGHLOAD_TEST},
        )

        assert check_limits_mock.mock_calls == [
            call(host, Operation.CHANGE_MEMORY, parse_timed_limits(mem_change_limits), params={"slot": 0}),
            call(host, Operation.PROFILE, parse_timed_limits(profile_limits), params=None),
        ]

    def test_escalate_repair_to_change_memory_when_limit_reached(self, mp):
        mem_repair_limits = [{"period": "1h", "limit": 1}]
        mem_change_fail_limits = [{"period": "1h", "limit": 2}]
        mem_change_limits = [{"period": "1h", "limit": 3}]
        profile_limits = [{"period": "1h", "limit": 4}]

        monkeypatch_config(mp, "automation.host_limits.max_repaired_memory", mem_repair_limits)
        monkeypatch_config(mp, "automation.host_limits.max_change_memory_failures", mem_change_fail_limits)
        monkeypatch_config(mp, "automation.host_limits.max_changed_memory", mem_change_limits)
        monkeypatch_config(mp, "automation.host_limits.max_memory_profiles", profile_limits)
        monkeypatch_config(mp, "automation.platform_support.repair-memory.platforms", [dict()])

        check_limits_mock = monkeypatch_check_limits(
            mp,
            {
                Operation.REPAIR_MEMORY.type: limit_breached,
                Operation.CHANGE_MEMORY.type: limit_not_breached,
            },
        )

        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.MEMORY})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = "hw-watcher: Reason mock ecc error We tried to repair memory (limits msg mock)."
        expected_params = {
            "request_type": RequestTypes.CORRUPTED_MEMORY.type,
            "operation": Operation.CHANGE_MEMORY.type,
            "slot": 0,
            "errors": ["Reason mock"],
            "status": HwWatcherCheckStatus.FAILED,
            "redeploy": True,
            "reboot": True,
        }
        assert decision == Decision(
            WalleAction.REPAIR_HARDWARE,
            reason=expected_reason,
            params=expected_params,
            checks=[CheckType.MEMORY],
            failure_type=FailureType.MEM_ECC,
            restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE, restrictions.AUTOMATED_REDEPLOY],
        )
        assert check_limits_mock.mock_calls == [
            call(host, Operation.REPAIR_MEMORY, parse_timed_limits(mem_repair_limits), params={"slot": 0}),
            call(host, Operation.CHANGE_MEMORY, parse_timed_limits(mem_change_limits), params={"slot": 0}),
        ]

    def test_escalate_change_memory_failure_to_profile_for_status_unknown(self, mp):
        mem_change_fail_limits = [{"period": "1h", "limit": 1}]
        mem_change_limits = [{"period": "1h", "limit": 2}]
        profile_limits = [{"period": "1h", "limit": 3}]

        monkeypatch_config(mp, "automation.host_limits.max_change_memory_failures", mem_change_fail_limits)
        monkeypatch_config(mp, "automation.host_limits.max_changed_memory", mem_change_limits)
        monkeypatch_config(mp, "automation.host_limits.max_memory_profiles", profile_limits)

        check_limits_mock = monkeypatch_check_limits(
            mp,
            {
                Operation.CHANGE_MEMORY.type: limit_breached,
                Operation.PROFILE.type: limit_not_breached,
            },
        )

        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))
        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)

        # hw watcher sets this status when previous memory change operation didn't help.
        # We should apply stricter limits.
        reasons[CheckType.MEMORY]["metadata"]["results"]["ecc"]["status"] = HwWatcherCheckStatus.UNKNOWN

        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.MEMORY})
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "hw-watcher: Reason mock ecc error"
            " Platform not supported repair memory"
            " We tried to change memory (limits msg mock)."
        )
        assert decision == Decision(
            WalleAction.PROFILE,
            reason=expected_reason,
            checks=[CheckType.MEMORY],
            restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE, restrictions.AUTOMATED_REDEPLOY],
            params={"profile_mode": ProfileMode.HIGHLOAD_TEST},
        )

        assert check_limits_mock.mock_calls == [
            call(host, Operation.CHANGE_MEMORY, parse_timed_limits(mem_change_fail_limits), params={"slot": 0}),
            call(host, Operation.PROFILE, parse_timed_limits(profile_limits), params=None),
        ]

    def test_escalate_profile_to_deactivate_when_profile_limit_reached(self, mp):
        mp.function(operations_log.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.MEMORY})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "hw-watcher: Reason mock ecc error"
            " Platform not supported repair memory"
            " We tried to change memory (limits msg mock)."
            " We tried to profile the host (limits msg mock)."
        )
        assert decision == Decision.deactivate(expected_reason)

    def test_escalate_profile_to_deactivate_when_automatic_profile_not_configured(self, mp):
        monkeypatch_check_limits(
            mp,
            {
                Operation.CHANGE_MEMORY.type: limit_breached,
                Operation.PROFILE.type: limit_not_breached,
            },
        )

        host = mock_host(status=Operation.CHANGE_MEMORY.host_status, project=Project(id="project-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.MEMORY})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "hw-watcher: Reason mock ecc error"
            " Platform not supported repair memory"
            " We tried to change memory (limits msg mock)."
            " 'project-mock' project doesn't have a default profile, so automatic profiling can't be used."
        )
        assert decision == Decision.deactivate(reason=expected_reason)


@pytest.mark.usefixtures("walle_test")
class TestCheckDiskEscalation:
    @staticmethod
    def _monkeypatch_limits(mp):
        redeploy_limits = [{"period": "1h", "limit": 1}]
        change_disk_limits = [{"period": "1h", "limit": 1}]
        fix_disk_with_profile_limits = [{"period": "1h", "limit": 2}]

        monkeypatch_config(mp, "automation.host_limits.max_changed_disks", change_disk_limits)
        monkeypatch_config(mp, "automation.host_limits.max_disk_fixes_via_profiling", fix_disk_with_profile_limits)
        monkeypatch_config(mp, "automation.host_limits.max_nvme_missing_redeployments", redeploy_limits)
        monkeypatch_config(mp, "automation.host_limits.max_nvme_missing_profiles", redeploy_limits)

        return (
            parse_timed_limits(redeploy_limits),
            parse_timed_limits(change_disk_limits),
            parse_timed_limits(fix_disk_with_profile_limits),
        )

    def test_escalate_profile_and_redeploy_to_change_disk_when_limit_reached(self, mp):
        def check_limits(h, operation, *args, **kwargs):
            if operation in [Operation.REDEPLOY, Operation.PROFILE]:
                return limit_breached
            return limit_not_breached

        redeploy_limits, change_disk_limits, fix_disk_with_profile_limits = self._monkeypatch_limits(mp)
        mock_check_limits = mp.function(operations_log.check_limits, module=escalation, side_effect=check_limits)

        host = mock_host(ipmi_mac="ipmi_mac")
        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        reasons[CheckType.DISK]["metadata"]["result"]["disk2replace"] = {
            "status": HwWatcherCheckStatus.FAILED,
            "reason": ["errors-description-mock"],
            "slot": 0,
        }
        reasons[CheckType.DISK]["metadata"]["result"]["eine_code"] = ["NVME_MISSING"]

        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.DISK})
        decision = decision_maker.make_decision(host, reasons)

        reason = (
            "Disk check failed. hw-watcher:\n* Reason mock\n* errors-description-mock"
            " We tried to profile the host (limits msg mock)."
            " We tried to redeploy the host (limits msg mock)."
        )
        params = {
            "slot": 0,
            "redeploy": True,
            "profile": True,
            "errors": ["errors-description-mock"],
            "eine_code": ["NVME_MISSING"],
        }
        assert decision == Decision.change_disk(reason, params)
        assert mock_check_limits.mock_calls == [
            call(host, Operation.PROFILE, redeploy_limits, params={'slot': 0}),
            call(host, Operation.REDEPLOY, redeploy_limits, params={'slot': 0}),
            call(host, Operation.CHANGE_DISK, redeploy_limits, params={'slot': 0}),
        ]

    def test_escalate_redeploy_to_deactivate_when_limit_reached(self, mp):
        redeploy_limits, change_disk_limits, fix_disk_with_profile_limits = self._monkeypatch_limits(mp)
        mock_check_limits = mp.function(operations_log.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host(ipmi_mac="ipmi_mac")
        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        reasons[CheckType.DISK]["metadata"]["result"]["disk2replace"] = {
            "status": HwWatcherCheckStatus.FAILED,
            "reason": ["errors-description-mock"],
            "slot": 0,
        }
        reasons[CheckType.DISK]["metadata"]["result"]["eine_code"] = ["NVME_MISSING"]

        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.DISK})
        decision = decision_maker.make_decision(host, reasons)

        reason = (
            "Disk check failed. hw-watcher:\n* Reason mock\n* errors-description-mock"
            " We tried to profile the host (limits msg mock)."
            " We tried to redeploy the host (limits msg mock)."
            " We tried to change disk (limits msg mock)."
        )

        assert decision == Decision.deactivate(reason)
        assert mock_check_limits.mock_calls == [
            call(host, Operation.PROFILE, redeploy_limits, params={'slot': 0}),
            call(host, Operation.REDEPLOY, redeploy_limits, params={'slot': 0}),
            call(host, Operation.CHANGE_DISK, redeploy_limits, params={'slot': 0}),
        ]

    def test_escalate_change_disk_to_deactivate_when_limit_reached(self, mp):
        redeploy_limits, change_disk_limits, fix_disk_with_profile_limits = self._monkeypatch_limits(mp)
        mock_check_limits = mp.function(operations_log.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host(ipmi_mac="ipmi_mac")
        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        reasons[CheckType.DISK]["metadata"]["result"]["disk2replace"] = {
            "status": HwWatcherCheckStatus.UNKNOWN,
            "reason": ["errors-description-mock"],
            "slot": 0,
        }

        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.DISK})
        decision = decision_maker.make_decision(host, reasons)

        reason = (
            "Disk check failed. hw-watcher:\n* Reason mock\n* errors-description-mock"
            " We tried to change disk (limits msg mock)."
        )
        assert decision == Decision.deactivate(reason)

        mock_check_limits.assert_called_once_with(host, Operation.CHANGE_DISK, change_disk_limits, params={'slot': 0})

    def test_escalate_repair_disk_cable_to_deactivate_when_limit_reached(self, mp):
        self._monkeypatch_limits(mp)
        cable_repair_limits = [{"period": "1d", "limit": 1}]
        monkeypatch_config(mp, "automation.host_limits.max_disk_cable_repairs", cable_repair_limits)
        mock_check_limits = mp.function(operations_log.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host()
        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        reasons[CheckType.DISK]["metadata"]["result"]["disk2replace"] = {
            "status": HwWatcherCheckStatus.UNKNOWN,
            "slot": 0,
            "reason": ["cable: SATA speed degradation 1.5 (max 6.0)"],
        }
        reasons[CheckType.DISK]["metadata"]["result"]["reason"] = ["Error mock"]

        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.DISK})
        decision = decision_maker.make_decision(host, reasons)

        reason = (
            "Disk check failed. hw-watcher:\n* Error mock\n* cable: SATA speed degradation 1.5 (max 6.0)"
            " We tried to replace disk cable (limits msg mock)."
        )
        assert decision == Decision.deactivate(reason)

        mock_check_limits.assert_called_once_with(
            host, Operation.REPAIR_DISK_CABLE, parse_timed_limits(cable_repair_limits), params={'slot': 0}
        )

    def test_escalate_profile_to_deactivate_when_limit_reached(self, mp):
        redeploy_limits, change_disk_limits, fix_disk_with_profile_limits = self._monkeypatch_limits(mp)
        mock_check_limits = mp.function(operations_log.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"), ipmi_mac="ipmi_mac")
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.DISK})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        reason = "Disk check failed. hw-watcher: Reason mock. We tried to profile the host (limits msg mock)."
        assert decision == Decision.deactivate(reason)

        mock_check_limits.assert_called_once_with(
            host, Operation.PROFILE, fix_disk_with_profile_limits, params={"modes": ProfileMode.DISK_RW_TEST}
        )

    def test_escalate_profile_to_deactivate_when_automatic_profile_not_configured(self, mp):
        mock_check_limits = mp.function(operations_log.check_limits, module=escalation, return_value=limit_not_breached)

        host = mock_host(project=Project(id="project-id-mock"), ipmi_mac="ipmi_mac")
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.DISK})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        reason = (
            "Disk check failed. hw-watcher: Reason mock."
            " 'project-id-mock' project doesn't have a default profile, so automatic profiling can't be used."
        )
        assert decision == Decision.deactivate(reason)
        assert not mock_check_limits.called


class TestBmcRepairEscalation:
    def test_escalate_to_deactivate_when_profile_does_not_help(self):
        host = mock_host(project=Project(id="project-id-mock"), status=Operation.PROFILE.host_status, task=mock_task())
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.BMC})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        bmc_status_reason = " ".join(reasons[CheckType.BMC]["metadata"]["result"]["reason"])
        reason = "Profiling hasn't helped: hw-watcher: {}".format(bmc_status_reason)

        assert decision == Decision.deactivate(reason)

    def test_escalate_to_deactivate_when_profile_limit_reached(self, mp):
        mp.function(escalation.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host(project=Project(id="project-id-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.BMC})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        reason = "hw-watcher: Reason mock We tried to profile the host (limits msg mock)."
        assert decision == Decision.deactivate(reason)

    def test_escalate_to_repair_ipmi_when_reset_bmc_didnt_help(self, mp):
        mp.function(operations_log.check_limits, module=escalation, return_value=limit_not_breached)

        host = mock_host(status=Operation.RESET_BMC.host_status, project=Project(id="project-id-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.BMC})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        reasons[CheckType.BMC]["metadata"]["result"]["reason"] = ["ipmi: broken thing doesn't work at all."]
        decision = decision_maker.make_decision(host, reasons)

        reason = "BMC reset cold didn't help: hw-watcher: ipmi: broken thing doesn't work at all."
        assert decision == Decision(
            WalleAction.REPAIR_HARDWARE,
            reason=reason,
            checks=[CheckType.BMC],
            failure_type=FailureType.BMC_IPMI,
            restrictions=[restrictions.AUTOMATED_BMC_REPAIR],
            params={"request_type": RequestTypes.IPMI_UNREACHABLE.type, "operation": Operation.REPAIR_BMC.type},
        )

    def test_escalate_to_repair_ipmi_when_bmc_reset_limit_breached(self, mp):
        mp.function(
            operations_log.check_limits,
            module=escalation,
            side_effect=[limit_breached, limit_not_breached, limit_not_breached],
        )

        host = mock_host(project=Project(id="project-id-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.BMC})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        reasons[CheckType.BMC]["metadata"]["result"]["reason"] = ["ipmi: broken thing doesn't work at all."]
        decision = decision_maker.make_decision(host, reasons)

        reason = "hw-watcher: ipmi: broken thing doesn't work at all. We tried to reset BMC (limits msg mock)."
        assert decision == Decision(
            WalleAction.REPAIR_HARDWARE,
            reason=reason,
            checks=[CheckType.BMC],
            failure_type=FailureType.BMC_IPMI,
            restrictions=[restrictions.AUTOMATED_BMC_REPAIR],
            params={"request_type": RequestTypes.IPMI_UNREACHABLE.type, "operation": Operation.REPAIR_BMC.type},
        )

    def test_escalate_to_deactivate_when_bmc_repair_limit_breached(self, mp):
        mp.function(operations_log.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host(project=Project(id="project-id-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.BMC})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        reasons[CheckType.BMC]["metadata"]["result"]["reason"] = ["bmc: some error, don't really know what's broken"]
        decision = decision_maker.make_decision(host, reasons)

        reason = (
            "hw-watcher: bmc: some error, don't really know what's broken We tried to repair BMC (limits msg mock)."
        )
        assert decision == Decision.deactivate(reason)


class TestGpuEscalation:
    def test_escalate_to_report_when_gpu_repair_limit_reached(self, mp):
        mp.function(escalation.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host(project=Project(id="project-id-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.GPU})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        hw_watcher_error_message = "availability: less local GPUs 8 than in bot 9"
        reasons[CheckType.GPU]["metadata"]["result"] = {
            "reason": [hw_watcher_error_message],
            "eine_code": ["GPU_MISSING"],
        }
        decision = decision_maker.make_decision(host, reasons)

        reason = (
            "GPU check failed. hw-watcher: "
            "availability: less local GPUs 8 than in bot 9. We tried to repair GPU (limits msg mock)."
        )

        expected_decision = Decision(
            WalleAction.REPORT_FAILURE,
            params={
                "request_type": RequestTypes.GPU_MISSING.type,
                "operation": Operation.REPAIR_GPU.type,
                "errors": [hw_watcher_error_message],
                "eine_code": ["GPU_MISSING"],
            },
            failure_type=FailureType.GPU_MISSING,
            restrictions=[restrictions.AUTOMATED_GPU_REPAIR],
            checks=[CheckType.GPU],
            reason=reason,
        )

        assert decision == expected_decision

    def test_escalate_gpu_failed_to_repair_hardware(self, mp):
        host = mock_host(project=Project(id="project-id-mock"), status=Operation.PROFILE.host_status, task=mock_task())
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.GPU})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        decision.params["operation"] = Operation.REPAIR_GPU.type

        assert decision == Decision(
            WalleAction.REPAIR_HARDWARE,
            reason=decision.reason,
            params=decision.params,
            checks=decision.checks,
            failure_type=decision.failure_type,
            restrictions=[restrictions.AUTOMATED_GPU_REPAIR],
        )


class TestCpuCachesEscalation:
    def test_escalate_to_second_time_node_when_max_extra_higload_limit_reached(self, walle_test, mp):
        mp.function(escalation.check_limits, module=escalation, return_value=limit_breached)

        host = mock_host(project=Project(id="project-id-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.CPU_CACHES})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        hw_watcher_error_message = "mcelog: Data CACHE Level-0 Eviction Error"
        reasons[CheckType.CPU_CACHES]["metadata"]["result"]["reason"] = [hw_watcher_error_message]
        decision = decision_maker.make_decision(host, reasons)

        reason = (
            "Host passes profile successfully but the problem does not go away. "
            "cpu check failed. hw-watcher: mcelog: Data CACHE Level-0 Eviction Error. "
            "We tried to repair CPU (limits msg mock). We tried to profile the host (limits msg mock)."
        )

        expected_decision = Decision(
            WalleAction.REPAIR_HARDWARE,
            params={
                'request_type': RequestTypes.SECOND_TIME_NODE.type,
                'operation': Operation.REPORT_SECOND_TIME_NODE.type,
                'redeploy': True,
                "power_on_before_repair": True,
            },
            restrictions=[restrictions.AUTOMATED_CPU_REPAIR, restrictions.AUTOMATED_REDEPLOY],
            checks=[CheckType.CPU_CACHES],
            reason=reason,
            failure_type=FailureType.SECOND_TIME_NODE,
        )
        assert decision == expected_decision


def mock_decision_for_checks(host, enabled_checks):
    decision_maker = get_modern_decision_maker(host.get_project(()), enabled_checks)
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)

    return decision_maker.make_decision(host, reasons)


class TestOverheatEscalation:
    @staticmethod
    def make_failed_cpu_check():
        return {
            "status": CheckStatus.FAILED,
            "metadata": {
                "result": {
                    "eine_code": ["CPU_OVERHEATING"],
                    "reason": ["Reason mock"],
                }
            },
        }

    def test_escalate_to_overheat_repair_when_profile_limit_reached(self, mp, walle_test):
        parsed_limits = monkeypatch_limits(
            mp,
            {
                "max_overheat_profiles": [{"period": "1d", "limit": 1}],
                "max_repaired_overheats": [{"period": "1d", "limit": 2}],
            },
        )

        check_limits_mock = mp.function(
            operations_log.check_limits, module=escalation, side_effect=[limit_breached, limit_not_breached]
        )
        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))

        rule = SingleCheckRule(CheckCpuThermalHWW())
        mock_reasons = {CheckType.CPU_CACHES: self.make_failed_cpu_check()}
        decision = rule.apply(host, mock_reasons, {CheckType.CPU_CACHES})

        expected_reason = "cpu check failed. hw-watcher: Reason mock. We tried to profile the host (limits msg mock)."

        expected_params = {
            "request_type": RequestTypes.CPU_OVERHEATED.type,
            "operation": Operation.REPAIR_OVERHEAT.type,
            "reboot": True,
        }

        assert decision == Decision(
            WalleAction.REPAIR_HARDWARE,
            reason=expected_reason,
            checks=[CheckType.CPU_CACHES],
            restrictions=[restrictions.AUTOMATED_OVERHEAT_REPAIR],
            failure_type=FailureType.CPU_OVERHEATED,
            params=expected_params,
        )

        profile_params = {"modes": ProfileMode.HIGHLOAD_TEST}
        assert check_limits_mock.mock_calls == [
            call(host, Operation.PROFILE, parsed_limits["max_overheat_profiles"], params=profile_params),
            call(host, Operation.REPAIR_OVERHEAT, parsed_limits["max_repaired_overheats"], params=None),
        ]

    def test_escalate_to_overheat_repair_when_profile_did_not_help(self, mp):
        parsed_limits = monkeypatch_limits(
            mp,
            {
                "max_overheat_profiles": [{"period": "1d", "limit": 1}],
                "max_repaired_overheats": [{"period": "1d", "limit": 2}],
            },
        )

        check_limits_mock = mp.function(
            operations_log.check_limits, module=escalation, side_effect=[limit_not_breached, limit_not_breached]
        )
        host = mock_host(
            project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"),
            status=Operation.PROFILE.host_status,
            task=mock_task(),
        )

        rule = SingleCheckRule(CheckCpuThermalHWW())
        mock_reasons = {CheckType.CPU_CACHES: self.make_failed_cpu_check()}
        decision = rule.apply(host, mock_reasons, {CheckType.CPU_CACHES})

        expected_reason = "Profile didn't help: cpu check failed. hw-watcher: Reason mock."
        expected_params = {
            "request_type": RequestTypes.CPU_OVERHEATED.type,
            "operation": Operation.REPAIR_OVERHEAT.type,
            "reboot": True,
        }

        assert decision == Decision(
            WalleAction.REPAIR_HARDWARE,
            reason=expected_reason,
            checks=[CheckType.CPU_CACHES],
            restrictions=[restrictions.AUTOMATED_OVERHEAT_REPAIR],
            failure_type=FailureType.CPU_OVERHEATED,
            params=expected_params,
        )

        assert check_limits_mock.mock_calls == [
            call(host, Operation.REPAIR_OVERHEAT, parsed_limits["max_repaired_overheats"], params=None),
        ]

    def test_escalate_to_deactivate_when_overheat_repair_limit_reached(self, mp):
        parsed_limits = monkeypatch_limits(
            mp,
            {
                "max_overheat_profiles": [{"period": "1d", "limit": 1}],
                "max_repaired_overheats": [{"period": "1d", "limit": 2}],
                "max_host_extra_highload_profiles": [{"period": "1d", "limit": 2}],
            },
        )

        check_limits_mock = mp.function(
            operations_log.check_limits,
            module=escalation,
            side_effect=[limit_breached, limit_breached, limit_not_breached],
        )
        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))

        rule = SingleCheckRule(CheckCpuThermalHWW())
        mock_reasons = {CheckType.CPU_CACHES: self.make_failed_cpu_check()}
        decision = rule.apply(host, mock_reasons, {CheckType.CPU_CACHES})

        expected_reason = (
            "cpu check failed. hw-watcher: Reason mock."
            " We tried to profile the host (limits msg mock)."
            " We tried to repair CPU overheat (limits msg mock)."
        )

        assert decision == Decision(
            WalleAction.PROFILE,
            reason=expected_reason,
            checks=[CheckType.CPU_CACHES],
            restrictions=[restrictions.AUTOMATED_OVERHEAT_REPAIR],
            params={"profile_mode": ProfileMode.EXTRA_HIGHLOAD_TEST},
        )

        highload_profile_params = {"modes": ProfileMode.HIGHLOAD_TEST}
        extra_highload_profile_params = {"modes": ProfileMode.EXTRA_HIGHLOAD_TEST}
        assert check_limits_mock.mock_calls == [
            call(host, Operation.PROFILE, parsed_limits["max_overheat_profiles"], params=highload_profile_params),
            call(host, Operation.REPAIR_OVERHEAT, parsed_limits["max_repaired_overheats"], params=None),
            call(
                host,
                Operation.PROFILE,
                parsed_limits["max_host_extra_highload_profiles"],
                params=extra_highload_profile_params,
            ),
        ]


class TestTaintedKernelEscalation:
    def test_escalate_to_profile_when_reboot_limit_reached(self, mp):
        reboots_limit = [{"period": "1h", "limit": 2}]

        monkeypatch_config(mp, "automation.host_limits.max_host_reboots", reboots_limit)

        check_limits_mock = mp.function(
            operations_log.check_limits,
            module=escalation,
            side_effect=[limit_breached, limit_not_breached, limit_not_breached],
        )

        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.TAINTED_KERNEL})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = "Kernel on host has been tainted: Reason mock. We tried to reboot the host (limits msg mock)."
        assert decision == Decision(
            WalleAction.PROFILE,
            reason=expected_reason,
            checks=[CheckType.TAINTED_KERNEL],
            params={"profile_mode": ProfileMode.HIGHLOAD_TEST},
        )

        assert check_limits_mock.mock_calls == [
            call(host, Operation.REBOOT, parse_timed_limits(reboots_limit), params=None),
            call(host, Operation.PROFILE, ANY, params=ANY),
        ]

    def test_escalate_to_profile_when_tainted_kernel_reboots_limit_reached(self, mp):
        reboots_limit = [{"period": "1h", "limit": 2}]
        tainted_kernel_limit = [{"period": "1h", "limit": 1}]

        monkeypatch_config(mp, "automation.host_limits.max_host_reboots", reboots_limit)
        monkeypatch_config(mp, "automation.host_limits.max_host_tainted_kernel_reboots", tainted_kernel_limit)

        check_limits_mock = mp.function(
            operations_log.check_limits,
            module=escalation,
            side_effect=[limit_not_breached, limit_breached, limit_not_breached],
        )

        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.TAINTED_KERNEL})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = "Kernel on host has been tainted: Reason mock. We tried to reboot the host (limits msg mock)."
        assert decision == Decision(
            WalleAction.PROFILE,
            reason=expected_reason,
            checks=[CheckType.TAINTED_KERNEL],
            params={"profile_mode": ProfileMode.HIGHLOAD_TEST},
        )

        assert check_limits_mock.mock_calls == [
            call(host, Operation.REBOOT, parse_timed_limits(reboots_limit), params=None),
            call(host, Operation.REBOOT, parse_timed_limits(tainted_kernel_limit), params=None),
            call(host, Operation.PROFILE, ANY, params=ANY),
        ]

    def test_escalate_profile_to_deactivate_when_profile_limit_reached(self, mp):
        reboots_limit = [{"period": "1h", "limit": 2}]
        tainted_kernel_limit = [{"period": "1h", "limit": 1}]
        profile_limit = [{"period": "1h", "limit": 3}]

        monkeypatch_config(mp, "automation.host_limits.max_host_reboots", reboots_limit)
        monkeypatch_config(mp, "automation.host_limits.max_host_tainted_kernel_reboots", tainted_kernel_limit)
        monkeypatch_config(mp, "automation.host_limits.max_host_profiles", profile_limit)

        check_limits_mock = monkeypatch_check_limits(
            mp,
            {
                Operation.REBOOT.type: limit_breached,
                Operation.PROFILE.type: limit_breached,
            },
        )

        host = mock_host(project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.TAINTED_KERNEL})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "Kernel on host has been tainted: Reason mock."
            " We tried to reboot the host (limits msg mock)."
            " We tried to profile the host (limits msg mock)."
        )
        assert decision == decision.deactivate(expected_reason)
        assert check_limits_mock.mock_calls == [
            call(host, Operation.REBOOT, parse_timed_limits(reboots_limit), params=None),
            call(
                host, Operation.PROFILE, parse_timed_limits(profile_limit), params={"modes": ProfileMode.HIGHLOAD_TEST}
            ),
        ]

    def test_escalate_profile_to_deactivate_when_automatic_profile_not_configured(self, mp):
        monkeypatch_check_limits(
            mp,
            {
                Operation.REBOOT.type: limit_breached,
                Operation.PROFILE.type: limit_not_breached,
            },
        )

        host = mock_host(project=Project(id="project-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), {CheckType.TAINTED_KERNEL})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = decision_maker.make_decision(host, reasons)

        expected_reason = (
            "Kernel on host has been tainted: Reason mock."
            " We tried to reboot the host (limits msg mock)."
            " 'project-mock' project doesn't have a default profile, so automatic profiling can't be used."
        )
        assert decision == Decision.deactivate(reason=expected_reason)


class TestMissingChecksEscalation:
    @staticmethod
    def _mock_health_reasons():
        hw_checks = [
            {
                "type": check_type,
                "status": random.choice([CheckStatus.MISSING, CheckStatus.STALED]),
                "stale_timestamp": timestamp() - 12 * HOUR_SECONDS,
                "status_mtime": timestamp() - 12 * HOUR_SECONDS,
            }
            for check_type in CheckType.ALL_HW_WATCHER
        ]

        walle_meta_check = {
            "type": CheckType.W_META,
            "status": CheckStatus.PASSED,
            "stale_timestamp": timestamp(),
            "status_mtime": timestamp() - HOUR_SECONDS,
        }

        return mock_status_reasons(check_status=CheckStatus.MISSING, check_overrides=hw_checks + [walle_meta_check])

    def test_escalate_to_redeploy_when_reboot_does_not_help(self, mp):
        mp.function(operations_log.check_limits, module=escalation, return_value=limit_not_breached)

        host = mock_host(project=Project(id="project-id-mock"), status=Operation.REBOOT.host_status, task=mock_task())
        decision_maker = get_modern_decision_maker(host.get_project(()), CheckType.ALL_HW_WATCHER)

        decision = decision_maker.make_decision(host, self._mock_health_reasons())

        reason = "Reboot hasn't helped: All hw-watcher checks are missing."
        assert decision == Decision(WalleAction.REDEPLOY, checks=CheckType.ALL_HW_WATCHER, reason=reason)

    def test_escalate_to_deactivate_when_redeploy_does_not_help(self, mp):
        mp.function(operations_log.check_limits, module=escalation, return_value=limit_not_breached)

        host = mock_host(project=Project(id="project-id-mock"), status=Operation.REDEPLOY.host_status, task=mock_task())
        decision_maker = get_modern_decision_maker(host.get_project(()), CheckType.ALL_HW_WATCHER)

        decision = decision_maker.make_decision(host, self._mock_health_reasons())

        reason = "Redeploying hasn't helped: All hw-watcher checks are missing."
        assert decision == Decision.deactivate(reason)

    def test_escalate_to_redeploy_when_reboot_limit_reached(self, mp):
        mp.function(operations_log.check_limits, module=escalation, side_effect=[limit_breached, limit_not_breached])

        host = mock_host(project=Project(id="project-id-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), CheckType.ALL_HW_WATCHER)

        decision = decision_maker.make_decision(host, self._mock_health_reasons())

        reason = "All hw-watcher checks are missing. We tried to reboot the host (limits msg mock)."
        assert decision == Decision(WalleAction.REDEPLOY, checks=CheckType.ALL_HW_WATCHER, reason=reason)

    def test_escalate_to_deactivate_when_redeploy_limit_reached(self, mp):
        mp.function(operations_log.check_limits, module=escalation, side_effect=[limit_breached, limit_breached])

        host = mock_host(project=Project(id="project-id-mock"))
        decision_maker = get_modern_decision_maker(host.get_project(()), CheckType.ALL_HW_WATCHER)

        decision = decision_maker.make_decision(host, self._mock_health_reasons())

        reason = (
            "All hw-watcher checks are missing."
            " We tried to reboot the host (limits msg mock)."
            " We tried to redeploy the host (limits msg mock)."
        )
        assert decision == Decision.deactivate(reason)


@pytest.mark.parametrize(
    ["check_type", "limit_name", "operation_type"],
    {
        (CheckType.TAINTED_KERNEL, "max_host_reboots", Operation.REBOOT.type),
        (CheckType.CPU, "max_repaired_cpu", Operation.REPAIR_CPU.type),
    },
)
def test_escalate_repair_decision_to_deactivate(mp, check_type, limit_name, operation_type):

    parsed_limits = monkeypatch_limits(mp, {limit_name: [{"period": "1h", "limit": 1}]})
    mock_check_limit = mp.function(operations_log.check_limits, module=escalation, return_value=limit_breached)

    host = mock_host()
    decision_maker = get_modern_decision_maker(host.get_project(()), {check_type})

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    decision = decision_maker.make_decision(host, reasons)

    assert decision == Decision.deactivate(ANY)
    operation = Operation(operation_type)
    mock_check_limit.assert_called_once_with(host, operation, parsed_limits[limit_name], params=None)


def test_escalate_capping_to_deactivate_when_reboot_has_not_helped(walle_test, mp):
    parsed_limits = monkeypatch_limits(
        mp,
        {
            "max_repaired_cappings": [{"period": "1d", "limit": 2}],
        },
    )

    check_limits_mock = mp.function(
        operations_log.check_limits, module=escalation, side_effect=[limit_not_breached, limit_not_breached]
    )
    host = mock_host(
        project=Project(profile="profile-mock", vlan_scheme="vlan-scheme-mock"),
        status=Operation.REBOOT.host_status,
        task=mock_task(),
    )
    decision = mock_decision_for_checks(host, {CheckType.CPU_CAPPING})

    expected_reason = "Reboot hasn't helped: CPU capping detected: {}.".format(['Reason mock'])
    profile_params = {"request_type": "cpu-capped", "operation": "repair-cpu-capping", "reboot": True}
    assert decision == Decision(
        WalleAction.REPAIR_HARDWARE,
        reason=expected_reason,
        checks=[CheckType.CPU_CAPPING],
        failure_type=FailureType.CPU_CAPPED,
        params=profile_params,
        restrictions=["automated-capping-repair"],
    )

    assert check_limits_mock.mock_calls == [
        call(host, Operation.REPAIR_CAPPING, parsed_limits["max_repaired_cappings"], params=None),
    ]
