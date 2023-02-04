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
def mock_export_clients_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.doorman.server.lib.tasks.ClientsYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_import_call_events_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.doorman.server.lib.tasks.CallEventsImportTask.__await__"
    )


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = [
        "geosmb_doorman__clients_yt_export",
        "geosmb_doorman__call_events_import",
    ]

    return config


async def test_export_clients_task_called(api, mock_export_clients_task):
    await asyncio.sleep(0.5)

    assert mock_export_clients_task.called


async def test_import_call_events_task_called(api, mock_import_call_events_task):
    await asyncio.sleep(0.5)

    assert mock_import_call_events_task.called
