import pytest


@pytest.mark.asyncio
async def test_ping_200(api_client):
    resp = await api_client.get("/ping")

    assert resp.status == 200
