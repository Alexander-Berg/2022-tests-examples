import pytest
from aiohttp import web

from hamcrest import has_properties, match_equality

from billing.yandex_pay.yandex_pay.api.handlers.mixins.psp_auth import PSPAuthMixin
from billing.yandex_pay.yandex_pay.api.handlers.psp.base import BasePSPAPIHandler
from billing.yandex_pay.yandex_pay.api.middlewares import middleware_exception_handler, middleware_logging_adapter
from billing.yandex_pay.yandex_pay.core.actions.psp.auth import AuthPSPRequestAction


class ExampleHandler(PSPAuthMixin, BasePSPAPIHandler):
    async def post(self):
        return web.json_response({})


@pytest.fixture
async def app(aiohttp_client, mocker):
    app = web.Application(middlewares=(middleware_logging_adapter, middleware_exception_handler))
    app.db_engine = mocker.Mock()
    app.file_storage = mocker.Mock()
    app.router.add_view('/path', ExampleHandler)
    return await aiohttp_client(app)


@pytest.mark.asyncio
async def test_calls_auth_psp_action(app, mock_action, mocker):
    mock = mock_action(AuthPSPRequestAction, (mocker.Mock(), mocker.Mock()))

    await app.post('/path', headers={'Authorization': '123'}, params={'pa': 'rams'}, data=b'ody')

    mock.assert_called_once_with(
        authorization_header='123',
        body=b'ody',
        method='POST',
        url=match_equality(
            has_properties({
                'path': '/path',
                'query': {'pa': 'rams'},
            })
        ),
    )
