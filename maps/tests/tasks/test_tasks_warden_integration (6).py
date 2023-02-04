import asyncio
from unittest.mock import AsyncMock

import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def mock_warden(mocker):
    _create = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.create_task", new_callable=AsyncMock
    )
    _create.return_value = {"task_id": 1, "status": "accepted", "time_limit": 2}

    _update = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.update_task", new_callable=AsyncMock
    )
    _update.return_value = {}

    return _create, _update


@pytest.fixture(autouse=True)
def mock_cdp_users_sync_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.marksman.server.lib.tasks.CdpUsersSyncTask.__await__",
        AsyncMock(),
    )


@pytest.fixture(autouse=True)
def mock_sync_segments_sizes_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.marksman.server.lib.domain.Domain.sync_segments_sizes",
        AsyncMock(),
    )


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = [
        "geosmb_marksman__cdp_users_sync_task",
        "geosmb_marksman__sync_segment_sizes",
    ]

    return config


async def test_sync_businesses_contacts_schema(api, mock_cdp_users_sync_task):
    await asyncio.sleep(0.5)

    mock_cdp_users_sync_task.assert_awaited()


async def test_sync_segments_sizes(api, mock_sync_segments_sizes_task):
    await asyncio.sleep(0.5)

    mock_sync_segments_sizes_task.assert_awaited()
