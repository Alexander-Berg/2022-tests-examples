"""Tests cancellation of created admin requests for the host."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
    any_task_status,
)
from walle.admin_requests import request as admin_requests
from walle.hosts import HostState
from walle.stages import Stages, Stage


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.CANCEL_ADMIN_REQUESTS))


def test_cancel(test, mp):
    cancel_all_by_host = mp.function(admin_requests.cancel_all_by_host)
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(stage=Stages.CANCEL_ADMIN_REQUESTS),
        }
    )

    handle_host(host)

    cancel_all_by_host.assert_called_once_with(host.inv, name=host.name)
    mock_complete_current_stage(host)

    test.hosts.assert_equal()
