# coding: utf-8
from __future__ import print_function

import socket
import contextlib

import mock
import pytest
import tornado.gen

from infra.netmon.agent.idl import common_pb2
from infra.netmon.agent.idl import tasks_pb2

from agent import application
from agent import utils
from agent import exceptions
from agent.diagnostic import planner

FAMILY = common_pb2.INET6
PROTOCOL = common_pb2.UDP


class CustomError(exceptions.AgentError):
    pass


@contextlib.contextmanager
def patch_schedule_checks(failed=False):
    report = mock.MagicMock()
    report.configure_mock(
        type=common_pb2.REGULAR_PROBE,
        family=socket.AF_INET6,
        protocol=PROTOCOL,
        source_addr=(socket.AF_INET6, (b"::", 1)),
        target_addr=(socket.AF_INET6, (b"::", 1)),
        received=9,
        lost=1,
        failed=failed,
        error=None,
        average=0.1,
        histogram=None,
        truncated=False,
        offender=None,
        generated=100 * utils.US
    )
    with mock.patch.object(application.UdpService, "schedule_checks", return_value=tornado.gen.maybe_future([report])) as mocked:
        yield mocked


@pytest.yield_fixture()
def schedule_checks_mock():
    with patch_schedule_checks() as mocked:
        yield mocked


@pytest.yield_fixture()
def schedule_failed_checks_mock():
    with patch_schedule_checks(failed=True) as mocked:
        yield mocked


@pytest.yield_fixture()
def schedule_raising_checks_mock():
    with mock.patch.object(application.UdpService, "schedule_checks", side_effect=CustomError("wrong")) as mocked:
        yield mocked


def run_planner(agent_app):
    agent_app.register(application.UdpService())
    on_changed = mock.Mock()

    arguments = tasks_pb2.TDiagnosticArguments(
        Family=FAMILY,
        Protocol=PROTOCOL,
        Duration=5,
        ProbesInParallel=2
    )
    result = tasks_pb2.TDiagnosticResult()

    probe_planner = planner.ProbePlanner(agent_app, arguments, result, on_changed=on_changed)
    agent_app.run_sync(probe_planner.run)

    return probe_planner, result, on_changed


def test_planner(agent_app, schedule_checks_mock):
    probe_planner, result, on_changed = run_planner(agent_app)
    assert probe_planner.success
    assert schedule_checks_mock.called
    assert on_changed.called
    assert result.Reports
    assert result.AggregatedReports
    assert result.Traceroutes


def test_failed_planner(agent_app, schedule_failed_checks_mock):
    probe_planner, result, _ = run_planner(agent_app)
    assert not probe_planner.success
    assert schedule_failed_checks_mock.called
    assert result.Reports
    assert not result.Traceroutes


def test_raising_planner(agent_app, schedule_raising_checks_mock):
    with pytest.raises(CustomError):
        run_planner(agent_app)
