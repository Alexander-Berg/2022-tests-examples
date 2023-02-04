from dataclasses import replace
from datetime import datetime, timezone
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
from pay.lib.entities.order import CardNetwork, Order, PaymentMethod, PaymentMethodType
from pay.lib.entities.receipt import TaxType
from pay.lib.entities.shipping import (
    Address,
    DeliveryCategory,
    Location,
    ShippingMethodType,
    ShippingOptions,
    ShippingWarehouse,
    YandexDeliveryOption,
    YandexDeliveryShippingParams,
)

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import all_of, assert_that, greater_than_or_equal_to, less_than

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.render import RenderMerchantOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.form_order import RenderMerchantOrderRequest


@pytest.mark.asyncio
async def test_calls_action(
    public_app,
    mock_user_authentication,
    body,
    headers,
    mock_render_merchant_order,
    expected_rendermerchantorderrequest,
    entity_auth_user,
    pay_session_id,
):
    await public_app.post(
        '/api/public/v1/orders/render',
        headers=headers,
        json=body,
    )

    mock_render_merchant_order.assert_run_once_with(
        user=entity_auth_user,
        pay_session_id=pay_session_id,
        rendermerchantorderrequest=expected_rendermerchantorderrequest,
    )


@pytest.mark.asyncio
async def test_calls_action__when_optionals_omitted(
    public_app,
    mock_user_authentication,
    body,
    headers,
    mock_render_merchant_order,
    expected_rendermerchantorderrequest,
    entity_auth_user,
    pay_session_id,
):
    body['cart'] = dict(body['cart'])
    body['cart'].pop('total')
    await public_app.post(
        '/api/public/v1/orders/render',
        headers=headers,
        json=body,
    )

    expected_rendermerchantorderrequest = replace(
        expected_rendermerchantorderrequest,
        cart=replace(
            expected_rendermerchantorderrequest.cart,
            total=None,
        ),
    )
    mock_render_merchant_order.assert_run_once_with(
        user=entity_auth_user,
        pay_session_id=pay_session_id,
        rendermerchantorderrequest=expected_rendermerchantorderrequest,
    )


@pytest.mark.asyncio
async def test_returned(
    public_app,
    mock_user_authentication,
    body,
    headers,
    mock_render_merchant_order,
    expected_response,
):
    r = await public_app.post(
        '/api/public/v1/orders/render',
        headers=headers,
        json=body,
    )

    assert_that(r.status, all_of(greater_than_or_equal_to(200), less_than(300)))
    response_data = await r.json()
    assert_that(response_data['data'], equal_to(expected_response))


@pytest.mark.asyncio
async def test_minimal_cart(
    public_app,
    mock_user_authentication,
    headers,
    mock_render_merchant_order,
    expected_response,
):
    r = await public_app.post(
        '/api/public/v1/orders/render',
        headers=headers,
        json={
            'merchant_id': '25c1d927-6a8a-4e58-a73d-4a72c0e4ff20',
            'currency_code': 'XTS',
            'cart': {
                'items': [
                    {'product_id': 'product_id', 'quantity': {'count': '123.45'}},
                ],
            },
        },
    )

    assert_that(r.status, all_of(greater_than_or_equal_to(200), less_than(300)))
    response_data = await r.json()
    assert_that(response_data['data'], equal_to(expected_response))


@pytest.mark.asyncio
async def test_returned__when_required_are_omitted(
    public_app,
    mock_user_authentication,
    headers,
    mock_render_merchant_order,
):
    r = await public_app.post(
        '/api/public/v1/orders/render',
        headers=headers,
        json={},
    )

    assert_that(r.status, equal_to(400))
    response_data = await r.json()
    assert_that(
        response_data['data'],
        equal_to(
            {
                'message': 'BAD_FORMAT',
                'params': {
                    'cart': {'items': ['Missing data for required field.']},
                    'merchant_id': ['Missing data for required field.'],
                    'currency_code': ['Missing data for required field.'],
                },
            }
        ),
    )


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
        'cart': {
            'items': [
                {
                    'type': 'PHYSICAL',
                    'total': '123.45',
                    'subtotal': '123.45',
                    'quantity': {'available': '123.45', 'count': '123.45'},
                    'discounted_unit_price': '123.45',
                    'title': 'title',
                    'measurements': {'weight': 1.5, 'width': 1.5, 'height': 1.5, 'length': 1.5},
                    'receipt': {'tax': 1},
                    'unit_price': '123.45',
                    'product_id': 'product_id',
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
        'metadata': 'metadata',
    }


@pytest.fixture
def expected_response(body):
    expected = dict(body)
    expected.pop('shipping_address_id')
    expected['shipping_address'] = dict(
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
        location=dict(
            latitude=30.15,
            longitude=15.30,
        ),
        locale='locale',
        address_line='address_line',
        district='district',
    )
    expected['shipping'] = {
        'available_methods': ['YANDEX_DELIVERY'],
        'available_courier_options': [],
        'yandex_delivery': {
            'options': [
                {
                    'yandex_delivery_option_id': 'yd-option-id',
                    'amount': '10.01',
                    'title': 'Доставка',
                    'category': 'TODAY',
                    'from_datetime': '2022-01-01T00:00:00+00:00',
                    'to_datetime': '2022-01-01T00:00:00+00:00',
                    'allowed_payment_methods': ['CARD'],
                    'receipt': {'tax': 1},
                },
            ]
        }
    }
    expected['order_id'] = 'order-id'
    expected['checkout_order_id'] = '344eaee8-7775-4b30-9e56-d2d7cdaa2c9a'
    expected['t'] = '123456'
    return {'order': expected}


@pytest.fixture
def expected_rendermerchantorderrequest(cart):
    return ensure_all_fields(
        RenderMerchantOrderRequest,
        merchant_id=UUID('25c1d927-6a8a-4e58-a73d-4a72c0e4ff20'),
        currency_code='XTS',
        cart=cart,
        order_id='order-id',
        payment_method=ensure_all_fields(
            PaymentMethod, method_type=PaymentMethodType.CARD, card_last4='0000', card_network=CardNetwork.MASTERCARD
        ),
        shipping_address_id='ship-a-id',
        metadata='metadata',
    )


@pytest.fixture
def pay_session_id():
    return 'sessid'


@pytest.fixture
def order(cart, entity_address, entity_contact):
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
                ]
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
        t='123456',
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
def mock_render_merchant_order(mock_action, order):  # noqa
    return mock_action(RenderMerchantOrderAction, return_value=order)
