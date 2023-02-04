import logging
import os
import random
import string
from copy import deepcopy

import aiohttp.pytest_plugin
import pytest

from sendr_pytest import *  # noqa
from sendr_tvm.common import TicketCheckResult
from sendr_tvm.qloud_async_tvm import QTickerChecker

from pay.bill_payments.bill_payments.api.app import BillPaymentsApplication
from pay.bill_payments.bill_payments.conf import settings
from pay.bill_payments.bill_payments.storage import Storage
from pay.bill_payments.bill_payments.tests.interactions import *  # noqa
from pay.bill_payments.bill_payments.utils.logging import BillPaymentsLoggerContext


def pytest_configure(config):
    os.environ['TVMTOOL_LOCAL_AUTHTOKEN'] = 'xxxxxxxxxxx'


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
def application(db_engine, bill_payments_settings) -> BillPaymentsApplication:
    return BillPaymentsApplication(db_engine=db_engine)


@pytest.fixture
async def app(aiohttp_client, application):
    return await aiohttp_client(application)


@pytest.fixture
async def dbconn(app, db_engine):
    # app dependency is required to ensure exit order
    async with db_engine.acquire() as conn:
        yield conn


@pytest.fixture
def storage(dbconn) -> Storage:
    return Storage(dbconn)


@pytest.fixture
def dummy_logger():
    return BillPaymentsLoggerContext(logging.getLogger('dummy_logger'), {})


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
def bill_payments_settings(settings_to_overwrite):
    original_settings = deepcopy(settings._settings)
    settings.update(settings_to_overwrite)

    yield settings

    settings._settings = original_settings


@pytest.fixture
def tvm_service_id(bill_payments_settings):
    return bill_payments_settings.TVM_ALLOWED_CLIENTS[0]


@pytest.fixture
def tvm_user_id(randn):
    return randn()


@pytest.fixture(autouse=True)
def mock_tvm(mocker, tvm_service_id, tvm_user_id):
    return mocker.patch.object(
        QTickerChecker,
        'check_headers',
        mocker.AsyncMock(
            return_value=TicketCheckResult(
                {'src': tvm_service_id},
                {'default_uid': tvm_user_id},
            ),
        ),
    )


@pytest.fixture
def rands_url_safe():
    def _inner(k: int = 16) -> str:
        return ''.join(random.choices(string.ascii_letters + string.digits, k=k))

    return _inner
