import json
import re
from base64 import b64encode
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_auth import CsrfChecker
from sendr_pytest.matchers import convert_then_match
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay.yandex_pay.conf import settings
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
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
def uid(randn):
    return randn()


@pytest.fixture
def blackbox_url(api_kind, yandex_pay_settings):
    method = 'sessionid' if api_kind == APIKind.WEB else 'oauth'
    return re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method={method}.*')


@pytest.fixture(autouse=True)
def authentication(api_kind, blackbox_url, uid, aioresponses_mocker):
    if api_kind == APIKind.WEB:
        payload = {
            'status': {'value': 'VALID'},
            'uid': {'value': uid},
            'user_ticket': FAKE_USER_TICKET,
            'login_id': 'login_id',
        }
    else:
        payload = {
            'status': {'value': 'VALID'},
            'oauth': {'uid': uid, 'client_id': 'client_id'},
            'user_ticket': FAKE_USER_TICKET,
            'login_id': 'login_id'
        }

    return aioresponses_mocker.get(
        url=blackbox_url, status=200, payload=payload, repeat=True
    )


@pytest.fixture(autouse=True)
def csrf_params(api_kind, app, uid, yandex_pay_settings):
    if api_kind == APIKind.WEB:
        key = app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
        return {
            'headers': {
                yandex_pay_settings.API_CSRF_TOKEN_HEADER: CsrfChecker.generate_token(
                    timestamp=int(utcnow().timestamp()),
                    key=key,
                    user=User(uid, FAKE_USER_TICKET),
                    yandexuid='yandexuid'
                ),
            },
            'cookies': {
                'yandexuid': 'yandexuid',
            }
        }
    return {}


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


@pytest.fixture(autouse=True)
def pay_plus_backend_mock(aioresponses_mocker):
    return aioresponses_mocker.post(
        re.compile(r'^.+/api/v1/cashback$'),
        status=200,
        payload={'data': {'category': '0.05', 'amount': '100.00'}},
    )


@pytest.fixture(autouse=True)
def antifraud_mock(aioresponses_mocker, yandex_pay_settings):
    return aioresponses_mocker.post(
        re.compile(f'{yandex_pay_settings.ANTIFRAUD_API_URL}/score'),
        status=200,
        payload={'status': 'success', 'action': 'ALLOW', 'tags': []},
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
            public_key=rands(),
            public_key_signature='public-key-signature',
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
def request_headers(api_kind, csrf_params, uaas_headers):
    headers = csrf_params.get('headers', {}) | uaas_headers
    if api_kind == APIKind.MOBILE:
        return {'Authorization': 'oauth fake_token'} | headers

    return headers


@pytest.fixture
def request_cookies(api_kind, csrf_params):
    csrf_cookies = csrf_params.get('cookies', {})
    if api_kind == APIKind.WEB:
        return {'Session_id': 'fake_session_id'} | csrf_cookies

    return csrf_cookies


@pytest.mark.asyncio
async def test_handler_should_return_cashback_amount(
    app,
    api_url,
    request_params,
    request_headers,
    request_cookies,
    pay_plus_backend_mock,
    psp,
    merchant_id,
):
    request_params['sheet']['order']['total']['amount'] = '1000'

    r = await app.post(
        api_url, json=request_params, headers=request_headers, cookies=request_cookies
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, has_entries({
        'status': 'success',
        'data': has_entries({
            'cashback': has_entries({
                'category': '0.05',
                'amount': convert_then_match(Decimal, equal_to(Decimal('100.0'))),
            }),
        }),
        'code': 200,
    }))

    pay_plus_backend_mock.assert_called_once()
    _, call_kwargs = pay_plus_backend_mock.call_args
    assert_that(
        call_kwargs,
        has_entries(
            json=dict(
                currency='RUB',
                amount='1000',
                psp_id=str(psp.psp_id),
                merchant={
                    'id': str(merchant_id),
                    'name': 'merchant-name',
                    'url': None,
                },
                trust_card_id=None,
                cashback_category_id='0.15',
            ),
            headers=has_entries(
                {'x-ya-user-ticket': FAKE_USER_TICKET}
            )
        )
    )


@pytest.mark.asyncio
async def test_no_cashback_for_cash_payment(
    app,
    api_url,
    request_params,
    request_headers,
    request_cookies,
    pay_plus_backend_mock,
):
    request_params['sheet']['payment_methods'] = request_params['sheet']['payment_methods'][1:]

    r = await app.post(api_url, json=request_params, headers=request_headers, cookies=request_cookies)
    json_body = await r.json()

    pay_plus_backend_mock.assert_not_called()
    assert_that(r.status, equal_to(400))
    assert_that(json_body, has_entries({
        'data': has_entries({
            'message': 'CARD_PAYMENT_METHOD_IS_MISSING'
        }),
    }))


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'card_id,should_call_trust', [(None, False), ('card-x123abc', True)]
)
async def test_get_cashback_with_card_id(
    app,
    api_url,
    request_params,
    request_headers,
    request_cookies,
    card_id,
    should_call_trust,
    aioresponses_mocker,
    yandex_pay_settings,
):
    mock_trust = aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
        status=200,
        payload={
            'bound_payment_methods': [
                {
                    'id': card_id,
                    'card_id': card_id,
                    'binding_systems': ['trust'],
                    'orig_uid': '',
                    'payment_method': 'card',
                    'system': 'MasterCard',
                    'payment_system': 'MasterCard',
                    'expiration_month': '12',
                    'expiration_year': '3099',
                    'card_bank': 'SBERBANK OF RUSSIA',
                    'expired': False,
                    'account': '123456****7890',
                    'last_paid_ts': utcnow().timestamp(),
                    'binding_ts': utcnow().timestamp(),
                }
            ]
        },
    )
    request_params['card_id'] = card_id

    r = await app.post(
        api_url, json=request_params, headers=request_headers, cookies=request_cookies
    )

    assert_that(r.status, equal_to(200))
    if should_call_trust:
        mock_trust.assert_called_once()
    else:
        mock_trust.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize('currency', settings.ALLOWED_CURRENCIES)
async def test_allowed_currencies(
    app,
    api_url,
    request_params,
    request_headers,
    request_cookies,
    currency,
):
    request_params['sheet']['currency_code'] = currency

    r = await app.post(
        api_url, json=request_params, headers=request_headers, cookies=request_cookies
    )

    assert_that(r.status, equal_to(200))


@pytest.mark.asyncio
@pytest.mark.parametrize('currency', ['XTS'])
async def test_other_currencies_not_allowed(
    app,
    api_url,
    request_params,
    request_headers,
    request_cookies,
    currency,
):
    request_params['sheet']['currency_code'] = currency

    r = await app.post(
        api_url, json=request_params, headers=request_headers, cookies=request_cookies
    )

    expected_error = {
        'code': 400, 'status': 'fail', 'data': {'message': 'INVALID_CURRENCY'}
    }

    assert_that(r.status, equal_to(400))
    assert_that(await r.json(), equal_to(expected_error))
