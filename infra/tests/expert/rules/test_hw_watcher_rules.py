"""Tests hw_watcher healing rules."""

import random

import pytest

import walle.operations_log.operations
from infra.walle.server.tests.expert.rules.util import check_decision, fast, make_decision
from infra.walle.server.tests.lib.util import TestCase, monkeypatch_config
from sepelib.core.constants import HOUR_SECONDS
from walle import restrictions
from walle.admin_requests.constants import RequestTypes
from walle.clients.eine import ProfileMode
from walle.constants import HostType
from walle.expert import rules
from walle.expert.constants import HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY
from walle.expert.decision import Decision
from walle.expert.failure_types import FailureType
from walle.expert.rules import SingleCheckRule, escalation
from walle.expert.rules.hw_watcher_rules.check_infiniband import INFINIBAND_EINE_CODE
from walle.expert.rules.hw_watcher_rules.util import get_reason_from_hw_watcher
from walle.expert.rules.utils import _should_be_disabled_check
from walle.expert.types import WalleAction, CheckType, CheckStatus, HwWatcherCheckStatus, get_walle_check_type
from walle.hosts import Host
from walle.models import monkeypatch_timestamp, timestamp
from walle.operations_log.constants import Operation
from walle.projects import Project
from walle.util.misc import drop_none


def should_be_disabled_mock(host, check_type):
    return False


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


def _get_complete_reasons():
    return {
        check_type: {
            "status": CheckStatus.PASSED,
            "effective_timestamp": timestamp(),
            "status_mtime": timestamp() - HOUR_SECONDS,
        }
        for check_type in CheckType.ALL
    }


def _get_incomplete_reasons():
    return {
        check_type: {
            "status": CheckStatus.MISSING if check_type in CheckType.ALL_NETMON else CheckStatus.PASSED,
            "effective_timestamp": timestamp(),
            "status_mtime": timestamp() - HOUR_SECONDS,
        }
        for check_type in CheckType.ALL
    }


def _insert_or_drop(d, key, value, func=lambda v: bool(v)):
    if func(value):
        d[key] = value
    else:
        d.pop(key, None)


def _add_decision_params(decision, **params):
    kwargs = decision.to_dict()
    kwargs["params"] = drop_none(dict(kwargs.get("params") or {}, **params))

    return Decision(**kwargs)


class TestMissingHwWatcherChecks:
    @staticmethod
    def _make_all_hw_checks_missing(reasons):
        for check_type in CheckType.ALL_HW_WATCHER:
            reasons[check_type] = {
                "status": random.choice([CheckStatus.MISSING, CheckStatus.STALED]),
                "stale_timestamp": timestamp() - 12 * HOUR_SECONDS,
                "status_mtime": timestamp() - 12 * HOUR_SECONDS,
            }

    @staticmethod
    def _make_hw_checks_invalid(reasons, count=len(CheckType.ALL_HARDWARE)):
        for check_type in CheckType.ALL_HARDWARE:
            status = CheckStatus.PASSED
            if count > 0:
                status = CheckStatus.INVALID
                count -= 1
            reasons[check_type] = {
                "status": status,
                "stale_timestamp": timestamp() - 12 * HOUR_SECONDS,
                "status_mtime": timestamp() - 12 * HOUR_SECONDS,
            }

    @pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
    def test_failing_checks_cause_reboot(self, incomplete_reasons, enable_hw_checks):
        self._make_all_hw_checks_missing(incomplete_reasons)
        rule = rules.MissingHwChecksRule()

        # fast is always False because we don't hit this rule in fast mode et al.
        check_decision(
            incomplete_reasons,
            fast=False,
            action=WalleAction.REBOOT,
            checks=CheckType.ALL_HW_WATCHER,
            reason="All hw-watcher checks are missing.",
            rule=rule,
        )

    @pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
    def test_invalid_checks_cause_reboot(self, incomplete_reasons, enable_hw_checks):
        self._make_hw_checks_invalid(incomplete_reasons)
        rule = rules.MissingHwChecksRule()

        # fast is always False because we don't hit this rule in fast mode et al.
        check_decision(
            incomplete_reasons,
            fast=False,
            action=WalleAction.REBOOT,
            checks=CheckType.ALL_HARDWARE,
            reason="Most of hardware checks are invalid.",
            rule=rule,
        )

    @pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
    @pytest.mark.parametrize("count,expected_reboot", [(4, False), (5, False), (6, True), (7, True), (8, True)])
    def test_not_all_invalid_checks_cause_reboot(self, incomplete_reasons, enable_hw_checks, count, expected_reboot):
        self._make_hw_checks_invalid(incomplete_reasons, count)
        rule = rules.MissingHwChecksRule()

        # fast is always False because we don't hit this rule in fast mode et al.
        if expected_reboot:
            check_decision(
                incomplete_reasons,
                fast=False,
                action=WalleAction.REBOOT,
                checks=CheckType.ALL_HARDWARE,
                reason="Most of hardware checks are invalid.",
                rule=rule,
            )
        else:
            check_decision(
                incomplete_reasons, fast=False, action=WalleAction.HEALTHY, reason="Host is healthy.", rule=rule
            )

    @pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
    @pytest.mark.parametrize("meta_status", CheckStatus.ALL)
    def test_produces_wait_if_meta_is_broken(self, incomplete_reasons, enable_hw_checks, meta_status):
        self._make_all_hw_checks_missing(incomplete_reasons)

        incomplete_reasons[CheckType.W_META] = {"status": meta_status}
        rule = rules.MissingHwChecksRule()
        if meta_status in {CheckStatus.PASSED, CheckStatus.FAILED}:
            check_decision(
                incomplete_reasons,
                action=WalleAction.REBOOT,
                checks=CheckType.ALL_HW_WATCHER,
                reason="All hw-watcher checks are missing.",
                fast=False,
                rule=rule,
            )
        else:
            reason = (
                "All hw-watcher checks are missing,"
                " but it's probably some other failure: walle_meta is {}.".format(meta_status)
            )
            check_decision(incomplete_reasons, action=WalleAction.WAIT, reason=reason, fast=False, rule=rule)

    @pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
    @pytest.mark.parametrize("meta_status", [CheckStatus.PASSED, CheckStatus.FAILED])
    def test_produces_wait_if_meta_is_too_fresh(self, incomplete_reasons, enable_hw_checks, meta_status):
        self._make_all_hw_checks_missing(incomplete_reasons)

        incomplete_reasons[CheckType.W_META] = {"status": meta_status, "status_mtime": timestamp()}
        reason = (
            "All hw-watcher checks are missing,"
            " but it's probably some other failure:"
            " walle_meta result is too fresh, wait hw-checks to catch up."
        )
        rule = rules.MissingHwChecksRule()
        check_decision(incomplete_reasons, action=WalleAction.WAIT, reason=reason, fast=False, rule=rule)

    @pytest.mark.parametrize("invalid", [True, False])
    def test_produces_wait_if_host_is_not_reachable(self, reasons, enable_hw_checks, invalid):
        if invalid:
            self._make_hw_checks_invalid(reasons)
        else:
            self._make_all_hw_checks_missing(reasons)

        reasons[CheckType.UNREACHABLE]["status"] = CheckStatus.FAILED
        reason = (
            "All hw-watcher checks are missing, but it's probably some other failure:"
            " Host is suspected to be unavailable: unreachable,"
            " which might be due to an infrastructure issue: switch recovering."
        )
        rule = rules.MissingHwChecksRule()
        host = Host(type=HostType.SERVER)
        check_decision(reasons, action=WalleAction.WAIT, reason=reason, fast=False, rule=rule, host=host)

    @pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
    @pytest.mark.parametrize("fresh_check", set(CheckType.ALL_HW_WATCHER) - {CheckType.INFINIBAND})
    @pytest.mark.parametrize("hw_check_status", [CheckStatus.MISSING, CheckStatus.STALED])
    def test_produces_wait_if_check_is_freshly_missing(
        self, incomplete_reasons, enable_hw_checks, fresh_check, hw_check_status
    ):
        stale_timestamp = timestamp() - 2 * HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1

        self._make_all_hw_checks_missing(incomplete_reasons)
        incomplete_reasons[fresh_check]["status"] = hw_check_status
        incomplete_reasons[fresh_check]["stale_timestamp"] = stale_timestamp
        incomplete_reasons[fresh_check]["status_mtime"] = stale_timestamp
        # for missing checks we may not have status mtime

        reason = (
            "All hw-watcher checks are missing,"
            " but it's probably some other failure:"
            " check {} has not been {} for long enough, "
            "need to wait more.".format(fresh_check, "staled" if hw_check_status == CheckStatus.STALED else "missing")
        )
        rule = rules.MissingHwChecksRule()
        check_decision(incomplete_reasons, action=WalleAction.WAIT, reason=reason, fast=False, rule=rule)

    @pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
    @pytest.mark.parametrize("void_check", set(CheckType.ALL_HW_WATCHER) - {CheckType.INFINIBAND})
    def test_void_checks_are_not_missing(self, incomplete_reasons, enable_hw_checks, void_check):
        self._make_all_hw_checks_missing(incomplete_reasons)
        incomplete_reasons[void_check]["status"] = CheckStatus.VOID

        possible_reasons = {
            msg.format(get_walle_check_type(check))
            for check in CheckType.ALL_HW_WATCHER
            for msg in ("No data for {} check result.", "{} check is staled.", "Wall-E has not received {} check.")
        }
        decision = make_decision(Host(), incomplete_reasons, fast=False)
        assert decision.action == WalleAction.WAIT
        assert decision.reason in possible_reasons


@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
@pytest.mark.parametrize("eine_code", [["EINE_CODE"], None, []])
def test_failed_cpu_caches_check(incomplete_reasons, fast, hw_checks_enabled, eine_code):
    incomplete_reasons[CheckType.CPU_CACHES] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": drop_none(
                {
                    "status": HwWatcherCheckStatus.FAILED,
                    "socket": 0,
                    "timestamp": timestamp(),
                    "eine_code": eine_code,
                    "raw": "MCE 0\nCPU 0 BANK 18\nMISC 60fe2010801a4086 ADDR 3f7f6176c0\n",
                    "reason": ["mcelog: Generic CACHE Level-2 Generic Error"],
                }
            )
        },
    }
    rule = SingleCheckRule(rules.CheckCpuCaches())

    if hw_checks_enabled:
        params = drop_none(
            {
                "slot": 0,
                "redeploy": True,
                "operation": Operation.REPAIR_CPU.type,
                "request_type": RequestTypes.CPU_FAILED.type,
                "eine_code": eine_code or None,
                "errors": [
                    "mcelog: Generic CACHE Level-2 Generic Error",
                    "MCE 0\nCPU 0 BANK 18\nMISC 60fe2010801a4086 ADDR 3f7f6176c0\n",
                ],
            }
        )
        check_decision(
            incomplete_reasons,
            fast,
            WalleAction.REPAIR_HARDWARE,
            checks=[CheckType.CPU_CACHES],
            params=params,
            rule=rule,
            failure_type=FailureType.CPU_FAILURE,
            restrictions=[restrictions.AUTOMATED_CPU_REPAIR, restrictions.AUTOMATED_REDEPLOY],
            reason="cpu check failed. hw-watcher: mcelog: Generic CACHE Level-2 Generic Error.",
        )
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
@pytest.mark.parametrize("eine_code", [["EINE_CODE"], None, []])
def test_failed_system_misconfigured_eine_code(incomplete_reasons, fast, hw_checks_enabled, eine_code):
    incomplete_reasons[CheckType.CPU_CACHES] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": drop_none(
                {
                    "status": HwWatcherCheckStatus.FAILED,
                    "socket": 0,
                    "timestamp": timestamp(),
                    "eine_code": ["SYSTEM_MISCONFIGURED"],
                    "raw": "MCE 0\nCPU 0 BANK 18\nMISC 60fe2010801a4086 ADDR 3f7f6176c0\n",
                    "reason": ["mcelog: Generic CACHE Level-2 Generic Error"],
                }
            )
        },
    }
    rule = SingleCheckRule(rules.CheckCpuCaches())

    if hw_checks_enabled:
        params = {"profile_mode": ProfileMode.HIGHLOAD_TEST}
        check_decision(
            incomplete_reasons,
            fast,
            WalleAction.PROFILE,
            checks=[CheckType.CPU_CACHES],
            params=params,
            reason="cpu check failed. hw-watcher: mcelog: Generic CACHE Level-2 Generic Error.",
            rule=rule,
            failure_type=FailureType.CPU_SYSTEM_MISCONFIGURED,
            restrictions=[restrictions.AUTOMATED_CPU_REPAIR],
        )

    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
@pytest.mark.parametrize("eine_code", [["EINE_CODE"], None, []])
def test_failed_cpu_caches_check_overheat_ignored(incomplete_reasons, fast, hw_checks_enabled, eine_code):
    incomplete_reasons[CheckType.CPU_CACHES] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": drop_none(
                {
                    "status": HwWatcherCheckStatus.FAILED,
                    "socket": None,
                    "timestamp": timestamp(),
                    "eine_code": ["CPU_OVERHEATING"],
                    "raw": "",
                    "reason": [
                        "overheat: CPU1 Core 3 temp is 86.0 (threshold 85.0)",
                        "overheat: CPU1 Core 6 temp is 89.0 (threshold 85.0)",
                        "overheat: CPU2 Core 2 temp is 88.0 (threshold 85.0)",
                        "overheat: CPU1 Core 0 temp is 85.0 (threshold 85.0)",
                        "overheat: CPU2 Core 1 temp is 88.0 (threshold 85.0)",
                    ],
                }
            )
        },
    }
    rule = SingleCheckRule(rules.CheckCpuCaches())
    check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_cpu_check(incomplete_reasons, fast, hw_checks_enabled):
    incomplete_reasons[CheckType.CPU] = {
        "status": CheckStatus.FAILED,
        "metadata": {"result": {"reason": ["offline cores"]}},
    }
    rule = SingleCheckRule(rules.CheckCpu())

    if hw_checks_enabled:
        check_decision(
            incomplete_reasons,
            fast,
            WalleAction.REPAIR_CPU,
            checks=[CheckType.CPU],
            reason="Problems with CPU detected: offline cores.",
            rule=rule,
            failure_type=FailureType.CPU_FAILURE,
        )
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_cpu_capping_check(incomplete_reasons, fast, hw_checks_enabled):
    incomplete_reasons[CheckType.CPU_CAPPING] = {
        "status": CheckStatus.FAILED,
        "metadata": {"reason": "something wrong"},
    }
    rule = SingleCheckRule(rules.CheckCpuCapping())

    if hw_checks_enabled:
        check_decision(
            incomplete_reasons,
            fast,
            WalleAction.REBOOT,
            checks=[CheckType.CPU_CAPPING],
            restrictions=[restrictions.AUTOMATED_CAPPING_REPAIR],
            reason="CPU capping detected: something wrong.",
            rule=rule,
            failure_type=FailureType.CPU_CAPPED,
        )
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize(
    ["rule", "reason", "failure_type", "request_type", "action", "operation_restrictions", "operation", "reboot"],
    [
        (
            rules.CheckBmcIpmi,
            "ipmi: broken thing does not work",
            FailureType.BMC_IPMI,
            RequestTypes.IPMI_UNREACHABLE.type,
            WalleAction.RESET_BMC,
            None,
            None,
            None,
        ),
        (
            rules.CheckBmcIpDns,
            "bmc: no IP address or DNS record",
            FailureType.BMC_IP_DNS,
            RequestTypes.IPMI_HOST_MISSING.type,
            WalleAction.REPAIR_HARDWARE,
            [restrictions.AUTOMATED_BMC_REPAIR],
            Operation.REPAIR_BMC.type,
            None,
        ),
        (
            rules.CheckBmcBattery,
            "battery: Current VBAT value 2.35 is lower threshold 2.8",
            FailureType.BMC_BATTERY,
            RequestTypes.BMC_LOW_BATTERY.type,
            WalleAction.REPAIR_HARDWARE,
            [restrictions.AUTOMATED_BMC_REPAIR],
            Operation.REPAIR_BMC.type,
            True,
        ),
        (
            rules.CheckBmcVoltage,
            "12V: Current +12V sensor value 11.44 is lower threshold 11.5",
            FailureType.BMC_VOLTAGE,
            RequestTypes.BMC_LOW_VOLTAGE.type,
            WalleAction.REPAIR_HARDWARE,
            [restrictions.AUTOMATED_BMC_REPAIR],
            Operation.REPAIR_BMC.type,
            True,
        ),
    ],
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
@pytest.mark.parametrize("eine_code", [["EINE_CODE"], None, []])
def test_failed_bmc_check(
    rule,
    incomplete_reasons,
    fast,
    hw_checks_enabled,
    eine_code,
    reason,
    failure_type,
    request_type,
    action,
    operation_restrictions,
    operation,
    reboot,
):
    host = Host(inv=100009, name="hostname-mock")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    incomplete_reasons[CheckType.BMC] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": drop_none(
                {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": [reason],
                    "eine_code": eine_code,
                }
            )
        },
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rule())

    if hw_checks_enabled:
        expected_decision = Decision(
            action=action,
            reason="hw-watcher: {}".format(reason),
            params=drop_none(
                {"request_type": request_type, "eine_code": eine_code or None, "operation": operation, "reboot": reboot}
            )
            or None,
            checks=[CheckType.BMC],
            failure_type=failure_type,
            restrictions=operation_restrictions,
        )
        assert expected_decision == make_decision(host, incomplete_reasons, fast=fast, rule=rule)
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_bmc_check_profile_needed(incomplete_reasons, enable_hw_checks):
    host = Host(inv=100009, name="hostname-mock")

    reason = "Some weird BMC faulire, flash needed"
    incomplete_reasons[CheckType.BMC] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": drop_none(
                {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": [reason],
                }
            )
        },
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rules.CheckBmcUnknown())

    expected_decision = Decision(
        action=WalleAction.PROFILE,
        reason="hw-watcher: {}".format(reason),
        failure_type=FailureType.BMC_OTHER,
        checks=[CheckType.BMC],
    )
    assert expected_decision == make_decision(host, incomplete_reasons, fast=False, rule=rule)


@pytest.mark.parametrize(
    ["reasons", "eine", "failure_type", "action", "request_type"],
    [
        (
            ["availability: less local GPUs 6 than in bot 8: 0321418015041, 0321418015869"],
            ["GPU_MISSING"],
            FailureType.GPU_MISSING,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.GPU_MISSING.type,
        ),
        (
            ["hang: hanging interaction with GPU GeForce GTX 1080 Ti PCIe 0000:05:00.0 Slot 1"],
            ["GPU_HANG"],
            FailureType.GPU_HANG,
            WalleAction.REBOOT,
            RequestTypes.GPU_HANG.type,
        ),
        (
            ["temperature: acitve thermal capping (sw thermal slowdown) on GPU GeForce GTX 1080 Ti PCIe 0000:04:00.0"],
            ["GPU_OVERHEAT"],
            FailureType.GPU_OVERHEAT,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.GPU_OVERHEATED.type,
        ),
        (
            ["inforom corrupted: some_error here"],
            ["GPU_INFOROM_CORRUPTED"],
            FailureType.GPU_INFOROM_CORRUPTED,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.GPU_INFOROM_CORRUPTED.type,
        ),
        (
            ["pcie: current pcie width: Something is wrong"],
            ["GPU_BANDWIDTH_TOO_LOW"],
            FailureType.GPU_BANDWIDTH_TOO_LOW,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.GPU_BANDWIDTH_TOO_LOW.type,
        ),
        (
            [
                "availability: less local GPUs 6 than in bot 8: 0321418015041, 0321418015869",
                "temperature: acitve thermal capping (sw thermal slowdown) on GPU GeForce GTX 1080 Ti PCIe 0000:04:00.0",
            ],
            ["GPU_MISSING"],
            FailureType.GPU_MISSING,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.GPU_MISSING.type,
        ),
        (
            [
                "retired: GPU Tesla V100-PCIE-32GB S/N 1563619007636 PCIe 0000:83:00.0 Slot 4 has to be reset for new retired pages"
            ],
            ["GPU_RETIRED_PAGES_PENDING"],
            FailureType.GPU_RETIRED_PAGES_PENDING,
            WalleAction.REBOOT,
            RequestTypes.GPU_RETIRED_PAGES_PENDING.type,
        ),
        (
            ["p2p: GPU Tesla V100-PCIE-32GB S/N 1563619007636 PCIe 0000:83:00.0 Slot 4 has to be reset"],
            ["GPU_P2P_FAILED"],
            FailureType.GPU_P2P_FAILED,
            WalleAction.REBOOT,
            RequestTypes.GPU_P2P_FAILED.type,
        ),
        (
            [
                "retired: GPU Tesla V100-PCIE-32GB S/N 1563619007636 PCIe 0000:83:00.0 Slot 4 has to be reset for new retired pages"
            ],
            ["GPU_RETIRED_PAGES"],
            FailureType.GPU_RETIRED_PAGES,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.GPU_RETIRED_PAGES.type,
        ),
        (
            ["memory: retired memory detected (single bit ecc: 16) on GPU Tesla M40 24GB S/N 0322616088168"],
            ["EINE_CODE"],
            None,
            WalleAction.HEALTHY,
            None,
        ),
        (
            ["clocks: low clocks 139 MHz with full utilization on GPU GeForce GTX 1080 Ti PCIe 0000:09:00.0 Slot 3."],
            ["GPU_POWER_CAPPING"],
            FailureType.GPU_POWER_CAPPING,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.GPU_POWER_CAPPING.type,
        ),
        (
            ["capping: acitve capping (hw slowdown) on GPU Tesla K40m S/N 0322714014526 PCIe 0000:83:00.0 Slot 2"],
            ["GPU_CAPPING"],
            FailureType.GPU_CAPPING,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.GPU_CAPPING.type,
        ),
        (
            ["power: GPU GeForce GTX 1080 Ti PCIe 0000:83:00.0 Slot 4 has unknown power draw and needs to be reset"],
            ["GPU_POWER_UNKNOWN"],
            FailureType.GPU_POWER_UNKNOWN,
            WalleAction.PROFILE,
            RequestTypes.GPU_POWER_UNKNOWN.type,
        ),
    ],
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_gpu_check(
    incomplete_reasons, fast, hw_checks_enabled, eine, reasons, failure_type, action, request_type
):
    host = Host(inv=100009, name="hostname-mock")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    incomplete_reasons[CheckType.GPU] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": drop_none(
                {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": reasons,
                    "eine_code": eine,
                }
            )
        },
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }

    if failure_type in [
        FailureType.GPU_RETIRED_PAGES_PENDING,
        FailureType.GPU_P2P_FAILED,
        FailureType.GPU_HANG,
        FailureType.GPU_POWER_UNKNOWN,
    ]:
        incomplete_reasons[CheckType.GPU]["metadata"]["result"]["slots"] = [4]

    rule = SingleCheckRule(rules.CheckGpu())

    if hw_checks_enabled:
        if action == WalleAction.HEALTHY:
            expected_decision = Decision.healthy("Host is healthy.")
        elif action == WalleAction.PROFILE:
            params = dict(
                profile_mode="default",
                request_type=request_type,
                slots=[4],
                failure_type=FailureType.GPU_POWER_UNKNOWN.name,
            )
            expected_decision = Decision(
                action=action,
                reason=get_reason_from_hw_watcher("GPU", reasons),
                checks=[CheckType.GPU],
                failure_type=failure_type,
                restrictions=[restrictions.AUTOMATED_GPU_REPAIR, restrictions.AUTOMATED_PROFILE],
                params=params,
            )
        elif action == WalleAction.REBOOT:
            params = dict(slots=[4], failure_type=failure_type, request_type=request_type)
            expected_decision = Decision(
                action=action,
                reason=get_reason_from_hw_watcher("GPU", reasons),
                checks=[CheckType.GPU],
                failure_type=failure_type,
                restrictions=[restrictions.AUTOMATED_GPU_REPAIR, restrictions.AUTOMATED_REBOOT],
                params=params,
            )
        else:
            expected_decision = Decision(
                action=action,
                reason=get_reason_from_hw_watcher("GPU", reasons),
                params=drop_none(
                    {
                        "request_type": request_type,
                        "operation": Operation.REPAIR_GPU.type,
                        "errors": reasons,
                        "eine_code": eine or None,
                    }
                ),
                checks=[CheckType.GPU],
                failure_type=failure_type,
                restrictions=[restrictions.AUTOMATED_GPU_REPAIR],
            )
        assert expected_decision == make_decision(host, incomplete_reasons, fast=fast, rule=rule)
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize(
    "metadata,params,reason,failure_type",
    (
        (
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["cable: 200 udma crc errors (threshold 1)"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "reason": ["error1", "error2"],
                    },
                }
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "errors": ["error1", "error2"],
                "redeploy": False,
                "reboot": True,
                "request_type": RequestTypes.BAD_DISK_CABLE.type,
                "operation": Operation.REPAIR_DISK_CABLE.type,
                "power_on_before_repair": False,
            },
            "Disk check failed. hw-watcher:\n* cable: 200 udma crc errors (threshold 1)\n* error1\n* error2",
            FailureType.DISK_CONNECTIVITY,
        ),
        (
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["some mock-reason"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["ATA_BUS_ERROR"],
                }
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "errors": ["error1", "error2"],
                "redeploy": False,
                "reboot": True,
                "request_type": RequestTypes.BAD_DISK_CABLE.type,
                "operation": Operation.REPAIR_DISK_CABLE.type,
                "power_on_before_repair": False,
                "eine_code": ["ATA_BUS_ERROR"],
            },
            "Disk check failed. hw-watcher:\n* some mock-reason\n* error1\n* error2",
            FailureType.DISK_CONNECTIVITY,
        ),
    ),
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_disk_cable_check(incomplete_reasons, enable_hw_checks, metadata, params, reason, failure_type):
    host = Host(restrictions=[])
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    incomplete_reasons[CheckType.DISK] = {
        "status": CheckStatus.FAILED,
        "metadata": metadata,
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rules.CheckDiskCable())

    operation_restrictions = [restrictions.AUTOMATED_DISK_CABLE_REPAIR, restrictions.AUTOMATED_REBOOT]

    check_decision(
        incomplete_reasons,
        fast=True,
        action=WalleAction.REPAIR_HARDWARE,
        reason=reason,
        params=params,
        checks=[CheckType.DISK],
        restrictions=operation_restrictions,
        host=host,
        hw_checks_enabled=True,
        rule=rule,
        failure_type=failure_type,
    )


@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_eine_disk_smart_code(incomplete_reasons, enable_hw_checks):
    host = Host(restrictions=[], ipmi_mac="ipmi_mac")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    any_eine_disk_smart_code = list(rules.CheckDiskSmartCodesRule._DRIVE_SMART_CODES)[0]
    params = {
        'slot': 6,
        'type': 'HDD',
        'model': 'HGST HUS726060ALE614',
        'serial': 'NAG3DJ7Y',
        'errors': [
            'dmesg: error count 10 (threshold 3)',
            'smart: Reallocated_Event_Count raw value: 1290 (threshold 15)',
            'smart: Reallocated_Sector_Ct raw value: 1290 (threshold 15)',
        ],
        'redeploy': False,
        'eine_code': [any_eine_disk_smart_code, 'ATA_IO_ERROR'],
    }
    metadata = {
        "result": {
            "disk2replace": {
                "disk_type": "HDD",
                "diskperformance": None,
                "model": "HGST HUS726060ALE614",
                "name": "sdg",
                "reason": [
                    "dmesg: error count 10 (threshold 3)",
                    "smart: Reallocated_Event_Count raw value: 1290 (threshold 15)",
                    "smart: Reallocated_Sector_Ct raw value: 1290 (threshold 15)",
                ],
                "serial": "NAG3DJ7Y",
                "size_in_bytes": 6001175126016,
                "slot": 6,
                "status": "FAILED",
            },
            "eine_code": [any_eine_disk_smart_code, "ATA_IO_ERROR"],
            "reason": [
                "dmesg: error count 10 (threshold 3)",
                "raid: array incomplete",
                "smart: Reallocated_Event_Count raw value: 1290 (threshold 15)",
                "smart: Reallocated_Sector_Ct raw value: 1290 (threshold 15)",
            ],
            "status": "FAILED",
            "timestamp": 1642511312.398477,
        }
    }
    incomplete_reasons[CheckType.DISK] = {
        "status": CheckStatus.FAILED,
        "metadata": metadata,
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rules.CheckDiskSmartCodesRule())
    action = WalleAction.CHANGE_DISK
    failure_type = rules.CheckDiskSmartCodesRule._DRIVE_SMART_CODES[any_eine_disk_smart_code]
    operation_restrictions = []
    reason = (
        "Disk check failed. hw-watcher:\n"
        "* dmesg: error count 10 (threshold 3)\n"
        "* raid: array incomplete\n"
        "* smart: Reallocated_Event_Count raw value: 1290 (threshold 15)\n"
        "* smart: Reallocated_Sector_Ct raw value: 1290 (threshold 15)\n"
        "* dmesg: error count 10 (threshold 3)\n"
        "* smart: Reallocated_Event_Count raw value: 1290 (threshold 15)\n"
        "* smart: Reallocated_Sector_Ct raw value: 1290 (threshold 15)"
    )
    check_decision(
        incomplete_reasons,
        fast=True,
        action=action,
        reason=reason,
        params=params,
        checks=[CheckType.DISK],
        failure_type=failure_type,
        restrictions=operation_restrictions,
        host=host,
        hw_checks_enabled=True,
        rule=rule,
    )


@pytest.mark.parametrize(
    "rule,rule_params,metadata,params,reason,action,failure_type",
    (
        (
            rules.CheckDisk,
            {},
            {"reason": "Reason mock"},
            {"redeploy": True},
            "Disk check failed: Reason mock",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_NO_CHECK_INFO,
        ),
        (
            rules.CheckDisk,
            {"redeploy_on_no_info": False},
            {"reason": "Reason mock"},
            {},
            "Disk check failed: Reason mock",
            WalleAction.DEACTIVATE,
            FailureType.DISK_NO_CHECK_INFO,
        ),
        (
            rules.CheckDisk,
            {},
            {"result": {"reason": ["Reason mock"]}},
            {"redeploy": True},
            "Disk check failed. hw-watcher: Reason mock.",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_UNKNOWN,
        ),
        (
            rules.CheckDisk,
            {"redeploy_on_unknown_disk": False},
            {"result": {"reason": ["Reason mock"]}},
            {},
            "Disk check failed. hw-watcher: Reason mock.",
            WalleAction.DEACTIVATE,
            FailureType.DISK_UNKNOWN,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "reason": ["error1", "error2"],
                    },
                }
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "errors": ["error1", "error2"],
                "redeploy": True,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (
            rules.CheckDisk,
            {"redeploy_on_unreserved_disk": False},
            {
                "result": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "reason": ["error1", "error2"],
                    },
                }
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "errors": ["error1", "error2"],
                "redeploy": False,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (
            rules.CheckDiskCable,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Error mock"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "reason": ["cable: 200 udma crc errors (threshold 1)", "error2"],
                    },
                }
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "errors": ["cable: 200 udma crc errors (threshold 1)", "error2"],
                "redeploy": False,
                "reboot": True,
                "request_type": RequestTypes.BAD_DISK_CABLE.type,
                "operation": Operation.REPAIR_DISK_CABLE.type,
                "power_on_before_repair": False,
            },
            "Disk check failed. hw-watcher:\n* Error mock\n* cable: 200 udma crc errors (threshold 1)\n* error2",
            WalleAction.REPAIR_HARDWARE,
            FailureType.DISK_CONNECTIVITY,
        ),
    ),
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
@pytest.mark.parametrize("eine_code", [["EINE_CODE"], None, []])
def test_failed_disk_check_with_eine_code(
    rule, rule_params, incomplete_reasons, enable_hw_checks, eine_code, metadata, params, reason, action, failure_type
):
    host = Host(restrictions=[], ipmi_mac="ipmi_mac")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    if "result" in metadata and "disk2replace" in metadata["result"]:
        _insert_or_drop(metadata["result"], "eine_code", eine_code, lambda v: v is not None)
        _insert_or_drop(params, "eine_code", eine_code)

    incomplete_reasons[CheckType.DISK] = {
        "status": CheckStatus.FAILED,
        "metadata": metadata,
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rule(**rule_params))
    operation_restrictions = []
    if not params.get("slot") and not params.get("serial") and action != WalleAction.DEACTIVATE:
        operation_restrictions.append(restrictions.AUTOMATED_PROFILE)
    if params.get("redeploy"):
        operation_restrictions.append(restrictions.AUTOMATED_REDEPLOY)
    if action == WalleAction.REPAIR_HARDWARE:
        operation_restrictions.extend([restrictions.AUTOMATED_DISK_CABLE_REPAIR, restrictions.AUTOMATED_REBOOT])

    check_decision(
        incomplete_reasons,
        fast=True,
        action=action,
        reason=reason,
        params=params,
        checks=[CheckType.DISK] if action != WalleAction.DEACTIVATE else None,
        failure_type=failure_type,
        restrictions=operation_restrictions,
        host=host,
        hw_checks_enabled=True,
        rule=rule,
    )


@pytest.mark.parametrize(
    ["reason", "params", "operation_restrictions", "failure_type", "eine_code"],
    [
        (
            "Reason mock",
            {"request_type": RequestTypes.MALFUNCTIONING_LINK.type, "operation": Operation.REPAIR_LINK.type},
            [],
            FailureType.LINK_MALFUNCTION,
            ["ANY"],
        ),
        (
            "speed: reason mock",
            {"request_type": RequestTypes.MALFUNCTIONING_LINK.type, "operation": Operation.REPAIR_LINK.type},
            [],
            FailureType.LINK_MALFUNCTION,
            ["ANY"],
        ),
        (
            "duplex: reason mock",
            {"request_type": RequestTypes.MALFUNCTIONING_LINK.type, "operation": Operation.REPAIR_LINK.type},
            [],
            FailureType.LINK_MALFUNCTION,
            ["ANY"],
        ),
        (
            "rx_crc_errors: reason mock",
            {
                "reboot": True,
                "request_type": RequestTypes.MALFUNCTIONING_LINK_RX_CRC_ERRORS.type,
                "operation": Operation.REPAIR_LINK.type,
            },
            [restrictions.AUTOMATED_REBOOT],
            FailureType.LINK_RX_CRC_ERRORS,
            ["NIC_ERR"],
        ),
        (
            "any: reason mock",
            {
                "reboot": True,
                "request_type": RequestTypes.MALFUNCTIONING_LINK_RX_CRC_ERRORS.type,
                "operation": Operation.REPAIR_LINK.type,
            },
            [restrictions.AUTOMATED_REBOOT],
            FailureType.LINK_RX_CRC_ERRORS,
            ["PCIE_DEVICE_BANDWIDTH_TOO_LOW"],
        ),
    ],
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_link_check(
    incomplete_reasons,
    fast,
    hw_checks_enabled,
    eine_code,
    reason,
    params,
    operation_restrictions,
    failure_type,
    walle_test,
):
    incomplete_reasons[CheckType.LINK] = {
        "status": CheckStatus.FAILED,
        "metadata": {"result": drop_none({"reason": [reason], "eine_code": eine_code})},
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rules.CheckLink())

    if hw_checks_enabled:
        _insert_or_drop(params, "eine_code", eine_code)
        check_decision(
            incomplete_reasons,
            fast,
            WalleAction.REPAIR_HARDWARE,
            checks=[CheckType.LINK],
            params=params,
            restrictions=[restrictions.AUTOMATED_LINK_REPAIR] + operation_restrictions,
            reason="Link check failed. hw-watcher: {}.".format(reason),
            rule=rule,
        )
    else:
        check_decision(
            incomplete_reasons,
            fast,
            WalleAction.HEALTHY,
            hw_checks_enabled=hw_checks_enabled,
            rule=rule,
            failure_type=failure_type,
        )


@pytest.mark.parametrize("restricted_disk_change", (True, False))
@pytest.mark.parametrize("restricted_redeploy", (True, False))
@pytest.mark.parametrize(
    "rule,rule_params,metadata,params,reason,action,failure_type",
    (
        (
            rules.CheckDisk,
            {},
            {"reason": "Reason mock"},
            {"redeploy": True},
            "Disk check failed: Reason mock",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_NO_CHECK_INFO,
        ),
        (
            rules.CheckDisk,
            {},
            {"result": {"reason": ["Reason mock"]}},
            {"redeploy": True},
            "Disk check failed. hw-watcher: Reason mock.",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_UNKNOWN,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "reason": ["error1", "error2"],
                    },
                },
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "errors": ["error1", "error2"],
                "redeploy": True,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "reason": ["error1", "error2"],
                    },
                },
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "errors": ["error1", "error2"],
                "redeploy": False,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "shelf_inv": "240288",
                        "reason": ["error1", "error2"],
                    },
                },
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "shelf_inv": "240288",
                "errors": ["error1", "error2"],
                "redeploy": True,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "model": "model-mock",
                        "serial": "serial-mock",
                        "disk_type": "type-mock",
                        "shelf_inv": "240288",
                        "reason": ["error1", "error2"],
                    },
                },
            },
            {
                "slot": 666,
                "type": "type-mock",
                "model": "model-mock",
                "serial": "serial-mock",
                "shelf_inv": "240288",
                "errors": ["error1", "error2"],
                "redeploy": False,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                },
            },
            {
                "slot": 666,
                "errors": ["error1", "error2"],
                "redeploy": True,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                },
            },
            {
                "slot": 666,
                "errors": ["error1", "error2"],
                "redeploy": False,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (  # WALLE-1420
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "reason": ["Disk without reservation is failed, status set by reaction option"],
                    "disk2replace": {
                        "serial": "P61227030909",
                        "reason": ["availability: disk found in bot, but not detected by system"],
                        "slot": -1,
                    },
                }
            },
            {
                "serial": "P61227030909",
                "errors": ["availability: disk found in bot, but not detected by system"],
                "redeploy": True,
            },
            "Disk check failed. hw-watcher:\n* Disk without reservation is failed, status set by reaction option\n"
            "* availability: disk found in bot, but not detected by system",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (  # WALLE-1420
            rules.CheckDisk,
            {"redeploy_on_unreserved_disk": False},
            {
                "result": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "reason": ["Disk without reservation is failed, status set by reaction option"],
                    "disk2replace": {
                        "serial": "P61227030909",
                        "reason": ["availability: disk found in bot, but not detected by system"],
                        "slot": -1,
                    },
                }
            },
            {
                "serial": "P61227030909",
                "errors": ["availability: disk found in bot, but not detected by system"],
                "redeploy": False,
            },
            "Disk check failed. hw-watcher:\n* Disk without reservation is failed, status set by reaction option\n"
            "* availability: disk found in bot, but not detected by system",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_COMMON,
        ),
        (
            rules.CheckDiskBadBlocks,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["ATA_IO_ERROR"],
                }
            },
            {
                "slot": 666,
                "redeploy": True,
                "errors": ["error1", "error2"],
                "eine_code": ["ATA_IO_ERROR"],
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.REDEPLOY,
            FailureType.DISK_BAD_BLOCKS,
        ),
        (
            rules.CheckDisk,
            {"redeploy_on_bad_blocks": False},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["ATA_IO_ERROR"],
                }
            },
            {
                "slot": 666,
                "redeploy": False,
                "errors": ["error1", "error2"],
                "eine_code": ["ATA_IO_ERROR"],
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_BAD_BLOCKS,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["NVME_LINK_DEGRADED"],
                }
            },
            {
                "slot": 666,
                "redeploy": False,
                "errors": ["error1", "error2"],
                "eine_code": ["NVME_LINK_DEGRADED"],
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_NVME_LINK_DEGRADED,
        ),
        (
            rules.CheckDiskPerformance,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["DRIVE_PERF_RAND_TO_LOW"],
                }
            },
            {
                "slot": 666,
                "redeploy": False,
                "errors": ["error1", "error2"],
                "eine_code": ["DRIVE_PERF_RAND_TO_LOW"],
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.REBOOT,
            FailureType.DISK_PERFORMANCE,
        ),
        (
            rules.CheckDisk,
            {"reboot_on_performance": False},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["DRIVE_PERF_RAND_TO_LOW"],
                }
            },
            {
                "slot": 666,
                "redeploy": False,
                "errors": ["error1", "error2"],
                "eine_code": ["DRIVE_PERF_RAND_TO_LOW"],
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.CHANGE_DISK,
            FailureType.DISK_PERFORMANCE,
        ),
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["NVME_MISSING"],
                }
            },
            {
                "slot": 666,
                "redeploy": False,
                "profile": True,
                "errors": ["error1", "error2"],
                "eine_code": ["NVME_MISSING"],
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.PROFILE,
            FailureType.DISK_NVME_MISSING,
        ),
    ),
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_disk_check(
    rule,
    rule_params,
    incomplete_reasons,
    restricted_disk_change,
    restricted_redeploy,
    fast,
    hw_checks_enabled,
    metadata,
    params,
    reason,
    action,
    failure_type,
):
    host = Host(restrictions=[], ipmi_mac="ipmi_mac")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    if restricted_disk_change:
        host.restrictions.append(restrictions.AUTOMATED_DISK_CHANGE)
    if restricted_redeploy:
        host.restrictions.append(restrictions.AUTOMATED_REDEPLOY)

    incomplete_reasons[CheckType.DISK] = {
        "status": CheckStatus.FAILED,
        "metadata": metadata,
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rule(**rule_params))

    operation_restrictions = []
    if (
        params is not None
        and ("NVME_LINK_DEGRADED" in params.get("eine_code", []) or not params.get("slot") and not params.get("serial"))
        and action != WalleAction.DEACTIVATE
    ):
        operation_restrictions.append(restrictions.AUTOMATED_PROFILE)
    if action != WalleAction.REDEPLOY and params is not None and params.get("redeploy"):
        operation_restrictions.append(restrictions.AUTOMATED_REDEPLOY)

    if (
        not hw_checks_enabled
        or restricted_disk_change
        and action == WalleAction.CHANGE_DISK
        or params.get("redeploy", False)
        and restricted_redeploy
    ):
        check_decision(
            incomplete_reasons, fast, WalleAction.HEALTHY, host=host, hw_checks_enabled=hw_checks_enabled, rule=rule
        )
    else:
        check_decision(
            incomplete_reasons,
            fast,
            action,
            reason,
            params,
            checks=[CheckType.DISK] if action != WalleAction.DEACTIVATE else None,
            restrictions=operation_restrictions,
            host=host,
            hw_checks_enabled=hw_checks_enabled,
            rule=rule,
            failure_type=failure_type,
        )


@pytest.mark.parametrize("restricted_profile", (True, False))
@pytest.mark.parametrize(
    "rule,rule_params,metadata,params,reason,action,failure_type",
    (
        (
            rules.CheckDisk,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["NVME_MISSING"],
                }
            },
            {
                "slot": 666,
                "redeploy": False,
                "profile": True,
                "errors": ["error1", "error2"],
                "eine_code": ["NVME_MISSING"],
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.PROFILE,
            FailureType.DISK_NVME_MISSING,
        ),
    ),
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_disk_check_nvme_missing(
    rule,
    rule_params,
    incomplete_reasons,
    restricted_profile,
    fast,
    hw_checks_enabled,
    metadata,
    params,
    reason,
    action,
    failure_type,
):
    host = Host(restrictions=[], ipmi_mac="ipmi_mac")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    if restricted_profile:
        host.restrictions.append(restrictions.AUTOMATED_PROFILE)

    incomplete_reasons[CheckType.DISK] = {
        "status": CheckStatus.FAILED,
        "metadata": metadata,
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rule(**rule_params))

    operation_restrictions = []

    if not hw_checks_enabled or restricted_profile:
        check_decision(
            incomplete_reasons, fast, WalleAction.HEALTHY, host=host, hw_checks_enabled=hw_checks_enabled, rule=rule
        )
    else:
        check_decision(
            incomplete_reasons,
            fast,
            action,
            reason,
            params,
            checks=[CheckType.DISK] if action != WalleAction.DEACTIVATE else None,
            restrictions=operation_restrictions,
            host=host,
            hw_checks_enabled=hw_checks_enabled,
            rule=rule,
            failure_type=failure_type,
        )


@pytest.mark.parametrize("restricted_profile", (True, False))
@pytest.mark.parametrize(
    "rule,rule_params,metadata,params,reason,action,failure_type",
    (
        (
            rules.CheckSsdPerfLow,
            {},
            {
                "result": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["Reason mock"],
                    "disk2replace": {
                        "slot": 666,
                        "reason": ["error1", "error2"],
                    },
                    "eine_code": ["SSD_PERF_RAND_TOO_LOW"],
                }
            },
            {
                "slot": 666,
                "redeploy": False,
                "profile": True,
                "errors": ["error1", "error2"],
                "eine_code": ["SSD_PERF_RAND_TOO_LOW"],
                "profile_mode": ProfileMode.DANGEROUS_HIGHLOAD_TEST,
            },
            "Disk check failed. hw-watcher:\n* Reason mock\n* error1\n* error2",
            WalleAction.PROFILE,
            FailureType.SSD_PERF_RAND_TOO_LOW,
        ),
    ),
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_disk_ssd_perf_rand_too_low(
    rule,
    rule_params,
    incomplete_reasons,
    restricted_profile,
    fast,
    hw_checks_enabled,
    metadata,
    params,
    reason,
    action,
    failure_type,
):
    host = Host(restrictions=[], ipmi_mac="ipmi_mac")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    if restricted_profile:
        host.restrictions.append(restrictions.AUTOMATED_PROFILE)

    incomplete_reasons[CheckType.DISK] = {
        "status": CheckStatus.FAILED,
        "metadata": metadata,
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rule(**rule_params))

    operation_restrictions = []

    if not hw_checks_enabled or restricted_profile:
        check_decision(
            incomplete_reasons, fast, WalleAction.HEALTHY, host=host, hw_checks_enabled=hw_checks_enabled, rule=rule
        )
    else:
        check_decision(
            incomplete_reasons,
            fast,
            action,
            reason,
            params,
            checks=[CheckType.DISK] if action != WalleAction.DEACTIVATE else None,
            restrictions=operation_restrictions,
            host=host,
            hw_checks_enabled=hw_checks_enabled,
            rule=rule,
            failure_type=failure_type,
        )


@pytest.mark.parametrize(
    ["results", "params", "decision", "eine_code_test"],
    [
        [
            {
                "ecc": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "slot": "slot-mock",
                    "reason": ["ecc-error1", "ecc-error2"],
                    "comment": "Replace both modules on the channel DIMM_E1 and DIMM_E0. Make sure the server boots up!",
                },
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["numa: error"],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_HARDWARE,
                "hw-watcher: ecc-error1 ecc-error2"
                " Replace both modules on the channel DIMM_E1 and DIMM_E0."
                " Make sure the server boots up!"
                " Platform not supported repair memory",
                {
                    "request_type": RequestTypes.CORRUPTED_MEMORY.type,
                    "operation": Operation.CHANGE_MEMORY.type,
                    "slot": "slot-mock",
                    "errors": ["ecc-error1", "ecc-error2"],
                    "status": HwWatcherCheckStatus.FAILED,
                    "redeploy": True,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_ECC,
                restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE, restrictions.AUTOMATED_REDEPLOY],
                checks=[CheckType.MEMORY],
            ),
            ["EINE_CODE"],
        ],
        [
            {
                "ecc": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "slot": "slot-mock",
                    "reason": ["ecc-error1", "ecc-error2"],
                    "comment": "DDR3_P1_H0: mcelog: 46 correctable errors during 1 day (threshold 1)",
                },
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["speed: error"],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_HARDWARE,
                "hw-watcher: ecc-error1 ecc-error2 DDR3_P1_H0: mcelog: 46 correctable errors during 1 day (threshold 1)"
                " Platform not supported repair memory",
                {
                    "request_type": RequestTypes.CORRUPTED_MEMORY.type,
                    "operation": Operation.CHANGE_MEMORY.type,
                    "slot": "slot-mock",
                    "errors": ["ecc-error1", "ecc-error2"],
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "redeploy": True,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_ECC,
                restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE, restrictions.AUTOMATED_REDEPLOY],
                checks=[CheckType.MEMORY],
            ),
            ["EINE_CODE"],
        ],
        [
            {
                "ecc": {"status": "OK"},
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "needed_mem": 272,
                    "real_mem": 256,
                    "comment": "16 modules installed: XXX.",
                    "reason": ["size: available 256 of 272 GB memory."],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_HARDWARE,
                "hw-watcher: size: available 256 of 272 GB memory. 16 modules installed: XXX.",
                {
                    "request_type": RequestTypes.INVALID_MEMORY_SIZE.type,
                    "operation": Operation.CHANGE_MEMORY.type,
                    "expected": 272,
                    "real": 256,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_SIZE,
                restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE],
                checks=[CheckType.MEMORY],
            ),
            ["EINE_CODE"],
        ],
        [
            {
                "ecc": {"status": "OK"},
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["numa: different sizes on numa nodes: 128 GB (node0), 64 GB (node1)"],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_HARDWARE,
                "hw-watcher: numa: different sizes on numa nodes: 128 GB (node0), 64 GB (node1)",
                checks=[CheckType.MEMORY],
                restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE],
                params={
                    "request_type": RequestTypes.INVALID_NUMA_MEMORY_NODES_SIZES.type,
                    "operation": Operation.CHANGE_MEMORY.type,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_NUMA,
            ),
            ["EINE_CODE"],
        ],
        [
            {
                "ecc": {"status": "OK"},
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["speed: memory speed 1867 is lower max 2133"],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_HARDWARE,
                "hw-watcher: speed: memory speed 1867 is lower max 2133",
                {
                    "request_type": RequestTypes.LOW_MEMORY_SPEED.type,
                    "operation": Operation.CHANGE_MEMORY.type,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_SPEED,
                restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE],
                checks=[CheckType.MEMORY],
            ),
            ["EINE_CODE"],
        ],
        [
            {
                "ecc": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "slot": "slot-mock",
                    "reason": ["ecc-error1", "ecc-error2"],
                    "comment": "DIMM_UE eine code is here.",
                },
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["numa: error"],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_HARDWARE,
                "hw-watcher: ecc-error1 ecc-error2 DIMM_UE eine code is here. Platform not supported repair memory",
                {
                    "request_type": RequestTypes.UNCORRECTABLE_ERRORS.type,
                    "operation": Operation.CHANGE_MEMORY.type,
                    "slot": "slot-mock",
                    "errors": ["ecc-error1", "ecc-error2"],
                    "status": HwWatcherCheckStatus.FAILED,
                    "redeploy": True,
                    "reboot": True,
                },
                checks=[CheckType.MEMORY],
                failure_type=FailureType.MEM_ECC,
                restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE, restrictions.AUTOMATED_REDEPLOY],
            ),
            ["DIMM_UE"],
        ],
        [
            {
                "ecc": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "slot": "slot-mock",
                    "reason": ["ecc-error1", "ecc-error2"],
                    "comment": "DIMM_UE eine code is here.",
                },
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["numa: error"],
                },
            },
            {"redeploy_on_ecc_failure": False},
            Decision(
                WalleAction.REPAIR_HARDWARE,
                "hw-watcher: ecc-error1 ecc-error2 DIMM_UE eine code is here. Platform not supported repair memory",
                {
                    "request_type": RequestTypes.UNCORRECTABLE_ERRORS.type,
                    "operation": Operation.CHANGE_MEMORY.type,
                    "slot": "slot-mock",
                    "errors": ["ecc-error1", "ecc-error2"],
                    "status": HwWatcherCheckStatus.FAILED,
                    "redeploy": False,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_ECC,
                checks=[CheckType.MEMORY],
                restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE],
            ),
            ["DIMM_UE"],
        ],
    ],
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_memory_check(incomplete_reasons, fast, hw_checks_enabled, results, params, decision, eine_code_test):
    host = Host(inv=100009, name="hostname-mock")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")

    for value in results.values():
        if value["status"] != "OK":
            _insert_or_drop(value, "eine_code", eine_code_test, lambda v: v is not None)

    incomplete_reasons[CheckType.MEMORY] = {
        "status": CheckStatus.FAILED,
        "metadata": {"results": results},
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rules.CheckMemory(**params))

    if hw_checks_enabled:
        decision = _add_decision_params(decision, eine_code=eine_code_test or None)
        assert decision == make_decision(host, incomplete_reasons, fast=fast, rule=rule)
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize(
    "results",
    (
        {
            "ecc": {"status": "OK"},
            "mem": {"status": "OK"},
        },
        {
            "ecc": {"status": "OK"},
            "mem": {
                "status": HwWatcherCheckStatus.FAILED,
                # needed_mem < real_mem
                "needed_mem": 1,
                "real_mem": 2,
            },
        },
    ),
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_memory_check_with_unsupported_errors(mp, incomplete_reasons, hw_checks_enabled, results):
    monkeypatch_config(mp, "automation.platform_support.repair-memory.platforms", [dict()])
    incomplete_reasons[CheckType.MEMORY] = {
        "status": CheckStatus.FAILED,
        "metadata": {"results": results},
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rules.CheckMemory())

    check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.usefixtures("enable_hw_checks")
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_memory_check_escalation_by_status(mp, incomplete_reasons, fast):
    """This decision shall not be escalated."""
    host = Host(inv=100009, name="hostname-mock", status=Operation.CHANGE_MEMORY.host_status)

    error_meta = {
        "failure_type": FailureType.MEM_ECC,
        "slot": 0,
        "reason": ["ecc check failed."],
        "comment": "memory unit failed",
        "status": HwWatcherCheckStatus.FAILED,
    }
    incomplete_reasons[CheckType.MEMORY] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "results": {
                "ecc": error_meta,
                "mem": {"status": HwWatcherCheckStatus.FAILED},
            }
        },
    }
    rule = SingleCheckRule(rules.CheckMemory())

    assert make_decision(host, incomplete_reasons, fast=fast, rule=rule) == Decision(
        WalleAction.REPAIR_HARDWARE,
        reason="hw-watcher: ecc check failed. memory unit failed Platform not supported repair memory",
        checks=[CheckType.MEMORY],
        restrictions=[restrictions.AUTOMATED_MEMORY_CHANGE, restrictions.AUTOMATED_REDEPLOY],
        failure_type=FailureType.MEM_ECC,
        params={
            "request_type": RequestTypes.CORRUPTED_MEMORY.type,
            "operation": Operation.CHANGE_MEMORY.type,
            "slot": error_meta["slot"],
            "status": HwWatcherCheckStatus.FAILED,
            "errors": error_meta["reason"],
            "redeploy": True,
            "reboot": True,
        },
    )


@pytest.mark.parametrize(
    ["results", "params", "decision", "eine_code_test"],
    [
        [
            {
                "ecc": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "slot": "slot-mock",
                    "reason": ["ecc-error1", "ecc-error2"],
                    "comment": "Replace both modules on the channel DIMM_E1 and DIMM_E0. Make sure the server boots up!",
                },
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["numa: error"],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_MEMORY,
                "hw-watcher: ecc-error1 ecc-error2"
                " Replace both modules on the channel DIMM_E1 and DIMM_E0."
                " Make sure the server boots up!",
                {
                    "request_type": RequestTypes.CORRUPTED_MEMORY.type,
                    "operation": Operation.REPAIR_MEMORY.type,
                    "slot": "slot-mock",
                    "errors": ["ecc-error1", "ecc-error2"],
                    "status": HwWatcherCheckStatus.FAILED,
                    "redeploy": True,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_ECC,
                restrictions=[restrictions.AUTOMATED_MEMORY_REPAIR, restrictions.AUTOMATED_REDEPLOY],
                checks=[CheckType.MEMORY],
            ),
            ["EINE_CODE"],
        ],
        [
            {
                "ecc": {
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "slot": "slot-mock",
                    "reason": ["ecc-error1", "ecc-error2"],
                    "comment": "DDR3_P1_H0: mcelog: 46 correctable errors during 1 day (threshold 1)",
                },
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["speed: error"],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_MEMORY,
                "hw-watcher: ecc-error1 ecc-error2 DDR3_P1_H0: mcelog: 46 correctable errors during 1 day (threshold 1)",
                {
                    "request_type": RequestTypes.CORRUPTED_MEMORY.type,
                    "operation": Operation.REPAIR_MEMORY.type,
                    "slot": "slot-mock",
                    "errors": ["ecc-error1", "ecc-error2"],
                    "status": HwWatcherCheckStatus.UNKNOWN,
                    "redeploy": True,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_ECC,
                restrictions=[restrictions.AUTOMATED_MEMORY_REPAIR, restrictions.AUTOMATED_REDEPLOY],
                checks=[CheckType.MEMORY],
            ),
            ["EINE_CODE"],
        ],
        [
            {
                "ecc": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "slot": "slot-mock",
                    "reason": ["ecc-error1", "ecc-error2"],
                    "comment": "DIMM_UE eine code is here.",
                },
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["numa: error"],
                },
            },
            {},
            Decision(
                WalleAction.REPAIR_MEMORY,
                "hw-watcher: ecc-error1 ecc-error2 DIMM_UE eine code is here.",
                {
                    "request_type": RequestTypes.UNCORRECTABLE_ERRORS.type,
                    "operation": Operation.REPAIR_MEMORY.type,
                    "slot": "slot-mock",
                    "errors": ["ecc-error1", "ecc-error2"],
                    "status": HwWatcherCheckStatus.FAILED,
                    "redeploy": True,
                    "reboot": True,
                },
                checks=[CheckType.MEMORY],
                failure_type=FailureType.MEM_ECC,
                restrictions=[restrictions.AUTOMATED_MEMORY_REPAIR, restrictions.AUTOMATED_REDEPLOY],
            ),
            ["DIMM_UE"],
        ],
        [
            {
                "ecc": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "slot": "slot-mock",
                    "reason": ["ecc-error1", "ecc-error2"],
                    "comment": "DIMM_UE eine code is here.",
                },
                "mem": {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": ["numa: error"],
                },
            },
            {"redeploy_on_ecc_failure": False},
            Decision(
                WalleAction.REPAIR_MEMORY,
                "hw-watcher: ecc-error1 ecc-error2 DIMM_UE eine code is here.",
                {
                    "request_type": RequestTypes.UNCORRECTABLE_ERRORS.type,
                    "operation": Operation.REPAIR_MEMORY.type,
                    "slot": "slot-mock",
                    "errors": ["ecc-error1", "ecc-error2"],
                    "status": HwWatcherCheckStatus.FAILED,
                    "redeploy": False,
                    "reboot": True,
                },
                failure_type=FailureType.MEM_ECC,
                checks=[CheckType.MEMORY],
                restrictions=[restrictions.AUTOMATED_MEMORY_REPAIR],
            ),
            ["DIMM_UE"],
        ],
    ],
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_memory_check_on_recoverable_platforms(
    mp, incomplete_reasons, fast, hw_checks_enabled, results, params, decision, eine_code_test
):
    host = Host(inv=100009, name="hostname-mock")
    host.get_project = lambda fields: Project(profile="profile-mock", vlan_scheme="vlan_scheme_mock")
    monkeypatch_config(mp, "automation.platform_support.repair-memory.platforms", [dict()])

    for value in results.values():
        if value["status"] != "OK":
            _insert_or_drop(value, "eine_code", eine_code_test, lambda v: v is not None)

    incomplete_reasons[CheckType.MEMORY] = {
        "status": CheckStatus.FAILED,
        "metadata": {"results": results},
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }
    rule = SingleCheckRule(rules.CheckMemory(**params))

    if hw_checks_enabled:
        decision = _add_decision_params(decision, eine_code=eine_code_test or None)
        assert decision == make_decision(host, incomplete_reasons, fast=fast, rule=rule)
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize("check_type", CheckType.ALL_HW_WATCHER)
def test_suspected_hardware_check(reasons, hw_checks_enabled, check_type):
    reasons[check_type] = {"status": CheckStatus.SUSPECTED}

    if hw_checks_enabled:
        check_decision(
            reasons,
            fast,
            WalleAction.WAIT,
            checks=[check_type],
            reason="{} check is suspected.".format(get_walle_check_type(check_type)),
        )
    else:
        check_decision(reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled)


@pytest.mark.parametrize(
    ["reasons", "eine", "failure_type", "action", "request_type"],
    [
        (
            ["symbol_error_counter                : 65535      (overflow)"],
            ["INFINIBAND_ERR"],
            FailureType.INFINIBAND_ERR,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.INFINIBAND_ERR.type,
        ),
        (
            ["availability: local and bot infiniband MAC mismatch: not found locally 0C42A1AF1300, 0C42A1D370FE"],
            ["INFINIBAND_MISMATCH"],
            FailureType.INFINIBAND_MISMATCH,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.INFINIBAND_MISMATCH.type,
        ),
        (
            ["availability: local and bot infiniband MAC mismatch: not found locally 0C42A1AF1300, 0C42A1D370FE"],
            ["INFINIBAND_INVALID_PHYS_STATE"],
            FailureType.INFINIBAND_INVALID_PHYS_STATE,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.INFINIBAND_INVALID_PHYS_STATE.type,
        ),
        (
            ["availability: local and bot infiniband MAC mismatch: not found locally 0C42A1AF1300, 0C42A1D370FE"],
            ["PCIE_DEVICE_BANDWIDTH_TOO_LOW"],
            FailureType.PCIE_DEVICE_BANDWIDTH_TOO_LOW,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.PCIE_DEVICE_BANDWIDTH_TOO_LOW.type,
        ),
        (
            ["infiniband low speed error"],
            [INFINIBAND_EINE_CODE.INFINIBAND_LOW_SPEED],
            FailureType.INFINIBAND_LOW_SPEED,
            WalleAction.REPAIR_HARDWARE,
            RequestTypes.INFINIBAND_LOW_SPEED.type,
        ),
    ],
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_infiniband_check_with_hw_repair(
    incomplete_reasons, fast, hw_checks_enabled, eine, reasons, failure_type, action, request_type
):
    host = Host(inv=100009, name="hostname-mock")
    host.get_project = lambda fields: Project(
        profile="profile-mock", vlan_scheme="vlan_scheme_mock", tags=["rtc.infiniband-enabled"]
    )

    incomplete_reasons[CheckType.INFINIBAND] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": drop_none(
                {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": reasons,
                    "eine_code": eine,
                }
            )
        },
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }

    rule = SingleCheckRule(rules.CheckInfiniband())

    if hw_checks_enabled:
        expected_decision = Decision(
            action=action,
            reason=get_reason_from_hw_watcher("infiniband", reasons),
            params=drop_none(
                {
                    "request_type": request_type,
                    "operation": Operation.REPAIR_INFINIBAND.type,
                    "errors": reasons,
                    "eine_code": eine,
                }
            ),
            checks=[CheckType.INFINIBAND],
            failure_type=failure_type,
            restrictions=[restrictions.AUTOMATED_INFINIBAND_REPAIR],
        )
        assert expected_decision == make_decision(host, incomplete_reasons, fast=fast, rule=rule)
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)


@pytest.mark.parametrize(
    ["reasons", "eine", "failure_type", "action", "request_type"],
    [
        (
            ["miscofigured: needs network card update"],
            ["INFINIBAND_MISCONFIGURED"],
            FailureType.INFINIBAND_MISCONFIGURED,
            WalleAction.PROFILE,
            None,
        ),
    ],
)
@pytest.mark.parametrize("incomplete_reasons", [_get_incomplete_reasons(), _get_complete_reasons()])
def test_failed_infiniband_check_with_profile_repair(
    incomplete_reasons, fast, hw_checks_enabled, eine, reasons, failure_type, action, request_type
):
    host = Host(inv=100009, name="hostname-mock")
    host.get_project = lambda fields: Project(
        profile="profile-mock", vlan_scheme="vlan_scheme_mock", tags=["rtc.infiniband-enabled"]
    )

    incomplete_reasons[CheckType.INFINIBAND] = {
        "status": CheckStatus.FAILED,
        "metadata": {
            "result": drop_none(
                {
                    "status": HwWatcherCheckStatus.FAILED,
                    "reason": reasons,
                    "eine_code": eine,
                }
            )
        },
        "effective_timestamp": timestamp() - HW_WATCHER_CHECK_MAX_POSSIBLE_DELAY + 1,
    }

    rule = SingleCheckRule(rules.CheckInfiniband())

    if hw_checks_enabled:
        expected_decision = Decision(
            action=action,
            reason=get_reason_from_hw_watcher("infiniband", reasons),
            params=drop_none(
                {
                    "operation": Operation.PROFILE.type,
                    "errors": reasons,
                    "eine_code": eine,
                    "profile_mode": ProfileMode.FIRMWARE_UPDATE,
                }
            ),
            checks=[CheckType.INFINIBAND],
            failure_type=failure_type,
            restrictions=[restrictions.AUTOMATED_INFINIBAND_REPAIR, restrictions.AUTOMATED_PROFILE],
        )
        assert expected_decision == make_decision(host, incomplete_reasons, fast=fast, rule=rule)
    else:
        check_decision(incomplete_reasons, fast, WalleAction.HEALTHY, hw_checks_enabled=hw_checks_enabled, rule=rule)
