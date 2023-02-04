from uuid import UUID

import pytest
from aiohttp import web

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.base import BaseHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.mixins.authenticate_merchant import AuthenticateMerchantMixin
from billing.yandex_pay_plus.yandex_pay_plus.api.middlewares import (
    middleware_exceptions_handler,
    middleware_logging_adapter,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.authenticate import AuthenticateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import MerchantAuthenticationError


class ExampleHandler(AuthenticateMerchantMixin, BaseHandler):
    async def get(self):
        return self.make_response({'merchant_id': str(self.merchant_id)})


@pytest.fixture
async def app(aiohttp_client, mocker):
    app = web.Application(middlewares=(middleware_logging_adapter, middleware_exceptions_handler))
    app.db_engine = mocker.Mock()
    app.file_storage = mocker.Mock()
    app.router.add_view('/path', ExampleHandler)
    return await aiohttp_client(app)


@pytest.mark.asyncio
async def test_calls_action(app, mock_action):
    mock = mock_action(AuthenticateMerchantAction, UUID('bbb9c171-2fab-45e6-b1f8-6212980aa9bb'))

    r = await app.get('/path', headers={'Authorization': 'Api-Key test-token'})
    data = await r.json()

    mock.assert_called_once_with(authorization_header='Api-Key test-token')
    assert_that(r.status, equal_to(200))
    assert_that(data, equal_to({'merchant_id': 'bbb9c171-2fab-45e6-b1f8-6212980aa9bb'}))


@pytest.mark.asyncio
async def test_incorrect_token(app, mock_action):
    mock = mock_action(AuthenticateMerchantAction, MerchantAuthenticationError)

    r = await app.get('/path')
    data = await r.json()

    mock.assert_called_once_with(authorization_header=None)
    assert_that(r.status, equal_to(401))
    assert_that(data, equal_to({'status': 'fail', 'code': 401, 'data': {'message': 'AUTHENTICATION_ERROR'}}))
