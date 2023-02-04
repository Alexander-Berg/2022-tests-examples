from datetime import timedelta

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.enums import SyncUserCardsTaskType
from billing.yandex_pay.yandex_pay.taskq.action import ActionWorker
from billing.yandex_pay.yandex_pay.taskq.storage_metrics import (
    PspPublicKeyMaxAgeMetricsWorker, SyncUserCardsQueueSizeMetricsWorker, TasksQueueSizeMetricsWorker
)
from billing.yandex_pay.yandex_pay.utils.stats import psp_key_age, sync_cards_queue_size, worker_tasks_queue_size


@pytest.mark.asyncio
async def test_sync_queue_size_metrics_worker(worker_app, dummy_logger, mocker):
    mapper_mocker = mocker.patch(
        'billing.yandex_pay.yandex_pay.storage.mappers.sync_user_cards_task.SyncUserCardsTaskMapper.get_queue_size',
        return_value=3
    )
    worker = SyncUserCardsQueueSizeMetricsWorker(logger=dummy_logger)
    worker.app = worker_app

    await worker.process_task()

    expected_calls = [
        mocker.call(task_type=SyncUserCardsTaskType.LIVE),
        mocker.call(task_type=SyncUserCardsTaskType.BULK),
    ]
    mapper_mocker.assert_has_calls(expected_calls, any_order=True)
    assert_that(
        sync_cards_queue_size.labels(SyncUserCardsTaskType.LIVE.value).get()[0][1],
        equal_to(3)
    )
    assert_that(
        sync_cards_queue_size.labels(SyncUserCardsTaskType.BULK.value).get()[0][1],
        equal_to(3)
    )


@pytest.mark.asyncio
async def test_psp_public_key_max_age_metrics_worker(worker_app, dummy_logger, mocker):
    time_now = utcnow().replace(microsecond=0)
    mapper_mocker = mocker.patch(
        'billing.yandex_pay.yandex_pay.storage.mappers.psp.PSPMapper.get_oldest_public_key_update_time',
        return_value=time_now
    )
    mocker.patch(
        'billing.yandex_pay.yandex_pay.taskq.storage_metrics.utcnow',
        return_value=time_now + timedelta(days=5)
    )

    worker = PspPublicKeyMaxAgeMetricsWorker(logger=dummy_logger)
    worker.app = worker_app

    await worker.process_task()

    mapper_mocker.assert_called_once()
    assert_that(
        psp_key_age.get()[0][1],
        equal_to(5)
    )


@pytest.mark.asyncio
async def test_task_queue_size_metrics_worker(worker_app, dummy_logger, mocker):
    mapper_mocker = mocker.patch(
        'billing.yandex_pay.yandex_pay.storage.mappers.task.TaskMapper.get_pending_tasks_count',
        return_value={'a1': 3, 'a2': 1}
    )
    all_actions = ['a1', 'a2'] + [a.action_name for a in ActionWorker.actions]

    worker = TasksQueueSizeMetricsWorker(logger=dummy_logger)
    worker.app = worker_app

    await worker.process_task()

    mapper_mocker.assert_called_once()
    assert_that(
        [worker_tasks_queue_size.labels(label).get()[0][1] for label in all_actions],
        equal_to([3, 1] + [0] * len(ActionWorker.actions))
    )
