import logging
import os
import re
from contextlib import asynccontextmanager
from copy import deepcopy
from typing import Collection, Type
from unittest import mock

import aiohttp.pytest_plugin
import pytest
from aiohttp import hdrs
from aioresponses import CallbackResult
from aioresponses import aioresponses as base_aioresponses

from sendr_pytest import *  # noqa
from sendr_pytest.mocks import mock_action  # noqa
from sendr_qlog.logging.adapters.logger import LoggerContext

from billing.yandex_pay_admin.yandex_pay_admin.api.app import YandexPayAdminApplication
from billing.yandex_pay_admin.yandex_pay_admin.api.handlers.base import BaseHandler
from billing.yandex_pay_admin.yandex_pay_admin.conf import settings
from billing.yandex_pay_admin.yandex_pay_admin.core.cryptogram import setup_cryptogram_crypters
from billing.yandex_pay_admin.yandex_pay_admin.storage import Storage
from billing.yandex_pay_admin.yandex_pay_admin.tests.db import *  # noqa
from billing.yandex_pay_admin.yandex_pay_admin.tests.entities import *  # noqa
from billing.yandex_pay_admin.yandex_pay_admin.tests.mocks import AsyncContextManagerMock
from billing.yandex_pay_admin.yandex_pay_admin.utils.env import configure_env


class aioresponses(base_aioresponses):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._mocks = {}

    def add(self, url, method, *args, **kwargs):
        super().add(url, method, *args, **kwargs)
        matcher = list(self._matches.values())[-1]
        mock_ = self._mocks[matcher] = mock.Mock()
        return mock_

    def head(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_HEAD, **kwargs)

    def get(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_GET, **kwargs)

    def post(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_POST, **kwargs)

    def put(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_PUT, **kwargs)

    def patch(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_PATCH, **kwargs)

    def delete(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_DELETE, **kwargs)

    def options(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_OPTIONS, **kwargs)

    async def match(self, method, url, **kwargs):
        response = await super().match(method, url, **kwargs)
        if response is None:
            print('Failed to match:', method, url)  # noqa: T001
        else:
            for matcher, mock_ in self._mocks.items():
                if matcher.match(method, url):
                    mock_(**kwargs)
                    break
        return response


@pytest.fixture
def setup_interactions_tvm(yandex_pay_admin_settings, rands, aioresponses_mocker):
    # Since we allow passthrough for localhost, setting up custom host for tvm
    from billing.yandex_pay_admin.yandex_pay_admin.interactions.base import TVM_CONFIG

    TVM_CONFIG['host'] = 'tvm'
    TVM_CONFIG['port'] = 80
    yandex_pay_admin_settings.TVM_URL = 'http://localhost'

    def tvm_callback(url, **kwargs):
        dst = kwargs['params']['dsts']
        return CallbackResult(
            status=200,
            payload={
                yandex_pay_admin_settings.TVM_CLIENT: {
                    'tvm_id': dst,
                    'ticket': f'service-ticket-f{rands()}',
                },
            },
        )

    aioresponses_mocker.get(
        re.compile('^http://tvm:80/tvm/tickets.*$'),
        callback=tvm_callback,
        repeat=True,
    )
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_admin_settings.TVM_URL}/tvm/checksrv.*$'),
        payload={'src': next(iter(yandex_pay_admin_settings.TVM_ALLOWED_SRC))},
        repeat=True,
    )


@pytest.fixture(autouse=True)
def aioresponses_mocker():
    with aioresponses(passthrough=['http://127.0.0.1:']) as m:
        yield m


def pytest_configure(config):
    os.environ['TVMTOOL_LOCAL_AUTHTOKEN'] = 'xxxxxxxxxxx'


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
def application(db_engine, yandex_pay_admin_settings) -> YandexPayAdminApplication:
    return YandexPayAdminApplication(db_engine=db_engine)


@pytest.fixture
async def app(aiohttp_client, application) -> aiohttp.ClientSession:
    return await aiohttp_client(application)


@pytest.fixture
async def testing_app(aiohttp_client, db_engine, yandex_pay_admin_settings):
    configure_env('testing')
    yield await aiohttp_client(YandexPayAdminApplication(db_engine=db_engine))
    configure_env()


@pytest.fixture
async def dbconn(app, db_engine, create_crypters):
    # app dependency is required to ensure exit order
    async with db_engine.acquire() as conn:
        yield conn


@pytest.fixture
def storage(dbconn) -> Storage:
    return Storage(dbconn)


@pytest.fixture
def rollback_after(storage):
    @asynccontextmanager
    async def _inner():
        async with storage.conn.begin_nested() as tr:
            try:
                yield
            finally:
                await tr.rollback()

    return _inner


@pytest.fixture
def request_id():
    return 'test-request-id'


@pytest.fixture
def dummy_logger():
    return LoggerContext(logging.getLogger('dummy_logger'), {})


@pytest.fixture
def dummy_logs(caplog, dummy_logger):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    def _inner():
        return [r for r in caplog.records if r.name == dummy_logger.logger.name]

    return _inner


@pytest.fixture
def disable_tvm_checking():
    BaseHandler.CHECK_TVM = False
    yield
    BaseHandler.CHECK_TVM = True


@pytest.fixture(autouse=True)
def yandex_pay_admin_settings(request):
    original_settings = deepcopy(settings._settings)

    marker = request.node.get_closest_marker("enable_swagger")
    settings.API_SWAGGER_ENABLED = marker is not None

    yield settings

    settings._settings = original_settings


@pytest.fixture
def actx_mock():
    return AsyncContextManagerMock


@pytest.fixture
def api_handlers(get_subclasses) -> Collection[Type[BaseHandler]]:
    all_subclasses = get_subclasses(BaseHandler)
    return [cls for cls in all_subclasses]


@pytest.fixture
def create_crypters():
    setup_cryptogram_crypters()
