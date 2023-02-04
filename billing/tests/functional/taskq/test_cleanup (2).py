import asyncio
from datetime import datetime, timedelta

import pytest

from sendr_taskqueue.worker.storage import TaskState
from sendr_utils import alist

from billing.yandex_pay_plus.yandex_pay_plus.storage import TaskMapper
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TaskType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.task import Task
from billing.yandex_pay_plus.yandex_pay_plus.taskq.cleanup import CleanupWorker


@pytest.fixture
async def prepare_tasks(storage) -> int:
    count = 20
    await storage.conn.execute('TRUNCATE yandex_pay_plus.tasks CASCADE;')
    tasks = [
        Task(task_type=TaskType.RUN_ACTION, state=TaskState.FAILED, updated=datetime.utcnow() - timedelta(days=365))
        for _ in range(count)
    ]
    for task in tasks:
        await storage.task.create(task)

    return count


@pytest.fixture(autouse=True)
def set_clean_tasks_batch_size_to_one(mocker):
    mocker.patch('billing.yandex_pay_plus.yandex_pay_plus.taskq.cleanup.clean_old_tasks_batch_size', 1)


@pytest.mark.asyncio
@pytest.mark.usefixtures('prepare_tasks')
async def test_should_clean_old_tasks(worker_app, dummy_logger, storage):
    worker = CleanupWorker(logger=dummy_logger)
    worker.app = worker_app

    await worker.process_task()

    tasks = await alist(storage.task.find())
    assert len(tasks) == 0


@pytest.mark.asyncio
async def test_should_process_concurrently_without_exceptions(
    worker_app, dummy_logger, storage, mocker, loop, prepare_tasks: int
):
    delete_spy = mocker.spy(TaskMapper, 'delete_finished_and_failed_tasks')
    workers_count = 5
    workers = []
    for i in range(workers_count):
        worker = CleanupWorker(logger=dummy_logger)
        worker.app = worker_app
        workers.append(worker)

    tasks = [loop.create_task(worker.process_task()) for worker in workers]

    await asyncio.gather(*tasks)

    assert prepare_tasks + 1 <= delete_spy.call_count < workers_count + prepare_tasks
