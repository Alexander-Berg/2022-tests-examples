import itertools
from datetime import datetime, timedelta

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, equal_to

from billing.yandex_pay.yandex_pay.core.entities.enums import TaskType
from billing.yandex_pay.yandex_pay.core.entities.task import Task, TaskState


@pytest.mark.asyncio
async def test_get_pending_tasks_count(storage):
    actions = ['action-1', 'action-2']
    for state, action_name in itertools.product((TaskState.PENDING, TaskState.FAILED), actions):
        await storage.task.create(
            Task(task_type=TaskType.RUN_ACTION, action_name=action_name, state=state)
        )

    assert_that(
        await storage.task.get_pending_tasks_count(),
        equal_to(dict([(action_name, 1) for action_name in actions]))
    )


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

    deleted_count = await storage.task.delete_finished_and_failed_tasks(
        batch_size=10, period_offset=timedelta(days=1)
    )

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
