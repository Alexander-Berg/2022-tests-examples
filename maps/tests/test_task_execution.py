import asyncio

import pytest

from maps_adv.export.tests import coro_mock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_create_task(mocker):
    mock = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.create_task", new_callable=coro_mock
    ).coro
    mock.return_value = {"task_id": 1, "status": "accepted", "time_limit": 2}
    return mock


@pytest.fixture
def mock_update_task(mocker):
    mock = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.update_task", new_callable=coro_mock
    ).coro
    mock.return_value = {}
    return mock


@pytest.fixture
def mock_pipeline(mocker):
    async def pipeline_coro():
        await asyncio.sleep(0.05)

    mock = mocker.patch(
        "maps_adv.export.lib.application.export_pipeline", new_callable=coro_mock
    ).coro
    mock.side_effect = pipeline_coro
    return mock


async def test_task_execution_in_background(
    setup_app, mock_create_task, mock_update_task, mock_pipeline
):
    await setup_app()

    await asyncio.sleep(0.1)

    assert mock_pipeline.call_count > 0
    assert mock_create_task.call_count > 0
    assert mock_update_task.call_count > 0
