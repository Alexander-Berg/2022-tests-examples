"""Tests host deactivation."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    handle_host,
    mock_task,
    check_stage_initialization,
    mock_host_deactivation,
)
from walle.stages import Stage, Stages


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.DEACTIVATE))


def test_handling(test):
    host = test.mock_host({"task": mock_task(stage=Stages.DEACTIVATE)})
    handle_host(host)
    mock_host_deactivation(host, reason="Host deactivation has been requested by [unknown]: [no reason provided].")
    test.hosts.assert_equal()


def test_deactivate_with_reason(test):
    host = test.mock_host({"task": mock_task(stage=Stages.DEACTIVATE, stage_params={"reason": "reason-mock"})})
    handle_host(host)
    mock_host_deactivation(host, reason="Host deactivation has been requested by [unknown]: reason-mock.")
    test.hosts.assert_equal()
