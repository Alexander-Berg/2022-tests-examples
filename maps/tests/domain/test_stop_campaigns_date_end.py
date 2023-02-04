from datetime import timedelta

import pytest

from maps_adv.adv_store.lib.domain import CampaignStatusEnum, ReasonCampaignStoppedEnum
from maps_adv.adv_store.lib.domain.unstop_campaigns import stop_campaigns_date_end
from maps_adv.adv_store.tests.utils import Any

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

    await stop_campaigns_date_end()

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

    await stop_campaigns_date_end()

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

    await stop_campaigns_date_end()

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

    await stop_campaigns_date_end()

    curr_status_entry = (
        await factory.get_all_status_history_for_campaign(campaign["id"])
    )[-1]
    assert curr_status_entry["metadata"] == {
        "due_to": Any(float),
        "reason_stopped": ReasonCampaignStoppedEnum.END_DATETIME.name,
    }
