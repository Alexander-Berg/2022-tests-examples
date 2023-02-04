import pytest

from maps_adv.adv_store.v2.tests import Any, dt

pytestmark = [pytest.mark.asyncio]


async def test_creates_campaign_events(factory, events_dm, con):

    timestamp = dt("2019-01-01 00:00:00")
    campaign_1_id = (await factory.create_campaign())["id"]
    campaign_2_id = (await factory.create_campaign())["id"]
    campaign_3_id = (await factory.create_campaign())["id"]

    await events_dm.create_events_stopped_budget_reached(
        timestamp=timestamp, campaign_ids=[campaign_1_id, campaign_2_id, campaign_3_id]
    )

    events = await factory.retrieve_campaign_events(campaign_1_id)
    events.extend(await factory.retrieve_campaign_events(campaign_2_id))
    events.extend(await factory.retrieve_campaign_events(campaign_3_id))

    assert events == [
        {
            "id": Any(int),
            "timestamp": timestamp,
            "campaign_id": campaign_1_id,
            "event_type": "STOPPED_BUDGET_REACHED",
            "event_data": {},
        },
        {
            "id": Any(int),
            "timestamp": timestamp,
            "campaign_id": campaign_2_id,
            "event_type": "STOPPED_BUDGET_REACHED",
            "event_data": {},
        },
        {
            "id": Any(int),
            "timestamp": timestamp,
            "campaign_id": campaign_3_id,
            "event_type": "STOPPED_BUDGET_REACHED",
            "event_data": {},
        },
    ]
