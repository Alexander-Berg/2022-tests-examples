import uuid
from datetime import timedelta

import pytest

from sendr_taskqueue.worker.storage import WorkerState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains_inanyorder

from billing.yandex_pay.yandex_pay.core.entities.sync_user_cards_task import SyncUserCardsTask
from billing.yandex_pay.yandex_pay.core.entities.sync_user_cards_worker import SyncUserCardsWorker


@pytest.mark.asyncio
async def test_creates_worker(storage):
    worker_entity = SyncUserCardsWorker(
        worker_id=uuid.uuid4(),
        host='hello.ru',
        state=WorkerState.RUNNING,
        heartbeat=utcnow(),
        uid=None,
    )

    created_worker = await storage.sync_user_cards_worker.create(worker_entity)

    worker_entity.startup = created_worker.startup

    assert worker_entity == created_worker


@pytest.mark.asyncio
async def test_saves_worker(storage):
    worker_entity = SyncUserCardsWorker(
        worker_id=uuid.uuid4().hex,
        host='hello.ru',
        state=WorkerState.RUNNING,
        heartbeat=utcnow(),
        uid=None,
    )

    created_worker = await storage.sync_user_cards_worker.create(worker_entity)

    created_worker.state = WorkerState.CLEANEDUP
    saved_worker: SyncUserCardsWorker = await storage.sync_user_cards_worker.save(created_worker)

    assert saved_worker.state == WorkerState.CLEANEDUP


@pytest.mark.asyncio
async def test_finds(storage):
    worker_entity = SyncUserCardsWorker(
        worker_id=uuid.uuid4(),
        host='hello.ru',
        state=WorkerState.RUNNING,
        heartbeat=utcnow(),
        uid=None,
    )

    created_worker = await storage.sync_user_cards_worker.create(worker_entity)

    found_workers = await alist(storage.sync_user_cards_worker.find(
        state=WorkerState.RUNNING,
        beat_after=created_worker.heartbeat,
    ))

    assert [created_worker] == found_workers


@pytest.mark.asyncio
async def test_can_delete_workers(storage):
    await storage.sync_user_cards_task.create(
        SyncUserCardsTask(
            uid=1,
            event_count=1,
            total_event_count=1,
            taken_event_count=0,
        )
    )
    workers = (
        SyncUserCardsWorker(
            worker_id=uuid.uuid4(),
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow() - timedelta(days=365)
        ),
        SyncUserCardsWorker(
            worker_id=uuid.uuid4(),
            host='',
            state=WorkerState.RUNNING,
            heartbeat=utcnow() - timedelta(days=365)
        ),
        SyncUserCardsWorker(
            worker_id=uuid.uuid4(),
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow() - timedelta(days=365),
            uid=1
        ),
        SyncUserCardsWorker(
            worker_id=uuid.uuid4(),
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow()
        ),
    )
    created_workers = []
    for worker in workers:
        created_workers.append(await storage.sync_user_cards_worker.create(worker))

    deleted_count = await storage.sync_user_cards_worker.delete_cleaned_up_workers(
        batch_size=10, period_offset=timedelta(days=1)
    )

    expected_left_workers = [
        created_workers[1],
        created_workers[2],
        created_workers[3],
    ]
    workers_from_base = await alist(
        storage.sync_user_cards_worker.find(state=list(WorkerState), beat_before=utcnow() + timedelta(hours=1))
    )
    assert deleted_count == 1
    assert_that(workers_from_base, contains_inanyorder(*expected_left_workers))

    zero_deleted_count = await storage.sync_user_cards_worker.delete_cleaned_up_workers(
        batch_size=10, period_offset=timedelta(days=1)
    )
    assert zero_deleted_count == 0


@pytest.mark.asyncio
async def test_delete_is_limited_by_batch_size(storage):
    workers = (
        SyncUserCardsWorker(
            worker_id=uuid.uuid4(),
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow() - timedelta(days=365)
        ),
        SyncUserCardsWorker(
            worker_id=uuid.uuid4(),
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow() - timedelta(days=365)
        ),
    )
    for workers in workers:
        await storage.sync_user_cards_worker.create(workers)

    deleted_count = await storage.sync_user_cards_worker.delete_cleaned_up_workers(
        batch_size=1,
        period_offset=timedelta(days=1)
    )
    assert deleted_count == 1

    deleted_count = await storage.sync_user_cards_worker.delete_cleaned_up_workers(
        batch_size=1,
        period_offset=timedelta(days=1)
    )
    assert deleted_count == 1

    deleted_count = await storage.sync_user_cards_worker.delete_cleaned_up_workers(
        batch_size=1,
        period_offset=timedelta(days=1)
    )
    assert deleted_count == 0
