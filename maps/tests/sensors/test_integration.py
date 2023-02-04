import aiohttp
import pytest
from smb.common.sensors import RateBuilder

from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]

url = "/sensors/"


async def test_can_work_with_sensors_manually(run):
    class Resources:
        def __init__(self, hub):
            self.hub = hub

        async def handle(self, *args, **kwargs):
            self.hub.take("writes", label="value").add(10)
            return aiohttp.web.Response(status=200)

    class App(Lasagna):
        SENSORS = {"writes": RateBuilder()}

        async def _setup_layers(self, db):
            resources = Resources(self.sensors)
            _api = aiohttp.web.Application()
            _api.add_routes([aiohttp.web.get("/kek/", resources.handle)])
            return _api

    app = App({})
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert await resp.json() == {
        "sensors": [
            {
                "labels": {"label": "value", "metric_group": "writes"},
                "type": "RATE",
                "value": 10,
            }
        ]
    }


async def test_may_be_totally_disabled(run):
    async def resource(*args, **kwargs):
        return aiohttp.web.Response(status=200)

    class App(Lasagna):
        SENSORS = None

        async def _setup_layers(self, db):
            assert self.sensors is None

            _api = aiohttp.web.Application()
            _api.add_routes([aiohttp.web.get("/kek/", resource)])
            return _api

    app = App({})
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert resp.status == 404
