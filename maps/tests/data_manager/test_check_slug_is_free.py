import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_false_if_i_try_to_find_exists_slug(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    result = await dm.check_slug_is_free(biz_id=14, slug="cafe")

    assert result is False


async def test_returns_false_if_i_try_to_find_exists_slug_and_alias(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe1")
    await dm.update_biz_state_slug(biz_id=15, slug="cafe2")
    result1 = await dm.check_slug_is_free(biz_id=14, slug="cafe1")
    result2 = await dm.check_slug_is_free(biz_id=14, slug="cafe2")
    assert result2 is False
    assert result1 is False


async def test_returns_true_if_i_try_to_find_free_slug_and_alias(factory, dm):
    await factory.insert_biz_state(biz_id=15, slug="cafe1")
    await dm.update_biz_state_slug(biz_id=15, slug="cafe2")
    result = await dm.check_slug_is_free(biz_id=14, slug="cafe3")
    assert result is True
