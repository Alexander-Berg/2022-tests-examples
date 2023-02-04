"""Tests project automation classes."""

import pytest

import walle.expert.automation
from infra.walle.server.tests.lib.util import TestCase, add_dns_domain_and_vlan_scheme_to_project
from walle import authorization
from walle.constants import VLAN_SCHEMES, VLAN_SCHEME_MTN
from walle.errors import DNSAutomationValidationError
from walle.models import timestamp


@pytest.fixture
def test(request, monkeypatch_timestamp):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.fixture(
    params=[walle.expert.automation.PROJECT_HEALING_AUTOMATION, walle.expert.automation.PROJECT_DNS_AUTOMATION]
)
def automation(request):
    return request.param


def test_automation_disable(test, automation):
    project = test.mock_project(
        {
            "id": "project-mock",
            "healing_automation": {"enabled": True, 'failure_log_start_time': timestamp()},
            "dns_automation": {"enabled": True, 'failure_log_start_time': timestamp()},
        }
    )

    automation.disable_automation(authorization.ISSUER_WALLE, project.id, reason="reason mock")
    setattr(
        project,
        automation._automation_field,
        {"enabled": False, "status_message": "reason mock", "failure_log_start_time": timestamp()},
    )

    test.projects.assert_equal()


@pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
def test_automation_enable_without_credit(test, automation, vlan_scheme):
    project = test.mock_project(
        add_dns_domain_and_vlan_scheme_to_project(
            {
                "id": "project-mock",
                "healing_automation": {"enabled": False, 'failure_log_start_time': timestamp()},
                "dns_automation": {"enabled": False, 'failure_log_start_time': timestamp()},
            },
            vlan_scheme=vlan_scheme,
        )
    )

    automation.enable_automation(authorization.ISSUER_WALLE, project.id, reason="reason mock")
    setattr(
        project,
        automation._automation_field,
        {"enabled": True, "status_message": "reason mock", "failure_log_start_time": timestamp()},
    )

    test.projects.assert_equal()


@pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
def test_automation_enable_credit(test, automation, vlan_scheme):
    project = test.mock_project(
        add_dns_domain_and_vlan_scheme_to_project(
            {
                "id": "project-mock",
                "healing_automation": {"enabled": False, "failure_log_start_time": 0},
                "dns_automation": {"enabled": False, "failure_log_start_time": 0},
            },
            vlan_scheme=vlan_scheme,
        )
    )

    automation.enable_automation(
        authorization.ISSUER_WALLE,
        project.id,
        reason="reason mock",
        credit_time=1200,
        credit={"max_rebooted_hosts": 100, "max_profiled_hosts": 20, "max_dns_fixes": 10},
    )
    setattr(
        project,
        automation._automation_field,
        {
            "enabled": True,
            "status_message": "reason mock",
            "failure_log_start_time": timestamp(),
            "credit_end_time": timestamp() + 1200,
            "credit": {"max_rebooted_hosts": 100, "max_profiled_hosts": 20, "max_dns_fixes": 10},
        },
    )

    test.projects.assert_equal()


def test_increase_credit_no_previous_credit(test, automation):
    project = test.mock_project(
        {
            "id": "project-mock",
            "healing_automation": {"enabled": True, 'failure_log_start_time': timestamp()},
            "dns_automation": {"enabled": True, 'failure_log_start_time': timestamp()},
        }
    )

    updated = automation.increase_credit(
        project.id, reason="reason mock", credit_time=1200, credit_name="max_rebooted_hosts", by_amount=11
    )

    setattr(
        project,
        automation._automation_field,
        {
            "enabled": True,
            'failure_log_start_time': timestamp(),
            "credit_end_time": timestamp() + 1200,
            "credit": {"max_rebooted_hosts": 11},
        },
    )

    assert updated
    test.projects.assert_equal()


@pytest.mark.parametrize("time_diff", [1000, -1000, -2000])
def test_increase_credit_with_previous_credit(test, automation, time_diff, monkeypatch_timestamp):
    current_credit_time = timestamp() + time_diff
    project = test.mock_project(
        {
            "id": "project-mock",
            "healing_automation": {
                "enabled": True,
                'failure_log_start_time': timestamp(),
                "credit_end_time": current_credit_time,
                "credit": {"max_rebooted_hosts": 100, "max_profiled_hosts": 20, "max_dns_fixes": 10},
            },
            "dns_automation": {
                "enabled": True,
                'failure_log_start_time': timestamp(),
                "credit_end_time": current_credit_time,
                "credit": {"max_rebooted_hosts": 100, "max_profiled_hosts": 20, "max_dns_fixes": 10},
            },
        }
    )

    updated = automation.increase_credit(
        project.id, reason="reason mock", credit_time=1200, credit_name="max_rebooted_hosts", by_amount=11
    )

    setattr(
        project,
        automation._automation_field,
        {
            "enabled": True,
            'failure_log_start_time': timestamp(),
            "credit_end_time": max(current_credit_time, timestamp() + 1200),
            "credit": {"max_rebooted_hosts": 111, "max_profiled_hosts": 20, "max_dns_fixes": 10},
        },
    )

    assert updated
    test.projects.assert_equal()


def test_increase_credit_automation_disabled(test, automation):
    project = test.mock_project(
        {
            "id": "project-mock",
            "healing_automation": {
                "enabled": False,
                'failure_log_start_time': timestamp(),
                "credit_end_time": timestamp(),
            },
            "dns_automation": {"enabled": False, 'failure_log_start_time': timestamp(), "credit_end_time": timestamp()},
        }
    )

    updated = automation.increase_credit(
        project.id, reason="reason mock", credit_time=1200, credit_name="max_rebooted_hosts", by_amount=11
    )

    setattr(
        project,
        automation._automation_field,
        {"enabled": False, 'failure_log_start_time': timestamp(), "credit_end_time": timestamp()},
    )

    assert updated is False
    test.projects.assert_equal()


class TestDnsAutomationValidateSettings:
    @pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
    def test_right_settings(self, test, vlan_scheme):
        project = test.mock_project(
            {"id": "project-mock", "dns_domain": "search.yandex.net", "vlan_scheme": vlan_scheme}
        )
        walle.expert.automation.PROJECT_DNS_AUTOMATION.validate_project_dns_settings(project)

    @pytest.mark.parametrize(
        "dns_domain, vlan_scheme", [(None, VLAN_SCHEME_MTN), ("search.yandex.net", None), (None, None)]
    )
    def test_fails_without_dns_domain_and_or_vlan_scheme(self, test, dns_domain, vlan_scheme):
        project = test.mock_project({"id": "project-mock", "dns_domain": dns_domain, "vlan_scheme": vlan_scheme})

        with pytest.raises(DNSAutomationValidationError):
            walle.expert.automation.PROJECT_DNS_AUTOMATION.validate_project_dns_settings(project)
