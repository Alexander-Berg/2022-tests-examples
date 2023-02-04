import asyncio

import pytest

from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = ["geosmb_cleaner__delete_clients"]

    return config


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
def mock_delete_clients(mocker):
    return mocker.patch(
        "maps_adv.geosmb.cleaner.server.lib.domain.Domain.delete_clients",
        new_callable=coro_mock,
    )


async def test_calls_task_delete_clients(api, mock_delete_clients):
    await asyncio.sleep(0.5)

    assert mock_delete_clients.called
