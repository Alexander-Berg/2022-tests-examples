from uuid import UUID

import pytest
from aiohttp import web

from sendr_aiohttp import Url
from sendr_auth import skip_authentication

from hamcrest import assert_that, has_entries, match_equality, not_none

from billing.yandex_pay_plus.yandex_pay_plus.api.base_app import YandexPayPlusApplication
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.base import BaseHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.merchant.base import BaseMerchantHandler
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.base import BaseAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.authenticate import AuthenticateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    AddressNotFoundError,
    ContactNotFoundError,
    CoreInsecureMerchantOriginSchemaError,
    CoreInvalidMerchantOriginError,
    CoreInvalidPaymentStatusError,
    MerchantMalformedResponseError,
    MerchantRejectedOrderError,
    MerchantUnexpectedError,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus


class ExampleAction(BaseAction):
    pass


@skip_authentication
class ExampleHandler(BaseHandler):
    async def get(self):
        await self.run_action(ExampleAction)
        return web.Response()


@skip_authentication
class ExampleMerchantHandler(BaseMerchantHandler):
    async def get(self):
        await self.run_action(ExampleAction)
        return web.Response()


@pytest.fixture
async def app(aiohttp_client, mocker, db_engine, yandex_pay_plus_settings):
    mocker.patch.object(
        YandexPayPlusApplication,
        '_urls',
        [
            [Url('/path', ExampleHandler, 'v_test')],
            [Url('/merchant/path', ExampleMerchantHandler, 'v_merchant_test')],
        ],
    )
    app = YandexPayPlusApplication(db_engine=db_engine)
    app.file_storage = mocker.Mock()
    return await aiohttp_client(app)


@pytest.mark.parametrize('exc, expected_code, expected_exc_params', (
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
    pytest.param(MerchantUnexpectedError, 400, None, id='MerchantUnexpectedError'),
    pytest.param(
        MerchantRejectedOrderError(reason_code='1', description='2'),
        400,
        {'description': '2'},
        id='MerchantRejectedOrderError',
    ),
    pytest.param(
        MerchantMalformedResponseError(
            validation_errors_description={'foo': ['bar']}
        ),
        400,
        {'validation_errors_description': {'foo': ['bar']}},
        id='MerchantRejectedOrderError',
    ),
    pytest.param(AddressNotFoundError, 404, None, id='AddressNotFoundError'),
    pytest.param(ContactNotFoundError, 404, None, id='ContactNotFoundError'),
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

    response = await app.get('/path')
    data = await response.json()
    assert_that(data, has_entries(expected_response))


@pytest.mark.parametrize('exc, expected_reason_code, expected_details', (
    pytest.param(
        CoreInvalidPaymentStatusError(expected=TransactionStatus.CHARGED, actual=TransactionStatus.AUTHORIZED),
        'INVALID_PAYMENT_STATUS',
        {'expected': ['CAPTURED'], 'actual': 'AUTHORIZED'},
        id='ContactNotFoundError'
    ),
))
@pytest.mark.asyncio
async def test_merchant_exception(app, mock_action, exc, expected_reason_code, expected_details):
    mock_action(ExampleAction, exc)
    mock_action(AuthenticateMerchantAction, UUID('63b99367-682f-43c6-bf43-574b3a667c74'))
    expected_response = {
        'reasonCode': expected_reason_code,
        'status': 'fail',
        'reason': match_equality(not_none()),
        'details': expected_details,
    }

    response = await app.get('/merchant/path')
    data = await response.json()
    assert_that(data, has_entries(expected_response))
