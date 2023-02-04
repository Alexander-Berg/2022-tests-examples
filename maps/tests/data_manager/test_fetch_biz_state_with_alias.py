import pytest
from datetime import datetime, timedelta

pytestmark = [pytest.mark.asyncio]


async def test_returns_biz_state_by_slug(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe_new")
    await factory.insert_slug_alias(biz_id=15, slug="cafe_old")

    result = await dm.fetch_biz_state_by_slug(slug="cafe_old")

    assert result == {
        "biz_id": 15,
        "slug": "cafe_old",
        "permalink": "54321",
        "stable_version": None,
        "unstable_version": None,
        "published": False,
        "blocked": False,
    }


async def test_returns_none_if_slug_not_in_biz_state_and_not_in_aliases(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await factory.insert_slug_alias(biz_id=15, slug="cafe_old")

    result = await dm.fetch_biz_state_by_slug(slug="pushkin")

    assert result is None


async def test_returns_none_if_slug_alias_expired(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await factory.insert_slug_alias(biz_id=15, slug="cafe_old", expiration_date=datetime.now() - timedelta(days=1))

    result = await dm.fetch_biz_state_by_slug(slug="cafe_old")

    assert result is None
