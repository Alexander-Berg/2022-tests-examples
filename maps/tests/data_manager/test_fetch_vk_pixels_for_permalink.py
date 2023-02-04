import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_data(factory, dm):
    await factory.create_vk_pixels(54321, [{"id": "QQQ", "goals": {"goal1": "WERT"}}])

    result = await dm.fetch_vk_pixels_for_permalink(54321)

    assert result == [{"id": "QQQ", "goals": {"goal1": "WERT"}}]


async def test_returns_empty_if_no_row(dm):
    result = await dm.fetch_vk_pixels_for_permalink(78901)

    assert result is None
