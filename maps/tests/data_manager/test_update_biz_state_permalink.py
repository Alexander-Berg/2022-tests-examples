import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId

pytestmark = [pytest.mark.asyncio]


async def test_updates_permalink(factory, dm):
    await factory.insert_biz_state(biz_id=15, permalink="12325")

    await dm.update_biz_state_permalink(biz_id=15, permalink="54321")

    assert (await factory.fetch_biz_state(biz_id=15))["permalink"] == "54321"


async def test_does_not_update_other_biz_id_slug(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe", permalink="12325")
    await factory.insert_biz_state(biz_id=25, slug="necafe", permalink="54321")

    await dm.update_biz_state_permalink(biz_id=15, permalink="87654")

    assert (await factory.fetch_biz_state(biz_id=25))["permalink"] == "54321"


async def test_returns_nothing(factory, dm):
    await factory.insert_biz_state(biz_id=15, permalink="12325")

    result = await dm.update_biz_state_permalink(biz_id=15, permalink="54321")

    assert result is None


async def test_raises_if_no_such_biz_id(factory, dm):
    with pytest.raises(NoDataForBizId):
        await dm.update_biz_state_permalink(biz_id=15, permalink="12325")
