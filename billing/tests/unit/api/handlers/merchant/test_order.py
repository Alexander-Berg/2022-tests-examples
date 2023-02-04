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
    Measurements,
)
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.order import CardNetwork, Order, PaymentMethod, PaymentMethodType, PaymentStatus, RequiredFields
from pay.lib.entities.shipping import (
    Address,
    Contact,
    DeliveryStatus,
    ShippingMethodType,
    ShippingOptions,
    ShippingPrice,
)

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import all_of, assert_that, greater_than_or_equal_to, less_than

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.authenticate import AuthenticateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.get import GetMerchantOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.merchant_order import GetMerchantOrderResponse
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation


@pytest.mark.asyncio
async def test_calls_action(public_app, mock_get_merchant_order, cart, shipping):
    order_id = 'merchant-order-id'
    await public_app.get(
        f'/api/merchant/v1/orders/{order_id}',
    )

    mock_get_merchant_order.assert_run_once_with(
        merchant_id=UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'),
        order_id=order_id,
    )


@pytest.mark.asyncio
async def test_returned(
    public_app,
    expected_response,
):
    order_id = 'merchant-order-id'
    r = await public_app.get(
        f'/api/merchant/v1/orders/{order_id}',
    )

    assert_that(r.status, all_of(greater_than_or_equal_to(200), less_than(300)))
    response_data = await r.json()
    assert_that(response_data['data'], equal_to(expected_response))


@pytest.fixture(autouse=True)
def mock_get_merchant_order(mock_action, operation, delivery, order):  # noqa
    return mock_action(
        GetMerchantOrderAction,
        return_value=GetMerchantOrderResponse(
            order=order,
            operations=[operation],
            delivery=delivery,
        ),
    )


@pytest.fixture(autouse=True)
def mock_merchant_authorization(mock_action):  # noqa
    return mock_action(AuthenticateMerchantAction, UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'))


@pytest.fixture
def operation():
    return ensure_all_fields(Operation)(
        operation_id=UUID('3ead36ed-5999-4e7b-810e-590ca13bea22'),
        merchant_id=UUID('a59f57bb-5f1a-4a8d-9101-f3f2b3b6f179'),
        checkout_order_id=UUID('8fb70911-1b4e-4dcf-86a4-37fa6e9538ec'),
        order_id='order-id',
        amount=Decimal('123.45'),
        operation_type=OperationType.CAPTURE,
        status=OperationStatus.PENDING,
        external_operation_id='external_operation_id',
        reason='reason',
        params={'pa': 'rams'},
        cart=None,
        shipping_method=None,
        created=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
        updated=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
    )


@pytest.fixture
def delivery():
    return ensure_all_fields(Delivery)(
        checkout_order_id=UUID('3cae7f32-9ac8-4a93-a8f4-7c55284e73a1'),
        merchant_id=UUID('ccd08e7a-64be-4bbc-8433-cf837de2a635'),
        delivery_id=UUID('4158d702-009c-403c-88a4-249ba802b7db'),
        external_id='external_id',
        price=Decimal('123.45'),
        warehouse=StorageWarehouse(
            address=Address(
                country='',
                locality='',
                building='',
            ),
            emergency_contact=Contact(),
            contact=Contact(),
        ),
        status=DeliveryStatus.NEW,
        raw_status='',
        actual_price=Decimal('123.45'),
        version=1,
        revision=1,
        created=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
        updated=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
    )


@pytest.fixture
def order(shipping, cart, entity_shipping_method, entity_contact, entity_address):
    entity_shipping_method.courier_option.allowed_payment_methods = [PaymentMethodType.CARD]
    return ensure_all_fields(Order)(
        currency_code='XTS',
        cart=cart,
        checkout_order_id=UUID('3cae7f32-9ac8-4a93-a8f4-7c55284e73a1'),
        merchant_id=UUID('ccd08e7a-64be-4bbc-8433-cf837de2a635'),
        order_amount=Decimal('123.45'),
        order_id='order-id',
        t='',
        payment_method=PaymentMethod(
            method_type=PaymentMethodType.CARD, card_last4='1234', card_network=CardNetwork.MASTERCARD
        ),
        shipping=ShippingOptions(available_methods=[], available_courier_options=[]),
        shipping_method=entity_shipping_method,
        shipping_address=entity_address,
        shipping_contact=entity_contact,
        billing_contact=entity_contact,
        metadata='metadata',
        available_payment_methods=[PaymentMethodType.CARD],
        enable_coupons=True,
        enable_comment_field=True,
        required_fields=RequiredFields(),
        created=datetime(2022, 12, 30, 12, 59, 59, tzinfo=timezone.utc),
        updated=datetime(2022, 12, 30, 12, 59, 59, tzinfo=timezone.utc),
        payment_status=PaymentStatus.AUTHORIZED,
        reason='the-reason',
    )


@pytest.fixture
def expected_response():
    return {
        'order': {
            'created': '2022-12-30T12:59:59+00:00',
            'cart': {
                'cartId': 'cart-id',
                'measurements': {
                    'length': 1.5,
                    'width': 1.5,
                    'height': 1.5,
                    'weight': 1.5,
                },
                'total': {
                    'label': 'label',
                    'amount': '123.45',
                },
                'discounts': [
                    {'description': 'description', 'discountId': 'discount_id', 'amount': '123.45'},
                ],
                'items': [
                    {
                        'type': 'PHYSICAL',
                        'unitPrice': '123.45',
                        'measurements': {'length': 1.5, 'width': 1.5, 'height': 1.5, 'weight': 1.5},
                        'title': 'title',
                        'productId': 'product_id',
                        'total': '123.45',
                        'subtotal': '123.45',
                        'discountedUnitPrice': '123.45',
                        'quantity': {'label': 'label', 'count': '123.45', 'available': '123.45'},
                        'receipt': None,
                    }
                ],
                'externalId': 'external-id',
                'coupons': [{'status': 'VALID', 'value': 'value', 'description': 'description'}],
            },
            'paymentMethod': {'cardLast4': '1234', 'cardNetwork': 'MASTERCARD', 'methodType': 'CARD'},
            'paymentStatus': 'AUTHORIZED',
            'orderAmount': '123.45',
            'updated': '2022-12-30T12:59:59+00:00',
            'currencyCode': 'XTS',
            'orderId': 'order-id',
            'merchantId': 'ccd08e7a-64be-4bbc-8433-cf837de2a635',
            'reason': 'the-reason',
            'shippingMethod': {
                'yandexDeliveryOption': None,
                'courierOption': {
                    'title': 'Доставка курьером',
                    'fromDate': '2022-03-01',
                    'toTime': None,
                    'provider': 'CDEK',
                    'category': 'STANDARD',
                    'amount': '39.00',
                    'toDate': None,
                    'fromTime': None,
                    'courierOptionId': 'courier-1',
                    'receipt': None,
                    'allowedPaymentMethods': ['CARD'],
                },
                'pickupOption': None,
                'methodType': 'COURIER',
            },
            'metadata': 'metadata',
        },
        'operations': [
            {
                'reason': 'reason',
                'status': 'PENDING',
                'externalOperationId': 'external_operation_id',
                'operationId': '3ead36ed-5999-4e7b-810e-590ca13bea22',
                'orderId': 'order-id',
                'updated': '2020-12-01T23:59:59+00:00',
                'amount': '123.45',
                'created': '2020-12-01T23:59:59+00:00',
                'operationType': 'CAPTURE',
                'params': {'pa': 'rams'},
            }
        ],
        'delivery': {
            'price': '123.45',
            'actualPrice': '123.45',
            'created': '2020-12-01T23:59:59+00:00',
            'updated': '2020-12-01T23:59:59+00:00',
            'status': 'NEW',
        },
    }


@pytest.fixture
def shipping():
    return ensure_all_fields(
        ShippingPrice,
        method_type=ShippingMethodType.COURIER,
        amount=Decimal('123.45'),
    )


@pytest.fixture
def cart():
    return ensure_all_fields(Cart)(
        cart_id='cart-id',
        items=[
            ensure_all_fields(CartItem)(
                type=CartItemType.PHYSICAL,
                product_id='product_id',
                quantity=ItemQuantity(
                    count=Decimal('123.45'),
                    available=Decimal('123.45'),
                    label='label',
                ),
                total=Decimal('123.45'),
                title='title',
                subtotal=Decimal('123.45'),
                unit_price=Decimal('123.45'),
                discounted_unit_price=Decimal('123.45'),
                measurements=ensure_all_fields(Measurements)(
                    weight=1.5,
                    height=1.5,
                    length=1.5,
                    width=1.5,
                ),
                receipt=None,
            )
        ],
        total=ensure_all_fields(CartTotal)(amount=Decimal('123.45'), label='label'),
        external_id='external-id',
        coupons=[ensure_all_fields(Coupon)(value='value', status=CouponStatus.VALID, description='description')],
        discounts=[
            ensure_all_fields(Discount)(
                discount_id='discount_id',
                amount=Decimal('123.45'),
                description='description',
            )
        ],
        measurements=ensure_all_fields(Measurements)(
            weight=1.5,
            height=1.5,
            length=1.5,
            width=1.5,
        ),
    )
