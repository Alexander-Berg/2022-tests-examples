from datetime import date, datetime, time, timezone
from decimal import Decimal
from uuid import UUID

import pytest
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
from pay.lib.entities.order import CardNetwork, Contact, Order, PaymentMethod, PaymentMethodType
from pay.lib.entities.receipt import TaxType
from pay.lib.entities.shipping import (
    Address,
    CourierOption,
    DeliveryCategory,
    Location,
    PickupOption,
    PickupSchedule,
    ShippingMethod,
    ShippingMethodType,
    YandexDeliveryOption,
)

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import all_of, assert_that, greater_than_or_equal_to, less_than

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.create import CreateMerchantOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.form_order import CreateMerchantOrderRequest


@pytest.mark.asyncio
async def test_calls_action(
    public_app,
    mock_user_authentication,
    body,
    headers,
    mock_create_merchant_order,
    expected_createmerchantorderrequest,
    entity_auth_user,
    pay_session_id,
):
    await public_app.post(
        '/api/public/v1/orders/create',
        headers=headers,
        json=body,
    )

    mock_create_merchant_order.assert_run_once_with(
        user=entity_auth_user,
        pay_session_id=pay_session_id,
        createmerchantorderrequest=expected_createmerchantorderrequest,
    )


@pytest.mark.asyncio
async def test_returned(
    public_app,
    mock_user_authentication,
    body,
    headers,
    mock_create_merchant_order,
    expected_response,
):
    r = await public_app.post(
        '/api/public/v1/orders/create',
        headers=headers,
        json=body,
    )

    assert_that(r.status, all_of(greater_than_or_equal_to(200), less_than(300)))
    response_data = await r.json()
    assert_that(response_data['data'], equal_to(expected_response))


@pytest.fixture
def headers():
    return {
        'x-pay-session-id': 'sessid',
    }


@pytest.fixture
def body():
    return {
        'merchant_id': '25c1d927-6a8a-4e58-a73d-4a72c0e4ff20',
        'currency_code': 'XTS',
        't': '',
        'cart': {
            'items': [
                {
                    'type': 'PHYSICAL',
                    'subtotal': '123.45',
                    'discounted_unit_price': '123.45',
                    'unit_price': '123.45',
                    'title': 'title',
                    'receipt': {'tax': 1, 'product_code': 'MTIz'},
                    'total': '123.45',
                    'quantity': {'count': '123.45', 'available': '123.45'},
                    'product_id': 'product_id',
                    'measurements': {'length': 1.5, 'weight': 1.5, 'height': 1.5, 'width': 1.5},
                }
            ],
            'total': {'amount': '123.45', 'label': 'label'},
            'external_id': 'external-id',
            'coupons': [{'value': 'value', 'status': 'VALID', 'description': 'description'}],
            'discounts': [
                {
                    'discount_id': 'discount_id',
                    'amount': '123.45',
                    'description': 'description',
                }
            ],
            'measurements': {
                'weight': 1.5,
                'height': 1.5,
                'length': 1.5,
                'width': 1.5,
            },
        },
        'order_id': 'order-id',
        'order_amount': '123.45',
        'payment_method': {'method_type': 'CARD', 'card_last4': '0000', 'card_network': 'MASTERCARD'},
        'shipping_address_id': 'ship-a-id',
        'shipping_contact_id': 'ship-c-id',
        'billing_contact': {'id': 'bill-c-id'},
        'metadata': 'metadata',
        'shipping_method': {
            'method_type': 'COURIER',
            'courier_option': {
                'courier_option_id': 'c-id',
                'provider': 'provider',
                'category': 'STANDARD',
                'title': 'label',
                'amount': '123.45',
                'from_date': '2000-01-01',
                'to_date': '2000-12-31',
                'from_time': '00:00:00',
                'to_time': '23:59:59',
            },
            'pickup_option': {
                'pickup_point_id': 'pickup_point_id',
                'provider': 'provider',
                'location': {'latitude': 30.15, 'longitude': 15.30},
                'title': 'title',
                'address': 'address',
                'from_date': '2000-01-01',
                'to_date': '2000-12-31',
                'amount': '123.45',
                'description': 'description',
                'phones': ['phone1'],
                'storage_period': 1,
                'schedule': [
                    {
                        'label': 'label',
                        'from_time': '00:00:00',
                        'to_time': '23:59:59',
                    }
                ],
            },
            'yandex_delivery_option': {
                'yandex_delivery_option_id': 'yd-option-id',
                'amount': '10.01',
                'title': 'Доставка',
                'category': 'TODAY',
                'from_datetime': '2022-01-01T00:00:00+00:00',
                'to_datetime': '2022-01-01T00:00:00+00:00',
                'allowed_payment_methods': ['CARD'],
                'receipt': {'tax': 1},
            },
        },
    }


@pytest.fixture
def expected_response(body):
    expected = dict(body)
    expected.pop('shipping_contact_id')
    expected.pop('shipping_address_id')
    expected.pop('t')
    expected['shipping_address'] = {
        'country': 'country',
        'locality': 'locality',
        'street': 'street',
        'building': 'building',
        'region': 'region',
        'id': 'id',
        'room': 'room',
        'entrance': 'entrance',
        'floor': 'floor',
        'intercom': 'intercom',
        'comment': 'comment',
        'zip': 'zip',
        'location': {
            'latitude': 30.15,
            'longitude': 15.30,
        },
        'locale': 'locale',
        'address_line': 'address_line',
        'district': 'district',
    }
    expected['billing_contact'] = expected['shipping_contact'] = {
        'id': 'id',
        'first_name': 'first_name',
        'second_name': 'second_name',
        'last_name': 'last_name',
        'email': 'email',
        'phone': 'phone',
    }
    expected['order_id'] = 'order-id'
    expected['checkout_order_id'] = '344eaee8-7775-4b30-9e56-d2d7cdaa2c9a'
    return {'order': expected}


@pytest.fixture
def expected_createmerchantorderrequest(shipping_method, cart):
    return ensure_all_fields(
        CreateMerchantOrderRequest,
        shipping_method=shipping_method,
        merchant_id=UUID('25c1d927-6a8a-4e58-a73d-4a72c0e4ff20'),
        currency_code='XTS',
        cart=cart,
        t='',
        order_id='order-id',
        order_amount=Decimal('123.45'),
        payment_method=ensure_all_fields(
            PaymentMethod, method_type=PaymentMethodType.CARD, card_last4='0000', card_network=CardNetwork.MASTERCARD
        ),
        shipping_address_id='ship-a-id',
        shipping_contact_id='ship-c-id',
        billing_contact=Contact(id='bill-c-id'),
        metadata='metadata',
    )


@pytest.fixture
def pay_session_id():
    return 'sessid'


@pytest.fixture
def order(cart, shipping_method):
    return Order(
        currency_code='XTS',
        cart=cart,
        checkout_order_id=UUID('344eaee8-7775-4b30-9e56-d2d7cdaa2c9a'),
        merchant_id=UUID('25c1d927-6a8a-4e58-a73d-4a72c0e4ff20'),
        order_amount=Decimal('123.45'),
        order_id='order-id',
        payment_method=PaymentMethod(
            method_type=PaymentMethodType.CARD, card_last4='0000', card_network=CardNetwork.MASTERCARD
        ),
        shipping_method=shipping_method,
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
        shipping_contact=Contact(
            id='id',
            first_name='first_name',
            second_name='second_name',
            last_name='last_name',
            email='email',
            phone='phone',
        ),
        billing_contact=Contact(
            id='id',
            first_name='first_name',
            second_name='second_name',
            last_name='last_name',
            email='email',
            phone='phone',
        ),
        metadata='metadata',
    )


@pytest.fixture
def shipping_method():
    return ensure_all_fields(
        ShippingMethod,
        method_type=ShippingMethodType.COURIER,
        courier_option=ensure_all_fields(
            CourierOption,
            courier_option_id='c-id',
            provider='provider',
            category=DeliveryCategory.STANDARD,
            title='label',
            amount=Decimal('123.45'),
            from_date=date(2000, 1, 1),
            to_date=date(2000, 12, 31),
            from_time=time(0, 0, 0),
            to_time=time(23, 59, 59),
            receipt=None,
            allowed_payment_methods=None,
        ),
        pickup_option=ensure_all_fields(
            PickupOption,
            pickup_point_id='pickup_point_id',
            provider='provider',
            location=ensure_all_fields(Location, latitude=30.15, longitude=15.30),
            title='title',
            address='address',
            from_date=date(2000, 1, 1),
            to_date=date(2000, 12, 31),
            amount=Decimal('123.45'),
            description='description',
            phones=['phone1'],
            storage_period=1,
            schedule=[
                ensure_all_fields(
                    PickupSchedule,
                    label='label',
                    from_time=time(0, 0, 0),
                    to_time=time(23, 59, 59),
                ),
            ],
            receipt=None,
            allowed_payment_methods=None,
        ),
        yandex_delivery_option=ensure_all_fields(
            YandexDeliveryOption,
            receipt=ItemReceipt(tax=TaxType.VAT_20),
            amount=Decimal('10.01'),
            title='Доставка',
            category=DeliveryCategory.TODAY,
            allowed_payment_methods=[PaymentMethodType.CARD],
            yandex_delivery_option_id='yd-option-id',
            from_datetime=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
            to_datetime=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
        ),
    )


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
                    product_code=b'123',
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


@pytest.fixture(autouse=True)
def mock_create_merchant_order(mock_action, order):  # noqa
    return mock_action(CreateMerchantOrderAction, return_value=order)
