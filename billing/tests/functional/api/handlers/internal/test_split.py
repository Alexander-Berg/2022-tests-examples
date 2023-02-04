import re

import pytest

from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, equal_to, has_properties

from billing.yandex_pay.yandex_pay.api.handlers.internal.split import SplitCallbackHandler
from billing.yandex_pay.yandex_pay.core.entities.enums import SplitCallbackEventType

URL = '/api/internal/v1/split/callback'


@pytest.fixture(autouse=True)
def mock_internal_tvm_with_acl(aioresponses_mocker, mocker, yandex_pay_settings):
    matcher = SplitCallbackHandler.TICKET_CHECKER.src_matchers[0]

    aioresponses_mocker.get(
        re.compile(f'.*{yandex_pay_settings.TVM_URL}/tvm/checksrv.*'),
        payload={'src': matcher.tvm_id, 'dst': matcher.tvm_id},
    )

    mocker.patch.object(matcher, 'acls', {'split'})


@pytest.fixture
def data():
    return {
        'order_id': '123',
        'external_id': '456',
        'status': 'approved',
        'merchant_id': 'yapay',
    }


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
@pytest.mark.parametrize('event_type', list(SplitCallbackEventType))
async def test_update_order_status(internal_app, data, event_type, old_tasks, storage):
    r = await internal_app.post(
        URL,
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json=data,
        params={
            'event_type': event_type.value
        }
    )

    assert_that(r.status, equal_to(200))

    filters = {'task_id': lambda field: ~field.in_(old_tasks)}
    tasks = await alist(storage.task.find(filters=filters))
    expected_tasks = [
        has_properties(
            action_name='plus_backend_update_order_status',
            state=TaskState.PENDING,
        )
    ]
    if event_type != SplitCallbackEventType.BNPL_FINISHED:
        expected_tasks.append(
            has_properties(
                action_name='update_merchant_order',
                state=TaskState.PENDING,
            )
        )

    assert_that(tasks, contains_inanyorder(*expected_tasks))
