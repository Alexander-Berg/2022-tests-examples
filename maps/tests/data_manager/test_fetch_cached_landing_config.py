import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_data(factory, dm):
    await factory.set_cached_landing_config(data={"k": "v"})

    result = await dm.fetch_cached_landing_config()

    assert result == {"k": "v"}


async def test_returns_empty_if_no_row(dm):
    result = await dm.fetch_cached_landing_config()

    assert result == {}
