from datetime import datetime

import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignDirectModerationWorkflowEnum
from maps_adv.adv_store.v2.tests import Any

pytestmark = [pytest.mark.asyncio]


async def test_creates_campaign_moderation(factory, moderation_dm, con):

    campaign_id = (await factory.create_campaign())["id"]

    moderation_id = await moderation_dm.create_direct_moderation_for_campaign(
        campaign_id=campaign_id, reviewer_uid=100500
    )

    moderation = await factory.get_campaign_direct_moderation(moderation_id)

    assert moderation == {
        "id": moderation_id,
        "created_at": Any(datetime),
        "campaign_id": campaign_id,
        "reviewer_uid": 100500,
        "status": "NEW",
        "workflow": "COMMON",
        "verdicts": [],
    }


@pytest.mark.parametrize(
    "workflow",
    [
        CampaignDirectModerationWorkflowEnum.AUTO_ACCEPT,
        CampaignDirectModerationWorkflowEnum.AUTO_REJECT,
        CampaignDirectModerationWorkflowEnum.MANUAL_ONLY,
    ],
)
async def test_creates_campaign_moderation_with_manual_workflow(
    factory, moderation_dm, workflow
):

    campaign_id = (await factory.create_campaign())["id"]

    moderation_id = await moderation_dm.create_direct_moderation_for_campaign(
        campaign_id=campaign_id, reviewer_uid=100500, workflow=workflow
    )

    moderation = await factory.get_campaign_direct_moderation(moderation_id)

    assert moderation == {
        "id": moderation_id,
        "created_at": Any(datetime),
        "campaign_id": campaign_id,
        "reviewer_uid": 100500,
        "status": "NEW",
        "workflow": workflow.name,
        "verdicts": [],
    }
