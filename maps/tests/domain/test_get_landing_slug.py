import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import NoBizIdForOrg, NoDataForBizId

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_data(domain, dm):
    dm.fetch_biz_state.coro.return_value = {
        "id": 15,
        "permalink": "123456",
        "published": True,
        "slug": "slug",
    }

    result = await domain.get_landing_slug(permalink=123456)

    assert result == "slug"


async def test_raises_if_no_biz_id_for_permalink(domain, bvm):
    bvm.fetch_biz_id_no_create_by_permalink.coro.return_value = None

    with pytest.raises(NoBizIdForOrg):
        await domain.get_landing_slug(permalink=123456)


async def test_raises_if_no_biz_state(domain, dm):
    dm.fetch_biz_state.coro.return_value = None

    with pytest.raises(NoDataForBizId):
        await domain.get_landing_slug(permalink=123456)


async def test_raises_if_not_published(domain, dm):
    dm.fetch_biz_state.coro.return_value = {
        "id": 15,
        "permalink": "123456",
        "published": False,
        "slug": "slug",
    }

    with pytest.raises(NoDataForBizId):
        await domain.get_landing_slug(permalink=123456)
