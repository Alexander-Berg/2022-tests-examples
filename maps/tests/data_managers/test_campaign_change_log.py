from datetime import datetime

import pytest

from maps_adv.adv_store.lib.data_managers import campaign_change_log
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum
from maps_adv.common.helpers import Any, dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture()
def create_campaign(factory):
    async def wrapper(**kwargs):
        kwargs["status"] = kwargs.get("status", CampaignStatusEnum.DRAFT)
        kwargs["author_id"] = kwargs.get("author_id", 123)
        return await factory.create_campaign_with_any_status(**kwargs)

    return wrapper


async def test_adds_change_log(create_campaign, factory):
    campaign_id = (await create_campaign(status=CampaignStatusEnum.ACTIVE))["id"]

    await campaign_change_log.insert_campaign_change_log(
        campaign_id=campaign_id,
        author_id=333,
        action_name="action.any.name",
        extparam="value",
    )

    result = await factory.list_campaign_change_log(campaign_id)
    assert result == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign_id,
            "author_id": 333,
            "status": "ACTIVE",
            "is_latest": True,
            "system_metadata": {"action": "action.any.name", "extparam": "value"},
            "state_before": {},
            "state_after": {
                "id": Any(int),
                "campaign": Any(dict),
                "billing": Any(dict),
                "placing": Any(dict),
                "week_schedule": None,
                "discounts": None,
                "action": Any(dict),
                "creative": Any(dict),
                "current_status_history": Any(dict),
            },
        }
    ]


async def test_moves_state_after_to_state_before(create_campaign, factory):
    campaign_id = (await create_campaign())["id"]
    change_log_id = (
        await factory.create_campaign_change_log(
            campaign_id=campaign_id, state_before={}, state_after={"state": "after"}
        )
    )["id"]

    await campaign_change_log.refresh_campaigns_change_logs(ids=[change_log_id])

    result = await factory.list_campaign_change_log(campaign_id)
    assert result[0]["state_before"] == {"state": "after"}


async def test_adds_in_state_after_new_state_of_campaign(create_campaign, factory):
    campaign_id = (await create_campaign())["id"]
    change_log_id = (
        await factory.create_campaign_change_log(
            campaign_id=campaign_id, state_after={"state": "after"}
        )
    )["id"]

    await campaign_change_log.refresh_campaigns_change_logs(ids=[change_log_id])

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


async def test_updates_status_to_actually(create_campaign, factory):
    campaign_id = (await create_campaign(status=CampaignStatusEnum.DRAFT))["id"]
    change_log_id = (
        await factory.create_campaign_change_log(
            campaign_id=campaign_id, status=CampaignStatusEnum.PAUSED
        )
    )["id"]

    await campaign_change_log.refresh_campaigns_change_logs(ids=[change_log_id])

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
    field, expected_result, create_campaign, factory
):
    campaign_id = (
        await create_campaign(author_id=132, status=CampaignStatusEnum.DRAFT)
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

    await campaign_change_log.refresh_campaigns_change_logs(ids=[change_log_id])

    result = await factory.list_campaign_change_log(campaign_id)
    assert result[0][field] == expected_result


async def test_doesnt_change_other_change_log_entries(create_campaign, factory):
    campaign_id = (await create_campaign(status=CampaignStatusEnum.DRAFT))["id"]
    await factory.create_campaign_change_log(
        campaign_id=campaign_id, status=CampaignStatusEnum.PAUSED
    )
    change_log_id = (
        await factory.create_campaign_change_log(status=CampaignStatusEnum.DONE)
    )["id"]

    await campaign_change_log.refresh_campaigns_change_logs(ids=[change_log_id])

    result = await factory.list_campaign_change_log(campaign_id)
    assert result[0]["status"] == "PAUSED"


async def test_updates_batch_change_log_entries(create_campaign, factory):
    campaign1_id = (await create_campaign(status=CampaignStatusEnum.DRAFT))["id"]
    campaign2_id = (await create_campaign(status=CampaignStatusEnum.DRAFT))["id"]
    change_log1 = await factory.create_campaign_change_log(
        campaign_id=campaign1_id, status=CampaignStatusEnum.PAUSED
    )
    change_log2 = await factory.create_campaign_change_log(
        campaign_id=campaign2_id, status=CampaignStatusEnum.DONE
    )

    await campaign_change_log.refresh_campaigns_change_logs(
        ids=[change_log1["id"], change_log2["id"]]
    )

    result1 = await factory.list_campaign_change_log(change_log1["campaign_id"])
    result2 = await factory.list_campaign_change_log(change_log2["campaign_id"])
    assert result1[0]["status"] == "DRAFT"
    assert result2[0]["status"] == "DRAFT"
