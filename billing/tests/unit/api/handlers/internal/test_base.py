import pytest
from aiohttp import web
from pay.lib.tvm.ticket_checker import TvmTicketChecker

from billing.yandex_pay.yandex_pay.api.handlers.internal.base import BaseInternalHandler
from billing.yandex_pay.yandex_pay.api.middlewares import middleware_exception_handler, middleware_logging_adapter


@pytest.fixture
def example_handler_cls():
    class ExampleHandler(BaseInternalHandler):
        CHECK_TVM = False

        async def get(self):
            return web.json_response({})

        async def options(self):
            return web.Response()

    return ExampleHandler


@pytest.fixture
def example_handler_cls_with_tvm(example_handler_cls):
    class ExampleHandler(example_handler_cls):
        CHECK_TVM = True

    return ExampleHandler


@pytest.fixture
async def app(aiohttp_client, mocker, example_handler_cls, example_handler_cls_with_tvm):
    app = web.Application(middlewares=(middleware_logging_adapter, middleware_exception_handler))
    app.db_engine = mocker.Mock()
    app.file_storage = mocker.Mock()
    app.router.add_view('/no-tvm', example_handler_cls)
    app.router.add_view('/yes-tvm', example_handler_cls_with_tvm)
    return await aiohttp_client(app)


@pytest.mark.asyncio
async def test_calls_check_tvm__when_required(app, mocker):
    mock = mocker.patch.object(TvmTicketChecker, 'check_tvm_service_ticket')

    r = await app.get('/yes-tvm')

    assert r.status == 200
    mock.assert_called_once()


@pytest.mark.asyncio
async def test_not_calls_check_tvm__when_not_required(app, mocker):
    mock = mocker.patch.object(TvmTicketChecker, 'check_tvm_service_ticket')

    r = await app.get('/no-tvm')

    assert r.status == 200
    mock.assert_not_called()


@pytest.mark.asyncio
async def test_not_calls_check_tvm__when_method_is_options(app, mocker):
    mock = mocker.patch.object(TvmTicketChecker, 'check_tvm_service_ticket')

    r = await app.options('/yes-tvm')

    assert r.status == 200
    mock.assert_not_called()
