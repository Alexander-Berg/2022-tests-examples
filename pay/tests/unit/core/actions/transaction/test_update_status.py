from uuid import uuid4

import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.transaction.update_status import UpdateTransactionStatusAction
from pay.bill_payments.bill_payments.core.exceptions import BillOrderNotFoundError, CoreFailError
from pay.bill_payments.bill_payments.interactions.kazna.entities import PaymentStatusCode
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.bill_order import BillOrder
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus, OrderStatus, TransactionStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order
from pay.bill_payments.bill_payments.storage.entities.transaction import PayerData, Transaction
from pay.bill_payments.bill_payments.utils.stats import transaction_status


@pytest.fixture
async def order(storage, user):
    return await storage.order.create(
        Order(
            uid=user.uid,
            order_id=uuid4(),
            status=OrderStatus.NEW,
        )
    )


@pytest.fixture
async def bill(storage, user, document):
    async def _bill():
        return await storage.bill.create(
            Bill(
                uid=user.uid,
                document_id=document.document_id,
                supplier_bill_id=str(uuid4()),
                status=BillStatus.NEW,
                amount=100,
                amount_to_pay=20,
                bill_date=utcnow(),
            )
        )

    return _bill


@pytest.fixture
async def transaction(storage, order):
    async def _transaction(status=None):
        return await storage.transaction.create(
            Transaction(
                transaction_id=uuid4(),
                order_id=order.order_id,
                status=status or TransactionStatus.NEW,
                amount=100,
                external_payment_id='123',
                payer_data=PayerData(payer_full_name='test'),
            )
        )

    return _transaction


@pytest.mark.parametrize(
    ('payment_status', 'transaction_init_status', 'transaction_expected', 'bill_expected', 'order_expected'),
    [
        (PaymentStatusCode.AUTH, TransactionStatus.NEW, TransactionStatus.PAID, BillStatus.PAID, OrderStatus.PAID),
        (PaymentStatusCode.CANCEL, TransactionStatus.NEW, TransactionStatus.CANCELLED, None, None),
        (PaymentStatusCode.CREATED, TransactionStatus.NEW, None, None, None),
        (PaymentStatusCode.REFUNDED, TransactionStatus.PAID, TransactionStatus.REFUNDED, None, None),
    ],
)
@pytest.mark.asyncio
async def test_update_status_success_flow(
    storage,
    transaction,
    bill,
    order,
    payment_status,
    transaction_init_status,
    transaction_expected,
    bill_expected,
    order_expected,
):
    transaction = await transaction(status=transaction_init_status)
    bill = await bill()
    await storage.bill_order.create(
        BillOrder(
            bill_id=bill.bill_id,
            order_id=order.order_id,
        )
    )
    expected_status = transaction_expected or transaction.status
    status_metric = transaction_status.labels(expected_status.name).get()[0][1]

    await UpdateTransactionStatusAction(transaction_id=transaction.transaction_id, status=payment_status).run()

    transaction_updated = await storage.transaction.get(transaction.transaction_id)
    bill_updated = await storage.bill.get(bill.bill_id)
    order_updated = await storage.order.get(order.order_id)

    assert transaction_updated.status == expected_status
    assert bill_updated.status == (bill_expected or bill.status)
    assert order_updated.status == (order_expected or order.status)

    assert_that(transaction_status.labels(expected_status.name).get()[0][1], equal_to(status_metric + 1))


@pytest.mark.parametrize(
    ('payment_status', 'transaction_init_status'),
    [
        (PaymentStatusCode.CANCEL, TransactionStatus.PAID),
        (PaymentStatusCode.CANCEL, TransactionStatus.REFUNDED),
        (PaymentStatusCode.AUTH, TransactionStatus.CANCELLED),
        (PaymentStatusCode.AUTH, TransactionStatus.REFUNDED),
        (PaymentStatusCode.REFUNDED, TransactionStatus.NEW),
        (PaymentStatusCode.REFUNDED, TransactionStatus.CANCELLED),
    ],
)
@pytest.mark.asyncio
async def test_update_status_wrong_transition(storage, transaction, payment_status, transaction_init_status):
    transaction = await transaction(status=transaction_init_status)
    with pytest.raises(CoreFailError) as e:
        await UpdateTransactionStatusAction(transaction_id=transaction.transaction_id, status=payment_status).run()

    assert (
        e.value.__cause__.value
        == f'Can\'t trigger event {payment_status.name} from state {transaction_init_status.name}!'
    )


@pytest.mark.asyncio
async def test_transaction_does_not_exist(storage):
    await UpdateTransactionStatusAction(transaction_id=str(uuid4()), status=TransactionStatus.PAID).run()

    assert_that(await alist(storage.transaction.find()), equal_to([]))


@pytest.mark.asyncio
async def test_update_same_status(storage, transaction):
    transaction = await transaction(status=TransactionStatus.PAID)
    await UpdateTransactionStatusAction(transaction_id=transaction.transaction_id, status=PaymentStatusCode.AUTH).run()

    assert_that(await alist(storage.transaction.find()), equal_to([transaction]))


@pytest.mark.asyncio
async def test_update_status_bill_order_not_found(storage, transaction, bill, order):
    transaction = await transaction()
    bill = await bill()
    with pytest.raises(BillOrderNotFoundError):
        await UpdateTransactionStatusAction(
            transaction_id=transaction.transaction_id, status=PaymentStatusCode.AUTH
        ).run()


@pytest.mark.asyncio
async def test_update_status_more_than_one_bill_for_order(storage, transaction, bill, order):
    transaction = await transaction()
    bill1 = await bill()
    bill2 = await bill()
    await storage.bill_order.create(
        BillOrder(
            bill_id=bill1.bill_id,
            order_id=order.order_id,
        )
    )
    await storage.bill_order.create(
        BillOrder(
            bill_id=bill2.bill_id,
            order_id=order.order_id,
        )
    )
    await UpdateTransactionStatusAction(transaction_id=transaction.transaction_id, status=PaymentStatusCode.AUTH).run()

    bill1_updated = await storage.bill.get(bill1.bill_id)
    bill2_updated = await storage.bill.get(bill2.bill_id)

    assert bill1_updated.status == BillStatus.PAID
    assert bill2_updated.status == BillStatus.PAID
