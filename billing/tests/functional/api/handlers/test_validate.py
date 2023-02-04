import re
import uuid

import pytest

from sendr_auth import CsrfChecker
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.conf import settings
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind

TSP_TOKEN_ID = 'gra-pes'
TOKEN_MOCK = 'le-mo-na-de'


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/validate',
        APIKind.MOBILE: '/api/mobile/v1/validate',
    }[api_kind]


@pytest.fixture
def authentication(api_kind, app, yandex_pay_settings, owner_uid, aioresponses_mocker):
    if api_kind == APIKind.WEB:
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=sessionid.*'),
            status=200,
            payload={'status': {'value': 'VALID'}, 'uid': {'value': owner_uid}, 'login_id': 'login_id'}
        )

        key = app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
        return {
            'headers': {
                yandex_pay_settings.API_CSRF_TOKEN_HEADER: CsrfChecker.generate_token(
                    timestamp=int(utcnow().timestamp()),
                    key=key,
                    user=User(owner_uid),
                    yandexuid='yandexuid'
                ),
            },
            'cookies': {
                'Session_id': 'sessionid',
                'yandexuid': 'yandexuid',
            },
        }

    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=oauth.*'),
        status=200,
        payload={
            'status': {'value': 'VALID'},
            'oauth': {'uid': owner_uid, 'client_id': 'client_id'},
            'login_id': 'login_id',
        }
    )
    return {
        'headers': {
            'Authorization': 'OAuth 123',
        }
    }


@pytest.fixture
def owner_uid(randn):
    return randn()


@pytest.fixture
def merchant_id():
    return str(uuid.uuid4())


@pytest.fixture
def psp_external_id(rands):
    return rands()


@pytest.fixture(autouse=True)
async def merchant(storage, rands, merchant_id):
    return await storage.merchant.create(
        Merchant(
            merchant_id=uuid.UUID(merchant_id),
            name=rands(),
        )
    )


@pytest.fixture(autouse=True)
async def merchant_origin(storage, merchant: Merchant):
    return await storage.merchant_origin.create(MerchantOrigin(
        merchant_id=merchant.merchant_id,
        origin='https://rotten-fruits.gov:443',
    ))


@pytest.fixture(autouse=True)
async def psp(storage, psp_external_id):
    return await create_psp_entity(
        storage,
        PSP(
            psp_id=uuid.uuid4(),
            psp_external_id=psp_external_id,
            public_key='public-key',
            public_key_signature='public-key-signature',
        )
    )


@pytest.fixture(params=['RUB'])
def currency(request):
    return request.param


@pytest.fixture
def payment_sheet(merchant_id, psp_external_id, currency):
    return {
        'version': 2,
        'currency_code': currency,
        'country_code': 'ru',
        'merchant': {
            'id': merchant_id,
            'name': 'merchant-name',
        },
        'order': {
            'id': 'order-id',
            'total': {
                'amount': '1.00',
            },
        },
        'payment_methods': [
            {
                'type': 'CARD',
                'gateway': psp_external_id,
                'gateway_merchant_id': 'hmnid',
                'allowed_auth_methods': ['CLOUD_TOKEN'],
                'allowed_card_networks': ['MASTERCARD'],
            },
            {'type': 'CASH'},
            {'type': 'SPLIT'},
        ],
    }


@pytest.mark.asyncio
async def test_unauthorized(
    app,
    api_url,
    api_kind,
    yandex_pay_settings,
    aioresponses_mocker,
    payment_sheet,
):
    params = {
        'sheet': payment_sheet,
    }
    if api_kind == APIKind.MOBILE:
        params['merchant_origin'] = 'https://rotten-fruits.gov'

    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*'),
        status=200,
        payload={'status': {'value': 'INVALID', 'id': 5}, 'error': 'signature has bad format or is broken'},
    )

    r = await app.post(
        api_url,
        json=params,
        cookies={'Session_id': 'sessionid'},
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(
        json_body,
        equal_to({
            'code': 401,
            'status': 'fail',
            'data': {
                'message': 'ACCESS_DENIED',
            }
        })
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('currency', settings.ALLOWED_CURRENCIES, indirect=True)
async def test_normal_response(
    app,
    api_kind,
    api_url,
    payment_sheet,
    owner_uid,
    authentication,
):
    params = {
        'sheet': payment_sheet,
    }
    if api_kind == APIKind.MOBILE:
        params['merchant_origin'] = 'https://rotten-fruits.gov'

    response = await app.post(
        api_url,
        json=params,
        **authentication,
    )
    json_body = await response.json()

    assert_that(response.status, equal_to(200))
    assert_that(json_body, equal_to({'code': 200, 'data': {}, 'status': 'success'}))


@pytest.mark.asyncio
async def test_zero_amount_is_valid_when_deferred(
    app,
    api_kind,
    api_url,
    payment_sheet,
    owner_uid,
    authentication,
):
    params = {
        'sheet': payment_sheet,
    }
    params['sheet']['order']['total']['amount'] = '0.00'
    params['sheet']['recurring_options'] = {
        'type': 'DEFERRED',
    }
    if api_kind == APIKind.MOBILE:
        params['merchant_origin'] = 'https://rotten-fruits.gov'

    response = await app.post(
        api_url,
        json=params,
        **authentication,
    )
    json_body = await response.json()

    assert_that(response.status, equal_to(200))
    assert_that(json_body, equal_to({'code': 200, 'data': {}, 'status': 'success'}))
