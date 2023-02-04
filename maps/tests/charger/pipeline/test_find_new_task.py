import pytest

from maps_adv.stat_controller.client.lib.charger import NoChargerTaskFound
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_will_find_new_task(mock_charger_find_new_task, charger_pipeline):
    await charger_pipeline.find_new_task()

    mock_charger_find_new_task.assert_called_with(executor_id="keker")


async def test_will_return_new_task_data(mock_charger_find_new_task, charger_pipeline):
    mock_charger_find_new_task.return_value = {
        "timing_from": dt(0),
        "timing_to": dt(300),
        "id": 100,
        "status": "accepted",
    }

    got = await charger_pipeline.find_new_task()

    assert got == {
        "timing_from": dt(0),
        "timing_to": dt(300),
        "id": 100,
        "status": "accepted",
    }


async def test_raises_if_no_tasks_found(mock_charger_find_new_task, charger_pipeline):
    mock_charger_find_new_task.side_effect = NoChargerTaskFound

    with pytest.raises(NoChargerTaskFound):
        await charger_pipeline.find_new_task()
