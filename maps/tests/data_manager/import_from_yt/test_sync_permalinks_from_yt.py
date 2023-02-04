import pytest

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [(51, 661)],
    [(52, 662)],
    [(53, 663)],
]


async def test_sync_permalinks(factory, dm):
    await factory.insert_biz_state(
        biz_id=51,
        slug="biz-51",
        permalink="551",
        stable_version=await factory.insert_landing_data(is_updated_from_geosearch=False)
    )
    await factory.insert_biz_state(
        biz_id=52,
        slug="biz-52",
        permalink="552",
        stable_version=await factory.insert_landing_data(is_updated_from_geosearch=True)
    )

    await dm.sync_permalinks_from_yt(AsyncIterator(input_data))

    assert "661" == (await factory.fetch_biz_state(51))["permalink"]
    assert "552" == (await factory.fetch_biz_state(52))["permalink"]
    assert None is await factory.fetch_biz_state(53)
