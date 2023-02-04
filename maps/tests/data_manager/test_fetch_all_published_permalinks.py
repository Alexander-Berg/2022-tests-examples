import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_published_permalinks(factory, dm):
    id1 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=1, slug="cafe1", permalink="111111", published=True, stable_version=id1
    )
    id2 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=2, slug="cafe2", permalink="222222", stable_version=id2
    )
    id3 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=3, slug="cafe3", permalink="333333", published=True, stable_version=id3
    )

    result = await dm.fetch_all_published_permalinks()

    assert sorted(result) == ["111111", "333333"]


async def test_returns_no_duplicates(factory, dm):
    id1 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=1, slug="cafe1", permalink="111111", published=True, stable_version=id1
    )
    id2 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=2, slug="cafe2", permalink="111111", stable_version=id2
    )
    id3 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=3, slug="cafe3", permalink="111111", published=True, stable_version=id3
    )

    result = await dm.fetch_all_published_permalinks()

    assert result == ["111111"]


async def test_respects_offset_and_limit(factory, dm):
    id1 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=1, slug="cafe1", permalink="111111", published=True, stable_version=id1
    )
    id2 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=2, slug="cafe2", permalink="222222", published=True, stable_version=id2
    )
    id3 = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=3, slug="cafe3", permalink="333333", published=True, stable_version=id3
    )

    result = await dm.fetch_all_published_permalinks(1, 1)

    assert result == ["222222"]
