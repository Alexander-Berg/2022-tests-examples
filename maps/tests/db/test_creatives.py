import pytest
from asyncpg.exceptions import CheckViolationError, NotNullViolationError

from maps_adv.adv_store.v2.lib.db.tables import (
    banner,
    billboard,
    icon,
    logo_and_text,
    pin,
    pin_search,
    text,
)

pytestmark = [pytest.mark.asyncio]


async def test_create_pin(db, pin_data):
    await db.rw.execute(pin.insert().values(pin_data))
    row = await db.rw.fetch_one(pin.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_icon(db, icon_data):
    await db.rw.execute(icon.insert().values(icon_data))
    row = await db.rw.fetch_one(icon.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_pin_search(db, pin_search_data):
    await db.rw.execute(pin_search.insert().values(pin_search_data))
    row = await db.rw.fetch_one(pin_search.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_text(db, text_data):
    await db.rw.execute(text.insert().values(text_data))
    row = await db.rw.fetch_one(text.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_banner(db, banner_data):
    await db.rw.execute(banner.insert().values(banner_data))
    row = await db.rw.fetch_one(banner.select())
    assert isinstance(row["campaign_id"], int)


async def test_create_billboard(db, billboard_data):
    await db.rw.execute(billboard.insert().values(billboard_data))
    row = await db.rw.fetch_one(billboard.select())
    assert isinstance(row["campaign_id"], int)


@pytest.mark.parametrize("column_name", ["campaign_id", "images"])
async def test_billboard_column_required(db, billboard_data, column_name):
    del billboard_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(billboard.insert().values(billboard_data))


async def test_billboard_campaign_id_not_null(db, billboard_data):
    billboard_data["campaign_id"] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(billboard.insert().values(billboard_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "images", "title", "subtitle"])
async def test_pin_column_required(db, pin_data, column_name):
    del pin_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(pin.insert().values(pin_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "title", "subtitle"])
async def test_pin_column_not_null(db, pin_data, column_name):
    pin_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(pin.insert().values(pin_data))


@pytest.mark.parametrize(
    "column_name", ["campaign_id", "images", "disclaimer", "show_ads_label"]
)
async def test_banner_column_required(db, banner_data, column_name):
    del banner_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(banner.insert().values(banner_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "disclaimer", "show_ads_label"])
async def test_banner_column_not_null(db, banner_data, column_name):
    banner_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(banner.insert().values(banner_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "text", "disclaimer"])
async def test_text_column_required(db, text_data, column_name):
    del text_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(text.insert().values(text_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "text", "disclaimer"])
async def test_text_column_not_null(db, text_data, column_name):
    text_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(text.insert().values(text_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "images", "text"])
async def test_logo_and_text_column_required(db, logo_and_text_data, column_name):
    del logo_and_text_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(logo_and_text.insert().values(logo_and_text_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "text"])
async def test_logo_and_text_not_null(db, logo_and_text_data, column_name):
    logo_and_text_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(logo_and_text.insert().values(logo_and_text_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "images", "title", "position"])
async def test_icon_column_required(db, icon_data, column_name):
    del icon_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(icon.insert().values(icon_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "title", "position"])
async def test_icon_column_not_null(db, icon_data, column_name):
    icon_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(icon.insert().values(icon_data))


@pytest.mark.parametrize(
    "column_name", ["campaign_id", "images", "title", "organizations"]
)
async def test_pin_search_column_required(db, pin_search_data, column_name):
    del pin_search_data[column_name]

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(pin_search.insert().values(pin_search_data))


@pytest.mark.parametrize("column_name", ["campaign_id", "title", "organizations"])
async def test_pin_search_column_not_null(db, pin_search_data, column_name):
    pin_search_data[column_name] = None

    with pytest.raises(NotNullViolationError):
        await db.rw.execute(pin_search.insert().values(pin_search_data))


@pytest.mark.parametrize("type_", [None, {"obj": "ect"}])
async def test_billboard_images_not_a_type(db, billboard_data, type_):
    billboard_data["images"] = type_

    with pytest.raises(CheckViolationError):
        await db.rw.execute(billboard.insert().values(billboard_data))


@pytest.mark.parametrize("type_", [None, {"obj": "ect"}])
async def test_pin_images_not_a_type(db, pin_data, type_):
    pin_data["images"] = type_

    with pytest.raises(CheckViolationError):
        await db.rw.execute(pin.insert().values(pin_data))


@pytest.mark.parametrize("type_", [None, {"obj": "ect"}])
async def test_banner_images_not_a_type(db, banner_data, type_):
    banner_data["images"] = type_

    with pytest.raises(CheckViolationError):
        await db.rw.execute(banner.insert().values(banner_data))


@pytest.mark.parametrize("type_", [None, {"obj": "ect"}])
async def test_icon_images_not_a_type(db, icon_data, type_):
    icon_data["images"] = type_

    with pytest.raises(CheckViolationError):
        await db.rw.execute(icon.insert().values(icon_data))


@pytest.mark.parametrize("type_", [None, {"obj": "ect"}])
async def test_pin_search_images_not_a_type(db, pin_search_data, type_):
    pin_search_data["images"] = type_

    with pytest.raises(CheckViolationError):
        await db.rw.execute(pin_search.insert().values(pin_search_data))


async def test_pin_search_organizations_not_empty(db, pin_search_data):
    pin_search_data["organizations"] = []

    with pytest.raises(CheckViolationError):
        await db.rw.execute(pin_search.insert().values(pin_search_data))
