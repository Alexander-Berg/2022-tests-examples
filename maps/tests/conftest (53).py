import aiohttp.pytest_plugin
import pytest

from maps_adv.points.server.lib import Application
from maps_adv.points.server.lib.db import DB

from . import coro_mock

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.pgswim.pytest_plugin_deprecated",
    "maps_adv.points.server.tests.factory",
    "maps_adv.points.server.tests.dms",
]
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


_config = {
    "DATABASE_URL": "postgresql://points:points@localhost:5433/points",
    "WARDEN_URL": None,
    "YT_TOKEN": None,
    "YT_FORECASTS_TABLE": None,
    "YT_CLUSTER": None,
}


@pytest.fixture
def config():
    return _config.copy()


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


class TestClient(aiohttp.test_utils.TestClient):
    async def get(self, path, *args, **kwargs):
        return await self.request("GET", path, *args, **kwargs)

    async def post(self, path, *args, **kwargs):
        return await self.request("POST", path, *args, **kwargs)

    async def put(self, path, *args, **kwargs):
        return await self.request("PUT", path, *args, **kwargs)

    async def patch(self, path, *args, **kwargs):
        return await self.request("PATCH", path, *args, **kwargs)

    async def delete(self, path, *args, **kwargs):
        return await self.request("DELETE", path, *args, **kwargs)

    async def request(self, method, path, *args, **kwargs):
        expected_status = kwargs.pop("expected_status", None)
        decode_as = kwargs.pop("decode_as", None)

        if "proto" in kwargs:
            proto = kwargs.pop("proto")
            kwargs["data"] = proto.SerializeToString()

        response = await super().request(method, path, *args, **kwargs)

        if expected_status:
            assert response.status == expected_status

        if decode_as:
            raw = await response.read()
            return decode_as.FromString(raw)

        if response.headers.get("content_type") == "application/json":
            return await response.json()
        else:
            return await response.read()


@pytest.fixture
async def setup_api(loop, db, aiohttp_client, request):
    _client = []

    async def _make(config):
        app = Application(config)
        api = app.setup(db)

        server = aiohttp.test_utils.TestServer(api, loop=loop)
        client = TestClient(server, loop=loop)
        _client.append(client)

        await client.start_server()
        return client

    yield _make

    await _client[0].close()


@pytest.fixture
async def api(setup_api, config):
    return await setup_api(config)


@pytest.fixture
def mock_create_task(mocker):
    return mocker.patch(
        "maps_adv.warden.client.lib.client.Client.create_task", new_callable=coro_mock
    ).coro


@pytest.fixture
def mock_update_task(mocker):
    return mocker.patch(
        "maps_adv.warden.client.lib.client.Client.update_task", new_callable=coro_mock
    ).coro
