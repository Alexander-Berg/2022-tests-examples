import pytest
from aiohttp import web

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.api.handlers.base import BaseHandler
from billing.yandex_pay.yandex_pay.api.middlewares import middleware_logging_adapter

RESOLVED_UID = 55555
REMOTE_IP = '192.0.2.2'
X_REAL_IP = '192.0.2.1'


class ExampleHandler(BaseHandler):
    async def get(self):
        return web.json_response({
            'user_ip': self.user_ip,
        })


@pytest.fixture(autouse=True)
def mock_remote_ip(mocker):
    return mocker.patch.object(web.Request, 'remote', REMOTE_IP)


@pytest.fixture
async def app(aiohttp_client, mocker):
    app = web.Application(middlewares=(middleware_logging_adapter,))
    app.db_engine = mocker.Mock()
    app.file_storage = mocker.Mock()
    app.router.add_view('/path', ExampleHandler)
    return await aiohttp_client(app)


@pytest.mark.asyncio
async def test_user_ip(app, mocker):
    res = await app.get('/path', headers={'X-Real-IP': X_REAL_IP})
    assert_that(
        (await res.json())['user_ip'],
        equal_to(X_REAL_IP),
    )


@pytest.mark.asyncio
async def test_user_ip_when_header_is_empty(app, mocker):
    res = await app.get('/path', headers={})
    assert_that(
        (await res.json())['user_ip'],
        equal_to(REMOTE_IP),
    )
