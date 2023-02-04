import pytest

from maps_adv.stat_controller.server.lib.data_managers import NormalizerStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_nothing_exists(normalizer_dm):
    assert await normalizer_dm.find_last() is None


@pytest.mark.parametrize("failed", (True, False))
@pytest.mark.parametrize("status", list(NormalizerStatus))
async def test_returns_last_by_timing(failed, status, normalizer_dm, factory):
    expected_task_id = await factory.normalizer(
        "executor0", dt(200), dt(260), status, failed=failed
    )
    await factory.normalizer("executor1", dt(100), dt(160))

    got = await normalizer_dm.find_last()

    assert got["id"] == expected_task_id


async def test_returns_task_data(normalizer_dm, factory):
    task_id = await factory.normalizer("executor0", dt(100), dt(160))

    got = await normalizer_dm.find_last()

    assert got == {"id": task_id, "timing_from": dt(100), "timing_to": dt(160)}
