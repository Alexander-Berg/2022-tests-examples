import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_fetch_landing_photos(domain, dm):
    db_photos = [{"id": "1", "url": "url1", "hidden": True}]
    dm.fetch_landing_photos.coro.return_value = db_photos
    result = await domain.fetch_landing_photos(biz_id=15, version=LandingVersion.STABLE)
    assert result == {"photos": db_photos}


async def test_hide_landing_photos(domain, dm):
    await domain.hide_landing_photos(biz_id=15, version=LandingVersion.STABLE, photo_id_to_hidden={"2", True})
