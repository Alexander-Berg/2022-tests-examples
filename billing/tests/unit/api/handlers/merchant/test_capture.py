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
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.receipt import TaxType
from pay.lib.entities.shipping import ShippingMethodType, ShippingPrice

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import all_of, assert_that, greater_than_or_equal_to, less_than

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.authenticate import AuthenticateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.ops.capture import CaptureAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation


@pytest.mark.asyncio
async def test_calls_action(public_app, body, mock_capture_action, cart, shipping):
    order_id = 'merchant-order-id'
    await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/capture',
        json=body,
    )

    mock_capture_action.assert_run_once_with(
        merchant_id=UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'),
        order_amount=Decimal('123.45'),
        order_id=order_id,
        external_operation_id='merchant-op-id',
        cart=cart,
        shipping=shipping,
    )


@pytest.mark.asyncio
async def test_returned(
    public_app,
    body,
    expected_response,
):
    order_id = 'merchant-order-id'
    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/capture',
        json=body,
    )

    assert_that(r.status, all_of(greater_than_or_equal_to(200), less_than(300)))
    response_data = await r.json()
    assert_that(response_data['data'], equal_to(expected_response))


@pytest.fixture
def body():
    return {
        'orderAmount': '123.45',
        'externalOperationId': 'merchant-op-id',
        'cart': {
            'items': [
                {
                    'type': 'PHYSICAL',
                    'product_id': 'product_id',
                    'quantity': {
                        'count': '123.45',
                        'available': '123.45',
                        'label': 'label',
                    },
                    'total': '123.45',
                    'title': 'title',
                    'subtotal': '123.45',
                    'unit_price': '123.45',
                    'discounted_unit_price': '123.45',
                    'measurements': {
                        'weight': 1.5,
                        'height': 1.5,
                        'length': 1.5,
                        'width': 1.5,
                    },
                    'receipt': {'tax': 1},
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
        'shipping': {
            'method_type': 'COURIER',
            'amount': '123.45',
        },
    }


@pytest.fixture(autouse=True)
def mock_capture_action(mock_action, operation):  # noqa
    return mock_action(CaptureAction, return_value=operation)


@pytest.fixture(autouse=True)
def mock_merchant_authorization(mock_action):  # noqa
    return mock_action(AuthenticateMerchantAction, UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'))


@pytest.fixture
def operation(body, cart):
    return ensure_all_fields(
        Operation,
        operation_id=UUID('3ead36ed-5999-4e7b-810e-590ca13bea22'),
        merchant_id=UUID('a59f57bb-5f1a-4a8d-9101-f3f2b3b6f179'),
        checkout_order_id=UUID('8fb70911-1b4e-4dcf-86a4-37fa6e9538ec'),
        order_id='merchant-order-id',
        amount=Decimal('123.45'),
        operation_type=OperationType.CAPTURE,
        status=OperationStatus.PENDING,
        external_operation_id='external_operation_id',
        reason='reason',
        params={},
        cart=cart,
        shipping_method=None,
        created=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
        updated=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
    )


@pytest.fixture
def expected_response():
    return {
        'operation': {
            'operationId': '3ead36ed-5999-4e7b-810e-590ca13bea22',
            'amount': '123.45',
            'orderId': 'merchant-order-id',
            'operationType': 'CAPTURE',
            'status': 'PENDING',
            'externalOperationId': 'external_operation_id',
            'reason': 'reason',
            'params': {},
            'created': '2020-12-01T23:59:59+00:00',
            'updated': '2020-12-01T23:59:59+00:00',
        }
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
    return Cart(
        items=[
            ensure_all_fields(
                CartItem,
                type=CartItemType.PHYSICAL,
                product_id='product_id',
                quantity=ItemQuantity(
                    count=Decimal('123.45'),
                    available=Decimal('123.45'),
                    label='label',
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
