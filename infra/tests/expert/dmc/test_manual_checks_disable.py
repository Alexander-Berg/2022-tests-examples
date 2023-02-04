import pytest

from infra.walle.server.tests.expert.dmc.util import fail_check
from infra.walle.server.tests.lib.util import monkeypatch_locks, mock_status_reasons, mock_host_health_status
from walle.expert import decisionmakers
from walle.expert.types import WalleAction, CheckType
from walle.hosts import HostState, HostStatus


@pytest.mark.parametrize("disabled_check_type", CheckType.ALL)
@pytest.mark.usefixtures("monkeypatch_audit_log", "mock_automation_plot")
def test_manual_disabled_check(test, mp, disabled_check_type):
    monkeypatch_locks(mp)

    project_with_disabled_check = test.mock_project(
        {
            "id": "some-id",
            "healing_automation": {"enabled": True},
            "dns_automation": {"enabled": True},
            "manually_disabled_checks": [disabled_check_type],
        }
    )
    host = test.mock_host(
        {
            "project": project_with_disabled_check.id,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status(),
        }
    )

    decision_maker = decisionmakers.get_decision_maker(project_with_disabled_check)
    reasons = mock_status_reasons(enabled_checks=CheckType.ALL + [pytest.CUSTOM_CHECK_NAME])
    fail_check(reasons, disabled_check_type)
    decision = decision_maker.make_decision(host, reasons)
    assert decision.action == WalleAction.HEALTHY


@pytest.mark.usefixtures("monkeypatch_audit_log", "mock_automation_plot")
def test_manual_disable_other_check(test, mp):
    monkeypatch_locks(mp)

    project_with_disabled_check = test.mock_project(
        {
            "id": "some-id",
            "healing_automation": {"enabled": True},
            "dns_automation": {"enabled": True},
            "manually_disabled_checks": [CheckType.UNREACHABLE],
        }
    )
    host = test.mock_host(
        {
            "project": project_with_disabled_check.id,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status(),
        }
    )

    decision_maker = decisionmakers.get_decision_maker(project_with_disabled_check)
    reasons = mock_status_reasons(enabled_checks=CheckType.ALL + [pytest.CUSTOM_CHECK_NAME])
    fail_check(reasons, CheckType.SSH)
    decision = decision_maker.make_decision(host, reasons)
    assert decision.action != WalleAction.HEALTHY
