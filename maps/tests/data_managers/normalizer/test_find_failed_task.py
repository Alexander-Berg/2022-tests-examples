import pytest

from maps_adv.stat_controller.server.lib.data_managers import NormalizerStatus
from maps_adv.stat_controller.server.lib.db import DbTaskStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_there_is_no_tasks(normalizer_dm):
    got = await normalizer_dm.find_failed_task()

    assert got is None


@pytest.mark.parametrize("status", list(NormalizerStatus))
async def test_returns_nothing_if_there_is_no_failed(status, factory, normalizer_dm):
    await factory.normalizer("executor0", dt(100), dt(160), status)

    got = await normalizer_dm.find_failed_task()

    assert got is None


async def test_returns_failed_task(factory, normalizer_dm):
    task_id = await factory.normalizer("executor0", dt(100), dt(160), failed=True)

    got = await normalizer_dm.find_failed_task()

    assert got["id"] == task_id


async def test_does_not_update_failed_task(factory, normalizer_dm, con):
    task_id = await factory.normalizer("executor0", dt(100), dt(160), failed=True)

    await normalizer_dm.find_failed_task()

    sql = (
        "SELECT EXISTS(SELECT tasks.id "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1 AND tasks.status is NULL "
        "AND tasks_log.status = $2 AND tasks_log.executor_id = $3)"
    )

    got = await con.fetchval(
        sql, task_id, DbTaskStatus.accepted_by_normalizer, "executor0"
    )
    assert got is True


async def test_returns_task_data(factory, normalizer_dm):
    task_id = await factory.normalizer("executor0", dt(100), dt(160), failed=True)

    got = await normalizer_dm.find_failed_task()

    assert got == {"id": task_id, "timing_from": dt(100), "timing_to": dt(160)}
