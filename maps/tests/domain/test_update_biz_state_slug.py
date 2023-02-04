import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId, SlugInUse

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(domain, dm):
    dm.update_biz_state_slug.coro.return_value = {"some": "data"}

    result = await domain.update_biz_state_slug(biz_id=15, slug="cafe")

    dm.update_biz_state_slug.assert_called_with(biz_id=15, slug="cafe")
    assert result == {"some": "data"}


@pytest.mark.parametrize("exc_cls", [NoDataForBizId, SlugInUse])
async def test_propagates_exceptions(domain, dm, exc_cls):
    dm.update_biz_state_slug.coro.side_effect = exc_cls

    with pytest.raises(exc_cls):
        await domain.update_biz_state_slug(biz_id=15, slug="cafe")
