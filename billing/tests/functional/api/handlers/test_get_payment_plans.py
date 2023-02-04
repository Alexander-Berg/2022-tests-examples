import re
from datetime import datetime, timezone
from uuid import uuid4

import pytest

from sendr_auth import CsrfChecker
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, match_equality

from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind
from billing.yandex_pay.yandex_pay.tests.matchers import convert_then_match

FAKE_USER_TICKET = 'fake_tvm_user_ticket'


def match_datetime(dt):
    return match_equality(convert_then_match(datetime.fromisoformat, dt))


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/split/get-plans',
        APIKind.MOBILE: '/api/mobile/v1/split/get-plans',
    }[api_kind]


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def login_id(rands):
    return rands()


@pytest.fixture
def user(uid, login_id):
    return User(uid, None, login_id)


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
def csrf_params(api_kind, app, user, yandex_pay_settings):
    if api_kind == APIKind.WEB:
        key = app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
        return {
            'headers': {
                yandex_pay_settings.API_CSRF_TOKEN_HEADER: CsrfChecker.generate_token(
                    timestamp=int(utcnow().timestamp()),
                    key=key,
                    user=user,
                    yandexuid='yandexuid'
                ),
            },
            'cookies': {
                'yandexuid': 'yandexuid',
            }
        }
    return {}


@pytest.fixture
def request_headers(api_kind, csrf_params):
    headers = csrf_params.get('headers', {})
    if api_kind == APIKind.MOBILE:
        return {'Authorization': 'oauth fake_token'} | headers

    return headers


@pytest.fixture
def request_cookies(api_kind, csrf_params):
    csrf_cookies = csrf_params.get('cookies', {})
    if api_kind == APIKind.WEB:
        return {'Session_id': 'fake_session_id'} | csrf_cookies

    return csrf_cookies


@pytest.fixture
def split_response():
    return {
        "plans": [
            {
                "class_name": "regular_instalment_plan",
                "constructor": "test",
                "status": "draft",
                "details": {
                    "deposit": "0.00",
                    "payments": [
                        {
                            "status": "coming",
                            "datetime": "2021-12-09T09:27:45Z",
                            "amount": "1026.00"
                        },
                        {
                            "status": "coming",
                            "datetime": "2021-12-09T09:37:45Z",
                            "amount": "1026.00"
                        }
                    ]
                },
                "user_id": "dd9b8d73-f2db-593d-5722-2d6edbdb42ba",
                "sum": "2052.00",
                "id": "c3b60686-b791-65d9-c069-817c20bcde9d"
            },
            {
                "class_name": "regular_instalment_plan",
                "constructor": "fast_loan_transfer_sums",
                "status": "draft",
                "details": {
                    "deposit": "1000.00",
                    "payments": [
                        {
                            "status": "coming",
                            "datetime": "2021-12-09T09:27:45Z",
                            "amount": "526.00"
                        },
                        {
                            "status": "coming",
                            "datetime": "2021-12-09T09:28:45Z",
                            "amount": "526.00"
                        },
                    ]
                },
                "user_id": "dd9b8d73-f2db-593d-5722-2d6edbdb42ba",
                "sum": "2052.00",
                "id": "898330cd-d091-4387-e035-65bb43db4762"
            }
        ]
    }


@pytest.fixture
def url(yandex_pay_settings):
    return re.compile(f'^{yandex_pay_settings.SPLIT_API_URL}/plan/check$')


@pytest.fixture(autouse=True)
def mock_split(aioresponses_mocker, url, split_response):
    return aioresponses_mocker.post(
        url=url,
        status=200,
        payload=split_response,
    )


@pytest.fixture(autouse=True)
async def merchant(storage, rands):
    return await storage.merchant.create(
        Merchant(
            merchant_id=uuid4(),
            name=rands(),
        )
    )


@pytest.fixture(autouse=True)
async def merchant_origin(storage, merchant: Merchant):
    return await storage.merchant_origin.create(MerchantOrigin(
        merchant_id=merchant.merchant_id,
        origin='https://rotten-fruits.gov:443',
    ))


@pytest.fixture
def request_params(merchant: Merchant):
    return {
        'sheet': {
            'version': 2,
            'currency_code': 'rub',
            'country_code': 'ru',
            'merchant': {
                'id': str(merchant.merchant_id),
                'name': 'merchant-name',
                'url': 'https://url.test',
            },
            'payment_methods': [{'type': 'SPLIT'}],
            'order': {
                'id': 'order-id',
                'total': {
                    'amount': '2052.00',
                    'label': 'total_label',
                },
            },
        },
    }


@pytest.fixture
def expected_response():
    return {
        "data": {
            "plans": [
                {
                    "sum": "2052.00",
                    "id": "c3b60686-b791-65d9-c069-817c20bcde9d",
                    "payments": [
                        {
                            "status": "coming",
                            "datetime": match_datetime(datetime(2021, 12, 9, 9, 27, 45, tzinfo=timezone.utc)),
                            "amount": "1026.00",
                        },
                        {
                            "status": "coming",
                            "datetime": match_datetime(datetime(2021, 12, 9, 9, 37, 45, tzinfo=timezone.utc)),
                            "amount": "1026.00",
                        }
                    ]
                }
            ]
        },
        "code": 200,
        "status": "success",
    }


@pytest.mark.asyncio
async def test_returned(
    app, api_url, request_params, request_headers, request_cookies, expected_response
):
    r = await app.post(
        api_url, json=request_params, headers=request_headers, cookies=request_cookies
    )

    assert_that(r.status, equal_to(200))
    assert_that(await r.json(), equal_to(expected_response))
