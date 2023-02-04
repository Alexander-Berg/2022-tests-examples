import pytest

from maps_adv.stat_controller.server.lib.data_managers import (
    ChargerStatus,
    CollectorStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_false_if_nothing_exists(charger_dm):
    assert await charger_dm.is_there_in_progress() is False


@pytest.mark.parametrize("status", list(NormalizerStatus))
async def test_returns_false_if_only_normalizer_tasks(status, factory, charger_dm):
    await factory.normalizer("executor0", dt(100), dt(160), status)

    assert await charger_dm.is_there_in_progress() is False


@pytest.mark.parametrize("status", list(CollectorStatus))
async def test_returns_false_if_only_collector_tasks(status, factory, charger_dm):
    await factory.collector("executor0", dt(100), dt(160), status)

    assert await charger_dm.is_there_in_progress() is False


async def test_returns_false_if_only_charged_tasks(factory, charger_dm):
    await factory.charger("executor0", dt(100), dt(160), ChargerStatus.completed)

    assert await charger_dm.is_there_in_progress() is False


@pytest.mark.parametrize(
    "status", [st for st in ChargerStatus if st != ChargerStatus.completed]
)
async def test_returns_true_if_any_not_charged_task(status, factory, charger_dm):
    await factory.normalizer("executor0", dt(100), dt(160), NormalizerStatus.completed)
    await factory.charger("executor1", dt(200), dt(260), status)

    assert await charger_dm.is_there_in_progress() is True
