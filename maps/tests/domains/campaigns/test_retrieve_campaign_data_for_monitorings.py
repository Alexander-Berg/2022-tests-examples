import pytest

from maps_adv.common.helpers.enums import CampaignTypeEnum

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_calls_dm_and_returns_data(campaigns_domain, campaigns_dm):
    campaigns_dm.retrieve_campaign_data_for_monitorings.coro.return_value = [
        {"id": 1, "campaign_type": CampaignTypeEnum.OVERVIEW_BANNER}
    ]

    got = await campaigns_domain.retrieve_campaign_data_for_monitorings([1])

    campaigns_dm.retrieve_campaign_data_for_monitorings.assert_called_with([1])

    assert got == [{"id": 1, "campaign_type": CampaignTypeEnum.OVERVIEW_BANNER}]
