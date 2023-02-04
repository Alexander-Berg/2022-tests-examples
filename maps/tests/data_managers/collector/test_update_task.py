import pytest

from maps_adv.stat_controller.server.lib.data_managers import (
    ChargerStatus,
    CollectorStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.lib.db import DbTaskStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "factory_name, status",
    (
        *[["charger", st] for st in list(ChargerStatus)],
        *[["normalizer", st] for st in list(NormalizerStatus)],
        *[["collector", st] for st in list(CollectorStatus)],
    ),
)
@pytest.mark.parametrize(
    "target, db_target",
    (
        [CollectorStatus.accepted, DbTaskStatus.accepted_by_collector],
        [CollectorStatus.completed, DbTaskStatus.collected],
    ),
)
async def test_will_update_from_any_status(
    factory_name, status, target, db_target, factory, collector_dm, con
):
    task_id = await factory[factory_name]("executor0", dt(100), dt(160), status)

    await collector_dm.update("executor1", task_id, target)

    sql = (
        "SELECT EXISTS(SELECT tasks.id "
        "FROM tasks JOIN tasks_log ON tasks.current_log_id = tasks_log.id "
        "WHERE tasks.id = $1 AND tasks.status = $2 AND tasks_log.status = $2)"
    )
    assert await con.fetchval(sql, task_id, db_target) is True


async def test_returns_nothing(factory, collector_dm):
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), ChargerStatus.completed
    )

    got = await collector_dm.update("executor1", task_id, CollectorStatus.accepted)

    assert got is None
