import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("versions",),
    [
        (["stable_version"],),
        (["unstable_version"],),
        (["stable_version", "unstable_version"],),
    ],
)
async def test_deletes_landing_data(factory, dm, versions):
    version_params = {
        version: await factory.insert_landing_data() for version in versions
    }
    await factory.insert_biz_state(biz_id=11, **version_params)

    await dm.delete_landing_by_biz_id(biz_id=11)

    assert await factory.list_all_landing_data() == []
    assert await factory.fetch_biz_state(biz_id=11) is None


async def test_does_not_affect_another_biz_state(factory, dm):
    for biz_id, slug in [(11, "cafe1"), (22, "cafe2")]:
        data_id = await factory.insert_landing_data()
        await factory.insert_biz_state(biz_id=biz_id, slug=slug, stable_version=data_id)

    await dm.delete_landing_by_biz_id(biz_id=11)

    assert await factory.fetch_biz_state(biz_id=22) is not None
    landing_data = await factory.fetch_landing_data(
        biz_id=22, kind=LandingVersion.STABLE
    )
    assert landing_data is not None


async def test_raises_if_no_such_biz_id(dm):
    with pytest.raises(NoDataForBizId):
        await dm.delete_landing_by_biz_id(biz_id=15)
