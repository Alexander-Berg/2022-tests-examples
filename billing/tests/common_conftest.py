import asyncio
import dataclasses
import logging
import os
from copy import deepcopy
from typing import Any, Collection, Dict, Optional, Type

import aiohttp.pytest_plugin
import aiohttp.test_utils
import pytest

from sendr_pytest import *  # noqa
from sendr_utils.abc import isabstract

from billing.yandex_pay.yandex_pay.api.app import YandexPayApplication
from billing.yandex_pay.yandex_pay.api.handlers.base import BaseHandler
from billing.yandex_pay.yandex_pay.api.internal_app import YandexPayInternalApplication
from billing.yandex_pay.yandex_pay.conf import settings
from billing.yandex_pay.yandex_pay.core.actions.base import BaseAction
from billing.yandex_pay.yandex_pay.file_storage import FileStorage
from billing.yandex_pay.yandex_pay.storage import Storage
from billing.yandex_pay.yandex_pay.tests.entities import *  # noqa
from billing.yandex_pay.yandex_pay.tests.interactions import *  # noqa
from billing.yandex_pay.yandex_pay.utils.env import configure_env

pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


def pytest_configure(config):
    os.environ['TVMTOOL_LOCAL_AUTHTOKEN'] = os.environ.get('TVMTOOL_LOCAL_AUTHTOKEN', 'xxxxxxxxxxx')


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
async def app(loop, aiohttp_client, db_engine, yandex_pay_settings):
    return await aiohttp_client(YandexPayApplication(db_engine=db_engine))


@pytest.fixture
def yandexpay_app(app: aiohttp.test_utils.TestClient):
    return app.app


@pytest.fixture
async def internal_app(aiohttp_client, db_engine, yandex_pay_settings):
    return await aiohttp_client(YandexPayInternalApplication(db_engine=db_engine))


@pytest.fixture
async def sandbox_app(aiohttp_client, db_engine, yandex_pay_settings):
    # Dependency on 'yandex_pay_settings' fixture is needed to guarantee
    # that the 'sandbox_app' fixture is only called after it
    configure_env('sandbox')
    yield await aiohttp_client(YandexPayApplication(db_engine=db_engine))
    configure_env()


@pytest.fixture
def worker_app(mocker, db_engine):
    return mocker.Mock(db_engine=db_engine)


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
    import logging

    from billing.yandex_pay.yandex_pay.utils.logging import YandexPayLoggerContext
    return YandexPayLoggerContext(logging.getLogger('dummy_logger'), {})


@pytest.fixture
def dummy_logs(caplog, dummy_logger):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    def _inner():
        return [r for r in caplog.records if r.name == dummy_logger.logger.name]

    return _inner


@pytest.fixture
def product_logger():
    from billing.yandex_pay.yandex_pay.utils.logging import get_product_logger
    return get_product_logger()


@pytest.fixture
def product_logs(caplog, product_logger):
    caplog.set_level(logging.INFO, logger=product_logger.name)

    def _inner():
        return [r for r in caplog.records if r.name == product_logger.name]

    return _inner


@pytest.fixture
def mocked_logger(mocker):
    return mocker.MagicMock()


@pytest.fixture(autouse=True)
def settings_to_overwrite(request):
    settings_ = deepcopy(getattr(request, 'param', {}))

    # Disabling Swagger by default to speed up unrelated tests.
    # Swagger and relevant CSRF tests must explicitly enable the setting.
    settings_.setdefault('API_SWAGGER_ENABLED', False)
    return settings_


@pytest.fixture(autouse=True)
def yandex_pay_settings(settings_to_overwrite):
    original_settings = deepcopy(settings._settings)
    settings.update(settings_to_overwrite)

    yield settings

    settings._settings = original_settings


@pytest.fixture
def request_id():
    return 'unittest-request-id'


@pytest.fixture
def api_handlers(get_subclasses) -> Collection[Type[BaseHandler]]:
    all_subclasses = get_subclasses(BaseHandler)
    return [cls for cls in all_subclasses if not isabstract(cls)]


@pytest.fixture
def file_storage():
    return FileStorage()


@pytest.fixture
def run_action(dummy_logger, request_id, db_engine, file_storage):
    """
    Частично дублируем логику BaseHandler по настройке контекста,
    чтобы можно было запускать action в functional тестах.
    """

    def inner(action_cls: Type[BaseAction], action_kwargs: Dict[str, Any]):
        async def coro():
            action_cls.setup_context(
                logger=dummy_logger,
                request_id=request_id,
                db_engine=db_engine,
                file_storage=file_storage,
                retry_budget=None,
            )
            return await action_cls(**action_kwargs).run()  # type: ignore

        return asyncio.create_task(coro())

    return inner


@dataclasses.dataclass
class RequestMaker:
    """
    Вспомогательный класс, чтобы делать запросы через TestClient.
    Тест-кейс получает на вход объект класса Request, который может быть преднастроен,
    но тест-кейс имеет возможность перед вызовом `make` поправить специфичные параметры запроса.
    """
    client: aiohttp.test_utils.TestClient

    method: str
    path: str
    json: Optional[dict] = None
    headers: Optional[dict] = dataclasses.field(default_factory=dict)
    cookies: Optional[dict] = dataclasses.field(default_factory=dict)

    async def make(self):
        response = await self.client.request(
            method=self.method,
            path=self.path,
            json=self.json,
            headers=self.headers,
            cookies=self.cookies,
        )

        try:
            json = await response.json()
        except Exception:
            json = None

        return response, json


@pytest.fixture
def request_maker():
    return RequestMaker
