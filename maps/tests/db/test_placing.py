import pytest
from asyncpg.exceptions import (
    CheckViolationError,
    NotNullViolationError,
    UniqueViolationError,
)

from maps_adv.adv_store.v2.lib.db.tables import area, organizations

pytestmark = [pytest.mark.asyncio]


async def test_create_organizations(db, organizations_data):
    await db.rw.execute(organizations.insert().values(organizations_data))
    row = await db.rw.fetch_one(organizations.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_area(db, area_data):
    await db.rw.execute(area.insert().values(area_data))
    row = await db.rw.fetch_one(area.select())
    assert isinstance(row["campaign_id"], int)


async def test_permalinks_not_nullable(db, organizations_data):
    organizations_data["permalinks"] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(organizations.insert().values(organizations_data))


async def test_area_version_not_nullable(db, area_data):
    area_data["version"] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(area.insert().values(area_data))


@pytest.mark.parametrize("column_name", ["permalinks", "campaign_id"])
async def test_permalinks_required(db, organizations_data, column_name):
    del organizations_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(organizations.insert().values(organizations_data))


@pytest.mark.parametrize("column_name", ["areas", "version", "campaign_id"])
async def test_area_field_required(db, area_data, column_name):
    del area_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(area.insert().values(area_data))


async def test_area_areas_is_not_json_object(db, area_data):
    area_data["areas"] = {"obj": "ect"}

    with pytest.raises(CheckViolationError):
        await db.rw.execute(area.insert().values(area_data))


async def test_permalinks_not_empty(db, organizations_data):
    organizations_data["permalinks"] = []

    with pytest.raises(CheckViolationError):
        await db.rw.execute(organizations.insert().values(organizations_data))


async def test_permalinks_each_positive(db, organizations_data):
    organizations_data["permalinks"][0] = 0

    with pytest.raises(CheckViolationError):
        await db.rw.execute(organizations.insert().values(organizations_data))


async def test_area_version_not_negative(db, area_data):
    area_data["version"] = -1

    with pytest.raises(CheckViolationError):
        await db.rw.execute(area.insert().values(area_data))


async def test_area_campaign_id_is_unique(db, area_data):
    await db.rw.execute(area.insert().values(area_data))

    with pytest.raises(UniqueViolationError):
        await db.rw.execute(area.insert().values(area_data))


async def test_organizations_campaign_id_is_unique(db, organizations_data):
    await db.rw.execute(organizations.insert().values(organizations_data))

    with pytest.raises(UniqueViolationError):
        await db.rw.execute(organizations.insert().values(organizations_data))
