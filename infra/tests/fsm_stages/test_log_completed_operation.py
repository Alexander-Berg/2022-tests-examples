"""Tests logging of host operation completion."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    ObjectMocker,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
    any_task_status,
)
from walle.hosts import HostState, TaskType
from walle.models import timestamp
from walle.operations_log.constants import Operation
from walle.operations_log.operations import OperationLog
from walle.stages import Stages, Stage
from walle.util.misc import drop_none


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.LOG_COMPLETED_OPERATION))


@pytest.mark.parametrize("params", (None, {"key": "value"}))
def test_log(test, params):
    operation = Operation.CHANGE_DISK

    log = ObjectMocker(OperationLog)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(
                stage=Stages.LOG_COMPLETED_OPERATION,
                stage_params=drop_none({"operation": operation.type, "params": params}),
            ),
        }
    )

    handle_host(host)
    _timestamp = timestamp()

    log.mock(
        dict(
            id="{}.{}.{}".format(host.task.audit_log_id, operation.type, _timestamp),
            audit_log_id=host.task.audit_log_id,
            host_inv=host.inv,
            host_name=host.name,
            host_uuid=host.uuid,
            project=host.project,
            type=operation.type,
            params=params,
            time=_timestamp,
            task_type=TaskType.AUTOMATED_HEALING,
        ),
        save=False,
    )
    mock_complete_current_stage(host)

    log.assert_equal()
    test.hosts.assert_equal()
