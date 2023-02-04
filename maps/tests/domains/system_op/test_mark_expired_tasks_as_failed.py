import pytest

from maps_adv.stat_controller.server.lib.domains import (
    ChargerStatus,
    NormalizerStatus,
    SystemOp,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def domain(system_op_dm, config):
    return SystemOp(system_op_dm, config["TASK_LIFETIME"])


@pytest.mark.parametrize(
    "factory_name, status",
    [("normalizer", NormalizerStatus.accepted), ("charger", ChargerStatus.accepted)],
)
async def test_will_mark_expired_tasks_as_failed(
    factory_name, status, domain, factory, config, con
):
    task_id = await factory[factory_name]("executor0", dt(100), dt(160), status)
    await factory.expire(task_id, config["TASK_LIFETIME"])

    await domain.mark_expired_tasks_as_failed()

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE status IS NULL AND id = $1)"
    assert await con.fetchval(sql, task_id) is True


@pytest.mark.parametrize(
    "factory_name, status",
    [("normalizer", NormalizerStatus.completed), ("charger", ChargerStatus.completed)],
)
async def test_completed_tasks_does_not_marked_as_failed(
    factory_name, status, domain, factory, config, con
):
    task_id = await factory[factory_name]("executor0", dt(100), dt(160), status)
    await factory.expire(task_id, config["TASK_LIFETIME"])

    await domain.mark_expired_tasks_as_failed()

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE status IS NULL)"
    assert await con.fetchval(sql) is False


async def test_does_not_affect_another_tasks(factory, domain, config, con):
    await factory.normalizer("executor0", dt(100), dt(160), NormalizerStatus.completed)
    failed_0 = await factory.normalizer("executor1", dt(200), dt(260), failed=True)
    await factory.charger("executor3", dt(300), dt(360))

    # This task should expire and be marked as failed
    failed_1 = await factory.charger("executor4", dt(400), dt(460))
    await factory.expire(failed_1, config["TASK_LIFETIME"])

    await domain.mark_expired_tasks_as_failed()

    sql = "SELECT id FROM tasks WHERE status IS NULL"
    assert set([r["id"] for r in await con.fetch(sql)]) == {failed_0, failed_1}


@pytest.mark.parametrize("factory_name", ("normalizer", "charger"))
async def test_does_not_mark_non_expired_tasks(factory_name, domain, factory, con):
    await factory[factory_name]("executor0", dt(100), dt(160))

    await domain.mark_expired_tasks_as_failed()

    sql = "SELECT EXISTS(SELECT id FROM tasks WHERE status IS NULL)"
    assert await con.fetchval(sql) is False
