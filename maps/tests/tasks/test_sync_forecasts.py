import asyncio

import pytest

from maps_adv.points.server.lib.tasks import SyncForecasts

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def setup_warden(mock_create_task, mock_update_task):
    mock_create_task.return_value = {
        "task_id": 1,
        "status": "accepted",
        "time_limit": 2,
    }


async def test_task_will_call_data_manager(loop, forecasts_dm):
    task = loop.create_task(SyncForecasts.schedule(forecasts_dm, "http://lol.kek"))

    await asyncio.sleep(0.1)
    assert forecasts_dm.sync_forecasts.call_count > 0

    task.cancel()


async def test_requests_warden_for_task(mock_create_task, forecasts_dm):
    await SyncForecasts(forecasts_dm, "http://lol.kek")()

    assert mock_create_task.call_count == 1


async def test_will_notify_warden_on_completion(mock_update_task, forecasts_dm):
    await SyncForecasts(forecasts_dm, "http://lol.kek")()

    mock_update_task.assert_called_with(1, "completed")


async def test_will_notify_warden_on_fail(forecasts_dm, mock_update_task):
    forecasts_dm.sync_forecasts.coro.side_effect = Exception()

    with pytest.raises(Exception):
        await SyncForecasts(forecasts_dm, "http://lol.kek")()

    mock_update_task.assert_called_with(1, "failed")


async def test_task_will_fail_by_warden_limit(forecasts_dm):
    forecasts_dm.sync_forecasts.coro.return_value = asyncio.sleep(3)

    with pytest.raises(asyncio.TimeoutError):
        await SyncForecasts(forecasts_dm, "http://lol.kek")()


async def test_retries_on_any_dm_exception(loop, forecasts_dm):
    forecasts_dm.sync_forecasts.coro.side_effect = Exception()
    task = loop.create_task(SyncForecasts.schedule(forecasts_dm, "http://lol.kek"))

    await asyncio.sleep(0.1)
    assert forecasts_dm.sync_forecasts.call_count > 1

    task.cancel()


async def test_retries_on_task_creation_error(loop, forecasts_dm, mock_create_task):
    mock_create_task.side_effect = Exception()
    task = loop.create_task(SyncForecasts.schedule(forecasts_dm, "http://lol.kek"))

    await asyncio.sleep(11)
    assert mock_create_task.call_count > 1

    task.cancel()


async def test_rerun_on_task_update_error(loop, forecasts_dm, mock_update_task):
    mock_update_task.side_effect = Exception()
    task = loop.create_task(SyncForecasts.schedule(forecasts_dm, "http://lol.kek"))

    await asyncio.sleep(0.1)
    assert forecasts_dm.sync_forecasts.call_count > 1

    task.cancel()
