from datetime import timedelta, timezone

import pytest

from maps_adv.adv_store.lib.data_managers.exceptions import CampaignNotFound
from maps_adv.adv_store.lib.data_managers.status_history import (
    get_current_campaign_status,
)
from maps_adv.adv_store.api.schemas.enums import CampaignStatusEnum

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("current_status", list(CampaignStatusEnum))
@pytest.mark.parametrize("prev_status", list(CampaignStatusEnum))
async def test_returns_latest_status(faker, factory, current_status, prev_status):
    past_dt = faker.past_datetime(tzinfo=timezone.utc)
    campaign = await factory.create_campaign()
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        author_id=33,
        status=prev_status,
        metadata={"some1": "metadata1"},
        changed_datetime=past_dt - timedelta(days=2),
    )
    await factory.create_status_entry(
        campaign_id=campaign["id"],
        author_id=44,
        status=current_status,
        metadata={"some2": "metadata2"},
        changed_datetime=past_dt - timedelta(days=1),
    )

    assert await get_current_campaign_status(campaign["id"]) == current_status


async def test_raises_for_inexistent_campaign():
    with pytest.raises(CampaignNotFound):
        await get_current_campaign_status(987)
