from decimal import Decimal
from uuid import UUID, uuid4

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_utils import alist, utcnow

from hamcrest import assert_that, has_entries, has_properties, instance_of

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.create import CreateCashbackRefundAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CashbackTransactionNotTerminatedError,
    RefundAlreadyExistsError,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments import TrustPaymentsClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_refund import CashbackRefund
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_transaction import CashbackTransaction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackRefundStatus,
    CashbackTransactionStatus,
    ClassicOrderStatus,
    PaymentMethodType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order


@pytest.mark.asyncio
async def test_calls_trust(mock_trust_create_refund, order, transaction):
    await CreateCashbackRefundAction(
        uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=Decimal('50')
    ).run()

    mock_trust_create_refund.assert_awaited_once_with(
        purchase_token='trust-purchase-token',
        amount=5,
        uid=500,
        reason_desc='Refund'
    )


@pytest.mark.asyncio
async def test_creates_refund_in_db(order, transaction, storage):
    now = utcnow()
    await CreateCashbackRefundAction(
        uid=order.uid, order_id=order.order_id, event_time=now, amount=Decimal('50')
    ).run()

    refund = await storage.cashback_refund.get_by_transaction_and_event_time(
        uid=transaction.uid, transaction_id=transaction.transaction_id, event_time=now
    )
    assert_that(
        refund,
        has_properties({
            'amount': Decimal('5'),
            'trust_refund_id': 'kinda-trust-refund-id',
            'event_time': now,
            'status': CashbackRefundStatus.CREATED,
        })
    )


@pytest.mark.asyncio
async def test_updates_order_refunds(order, transaction, storage):
    await CreateCashbackRefundAction(
        uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=Decimal('50')
    ).run()

    order = await storage.order.get(order.uid, order.order_id)
    assert_that(
        order,
        has_properties({
            'refunded_amount': Decimal('50'),
            'refunded_cashback': Decimal('5'),
        })
    )


@pytest.mark.asyncio
async def test_creates_start_refund_task(order, transaction, storage, yandex_pay_plus_settings):
    await CreateCashbackRefundAction(
        uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=Decimal('50')
    ).run()

    [task] = await alist(storage.task.find())
    assert_that(
        task,
        has_properties({
            'params': has_entries({
                'action_kwargs': has_entries({
                    'refund_id': convert_then_match(UUID, instance_of(UUID))
                }),
                'max_retries': yandex_pay_plus_settings.TASKQ_START_CASHBACK_REFUND_MAX_TRIES,
            }),
        })
    )


@pytest.mark.asyncio
async def test_does_not_call_trust_when_cashback_amount_is_too_low(order, transaction, mock_trust_create_refund):
    """
    Т.к. кэшбэк = 10%, то floor(9*0.1) = floor(0.9) = 0.
    """
    await CreateCashbackRefundAction(
        uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=Decimal('9')
    ).run()

    mock_trust_create_refund.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize('money_refunds, expected_cashback_refunds', (
    pytest.param(('100',), (10,), id='full-refund'),
    pytest.param(
        (40,),
        (4,),
        id='partial-refund',
    ),
    pytest.param(
        (39,),
        (3,),
        id='partial-refund-with-floor',
    ),
    pytest.param(
        (19, 21),
        (1, 3),
        id='does-not-accumulate-rounding-error'
    ),
    pytest.param(
        ('49.5', '51.5'),
        (4, 6),
        id='refund-with-minors'
    ),
    pytest.param(
        ('0.5',) * 40,
        (1, 1),
        id='refund-many-minors'
    ),
))
async def test_cashback_refund_amounts(
    mocker, mock_trust_create_refund, order, transaction, money_refunds, expected_cashback_refunds
):
    for refund in money_refunds:
        await CreateCashbackRefundAction(
            uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=Decimal(refund)
        ).run()

    mock_trust_create_refund.assert_has_calls([
        mocker.call(
            purchase_token='trust-purchase-token',
            amount=cashback_refund,
            uid=500,
            reason_desc='Refund',
        ) for cashback_refund in expected_cashback_refunds
    ])


@pytest.mark.asyncio
async def test_cashback_refund_when_cashback_was_limited(storage, mock_trust_create_refund, order, transaction):
    order.amount = Decimal('100')
    order.category = Decimal('0.1')
    order.cashback = Decimal('5')
    order = await storage.order.save(order)

    await CreateCashbackRefundAction(
        uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=order.amount,
    ).run()

    mock_trust_create_refund.assert_called_once_with(
        purchase_token='trust-purchase-token',
        amount=5,
        uid=500,
        reason_desc='Refund',
    )


@pytest.mark.asyncio
async def test_raises_exception_if_refund_already_exist(order, transaction, storage, refund):
    with pytest.raises(RefundAlreadyExistsError):
        await CreateCashbackRefundAction(
            uid=order.uid, order_id=order.order_id, event_time=refund.event_time, amount=Decimal('50')
        ).run()


@pytest.mark.asyncio
async def test_raises_when_transaction_is_not_cleared(order, transaction, storage):
    transaction.status = CashbackTransactionStatus.STARTED
    await storage.cashback_transaction.save(transaction)

    with pytest.raises(CashbackTransactionNotTerminatedError):
        await CreateCashbackRefundAction(
            uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=Decimal('9')
        ).run()


@pytest.mark.asyncio
async def test_raises_when_transaction_does_not_exists(order, storage):
    with pytest.raises(CashbackTransactionNotTerminatedError):
        await CreateCashbackRefundAction(
            uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=Decimal('9')
        ).run()


@pytest.mark.asyncio
async def test_does_not_call_trust_when_transaction_failed(order, transaction, storage, mock_trust_create_refund):
    transaction.status = CashbackTransactionStatus.FAILED
    await storage.cashback_transaction.save(transaction)

    await CreateCashbackRefundAction(
        uid=order.uid, order_id=order.order_id, event_time=utcnow(), amount=Decimal('9')
    ).run()

    mock_trust_create_refund.assert_not_called()


@pytest.mark.asyncio
async def test_converts_parameters(order):
    now = utcnow()

    action = CreateCashbackRefundAction(
        uid=order.uid, order_id=order.order_id, event_time=now.isoformat(), amount='9'
    )

    assert_that(
        action,
        has_properties({
            'event_time': now,
            'amount': Decimal('9'),
        })
    )


@pytest.fixture(autouse=True)
def mock_trust_create_refund(mocker):
    return mocker.patch.object(
        TrustPaymentsClient,
        'create_plus_refund_transaction',
        mocker.AsyncMock(side_effect=('kinda-trust-refund-id', 'kinda-trust-refund-id-2'))
    )


@pytest.fixture
async def customer(storage):
    return await CreateCustomerAction(uid=500).run()


@pytest.fixture
async def order(storage, customer):
    return await storage.order.create(
        Order(
            uid=customer.uid,
            message_id='1:msgid',
            currency='XTS',
            amount=Decimal('100'),
            cashback=Decimal('10'),
            cashback_category=Decimal('0.1'),
            status=ClassicOrderStatus.NEW,
            merchant_id=uuid4(),
            psp_id=uuid4(),
            payment_method_type=PaymentMethodType.CARD,
        )
    )


@pytest.fixture
async def account(storage, customer):
    return await storage.cashback_account.create(
        CashbackAccount(uid=customer.uid, currency='XTS', trust_account_id='trust-account-id')
    )


@pytest.fixture
async def transaction(storage, order, account):
    return await storage.cashback_transaction.create(
        CashbackTransaction(
            uid=order.uid,
            order_id=order.order_id,
            account_id=account.account_id,
            trust_purchase_token='trust-purchase-token',
            status=CashbackTransactionStatus.CLEARED,
            has_plus=True,
        )
    )


@pytest.fixture
async def refund(storage, transaction):
    return await storage.cashback_refund.create(
        CashbackRefund(
            refund_id=UUID('98a12f09-8ed7-4644-9aad-b7bba62d817a'),
            uid=transaction.uid,
            transaction_id=transaction.transaction_id,
            trust_refund_id='refund-id',
            event_time=utcnow(),
            status=CashbackRefundStatus.CREATED,
            amount=Decimal('50'),
        )
    )
