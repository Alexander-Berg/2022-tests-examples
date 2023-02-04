from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.operation import OperationStatus, OperationType

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, spy_action  # noqa
from sendr_utils import alist

from hamcrest import assert_that, has_entries, match_equality, not_none

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.ops.cancel import CancelPaymentAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.cancel import CancelAsyncableAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CoreInvalidPaymentStatusError, OrderNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageCart
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TaskType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import TransactionStatus


@pytest.mark.asyncio
async def test_returned(params, checkout_order, stored_merchant):
    operation = await CancelPaymentAction(**params).run()

    assert_that(
        operation,
        equal_to(
            ensure_all_fields(
                Operation,
                operation_id=match_equality(not_none()),
                checkout_order_id=checkout_order.checkout_order_id,
                order_id=checkout_order.order_id,
                merchant_id=stored_merchant.merchant_id,
                amount=Decimal('100'),
                operation_type=OperationType.VOID,
                status=OperationStatus.PENDING,
                external_operation_id='merchant-op-id',
                reason=None,
                params={'cancel_reason': 'the-reason'},
                cart=StorageCart.from_cart(checkout_order.cart),
                shipping_method=None,
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
            )
        ),
    )


@pytest.mark.asyncio
async def test_does_not_update_order(storage, params, checkout_order, stored_merchant):
    await CancelPaymentAction(**params).run()

    assert_that(
        await storage.checkout_order.get(checkout_order.checkout_order_id),
        equal_to(checkout_order),
    )


@pytest.mark.asyncio
async def test_schedules_cancel_operation(params, checkout_order, stored_merchant, storage, transaction):
    operation = await CancelPaymentAction(**params).run()

    [task] = await alist(
        storage.task.find(
            filters={'task_type': TaskType.RUN_ACTION, 'action_name': CancelAsyncableAction.action_name},
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
async def test_invalid_payment_status(storage, params, transaction):
    transaction.status = TransactionStatus.NEW
    await storage.transaction.save(transaction)

    with pytest.raises(CoreInvalidPaymentStatusError):
        await CancelPaymentAction(**params).run()


@pytest.mark.asyncio
async def test_order_not_found(storage, params):
    params['order_id'] += '-nonexistent'
    with pytest.raises(OrderNotFoundError):
        await CancelPaymentAction(**params).run()


@pytest.mark.asyncio
async def test_order_not_found_if_merchant_is_wrong(storage, entity_merchant, checkout_order, params):
    merchant = await storage.merchant.create(entity_merchant)
    checkout_order.merchant_id = merchant.merchant_id
    checkout_order = await storage.checkout_order.save(checkout_order)

    with pytest.raises(OrderNotFoundError):
        await CancelPaymentAction(**params).run()


@pytest.fixture
def params(checkout_order):
    return {
        'merchant_id': checkout_order.merchant_id,
        'order_id': checkout_order.order_id,
        'external_operation_id': 'merchant-op-id',
        'reason': 'the-reason',
    }


@pytest.fixture
async def checkout_order(storage, stored_checkout_order):
    return await storage.checkout_order.save(
        replace(
            stored_checkout_order,
            authorize_amount=Decimal('100'),
            order_amount=Decimal('100'),
        )
    )


@pytest.fixture(autouse=True)
async def transaction(storage, stored_transaction):
    return await storage.transaction.save(
        replace(
            stored_transaction,
            status=TransactionStatus.AUTHORIZED,
        )
    )
