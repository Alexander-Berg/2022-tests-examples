import asyncio
import re
import uuid
from typing import ClassVar

import pytest

from sendr_auth import CsrfChecker
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.checkout import CREATE_PLUS_ORDER_TASK_NAME
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions.antifraud.sandbox import SandboxAntifraudClient
from billing.yandex_pay.yandex_pay.storage import SandboxCardMapper


@pytest.fixture
def owner_uid(randn):
    return randn()


@pytest.fixture
def fake_user_ticket(rands):
    return rands()


@pytest.fixture
def psp_external_id(rands):
    return rands()


@pytest.fixture
def merchant_id():
    return str(uuid.uuid4())


@pytest.fixture
def token_mock(rands):
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
def authentication(
    sandbox_app, yandex_pay_settings, owner_uid, aioresponses_mocker, fake_user_ticket
):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*'),
        status=200,
        payload={
            'status': {'value': 'VALID'},
            'uid': {'value': owner_uid},
            'user_ticket': fake_user_ticket,
            'login_id': 'login_id',
        },
        repeat=True,
    )

    key = sandbox_app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
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


class SandboxBase:
    CARD_ID: ClassVar[str] = None
    AUTH_METHOD: ClassVar[str] = None

    @pytest.fixture
    async def card(self, owner_uid) -> Card:
        return await SandboxCardMapper(
            connection=None,  # type: ignore
        ).get_by_card_id_and_uid(self.CARD_ID, owner_uid)

    @pytest.fixture
    async def card_id(self, sandbox_app, authentication):
        r = await sandbox_app.get('/api/v1/user_cards', **authentication)
        response = await r.json()
        for card in response['data']['cards']:
            if self.AUTH_METHOD in card['allowed_auth_methods']:
                return card['id']
        assert False

    @pytest.fixture
    def params(self, card_id, psp_external_id, merchant_id):
        return {
            'card_id': card_id,
            'sheet': {
                'version': 2,
                'currency_code': 'RUB',
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
                'payment_methods': [{
                    'type': 'CARD',
                    'gateway': psp_external_id,
                    'gateway_merchant_id': 'hmnid',
                    'allowed_auth_methods': ['CLOUD_TOKEN', 'PAN_ONLY'],
                    'allowed_card_networks': ['MASTERCARD'],
                    'verification_details': True,  # чтобы стриггерить антифрод
                }],
            },
            'challenge_return_path': 'https://challenge-return-path.test',
        }

    @pytest.fixture
    def expected_json_body(self, card, token_mock):
        return {
            'code': 200,
            'status': 'success',
            'data': {
                'payment_token': token_mock,
                'payment_method_info': {
                    'type': 'CARD',
                    'card_last4': card.last4,
                    'card_network': card.tsp.value.upper(),
                    'mit_info': {'recurring': False, 'deferred': False},
                }
            }
        }


@pytest.fixture(autouse=True)
def enable_pay_plus_interaction(yandex_pay_settings):
    # sandbox must not call plus backend even if SHOULD_CREATE_ORDER_IN_PAY_PLUS is True
    yandex_pay_settings.SHOULD_CREATE_ORDER_IN_PAY_PLUS = True


@pytest.fixture(autouse=True)
def pay_backend_mock_exception(aioresponses_mocker):
    # throw an exception if plus backend is called
    return aioresponses_mocker.post(
        re.compile(r'^.+/api/v1/orders$'),
        exception=Exception('should not have been called'),
    )


@pytest.fixture(autouse=True)
def spy_antifraud_client(mocker):
    return mocker.spy(SandboxAntifraudClient, 'get_challenge')


class TestSandboxCloudTokenCard(SandboxBase):
    CARD_ID = uuid.UUID('caa9c171-2fab-45e6-b1f8-6212980aa9aa', version=4)
    AUTH_METHOD = 'CLOUD_TOKEN'

    @pytest.fixture(autouse=True)
    async def cancel_dangling_async_tasks(self):
        # CheckoutAction creates an asyncio task for UpdateEnrollmentMetadataAction,
        # which is intentionally not awaited. This causes a connection error
        # ('psycopg2.InterfaceError: cursor already closed') when pytest tries to
        # close the DB connection during the test teardown.
        # As a cure, this fixture gracefully cancels such dangling task(s)
        # and awaits the cancellations before pytest comes with its sledgehammer.
        yield

        for task in asyncio.all_tasks():
            if task.get_name() in {CREATE_PLUS_ORDER_TASK_NAME}:
                task.cancel()
                try:
                    await task
                except asyncio.CancelledError:
                    pass

    @pytest.fixture
    def mastercard_checkout_response_body(self, token_mock):
        return {
            'code': 200,
            'data': {
                'masked_card': {
                    'dateOfCardCreated': '2020-12-05T23:53:59.579Z',
                    'dateOfCardLastUsed': '2020-12-10T13:02:04.196Z',
                    'digitalCardData': {
                        'artUri': (
                            'https://sbx.assets.mastercard.com/'
                            'card-art/combined-image-asset/c911d734-2f4b-4326-8363-afb0ffe2bc2b.png'
                        ),
                        'descriptorName': 'Example Bank Product Configuration',
                        'status': 'ACTIVE',
                    },
                    'maskedBillingAddress': None,
                    'panBin': '520473',
                    'panExpirationMonth': '01',
                    'panExpirationYear': '2099',
                    'panLastFour': '4784',
                    'paymentAccountReference': '',
                    'paymentCardDescriptor': '',
                    'paymentCardType': 'CREDIT',
                    'srcDigitalCardId': '198e7a1f-c253-46dc-92d9-520356ed6dee'
                },
                'masked_consumer': {
                    'dateConsumerAdded': '2020-12-05T23:52:14.183Z',
                    'maskedConsumerIdentity': {
                        'identityType': 'EXTERNAL_ACCOUNT_ID',
                        'maskedIdentityValue': '4053525715'
                    },
                    'maskedFullName': '',
                    'maskedMobileNumber': {},
                    'srcConsumerId': '11b91ca2-b98e-482f-891c-3c45167b9070',
                    'status': 'ACTIVE'
                },
                'payment_token': token_mock,

            },
            'status': 'success'
        }

    @pytest.mark.asyncio
    async def test_should_respond_with_payment_token(
        self,
        sandbox_app,
        yandex_pay_settings,
        aioresponses_mocker,
        params,
        expected_json_body,
        mastercard_checkout_response_body,
        authentication,
        pay_backend_mock_exception,
    ):
        params['sheet']['payment_methods'][0]['allowed_auth_methods'] = ['CLOUD_TOKEN']
        aioresponses_mocker.post(
            re.compile(f'^{yandex_pay_settings.DUCKGO_API_URL}.*'),
            status=200,
            payload=mastercard_checkout_response_body
        )

        r = await sandbox_app.post(
            'api/v1/checkout',
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_json_body))
        pay_backend_mock_exception.assert_not_called()


class TestSandboxPanCard(SandboxBase):
    CARD_ID = uuid.UUID('fe0dec60-e2bf-4cc1-8c23-be668650a27d', version=4)
    AUTH_METHOD = 'PAN_ONLY'

    @pytest.fixture
    def pan_checkout_response_body(self, token_mock):
        return {
            'code': 200,
            'data': {
                'payment_token': token_mock,

            },
            'status': 'success'
        }

    @pytest.mark.asyncio
    async def test_should_respond_with_payment_token(
        self,
        sandbox_app,
        yandex_pay_settings,
        aioresponses_mocker,
        params,
        expected_json_body,
        pan_checkout_response_body,
        authentication,
        pay_backend_mock_exception,
        spy_antifraud_client,
    ):
        aioresponses_mocker.post(
            re.compile(f'^{yandex_pay_settings.SANDBOX_DUCKGO_INTERNAL_API_URL}.*'),
            status=200,
            payload=pan_checkout_response_body
        )

        params['sheet']['payment_methods'][0]['allowed_card_networks'] = ['VISA']

        r = await sandbox_app.post(
            'api/v1/checkout',
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_json_body))
        pay_backend_mock_exception.assert_not_called()
        spy_antifraud_client.assert_called_once()


class TestSandboxNotFoundCard:
    CARD_ID = uuid.uuid4()

    @pytest.fixture
    def expected_json_body(self):
        return {
            'status': 'fail',
            'data': {
                'message': 'CARD_NOT_FOUND'
            },
            'code': 404
        }

    @pytest.fixture
    def params(self, psp_external_id, merchant_id):
        return {
            'card_id': str(self.CARD_ID),
            'sheet': {
                'version': 2,
                'currency_code': 'RUB',
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
                'payment_methods': [{
                    'type': 'CARD',
                    'gateway': psp_external_id,
                    'gateway_merchant_id': 'hmnid',
                    'allowed_auth_methods': ['CLOUD_TOKEN'],
                    'allowed_card_networks': ['MASTERCARD'],
                }],
            },
        }

    @pytest.mark.asyncio
    async def test_should_raise_not_found(
        self,
        sandbox_app,
        params,
        expected_json_body,
        authentication,
        pay_backend_mock_exception,
    ):
        r = await sandbox_app.post(
            'api/v1/checkout',
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert_that(r.status, equal_to(404))
        assert_that(json_body, equal_to(expected_json_body))
        pay_backend_mock_exception.assert_not_called()
