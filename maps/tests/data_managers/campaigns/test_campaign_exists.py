import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_true_for_existing_campaign(factory, campaigns_dm):
    campaign_id = (await factory.create_campaign())["id"]

    assert await campaigns_dm.campaign_exists(campaign_id) is True


async def test_returns_false_for_nonexistent_campaign(campaigns_dm):
    assert await campaigns_dm.campaign_exists(111) is False
