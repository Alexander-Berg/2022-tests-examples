from asyncio import sleep

import pytest
from asyncpg.exceptions import UniqueViolationError

pytestmark = [pytest.mark.asyncio]

STATUSES = ["some_status", "completed", "failed"]


@pytest.mark.parametrize("status", STATUSES)
async def test_updates_task(status, type_id, factory, dm, con):
    task_id = (await factory.create_task("executor0", type_id))["task_id"]

    await dm.update_task("executor0", task_id, status)

    sql = """
    SELECT EXISTS(
        SELECT id
        FROM tasks
        WHERE id = $1 AND type_id = $2 AND status = $3
    )
    """
    assert await con.fetchval(sql, task_id, type_id, status) is True


@pytest.mark.real_db
async def test_not_updates_task_intake_time(type_id, factory, dm, con):
    task_id = (await factory.create_task("executor0", type_id))["task_id"]
    await sleep(0.1)

    await dm.update_task("executor0", task_id, "some_status")

    row = await con.fetchrow(
        "SELECT created, intake_time FROM tasks WHERE id = $1", task_id
    )
    assert row["created"] == row["intake_time"]


@pytest.mark.parametrize("status", STATUSES)
@pytest.mark.parametrize("metadata", [{}, [], {"some": "json"}])
async def test_creates_log_entry_for_task_with_metadata(
    status, metadata, task_id, dm, con
):
    await dm.update_task("executor0", task_id, status, metadata=metadata)

    sql = """
    SELECT EXISTS(
        SELECT id
        FROM tasks_log
        WHERE
            task_id = $1
            AND status = $2
            AND executor_id = 'executor0'
            AND metadata = $3
    )
    """
    assert await con.fetchval(sql, task_id, status, metadata) is True


@pytest.mark.parametrize("status", STATUSES)
async def test_creates_log_entry_for_task_without_metadata(status, task_id, dm, con):
    await dm.update_task("executor0", task_id, status)

    sql = """
    SELECT EXISTS(
        SELECT id
        FROM tasks_log
        WHERE
            task_id = $1
            AND status = $2
            AND executor_id = 'executor0'
            AND metadata IS NULL
    )
    """
    assert await con.fetchval(sql, task_id, status) is True


async def test_returns_nothing(task_id, dm, con):
    got = await dm.update_task("executor0", task_id, "completed")

    assert got is None


async def test_raises_when_trying_to_update_twice(task_id, dm, con):
    await dm.update_task("executor0", task_id, "completed")

    with pytest.raises(UniqueViolationError):
        await dm.update_task("executor0", task_id, "completed")
