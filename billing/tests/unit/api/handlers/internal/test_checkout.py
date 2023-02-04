from datetime import timedelta
from decimal import Decimal

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.payment_token.internal.checkout import (
    CreateInternalCheckoutPaymentTokenAction, InternalCheckoutResponse
)
from billing.yandex_pay.yandex_pay.core.entities.checkout import PaymentMethodInfo
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.message import Message
from billing.yandex_pay.yandex_pay.core.entities.user import User

URL = '/api/internal/v1/checkout/payment_tokens'


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def payment_token(rands):
    return rands()


@pytest.fixture
def message_id(rands):
    return rands()


@pytest.fixture
def create_token_data(uid):
    return {
        'uid': uid,
        'gateway_merchant_id': 'gateway_merchant_id',
        'card_id': 'card_id',
        'currency': 'XTS',
        'amount': '12.34',
        'auth_methods': ['CLOUD_TOKEN', 'PAN_ONLY'],
        'psp_external_id': 'psp_external_id',
    }


@pytest.fixture
def internal_checkout_response(message_id, payment_token):
    message = Message(message_id, utcnow() + timedelta(days=1))

    return InternalCheckoutResponse(
        payment_method_info=PaymentMethodInfo(
            method_type=PaymentMethodType.CARD,
            payment_token=payment_token,
            auth_method=AuthMethod.CLOUD_TOKEN,
            card_last4='0000',
            card_network=CardNetwork.MASTERCARD,
        ),
        message=message,
    )


@pytest.fixture(autouse=True)
def mock_internal_checkout_action(mock_action, internal_checkout_response):
    return mock_action(CreateInternalCheckoutPaymentTokenAction, internal_checkout_response)


@pytest.fixture(autouse=True)
def mock_internal_tvm_auto(mock_internal_tvm):
    pass


@pytest.mark.asyncio
async def test_post(
    internal_app,
    create_token_data,
    payment_token,
    message_id,
):
    r = await internal_app.post(URL, json=create_token_data, raise_for_status=True)
    json_body = await r.json()

    expected_response = {
        'code': 200,
        'status': 'success',
        'data': {
            'payment_token': payment_token,
            'message_id': message_id,
            'payment_method_info': {
                'auth_method': 'CLOUD_TOKEN',
                'card_last4': '0000',
                'card_network': 'MASTERCARD',
            }
        },
    }
    assert_that(json_body, equal_to(expected_response))


@pytest.mark.asyncio
async def test_internal_checkout_action_called(
    internal_app,
    create_token_data,
    mock_internal_checkout_action,
    uid,
):
    await internal_app.post(URL, json=create_token_data, raise_for_status=True)

    mock_internal_checkout_action.assert_called_once_with(
        user=User(uid),
        gateway_merchant_id='gateway_merchant_id',
        card_id='card_id',
        currency='XTS',
        amount=Decimal('12.34'),
        auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
        psp_external_id='psp_external_id',
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('bad_currency', ['xts', 'XT', ''])
async def test_post_invalid_currency(
    internal_app,
    mock_internal_checkout_action,
    create_token_data,
    bad_currency,
):
    create_token_data['currency'] = bad_currency

    r = await internal_app.post(URL, json=create_token_data)
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    expected_response = {
        'code': 400,
        'status': 'fail',
        'data': {
            'params': {'currency': ['Not a valid ISO 4217 alpha code.']},
            'message': 'BAD_REQUEST',
        },
    }
    assert_that(json_body, equal_to(expected_response))

    mock_internal_checkout_action.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'key', ['gateway_merchant_id', 'card_id', 'psp_external_id']
)
async def test_post_empty_string_key(
    internal_app,
    mock_internal_checkout_action,
    create_token_data,
    key,
):
    create_token_data[key] = ''

    r = await internal_app.post(URL, json=create_token_data)
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    expected_response = {
        'code': 400,
        'status': 'fail',
        'data': {
            'params': {key: ['String should not be empty.']},
            'message': 'BAD_REQUEST',
        },
    }
    assert_that(json_body, equal_to(expected_response))

    mock_internal_checkout_action.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'auth_methods,expected_error',
    [
        (['CLOUD_TOKEN', 'PAN_ONLY', 'CLOUD_TOKEN'], 'Duplicate item with value AuthMethod.CLOUD_TOKEN'),
        ([], 'Shorter than minimum length 1.'),
    ]
)
async def test_post_invalid_auth_methods(
    internal_app,
    mock_internal_checkout_action,
    create_token_data,
    auth_methods,
    expected_error,
):
    create_token_data['auth_methods'] = auth_methods

    r = await internal_app.post(URL, json=create_token_data)
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    expected_response = {
        'code': 400,
        'status': 'fail',
        'data': {
            'params': {'auth_methods': [expected_error]},
            'message': 'BAD_REQUEST',
        },
    }
    assert_that(json_body, equal_to(expected_response))

    mock_internal_checkout_action.assert_not_called()
