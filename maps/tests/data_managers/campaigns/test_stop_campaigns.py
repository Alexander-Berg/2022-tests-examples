from datetime import datetime

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    CampaignStatusEnum,
    ReasonCampaignStoppedEnum,
)
from maps_adv.adv_store.v2.tests import Any, dt

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


async def test_returns_nothing(factory, campaigns_dm):
    campaign_id1 = (await factory.create_campaign())["id"]
    campaign_id2 = (await factory.create_campaign())["id"]

    got = await campaigns_dm.stop_campaigns(
        processed_at=dt("2020-02-20 11:00:00"),
        campaigns=[
            dict(
                campaign_id=campaign_id1,
                reason_stopped=ReasonCampaignStoppedEnum.DAILY_BUDGET_REACHED,
            ),
            dict(
                campaign_id=campaign_id2,
                reason_stopped=ReasonCampaignStoppedEnum.DAILY_BUDGET_REACHED,
            ),
        ],
    )

    assert got is None


@pytest.mark.parametrize(
    "reason_stopped, expected_status",
    [
        (ReasonCampaignStoppedEnum.DAILY_BUDGET_REACHED, CampaignStatusEnum.ACTIVE),
        (ReasonCampaignStoppedEnum.BUDGET_REACHED, CampaignStatusEnum.DONE),
        (ReasonCampaignStoppedEnum.ORDER_LIMIT_REACHED, CampaignStatusEnum.ACTIVE),
    ],
)
async def test_updates_status_history(
    reason_stopped, expected_status, factory, campaigns_dm, con
):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]

    await campaigns_dm.stop_campaigns(
        processed_at=dt("2020-02-20 11:00:00"),
        campaigns=[dict(campaign_id=campaign_id, reason_stopped=reason_stopped)],
    )

    campaign_status_data = await factory.fetch_last_campaign_status_data(campaign_id)

    assert campaign_status_data == dict(
        campaign_id=campaign_id,
        author_id=0,
        status=expected_status.name,
        metadata={"processed_at": 1582196400, "reason_stopped": reason_stopped.name},
        changed_datetime=Any(datetime),
    )


async def test_closes_only_requested_campaigns(factory, campaigns_dm):
    campaign1_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]
    campaign2_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]
    campaign3_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]

    await campaigns_dm.stop_campaigns(
        processed_at=dt("2020-02-20 11:00:00"),
        campaigns=[
            dict(
                campaign_id=campaign1_id,
                processed_at=1582196400,
                reason_stopped=ReasonCampaignStoppedEnum.ORDER_LIMIT_REACHED,
            ),
            dict(
                campaign_id=campaign2_id,
                processed_at=1582196400,
                reason_stopped=ReasonCampaignStoppedEnum.BUDGET_REACHED,
            ),
        ],
    )

    campaign1_status = (await factory.fetch_last_campaign_status_data(campaign1_id))[
        "status"
    ]
    campaign2_status = (await factory.fetch_last_campaign_status_data(campaign2_id))[
        "status"
    ]
    campaign3_status = (await factory.fetch_last_campaign_status_data(campaign3_id))[
        "status"
    ]

    assert campaign1_status == CampaignStatusEnum.ACTIVE.name
    assert campaign2_status == CampaignStatusEnum.DONE.name
    assert campaign3_status == CampaignStatusEnum.ACTIVE.name


async def test_adds_change_log_records_for_updated_campaigns(factory, campaigns_dm):
    campaign1_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]
    campaign2_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]
    campaign3_id = (await factory.create_campaign(status=CampaignStatusEnum.ACTIVE))[
        "id"
    ]

    await campaigns_dm.stop_campaigns(
        processed_at=dt("2020-02-20 11:00:00"),
        campaigns=[
            dict(
                campaign_id=campaign1_id,
                processed_at=1582196400,
                reason_stopped=ReasonCampaignStoppedEnum.ORDER_LIMIT_REACHED,
            ),
            dict(
                campaign_id=campaign2_id,
                processed_at=1582196400,
                reason_stopped=ReasonCampaignStoppedEnum.BUDGET_REACHED,
            ),
        ],
    )

    result1 = await factory.list_campaign_change_log(campaign1_id)
    result2 = await factory.list_campaign_change_log(campaign2_id)
    result3 = await factory.list_campaign_change_log(campaign3_id)

    assert result1 == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign1_id,
            "author_id": 0,
            "status": "ACTIVE",
            "system_metadata": {
                "action": "campaign.stopped",
                "processed_at": 1582196400,
                "reason_stopped": "ORDER_LIMIT_REACHED",
            },
            "state_before": Any(dict),
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]
    assert result2 == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign2_id,
            "author_id": 0,
            "status": "DONE",
            "system_metadata": {
                "action": "campaign.stopped",
                "processed_at": 1582196400,
                "reason_stopped": "BUDGET_REACHED",
            },
            "state_before": Any(dict),
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]
    assert result3 == []
    assert result1[0]["state_before"]["current_status_history"]["status"] == "ACTIVE"
    assert result2[0]["state_before"]["current_status_history"]["status"] == "ACTIVE"
