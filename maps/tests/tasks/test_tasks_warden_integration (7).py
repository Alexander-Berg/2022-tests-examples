import asyncio

import pytest

from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def mock_warden(mocker):
    _create = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.create_task", new_callable=coro_mock
    )
    _create.coro.return_value = {"task_id": 1, "status": "accepted", "time_limit": 2}

    _update = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.update_task", new_callable=coro_mock
    )
    _update.coro.return_value = {}

    return _create, _update


@pytest.fixture(autouse=True)
def mock_events_import_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.promoter.server.lib.tasks.LeadEventsYtImportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_leads_export_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.promoter.server.lib.tasks.LeadsYtExportTask.__await__"
    )


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = [
        "geosmb_promoter__import_recent_events",
        "geosmb_promoter__export_leads",
    ]

    return config


async def test_events_import_task_called(api, mock_events_import_task):
    await asyncio.sleep(0.5)

    assert mock_events_import_task.called


async def test_leads_export_task_called(api, mock_leads_export_task):
    await asyncio.sleep(0.5)

    assert mock_leads_export_task.called
