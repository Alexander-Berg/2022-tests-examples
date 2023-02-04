import asyncio
import uuid
from datetime import datetime, timedelta

import pytest

from sendr_taskqueue.worker.storage import TaskState, Worker, WorkerState
from sendr_utils import alist

from billing.yandex_pay.yandex_pay.core.entities.enums import TaskType, WorkerType
from billing.yandex_pay.yandex_pay.core.entities.sync_user_cards_worker import SyncUserCardsWorker
from billing.yandex_pay.yandex_pay.core.entities.task import Task
from billing.yandex_pay.yandex_pay.storage import TaskMapper
from billing.yandex_pay.yandex_pay.taskq.cleanup import CleanupWorker


class TestTasksCleanup:
    @pytest.fixture
    async def prepare_tasks(self, storage) -> int:
        count = 20
        await storage.conn.execute('TRUNCATE yandex_pay.tasks CASCADE;')
        tasks = [
            Task(task_type=TaskType.RUN_ACTION, state=TaskState.FAILED, updated=datetime.utcnow() - timedelta(days=365))
            for _ in range(count)
        ]
        for task in tasks:
            await storage.task.create(task)

        return count

    @pytest.fixture(autouse=True)
    def set_clean_tasks_batch_size_to_one(self, mocker):
        mocker.patch('billing.yandex_pay.yandex_pay.taskq.cleanup.clean_old_tasks_batch_size', 1)

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('prepare_tasks')
    async def test_should_clean_old_tasks(self, worker_app, dummy_logger, storage):
        worker = CleanupWorker(logger=dummy_logger)
        worker.app = worker_app

        await worker.process_task()

        tasks = await alist(storage.task.find())
        assert len(tasks) == 0

    @pytest.mark.asyncio
    async def test_should_process_concurrently_without_exceptions(
        self, worker_app, dummy_logger, storage, mocker, loop, prepare_tasks: int
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


class TestWorkersCleanup:
    @pytest.fixture
    async def prepare_workers(self, storage) -> int:
        count = 3
        await storage.conn.execute('TRUNCATE yandex_pay.workers CASCADE;')
        workers = [
            Worker(
                worker_id=str(uuid.uuid4()),
                worker_type=WorkerType.CLEANUP_WORKER,
                host='',
                state=WorkerState.CLEANEDUP,
                heartbeat=datetime.utcnow() - timedelta(days=365),
            )
            for _ in range(count)
        ]
        for worker in workers:
            await storage.worker.create(worker)

        return count

    @pytest.fixture(autouse=True)
    def set_clean_workers_batch_size_to_one(self, mocker):
        mocker.patch('billing.yandex_pay.yandex_pay.taskq.cleanup.clean_old_workers_batch_size', 1)

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('prepare_workers')
    async def test_should_clean_old_workers(self, worker_app, dummy_logger, storage):
        worker = CleanupWorker(logger=dummy_logger)
        worker.app = worker_app

        await worker.process_task()

        tasks = await alist(storage.worker.find(states=list(WorkerState)))
        assert len(tasks) == 0


class TestSyncWorkersCleanup:
    @pytest.fixture
    async def prepare_sync_workers(self, storage) -> int:
        count = 3
        await storage.conn.execute('TRUNCATE yandex_pay.sync_user_cards_workers CASCADE;')
        workers = [
            SyncUserCardsWorker(
                worker_id=uuid.uuid4(),
                host='',
                state=WorkerState.CLEANEDUP,
                heartbeat=datetime.utcnow() - timedelta(days=365),
            )
            for _ in range(count)
        ]
        for worker in workers:
            await storage.sync_user_cards_worker.create(worker)

        return count

    @pytest.fixture(autouse=True)
    def set_clean_sync_workers_batch_size_to_one(self, mocker):
        mocker.patch('billing.yandex_pay.yandex_pay.taskq.cleanup.clean_old_sync_workers_batch_size', 1)

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('prepare_sync_workers')
    async def test_should_clean_old_sync_workers(self, worker_app, dummy_logger, storage):
        worker = CleanupWorker(logger=dummy_logger)
        worker.app = worker_app

        await worker.process_task()

        tasks = await alist(storage.sync_user_cards_worker.find(
            state=list(WorkerState), beat_before=datetime.utcnow() + timedelta(hours=1))
        )
        assert len(tasks) == 0
