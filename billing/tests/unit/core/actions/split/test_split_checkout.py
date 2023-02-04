import logging
from decimal import Decimal
from uuid import UUID, uuid4

import pytest
from pay.lib.entities.enums import ShippingMethodType
from pay.lib.entities.payment_sheet import PaymentOrder, PaymentOrderTotal
from pay.lib.interactions.merchant.exceptions import MerchantResponseError
from pay.lib.interactions.split.entities import YandexSplitOrderCheckoutInfo
from pay.lib.interactions.split.exceptions import YandexSplitResponseError

from hamcrest import assert_that, contains, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.card.get import GetUserCardByCardIdAction
from billing.yandex_pay.yandex_pay.core.actions.merchant.create_order import CreateMerchantOrder
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.create_order import YandexPayPlusCreateOrderAction
from billing.yandex_pay.yandex_pay.core.actions.split.checkout import SplitCheckoutAction
from billing.yandex_pay.yandex_pay.core.entities.card import UserCard
from billing.yandex_pay.yandex_pay.core.entities.checkout import CheckoutContext, PaymentMethodInfo
from billing.yandex_pay.yandex_pay.core.entities.enums import CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import (
    DirectShippingMethod, DirectShippingMethodAddress, PaymentMerchant, PaymentMethod, PaymentSheet,
    PickupShippingMethod, PickupShippingMethodAddress, PickupShippingMethodAddressLocation, ShippingContact,
    ShippingMethod
)
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import MerchantOrderValidationError, SplitNotAvailableError
from billing.yandex_pay.yandex_pay.interactions.plus_backend import PlusOrder
from billing.yandex_pay.yandex_pay.interactions.split import YandexSplitClient


@pytest.fixture
def split_response():
    order_id = uuid4()
    return YandexSplitOrderCheckoutInfo(
        order_id=str(order_id),
        checkout_url=f'https://test.bnpl.yandex.ru/checkout/{order_id}'
    )


@pytest.fixture
def trust_card_id():
    return 'card-x112233'


@pytest.fixture
def plus_points():
    return Decimal('10')


@pytest.fixture(autouse=True)
def mock_split(mocker, split_response):
    response = mocker.AsyncMock(return_value=split_response)
    return mocker.patch.object(YandexSplitClient, 'create_order', response)


@pytest.fixture(autouse=True)
def mock_merchant(mock_action):
    return mock_action(CreateMerchantOrder)


@pytest.fixture(autouse=True)
def mock_get_trust_card(mock_action, mocker, trust_card_id):
    mock_user_card = mocker.Mock(spec=UserCard, trust_card_id=trust_card_id)
    return mock_action(GetUserCardByCardIdAction, mock_user_card)


@pytest.fixture(autouse=True)
def mock_create_plus_order(mock_action, mocker, plus_points):
    mock_plus_order = mocker.Mock(spec=PlusOrder, cashback=plus_points)
    return mock_action(YandexPayPlusCreateOrderAction, mock_plus_order)


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def login_id(rands):
    return rands()


@pytest.fixture
def user(uid, login_id):
    return User(uid, None, login_id)


@pytest.fixture
def amount():
    return Decimal('2052.00')


@pytest.fixture
def user_ip():
    return '127.0.0.1'


@pytest.fixture
def user_agent(rands):
    return rands()


@pytest.fixture
def sheet(amount):
    return PaymentSheet(
        merchant=PaymentMerchant(
            id=UUID('789b29e6-d8f2-4e14-8c3f-33679ca590e3'),
            name='merchant-name',
            url='http://site.test',
        ),
        version=2,
        currency_code='xts',
        country_code='ru',
        payment_methods=[
            PaymentMethod(method_type=PaymentMethodType.SPLIT)
        ],
        order=PaymentOrder(
            id='order-id',
            total=PaymentOrderTotal(
                amount=amount,
            ),
        ),
    )


@pytest.fixture
def shipping_method_direct():
    return ShippingMethod(
        method_type=ShippingMethodType.DIRECT,
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


@pytest.fixture
def shipping_method_pickup():
    return ShippingMethod(
        method_type=ShippingMethodType.PICKUP,
        pickup=PickupShippingMethod(
            provider='POCHTA',
            address=PickupShippingMethodAddress(
                formatted='fake',
                location=PickupShippingMethodAddressLocation(longitude=37.353126),
            ),
            id='pickup_id',
        ),
    )


@pytest.fixture
def shipping_contact():
    return ShippingContact()


@pytest.fixture
def checkout_context(trust_card_id, yandex_pay_settings):
    psp = PSP(
        psp_external_id='yandex-split',
        public_key='',
        public_key_signature='',
        psp_id=UUID(yandex_pay_settings.SPLIT_PSP_INTERNAL_ID),
    )
    return CheckoutContext(
        payment_method_info=PaymentMethodInfo(
            method_type=PaymentMethodType.SPLIT,
            card_last4='0000',
        ),
        trust_card_id=trust_card_id,
        psp=psp,
    )


@pytest.mark.asyncio
async def test_returned(user, sheet, checkout_context, split_response, user_ip, user_agent):
    result = await SplitCheckoutAction(
        user=user,
        sheet=sheet,
        checkout_context=checkout_context,
        user_ip=user_ip,
        user_agent=user_agent,
    ).run()

    assert_that(result, equal_to(split_response))


@pytest.mark.asyncio
async def test_merchant_called(
    user,
    sheet,
    checkout_context,
    user_ip,
    user_agent,
    mock_merchant,
    mocker,
):
    shipping_method = mocker.Mock(spec=ShippingMethod)
    shipping_contact = mocker.Mock(spec=ShippingContact)

    await SplitCheckoutAction(
        user=user,
        sheet=sheet,
        checkout_context=checkout_context,
        user_ip=user_ip,
        user_agent=user_agent,
        shipping_method=shipping_method,
        shipping_contact=shipping_contact,
    ).run()

    mock_merchant.assert_called_once_with(
        user=user,
        sheet=sheet,
        shipping_method=shipping_method,
        shipping_contact=shipping_contact,
    )


@pytest.mark.asyncio
async def test_plus_action_called(
    user,
    sheet,
    checkout_context,
    user_ip,
    user_agent,
    mock_create_plus_order,
):
    await SplitCheckoutAction(
        user=user,
        sheet=sheet,
        checkout_context=checkout_context,
        user_ip=user_ip,
        user_agent=user_agent,
    ).run()

    mock_create_plus_order.assert_called_once_with(
        user=user,
        message_id=f'2:{sheet.merchant.id}_{sheet.order.id}',
        merchant=sheet.merchant,
        psp_id=checkout_context.psp_id,
        currency=sheet.currency_code,
        amount=sheet.order.total.amount,
        trust_card_id=checkout_context.trust_card_id,
        last4=checkout_context.last4,
        cashback_category_id=None,
        country_code=sheet.country_code,
        order_basket={'id': 'order-id', 'total': {'amount': '2052.00', 'label': None}, 'items': None},
        card_network=checkout_context.card_network,
        card_id=checkout_context.card_id,
        antifraud_external_id=checkout_context.antifraud_external_id,
        user_agent=user_agent,
        user_ip=user_ip,
        payment_method_type=PaymentMethodType.SPLIT,
    )


@pytest.mark.asyncio
async def test_split_client_called(
    user,
    sheet,
    uid,
    login_id,
    amount,
    checkout_context,
    plus_points,
    user_ip,
    user_agent,
    trust_card_id,
    mock_split,
    yandex_pay_settings,
):
    await SplitCheckoutAction(
        user=user,
        sheet=sheet,
        checkout_context=checkout_context,
        user_ip=user_ip,
        user_agent=user_agent,
    ).run()

    mock_split.assert_called_once_with(
        uid=uid,
        currency='xts',
        amount=amount,
        login_id=login_id,
        external_order_id=sheet.order.id,
        trust_card_id=trust_card_id,
        merchant_id=yandex_pay_settings.SPLIT_INTERNAL_MERCHANT_ID,
        plus_points=plus_points,
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'card_network,expected_trust_card_id',
    [
        (CardNetwork.MASTERCARD, 'fake_trust_card_id'),
        (CardNetwork.VISA, 'card-x112233'),
        (CardNetwork.UNKNOWN, 'card-x112233'),
        (None, 'card-x112233'),
    ]
)
async def test_split_client_called_with_trust_card_id_mapped(
    user,
    sheet,
    uid,
    login_id,
    amount,
    checkout_context,
    plus_points,
    user_ip,
    user_agent,
    mock_split,
    yandex_pay_settings,
    card_network,
    expected_trust_card_id,
):
    yandex_pay_settings.SPLIT_TRUST_CARD_ID_MAPPING = {'MASTERCARD': 'fake_trust_card_id'}
    checkout_context.payment_method_info.card_network = card_network

    await SplitCheckoutAction(
        user=user,
        sheet=sheet,
        checkout_context=checkout_context,
        user_ip=user_ip,
        user_agent=user_agent,
    ).run()

    mock_split.assert_called_once_with(
        uid=uid,
        currency='xts',
        amount=amount,
        login_id=login_id,
        external_order_id=sheet.order.id,
        trust_card_id=expected_trust_card_id,
        merchant_id=yandex_pay_settings.SPLIT_INTERNAL_MERCHANT_ID,
        plus_points=plus_points,
    )


@pytest.mark.asyncio
async def test_call_logged(
    user, sheet, dummy_logs, uid, login_id, checkout_context, split_response, user_ip, user_agent
):
    await SplitCheckoutAction(
        user=user,
        sheet=sheet,
        checkout_context=checkout_context,
        user_ip=user_ip,
        user_agent=user_agent,
    ).run()

    logs = dummy_logs()
    message_id = f'2:{sheet.merchant.id}_{sheet.order.id}'
    assert_that(
        logs,
        contains(
            has_properties(
                message='SPLIT_ORDER_REQUESTED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=uid,
                    login_id=login_id,
                    sheet=sheet,
                    message_id=message_id,
                )
            ),
            has_properties(
                message='SPLIT_ORDER_CREATED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=uid,
                    login_id=login_id,
                    sheet=sheet,
                    split_order_info=split_response,
                    message_id=message_id,
                )
            ),
        ),
    )


@pytest.mark.asyncio
async def test_split_disabled(
    user, sheet, dummy_logs, uid, login_id, checkout_context, user_ip, user_agent, yandex_pay_settings
):
    yandex_pay_settings.SPLIT_PAYMENTS_ENABLED = False

    with pytest.raises(SplitNotAvailableError):
        await SplitCheckoutAction(
            user=user,
            sheet=sheet,
            checkout_context=checkout_context,
            user_ip=user_ip,
            user_agent=user_agent,
        ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='SPLIT_GLOBALLY_DISABLED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=uid,
                    login_id=login_id,
                    sheet=sheet,
                )
            )
        ),
    )


@pytest.mark.asyncio
async def test_split_rejects(
    user, sheet, dummy_logs, uid, login_id, checkout_context, user_ip, user_agent, mock_split
):
    mock_split.side_effect = YandexSplitResponseError(
        status_code=400,
        method='test',
        service='split',
    )

    with pytest.raises(SplitNotAvailableError):
        await SplitCheckoutAction(
            user=user,
            sheet=sheet,
            checkout_context=checkout_context,
            user_ip=user_ip,
            user_agent=user_agent,
        ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='SPLIT_RESPONSE_ERROR',
                levelno=logging.ERROR,
                _context=has_entries(
                    uid=uid,
                    login_id=login_id,
                    sheet=sheet,
                )
            )
        ),
    )


@pytest.mark.asyncio
async def test_merchant_not_in_whitelist(
    user, sheet, dummy_logs, uid, login_id, checkout_context, user_ip, user_agent, mocker
):
    mock_whitelist = {uuid4()}
    mocker.patch.object(SplitCheckoutAction, 'merchant_id_whitelist', mock_whitelist)

    with pytest.raises(SplitNotAvailableError):
        await SplitCheckoutAction(
            user=user,
            sheet=sheet,
            checkout_context=checkout_context,
            user_ip=user_ip,
            user_agent=user_agent,
        ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='MERCHANT_NOT_IN_SPLIT_WHITELIST',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=uid,
                    login_id=login_id,
                    sheet=sheet,
                    merchant_id=sheet.merchant.id,
                    merchant_id_whitelist=mock_whitelist,
                )
            )
        ),
    )


@pytest.mark.asyncio
async def test_pay_plus_disabled(
    user,
    uid,
    login_id,
    amount,
    sheet,
    checkout_context,
    user_ip,
    user_agent,
    trust_card_id,
    yandex_pay_settings,
    mock_create_plus_order,
    mock_split,
):
    yandex_pay_settings.SHOULD_CREATE_ORDER_IN_PAY_PLUS = False

    await SplitCheckoutAction(
        user=user,
        sheet=sheet,
        checkout_context=checkout_context,
        user_ip=user_ip,
        user_agent=user_agent,
    ).run()

    mock_create_plus_order.assert_not_called()
    mock_split.assert_called_once_with(
        uid=uid,
        currency='xts',
        amount=amount,
        login_id=login_id,
        external_order_id=sheet.order.id,
        trust_card_id=trust_card_id,
        merchant_id=yandex_pay_settings.SPLIT_INTERNAL_MERCHANT_ID,
        plus_points=None,
    )


@pytest.mark.asyncio
async def test_merchant_says_no(
    user,
    sheet,
    checkout_context,
    user_ip,
    user_agent,
    mock_create_plus_order,
    mock_split,
    mock_merchant,
):
    exc = MerchantResponseError(
        status_code=400,
        method='POST',
        service='test',
    )
    mock_merchant.side_effect = exc

    with pytest.raises(MerchantOrderValidationError):
        await SplitCheckoutAction(
            user=user,
            sheet=sheet,
            checkout_context=checkout_context,
            user_ip=user_ip,
            user_agent=user_agent,
        ).run()

    mock_create_plus_order.assert_not_called()
    mock_split.assert_not_called()
