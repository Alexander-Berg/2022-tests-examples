import pytest

from maps_adv.stat_controller.server.lib.data_managers import ChargerStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", list(ChargerStatus))
async def test_returns_task_details(status, factory, charger_dm):
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), status, state="some_state"
    )

    got = await charger_dm.retrieve_details(task_id)

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "current_log_id": await factory.cur_log_id(task_id),
        "executor_id": "executor0",
        "status": status,
        "execution_state": "some_state",
    }


async def test_no_cross_task(factory, charger_dm):
    task_id = await factory.charger("executor0", dt(100), dt(160))
    await factory.charger("executor1", dt(200), dt(260), ChargerStatus.completed)

    got = await charger_dm.retrieve_details(task_id)

    assert got == {
        "id": task_id,
        "timing_from": dt(100),
        "timing_to": dt(160),
        "current_log_id": await factory.cur_log_id(task_id),
        "executor_id": "executor0",
        "status": ChargerStatus.accepted,
        "execution_state": None,
    }
