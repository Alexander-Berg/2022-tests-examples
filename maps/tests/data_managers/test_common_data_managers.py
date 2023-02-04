import pytest

from maps_adv.adv_store.lib.data_managers.common import does_campaign_exist

pytestmark = [pytest.mark.asyncio]


async def test_campaign_exists(factory):
    campaign = await factory.create_campaign()

    assert await does_campaign_exist(campaign["id"])


async def test_campaign_does_not_exists(factory):
    assert not (await does_campaign_exist(1234))
