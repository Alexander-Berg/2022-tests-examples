import re
import uuid

import pytest

from sendr_auth import CsrfChecker
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.file_storage.geobase import GeobaseStorage
from billing.yandex_pay.yandex_pay.interactions import TrustPaymentsClient
from billing.yandex_pay.yandex_pay.interactions.trust_payments import TrustPaymentMethod
from billing.yandex_pay.yandex_pay.tests.entities import APIKind

TRUST_CARD_ID = 'card-x1a1234567a12abcd12345a1a'


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/is_ready_to_pay',
        APIKind.MOBILE: '/api/mobile/v1/is_ready_to_pay',
    }[api_kind]


@pytest.fixture
def set_req_authentication(api_kind, app, yandex_pay_settings, owner_uid, aioresponses_mocker):
    def set_web(req, uid=None):
        uid = uid if uid is not None else owner_uid
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=sessionid.*'),
            status=200,
            payload={'status': {'value': 'VALID'}, 'uid': {'value': uid}, 'login_id': 'login_id'}
        )
        req.cookies['Session_id'] = 'sessionid'
        return req

    def set_oauth(req, uid=None):
        uid = uid if uid is not None else owner_uid
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=oauth.*'),
            status=200,
            payload={
                'status': {'value': 'VALID'},
                'oauth': {'uid': uid, 'client_id': 'client_id'},
                'login_id': 'login_id',
            }
        )
        req.headers['Authorization'] = 'OAuth 123'
        return req

    if api_kind == APIKind.WEB:
        return set_web

    return set_oauth


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


@pytest.fixture
def trust_payment_method(owner_uid):
    return TrustPaymentMethod(
        id=TRUST_CARD_ID,
        card_id=TRUST_CARD_ID,
        binding_systems=['trust'],
        orig_uid=str(owner_uid),
        payment_method='card',
        system='MasterCard',
        payment_system='MasterCard',
        expiration_month='9',
        expiration_year='2099',
        card_bank='SBERBANK OF RUSSIA',
        expired=False,
        account='1111****1234',
        last_paid_ts=utcnow(),
        binding_ts=utcnow(),
    )


@pytest.fixture(autouse=True)
def mock_trust_gateway_lpm(mocker, trust_payment_method):
    return mocker.patch.object(
        TrustPaymentsClient,
        'get_payment_methods',
        mocker.AsyncMock(
            return_value=[trust_payment_method],
        ),
    )


@pytest.fixture
def request_json(merchant_id, psp_external_id):
    return {
        'merchant_id': merchant_id,
        'merchant_origin': 'https://rotten-fruits.gov',
        'existing_payment_method_required': True,
        'payment_methods': [{
            'type': 'CARD',
            'gateway': psp_external_id,
            'gateway_merchant_id': 'hmnid',
            'allowed_auth_methods': ['CLOUD_TOKEN'],
            'allowed_card_networks': ['MASTERCARD'],
        }]
    }


@pytest.fixture
def set_request_csrf(api_kind, yandexpay_app, yandex_pay_settings, owner_uid, aioresponses_mocker):
    """
    Функция проставит корректный CSRF токен на запросе.
    """
    def set_(req):
        if api_kind == APIKind.WEB:
            key = yandexpay_app.file_storage.csrf_anti_forgery_key.get_actual_key()
            token = CsrfChecker.generate_token(
                timestamp=int(utcnow().timestamp()),
                key=key,
                user=User(owner_uid),
                yandexuid=req.cookies.setdefault('yandexuid', 'yandexuid'),
            )

            req.headers[yandex_pay_settings.API_CSRF_TOKEN_HEADER] = token

        return req

    return set_


@pytest.fixture(autouse=True)
def req(app, api_url, yandex_pay_settings, request_json, request_maker):
    req = request_maker(
        client=app,
        method='post',
        path=api_url,
        json=request_json,
        headers={'User-Agent': ''},
        cookies={'Session_id': 'sessionid'},
    )
    return req


@pytest.mark.asyncio
@pytest.mark.parametrize('existing_payment_method_required, expected_readiness', (
    (True, False),
    (False, True),
))
async def test_unauthorized(
    app,
    yandex_pay_settings,
    aioresponses_mocker,
    existing_payment_method_required,
    expected_readiness,
    req,
):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*'),
        status=200,
        payload={'status': {'value': 'INVALID', 'id': 5}, 'error': 'signature has bad format or is broken'},
    )

    req.json['existing_payment_method_required'] = existing_payment_method_required

    r, json_body = await req.make()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to({
        'code': 200,
        'status': 'success',
        'data': {
            'is_ready_to_pay': expected_readiness,
        }
    }))


@pytest.mark.parametrize('api_kind', [APIKind.WEB])
@pytest.mark.asyncio
async def test_csrf_validation_failed__when_authorized(
    app, owner_uid, aioresponses_mocker, yandex_pay_settings, req
):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*'),
        status=200,
        payload={'status': {'value': 'VALID'}, 'uid': {'value': owner_uid}, 'login_id': 'login_id'}
    )

    req.headers[yandex_pay_settings.API_CSRF_TOKEN_HEADER] = 'ooops!'

    response, json_body = await req.make()

    assert_that(response.status, equal_to(403))
    assert_that(json_body, equal_to({'code': 403, 'data': {'message': 'INVALID_TIMESTAMP'}, 'status': 'fail'}))


class TestForPanOnlyCard:
    IOS_USER_AGENT = (
        'Mozilla/5.0 (iPad; CPU OS 10_2 like Mac OS X) '
        'AppleWebKit/602.3.12 (KHTML, like Gecko) Mobile/14C92'
    )

    ANDROID_YANDEX_USER_AGENT = (
        'Mozilla/5.0 (Linux; Android 9; LLD-L31) AppleWebKit/537.36 (KHTML, like Gecko) '
        'Chrome/72.0.3626.119 YaBrowser/19.3.4.333.00 Mobile Safari/537.36'
    )

    @pytest.fixture
    def trust_payment_method(self, owner_uid):
        return TrustPaymentMethod(
            id=TRUST_CARD_ID,
            card_id=TRUST_CARD_ID,
            binding_systems=['trust'],
            orig_uid=str(owner_uid),
            payment_method='card',
            system='MasterCard',
            payment_system='MasterCard',
            expiration_month='9',
            expiration_year='2099',
            card_bank='SBERBANK OF RUSSIA',
            expired=False,
            account='1111****1234',
            last_paid_ts=utcnow(),
            binding_ts=utcnow(),
        )

    @pytest.fixture(autouse=True)
    def mock_trust_gateway_lpm(self, mocker, trust_payment_method):
        return mocker.patch.object(
            TrustPaymentsClient,
            'get_payment_methods',
            mocker.AsyncMock(
                return_value=[trust_payment_method],
            ),
        )

    @pytest.fixture
    def request_json(self, api_kind, merchant_id, psp_external_id):
        return {
            'merchant_id': merchant_id,
            'merchant_origin': 'https://rotten-fruits.gov' if api_kind == APIKind.WEB else None,
            'existing_payment_method_required': True,
            'payment_methods': [{
                'type': 'CARD',
                'gateway': psp_external_id,
                'gateway_merchant_id': 'hmnid',
                'allowed_auth_methods': ['CLOUD_TOKEN'],
                'allowed_card_networks': ['MASTERCARD'],
            }]
        }

    @pytest.fixture(autouse=True)
    def req(self, app, api_url, set_req_authentication, set_request_csrf, yandex_pay_settings, request_json,
            request_maker):
        return set_request_csrf(set_req_authentication(request_maker(
            client=app,
            method='post',
            path=api_url,
            json=request_json,
        )))

    @pytest.mark.asyncio
    async def test_is_not_ready_to_pay_if_payment_methods_are_checked(
        self, req
    ):
        response, json = await req.make()

        assert_that(response.status, equal_to(200))
        assert_that(json, equal_to({
            'code': 200,
            'status': 'success',
            'data': {
                'is_ready_to_pay': False,
            }
        }))

    @pytest.mark.asyncio
    async def test_is_not_ready_to_pay_if_region_is_forbidden(
        self, app, yandex_pay_settings, req
    ):
        yandex_pay_settings.API_CHECK_REGION = True
        region_id = 4030
        app.server.app.file_storage.geobase = GeobaseStorage(forbidden_regions=[{'region_id': region_id}])
        req.headers.update({'x-region-id': str(region_id), 'x-region-suspected': '1'})

        req.json['payment_methods'][0]['allowed_auth_methods'] = ['PAN_ONLY']

        response, json = await req.make()

        assert_that(response.status, equal_to(200))
        assert_that(json, equal_to({
            'code': 200,
            'status': 'success',
            'data': {
                'is_ready_to_pay': False,
            }
        }))

    @pytest.mark.parametrize('api_kind', [APIKind.WEB])
    @pytest.mark.parametrize(
        ('user_agent_header', 'is_ready_to_pay'),
        (
            (IOS_USER_AGENT, True),
            # оно работает c Android / YandexBrowser
            (ANDROID_YANDEX_USER_AGENT, True),
        )
    )
    @pytest.mark.asyncio
    async def test_is_ready_to_pay_for_different_user_agents(
        self, req, is_ready_to_pay, user_agent_header, api_kind
    ):
        """
        Один тест, что .
        """

        req.json['existing_payment_method_required'] = False
        req.json['payment_methods'] = None
        req.headers['User-Agent'] = user_agent_header

        response, json = await req.make()

        assert_that(response.status, equal_to(200))
        assert_that(json, equal_to({
            'code': 200,
            'status': 'success',
            'data': {
                'is_ready_to_pay': is_ready_to_pay,
            }
        }))

    @pytest.mark.asyncio
    async def test_normal_response_when_payment_methods_not_required(
        self, req, api_kind
    ):
        req.json['existing_payment_method_required'] = False
        req.json['payment_methods'] = None

        if api_kind == APIKind.WEB:
            req.headers['User-Agent'] = ''

        response, json = await req.make()

        assert_that(response.status, equal_to(200))
        assert_that(json, equal_to({
            'code': 200,
            'status': 'success',
            'data': {
                'is_ready_to_pay': True,
            }
        }))
