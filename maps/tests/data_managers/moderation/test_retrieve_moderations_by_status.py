from datetime import datetime

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignDirectModerationStatusEnum,
    CampaignDirectModerationWorkflowEnum,
)
from maps_adv.common.helpers import Any

pytestmark = [pytest.mark.asyncio]


async def test_returns_nothing_if_no_moderations(moderation_dm):
    got = await moderation_dm.retrieve_direct_moderations_by_status(
        CampaignDirectModerationStatusEnum.NEW
    )

    assert got == []


@pytest.mark.parametrize("status", list(CampaignDirectModerationStatusEnum))
async def test_returns_moderations_with_status(factory, moderation_dm, status):
    campaign_id = (await factory.create_campaign())["id"]
    moderation_id = await factory.create_campaign_direct_moderation(
        campaign_id=campaign_id, status=status
    )
    got = await moderation_dm.retrieve_direct_moderations_by_status(status)

    assert got == [
        {
            "id": moderation_id,
            "created_at": Any(datetime),
            "campaign_id": campaign_id,
            "reviewer_uid": 1234567,
            "status": status,
            "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
            "verdicts": [],
        }
    ]


@pytest.mark.parametrize("status", list(CampaignDirectModerationStatusEnum))
async def test_does_not_return_moderations_with_other_status(
    factory, moderation_dm, status
):
    campaign_id = (await factory.create_campaign())["id"]
    moderation_id = await factory.create_campaign_direct_moderation(
        campaign_id=campaign_id, status=status
    )

    other_statuses = [
        st for st in iter(CampaignDirectModerationStatusEnum) if st != status
    ]

    for other_status in other_statuses:
        other_campaign_id = (await factory.create_campaign())["id"]
        await factory.create_campaign_direct_moderation(
            campaign_id=other_campaign_id, status=other_status
        )

    got = await moderation_dm.retrieve_direct_moderations_by_status(status)

    assert got == [
        {
            "id": moderation_id,
            "created_at": Any(datetime),
            "campaign_id": campaign_id,
            "reviewer_uid": 1234567,
            "status": status,
            "workflow": CampaignDirectModerationWorkflowEnum.COMMON,
            "verdicts": [],
        }
    ]
