import logging
from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import pytest
from cryptography.fernet import Fernet
from freezegun import freeze_time
from pay.lib.entities.payment_token import MITInfo

from sendr_core.exceptions import CoreFailError
from sendr_pytest.helpers import ensure_all_fields
from sendr_utils import utcnow

from hamcrest import (
    assert_that, equal_to, has_entries, has_items, has_properties, has_string, instance_of, match_equality
)

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.card.get import GetUserCardByCardIdAction
from billing.yandex_pay.yandex_pay.core.actions.payment_token.cloud_token import CreateCloudTokenAction
from billing.yandex_pay.yandex_pay.core.actions.payment_token.internal.checkout import (
    CreateInternalCheckoutPaymentTokenAction, InternalCheckoutResponse
)
from billing.yandex_pay.yandex_pay.core.actions.payment_token.pan_token import CreatePANTokenAction
from billing.yandex_pay.yandex_pay.core.amount import normalize_amount
from billing.yandex_pay.yandex_pay.core.entities.card import Card, ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.checkout import PaymentMethodInfo
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork, TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.message import Message
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreCardNotFoundError, CoreEnrollmentNotFoundError, CoreInvalidAmountError, CoreInvalidCurrencyError,
    CoreNoAvailableAuthMethodsError, CorePSPAccountError, CorePSPNotFoundError
)
from billing.yandex_pay.yandex_pay.interactions.trust_payments import TrustPaymentMethod
from billing.yandex_pay.yandex_pay.tests.utils import correct_uuid4
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank
from billing.yandex_pay.yandex_pay.utils.stats import unknown_trust_payment_system

GATEWAY_MERCHANT_ID = 'gateway_merchant_id'
CURRENCY = 'XTS'
AMOUNT = Decimal('1000')


@pytest.fixture
def user(randn):
    return User(randn())


@pytest.fixture
def pan_token_mock(rands):
    return rands()


@pytest.fixture
def cloud_token_mock(rands):
    return rands()


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def card_id():
    return uuid4()


@pytest.fixture
def enrollment(card_id, rands):
    return Enrollment(
        card_id=card_id,
        merchant_id=None,
        tsp_card_id=None,
        tsp_token_id=rands(),
        tsp_token_status=TSPTokenStatus.ACTIVE,
        card_last4='0000',
    )


@pytest.fixture
def card(card_id, enrollment: Enrollment):
    return Card(
        trust_card_id='trust_card_id',
        owner_uid=123,
        tsp=TSPType.MASTERCARD,
        expire=utcnow(),
        last4='0000',
        card_id=card_id,
        enrollment=enrollment,
    )


@pytest.fixture
def user_card(user, card):
    return UserCard(
        card_id='some_id',
        owner_uid=user.uid,
        card_network=CardNetwork.MASTERCARD,
        last4=card.last4,
        allowed_auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
        created=utcnow(),
        last_paid=utcnow(),
        issuer_bank=IssuerBank.ALFABANK,
        expiration_date=ExpirationDate(
            month=1,
            year=utcnow().year + 3,
        ),
        card=card,
        trust_card_id=card.trust_card_id,
    )


@pytest.fixture
def auth_methods():
    return [AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN]


@pytest.fixture
def mit_info():
    return MITInfo(recurring=True)


@pytest.fixture
def action_kwargs(user, merchant_id, card, auth_methods, psp, mit_info):
    return dict(
        user=user,
        gateway_merchant_id=GATEWAY_MERCHANT_ID,
        merchant_id=merchant_id,
        card_id=str(card.card_id),
        currency=CURRENCY,
        amount=AMOUNT,
        auth_methods=auth_methods,
        psp_external_id=psp.psp_external_id,
        mit_info=mit_info,
    )


@pytest.fixture
def call_action(action_kwargs):
    async def _inner(**kwargs):
        kwargs = action_kwargs | kwargs
        return await CreateInternalCheckoutPaymentTokenAction(**kwargs).run()

    return _inner


@pytest.fixture(autouse=True)
def mock_create_pan_token(mock_action, pan_token_mock):
    return mock_action(CreatePANTokenAction, pan_token_mock)


@pytest.fixture(autouse=True)
def mock_create_cloud_token(mock_action, cloud_token_mock):
    return mock_action(CreateCloudTokenAction, cloud_token_mock)


@pytest.fixture(autouse=True)
def mock_get_user_card(mock_action, user_card):
    return mock_action(GetUserCardByCardIdAction, user_card)


@pytest.fixture(autouse=True)
async def psp(storage) -> PSP:
    return await storage.psp.create(
        PSP(
            psp_external_id='psp_external_id',
            psp_id=uuid4(),
            public_key='pubkey',
            public_key_signature='pubkeysig',
        )
    )


@pytest.mark.asyncio
async def test_unknown_trust_payment_system(call_action, user_card, mocker, dummy_logs):
    user_card.card = None
    unknown_payment_system = 'Unknown_payment_system'
    user_card.trust_payment_method = mocker.Mock(
        spec=TrustPaymentMethod,
        payment_system=unknown_payment_system,
    )
    before = unknown_trust_payment_system.get()

    await call_action()

    after = unknown_trust_payment_system.get()
    assert_that(after[0][1] - before[0][1], equal_to(1))

    logs = dummy_logs()
    assert_that(
        logs,
        has_items(
            has_properties(
                message='Unknown card_network from trust',
                levelno=logging.ERROR,
                _context=has_entries(
                    card_network=unknown_payment_system
                )
            )
        ),
    )


class TestInternalCheckoutFails:
    @pytest.mark.asyncio
    async def test_auth_methods_are_empty(self, call_action):
        with pytest.raises(CoreNoAvailableAuthMethodsError):
            await call_action(auth_methods=[])

    @pytest.mark.asyncio
    async def test_psp_not_specified(self, action_kwargs, call_action):
        action_kwargs.pop('psp_external_id')

        with pytest.raises(CorePSPNotFoundError):
            await call_action()

    @pytest.mark.asyncio
    async def test_psp_not_found(self, call_action):
        with pytest.raises(CorePSPNotFoundError):
            await call_action(psp_external_id='non-existent')

    @pytest.mark.asyncio
    async def test_psp_is_blocked(self, storage, psp, call_action):
        psp.is_blocked = True
        await storage.psp.save(psp)

        with pytest.raises(CorePSPAccountError):
            await call_action()

    @pytest.mark.asyncio
    async def test_card_does_not_belong_to_user(self, user_card, call_action):
        user_card.owner_uid += 1

        with pytest.raises(CoreFailError) as exc_info:
            await call_action(user_card=user_card)

        # this is critically important error since this is the only place
        # in CreateInternalCheckoutPaymentTokenAction where we ensure
        # the card belongs to the user; hence worth having a regression test,
        # even though we don't normally test asserts
        assert_that(exc_info.value.__cause__, instance_of(AssertionError))


class TestCloudToken:
    @pytest.fixture(autouse=True)
    def ensure_pan_checkout_wont_work(self, mock_create_pan_token):
        mock_create_pan_token.side_effect = Exception

    @pytest.fixture
    def auth_methods(self):
        return [AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY]

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'auth_methods',
        (
            [AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
            [AuthMethod.CLOUD_TOKEN],
        )
    )
    @pytest.mark.parametrize('user_card_passed', [True, False])
    async def test_returned(
        self,
        card,
        cloud_token_mock,
        call_action,
        action_kwargs,
        user_card,
        auth_methods,
        user_card_passed,
    ):
        expected = InternalCheckoutResponse(
            payment_method_info=PaymentMethodInfo(
                method_type=PaymentMethodType.CARD,
                payment_token=cloud_token_mock,
                auth_method=AuthMethod.CLOUD_TOKEN,
                card_last4=card.last4,
                card_network=CardNetwork.from_tsp_type(card.tsp),
                mit_info=MITInfo(recurring=True),
            ),
            message=match_equality(instance_of(Message)),
        )
        action_kwargs['auth_methods'] = auth_methods
        if user_card_passed:
            action_kwargs.pop('card_id')
            action_kwargs['user_card'] = user_card

        returned = await call_action()

        assert_that(returned, equal_to(expected))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('user_card_passed', [True, False])
    async def test_get_user_card_action_called(
        self,
        call_action,
        action_kwargs,
        user_card,
        user_card_passed,
        mock_get_user_card,
    ):
        if user_card_passed:
            action_kwargs.pop('card_id')
            action_kwargs['user_card'] = user_card

        await call_action()

        assert_that(mock_get_user_card.call_count, equal_to(int(not user_card_passed)))

    @pytest.mark.asyncio
    async def test_action_call_logged(
        self,
        call_action,
        user,
        dummy_logs,
        product_logs,
        psp,
        user_card,
        action_kwargs,
    ):
        returned = await call_action()

        [log] = dummy_logs()
        assert_that(
            log,
            has_properties(
                message='INTERNAL_CHECKOUT_PAYMENT_TOKEN_REQUESTED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    gateway_merchant_id=GATEWAY_MERCHANT_ID,
                    merchant_id=action_kwargs['merchant_id'],
                    currency=CURRENCY,
                    amount=AMOUNT,
                    auth_methods=action_kwargs['auth_methods'],
                ),
            ),
        )

        [product_log] = product_logs()
        assert_that(
            product_log,
            has_properties(
                message='INTERNAL_CHECKOUT_PAYMENT_TOKEN_ISSUED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    gateway_merchant_id=GATEWAY_MERCHANT_ID,
                    merchant_id=action_kwargs['merchant_id'],
                    currency=CURRENCY,
                    amount=AMOUNT,
                    auth_methods=action_kwargs['auth_methods'],
                    auth_method=AuthMethod.CLOUD_TOKEN,
                    psp_id=psp.psp_id,
                    user_card=user_card,
                    message_id=returned['message'].message_id,
                    message_expires_at=returned['message'].expires_at,
                ),
            ),
        )

    @pytest.mark.asyncio
    @freeze_time('2021-12-31')
    async def test_calls_create_cloud_token_with_correct_message_expiration(
        self,
        yandex_pay_settings,
        mock_create_cloud_token,
        call_action,
    ):
        """При выписывании токена через утку, корректно генерируем время действия токена."""
        token_lifespan = 777
        yandex_pay_settings.API_PAYMENT_TOKEN_LIFESPAN_SECONDS = token_lifespan
        expected_expiration = utcnow() + timedelta(seconds=token_lifespan)

        result = await call_action()

        mock_create_cloud_token.assert_called_once()
        message = mock_create_cloud_token.call_args.kwargs['message']
        assert_that(message.expires_at, equal_to(expected_expiration))

        assert_that(result['message'], equal_to(message))

    @pytest.mark.asyncio
    async def test_calls_duckgo_service_with_correct_message_id(
        self,
        yandex_pay_settings,
        call_action,
        user,
    ):
        """Выписываем токен через утку, генерируя корректный message_id."""
        fernet = Fernet(yandex_pay_settings.DUCKGO_FERNET_KEY)

        result = await call_action()

        message = result['message']
        # отбрасываем версию и достаем часть "{}:{}".format(uid, guid)
        decrypted = fernet.decrypt(message.message_id.split(':')[1].encode()).decode()
        uid, guid = decrypted.split(':')
        assert_that(int(uid), equal_to(user.uid))
        assert_that(correct_uuid4(guid), equal_to(True))

    @pytest.mark.asyncio
    async def test_when_no_mit_info_supplied__passes_default_mit_info(
        self,
        yandex_pay_settings,
        mock_create_cloud_token,
        call_action,
        action_kwargs,
    ):
        del action_kwargs['mit_info']
        await call_action(**action_kwargs)

        mit_info = mock_create_cloud_token.call_args.kwargs['mit_info']
        assert_that(
            mit_info,
            equal_to(
                ensure_all_fields(MITInfo)(
                    recurring=False,
                    deferred=False,
                )
            )
        )

    @pytest.mark.asyncio
    async def test_calls_create_cloud_token_action(
        self,
        mock_create_cloud_token,
        card,
        psp,
        mit_info,
        call_action,
    ):
        await call_action()

        mock_create_cloud_token.assert_called_once_with(
            tsp=TSPType.MASTERCARD,
            enrollment=card.enrollment,
            psp=psp,
            amount=normalize_amount(AMOUNT, CURRENCY),
            currency=CURRENCY,
            mit_info=mit_info,
            gateway_merchant_id=GATEWAY_MERCHANT_ID,
            message=match_equality(instance_of(Message)),
        )

    @pytest.mark.asyncio
    async def test_should_fallback_to_cloud_token(
        self,
        mock_create_pan_token,
        card,
        cloud_token_mock,
        call_action,
    ):
        expected = InternalCheckoutResponse(
            payment_method_info=PaymentMethodInfo(
                method_type=PaymentMethodType.CARD,
                payment_token=cloud_token_mock,
                auth_method=AuthMethod.CLOUD_TOKEN,
                card_last4=card.last4,
                card_network=CardNetwork.from_tsp_type(card.tsp),
                mit_info=MITInfo(recurring=True),
            ),
            message=match_equality(instance_of(Message)),
        )

        result = await call_action(auth_methods=[AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN])

        mock_create_pan_token.assert_called_once()
        assert_that(result, equal_to(expected))

    @pytest.mark.asyncio
    async def test_checkout_should_fail_no_fallback_auth_method_set(
        self,
        mock_create_cloud_token,
        dummy_logs,
        call_action,
    ):
        mock_create_cloud_token.side_effect = Exception('cloud token fails')

        with pytest.raises(CoreFailError) as exc_info:
            await call_action(auth_methods=[AuthMethod.CLOUD_TOKEN])

        assert_that(exc_info.value.__cause__, has_string('cloud token fails'))

        logs = dummy_logs()
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='TOKEN_ISSUE_ATTEMPT_FAILED',
                    levelno=logging.ERROR,
                )
            )
        )

    @pytest.mark.asyncio
    async def test_enrollment_not_found(self, card, call_action):
        card.enrollment = None

        with pytest.raises(CoreEnrollmentNotFoundError):
            await call_action(auth_methods=[AuthMethod.CLOUD_TOKEN])

    @pytest.mark.asyncio
    async def test_card_not_found(self, user_card, call_action):
        user_card.card = None

        with pytest.raises(CoreCardNotFoundError):
            await call_action(auth_methods=[AuthMethod.CLOUD_TOKEN])

    @pytest.mark.asyncio
    async def test_invalid_currency(self, call_action):
        with pytest.raises(CoreInvalidCurrencyError):
            await call_action(currency='not a currency')

    @pytest.mark.asyncio
    @pytest.mark.parametrize('amount', [Decimal('0'), Decimal('-1'), Decimal('10.234')])
    async def test_invalid_amount(self, call_action, amount):
        with pytest.raises(CoreInvalidAmountError):
            await call_action(amount=amount, mit_info=MITInfo())

    @pytest.mark.asyncio
    @pytest.mark.parametrize('mit_info', [MITInfo(recurring=True), MITInfo(deferred=True)])
    async def test_zero_amount_is_valid_when_mit_info_not_empty(self, call_action, mit_info):
        await call_action(amount=Decimal('0'), mit_info=mit_info)


class TestPANOnly:
    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'auth_methods',
        (
            [AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN],
            [AuthMethod.PAN_ONLY],
        )
    )
    @pytest.mark.parametrize('user_card_passed', [True, False])
    async def test_returned(
        self,
        card,
        pan_token_mock,
        call_action,
        action_kwargs,
        user_card,
        auth_methods,
        user_card_passed,
    ):
        expected = InternalCheckoutResponse(
            payment_method_info=PaymentMethodInfo(
                method_type=PaymentMethodType.CARD,
                payment_token=pan_token_mock,
                auth_method=AuthMethod.PAN_ONLY,
                card_last4=card.last4,
                card_network=CardNetwork.from_tsp_type(card.tsp),
                mit_info=MITInfo(recurring=True),
            ),
            message=match_equality(instance_of(Message)),
        )
        action_kwargs['auth_methods'] = auth_methods
        if user_card_passed:
            action_kwargs.pop('card_id')
            action_kwargs['user_card'] = user_card

        returned = await call_action()

        assert_that(returned, equal_to(expected))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('user_card_passed', [True, False])
    async def test_get_user_card_action_called(
        self,
        call_action,
        action_kwargs,
        user_card,
        user_card_passed,
        mock_get_user_card,
    ):
        if user_card_passed:
            action_kwargs.pop('card_id')
            action_kwargs['user_card'] = user_card

        await call_action()

        assert_that(mock_get_user_card.call_count, equal_to(int(not user_card_passed)))

    @pytest.mark.asyncio
    async def test_calls_create_pan_token_action(
        self,
        mock_create_pan_token,
        card,
        psp,
        mit_info,
        call_action,
    ):
        await call_action()

        mock_create_pan_token.assert_called_once_with(
            trust_card_id='trust_card_id',
            psp=psp,
            amount=normalize_amount(AMOUNT, CURRENCY),
            currency=CURRENCY,
            mit_info=mit_info,
            gateway_merchant_id=GATEWAY_MERCHANT_ID,
            message=match_equality(instance_of(Message)),
        )

    @pytest.mark.asyncio
    @freeze_time('2021-12-31')
    async def test_calls_pan_token_action_with_correct_message(
        self,
        yandex_pay_settings,
        mock_create_pan_token,
        call_action,
    ):
        """При выписывании токена через cardproxy, корректно генерируем время действия токена."""
        token_lifespan = 777
        yandex_pay_settings.API_PAYMENT_TOKEN_LIFESPAN_SECONDS = token_lifespan
        expected_expiration = utcnow() + timedelta(seconds=token_lifespan)

        result = await call_action()

        message = result['message']
        mock_create_pan_token.assert_called_once()
        assert_that(
            mock_create_pan_token.call_args.kwargs,
            has_entries(message=message),
        )

        assert_that(message.expires_at, equal_to(expected_expiration))

    @pytest.mark.asyncio
    async def test_calls_generates_correct_message(
        self,
        yandex_pay_settings,
        call_action,
        user,
    ):
        """Выписываем токен, генерируя корректный message_id."""
        fernet = Fernet(yandex_pay_settings.DUCKGO_FERNET_KEY)

        result = await call_action()

        message = result['message']
        # отбрасываем версию и достаем часть "{}:{}".format(uid, guid)
        decrypted = fernet.decrypt(message.message_id.split(':')[1].encode()).decode()
        uid, guid = decrypted.split(':')
        assert_that(int(uid), equal_to(user.uid))
        assert_that(correct_uuid4(guid), equal_to(True))

    @pytest.mark.asyncio
    async def test_when_no_mit_info_supplied__passes_default_mit_info(
        self,
        yandex_pay_settings,
        mock_create_pan_token,
        call_action,
        action_kwargs,
    ):
        del action_kwargs['mit_info']
        await call_action(**action_kwargs)

        mit_info = mock_create_pan_token.call_args.kwargs['mit_info']
        assert_that(
            mit_info,
            equal_to(
                ensure_all_fields(MITInfo)(
                    recurring=False,
                    deferred=False,
                )
            )
        )

    @pytest.mark.asyncio
    async def test_should_fallback_to_pan_only(
        self,
        mock_create_cloud_token,
        card,
        pan_token_mock,
        call_action,
    ):
        mock_create_cloud_token.side_effect = Exception
        expected = InternalCheckoutResponse(
            payment_method_info=PaymentMethodInfo(
                method_type=PaymentMethodType.CARD,
                payment_token=pan_token_mock,
                auth_method=AuthMethod.PAN_ONLY,
                card_last4=card.last4,
                card_network=CardNetwork.from_tsp_type(card.tsp),
                mit_info=MITInfo(recurring=True),
            ),
            message=match_equality(instance_of(Message)),
        )

        result = await call_action(auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY])

        mock_create_cloud_token.assert_called_once()
        assert_that(result, equal_to(expected))

    @pytest.mark.asyncio
    async def test_checkout_should_fail_no_fallback_auth_method_set(
        self,
        mock_create_pan_token,
        dummy_logs,
        call_action,
    ):
        mock_create_pan_token.side_effect = Exception('pan only fails')

        with pytest.raises(CoreFailError) as exc_info:
            await call_action(auth_methods=[AuthMethod.PAN_ONLY])

        assert_that(exc_info.value.__cause__, has_string('pan only fails'))

        logs = dummy_logs()
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='TOKEN_ISSUE_ATTEMPT_FAILED',
                    levelno=logging.ERROR,
                )
            )
        )

    @pytest.mark.asyncio
    async def test_invalid_currency(self, call_action):
        with pytest.raises(CoreInvalidCurrencyError):
            await call_action(currency='not a currency')

    @pytest.mark.asyncio
    @pytest.mark.parametrize('amount', [Decimal('0'), Decimal('-1'), Decimal('10.234')])
    async def test_invalid_amount(self, call_action, amount):
        with pytest.raises(CoreInvalidAmountError):
            await call_action(amount=amount, mit_info=MITInfo())

    @pytest.mark.asyncio
    @pytest.mark.parametrize('mit_info', [MITInfo(recurring=True), MITInfo(deferred=True)])
    async def test_zero_amount_is_valid_when_mit_info_not_empty(self, call_action, mit_info):
        await call_action(amount=Decimal('0'), mit_info=mit_info)
