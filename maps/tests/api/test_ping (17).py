import pytest

pytestmark = [pytest.mark.asyncio]

url = "/ping"


async def test_returns_200(api):
    await api.get(url, expected_status=200)


@pytest.mark.real_db
async def test_returns_500_if_pool_is_disconnected(db, api):
    await db.close()

    await api.get(url, expected_status=500)
