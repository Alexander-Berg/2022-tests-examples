import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_200(api):
    response = await api.get("/ping")

    assert response.status == 200


async def test_returns_500_for_rw_connection_in_ro_mode(api, con):
    async with con.transaction():
        await con.execute("SET TRANSACTION READ ONLY")

        response = await api.get("/ping")

        assert response.status == 500


@pytest.mark.real_db
async def test_returns_500_if_pool_is_disconnected(api, db):
    await db.close()

    response = await api.get("/ping")

    assert response.status == 500
