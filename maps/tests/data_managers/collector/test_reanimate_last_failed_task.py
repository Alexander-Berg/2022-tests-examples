import pytest

from maps_adv.stat_controller.server.lib.data_managers import (
    ChargerStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.lib.db import DbTaskStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_there_is_no_tasks(collector_dm):
    got = await collector_dm.reanimate_last_failed("executor0")

    assert got is None


async def test_returns_nothing_if_there_is_no_failed(collector_dm, factory):
    await factory.collector("executor0", dt(100), dt(160))

    got = await collector_dm.reanimate_last_failed("executor1")

    assert got is None


@pytest.mark.parametrize("failed", (True, False))
@pytest.mark.parametrize(
    "factory_name, status",
    (
        *[["charger", st] for st in list(ChargerStatus)],
        *[["normalizer", st] for st in list(NormalizerStatus)],
    ),
)
async def test_does_not_returns_non_collector_tasks(
    failed, factory_name, status, factory, collector_dm
):
    await factory[factory_name]("executor0", dt(100), dt(160), status, failed=failed)

    got = await collector_dm.reanimate_last_failed("executor1")

    assert got is None


async def test_returns_last_failed_task(factory, collector_dm):
    await factory.collector("executor0", dt(100), dt(160), failed=True)
    task_id = await factory.collector("executor1", dt(200), dt(260), failed=True)

    got = await collector_dm.reanimate_last_failed("executor2")

    assert got["id"] == task_id


async def test_returns_last_failed_if_latest_is_not_failed(factory, collector_dm):
    task_id = await factory.collector("executor0", dt(100), dt(160), failed=True)
    await factory.collector("executor1", dt(200), dt(260))

    got = await collector_dm.reanimate_last_failed("executor2")

    assert got["id"] == task_id


async def test_no_cross_task_selection(factory, collector_dm):
    await factory.normalizer("executor0", dt(100), dt(160), failed=True)
    await factory.charger("executor1", dt(200), dt(260), failed=True)
    await factory.collector("executor2", dt(300), dt(360))

    got = await collector_dm.reanimate_last_failed("executor3")

    assert got is None


async def test_updates_failed_task(factory, collector_dm, con):
    task_id = await factory.collector("executor0", dt(100), dt(160), failed=True)

    await collector_dm.reanimate_last_failed("executor1")

    sql = (
        "SELECT EXISTS(SELECT tasks.id "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1 AND tasks.status = $2 "
        "AND tasks_log.status = $2 AND tasks_log.executor_id = $3)"
    )
    got = await con.fetchval(
        sql, task_id, DbTaskStatus.accepted_by_collector, "executor1"
    )
    assert got is True


async def test_returns_task_data(factory, collector_dm):
    task_id = await factory.collector("executor0", dt(100), dt(160), failed=True)

    got = await collector_dm.reanimate_last_failed("executor1")

    assert got == {"id": task_id, "timing_from": dt(100), "timing_to": dt(160)}
