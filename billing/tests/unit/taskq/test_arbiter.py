from copy import deepcopy
from datetime import datetime, timedelta, timezone
from uuid import uuid4

import pytest

from sendr_taskqueue.worker.storage.db.entities import WorkerState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_properties, has_property

from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enums import TokenizationQueueState, TokenizationWorkerType, TSPType
from billing.yandex_pay.yandex_pay.core.entities.tokenization_queue import TokenizationQueue, TokenizationWorker
from billing.yandex_pay.yandex_pay.taskq.arbiter import TokenizationArbiterWorker


@pytest.fixture
def past_time():
    return datetime(2000, 1, 1, 0, 0, 0, tzinfo=timezone.utc)


@pytest.fixture
def card_to_tokenize_entity():
    return Card(
        trust_card_id='trust-card-id',
        owner_uid=5555,
        tsp=TSPType.VISA,
        expire=utcnow() + timedelta(days=365),
        last4='1234',
    )


@pytest.fixture
async def card_to_tokenize(storage, card_to_tokenize_entity):
    return await storage.card.create(card_to_tokenize_entity)


@pytest.fixture
def create_task(storage, past_time, card_to_tokenize):
    async def _inner(
        state=TokenizationQueueState.PENDING,
        run_at=past_time,
        card_id=card_to_tokenize.card_id,
        **kwargs,
    ):
        return await storage.tokenization_queue.create(
            TokenizationQueue(
                card_id=card_id,
                run_at=run_at,
                state=state,
                **kwargs,
            )
        )

    return _inner


@pytest.fixture
def create_worker(storage, past_time, rands):
    async def _inner(
        worker_type=TokenizationWorkerType.TOKENIZATION_MASTERCARD,
        state=WorkerState.RUNNING,
        heartbeat=past_time,
        **kwargs,
    ):
        return await storage.tokenization_worker.create(
            TokenizationWorker(
                worker_type=worker_type,
                worker_id=uuid4(),
                host=rands(),
                heartbeat=heartbeat,
                state=state,
                **kwargs,
            )
        )

    return _inner


@pytest.fixture
async def worker(create_worker):
    return await create_worker(heartbeat=utcnow() - timedelta(days=1))


@pytest.fixture
async def task(create_task, worker):
    return await create_task(state=TokenizationQueueState.PROCESSING, worker_id=worker.worker_id)


@pytest.fixture
def arbiter(worker_app, dummy_logger):
    arbiter = TokenizationArbiterWorker(logger=dummy_logger, workers=())
    arbiter.app = worker_app
    return arbiter


@pytest.mark.asyncio
async def test_worker_and_task_cleanup(storage, arbiter, worker, task, create_worker):
    assert_that(
        task,
        has_properties({'worker_id': worker.worker_id, 'state': TokenizationQueueState.PROCESSING})
    )

    await arbiter.clean_tasks(arbiter.app)

    worker = await storage.tokenization_worker.get(worker.worker_id)
    assert_that(worker, has_property('state', WorkerState.CLEANEDUP))

    task = await storage.tokenization_queue.get(task.tokenization_queue_id)
    assert_that(
        task,
        has_properties({'worker_id': None, 'state': TokenizationQueueState.PENDING}),
    )


@pytest.mark.asyncio
async def test_cleanup_worker_with_no_tasks(storage, arbiter, create_worker):
    empty_worker = await create_worker(heartbeat=utcnow() - timedelta(days=1))
    await arbiter.clean_tasks(arbiter.app)

    empty_worker = await storage.tokenization_worker.get(empty_worker.worker_id)
    assert_that(empty_worker, has_property('state', WorkerState.CLEANEDUP))
    assert_that(
        await alist(storage.tokenization_queue.find()),
        equal_to([]),
    )


@pytest.mark.asyncio
async def test_task_not_cleanedup_if_not_in_processing_state(
    storage, arbiter, worker, task, create_task, card_to_tokenize_entity
):
    card_to_tokenize_entity = deepcopy(card_to_tokenize_entity)
    card_to_tokenize_entity.card_id = uuid4()
    card_to_tokenize_entity.owner_uid = 6666
    other_card = await storage.card.create(card_to_tokenize_entity)
    other_task_data = dict(
        state=TokenizationQueueState.PENDING_JOB,
        worker_id=worker.worker_id,
        card_id=other_card.card_id,
    )
    other_task = await create_task(**other_task_data)

    await arbiter.clean_tasks(arbiter.app)

    other_task = await storage.tokenization_queue.get(other_task.tokenization_queue_id)
    assert_that(other_task, has_properties(other_task_data))

    task = await storage.tokenization_queue.get(task.tokenization_queue_id)
    assert_that(
        task,
        has_properties({'worker_id': None, 'state': TokenizationQueueState.PENDING}),
    )
