import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(domain, dm):
    await domain.delete_landing_by_biz_id(biz_id=15)

    dm.delete_landing_by_biz_id.assert_called_with(biz_id=15)


async def test_propagates_exception(domain, dm):
    dm.delete_landing_by_biz_id.coro.side_effect = NoDataForBizId

    with pytest.raises(NoDataForBizId):
        await domain.delete_landing_by_biz_id(biz_id=15)
