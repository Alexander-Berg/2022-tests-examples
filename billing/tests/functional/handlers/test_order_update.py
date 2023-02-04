import random
import uuid
from datetime import datetime, timedelta
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.api.schemas.order import OrderSchema
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import EnsureCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.status_event.create import CreateOrderStatusEventAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.status_event.process import (
    ProcessOrderStatusEventsAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.create import CreateCashbackRefundAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    ClassicOrderStatus,
    PaymentMethodType,
    TaskType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order


@pytest.fixture(scope='module')
def uid():
    return random.randint(1, 10 ** 8)


@pytest.fixture
def message_id(rands_url_safe):
    return rands_url_safe(k=32)


@pytest.fixture
def currency():
    return 'XTS'


@pytest.fixture(autouse=True)
def mock_currency_default_cashback(yandex_pay_plus_settings, currency):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT[currency] = 1000
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT[currency] = 3000


@pytest.fixture
def time_now():
    return utcnow()


@pytest.fixture
async def customer(uid):
    return await EnsureCustomerAction(uid=uid).run()


@pytest.fixture(autouse=True)
async def budget(storage, currency):
    return await storage.cashback_budget.create(
        CashbackBudget(
            budget_id=uuid.uuid4(),
            currency=currency,
            spent=Decimal('0'),
            spending_limit=Decimal('1000000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10),
        )
    )


@pytest.fixture
def order_data(customer, message_id, currency):
    order = Order(
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
    merchant = {
        'id': str(order.merchant_id),
        'name': 'name',
        'url': 'https://url.test',
    }
    payload = {
        'uid': order.uid,
        'message_id': order.message_id,
        'currency': currency,
        'psp_id': str(order.psp_id),
        'trust_card_id': 'xxx',
        'amount': '100.0',
        'merchant': merchant,
        'cashback_category_id': '0.1',
        'payment_method_type': order.payment_method_type.value,
    }
    return payload


@pytest.fixture(autouse=True)
async def order(app, storage, order_data, message_id):
    r = await app.post('/api/v1/orders', json=order_data)
    await r.json()

    order = await storage.order.get_by_message_id(message_id)
    return order


@pytest.fixture
async def historical_tasks(storage):
    return {t.task_id async for t in storage.task.find()}


class TestSynchronousOrderUpdate:
    @pytest.fixture(autouse=True)
    def turn_async_update_off(self, yandex_pay_plus_settings):
        yandex_pay_plus_settings.ASYNC_ORDER_STATUS_UPDATE_ENABLED = False

    @pytest.mark.asyncio
    async def test_updates_order(self, app, message_id, uid, time_now, storage, order):
        json_body = {
            'event_time': time_now.isoformat(sep=' '),
            'status': ClassicOrderStatus.SUCCESS.value,
            'amount': '100',
        }

        await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)
        updated = await storage.order.get_by_message_id(message_id)

        assert_that(
            updated,
            has_properties(
                uid=uid,
                status=ClassicOrderStatus.SUCCESS,
                order_event_processed=time_now,
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('prev_status,status', (
        (ClassicOrderStatus.NEW, ClassicOrderStatus.FAIL),
        (ClassicOrderStatus.NEW, ClassicOrderStatus.HOLD),
        (ClassicOrderStatus.NEW, ClassicOrderStatus.REVERSE),
        (ClassicOrderStatus.NEW, ClassicOrderStatus.SUCCESS),
        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.REFUND),
        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.CHARGEBACK),
    ))
    async def test_result(
        self, app, prev_status, status, message_id, time_now, storage, order
    ):
        order.status = prev_status
        await storage.order.save(order)
        json_body = {
            'event_time': time_now.isoformat(sep=' '),
            'status': status.value,
            'amount': '100',
        }

        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)
        updated = await storage.order.get_by_message_id(message_id)
        expected = {
            'status': 'success',
            'code': 200,
            'data': OrderSchema().dump(updated).data,
        }

        assert_that(response.status, equal_to(200))
        assert_that(await response.json(), equal_to(expected))

    @pytest.mark.asyncio
    async def test_refund_creates_task(
        self, app, order, message_id, uid, time_now, historical_tasks, storage
    ):
        order = await storage.order.get_by_message_id(message_id)
        order.status = ClassicOrderStatus.SUCCESS
        order = await storage.order.save(order)
        json_body = {
            'event_time': time_now.isoformat(sep=' '),
            'status': ClassicOrderStatus.REFUND.value,
            'amount': '100',
        }

        await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)

        filters = {'action_name': 'create_cashback_refund', 'task_id': lambda field: ~field.in_(historical_tasks)}
        [new_task] = await alist(storage.task.find(filters=filters))

        assert_that(
            new_task,
            has_properties(
                task_type=TaskType.RUN_ACTION,
                action_name=CreateCashbackRefundAction.action_name,
                state=TaskState.PENDING,
                params=has_entries(
                    action_kwargs={
                        'uid': uid,
                        'order_id': order.order_id,
                        'event_time': time_now.isoformat(sep=' '),
                        'amount': '100',
                    }
                )
            )
        )


class TestAsyncOrderUpdate:
    @pytest.fixture(autouse=True)
    def turn_async_update_on(self, yandex_pay_plus_settings):
        yandex_pay_plus_settings.ASYNC_ORDER_STATUS_UPDATE_ENABLED = True

    @pytest.fixture(autouse=True)
    def patch_event_run_at(self, mocker):
        mocker.patch.object(CreateOrderStatusEventAction, 'run_at_delay_sec', -10)

    @pytest.fixture
    def event_time(self, time_now):
        return time_now - timedelta(seconds=10)

    @pytest.fixture
    def expected_response(self):
        return {
            'status': 'success',
            'code': 200,
            'data': {},
        }

    @pytest.mark.asyncio
    async def test_update_order(self, app, message_id, uid, event_time, storage, order):
        json_body = {
            'event_time': event_time.isoformat(sep=' '),
            'status': ClassicOrderStatus.SUCCESS.value,
            'amount': '200.00',
        }

        await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)
        await ProcessOrderStatusEventsAction().run()

        updated = await storage.order.get_by_message_id(message_id)
        assert_that(
            updated,
            has_properties(
                uid=uid,
                status=ClassicOrderStatus.SUCCESS,
                order_event_processed=event_time,
                amount=Decimal(200),
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('prev_status,status', (
        (ClassicOrderStatus.NEW, ClassicOrderStatus.FAIL),
        (ClassicOrderStatus.NEW, ClassicOrderStatus.HOLD),
        (ClassicOrderStatus.NEW, ClassicOrderStatus.SUCCESS),
        (ClassicOrderStatus.HOLD, ClassicOrderStatus.SUCCESS),
        (ClassicOrderStatus.HOLD, ClassicOrderStatus.REVERSE),
        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.REFUND),
        (ClassicOrderStatus.SUCCESS, ClassicOrderStatus.CHARGEBACK),
    ))
    async def test_result(
        self, app, prev_status, status, message_id, event_time, storage, order, expected_response
    ):
        order.status = prev_status
        await storage.order.save(order)
        json_body = {
            'event_time': event_time.isoformat(sep=' '),
            'status': status.value,
            'amount': '100',
        }

        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)
        await ProcessOrderStatusEventsAction().run()

        updated = await storage.order.get_by_message_id(message_id)
        assert_that(updated, has_properties(status=status))

        assert_that(response.status, equal_to(200))
        assert_that(await response.json(), equal_to(expected_response))

    @pytest.mark.parametrize(
        'target_status', (ClassicOrderStatus.REFUND, ClassicOrderStatus.REVERSE, ClassicOrderStatus.CHARGEBACK)
    )
    @pytest.mark.asyncio
    async def test_refund_creates_task(
        self, app, order, message_id, uid, event_time, storage, historical_tasks, target_status
    ):
        order = await storage.order.get_by_message_id(message_id)
        order.status = ClassicOrderStatus.SUCCESS
        order = await storage.order.save(order)
        json_body = {
            'event_time': event_time.isoformat(sep=' '),
            'status': target_status.value,
            'amount': '100',
        }

        await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)
        await ProcessOrderStatusEventsAction().run()

        filters = {'action_name': 'create_cashback_refund', 'task_id': lambda field: ~field.in_(historical_tasks)}
        [new_task] = await alist(storage.task.find(filters=filters))
        assert_that(
            new_task,
            has_properties(
                task_type=TaskType.RUN_ACTION,
                action_name=CreateCashbackRefundAction.action_name,
                state=TaskState.PENDING,
                params=has_entries(
                    action_kwargs=has_entries(
                        uid=uid,
                        order_id=order.order_id,
                        event_time=convert_then_match(
                            datetime.fromisoformat, event_time
                        ),
                        amount=convert_then_match(Decimal, Decimal(100)),
                    )
                )
            )
        )
