# coding: utf-8
from __future__ import print_function

import os

import mock
import tornado.gen
import pathlib2

import pytest

import agent
import agent.topology
import agent.settings
import agent.application
import agent.rpc


@pytest.fixture(autouse=True)
def monkeypatch_download_topology(monkeypatch):
    agent.settings.current().freeze_topology = True

    @tornado.gen.coroutine
    def callback(*args, **kwargs):
        raise RuntimeError("test shouldn't use external data")

    monkeypatch.setattr(agent.topology, "_download_topology", callback)


@pytest.yield_fixture(autouse=True)
def resolve_fqdn_by_address_mock():
    with mock.patch.object(agent.application.IfaceService,
                           "_resolve_fqdn_by_address",
                           return_value=tornado.gen.maybe_future(None)) as mocked:
        yield mocked


@pytest.fixture(scope="session", autouse=True)
def current_var_dir():
    agent.settings.current().var_dir = pathlib2.Path(os.getcwd())


@pytest.fixture(scope="session", autouse=True)
def disable_unistat_pusher():
    agent.settings.current().unistat_pusher = False


@pytest.fixture()
def agent_app():
    return agent.application.Application()


@pytest.fixture()
def rpc_client(agent_app):
    agent_app.register(agent.rpc.RpcClient())
    return agent_app[agent.rpc.RpcClient]
