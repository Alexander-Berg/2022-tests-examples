import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import (
    NoDataForBizId,
    NoStableVersionForPublishing,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize("old_published, new_published", [(True, False), (False, True)])
async def test_uses_dm_if_publicity_changes(domain, dm, old_published, new_published):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "cafe",
        "stable_version": 11,
        "unstable_version": 22,
        "published": old_published,
    }

    await domain.set_landing_publicity(biz_id=15, is_published=new_published)

    dm.set_landing_publicity.assert_called_with(biz_id=15, is_published=new_published)


@pytest.mark.parametrize("published", [True, False])
async def test_does_not_use_dm_if_publicity_not_changes(domain, dm, published):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "cafe",
        "stable_version": 11,
        "unstable_version": 22,
        "published": published,
    }

    await domain.set_landing_publicity(biz_id=15, is_published=published)

    dm.set_landing_publicity.assert_not_called()


async def test_returns_nothing(domain, dm):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "cafe",
        "stable_version": 11,
        "unstable_version": 22,
        "published": True,
    }

    got = await domain.set_landing_publicity(biz_id=15, is_published=True)

    assert got is None


async def test_raises_for_unknown_biz_id(domain, dm):
    dm.fetch_biz_state.coro.return_value = None

    with pytest.raises(NoDataForBizId):
        await domain.set_landing_publicity(biz_id=15, is_published=True)


async def test_raises_if_publish_landing_without_stable_version(domain, dm):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "cafe",
        "stable_version": None,
        "unstable_version": 22,
        "published": False,
    }

    with pytest.raises(NoStableVersionForPublishing):
        await domain.set_landing_publicity(biz_id=15, is_published=True)


async def test_does_not_raise_if_unpublish_landing_without_stable_version(domain, dm):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "cafe",
        "stable_version": None,
        "unstable_version": 22,
        "published": True,
    }

    try:
        await domain.set_landing_publicity(biz_id=15, is_published=False)
    except NoStableVersionForPublishing:
        pytest.fail("Should not raise NoStableVersionForPublishing")
