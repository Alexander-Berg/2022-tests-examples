import pytest
from asyncpg.exceptions import CheckViolationError, NotNullViolationError

from maps_adv.adv_store.v2.lib.db.tables import week_schedule

pytestmark = [pytest.mark.asyncio]


async def test_create(db, week_schedule_data):
    await db.rw.execute(week_schedule.insert().values(week_schedule_data))
    row = await db.rw.fetch_one(week_schedule.select())
    assert isinstance(row["campaign_id"], int)


@pytest.mark.parametrize("column_name", ["start", "end"])
async def test_field_not_nullable(db, week_schedule_data, column_name):
    week_schedule_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(week_schedule.insert().values(week_schedule_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "start", "end"])
async def test_field_required(db, week_schedule_data, column_name):
    del week_schedule_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(week_schedule.insert().values(week_schedule_data))


@pytest.mark.parametrize("column_name", ["start", "end"])
async def test_field_not_negative(db, week_schedule_data, column_name):
    week_schedule_data[column_name] = -1

    with pytest.raises(CheckViolationError):
        await db.rw.execute(week_schedule.insert().values(week_schedule_data))


@pytest.mark.parametrize("column_name", ["start", "end"])
async def test_field_le_than_10080(db, week_schedule_data, column_name):
    week_schedule_data[column_name] = 10080 + 1

    with pytest.raises(CheckViolationError):
        await db.rw.execute(week_schedule.insert().values(week_schedule_data))


async def test_ok_if_end_eq_10080(db, campaign_id):
    week_schedule_data = {"campaign_id": campaign_id, "start": 0, "end": 10080}

    try:
        await db.rw.execute(week_schedule.insert().values(week_schedule_data))
    except CheckViolationError:
        pytest.fail()


async def test_start_less_than_end(db, week_schedule_data):
    week_schedule_data["start"] = 123
    week_schedule_data["end"] = 123

    with pytest.raises(CheckViolationError):
        await db.rw.execute(week_schedule.insert().values(week_schedule_data))
