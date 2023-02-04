import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio]


async def test_fetch_structured_landing_photos(factory, dm):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        photos=[{"id": "1", "url": "url1"}, {"id": "2", "url": "url2"}],
        photo_settings={"hidden_ids": ["2"]},
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", permalink="54321", stable_version=data_id
    )

    expected_result = [
        {"id": "1", "url": "url1", "hidden": False},
        {"id": "2", "url": "url2", "hidden": True},
    ]
    assert await dm.fetch_landing_photos(biz_id=22, version=LandingVersion.STABLE) == expected_result
    assert await dm.fetch_landing_photos(biz_id=22, version=LandingVersion.UNSTABLE) == []


async def test_fetch_unstructured_landing_photos(factory, dm):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        photos=["url1", "url2"],
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", permalink="54321", stable_version=data_id
    )

    expected_result = [
        {"url": "url1", "hidden": False},
        {"url": "url2", "hidden": False},
    ]
    assert await dm.fetch_landing_photos(biz_id=22, version=LandingVersion.STABLE) == expected_result
    assert await dm.fetch_landing_photos(biz_id=22, version=LandingVersion.UNSTABLE) == []
