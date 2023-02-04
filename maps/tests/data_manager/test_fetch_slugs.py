import pytest

pytestmark = [pytest.mark.asyncio]


async def test_fetch_published_slugs(factory, dm):
    await factory.insert_biz_state(biz_id=1, slug="s1", published=True)
    await factory.insert_biz_state(biz_id=2, slug="s2", published=True)
    await factory.insert_biz_state(biz_id=3, slug="s3", published=True)
    await factory.insert_biz_state(biz_id=4, slug="s4", published=False)

    assert set(await dm.fetch_published_slugs(offset=0, limit=2)) == {"s1", "s2"}
    assert set(await dm.fetch_published_slugs(offset=2, limit=4)) == {"s3"}
