import pytest

pytestmark = [pytest.mark.asyncio]


async def test_creates_cached_landing_config(factory, dm):
    await dm.set_cached_landing_config({"k": "v"})

    assert await factory.fetch_cached_landing_config() == {"k": "v"}


async def test_updates_cached_landing_config(factory, dm):
    await factory.set_cached_landing_config({"k": "before"})

    await dm.set_cached_landing_config({"k": "after"})

    assert await factory.fetch_cached_landing_config() == {"k": "after"}
