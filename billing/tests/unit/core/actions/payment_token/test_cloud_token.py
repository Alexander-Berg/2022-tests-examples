from uuid import uuid4

import pytest
from pay.lib.entities.payment_token import MITInfo

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.payment_token.cloud_token import CreateCloudTokenAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreInvalidAuthMethodError, CoreTSPNotSupportedError, InternalTokenIssueError
)
from billing.yandex_pay.yandex_pay.core.payment_token import generate_message
from billing.yandex_pay.yandex_pay.interactions import DuckGoClient
from billing.yandex_pay.yandex_pay.interactions.duckgo.entities.checkout import (
    MasterCardCheckoutResult, VisaCheckoutResult
)


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(
        PSP(
            psp_external_id='gw-id',
            psp_id=uuid4(),
            public_key='pubkey',
            public_key_signature='pubkeysig',
        )
    )


@pytest.fixture
async def card(storage, uid):
    return await storage.card.create(
        Card(
            trust_card_id='trust_card_id',
            owner_uid=uid,
            tsp=TSPType.MASTERCARD,
            expire=utcnow(),
            last4='0000',
        )
    )


@pytest.fixture
async def enrollment(storage, card):
    return await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_card_id=None,
            tsp_token_id='tsp-token-id',
            tsp_token_status=TSPTokenStatus.ACTIVE,
            card_last4=card.last4,
        )
    )


@pytest.fixture
def message(uid):
    return generate_message(uid)


@pytest.fixture
def payment_token(rands):
    return rands(k=36)


@pytest.fixture(autouse=True)
def enable_tokens(yandex_pay_settings):
    yandex_pay_settings.API_TOKENS_ENABLED = True


@pytest.fixture
def params(enrollment, psp, message):
    return dict(
        tsp=TSPType.MASTERCARD,
        enrollment=enrollment,
        psp=psp,
        amount=123,
        currency='XTS',
        gateway_merchant_id='any',
        message=message,
        mit_info=MITInfo(deferred=True),
    )


class TestCreateCloudTokenActionSuccess:
    @pytest.fixture
    def mock_mastercard_response(self, payment_token):
        mock_response = {
            "payment_token": payment_token,
            "masked_card": {
                "src_digital_card_id": "fake",
                "pan_bin": "520473",
                "pan_last_four": "4784",
                "token_last_four": "2069",
                "digital_card_data": {
                    "status": "ACTIVE",
                    "art_uri": "fake.png",
                    "descriptor_name": "Example Bank Product Configuration",
                },
                "payment_card_type": "CREDIT",
                "pan_expiration_year": "2025",
                "date_of_card_created": "2021-05-11 15:24:32.941000+00:00",
                "pan_expiration_month": "05",
                "date_of_card_last_used": "2021-05-11 16:17:47.826000+00:00"
            },
            "masked_consumer": {
                "status": "ACTIVE",
                "src_consumer_id": "fake",
                "masked_full_name": "",
                "date_consumer_added": "2021-05-11 15:24:32.941000+00:00",
                "masked_consumer_identity": {
                    "identity_type": "EXTERNAL_ACCOUNT_ID",
                    "masked_identity_value": "fake",
                }
            }
        }
        return MasterCardCheckoutResult(**mock_response)

    @pytest.mark.asyncio
    async def test_create_mastercard_cloud_token(self, params, payment_token, mock_mastercard_response, mocker):
        mock = mocker.patch.object(
            DuckGoClient,
            'mastercard_checkout',
            mocker.AsyncMock(return_value=mock_mastercard_response),
        )
        params['tsp'] = TSPType.MASTERCARD

        token = await CreateCloudTokenAction(**params).run()

        assert_that(token, equal_to(payment_token))

        mock.assert_awaited_once_with(
            card_id=params['enrollment'].tsp_token_id,
            gateway_merchant_id='any',
            recipient_id=params['psp'].psp_external_id,
            recipient_pub_key=params['psp'].public_key,
            recipient_pub_key_signature=params['psp'].public_key_signature,
            transaction_currency='XTS',
            transaction_amount=123,
            mit_info=params['mit_info'],
            message_id=params['message'].message_id,
            message_expiration=params['message'].expiration_epoch_ms,
        )

    @pytest.mark.asyncio
    async def test_create_visa_cloud_token(self, params, payment_token, mocker):
        mock = mocker.patch.object(
            DuckGoClient,
            'visa_checkout',
            mocker.AsyncMock(
                return_value=VisaCheckoutResult(payment_token=payment_token)
            ),
        )
        params['tsp'] = TSPType.VISA

        token = await CreateCloudTokenAction(**params).run()

        assert_that(token, equal_to(payment_token))

        mock.assert_awaited_once_with(
            card_id=params['enrollment'].tsp_token_id,
            gateway_merchant_id='any',
            recipient_id=params['psp'].psp_external_id,
            recipient_pub_key=params['psp'].public_key,
            recipient_pub_key_signature=params['psp'].public_key_signature,
            transaction_currency='XTS',
            transaction_amount=123,
            mit_info=params['mit_info'],
            message_id=params['message'].message_id,
            message_expiration=params['message'].expiration_epoch_ms,
            relationship_id='YaPay_YaRu_IWP_TRTSP',
        )


class TestCreateCloudTokenActionFailure:
    @pytest.mark.asyncio
    @pytest.mark.parametrize('tsp', list(TSPType))
    async def test_cloud_tokens_not_allowed_if_global_setting_disabled(
        self, yandex_pay_settings, params, tsp
    ):
        yandex_pay_settings.API_TOKENS_ENABLED = False
        params['tsp'] = tsp

        with pytest.raises(CoreInvalidAuthMethodError):
            await CreateCloudTokenAction(**params).run()

    @pytest.mark.asyncio
    async def test_tsp_not_supported(self, params):
        params['tsp'] = TSPType.UNKNOWN
        with pytest.raises(CoreTSPNotSupportedError):
            await CreateCloudTokenAction(**params).run()

    @pytest.mark.asyncio
    async def test_enrollment_expired(self, params):
        params['enrollment'].expire = utcnow()

        with pytest.raises(InternalTokenIssueError) as exc_info:
            await CreateCloudTokenAction(**params).run()

        assert_that(exc_info.value.description, equal_to('Enrollment expired'))

    @pytest.mark.asyncio
    async def test_enrollment_inactive(self, params):
        params['enrollment'].tsp_token_status = TSPTokenStatus.SUSPENDED

        with pytest.raises(InternalTokenIssueError) as exc_info:
            await CreateCloudTokenAction(**params).run()

        assert_that(exc_info.value.description, equal_to('Enrollment is not active'))
