import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio]


async def test_return_status_after_creation(factory, campaigns_dm):
    campaign = await factory.create_campaign()

    result = await campaigns_dm.get_status(campaign["id"])

    assert result == CampaignStatusEnum.DRAFT


async def test_return_status_after_change(factory, campaigns_dm):
    campaign = await factory.create_campaign()
    await factory.set_status(campaign["id"], status=CampaignStatusEnum.ACTIVE)

    result = await campaigns_dm.get_status(campaign["id"])

    assert result == CampaignStatusEnum.ACTIVE
