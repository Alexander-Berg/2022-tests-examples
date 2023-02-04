import re
from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP


@pytest.fixture
def api_url():
    return '/api/public/v1/cashback'


@pytest.fixture(autouse=True)
async def budget(storage):
    return await storage.cashback_budget.create(
        CashbackBudget(
            budget_id=uuid4(),
            currency='RUB',
            spent=Decimal('0'),
            spending_limit=Decimal('1000000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10),
        )
    )


@pytest.fixture(autouse=True)
def antifraud_mock(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.post(
        re.compile(f'{yandex_pay_plus_settings.ANTIFRAUD_API_URL}/score'),
        status=200,
        payload={'status': 'success', 'action': 'ALLOW', 'tags': []},
    )


@pytest.fixture(autouse=True)
def cards_mock(aioresponses_mocker, yandex_pay_plus_settings, entity_auth_user):
    return aioresponses_mocker.get(
        f'{yandex_pay_plus_settings.YANDEX_PAY_API_URL}'
        f'/api/internal/v1/user/cards/card-x123abc?uid={entity_auth_user.uid}',
        status=200,
        payload={
            'data': {
                'card_id': 'card-x123abc',
                'last4': '7890',
                'card_network': 'MASTERCARD',
                'issuer_bank': 'ALFABANK',
                'expiration_date': {'month': 10, 'year': 2031},
                'trust_card_id': 'card-x123abc',
            }
        },
    )


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def psp_external_id(rands):
    return rands()


@pytest.fixture(autouse=True)
async def merchant(storage, rands, merchant_id):
    return await storage.merchant.create(
        Merchant(
            merchant_id=merchant_id,
            name=rands(),
        )
    )


@pytest.fixture(autouse=True)
async def psp(storage, psp_external_id, rands):
    return await create_psp_entity(
        storage,
        PSP(
            psp_id=uuid4(),
            psp_external_id=psp_external_id,
        )
    )


@pytest.fixture
def request_params(psp_external_id, merchant_id):
    return {
        'sheet': {
            'version': 2,
            'currency_code': 'RUB',
            'country_code': 'ru',
            'merchant': {
                'id': str(merchant_id),
                'name': 'merchant-name',
            },
            'order': {
                'id': 'order-id',
                'total': {
                    'amount': '100.00',
                },
                'items': [{
                    'id': '2',
                    'label': 'Item',
                    'amount': '1474.86',
                    'quantity': {'count': '2'},
                }]
            },
            'payment_methods': [{
                'type': 'CARD',
                'gateway': psp_external_id,
                'gateway_merchant_id': 'hmnid',
                'allowed_auth_methods': ['CLOUD_TOKEN', 'PAN_ONLY'],
                'allowed_card_networks': ['MASTERCARD'],
            }, {
                'type': 'CASH',
            }],
        },
    }


@pytest.fixture
def checkout_params(merchant_id):
    return {
        'checkout_sheet': {
            'currency_code': 'RUB',
            'merchant_id': str(merchant_id),
            'cart_total': '100.00',
        },
    }


@pytest.fixture
def app(public_app, authenticate_client):
    authenticate_client(public_app)
    return public_app


@pytest.mark.asyncio
@pytest.mark.parametrize('is_checkout', (True, False))
async def test_handler_should_return_cashback_amount(app, api_url, request_params, checkout_params, is_checkout):
    params = checkout_params if is_checkout else request_params
    r = await app.post(api_url, json=params)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to({
        'status': 'success',
        'data': {
            'cashback': {
                'category': '0.03',
                'amount': '3',
            },
        },
        'code': 200,
    }))


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'card_id,should_call_pay', [(None, False), ('card-x123abc', True)]
)
async def test_get_cashback_with_card_id(
    app,
    api_url,
    request_params,
    card_id,
    should_call_pay,
    cards_mock,
    aioresponses_mocker,
    entity_auth_user,
    yandex_pay_plus_settings,
):
    request_params['card_id'] = card_id

    r = await app.post(api_url, json=request_params)

    assert_that(r.status, equal_to(200))
    if should_call_pay:
        cards_mock.assert_called_once()
    else:
        cards_mock.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize('currency', ['XTS'])
async def test_other_currencies_not_allowed(
    app,
    api_url,
    request_params,
    currency,
):
    request_params['sheet']['currency_code'] = currency
    request_params['card_id'] = 'card-x123abc'

    r = await app.post(api_url, json=request_params)

    expected_error = {
        'code': 400, 'status': 'fail', 'data': {'message': 'CURRENCY_NOT_SUPPORTED'}
    }

    assert_that(r.status, equal_to(400))
    assert_that(await r.json(), equal_to(expected_error))
