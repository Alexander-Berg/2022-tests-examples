"""Tests host health status reset."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
    drop_none,
    any_task_status,
)
from walle.hosts import HostState
from walle.models import timestamp
from walle.stages import Stages, Stage


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.RESET_HEALTH_STATUS))


def test_reset_free(test):
    host = test.mock_host(
        {"state": HostState.FREE, "status": any_task_status(), "task": mock_task(stage=Stages.RESET_HEALTH_STATUS)}
    )

    handle_host(host)
    mock_complete_current_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize("health_status_accuracy", (None, 666))
def test_reset_assigned(test, health_status_accuracy):
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(
                stage=Stages.RESET_HEALTH_STATUS,
                stage_params=drop_none({"health_status_accuracy": health_status_accuracy}) or None,
            ),
        }
    )

    handle_host(host)

    host.checks_min_time = timestamp()
    mock_complete_current_stage(host, inc_revision=1)

    test.hosts.assert_equal()
