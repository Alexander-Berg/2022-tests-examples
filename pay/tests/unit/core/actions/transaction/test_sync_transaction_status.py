from datetime import timedelta
from uuid import uuid4

import pytest
from freezegun import freeze_time

from sendr_utils import alist, utcnow

from hamcrest import assert_that, has_item, has_properties

from pay.bill_payments.bill_payments.core.actions.transaction.sync_status import SyncTransactionStatusAction
from pay.bill_payments.bill_payments.core.actions.transaction.update_status import UpdateTransactionStatusAction
from pay.bill_payments.bill_payments.interactions.kazna import KaznaClient
from pay.bill_payments.bill_payments.interactions.kazna.entities import (
    DepartmentType,
    PaymentInfoResponse,
    PaymentStatus,
    PaymentStatusCode,
)
from pay.bill_payments.bill_payments.storage.entities.task import TaskState


@pytest.mark.asyncio
async def test_sync_transaction_status_not_new(
    mock_action,
    mocker,
):
    transaction_id = str(uuid4())
    mock = mock_action(
        UpdateTransactionStatusAction,
        None,
    )
    mocker.patch.object(
        KaznaClient,
        'payment_info',
        mocker.AsyncMock(
            return_value=PaymentInfoResponse(
                payment_id=123,
                order_id=transaction_id,
                status=PaymentStatus(code=PaymentStatusCode.AUTH, name='auth'),
                dep_type=DepartmentType.GIBDD,
            )
        ),
    )
    await SyncTransactionStatusAction(external_payment_id='123').run()

    mock.assert_called_once_with(transaction_id=transaction_id, status=PaymentStatusCode.AUTH)


@freeze_time('2021-12-20 20:20:20')
@pytest.mark.asyncio
async def test_sync_transaction_status_new(
    storage,
    mock_action,
    mocker,
):
    transaction_id = str(uuid4())
    mock = mock_action(
        UpdateTransactionStatusAction,
        None,
    )
    mocker.patch.object(
        KaznaClient,
        'payment_info',
        mocker.AsyncMock(
            return_value=PaymentInfoResponse(
                payment_id=123,
                order_id=transaction_id,
                status=PaymentStatus(code=PaymentStatusCode.CREATED, name='created'),
                dep_type=DepartmentType.GIBDD,
            )
        ),
    )
    await SyncTransactionStatusAction(external_payment_id='123').run()

    mock.assert_not_called()

    created_tasks = await alist(storage.task.find())
    assert_that(
        created_tasks,
        has_item(
            has_properties(
                action_name='sync_transaction_status',
                run_at=utcnow() + timedelta(seconds=30),
                state=TaskState.PENDING,
                params={'max_retries': 10, 'action_kwargs': {'external_payment_id': '123', 'attempt': 1}},
            )
        ),
    )


@freeze_time('2021-12-20 20:20:20')
@pytest.mark.asyncio
async def test_max_attempts(
    storage,
    mock_action,
    mocker,
):
    transaction_id = str(uuid4())
    mock = mock_action(UpdateTransactionStatusAction, None)
    mocker.patch.object(
        KaznaClient,
        'payment_info',
        mocker.AsyncMock(
            return_value=PaymentInfoResponse(
                payment_id=123,
                order_id=transaction_id,
                status=PaymentStatus(code=PaymentStatusCode.CREATED, name='created'),
                dep_type=DepartmentType.GIBDD,
            )
        ),
    )
    await SyncTransactionStatusAction(external_payment_id='123', attempt=SyncTransactionStatusAction.MAX_ATTEMPTS).run()

    mock.assert_not_called()

    created_tasks = await alist(storage.task.find())
    assert len(created_tasks) == 0
