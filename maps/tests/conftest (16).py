import asyncio
from unittest.mock import Mock

import aiohttp
import pytest
from aiohttp.test_utils import TestClient as DefaultTestClient

from maps_adv.common.helpers import coro_mock
from maps_adv.common.lasagna import LasagnaAccessLogger

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "smb.common.aiotvm.pytest.plugin",
]


class TestClient(DefaultTestClient):
    async def start_server(self, **kwargs):
        await self._server.start_server(loop=self._loop, **kwargs)


@pytest.fixture
async def run():
    _client = {}

    async def _run(app, **kwargs):
        loop = asyncio.get_event_loop()
        api = await app.setup(Mock())

        server = aiohttp.test_utils.TestServer(api, loop=loop)
        client = _client["client"] = TestClient(server, loop=loop)

        await client.start_server(access_log_class=LasagnaAccessLogger)
        return client

    yield _run

    if "client" in _client:
        await _client["client"].close()


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
