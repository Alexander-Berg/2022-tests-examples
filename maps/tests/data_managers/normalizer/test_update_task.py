import pytest
from asyncpg.exceptions import UniqueViolationError

from maps_adv.stat_controller.server.lib.data_managers import NormalizerStatus
from maps_adv.stat_controller.server.lib.db import DbTaskStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def task_id(factory):
    return await factory.normalizer("lolkek", dt(100), dt(160))


async def test_task_log_created(task_id, normalizer_dm, con):
    await normalizer_dm.update("lolkek", task_id, NormalizerStatus.completed)

    sql = (
        "SELECT COUNT(*) FROM tasks_log "
        "WHERE task_id = $1 AND status = $2 AND executor_id = $3"
    )
    assert await con.fetchval(sql, task_id, DbTaskStatus.normalized, "lolkek") == 1


async def test_task_reference_to_log_updated(task_id, normalizer_dm, con):
    await normalizer_dm.update("lolkek", task_id, NormalizerStatus.completed)

    sql = (
        "SELECT EXISTS(SELECT tasks.id "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1 AND tasks_log.status = $2)"
    )
    assert await con.fetchval(sql, task_id, DbTaskStatus.normalized) is True


async def test_task_has_denormalized_status(task_id, normalizer_dm, con):
    await normalizer_dm.update("lolkek", task_id, NormalizerStatus.completed)

    sql = (
        "SELECT EXISTS(SELECT tasks.id FROM tasks "
        "WHERE tasks.id = $1 AND tasks.status = $2)"
    )
    assert await con.fetchval(sql, task_id, DbTaskStatus.normalized) is True


async def test_no_cross_task_updates(factory, normalizer_dm, con):
    await factory.normalizer("executor0", dt(100), dt(160), NormalizerStatus.completed)
    task_id = await factory.normalizer(
        "executor1", dt(200), dt(260), NormalizerStatus.completed
    )
    await factory.normalizer("executor2", dt(300), dt(360), NormalizerStatus.completed)

    sql = (
        "SELECT EXISTS(SELECT tasks.id "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1 AND tasks.status = $2 "
        "AND tasks_log.status = $2 AND tasks_log.executor_id = $3)"
    )
    got = await con.fetchval(sql, task_id, DbTaskStatus.normalized, "executor1")
    assert got is True


async def test_raises_if_already_set(task_id, normalizer_dm, con):
    await normalizer_dm.update("lolkek", task_id, NormalizerStatus.completed)

    with pytest.raises(UniqueViolationError):
        await normalizer_dm.update("lolkek", task_id, NormalizerStatus.completed)
