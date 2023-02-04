import pytest

pytestmark = [pytest.mark.asyncio]


async def test_updates_permalink(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe", permalink="1234567")
    await factory.insert_biz_state(biz_id=16, slug="cafe2", permalink="1234567")

    await dm.update_permalink_from_geosearch(
        old_permalink="1234567", new_permalink="7654321"
    )

    assert (await factory.fetch_biz_state(biz_id=15))["permalink"] == "7654321"
    assert (await factory.fetch_biz_state(biz_id=16))["permalink"] == "7654321"


async def test_does_not_affect_other_permalinks(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe", permalink="1234567")
    await factory.insert_biz_state(biz_id=16, slug="cafe2", permalink="2345678")

    await dm.update_permalink_from_geosearch(
        old_permalink="1234567", new_permalink="7654321"
    )

    assert (await factory.fetch_biz_state(biz_id=15))["permalink"] == "7654321"
    assert (await factory.fetch_biz_state(biz_id=16))["permalink"] == "2345678"
