"""Tests project switching for a host."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
)
from walle.hosts import HostState
from walle.restrictions import AUTOMATED_REDEPLOY, AUTOMATED_HEALING, AUTOMATED_DNS, strip_restrictions
from walle.stages import Stages, Stage


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.SWITCH_PROJECT))


def test_switch_project(test, monkeypatch_locks):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host(
        {"state": HostState.FREE, "task": mock_task(stage=Stages.SWITCH_PROJECT, stage_params={"project": project.id})}
    )

    handle_host(host)
    host.project = project.id
    mock_complete_current_stage(host, inc_revision=1)

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_restrictions", [[AUTOMATED_DNS, AUTOMATED_HEALING, AUTOMATED_REDEPLOY], [], None])
def test_switch_project_with_restrictions(test, monkeypatch_locks, host_restrictions):
    project = test.mock_project({"id": "some-id"})
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.SWITCH_PROJECT,
                stage_params={"project": project.id, "host_restrictions": host_restrictions},
            ),
        }
    )

    handle_host(host)
    host.project = project.id
    if host_restrictions is not None:
        if host_restrictions:
            host.restrictions = strip_restrictions(host_restrictions)
        else:
            del host.restrictions
    mock_complete_current_stage(host, inc_revision=1)

    test.hosts.assert_equal()
