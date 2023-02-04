import uuid
from datetime import timedelta

import pytest

from sendr_taskqueue.worker.storage import WorkerState

import hamcrest as h

from billing.yandex_pay.yandex_pay.core.entities.sync_user_cards_task import SyncUserCardsTask
from billing.yandex_pay.yandex_pay.core.entities.sync_user_cards_worker import SyncUserCardsWorker
from billing.yandex_pay.yandex_pay.taskq.sync_queue.arbiter import SyncQueueArbiterWorker
from billing.yandex_pay.yandex_pay.tests.utils import helper_get_db_now


@pytest.fixture
def arbiter(worker_app, dummy_logger):
    arbiter = SyncQueueArbiterWorker(logger=dummy_logger, workers=())
    arbiter.app = worker_app
    return arbiter


@pytest.fixture
async def create_worker(storage):
    async def create(**fields):
        worker = SyncUserCardsWorker(**fields)
        return await storage.sync_user_cards_worker.create(worker)

    return create


@pytest.mark.parametrize('worker_state', [WorkerState.RUNNING, WorkerState.SHUTDOWN])
@pytest.mark.asyncio
async def test_arbiter_cleans_up_worker_and_task(
    storage,
    arbiter,
    create_worker,
    worker_app,
    session_uid,
    worker_state,
):
    uid = session_uid
    await storage.sync_user_cards_task.create(
        SyncUserCardsTask(
            uid=uid,
            event_count=0,
            total_event_count=888,
            taken_event_count=777,
        )
    )

    worker_id = uuid.uuid4().hex
    await create_worker(
        uid=uid,
        worker_id=worker_id,
        host='localhost',
        state=WorkerState.RUNNING,
        heartbeat=await helper_get_db_now(storage) - timedelta(hours=5),
    )

    await arbiter.clean_tasks(app=worker_app)

    worker = await storage.sync_user_cards_worker.get(worker_id)

    h.assert_that(
        worker,
        h.has_properties(
            dict(
                uid=None,
                state=WorkerState.CLEANEDUP,
            )
        )
    )

    task = await storage.sync_user_cards_task.get(uid)
    h.assert_that(
        task,
        h.has_properties(
            dict(
                event_count=777,
                total_event_count=888,
                taken_event_count=0,
            )
        )
    )


@pytest.mark.parametrize('worker_state', [WorkerState.RUNNING, WorkerState.SHUTDOWN])
@pytest.mark.asyncio
async def test_arbiter_cleans_up_worker(storage, arbiter, create_worker, worker_app, worker_state):
    worker_id = uuid.uuid4().hex
    await create_worker(
        uid=None,
        worker_id=worker_id,
        host='localhost',
        state=WorkerState.RUNNING,
        heartbeat=await helper_get_db_now(storage) - timedelta(hours=5),
    )

    await arbiter.clean_tasks(app=worker_app)

    worker = await storage.sync_user_cards_worker.get(worker_id)

    h.assert_that(
        worker,
        h.has_properties(
            dict(
                uid=None,
                state=WorkerState.CLEANEDUP,
            )
        )
    )
