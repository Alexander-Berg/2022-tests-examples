import logging
from datetime import datetime, timedelta
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_core.exceptions import CoreFailError
from sendr_pytest.matchers import convert_then_match
from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    equal_to,
    has_entries,
    has_length,
    has_properties,
    has_string,
    instance_of,
)

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.create_transaction import (
    CreateCashbackTransactionAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.receipt import SendOrderReceiptAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.send_to_payments_history import (
    SendToPaymentsHistoryAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.update_status import UpdateOrderStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.create import CreateCashbackRefundAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import OrderEventTooOldError, OrderNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_refund import CashbackRefund
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_transaction import CashbackTransaction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackRefundStatus,
    CashbackTransactionStatus,
    ClassicOrderStatus,
    PaymentMethodType,
    TaskType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order
from billing.yandex_pay_plus.yandex_pay_plus.utils.logging import get_product_logger


@pytest.fixture
def uid():
    return 1223334444


@pytest.fixture
def message_id():
    return 'fake_message_id'


@pytest.fixture
def currency():
    return 'XTS'


@pytest.fixture
def time_now():
    return utcnow()


@pytest.fixture
async def customer(uid):
    return await CreateCustomerAction(uid=uid).run()


@pytest.fixture(autouse=True)
def mock_xts_default_cashback(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT['XTS'] = 1000
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT['XTS'] = 3000


@pytest.fixture(autouse=True)
def mock_send_to_payments_history_action(mock_action):
    return mock_action(SendToPaymentsHistoryAction)


class TestUpdateOrderStatusAction:
    @pytest.fixture(autouse=True)
    async def budget(self, storage):
        return await storage.cashback_budget.create(
            CashbackBudget(
                budget_id=uuid4(),
                currency='XTS',
                spent=Decimal('0'),
                spending_limit=Decimal('1000000'),
                period_start=utcnow() - timedelta(days=1),
                period_end=utcnow() + timedelta(days=10),
            )
        )

    @pytest.fixture(autouse=True)
    async def order(self, storage, message_id, customer, currency):
        return await storage.order.create(
            Order(
                uid=customer.uid,
                message_id=message_id,
                currency=currency,
                amount=Decimal('100'),
                psp_id=uuid4(),
                merchant_id=uuid4(),
                cashback=Decimal('10'),
                cashback_category=Decimal('0.1'),
                status=ClassicOrderStatus.NEW,
                payment_method_type=PaymentMethodType.CARD,
            )
        )

    @pytest.fixture
    async def cashback_account(self, storage, currency, customer):
        return await storage.cashback_account.create(
            CashbackAccount(
                uid=customer.uid,
                currency=currency,
                trust_account_id='fake_trust_account_id',
            )
        )

    @pytest.mark.asyncio
    async def test_updates_status(self, storage, message_id, uid, time_now):
        await UpdateOrderStatusAction(
            message_id=message_id, status=ClassicOrderStatus.SUCCESS, event_time=time_now
        ).run()

        updated = await storage.order.get_by_message_id(message_id)

        assert_that(
            updated,
            has_properties(
                message_id=message_id,
                uid=uid,
                status=ClassicOrderStatus.SUCCESS,
                order_event_processed=time_now,
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('prev_status, status, expected_new_status', (
        (ClassicOrderStatus.NEW, ClassicOrderStatus.HOLD, ClassicOrderStatus.HOLD),
        (ClassicOrderStatus.HOLD, ClassicOrderStatus.SUCCESS, ClassicOrderStatus.SUCCESS),
        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.REFUND, ClassicOrderStatus.REFUND),
        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.CHARGEBACK, ClassicOrderStatus.CHARGEBACK),
        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.REVERSE, ClassicOrderStatus.REVERSE),
        (ClassicOrderStatus.REFUND, ClassicOrderStatus.CHARGEBACK, ClassicOrderStatus.CHARGEBACK),
        (ClassicOrderStatus.CHARGEBACK, ClassicOrderStatus.REFUND, ClassicOrderStatus.REFUND),

        (ClassicOrderStatus.REFUND, ClassicOrderStatus.SUCCESS, ClassicOrderStatus.REFUND),
        (ClassicOrderStatus.REFUND, ClassicOrderStatus.REVERSE, ClassicOrderStatus.REVERSE),
        (ClassicOrderStatus.REFUND, ClassicOrderStatus.HOLD, ClassicOrderStatus.REFUND),

        (ClassicOrderStatus.REVERSE, ClassicOrderStatus.SUCCESS, ClassicOrderStatus.REVERSE),
        (ClassicOrderStatus.REVERSE, ClassicOrderStatus.REVERSE, ClassicOrderStatus.REVERSE),
        (ClassicOrderStatus.REVERSE, ClassicOrderStatus.HOLD, ClassicOrderStatus.REVERSE),

        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.FAIL, ClassicOrderStatus.SUCCESS),
        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.HOLD, ClassicOrderStatus.SUCCESS),
    ))
    async def test_status_mapping(
        self, storage, prev_status, status, expected_new_status, message_id, uid, time_now, order
    ):
        order.status = prev_status
        order.event_time = time_now - timedelta(minutes=1)
        await storage.order.save(order)

        await UpdateOrderStatusAction(
            message_id=message_id, status=status, event_time=time_now
        ).run()

        updated = await storage.order.get_by_message_id(message_id)
        assert_that(
            updated,
            has_properties(
                status=expected_new_status,
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('status', list(ClassicOrderStatus))
    @pytest.mark.parametrize('prev_status', list(ClassicOrderStatus))
    async def test_force_status_flag_enabled(
        self, storage, prev_status, status, order, time_now
    ):
        order.status = prev_status
        order.event_time = time_now - timedelta(minutes=1)
        await storage.order.save(order)

        await UpdateOrderStatusAction(
            order=order, status=status, event_time=time_now, force_status=True
        ).run()

        updated = await storage.order.get_by_message_id(order.message_id)
        assert_that(updated, has_properties(status=status))

    @pytest.mark.asyncio
    async def test_when_order_is_missing(self, time_now):
        with pytest.raises(OrderNotFoundError):
            await UpdateOrderStatusAction(
                message_id='missing', status=ClassicOrderStatus.SUCCESS, event_time=time_now
            ).run()

    @pytest.mark.asyncio
    async def test_both_order_and_message_id_given(self, order):
        with pytest.raises(CoreFailError) as exc_info:
            await UpdateOrderStatusAction(
                message_id=order.message_id,
                order=order,
                status=ClassicOrderStatus.SUCCESS,
                event_time=utcnow(),
            ).run()

        pattern = 'Either message_id or the order object must be provided'
        assert_that(exc_info.value.__cause__, has_string(pattern))

    @pytest.mark.asyncio
    async def test_event_too_old(self, storage, message_id, time_now, order):
        order.order_event_processed = time_now
        await storage.order.save(order)

        with pytest.raises(OrderEventTooOldError):
            await UpdateOrderStatusAction(
                message_id=message_id, status=ClassicOrderStatus.SUCCESS, event_time=time_now
            ).run()

    @pytest.mark.asyncio
    async def test_check_event_time_flag_off(
        self, storage, message_id, time_now, order
    ):
        order.order_event_processed = time_now
        await storage.order.save(order)

        await UpdateOrderStatusAction(
            message_id=message_id,
            status=ClassicOrderStatus.SUCCESS,
            event_time=time_now,
            check_event_time=False,
        ).run()

    @pytest.mark.asyncio
    async def test_naive_event_timestamp_not_allowed(self, message_id):
        with pytest.raises(CoreFailError) as exc_info:
            await UpdateOrderStatusAction(
                message_id=message_id,
                status=ClassicOrderStatus.HOLD,
                event_time=datetime.now(),
            ).run()

        pattern = 'Naive timestamps are not acceptable'
        assert_that(exc_info.value.__cause__, instance_of(AssertionError))
        assert_that(exc_info.value.__cause__, has_string(pattern))

    class TestSuccessStatus:
        @pytest.mark.asyncio
        @pytest.mark.parametrize('payment_method_type', list(PaymentMethodType))
        async def test_success__creates_async_actions(
            self, message_id, time_now, payment_method_type, storage
        ):
            order = await storage.order.get_by_message_id(message_id)
            order.payment_method_type = payment_method_type
            await storage.order.save(order)

            await UpdateOrderStatusAction(
                message_id=message_id, status=ClassicOrderStatus.SUCCESS, event_time=time_now, amount=Decimal(100),
            ).run()

            expected_tasks = [
                has_properties(
                    task_type=TaskType.RUN_ACTION,
                    action_name=CreateCashbackTransactionAction.action_name,
                    params=has_entries(
                        action_kwargs={'uid': order.uid, 'order_id': order.order_id},
                    ),
                    state=TaskState.PENDING,
                ),
            ]

            if payment_method_type == PaymentMethodType.CARD:
                expected_tasks.append(
                    has_properties(
                        task_type=TaskType.RUN_ACTION,
                        action_name=SendOrderReceiptAction.action_name,
                        params=has_entries(
                            action_kwargs={'uid': order.uid, 'order_id': order.order_id},
                        ),
                        state=TaskState.PENDING,
                    ),
                )

            assert_that(
                await alist(storage.task.find()),
                contains_inanyorder(*expected_tasks),
            )

        @pytest.mark.asyncio
        @pytest.mark.parametrize('payment_method_type', list(PaymentMethodType))
        async def test_hold__creates_async_actions(
            self, message_id, time_now, payment_method_type, storage
        ):
            order = await storage.order.get_by_message_id(message_id)
            order.payment_method_type = payment_method_type
            await storage.order.save(order)

            await UpdateOrderStatusAction(
                message_id=message_id, status=ClassicOrderStatus.HOLD, event_time=time_now, amount=Decimal(100),
            ).run()

            expected_tasks = []

            if payment_method_type == PaymentMethodType.CARD:
                expected_tasks = [
                    has_properties(
                        task_type=TaskType.RUN_ACTION,
                        action_name=SendOrderReceiptAction.action_name,
                        params=has_entries(
                            action_kwargs={'uid': order.uid, 'order_id': order.order_id},
                        ),
                        state=TaskState.PENDING,
                    ),
                ]

            assert_that(
                await alist(storage.task.find()),
                contains_inanyorder(*expected_tasks),
            )

        @pytest.mark.asyncio
        async def test_success__when_cashback_is_zero__does_not_create_transaction_task(
            self, message_id, time_now, storage, order, budget
        ):
            await storage.cashback_budget.delete(budget)
            order.cashback = Decimal('0')
            await storage.order.save(order)

            await UpdateOrderStatusAction(
                message_id=message_id, status=ClassicOrderStatus.SUCCESS, event_time=time_now
            ).run()

            assert_that(
                await alist(storage.task.find()),
                contains(
                    has_properties(action_name=SendOrderReceiptAction.action_name),
                )
            )

        @pytest.mark.asyncio
        async def test_success__when_transaction_already_exists__create_transaction_task_not_created(
            self, message_id, uid, cashback_account, order, time_now, storage
        ):
            await storage.cashback_transaction.create(
                CashbackTransaction(
                    uid=uid,
                    order_id=order.order_id,
                    account_id=cashback_account.account_id,
                    trust_purchase_token='fake_purchase_token',
                    status=CashbackTransactionStatus.STARTED,
                    has_plus=False,
                )
            )

            await UpdateOrderStatusAction(
                message_id=message_id, status=ClassicOrderStatus.SUCCESS, event_time=time_now
            ).run()

            assert_that(
                await alist(storage.task.find()),
                contains(
                    has_properties(action_name=SendOrderReceiptAction.action_name),
                )
            )

    class TestRefundStatus:
        @pytest.mark.asyncio
        @pytest.mark.parametrize('status', list(ClassicOrderStatus.get_refund_statuses()))
        async def test_refund__when_order_status_is_not_success(
            self, storage, message_id, status, time_now, order
        ):
            await UpdateOrderStatusAction(
                message_id=message_id, status=status, event_time=time_now
            ).run()

            order = await storage.order.get(order.uid, order.order_id)
            assert_that(order.status, equal_to(ClassicOrderStatus.FAIL))

            # no tasks created
            assert_that(await alist(storage.task.find()), equal_to([]))

        @pytest.mark.parametrize('amount, expected_refund_amount', (
            (None, Decimal('90')),
            (Decimal('10'), Decimal('10')),
            (Decimal('100'), Decimal('90')),
        ))
        @pytest.mark.asyncio
        async def test_refund__creates_refund_task_with_correct_params(
            self, message_id, time_now, storage, order, amount, expected_refund_amount
        ):
            order.status = ClassicOrderStatus.SUCCESS
            order.event_time = time_now - timedelta(minutes=1)
            order.amount = Decimal('100')
            order.refunded_amount = Decimal('10')
            await storage.order.save(order)

            updated = await UpdateOrderStatusAction(
                message_id=message_id,
                status=ClassicOrderStatus.REFUND,
                event_time=time_now,
                amount=amount,
            ).run()

            tasks = await alist(storage.task.find())
            assert_that(tasks, has_length(1))
            assert_that(
                tasks[0],
                has_properties(
                    task_type=TaskType.RUN_ACTION,
                    action_name=CreateCashbackRefundAction.action_name,
                    params=has_entries(
                        action_kwargs=has_entries({
                            'uid': updated.uid,
                            'order_id': updated.order_id,
                            'event_time': convert_then_match(datetime.fromisoformat, equal_to(time_now)),
                            'amount': convert_then_match(Decimal, equal_to(expected_refund_amount)),
                        }),
                    ),
                    state=TaskState.PENDING,
                )
            )

        @pytest.mark.asyncio
        async def test_refund__when_refund_transaction_already_exists__should_not_init_refund_transaction(
            self, message_id, uid, cashback_account, order, time_now, storage
        ):
            order.status = ClassicOrderStatus.SUCCESS
            await storage.order.save(order)
            transaction = await storage.cashback_transaction.create(
                CashbackTransaction(
                    uid=uid,
                    order_id=order.order_id,
                    account_id=cashback_account.account_id,
                    trust_purchase_token='fake_purchase_token',
                    status=CashbackTransactionStatus.STARTED,
                    has_plus=False,
                )
            )
            await storage.cashback_refund.create(
                CashbackRefund(
                    refund_id=uuid4(),
                    uid=transaction.uid,
                    transaction_id=transaction.transaction_id,
                    trust_refund_id='kinda-trust-refund-id',
                    event_time=time_now,
                    status=CashbackRefundStatus.CREATED,
                    amount=Decimal('100')
                )
            )

            await UpdateOrderStatusAction(
                message_id=message_id, status=ClassicOrderStatus.REFUND, event_time=time_now
            ).run()

            assert_that(await alist(storage.task.find()), equal_to([]))

        @pytest.mark.asyncio
        async def test_refund__when_refund_amount_is_zero__does_not_create_refund_task(
            self, message_id, time_now, storage, order
        ):
            order.status = ClassicOrderStatus.SUCCESS
            await storage.order.save(order)

            await UpdateOrderStatusAction(
                message_id=message_id,
                status=ClassicOrderStatus.REFUND,
                event_time=time_now,
                amount=Decimal('0'),
            ).run()

            assert_that(await alist(storage.task.find()), equal_to([]))

    class TestFailureStatus:
        @pytest.mark.asyncio
        @pytest.mark.parametrize(
            'status',
            list({ClassicOrderStatus.FAIL, ClassicOrderStatus.REVERSE}),
        )
        async def test_failure__does_not_create_transaction_task(
            self, status, message_id, time_now, storage
        ):
            await UpdateOrderStatusAction(
                message_id=message_id, status=status, event_time=time_now
            ).run()

            assert_that(await alist(storage.task.find()), equal_to([]))

    @pytest.mark.asyncio
    async def test_order_update_reflected_in_product_log(
        self, message_id, time_now, order, caplog
    ):
        status = ClassicOrderStatus.SUCCESS
        product_log = get_product_logger()
        caplog.set_level(logging.INFO, logger=product_log.name)
        reason_code = 'fake_reason_code'
        reason = 'fake_reason'

        updated_order = await UpdateOrderStatusAction(
            message_id=message_id,
            status=status,
            event_time=time_now,
            reason=reason,
            reason_code=reason_code,
        ).run()

        logs = [r for r in caplog.records if r.name == product_log.name]
        assert_that(logs, has_length(1))
        assert_that(
            logs[0],
            has_properties(
                message='Order status updated',
                _context=has_entries(
                    status=status,
                    original_order=order,
                    updated_order=updated_order,
                    reason=reason,
                    reason_code=reason_code,
                ),
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'prev_status, status',
        [
            (ClassicOrderStatus.HOLD, ClassicOrderStatus.SUCCESS),
            (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.HOLD),
            (ClassicOrderStatus.CHARGEBACK, ClassicOrderStatus.REFUND),
            (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.CHARGEBACK)
        ]
    )
    async def test_order_sent_to_payments_history(
        self,
        status,
        prev_status,
        message_id,
        order,
        time_now,
        storage,
        mock_send_to_payments_history_action
    ):
        order.status = prev_status
        order.event_time = time_now - timedelta(minutes=1)
        await storage.order.save(order)
        await UpdateOrderStatusAction(
            message_id=message_id, status=status, event_time=time_now
        ).run()

        mock_send_to_payments_history_action.assert_called_once()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'prev_status, status',
        [
            (ClassicOrderStatus.NEW, ClassicOrderStatus.NEW),
            (ClassicOrderStatus.HOLD, ClassicOrderStatus.REFUND),  # fail,
            (ClassicOrderStatus.NEW, ClassicOrderStatus.FAIL),
        ]
    )
    async def test_order_not_sent_to_payments_history(
        self,
        status,
        prev_status,
        message_id,
        order,
        time_now,
        storage,
        mock_send_to_payments_history_action
    ):
        order.status = prev_status
        order.event_time = time_now - timedelta(minutes=1)
        await storage.order.save(order)
        await UpdateOrderStatusAction(
            message_id=message_id, status=status, event_time=time_now
        ).run()

        mock_send_to_payments_history_action.assert_not_called()
