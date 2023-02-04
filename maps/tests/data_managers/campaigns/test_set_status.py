from datetime import datetime

import pytest

from maps_adv.adv_store.v2.lib.data_managers.campaigns import (
    CampaignsChangeLogActionName,
)
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum
from maps_adv.common.helpers import Any

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", list(CampaignStatusEnum))
async def test_will_set_campaign_status(status, factory, campaigns_dm, con):
    campaign_id = (await factory.create_campaign())["id"]

    await campaigns_dm.set_status(campaign_id, author_id=1234, status=status)

    sql = """
        SELECT EXISTS(
            SELECT *
            FROM status_history
            WHERE campaign_id = $1
                AND author_id = 1234
                AND status = $2
        )
    """
    assert await con.fetchval(sql, campaign_id, status.name) is True


async def test_does_not_change_previously_set_status(factory, campaigns_dm, con):
    campaign_id = (await factory.create_campaign())["id"]

    await campaigns_dm.set_status(
        campaign_id, author_id=1234, status=CampaignStatusEnum.REVIEW
    )
    await campaigns_dm.set_status(
        campaign_id, author_id=5678, status=CampaignStatusEnum.ACTIVE
    )

    sql = """
        SELECT EXISTS(
            SELECT *
            FROM status_history
            WHERE campaign_id = $1
                AND author_id = 1234
                AND status = 'REVIEW'
        )
    """
    assert await con.fetchval(sql, campaign_id) is True


async def test_will_create_new_status_record(factory, campaigns_dm, con):
    campaign_id = (await factory.create_campaign())["id"]

    await campaigns_dm.set_status(
        campaign_id, author_id=1234, status=CampaignStatusEnum.REVIEW
    )

    sql = """
        SELECT COUNT(*)
        FROM status_history
        WHERE campaign_id = $1
    """
    assert await con.fetchval(sql, campaign_id) == 2  # because created in draft


async def test_returns_nothing(factory, campaigns_dm):
    campaign_id = (await factory.create_campaign())["id"]

    got = await campaigns_dm.set_status(
        campaign_id, author_id=1234, status=CampaignStatusEnum.REVIEW
    )

    assert got is None


async def test_will_activate_paused_campaign(factory, campaigns_dm, con):
    campaign_id = (await factory.create_campaign())["id"]

    await campaigns_dm.set_status(
        campaign_id, author_id=5678, status=CampaignStatusEnum.PAUSED
    )

    await campaigns_dm.set_status(
        campaign_id, author_id=5678, status=CampaignStatusEnum.ACTIVE
    )

    sql = """
        SELECT status
        FROM status_history
        WHERE campaign_id = $1
        ORDER BY changed_datetime DESC
        LIMIT 1
    """
    assert await con.fetchval(sql, campaign_id) == "ACTIVE"


@pytest.mark.parametrize(
    ["params", "expected_system_metadata"],
    [
        ({}, {"action": "campaign.change_status"}),
        (
            {"change_log_action_name": CampaignsChangeLogActionName.CAMPAIGN_REVIEWED},
            {"action": "campaign.reviewed"},
        ),
    ],
)
async def test_adds_change_log_record_for_campaign_if_changing_status(
    params, expected_system_metadata, factory, campaigns_dm, con
):
    campaign_id = (await factory.create_campaign())["id"]

    await campaigns_dm.set_status(
        campaign_id, author_id=1234, status=CampaignStatusEnum.REVIEW, **params
    )

    result = await factory.list_campaign_change_log(campaign_id)
    assert result == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign_id,
            "author_id": 1234,
            "status": "REVIEW",
            "system_metadata": expected_system_metadata,
            "state_before": Any(dict),
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]
    assert result[0]["state_before"]["current_status_history"]["status"] == "DRAFT"
    assert result[0]["state_after"]["current_status_history"]["status"] == "REVIEW"
