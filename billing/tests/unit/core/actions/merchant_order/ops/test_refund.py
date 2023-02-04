from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.cart import Cart, CartItem, CartTotal, ItemQuantity, ItemReceipt
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.receipt import TaxType
from pay.lib.entities.shipping import ShippingMethodType, ShippingPrice

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, spy_action  # noqa
from sendr_utils import alist

from hamcrest import assert_that, has_entries, has_property, match_equality, not_none

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.ops.refund import RefundAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.prices import UpdatePricesAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.refund import RefundAsyncableAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreInvalidPaymentStatusError,
    OrderAmountExpectationError,
    OrderNotFoundError,
    RefundAmountExceededError,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageCart, StorageShippingMethod
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TaskType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import TransactionStatus


@pytest.mark.asyncio
async def test_returned(params, checkout_order, stored_merchant):
    operation = await RefundAction(**params).run()

    assert_that(
        operation,
        equal_to(
            ensure_all_fields(
                Operation,
                operation_id=match_equality(not_none()),
                checkout_order_id=checkout_order.checkout_order_id,
                order_id=checkout_order.order_id,
                merchant_id=stored_merchant.merchant_id,
                amount=Decimal('10'),
                operation_type=OperationType.REFUND,
                status=OperationStatus.PENDING,
                external_operation_id='merchant-op-id',
                reason=None,
                params={},
                cart=StorageCart.from_cart(params['cart']),
                shipping_method=replace(
                    checkout_order.shipping_method,
                    courier_option=replace(
                        checkout_order.shipping_method.courier_option,
                        amount=Decimal('1'),
                    ),
                ),
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
            )
        ),
    )


@pytest.mark.asyncio
async def test_does_not_update_order(storage, params, checkout_order, stored_merchant):
    await RefundAction(**params).run()

    assert_that(
        await storage.checkout_order.get(checkout_order.checkout_order_id),
        equal_to(checkout_order),
    )


@pytest.mark.asyncio
async def test_schedules_refund_operation(params, checkout_order, stored_merchant, storage, transaction):
    operation = await RefundAction(**params).run()

    [task] = await alist(
        storage.task.find(
            filters={'task_type': TaskType.RUN_ACTION, 'action_name': RefundAsyncableAction.action_name},
            order=('-created',),
            limit=1,
        )
    )
    assert_that(
        task.params,
        has_entries(
            {
                'action_kwargs': {
                    'operation_id': str(operation.operation_id),
                    'transaction_id': str(transaction.transaction_id),
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_calls_update_prices(storage, params, checkout_order, spy_action):  # noqa
    spy = spy_action(UpdatePricesAction)

    await RefundAction(**params).run()

    spy.assert_run_once_with(
        checkout_order=match_equality(has_property('checkout_order_id', checkout_order.checkout_order_id)),
        cart=params['cart'],
        shipping=params['shipping'],
    )


@pytest.mark.asyncio
async def test_invalid_payment_status(storage, params, transaction):
    transaction.status = TransactionStatus.NEW
    await storage.transaction.save(transaction)

    with pytest.raises(CoreInvalidPaymentStatusError):
        await RefundAction(**params).run()


@pytest.mark.asyncio
async def test_order_not_found(storage, params):
    params['order_id'] += '-nonexistent'
    with pytest.raises(OrderNotFoundError):
        await RefundAction(**params).run()


@pytest.mark.asyncio
async def test_order_not_found_if_merchant_is_wrong(storage, entity_merchant, checkout_order, params):
    merchant = await storage.merchant.create(entity_merchant)
    checkout_order.merchant_id = merchant.merchant_id
    checkout_order = await storage.checkout_order.save(checkout_order)

    with pytest.raises(OrderNotFoundError):
        await RefundAction(**params).run()


@pytest.mark.asyncio
async def test_amount_expectation_failed(storage, checkout_order, params, transaction):
    params['order_amount'] = Decimal('10.00')
    params['refund_amount'] = Decimal('100.00')
    params['cart'].total.amount = Decimal('0')
    params['shipping'].amount = Decimal('0')

    with pytest.raises(OrderAmountExpectationError) as exc_info:
        await RefundAction(**params).run()

    assert_that(
        exc_info.value.params,
        equal_to(
            {
                'expected_order_amount': '10.00',
                'current_order_amount': '100.00',
                'refund_amount': '100.00',
                'description': match_equality(not_none()),
            }
        ),
    )


@pytest.mark.asyncio
async def test_amount_exceeded(storage, checkout_order, params, transaction):
    params['order_amount'] = Decimal('-5.00')
    params['refund_amount'] = Decimal('105.00')
    params['cart'].total.amount = Decimal('0')
    params['shipping'].amount = Decimal('0')

    with pytest.raises(RefundAmountExceededError) as exc_info:
        await RefundAction(**params).run()

    assert_that(
        exc_info.value.params,
        equal_to(
            {
                'max_amount': '100.00',
                'requested_amount': '105.00',
            }
        ),
    )


@pytest.fixture
def params(checkout_order, shipping):
    return {
        'merchant_id': checkout_order.merchant_id,
        'order_id': checkout_order.order_id,
        'order_amount': Decimal('90'),
        'refund_amount': Decimal('10'),
        'external_operation_id': 'merchant-op-id',
        'cart': Cart(
            items=[
                CartItem(
                    product_id='refund_product',
                    title='title',
                    total=Decimal('15'),
                    discounted_unit_price=Decimal('15'),
                    quantity=ItemQuantity(count=Decimal('1')),
                    receipt=ItemReceipt(
                        tax=TaxType.VAT_20,
                    ),
                ),
            ],
            total=CartTotal(amount=Decimal('89')),
        ),
        'shipping': shipping,
    }


@pytest.fixture
def shipping():
    return ShippingPrice(
        amount=Decimal('1'),
        method_type=ShippingMethodType.COURIER,
    )


@pytest.fixture
async def checkout_order(storage, entity_shipping_method, stored_checkout_order):
    entity_shipping_method.courier_option.amount = Decimal('0')
    return await storage.checkout_order.save(
        replace(
            stored_checkout_order,
            authorize_amount=Decimal('100'),
            capture_amount=Decimal('100'),
            order_amount=Decimal('100'),
            shipping_method=StorageShippingMethod.from_shipping_method(entity_shipping_method),
        )
    )


@pytest.fixture(autouse=True)
async def transaction(storage, stored_transaction):
    return await storage.transaction.save(
        replace(
            stored_transaction,
            status=TransactionStatus.CHARGED,
        )
    )
