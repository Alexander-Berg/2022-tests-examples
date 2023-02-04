import pytest


pytestmark = pytest.mark.asyncio


async def test_get_purposes(f, client):
    await f.create_purpose()
    response = await client.get('api/purposes/')
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1
