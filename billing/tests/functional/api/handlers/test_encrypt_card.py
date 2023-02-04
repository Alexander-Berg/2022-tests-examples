import base64
import re
from datetime import timedelta
from urllib.parse import urljoin
from uuid import UUID, uuid4

import pytest
from aioresponses import CallbackResult

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPType
from billing.yandex_pay.yandex_pay.interactions import CardProxyClient


@pytest.fixture
def owner_uid(randn):
    return randn()


@pytest.fixture
def trust_card_id(rands):
    return rands()


@pytest.fixture
def yandex_pay_trust_card_id(trust_card_id):
    return f'card-x{trust_card_id}'


@pytest.fixture
def card_expiration_date():
    return utcnow() + timedelta(days=365)


@pytest.fixture
def predefined_card_id():
    return str(uuid4())


@pytest.fixture
async def card(
    storage, owner_uid, predefined_card_id, card_expiration_date, yandex_pay_trust_card_id
):
    return await storage.card.create(
        Card(
            trust_card_id=yandex_pay_trust_card_id,
            owner_uid=owner_uid,
            tsp=TSPType.MASTERCARD,
            expire=card_expiration_date,
            last4='0000',
            card_id=UUID(predefined_card_id),
        )
    )


@pytest.fixture
def authentication(yandex_pay_settings, owner_uid, aioresponses_mocker):
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
def payment_method_data(owner_uid, card_expiration_date, yandex_pay_trust_card_id):
    return {
        "card_id": yandex_pay_trust_card_id,
        "orig_uid": str(owner_uid),
        "region_id": 225,
        "payment_method": "card",
        "system": "MasterCard",
        "expiration_month": str(card_expiration_date.month),
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
        "expiration_year": str(card_expiration_date.year),
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


@pytest.fixture(autouse=True)
def mock_get_card_trust_gateway(aioresponses_mocker, yandex_pay_settings, payment_method_data):
    return aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
        payload={'status': 'success', 'bound_payment_methods': [payment_method_data]},
    )


def paysys_payload(card_id, card_expiration_date):
    return {
        'card_id': card_id,
        'card_token': f'token-{card_id}',
        'holder': 'CARD HOLDER',
        'expiration_year': card_expiration_date.year % 100,  # last 2 digits of the year
        'expiration_month': 1,
    }


@pytest.fixture(autouse=True)
def mock_get_card_paysys(aioresponses_mocker, yandex_pay_settings, card_expiration_date):
    def callback(url, **kwargs):
        card_id = str(url).split('/')[-1]
        return CallbackResult(payload=paysys_payload(card_id, card_expiration_date))

    return aioresponses_mocker.get(
        re.compile(f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/.*'),
        callback=callback,
    )


def fake_encryption(value: str):
    return base64.b64encode(value.encode()).decode()


@pytest.fixture(autouse=True)
def mock_cardproxy(aioresponses_mocker, yandex_pay_settings):
    def callback(url, **kwargs):
        token = kwargs['headers']['X-Diehard-Card-Token']
        encrypted_card = fake_encryption(token)
        return CallbackResult(payload={'data': {'encrypted_card': encrypted_card}})

    return aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        callback=callback,
    )


@pytest.mark.asyncio
async def test_encrypt_card_by_trust_id(
    app,
    authentication,
    yandex_pay_trust_card_id,
    trust_card_id,
):
    r = await app.post(
        '/api/mobile/v1/wallet/thales/encrypted_card',
        json={'card_id': yandex_pay_trust_card_id},
        **authentication,
    )

    assert_that(r.status, equal_to(200))
    response_json = await r.json()
    expected_encrypted_card = fake_encryption(f'token-{trust_card_id}')
    assert_that(
        response_json,
        equal_to(
            {
                'code': 200,
                'status': 'success',
                'data': {'encrypted_card': expected_encrypted_card}
            }
        ),
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('card')
async def test_encrypt_card_by_pay_card_id(
    app,
    authentication,
    trust_card_id,
    predefined_card_id,
    storage,
):
    r = await app.post(
        '/api/mobile/v1/wallet/thales/encrypted_card',
        json={'card_id': predefined_card_id},
        **authentication,
    )

    assert_that(r.status, equal_to(200))
    response_json = await r.json()
    expected_encrypted_card = fake_encryption(f'token-{trust_card_id}')
    assert_that(
        response_json,
        equal_to(
            {
                'code': 200,
                'status': 'success',
                'data': {'encrypted_card': expected_encrypted_card}
            }
        ),
    )


@pytest.mark.asyncio
async def test_encrypt_card_trust_card_missing(app, authentication):
    r = await app.post(
        '/api/mobile/v1/wallet/thales/encrypted_card',
        json={'card_id': 'card-x-missing'},
        **authentication,
    )

    assert_that(r.status, equal_to(404))
    response_json = await r.json()
    assert_that(
        response_json,
        equal_to(
            {
                'code': 404,
                'status': 'fail',
                'data': {'message': 'CARD_NOT_FOUND'},
            }
        ),
    )


@pytest.mark.asyncio
async def test_encrypt_card_pay_card_missing(app, authentication, predefined_card_id):
    # card is missing since 'card' fixture is not activated
    r = await app.post(
        '/api/mobile/v1/wallet/thales/encrypted_card',
        json={'card_id': predefined_card_id},
        **authentication,
    )

    assert_that(r.status, equal_to(404))
    response_json = await r.json()
    assert_that(
        response_json,
        equal_to(
            {
                'code': 404,
                'status': 'fail',
                'data': {'message': 'CARD_NOT_FOUND'},
            }
        ),
    )
