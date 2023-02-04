import json
from base64 import b64encode
from decimal import Decimal
from uuid import UUID

import pytest
from pay.lib.entities.payment_sheet import PaymentOrder, PaymentOrderTotal

from hamcrest import assert_that, equal_to, has_entry

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.cashback import GetCashbackAction
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import PaymentMerchant, PaymentMethod, PaymentSheet
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind

FAKE_USER_TICKET = 'fake_tvm_user_ticket'


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/cashback',
        APIKind.MOBILE: '/api/mobile/v1/cashback',
    }[api_kind]


@pytest.fixture
def session_info_uid(randn):
    return randn()


@pytest.fixture
def fake_user(session_info_uid):
    return User(session_info_uid, FAKE_USER_TICKET)


@pytest.fixture
def mock_authentication(mocker, fake_user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=fake_user))


@pytest.fixture(autouse=True)
def mock_get_cashback(mock_action):
    return mock_action(
        GetCashbackAction,
        {'category': Decimal('0.05'), 'amount': Decimal('100.00')}
    )


@pytest.fixture
def uaas_headers(yandex_pay_settings):
    pay_testitem = [
        {
            'HANDLER': yandex_pay_settings.API_UAAS_HANDLER,
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
    mock_authentication,
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
    mock_authentication,
    fake_user,
    card_id,
):
    request_params['card_id'] = card_id
    request_params['payment_method_type'] = PaymentMethodType.CASH.value
    await app.post(
        api_url,
        json=request_params,
        headers={'user-agent': 'agent', 'x-real-ip': 'some_ip'},
    )

    mock_get_cashback.assert_called_once_with(
        user=fake_user,
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
                    method_type=PaymentMethodType.CARD,
                    gateway='yandex-trust',
                    gateway_merchant_id='gw-id',
                    allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                    allowed_card_networks=[CardNetwork.MASTERCARD],
                ),
            ],
        ),
        payment_method_type=PaymentMethodType.CASH,
        card_id=card_id,
        user_ip='some_ip',
        user_agent='agent',
        cashback_category_id=None,
    )


@pytest.mark.asyncio
async def test_cashback_category_from_uaas_propagated_to_action(
    app,
    api_url,
    request_params,
    mock_get_cashback,
    mock_authentication,
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
async def test_authentication_performed(app, api_url, request_params, mock_authentication):
    r = await app.post(api_url, json=request_params)

    assert_that(r.status, equal_to(200))
    mock_authentication.assert_called_once()


@pytest.mark.asyncio
async def test_no_auth(app, api_url, request_params, expected_json_body):
    r = await app.post(api_url, json=request_params)

    assert_that(r.status, equal_to(200))
    assert_that(await r.json(), equal_to(expected_json_body))
