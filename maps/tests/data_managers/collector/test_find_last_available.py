import pytest

from maps_adv.stat_controller.server.lib.data_managers import (
    ChargerStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_nothing_exists(collector_dm):
    got = await collector_dm.find_last_available()

    assert got is None


@pytest.mark.parametrize(
    "factory_name, status",
    [
        *[["charger", st] for st in ChargerStatus.in_progress()],
        *[["normalizer", st] for st in list(NormalizerStatus)],
    ],
)
async def test_returnins_nothing_if_no_charged_available(
    factory_name, status, factory, collector_dm
):
    await factory[factory_name]("executor0", dt(100), dt(160), status)

    got = await collector_dm.find_last_available()

    assert got is None


async def test_returns_last_completed_charged(factory, collector_dm):
    await factory.charger("executor0", dt(100), dt(160), ChargerStatus.completed)
    task_id = await factory.charger(
        "executor1", dt(200), dt(260), ChargerStatus.completed
    )

    got = await collector_dm.find_last_available()

    assert got["id"] == task_id


async def test_returns_task_data(factory, collector_dm):
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), ChargerStatus.completed
    )

    got = await collector_dm.find_last_available()

    assert got == {"id": task_id, "timing_from": dt(100), "timing_to": dt(160)}
