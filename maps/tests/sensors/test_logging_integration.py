import logging

import aiohttp
import pytest

from maps_adv.common.helpers import Any
from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]

url = "/sensors/"


def make_app(logger_name: str = "example", logger_level: str = "ERROR"):
    async def _resource(*args, **kwargs):
        logger = logging.getLogger(logger_name)
        logger.log(logging.getLevelName(logger_level), "LOLKEKMAKAREK %s", "KEKARG")

        return aiohttp.web.Response()

    class App(Lasagna):
        async def _setup_layers(self, db):
            _api = aiohttp.web.Application()
            _api.add_routes([aiohttp.web.get("/kek/", _resource)])
            return _api

    return App({})


@pytest.mark.parametrize("name, level", (["lol", "ERROR"], ["kek", "WARNING"]))
async def test_labels_with_name_and_level(name, level, run):
    app = make_app(name, level)
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert {
        "labels": {"name": name, "level": level, "metric_group": "logging"},
        "type": "RATE",
        "value": 1,
    } in (await resp.json())["sensors"]


@pytest.mark.parametrize(
    "logger_name",
    [
        "aiohttp.access",
        "aiohttp.client",
        "aiohttp.internal",
        "aiohttp.server",
        "aiohttp.web",
        "aiohttp.websocket",
    ],
)
async def test_excludes_aiohttp_logs(logger_name, run):
    logger = logging.getLogger()
    logger.setLevel("DEBUG")
    app = make_app(logger_name, "INFO")
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert {
        "labels": {"name": logger_name, "level": "INFO", "metric_group": "logging"},
        "type": "RATE",
        "value": Any(int),
    } not in (await resp.json())["sensors"]


@pytest.mark.parametrize("logging_level, contains", (["ERROR", False], ["DEBUG", True]))
async def test_logs_only_configured_levels(logging_level, contains, run):
    logger = logging.getLogger()
    logger.setLevel(logging_level)
    app = make_app(logger_level="DEBUG")
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert (
        {
            "labels": {"name": "example", "level": "DEBUG", "metric_group": "logging"},
            "type": "RATE",
            "value": 1,
        }
        in (await resp.json())["sensors"]
    ) is contains


async def test_not_logged_if_sensor_not_defined(run):
    async def _resource(*args, **kwargs):
        logger = logging.getLogger("exanple")
        logger.error("LOLKEKMAKAREK %s", "KEKARG")

        return aiohttp.web.Response()

    class App(Lasagna):

        SENSORS = Lasagna.SENSORS.copy()
        SENSORS.pop("logging")

        async def _setup_layers(self, db):
            _api = aiohttp.web.Application()
            _api.add_routes([aiohttp.web.get("/kek/", _resource)])
            return _api

    app = App({})
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert {
        "labels": {"name": "example", "level": "ERROR", "metric_group": "logging"},
        "type": "RATE",
        "value": 1,
    } not in (await resp.json())["sensors"]
