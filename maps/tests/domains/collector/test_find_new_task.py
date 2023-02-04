import pytest

from maps_adv.stat_controller.server.lib.domains import (
    ChargerStatus,
    Collector,
    CollectorStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def domain(collector_dm):
    return Collector(collector_dm)


@pytest.fixture
async def charged(factory):
    return await factory.charger("executor0", dt(100), dt(160), ChargerStatus.completed)


async def test_returns_nothing_if_there_is_no_tasks(domain):
    assert await domain.find_new("executor0") is None


async def test_returns_task_updated_from_charged(charged, domain):
    got = await domain.find_new("executor0")

    assert got["id"] == charged


@pytest.mark.parametrize(
    "factory_name, status",
    [
        *[["charger", st] for st in ChargerStatus.in_progress()],
        *[["normalizer", st] for st in list(NormalizerStatus)],
    ],
)
async def test_does_not_use_not_charged_tasks(factory_name, status, factory, domain):
    await factory[factory_name]("executor0", dt(100), dt(160), status)

    got = await domain.find_new("executor1")

    assert got is None


@pytest.mark.parametrize("status", [st for st in CollectorStatus])
async def test_returns_nothing_if_only_collector_tasks_available(
    status, domain, factory
):
    await factory.collector("executor0", dt(100), dt(160), status)

    got = await domain.find_new("executor1")

    assert got is None


async def test_uses_last_charged_if_there_are_many(charged, domain, factory):
    await factory.charger("executor1", dt(0), dt(60), ChargerStatus.completed)

    got = await domain.find_new("executor2")

    assert got["id"] == charged


async def test_can_give_tasks_if_collecting_in_progress(charged, domain, factory):
    await factory.collector("executor1", dt(200), dt(260))

    got = await domain.find_new("executor3")

    assert got["id"] == charged


async def test_updates_status_to_accepted_by_collector(charged, domain, factory):
    got = await domain.find_new("executor1")

    details = await factory.collector.details(got["id"])
    assert details["status"] == CollectorStatus.accepted


async def test_returns_task_data(charged, domain):
    got = await domain.find_new("executor1")

    assert got == {"id": charged, "timing_from": dt(100), "timing_to": dt(160)}


@pytest.mark.parametrize("status", ChargerStatus.in_progress())
async def test_returns_reanimated_if_there_is_no_available(status, domain, factory):
    task_id = await factory.collector("executor0", dt(100), dt(160), failed=True)
    await factory.charger("executor1", dt(200), dt(260), status)

    got = await domain.find_new("executor2")

    assert got["id"] == task_id


async def test_returns_new_instead_of_reanimated_if_available(domain, factory):
    await factory.collector("executor0", dt(100), dt(160), failed=True)
    task_id = await factory.charger(
        "executor1", dt(200), dt(260), ChargerStatus.completed
    )

    got = await domain.find_new("executor2")

    assert got["id"] == task_id


async def test_returns_reanimated_task_data(domain, factory):
    task_id = await factory.collector("executor0", dt(100), dt(160), failed=True)

    got = await domain.find_new("executor1")

    assert got == {"id": task_id, "timing_from": dt(100), "timing_to": dt(160)}
