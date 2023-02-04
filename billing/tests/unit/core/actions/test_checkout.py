import asyncio
import logging
import sys
import uuid
from copy import copy, deepcopy
from dataclasses import replace
from datetime import timedelta
from decimal import Decimal

import pytest
from aiohttp import ClientConnectionError
from pay.lib.entities.enums import ShippingMethodType
from pay.lib.entities.payment_sheet import MITOptionsType, PaymentOrder, PaymentOrderTotal
from pay.lib.entities.payment_token import MITInfo
from pay.lib.interactions.antifraud.entities import Challenge, ChallengeStatus
from pay.lib.interactions.split.entities import YandexSplitOrderCheckoutInfo

from sendr_pytest.helpers import ensure_all_fields
from sendr_utils import alist, utcnow

from hamcrest import (
    assert_that, close_to, contains, equal_to, has_entries, has_item, has_properties, instance_of, match_equality,
    not_none
)

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions import checkout as checkout_module
from billing.yandex_pay.yandex_pay.core.actions.antifraud.cashback import CheckCashbackAllowedAction
from billing.yandex_pay.yandex_pay.core.actions.checkout import CheckoutAction
from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_metadata import UpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.actions.payment_sheet.parse_sheet import ParseSheetAction
from billing.yandex_pay.yandex_pay.core.actions.payment_token.internal.checkout import (
    CreateInternalCheckoutPaymentTokenAction, InternalCheckoutResponse
)
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.create_order import YandexPayPlusCreateOrderAction
from billing.yandex_pay.yandex_pay.core.actions.split.checkout import SplitCheckoutAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card, ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.checkout import CheckoutContext, MITCustomerChoices, PaymentMethodInfo
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork, TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.message import Message
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import (
    DirectShippingMethod, DirectShippingMethodAddress, MITOptions, ParsedPaymentSheet, PaymentMerchant, PaymentMethod,
    PaymentSheet, PickupShippingMethod, PickupShippingMethodAddress, PickupShippingMethodAddressLocation,
    ShippingContact, ShippingMethod
)
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import (
    MITMisconfiguredError, MITNotAllowedError, SplitNotAvailableError
)
from billing.yandex_pay.yandex_pay.interactions import AntifraudClient, UnifiedAgentMetricPushClient
from billing.yandex_pay.yandex_pay.interactions.plus_backend import YandexPayPlusClient
from billing.yandex_pay.yandex_pay.interactions.plus_backend.entities import (
    OrderStatus, PlusOrder, YandexPayPlusMerchant
)
from billing.yandex_pay.yandex_pay.interactions.trust_gateway import TrustPaymentMethod
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank
from billing.yandex_pay.yandex_pay.utils.stats import pay_plus_create_order_failures, split_checkout_failures

MODULE_PREFIX = 'billing.yandex_pay.yandex_pay.core.actions.checkout'
OWNER_UID = 5555
FAKE_TVM_TICKET = 'fake_tvm_user_ticket'
FAKE_LOGIN_ID = 'fake_login_id'
PREDEFINED_CARD_ID = 'aaf024bb-2f0e-4cad-9010-40328ffcae9a'
PREDEFINED_VISA_CARD_ID = 'cfebea81-b1ab-4863-96ff-030af1e18611'
PREDEFINED_ENROLLMENT_ID = uuid.UUID('d247a90a-faaf-4649-a47c-95adfab53c25')
MERCHANT_ID = uuid.UUID('789b29e6-d8f2-4e14-8c3f-33679ca590e3')
PSP_EXTERNAL_ID = 'yandex-trust'
TSP_TOKEN_ID = 'gra-pes'
CLOUD_TOKEN_MOCK = 'le-mo-na-de'
PAN_ONLY_TOKEN_MOCK = 'de-na-mo-le'
FORBIDDEN_CARD_NETWORKS = {CardNetwork.MASTERCARD, CardNetwork.VISA}
CORRECT_SHEET = PaymentSheet(
    merchant=PaymentMerchant(
        id=MERCHANT_ID,
        name='merchant-name',
        url='http://site.test',
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
AMOUNT = 100
CARD_TOKEN = 'card-token'
EXP_YEAR = 2020
EXP_MONTH = 6
TRUST_CARD_LAST4 = '1234'
TRUST_PAYMENT_METHOD = TrustPaymentMethod(
    id='whatever',
    card_id='whatever',
    binding_systems=['trust'],
    orig_uid=str(OWNER_UID),
    payment_method='card',
    system='MasterCard',
    payment_system='MasterCard',
    expiration_month=str(EXP_MONTH),
    expiration_year=str(EXP_YEAR),
    card_bank='SBERBANK OF RUSSIA',
    expired=False,
    account=f'1111****{TRUST_CARD_LAST4}',
    last_paid_ts=utcnow(),
    binding_ts=utcnow(),
)

CARD = Card(
    trust_card_id=CARD_TOKEN,
    owner_uid=OWNER_UID,
    tsp=TSPType.MASTERCARD,
    expire=utcnow(),
    last4='0000',
    card_id=uuid.UUID(PREDEFINED_CARD_ID),
)
VISA_CARD = Card(
    trust_card_id='trust-visa-card-id',
    owner_uid=OWNER_UID,
    tsp=TSPType.VISA,
    expire=utcnow(),
    last4='0000',
    card_id=uuid.UUID(PREDEFINED_VISA_CARD_ID),
)
ENROLLMENT = Enrollment(
    enrollment_id=PREDEFINED_ENROLLMENT_ID,
    card_id=uuid.UUID(PREDEFINED_CARD_ID),
    merchant_id=None,
    tsp_token_status=TSPTokenStatus.ACTIVE,
    tsp_card_id=None,
    tsp_token_id=TSP_TOKEN_ID,
    expire=CARD.expire,
    card_last4=CARD.last4,
)
CARD_PSP = PSP(
    psp_id=uuid.uuid4(),
    psp_external_id=PSP_EXTERNAL_ID,
    public_key='public-key',
    public_key_signature='public-key-signature',
)

CLOUD_USER_CARD = UserCard(
    card_id='card_id',
    owner_uid=OWNER_UID,
    last4='0000',
    card_network=CardNetwork.JCB,
    bin='111000',
    allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
    last_paid=utcnow(),
    created=utcnow(),
    trust_payment_method=TRUST_PAYMENT_METHOD,
    trust_binding=None,
    issuer_bank=IssuerBank.TINKOFF,
    expiration_date=ExpirationDate(
        month=10,
        year=2010,
    ),
    card=replace(CARD, enrollment=ENROLLMENT),
    trust_card_id=CARD.trust_card_id,
)
PAN_USER_CARD = replace(CLOUD_USER_CARD, card=None)

PARSED_CLOUD_TOKEN_SHEET = ParsedPaymentSheet(
    user_card=CLOUD_USER_CARD,
    amount=AMOUNT,
    psp=CARD_PSP,
    auth_methods=[AuthMethod.CLOUD_TOKEN],
    payment_method_type=PaymentMethodType.CARD,
)
PARSED_PAN_ONLY_SHEET = ParsedPaymentSheet(
    user_card=PAN_USER_CARD,
    amount=AMOUNT,
    psp=CARD_PSP,
    auth_methods=[AuthMethod.PAN_ONLY],
    payment_method_type=PaymentMethodType.CARD,
)
PARSED_CASH_SHEET = ParsedPaymentSheet(
    payment_method_type=PaymentMethodType.CASH,
    amount=AMOUNT,
    auth_methods=[],
)
ORIGIN = 'https://origin.test'


@pytest.fixture
def user():
    return User(OWNER_UID, FAKE_TVM_TICKET, FAKE_LOGIN_ID)


@pytest.fixture
def plus_order():
    return PlusOrder(
        order_id=1,
        uid=OWNER_UID,
        message_id='fake_message_id',
        currency=CORRECT_SHEET.currency_code,
        amount=Decimal('100.00'),
        cashback=Decimal('5.00'),
        cashback_category=Decimal('0.05'),
        status=OrderStatus.NEW,
        psp_id=uuid.uuid4(),
        merchant_id=MERCHANT_ID,
        payment_method_type=PaymentMethodType.CARD,
    )


@pytest.fixture(autouse=True)
def enable_plus_interaction(yandex_pay_settings):
    yandex_pay_settings.SHOULD_CREATE_ORDER_IN_PAY_PLUS = True


@pytest.fixture(autouse=True)
def mock_plus_client(mocker, plus_order):
    return mocker.patch.object(
        YandexPayPlusClient,
        'create_order',
        mocker.AsyncMock(return_value=plus_order),
    )


@pytest.fixture(autouse=True)
def mock_antifraud_client(mocker):
    return mocker.patch.object(
        AntifraudClient,
        'get_challenge',
        mocker.AsyncMock(return_value=Challenge(status=ChallengeStatus.NOT_REQUIRED)),
    )


@pytest.fixture
def internal_checkout_response(rands):
    payment_token = rands()
    message = Message(rands(), utcnow() + timedelta(days=1))

    return InternalCheckoutResponse(
        payment_method_info=PaymentMethodInfo(
            method_type=PaymentMethodType.CARD,
            payment_token=payment_token,
            auth_method=AuthMethod.CLOUD_TOKEN,
            card_last4=CARD.last4,
            card_network=CardNetwork.from_tsp_type(CARD.tsp),
            mit_info=MITInfo(),
        ),
        message=message,
    )


@pytest.fixture(autouse=True)
def mock_internal_checkout_action(mock_action, internal_checkout_response):
    return mock_action(CreateInternalCheckoutPaymentTokenAction, internal_checkout_response)


@pytest.fixture(autouse=True)
def mock_check_cashback_allowed_action(mock_action):
    return mock_action(CheckCashbackAllowedAction, True)


def _get_pending_tasks():
    return [
        task for task in asyncio.all_tasks()
        if task.get_name() in {checkout_module.CREATE_PLUS_ORDER_TASK_NAME}
    ]


@pytest.fixture(autouse=True)
async def cancel_dangling_async_tasks():
    # CheckoutAction creates an asyncio tasks for UpdateEnrollmentMetadataAction
    # and YandexPayPlusCreateOrderAction, which are intentionally not awaited.
    # This causes a connection error
    # ('psycopg2.InterfaceError: cursor already closed') when pytest tries to
    # close the DB connection during the test teardown.
    # As a cure, this fixture gracefully cancels such dangling task(s)
    # and awaits the cancellations before pytest comes with its sledgehammer.
    yield

    for task in _get_pending_tasks():
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass


def patch_run_async_with_lock(mocker, lock, action_cls):
    async def run_async(self, **kwargs):
        async with lock:
            return await super(action_cls, self).run_async(**kwargs)

    mocker.patch.object(action_cls, 'run_async', run_async)


@pytest.fixture(autouse=True)
def patch_async_runs(mocker, loop):
    # this is needed to prevent the actions from capturing the same connection cursor
    # did not reproduce in the functional tests, therefore patching it only for unit
    if sys.version_info >= (3, 10):
        lock = asyncio.Lock()
    else:
        lock = asyncio.Lock(loop=loop)

    for action_cls in (UpdateEnrollmentMetadataAction, YandexPayPlusCreateOrderAction):
        patch_run_async_with_lock(mocker, lock, action_cls)


@pytest.fixture(autouse=True)
def mock_ua_push_client(mocker):
    mock = mocker.AsyncMock(return_value=None)
    return mocker.patch.object(UnifiedAgentMetricPushClient, 'send', mock)


@pytest.fixture
def split_psp(yandex_pay_settings):
    return PSP(
        psp_external_id='yandex-split',
        public_key='',
        public_key_signature='',
        psp_id=uuid.UUID(yandex_pay_settings.SPLIT_PSP_INTERNAL_ID),
    )


@pytest.mark.asyncio
async def test_checkout_action_logs_message(mock_action, mocker, user, product_logs):
    mock_action(ParseSheetAction, PARSED_CLOUD_TOKEN_SHEET)
    fake_merchant_origin = mocker.Mock()
    shipping_method = ShippingMethod(
        method_type=ShippingMethodType.PICKUP,
        pickup=PickupShippingMethod(
            provider='POCHTA',
            address=PickupShippingMethodAddress(
                formatted='fake',
                location=PickupShippingMethodAddressLocation(longitude=37.353126),
            ),
            id='pickup_id',
        ),
        direct=DirectShippingMethod(
            provider='any',
            amount='not_a_number',
            address=DirectShippingMethodAddress(
                id='-1',
                country=False,
                locality=False,
            ),
            id='direct_id',
        ),
    )
    shipping_contact = ShippingContact()
    action = CheckoutAction(
        user=user,
        card_id=PREDEFINED_CARD_ID,
        sheet=CORRECT_SHEET,
        user_agent='agent',
        user_ip='ip',
        merchant_origin=fake_merchant_origin,
        challenge_return_path='some_path',
        shipping_method=shipping_method,
        shipping_contact=shipping_contact
    )
    spy = mocker.spy(checkout_module, 'generate_message')

    await action.run()

    spy.assert_called_once_with(user.uid)

    logs = product_logs()
    assert_that(
        logs,
        contains(
            has_properties(
                message='Order received',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    card_id=PREDEFINED_CARD_ID,
                    sheet=CORRECT_SHEET,
                    payment_method_type=PaymentMethodType.CARD,
                    merchant_origin=fake_merchant_origin,
                    challenge_return_path='some_path',
                    shipping_method=shipping_method,
                    shipping_contact=shipping_contact,
                ),
            ),
            has_properties(
                message='Token received for order',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    auth_method=AuthMethod.CLOUD_TOKEN,
                    card_id=PREDEFINED_CARD_ID,
                    sheet=CORRECT_SHEET,
                    payment_method_type=PaymentMethodType.CARD,
                    merchant_origin=fake_merchant_origin,
                    challenge_return_path='some_path',
                    message_id=spy.spy_return.message_id,
                    message_expires_at=spy.spy_return.expires_at,
                    shipping_method=shipping_method,
                    shipping_contact=shipping_contact,
                ),
            )
        )
    )


@pytest.mark.asyncio
async def test_cash_checkout(
    mock_action, product_logs, user, mock_internal_checkout_action
):
    mock_action(ParseSheetAction, PARSED_CASH_SHEET)
    sheet = deepcopy(CORRECT_SHEET)
    sheet.payment_methods[0].method_type = PaymentMethodType.CASH

    expected = CheckoutContext(
        payment_method_info=PaymentMethodInfo(
            method_type=PaymentMethodType.CASH,
        )
    )

    result = await CheckoutAction(
        user=user,
        card_id='',
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        payment_method_type=PaymentMethodType.CASH,
    ).run()

    assert_that(result, equal_to(expected))
    mock_internal_checkout_action.assert_not_called()

    [log] = product_logs()
    assert_that(
        log,
        has_properties(
            message='Order received',
            _context=has_entries(
                uid=user.uid,
                card_id='',
                sheet=sheet,
                payment_method_type=PaymentMethodType.CASH,
                merchant_origin=None,
                challenge_return_path=None,
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('parsed_sheet', [PARSED_CLOUD_TOKEN_SHEET, PARSED_PAN_ONLY_SHEET])
async def test_calls_parse_sheet(mock_action, user, parsed_sheet):
    mock_parse_sheet_action = mock_action(ParseSheetAction, parsed_sheet)

    await CheckoutAction(
        user=user,
        card_id=PREDEFINED_CARD_ID,
        sheet=CORRECT_SHEET,
        user_agent='agent',
        user_ip='ip',
        merchant_origin=ORIGIN,
        forbidden_card_networks=FORBIDDEN_CARD_NETWORKS,
    ).run()

    mock_parse_sheet_action.assert_called_once_with(
        user=user,
        card_id=PREDEFINED_CARD_ID,
        payment_method_type=PaymentMethodType.CARD,
        sheet=CORRECT_SHEET,
        merchant_origin=ORIGIN,
        validate_origin=True,
        forbidden_card_networks=FORBIDDEN_CARD_NETWORKS,
    )


class TestPlusOrderCreation:
    @pytest.fixture(params=[None, '0.05'])
    def cashback_category_id(self, request):
        return request.param

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'parsed_sheet,expected_trust_card_id,expected_last4',
        [
            (PARSED_CLOUD_TOKEN_SHEET, f'card-x{CARD.trust_card_id}', CARD.last4),
            (PARSED_PAN_ONLY_SHEET, f'card-x{TRUST_PAYMENT_METHOD.card_id}', TRUST_PAYMENT_METHOD.last4),
        ]
    )
    async def test_creates_pay_plus_create_order_task(
        self,
        mock_action,
        user,
        mock_plus_client,
        parsed_sheet,
        expected_trust_card_id,
        expected_last4,
        cashback_category_id,
        storage,
        internal_checkout_response,
    ):
        mock_action(ParseSheetAction, parsed_sheet)
        internal_checkout_response['payment_method_info'].card_last4 = expected_last4

        await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
            cashback_category_id=cashback_category_id,
        ).run()

        await asyncio.gather(*_get_pending_tasks())

        filters = {'action_name': YandexPayPlusCreateOrderAction.action_name}
        [task] = await alist(storage.task.find(filters=filters))

        init_kwargs = YandexPayPlusCreateOrderAction.deserialize_kwargs(
            task.params['action_kwargs']
        )
        await YandexPayPlusCreateOrderAction(**init_kwargs).run()

        mock_plus_client.assert_awaited_once_with(
            uid=user.uid,
            message_id=task.params['action_kwargs']['message_id'],
            merchant=YandexPayPlusMerchant(
                id=CORRECT_SHEET.merchant.id,
                name='merchant-name',
                url='http://site.test',
            ),
            psp_id=CARD_PSP.psp_id,
            currency='RUB',
            amount=Decimal('1.00'),
            trust_card_id=expected_trust_card_id,
            last4=expected_last4,
            cashback_category_id=cashback_category_id,
            country_code='ru',
            order_basket={'id': 'order-id', 'total': {'amount': '1.00', 'label': None}, 'items': None},
            card_network='MASTERCARD',
            card_id=parsed_sheet.db_card.card_id if parsed_sheet.db_card else None,
            antifraud_external_id=None,
            payment_method_type=PaymentMethodType.CARD,
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'parsed_sheet,expected_trust_card_id,expected_last4',
        [
            (PARSED_CLOUD_TOKEN_SHEET, f'card-x{CARD.trust_card_id}', CARD.last4),
            (PARSED_PAN_ONLY_SHEET, f'card-x{TRUST_PAYMENT_METHOD.card_id}', TRUST_PAYMENT_METHOD.last4),
        ]
    )
    async def test_calls_pay_plus_create_order(
        self,
        mock_action,
        user,
        mock_plus_client,
        parsed_sheet,
        expected_trust_card_id,
        expected_last4,
        cashback_category_id,
        yandex_pay_settings,
        internal_checkout_response,
    ):
        yandex_pay_settings.ASYNC_CASHBACK_ORDER_CREATION_ENABLED = False
        parsed_sheet = copy(parsed_sheet)
        parsed_sheet.verification_details = True
        mock_action(ParseSheetAction, parsed_sheet)
        internal_checkout_response['payment_method_info'].card_last4 = expected_last4

        await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
            cashback_category_id=cashback_category_id,
            challenge_return_path='some_path',
        ).run()

        await asyncio.gather(*_get_pending_tasks())

        mock_plus_client.assert_awaited_once_with(
            uid=user.uid,
            message_id=match_equality(instance_of(str)),
            merchant=YandexPayPlusMerchant(
                id=CORRECT_SHEET.merchant.id,
                name='merchant-name',
                url='http://site.test',
            ),
            psp_id=CARD_PSP.psp_id,
            currency='RUB',
            amount=Decimal('1.00'),
            trust_card_id=expected_trust_card_id,
            last4=expected_last4,
            cashback_category_id=cashback_category_id,
            country_code='ru',
            order_basket={'id': 'order-id', 'total': {'amount': '1.00', 'label': None}, 'items': None},
            card_network='MASTERCARD',
            card_id=parsed_sheet.db_card.card_id if parsed_sheet.db_card else None,
            antifraud_external_id=match_equality(instance_of(str)),
            payment_method_type=PaymentMethodType.CARD,
        )
        call_kwargs = mock_plus_client.call_args.kwargs
        assert_that(call_kwargs['message_id'], equal_to(call_kwargs['antifraud_external_id']))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('error', [ClientConnectionError, Exception])
    async def test_should_log_pay_plus_action_failure(
        self, mock_action, user, error, dummy_logs, mocker
    ):
        mock_action(ParseSheetAction, PARSED_CLOUD_TOKEN_SHEET)
        mocker.patch.object(YandexPayPlusCreateOrderAction, 'run_async', side_effect=error)
        mock_counter_inc = mocker.patch.object(pay_plus_create_order_failures, 'inc')
        spy = mocker.spy(checkout_module, 'generate_message')

        await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
        ).run()
        await asyncio.gather(*_get_pending_tasks())

        mock_counter_inc.assert_called_once_with()

        [*_, log] = dummy_logs()
        assert_that(
            log,
            has_properties(
                message='Failed to schedule plus cashback order creation',
                levelno=logging.ERROR,
                _context=has_entries(
                    message_id=spy.spy_return.message_id,
                    card_id=PREDEFINED_CARD_ID,
                    uid=user.uid,
                    sheet=CORRECT_SHEET,
                )
            )
        )


class TestAntifraud:
    @pytest.mark.asyncio
    @pytest.mark.parametrize('enable_with', ('verification_details', 'settings'))
    async def test_calls_antifraud_client_with_expected_args(
        self, mock_action, user, mock_antifraud_client, yandex_pay_settings, enable_with
    ):
        sheet = copy(PARSED_CLOUD_TOKEN_SHEET)
        if enable_with == 'verification_details':
            sheet.verification_details = True
            yandex_pay_settings.CHECKOUT_ANTIFRAUD_ENABLED = False
        else:
            sheet.verification_details = False
            yandex_pay_settings.CHECKOUT_ANTIFRAUD_ENABLED = True
            yandex_pay_settings.CHECKOUT_ANTIFRAUD_MERCHANTS = {
                str(MERCHANT_ID): {
                    'RUB': 1000000
                }
            }
        mock_action(ParseSheetAction, sheet)

        await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            challenge_return_path='some_path',
            user_agent='agent007',
            user_ip='ip228',
        ).run()

        mock_antifraud_client.assert_awaited_once_with(
            external_id=match_equality(not_none()),
            amount=sheet.amount,
            trust_card_id=sheet.trust_card_id,
            timestamp=match_equality(close_to(utcnow().timestamp() * 1000, 60_000)),
            user_agent='agent007',
            user_ip='ip228',
            uid=user.uid,
            login_id=FAKE_LOGIN_ID,
            currency_number='643',
            return_path='some_path',
            device_id=None,
        )

    @pytest.mark.asyncio
    async def test_should_not_call_antifraud_client_if_merchant_id_is_not_in_list(
        self, mock_action, user, mock_antifraud_client, yandex_pay_settings
    ):
        sheet = copy(PARSED_CLOUD_TOKEN_SHEET)
        yandex_pay_settings.CHECKOUT_ANTIFRAUD_MERCHANTS = {}
        mock_action(ParseSheetAction, sheet)

        await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
        ).run()

        mock_antifraud_client.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_should_not_call_antifraud_client_if_sheet_amount_bigger_than_setting_amount(
        self, mock_action, user, mock_antifraud_client, yandex_pay_settings
    ):
        sheet = copy(PARSED_CLOUD_TOKEN_SHEET)
        yandex_pay_settings.CHECKOUT_ANTIFRAUD_MERCHANTS = {
            str(MERCHANT_ID): {
                'RUB': sheet.amount - 1
            }
        }
        mock_action(ParseSheetAction, sheet)

        await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
        ).run()

        mock_antifraud_client.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_should_not_call_antifraud_client_if_disabled(
        self, mock_action, user, mock_antifraud_client, yandex_pay_settings
    ):
        yandex_pay_settings.CHECKOUT_ANTIFRAUD_ENABLED = False
        yandex_pay_settings.CHECKOUT_ANTIFRAUD_MERCHANTS = {
            str(MERCHANT_ID): {
                'RUB': 1000000
            }
        }
        sheet = copy(PARSED_CLOUD_TOKEN_SHEET)
        sheet.verification_details = False
        mock_action(ParseSheetAction, sheet)

        await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
        ).run()

        mock_antifraud_client.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_should_not_call_antifraud_client_if_sheet_total_amount_is_zero(
        self, mock_action, user, mock_antifraud_client, yandex_pay_settings
    ):
        sheet = copy(PARSED_CLOUD_TOKEN_SHEET)
        sheet.amount = Decimal('0')
        yandex_pay_settings.CHECKOUT_ANTIFRAUD_MERCHANTS = {}
        mock_action(ParseSheetAction, sheet)

        await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
        ).run()

        mock_antifraud_client.assert_not_awaited()


class TestCardCheckout:
    @pytest.fixture(params=[PARSED_CLOUD_TOKEN_SHEET, PARSED_PAN_ONLY_SHEET])
    def parsed_sheet(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def mock_parse_sheet(self, mock_action, parsed_sheet):
        mock_action(ParseSheetAction, parsed_sheet)

    @pytest.fixture
    def params(self, user):
        return dict(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
        )

    @pytest.mark.asyncio
    async def test_returned(self, user, internal_checkout_response, parsed_sheet, params):
        expected = CheckoutContext(
            payment_method_info=internal_checkout_response['payment_method_info'],
            card_id=parsed_sheet.card_id,
            trust_card_id=parsed_sheet.trust_card_id,
            psp=CARD_PSP,
        )

        returned = await CheckoutAction(**params).run()

        assert_that(returned, equal_to(expected))

    @pytest.mark.asyncio
    async def test_internal_checkout_action_called(
        self, user, mock_internal_checkout_action, parsed_sheet, params
    ):
        await CheckoutAction(**params).run()

        mock_internal_checkout_action.assert_called_once_with(
            user=user,
            psp=CARD_PSP,
            gateway_merchant_id=CORRECT_SHEET.card_payment_method.gateway_merchant_id,
            merchant_id=CORRECT_SHEET.merchant.id,
            currency=CORRECT_SHEET.currency_code,
            amount=CORRECT_SHEET.order.total.amount,
            auth_methods=parsed_sheet.auth_methods,
            message=match_equality(instance_of(Message)),
            user_card=parsed_sheet.user_card,
            mit_info=ensure_all_fields(MITInfo)(
                recurring=False,
                deferred=False,
            ),
        )

    @pytest.mark.parametrize(
        'mit_options, mit_choices, expected_info',
        (
            pytest.param(None, MITCustomerChoices(), MITInfo(), id='no mit requested => all false'),
            pytest.param(
                MITOptions(type=MITOptionsType.RECURRING, optional=True),
                MITCustomerChoices(allowed=False),
                MITInfo(),
                id='RECURRING+optional but not allowed => all false'
            ),
            pytest.param(
                MITOptions(
                    type=MITOptionsType.RECURRING, optional=True
                ),
                MITCustomerChoices(allowed=True),
                MITInfo(recurring=True),
                id='RECURRING+optional and is allowed => recurring set'
            ),
            pytest.param(
                MITOptions(type=MITOptionsType.DEFERRED),
                MITCustomerChoices(allowed=True),
                MITInfo(deferred=True),
                id='DEFERRED and is allowed => deferred set'
            ),
        )
    )
    @pytest.mark.asyncio
    async def test_mit_info(
        self, mock_internal_checkout_action, mit_options, params, mit_choices, expected_info
    ):
        params['sheet'].mit_options = mit_options
        params['mit_customer_choices'] = mit_choices

        await CheckoutAction(**params).run()

        assert_that(
            mock_internal_checkout_action.call_args.kwargs['mit_info'],
            equal_to(expected_info),
        )

    @pytest.mark.asyncio
    async def test_mit_info_when_requested_but_customer_choice_was_not_received(
        self, mock_internal_checkout_action, params
    ):
        params['sheet'].mit_options = MITOptions(type=MITOptionsType.RECURRING)
        params['mit_customer_choices'] = None

        with pytest.raises(MITMisconfiguredError):
            await CheckoutAction(**params).run()

    @pytest.mark.parametrize(
        'mit_options',
        (
            MITOptions(type=MITOptionsType.RECURRING, optional=False),
            MITOptions(type=MITOptionsType.DEFERRED),
        )
    )
    @pytest.mark.asyncio
    async def test_mit_info_when_required_but_not_allowed(
        self, mock_internal_checkout_action, params, mit_options
    ):
        params['sheet'].mit_options = mit_options
        params['mit_customer_choices'] = MITCustomerChoices(allowed=False)

        with pytest.raises(MITNotAllowedError):
            await CheckoutAction(**params).run()


class TestSplitCheckout:
    @pytest.fixture(autouse=True)
    def mock_parse_sheet(self, mock_action):
        parsed_sheet = replace(PARSED_CLOUD_TOKEN_SHEET, payment_method_type=PaymentMethodType.SPLIT)
        mock_action(ParseSheetAction, parsed_sheet)

    @pytest.mark.asyncio
    async def test_split_checkout_success(
        self,
        mock_action,
        user,
        yandex_pay_settings,
        mock_plus_client,
        mock_internal_checkout_action,
        split_psp,
    ):
        order_id = uuid.uuid4()
        split_checkout_info = YandexSplitOrderCheckoutInfo(
            order_id=str(order_id),
            checkout_url=f'https://test.bnpl.yandex.ru/checkout/{order_id}',
        )
        mock_split_checkout = mock_action(SplitCheckoutAction, split_checkout_info)

        result = await CheckoutAction(
            user=user,
            card_id=PREDEFINED_CARD_ID,
            sheet=CORRECT_SHEET,
            user_agent='agent',
            user_ip='ip',
            payment_method_type=PaymentMethodType.SPLIT,
        ).run()

        assert_that(
            result,
            equal_to(
                CheckoutContext(
                    payment_method_info=PaymentMethodInfo(
                        method_type=PaymentMethodType.SPLIT,
                        card_last4='0000',
                        card_network=CardNetwork.MASTERCARD,
                        split_meta=split_checkout_info,
                    ),
                    card_id=uuid.UUID(PREDEFINED_CARD_ID),
                    trust_card_id=CARD_TOKEN,
                    psp=split_psp,
                )
            )
        )
        mock_split_checkout.assert_called_once_with(
            user=user,
            sheet=CORRECT_SHEET,
            checkout_context=result,
            user_ip='ip',
            user_agent='agent',
            shipping_method=None,
            shipping_contact=None,
            cashback_category_id=None,
        )
        mock_internal_checkout_action.assert_not_called()

    @pytest.mark.asyncio
    async def test_split_checkout_failure(self, mock_action, mocker, user, dummy_logs):
        mock_action(SplitCheckoutAction, SplitNotAvailableError)
        mock_stat_inc = mocker.patch.object(split_checkout_failures, 'inc')

        with pytest.raises(SplitNotAvailableError):
            await CheckoutAction(
                user=user,
                card_id=PREDEFINED_CARD_ID,
                sheet=CORRECT_SHEET,
                user_agent='agent',
                user_ip='ip',
                payment_method_type=PaymentMethodType.SPLIT,
            ).run()

        mock_stat_inc.assert_called_once()
        logs = dummy_logs()
        assert_that(
            logs,
            has_item(
                has_properties(
                    message='SPLIT_CHECKOUT_FAILED',
                    levelno=logging.ERROR,
                    _context=has_entries(
                        uid=user.uid,
                        card_id=PREDEFINED_CARD_ID,
                        sheet=CORRECT_SHEET,
                        payment_method_type=PaymentMethodType.SPLIT,
                    ),
                )
            )
        )
