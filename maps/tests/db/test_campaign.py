import datetime

import pytest
from asyncpg.exceptions import CheckViolationError, NotNullViolationError

from maps_adv.adv_store.v2.lib.db.tables import campaign

pytestmark = [pytest.mark.asyncio]


async def test_create(db, db_campaign_data):
    await db.rw.execute(campaign.insert().values(db_campaign_data))
    row = await db.rw.fetch_one(campaign.select())
    assert isinstance(row["id"], int)


@pytest.mark.parametrize(
    "column_name", ["order_id", "user_display_limit", "user_daily_display_limit"]
)
async def test_raises_for_negative(db, db_campaign_data, column_name):
    db_campaign_data[column_name] = -1

    with pytest.raises(CheckViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


@pytest.mark.parametrize(
    "column_name",
    [
        "author_id",
        "name",
        "publication_envs",
        "campaign_type",
        "start_datetime",
        "end_datetime",
        "comment",
        "timezone",
        "billing_id",
        "platforms",
        "changed_datetime",
    ],
)
async def test_column_not_nullable(db, db_campaign_data, column_name):
    db_campaign_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


@pytest.mark.parametrize(
    "column_name",
    [
        "order_id",
        "user_display_limit",
        "user_daily_display_limit",
        "rubric",
        "display_probability",
        "display_probability_auto",
    ],
)
async def test_nullable_columns_can_be_missed(db, db_campaign_data, column_name):
    db_campaign_data[column_name] = None

    await db.rw.execute(campaign.insert().values(db_campaign_data))


@pytest.mark.parametrize(
    "column_name",
    [
        "author_id",
        "name",
        "publication_envs",
        "campaign_type",
        "start_datetime",
        "end_datetime",
        "timezone",
        "billing_id",
        "platforms",
    ],
)
async def test_column_required(db, db_campaign_data, column_name):
    del db_campaign_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


async def test_targeting_is_not_json_object(db, db_campaign_data):
    db_campaign_data["targeting"] = ["array"]

    with pytest.raises(CheckViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


@pytest.mark.parametrize("column_name", ["publication_envs", "platforms"])
async def test_column_not_empty(db, db_campaign_data, column_name):
    db_campaign_data[column_name] = []

    with pytest.raises(CheckViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


async def test_start_datetime_earlier_than_end_datetime(db, db_campaign_data):
    db_campaign_data["start_datetime"] = db_campaign_data[
        "end_datetime"
    ] + datetime.timedelta(days=1)

    with pytest.raises(CheckViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


@pytest.mark.parametrize("column_name", ["name", "timezone"])
async def test_column_str_not_epmty(db, db_campaign_data, column_name):
    db_campaign_data[column_name] = ""

    with pytest.raises(CheckViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


async def test_comment_default(db, db_campaign_data):
    del db_campaign_data["comment"]

    await db.rw.execute(campaign.insert().values(db_campaign_data))

    row = await db.rw.fetch_one(campaign.select())
    assert row["comment"] == ""


async def test_targeting_default(db, db_campaign_data):
    del db_campaign_data["targeting"]

    await db.rw.execute(campaign.insert().values(db_campaign_data))

    row = await db.rw.fetch_one(campaign.select())
    assert row["targeting"] == {}


async def test_changed_datetime_default(db, db_campaign_data):
    del db_campaign_data["changed_datetime"]

    await db.rw.execute(campaign.insert().values(db_campaign_data))

    row = await db.rw.fetch_one(campaign.select())
    assert isinstance(row["changed_datetime"], datetime.datetime)


@pytest.mark.parametrize(
    "column_name, max_value",
    [("display_probability", 1.0), ("display_probability_auto", 1.0)],
)
async def test_raises_if_value_greater_than_max(
    db, db_campaign_data, column_name, max_value
):
    db_campaign_data[column_name] = max_value + 0.01

    with pytest.raises(CheckViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


async def test_manul_order_id_can_be_missed(db, db_campaign_data):
    db_campaign_data["manul_order_id"] = None

    await db.rw.execute(campaign.insert().values(db_campaign_data))


async def test_raises_for_negative_manul_order_id(db, db_campaign_data):
    db_campaign_data["manul_order_id"] = -1

    with pytest.raises(CheckViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))


async def test_manul_order_id_can_be_set(db, db_campaign_data):
    db_campaign_data["order_id"] = None
    db_campaign_data["manul_order_id"] = 11

    await db.rw.execute(campaign.insert().values(db_campaign_data))


async def test_raises_if_both_order_fields_setup(db, db_campaign_data):
    db_campaign_data["manul_order_id"] = 11

    with pytest.raises(CheckViolationError):
        await db.rw.execute(campaign.insert().values(db_campaign_data))
