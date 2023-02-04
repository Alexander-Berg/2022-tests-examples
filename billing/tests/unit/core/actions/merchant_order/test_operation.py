from dataclasses import replace
from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.enums import OperationStatus, OperationType

from sendr_pytest.helpers import ensure_all_fields

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.operation import (
    CreateOrderOperationAction,
    GetOrderOperationByExternalIdAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    AnotherOperationInProgressError,
    OperationNotFoundError,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation


@pytest.mark.asyncio
async def test_success(operation):
    result = await GetOrderOperationByExternalIdAction(
        merchant_id=operation.merchant_id,
        external_operation_id='ext-op-id',
    ).run()

    assert_that(result, equal_to(operation))


@pytest.mark.asyncio
async def test_operation_doesnt_exist(stored_operation, rands):
    with pytest.raises(OperationNotFoundError):
        await GetOrderOperationByExternalIdAction(
            merchant_id=stored_operation.merchant_id, external_operation_id=rands()
        ).run()


@pytest.mark.asyncio
async def test_operation_doesnt_belong_to_merchant(stored_operation):
    with pytest.raises(OperationNotFoundError):
        await GetOrderOperationByExternalIdAction(
            merchant_id=uuid4(),
            external_operation_id='ext-op-id',
        ).run()


@pytest.mark.asyncio
async def test_creates_operation(stored_checkout_order):
    operation = await CreateOrderOperationAction(
        checkout_order=stored_checkout_order,
        amount=Decimal('123'),
        operation_type=OperationType.CAPTURE,
        external_id='ext-id',
        params=dict(a='b'),
    ).run()

    expected = ensure_all_fields(Operation)(
        operation_id=operation.operation_id,
        merchant_id=stored_checkout_order.merchant_id,
        checkout_order_id=stored_checkout_order.checkout_order_id,
        order_id=stored_checkout_order.order_id,
        operation_type=OperationType.CAPTURE,
        status=OperationStatus.PENDING,
        amount=Decimal('123'),
        external_operation_id='ext-id',
        cart=stored_checkout_order.cart,
        shipping_method=stored_checkout_order.shipping_method,
        params=dict(a='b'),
        reason=None,
        created=operation.created,
        updated=operation.updated,
    )
    assert_that(operation, equal_to(expected))


@pytest.mark.asyncio
async def test_returns_existing_operation(stored_checkout_order, stored_operation):
    operation = await CreateOrderOperationAction(
        checkout_order=stored_checkout_order,
        amount=stored_operation.amount,
        operation_type=stored_operation.operation_type,
        external_id=stored_operation.external_operation_id,
        params=stored_operation.params,
    ).run()

    assert_that(operation, equal_to(stored_operation))


@pytest.mark.asyncio
async def test_another_operation_in_progress(stored_checkout_order, stored_operation):
    with pytest.raises(AnotherOperationInProgressError) as exc_info:
        await CreateOrderOperationAction(
            checkout_order=stored_checkout_order,
            amount=Decimal('123'),
            operation_type=OperationType.CAPTURE,
        ).run()

    assert_that(
        exc_info.value.description,
        equal_to('Another AUTHORIZE operation already in progress. Please wait until it finishes'),
    )


@pytest.fixture
async def operation(storage, stored_operation):
    return await storage.order_operation.save(
        replace(
            stored_operation,
            external_operation_id='ext-op-id',
        )
    )
