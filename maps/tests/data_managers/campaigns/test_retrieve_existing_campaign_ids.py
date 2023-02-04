import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_only_existing_campaigns(factory, campaigns_dm):
    campaign_id1 = (await factory.create_campaign())["id"]
    campaign_id2 = (await factory.create_campaign())["id"]

    got = await campaigns_dm.retrieve_existing_campaign_ids(
        {campaign_id1, campaign_id2, 9999}
    )

    assert got == {campaign_id1, campaign_id2}


async def test_returns_nothing_if_nothing_requested(factory, campaigns_dm):
    await factory.create_campaign()

    got = await campaigns_dm.retrieve_existing_campaign_ids(set())

    assert got == set()


async def test_returns_nothing_if_no_campaigns_in_list_exists(factory, campaigns_dm):
    await factory.create_campaign()

    got = await campaigns_dm.retrieve_existing_campaign_ids({8888, 9999})

    assert got == set()
