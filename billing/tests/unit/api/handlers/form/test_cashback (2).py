import json
from base64 import b64encode
from decimal import Decimal
from uuid import UUID

import pytest
from pay.lib.entities.enums import AuthMethod, CardNetwork, ClassicPaymentMethodType, PaymentMethodType
from pay.lib.entities.payment_sheet import PaymentMerchant, PaymentMethod, PaymentOrder, PaymentOrderTotal, PaymentSheet

from hamcrest import assert_that, equal_to, has_entry

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.get_adapter import GetCashbackAdapterAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.cashback import Cashback, CashbackRequest


@pytest.fixture
def api_url():
    return '/api/public/v1/cashback'


@pytest.fixture
def app(public_app):
    return public_app


@pytest.fixture(autouse=True)
def mock_get_cashback(mock_action):
    return mock_action(
        GetCashbackAdapterAction,
        Cashback(category=Decimal('0.05'), amount=Decimal('100.00'), order_limit=Decimal('3000.00')),
    )


@pytest.fixture
def uaas_headers():
    pay_testitem = [
        {
            'HANDLER': 'YANDEX_PAY_BACKEND',
            'CONTEXT': {
                'MAIN': {
                    'YANDEX_PAY_BACKEND': {'yandex_pay_plus.cashback_category': '0.15'}
                }
            }
        }
    ]
    other_testitem = [
        {
            'HANDLER': 'OTHER',
            'CONTEXT': {
                'MAIN': {
                    'OTHER': {'setting': 'fake'}
                }
            }
        }
    ]
    flags = ','.join(
        b64encode(json.dumps(each).encode()).decode()
        for each in (other_testitem, pay_testitem)
    )
    return {
        'X-Yandex-ExpFlags': flags,
        'X-Yandex-ExpBoxes': '398290,0,-1;398773,0,-1',
    }


@pytest.fixture
def request_params():
    return {
        'sheet': {
            'version': 2,
            'currency_code': 'USD',
            'country_code': 'ru',
            'merchant': {
                'id': '50fd0b78-0630-4f24-a532-9e1aac5ea859',
                'name': 'merchant-name',
                'url': 'https://url.test',
            },
            'payment_methods': [{
                'type': 'CARD',
                'gateway': 'yandex-trust',
                'gateway_merchant_id': 'gw-id',
                'allowed_auth_methods': ['CLOUD_TOKEN'],
                'allowed_card_networks': ['MASTERCARD'],
            }],
            'order': {
                'id': 'order-id',
                'total': {
                    'amount': '1.00',
                    'label': 'total_label',
                },
            },
        },
    }


@pytest.fixture
def expected_json_body():
    return {
        'status': 'success',
        'data': {
            'cashback': {
                'category': '0.05',
                'amount': '100.00',
            }
        },
        'code': 200,
    }


@pytest.mark.asyncio
async def test_handler_should_return_cashback_limit(
    app,
    api_url,
    request_params,
    mock_user_authentication,
    expected_json_body,
):
    r = await app.post(api_url, json=request_params)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_json_body))


@pytest.mark.asyncio
@pytest.mark.parametrize('card_id', [None, 'fake_card_id'])
async def test_handler_calls_action(
    app,
    api_url,
    request_params,
    mock_get_cashback,
    mock_user_authentication,
    entity_auth_user,
    card_id,
):
    request_params['card_id'] = card_id
    request_params['payment_method_type'] = ClassicPaymentMethodType.CASH.value
    await app.post(
        api_url,
        json=request_params,
        headers={'user-agent': 'agent', 'x-real-ip': 'some_ip'},
        raise_for_status=True,
    )

    request = CashbackRequest(
        sheet=PaymentSheet(
            version=2,
            order=PaymentOrder(
                id='order-id',
                total=PaymentOrderTotal(
                    amount=Decimal('1.00'),
                    label='total_label',
                ),
            ),
            merchant=PaymentMerchant(
                id=UUID('50fd0b78-0630-4f24-a532-9e1aac5ea859'),
                name='merchant-name',
                url='https://url.test',
            ),
            currency_code='USD',
            country_code='ru',
            payment_methods=[
                PaymentMethod(
                    method_type=ClassicPaymentMethodType.CARD,
                    gateway='yandex-trust',
                    gateway_merchant_id='gw-id',
                    allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                    allowed_card_networks=[CardNetwork.MASTERCARD],
                ),
            ],
        ),
        payment_method_type=PaymentMethodType.CASH_ON_DELIVERY,
        card_id=card_id,
    )
    mock_get_cashback.assert_called_once_with(
        cashbackrequest=request,
        user=entity_auth_user,
        user_ip='some_ip',
        user_agent='agent',
    )


@pytest.mark.asyncio
async def test_cashback_category_from_uaas_propagated_to_action(
    app,
    api_url,
    request_params,
    mock_get_cashback,
    mock_user_authentication,
    uaas_headers,
):
    await app.post(
        api_url,
        json=request_params,
        headers=uaas_headers,
    )

    mock_get_cashback.assert_called_once()
    _, kwargs = mock_get_cashback.call_args
    assert_that(kwargs, has_entry('cashback_category_id', '0.15'))


@pytest.mark.asyncio
async def test_request_without_sheets_is_not_allowed(app, api_url, mock_user_authentication):
    r = await app.post(api_url, json={})
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_body['data'],
        equal_to(
            {'params': {'_schema': ['Either `sheet` or `checkout_sheet` should be specified']}, 'message': 'BAD_FORMAT'}
        ),
    )


@pytest.mark.asyncio
async def test_request_with_both_sheets_is_not_allowed(app, api_url, request_params, mock_user_authentication):
    request_params['checkout_sheet'] = {
        'merchant_id': request_params['sheet']['merchant']['id'],
        'cart_total': '100.00',
        'currency_code': request_params['sheet']['currency_code'],
    }
    r = await app.post(api_url, json=request_params)
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_body['data'],
        equal_to(
            {
                'params': {'_schema': ['The presence of both `sheet` and `checkout_sheet` is not allowed']},
                'message': 'BAD_FORMAT',
            }
        ),
    )


@pytest.mark.asyncio
async def test_authentication_performed(app, api_url, request_params, mock_user_authentication):
    r = await app.post(api_url, json=request_params)

    assert_that(r.status, equal_to(200))
    mock_user_authentication.assert_called_once()


@pytest.mark.asyncio
async def test_no_auth(app, api_url, request_params, expected_json_body):
    r = await app.post(api_url, json=request_params)

    assert_that(r.status, equal_to(200))
    assert_that(await r.json(), equal_to(expected_json_body))
