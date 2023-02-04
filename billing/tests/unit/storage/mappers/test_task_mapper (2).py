from datetime import datetime, timedelta

import pytest

from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TaskType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.task import Task


@pytest.mark.asyncio
async def test_can_delete_tasks(storage):
    tasks = (
        Task(task_type=TaskType.RUN_ACTION, state=TaskState.FAILED, updated=datetime.utcnow() - timedelta(days=365)),
        Task(task_type=TaskType.RUN_ACTION, state=TaskState.FINISHED, updated=datetime.utcnow() - timedelta(days=365)),
        Task(task_type=TaskType.RUN_ACTION, state=TaskState.PENDING, updated=datetime.utcnow() - timedelta(days=365)),
        Task(task_type=TaskType.RUN_ACTION, state=TaskState.FAILED, updated=datetime.utcnow()),
        Task(task_type=TaskType.RUN_ACTION, state=TaskState.FINISHED, updated=datetime.utcnow()),
    )
    created_tasks = []
    for task in tasks:
        created_tasks.append(await storage.task.create(task))

    deleted_count = await storage.task.delete_finished_and_failed_tasks(batch_size=10, period_offset=timedelta(days=1))

    expected_left_tasks = [
        created_tasks[2],
        created_tasks[3],
        created_tasks[4],
    ]
    tasks_from_base = await alist(storage.task.find())
    assert deleted_count == 2
    assert_that(tasks_from_base, contains_inanyorder(*expected_left_tasks))

    zero_deleted_count = await storage.task.delete_finished_and_failed_tasks(
        batch_size=10, period_offset=timedelta(days=1)
    )
    assert zero_deleted_count == 0


@pytest.mark.asyncio
async def test_delete_is_limited_by_batch_size(storage):
    tasks = (
        Task(task_type=TaskType.RUN_ACTION, state=TaskState.FAILED, updated=datetime.utcnow() - timedelta(days=365)),
        Task(task_type=TaskType.RUN_ACTION, state=TaskState.FINISHED, updated=datetime.utcnow() - timedelta(days=365)),
    )
    for task in tasks:
        await storage.task.create(task)

    deleted_count = await storage.task.delete_finished_and_failed_tasks(batch_size=1, period_offset=timedelta(days=1))
    assert deleted_count == 1

    deleted_count = await storage.task.delete_finished_and_failed_tasks(batch_size=1, period_offset=timedelta(days=1))
    assert deleted_count == 1

    deleted_count = await storage.task.delete_finished_and_failed_tasks(batch_size=1, period_offset=timedelta(days=1))
    assert deleted_count == 0
