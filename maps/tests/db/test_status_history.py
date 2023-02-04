import pytest
from asyncpg.exceptions import CheckViolationError, NotNullViolationError

from maps_adv.adv_store.v2.lib.db.tables import status_history

pytestmark = [pytest.mark.asyncio]


async def test_create(db, status_history_data):
    await db.rw.execute(status_history.insert().values(status_history_data))
    row = await db.rw.fetch_one(status_history.select())
    assert isinstance(row["campaign_id"], int)


@pytest.mark.parametrize("column_name", ["campaign_id", "author_id", "status"])
async def test_column_required(db, status_history_data, column_name):
    del status_history_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(status_history.insert().values(status_history_data))


@pytest.mark.parametrize(
    "column_name", ["campaign_id", "author_id", "status", "changed_datetime"]
)
async def test_status_history_not_nullable(db, status_history_data, column_name):
    status_history_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(status_history.insert().values(status_history_data))


async def test_metadata_not_array(db, status_history_data):
    status_history_data["metadata"] = ["array"]

    with pytest.raises(CheckViolationError):
        await db.rw.execute(status_history.insert().values(status_history_data))


async def test_metadata_not_none(db, status_history_data):
    status_history_data["metadata"] = None

    with pytest.raises(CheckViolationError):
        await db.rw.execute(status_history.insert().values(status_history_data))
