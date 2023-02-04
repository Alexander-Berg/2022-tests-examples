import pytest


@pytest.mark.asyncio
async def test_ping(app):
    r = await app.get('/ping')
    assert r.status == 200


@pytest.mark.asyncio
async def test_pingdb(app):
    r = await app.get('/pingdb')
    assert r.status == 200
