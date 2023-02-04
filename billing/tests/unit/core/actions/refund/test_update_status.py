from decimal import Decimal
from uuid import UUID, uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
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
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order
from billing.yandex_pay_plus.yandex_pay_plus.utils.stats import trust_plus_refund_failed

REFUND_ID = UUID('98a12f09-8ed7-4644-9aad-b7bba62d817a')
TRUST_REFUND_ID = 'refund-id'


@pytest.fixture
def trust_refund_status():
    return TrustRefundStatus.SUCCESS


@pytest.fixture(autouse=True)
async def setup(storage):
    await CreateCustomerAction(uid=500).run()
    order = await storage.order.create(
        Order(
            uid=500,
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
    account = await storage.cashback_account.create(
        CashbackAccount(uid=500, currency='XTS', trust_account_id='trust-account-id', account_id=1)
    )
    transaction = await storage.cashback_transaction.create(
        CashbackTransaction(
            uid=500,
            order_id=order.order_id,
            account_id=account.account_id,
            trust_purchase_token='trust-purchase-token',
            status=CashbackTransactionStatus.CLEARED,
            has_plus=True,
        )
    )
    await storage.cashback_refund.create(
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
def mock_trust_get_refund(mocker, trust_refund_status):
    return mocker.patch.object(
        TrustPaymentsClient,
        'get_plus_refund_transaction',
        mocker.AsyncMock(return_value=TrustRefund(status=trust_refund_status, status_desc='the-desc'))
    )


@pytest.mark.asyncio
async def test_calls_get_refund(mock_trust_get_refund):
    await UpdateCashbackRefundStatusAction(REFUND_ID).run()

    mock_trust_get_refund.assert_awaited_once_with(trust_refund_id=TRUST_REFUND_ID)


@pytest.mark.asyncio
async def test_updates_refund_status(storage):
    await UpdateCashbackRefundStatusAction(REFUND_ID).run()

    refund = await storage.cashback_refund.get(REFUND_ID)
    assert_that(refund.status, equal_to(CashbackRefundStatus.SUCCESS))


@pytest.mark.asyncio
async def test_converts_str_refund_id_to_uuid():
    action = UpdateCashbackRefundStatusAction(str(REFUND_ID))

    await action.run()

    assert_that(action.refund_id, equal_to(REFUND_ID))


@pytest.mark.parametrize('trust_refund_status', (TrustRefundStatus.FAILED, TrustRefundStatus.ERROR))
@pytest.mark.asyncio
async def test_reports_failed_transaction(mocker, storage):
    [_, before] = trust_plus_refund_failed.get()[0]

    await UpdateCashbackRefundStatusAction(REFUND_ID).run()

    [_, after] = trust_plus_refund_failed.get()[0]
    assert_that(after, equal_to(before + 1))
