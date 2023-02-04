import json

import pytest

from maps_adv.common.pg_engine import DB, UnexpectedTransactionMode

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("pg_engine_wait_for_db")]


class DBWithCodecs(DB):
    async def _set_codecs(con):
        await con.set_type_codec(
            "json", encoder=json.dumps, decoder=json.loads, schema="pg_catalog"
        )


@pytest.fixture
def make_engine(pg_engine_database_url):
    async def _make(**kwargs):
        return await DBWithCodecs.create(
            pg_engine_database_url, pg_engine_database_url, **kwargs
        )

    return _make


async def test_will_use_one_pool_if_ro_is_not_specified(pg_engine_database_url):
    db = await DB.create(pg_engine_database_url)

    assert db._rw == db._ro

    await db.close()


async def test_will_use_different_pools_if_ro_is_specified(pg_engine_database_url):
    db = await DB.create(pg_engine_database_url, pg_engine_database_url)

    assert db._rw != db._ro

    await db.close()


async def test_returns_new_con_on_every_acquire(make_engine):
    db = await make_engine()

    async with db.acquire() as con_0:
        async with db.acquire() as con_1:
            assert con_0 != con_1

    await db.close()


async def test_returns_the_same_con_when_configured(make_engine):
    db = await make_engine(use_single_connection=True)

    async with db.acquire() as con_0:
        async with db.acquire() as con_1:
            assert con_0 == con_1

    await db.release(con_0, force=True)
    await db.release(con_1, force=True)
    await db.close()


async def test_did_not_raise_for_valid_connection(make_engine):
    db = await make_engine(use_single_connection=True)
    _con = await db.acquire()

    await db.check_pools()

    await db.release(_con, force=True)
    await db.close()


async def test_raises_for_rw_connection_in_ro_mode(make_engine):
    db = await make_engine(use_single_connection=True)

    async with db.acquire() as con:
        async with con.transaction():
            await con.execute("SET TRANSACTION READ ONLY")

            with pytest.raises(UnexpectedTransactionMode):
                await db.check_pools()

    await db.release(con, force=True)
    await db.close()


async def test_raises_if_pool_is_disconnected(make_engine):
    db = await make_engine()

    await db.close()

    with pytest.raises(Exception):
        await db.check_pools()


async def test_user_defined_codecs_are_set(make_engine):
    db = await make_engine(use_single_connection=True)

    async with db.acquire() as con:
        got = await con.fetchval("SELECT $1::json", {"a": "a"})
        assert got == {"a": "a"}

    await db.release(con, force=True)
    await db.close()
