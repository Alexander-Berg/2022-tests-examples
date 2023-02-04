from uuid import UUID

import pytest
from aiohttp import web

from sendr_qlog.http.aiohttp import get_middleware_logging_adapter

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.api.handlers.base import BaseHandler
from billing.yandex_pay_admin.yandex_pay_admin.api.middlewares import middleware_exception_handler
from billing.yandex_pay_admin.yandex_pay_admin.api.mixins.authenticate_agent import AuthenticateAgentMixin
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.agent.authenticate import AuthenticateAgentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import AgentAuthenticationError


class ExampleHandler(AuthenticateAgentMixin, BaseHandler):
    async def get(self):
        return self.make_response({'agent_id': str(self.agent_id)})


@pytest.fixture
async def app(aiohttp_client, mocker):
    app = web.Application(middlewares=(get_middleware_logging_adapter(), middleware_exception_handler))
    app.db_engine = mocker.Mock()
    app.file_storage = mocker.Mock()
    app.router.add_view('/path', ExampleHandler)
    return await aiohttp_client(app)


@pytest.mark.asyncio
async def test_calls_action(app, mock_action):
    mock = mock_action(AuthenticateAgentAction, UUID('bbb9c171-2fab-45e6-b1f8-6212980aa9bb'))

    r = await app.get('/path', headers={'Authorization': 'OAuth test-token', 'X-Real-Ip': '1.1.1.1'})
    data = await r.json()

    mock.assert_called_once_with(authorization_header='OAuth test-token', user_ip='1.1.1.1')
    assert_that(r.status, equal_to(200))
    assert_that(data, equal_to({'agent_id': 'bbb9c171-2fab-45e6-b1f8-6212980aa9bb'}))


@pytest.mark.asyncio
async def test_exception(app, mock_action):
    mock = mock_action(AuthenticateAgentAction, AgentAuthenticationError)

    r = await app.get('/path', headers={'X-Real-Ip': '1.1.1.1'})
    data = await r.json()

    mock.assert_called_once_with(authorization_header=None, user_ip='1.1.1.1')
    assert_that(r.status, equal_to(403))
    assert_that(data, equal_to({'status': 'fail', 'code': 403, 'data': {'message': 'AUTHENTICATION_ERROR'}}))
