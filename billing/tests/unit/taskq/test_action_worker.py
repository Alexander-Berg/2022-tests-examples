import logging
from datetime import timedelta

import pytest

from sendr_taskqueue.worker.storage.action import AsyncDBActionMeta
from sendr_taskqueue.worker.storage.db.entities import TaskState, Worker, WorkerState
from sendr_utils import utcnow

from hamcrest import assert_that, contains, equal_to, has_item, has_items, has_properties, is_, match_equality, none
from hamcrest.library.number.ordering_comparison import greater_than

from billing.yandex_pay.yandex_pay.core.actions.base import BaseAction, BaseAsyncDBAction
from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_enrollment import UpdateEnrollmentAction
from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_metadata import UpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.create_order import YandexPayPlusCreateOrderAction
from billing.yandex_pay.yandex_pay.core.actions.visa_metadata import VisaUpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.entities.enums import TaskType, WorkerType
from billing.yandex_pay.yandex_pay.core.entities.task import Task
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreCardRemovedError, CoreEnrollmentNotFoundError, CoreInvalidCurrencyError, CoreNotFoundError,
    CoreOrderCreationError
)
from billing.yandex_pay.yandex_pay.taskq.action import ActionWorker
from billing.yandex_pay.yandex_pay.utils.stats import worker_tasks_count, worker_tasks_duration


@pytest.fixture
async def task(storage):
    return await storage.task.create(
        Task(
            action_name='fake',
            task_type=TaskType.RUN_ACTION,
            retries=0,
            params={'max_retries': 5},
            run_at=utcnow() - timedelta(minutes=10),
        )
    )


@pytest.fixture
def fake_action_cls(mocker):
    mocker.patch.object(AsyncDBActionMeta, '_actions', {})

    class FakeAction(BaseAsyncDBAction):
        action_name = 'fake'

        async def handle(self):
            return None

    return FakeAction


class TestShouldRetryException:
    @pytest.mark.parametrize(
        'exception,action_cls,expected',
        [
            (CoreEnrollmentNotFoundError(), UpdateEnrollmentAction, True),
            (CoreCardRemovedError(), VisaUpdateEnrollmentMetadataAction, False),
            (CoreInvalidCurrencyError(), YandexPayPlusCreateOrderAction, False),
            (CoreOrderCreationError(), YandexPayPlusCreateOrderAction, True),
        ],
    )
    @pytest.mark.asyncio
    async def test_should_retry_exception_defined_in_action_class(
        self, exception, action_cls, expected, mocker
    ):
        worker = ActionWorker()
        spy = mocker.spy(action_cls, 'should_retry_exception')

        assert_that(worker.should_retry_exception(action_cls, exception), is_(expected))
        spy.assert_called_once_with(exception)
        assert_that(spy.spy_return, is_(expected))

    @pytest.mark.parametrize(
        'action_cls',
        [
            UpdateEnrollmentMetadataAction,
            UpdateEnrollmentAction,
            VisaUpdateEnrollmentMetadataAction,
        ],
    )
    @pytest.mark.parametrize('retry_exceptions', [True, False])
    @pytest.mark.asyncio
    async def test_should_retry_exception_falls_back_to_worker_logic(
        self, action_cls, retry_exceptions, mocker
    ):
        worker = ActionWorker()
        worker.retry_exceptions = retry_exceptions
        exception = Exception()
        spy = mocker.spy(action_cls, 'should_retry_exception')

        assert_that(
            worker.should_retry_exception(action_cls, exception), is_(retry_exceptions)
        )
        spy.assert_called_once_with(exception)
        assert_that(spy.spy_return, none())

    @pytest.mark.parametrize(
        'exception,expected',
        [
            (CoreCardRemovedError(), False),
            (CoreEnrollmentNotFoundError(), True),
            (ValueError(), True),
            (TypeError(), True),
            (RuntimeError(), False),
        ],
    )
    @pytest.mark.asyncio
    async def test_action_worker_retry_exceptions_set_to_tuple(self, exception, expected):
        worker = ActionWorker()
        worker.retry_exceptions = (
            (ValueError, True),
            TypeError,
            (CoreCardRemovedError, False),
            (CoreNotFoundError, True),
        )

        assert_that(worker.should_retry_exception(BaseAction, exception), is_(expected))


class TestTaskRetry:
    @pytest.mark.asyncio
    async def test_task_is_retried(
        self, dummy_logger, storage, task, mocker, fake_action_cls, caplog
    ):
        caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

        mocker.patch.object(fake_action_cls, 'should_retry_exception', return_value=True)
        mocker.patch.object(ActionWorker, 'actions', (fake_action_cls,))
        worker = ActionWorker()
        exception = Exception()

        await storage.worker.create(
            Worker(
                worker_id=worker.worker_id,
                worker_type=WorkerType.RUN_ACTION,
                host='test',
                state=WorkerState.RUNNING,
            )
        )
        storage_ctx = mocker.MagicMock()
        storage_ctx.return_value.__aenter__.return_value = storage
        mocker.patch.multiple(
            worker,
            logger=dummy_logger,
            storage_context=storage_ctx,
            process_action=mocker.AsyncMock(side_effect=exception),
        )

        await worker.process_task()

        fake_action_cls.should_retry_exception.assert_called_once_with(exception)
        task = await storage.task.get(task.task_id)
        assert_that(
            task,
            has_properties(
                retries=1,
                state=TaskState.PENDING
            )
        )

        logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='Failed to run action for task',
                    levelno=logging.ERROR,
                ),
                has_properties(
                    message='Will retry task',
                    levelno=logging.INFO,
                ),
            ),
        )

    @pytest.mark.asyncio
    async def test_task_is_done(self, storage, fake_action_cls, mocker, dummy_logger, task):
        mocker.patch.object(fake_action_cls, 'should_retry_exception', return_value=False)
        mocker.patch.object(fake_action_cls, 'report_task_failure')
        mocker.patch.object(ActionWorker, 'actions', (fake_action_cls,))

        worker = ActionWorker()

        await storage.worker.create(
            Worker(
                worker_id=worker.worker_id,
                worker_type=WorkerType.RUN_ACTION,
                host='test',
                state=WorkerState.RUNNING,
            )
        )
        storage_ctx = mocker.MagicMock()
        storage_ctx.return_value.__aenter__.return_value = storage
        mocker.patch.multiple(
            worker,
            logger=dummy_logger,
            storage_context=storage_ctx,
        )

        await worker.process_task()

        assert_that(
            list(worker_tasks_duration.get()),
            has_item(
                contains(
                    'worker_tasks_duration_fake_done_sum_summ',
                    greater_than(0)
                )
            ),
        )

        assert_that(
            list(worker_tasks_count.get()),
            has_item(
                equal_to(('worker_tasks_count_fake_done_summ', 1.0)),
            ),
        )

    @pytest.mark.asyncio
    async def test_task_is_failed(
        self, dummy_logger, storage, task, fake_action_cls, mocker, caplog
    ):
        caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)
        exception = Exception()

        mocker.patch.object(fake_action_cls, 'should_retry_exception', return_value=False)
        mocker.patch.object(fake_action_cls, 'run', side_effect=exception)
        mock_report = mocker.patch.object(fake_action_cls, 'report_task_failure')
        mocker.patch.object(ActionWorker, 'actions', (fake_action_cls,))

        worker = ActionWorker()
        await storage.worker.create(
            Worker(
                worker_id=worker.worker_id,
                worker_type=WorkerType.RUN_ACTION,
                host='test',
                state=WorkerState.RUNNING,
            )
        )
        storage_ctx = mocker.MagicMock()
        storage_ctx.return_value.__aenter__.return_value = storage
        mocker.patch.multiple(
            worker,
            logger=dummy_logger,
            storage_context=storage_ctx,
        )

        await worker.process_task()

        assert_that(
            list(worker_tasks_duration.get()),
            has_item(
                contains(
                    'worker_tasks_duration_fake_fail_sum_summ',
                    greater_than(0)
                )
            ),
        )

        assert_that(
            worker_tasks_count.get(),
            has_item(
                equal_to(('worker_tasks_count_fake_fail_summ', 1.0))
            ),
        )

        fake_action_cls.should_retry_exception.assert_called_once_with(exception)
        task = await storage.task.get(task.task_id)
        assert_that(
            task,
            has_properties(
                retries=0,
                state=TaskState.FAILED,
            ),
        )

        mock_report.assert_awaited_once_with(
            task=match_equality(
                has_properties(task_id=task.task_id, state=TaskState.FAILED)
            ),
            reason='Action failed because of exception Exception',
        )

        logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='Failed to run action for task',
                    levelno=logging.ERROR,
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_report_task_exhausted_attempts(
        self, storage, task, fake_action_cls, mocker, dummy_logger
    ):
        mocker.patch.object(fake_action_cls, 'should_retry_exception', return_value=False)
        mock_report = mocker.patch.object(fake_action_cls, 'report_task_failure')
        mocker.patch.object(ActionWorker, 'actions', (fake_action_cls,))

        worker = ActionWorker()
        exception = Exception()
        await storage.worker.create(
            Worker(
                worker_id=worker.worker_id,
                worker_type=WorkerType.RUN_ACTION,
                host='test',
                state=WorkerState.RUNNING,
            )
        )
        task.retries = task.params['max_retries']
        await storage.task.save(task)

        storage_ctx = mocker.MagicMock()
        storage_ctx.return_value.__aenter__.return_value = storage
        mocker.patch.multiple(
            worker,
            logger=dummy_logger,
            storage_context=storage_ctx,
            process_action=mocker.AsyncMock(side_effect=exception),
        )

        await worker.process_task()

        assert_that(
            worker_tasks_count.get(),
            has_item(
                equal_to(('worker_tasks_count_fake_retries_exhausted_summ', 1.0)),
            ),
        )

        mock_report.assert_awaited_once_with(
            task=match_equality(
                has_properties(task_id=task.task_id, state=TaskState.FAILED)
            ),
            reason='Max retries exceeded',
        )


@pytest.mark.asyncio
async def test_get_params_calls_deserialize_kwargs_action_method(
    fake_action_cls, task, storage, mocker
):
    spy = mocker.spy(fake_action_cls, 'deserialize_kwargs')
    mocker.patch.object(ActionWorker, 'actions', (fake_action_cls,))
    worker = ActionWorker()

    action_kwargs = {'foo': 'bar', 'fake': True}
    task.params['action_kwargs'] = action_kwargs
    await storage.task.save(task)

    kwargs = worker.get_params(task)
    assert_that(kwargs, equal_to(action_kwargs))

    spy.assert_called_once_with(action_kwargs)
