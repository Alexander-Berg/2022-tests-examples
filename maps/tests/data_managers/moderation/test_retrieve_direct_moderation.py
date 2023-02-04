import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignDirectModerationStatusEnum,
    CampaignDirectModerationWorkflowEnum,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_moderation(factory, moderation_dm):
    campaign_id = (await factory.create_campaign())["id"]
    moderation_id = await factory.create_campaign_direct_moderation(
        campaign_id=campaign_id,
        reviewer_uid=1234567,
        created_at=dt("2020-09-30 00:00:00"),
    )

    got = await moderation_dm.retrieve_direct_moderation(moderation_id)

    assert got == {
        "campaign_id": campaign_id,
        "id": moderation_id,
        "created_at": dt("2020-09-30 00:00:00"),
        "reviewer_uid": 1234567,
        "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
        "status": CampaignDirectModerationStatusEnum.NEW,
        "verdicts": [],
    }
