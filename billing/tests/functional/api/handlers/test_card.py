import re
import uuid
from copy import deepcopy
from datetime import datetime, timedelta, timezone

import pytest

from sendr_auth import CsrfChecker
from sendr_pytest.matchers import convert_then_match
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_properties, instance_of

from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.file_storage.geobase import GeobaseStorage
from billing.yandex_pay.yandex_pay.tests.entities import APIKind


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/user_cards',
        APIKind.MOBILE: '/api/mobile/v1/user_cards',
    }[api_kind]


@pytest.fixture
def authentication(api_kind, yandex_pay_settings, owner_uid, aioresponses_mocker):
    if api_kind == APIKind.WEB:
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=sessionid.*'),
            status=200,
            payload={'status': {'value': 'VALID'}, 'uid': {'value': owner_uid}, 'login_id': 'login_id'}
        )

        return {
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
async def merchant(storage, merchant_entity):
    return await storage.merchant.create(merchant_entity)


@pytest.fixture
def card_expiration_date():
    return utcnow() + timedelta(days=365)


@pytest.fixture
async def cards(storage, merchant, rands, owner_uid, card_expiration_date):
    cards = []
    for merchant_id in [None, merchant.merchant_id]:
        card = await storage.card.create(Card(
            trust_card_id=rands(),
            owner_uid=owner_uid,
            tsp=TSPType.MASTERCARD,
            expire=card_expiration_date,
            last4='1234',
            issuer_bank='alfa-bank',
        ))
        await storage.enrollment.create(Enrollment(
            card_id=card.card_id,
            merchant_id=merchant_id,
            tsp_card_id=f'tsp-card-id{rands()}',
            tsp_token_id=f'tsp-token-id{rands()}',
            tsp_token_status=TSPTokenStatus.ACTIVE,
            expire=card_expiration_date,
            card_last4=card.last4,
        ))
        cards.append(card)
    card_without_enrollment = await storage.card.create(Card(
        trust_card_id=rands(),
        owner_uid=owner_uid,
        tsp=TSPType.UNKNOWN,
        expire=card_expiration_date,
        last4='1234',
    ))
    cards.append(card_without_enrollment)
    return cards


@pytest.fixture
def trust_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
def payment_method_data(owner_uid, trust_id):
    return {
        "card_id": trust_id,
        "orig_uid": str(owner_uid),
        "region_id": 225,
        "payment_method": "card",
        "system": "MasterCard",
        "expiration_month": "01",
        "card_country": "RUS",
        "binding_ts": "1586458392.247",
        "ebin_tags_version": 0,
        "card_level": "STANDARD",
        "holder": "Card Holder",
        "last_paid_ts": "1586458392.247",
        "id": "card-a1a1234567a12abcd12345a1a",
        "payment_system": "Maestro",
        "account": "123456****7890",
        "ebin_tags": [],
        "expiration_year": "2030",
        "aliases": [
            "card-xa1a1234567a12abcd12345a1a"
        ],
        "expired": False,
        "card_bank": "SBERBANK OF RUSSIA",
        "recommended_verification_type": "standard2",
        "binding_systems": [
            "trust"
        ]
    }


@pytest.fixture
def payment_methods_response_data(payment_method_data):
    response_data = {
        "status": "success",
        "bound_payment_methods": [
            payment_method_data,
        ],
    }
    return response_data


@pytest.fixture
def expected_json_body(cards, owner_uid, payment_method_data, yandex_pay_settings, card_expiration_date):
    return {
        'code': 200,
        'status': 'success',
        'data': {
            'cards': [
                {
                    'card_network': 'MAESTRO',
                    'last4': '7890',
                    'bin': '123456',
                    'card_art': {},
                    'issuer_bank': 'SBERBANK',
                    'id': str(payment_method_data['card_id']),
                    'uid': owner_uid,
                    'allowed_auth_methods': ['PAN_ONLY'],
                    'expiration_date': {
                        'month': 1,
                        'year': 2030,
                    },
                    'trust_card_id': payment_method_data['card_id'],
                },
                {
                    'card_network': 'MASTERCARD',
                    'last4': '1234',
                    'bin': None,
                    'card_art': {},
                    'issuer_bank': 'ALFABANK',
                    'id': str(cards[0].card_id),
                    'uid': owner_uid,
                    'allowed_auth_methods': ['CLOUD_TOKEN'],
                    'expiration_date': {
                        'month': card_expiration_date.month,
                        'year': card_expiration_date.year,
                    },
                    'trust_card_id': cards[0].trust_card_id,
                },
            ]
        }
    }


@pytest.fixture
def expected_pan_only_json_body(expected_json_body):
    pan_only_body = deepcopy(expected_json_body)
    pan_only_body['data']['cards'].pop(1)
    return pan_only_body


@pytest.fixture
def turn_off_tokens(yandex_pay_settings):
    yandex_pay_settings.API_TOKENS_ENABLED = False


class TestUserCards:
    @pytest.mark.asyncio
    async def test_should_respond_user_cards(
        self,
        app,
        api_url,
        authentication,
        yandex_pay_settings,
        aioresponses_mocker,
        expected_json_body,
        payment_methods_response_data,
    ):
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=payment_methods_response_data,
        )

        r = await app.get(api_url, **authentication)
        json_body = await r.json()
        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_json_body))

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('turn_off_tokens')
    async def test_should_not_respond_card_with_cloud_token_only_if_tokens_are_disabled(
        self,
        app,
        api_url,
        authentication,
        yandex_pay_settings,
        aioresponses_mocker,
        expected_pan_only_json_body,
        payment_methods_response_data,
    ):
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=payment_methods_response_data,
        )

        r = await app.get(api_url, **authentication)
        json_body = await r.json()
        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_pan_only_json_body))

    @pytest.mark.asyncio
    async def test_unauthorized(self, app, api_url, yandex_pay_settings, aioresponses_mocker):
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*'),
            status=200,
            payload={'status': {'value': 'INVALID', 'id': 5}, 'error': 'signature has bad format or is broken'},
        )
        r = await app.get(api_url, headers={'User-Agent': ''})
        json_body = await r.json()
        assert_that(r.status, equal_to(401))
        assert_that(
            json_body,
            equal_to({
                'code': 401,
                'status': 'fail',
                'data': {
                    'message': 'MISSING_CREDENTIALS',
                }
            })
        )

    @pytest.mark.asyncio
    async def test_restricted_region(
        self,
        app,
        api_url,
        authentication,
        yandex_pay_settings,
        aioresponses_mocker,
        expected_json_body,
        cards,
        owner_uid,
        payment_methods_response_data,
    ):
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=payment_methods_response_data,
        )

        yandex_pay_settings.API_CHECK_REGION = True

        region_id = 4030
        app.server.app.file_storage.geobase = GeobaseStorage(forbidden_regions=[{'region_id': region_id}])
        authentication.setdefault('headers', {}).update({'x-region-id': str(region_id), 'x-region-suspected': '1'})

        for card in expected_json_body['data']['cards']:
            card['allowed_auth_methods'].clear()

        r = await app.get(api_url, **authentication)
        json_body = await r.json()
        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_json_body))


class TestCardIsBothInTrustAndYandexPay:
    @pytest.fixture
    def trust_id(self, owner_uid, cards: list[Card]):
        return cards[0].trust_card_id

    @pytest.fixture
    def expected_intersection(self, cards, owner_uid, payment_method_data):
        return {
            'code': 200,
            'status': 'success',
            'data': {
                'cards': [
                    {
                        'card_network': 'MAESTRO',
                        'last4': '1234',
                        'bin': '123456',
                        'card_art': {},
                        'issuer_bank': 'ALFABANK',
                        'id': str(cards[0].card_id),
                        'uid': owner_uid,
                        'allowed_auth_methods': ['CLOUD_TOKEN', 'PAN_ONLY'],
                        'expiration_date': {
                            'month': 1,
                            'year': 2030,
                        },
                        'trust_card_id': cards[0].trust_card_id,
                    },
                ]
            }
        }

    @pytest.mark.asyncio
    async def test_should_respond_user_cards(
        self,
        app,
        api_url,
        authentication,
        yandex_pay_settings,
        aioresponses_mocker,
        expected_intersection,
        payment_methods_response_data,
    ):
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=payment_methods_response_data,
        )

        r = await app.get(api_url, **authentication)
        json_body = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_intersection))

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('turn_off_tokens')
    async def test_should_not_read_and_merge_pay_card_if_tokens_are_disabled(
        self,
        app,
        api_url,
        authentication,
        yandex_pay_settings,
        aioresponses_mocker,
        expected_pan_only_json_body,
        payment_methods_response_data,
    ):
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=payment_methods_response_data,
        )

        r = await app.get(api_url, **authentication)
        json_body = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_pan_only_json_body))


class TestSyncUserCard:
    @pytest.fixture
    def csrf_params(self, api_kind, app, owner_uid, yandex_pay_settings):
        if api_kind == APIKind.WEB:
            key = app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
            return {
                'headers': {
                    yandex_pay_settings.API_CSRF_TOKEN_HEADER: CsrfChecker.generate_token(
                        timestamp=int(utcnow().timestamp()),
                        key=key,
                        user=User(owner_uid),
                        yandexuid='yandexuid',
                    ),
                },
                'cookies': {
                    'yandexuid': 'yandexuid',
                }
            }
        return {}

    @pytest.fixture
    def request_auth(self, authentication, csrf_params):
        headers = authentication.get('headers', {}) | csrf_params.get('headers', {})
        cookies = authentication.get('cookies', {}) | csrf_params.get('cookies', {})
        return {'headers': headers, 'cookies': cookies}

    @pytest.fixture
    def api_url(self, api_kind):
        return {
            APIKind.WEB: '/api/v1/sync_user_card',
            APIKind.MOBILE: '/api/mobile/v1/sync_user_card',
        }[api_kind]

    @pytest.mark.asyncio
    async def test_unauthorized(self, app, api_url, yandex_pay_settings, aioresponses_mocker, trust_id):
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*'),
            status=200,
            payload={'status': {'value': 'INVALID', 'id': 5}, 'error': 'signature has bad format or is broken'},
        )
        r = await app.post(api_url, json={'card_id': trust_id}, headers={'User-Agent': ''})
        json_body = await r.json()
        assert_that(r.status, equal_to(401))
        assert_that(
            json_body,
            equal_to({
                'code': 401,
                'status': 'fail',
                'data': {
                    'message': 'MISSING_CREDENTIALS',
                }
            })
        )

    @pytest.mark.asyncio
    async def test_returns_card_state(
        self, app, api_url, aioresponses_mocker, yandex_pay_settings, trust_id, request_auth
    ):
        r = await app.post(
            api_url,
            json={'card_id': trust_id},
            **request_auth,
        )
        json_body = await r.json()

        assert_that(
            json_body,
            has_entries({
                'status': 'success',
                'code': 200,
                'data': has_entries({
                    'id': convert_then_match(
                        uuid.UUID, instance_of(uuid.UUID)
                    ),
                    'is_expired': False,
                    'is_removed': False,
                    'last4': '0000',
                })
            })
        )

    @pytest.mark.asyncio
    async def test_creates_card(
        self, app, api_url, aioresponses_mocker, yandex_pay_settings, storage, owner_uid, trust_id, request_auth
    ):
        await app.post(
            api_url,
            json={'card_id': trust_id},
            **request_auth,
        )

        card = await storage.card.get_by_trust_card_id_and_uid(trust_card_id=trust_id, owner_uid=owner_uid)
        assert_that(
            card,
            has_properties({
                'is_removed': False,
                'last4': '0000',
                'tsp': TSPType.MASTERCARD,
                'expire': datetime(2050, 1, 31, 0, 0, 0, tzinfo=timezone.utc),  # ага, надо поправить вычисление expire
                'issuer_bank': 'sbrbanku',
            })
        )

    @pytest.fixture(autouse=True)
    def mock_trust_bindings(self, aioresponses_mocker, yandex_pay_settings, trust_id):
        return aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYSYS_API_URL}.*'),
            status=200,
            payload={
                'id': 'whatever',
                'result': [{
                    'expiration_month': '01',
                    'binding_ts': 1622544806.585433,
                    'holder': 'CARD HOLDER',
                    'id': trust_id,
                    'bank': 'sbrbanku',
                    'expiration_year': '2050',
                    'masked_number': '000000****0000',
                    'is_verified': True,
                    'system': 'mastercard',
                    'is_removed': False,
                    'remove_ts': 0,
                    'is_expired': False,
                }],
            },
            repeat=True,
        )
