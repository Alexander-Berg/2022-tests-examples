import pytest
from asyncpg.exceptions import CheckViolationError, NotNullViolationError

from maps_adv.adv_store.v2.lib.db.tables import (
    download_app,
    open_site,
    phone_call,
    search,
)

pytestmark = [pytest.mark.asyncio]


async def test_create_search(db, search_data):
    await db.rw.execute(search.insert().values(search_data))
    row = await db.rw.fetch_one(search.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_phone_call(db, phone_call_data):
    await db.rw.execute(phone_call.insert().values(phone_call_data))
    row = await db.rw.fetch_one(phone_call.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_download_app(db, download_app_data):
    await db.rw.execute(download_app.insert().values(download_app_data))
    row = await db.rw.fetch_one(download_app.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_open_site(db, open_site_data):
    await db.rw.execute(open_site.insert().values(open_site_data))
    row = await db.rw.fetch_one(open_site.select())
    assert isinstance(row["campaign_id"], int)


@pytest.mark.parametrize("column_name", ["campaign_id", "url"])
async def test_open_site_column_required(db, open_site_data, column_name):
    del open_site_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(open_site.insert().values(open_site_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "url"])
async def test_open_site_column_not_null(db, open_site_data, column_name):
    open_site_data["campaign_id"] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(open_site.insert().values(open_site_data))


@pytest.mark.parametrize(
    "column_name", ["campaign_id", "organizations", "history_text"]
)
async def test_search_column_required(db, search_data, column_name):
    del search_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(search.insert().values(search_data))


@pytest.mark.parametrize(
    "column_name", ["campaign_id", "organizations", "history_text"]
)
async def test_search_column_not_null(db, search_data, column_name):
    search_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(search.insert().values(search_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "phone"])
async def test_phone_call_column_required(db, phone_call_data, column_name):
    del phone_call_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(phone_call.insert().values(phone_call_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "phone"])
async def test_phone_call_column_not_null(db, phone_call_data, column_name):
    phone_call_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(phone_call.insert().values(phone_call_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "url"])
async def test_download_app_column_required(db, download_app_data, column_name):
    del download_app_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(download_app.insert().values(download_app_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "url"])
async def test_download_app_column_not_null(db, download_app_data, column_name):
    download_app_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(download_app.insert().values(download_app_data))


async def test_search_organizations_not_empty(db, search_data):
    search_data["organizations"] = []

    with pytest.raises(CheckViolationError):
        await db.rw.execute(search.insert().values(search_data))


async def test_search_organizations_each_positive(db, search_data):
    search_data["organizations"][0] = 0

    with pytest.raises(CheckViolationError):
        await db.rw.execute(search.insert().values(search_data))
