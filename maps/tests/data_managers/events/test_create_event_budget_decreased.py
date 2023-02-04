from decimal import Decimal

import pytest

from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio]


async def test_creates_campaign_event(factory, events_dm, con):

    timestamp = dt("2019-01-01 00:00:00")
    campaign_id = (await factory.create_campaign())["id"]

    event_id = await events_dm.create_event_budget_decreased(
        timestamp=timestamp,
        campaign_id=campaign_id,
        initiator_id=123,
        prev_budget=Decimal("1000"),
    )

    events = await factory.retrieve_campaign_events(campaign_id)

    assert events == [
        {
            "id": event_id,
            "timestamp": timestamp,
            "campaign_id": campaign_id,
            "event_type": "BUDGET_DECREASED",
            "event_data": {"initiator": 123, "prev_budget": "1000"},
        }
    ]
