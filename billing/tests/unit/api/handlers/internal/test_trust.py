from decimal import Decimal

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.payment_token.internal.trust import (
    CreateInternalTrustPaymentTokenAction
)
from billing.yandex_pay.yandex_pay.core.entities.user import User

URL = '/api/internal/v1/trust/payment_tokens'


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def payment_token(rands):
    return rands()


@pytest.fixture
def create_token_data(uid):
    return {
        'trust_card_id': 'trust_card_id',
        'uid': uid,
        'amount': '12.34',
        'currency': 'XTS',
    }


@pytest.fixture(autouse=True)
def mock_internal_trust_token_action(mock_action, payment_token):
    return mock_action(CreateInternalTrustPaymentTokenAction, payment_token)


@pytest.mark.asyncio
async def test_post(
    internal_app,
    mock_internal_trust_token_action,
    mock_internal_tvm,
    create_token_data,
    payment_token,
    uid,
):
    r = await internal_app.post(URL, json=create_token_data)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    expected_response = {
        'code': 200,
        'status': 'success',
        'data': {'payment_token': payment_token},
    }
    assert_that(json_body, equal_to(expected_response))

    mock_internal_trust_token_action.assert_called_once_with(
        trust_card_id='trust_card_id',
        user=User(uid),
        amount=Decimal('12.34'),
        currency='XTS',
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('bad_currency', ['xts', 'XT', ''])
async def test_post_invalid_currency(
    internal_app,
    mock_internal_trust_token_action,
    mock_internal_tvm,
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

    mock_internal_trust_token_action.assert_not_called()


@pytest.mark.asyncio
async def test_post_empty_trust_card_id(
    internal_app,
    mock_internal_trust_token_action,
    mock_internal_tvm,
    create_token_data,
):
    create_token_data['trust_card_id'] = ''

    r = await internal_app.post(URL, json=create_token_data)
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    expected_response = {
        'code': 400,
        'status': 'fail',
        'data': {
            'params': {'trust_card_id': ['String should not be empty.']},
            'message': 'BAD_REQUEST',
        },
    }
    assert_that(json_body, equal_to(expected_response))

    mock_internal_trust_token_action.assert_not_called()
