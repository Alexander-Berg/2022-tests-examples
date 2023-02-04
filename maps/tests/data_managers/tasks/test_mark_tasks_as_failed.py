from asyncio import sleep
from datetime import datetime, timezone

import pytest
from asyncpg.exceptions import UniqueViolationError

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


async def test_fails_if_limit_and_extra_time_are_expired(factory, dm, con):
    type_id = await factory.create_task_type("short", time_limit=1)
    task = await factory.create_task("executor0", type_id)

    await sleep(3)
    await dm.mark_tasks_as_failed(extra_time=1)

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE id = $1 AND status = 'failed')"
    assert await con.fetchval(sql, task["task_id"]) is True


@pytest.mark.parametrize("time_limit, extra_time", [(1, 2), (2, 1)])
async def test_not_fails_if_limit_or_extra_time_are_not_expired(
    time_limit, extra_time, factory, dm, con
):
    type_id = await factory.create_task_type("short", time_limit=time_limit)
    task = await factory.create_task(
        "executor0", type_id, created=datetime.now(tz=timezone.utc)
    )

    await sleep(2)
    await dm.mark_tasks_as_failed(extra_time=extra_time)

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE id  = $1 AND status = 'failed')"
    assert await con.fetchval(sql, task["task_id"]) is False


async def test_ignores_completed_tasks(factory, dm, con):
    type_id = await factory.create_task_type("type_one", time_limit=1)
    await factory.create_task("executor2", type_id, "completed")

    await sleep(3)
    await dm.mark_tasks_as_failed(extra_time=1)

    sql = """
    SELECT exists(
        SELECT tasks.id
        FROM tasks JOIN tasks_log ON tasks.id = tasks_log.task_id
        WHERE (tasks.status = 'failed' OR tasks_log.status = 'failed')
    )
    """
    assert await con.fetchval(sql) is False


async def test_ignores_failed_tasks(factory, dm, con):
    type_id = await factory.create_task_type("type_one", time_limit=1)
    await factory.create_task("executor1", type_id, "failed")

    await sleep(3)

    try:
        await dm.mark_tasks_as_failed(extra_time=1)
    except UniqueViolationError:
        pytest.fail("Constraint Violation attempt")


async def test_creates_log_entry(factory, dm, con):
    type_id = await factory.create_task_type("task_type", time_limit=1)
    task = await factory.create_task("executor", type_id)

    await sleep(3)
    await dm.mark_tasks_as_failed(extra_time=1)

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM tasks_log
            WHERE
                task_id = $1
                AND status = 'failed'
                AND executor_id IS NULL
        )
    """
    assert await con.fetchval(sql, task["task_id"]) is True


async def test_returns_nothing(factory, dm):
    type_id = await factory.create_task_type("task_type", time_limit=1)
    await factory.create_task("executor", type_id)

    await sleep(3)
    got = await dm.mark_tasks_as_failed(extra_time=1)

    assert got is None


async def test_time_limits_do_not_cross(factory, dm, con):
    type_one_id = await factory.create_task_type("type_one", time_limit=1)
    type_two_id = await factory.create_task_type("type_two", time_limit=50)
    failed_task = await factory.create_task(
        "executor1", type_one_id, created=datetime.now(tz=timezone.utc)
    )
    await factory.create_task(
        "executor3", type_two_id, created=datetime.now(tz=timezone.utc)
    )

    await sleep(3)
    await dm.mark_tasks_as_failed(extra_time=1)

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE id != $1 AND status = 'failed')"
    assert await con.fetchval(sql, failed_task["task_id"]) is False
