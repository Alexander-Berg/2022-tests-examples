import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(domain, dm):
    dm.fetch_biz_state_by_slug.coro.return_value = {"some": "data"}

    await domain.check_slug_is_free(slug="cafe")

    dm.fetch_biz_state_by_slug.assert_called_with(slug="cafe")


async def test_return_true_if_no_biz_state_with_this_slug(domain, dm):
    dm.fetch_biz_state_by_slug.coro.return_value = None

    result = await domain.check_slug_is_free(slug="cafe")

    assert result is True


async def test_returns_false_if_biz_state_with_this_slug_exists(domain, dm):
    dm.fetch_biz_state_by_slug.coro.return_value = {"some": "data"}

    result = await domain.check_slug_is_free(slug="cafe")

    assert result is False


async def test_returns_false_if_biz_state_with_this_slug_exists_and_it_is_not_me(domain, dm):
    dm.check_slug_is_free.coro.return_value = False

    result = await domain.check_slug_is_free(slug="cafe", biz_id=1)

    assert result is False


async def test_returns_true_if_biz_state_with_this_slug_not_exists_and_it_is_not_me(domain, dm):
    dm.check_slug_is_free.coro.return_value = True

    result = await domain.check_slug_is_free(slug="cafe", biz_id=1)

    assert result is True
