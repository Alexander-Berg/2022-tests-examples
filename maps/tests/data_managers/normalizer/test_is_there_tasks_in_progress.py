import pytest

from maps_adv.stat_controller.server.lib.data_managers import (
    ChargerStatus,
    CollectorStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_false_if_nothing_exists(normalizer_dm):
    assert await normalizer_dm.is_there_in_progress() is False


@pytest.mark.parametrize("status", list(ChargerStatus))
async def test_returns_false_if_only_charger_tasks(status, factory, normalizer_dm):
    await factory.charger("executor0", dt(100), dt(160), status)

    assert await normalizer_dm.is_there_in_progress() is False


@pytest.mark.parametrize("status", list(CollectorStatus))
async def test_returns_false_if_only_collector_tasks(status, factory, normalizer_dm):
    await factory.collector("executor0", dt(100), dt(160), status)

    assert await normalizer_dm.is_there_in_progress() is False


async def test_returns_false_if_only_normalized_tasks(factory, normalizer_dm):
    await factory.normalizer("executor0", dt(100), dt(160), NormalizerStatus.completed)

    assert await normalizer_dm.is_there_in_progress() is False


async def test_returns_false_if_only_normalized_failed_tasks(factory, normalizer_dm):
    await factory.normalizer("executor0", dt(100), dt(160), failed=True)

    assert await normalizer_dm.is_there_in_progress() is False


async def test_returns_true_if_any_not_normalized_task(factory, normalizer_dm):
    await factory.normalizer("executor0", dt(100), dt(160), NormalizerStatus.completed)
    await factory.normalizer("executor1", dt(200), dt(260), NormalizerStatus.accepted)

    assert await normalizer_dm.is_there_in_progress() is True
