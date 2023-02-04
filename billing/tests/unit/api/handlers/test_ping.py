import pytest


@pytest.mark.asyncio
async def test_ping(app):
    r = await app.get("/ping")
    assert r.status == 200
