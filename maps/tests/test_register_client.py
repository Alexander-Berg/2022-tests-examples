import aiohttp
import pytest
from smb.common.http_client import HttpClient

from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def run_app_mock(mocker):
    mocker.patch("aiohttp.web.run_app", lambda a, *args, **kwargs: a)


class SampleClient(HttpClient):
    pass


@pytest.fixture
async def client():
    async with SampleClient(url="http://test.server") as _client:
        yield _client


async def test_returns_the_same_client(client):
    returned_client = None

    class SampleApp(Lasagna):
        SWIM_ENGINE_CLS = None

        async def _setup_layers(self, db):
            nonlocal returned_client
            returned_client = self.register_client(client)
            return aiohttp.web.Application()

    app = SampleApp({})
    await app.run()

    assert returned_client is client


async def test_merges_with_client_sensors(client):
    class SampleApp(Lasagna):
        SWIM_ENGINE_CLS = None

        async def _setup_layers(self, db):
            self.register_client(client)
            return aiohttp.web.Application()

    app = SampleApp({})
    await app.run()

    app.sensors.take_group("SampleClient_response_time") is client.sensors.take_group(
        "response_time"
    )
