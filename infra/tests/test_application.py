# coding: utf-8
from __future__ import print_function

import tornado.gen
import tornado.httpclient

import mock
import pytest

from agent import application
from agent import rpc
from agent import tasks


@pytest.yield_fixture()
def rpc_mock(agent_app):
    attrs = {
        'enqueued_tasks.return_value':  tornado.gen.maybe_future(None),
        'finish_tasks.return_value':    tornado.gen.maybe_future(None),
        'terminated_host.return_value': tornado.gen.maybe_future(None)
    }
    mocked = mock.MagicMock(**attrs)
    with mock.patch.dict(agent_app._services, {rpc.RpcClient: mocked}):
        yield mocked


@mock.patch.object(tasks.TaskDispatcher, '_loop_interval', 0.001)
def test_app_shutdown_race(rpc_mock):
    for iteration in xrange(1000):
        rpc_mock.reset_mock()

        # Tickers don't restart when cancelled, so we need to re-create
        # the application on each iteration
        app = application.Application()
        app.register(application.BackendMaintainerService())

        app.register(application.IfaceService())
        app.register(tasks.TaskDispatcher())

        with mock.patch.dict(app._services, {rpc.RpcClient: rpc_mock}):
            app._ioloop.call_later(0.02, app.shutdown)
            app.start_loop()
            calls = [name for name, args, kwargs in rpc_mock.method_calls]
            last_call_index = {
                method_name: max(idx for idx, name in enumerate(calls) if name == method_name)
                for method_name in ('enqueued_tasks', 'terminated_host')
            }
            assert last_call_index['terminated_host'] > last_call_index['enqueued_tasks']
