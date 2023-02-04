import pytest

from maps_adv.stat_controller.server.lib.data_managers import (
    ChargerStatus,
    NormalizerStatus,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_noting_exists(charger_dm):
    got = await charger_dm.find_oldest_available()

    assert got is None


async def test_returns_nothing_if_no_normalized_or_failed_available(
    factory, charger_dm
):
    await factory.normalizer("executor0", dt(100), dt(160))
    await factory.collector("executor1", dt(200), dt(260))

    got = await charger_dm.find_oldest_available()

    assert got is None


@pytest.mark.parametrize("status", [st for st in ChargerStatus])
async def test_returns_nothing_if_only_not_failed_charger_available(
    status, factory, charger_dm
):
    await factory.charger("executor0", dt(100), dt(160), status)

    got = await charger_dm.find_oldest_available()

    assert got is None


async def test_returns_oldest_completed_normalized(factory, charger_dm):
    task_id = await factory.normalizer(
        "executor0", dt(100), dt(160), NormalizerStatus.completed
    )
    await factory.normalizer("executor1", dt(200), dt(260), NormalizerStatus.completed)

    got = await charger_dm.find_oldest_available()

    assert got["id"] == task_id


async def test_returns_oldest_failed_task(factory, charger_dm):
    task_id = await factory.charger("executor0", dt(100), dt(160), failed=True)
    await factory.charger("executor1", dt(200), dt(260), failed=True)

    got = await charger_dm.find_oldest_available()

    assert got["id"] == task_id


async def test_returns_failed_if_it_is_oldest_task(factory, charger_dm):
    task_id = await factory.charger("executor0", dt(100), dt(160), failed=True)
    await factory.normalizer("executor1", dt(200), dt(260), NormalizerStatus.completed)

    got = await charger_dm.find_oldest_available()

    assert got["id"] == task_id


async def test_returns_normalized_if_it_is_oldest_task(factory, charger_dm):
    task_id = await factory.normalizer(
        "executor1", dt(100), dt(160), NormalizerStatus.completed
    )
    await factory.charger("executor0", dt(200), dt(260), failed=True)

    got = await charger_dm.find_oldest_available()

    assert got["id"] == task_id


async def test_no_cross_task_selection(factory, charger_dm):
    await factory.normalizer("executor0", dt(100), dt(160), failed=True)
    await factory.charger("executor1", dt(200), dt(260))

    got = await charger_dm.find_oldest_available()

    assert got is None


@pytest.mark.parametrize(
    "status", [st for st in ChargerStatus if st != ChargerStatus.completed]
)
async def test_returns_task_data_for_failed_task(status, factory, charger_dm):
    state = ["a", {"int": 10, "decimal-like": "100.35"}]
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), status, failed=True, state=state
    )

    got = await charger_dm.find_oldest_available()

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "status": status,
        "execution_state": ["a", {"int": 10, "decimal-like": "100.35"}],
    }


async def test_returns_task_data_for_normalized_task(factory, charger_dm):
    task_id = await factory.normalizer(
        "executor0", dt(100), dt(160), NormalizerStatus.completed
    )

    got = await charger_dm.find_oldest_available()

    assert got == {"id": task_id, "timing_from": dt(100), "timing_to": dt(160)}
