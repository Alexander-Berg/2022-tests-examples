import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum
from maps_adv.common.helpers import Any, dt

pytestmark = [pytest.mark.asyncio]


async def test_moves_state_after_to_state_before(con, factory, campaigns_dm):
    campaign_id = (await factory.create_campaign())["id"]
    change_log_id = (
        await factory.create_campaign_change_log(
            campaign_id=campaign_id, state_before={}, state_after={"state": "after"}
        )
    )["id"]

    await campaigns_dm._refresh_campaigns_change_logs(con, ids=[change_log_id])

    result = await factory.list_campaign_change_log(campaign_id)
    assert result[0]["state_before"] == {"state": "after"}


async def test_adds_in_state_after_new_state_of_campaign(con, factory, campaigns_dm):
    campaign_id = (await factory.create_campaign())["id"]
    change_log_id = (
        await factory.create_campaign_change_log(
            campaign_id=campaign_id, state_after={"state": "after"}
        )
    )["id"]

    await campaigns_dm._refresh_campaigns_change_logs(con, ids=[change_log_id])

    result = await factory.list_campaign_change_log(campaign_id)
    assert result[0]["state_after"] == {
        "id": Any(int),
        "campaign": Any(dict),
        "billing": Any(dict),
        "placing": Any(dict),
        "week_schedule": None,
        "discounts": None,
        "action": Any(dict),
        "creative": Any(dict),
        "current_status_history": Any(dict),
    }


async def test_updates_status_to_actually(con, factory, campaigns_dm):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.DRAFT))["id"]
    change_log_id = (
        await factory.create_campaign_change_log(
            campaign_id=campaign_id, status=CampaignStatusEnum.PAUSED
        )
    )["id"]

    await campaigns_dm._refresh_campaigns_change_logs(con, ids=[change_log_id])

    result = await factory.list_campaign_change_log(campaign_id)
    assert result[0]["status"] == "DRAFT"


@pytest.mark.parametrize(
    ["field", "expected_result"],
    [
        ("author_id", 321),
        ("created_at", dt("2000-01-01 00:00:00")),
        ("system_metadata", {"system": "metadata"}),
    ],
)
async def test_doesnt_change_some_fields(
    field, expected_result, con, factory, campaigns_dm
):
    campaign_id = (
        await factory.create_campaign(author_id=132, status=CampaignStatusEnum.DRAFT)
    )["id"]
    change_log_id = (
        await factory.create_campaign_change_log(
            campaign_id=campaign_id,
            author_id=321,
            status=CampaignStatusEnum.PAUSED,
            created_at=dt("2000-01-01 00:00:00"),
            system_metadata={"system": "metadata"},
            state_before={},
            state_after={},
            is_latest=False,
        )
    )["id"]

    await campaigns_dm._refresh_campaigns_change_logs(con, ids=[change_log_id])

    result = await factory.list_campaign_change_log(campaign_id)
    assert result[0][field] == expected_result


async def test_doesnt_change_other_change_log_entries(con, factory, campaigns_dm):
    campaign_id = (await factory.create_campaign(status=CampaignStatusEnum.DRAFT))["id"]
    await factory.create_campaign_change_log(
        campaign_id=campaign_id, status=CampaignStatusEnum.PAUSED
    )
    change_log_id = (
        await factory.create_campaign_change_log(status=CampaignStatusEnum.DONE)
    )["id"]

    await campaigns_dm._refresh_campaigns_change_logs(con, ids=[change_log_id])

    result = await factory.list_campaign_change_log(campaign_id)
    assert result[0]["status"] == "PAUSED"


async def test_updates_batch_change_log_entries(con, factory, campaigns_dm):
    change_log1 = await factory.create_campaign_change_log(
        status=CampaignStatusEnum.PAUSED
    )
    change_log2 = await factory.create_campaign_change_log(
        status=CampaignStatusEnum.DONE
    )

    await campaigns_dm._refresh_campaigns_change_logs(
        con, ids=[change_log1["id"], change_log2["id"]]
    )

    result1 = await factory.list_campaign_change_log(change_log1["campaign_id"])
    result2 = await factory.list_campaign_change_log(change_log2["campaign_id"])
    assert result1[0]["status"] == "DRAFT"
    assert result2[0]["status"] == "DRAFT"
