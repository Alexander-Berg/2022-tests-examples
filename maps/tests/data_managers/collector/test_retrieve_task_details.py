import pytest

from maps_adv.stat_controller.server.lib.data_managers import CollectorStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", list(CollectorStatus))
async def test_returns_task_details(status, factory, collector_dm):
    task_id = await factory.collector("executor0", dt(100), dt(160), status)

    got = await collector_dm.retrieve_details(task_id)

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "current_log_id": await factory.cur_log_id(task_id),
        "executor_id": "executor0",
        "status": status,
        "execution_state": None,
    }


async def test_no_cross_task(factory, collector_dm):
    task_id = await factory.collector("executor0", dt(100), dt(160))
    await factory.collector("executor1", dt(200), dt(260), CollectorStatus.completed)

    got = await collector_dm.retrieve_details(task_id)

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "current_log_id": await factory.cur_log_id(task_id),
        "executor_id": "executor0",
        "status": CollectorStatus.accepted,
        "execution_state": None,
    }
