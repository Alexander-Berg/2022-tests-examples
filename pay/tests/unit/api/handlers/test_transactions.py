from datetime import datetime, timezone
from uuid import UUID

import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.transaction.create import CreateTransactionAction
from pay.bill_payments.bill_payments.core.entities.mpi_3ds_info import MPI3DSInfo
from pay.bill_payments.bill_payments.storage.entities.enums import OrderStatus, PaymentMethodType
from pay.bill_payments.bill_payments.storage.entities.transaction import PayerData, Transaction
from pay.bill_payments.bill_payments.tests.utils import remove_keys, set_key

EXAMPLE_POST_DATA = dict(
    payment_method='YANDEX_PAY',
    payment_token='BASE64TOKEN',
    payer_full_name='Иванов Иван Иванович',
    mpi_3ds_info=dict(
        browser_language='ru',
        browser_tz='-180',
        browser_accept_header='ACCEPT_HEADER',
        browser_ip='192.0.2.1',
        browser_screen_width=1920,
        browser_screen_height=1080,
        window_width=640,
        window_height=480,
        browser_color_depth=24,
        browser_user_agent='USER_AGENT',
        browser_javascript_enabled=True,
    ),
    return_url='https://return.test',
)
EXAMPLE_TRANSACTION = Transaction(
    status=OrderStatus.NEW,
    amount=15000,
    external_payment_id='external-payment-id',
    order_id=UUID('ed8716c9-7f68-4f4d-83a2-7c8882002ce3'),
    transaction_id=UUID('6dee0386-682c-4df8-b3ff-8ce79ea9bd57'),
    payer_data=PayerData(payer_full_name='Иванов Иван Иванович'),
    created=datetime(2020, 12, 30, 14, 31, 59, tzinfo=timezone.utc),
    updated=datetime(2020, 12, 30, 14, 31, 59, tzinfo=timezone.utc),
    acs_url='https://acs.test/path?pa=rams&parapapa=ams',
)
URI = '/api/v1/orders/ed8716c9-7f68-4f4d-83a2-7c8882002ce3/transactions'


@pytest.fixture(autouse=True)
def mock_authentication(mocker, auth_user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=auth_user))


@pytest.mark.asyncio
async def check_schema_error(response, expected_error):
    assert_that(response.status, equal_to(400))
    assert_that(
        await response.json(),
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': expected_error,
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_post_transactions_response(app, mock_action):
    mock_action(CreateTransactionAction, EXAMPLE_TRANSACTION)

    resp = await app.post(URI, json=EXAMPLE_POST_DATA)

    assert_that(resp.status, equal_to(201))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'status': 'success',
                'code': 201,
                'data': {
                    'transaction': {
                        'status': 'NEW',
                        'transaction_id': '6dee0386-682c-4df8-b3ff-8ce79ea9bd57',
                        'order_id': 'ed8716c9-7f68-4f4d-83a2-7c8882002ce3',
                        'acs_url': 'https://acs.test/path?pa=rams&parapapa=ams',
                        'payment_method': 'YANDEX_PAY',
                        'amount': 15000,
                        'created': '2020-12-30T14:31:59+00:00',
                        'updated': '2020-12-30T14:31:59+00:00',
                    }
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_post_transactions_validates_payment_method(app):
    data = set_key(EXAMPLE_POST_DATA, 'payment_method', 'GOOGLE_PAY')
    resp = await app.post(URI, json=data)

    await check_schema_error(resp, {'payment_method': ['Invalid enum member GOOGLE_PAY']})


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'path',
    (
        pytest.param('payment_token', id='empty-payment-token'),
        pytest.param('payer_full_name', id='empty-payer-full-name'),
    ),
)
async def test_post_transactions_validates_empty_strings(app, path):
    data = set_key(EXAMPLE_POST_DATA, path, '')
    resp = await app.post(URI, json=data)

    await check_schema_error(resp, set_key({}, path, ['String should not be empty.']))


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'url',
    (
        pytest.param('', id='empty-return-url'),
        pytest.param('http://foo.bar', id='return-url-should-be-https'),
    ),
)
async def test_post_transactions_validates_return_url(app, url):
    data = set_key(EXAMPLE_POST_DATA, 'return_url', url)
    resp = await app.post(URI, json=data)

    await check_schema_error(resp, {'return_url': ['Not a valid URL.']})


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'path',
    (
        'mpi_3ds_info.browser_screen_height',
        'mpi_3ds_info.browser_screen_width',
        'mpi_3ds_info.window_height',
        'mpi_3ds_info.window_width',
        'mpi_3ds_info.browser_color_depth',
    ),
)
async def test_post_transactions_validates_integer_field(app, path):
    data = set_key(EXAMPLE_POST_DATA, path, 'not-an-integer')
    resp = await app.post(URI, json=data)

    await check_schema_error(resp, set_key({}, path, ['Not a valid integer.']))


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'path',
    (
        'payment_method',
        'return_url',
        'payment_token',
        'payer_full_name',
        'mpi_3ds_info.browser_screen_height',
        'mpi_3ds_info.browser_screen_width',
        'mpi_3ds_info.window_height',
        'mpi_3ds_info.window_width',
        'mpi_3ds_info.browser_color_depth',
        'mpi_3ds_info.browser_javascript_enabled',
        'mpi_3ds_info.browser_ip',
        'mpi_3ds_info.browser_tz',
        'mpi_3ds_info.browser_language',
        'mpi_3ds_info.browser_user_agent',
        'mpi_3ds_info.browser_accept_header',
    ),
)
async def test_post_transactions_validates_required_field(app, path):
    data = remove_keys(EXAMPLE_POST_DATA, path)
    resp = await app.post(URI, json=data)

    await check_schema_error(resp, set_key({}, path, ['Missing data for required field.']))


@pytest.mark.asyncio
async def test_post_transactions_calls_action(app, mock_action, auth_user):
    mock = mock_action(CreateTransactionAction, EXAMPLE_TRANSACTION)

    await app.post(URI, json=EXAMPLE_POST_DATA)

    expected_call_params = {
        'uid': auth_user.uid,
        'payment_token': 'BASE64TOKEN',
        'payment_method': PaymentMethodType.YANDEX_PAY,
        'payer_full_name': 'Иванов Иван Иванович',
        'mpi_3ds_info': MPI3DSInfo(
            browser_accept_header='ACCEPT_HEADER',
            browser_color_depth=24,
            browser_ip='192.0.2.1',
            browser_language='ru',
            browser_screen_width=1920,
            browser_screen_height=1080,
            browser_tz='-180',
            browser_user_agent='USER_AGENT',
            browser_javascript_enabled=True,
            window_width=640,
            window_height=480,
        ),
        'return_url': 'https://return.test',
        'order_id': UUID('ed8716c9-7f68-4f4d-83a2-7c8882002ce3'),
    }
    mock.assert_called_once_with(**expected_call_params)
