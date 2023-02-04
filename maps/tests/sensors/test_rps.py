import asyncio

import aiohttp
import pytest

from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]

url = "/sensors/"


def make_app(url, status, *, wait=0, exc=None):
    async def _resource(*args, **kwargs):
        if wait:
            await asyncio.sleep(wait)

        if exc:
            raise exc

        return aiohttp.web.Response(status=status)

    class App(Lasagna):
        async def _setup_layers(self, db):
            _api = aiohttp.web.Application()
            _api.add_routes([aiohttp.web.get(url, _resource)])
            return _api

    return App({})


async def test_metrics_are_empty_by_default(run):
    app = make_app("/kek/", 200)
    client = await run(app)

    resp = await client.get(url)

    assert await resp.json() == {"sensors": []}


@pytest.mark.parametrize("path", ["/lol/", "/kek/", "/makarek/"])
@pytest.mark.parametrize("response_code", [200, 201, 204, 400, 404, 500])
async def test_labels_with_path_and_code(path, response_code, run):
    app = make_app(path, response_code)
    client = await run(app)

    await client.get(path)
    resp = await client.get(url)

    assert {
        "labels": {"path": path, "response_code": response_code, "metric_group": "rps"},
        "type": "RATE",
        "value": 1,
    } in (await resp.json())["sensors"]


@pytest.mark.parametrize("count", range(1, 5))
async def test_counts_all_requests(count, run):
    app = make_app("/kek/", 200)
    client = await run(app)

    for _ in range(count):
        await client.get("/kek/")
    resp = await client.get(url)

    assert {
        "labels": {"path": "/kek/", "response_code": 200, "metric_group": "rps"},
        "type": "RATE",
        "value": count,
    } in (await resp.json())["sensors"]


@pytest.mark.parametrize(
    "resource_path, request_url, expected",
    (
        ["/sample/{name}/", "/sample/yopta/", "/sample/{name}/"],
        [r"/{number:\d+}/", "/100500/", "/{number}/"],
        [r"/{name}/{number:\d+}/", "/yopta/100500/", "/{name}/{number}/"],
    ),
)
async def test_dynamic_routes_are_logged_well(
    resource_path, request_url, expected, run
):
    app = make_app(resource_path, 200)
    client = await run(app)

    await client.get(request_url)
    resp = await client.get(url)

    assert {
        "labels": {"metric_group": "rps", "path": expected, "response_code": 200},
        "type": "RATE",
        "value": 1,
    } in (await resp.json())["sensors"]


async def test_unknown_routes_are_not_logged(run):
    app = make_app("/lol/", 200)
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert await resp.json() == {"sensors": []}


async def test_server_errors_are_logged(run):
    app = make_app("/kek/", 200, exc=Exception("Unexpected error"))
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert {
        "labels": {"metric_group": "rps", "path": "/kek/", "response_code": 500},
        "type": "RATE",
        "value": 1,
    } in (await resp.json())["sensors"]


async def test_cancelled_requests_are_logged(run):
    app = make_app("/kek/", 200, wait=1)
    client = await run(app)

    with pytest.raises(asyncio.TimeoutError):
        await asyncio.wait_for(client.get("/kek/"), 0.1)
    resp = await client.get(url)

    assert {
        "labels": {
            "metric_group": "rps",
            "path": "/kek/",
            "response_code": "closed_by_client",
        },
        "type": "RATE",
        "value": 1,
    } in (await resp.json())["sensors"]


async def test_not_logged_if_sensor_not_defined(run):
    async def _resource(*args, **kwargs):
        return aiohttp.web.Response(status=200)

    class App(Lasagna):

        SENSORS = Lasagna.SENSORS.copy()
        SENSORS.pop("rps")

        async def _setup_layers(self, db):
            _api = aiohttp.web.Application()
            _api.add_routes([aiohttp.web.get("/kek/", _resource)])
            return _api

    app = App({})
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert {
        "labels": {"metric_group": "rps", "path": "/kek/", "response_code": 200},
        "type": "RATE",
        "value": 1,
    } not in (await resp.json())["sensors"]
