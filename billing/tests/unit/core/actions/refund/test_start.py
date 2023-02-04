from decimal import Decimal
from uuid import UUID, uuid4

import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.account.balance import (
    LogCashbackAccountBalanceAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.start import StartCashbackRefundAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.update_status import UpdateCashbackRefundStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments import TrustPaymentsClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.entities import TrustRefund
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.enums import TrustRefundStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_refund import CashbackRefund
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_transaction import CashbackTransaction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackRefundStatus,
    CashbackTransactionStatus,
    ClassicOrderStatus,
    PaymentMethodType,
    TaskState,
    TaskType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order

REFUND_ID = UUID('98a12f09-8ed7-4644-9aad-b7bba62d817a')
TRUST_REFUND_ID = 'refund-id'


@pytest.fixture
async def customer():
    return await CreateCustomerAction(uid=500).run()


@pytest.fixture
async def account(storage, customer):
    return await storage.cashback_account.create(
        CashbackAccount(uid=customer.uid, currency='XTS', trust_account_id='trust-account-id', account_id=1)
    )


@pytest.fixture
async def order(storage, account):
    return await storage.order.create(
        Order(
            uid=account.uid,
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
async def transaction(storage, account, order):
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


@pytest.fixture(autouse=True)
async def refund(storage, transaction):
    return await storage.cashback_refund.create(
        CashbackRefund(
            refund_id=REFUND_ID,
            uid=transaction.uid,
            transaction_id=transaction.transaction_id,
            trust_refund_id=TRUST_REFUND_ID,
            event_time=utcnow(),
            status=CashbackRefundStatus.CREATED,
            amount=Decimal('50'),
        )
    )


@pytest.fixture(autouse=True)
def mock_trust_start_refund(mocker):
    return mocker.patch.object(
        TrustPaymentsClient,
        'start_plus_refund_transaction',
        mocker.AsyncMock(return_value=TrustRefund(status=TrustRefundStatus.SUCCESS, status_desc='the-desc'))
    )


@pytest.fixture(autouse=True)
def mock_log_cashback_account_balance(mock_action):
    return mock_action(LogCashbackAccountBalanceAction)


@pytest.mark.asyncio
async def test_calls_start_refund(mock_trust_start_refund):
    await StartCashbackRefundAction(REFUND_ID).run()

    mock_trust_start_refund.assert_awaited_once_with(trust_refund_id=TRUST_REFUND_ID)


@pytest.mark.asyncio
async def test_calls_log_account_balance(mock_log_cashback_account_balance, account):
    await StartCashbackRefundAction(REFUND_ID).run()

    mock_log_cashback_account_balance.assert_called_once_with(
        cashback_account=account,
        message_id='1:msgid',
        cashback_transaction_purchase_token='trust-purchase-token',
    )


@pytest.mark.asyncio
async def test_updates_refund_status(storage):
    await StartCashbackRefundAction(REFUND_ID).run()

    refund = await storage.cashback_refund.get(REFUND_ID)
    assert_that(refund.status, equal_to(CashbackRefundStatus.IN_PROGRESS))


@pytest.mark.asyncio
async def test_creates_check_refund_task(storage):
    await StartCashbackRefundAction(REFUND_ID).run()

    [task] = await alist(
        storage.task.find(
            filters={
                'task_type': lambda field: field.in_((TaskType.RUN_ACTION,)),
                'state': lambda field: field.in_((TaskState.PENDING,)),
                'action_name': lambda field: field.in_((UpdateCashbackRefundStatusAction.action_name,)),
            },
            order=('-created',),
            limit=1,
        )
    )
    assert_that(
        task.params,
        has_entries(
            action_kwargs={
                'refund_id': str(REFUND_ID),
            },
        )
    )


@pytest.mark.asyncio
async def test_converts_str_refund_id_to_uuid():
    action = StartCashbackRefundAction(str(REFUND_ID))

    await action.run()

    assert_that(action.refund_id, equal_to(REFUND_ID))
