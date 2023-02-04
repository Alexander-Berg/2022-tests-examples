import logging
import os
import random
import string
import sys
from copy import deepcopy
from typing import Collection, Type

import aiohttp.pytest_plugin
import pytest

from sendr_pytest import *  # noqa
from sendr_tvm.qloud_async_tvm import ServiceTicket, UserTicket
from sendr_utils.abc import isabstract

from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.base import BaseHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.internal.base import BaseInternalHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.internal_app import YandexPayPlusInternalApplication
from billing.yandex_pay_plus.yandex_pay_plus.api.public_app import YandexPayPlusPublicApplication
from billing.yandex_pay_plus.yandex_pay_plus.conf import settings
from billing.yandex_pay_plus.yandex_pay_plus.core.cryptogram import setup_cryptogram_crypters
from billing.yandex_pay_plus.yandex_pay_plus.storage import Storage
from billing.yandex_pay_plus.yandex_pay_plus.tests.entities import *  # noqa
from billing.yandex_pay_plus.yandex_pay_plus.tests.interactions import *  # noqa
from billing.yandex_pay_plus.yandex_pay_plus.tests.stored_entities import *  # noqa
from billing.yandex_pay_plus.yandex_pay_plus.utils.logging import YandexPayLoggerContext


def pytest_configure(config):
    os.environ['TVMTOOL_LOCAL_AUTHTOKEN'] = 'xxxxxxxxxxx'


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
def application(db_engine) -> YandexPayPlusInternalApplication:
    return YandexPayPlusInternalApplication(db_engine=db_engine)


@pytest.fixture
def create_crypters():
    setup_cryptogram_crypters()


@pytest.fixture
async def app(aiohttp_client, application):
    return await aiohttp_client(application)


@pytest.fixture
async def public_app(aiohttp_client, db_engine):
    return await aiohttp_client(YandexPayPlusPublicApplication(db_engine=db_engine))


@pytest.fixture
async def internal_app(aiohttp_client, db_engine):
    return await aiohttp_client(YandexPayPlusInternalApplication(db_engine=db_engine))


@pytest.fixture
async def dbconn(app, db_engine, create_crypters):
    # app dependency is required to ensure exit order
    async with db_engine.acquire() as conn:
        yield conn


@pytest.fixture
def storage(dbconn) -> Storage:
    return Storage(dbconn)


@pytest.fixture
def dummy_logger():
    logger = logging.getLogger('dummy_logger')
    if (level := os.environ.get('PAY_PLUS_TESTS_LOG_LEVEL', '')) != '':
        logger.setLevel(level)
        logger.addHandler(logging.StreamHandler(sys.stdout))
    return YandexPayLoggerContext(logger, {})


@pytest.fixture
def dummy_logs(caplog, dummy_logger):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    def _inner():
        return [r for r in caplog.records if r.name == dummy_logger.logger.name]

    return _inner


@pytest.fixture(autouse=True)
def settings_to_overwrite(request):
    settings_ = deepcopy(getattr(request, 'param', {}))

    # Disabling Swagger by default to speed up unrelated tests.
    # Swagger and relevant CSRF tests must explicitly enable the setting.
    settings_.setdefault('API_SWAGGER_ENABLED', False)
    return settings_


@pytest.fixture(autouse=True)
def yandex_pay_plus_settings(settings_to_overwrite):
    original_settings = deepcopy(settings._settings)
    settings.update(settings_to_overwrite)

    yield settings

    settings._settings = original_settings


@pytest.fixture
def tvm_service_id(yandex_pay_plus_settings):
    return yandex_pay_plus_settings.TVM_ALLOWED_SRC[0]


@pytest.fixture
def tvm_user_id(randn):
    return randn()


@pytest.fixture(autouse=True)
def mock_tvm(mocker, tvm_service_id, tvm_user_id):
    service_ticket = mocker.AsyncMock(
        return_value=ServiceTicket(tvm_service_id) if tvm_service_id is not None else None
    )
    user_ticket = mocker.AsyncMock(return_value=UserTicket(tvm_user_id) if tvm_user_id is not None else None)

    mocker.patch.object(BaseInternalHandler.TICKET_CHECKER, 'check_tvm_service_ticket', service_ticket)
    mocker.patch.object(BaseInternalHandler.TICKET_CHECKER, 'get_tvm_user_ticket', user_ticket)


@pytest.fixture
def rands_url_safe():
    def _inner(k: int = 16) -> str:
        return ''.join(random.choices(string.ascii_letters + string.digits, k=k))

    return _inner


@pytest.fixture
def api_handlers(get_subclasses) -> Collection[Type[BaseHandler]]:
    all_subclasses = get_subclasses(BaseHandler)
    return [cls for cls in all_subclasses if not isabstract(cls)]


@pytest.fixture
def worker_app(mocker, db_engine):
    return mocker.Mock(db_engine=db_engine)
