from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.adv_store.lib.data_managers import CampaignStatusEnum
from maps_adv.adv_store.lib.data_managers.enums import ReasonCampaignStoppedEnum
from maps_adv.adv_store.lib.data_managers.unstop_campaigns import (
    stop_campaigns_date_end,
)
from maps_adv.common.helpers import Any

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "prev_status", [CampaignStatusEnum.ACTIVE, CampaignStatusEnum.PAUSED]
)
async def test_campaign_gets_stopped_if_finished(faker, factory, prev_status):
    end_datetime = faker.past_datetime()
    start_datetime = end_datetime - timedelta(days=3)
    campaign = await factory.create_campaign(
        start_datetime=start_datetime, end_datetime=end_datetime
    )
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        changed_datetime=faker.past_datetime(),
        status=prev_status,
    )

    await stop_campaigns_date_end(datetime.now(tz=timezone.utc))

    assert await factory.get_campaign_statuses(campaign["id"]) == [
        prev_status,
        CampaignStatusEnum.DONE,
    ]


@pytest.mark.parametrize(
    "prev_status", [CampaignStatusEnum.ACTIVE, CampaignStatusEnum.PAUSED]
)
async def test_campaign_not_gets_stopped_if_not_finished(faker, factory, prev_status):
    end_datetime = faker.future_datetime()
    start_datetime = end_datetime - timedelta(days=3)
    campaign = await factory.create_campaign(
        start_datetime=start_datetime, end_datetime=end_datetime
    )
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        changed_datetime=faker.past_datetime(),
        status=prev_status,
    )

    await stop_campaigns_date_end(datetime.now(tz=timezone.utc))

    assert await factory.get_campaign_statuses(campaign["id"]) == [prev_status]


@pytest.mark.parametrize(
    "prev_status",
    [
        CampaignStatusEnum.ARCHIVED,
        CampaignStatusEnum.DONE,
        CampaignStatusEnum.DRAFT,
        CampaignStatusEnum.REJECTED,
        CampaignStatusEnum.REVIEW,
    ],
)
async def test_campaign_not_stopped_if_not_active_or_paused(
    faker, factory, prev_status
):
    end_datetime = faker.past_datetime()
    start_datetime = end_datetime - timedelta(days=3)
    campaign = await factory.create_campaign(
        start_datetime=start_datetime, end_datetime=end_datetime
    )
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        changed_datetime=faker.past_datetime(),
        status=prev_status,
    )

    await stop_campaigns_date_end(datetime.now(tz=timezone.utc))

    assert await factory.get_campaign_statuses(campaign["id"]) == [prev_status]


async def test_when_stopped_metadata_is_set(faker, factory):
    end_datetime = faker.past_datetime()
    start_datetime = end_datetime - timedelta(days=3)
    campaign = await factory.create_campaign(
        start_datetime=start_datetime, end_datetime=end_datetime
    )

    await factory.create_status_entry(
        campaign_id=campaign["id"],
        changed_datetime=faker.past_datetime(),
        status=CampaignStatusEnum.ACTIVE,
    )

    due_to = datetime.now(tz=timezone.utc)
    await stop_campaigns_date_end(due_to)

    curr_status_entry = (
        await factory.get_all_status_history_for_campaign(campaign["id"])
    )[-1]
    assert curr_status_entry["metadata"] == {
        "due_to": due_to.timestamp(),
        "reason_stopped": ReasonCampaignStoppedEnum.END_DATETIME.name,
    }


@pytest.mark.parametrize(
    ["timezone", "end_datetime", "passed_time", "updated_end_datetime"],
    [
        (
            "Europe/Moscow",
            datetime(2022, 6, 25, 20, 59, 59, tzinfo=timezone.utc),
            timedelta(seconds=1),
            datetime(2022, 6, 26, 20, 59, 59, tzinfo=timezone.utc),
        ),
        (
            "Asia/Novosibirsk",
            datetime(2022, 6, 25, 16, 59, 59, tzinfo=timezone.utc),
            timedelta(minutes=1),
            datetime(2022, 6, 26, 16, 59, 59, tzinfo=timezone.utc),
        ),
        (
            "Asia/Novosibirsk",
            datetime(2022, 6, 25, 16, 59, 59, tzinfo=timezone.utc),
            timedelta(minutes=1) + timedelta(days=3),
            datetime(2022, 6, 29, 16, 59, 59, tzinfo=timezone.utc),
        ),
    ]
)
async def test_auto_prolongation(
    timezone, end_datetime, passed_time, updated_end_datetime, factory
):
    start_datetime = end_datetime - timedelta(days=3)
    campaign = await factory.create_campaign(
        start_datetime=start_datetime,
        end_datetime=end_datetime,
        timezone=timezone,
        settings={
            "auto_prolongation": True
        }
    )
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        changed_datetime=start_datetime,
        status=CampaignStatusEnum.ACTIVE,
    )

    await stop_campaigns_date_end(end_datetime + passed_time)

    result_campaigns = await factory.get_all_campaigns()
    assert len(result_campaigns) == 1
    assert result_campaigns[0]["end_datetime"] == updated_end_datetime

    change_log = await factory.list_campaign_change_log(campaign_id=campaign["id"])
    assert len(change_log) == 1
    assert change_log[0]["state_before"]["campaign"]["end_datetime"] == end_datetime.isoformat()
    assert change_log[0]["state_after"]["campaign"]["end_datetime"] == updated_end_datetime.isoformat()
    assert change_log[0]["system_metadata"] == {
        "action" : "campaign.prolongated",
    }

    status_history = await factory.get_all_status_history_for_campaign(campaign["id"])
    assert len(status_history) == 1  # no status enrties added


async def test_adds_change_log_record_for_updated_campaign(factory):
    end_datetime = datetime.now() - timedelta(days=3)
    start_datetime = end_datetime - timedelta(days=3)
    campaign = await factory.create_campaign(
        start_datetime=start_datetime, end_datetime=end_datetime
    )
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        changed_datetime=start_datetime,
        status=CampaignStatusEnum.ACTIVE,
    )

    due_to = datetime.now(tz=timezone.utc)
    await stop_campaigns_date_end(due_to)

    result = await factory.list_campaign_change_log(campaign_id=campaign["id"])
    assert result == [
        {
            "id": Any(int),
            "created_at": Any(datetime),
            "campaign_id": campaign["id"],
            "author_id": 0,
            "status": "DONE",
            "system_metadata": {
                "action": "campaign.stopped",
                "due_to": due_to.timestamp(),
                "reason_stopped": "END_DATETIME",
            },
            "state_before": Any(dict),
            "state_after": Any(dict),
            "is_latest": True,
        }
    ]
