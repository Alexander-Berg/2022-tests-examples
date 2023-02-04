import random
import re
import string
from datetime import datetime, timedelta
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow, without_none

from hamcrest import assert_that, equal_to, greater_than, has_entries, has_items, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.category import GetCashbackCategoryAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.create_transaction import (
    CreateCashbackTransactionAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.start_transaction import (
    StartCashbackTransactionAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.update_transaction_status import (
    UpdateCashbackTransactionStatusAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.status_event.create import CreateOrderStatusEventAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.status_event.process import (
    ProcessOrderStatusEventsAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.create import CreateCashbackRefundAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.start import StartCashbackRefundAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.update_status import UpdateCashbackRefundStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackRefundStatus,
    CashbackTransactionStatus,
    ClassicOrderStatus,
)

CURRENCY = 'XTS'


@pytest.fixture
async def close_existing_budgets(storage):
    # this is to ensure that the budget created in the module
    # is always captured as the richest available for chosen currency
    filters = {'currency': CURRENCY}

    for budget in await alist(storage.cashback_budget.find(filters=filters)):
        budget.period_end = utcnow() - timedelta(days=1)
        await storage.cashback_budget.save(budget)


@pytest.fixture
async def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
async def uid(randn):
    return randn()


@pytest.fixture
def message_id():
    return ''.join(random.choices(string.ascii_letters + string.digits, k=32))


@pytest.fixture
def amount():
    return Decimal(100)


@pytest.fixture
def trust_account_id(rands):
    return rands()


@pytest.fixture
def cashback(amount):
    return amount * Decimal('0.1')


@pytest.fixture(autouse=True)
def set_default_cashback(yandex_pay_plus_settings, mocker):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT = {CURRENCY: '1000'}
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT = {CURRENCY: '3000'}
    mocker.patch.object(GetCashbackCategoryAction, 'cashback_categories', [Decimal('0.05'), Decimal('0.1')])
    mocker.patch.object(GetCashbackCategoryAction, 'cashback_default_category', Decimal('0.05'))


@pytest.fixture
def patch_async_update(yandex_pay_plus_settings, request):
    yandex_pay_plus_settings.ASYNC_ORDER_STATUS_UPDATE_ENABLED = request.param


@pytest.fixture(autouse=True)
async def budget(storage, close_existing_budgets):
    return await storage.cashback_budget.create(
        CashbackBudget(
            budget_id=uuid4(),
            currency=CURRENCY,
            spent=Decimal('0'),
            spending_limit=Decimal('1000000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10),
        )
    )


@pytest.fixture(autouse=True)
def mock_have_plus(aioresponses_mocker, uid):
    return aioresponses_mocker.get(
        re.compile(r'.*/blackbox\?.*'),
        payload={
            'users': [
                {
                    'id': str(uid),
                    'uid': {'value': str(uid)},
                    'attributes': {'1015': '1'}
                }
            ]
        }
    )


@pytest.fixture(autouse=True)
def mock_create_trust_account(aioresponses_mocker, yandex_pay_plus_settings, trust_account_id):
    return aioresponses_mocker.post(
        f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/account',
        payload={
            'status': 'success',
            'id': trust_account_id,
            'currency': CURRENCY,
        }
    )


@pytest.fixture(autouse=True)
def mock_get_payment_methods(aioresponses_mocker, yandex_pay_plus_settings, trust_account_id):
    return aioresponses_mocker.get(
        f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/payment-methods',
        payload={
            'status': 'success',
            'bound_payment_methods': [
                {
                    'id': f'yandex-account-{trust_account_id}',
                    'payment_method': 'yandex_account',
                    'balance': '1000.00',
                    'account': trust_account_id,
                    'currency': CURRENCY,
                }
            ]
        }
    )


@pytest.fixture(autouse=True)
def mock_create_trust_transaction(aioresponses_mocker, yandex_pay_plus_settings, rands):
    return aioresponses_mocker.post(
        f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/topup',
        payload={
            'status': 'success',
            'purchase_token': rands(),
        },
    )


@pytest.fixture(autouse=True)
def mock_create_trust_refund(aioresponses_mocker, yandex_pay_plus_settings, rands):
    return aioresponses_mocker.post(
        f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/refunds',
        payload={
            'status': 'success',
            'trust_refund_id': rands(),
        },
    )


@pytest.fixture(autouse=True)
def mock_start_trust_transaction(aioresponses_mocker, yandex_pay_plus_settings):
    url = re.compile(
        f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}'
        f'/trust-payments/v2/topup/.+/start$',
    )
    return aioresponses_mocker.post(
        url,
        payload={
            'status': 'success',
            'payment_status': 'started',
        },
    )


@pytest.fixture(autouse=True)
def mock_start_refund_transaction(aioresponses_mocker, yandex_pay_plus_settings):
    url = re.compile(
        f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}'
        f'/trust-payments/v2/refunds/.+/start$',
    )
    return aioresponses_mocker.post(
        url,
        payload={
            'status': 'success',
            'status_desc': 'started',
        },
    )


@pytest.fixture(autouse=True)
def mock_get_trust_transaction_status(aioresponses_mocker, yandex_pay_plus_settings):
    url = re.compile(
        f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}'
        f'/trust-payments/v2/payments/.+$',
    )
    return aioresponses_mocker.get(
        url,
        payload={
            'status': 'success',
            'payment_status': 'cleared',
        },
    )


@pytest.fixture(autouse=True)
def mock_get_trust_refund_status(aioresponses_mocker, yandex_pay_plus_settings):
    url = re.compile(
        f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}'
        f'/trust-payments/v2/refunds/[^/]+$',
    )
    return aioresponses_mocker.get(
        url,
        payload={
            'status': 'success',
            'status_desc': 'hello',
        },
    )


async def create_order(app, uid, message_id, amount, trust_card_id):
    merchant = {
        'id': str(uuid4()),
        'name': 'name',
        'url': 'https://url.test',
    }
    order_data = {
        'uid': uid,
        'message_id': message_id,
        'currency': CURRENCY,
        'amount': str(amount),
        'psp_id': str(uuid4()),
        'merchant': merchant,
        'trust_card_id': trust_card_id,
        'cashback_category_id': '0.1'
    }
    r = await app.post('/api/v1/orders', json=order_data)

    data = await r.json()
    assert_that(r.status, equal_to(200), data)


async def update_order_status(app, message_id, status, amount=None, event_time_delta=None):
    event_time_delta = event_time_delta or timedelta(seconds=0)
    event_time = utcnow() + event_time_delta
    data = {
        'event_time': event_time.isoformat(sep=' '),
        'status': status,
        'amount': None if amount is None else str(amount),
    }
    r = await app.patch(f'/api/v1/orders/{message_id}/status', json=without_none(data))

    data = await r.json()
    assert_that(r.status, equal_to(200), data)
    return event_time


async def run_task_for_transaction_creation(storage, order):
    filters = {'action_name': CreateCashbackTransactionAction.action_name}
    transaction_creation_tasks = await alist(storage.task.find(filters=filters))
    action_kwargs = {'uid': order.uid, 'order_id': order.order_id}
    assert_that(
        transaction_creation_tasks,
        has_items(
            has_properties(
                params=has_entries(
                    action_kwargs=action_kwargs
                ),
                state=TaskState.PENDING,
            )
        )
    )

    await CreateCashbackTransactionAction(**action_kwargs).run()


async def run_task_for_transaction_start(storage, transaction):
    filters = {'action_name': StartCashbackTransactionAction.action_name}
    transaction_start_tasks = await alist(storage.task.find(filters=filters))
    action_kwargs = {
        'uid': transaction.uid,
        'transaction_id': transaction.transaction_id
    }
    assert_that(
        transaction_start_tasks,
        has_items(
            has_properties(
                params=has_entries(
                    action_kwargs=action_kwargs
                ),
                state=TaskState.PENDING,
            )
        )
    )

    await StartCashbackTransactionAction(**action_kwargs).run()


async def run_task_for_transaction_status_update(storage, transaction):
    filters = {'action_name': UpdateCashbackTransactionStatusAction.action_name}
    transaction_update_tasks = await alist(storage.task.find(filters=filters))
    action_kwargs = {
        'uid': transaction.uid,
        'transaction_id': transaction.transaction_id
    }
    assert_that(
        transaction_update_tasks,
        has_items(
            has_properties(
                params=has_entries(
                    action_kwargs=action_kwargs
                ),
                state=TaskState.PENDING,
                run_at=greater_than(utcnow()),
            )
        )
    )

    await UpdateCashbackTransactionStatusAction(**action_kwargs).run()


async def run_task_for_refund_creation(storage, order, event_time, amount):
    filters = {'action_name': CreateCashbackRefundAction.action_name}
    tasks = await alist(storage.task.find(filters=filters))
    action_kwargs = {
        'uid': order.uid,
        'order_id': order.order_id,
        'event_time': event_time.isoformat(sep=' '),
        'amount': str(amount),
    }
    assert_that(
        tasks,
        has_items(
            has_properties(
                params=has_entries(
                    action_kwargs=has_entries(
                        uid=action_kwargs['uid'],
                        order_id=action_kwargs['order_id'],
                        event_time=convert_then_match(
                            datetime.fromisoformat, event_time
                        ),
                        amount=convert_then_match(Decimal, amount),
                    )
                ),
                state=TaskState.PENDING,
            )
        )
    )

    await CreateCashbackRefundAction(**action_kwargs).run()


async def run_task_for_refund_start(storage, refund):
    filters = {'action_name': StartCashbackRefundAction.action_name}
    tasks = await alist(storage.task.find(filters=filters))
    action_kwargs = {'refund_id': str(refund.refund_id)}
    assert_that(
        tasks,
        has_items(
            has_properties(
                params=has_entries(
                    action_kwargs=action_kwargs
                ),
                state=TaskState.PENDING,
            )
        )
    )

    await StartCashbackRefundAction(**action_kwargs).run()


async def run_task_for_refund_status_check(storage, refund):
    filters = {'action_name': UpdateCashbackRefundStatusAction.action_name}
    refund_check_tasks = await alist(storage.task.find(filters=filters))
    action_kwargs = {
        'refund_id': str(refund.refund_id),
    }
    assert_that(
        refund_check_tasks,
        has_items(
            has_properties(
                params=has_entries(
                    action_kwargs=action_kwargs
                ),
                state=TaskState.PENDING,
            )
        )
    )

    await UpdateCashbackRefundStatusAction(**action_kwargs).run()


@pytest.mark.asyncio
@pytest.mark.parametrize('patch_async_update', [False], indirect=True)
async def test_synchronous_flow(
    app,
    storage,
    budget,
    trust_card_id,
    uid,
    message_id,
    patch_async_update,
    amount,
    cashback,
):
    # 1. create order
    await create_order(app, uid, message_id, amount, trust_card_id)
    order = await storage.order.get_by_message_id(message_id)
    assert_that(
        order,
        has_properties(
            uid=uid,
            cashback=10,
        )
    )

    updated_budget = await storage.cashback_budget.get(budget.budget_id)
    assert_that(
        updated_budget.get_unspent(),
        equal_to(budget.get_unspent())
    )

    # 2. update order status
    await update_order_status(app, message_id, ClassicOrderStatus.SUCCESS.value)

    # 3. resize the order
    order = await storage.order.get_by_message_id(message_id)
    order.amount /= 2
    order.cashback /= 2
    updated_cashback = cashback / 2
    await storage.order.save(order)

    # 4. create transaction
    await run_task_for_transaction_creation(storage, order)
    order = await storage.order.get_by_message_id(message_id)

    reservation = await storage.cashback_reservation.get(
        order.uid, order.reservation_id
    )
    assert_that(reservation.amount, equal_to(updated_cashback))

    # check the budget was resized
    updated_budget = await storage.cashback_budget.get(budget.budget_id)
    assert_that(
        updated_budget.get_unspent(),
        equal_to(budget.get_unspent() - updated_cashback),
    )

    transaction = await storage.cashback_transaction.get_by_uid_and_order_id(
        uid=order.uid, order_id=order.order_id,
    )
    assert_that(transaction.status, equal_to(CashbackTransactionStatus.CREATED))

    # 5. start transaction
    await run_task_for_transaction_start(storage, transaction)

    # check transaction status updated
    transaction = await storage.cashback_transaction.get(
        transaction.uid, transaction.transaction_id
    )
    assert_that(transaction.status, equal_to(CashbackTransactionStatus.STARTED))

    # check the reservation hasn't changed
    loaded_reservation = await storage.cashback_reservation.get(
        reservation.uid, reservation.reservation_id
    )
    assert_that(loaded_reservation, equal_to(reservation))
    order = await storage.order.get_by_message_id(message_id)
    assert_that(order.reservation_id, equal_to(reservation.reservation_id))

    # check the budget hasn't changed
    updated_budget = await storage.cashback_budget.get(budget.budget_id)
    assert_that(
        updated_budget.get_unspent(),
        equal_to(budget.get_unspent() - updated_cashback)
    )

    # 6. update transaction status
    await run_task_for_transaction_status_update(storage, transaction)

    # check transaction status updated
    transaction = await storage.cashback_transaction.get(
        transaction.uid, transaction.transaction_id
    )
    assert_that(transaction.status, equal_to(CashbackTransactionStatus.CLEARED))


@pytest.mark.asyncio
@pytest.mark.parametrize('patch_async_update', [True], indirect=True)
async def test_async_flow(
    app,
    storage,
    budget,
    trust_card_id,
    uid,
    message_id,
    patch_async_update,
    amount,
    cashback,
    mocker,
):
    mocker.patch.object(CreateOrderStatusEventAction, 'run_at_delay_sec', -10)

    # 1. create order
    await create_order(app, uid, message_id, amount, trust_card_id)
    order = await storage.order.get_by_message_id(message_id)
    assert_that(
        order,
        has_properties(
            uid=uid,
            cashback=10,
        )
    )

    updated_budget = await storage.cashback_budget.get(budget.budget_id)
    assert_that(
        updated_budget.get_unspent(),
        equal_to(budget.get_unspent())
    )

    # update amount and cashback
    updated_amount = amount / 2
    updated_cashback = cashback / 2

    # 2. update order status to SUCCESS
    event_sequence = [(ClassicOrderStatus.HOLD, None), (ClassicOrderStatus.SUCCESS, updated_amount)]
    for status, amount in event_sequence:
        await update_order_status(
            app,
            message_id,
            status.value,
            amount=amount,
            event_time_delta=timedelta(seconds=-10),
        )

    await ProcessOrderStatusEventsAction().run()

    # 3. create cashback transaction
    await run_task_for_transaction_creation(storage, order)
    order = await storage.order.get_by_message_id(message_id)

    reservation = await storage.cashback_reservation.get(
        order.uid, order.reservation_id
    )
    assert_that(reservation.amount, equal_to(updated_cashback))

    # check the budget was resized
    updated_budget = await storage.cashback_budget.get(budget.budget_id)
    assert_that(
        updated_budget.get_unspent(),
        equal_to(budget.get_unspent() - updated_cashback)
    )

    transaction = await storage.cashback_transaction.get_by_uid_and_order_id(
        uid=order.uid, order_id=order.order_id,
    )
    assert_that(transaction.status, equal_to(CashbackTransactionStatus.CREATED))

    # 4. start transaction
    await run_task_for_transaction_start(storage, transaction)

    # check transaction status updated
    transaction = await storage.cashback_transaction.get(
        transaction.uid, transaction.transaction_id
    )
    assert_that(transaction.status, equal_to(CashbackTransactionStatus.STARTED))

    # check the reservation hasn't changed
    loaded_reservation = await storage.cashback_reservation.get(
        reservation.uid, reservation.reservation_id
    )
    assert_that(loaded_reservation, equal_to(reservation))
    order = await storage.order.get_by_message_id(message_id)
    assert_that(order.reservation_id, equal_to(reservation.reservation_id))

    # check the budget hasn't changed
    updated_budget = await storage.cashback_budget.get(budget.budget_id)
    assert_that(
        updated_budget.get_unspent(),
        equal_to(budget.get_unspent() - updated_cashback)
    )

    # 5. update transaction status
    await run_task_for_transaction_status_update(storage, transaction)

    # check transaction status updated
    transaction = await storage.cashback_transaction.get(
        transaction.uid, transaction.transaction_id
    )
    assert_that(transaction.status, equal_to(CashbackTransactionStatus.CLEARED))

    # 6. Refund the order
    event_time = await update_order_status(
        app,
        message_id,
        ClassicOrderStatus.REFUND.value,
        event_time_delta=timedelta(seconds=-10),
    )
    await ProcessOrderStatusEventsAction().run()

    # 7. create cashback refund transaction
    await run_task_for_refund_creation(storage, order, event_time, updated_amount)
    order = await storage.order.get_by_message_id(message_id)
    assert_that(order.status, equal_to(ClassicOrderStatus.REFUND))

    refund = await storage.cashback_refund.get_by_transaction_and_event_time(
        uid=order.uid,
        transaction_id=transaction.transaction_id,
        event_time=event_time,
    )

    # 8. start cashback refund transaction
    await run_task_for_refund_start(storage, refund)
    refund = await storage.cashback_refund.get(refund.refund_id)
    assert_that(refund.status, equal_to(CashbackRefundStatus.IN_PROGRESS))

    await run_task_for_refund_status_check(storage, refund)
    refund = await storage.cashback_refund.get(refund.refund_id)
    assert_that(refund.status, equal_to(CashbackRefundStatus.SUCCESS))

    order = await storage.order.get_by_message_id(message_id)
    assert_that(
        order,
        has_properties(
            status=ClassicOrderStatus.REFUND,
            amount=updated_amount,
            cashback=updated_cashback,
            refunded_amount=updated_amount,
            refunded_cashback=updated_cashback,
            residual_amount=equal_to(0),
        )
    )
