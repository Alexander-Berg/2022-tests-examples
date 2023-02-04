import pytest
from aiohttp import web

from hamcrest import assert_that, contains_inanyorder, equal_to

from billing.yandex_pay.yandex_pay.api.handlers.base import BaseHandler
from billing.yandex_pay.yandex_pay.api.handlers.mixins.geo import GeoCheckMixin
from billing.yandex_pay.yandex_pay.api.middlewares import middleware_exception_handler, middleware_logging_adapter
from billing.yandex_pay.yandex_pay.core.actions.geo import CheckRegionAction
from billing.yandex_pay.yandex_pay.core.exceptions import CoreForbiddenRegionError


class ExampleHandler(GeoCheckMixin, BaseHandler):
    async def get(self):
        fcn = [x.value for x in self.forbidden_card_networks] if self.forbidden_card_networks else []
        return web.json_response({'forbidden_card_networks': fcn})


@pytest.fixture
async def app(aiohttp_client, mocker):
    app = web.Application(middlewares=(middleware_logging_adapter, middleware_exception_handler))
    app.db_engine = mocker.Mock()
    app.file_storage = mocker.Mock()
    app.router.add_view('/path', ExampleHandler)
    return await aiohttp_client(app)


@pytest.mark.asyncio
async def test_calls_check_region_action(app, mock_action):
    mock = mock_action(CheckRegionAction)

    await app.get('/path', headers={'x-region-id': '1', 'x-region-suspected': '2'})

    mock.assert_called_once_with(region_id=1)


@pytest.mark.asyncio
async def test_forbidden_card_networks_is_filled(app, mock_action):
    mock_action(CheckRegionAction, CoreForbiddenRegionError)

    resp = await app.get('/path', headers={'x-region-id': '1', 'x-region-suspected': '2'})

    assert_that(resp.status, equal_to(200))
    assert_that(
        (await resp.json())['forbidden_card_networks'],
        contains_inanyorder('MASTERCARD', 'VISA', 'MAESTRO', 'VISAELECTRON')
    )


@pytest.mark.asyncio
async def test_when_headers_are_invalid__returns_200(app, mock_action):
    mock_action(CheckRegionAction)

    resp = await app.get('/path', headers={'x-region-id': 'invalid'})

    assert_that(resp.status, equal_to(200))


@pytest.mark.asyncio
async def test_when_headers_are_invalid__calls_action_with_nones(app, mock_action):
    mock = mock_action(CheckRegionAction)

    await app.get('/path', headers={'x-region-id': 'invalid'})

    mock.assert_called_once_with(region_id=None)
