import pytest

from maps_adv.stat_controller.client.lib.normalizer import Client
from maps_adv.stat_tasks_starter.lib.normalizer.pipeline import Pipeline
from maps_adv.stat_tasks_starter.tests.tools import coro_mock


@pytest.fixture
def mock_normalizer_find_new_task(mocker):
    return mocker.patch(
        "maps_adv.stat_controller.client.lib.normalizer.Client.find_new_task",
        new_callable=coro_mock,
    ).coro


@pytest.fixture
def mock_normalizer_update_task(mocker):
    return mocker.patch(
        "maps_adv.stat_controller.client.lib.normalizer.Client.update_task",
        new_callable=coro_mock,
    ).coro


@pytest.fixture
async def normalizer_pipeline(
    config, mock_normalizer_find_new_task, mock_normalizer_update_task
):
    async with Client(
        "http://kekland.com",
        retry_settings={
            "max_attempts": config.RETRY_MAX_ATTEMPTS,
            "wait_multiplier": config.RETRY_WAIT_MULTIPLIER,
        },
    ) as client:
        yield Pipeline("keker", client)
