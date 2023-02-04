"""Tests automation limits."""

from unittest.mock import ANY, call

import pytest

import walle.projects as walle_projects
from infra.walle.server.tests.expert.dmc.util import possible_action_and_checks_combinations
from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_status_reasons,
    handle_failure,
    mock_task,
    mock_schedule_host_reboot,
    mock_schedule_host_profiling,
    mock_decision,
    handle_monitoring,
    mock_schedule_host_redeployment,
    mock_schedule_host_deactivation,
    patch,
    mock_schedule_disk_change,
    monkeypatch_audit_log,
    mock_schedule_report_task,
    mock_schedule_repair_rack_overheat_task,
    monkeypatch_clients_for_host,
    mock_schedule_repair_rack_task,
    mock_schedule_hardware_repair,
    mock_schedule_bmc_reset,
    CUSTOM_CHECK_TYPE,
    predefined_task_status,
    AUDIT_LOG_ID,
)
from sepelib.core import config, constants
from walle import audit_log, authorization
from walle.application import app
from walle.clients.eine import ProfileMode
from walle.constants import DNS_VLAN_SCHEMES
from walle.expert import failure_log
from walle.expert.automation import AutomationDisabledGloballyError, AutomationDisabledForProjectError, dns_automation
from walle.expert.automation.automation_context import AutomationDisabledError, CheckDisabledError
from walle.expert.automation.limits import check_total_limits
from walle.expert.decision import Decision
from walle.expert.types import WalleAction, get_limit_name, get_walle_check_type, CheckType
from walle.hosts import HostState, HostStatus, HealthStatus, TaskType
from walle.models import timestamp, TimedLimitDocument
from walle.stages import Stages
from walle.util.limits import parse_period, nice_period
from walle.util.misc import first

_ACTIONS = sorted(set(WalleAction.ALL_DMC) - {WalleAction.HEALTHY, WalleAction.WAIT})


def _nice_period(limit_data):
    return nice_period(parse_period(limit_data["period"]))


@pytest.fixture
def test(request, monkeypatch_timestamp, mp, monkeypatch_locks, mp_juggler_source):
    return TestCase.create(request)


@pytest.mark.parametrize(["action", "checks"], possible_action_and_checks_combinations(_ACTIONS))
@pytest.mark.parametrize("exceed_limit", (True, False))
def test_check_total_limits(mp, action, checks, exceed_limit):
    calls = []
    decision = Decision(action, "Reason-mock", checks=checks)

    def check_total_action_limits(failure, start_time, limits, project_id=None):
        calls.append((failure, start_time))
        return not exceed_limit

    mp.setattr(failure_log, "check_total_action_limits", check_total_action_limits)

    result, failure = check_total_limits(decision.failures, 99, config.get_value("automation"))

    if checks and exceed_limit:
        # only hit first limit
        expected_failure = get_walle_check_type(checks[0])
        assert calls == [(expected_failure, 99)]
    elif checks and not exceed_limit:
        expected_failure = None
        assert calls == [(get_walle_check_type(check), 99) for check in checks]
    else:
        expected_failure = action if exceed_limit else None
        assert calls == [(action, 99)]

    assert result == (not exceed_limit)
    assert failure == expected_failure


def test_global_timed_limits_overrides(walle_test, mp):
    calls = []
    check_type = CheckType.SSH
    limit = 3

    settings = app.settings()
    failure_name = get_limit_name(get_walle_check_type(check_type))
    settings.global_timed_limits_overrides[failure_name] = TimedLimitDocument("1h", limit)
    settings.save()

    def check_total_action_limits(failure, start_time, limits, project_id=None):
        if len(calls) >= limit:
            return False
        else:
            calls.append((failure, start_time))
            return True

    mp.setattr(failure_log, "check_total_action_limits", check_total_action_limits)

    decision = mock_decision(WalleAction.REBOOT, checks=[check_type])
    for _ in range(limit):
        result, failure = check_total_limits(decision.failures, 99, config.get_value("automation"))
        assert not failure
        assert result

    result, failure = check_total_limits(decision.failures, 99, config.get_value("automation"))
    assert failure
    assert not result


def test_too_many_dns_fixes(mp, test):
    mp.setitem(config.get_value("automation"), "max_dns_fixes", [{"period": "1h", "limit": 1}])

    project = test.mock_project({"id": "with-dns-support", "vlan_scheme": DNS_VLAN_SCHEMES[0]})
    host1 = test.mock_host({"inv": 100001, "name": "walle-dns-autotest-1.search.yandex.net", "project": project.id})
    host2 = test.mock_host({"inv": 100002, "name": "walle-dns-autotest-2.search.yandex.net", "project": project.id})

    decision = Decision(WalleAction.FIX_DNS, "reason-mock")
    automation = dns_automation(project.id)

    automation.register_automated_failure(host1, decision)

    with pytest.raises(AutomationDisabledGloballyError) as exc_info:
        automation.register_automated_failure(host2, decision)

    error_str = exc_info.value.args[0].lower()
    assert "too many failures has occurred with resolution 'fix-dns'" in error_str
    error_lines = error_str.split('\n')
    assert len(error_lines) == 3
    hosts_count = int(error_lines[1].split()[0])
    assert hosts_count == 2
    test.hosts.assert_equal()


@pytest.mark.parametrize(["action", "checks"], possible_action_and_checks_combinations(_ACTIONS))
@pytest.mark.parametrize(
    ["status", "task"], [(predefined_task_status(), mock_task(Stages.MONITOR)), (HostStatus.READY, None)]
)
@patch("walle.util.notifications.on_healing_automation_disabled", return_value=True)
def test_too_many_failures_globally(notify, mp, test, action, checks, status, task):
    decision = mock_decision(action, checks=checks)
    for failure in decision.failures:
        mp.setitem(config.get_value("automation"), get_limit_name(failure), [{"period": "1h", "limit": 0}])

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": status,
            "task": task,
        }
    )
    test.audit_log.mock({"id": AUDIT_LOG_ID, "type": audit_log.TYPE_NO_ACTION}, save=False)
    mock_audit_log = mp.function(audit_log.on_failure_processing_cancelled)

    assert config.get_value("automation.enabled")
    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation

    if status == HostStatus.READY:
        with pytest.raises(AutomationDisabledError):
            handle_failure(host, decision, reasons)
    else:
        with pytest.raises(AutomationDisabledError):
            handle_monitoring(host, decision, reasons)

    assert notify.mock_calls == [call(authorization.ISSUER_WALLE, reason=ANY)]
    assert app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation

    test.hosts.assert_equal()
    assert mock_audit_log.called


@pytest.mark.parametrize(
    ["status", "task"], [(predefined_task_status(), mock_task(Stages.MONITOR)), (HostStatus.READY, None)]
)
def test_no_global_limit_for_custom_checks(mp, test, status, task):
    decision = mock_decision(WalleAction.REBOOT, checks=[CUSTOM_CHECK_TYPE])
    mp.setitem(config.get_value("automation"), get_limit_name(CUSTOM_CHECK_TYPE), [{"period": "1h", "limit": 0}])

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host({"state": HostState.ASSIGNED, "status": status, "task": task})
    monkeypatch_audit_log(mp)

    assert config.get_value("automation.enabled")
    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation

    if status == HostStatus.READY:
        handle_failure(host, decision, reasons)
    else:
        handle_monitoring(host, decision, reasons)

    mock_schedule_host_reboot(host, manual=False, reason="Reason mock.", extra_checks=[CUSTOM_CHECK_TYPE])

    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation

    test.hosts.assert_equal()


@pytest.mark.parametrize(["action", "checks"], possible_action_and_checks_combinations(_ACTIONS))
@pytest.mark.parametrize(
    ["status", "task"], [(predefined_task_status(), mock_task(Stages.MONITOR)), (HostStatus.READY, None)]
)
@pytest.mark.parametrize("credit", ("none", "yes", "outdated"))
@patch("walle.util.notifications.on_healing_automation_disabled", return_value=True)
def test_too_many_health_failures_for_project(notify, mp, test, action, checks, credit, status, task):
    decision = mock_decision(action, checks=checks)
    limit_names = list(map(get_limit_name, decision.failures))

    project = test.mock_project({"id": "some-id"})
    assert project.healing_automation.enabled is True
    assert project.dns_automation.enabled is True

    limit_data = {"period": "1h", "limit": 0, "current": 1}
    for limit_name in limit_names:
        project.automation_limits[limit_name] = [{k: limit_data[k] for k in ["period", "limit"]}]

    if credit == "none":
        pass

    elif credit == "yes":
        for limit_name in limit_names:
            project.healing_automation.credit = {limit_name: 0}
        project.healing_automation.credit_end_time = timestamp() + constants.DAY_SECONDS

    elif credit == "outdated":
        for limit_name in limit_names:
            project.healing_automation.credit = {limit_name: 1}
        project.healing_automation.credit_end_time = timestamp()
    else:
        assert False

    project.save()

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host(
        {
            "project": project.id,
            "state": HostState.ASSIGNED,
            "status": status,
            "task": task,
        }
    )
    mock_audit_log = mp.function(audit_log.on_failure_processing_cancelled)

    assert config.get_value("automation.enabled")
    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation

    if status == HostStatus.READY:
        with pytest.raises(AutomationDisabledError):
            handle_failure(host, decision, reasons)
    else:
        with pytest.raises(AutomationDisabledForProjectError):
            handle_monitoring(host, decision, reasons)

    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation

    assert notify.mock_calls == [call(authorization.ISSUER_WALLE, host.project, reason=ANY)]

    test.hosts.assert_equal()
    assert mock_audit_log.called


@pytest.mark.parametrize(
    ["status", "task"], [(predefined_task_status(), mock_task(Stages.MONITOR)), (HostStatus.READY, None)]
)
@pytest.mark.parametrize("credit", ("none", "yes", "outdated"))
@patch("walle.util.notifications.on_automation_plot_check_disabled", return_value=True)
def test_too_many_failures_for_automation_plot(notify, mp, test, credit, status, task):
    custom_check_name = "custom_check_name"
    automation_plot = test.automation_plot.mock(
        {
            "checks": [
                {
                    "name": custom_check_name,
                    "enabled": True,
                    "reboot": True,
                    "redeploy": True,
                },
                {
                    "name": "some_other_check_name",
                    "enabled": True,
                    "reboot": True,
                    "redeploy": True,
                },
            ]
        }
    )

    decision = mock_decision(WalleAction.REBOOT, checks=[custom_check_name])
    limit_names = list(map(get_limit_name, decision.failures))

    project = test.mock_project({"id": "some-id"})
    assert project.healing_automation.enabled is True
    assert project.dns_automation.enabled is True

    limit_data = {"period": "1h", "limit": 0, "current": 1}
    for limit_name in limit_names:
        project.automation_limits[limit_name] = [{k: limit_data[k] for k in ["period", "limit"]}]

    if credit == "none":
        pass

    elif credit == "yes":
        for limit_name in limit_names:
            project.healing_automation.credit = {limit_name: 0}
        project.healing_automation.credit_end_time = timestamp() + constants.DAY_SECONDS

    elif credit == "outdated":
        for limit_name in limit_names:
            project.healing_automation.credit = {limit_name: 1}
        project.healing_automation.credit_end_time = timestamp()
    else:
        assert False

    project.save()

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host(
        {
            "project": project.id,
            "state": HostState.ASSIGNED,
            "status": status,
            "task": task,
        }
    )
    mock_audit_log = mp.function(audit_log.on_failure_processing_cancelled)

    assert config.get_value("automation.enabled")
    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation

    with pytest.raises(CheckDisabledError):
        if status == HostStatus.READY:
            handle_failure(host, decision, reasons, automation_plot=automation_plot)
        else:
            handle_monitoring(host, decision, reasons, automation_plot=automation_plot)

    test.hosts.assert_equal()
    test.projects.assert_equal()
    test.automation_plot.assert_equal()

    assert automation_plot.checks[0]["enabled"] is False
    assert mock_audit_log.called
    assert notify.mock_calls == [call(authorization.ISSUER_WALLE, automation_plot.id, custom_check_name, reason=ANY)]

    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation


@pytest.mark.parametrize("credit", ("none", "yes", "outdated"))
@patch("walle.util.notifications.on_dns_automation_disabled", return_value=True)
def test_too_many_dns_fixes_for_project(notify, mp, test, credit):
    limit_name = "max_dns_fixes"

    project = test.mock_project({"id": "some-id"})
    assert project.healing_automation.enabled is True
    assert project.dns_automation.enabled is True

    limit_data = {"period": "1h", "limit": 0, "current": 1}
    project.automation_limits[limit_name] = [{k: limit_data[k] for k in ["period", "limit"]}]

    if credit == "none":
        pass
    elif credit == "yes":
        project.dns_automation.credit = {limit_name: 0}
        project.dns_automation.credit_end_time = timestamp() + constants.DAY_SECONDS
    elif credit == "outdated":
        project.dns_automation.credit = {limit_name: 1}
        project.dns_automation.credit_end_time = timestamp()
    else:
        assert False

    project.save()

    host = test.mock_host({"project": project.id})
    mock_audit_log = mp.function(audit_log.on_failure_processing_cancelled)

    assert config.get_value("automation.enabled")
    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation

    decision = Decision(WalleAction.FIX_DNS, "reason-mock")
    automation = dns_automation(host.project)

    with pytest.raises(AutomationDisabledForProjectError):
        automation.register_automated_failure(host, decision)

    assert not app.settings().disable_healing_automation
    assert not app.settings().disable_dns_automation
    reason_start = "Too many failures has occurred with resolution '{}'".format(WalleAction.FIX_DNS)
    assert reason_start in notify.mock_calls[0][2]["reason"]

    test.hosts.assert_equal()
    db_project = walle_projects.get_by_id(project.id)
    db_message = db_project.dns_automation.status_message
    assert reason_start in db_message
    assert mock_audit_log.called


@pytest.mark.parametrize(["action", "checks"], possible_action_and_checks_combinations(_ACTIONS))
@patch("walle.expert.failure_log.register_failure")
def test_healing_automation_credit(register_failure, mp, test, action, checks):
    monkeypatch_audit_log(mp)

    decision = mock_decision(action, checks=checks)
    limit_names = list(map(get_limit_name, decision.failures))

    project = test.mock_project({"id": "some-id"})
    assert project.healing_automation.enabled is True
    assert project.dns_automation.enabled is True
    project.healing_automation.credit = {}
    for limit_name in limit_names:
        project.automation_limits[limit_name] = [{"period": "1h", "limit": 0}]
        project.healing_automation.credit[limit_name] = 1
        # and to assert we are not using wrong automation type here:
        project.dns_automation.credit = {limit_name: 1}

    project.healing_automation.credit_end_time = timestamp() + constants.DAY_SECONDS
    project.dns_automation.credit_end_time = timestamp() + constants.DAY_SECONDS
    project.save()

    host = test.mock_host(
        {
            "project": project.id,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
        }
    )
    monkeypatch_clients_for_host(mp, host)
    mock_audit_log = mp.function(audit_log.on_failure_processing_cancelled)

    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    handle_failure(host, decision, reasons)

    for limit_name in limit_names:
        project.healing_automation.credit[limit_name] -= 1

    if action == WalleAction.REBOOT:
        mock_schedule_host_reboot(host, manual=False, extra_checks=decision.checks, reason=decision.reason)
    elif action == WalleAction.PROFILE:
        mock_schedule_host_profiling(
            host,
            profile_mode=ProfileMode.HIGHLOAD_TEST,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif action == WalleAction.REDEPLOY:
        mock_schedule_host_redeployment(
            host,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif action == WalleAction.REPAIR_CPU:
        mock_schedule_host_profiling(
            host,
            profile_mode=ProfileMode.HIGHLOAD_TEST,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif action == WalleAction.REPAIR_MEMORY:
        mock_schedule_host_profiling(
            host,
            profile_mode=ProfileMode.BASIC_TEST,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif action == WalleAction.REPAIR_REBOOTS:
        mock_schedule_host_redeployment(
            host,
            custom_profile_mode=ProfileMode.EXTRA_HIGHLOAD_TEST,
            manual=False,
            extra_checks=decision.checks,
            failure=decision.failures[0],
            reason=decision.reason,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    elif action == WalleAction.REPAIR_HARDWARE:
        mock_schedule_hardware_repair(host, decision)
    elif action == WalleAction.RESET_BMC:
        mock_schedule_bmc_reset(host, reason=decision.reason, checks=decision.checks, failure=decision.failures[0])
    elif action == WalleAction.CHANGE_DISK:
        mock_schedule_disk_change(host, decision)
    elif action == WalleAction.REPORT_FAILURE:
        mock_schedule_report_task(host, checks, reason=decision.reason)
    elif action == WalleAction.REPAIR_RACK_FAILURE:
        mock_schedule_repair_rack_task(host, checks, reason=decision.reason)
    elif action == WalleAction.REPAIR_RACK_OVERHEAT:
        mock_schedule_repair_rack_overheat_task(host, checks, reason=decision.reason)
    elif action == WalleAction.DEACTIVATE:
        mock_schedule_host_deactivation(host, manual=False, reason=decision.reason)
    else:
        assert False

    checks = list(map(get_walle_check_type, checks)) if checks else None
    register_failure.assert_called_once_with(host, checks or [action], checks or [action], False)

    test.projects.assert_equal()
    test.hosts.assert_equal()
    assert not mock_audit_log.called


@patch("walle.expert.failure_log.register_failure")
@pytest.mark.parametrize("state", [HostState.ASSIGNED] + HostState.ALL_IGNORED_LIMITS_COUNTING)
def test_dns_automation_credit(register_failure, test, state):
    decision = mock_decision(WalleAction.FIX_DNS)
    limit_name = get_limit_name(first(decision.failures))

    project = test.mock_project({"id": "some-id"})
    assert project.healing_automation.enabled is True
    assert project.dns_automation.enabled is True
    project.automation_limits[limit_name] = [{"period": "1h", "limit": 0}]
    project.dns_automation.credit = {limit_name: 1}
    project.dns_automation.credit_end_time = timestamp() + constants.DAY_SECONDS
    # and to assert we are not using wrong automation type here
    project.healing_automation.credit = {limit_name: 1}
    project.healing_automation.credit_end_time = timestamp() + constants.DAY_SECONDS
    project.save()

    automation = dns_automation(project.id)
    host = test.mock_host({"project": project.id, "state": state})
    is_counting = state not in HostState.ALL_IGNORED_LIMITS_COUNTING

    # credited action
    automation.register_automated_failure(host, decision)

    if is_counting:
        project.dns_automation.credit[limit_name] -= 1
    # not credited action
    automation.register_automated_failure(host, decision)

    assert register_failure.mock_calls == [
        call(host, [decision.action], [decision.action] if is_counting else None, not is_counting),
        call(host, [decision.action], [] if is_counting else None, not is_counting),
    ]

    test.projects.assert_equal()
    test.hosts.assert_equal()
