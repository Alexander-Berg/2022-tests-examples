import pytest

from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio]


async def test_returns_campaigns_for_requested_ids(factory, campaigns_dm):
    campaign1_id = (await factory.create_campaign())["id"]
    campaign2_id = (await factory.create_campaign())["id"]
    # noise
    await factory.create_campaign()
    await factory.create_campaign()

    got = await campaigns_dm.retrieve_campaign_data_for_monitorings(
        ids=[campaign1_id, campaign2_id]
    )

    assert got == [
        {"id": campaign1_id, "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER},
        {"id": campaign2_id, "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER},
    ]


async def test_returns_nothing_if_no_campaigns(factory, campaigns_dm):
    got = await campaigns_dm.retrieve_campaign_data_for_monitorings(ids=[111, 222])

    assert got == []
