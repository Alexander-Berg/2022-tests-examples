from datetime import datetime

import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignDirectModerationStatusEnum
from maps_adv.adv_store.v2.tests import Any

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", list(CampaignDirectModerationStatusEnum))
async def test_update_campaign_direct_moderation_status(factory, moderation_dm, status):

    campaign_id = (await factory.create_campaign())["id"]

    moderation_id = await factory.create_campaign_direct_moderation(campaign_id)

    await moderation_dm.update_direct_moderation(moderation_id, status)

    moderation = await factory.get_campaign_direct_moderation(moderation_id)

    assert moderation == {
        "id": moderation_id,
        "created_at": Any(datetime),
        "campaign_id": campaign_id,
        "reviewer_uid": 1234567,
        "status": status.name,
        "workflow": "COMMON",
        "verdicts": [],
    }
