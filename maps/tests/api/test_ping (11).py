import pytest

pytestmark = [pytest.mark.asyncio]

url = "/ping"


async def test_returns_204(api):
    await api.get(url, expected_status=204)


async def test_returns_500_for_broken_engine(api, db):
    db.pools[0].rating = 0.0

    await api.get(url, expected_status=500)


@pytest.mark.real_db
async def test_returns_500_if_pool_is_disconnected(api, db):
    await db.close()

    await api.get(url, expected_status=500)
