import pytest

from maps_adv.adv_store.v2.lib.data_managers.exceptions import DirectModerationNotFound

pytestmark = [pytest.mark.asyncio]


async def test_will_set_campaign_direct_moderation(factory, campaigns_dm):
    campaign_id = (await factory.create_campaign())["id"]
    moderation_id = await factory.create_campaign_direct_moderation(
        campaign_id=campaign_id
    )

    await campaigns_dm.set_direct_moderation(campaign_id, moderation_id)

    result = await factory.retrieve_campaign(campaign_id)
    assert result["direct_moderation_id"] == moderation_id


async def test_will_throw_error_if_no_direct_moderation(factory, campaigns_dm):
    campaign_id = (await factory.create_campaign())["id"]

    with pytest.raises(DirectModerationNotFound):
        await campaigns_dm.set_direct_moderation(campaign_id, 100500)
