import pytest
from aiohttp import web

from sendr_aiohttp import Url
from sendr_auth import skip_authentication

from hamcrest import assert_that, has_entries

from billing.yandex_pay.yandex_pay.api.app import YandexPayApplication
from billing.yandex_pay.yandex_pay.api.handlers.base import BaseHandler
from billing.yandex_pay.yandex_pay.core.actions.base import BaseAction
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreAddressNotFoundError, CoreCardExpiredError, CoreCardNotFoundError, CoreContactNotFoundError,
    CoreInsecureMerchantOriginSchemaError, CoreInvalidMerchantOriginError, CoreMerchantOriginNotFound
)


class ExampleAction(BaseAction):
    pass


@skip_authentication
class ExampleHandler(BaseHandler):
    async def get(self):
        await self.run_action(ExampleAction)
        return web.Response()


@pytest.fixture
async def app(aiohttp_client, mocker, db_engine, yandex_pay_settings):
    mocker.patch.object(
        YandexPayApplication,
        'URLS',
        YandexPayApplication.URLS + [[Url('/path', ExampleHandler, 'v_test')]],
    )
    app = YandexPayApplication(db_engine=db_engine)
    app.file_storage = mocker.Mock()
    return await aiohttp_client(app)


@pytest.mark.parametrize('exc, expected_code, expected_exc_params', (
    pytest.param(
        CoreMerchantOriginNotFound,
        404,
        {'description': 'Unknown Merchant origin. Check web origin registration in Yandex Pay.'},
        id='CoreMerchantOriginNotFound',
    ),
    pytest.param(
        CoreInsecureMerchantOriginSchemaError(origin='http://foo.test'),
        400,
        {'description': 'Insecure origin schema: HTTPS is expected.', 'origin': 'http://foo.test'},
        id='CoreInsecureMerchantOriginSchemaError',
    ),
    pytest.param(
        CoreInvalidMerchantOriginError(origin='http://foo.test', description='desc'),
        400,
        {'description': 'desc', 'origin': 'http://foo.test'},
        id='CoreInvalidMerchantOriginError',
    ),
    pytest.param(CoreCardNotFoundError, 404, None, id='CoreCardNotFoundError'),
    pytest.param(CoreCardExpiredError, 400, None, id='CoreCardExpiredError'),
    pytest.param(CoreAddressNotFoundError, 404, None, id='CoreAddressNotFoundError'),
    pytest.param(CoreContactNotFoundError, 404, None, id='CoreContactNotFoundError'),
))
@pytest.mark.asyncio
async def test_action_raised_exception(app, mock_action, exc, expected_code, expected_exc_params):
    mock_action(ExampleAction, exc)
    params = {'params': expected_exc_params} if expected_exc_params is not None else {}
    expected_response = {
        'code': expected_code,
        'status': 'fail',
        'data': {
            'message': exc.message,
            **params,
        }
    }

    response = await app.get('path')
    data = await response.json()
    assert_that(data, has_entries(expected_response))
