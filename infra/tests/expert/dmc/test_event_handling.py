"""Tests handling host health status events."""

from unittest.mock import call, ANY

import pytest

from infra.walle.server.tests.expert.dmc.util import possible_action_and_checks_combinations
from infra.walle.server.tests.lib.util import (
    mock_decision,
    TestCase,
    mock_schedule_host_reboot,
    mock_schedule_host_profiling,
    mock_schedule_host_redeployment,
    mock_schedule_host_deactivation,
    mock_status_reasons,
    mock_task,
    monkeypatch_clients_for_host,
    patch,
    mock_schedule_disk_change,
    handle_monitoring,
    mock_host_health_status,
    mock_host_health_checks,
    mock_schedule_report_task,
    AUDIT_LOG_ID,
    mock_schedule_repair_rack_task,
    any_task_status,
    patch_attr,
    mock_schedule_hardware_repair,
    mock_schedule_repair_rack_overheat_task,
    mock_schedule_bmc_reset,
    monkeypatch_config,
)
from walle import restrictions, authorization
from walle.clients.eine import ProfileMode
from walle.expert import dmc
from walle.expert.decision import Decision
from walle.expert.decisionmakers import get_decision_maker
from walle.expert.juggler import get_health_status_reasons
from walle.expert.types import WalleAction, CheckType, CheckStatus
from walle.hosts import Host, HostState, HostStatus, HealthStatus, TaskType
from walle.models import timestamp
from walle.operations_log.constants import Operation
from walle.stages import Stages


@pytest.fixture()
def test(monkeypatch_timestamp, request, mp_juggler_source):
    return TestCase.create(request)


def decisions_with_possible_action_and_checks_combinations():
    for action, checks in possible_action_and_checks_combinations():
        if action == WalleAction.PROFILE:
            # one more available option for profile decision
            yield mock_decision(action, checks=checks, params={"profile_mode": ProfileMode.DISK_RW_TEST})

        params = None
        if action == WalleAction.REPAIR_HARDWARE:
            params = {"request_type": "mock", "operation": Operation.REPAIR_HARDWARE.type}
        if action == WalleAction.REPAIR_CPU:
            params = {"operation": Operation.REPAIR_CPU.type}
        if action == WalleAction.REPAIR_MEMORY:
            params = {"operation": Operation.REPAIR_MEMORY.type}

        yield mock_decision(action, checks=checks, params=params)


def test_ok_for_ready_host(test):
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status(status=HealthStatus.STATUS_OK),
        }
    )

    decision_maker = get_decision_maker(test.default_project)
    reasons = mock_status_reasons(status=HealthStatus.STATUS_OK)
    decision = decision_maker.make_decision(host, reasons)

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    test.hosts.assert_equal()
    test.failure_log.assert_equal()


class TestHealthyForDeadHost:
    @pytest.mark.usefixtures("monkeypatch_audit_log")
    def test_healthy_for_dead_host_forced_host_to_ready(self, test):
        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "status": HostStatus.DEAD,
                "status_author": authorization.ISSUER_WALLE,
                "health": mock_host_health_status(status=HealthStatus.STATUS_OK),
            }
        )

        decision_maker = get_decision_maker(test.default_project)
        reasons = mock_status_reasons(status=HealthStatus.STATUS_OK)
        decision = decision_maker.make_decision(host, reasons)

        dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

        host.set_status(
            HostStatus.READY, authorization.ISSUER_WALLE, AUDIT_LOG_ID, reason="Host is available.", confirmed=False
        )

        test.hosts.assert_equal()
        test.failure_log.assert_equal()

    @pytest.mark.usefixtures("monkeypatch_audit_log")
    def test_healthy_for_dead_host_forces_only_hosts_with_walle_as_status_author(self, test):
        # We should not force to ready hosts that were forced to dead either manually or by manually scheduled task.
        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "status": HostStatus.DEAD,
                "status_author": test.api_issuer,
                "health": mock_host_health_status(status=HealthStatus.STATUS_OK),
            }
        )

        decision_maker = get_decision_maker(test.default_project)
        reasons = mock_status_reasons(status=HealthStatus.STATUS_OK)
        decision = decision_maker.make_decision(host, reasons)

        dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

        # did nothing
        test.hosts.assert_equal()
        test.failure_log.assert_equal()

    @pytest.mark.usefixtures("monkeypatch_audit_log")
    def test_healthy_for_dead_host_forces_only_dead_hosts(self, test):
        # We should not force to ready hosts that were forced to other statuses
        host = test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "status": HostStatus.MANUAL,
                "status_author": authorization.ISSUER_WALLE,
                "health": mock_host_health_status(status=HealthStatus.STATUS_OK),
            }
        )

        decision_maker = get_decision_maker(test.default_project)
        reasons = mock_status_reasons(status=HealthStatus.STATUS_OK)
        decision = decision_maker.make_decision(host, reasons)

        dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

        # did nothing
        test.hosts.assert_equal()
        test.failure_log.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("old_check_type", CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE)
def test_check_min_times_status_reasons(old_check_type):
    enabled_checks = CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE + CheckType.ALL_WALLE + CheckType.ALL_NETMON

    curr_time = timestamp()
    check_mock = {
        "type": old_check_type,
        "timestamp": curr_time,
        "status": CheckStatus.PASSED,
        "stale_timestamp": curr_time,
        "status_mtime": curr_time,
        "effective_timestamp": curr_time - 1,
        "metadata": {},
    }
    checks = mock_host_health_checks(
        status=HealthStatus.STATUS_FAILURE,
        check_status=CheckStatus.PASSED,
        effective_timestamp=curr_time,
        check_overrides=[check_mock],
        enabled_checks=enabled_checks,
    )

    expected_failure_reasons = {}
    for check in checks:
        expected_failure_reasons[check["type"]] = {
            "status": CheckStatus.UNSURE if check["type"] == old_check_type else check["status"],
            "metadata": check["metadata"],
            "timestamp": check["timestamp"],
            "status_mtime": check["status_mtime"],
            "effective_timestamp": check["effective_timestamp"],
            "stale_timestamp": check["stale_timestamp"],
        }

    failure_reasons = get_health_status_reasons(checks, enabled_checks, check_min_time=curr_time)
    assert expected_failure_reasons == failure_reasons


@pytest.mark.parametrize("old_check_type", CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE)
def test_check_stale_time_status_reasons(old_check_type):
    enabled_checks = CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE + CheckType.ALL_WALLE + CheckType.ALL_NETMON

    curr_time = timestamp()
    check_mock = {
        "type": old_check_type,
        "timestamp": curr_time,
        "status": CheckStatus.PASSED,
        "stale_timestamp": curr_time - 1,
        "status_mtime": curr_time,  # wall-e does not even look at these timestamps for staled checks
        "effective_timestamp": curr_time,  # wall-e does not even look at these timestamps for staled checks
        "metadata": {},
    }
    checks = mock_host_health_checks(
        status=HealthStatus.STATUS_FAILURE,
        check_status=CheckStatus.PASSED,
        effective_timestamp=curr_time,
        check_overrides=[check_mock],
        enabled_checks=enabled_checks,
    )

    expected_failure_reasons = {}
    for check in checks:
        expected_failure_reasons[check["type"]] = {
            "status": CheckStatus.STALED if check["type"] == old_check_type else check["status"],
            "metadata": check["metadata"],
            "timestamp": check["timestamp"],
            "status_mtime": check["status_mtime"],
            "effective_timestamp": check["effective_timestamp"],
            "stale_timestamp": check["stale_timestamp"],
        }

    failure_reasons = get_health_status_reasons(checks, enabled_checks, check_min_time=curr_time)
    assert expected_failure_reasons == failure_reasons


@pytest.mark.usefixtures("monkeypatch_audit_log", "monkeypatch_locks")
def test_logs_failure_with_failure_logger(mp, test):
    decision_maker = get_decision_maker(test.default_project)
    decision = Decision(WalleAction.REBOOT, reason="host is dead", checks=["ssh"], failures=["ssh-failed"])
    mp.method(decision_maker.make_decision, return_value=decision)

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    project = test.mock_project({"id": "test-project-mock", "tags": ["rtc", "yt"]})
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "project": project.id,
            "health": mock_host_health_status(reasons=reasons),
        }
    )

    monkeypatch_clients_for_host(mp, host)
    failures_logger_mock = patch_attr(mp, dmc, "failures_logger")

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    failures_logger_mock().log.assert_called_once_with(
        inv=host.inv,
        hostname=host.name,
        project=project.id,
        tags=project.tags,
        failure_type="ssh-failed",
        failure_timestamp=timestamp(),
        repair_begin_timestamp=timestamp(),
    )


@patch("walle.expert.failure_log.register_failure")
@pytest.mark.parametrize("decision", decisions_with_possible_action_and_checks_combinations())
@pytest.mark.usefixtures("monkeypatch_audit_log", "monkeypatch_locks")
@pytest.mark.parametrize("use_cloud_post_processor", (True, False))
def test_failure_for_ready_host(register_failure, mp, test, decision, use_cloud_post_processor):
    expected_failures = []

    decision_maker = get_decision_maker(test.default_project)
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status(reasons=reasons),
        }
    )

    if use_cloud_post_processor:
        monkeypatch_config(mp, "projects_with_cloud_task_post_processor", [host.project])

    monkeypatch_clients_for_host(mp, host)

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    if decision.action not in (WalleAction.HEALTHY, WalleAction.WAIT):
        expected_failures.append(call(host, decision.walle_checks or [decision.action], [], False))

    if decision.action in (WalleAction.HEALTHY, WalleAction.WAIT):
        pass
    elif decision.action == WalleAction.REBOOT:
        mock_schedule_host_reboot(
            host,
            manual=False,
            extra_checks=decision.checks,
            reason=decision.reason,
            use_cloud_post_processor=use_cloud_post_processor,
        )
    elif decision.action == WalleAction.PROFILE:
        if decision.get_param("profile_mode") == ProfileMode.DISK_RW_TEST:
            mock_schedule_host_profiling(
                host,
                profile_mode=decision.params["profile_mode"],
                provisioner=host.provisioner,
                deploy_config=host.config,
                manual=False,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                use_cloud_post_processor=use_cloud_post_processor,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        else:
            mock_schedule_host_profiling(
                host,
                profile_mode=ProfileMode.HIGHLOAD_TEST,
                manual=False,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                use_cloud_post_processor=use_cloud_post_processor,
                task_type=TaskType.AUTOMATED_HEALING,
            )
    elif decision.action == WalleAction.REDEPLOY:
        mock_schedule_host_redeployment(
            host,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif decision.action == WalleAction.REPAIR_HARDWARE:
        mock_schedule_hardware_repair(host, decision, use_cloud_post_processor=use_cloud_post_processor)
    elif decision.action == WalleAction.RESET_BMC:
        mock_schedule_bmc_reset(
            host,
            reason=decision.reason,
            checks=decision.checks,
            failure=decision.failures[0],
            use_cloud_post_processor=use_cloud_post_processor,
        )
    elif decision.action == WalleAction.REPAIR_CPU:
        mock_schedule_host_profiling(
            host,
            profile_mode=ProfileMode.HIGHLOAD_TEST,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            operation_type=Operation.REPAIR_CPU.type,
            use_cloud_post_processor=use_cloud_post_processor,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif decision.action == WalleAction.REPAIR_MEMORY:
        mock_schedule_host_profiling(
            host,
            profile_mode=ProfileMode.BASIC_TEST,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            operation_type=Operation.REPAIR_MEMORY.type,
            use_cloud_post_processor=use_cloud_post_processor,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif decision.action == WalleAction.REPAIR_REBOOTS:
        mock_schedule_host_redeployment(
            host,
            custom_profile_mode=ProfileMode.EXTRA_HIGHLOAD_TEST,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif decision.action == WalleAction.CHANGE_DISK:
        mock_schedule_disk_change(host, decision)
    elif decision.action == WalleAction.REPORT_FAILURE:
        mock_schedule_report_task(host, decision.checks, reason=decision.reason)
    elif decision.action == WalleAction.REPAIR_RACK_FAILURE:
        mock_schedule_repair_rack_task(host, decision.checks, reason=decision.reason)
    elif decision.action == WalleAction.REPAIR_RACK_OVERHEAT:
        mock_schedule_repair_rack_overheat_task(host, decision.checks, reason=decision.reason)
    elif decision.action == WalleAction.DEACTIVATE:
        mock_schedule_host_deactivation(host, manual=False, reason=decision.reason)
    else:
        assert False

    test.hosts.assert_equal()
    assert register_failure.mock_calls == expected_failures


@patch("walle.expert.failure_log.register_failure")
@pytest.mark.parametrize("decision", list(decisions_with_possible_action_and_checks_combinations()))
@pytest.mark.parametrize("status", set(HostStatus.ALL) - {HostStatus.READY})
@pytest.mark.usefixtures("monkeypatch_audit_log", "monkeypatch_locks")
def test_failure_for_non_ready_host(register_failure, test, decision, status):
    expected_failures = []

    decision_maker = get_decision_maker(test.default_project)
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": status,
            "task": None if status in HostStatus.ALL_STEADY else mock_task(stage=Stages.MONITOR),
            "health": None,  # hosts with task may have health missing, this is a legitimate case
        }
    )

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    if decision.action not in (WalleAction.HEALTHY, WalleAction.WAIT):
        expected_failures.append(call(host, decision.walle_checks or [decision.action], []))

    test.hosts.assert_equal()
    assert not register_failure.called


@pytest.mark.parametrize("task_type", set(TaskType.ALL) - {TaskType.AUTOMATED_HEALING})
@pytest.mark.parametrize("enable_auto_healing", (None, False, True))
@pytest.mark.parametrize("decision", list(decisions_with_possible_action_and_checks_combinations()))
@pytest.mark.parametrize("restrictions", (None, [restrictions.AUTOMATION]))
@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_failed_non_healing_task(mp, test, task_type, enable_auto_healing, decision, restrictions):
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "restrictions": restrictions,
            "task": mock_task(
                type=task_type, owner=test.api_issuer, stage=Stages.MONITOR, enable_auto_healing=enable_auto_healing
            ),
            "health": None,  # hosts with task can miss health, this is a legitimate case
        }
    )
    monkeypatch_clients_for_host(mp, host)

    dmc_decision = handle_monitoring(host, decision, reasons)

    if decision.action in (WalleAction.HEALTHY, WalleAction.WAIT):
        # return decision but do not perform any actions.
        assert dmc_decision == decision
    elif enable_auto_healing and restrictions is None:
        # return decision and schedule action
        assert dmc_decision == decision

        if decision.action == WalleAction.REBOOT:
            mock_schedule_host_reboot(
                host, manual=False, issuer=test.api_issuer, extra_checks=decision.checks, reason=decision.reason
            )
        elif decision.action == WalleAction.PROFILE:
            if decision.get_param("profile_mode") == ProfileMode.DISK_RW_TEST:
                mock_schedule_host_profiling(
                    host,
                    profile_mode=decision.params["profile_mode"],
                    provisioner=host.provisioner,
                    deploy_config=host.config,
                    manual=False,
                    issuer=test.api_issuer,
                    extra_checks=decision.checks,
                    failure=decision.failures[0],
                    reason=decision.reason,
                    task_type=TaskType.AUTOMATED_HEALING,
                )
            else:
                mock_schedule_host_profiling(
                    host,
                    profile_mode=ProfileMode.HIGHLOAD_TEST,
                    manual=False,
                    issuer=test.api_issuer,
                    extra_checks=decision.checks,
                    failure=decision.failures[0],
                    reason=decision.reason,
                    task_type=TaskType.AUTOMATED_HEALING,
                )
        elif decision.action == WalleAction.REDEPLOY:
            mock_schedule_host_redeployment(
                host,
                manual=False,
                issuer=test.api_issuer,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        elif decision.action == WalleAction.REPAIR_HARDWARE:
            mock_schedule_hardware_repair(host, decision, issuer=test.api_issuer)
        elif decision.action == WalleAction.RESET_BMC:
            mock_schedule_bmc_reset(
                host,
                issuer=test.api_issuer,
                reason=decision.reason,
                checks=decision.checks,
                failure=decision.failures[0],
            )
        elif decision.action == WalleAction.REPAIR_CPU:
            mock_schedule_host_profiling(
                host,
                profile_mode=ProfileMode.HIGHLOAD_TEST,
                manual=False,
                issuer=test.api_issuer,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                operation_type=Operation.REPAIR_CPU.type,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        elif decision.action == WalleAction.REPAIR_MEMORY:
            mock_schedule_host_profiling(
                host,
                profile_mode=ProfileMode.BASIC_TEST,
                manual=False,
                issuer=test.api_issuer,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                operation_type=Operation.REPAIR_MEMORY.type,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        elif decision.action == WalleAction.REPAIR_REBOOTS:
            mock_schedule_host_redeployment(
                host,
                custom_profile_mode=ProfileMode.EXTRA_HIGHLOAD_TEST,
                manual=False,
                issuer=test.api_issuer,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        elif decision.action == WalleAction.CHANGE_DISK:
            mock_schedule_disk_change(host, decision, issuer=test.api_issuer)
        elif decision.action == WalleAction.REPORT_FAILURE:
            mock_schedule_report_task(host, decision.checks, issuer=test.api_issuer, reason=decision.reason)
        elif decision.action == WalleAction.REPAIR_RACK_FAILURE:
            mock_schedule_repair_rack_task(host, decision.checks, issuer=test.api_issuer, reason=decision.reason)
        elif decision.action == WalleAction.REPAIR_RACK_OVERHEAT:
            mock_schedule_repair_rack_overheat_task(
                host, decision.checks, issuer=test.api_issuer, reason=decision.reason
            )
        elif decision.action == WalleAction.DEACTIVATE:
            mock_schedule_host_deactivation(host, manual=False, issuer=test.api_issuer, reason=decision.reason)
        else:
            assert False
    elif enable_auto_healing and decision.action == WalleAction.DEACTIVATE:
        mock_schedule_host_deactivation(host, manual=False, issuer=test.api_issuer, reason=decision.reason)

    elif enable_auto_healing:  # restrictions applied, can't heal, must deactivate.
        assert dmc_decision == Decision.failure(ANY)
    else:
        assert dmc_decision == Decision.failure(reason=ANY)

    test.hosts.assert_equal()


@patch("walle.expert.failure_log.register_failure")
@pytest.mark.usefixtures("monkeypatch_audit_log")
@pytest.mark.parametrize("restriction", [None] + restrictions.ALL)
@pytest.mark.parametrize("decision", list(decisions_with_possible_action_and_checks_combinations()))
def test_failed_healing(register_failure, mp, test, restriction, decision):
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "restrictions": None if restriction is None else [restriction],
            "task": mock_task(type=TaskType.AUTOMATED_HEALING, stage=Stages.MONITOR),
            "health": None,  # hosts with task can miss health, this is a legitimate case
        }
    )

    clients = monkeypatch_clients_for_host(mp, host)

    dmc_decision = handle_monitoring(host, decision, reasons)

    expected_failures = []
    expected_decision = decision
    expected_client_calls = []

    if decision.action in (WalleAction.HEALTHY, WalleAction.WAIT):
        pass
    elif (
        decision.action != WalleAction.DEACTIVATE
        and host.applied_restrictions(restrictions.AUTOMATION, restrictions.AUTOMATED_HEALING)
        or decision.action in (WalleAction.REBOOT, WalleAction.PROFILE, WalleAction.CHANGE_DISK, WalleAction.REDEPLOY)
        and host.applied_restrictions(restrictions.REBOOT, restrictions.AUTOMATED_REBOOT)
        or decision.action == WalleAction.PROFILE
        and host.applied_restrictions(restrictions.PROFILE, restrictions.AUTOMATED_PROFILE)
        or decision.action == WalleAction.PROFILE
        and host.applied_restrictions(
            restrictions.PROFILE, restrictions.AUTOMATED_PROFILE, restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP
        )
        or decision.action == WalleAction.PROFILE
        and decision.get_param("profile_mode") == ProfileMode.DISK_RW_TEST
        and host.applied_restrictions(
            restrictions.REDEPLOY,
            restrictions.AUTOMATED_REDEPLOY,
            restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
        )
        or decision.action == WalleAction.PROFILE
        and decision.get_param("profile_mode") == ProfileMode.DISK_RW_TEST
        and host.applied_restrictions(restrictions.REDEPLOY, restrictions.AUTOMATED_REDEPLOY)
        or decision.action == WalleAction.REPAIR_HARDWARE
        and host.applied_restrictions(restrictions.AUTOMATED_REPAIRING)
        or decision.action == WalleAction.RESET_BMC
        and host.applied_restrictions(
            restrictions.AUTOMATED_BMC_REPAIR,
            restrictions.REBOOT,
            restrictions.AUTOMATED_REBOOT,
            restrictions.AUTOMATED_REPAIRING,
        )
        or decision.action == WalleAction.REPAIR_CPU
        and host.applied_restrictions(
            restrictions.REBOOT,
            restrictions.AUTOMATED_REBOOT,
            restrictions.PROFILE,
            restrictions.AUTOMATED_PROFILE,
            restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
            restrictions.AUTOMATED_REPAIRING,
            restrictions.AUTOMATED_CPU_REPAIR,
        )
        or decision.action == WalleAction.REPAIR_MEMORY
        and host.applied_restrictions(
            restrictions.REBOOT,
            restrictions.AUTOMATED_REBOOT,
            restrictions.PROFILE,
            restrictions.AUTOMATED_PROFILE,
            restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
            restrictions.AUTOMATED_REPAIRING,
            restrictions.AUTOMATED_MEMORY_REPAIR,
        )
        or decision.action == WalleAction.REPAIR_REBOOTS
        and host.applied_restrictions(
            restrictions.REBOOT,
            restrictions.AUTOMATED_REBOOT,
            restrictions.PROFILE,
            restrictions.AUTOMATED_PROFILE,
            restrictions.AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
            restrictions.REDEPLOY,
            restrictions.AUTOMATED_REDEPLOY,
        )
        or decision.action == WalleAction.CHANGE_DISK
        and host.applied_restrictions(
            restrictions.AUTOMATED_DISK_CHANGE,
            restrictions.AUTOMATED_REPAIRING,
            restrictions.REDEPLOY,
            restrictions.AUTOMATED_REDEPLOY,
        )
        or decision.action == WalleAction.REDEPLOY
        and host.applied_restrictions(restrictions.REDEPLOY, restrictions.AUTOMATED_REDEPLOY)
        or decision.action == WalleAction.REPORT_FAILURE
        and host.applied_restrictions(restrictions.AUTOMATED_HEALING)
        or decision.action == WalleAction.REPAIR_RACK_FAILURE
        and host.applied_restrictions(restrictions.AUTOMATED_RACK_REPAIR, restrictions.AUTOMATED_REPAIRING)
        or decision.action == WalleAction.REPAIR_RACK_OVERHEAT
        and host.applied_restrictions(restrictions.AUTOMATED_RACK_REPAIR, restrictions.AUTOMATED_REPAIRING)
    ):
        expected_failures.append(call(host, decision.walle_checks or [decision.action], [], False))
        expected_decision = Decision.failure(reason=ANY)

    else:
        expected_failures.append(call(host, decision.walle_checks or [decision.action], [], False))

        if decision.action == WalleAction.REBOOT:
            mock_schedule_host_reboot(host, manual=False, extra_checks=decision.checks, reason=decision.reason)
        elif decision.action == WalleAction.PROFILE:
            if decision.get_param("profile_mode") == ProfileMode.DISK_RW_TEST:
                mock_schedule_host_profiling(
                    host,
                    profile_mode=decision.params["profile_mode"],
                    provisioner=host.provisioner,
                    deploy_config=host.config,
                    manual=False,
                    extra_checks=decision.checks,
                    failure=decision.failures[0],
                    reason=decision.reason,
                    task_type=TaskType.AUTOMATED_HEALING,
                )
            else:
                mock_schedule_host_profiling(
                    host,
                    profile_mode=ProfileMode.HIGHLOAD_TEST,
                    manual=False,
                    extra_checks=decision.checks,
                    failure=decision.failures[0],
                    reason=decision.reason,
                    task_type=TaskType.AUTOMATED_HEALING,
                )
        elif decision.action == WalleAction.REDEPLOY:
            mock_schedule_host_redeployment(
                host,
                manual=False,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        elif decision.action == WalleAction.REPAIR_CPU:
            mock_schedule_host_profiling(
                host,
                profile_mode=ProfileMode.HIGHLOAD_TEST,
                manual=False,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                operation_type=Operation.REPAIR_CPU.type,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        elif decision.action == WalleAction.REPAIR_MEMORY:
            mock_schedule_host_profiling(
                host,
                profile_mode=ProfileMode.BASIC_TEST,
                manual=False,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                operation_type=Operation.REPAIR_MEMORY.type,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        elif decision.action == WalleAction.REPAIR_REBOOTS:
            mock_schedule_host_redeployment(
                host,
                custom_profile_mode=ProfileMode.EXTRA_HIGHLOAD_TEST,
                manual=False,
                extra_checks=decision.checks,
                failure=decision.failures[0],
                reason=decision.reason,
                task_type=TaskType.AUTOMATED_HEALING,
            )
        elif decision.action == WalleAction.REPAIR_HARDWARE:
            mock_schedule_hardware_repair(host, decision)
        elif decision.action == WalleAction.RESET_BMC:
            mock_schedule_bmc_reset(host, reason=decision.reason, checks=decision.checks, failure=decision.failures[0])
        elif decision.action == WalleAction.CHANGE_DISK:
            mock_schedule_disk_change(host, decision)
        elif decision.action == WalleAction.REPORT_FAILURE:
            mock_schedule_report_task(host, decision.checks, reason=decision.reason)
        elif decision.action == WalleAction.REPAIR_RACK_FAILURE:
            mock_schedule_repair_rack_task(host, decision.checks, reason=decision.reason)
        elif decision.action == WalleAction.REPAIR_RACK_OVERHEAT:
            mock_schedule_repair_rack_overheat_task(host, decision.checks, reason=decision.reason)
        elif decision.action == WalleAction.DEACTIVATE:
            mock_schedule_host_deactivation(host, manual=False, reason=decision.reason)
        else:
            assert False

    assert clients.mock_calls == expected_client_calls
    assert register_failure.mock_calls == expected_failures
    assert dmc_decision == expected_decision

    if host.task is not None:
        # Several audit log entries may be created during the test, so we can't monkeypatch audit log UUID
        Host.objects(inv=host.inv).update(set__task__audit_log_id=host.task.audit_log_id)

    test.hosts.assert_equal()
