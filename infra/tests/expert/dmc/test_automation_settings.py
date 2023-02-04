"""Tests event handling with various automation settings."""

from unittest import mock

import pytest

import walle.clients.dns.slayer_dns_api
from infra.walle.server.tests.lib.util import (
    any_task_status,
    patch,
    monkeypatch_locks,
    mock_status_reasons,
    mock_task,
    handle_failure,
    fix_dns,
    mock_schedule_host_reboot,
    mock_schedule_host_deactivation,
    mock_host_health_status,
)
from sepelib.core import config
from sepelib.core.constants import HOUR_SECONDS
from walle import restrictions
from walle.application import app
from walle.constants import VLAN_SCHEME_SEARCH
from walle.expert import dmc, decisionmakers
from walle.expert.automation import AutomationDisabledError
from walle.expert.decision import Decision
from walle.expert.failure_types import FailureType
from walle.expert.types import WalleAction, CheckType
from walle.hosts import HostState, HostStatus, HealthStatus
from walle.models import timestamp
from walle.stages import Stages


@pytest.mark.usefixtures("monkeypatch_audit_log", "mock_automation_plot")
def test_failure_with_disabled_global_automation(test, mp):
    monkeypatch_locks(mp)

    settings = app.settings()
    host = test.mock_host(
        {"state": HostState.ASSIGNED, "status": HostStatus.READY, "health": mock_host_health_status()}
    )

    enabled_checks = CheckType.ALL_AVAILABILITY + CheckType.ALL_INFRASTRUCTURE
    decision_maker = decisionmakers.get_decision_maker(test.default_project, enabled_checks=enabled_checks)
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE, enabled_checks=enabled_checks)
    decision = decision_maker.make_decision(host, reasons)

    settings.disable_healing_automation = True
    settings.disable_dns_automation = False
    settings.save()

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    # don't change host's status, automation is disabled
    test.hosts.assert_equal()
    test.failure_log.assert_equal()

    settings.disable_healing_automation = False
    settings.disable_dns_automation = False
    settings.save()
    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    # change host's status, automation is enabled
    mock_schedule_host_reboot(
        host,
        manual=False,
        extra_checks=decision.checks,
        reason="Host is not available: unreachable, ssh.",
        failure_type=FailureType.AVAILABILITY,
    )
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log", "mock_automation_plot")
def test_failure_with_disabled_project_automation(test, mp):
    monkeypatch_locks(mp)

    project = test.mock_project(
        {
            "id": "some-id",
            "healing_automation": {"enabled": False},
            "dns_automation": {"enabled": True},
        }
    )
    host = test.mock_host(
        {
            "project": project.id,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status(),
        }
    )

    enabled_checks = CheckType.ALL_AVAILABILITY + CheckType.ALL_INFRASTRUCTURE
    decision_maker = decisionmakers.get_decision_maker(project, enabled_checks=enabled_checks)
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE, enabled_checks=enabled_checks)
    decision = decision_maker.make_decision(host, reasons)

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    test.hosts.assert_equal()
    test.failure_log.assert_equal()

    project.healing_automation.enabled = True
    project.save()
    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    # change host's status, automation is enabled
    mock_schedule_host_reboot(
        host,
        manual=False,
        extra_checks=decision.checks,
        reason="Host is not available: unreachable, ssh.",
        failure_type=FailureType.AVAILABILITY,
    )

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log", "mock_automation_plot")
@pytest.mark.parametrize("state", set(HostState.ALL) - set(HostState.ALL_DMC))
def test_do_not_process_in_forbidden_states(test, mp, state):
    monkeypatch_locks(mp)

    project = test.mock_project(
        {
            "id": "some-id",
            "healing_automation": {"enabled": False},
            "dns_automation": {"enabled": True},
        }
    )
    host = test.mock_host(
        {"project": project.id, "state": state, "status": HostStatus.READY, "health": mock_host_health_status()}
    )

    enabled_checks = CheckType.ALL_AVAILABILITY + CheckType.ALL_INFRASTRUCTURE
    decision_maker = decisionmakers.get_decision_maker(project, enabled_checks=enabled_checks)
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE, enabled_checks=enabled_checks)
    decision = decision_maker.make_decision(host, reasons)

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    test.hosts.assert_equal()
    test.failure_log.assert_equal()


@pytest.mark.parametrize("restriction", (restrictions.AUTOMATED_HEALING, restrictions.AUTOMATION, restrictions.REBOOT))
@pytest.mark.usefixtures("monkeypatch_audit_log", "mock_automation_plot")
def test_failure_with_restricted_automation(test, mp, restriction):
    monkeypatch_locks(mp)

    enabled_checks = CheckType.ALL_AVAILABILITY + CheckType.ALL_INFRASTRUCTURE
    decision_maker = decisionmakers.get_decision_maker(test.default_project, enabled_checks=enabled_checks)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "restrictions": [restriction],
            "health": mock_host_health_status(),
        }
    )
    reasons = mock_status_reasons(
        juggler_check_time=timestamp(),
        status_mtime=timestamp() - HOUR_SECONDS,
        effective_timestamp=timestamp(),
        status=HealthStatus.STATUS_FAILURE,
        enabled_checks=enabled_checks,
    )
    decision = decision_maker.make_decision(host, reasons)

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)

    # host deactivated instead of reboot because reboot is forbidden.
    reason = (
        "Host is not available: unreachable, ssh. "
        "Action 'reboot' can not be performed: "
        "Operation restricted for this host. "
        "Restrictions applied: [{}], operation restrictions: [{}]. "
        "Deactivating host."
    ).format(restriction, restrictions.AUTOMATED_REBOOT)
    mock_schedule_host_deactivation(host, manual=False, reason=reason)
    test.hosts.assert_equal()


class TestFailureHandling:
    @pytest.fixture
    def dmc_can_act(self, test):
        self.project = test.mock_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": True},
                "dns_automation": {"enabled": True},
                "vlan_scheme": VLAN_SCHEME_SEARCH,
                "dns_domain": "wall-e.search.yandex.net",
            }
        )
        self.host = test.mock_host(
            {
                "name": "mock-host.wall-e.search.yandex.net",
                "project": self.project.id,
                "state": HostState.ASSIGNED,
                "status": HostStatus.READY,
            }
        )

    @pytest.fixture(
        params=[
            (any_task_status(), Stages.MONITOR),
            (HostStatus.MANUAL, None),
            (HostStatus.DEAD, None),
        ]
    )
    def dmc_can_not_act(self, request, test):
        status, stage = request.param

        self.project = test.mock_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": True},
                "dns_automation": {"enabled": True},
            }
        )
        self.host = test.mock_host(
            {
                "project": self.project.id,
                "state": HostState.ASSIGNED,
                "status": status,
                "task": None if stage is None else mock_task(stage=stage),
            }
        )

    def assert_action_not_taken(self, test, raises=True):
        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        decision = Decision(WalleAction.REBOOT, "reason-mock", checks=CheckType.ALL_AVAILABILITY)

        if raises:
            with pytest.raises(AutomationDisabledError):
                handle_failure(self.host, decision, reasons)
        else:
            handle_failure(self.host, decision, reasons)

        test.hosts.assert_equal()
        test.failure_log.assert_equal()

    @pytest.mark.usefixtures("dmc_can_act")
    def test_global_automation_disabled_via_config_and_dmc_can_act(self, test):
        with mock.patch.dict(config.get_value("automation"), {"enabled": False}):
            self.assert_action_not_taken(test)

    @pytest.mark.usefixtures("dmc_can_not_act")
    def test_global_automation_disabled_via_config_and_dmc_can_not_act(self, test):
        with mock.patch.dict(config.get_value("automation"), {"enabled": False}):
            self.assert_action_not_taken(test, raises=False)

    @pytest.mark.usefixtures("dmc_can_act")
    def test_global_automation_disabled_via_instance_settings_and_dmc_can_act(self, test):
        with mock.patch.dict(config.get_value("automation"), {"enabled": True}):
            settings = app.settings()
            settings.disable_healing_automation = True
            settings.disable_dns_automation = False
            settings.save()

            self.assert_action_not_taken(test)

    @pytest.mark.usefixtures("dmc_can_not_act")
    def test_global_automation_disabled_via_instance_settings_and_dmc_can_not_act(self, test):
        with mock.patch.dict(config.get_value("automation"), {"enabled": True}):
            settings = app.settings()
            settings.disable_healing_automation = True
            settings.disable_dns_automation = False
            settings.save()

            self.assert_action_not_taken(test, raises=False)

    @pytest.mark.usefixtures("dmc_can_act")
    def test_disabled_project_healing_automation_and_dmc_can_act(self, test):
        self.project.healing_automation.enabled = False
        self.project.dns_automation.enabled = True
        self.project.save()

        self.assert_action_not_taken(test)

    @pytest.mark.usefixtures("dmc_can_not_act")
    def test_disabled_project_healing_automation_and_dmc_can_not_act(self, test):
        self.project.healing_automation.enabled = False
        self.project.dns_automation.enabled = True
        self.project.save()

        self.assert_action_not_taken(test, raises=False)

    @pytest.mark.usefixtures("dmc_can_act")
    def test_disabled_project_dns_automation(self, test, mp):
        self.project.healing_automation.enabled = True
        self.project.dns_automation.enabled = False
        self.project.save()

        mock_dns_client = mp.function(walle.clients.dns.slayer_dns_api.DnsClient)

        mp.function(walle.network._get_fb_vlan_for_search_scheme, return_value=(None, 700))
        mp.function(walle.network._get_ipv6_list_with_eui64_and_ipv4_support, return_value=(["12::00"]))

        fix_dns(self.host)

        assert not mock_dns_client().apply_operations.called

        test.hosts.assert_equal()
        test.failure_log.assert_equal()
