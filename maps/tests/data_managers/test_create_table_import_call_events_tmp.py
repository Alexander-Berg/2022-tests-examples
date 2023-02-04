import pytest

pytestmark = [pytest.mark.asyncio]


async def test_creates_empty_table_if_not_exists(dm, con):
    await dm.create_table_import_call_events_tmp()

    rows = await con.fetch("SELECT * FROM import_call_events_tmp")
    assert rows == []


async def test_recreates_existed_table(dm, con):
    await con.execute(
        """
        CREATE TABLE import_call_events_tmp (id int8);
        INSERT INTO import_call_events_tmp VALUES (1);
        """
    )

    await dm.create_table_import_call_events_tmp()

    rows = await con.fetch("SELECT * FROM import_call_events_tmp")
    assert rows == []
