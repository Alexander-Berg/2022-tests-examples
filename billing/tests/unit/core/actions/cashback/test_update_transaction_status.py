import logging
from datetime import timedelta
from decimal import Decimal
from uuid import UUID, uuid4

import pytest
from pay.lib.entities.payment_sheet import PaymentMerchant as Merchant

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.map_statuses import (
    map_trust_payment_to_plus_transaction_status,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.update_transaction_status import (
    UpdateCashbackTransactionStatusAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.create import CreateOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.send_to_payments_history import (
    SendToPaymentsHistoryAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    TransactionNotFoundError,
    TransactionNotInFinalStateError,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions import TrustPaymentsClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments import TrustPayment
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.enums import TrustPaymentStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_transaction import CashbackTransaction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackTransactionStatus,
    ClassicOrderStatus,
    PaymentMethodType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order

FAKE_PURCHASE_TOKEN = 'fake_trust_purchase_token'


@pytest.fixture
async def customer(randn, storage):
    return await CreateCustomerAction(uid=randn()).run()


@pytest.fixture
async def cashback_account(customer: Customer, storage):
    return await storage.cashback_account.create(
        CashbackAccount(
            uid=customer.uid,
            currency='XTS',
            trust_account_id='fake_trust_account_id',
        )
    )


@pytest.fixture(autouse=True)
def mock_xts_default_cashback(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT['XTS'] = 1000
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT['XTS'] = 3000


@pytest.fixture
async def budget(storage):
    return await storage.cashback_budget.create(
        CashbackBudget(
            budget_id=uuid4(),
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=30),
        )
    )


@pytest.fixture
async def order(customer: Customer, storage, budget):
    order = await CreateOrderAction(
        uid=customer.uid,
        message_id='msgid',
        currency='XTS',
        amount=Decimal('100'),
        psp_id=UUID('f4f9b696-6f6f-4673-beb0-d0b70c4052f9'),
        merchant=Merchant(
            id=UUID('6031216d-520c-47c4-832e-bfe663991d79'),
            name='the-name',
            url='https://url.test',
        ),
        trust_card_id='card-x123abc',
        payment_method_type=PaymentMethodType.CARD,
    ).run()
    order.status = ClassicOrderStatus.HOLD
    return await storage.order.save(order)


@pytest.fixture
async def cashback_transaction(
    cashback_account: CashbackAccount,
    order: Order,
    storage,
):
    return await storage.cashback_transaction.create(
        CashbackTransaction(
            uid=order.uid,
            order_id=order.order_id,
            account_id=cashback_account.account_id,
            trust_purchase_token=FAKE_PURCHASE_TOKEN,
            status=CashbackTransactionStatus.STARTED,
            has_plus=True,
        )
    )


@pytest.fixture(params=[TrustPaymentStatus.CLEARED])
def trust_payment_status(request):
    return request.param


@pytest.fixture
def mock_get_plus_transaction_status(mocker, trust_payment_status):
    trust_payment = TrustPayment(payment_status=trust_payment_status)
    return mocker.patch.object(
        TrustPaymentsClient,
        'get_plus_transaction_status',
        mocker.AsyncMock(return_value=trust_payment)
    )


@pytest.fixture(autouse=True)
def mock_send_to_payments_history_action(mock_action):
    return mock_action(SendToPaymentsHistoryAction)


def test_serialize():
    action = UpdateCashbackTransactionStatusAction(uid=123, transaction_id=456)
    assert_that(
        UpdateCashbackTransactionStatusAction.serialize_kwargs(action._init_kwargs),
        equal_to({'uid': 123, 'transaction_id': 456})
    )


def test_deserialize():
    action = UpdateCashbackTransactionStatusAction(uid=123, transaction_id=456)
    serialized = UpdateCashbackTransactionStatusAction.serialize_kwargs(action._init_kwargs)
    deserialized = UpdateCashbackTransactionStatusAction.deserialize_kwargs(serialized)

    action_from_serialized = UpdateCashbackTransactionStatusAction(**deserialized)

    assert_that(action._init_kwargs, equal_to(action_from_serialized._init_kwargs))


@pytest.mark.asyncio
async def test_update_transaction_status_succeeds(
    storage,
    mock_get_plus_transaction_status,
    cashback_transaction,
    mock_send_to_payments_history_action,
    order
):
    loaded = await storage.cashback_transaction.get(
        cashback_transaction.uid, cashback_transaction.transaction_id
    )
    assert_that(loaded, has_properties(status=CashbackTransactionStatus.STARTED))

    await UpdateCashbackTransactionStatusAction(
        uid=cashback_transaction.uid, transaction_id=cashback_transaction.transaction_id
    ).run()

    loaded = await storage.cashback_transaction.get(
        cashback_transaction.uid, cashback_transaction.transaction_id
    )

    assert_that(loaded, has_properties(status=CashbackTransactionStatus.CLEARED))
    mock_get_plus_transaction_status.assert_awaited_once_with(
        purchase_token=FAKE_PURCHASE_TOKEN
    )
    mock_send_to_payments_history_action.assert_called_once()
    assert mock_send_to_payments_history_action.call_args.kwargs == {
        'uid': order.uid,
        'order_id': order.order_id,
        'transaction_id': cashback_transaction.transaction_id
    }


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_get_plus_transaction_status')
async def test_update_transaction_status_is_logged(
    cashback_transaction, dummy_logger, caplog
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    await UpdateCashbackTransactionStatusAction(
        uid=cashback_transaction.uid, transaction_id=cashback_transaction.transaction_id
    ).run()

    [log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        log,
        has_properties(
            message='Cashback transaction status updated',
            levelno=logging.INFO,
            _context=has_entries(
                uid=cashback_transaction.uid,
                transaction_id=cashback_transaction.transaction_id,
                trust_purchase_token=FAKE_PURCHASE_TOKEN,
                transaction_status=CashbackTransactionStatus.CLEARED,
                trust_payment_status=TrustPaymentStatus.CLEARED,
            )
        )
    )


@pytest.mark.asyncio
async def test_update_transaction_status_fails_missing_transaction(customer):
    with pytest.raises(TransactionNotFoundError):
        await UpdateCashbackTransactionStatusAction(
            uid=customer.uid, transaction_id=123
        ).run()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'trust_payment_status', [TrustPaymentStatus.STARTED], indirect=True
)
@pytest.mark.usefixtures('mock_get_plus_transaction_status')
async def test_update_transaction_status_fails_not_final_status(
    cashback_transaction, dummy_logger, caplog, mock_send_to_payments_history_action
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    with pytest.raises(TransactionNotInFinalStateError):
        await UpdateCashbackTransactionStatusAction(
            uid=cashback_transaction.uid,
            transaction_id=cashback_transaction.transaction_id,
        ).run()

    [log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        log,
        has_properties(
            message='Cashback transaction status is not final, will poll it again later',
            levelno=logging.INFO,
            _context=has_entries(
                uid=cashback_transaction.uid,
                transaction_id=cashback_transaction.transaction_id,
                trust_purchase_token=FAKE_PURCHASE_TOKEN,
                transaction_status=CashbackTransactionStatus.STARTED,
                trust_payment_status=TrustPaymentStatus.STARTED,
            )
        )
    )
    mock_send_to_payments_history_action.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'trust_payment_status',
    [
        each for each in TrustPaymentStatus
        if map_trust_payment_to_plus_transaction_status(each) == CashbackTransactionStatus.FAILED
    ],
    indirect=True,
)
@pytest.mark.usefixtures('mock_get_plus_transaction_status')
async def test_update_transaction_status_failed_trust_status_reported_to_stats(
    storage, cashback_transaction, mocker
):
    mock_counter = mocker.patch(
        'billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.'
        'update_transaction_status.trust_plus_transaction_failed'
    )

    await UpdateCashbackTransactionStatusAction(
        uid=cashback_transaction.uid, transaction_id=cashback_transaction.transaction_id
    ).run()

    loaded = await storage.cashback_transaction.get(
        cashback_transaction.uid, cashback_transaction.transaction_id
    )

    assert_that(loaded, has_properties(status=CashbackTransactionStatus.FAILED))
    mock_counter.labels.return_value.inc.assert_called_once_with()
