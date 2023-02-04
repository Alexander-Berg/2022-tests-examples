import pytest

from maps_adv.geosmb.marksman.server.lib.exceptions import BizNotAdded

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(domain, dm):
    dm.list_business_segments_data.return_value = {
        "biz_id": 15,
        "permalink": 16,
        "counter_id": 17,
        "segments": [{"segment_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200}],
    }

    result = await domain.list_business_segments_data(biz_id=15)

    dm.list_business_segments_data.assert_awaited_with(biz_id=15)
    assert result == {
        "biz_id": 15,
        "permalink": 16,
        "counter_id": 17,
        "segments": [{"segment_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200}],
    }


async def test_raises_if_dm_returns_none(domain, dm):
    dm.list_business_segments_data.return_value = None

    with pytest.raises(BizNotAdded):
        await domain.list_business_segments_data(biz_id=15)
