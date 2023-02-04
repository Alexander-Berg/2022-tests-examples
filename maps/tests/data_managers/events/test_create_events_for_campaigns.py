import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignEventTypeEnum
from maps_adv.adv_store.v2.tests import Any, dt

pytestmark = [pytest.mark.asyncio]


async def test_creates_campaign_events(factory, events_dm, con):

    timestamp = dt("2019-01-01 00:00:00")
    campaign_1_id = (await factory.create_campaign())["id"]
    campaign_2_id = (await factory.create_campaign())["id"]
    campaign_3_id = (await factory.create_campaign())["id"]

    await events_dm.create_events_for_campaigns(
        timestamp=timestamp,
        campaign_ids=[campaign_1_id, campaign_2_id, campaign_3_id],
        event_type=CampaignEventTypeEnum.BUDGET_DECREASED,
        event_data={"data": "some_data"},
    )

    events = await factory.retrieve_campaign_events(campaign_1_id)
    events.extend(await factory.retrieve_campaign_events(campaign_2_id))
    events.extend(await factory.retrieve_campaign_events(campaign_3_id))

    assert events == [
        {
            "id": Any(int),
            "timestamp": timestamp,
            "campaign_id": campaign_1_id,
            "event_type": "BUDGET_DECREASED",
            "event_data": {"data": "some_data"},
        },
        {
            "id": Any(int),
            "timestamp": timestamp,
            "campaign_id": campaign_2_id,
            "event_type": "BUDGET_DECREASED",
            "event_data": {"data": "some_data"},
        },
        {
            "id": Any(int),
            "timestamp": timestamp,
            "campaign_id": campaign_3_id,
            "event_type": "BUDGET_DECREASED",
            "event_data": {"data": "some_data"},
        },
    ]
