from datetime import datetime, timedelta, timezone
from maps_adv.adv_store.v2.lib.data_managers.exceptions import CampaignNotFound
from maps_adv.adv_store.v2.lib.domains.campaigns import PaidTillIsTooSmall

import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_data_manager(campaigns_domain, campaigns_dm):
    campaigns_dm.retrieve_campaign.coro.return_value = {"paid_till": None}
    paid_till = datetime.now(timezone.utc) + timedelta(days=1)
    await campaigns_domain.set_paid_till(100, paid_till)
    campaigns_dm.set_paid_till.assert_called_with(100, paid_till)


async def test_raises_on_no_campaign(campaigns_domain, campaigns_dm):
    campaigns_dm.retrieve_campaign.coro.side_effect = CampaignNotFound()
    paid_till = datetime.now(timezone.utc) + timedelta(weeks=1)
    with pytest.raises(CampaignNotFound):
        await campaigns_domain.set_paid_till(100, paid_till)


async def test_raises_on_before_now(campaigns_domain, campaigns_dm):
    campaigns_dm.retrieve_campaign.coro.return_value = {"paid_till": None}
    paid_till = datetime.now(timezone.utc) - timedelta(days=1)
    with pytest.raises(PaidTillIsTooSmall):
        await campaigns_domain.set_paid_till(100, paid_till)


async def test_raises_on_before_current(campaigns_domain, campaigns_dm):
    paid_till = datetime.now(timezone.utc) + timedelta(days=1)
    campaigns_dm.retrieve_campaign.coro.return_value = {"paid_till": paid_till}
    with pytest.raises(PaidTillIsTooSmall):
        await campaigns_domain.set_paid_till(100, paid_till - timedelta(days=1))
