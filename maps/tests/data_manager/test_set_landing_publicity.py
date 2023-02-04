import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("old_published", [True, False])
@pytest.mark.parametrize("new_published", [True, False])
async def test_sets_publicity_data(factory, dm, old_published, new_published):
    await factory.insert_biz_state(biz_id=15, slug="cafe", published=old_published)

    await dm.set_landing_publicity(biz_id=15, is_published=new_published)

    state = await factory.fetch_biz_state(biz_id=15)
    assert state["published"] == new_published


async def test_returns_nothing(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    result = await dm.set_landing_publicity(biz_id=15, is_published=True)

    assert result is None
