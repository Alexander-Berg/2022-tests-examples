import logging
from copy import deepcopy  # noqa
from unittest import mock

import pytest
from billing.hot.faas.lib.protos.faas_pb2 import ProcessorResponse
from sendr_pytest import *  # noqa
from sendr_pytest import collision_watcher  # noqa

from sendr_qlog import LoggerContext

from billing.hot.faas.python.faas.api.app import FaasApplication
from billing.hot.faas.python.faas.core.function import FunctionRegistry
from billing.hot.faas.python.faas.utils.loop import get_loop

pytest_plugins = ["aiohttp.pytest_plugin"]


def pytest_configure(config):
    collision_watcher.pytest_configure(config)


def pytest_addoption(parser):
    collision_watcher.pytest_addoption(parser)


def pytest_generate_tests(metafunc):
    collision_watcher.pytest_generate_tests(metafunc)


def pytest_collection_modifyitems(session, config, items):
    collision_watcher.pytest_collection_modifyitems(session, config, items)


@pytest.yield_fixture()
def event_loop():
    loop = get_loop()
    yield loop
    loop.close()


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
async def app(aiohttp_client, function_registry):
    return await aiohttp_client(FaasApplication(function_registry=function_registry))


@pytest.fixture
def test_logger():
    return LoggerContext(logging.getLogger("test_logger"), {})


@pytest.fixture(params=[pytest.param(True, id="async"), pytest.param(False, id="sync")])
def iscoroutinefunction(request):
    return request.param


@pytest.fixture
def function_result():
    return ProcessorResponse()


@pytest.fixture
def function(function_result, iscoroutinefunction):
    def _inner_sync(a, b):
        return function_result

    async def _inner_async(a, b):
        return function_result

    return (
        mock.AsyncMock(wraps=_inner_async)
        if iscoroutinefunction
        else mock.Mock(wraps=_inner_sync)
    )


@pytest.fixture
def function_registry(function):
    return FunctionRegistry(function)


@pytest.fixture(autouse=True)
def faas_settings():
    from billing.hot.faas.python.faas.conf import settings

    data = deepcopy(settings._settings)

    yield settings

    settings._settings = data
