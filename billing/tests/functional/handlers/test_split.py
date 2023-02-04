import re
from dataclasses import replace
from uuid import uuid4

import pytest

from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_item, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.internal.split import SplitCallbackHandler
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import SplitCallbackEventType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import ClassicOrderStatus, OrderEventStatus

URL = '/api/internal/v2/split/callback'


@pytest.fixture(autouse=True)
def mock_internal_tvm_with_acl(aioresponses_mocker, mocker, yandex_pay_plus_settings):
    matcher = SplitCallbackHandler.TICKET_CHECKER.src_matchers[0]

    aioresponses_mocker.get(
        re.compile(f'.*{yandex_pay_plus_settings.TVM_URL}/tvm/checksrv.*'),
        payload={'src': matcher.tvm_id, 'dst': matcher.tvm_id},
    )

    mocker.patch.object(matcher, 'acls', {'split'})


@pytest.fixture
def data(randn):
    return {
        'order_id': str(uuid4()),
        'external_id': str(randn),
        'status': 'approved',
        'merchant_id': 'yapay',
    }


@pytest.fixture
async def storage_merchant(storage, entity_merchant, data):
    merchant = await storage.merchant.create(
        replace(
            entity_merchant,
            split_merchant_id=data['merchant_id'],
            merchant_id=uuid4(),
        )
    )
    yield merchant

    await storage.merchant.delete(merchant)


@pytest.fixture(autouse=True)
async def old_tasks(storage):
    old_tasks_ids = {
        task.task_id
        async for task in storage.task.find()
    }
    yield old_tasks_ids

    filters = {'task_id': lambda field: ~field.in_(old_tasks_ids)}
    async for task in storage.task.find(filters=filters):
        await storage.task.delete(task)


@pytest.mark.asyncio
@pytest.mark.parametrize('event_type', [SplitCallbackEventType.ORDER_UPDATE, None])
async def test_update_order_status__bnpl_not_finished(
    app, data, event_type, old_tasks, storage, yandex_pay_plus_settings, storage_merchant
):
    params = None if event_type is None else {'event_type': event_type.value}

    r = await app.post(
        URL,
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json=data,
        params=params,
    )

    assert_that(r.status, equal_to(200))

    filters = {'task_id': lambda field: ~field.in_(old_tasks)}
    tasks = await alist(storage.task.find(filters=filters))
    assert_that(
        tasks,
        has_item(
            has_properties(
                action_name='update_merchant_order',
                state=TaskState.PENDING,
            )
        )
    )

    events = await storage.order_status_event.find_by_message_id(
        f'2:{storage_merchant.merchant_id}_{data["external_id"]}'
    )
    assert_that(
        events,
        has_item(
            has_properties(
                order_status=ClassicOrderStatus.HOLD,
                event_status=OrderEventStatus.PENDING,
            )
        )
    )


@pytest.mark.asyncio
async def test_update_order_status__bnpl_finished(
    app, data, old_tasks, storage, yandex_pay_plus_settings, storage_merchant
):
    r = await app.post(
        URL,
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json=data,
        params={'event_type': SplitCallbackEventType.BNPL_FINISHED.value},
    )

    assert_that(r.status, equal_to(200))

    filters = {'task_id': lambda field: ~field.in_(old_tasks)}
    tasks = await alist(storage.task.find(filters=filters))
    assert_that(tasks, equal_to([]))

    events = await storage.order_status_event.find_by_message_id(
        f'2:{storage_merchant.merchant_id}_{data["external_id"]}'
    )
    assert_that(
        events,
        has_item(
            has_properties(
                order_status=ClassicOrderStatus.SUCCESS,
                event_status=OrderEventStatus.PENDING,
            )
        )
    )
