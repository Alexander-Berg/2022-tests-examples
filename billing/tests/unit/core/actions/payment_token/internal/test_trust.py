import logging
from datetime import timedelta
from decimal import Decimal

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains, equal_to, has_entries, has_properties

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.payment_token.internal.checkout import (
    CreateInternalCheckoutPaymentTokenAction, InternalCheckoutResponse
)
from billing.yandex_pay.yandex_pay.core.actions.payment_token.internal.trust import (
    CreateInternalTrustPaymentTokenAction
)
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.checkout import PaymentMethodInfo
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork, TSPType
from billing.yandex_pay.yandex_pay.core.entities.message import Message
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import CoreCardNotFoundError, InternalTokenIssueError
from billing.yandex_pay.yandex_pay.interactions import TrustPaymentsClient


@pytest.fixture
def user(randn):
    return User(randn())


@pytest.fixture(autouse=True)
async def card(storage, user):
    return await storage.card.create(
        Card(
            trust_card_id='real_trust_card_id',
            owner_uid=user.uid,
            tsp=TSPType.MASTERCARD,
            expire=utcnow(),
            last4='0000',
        )
    )


@pytest.fixture
def payment_token(rands):
    return rands()


@pytest.fixture
def internal_checkout_response(rands, payment_token, card):
    message = Message(rands(), utcnow() + timedelta(days=1))

    return InternalCheckoutResponse(
        payment_method_info=PaymentMethodInfo(
            method_type=PaymentMethodType.CARD,
            payment_token=payment_token,
            auth_method=AuthMethod.CLOUD_TOKEN,
            card_last4=card.last4,
            card_network=CardNetwork.from_tsp_type(card.tsp),
        ),
        message=message,
    )


@pytest.fixture(autouse=True)
def mock_internal_checkout_action(mock_action, internal_checkout_response):
    return mock_action(CreateInternalCheckoutPaymentTokenAction, internal_checkout_response)


@pytest.fixture(autouse=True)
def mock_trust_get_payment_methods(mocker):
    return mocker.patch.object(
        TrustPaymentsClient,
        'get_payment_methods',
        mocker.AsyncMock(return_value=[mocker.Mock(id='trust_card_id', card_id='real_trust_card_id')]),
    )


@pytest.fixture
def call_action(user, card):
    async def _inner(**kwargs):
        kwargs = dict(
            trust_card_id=card.trust_card_id,
            user=user,
            amount=Decimal('10.23'),
            currency='XTS',
        ) | kwargs
        return await CreateInternalTrustPaymentTokenAction(**kwargs).run()

    return _inner


class TestCreateInternalPaymentTokenActionSuccess:
    @pytest.mark.asyncio
    async def test_creates_token(self, payment_token, call_action):
        token = await call_action()

        assert_that(token, equal_to(payment_token))

    @pytest.mark.asyncio
    async def test_calls_get_payment_methods(self, mock_trust_get_payment_methods, call_action, user):
        await call_action()

        mock_trust_get_payment_methods.assert_awaited_once_with(uid=user.uid)

    @pytest.mark.asyncio
    async def test_calls_internal_checkout_action(
        self, mock_internal_checkout_action, user, card, call_action
    ):
        await call_action()

        mock_internal_checkout_action.assert_called_once_with(
            user=user,
            gateway_merchant_id=CreateInternalTrustPaymentTokenAction.gateway_merchant_id,
            card_id=str(card.card_id),
            currency='XTS',
            amount=Decimal('10.23'),
            auth_methods=[AuthMethod.CLOUD_TOKEN],
            psp_external_id=CreateInternalTrustPaymentTokenAction.trust_psp_external_id,
        )

    @pytest.mark.asyncio
    async def test_create_cloud_token_logged(
        self, user, card, internal_checkout_response, call_action, dummy_logs
    ):
        await call_action()

        logs = dummy_logs()
        assert_that(
            logs,
            contains(
                has_properties(
                    message='INTERNAL_TRUST_CLOUD_TOKEN_ISSUE_START',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=user.uid,
                        paymethod_id=card.trust_card_id,
                        trust_card_id=card.trust_card_id,
                        amount=Decimal('10.23'),
                        currency='XTS',
                    ),
                ),
                has_properties(
                    message='INTERNAL_TRUST_CLOUD_TOKEN_ISSUED',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=user.uid,
                        paymethod_id=card.trust_card_id,
                        trust_card_id='real_trust_card_id',
                        amount=Decimal('10.23'),
                        currency='XTS',
                        psp_external_id=CreateInternalTrustPaymentTokenAction.trust_psp_external_id,
                        card_id=card.card_id,
                        message_id=internal_checkout_response['message'].message_id,
                        message_expires_at=internal_checkout_response['message'].expires_at,
                    ),
                ),
            )
        )


class TestCreateInternalPaymentTokenActionFailure:
    @pytest.mark.asyncio
    async def test_internal_cloud_tokens_disabled(self, call_action, yandex_pay_settings):
        yandex_pay_settings.TRUST_INTERNAL_CLOUD_TOKENS_ENABLED = False

        with pytest.raises(InternalTokenIssueError) as exc_info:
            await call_action()

        assert_that(
            exc_info.value, has_properties(description='Internal cloud tokens disabled')
        )

    @pytest.mark.asyncio
    async def test_internal_cloud_tokens_disabled_for_uid(
        self, call_action, user, yandex_pay_settings
    ):
        yandex_pay_settings.TRUST_INTERNAL_CLOUD_TOKENS_UID_WHITELIST = (user.uid + 1,)

        with pytest.raises(InternalTokenIssueError) as exc_info:
            await call_action()

        assert_that(
            exc_info.value,
            has_properties(description='Internal cloud tokens disabled for uid'),
        )

    @pytest.mark.asyncio
    async def test_tsp_not_in_whitelist(self, call_action, mocker):
        mocker.patch.object(CreateInternalTrustPaymentTokenAction, 'trust_tsp_whitelist', {})

        with pytest.raises(InternalTokenIssueError) as exc_info:
            await call_action()

        assert_that(
            exc_info.value,
            has_properties(
                description="Cloud tokens for TSP 'mastercard' are not allowed"
            )
        )

    @pytest.mark.asyncio
    async def test_card_not_found(self, call_action):
        with pytest.raises(CoreCardNotFoundError):
            await call_action(trust_card_id='nonexistent')

    @pytest.mark.asyncio
    async def test_card_not_found_in_trust(self, call_action):
        with pytest.raises(CoreCardNotFoundError):
            await call_action(trust_card_id='card-x0000')
