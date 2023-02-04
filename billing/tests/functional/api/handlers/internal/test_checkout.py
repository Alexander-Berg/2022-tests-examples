import re
from datetime import timedelta
from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, instance_of, match_equality

from billing.yandex_pay.yandex_pay.api.handlers.internal.checkout import CheckoutPaymentTokenHandler
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP

URL = '/api/internal/v1/checkout/payment_tokens'


@pytest.fixture(autouse=True)
def mock_internal_tvm_with_acl(aioresponses_mocker, mocker, yandex_pay_settings):
    matcher = CheckoutPaymentTokenHandler.TICKET_CHECKER.src_matchers[0]

    aioresponses_mocker.get(
        re.compile(f'.*{yandex_pay_settings.TVM_URL}/tvm/checksrv.*'),
        payload={'src': matcher.tvm_id, 'dst': matcher.tvm_id},
    )

    return mocker.patch.object(matcher, 'acls', {'internal_payment_token'})


@pytest.fixture
def payment_token_mastercard(rands):
    return rands()


@pytest.fixture
def payment_token_visa(rands):
    return rands()


@pytest.fixture
def payment_token_pan_only(rands):
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
def mock_trust_cardproxy(aioresponses_mocker, yandex_pay_settings, payment_token_pan_only):
    aioresponses_mocker.post(
        re.compile(f'^{yandex_pay_settings.CARD_PROXY_API_URL}.*'),
        status=200,
        payload={'status': 'success', 'data': {'payment_token': payment_token_pan_only}}
    )


@pytest.fixture
def mock_trust_paysys(aioresponses_mocker, yandex_pay_settings, trust_card_id):
    obj_id = trust_card_id.removeprefix('card-x')
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/{obj_id}'),
        status=200,
        payload={
            'status': 'success',
            'card_id': obj_id,
            'card_token': 'card-token',
            'expiration_month': 1,
            'expiration_year': 2020,
            'holder': None,
        },
    )


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def card_tsp():
    return TSPType.MASTERCARD


@pytest.fixture
async def card(storage, uid, trust_card_id, card_tsp):
    return await storage.card.create(
        Card(
            trust_card_id=trust_card_id,
            owner_uid=uid,
            tsp=card_tsp,
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


@pytest.fixture(autouse=True)
async def psp(storage, rands):
    return await create_psp_entity(
        storage,
        PSP(
            psp_id=uuid4(),
            psp_external_id=rands(),
            public_key='public-key',
            public_key_signature='public-key-signature',
        )
    )


@pytest.fixture
async def mock_trust_gateway(
    storage, aioresponses_mocker, yandex_pay_settings, card
):
    card_network = card.tsp.value.upper()
    payload = {
        'status': 'success',
        'bound_payment_methods': [
            {
                'region_id': 225,
                'payment_method': 'card',
                'system': card_network,
                'expiration_month': '01',
                'card_country': 'RUS',
                'binding_ts': '1586458392.247',
                'ebin_tags_version': 0,
                'card_level': 'STANDARD',
                'holder': 'Card Holder',
                'id': card.trust_card_id,
                'payment_system': card_network,
                'last_paid_ts': '1586458392.247',
                'account': f'****{card.last4}',
                'ebin_tags': [],
                'expiration_year': '2030',
                'aliases': [
                    card.trust_card_id,
                ],
                'expired': False,
                'card_bank': 'SBERBANK OF RUSSIA',
                'card_id': card.trust_card_id,
                'recommended_verification_type': 'standard2',
                'orig_uid': str(card.owner_uid),
                'binding_systems': ['trust'],
            }
        ],
    }
    return aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
        payload=payload,
    )


@pytest.mark.usefixtures('enrollment', 'mock_trust_gateway')
class TestCloudToken:
    @pytest.fixture
    async def payload(self, card, psp, uid):
        return {
            'uid': uid,
            'gateway_merchant_id': 'gateway_merchant_id',
            'card_id': str(card.card_id),
            'amount': '12.34',
            'currency': 'XTS',
            'auth_methods': ['CLOUD_TOKEN', 'PAN_ONLY'],
            'psp_external_id': psp.psp_external_id,
        }

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('mock_duckgo_mastercard')
    async def test_create_internal_token_mastercard(
        self,
        payload,
        payment_token_mastercard,
        internal_app,
    ):
        r = await internal_app.post(
            URL,
            headers={'x-ya-service-ticket': 'dummy-service-ticket'},
            json=payload,
            raise_for_status=True,
        )

        expected_response = {
            'code': 200,
            'status': 'success',
            'data': {
                'payment_token': payment_token_mastercard,
                'payment_method_info': {
                    'card_network': 'MASTERCARD',
                    'card_last4': '0000',
                    'auth_method': 'CLOUD_TOKEN',
                },
                'message_id': match_equality(instance_of(str)),
            },
        }
        assert_that(await r.json(), equal_to(expected_response))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('card_tsp', [TSPType.VISA])
    @pytest.mark.usefixtures('mock_duckgo_visa')
    async def test_create_internal_token_visa(
        self,
        payload,
        payment_token_visa,
        internal_app,
    ):
        r = await internal_app.post(
            URL,
            headers={'x-ya-service-ticket': 'dummy-service-ticket'},
            json=payload,
            raise_for_status=True,
        )

        expected_response = {
            'code': 200,
            'status': 'success',
            'data': {
                'payment_token': payment_token_visa,
                'payment_method_info': {
                    'card_network': 'VISA',
                    'card_last4': '0000',
                    'auth_method': 'CLOUD_TOKEN',
                },
                'message_id': match_equality(instance_of(str)),
            },
        }
        assert_that(await r.json(), equal_to(expected_response))

    @pytest.mark.asyncio
    async def test_create_internal_cloud_token__card_not_found(
        self,
        storage,
        payload,
        internal_app,
        card,
        enrollment,
    ):
        await storage.enrollment.delete(enrollment)
        await storage.card.delete(card)

        r = await internal_app.post(
            URL,
            headers={'x-ya-service-ticket': 'dummy-service-ticket'},
            json=payload,
        )

        assert_that(r.status, equal_to(404))
        expected_response = {
            'code': 404,
            'status': 'fail',
            'data': {'message': 'CARD_NOT_FOUND'},
        }
        assert_that(await r.json(), equal_to(expected_response))


@pytest.mark.usefixtures('mock_trust_gateway', 'mock_trust_paysys', 'mock_trust_cardproxy')
class TestPanOnly:
    @pytest.fixture
    async def payload(self, card, psp, uid):
        return {
            'uid': uid,
            'gateway_merchant_id': 'gateway_merchant_id',
            'card_id': str(card.card_id),
            'amount': '12.34',
            'currency': 'XTS',
            'auth_methods': ['PAN_ONLY', 'CLOUD_TOKEN'],
            'psp_external_id': psp.psp_external_id,
        }

    @pytest.mark.asyncio
    @pytest.mark.parametrize('card_tsp', list(TSPType))
    async def test_create_internal_token_pan_only__pay_card_exists(
        self,
        payload,
        payment_token_pan_only,
        internal_app,
        card_tsp,
    ):
        r = await internal_app.post(
            URL,
            headers={'x-ya-service-ticket': 'dummy-service-ticket'},
            json=payload,
            raise_for_status=True,
        )

        expected_response = {
            'code': 200,
            'status': 'success',
            'data': {
                'payment_token': payment_token_pan_only,
                'payment_method_info': {
                    'card_network': card_tsp.value.upper(),
                    'card_last4': '0000',
                    'auth_method': 'PAN_ONLY',
                },
                'message_id': match_equality(instance_of(str)),
            },
        }
        assert_that(await r.json(), equal_to(expected_response))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('card_tsp', list(TSPType))
    async def test_create_internal_token_pan_only__pay_card_not_exists(
        self,
        storage,
        payload,
        payment_token_pan_only,
        internal_app,
        card_tsp,
        card,
        trust_card_id,
    ):
        payload['card_id'] = trust_card_id
        await storage.card.delete(card)

        r = await internal_app.post(
            URL,
            headers={'x-ya-service-ticket': 'dummy-service-ticket'},
            json=payload,
            raise_for_status=True,
        )

        expected_response = {
            'code': 200,
            'status': 'success',
            'data': {
                'payment_token': payment_token_pan_only,
                'payment_method_info': {
                    'card_network': card_tsp.value.upper(),
                    'card_last4': '0000',
                    'auth_method': 'PAN_ONLY',
                },
                'message_id': match_equality(instance_of(str)),
            },
        }
        assert_that(await r.json(), equal_to(expected_response))

    @pytest.mark.asyncio
    async def test_create_internal_token_pan_only__card_not_found(
        self,
        storage,
        payload,
        internal_app,
        card,
    ):
        await storage.card.delete(card)

        r = await internal_app.post(
            URL,
            headers={'x-ya-service-ticket': 'dummy-service-ticket'},
            json=payload,
        )

        assert_that(r.status, equal_to(404))
        expected_response = {
            'code': 404,
            'status': 'fail',
            'data': {'message': 'CARD_NOT_FOUND'},
        }
        assert_that(await r.json(), equal_to(expected_response))
