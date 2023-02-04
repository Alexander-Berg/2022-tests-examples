import pytest

from maps_adv.adv_store.v2.lib.db import DB

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def real_db(config):
    db = await DB.create(config["DATABASE_URL"])
    yield db
    await db.close()


async def test_did_not_raise_for_valid_connection(db):
    await db.healthcheck()


async def test_raises_if_pool_is_disconnected(real_db, con):
    await real_db.close()

    with pytest.raises(Exception):
        await real_db.healthcheck()
