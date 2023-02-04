from uuid import uuid4

import pytest

from pay.bill_payments.bill_payments.core.actions.transaction.get import GetTransactionAction
from pay.bill_payments.bill_payments.core.exceptions import TransactionNotFoundError
from pay.bill_payments.bill_payments.interactions import KaznaClient
from pay.bill_payments.bill_payments.interactions.kazna import PaymentInfoResponse
from pay.bill_payments.bill_payments.interactions.kazna.entities import DepartmentType, PaymentStatus, PaymentStatusCode
from pay.bill_payments.bill_payments.storage.entities.enums import OrderStatus, TransactionStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order
from pay.bill_payments.bill_payments.storage.entities.transaction import PayerData, Transaction


@pytest.fixture
async def order(storage, user) -> Order:
    return await storage.order.create(
        Order(
            uid=user.uid,
            order_id=uuid4(),
            status=OrderStatus.NEW,
        )
    )


@pytest.fixture(params=[TransactionStatus.NEW])
def transaction_status(request):
    return request.param


@pytest.fixture
async def transaction(storage, order, transaction_status) -> Transaction:
    return await storage.transaction.create(
        Transaction(
            transaction_id=uuid4(),
            order_id=order.order_id,
            status=transaction_status,
            amount=100,
            external_payment_id='123',
            payer_data=PayerData(payer_full_name='test'),
        )
    )


@pytest.fixture
def kazna_mock(mocker, transaction):
    return mocker.patch.object(
        KaznaClient,
        'payment_info',
        mocker.AsyncMock(
            return_value=PaymentInfoResponse(
                payment_id=123,
                order_id=transaction.transaction_id,
                status=PaymentStatus(code=PaymentStatusCode.CANCEL, name='cancel'),
                dep_type=DepartmentType.GIBDD,
            )
        ),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('transaction_status', [TransactionStatus.NEW])
async def test_calls_kazna(kazna_mock, transaction):
    result = await GetTransactionAction(transaction_id=transaction.transaction_id).run()

    kazna_mock.assert_called_once()
    transaction.status = TransactionStatus.CANCELLED
    assert result == transaction


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'transaction_status', [TransactionStatus.PAID, TransactionStatus.CANCELLED, TransactionStatus.REFUNDED]
)
async def test_kazna_not_called(kazna_mock, transaction):
    result = await GetTransactionAction(transaction_id=transaction.transaction_id).run()

    kazna_mock.assert_not_called()
    assert result == transaction


@pytest.mark.asyncio
async def test_transaction_not_found():
    with pytest.raises(TransactionNotFoundError):
        await GetTransactionAction(transaction_id=uuid4()).run()
