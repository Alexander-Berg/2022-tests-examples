from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.stat_controller.server.lib.data_managers import (
    ChargerStatus,
    CollectorStatus,
    NormalizerStatus,
    SystemOp,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


def expired_datetime(lifetime: int = 300):
    return datetime.now(tz=timezone.utc) - timedelta(seconds=lifetime)


@pytest.fixture
def manager(db):
    return SystemOp(db)


@pytest.mark.parametrize(
    "factory_name, status",
    (
        ["normalizer", NormalizerStatus.accepted],
        *[["charger", st] for st in ChargerStatus.in_progress()],
        ["collector", CollectorStatus.accepted],
    ),
)
async def test_will_mark_non_completed_tasks_as_expired(
    factory_name, status, factory, manager, con
):
    task_id = await factory[factory_name]("executor0", dt(100), dt(160), status)
    await factory.expire(task_id, 300)

    await manager.mark_expired_tasks_as_failed(expired_datetime(300))

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE status IS NULL AND id = $1)"
    assert await con.fetchval(sql, task_id) is True


@pytest.mark.parametrize(
    "factory_name, status",
    (
        ["normalizer", NormalizerStatus.completed],
        ["charger", ChargerStatus.completed],
        ["collector", CollectorStatus.completed],
    ),
)
async def test_completed_tasks_not_marked_as_failed(
    factory_name, status, factory, manager, con
):
    task_id = await factory[factory_name]("executor0", dt(100), dt(160), status)
    await factory.expire(task_id, 300)

    await manager.mark_expired_tasks_as_failed(expired_datetime(300))

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE status IS NULL AND id = $1)"
    assert await con.fetchval(sql, task_id) is False


async def test_does_not_affect_another_non_expired_tasks(factory, manager, con):
    await factory.normalizer("executor0", dt(100), dt(160))
    failed_0 = await factory.normalizer("executor1", dt(200), dt(260))
    await factory.expire(failed_0, 300)

    await factory.charger("executor2", dt(300), dt(360))
    failed_1 = await factory.charger("executor3", dt(400), dt(460))
    await factory.expire(failed_1, 300)

    await factory.collector("executor4", dt(500), dt(560))
    failed_2 = await factory.collector("executor5", dt(600), dt(660))
    await factory.expire(failed_2, 300)

    await manager.mark_expired_tasks_as_failed(expired_datetime(300))

    sql = "SELECT id FROM tasks WHERE status IS NULL"
    assert set([r["id"] for r in await con.fetch(sql)]) == {
        failed_0,
        failed_1,
        failed_2,
    }


@pytest.mark.parametrize("factory_name", ("normalizer", "charger", "collector"))
async def test_does_not_mark_non_expired_tasks(factory_name, factory, manager, con):
    await factory[factory_name]("executor0", dt(100), dt(160))

    await manager.mark_expired_tasks_as_failed(expired_datetime(300))

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE status IS NULL)"
    assert await con.fetchval(sql) is False
