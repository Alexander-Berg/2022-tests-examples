import pytest

pytestmark = [pytest.mark.asyncio]


async def test_ping(client):
    assert (await client.get("/ping")).status == 200
