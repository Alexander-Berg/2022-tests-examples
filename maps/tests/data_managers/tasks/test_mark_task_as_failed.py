import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", ["accepted", "complete", "some_status"])
async def test_marks_task_as_failed(type_id, factory, dm, con, status):
    task = await factory.create_task("executor0", type_id, status=status)

    await dm.mark_task_as_failed(task["task_id"])

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE id = $1 AND status = 'failed')"
    assert await con.fetchval(sql, task["task_id"]) is True


async def test_creates_log_entry(type_id, factory, dm, con):
    task = await factory.create_task("executor0", type_id)

    await dm.mark_task_as_failed(task["task_id"])

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


async def test_does_nothing_if_task_is_already_failed(type_id, factory, dm, con):
    task = await factory.create_task("executor0", type_id, status="failed")

    await dm.mark_task_as_failed(task["task_id"])

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM tasks_log
            WHERE
                task_id = $1
                AND status = 'failed'
                AND executor_id = 'executor0'
        )
    """
    assert await con.fetchval(sql, task["task_id"]) is True
    sql = """
        SELECT COUNT(*)
        FROM tasks_log
        WHERE task_id = $1
    """
    assert await con.fetchval(sql, task["task_id"]) == 2


async def test_does_not_modifies_other_tasks_of_same_type(type_id, factory, dm, con):
    task = await factory.create_task("executor0", type_id)
    another_task = await factory.create_task("executor1", type_id)

    await dm.mark_task_as_failed(task["task_id"])

    sql = """
        SELECT status
        FROM tasks
        WHERE id = $1
    """
    assert await con.fetchval(sql, another_task["task_id"]) == "accepted"
    sql = """
        SELECT status
        FROM tasks_log
        WHERE task_id = $1
    """
    result = await con.fetch(sql, another_task["task_id"])
    assert len(result) == 1
    assert result[0]["status"] == "accepted"


async def test_does_not_modifies_tasks_of_another_type(
    type_id, another_type_id, factory, dm, con
):
    task = await factory.create_task("executor0", type_id)
    another_task = await factory.create_task("executor1", another_type_id)

    await dm.mark_task_as_failed(task["task_id"])

    sql = """
        SELECT status
        FROM tasks
        WHERE id = $1
    """
    assert await con.fetchval(sql, another_task["task_id"]) == "accepted"
    sql = """
        SELECT status
        FROM tasks_log
        WHERE task_id = $1
    """
    result = await con.fetch(sql, another_task["task_id"])
    assert len(result) == 1
    assert result[0]["status"] == "accepted"


async def test_returns_none(type_id, factory, dm):
    task = await factory.create_task("executor0", type_id)

    assert await dm.mark_task_as_failed(task["task_id"]) is None
