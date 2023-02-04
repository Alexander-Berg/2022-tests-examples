import pytest

from maps_adv.geosmb.landlord.server.lib.enums import Feature

pytestmark = [pytest.mark.asyncio]


async def test_returns_data(factory, dm):
    await factory.set_cached_landing_config(
        data={"features": {Feature.TEST.value: "value"}}
    )

    result = await dm.fetch_cached_landing_config_feature(Feature.TEST)

    assert result == "value"


async def test_returns_empty_if_no_row(dm):
    result = await dm.fetch_cached_landing_config_feature(Feature.TEST)

    assert result is None
