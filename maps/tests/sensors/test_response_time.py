import asyncio
from copy import deepcopy

import aiohttp
import pytest

from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]

url = "/sensors/"


@pytest.fixture
def example_metric():
    return deepcopy(_example_metrics)


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
async def test_labels_with_path_and_code(path, response_code, example_metric, run):
    app = make_app(path, response_code)
    client = await run(app)

    await client.get(path)
    resp = await client.get(url)

    example_metric["labels"]["path"] = path
    example_metric["labels"]["response_code"] = response_code
    example_metric["hist"]["buckets"][0] = 1
    assert example_metric in (await resp.json())["sensors"]


@pytest.mark.parametrize("count", range(1, 5))
async def test_counts_all_requests(count, example_metric, run):
    app = make_app("/kek/", 200)
    client = await run(app)

    for _ in range(count):
        await client.get("/kek/")
    resp = await client.get(url)

    example_metric["hist"]["buckets"][0] = count
    assert example_metric in (await resp.json())["sensors"]


@pytest.mark.parametrize(
    "time, bucket_index", ([0.19, 0], [0.2, 1], [0.65, 3], [2, 10])
)
async def test_uses_expected_buckets(time, bucket_index, example_metric, run):
    app = make_app("/kek/", 200, wait=time)
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    example_metric["hist"]["buckets"][bucket_index] = 1
    assert example_metric in (await resp.json())["sensors"]


@pytest.mark.parametrize(
    "resource_path, request_url, expected",
    (
        ["/sample/{name}/", "/sample/yopta/", "/sample/{name}/"],
        [r"/{number:\d+}/", "/100500/", "/{number}/"],
        [r"/{name}/{number:\d+}/", "/yopta/100500/", "/{name}/{number}/"],
    ),
)
async def test_dynamic_routes_are_logged_well(
    resource_path, request_url, expected, example_metric, run
):
    app = make_app(resource_path, 200)
    client = await run(app)

    await client.get(request_url)
    resp = await client.get(url)

    example_metric["labels"]["path"] = expected
    example_metric["hist"]["buckets"][0] = 1
    assert example_metric in (await resp.json())["sensors"]


async def test_unknown_routes_are_not_logged(run):
    app = make_app("/lol/", 200)
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    assert await resp.json() == {"sensors": []}


async def test_server_errors_are_logged(example_metric, run):
    app = make_app("/kek/", 200, exc=Exception("Unexpected error"))
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    example_metric["labels"]["response_code"] = 500
    example_metric["hist"]["buckets"][0] = 1
    assert example_metric in (await resp.json())["sensors"]


async def test_cancelled_requests_are_logged(example_metric, run):
    app = make_app("/kek/", 200, wait=1)
    client = await run(app)

    with pytest.raises(asyncio.TimeoutError):
        await asyncio.wait_for(client.get("/kek/"), 0.3)
    resp = await client.get(url)

    example_metric["labels"]["response_code"] = "closed_by_client"
    example_metric["hist"]["buckets"][1] = 1
    assert example_metric in (await resp.json())["sensors"]


async def test_not_logged_if_sensor_not_defined(run, example_metric):
    async def _resource(*args, **kwargs):
        return aiohttp.web.Response(status=200)

    class App(Lasagna):

        SENSORS = Lasagna.SENSORS.copy()
        SENSORS.pop("response_time")

        async def _setup_layers(self, db):
            _api = aiohttp.web.Application()
            _api.add_routes([aiohttp.web.get("/kek/", _resource)])
            return _api

    app = App({})
    client = await run(app)

    await client.get("/kek/")
    resp = await client.get(url)

    example_metric["hist"]["buckets"][0] = 1
    assert example_metric not in (await resp.json())["sensors"]


_example_metrics = {
    "labels": {"path": "/kek/", "response_code": 200, "metric_group": "response_time"},
    "type": "HIST_RATE",
    "hist": {"bounds": list(range(200, 10200, 200)), "buckets": [0] * 50, "inf": 0},
}
