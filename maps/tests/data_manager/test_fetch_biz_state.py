import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_biz_state_by_biz_id(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    result = await dm.fetch_biz_state(biz_id=15)

    assert result == {
        "biz_id": 15,
        "slug": "cafe",
        "permalink": "54321",
        "stable_version": None,
        "unstable_version": None,
        "published": False,
        "blocked": False,
    }


async def test_returns_biz_state_by_slug(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    result = await dm.fetch_biz_state_by_slug(slug="cafe")

    assert result == {
        "biz_id": 15,
        "slug": "cafe",
        "permalink": "54321",
        "stable_version": None,
        "unstable_version": None,
        "published": False,
        "blocked": False,
    }


async def test_returns_none_if_no_state_for_biz_id(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    result = await dm.fetch_biz_state(biz_id=22)

    assert result is None


async def test_returns_none_if_no_state_for_slug(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    result = await dm.fetch_biz_state_by_slug(slug="restoran")

    assert result is None


async def test_raises_if_biz_state_are_none(factory, dm):
    with pytest.raises(ValueError):
        await dm.fetch_biz_state(biz_id=None)


async def test_raises_if_slug_are_none(factory, dm):
    with pytest.raises(ValueError):
        await dm.fetch_biz_state_by_slug(slug=None)
