import pytest
from datetime import datetime, timedelta, timezone

pytestmark = [pytest.mark.asyncio]


async def test_creates_with_empty_paid_till(factory, campaigns_dm):
    campaign = await factory.create_campaign()

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["paid_till"] is None


async def test_sets_paid_till(factory, campaigns_dm):
    campaign = await factory.create_campaign()
    paid_till = datetime.now(timezone.utc) + timedelta(days=1)
    await campaigns_dm.set_paid_till(campaign["id"], paid_till)

    result = await campaigns_dm.retrieve_campaign(campaign["id"])

    assert result["paid_till"] == paid_till


async def test_updates_paid_till(factory, campaigns_dm):
    campaign = await factory.create_campaign()

    paid_till = datetime.now(timezone.utc) + timedelta(weeks=1)
    await campaigns_dm.set_paid_till(campaign["id"], paid_till)
    result = await campaigns_dm.retrieve_campaign(campaign["id"])
    assert result["paid_till"] == paid_till

    paid_till = datetime.now(timezone.utc) + timedelta(weeks=2)
    await campaigns_dm.set_paid_till(campaign["id"], paid_till)
    result = await campaigns_dm.retrieve_campaign(campaign["id"])
    assert result["paid_till"] == paid_till

    await campaigns_dm.set_paid_till(campaign["id"], None)
    result = await campaigns_dm.retrieve_campaign(campaign["id"])
    assert result["paid_till"] is None
