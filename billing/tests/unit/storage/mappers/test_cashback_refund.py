from decimal import Decimal
from uuid import uuid4

import psycopg2.errors
import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_property

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_refund import CashbackRefund
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_transaction import CashbackTransaction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer_serial import CustomerSerial
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackRefundStatus,
    CashbackTransactionStatus,
    ClassicOrderStatus,
    PaymentMethodType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order


@pytest.mark.asyncio
async def test_create(storage, make_cashback_refund):
    cashback_refund = make_cashback_refund()

    created = await storage.cashback_refund.create(cashback_refund)

    cashback_refund.created = created.created
    cashback_refund.updated = created.updated
    assert_that(
        created,
        equal_to(cashback_refund),
    )


@pytest.mark.asyncio
async def test_create_requires_transaction_to_exist(storage, transaction, make_cashback_refund):
    cashback_refund = make_cashback_refund(transaction_id=transaction.transaction_id + 10)

    with pytest.raises(psycopg2.errors.ForeignKeyViolation):
        await storage.cashback_refund.create(cashback_refund)


@pytest.mark.asyncio
async def test_create_requires_event_time_to_be_unique_for_transaction(storage, transaction, make_cashback_refund):
    now = utcnow()
    await storage.cashback_refund.create(make_cashback_refund(event_time=now, trust_refund_id='1'))

    with pytest.raises(psycopg2.errors.UniqueViolation):
        await storage.cashback_refund.create(make_cashback_refund(event_time=now, trust_refund_id='2'))


@pytest.mark.asyncio
async def test_create_requires_trust_refund_id_to_be_unique(storage, transaction, make_cashback_refund):
    await storage.cashback_refund.create(make_cashback_refund(trust_refund_id='id1', event_time=utcnow()))

    with pytest.raises(psycopg2.errors.UniqueViolation):
        await storage.cashback_refund.create(make_cashback_refund(trust_refund_id='id1', event_time=utcnow()))


@pytest.mark.asyncio
async def test_get(storage, customer, make_cashback_refund):
    cashback_refund = make_cashback_refund()

    created = await storage.cashback_refund.create(cashback_refund)

    got = await storage.cashback_refund.get(created.refund_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(CashbackRefund.DoesNotExist):
        await storage.cashback_refund.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, order, transaction):
    cashback_refund = CashbackRefund(
        refund_id=uuid4(),
        uid=transaction.uid,
        transaction_id=transaction.transaction_id,
        trust_refund_id='refund-id',
        event_time=utcnow(),
        status=CashbackRefundStatus.CREATED,
        amount=Decimal('10'),
    )
    created = await storage.cashback_refund.create(cashback_refund)
    created.trust_refund_id = 'refund-id-2'
    created.status = CashbackRefundStatus.SUCCESS
    created.event_time = utcnow()
    created.amount = Decimal('20')

    saved = await storage.cashback_refund.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_transaction_and_event_time(storage, make_cashback_refund, customer, order, transaction):
    now = utcnow()
    await storage.cashback_refund.create(make_cashback_refund(event_time=now))

    other_now = utcnow()
    other_refund = await storage.cashback_refund.create(make_cashback_refund(event_time=other_now))

    assert_that(
        await storage.cashback_refund.get_by_transaction_and_event_time(
            uid=transaction.uid, transaction_id=transaction.transaction_id, event_time=other_now
        ),
        has_property('refund_id', other_refund.refund_id),
    )


@pytest.fixture
async def customer(storage):
    return await storage.customer.create(Customer(uid=1400))


@pytest.fixture(autouse=True)
async def customer_serial(storage, customer):
    return await storage.customer_serial.create(CustomerSerial(uid=customer.uid))


@pytest.fixture
async def order(storage, customer):
    return await storage.order.create(
        Order(
            uid=customer.uid,
            message_id='msgid:5555',
            currency='XTS',
            status=ClassicOrderStatus.SUCCESS,
            amount=Decimal('100'),
            cashback=Decimal('10'),
            cashback_category=Decimal('0.10'),
            merchant_id=uuid4(),
            psp_id=uuid4(),
            payment_method_type=PaymentMethodType.CARD,
        ),
    )


@pytest.fixture(autouse=True)
async def account(storage, customer, customer_serial):
    return await storage.cashback_account.create(
        CashbackAccount(
            uid=customer.uid,
            currency='XTS',
            trust_account_id='ididid',
        )
    )


@pytest.fixture
async def transaction(storage, order, customer, account, rands):
    return await storage.cashback_transaction.create(
        CashbackTransaction(
            uid=customer.uid,
            order_id=order.order_id,
            account_id=account.account_id,
            trust_purchase_token=rands(),
            status=CashbackTransactionStatus.CLEARED,
            has_plus=True,
        )
    )


@pytest.fixture
def make_cashback_refund(storage, account, transaction, rands):
    def _make_cashback_refund(**kwargs):
        cashback_refund = CashbackRefund(
            refund_id=uuid4(),
            uid=transaction.uid,
            transaction_id=transaction.transaction_id,
            trust_refund_id=rands(),
            event_time=utcnow(),
            status=CashbackRefundStatus.CREATED,
            amount=Decimal('10'),
        )
        for key in kwargs:
            setattr(cashback_refund, key, kwargs[key])
        return cashback_refund
    return _make_cashback_refund
