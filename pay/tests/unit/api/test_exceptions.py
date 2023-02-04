import traceback
from uuid import UUID

import pytest
from aiohttp import web

from sendr_aiohttp import Url
from sendr_auth import skip_authentication
from sendr_core.exceptions import BaseCoreError

from hamcrest import assert_that, equal_to, has_entries

from pay.bill_payments.bill_payments.api.app import BillPaymentsApplication
from pay.bill_payments.bill_payments.api.handlers.base import BaseHandler, raise_core_exception_result
from pay.bill_payments.bill_payments.core.actions.base import BaseAction
from pay.bill_payments.bill_payments.core.exceptions import (
    BillAlreadyPaidError,
    BillNotFoundError,
    CoreAlreadyExistsError,
    CoreDataError,
    CoreNotFoundError,
    DocumentNotFoundError,
    MixedDepartmentsError,
    OrderNotFoundError,
    PaymentMethodNotSupportedError,
    TooManyDocumentsError,
    UserAlreadyExistsError,
    UserNotFoundError,
)


class ExampleAction(BaseAction):
    pass


@skip_authentication
class ExampleHandler(BaseHandler):
    async def get(self):
        await self.run_action(ExampleAction)
        return web.Response()


@pytest.fixture
async def app(aiohttp_client, mocker, db_engine, bill_payments_settings):
    mocker.patch.object(
        BillPaymentsApplication,
        '_urls',
        BillPaymentsApplication._urls + ([Url('/path', ExampleHandler, 'v_test')],),
    )
    app = BillPaymentsApplication(db_engine=db_engine)
    app.file_storage = mocker.Mock()
    app.pushers = mocker.Mock()
    return await aiohttp_client(app)


@pytest.mark.parametrize(
    'exc, expected_code, expected_message, expected_exc_params',
    (
        pytest.param(CoreAlreadyExistsError, 409, 'ALREADY_EXISTS', None, id='CoreAlreadyExistsError'),
        pytest.param(CoreNotFoundError, 404, 'NOT_FOUND', None, id='CoreNotFoundError'),
        pytest.param(CoreDataError, 400, 'DATA_ERROR', None, id='CoreDataError'),
        pytest.param(DocumentNotFoundError, 404, 'DOCUMENT_NOT_FOUND', None, id='DocumentNotFoundError'),
        pytest.param(TooManyDocumentsError(10), 400, 'TOO_MANY_DOCUMENTS', {'limit': 10}, id='TooManyDocumentsError'),
        pytest.param(UserAlreadyExistsError, 409, 'USER_ALREADY_EXISTS', None, id='UserAlreadyExistsError'),
        pytest.param(UserNotFoundError, 404, 'USER_NOT_FOUND', None, id='UserNotFoundError'),
        pytest.param(
            PaymentMethodNotSupportedError,
            400,
            'PAYMENT_METHOD_NOT_SUPPORTED',
            None,
            id='PaymentMethodNotSupportedError',
        ),
        pytest.param(MixedDepartmentsError, 400, 'MIXED_DEPARTMENTS', None, id='MixedDepartmentsError'),
        pytest.param(
            BillNotFoundError(bill_id=UUID('146c44be-a526-455b-bb7a-e7d633099c5c')),
            404,
            'BILL_NOT_FOUND',
            {'bill_id': '146c44be-a526-455b-bb7a-e7d633099c5c'},
            id='BillNotFoundError',
        ),
        pytest.param(BillAlreadyPaidError, 400, 'BILL_ALREADY_PAID', None, id='BillAlreadyPaidError'),
        pytest.param(OrderNotFoundError, 404, 'ORDER_NOT_FOUND', None, id='OrderNotFoundError'),
    ),
)
@pytest.mark.asyncio
async def test_action_raised_exception(app, mock_action, exc, expected_message, expected_code, expected_exc_params):
    mock_action(ExampleAction, exc)
    params = {'params': expected_exc_params} if expected_exc_params is not None else {}

    response = await app.get('/path')

    assert_that(response.status, equal_to(expected_code))
    data = await response.json()
    expected_response = {
        'code': expected_code,
        'status': 'fail',
        'data': {
            'message': expected_message,
            **params,
        },
    }
    assert_that(data, has_entries(expected_response))


@pytest.mark.asyncio
async def test_should_rethrow_stack_trace(app, mock_action):
    mock_action(ExampleAction, CoreNotFoundError)

    internal_error = None
    try:
        await ExampleAction().run()
    except BaseCoreError as exc:
        internal_error = exc

    trace = ''
    try:
        raise_core_exception_result(internal_error)
    except:
        trace = traceback.format_exc()

    assert 'CoreNotFoundError' in trace
