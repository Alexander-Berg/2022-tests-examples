import pytest

from maps_adv.stat_controller.client.lib.normalizer import NoNormalizerTaskFound
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


async def test_will_find_new_task(mock_normalizer_find_new_task, normalizer_pipeline):
    await normalizer_pipeline.find_new_task()

    mock_normalizer_find_new_task.assert_called_with(executor_id="keker")


async def test_will_return_new_task_data(
    mock_normalizer_find_new_task, normalizer_pipeline
):
    mock_normalizer_find_new_task.return_value = {
        "timing_from": dt("2019-01-01 12:00:00"),
        "timing_to": dt("2019-01-01 12:05:00"),
        "id": 100,
    }

    got = await normalizer_pipeline.find_new_task()

    assert got["id"] == 100
    assert got["timing_from"] == dt("2019-01-01 12:00:00")
    assert got["timing_to"] == dt("2019-01-01 12:05:00")


async def test_raises_if_not_tasks_found(
    mock_normalizer_find_new_task, normalizer_pipeline
):
    mock_normalizer_find_new_task.side_effect = NoNormalizerTaskFound

    with pytest.raises(NoNormalizerTaskFound):
        await normalizer_pipeline.find_new_task()
