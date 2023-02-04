import logging
from decimal import Decimal
from uuid import UUID

import pytest
from pay.lib.entities.enums import ShippingMethodType
from pay.lib.entities.payment_sheet import PaymentOrder, PaymentOrderTotal
from pay.lib.interactions.merchant.entities import PickupPoint, ShippingMethodInfo, ShippingOption
from pay.lib.interactions.passport_addresses.entities import Address, Contact, Service

from hamcrest import assert_that, has_item, has_properties

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.address import GetAddressAction
from billing.yandex_pay.yandex_pay.core.actions.card.get import GetUserCardByCardIdAction
from billing.yandex_pay.yandex_pay.core.actions.contact import GetContactAction
from billing.yandex_pay.yandex_pay.core.actions.merchant.create_order import CreateMerchantOrder
from billing.yandex_pay.yandex_pay.core.entities.card import UserCard
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import (
    DirectShippingMethod, DirectShippingMethodAddress, PaymentMerchant, PaymentMethod, PaymentSheet,
    PickupShippingMethod, PickupShippingMethodAddress, PickupShippingMethodAddressLocation, ShippingContact,
    ShippingMethod
)
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions.merchant import MerchantClient
from billing.yandex_pay.yandex_pay.utils.stats import merchant_order_create_failures


@pytest.fixture(autouse=True)
def mock_merchant(mocker):
    response = mocker.AsyncMock(return_value={'code': 200})
    return mocker.patch.object(MerchantClient, 'create_order', response)


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
def trust_card_id():
    return 'card-x112233'


@pytest.fixture(autouse=True)
def mock_get_trust_card(mock_action, mocker, trust_card_id):
    mock_user_card = mocker.Mock(spec=UserCard, trust_card_id=trust_card_id)
    return mock_action(GetUserCardByCardIdAction, mock_user_card)


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
                id=-1,
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


@pytest.mark.asyncio
async def test_create_merchant_order_success_without_shipping(
    caplog,
    mock_merchant,
    yandex_pay_settings,
    user, sheet: PaymentSheet
):
    caplog.set_level(logging.INFO)
    await CreateMerchantOrder(
        user=user,
        sheet=sheet,
    ).run()

    mock_merchant.assert_called_once_with(
        base_url=yandex_pay_settings.SPLIT_MERCHANT_URL,
        merchant_id=sheet.merchant.id,
        currency_code=sheet.currency_code,
        order=sheet.order,
        shipping_method_info=None,
        shipping_contact=None,
    )

    assert_that(
        caplog.records,
        has_item(
            has_properties(
                message='CREATE_MERCHANT_ORDER_SUCCESS',
            )
        )
    )


@pytest.mark.asyncio
async def test_create_merchant_order_success_with_shipping_direct_and_shipping_contact(
    mock_merchant,
    mock_action,
    yandex_pay_settings,
    user,
    sheet: PaymentSheet,
    shipping_method_direct: ShippingMethod
):
    address = Address(
        country='C',
        locality='L',
        street='S',
        building='B',
        address_line='A',
        id='1',
        type='work',
        owner_service=Service.PAY,
    )
    contact = Contact(
        id='123',
        first_name='hello',
        second_name='world',
    )
    mock_action(GetAddressAction, address)
    mock_action(GetContactAction, contact)

    await CreateMerchantOrder(
        user=user,
        sheet=sheet,
        shipping_method=shipping_method_direct,
        shipping_contact=contact,
    ).run()

    mock_merchant.assert_called_once_with(
        base_url=yandex_pay_settings.SPLIT_MERCHANT_URL,
        merchant_id=sheet.merchant.id,
        currency_code=sheet.currency_code,
        order=sheet.order,
        shipping_method_info=ShippingMethodInfo(
            type=shipping_method_direct.method_type,
            shipping_address=address,
            shipping_option=ShippingOption(id=shipping_method_direct.direct.id)
        ),
        shipping_contact=contact,
    )


@pytest.mark.asyncio
async def test_create_merchant_order_success_with_shipping_pickup(
    mock_merchant,
    yandex_pay_settings,
    user,
    sheet: PaymentSheet,
    shipping_method_pickup: ShippingMethod
):
    await CreateMerchantOrder(
        user=user,
        sheet=sheet,
        shipping_method=shipping_method_pickup,
    ).run()

    mock_merchant.assert_called_once_with(
        base_url=yandex_pay_settings.SPLIT_MERCHANT_URL,
        merchant_id=sheet.merchant.id,
        currency_code=sheet.currency_code,
        order=sheet.order,
        shipping_method_info=ShippingMethodInfo(
            type=shipping_method_pickup.method_type,
            pickup_point=PickupPoint(id=shipping_method_pickup.pickup.id)
        ),
        shipping_contact=None,
    )


@pytest.mark.asyncio
async def test_create_merchant_order_failed(
    caplog,
    mock_merchant,
    user,
    sheet: PaymentSheet,
):
    mock_merchant.side_effect = Exception

    with pytest.raises(Exception):
        await CreateMerchantOrder(
            user=user,
            sheet=sheet,
        ).run()

    assert merchant_order_create_failures.get()[0][1] == 1
    assert_that(
        caplog.records,
        has_item(
            has_properties(
                message='CREATE_MERCHANT_ORDER_ERROR',
            )
        )
    )
