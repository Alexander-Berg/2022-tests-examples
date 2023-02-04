"""Tests bmc reset."""

from unittest.mock import call

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    handle_host,
    mock_task,
    monkeypatch_clients_for_host,
    check_stage_initialization,
    mock_complete_current_stage,
)
from walle.stages import Stage, Stages


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


# Stage initialization


def test_initiate_reset_bmc_stage(test):
    check_stage_initialization(test, Stage(name=Stages.RESET_BMC))


def test_power_on(test, monkeypatch):
    host = test.mock_host({"task": mock_task(stage=Stage(name=Stages.RESET_BMC))})
    clients = monkeypatch_clients_for_host(monkeypatch, host)

    handle_host(host)

    assert clients.mock_calls == [call.hardware.bmc_reset(clients.hardware.BMC_RESET_COLD)]

    mock_complete_current_stage(host)
    test.hosts.assert_equal()
