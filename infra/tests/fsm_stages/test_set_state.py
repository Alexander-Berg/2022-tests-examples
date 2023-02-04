"""Tests maintenance setting stage."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
    AUDIT_LOG_ID,
)
from walle.hosts import HostState, HostOperationState, HostStatus, StateExpire
from walle.stages import Stages, Stage
from walle.util.misc import drop_none


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.SET_MAINTENANCE))


@pytest.mark.usefixtures("monkeypatch_audit_log")
class TestSetMaintenance:
    _TICKET_KEY_TASK = "ticket-key-task"
    _TICKET_KEY_HOST = "ticket-key-host"

    @staticmethod
    def _mock_host_with_set_maintenance_stage(
        test, ticket_key=_TICKET_KEY_TASK, operation_state=HostOperationState.OPERATION, **task_kwargs
    ):
        return test.mock_host(
            {
                "state": HostState.ASSIGNED,
                "status": HostStatus.READY,
                "ticket": TestSetMaintenance._TICKET_KEY_HOST,
                "task": mock_task(
                    stage=Stages.SET_MAINTENANCE,
                    stage_params=drop_none(
                        {
                            "ticket_key": ticket_key,
                            "timeout_time": 10,
                            "timeout_status": HostStatus.READY,
                            "operation_state": operation_state,
                        }
                    ),
                    owner=test.api_issuer,
                    **task_kwargs
                ),
            }
        )

    @staticmethod
    def _mock_set_maintenance(host, issuer, ticket_key=_TICKET_KEY_TASK):
        state_expire = StateExpire(ticket=ticket_key, time=10, status=HostStatus.READY, issuer=issuer)
        host.set_state(HostState.MAINTENANCE, issuer, AUDIT_LOG_ID, expire=state_expire)

    def test_set_maintenance_without_cms_task(self, test):
        host = self._mock_host_with_set_maintenance_stage(test)

        handle_host(host)

        mock_complete_current_stage(host, inc_revision=1)
        self._mock_set_maintenance(host, test.api_issuer)

        test.hosts.assert_equal()

    def test_set_maintenance_with_cms_task(self, test):
        host = self._mock_host_with_set_maintenance_stage(test, cms_task_id="mock-cms-task-id")

        handle_host(host)

        mock_complete_current_stage(host, inc_revision=1)
        self._mock_set_maintenance(host, test.api_issuer)
        host.cms_task_id = "mock-cms-task-id"

        test.hosts.assert_equal()

    def test_set_maintenance_with_decommission(self, test):
        host = self._mock_host_with_set_maintenance_stage(test, operation_state=HostOperationState.DECOMMISSIONED)

        handle_host(host)

        mock_complete_current_stage(host, inc_revision=1)
        self._mock_set_maintenance(host, test.api_issuer)
        host.operation_state = HostOperationState.DECOMMISSIONED

        test.hosts.assert_equal()

    def test_set_maintenance_without_ticket(self, test):
        # backwards compatibility
        host = self._mock_host_with_set_maintenance_stage(test, ticket_key=None)

        handle_host(host)

        mock_complete_current_stage(host, inc_revision=1)
        self._mock_set_maintenance(host, test.api_issuer, ticket_key=self._TICKET_KEY_HOST)

        test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_set_assigned_task(test):
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": HostStatus.MANUAL,
            "cms_task_id": "mock-cms-task-id",
            "task": mock_task(stage=Stages.SET_ASSIGNED, owner=test.api_issuer, cms_task_id="mock-cms-task-id"),
        }
    )

    handle_host(host)

    mock_complete_current_stage(host, inc_revision=1)
    host.set_state(HostState.ASSIGNED, test.api_issuer, AUDIT_LOG_ID)
    del host.cms_task_id

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_set_assigned_from_decommissioned_task(test):
    host = test.mock_host(
        {
            "state": HostState.MAINTENANCE,
            "status": HostStatus.MANUAL,
            "operation_state": HostOperationState.DECOMMISSIONED,
            "cms_task_id": "mock-cms-task-id",
            "task": mock_task(stage=Stages.SET_ASSIGNED, owner=test.api_issuer, cms_task_id="mock-cms-task-id"),
        }
    )

    handle_host(host)

    mock_complete_current_stage(host, inc_revision=1)
    host.set_state(HostState.ASSIGNED, test.api_issuer, AUDIT_LOG_ID)
    del host.cms_task_id
    host.operation_state = HostOperationState.OPERATION

    test.hosts.assert_equal()
