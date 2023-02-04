import pytest
from aiohttp import web

from sendr_aiohttp import Url

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay.yandex_pay.api.app import YandexPayApplication
from billing.yandex_pay.yandex_pay.api.handlers.events.base import BaseLoggingNotificationHandler
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPType


class ExampleHandler(BaseLoggingNotificationHandler):
    tsp = TSPType.UNKNOWN

    async def post(self):
        return web.Response()


@pytest.fixture
async def app(aiohttp_client, mocker, db_engine, mocked_logger, yandex_pay_settings):
    yandex_pay_settings.API_CHECK_CLIENT_SSL = True
    mocker.patch.object(
        YandexPayApplication,
        'URLS',
        YandexPayApplication.URLS + [[Url('/path', ExampleHandler, 'v_test')]],
    )
    mocker.patch.object(
        BaseLoggingNotificationHandler,
        'logger',
        mocker.PropertyMock(return_value=mocked_logger)
    )
    app = YandexPayApplication(db_engine=db_engine)
    app.file_storage = mocker.Mock()
    return await aiohttp_client(app)


@pytest.mark.asyncio
async def test_ssl_client_certificate_is_missing(app, mocked_logger):
    response = await app.post(
        'path',
        headers={
            'X-SSL-Client-Verify': 'undefined',
            'X-SSL-Client-Subject': 'undefined',
            'X-SSL-Client-CN': 'undefined',
        }
    )

    assert_that(response.status, equal_to(400))

    _, call_kwargs = mocked_logger.context_push.call_args_list[0]
    assert_that(
        call_kwargs,
        has_entries(
            ssl_client=dict(verify='undefined', subject='undefined', cn='undefined'),
        )
    )

    mocked_logger.exception.assert_called_once_with('Failed to check client certificate')


@pytest.mark.asyncio
async def test_ssl_client_certificate_is_present(app, mocked_logger):
    response = await app.post(
        'path',
        headers={
            'X-SSL-Client-Verify': '0',
            'X-SSL-Client-Subject': 'subject',
            'X-SSL-Client-CN': 'cn',
        }
    )

    assert_that(response.status, equal_to(200))

    _, call_kwargs = mocked_logger.context_push.call_args_list[0]
    assert_that(
        call_kwargs,
        has_entries(
            ssl_client=dict(verify='0', subject='subject', cn='cn'),
        )
    )
