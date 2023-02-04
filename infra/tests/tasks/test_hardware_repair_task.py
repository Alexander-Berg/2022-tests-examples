import pytest

from infra.walle.server.tests.lib.util import (
    mock_decision,
    TestCase,
    mock_status_reasons,
    mock_host_health_status,
    mock_schedule_hardware_repair,
    CUSTOM_CHECK_TYPE,
)
from walle.expert import dmc
from walle.expert.decisionmakers import get_decision_maker
from walle.expert.types import WalleAction
from walle.hosts import HostState, HostStatus, HealthStatus
from walle.operations_log.constants import Operation


@pytest.fixture()
def test(monkeypatch_timestamp, request, mp_juggler_source):
    return TestCase.create(request)


@pytest.mark.usefixtures("monkeypatch_audit_log", "monkeypatch_locks")
def test_hardware_repair_task_save_slot_in_params(test):
    params = {"reason": "test", "slot": 1, "eine_code": "test_code", "operation": Operation.REPAIR_HARDWARE.type}
    reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status(reasons=reasons),
        }
    )
    decision_maker = get_decision_maker(test.default_project)
    decision = mock_decision(WalleAction.REPAIR_HARDWARE, params=params, checks=[CUSTOM_CHECK_TYPE])

    dmc.handle_decision(host.copy(), decision, reasons, decision_maker)
    mock_schedule_hardware_repair(host, decision)

    test.hosts.assert_equal()
