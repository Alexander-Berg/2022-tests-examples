import pytest
from aiohttp import web

from sendr_tvm.qloud_async_tvm import ServiceTicket, TicketParsingException, TvmChecker, UserTicket

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.api.handlers.mixins.tvm import BaseHandler, TvmCheckMixin


class Handler(TvmCheckMixin, BaseHandler):
    async def get(self):
        return web.json_response({'uid': self.user.uid})

    async def options(self):
        return web.Response()


@pytest.fixture
async def app(aiohttp_client, application):
    application.router.add_view('/tvm', Handler)
    return await aiohttp_client(application)


@pytest.mark.asyncio
async def test_no_tickets(app):
    r = await app.get('/tvm')

    assert_that(r.status, equal_to(403))
    assert_that(
        await r.json(), equal_to({'status': 'fail', 'data': {'message': 'Missing service ticket'}, 'code': 403})
    )


@pytest.mark.asyncio
async def test_options(app):
    r = await app.options('/tvm')

    assert_that(r.status, equal_to(200))


@pytest.mark.asyncio
async def test_no_user_ticket(app, mocker, bill_payments_settings):
    bill_payments_settings.TVM_ALLOWED_CLIENTS = (222,)
    mocker.patch.object(TvmChecker, 'parse_service_ticket', mocker.AsyncMock(return_value=ServiceTicket(src=222)))
    r = await app.get('/tvm', headers={'x-ya-service-ticket': 'fake_service_ticket'})

    assert_that(r.status, equal_to(403))
    assert_that(await r.json(), equal_to({'status': 'fail', 'data': {'message': 'Missing user ticket'}, 'code': 403}))


@pytest.mark.asyncio
async def test_not_allowed_src(app, mocker):
    mocker.patch.object(TvmChecker, 'parse_service_ticket', mocker.AsyncMock(return_value=ServiceTicket(src=222)))

    r = await app.get(
        '/tvm',
        headers={
            'x-ya-service-ticket': 'fake_service_ticket',
            'x-ya-user-ticket': 'fake_user_ticket',
        },
    )

    assert_that(r.status, equal_to(403))
    assert_that(await r.json(), equal_to({'status': 'fail', 'data': {'message': 'Not allowed for 222'}, 'code': 403}))


@pytest.mark.asyncio
async def test_success(app, mocker, bill_payments_settings):
    bill_payments_settings.TVM_ALLOWED_CLIENTS = (222,)
    mocker.patch.object(TvmChecker, 'parse_service_ticket', mocker.AsyncMock(return_value=ServiceTicket(src=222)))
    mocker.patch.object(TvmChecker, 'parse_user_ticket', mocker.AsyncMock(return_value=UserTicket(uid=1)))

    r = await app.get(
        '/tvm',
        headers={
            'x-ya-service-ticket': 'fake_service_ticket',
            'x-ya-user-ticket': 'fake_user_ticket',
        },
    )

    assert_that(r.status, equal_to(200))
    assert_that(await r.json(), equal_to({'uid': 1}))


@pytest.mark.asyncio
async def test_parse_service_ticket_exception(app, mocker, bill_payments_settings):
    bill_payments_settings.TVM_ALLOWED_CLIENTS = (222,)
    mocker.patch.object(TvmChecker, 'parse_service_ticket', side_effect=TicketParsingException('', '', ''))

    r = await app.get(
        '/tvm',
        headers={
            'x-ya-service-ticket': 'fake_service_ticket',
            'x-ya-user-ticket': 'fake_user_ticket',
        },
    )

    assert_that(r.status, equal_to(403))
    assert_that(
        await r.json(), equal_to({'status': 'fail', 'data': {'message': 'Service not authorized'}, 'code': 403})
    )


@pytest.mark.asyncio
async def test_parse_user_ticket_exception(app, mocker, bill_payments_settings):
    bill_payments_settings.TVM_ALLOWED_CLIENTS = (222,)
    mocker.patch.object(TvmChecker, 'parse_service_ticket', mocker.AsyncMock(return_value=ServiceTicket(src=222)))
    mocker.patch.object(TvmChecker, 'parse_user_ticket', side_effect=TicketParsingException('', '', ''))

    r = await app.get(
        '/tvm',
        headers={
            'x-ya-service-ticket': 'fake_service_ticket',
            'x-ya-user-ticket': 'fake_user_ticket',
        },
    )

    assert_that(r.status, equal_to(403))
    assert_that(await r.json(), equal_to({'status': 'fail', 'data': {'message': 'User not authorized'}, 'code': 403}))


@pytest.mark.asyncio
async def test_blackbox_auth(app, mocker, auth_user):
    blackbox_mock = mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=auth_user))
    r = await app.get('/tvm')

    blackbox_mock.assert_called_once()
    assert_that(r.status, equal_to(200))
    assert_that(await r.json(), equal_to({'uid': auth_user.uid}))


@pytest.mark.asyncio
async def test_blackbox_auth_disabled(app, mocker, auth_user, bill_payments_settings):
    bill_payments_settings.API_BLACKBOX_AUTHENTICATION_ENABLED = False
    blackbox_mock = mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=auth_user))
    r = await app.get('/tvm')

    blackbox_mock.assert_called_once()
    assert_that(r.status, equal_to(403))
    assert_that(
        await r.json(), equal_to({'status': 'fail', 'data': {'message': 'Missing service ticket'}, 'code': 403})
    )
