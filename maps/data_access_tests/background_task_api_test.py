import pytest

from maps.infra.sedem.machine.lib.background_task_api import (
    TasksApi,
    TaskActionType,
    TaskStatus,
    TaskDocument
)
from maps.infra.sedem.machine.tests.typing import (
    MongoFixture,
    TaskFactory
)


@pytest.mark.asyncio
async def test_lookup_enqueued_tasks_ids(mongo: MongoFixture,
                                         task_factory: TaskFactory) -> None:

    for action in (TaskActionType.COMMENT_ST_TICKET, TaskActionType.COPY_ST_TICKET):
        await task_factory(
            action_type=action,
            payload={'release_id': '123'}
        )

    expected_tasks_ids = []
    for status in (TaskStatus.DONE, TaskStatus.ENQUEUED, TaskStatus.ENQUEUED):
        task_id = await task_factory(
            action_type=TaskActionType.CREATE_ST_TICKET,
            payload={'release_id': '123'},
            status=status
        )
        if status != TaskStatus.DONE:
            expected_tasks_ids.append(task_id)

    async with await mongo.async_client.start_session() as session:
        ids_cursor = TasksApi(session).lookup_enqueued_tasks_ids(action_type=TaskActionType.CREATE_ST_TICKET)
        task_ids = [task_id async for task_id in ids_cursor]
    assert len(task_ids) == 2
    assert sorted(expected_tasks_ids) == sorted(task_ids)


@pytest.mark.asyncio
async def test_load_task_by_id(mongo: MongoFixture,
                               task_factory: TaskFactory) -> None:
    await task_factory(
        action_type=TaskActionType.COMMENT_ST_TICKET,
        payload={'release_id': '123'},
        status=TaskStatus.DONE
    )

    task_id = await task_factory(
        action_type=TaskActionType.COMMENT_ST_TICKET,
        payload={'release_id': '123'}
    )

    await task_factory(
        action_type=TaskActionType.COPY_ST_TICKET,
        payload={'release_id': '456'}
    )

    async with await mongo.async_client.start_session() as session:
        task = await TasksApi(session).load_task(task_id)

    assert task == TaskDocument(
        payload={'release_id': '123'},
        action_type=TaskActionType.COMMENT_ST_TICKET,
        status=TaskStatus.ENQUEUED
    )


@pytest.mark.asyncio
async def test_create_task(mongo: MongoFixture) -> None:
    async with await mongo.async_client.start_session() as session:
        task_id = await TasksApi(session).create_task(
            action_type=TaskActionType.COMMENT_ST_TICKET,
            payload={'text': '123'}
        )

    async with await mongo.async_client.start_session() as session:
        task = await TasksApi(session).load_task(task_id)

    assert task == TaskDocument(
        payload={'text': '123'},
        action_type=TaskActionType.COMMENT_ST_TICKET,
        status=TaskStatus.ENQUEUED)


@pytest.mark.asyncio
async def test_set_task_done(mongo: MongoFixture,
                             task_factory: TaskFactory) -> None:
    task_id_done = await task_factory(
        action_type=TaskActionType.COMMENT_ST_TICKET,
        payload={'release_id': '123'}
    )
    tas_id_enqueued = await task_factory(
        action_type=TaskActionType.COMMENT_ST_TICKET,
        payload={'release_id': '456'}
    )

    async with await mongo.async_client.start_session() as session:
        await TasksApi(session).set_task_done(task_id_done)

    async with await mongo.async_client.start_session() as session:
        task = await TasksApi(session).load_task(task_id_done)

    assert task == TaskDocument(
        payload={'release_id': '123'},
        action_type=TaskActionType.COMMENT_ST_TICKET,
        status=TaskStatus.DONE
    )

    async with await mongo.async_client.start_session() as session:
        task = await TasksApi(session).load_task(tas_id_enqueued)

    assert task == TaskDocument(
        payload={'release_id': '456'},
        action_type=TaskActionType.COMMENT_ST_TICKET,
        status=TaskStatus.ENQUEUED
    )
