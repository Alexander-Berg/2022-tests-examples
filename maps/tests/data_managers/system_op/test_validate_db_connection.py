import pytest

from maps_adv.stat_controller.server.lib.data_managers import UnexpectedTransactionMode

pytestmark = [pytest.mark.asyncio]


async def test_did_not_raise_for_valid_connection(system_op_dm):
    await system_op_dm.validate_db_connection()


async def test_raises_for_rw_connection_in_ro_mode(system_op_dm, con):
    async with con.transaction():
        await con.execute("SET TRANSACTION READ ONLY")

        with pytest.raises(UnexpectedTransactionMode):
            await system_op_dm.validate_db_connection()


@pytest.mark.real_db
async def test_raises_if_pool_is_disconnected(system_op_dm, db):
    await db.close()

    with pytest.raises(Exception):
        await system_op_dm.validate_db_connection()
