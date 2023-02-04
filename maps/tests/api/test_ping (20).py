import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def client(app, aiohttp_client):
    return await aiohttp_client(app.api)


async def test_returns_200(client):
    response = await client.get("/ping")

    assert response.status == 200
