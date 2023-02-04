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
def mock_process_unvalidated(mocker):
    return mocker.patch(
        "maps_adv.geosmb.harmonist.server.lib.domain.Domain.process_unvalidated",
        coro_mock(),
    )


@pytest.fixture(autouse=True)
def mock_process_unimported(mocker):
    return mocker.patch(
        "maps_adv.geosmb.harmonist.server.lib.domain.Domain.process_unimported",
        coro_mock(),
    )


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = [
        "geosmb_harmonist__process_unvalidated",
        "geosmb_harmonist__process_unimported",
    ]

    return config


async def test_process_unvalidated(api, mock_process_unvalidated):
    await asyncio.sleep(0.5)

    assert mock_process_unvalidated.called


async def test_process_unimported(api, mock_process_unimported):
    await asyncio.sleep(0.5)

    assert mock_process_unimported.called
