import asyncio
from datetime import datetime
from unittest.mock import patch

import pytest

from maps_adv.stat_tasks_starter.tests.tools import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("run_server")]

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
def mock_pipeline(mock_normalizer_find_new_task, mock_normalizer_update_task):
    mock_normalizer_find_new_task.return_value = {
        "timing_from": dt("2019-04-10 11:05:55"),
        "timing_to": dt("2019-04-10 11:11:55"),
        "id": 100,
    }
    mock_normalizer_update_task.return_value = {
        "timing_from": dt("2019-04-10 11:05:55"),
        "timing_to": dt("2019-04-10 11:11:55"),
        "id": 100,
    }


@pytest.fixture
def config(config):
    with patch.dict(
        config.options_map,
        {
            "TASKS_TO_START": {"default": ["Normalizer"]},
            "RELAUNCH_INTERVAL": {"default": 2},
        },
    ):
        config.init()
        yield config


@pytest.fixture
async def run_server(mock_pipeline, app, clear_normalized_ch, aiohttp_server):
    clear_normalized_ch()
    await aiohttp_server(app.api)


async def test_immediately_starts_data_transferring(ch_client):
    await asyncio.sleep(1)

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.normalized_sample")
    assert set(existing_data_in_db) == set(expected_sample)


async def test_will_periodically_run(ch_client, clear_normalized_ch):
    await asyncio.sleep(1)  # normalized now
    clear_normalized_ch()  # ..and deleted by us

    await asyncio.sleep(3)  # wait for next task start

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.normalized_sample")
    assert set(existing_data_in_db) == set(expected_sample)
