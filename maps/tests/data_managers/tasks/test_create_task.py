import pytest
from asyncpg.exceptions import ForeignKeyViolationError, UniqueViolationError

from maps_adv.common.helpers import Any, dt

pytestmark = [pytest.mark.asyncio]


async def test_creates_task(type_id, dm, con):
    task_details = await dm.create_task(
        "executor0", type_id, scheduled_time=dt("2019-12-01 06:00:00")
    )

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM tasks
            WHERE
                id = $1
                AND type_id = $2
                AND status = 'accepted'
                AND scheduled_time = '2019-12-01 06:00:00'
        )
    """
    assert await con.fetchval(sql, task_details["task_id"], type_id) is True


@pytest.mark.parametrize("metadata", [{}, [], {"some": "json"}])
async def test_creates_log_entry_with_metadata(metadata, type_id, dm, con):
    task_details = await dm.create_task(
        "executor0",
        type_id,
        scheduled_time=dt("2019-12-01 06:00:00"),
        metadata=metadata,
    )

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM tasks_log
            WHERE
                task_id = $1
                AND status = 'accepted'
                AND executor_id = 'executor0'
                AND metadata = $2
        )
    """

    assert await con.fetchval(sql, task_details["task_id"], metadata) is True


async def test_creates_log_entry_without_metadata(type_id, dm, con):
    task_details = await dm.create_task(
        "executor0", type_id, scheduled_time=dt("2019-12-01 06:00:00")
    )

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM tasks_log
            WHERE
                task_id = $1
                AND status = 'accepted'
                AND executor_id = 'executor0'
                AND metadata IS NULL
        )
    """

    assert await con.fetchval(sql, task_details["task_id"]) is True


async def test_returns_task_details(type_id, dm):
    got = await dm.create_task(
        "executor0", type_id, scheduled_time=dt("2019-12-01 06:00:00")
    )

    assert got == dict(task_id=Any(int), status="accepted")


async def test_raises_for_unexistant_type(dm, con):
    with pytest.raises(ForeignKeyViolationError):
        await dm.create_task(
            "executor0", 100500, scheduled_time=dt("2019-12-01 06:00:00")
        )


async def test_raises_for_two_identical_tasks(type_id, dm, con):
    await dm.create_task("executor0", type_id, scheduled_time=dt("2019-12-01 06:00:00"))

    with pytest.raises(UniqueViolationError):
        await dm.create_task(
            "executor0", type_id, scheduled_time=dt("2019-12-01 06:00:00")
        )
