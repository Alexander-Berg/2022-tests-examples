from datetime import datetime, timezone
from decimal import Decimal
from uuid import UUID

import pytest
from cryptography.fernet import Fernet
from freezegun import freeze_time
from pay.lib.entities.cart import (
    Cart,
    CartItem,
    CartItemType,
    CartTotal,
    Coupon,
    CouponStatus,
    Discount,
    ItemQuantity,
    ItemReceipt,
    Measurements,
)
from pay.lib.entities.order import CardNetwork, Order, PaymentMethod, PaymentMethodType
from pay.lib.entities.receipt import TaxType
from pay.lib.entities.shipping import (
    Address,
    DeliveryCategory,
    Location,
    ShippingMethod,
    ShippingMethodType,
    ShippingOptions,
    ShippingWarehouse,
    YandexDeliveryOption,
    YandexDeliveryShippingParams,
)

from sendr_core.exceptions import CoreFailError
from sendr_pytest.helpers import ensure_all_fields
from sendr_utils import utcnow

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.order_token import (
    DecryptOrderTokenCryptogramAction,
    GetOrderTokenCryptogramAction,
    create_order_token_crypter,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.merchant_order import MerchantOrderToken
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import OrderTokenDecryptionError


@pytest.fixture
def cart():
    return Cart(
        items=[
            ensure_all_fields(
                CartItem,
                type=CartItemType.PHYSICAL,
                product_id='product_id',
                quantity=ItemQuantity(
                    count=Decimal('123.45'),
                    available=Decimal('123.45'),
                ),
                receipt=ItemReceipt(
                    tax=TaxType.VAT_20,
                ),
                total=Decimal('123.45'),
                title='title',
                subtotal=Decimal('123.45'),
                unit_price=Decimal('123.45'),
                discounted_unit_price=Decimal('123.45'),
                measurements=ensure_all_fields(
                    Measurements,
                    weight=1.5,
                    height=1.5,
                    length=1.5,
                    width=1.5,
                ),
            )
        ],
        total=ensure_all_fields(CartTotal, amount=Decimal('123.45'), label='label'),
        external_id='external-id',
        coupons=[ensure_all_fields(Coupon, value='value', status=CouponStatus.VALID, description='description')],
        discounts=[
            ensure_all_fields(
                Discount,
                discount_id='discount_id',
                amount=Decimal('123.45'),
                description='description',
            )
        ],
        measurements=ensure_all_fields(
            Measurements,
            weight=1.5,
            height=1.5,
            length=1.5,
            width=1.5,
        ),
    )


@pytest.fixture
def order(cart, entity_address, entity_contact):
    o = Order(
        currency_code='XTS',
        cart=cart,
        checkout_order_id=UUID('344eaee8-7775-4b30-9e56-d2d7cdaa2c9a'),
        merchant_id=UUID('25c1d927-6a8a-4e58-a73d-4a72c0e4ff20'),
        order_amount=Decimal('123.45'),
        order_id='order-id',
        payment_method=PaymentMethod(
            method_type=PaymentMethodType.CARD, card_last4='0000', card_network=CardNetwork.MASTERCARD
        ),
        shipping=ShippingOptions(
            available_methods=[ShippingMethodType.YANDEX_DELIVERY],
            available_courier_options=[],
            yandex_delivery=YandexDeliveryShippingParams(
                warehouse=ShippingWarehouse(
                    address=entity_address,
                    contact=entity_contact,
                    emergency_contact=entity_contact,
                ),
                options=[
                    ensure_all_fields(YandexDeliveryOption)(
                        receipt=ItemReceipt(tax=TaxType.VAT_20),
                        amount=Decimal('10.01'),
                        title='Доставка',
                        category=DeliveryCategory.TODAY,
                        allowed_payment_methods=[PaymentMethodType.CARD],
                        yandex_delivery_option_id='yd-option-id',
                        from_datetime=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                        to_datetime=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                    ),
                ],
            ),
        ),
        shipping_address=Address(
            country='country',
            locality='locality',
            street='street',
            building='building',
            region='region',
            id='id',
            room='room',
            entrance='entrance',
            floor='floor',
            intercom='intercom',
            comment='comment',
            zip='zip',
            location=Location(
                latitude=30.15,
                longitude=15.30,
            ),
            locale='locale',
            address_line='address_line',
            district='district',
        ),
        metadata='metadata',
    )

    assert o.shipping
    assert o.shipping.yandex_delivery
    assert o.shipping.yandex_delivery.options

    o.shipping_method = ShippingMethod(
        method_type=ShippingMethodType.YANDEX_DELIVERY,
        yandex_delivery_option=o.shipping.yandex_delivery.options[0],
    )

    return o


@pytest.mark.asyncio
async def test_get_order_token_cryptogramm(order, stored_merchant, entity_auth_user):
    token = await GetOrderTokenCryptogramAction(
        user=entity_auth_user,
        merchant_id=stored_merchant.merchant_id,
        order=order,
    ).run()

    assert token is not None


@pytest.mark.asyncio
async def test_decrypt_order_token_cryptogramm__bad_token(yandex_pay_plus_settings):
    with pytest.raises(OrderTokenDecryptionError):
        await DecryptOrderTokenCryptogramAction(token='bad token').run()

    with pytest.raises(OrderTokenDecryptionError):
        f = Fernet(Fernet.generate_key())
        token = f.encrypt(b'{"hello": "world"}')
        await DecryptOrderTokenCryptogramAction(token=f"1.{token.decode('utf-8')}").run()

    with pytest.raises(CoreFailError):
        crypter = create_order_token_crypter()
        token = crypter.encrypt('{"hello": "world"}')
        await DecryptOrderTokenCryptogramAction(token=token).run()


@pytest.mark.asyncio
@freeze_time(utcnow())
async def test_decrypt_order_token__success(order, stored_merchant, entity_auth_user):
    token = await GetOrderTokenCryptogramAction(
        user=entity_auth_user,
        merchant_id=stored_merchant.merchant_id,
        order=order,
    ).run()

    order_token = await DecryptOrderTokenCryptogramAction(token=token).run()

    assert order_token == MerchantOrderToken(
        uid=entity_auth_user.uid,
        merchant_id=stored_merchant.merchant_id,
        cart_hash=order.cart.hash(),
        token_created=utcnow(),
        shipping_warehouse=order.shipping.yandex_delivery.warehouse,
        shipping_options=order.shipping.yandex_delivery.options,
        shipping_address_id=order.shipping_address.id,
    )
