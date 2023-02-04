import logging
import os

import aiohttp.pytest_plugin
import aiohttp.test_utils
import pytest

from sendr_pytest import *  # noqa
from sendr_qlog.logging.adapters.logger import LoggerContext

from pay.lib.tests.interactions import *  # noqa

pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


def pytest_configure(config):
    os.environ['TVMTOOL_LOCAL_AUTHTOKEN'] = os.environ.get('TVMTOOL_LOCAL_AUTHTOKEN', 'xxxxxxxxxxx')


@pytest.fixture
def loop(event_loop):
    return event_loop


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
def mocked_logger(mocker):
    return mocker.MagicMock()


@pytest.fixture
def request_id():
    return 'unittest-request-id'


@pytest.fixture
def create_interaction_client(dummy_logger, request_id):
    TVM_NOT_SET = object()

    def _inner(client_cls, tvm_id=TVM_NOT_SET):
        params = {
            'logger': dummy_logger,
            'request_id': request_id,
        }
        if tvm_id is not TVM_NOT_SET:
            params['tvm_id'] = tvm_id
        return client_cls(**params)

    return _inner
