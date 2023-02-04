import pytest

from maps_adv.geosmb.example.server import make_api

pytestmark = [pytest.mark.asyncio]


async def test_ping(aiohttp_client):
    client = await aiohttp_client(make_api())

    resp = await client.get("/ping")

    assert resp.status == 200


@pytest.mark.parametrize("data", (b"", b"azaza"))
async def test_post_echo(data, aiohttp_client):
    client = await aiohttp_client(make_api())

    resp = await client.post("/echo/", data=data)

    assert resp.status == 200
    assert await resp.read() == data


async def test_get_echo(aiohttp_client):
    client = await aiohttp_client(make_api())

    resp = await client.get("/echo/")

    assert resp.status == 200
    assert await resp.read() == b""


async def test_error(aiohttp_client):
    client = await aiohttp_client(make_api())

    resp = await client.get("/error/")

    assert resp.status == 500
