from decimal import Decimal
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to, has_property

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_transaction import CashbackTransaction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer_serial import CustomerSerial
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackTransactionStatus,
    ClassicOrderStatus,
    PaymentMethodType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order


@pytest.mark.asyncio
async def test_create(storage, customer, customer_serial, make_cashback_transaction):
    cashback_transaction = make_cashback_transaction()

    created = await storage.cashback_transaction.create(cashback_transaction)

    cashback_transaction.transaction_id = customer_serial.transaction_id
    cashback_transaction.created = created.created
    cashback_transaction.updated = created.updated
    assert_that(
        created,
        equal_to(cashback_transaction),
    )


@pytest.mark.asyncio
async def test_get(storage, customer, make_cashback_transaction):
    cashback_transaction = make_cashback_transaction()

    created = await storage.cashback_transaction.create(cashback_transaction)

    got = await storage.cashback_transaction.get(created.uid, created.transaction_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(CashbackTransaction.DoesNotExist):
        await storage.cashback_transaction.get(1, 2)


@pytest.mark.asyncio
async def test_save(storage, order, account):
    cashback_transaction = CashbackTransaction(
        uid=order.uid,
        order_id=order.order_id,
        account_id=account.account_id,
        trust_purchase_token='xxxxyyyy',
        status=CashbackTransactionStatus.STARTED,
        has_plus=False,
    )
    created = await storage.cashback_transaction.create(cashback_transaction)
    created.trust_purchase_token = 'aaabbb'
    created.status = CashbackTransactionStatus.CLEARED
    created.has_plus = True

    saved = await storage.cashback_transaction.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_uid_and_order_id(storage, make_cashback_transaction, customer, order):
    other_order = await storage.order.create(
        Order(
            uid=customer.uid,
            message_id='msgid:6666',
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
    await storage.cashback_transaction.create(
        make_cashback_transaction(uid=customer.uid, order_id=order.order_id, trust_purchase_token='111')
    )
    await storage.cashback_transaction.create(
        make_cashback_transaction(uid=customer.uid, order_id=other_order.order_id, trust_purchase_token='222')
    )

    assert_that(
        await storage.cashback_transaction.get_by_uid_and_order_id(uid=customer.uid, order_id=other_order.order_id),
        has_property('trust_purchase_token', '222'),
    )


@pytest.fixture
async def customer(storage):
    return await storage.customer.create(Customer(uid=1400))


@pytest.fixture(autouse=True)
async def customer_serial(storage, customer):
    return await storage.customer_serial.create(CustomerSerial(uid=customer.uid, transaction_id=10))


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
def make_cashback_transaction(storage, order, account):
    def _make_cashback_transaction(**kwargs):
        cashback_transaction = CashbackTransaction(
            uid=order.uid,
            order_id=order.order_id,
            account_id=account.account_id,
            trust_purchase_token='xxxxyyyy',
            status=CashbackTransactionStatus.STARTED,
            has_plus=False,
        )
        for key in kwargs:
            setattr(cashback_transaction, key, kwargs[key])
        return cashback_transaction
    return _make_cashback_transaction
