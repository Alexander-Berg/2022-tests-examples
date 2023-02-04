# coding: utf-8
from __future__ import print_function

import tornado.gen
import pytest
import mock

from infra.netmon.agent.idl import common_pb2
from infra.netmon.agent.idl import tasks_pb2

from agent import tasks
from agent import rpc
from agent import encoding
from agent import const
from agent import application
from agent.settings import Settings


@pytest.fixture()
def current_hostname():
    return encoding.safe_str(Settings.current().hostname)


@pytest.yield_fixture()
def execute_diagnostic_task_mock(monkeypatch):
    callback = mock.Mock()

    @tornado.gen.coroutine
    def inner_task(task, arguments, result):
        result.Arguments.CopyFrom(tasks_pb2.TDiagnosticArguments(
            Family=common_pb2.INET6,
            Protocol=common_pb2.ICMP
        ))
        yield tornado.gen.moment

    callback.side_effect = inner_task
    monkeypatch.setattr(tasks, "diagnostic_task", callback)
    yield callback


@pytest.yield_fixture()
def enqueued_tasks_mock(current_hostname):
    value = tornado.gen.maybe_future([
        tasks_pb2.TEnqueuedTask(
            Key=b"some-key",
            Host=current_hostname,
            Generated=123,
            Deadline=234
        ),
        tasks_pb2.TEnqueuedTask(
            Key=b"another-key",
            Host=current_hostname,
            Generated=567,
            Deadline=678,
            Diagnostic=tasks_pb2.TDiagnosticArguments(
                Family=common_pb2.INET6,
                Protocol=common_pb2.ICMP
            )
        )
    ])
    with mock.patch.object(rpc.RpcClient, "enqueued_tasks", return_value=value) as mocked:
        yield mocked


@pytest.yield_fixture()
def finish_tasks_mock():
    value = tornado.gen.maybe_future([True])
    with mock.patch.object(rpc.RpcClient, "finish_tasks", return_value=value) as mocked:
        yield mocked


@pytest.mark.skip(reason="flaky test")
def test_tasks(agent_app, rpc_client, current_hostname, enqueued_tasks_mock, finish_tasks_mock, execute_diagnostic_task_mock):
    agent_app.register(application.IfaceService())
    agent_app.register(tasks.TaskExecutor(max_parallel_tasks=2))
    agent_app.register(tasks.TaskDispatcher())

    @tornado.gen.coroutine
    def do_work():
        yield agent_app[application.IfaceService]._loop.wait()
        yield agent_app[tasks.TaskDispatcher]._loop.wait()
        yield agent_app[tasks.TaskExecutor]._loop.wait()
        yield agent_app[tasks.TaskDispatcher]._loop.wait()

    agent_app.run_sync(do_work)

    enqueued_tasks_mock.assert_called_with(current_hostname)
    assert enqueued_tasks_mock.call_count >= 2

    assert execute_diagnostic_task_mock.call_count >= 1

    assert finish_tasks_mock.call_count >= 2
    (first_call, second_call) = finish_tasks_mock.call_args_list
    [first] = first_call[0][0]
    [second, third] = second_call[0][0]
    assert not first.ParentKey and first.Version.Version == const.FULL_VERSION and first.Finished
    assert second.ParentKey == b"some-key" and second.Error and second.Finished
    assert third.ParentKey == b"another-key" and not third.Error and third.Finished


def test_too_many_tasks(agent_app, rpc_client, current_hostname, enqueued_tasks_mock, finish_tasks_mock, execute_diagnostic_task_mock):
    agent_app.register(application.IfaceService())
    agent_app.register(tasks.TaskExecutor(max_parallel_tasks=1))
    agent_app.register(tasks.TaskDispatcher())

    @tornado.gen.coroutine
    def do_work():
        yield agent_app[application.IfaceService]._loop.wait()
        yield agent_app[tasks.TaskDispatcher]._loop.wait()
        yield agent_app[tasks.TaskExecutor]._loop.wait()
        yield agent_app[tasks.TaskExecutor]._loop.wait()
        yield agent_app[tasks.TaskDispatcher]._loop.wait()

    agent_app.run_sync(do_work)

    # version and our tasks
    assert finish_tasks_mock.call_count >= 3
