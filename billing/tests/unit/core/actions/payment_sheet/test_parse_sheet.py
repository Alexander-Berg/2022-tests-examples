import logging
import uuid
from dataclasses import replace
from datetime import timedelta
from decimal import Decimal

import pytest
from pay.lib.entities.payment_sheet import PaymentOrder, PaymentOrderTotal

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.payment_sheet.parse_sheet import ParseSheetAction
from billing.yandex_pay.yandex_pay.core.actions.payment_sheet.validate_sheet import (
    ValidatePaymentSheetAction, ValidatePaymentSheetResult
)
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.card import Card, ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork, TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import (
    ParsedPaymentSheet, PaymentMerchant, PaymentMethod, PaymentSheet
)
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreCardNotFoundError, CoreForbiddenRegionError, CoreInvalidAuthMethodError
)
from billing.yandex_pay.yandex_pay.interactions.trust_payments import TrustPaymentMethod, TrustPaymentsClient
from billing.yandex_pay.yandex_pay.interactions.trust_paysys import TrustPaysysCardInfo, TrustPaysysClient
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank

OWNER_UID = 5555
TRUST_CARD_ID = 'card-x1a1234567a12abcd12345a1a'
PREDEFINED_CARD_ID = 'aaf024bb-2f0e-4cad-9010-40328ffcae9a'
PREDEFINED_EXPIRED_CARD_ID = '1c20ab5a-813b-470c-bfa6-1300033ac623'
PREDEFINED_CARD_WITHOUT_TOKEN_ID = '2e09d3f8-48aa-45db-94fe-28a42302b9f5'
PREDEFINED_CARD_WITH_SUSPENDED_TOKEN_ID = 'e3dc2b14-33d9-49a0-a49e-889aab28b786'
PREDEFINED_CARD_WITH_EXPIRED_TOKEN_ID = 'ee7801cd-a488-40e3-bd73-4adc12dde58e'
PREDEFINED_VISA_CARD_ID = 'cfebea81-b1ab-4863-96ff-030af1e18611'
MERCHANT_ID = uuid.UUID('789b29e6-d8f2-4e14-8c3f-33679ca590e3')
MERCHANT_ORIGIN = 'https://market.yandex.ru:443'
PSP_EXTERNAL_ID = 'yandex-trust'
TOKEN_MOCK = 'le-mo-na-de'
FORBIDDEN_CARD_NETWORKS = {CardNetwork.MASTERCARD, CardNetwork.VISA}
MAX_PAYMENT_SHEET_AMOUNT = 1000
ALIEN_TRUST_CARD_ID = 'card-x555111444111222'


@pytest.fixture
def user():
    return User(OWNER_UID)


@pytest.fixture
def correct_sheet():
    return PaymentSheet(
        merchant=PaymentMerchant(
            id=MERCHANT_ID,
            name='merchant-name',
        ),
        version=2,
        currency_code='rub',
        country_code='ru',
        payment_methods=[
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway=PSP_EXTERNAL_ID,
                gateway_merchant_id='yandex-payments',
                allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
        ],
        order=PaymentOrder(
            id='order-id',
            total=PaymentOrderTotal(
                amount=Decimal('1.00'),
            ),
        ),
    )


@pytest.fixture
def correct_sheet_pan_only():
    return PaymentSheet(
        merchant=PaymentMerchant(
            id=MERCHANT_ID,
            name='merchant-name',
        ),
        version=2,
        currency_code='RUB',
        country_code='RU',
        payment_methods=[
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway=PSP_EXTERNAL_ID,
                gateway_merchant_id='yandex-payments',
                allowed_auth_methods=[AuthMethod.PAN_ONLY],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
        ],
        order=PaymentOrder(
            id='order-id',
            total=PaymentOrderTotal(
                amount=Decimal('1.00'),
            ),
        ),
    )


@pytest.fixture(autouse=True)
def set_max_payment_sheet_amount(yandex_pay_settings):
    yandex_pay_settings.API_MAX_PAYMENT_SHEET_AMOUNT = MAX_PAYMENT_SHEET_AMOUNT


@pytest.fixture
def paysys_card_info():
    return TrustPaysysCardInfo(
        card_id='1',
        card_token='card-token',
        holder='Holder',
        expiration_year=2000,
        expiration_month=1
    )


@pytest.fixture(autouse=True)
def mock_trust_paysys_get_card(mocker, paysys_card_info):
    return mocker.patch.object(
        TrustPaysysClient,
        'get_card',
        mocker.AsyncMock(
            return_value=paysys_card_info,
        ),
    )


@pytest.fixture
def payment_method():
    return TrustPaymentMethod(
        id=TRUST_CARD_ID,
        card_id=TRUST_CARD_ID,
        binding_systems=['trust'],
        orig_uid=str(OWNER_UID),
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


@pytest.fixture
def user_card(payment_method):
    return UserCard(
        card_id=TRUST_CARD_ID,
        owner_uid=OWNER_UID,
        last4=payment_method.last4,
        card_network=CardNetwork.MASTERCARD,
        bin=payment_method.bin,
        allowed_auth_methods=[AuthMethod.PAN_ONLY],
        last_paid=payment_method.last_paid_ts,
        created=payment_method.binding_ts,
        trust_payment_method=payment_method,
        trust_binding=None,
        issuer_bank=IssuerBank.SBERBANK,
        expiration_date=ExpirationDate(
            month=9,
            year=2099,
        ),
        trust_card_id=TRUST_CARD_ID,
    )


@pytest.fixture(autouse=True)
def mock_trust_gateway_lpm(mocker, payment_method):
    return mocker.patch.object(
        TrustPaymentsClient,
        'get_payment_methods',
        mocker.AsyncMock(
            return_value=[payment_method],
        ),
    )


@pytest.fixture
def card_expiration_date():
    return utcnow() + timedelta(days=365)


@pytest.fixture(autouse=True)
async def card(storage, card_expiration_date):
    return await storage.card.create(
        Card(
            trust_card_id=TRUST_CARD_ID,
            owner_uid=OWNER_UID,
            tsp=TSPType.MASTERCARD,
            expire=card_expiration_date,
            last4='0000',
            card_id=uuid.UUID(PREDEFINED_CARD_ID),
        )
    )


@pytest.fixture(autouse=True)
async def expired_card(storage):
    return await storage.card.create(
        Card(
            trust_card_id='trust-card-expired',
            owner_uid=OWNER_UID,
            tsp=TSPType.MASTERCARD,
            expire=utcnow(),
            last4='0000',
            card_id=uuid.UUID(PREDEFINED_EXPIRED_CARD_ID),
        )
    )


@pytest.fixture(autouse=True)
async def card_without_token(storage, card_expiration_date):
    return await storage.card.create(
        Card(
            trust_card_id='trust-card-without-token-id',
            owner_uid=OWNER_UID,
            tsp=TSPType.MASTERCARD,
            expire=card_expiration_date,
            last4='0000',
            card_id=uuid.UUID(PREDEFINED_CARD_WITHOUT_TOKEN_ID),
        )
    )


@pytest.fixture(autouse=True)
async def card_with_suspended_token(storage, card_expiration_date):
    return await storage.card.create(
        Card(
            trust_card_id='trust-card-suspended-token-id',
            owner_uid=OWNER_UID,
            tsp=TSPType.MASTERCARD,
            expire=card_expiration_date,
            last4='0000',
            card_id=uuid.UUID(PREDEFINED_CARD_WITH_SUSPENDED_TOKEN_ID),
        )
    )


@pytest.fixture(autouse=True)
async def card_with_expired_token(storage, card_expiration_date):
    return await storage.card.create(
        Card(
            trust_card_id='trust-card-expired-token-id',
            owner_uid=OWNER_UID,
            tsp=TSPType.MASTERCARD,
            expire=card_expiration_date,
            last4='0000',
            card_id=uuid.UUID(PREDEFINED_CARD_WITH_EXPIRED_TOKEN_ID),
        )
    )


@pytest.fixture(autouse=True)
async def visa_card(storage, card_expiration_date):
    return await storage.card.create(
        Card(
            trust_card_id='trust-visa-card-id',
            owner_uid=OWNER_UID,
            tsp=TSPType.VISA,
            expire=card_expiration_date,
            last4='0000',
            card_id=uuid.UUID(PREDEFINED_VISA_CARD_ID),
        )
    )


@pytest.fixture(autouse=True)
async def enrollment_ecommerce(storage, card, card_expiration_date):
    return await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_token_status=TSPTokenStatus.ACTIVE,
            tsp_card_id=None,
            tsp_token_id=str(uuid.uuid4()),
            expire=card_expiration_date,
            card_last4=card.last4,
        )
    )


@pytest.fixture(autouse=True)
async def enrollment_ecommerce_suspended(storage, card_with_suspended_token, card_expiration_date):
    return await storage.enrollment.create(
        Enrollment(
            card_id=card_with_suspended_token.card_id,
            merchant_id=None,
            tsp_token_status=TSPTokenStatus.SUSPENDED,
            tsp_card_id=None,
            tsp_token_id=str(uuid.uuid4()),
            expire=card_expiration_date,
            card_last4=card_with_suspended_token.last4,
        )
    )


@pytest.fixture(autouse=True)
async def enrollment_ecommerce_expired(storage, card_with_expired_token):
    return await storage.enrollment.create(
        Enrollment(
            card_id=card_with_expired_token.card_id,
            merchant_id=None,
            tsp_token_status=TSPTokenStatus.ACTIVE,
            tsp_card_id=None,
            tsp_token_id=str(uuid.uuid4()),
            expire=utcnow(),
            card_last4=card_with_expired_token.last4,
        )
    )


@pytest.fixture(autouse=True)
async def merchant(storage):
    return await storage.merchant.create(
        Merchant(
            merchant_id=MERCHANT_ID,
            name='the-name',
        )
    )


@pytest.fixture(autouse=True)
async def psp(storage):
    return await create_psp_entity(
        storage,
        PSP(
            psp_id=uuid.uuid4(),
            psp_external_id=PSP_EXTERNAL_ID,
            public_key='public-key',
            public_key_signature='public-key-signature',
        )
    )


@pytest.mark.asyncio
async def test_success_db_card_id(
    correct_sheet: PaymentSheet,
    card: Card,
    enrollment_ecommerce: Enrollment,
    psp: PSP,
    payment_method,
    user,
    user_card,
    dummy_logs,
):
    """
    Успех, если перед ключ карты в базе.
    """
    result = await ParseSheetAction(
        user=user,
        card_id=PREDEFINED_CARD_ID,
        sheet=correct_sheet,
        merchant_origin=MERCHANT_ORIGIN,
    ).run()

    card_with_enrollment = replace(card, enrollment=enrollment_ecommerce)
    user_card = replace(
        user_card,
        card=card_with_enrollment,
        card_id=PREDEFINED_CARD_ID,
        last4=card_with_enrollment.last4,
        allowed_auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
    )

    assert_that(
        result,
        equal_to(
            ParsedPaymentSheet(
                psp=psp,
                amount=int(correct_sheet.order.total.amount * 100),
                user_card=user_card,
                auth_methods=[AuthMethod.CLOUD_TOKEN],
                payment_method_type=PaymentMethodType.CARD,
            )
        ),
    )

    assert_that(
        dummy_logs(),
        has_item(
            has_properties(
                message='Payment sheet parsed',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    card_id=PREDEFINED_CARD_ID,
                    sheet=correct_sheet,
                    payment_method_type=PaymentMethodType.CARD,
                    merchant_origin=MERCHANT_ORIGIN,
                    validate_origin=True,
                    forbidden_card_networks=None,
                )
            )
        )
    )


@pytest.mark.asyncio
async def test_success_trust_api_card_id(
    correct_sheet_pan_only: PaymentSheet,
    psp: PSP,
    paysys_card_info,
    payment_method,
    user,
    user_card,
    dummy_logs,
):
    """
    Успех, если передан ключ карты в API Trust.
    """
    result = await ParseSheetAction(
        user=user,
        card_id=TRUST_CARD_ID,
        sheet=correct_sheet_pan_only,
        merchant_origin=MERCHANT_ORIGIN,
    ).run()

    assert_that(
        result,
        equal_to(
            ParsedPaymentSheet(
                psp=psp,
                amount=int(correct_sheet_pan_only.order.total.amount * 100),
                user_card=user_card,
                auth_methods=[AuthMethod.PAN_ONLY],
                payment_method_type=PaymentMethodType.CARD,
            )
        ),
    )

    assert_that(
        dummy_logs(),
        has_item(
            has_properties(
                message='Payment sheet parsed',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    card_id=TRUST_CARD_ID,
                    sheet=correct_sheet_pan_only,
                    payment_method_type=PaymentMethodType.CARD,
                    merchant_origin=MERCHANT_ORIGIN,
                    validate_origin=True,
                    forbidden_card_networks=None,
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'payment_method_type',
    [each for each in PaymentMethodType if each != PaymentMethodType.CASH],
)
async def test_forbidden_card_network(
    correct_sheet_pan_only: PaymentSheet,
    psp: PSP,
    paysys_card_info,
    payment_method,
    user,
    dummy_logs,
    payment_method_type,
):
    with pytest.raises(CoreForbiddenRegionError):
        await ParseSheetAction(
            user=user,
            card_id=TRUST_CARD_ID,
            sheet=correct_sheet_pan_only,
            merchant_origin=MERCHANT_ORIGIN,
            forbidden_card_networks=FORBIDDEN_CARD_NETWORKS,
            payment_method_type=payment_method_type,
        ).run()

    assert_that(
        dummy_logs(),
        has_item(
            has_properties(
                message='Forbidden region',
                levelno=logging.ERROR,
                _context=has_entries(
                    uid=user.uid,
                    card_id=TRUST_CARD_ID,
                    sheet=correct_sheet_pan_only,
                    payment_method_type=payment_method_type,
                    merchant_origin=MERCHANT_ORIGIN,
                    validate_origin=True,
                    forbidden_card_networks=FORBIDDEN_CARD_NETWORKS,
                )
            )
        )
    )


@pytest.mark.asyncio
async def test_missing_card_allowed_for_cash(correct_sheet: PaymentSheet, user):
    await ParseSheetAction(
        user=user,
        card_id=None,
        sheet=correct_sheet,
        merchant_origin=MERCHANT_ORIGIN,
        payment_method_type=PaymentMethodType.CASH,
    ).run()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'payment_method_type',
    [each for each in PaymentMethodType if each != PaymentMethodType.CASH],
)
async def test_missing_card_forbidden_for_non_cash_methods(
    correct_sheet: PaymentSheet, user, payment_method_type,
):
    with pytest.raises(CoreCardNotFoundError):
        await ParseSheetAction(
            user=user,
            card_id=None,
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
            payment_method_type=payment_method_type,
        ).run()


@pytest.mark.asyncio
async def test_calls_validate_action(
    correct_sheet: PaymentSheet,
    card: Card,
    enrollment_ecommerce: Enrollment,
    psp: PSP,
    payment_method,
    user,
    mock_action
):
    mock = mock_action(ValidatePaymentSheetAction, ValidatePaymentSheetResult(psp=psp, normalized_amount=100))

    await ParseSheetAction(
        user=user,
        card_id=PREDEFINED_CARD_ID,
        sheet=correct_sheet,
        merchant_origin=MERCHANT_ORIGIN,
        validate_origin=True,
    ).run()

    mock.assert_called_with(sheet=correct_sheet, merchant_origin=MERCHANT_ORIGIN, validate_origin=True)


class TestGetUserCard:
    @pytest.mark.asyncio
    async def test_when_trust_card_id_supplied__calls_lpm(
        self, mock_trust_gateway_lpm, correct_sheet_pan_only, user
    ):
        await ParseSheetAction(
            user=user,
            card_id=TRUST_CARD_ID,
            sheet=correct_sheet_pan_only,
        ).run()

        mock_trust_gateway_lpm.assert_called_once_with(uid=user.uid)

    @pytest.mark.asyncio
    async def test_when_yandexpay_card_id_supplied_but_method_is_panonly__calls_lpm(
        self, correct_sheet_pan_only, mock_trust_gateway_lpm, user
    ):
        await ParseSheetAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=correct_sheet_pan_only,
        ).run()

        mock_trust_gateway_lpm.assert_called_once_with(uid=user.uid)

    @pytest.mark.asyncio
    async def test_error_because_of_not_owned_card(self, correct_sheet):
        with pytest.raises(CoreCardNotFoundError):
            await ParseSheetAction(
                user=User(OWNER_UID + 1),
                card_id=PREDEFINED_CARD_ID,
                sheet=correct_sheet,
            ).run()

    @pytest.mark.asyncio
    async def test_error_because_of_unknown_card_id(self, correct_sheet, user):
        with pytest.raises(CoreCardNotFoundError):
            await ParseSheetAction(
                user=user,
                card_id=str(uuid.uuid4()),
                sheet=correct_sheet,
            ).run()

    @pytest.mark.asyncio
    async def test_pan_only_card_not_found_in_trust(self, correct_sheet_pan_only, user):
        with pytest.raises(CoreCardNotFoundError):
            await ParseSheetAction(
                user=user,
                card_id=ALIEN_TRUST_CARD_ID,
                sheet=correct_sheet_pan_only,
            ).run()

    @pytest.mark.asyncio
    async def test_pan_only_fails_if_card_in_pay_db_expired(
        self, correct_sheet_pan_only, user
    ):
        with pytest.raises(CoreCardNotFoundError):
            await ParseSheetAction(
                user=user,
                card_id=PREDEFINED_EXPIRED_CARD_ID,
                sheet=correct_sheet_pan_only,
            ).run()


class TestChooseAuthMethodInParseSheetAction:
    @pytest.mark.asyncio
    async def test_error_because_card_has_no_token(self, correct_sheet, user, dummy_logs):
        """
        Запрошен способ авторизации CLOUD_TOKEN, однако у карты вообще нет токена в базе.
        """
        with pytest.raises(CoreInvalidAuthMethodError):
            await ParseSheetAction(
                user=user,
                card_id=PREDEFINED_CARD_WITHOUT_TOKEN_ID,
                sheet=correct_sheet,
            ).run()

        assert_that(
            dummy_logs(),
            has_item(
                has_properties(
                    message='No applicable auth methods',
                    levelno=logging.WARNING,
                    _context=has_entries(
                        uid=user.uid,
                        card_id=PREDEFINED_CARD_WITHOUT_TOKEN_ID,
                        sheet=correct_sheet,
                        payment_method_type=PaymentMethodType.CARD,
                        merchant_origin=None,
                        validate_origin=True,
                        forbidden_card_networks=None,
                    )
                )
            )
        )

    @pytest.mark.asyncio
    async def test_error_because_card_token_is_not_active(self, correct_sheet, user):
        """
        Запрошен способ авторизации CLOUD_TOKEN, но у карты токен не активен.
        """
        with pytest.raises(CoreInvalidAuthMethodError):
            await ParseSheetAction(
                user=user,
                card_id=PREDEFINED_CARD_WITH_SUSPENDED_TOKEN_ID,
                sheet=correct_sheet,
            ).run()

    @pytest.mark.asyncio
    async def test_error_because_card_token_expired(self, correct_sheet, user):
        """
        Запрошен способ авторизации CLOUD_TOKEN, но у карты токен истек.
        """
        with pytest.raises(CoreInvalidAuthMethodError):
            await ParseSheetAction(
                user=user,
                card_id=PREDEFINED_CARD_WITH_EXPIRED_TOKEN_ID,
                sheet=correct_sheet,
            ).run()

    @pytest.mark.asyncio
    async def test_error_because_card_network_not_matches_payment_methods(
        self, correct_sheet, user
    ):
        """
        Передали карту платежной системы, которой нет в запрошенных payment_methods.
        """
        correct_sheet.payment_methods[0].allowed_card_networks = [CardNetwork.MASTERCARD]
        with pytest.raises(CoreInvalidAuthMethodError):
            await ParseSheetAction(
                user=user,
                card_id=PREDEFINED_VISA_CARD_ID,
                sheet=correct_sheet,
            ).run()

    @pytest.mark.asyncio
    async def test_error_when_card_is_expired_for_pan_only_card(
        self, correct_sheet_pan_only, psp, mocker, payment_method, user
    ):
        """
        Карты нет в базе, но есть в Трасте, однако флажок expired установлен.
        Нельзя выбрать метод авторизации PAN_ONLY.
        """
        payment_method.expired = True
        mocker.patch.object(
            TrustPaymentsClient,
            'get_payment_methods',
            mocker.AsyncMock(
                return_value=[payment_method],
            ),
        )

        with pytest.raises(CoreInvalidAuthMethodError):
            await ParseSheetAction(
                user=user,
                card_id=TRUST_CARD_ID,
                sheet=correct_sheet_pan_only,
            ).run()
