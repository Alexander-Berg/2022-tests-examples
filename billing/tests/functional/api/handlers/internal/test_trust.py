import re
from datetime import timedelta
from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.api.handlers.internal.trust import TrustPaymentTokenHandler
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP

URL = '/api/internal/v1/trust/payment_tokens'


@pytest.fixture
def mock_internal_tvm_with_acl(aioresponses_mocker, mocker, yandex_pay_settings):
    matcher = TrustPaymentTokenHandler.TICKET_CHECKER.src_matchers[0]

    aioresponses_mocker.get(
        re.compile(f'.*{yandex_pay_settings.TVM_URL}/tvm/checksrv.*'),
        payload={'src': matcher.tvm_id, 'dst': matcher.tvm_id},
    )

    mocker.patch.object(matcher, 'acls', {'trust_payment_token'})


@pytest.fixture
def payment_token_mastercard(rands):
    return rands()


@pytest.fixture
def payment_token_visa(rands):
    return rands()


@pytest.fixture
def mock_mastercard_response(payment_token_mastercard):
    return {
        "data": {
            "payment_token": payment_token_mastercard,
            "masked_card": {
                "srcDigitalCardId": "fake",
                "panBin": "520473",
                "panLastFour": "4784",
                "tokenLastFour": "2069",
                "digitalCardData": {
                    "status": "ACTIVE",
                    "artUri": "fake.png",
                    "descriptorName": "Example Bank Product Configuration",
                },
                "paymentCardType": "CREDIT",
                "panExpirationYear": "2025",
                "dateOfCardCreated": "2021-05-11 15:24:32.941000+00:00",
                "panExpirationMonth": "05",
                "dateOfCardLastUsed": "2021-05-11 16:17:47.826000+00:00"
            },
            "maskedConsumer": {
                "status": "ACTIVE",
                "srcConsumerId": "fake",
                "maskedFullName": "",
                "dateConsumerAdded": "2021-05-11 15:24:32.941000+00:00",
                "maskedConsumerIdentity": {
                    "identityType": "EXTERNAL_ACCOUNT_ID",
                    "maskedIdentityValue": "fake",
                }
            }
        }
    }


@pytest.fixture
def mock_duckgo_mastercard(aioresponses_mocker, yandex_pay_settings, mock_mastercard_response):
    aioresponses_mocker.post(
        f'{yandex_pay_settings.DUCKGO_API_URL}/v1/mastercard/checkout',
        payload=mock_mastercard_response,
    )


@pytest.fixture
def mock_duckgo_visa(aioresponses_mocker, yandex_pay_settings, payment_token_visa):
    aioresponses_mocker.post(
        f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/checkout',
        payload={'data': {'payment_token': payment_token_visa}},
    )


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
async def psp(storage, yandex_pay_settings, rands):
    psp = await storage.psp.create(
        PSP(
            psp_external_id=yandex_pay_settings.TRUST_PSP_EXTERNAL_ID,
            psp_id=uuid4(),
            public_key=rands(),
            public_key_signature=rands(),
        )
    )

    yield psp

    await storage.psp.delete(psp)


@pytest.fixture
async def card(storage, uid, trust_card_id):
    return await storage.card.create(
        Card(
            trust_card_id=trust_card_id,
            owner_uid=uid,
            tsp=TSPType.MASTERCARD,
            expire=utcnow() + timedelta(days=1),
            last4='0000',
        )
    )


@pytest.fixture
async def enrollment(storage, card, rands):
    return await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_card_id=None,
            tsp_token_id=rands(),
            tsp_token_status=TSPTokenStatus.ACTIVE,
            card_last4=card.last4,
        )
    )


@pytest.fixture
async def payload(card, uid):
    return {
        'trust_card_id': card.trust_card_id,
        'uid': uid,
        'amount': '12.34',
        'currency': 'XTS',
    }


@pytest.fixture(autouse=True)
async def mock_trust_gateway_response(
    aioresponses_mocker, yandex_pay_settings, trust_card_id
):
    return aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
        payload={
            'bound_payment_methods': [
                {
                    'id': trust_card_id,
                    'card_id': trust_card_id,
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
        repeat=True,
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures(
    'mock_internal_tvm_with_acl', 'mock_duckgo_mastercard', 'psp', 'enrollment'
)
async def test_create_internal_token_mastercard(payload, payment_token_mastercard, internal_app):
    r = await internal_app.post(
        URL,
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json=payload,
    )

    assert_that(r.status, equal_to(200))

    expected_response = {
        'code': 200,
        'status': 'success',
        'data': {'payment_token': payment_token_mastercard},
    }
    assert_that(await r.json(), equal_to(expected_response))


@pytest.mark.asyncio
@pytest.mark.usefixtures(
    'mock_internal_tvm_with_acl', 'mock_duckgo_visa', 'psp', 'enrollment'
)
async def test_create_internal_token_visa(
    storage, card, payload, payment_token_visa, internal_app
):
    card.tsp = TSPType.VISA
    await storage.card.save(card)

    r = await internal_app.post(
        URL,
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json=payload,
    )

    assert_that(r.status, equal_to(200))

    expected_response = {
        'code': 200,
        'status': 'success',
        'data': {'payment_token': payment_token_visa},
    }
    assert_that(await r.json(), equal_to(expected_response))


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_internal_tvm_with_acl')
@pytest.mark.parametrize(
    'url',
    [
        pytest.param(f'/api/internal/v1/merchants/{uuid4()}', id='get_merchant'),
        pytest.param(f'/api/internal/v1/psp/{uuid4()}', id='get_psp'),
    ]
)
async def test_ensure_other_endpoints_unavailable_with_trust_payment_token_acl(
    payload, internal_app, url
):
    r = await internal_app.get(
        url,
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
    )

    assert_that(r.status, equal_to(403))
    expected_response = {
        'code': 403,
        'status': 'fail',
        'data': {'message': 'SERVICE_NOT_ALLOWED'},
    }
    assert_that(await r.json(), equal_to(expected_response))
