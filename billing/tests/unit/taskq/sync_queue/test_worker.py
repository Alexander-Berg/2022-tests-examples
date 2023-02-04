from datetime import timedelta
from unittest.mock import Mock

import pytest

import sendr_utils
from sendr_interactions.exceptions import InteractionResponseError
from sendr_taskqueue.worker.storage import WorkerState
from sendr_taskqueue.worker.storage.db.entities import TaskState

import hamcrest as h

from billing.yandex_pay.yandex_pay.core.actions.sync_user_cards_from_trust import SyncUserCardsFromTrustAction
from billing.yandex_pay.yandex_pay.core.actions.tokenization.mastercard import MastercardTokenizationAction
from billing.yandex_pay.yandex_pay.core.actions.tokenization.sync_user_tokens import SyncUserTokensAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enums import SyncUserCardsTaskType, TSPType
from billing.yandex_pay.yandex_pay.core.entities.sync_user_cards_task import SyncUserCardsTask
from billing.yandex_pay.yandex_pay.core.exceptions import CoreSyncTokensError
from billing.yandex_pay.yandex_pay.taskq.sync_queue.worker import (
    BaseSyncQueueWorker, BulkSyncQueueWorker, LiveSyncQueueWorker
)
from billing.yandex_pay.yandex_pay.tests.utils import helper_get_db_now


@pytest.fixture(params=(SyncUserCardsTaskType.LIVE, SyncUserCardsTaskType.BULK))
def task_type(request):
    return request.param


@pytest.fixture
async def worker(dummy_logger, worker_app, task_type):
    if task_type == SyncUserCardsTaskType.LIVE:
        worker = LiveSyncQueueWorker(logger=dummy_logger)
    else:
        worker = BulkSyncQueueWorker(logger=dummy_logger)
    worker.app = worker_app
    await worker.register_worker(worker_app)
    return worker


@pytest.fixture
def card(mocker):
    card = mocker.MagicMock(spec=Card)
    card.enrollment = None
    card.is_inactive.return_value = False
    card.issuer_bank = 'test'
    card.tsp = TSPType.MASTERCARD

    yield card


@pytest.fixture
def mock_mastercard_tokenization(mocker):
    yield mocker.patch.object(
        MastercardTokenizationAction,
        'run',
    )


@pytest.fixture
def mock_sync_user_cards(mocker):
    yield mocker.patch.object(
        SyncUserCardsFromTrustAction,
        'run',
        return_value=mocker.AsyncMock()
    )


@pytest.mark.asyncio
async def test_get_task(worker, storage, session_uid, task_type):
    uid = session_uid
    await storage.sync_user_cards_task.create(
        SyncUserCardsTask(
            uid=uid,
            event_count=1,
            total_event_count=1,
            taken_event_count=0,
            run_at=await helper_get_db_now(storage) - timedelta(minutes=1),
            task_type=task_type,
        )
    )

    task_fetched = await worker.get_task(storage)

    h.assert_that(
        task_fetched,
        h.has_properties(
            dict(
                event_count=0,
                total_event_count=1,
                taken_event_count=1,
            )
        )
    )

    task_fetched_from_db = await storage.sync_user_cards_task.get(uid)
    h.assert_that(
        task_fetched_from_db,
        h.has_properties(
            dict(
                event_count=0,
                total_event_count=1,
                taken_event_count=1,
            )
        )
    )

    worker_entity_fetched = await storage.sync_user_cards_worker.get(worker.worker_id)
    h.assert_that(
        worker_entity_fetched,
        h.has_properties(
            dict(
                uid=uid,
                state=WorkerState.RUNNING,
            )
        )
    )


@pytest.mark.asyncio
async def test_process_task_normally(worker, storage, mocker, session_uid, task_type):
    uid = session_uid
    await storage.sync_user_cards_task.create(
        SyncUserCardsTask(
            uid=uid,
            event_count=1,
            total_event_count=1,
            taken_event_count=0,
            run_at=await helper_get_db_now(storage) - timedelta(minutes=1),
            task_type=task_type,
        )
    )

    execute_task_mock: Mock = mocker.patch.object(worker, 'execute_task')

    await worker.process_task()

    execute_task_mock.assert_called_once()

    task_processed = await storage.sync_user_cards_task.get(uid)
    h.assert_that(
        task_processed,
        h.has_properties(
            dict(
                event_count=0,
                total_event_count=1,
                taken_event_count=0,
            )
        )
    )

    worker_entity_after_task_is_done = await storage.sync_user_cards_worker.get(worker.worker_id)
    h.assert_that(
        worker_entity_after_task_is_done,
        h.has_properties(
            dict(
                uid=None,
                state=WorkerState.RUNNING,
            )
        )
    )


@pytest.mark.asyncio
async def test_process_task_retry(worker, storage, mocker, session_uid, task_type):
    uid = session_uid
    await storage.sync_user_cards_task.create(
        SyncUserCardsTask(
            uid=uid,
            event_count=1,
            total_event_count=1,
            taken_event_count=0,
            run_at=await helper_get_db_now(storage) - timedelta(minutes=1),
            task_type=task_type,
        )
    )

    execute_task_mock: Mock = mocker.patch.object(worker, 'execute_task', side_effect=RuntimeError)

    await worker.process_task()

    execute_task_mock.assert_called_once()

    task_processed = await storage.sync_user_cards_task.get(uid)
    h.assert_that(
        task_processed,
        h.has_properties(
            dict(
                event_count=1,
                total_event_count=1,
                taken_event_count=0,
                tries=1,
            )
        )
    )

    worker_entity_after_task_is_done = await storage.sync_user_cards_worker.get(worker.worker_id)
    h.assert_that(
        worker_entity_after_task_is_done,
        h.has_properties(
            dict(
                uid=None,
                state=WorkerState.RUNNING,
            )
        )
    )


@pytest.mark.parametrize(
    "tries,exception,delay_expected",
    [
        (0, Exception(), BaseSyncQueueWorker.retry_initial_delay),
        (1, Exception(), BaseSyncQueueWorker.retry_initial_delay * BaseSyncQueueWorker.retry_delay_multiplier),
        (10, Exception(), BaseSyncQueueWorker.retry_max_delay),
        (0, CoreSyncTokensError(), BaseSyncQueueWorker.retry_initial_delay),
        (
            1,
            CoreSyncTokensError(),
            BaseSyncQueueWorker.retry_initial_delay * BaseSyncQueueWorker.retry_delay_multiplier
        ),
        (1, CoreSyncTokensError(status_codes=[500]),
         BaseSyncQueueWorker.retry_initial_delay * BaseSyncQueueWorker.retry_delay_multiplier),
        (1, CoreSyncTokensError(status_codes=[500, 400]),
         BaseSyncQueueWorker.retry_initial_delay * BaseSyncQueueWorker.retry_delay_multiplier),
        (1, CoreSyncTokensError(status_codes=[400]), BaseSyncQueueWorker.retry_long_delay),
        (0, CoreSyncTokensError(status_codes=[404, 500]), BaseSyncQueueWorker.retry_initial_delay),
        (0, CoreSyncTokensError(status_codes=[400, 500]), BaseSyncQueueWorker.retry_initial_delay),
        (0, CoreSyncTokensError(status_codes=[400, 400]), BaseSyncQueueWorker.retry_long_delay),
        (0, CoreSyncTokensError(status_codes=[422]), None),
        (0, CoreSyncTokensError(status_codes=[422, 400]), BaseSyncQueueWorker.retry_long_delay),
        (0, CoreSyncTokensError(status_codes=[422, 500]), BaseSyncQueueWorker.retry_initial_delay),

    ]
)
@pytest.mark.asyncio
async def test_retry_delay(worker, tries, exception, delay_expected):
    assert worker.get_task_retry_delay_sec(tries, exception) == delay_expected


@pytest.mark.parametrize('status_code,delay', [
    (400, BaseSyncQueueWorker.retry_long_delay),
    (500, BaseSyncQueueWorker.retry_initial_delay),
])
@pytest.mark.asyncio
async def test_cardproxy_client_failure_retry(
    mocker, storage, worker, session_uid, card,
    mock_mastercard_tokenization,
    mock_sync_user_cards,
    status_code,
    delay,
    task_type,
):
    uid = session_uid
    now = sendr_utils.utcnow()
    await storage.sync_user_cards_task.create(
        SyncUserCardsTask(
            uid=uid,
            event_count=1,
            total_event_count=1,
            taken_event_count=0,
            run_at=now - timedelta(seconds=1),
            task_type=task_type,
        )
    )
    mocker.patch.object(
        SyncUserTokensAction,
        '_get_user_cards',
        return_value=[card],
    )
    mock_mastercard_tokenization.side_effect = InteractionResponseError(
        status_code=status_code,
        method='test',
        service='test',
    )

    await worker.process_task()

    task_processed = await storage.sync_user_cards_task.get(uid)

    assert task_processed.run_at >= (now + timedelta(seconds=delay - 1))


@pytest.mark.asyncio
async def test_task_retry_exhausted(
    mocker, storage, worker, session_uid, task_type,
):
    now = sendr_utils.utcnow()
    task = await storage.sync_user_cards_task.create(
        SyncUserCardsTask(
            uid=session_uid,
            event_count=1,
            total_event_count=0,
            taken_event_count=0,
            run_at=now - timedelta(seconds=1),
            task_type=task_type,
        )
    )
    mocker.patch.object(worker, 'execute_task', side_effect=RuntimeError)
    mocker.patch.object(worker, 'get_task_retry_delay_sec', return_value=0)

    while not storage.sync_user_cards_task.retries_exhausted(task.tries):
        await worker.process_task()
        task = await storage.sync_user_cards_task.get(session_uid)

    assert task.state == TaskState.FAILED
    assert storage.sync_user_cards_task.retries_exhausted(task.tries)


@pytest.mark.asyncio
async def test_task_failed_when_delay_none(
    mocker, storage, worker, session_uid, task_type,
):
    now = sendr_utils.utcnow()
    task = await storage.sync_user_cards_task.create(
        SyncUserCardsTask(
            uid=session_uid,
            event_count=1,
            total_event_count=0,
            taken_event_count=0,
            run_at=now - timedelta(seconds=1),
            task_type=task_type,
        )
    )
    mocker.patch.object(worker, 'execute_task', side_effect=RuntimeError)
    mocker.patch.object(worker, 'get_task_retry_delay_sec', return_value=None)

    await worker.process_task()
    task = await storage.sync_user_cards_task.get(session_uid)

    assert task.state == TaskState.FAILED
    assert storage.sync_user_cards_task.retries_exhausted(task.tries) is not None
