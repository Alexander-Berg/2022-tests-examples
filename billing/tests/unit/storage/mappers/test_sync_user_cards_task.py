import asyncio
import datetime

import pytest

from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import utcnow

import hamcrest as h

from billing.yandex_pay.yandex_pay.core.entities.enums import SyncUserCardsTaskType
from billing.yandex_pay.yandex_pay.core.entities.sync_user_cards_task import SyncUserCardsTask
from billing.yandex_pay.yandex_pay.storage import StorageContext
from billing.yandex_pay.yandex_pay.storage.db.tables import sync_user_cards_task as t_sync_user_cards_task
from billing.yandex_pay.yandex_pay.tests.utils import helper_get_db_now


async def helper_truncate_tasks(storage):
    await storage.conn.execute(f'truncate table {t_sync_user_cards_task.fullname} cascade;')


@pytest.fixture(name='db_engine')
def fixture_db_engine(raw_db_engine):
    return raw_db_engine


@pytest.fixture(name='storage_context')
async def fixture_storage_context(raw_db_engine, dummy_logger):
    def sctx():
        return StorageContext(db_engine=raw_db_engine, logger=dummy_logger)

    return sctx


@pytest.fixture(name='storage')
async def fixture_storage(storage_context):
    async with storage_context() as storage:
        yield storage
        # TODO убрать truncate, каждый тест работает в отдельной транзакции
        await helper_truncate_tasks(storage)


@pytest.mark.asyncio
async def test_register_event(storage, session_uid):
    task = await storage.sync_user_cards_task.register_event(
        uid=session_uid, task_type=SyncUserCardsTaskType.LIVE
    )

    db_now = await helper_get_db_now(storage)

    h.assert_that(
        task,
        h.has_properties(
            uid=session_uid,
            event_count=1,
            total_event_count=1,
            taken_event_count=0,
            tries=0,
            run_at=h.less_than(db_now),
            task_type=SyncUserCardsTaskType.LIVE,
            state=TaskState.PENDING,
        )
    )

    updated_task = await storage.sync_user_cards_task.register_event(
        uid=task.uid, task_type=SyncUserCardsTaskType.LIVE
    )
    h.assert_that(
        updated_task,
        h.has_properties(
            uid=task.uid,
            event_count=2,
            total_event_count=2,
            taken_event_count=0,
            tries=0,
            task_type=SyncUserCardsTaskType.LIVE,
            state=TaskState.PENDING,
        )
    )


@pytest.mark.asyncio
async def test_concurrent_register_event(storage_context, session_uid):
    """
    Оптимистичный конкурентный тест: просто запустим 10 корутин и проверим, что итоговый счетчик тоже равен 10.
    """

    async def concurrent_task():
        async with storage_context() as storage:
            return await storage.sync_user_cards_task.register_event(
                uid=session_uid, task_type=SyncUserCardsTaskType.LIVE
            )

    concurrent_tasks = [asyncio.create_task(concurrent_task()) for _ in range(10)]
    await asyncio.gather(*concurrent_tasks)

    async with storage_context() as storage:
        task: SyncUserCardsTask = await storage.sync_user_cards_task.get(session_uid)

    assert task.event_count == 10
    assert task.total_event_count == 10


@pytest.mark.asyncio
async def test_should_change_type_if_register_live_for_exist_bulk_event(storage, session_uid):
    task = await storage.sync_user_cards_task.register_event(
        uid=session_uid, task_type=SyncUserCardsTaskType.BULK
    )

    h.assert_that(
        task,
        h.has_properties(
            task_type=SyncUserCardsTaskType.BULK,
        )
    )

    updated_task = await storage.sync_user_cards_task.register_event(
        uid=task.uid, task_type=SyncUserCardsTaskType.LIVE
    )
    h.assert_that(
        updated_task,
        h.has_properties(
            task_type=SyncUserCardsTaskType.LIVE,
        )
    )


@pytest.mark.asyncio
async def test_should_not_add_event_and_change_type_if_register_bulk_for_exist_live_event(storage, session_uid):
    task = await storage.sync_user_cards_task.register_event(
        uid=session_uid, task_type=SyncUserCardsTaskType.LIVE
    )

    h.assert_that(
        task,
        h.has_properties(
            event_count=1,
            task_type=SyncUserCardsTaskType.LIVE,
        )
    )

    updated_task = await storage.sync_user_cards_task.register_event(
        uid=task.uid, task_type=SyncUserCardsTaskType.BULK
    )
    assert updated_task is None

    updated_task = await storage.sync_user_cards_task.get(task.uid)
    h.assert_that(
        updated_task,
        h.has_properties(
            event_count=1,
            task_type=SyncUserCardsTaskType.LIVE,
        )
    )


@pytest.mark.asyncio
async def test_should_add_event_if_register_bulk_for_exist_bulk(storage, session_uid):
    await storage.sync_user_cards_task.register_event(
        uid=session_uid, task_type=SyncUserCardsTaskType.BULK
    )
    task = await storage.sync_user_cards_task.register_event(
        uid=session_uid, task_type=SyncUserCardsTaskType.BULK
    )

    h.assert_that(
        task,
        h.has_properties(
            event_count=2,
            task_type=SyncUserCardsTaskType.BULK,
        )
    )


@pytest.mark.asyncio
async def test_create(storage, session_uid):
    task = SyncUserCardsTask(
        uid=session_uid,
        event_count=1,
        total_event_count=13,
        taken_event_count=0,
    )

    task_created = await storage.sync_user_cards_task.create(task)

    h.assert_that(
        task_created,
        h.has_properties(
            uid=session_uid,
            event_count=1,
            total_event_count=13,
            taken_event_count=0,
            run_at=h.less_than(await helper_get_db_now(storage)),
            state=TaskState.PENDING,
        )
    )


@pytest.mark.asyncio
async def test_can_get_queue_size(storage):
    first_task = SyncUserCardsTask(
        uid=1,
        event_count=1,
        total_event_count=10,
        taken_event_count=0,
        tries=0,
        task_type=SyncUserCardsTaskType.LIVE,
        state=TaskState.PENDING,
    )
    await storage.sync_user_cards_task.create(first_task)
    second_task = SyncUserCardsTask(
        uid=2,
        event_count=0,
        total_event_count=10,
        taken_event_count=0,
        tries=0,
        task_type=SyncUserCardsTaskType.LIVE,
        state=TaskState.FINISHED,
    )
    await storage.sync_user_cards_task.create(second_task)
    third_task = SyncUserCardsTask(
        uid=3,
        event_count=2,
        total_event_count=10,
        taken_event_count=1,
        tries=1,
        task_type=SyncUserCardsTaskType.LIVE,
        state=TaskState.PROCESSING,
    )
    await storage.sync_user_cards_task.create(third_task)
    fourth_task = SyncUserCardsTask(
        uid=4,
        event_count=1,
        total_event_count=10,
        taken_event_count=0,
        tries=1000,
        task_type=SyncUserCardsTaskType.LIVE,
        state=TaskState.FAILED,
    )
    await storage.sync_user_cards_task.create(fourth_task)
    fifth_task = SyncUserCardsTask(
        uid=5,
        event_count=1,
        total_event_count=10,
        taken_event_count=0,
        tries=0,
        task_type=SyncUserCardsTaskType.BULK,
        state=TaskState.PENDING,
    )
    await storage.sync_user_cards_task.create(fifth_task)

    live_queue_size = await storage.sync_user_cards_task.get_queue_size(SyncUserCardsTaskType.LIVE)

    assert live_queue_size == 1

    bulk_queue_size = await storage.sync_user_cards_task.get_queue_size(SyncUserCardsTaskType.BULK)

    assert bulk_queue_size == 1


@pytest.mark.asyncio
async def test_save(storage, session_uid):
    task = SyncUserCardsTask(
        uid=session_uid,
        event_count=1,
        total_event_count=13,
        taken_event_count=0,
    )

    task_created = await storage.sync_user_cards_task.create(task)

    db_now = await helper_get_db_now(storage)

    h.assert_that(
        task_created,
        h.has_properties(
            uid=session_uid,
            event_count=1,
            total_event_count=13,
            taken_event_count=0,
            run_at=h.less_than(db_now)
        ),
        'Sanity check',
    )

    task_created.run_at = db_now + datetime.timedelta(hours=1)
    task_created.event_count = 145
    task_created.total_event_count = 456
    task_created.taken_event_count = 56

    task_saved = await storage.sync_user_cards_task.save(task_created)
    h.assert_that(
        task_saved,
        h.has_properties(
            uid=session_uid,
            event_count=145,
            total_event_count=456,
            taken_event_count=56,
            run_at=db_now + datetime.timedelta(hours=1),
        ),
    )


@pytest.mark.asyncio
async def test_requeue(storage, session_uid):
    task = SyncUserCardsTask(
        uid=session_uid,
        event_count=1,
        total_event_count=13,
        taken_event_count=10,
        tries=0,
        state=TaskState.PROCESSING,
    )

    await storage.sync_user_cards_task.create(task)

    time_before_update = await helper_get_db_now(storage)

    task_updated = await storage.sync_user_cards_task.requeue(
        uid=task.uid,
        is_retry=True,
        run_at_delay_sec=30,
    )

    time_after_update = await helper_get_db_now(storage)

    h.assert_that(
        task_updated,
        h.has_properties(
            event_count=11,
            total_event_count=13,
            run_at=h.all_of(
                h.greater_than(time_before_update + datetime.timedelta(seconds=30)),
                h.less_than(time_after_update + datetime.timedelta(seconds=30))
            ),
            tries=1,
            state=TaskState.PENDING,
        )
    )

    task_updated = await storage.sync_user_cards_task.register_event(
        uid=task.uid, task_type=SyncUserCardsTaskType.LIVE
    )
    time_after_update = await helper_get_db_now(storage)

    assert task_updated.run_at < time_after_update


@pytest.mark.asyncio
async def test_commit_taken_events(storage, session_uid):
    task = SyncUserCardsTask(
        uid=session_uid,
        event_count=1,
        total_event_count=13,
        taken_event_count=10,
        tries=10,
        state=TaskState.PROCESSING,
    )

    await storage.sync_user_cards_task.create(task)

    task_updated = await storage.sync_user_cards_task.commit_taken_events(uid=task.uid)

    h.assert_that(
        task_updated,
        h.has_properties(
            event_count=1,
            taken_event_count=0,
            tries=0,
            state=TaskState.FINISHED,
        )
    )


@pytest.mark.asyncio
async def test_take_events(storage, session_uid):
    task = SyncUserCardsTask(
        uid=session_uid,
        event_count=20,
        total_event_count=20,
        taken_event_count=0,
        state=TaskState.PENDING,
    )

    await storage.sync_user_cards_task.create(task)

    task_updated = await storage.sync_user_cards_task.take_events(uid=task.uid)

    h.assert_that(
        task_updated,
        h.has_properties(
            event_count=0,
            taken_event_count=20,
            total_event_count=20,
            state=TaskState.PROCESSING,
        )
    )


class TestGetForWork:
    @pytest.mark.asyncio
    async def test_taken_events(self, storage, session_uid):
        task = SyncUserCardsTask(
            uid=session_uid,
            event_count=1,
            total_event_count=13,
            taken_event_count=10,
            tries=1,
            state=TaskState.PROCESSING,
        )

        await storage.sync_user_cards_task.create(task)

        with pytest.raises(SyncUserCardsTask.DoesNotExist):
            async with storage.conn.begin():
                await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        await storage.sync_user_cards_task.requeue(session_uid)
        async with storage.conn.begin():
            task_fetched = await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        assert task_fetched.uid == task.uid

    @pytest.mark.asyncio
    async def test_should_filter_by_task_type(self, storage, session_uid):
        task = SyncUserCardsTask(
            uid=session_uid,
            event_count=10,
            total_event_count=15,
            taken_event_count=0,
            tries=0,
            task_type=SyncUserCardsTaskType.BULK,
            state=TaskState.PENDING,
        )

        await storage.sync_user_cards_task.create(task)

        with pytest.raises(SyncUserCardsTask.DoesNotExist):
            async with storage.conn.begin():
                await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        async with storage.conn.begin():
            task_fetched = await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.BULK)

        assert task_fetched.uid == task.uid
        assert task_fetched.state == TaskState.PENDING

    @pytest.mark.asyncio
    async def test_event_count(self, storage, session_uid):
        task = SyncUserCardsTask(
            uid=session_uid,
            event_count=0,
            total_event_count=13,
            taken_event_count=0,
            state=TaskState.FINISHED,
        )
        await storage.sync_user_cards_task.create(task)

        with pytest.raises(SyncUserCardsTask.DoesNotExist):
            async with storage.conn.begin():
                await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        await storage.sync_user_cards_task.register_event(session_uid, SyncUserCardsTaskType.LIVE)
        async with storage.conn.begin():
            task_fetched = await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        assert task_fetched.uid == session_uid

    @pytest.mark.asyncio
    async def test_run_at(self, storage, session_uid):
        task = SyncUserCardsTask(
            uid=session_uid,
            event_count=1,
            total_event_count=13,
            taken_event_count=0,
            run_at=utcnow() + datetime.timedelta(hours=1),
        )
        await storage.sync_user_cards_task.create(task)

        with pytest.raises(SyncUserCardsTask.DoesNotExist):
            async with storage.conn.begin():
                await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        await storage.sync_user_cards_task.requeue(session_uid, run_at_delay_sec=0)
        async with storage.conn.begin():
            task_fetched = await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        assert task_fetched.uid == session_uid

    @pytest.mark.asyncio
    async def test_work_retry_limit(self, storage, session_uid):
        task = SyncUserCardsTask(
            uid=session_uid,
            event_count=1,
            total_event_count=0,
            taken_event_count=0,
            tries=1000,
            run_at=await helper_get_db_now(storage) - datetime.timedelta(hours=1),
            state=TaskState.FAILED
        )
        await storage.sync_user_cards_task.create(task)

        with pytest.raises(SyncUserCardsTask.DoesNotExist):
            async with storage.conn.begin():
                await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        task.state = TaskState.PENDING
        await storage.sync_user_cards_task.save(task)
        async with storage.conn.begin():
            task_fetched = await storage.sync_user_cards_task.get_for_work(SyncUserCardsTaskType.LIVE)

        assert task_fetched.uid == session_uid
