import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId, SlugInUse, ToMuchSlugs

pytestmark = [pytest.mark.asyncio]


async def test_updates_slug(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    await dm.update_biz_state_slug(biz_id=15, slug="restoran")

    assert (await factory.fetch_biz_state(biz_id=15))["slug"] == "restoran"
    assert (await factory.fetch_aliases(biz_id=15))[0]["slug"] == "cafe"


async def test_updates_slug_allow_rollback(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    await dm.update_biz_state_slug(biz_id=15, slug="restoran")
    await dm.update_biz_state_slug(biz_id=15, slug="cafe")

    assert (await factory.fetch_biz_state(biz_id=15))["slug"] == "cafe"
    assert (await factory.fetch_aliases(biz_id=15))[0]["slug"] == "cafe"
    assert (await factory.fetch_aliases(biz_id=15))[1]["slug"] == "restoran"


async def test_updates_slug_to_much_rise_exception(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    await dm.update_biz_state_slug(biz_id=15, slug="cafe1")
    await dm.update_biz_state_slug(biz_id=15, slug="cafe2")
    await dm.update_biz_state_slug(biz_id=15, slug="cafe3")
    await dm.update_biz_state_slug(biz_id=15, slug="cafe4")
    await dm.update_biz_state_slug(biz_id=15, slug="cafe5")

    with pytest.raises(ToMuchSlugs):
        await dm.update_biz_state_slug(biz_id=15, slug="cafe6")


async def test_does_not_update_other_biz_id_slug(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await factory.insert_biz_state(biz_id=25, slug="sushi")

    await dm.update_biz_state_slug(biz_id=15, slug="restoran")

    assert (await factory.fetch_biz_state(biz_id=25))["slug"] == "sushi"


async def test_returns_nothing(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    result = await dm.update_biz_state_slug(biz_id=15, slug="restoran")

    assert result is None


async def test_raises_if_slug_in_use(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await factory.insert_biz_state(biz_id=25, slug="restoran")

    with pytest.raises(SlugInUse):
        await dm.update_biz_state_slug(biz_id=15, slug="restoran")


async def test_raises_if_slug_in_use_by_alias(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await dm.update_biz_state_slug(biz_id=15, slug="cafe2")
    await factory.insert_biz_state(biz_id=25, slug="restoran")

    with pytest.raises(SlugInUse):
        await dm.update_biz_state_slug(biz_id=25, slug="cafe")


async def test_raises_if_no_such_biz_id(factory, dm):
    await factory.insert_biz_state(biz_id=25, slug="restoran")

    with pytest.raises(NoDataForBizId):
        await dm.update_biz_state_slug(biz_id=15, slug="restoran")
