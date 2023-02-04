from datetime import datetime

import pytest

from maps_adv.stat_controller.client.lib import UnknownResponse
from maps_adv.stat_controller.client.lib.normalizer import TaskStatus
from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio]

expected_sample = [
    (
        datetime.utcfromtimestamp(1554894569),
        7726,
        "37b36bd03bf84fcfe8f95ab43191653c",
        4,
        "4CE92B30-6A33-457D-A7D4-1B8CBAD54597",
        "iOS",
        "1112",
        11476,
        55.718732876522175,
        37.40151579701865,
        "geoadv.bb.action.makeRoute",
    ),
    (
        datetime.utcfromtimestamp(1554894569),
        7728,
        "525ef1ea2bd9042d64ac32d3ea4adbcb",
        4,
        "4CE92B30-6A33-457D-A7D4-1B8CBAD54597",
        "iOS",
        "1112",
        11476,
        55.72769056007629,
        37.470415387480365,
        "geoadv.bb.action.makeRoute",
    ),
]


@pytest.fixture
def pipeline(
    mock_normalizer_find_new_task, mock_normalizer_update_task, normalizer_pipeline
):
    mock_normalizer_find_new_task.return_value = {
        "timing_from": dt("2019-04-10 11:05:55"),
        "timing_to": dt("2019-04-10 11:11:55"),
        "id": 100,
    }
    mock_normalizer_update_task.side_effect = {
        "id": 100,
        "timing_from": dt("2019-04-10 11:05:55"),
        "timing_to": dt("2019-04-10 11:11:55"),
        "executor_id": "keker",
        "status": TaskStatus.completed,
    }
    return normalizer_pipeline


async def test_will_be_normalized(pipeline, ch_client):
    await pipeline()

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.normalized_sample")
    assert set(existing_data_in_db) == set(expected_sample)


async def test_nothing_normalized_on_exceptions_when_finding_new_task(
    mock_normalizer_find_new_task, pipeline, ch_client
):
    mock_normalizer_find_new_task.side_effect = UnknownResponse(
        status_code=500, payload={}
    )

    with pytest.raises(UnknownResponse):
        await pipeline()

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.normalized_sample")
    assert existing_data_in_db == []


async def test_nothing_normalized_on_exceptions_when_update(
    mock_normalizer_update_task, pipeline, ch_client
):
    mock_normalizer_update_task.side_effect = UnknownResponse(
        status_code=500, payload={}
    )

    with pytest.raises(UnknownResponse):
        await pipeline()

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.normalized_sample")
    assert set(existing_data_in_db) == set(expected_sample)
