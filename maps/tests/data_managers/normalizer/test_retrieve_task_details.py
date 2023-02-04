import pytest

from maps_adv.stat_controller.server.lib.data_managers import NormalizerStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", list(NormalizerStatus))
async def test_returns_task_details(status, normalizer_dm, factory):
    task_id = await factory.normalizer("executor0", dt(100), dt(160), status)

    got = await normalizer_dm.retrieve_details(task_id)

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "current_log_id": await factory.cur_log_id(task_id),
        "executor_id": "executor0",
        "status": status,
        "execution_state": None,
    }


async def test_no_cross_task(normalizer_dm, factory):
    task_id = await factory.normalizer("executor0", dt(100), dt(160))
    await factory.normalizer(
        "executor1", dt(10000), dt(10060), NormalizerStatus.completed
    )

    got = await normalizer_dm.retrieve_details(task_id)

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "current_log_id": await factory.cur_log_id(task_id),
        "executor_id": "executor0",
        "status": NormalizerStatus.accepted,
        "execution_state": None,
    }
