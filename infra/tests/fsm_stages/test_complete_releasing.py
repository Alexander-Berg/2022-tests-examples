"""Tests completion of host releasing."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
)
from walle import constants as walle_constants, restrictions
from walle.hosts import HostState
from walle.operations_log.constants import Operation
from walle.stages import Stages, Stage


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.COMPLETE_RELEASING))


def test_release(test):
    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": Operation.SWITCH_PROJECT.host_status,
            "provisioner": walle_constants.PROVISIONER_LUI,
            "config": "config-mock",
            "deploy_tags": ["ubuntu", "12.04", "LTS"],
            "restrictions": [restrictions.AUTOMATION],
            "task": mock_task(stage=Stages.COMPLETE_RELEASING),
        }
    )

    handle_host(host)

    del host.provisioner
    del host.config
    del host.restrictions
    del host.deploy_tags
    del host.operation_state
    host.set_state(HostState.FREE, issuer=host.task.owner, audit_log_id=host.task.audit_log_id)

    mock_complete_current_stage(host, inc_revision=1)

    test.hosts.assert_equal()
